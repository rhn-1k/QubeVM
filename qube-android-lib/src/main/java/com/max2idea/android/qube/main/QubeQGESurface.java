/*
Copyright (C) Max Kastanas 2012
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.main;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.max2idea.android.qube.machine.MachineAction;

import java.util.ArrayList;

/**
 * Draws the guest framebuffer (see QubeGfx) and turns touch input into pointer events,
 * sent via QubeInput. Trackpad mode uses relative deltas run through PointerAcceleration
 * for a smooth feel.
 */
public class QubeQGESurface extends View implements View.OnTouchListener {
    private static final String TAG = "QubeQGESurface";

    MouseState mouseState = new MouseState();
    private boolean firstTouch = false;

    private final QubeQGEActivity QGEActivity;
    private final PointerAcceleration pointerAcceleration;
    private final Paint framePaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Matrix frameMatrix = new Matrix();
    private Bitmap frameBitmap;
 
    public QubeQGESurface(QubeQGEActivity QGEActivity, Context context) {
        super(context);
        this.QGEActivity = QGEActivity;
        this.pointerAcceleration = new PointerAcceleration(context);
        setOnTouchListener(this);
        setOnGenericMotionListener(new ExternalMouseListener());
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    public void setFrameBitmap(Bitmap bitmap) {
        frameBitmap = bitmap;
        requestLayout();
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (frameBitmap == null)
            return;
        float sx = getWidth() / (float) frameBitmap.getWidth();
        float sy = getHeight() / (float) frameBitmap.getHeight();
        float scale = Math.min(sx, sy);
        frameMatrix.setScale(scale, scale);
        frameMatrix.postTranslate(
                (getWidth() - frameBitmap.getWidth() * scale) / 2f,
                (getHeight() - frameBitmap.getHeight() * scale) / 2f);
        canvas.drawBitmap(frameBitmap, frameMatrix, framePaint);
    }

    public void refreshSurfaceView() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                QGEActivity.setFullscreen();
                QGEActivity.notifyAction(MachineAction.DISPLAY_CHANGED,
                        new Object[]{getWidth(), getHeight(), getResources().getConfiguration().orientation});
            }
        }).start();
    }

     // Converts a point in view coordinates into framebuffer coordinates, using the same
     // scale/offset onDraw() paints with
    private float[] toFrameBufferPoint(float x, float y) {
        if (frameBitmap == null)
            return new float[]{x, y};
        Matrix inverse = new Matrix();
        frameMatrix.invert(inverse);
        float[] pt = new float[]{x, y};
        inverse.mapPoints(pt);
        return pt;
    }

    public boolean onTouchProcess(View v, MotionEvent event) {
        int action = event.getActionMasked();

        // ACTION_CANCEL fires when gesture nav intercepts the touch mid swipe
        // reset state or the next move jumps using stale coordinates
        if (action == MotionEvent.ACTION_CANCEL) {
            resetTouchState();
            return false;
        }

        mouseState.x = event.getX();
        mouseState.y = event.getY();

        processMouseMovement(action, event.getToolType(0), mouseState.x, mouseState.y);
        processMouseButton(event, action, mouseState.x, mouseState.y);
        return false;
    }

    private void resetTouchState() {
        mouseState.mouseUp = true;
        mouseState.down_pending = false;
        mouseState.lastMouseButtonDown = -1;
        firstTouch = false;
        pointerAcceleration.clear();
    }

    private void processMouseMovement(int action, int toolType, float x, float y) {
        if (action == MotionEvent.ACTION_MOVE) {
            if (mouseState.mouseUp) {
                mouseState.old_x = x;
                mouseState.old_y = y;
                mouseState.mouseUp = false;
                pointerAcceleration.clear();
            }

            if (QGEActivity.isRelativeMode(toolType)) {
                float dx = pointerAcceleration.updateDx(x - mouseState.old_x);
                float dy = pointerAcceleration.updateDy(y - mouseState.old_y);
                QGEActivity.sendRelativeMove(dx, dy);
            } else {
                float[] pt = toFrameBufferPoint(x, y);
                QGEActivity.sendAbsoluteMove(pt[0], pt[1]);
            }

            mouseState.old_x = x;
            mouseState.old_y = y;
        }
    }

    private void processMouseButton(MotionEvent event, int action, float x, float y) {
        processPendingMouseButtonDown(action, event.getToolType(0), x, y);
        int mouseButton = getMouseButton(event);

        if (action == MotionEvent.ACTION_UP) {
            mouseState.addAction(event.getToolType(0), System.currentTimeMillis(), event.getActionMasked(), x, y);
            //XXX: The Button state might not be available when the action is UP
            //  we should release all mouse buttons to be safe since we don't know which one fired the event
            if (mouseButton == Config.MOUSE_BUTTON_MIDDLE || mouseButton == Config.MOUSE_BUTTON_RIGHT
                    || mouseButton != 0) {
                QGEActivity.sendMouseEvent(mouseButton, MotionEvent.ACTION_UP, event.getToolType(0), x, y);
            } else { // if we don't have information about which button we can make some guesses
                guessMouseButtonUp(event.getToolType(0), x, y);
            }
            mouseState.lastMouseButtonDown = -1;
            mouseState.mouseUp = true;
            pointerAcceleration.clear();

        } else if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mouseState.addAction(event.getToolType(0), System.currentTimeMillis(), event.getActionMasked(), x, y);
            //XXX: Some touch events for touchscreen mode are primary so we force left mouse button
            if (mouseButton == 0 && MotionEvent.TOOL_TYPE_FINGER == event.getToolType(0)) {
                mouseButton = Config.MOUSE_BUTTON_LEFT;
            }

            if (QGEActivity.isRelativeMode(event.getToolType(0))) {
                setPendingMouseDown(x, y, mouseButton);
                firstTouch = true;
            } else {
                float[] pt = toFrameBufferPoint(x, y);
                QGEActivity.sendAbsoluteMove(pt[0], pt[1]);
                QGEActivity.sendMouseEvent(mouseButton, MotionEvent.ACTION_DOWN, event.getToolType(0), x, y);
            }
            mouseState.lastMouseButtonDown = mouseButton;
        } else if (action == MotionEvent.ACTION_SCROLL) {
            QGEActivity.sendScroll(event.getAxisValue(MotionEvent.AXIS_VSCROLL));
        }
    }

    private void processPendingMouseButtonDown(int action, int toolType, float x, float y) {
        long delta = System.currentTimeMillis() - mouseState.down_event_time;
        if (mouseState.down_pending && QGEActivity.isRelativeMode(toolType)
                && (Math.abs(x - mouseState.down_x) < 20 && Math.abs(y - mouseState.down_y) < 20)
                && ((action == MotionEvent.ACTION_MOVE && delta > 400)
                || action == MotionEvent.ACTION_UP)) {
            QGEActivity.sendMouseEvent(mouseState.down_mouse_button, MotionEvent.ACTION_DOWN, toolType, x, y);
            mouseState.down_pending = false;
        } else if (System.currentTimeMillis() - mouseState.down_event_time > 400) {
            mouseState.down_pending = false;
        }
    }

    private int getMouseButton(MotionEvent event) {
        int mouseButton = 0;
        if (event.getButtonState() == MotionEvent.BUTTON_PRIMARY)
            mouseButton = Config.MOUSE_BUTTON_LEFT;
        else if (event.getButtonState() == MotionEvent.BUTTON_SECONDARY)
            mouseButton = Config.MOUSE_BUTTON_RIGHT;
        else if (event.getButtonState() == MotionEvent.BUTTON_TERTIARY)
            mouseButton = Config.MOUSE_BUTTON_MIDDLE;
        return mouseButton;
    }

    private void guessMouseButtonUp(int toolType, float x, float y) {
        //Or only the last one pressed
        if (mouseState.lastMouseButtonDown > 0) {
            QGEActivity.sendMouseEvent(mouseState.lastMouseButtonDown, MotionEvent.ACTION_UP, toolType, x, y);
        } else {
            //All buttons
            QGEActivity.sendMouseEvent(Config.MOUSE_BUTTON_LEFT, MotionEvent.ACTION_UP, toolType, x, y);
            QGEActivity.sendMouseEvent(Config.MOUSE_BUTTON_RIGHT, MotionEvent.ACTION_UP, toolType, x, y);
            QGEActivity.sendMouseEvent(Config.MOUSE_BUTTON_MIDDLE, MotionEvent.ACTION_UP, toolType, x, y);
        }
    }

    private void setPendingMouseDown(float x, float y, int mouseButton) {
        mouseState.down_pending = true;
        mouseState.down_x = x;
        mouseState.down_y = y;
        mouseState.down_mouse_button = mouseButton;
        mouseState.down_event_time = System.currentTimeMillis();
    }

    public boolean onTouch(View v, MotionEvent event) {
        return processExternalMouseEvents(v, event);
    }

    /**
     * For External Mouse we need absolute coordinates so we capture the events from the
     * surface view so this should be called from within the surfaceview's onTouch() callback
     *
     * @param v     View originating
     * @param event MotionEvent to be processed
     * @return true if event is consumed
     */
    private boolean processExternalMouseEvents(View v, MotionEvent event) {
        if (event.getToolType(0) != MotionEvent.TOOL_TYPE_FINGER
                || QGEActivity.mouseMode == QubeQGEActivity.MouseMode.TOUCHSCREEN) {
            onTouchProcess(v, event);
            return true;
        }
        return false;
    }

    class MouseState {
        public float x = 0;
        public float y = 0;
        public float old_x = 0;
        public float old_y = 0;
        public float down_x = 0;
        public float down_y = 0;
        public int down_mouse_button = 0;
        public long down_event_time = 0;
        public ArrayList<MouseAction> taps = new ArrayList<>();
        private boolean mouseUp = true;
        private int lastMouseButtonDown = -1;
        private boolean down_pending = false;

        public void addAction(int toolType, long time, int actionMasked, float x, float y) {
            if (taps.size() > 1 && time - taps.get(taps.size() - 2).time > 200) {
                taps.clear();
            } else if (taps.size() == 4) {
                taps.clear();
            }
            taps.add(new MouseAction(toolType, time, actionMasked, (int) x, (int) y));
        }

        public boolean isDoubleTap() {
            return QubeQGEActivity.mouseMode == QubeQGEActivity.MouseMode.TOUCHSCREEN
                    && taps.size() >= 3
                    && taps.get(0).toolType == MotionEvent.TOOL_TYPE_FINGER
                    && taps.get(0).action == MotionEvent.ACTION_DOWN
                    && taps.get(1).toolType == MotionEvent.TOOL_TYPE_FINGER
                    && taps.get(1).action == MotionEvent.ACTION_UP
                    && taps.get(0).x == taps.get(1).x
                    && taps.get(0).y == taps.get(1).y
                    && taps.get(2).toolType == MotionEvent.TOOL_TYPE_FINGER
                    && taps.get(2).action == MotionEvent.ACTION_DOWN;
        }

        public class MouseAction {
            private final long time;
            int action;
            int toolType;
            int x, y;

            public MouseAction(int toolType, long time, int actionMasked, int x, int y) {
                this.action = actionMasked;
                this.time = time;
                this.toolType = toolType;
                this.x = x;
                this.y = y;
            }
        }
    }

    class ExternalMouseListener implements View.OnGenericMotionListener {

        @Override
        public boolean onGenericMotion(View v, MotionEvent event) {
            switch (event.getSource()) {
                case InputDevice.SOURCE_MOUSE:
                    if (event.getActionMasked() == MotionEvent.ACTION_SCROLL) {
                        QGEActivity.sendScroll(event.getAxisValue(MotionEvent.AXIS_VSCROLL, 0));
                        return true;
                    } else if (event.getActionMasked() == MotionEvent.ACTION_HOVER_MOVE) {
                        float[] pt = toFrameBufferPoint(event.getX(), event.getY());
                        QGEActivity.sendAbsoluteMove(pt[0], pt[1]);
                        return true;
                    }
                    break;
                default:
                    break;
            }
            return false;
        }
    }
}