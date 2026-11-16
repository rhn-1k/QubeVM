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
import android.view.ViewConfiguration;

import com.max2idea.android.qube.machine.MachineAction;

import java.util.ArrayList;

/**
 * Draws the guest framebuffer (see QubeGfx) and turns touch input into pointer events,
 * sent via QubeInput. Trackpad mode uses relative deltas run through PointerAcceleration
 * for a smooth experience.
 */
public class QubeQGESurface extends View implements View.OnTouchListener {
    private static final String TAG = "QubeQGESurface";

    private static final float MAX_ZOOM = 6f;
    // external mouse scrolls the zoomed picture inside this band along an edge (fraction of the shorter side)
    private static final float EDGE_ZONE = 0.08f;
    private static final float EDGE_SCROLL_DP_PER_FRAME = 10f;

    MouseState mouseState = new MouseState();
    // true from ACTION_DOWN until the finger has moved past the touch slop
    private boolean firstTouch = false;
    private final float touchSlop;
    private final float edgeScrollSpeed;

    private final QubeQGEActivity QGEActivity;
    private final PointerAcceleration pointerAcceleration;
    private final Paint framePaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Matrix frameMatrix = new Matrix();
    private Bitmap frameBitmap;

    // Magnifier, zoom 1 is the normal fit-to-view picture, pan is how far the picture is
    // shifted from the view center, in view pixels
    private boolean zoomMode = false;
    private float zoom = 1f;
    private float panX = 0f;
    private float panY = 0f;

    // Two finger zoom/pan gesture, swallows all touch events until the last finger is up
    private boolean gestureActive = false;
    private boolean gestureHasBaseline = false;
    private boolean gestureScaling = false;
    private float gestureStartSpan = 0f;
    private float gestureLastSpan = 0f;
    private float gestureLastX = 0f;
    private float gestureLastY = 0f;
    private final float[] fingers = new float[3];
    private final int[] tmpLocation = new int[2];
    // trackpad events come in via the activity, so they are in window coordinates, not ours
    private boolean eventsInWindowSpace = false;

    // Touchscreen mode with the magnifier on: the press is held back for a tap timeout so
    // a second finger arriving can still turn it into a zoom instead of a click
    private boolean absDownPending = false;
    private int absDownButton = 0;
    private float absDownX = 0f;
    private float absDownY = 0f;
    private final Runnable absDownRunnable = new Runnable() {
        @Override
        public void run() {
            flushAbsoluteDown();
        }
    };

    // External mouse parked near an edge keeps scrolling the zoomed picture
    private float hoverX = 0f;
    private float hoverY = 0f;
    private boolean hoverActive = false;
    private boolean edgeScrollPosted = false;
    private final Runnable edgeScrollRunnable = new Runnable() {
        @Override
        public void run() {
            edgeScrollPosted = false;
            if (!zoomMode || zoom <= 1f || !hoverActive || frameBitmap == null)
                return;
            float w = getWidth();
            float h = getHeight();
            float zone = Math.min(w, h) * EDGE_ZONE;
            float sx = edgeStrength(hoverX, w, zone);
            float sy = edgeStrength(hoverY, h, zone);
            if (sx == 0f && sy == 0f)
                return;
            updateFrameMatrix();
            float oldX = panX;
            float oldY = panY;
            panX += sx * edgeScrollSpeed;
            panY += sy * edgeScrollSpeed;
            updateFrameMatrix(); // clamps
            if (panX == oldX && panY == oldY)
                return; // already at the edge of the picture
            invalidate();
            // the picture moved under a parked pointer, so the guest pointer has to follow
            float[] pt = toFrameBufferPoint(hoverX, hoverY);
            QGEActivity.sendAbsoluteMove(pt[0], pt[1]);
            edgeScrollPosted = true;
            postOnAnimation(this);
        }
    };

    public QubeQGESurface(QubeQGEActivity QGEActivity, Context context) {
        super(context);
        this.QGEActivity = QGEActivity;
        this.pointerAcceleration = new PointerAcceleration(context);
        this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        this.edgeScrollSpeed = EDGE_SCROLL_DP_PER_FRAME * context.getResources().getDisplayMetrics().density;
        setOnTouchListener(this);
        setOnGenericMotionListener(new ExternalMouseListener());
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    public void setFrameBitmap(Bitmap bitmap) {
        boolean sizeChanged = frameBitmap == null || bitmap == null
                || frameBitmap.getWidth() != bitmap.getWidth()
                || frameBitmap.getHeight() != bitmap.getHeight();
        frameBitmap = bitmap;
        if (sizeChanged) {
            // the guest changed resolution, the old zoom no longer points at anything
            zoom = 1f;
            panX = 0f;
            panY = 0f;
        }
        requestLayout();
        postInvalidate();
    }

    public boolean isZoomMode() {
        return zoomMode;
    }

    public void setZoomMode(boolean enabled) {
        if (zoomMode == enabled)
            return;
        zoomMode = enabled;
        if (!enabled) {
            flushAbsoluteDown();
            gestureActive = false;
            gestureHasBaseline = false;
            hoverActive = false;
            removeCallbacks(edgeScrollRunnable);
            edgeScrollPosted = false;
            zoom = 1f;
            panX = 0f;
            panY = 0f;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (frameBitmap == null)
            return;
        updateFrameMatrix();
        canvas.drawBitmap(frameBitmap, frameMatrix, framePaint);
    }

    // Maps framebuffer to view coords, fit and center, then magnifier zoom/pan
    // toFrameBufferPoint() inverts this matrix
    private void updateFrameMatrix() {
        if (frameBitmap == null)
            return;
        float vw = getWidth();
        float vh = getHeight();
        if (vw <= 0f || vh <= 0f)
            return;
        float bw = frameBitmap.getWidth();
        float bh = frameBitmap.getHeight();
        float scale = Math.min(vw / bw, vh / bh) * zoom;
        // keep the picture from being dragged off screen, an axis that fits stays centered
        float maxPanX = Math.max(0f, (bw * scale - vw) / 2f);
        float maxPanY = Math.max(0f, (bh * scale - vh) / 2f);
        panX = Math.max(-maxPanX, Math.min(maxPanX, panX));
        panY = Math.max(-maxPanY, Math.min(maxPanY, panY));
        frameMatrix.setScale(scale, scale);
        frameMatrix.postTranslate((vw - bw * scale) / 2f + panX, (vh - bh * scale) / 2f + panY);
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
        updateFrameMatrix();
        Matrix inverse = new Matrix();
        frameMatrix.invert(inverse);
        float[] pt = new float[]{x, y};
        inverse.mapPoints(pt);
        return pt;
    }

    // Trackpad events are delivered to the activity, which hands them over here
    public boolean onActivityTouch(MotionEvent event) {
        eventsInWindowSpace = true;
        try {
            return onTouchProcess(this, event);
        } finally {
            eventsInWindowSpace = false;
        }
    }

    public boolean onTouchProcess(View v, MotionEvent event) {
        if (zoomMode && handleZoomGesture(event))
            return true;
        int action = event.getActionMasked();
        mouseState.x = event.getX();
        mouseState.y = event.getY();

        processMouseMovement(action, event.getToolType(0), mouseState.x, mouseState.y);
        processMouseButton(event, action, mouseState.x, mouseState.y);
        return false;
    }

    // Two fingers, pinch zooms, moving them together scrolls, fingers only (no mouse/stylus)
    // Returns true when the event was consumed by the gesture
    private boolean handleZoomGesture(MotionEvent event) {
        if (frameBitmap == null)
            return false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                gestureActive = false; // a new touch always starts clean
                return false;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (!gestureActive && event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER)
                    startGesture(event);
                else if (gestureActive)
                    setBaseline(event, -1); // another finger joined, centroid jumps
                return gestureActive;
            case MotionEvent.ACTION_MOVE:
                if (!gestureActive)
                    return false;
                updateGesture(event);
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                if (!gestureActive)
                    return false;
                setBaseline(event, event.getActionIndex()); // rebase without the lifted finger
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!gestureActive)
                    return false;
                gestureActive = false;
                gestureHasBaseline = false;
                // the up of the first finger never reaches the normal path, so reset what it needs
                mouseState.mouseUp = true;
                mouseState.lastMouseButtonDown = -1;
                firstTouch = false;
                pointerAcceleration.clear();
                return true;
            default:
                return gestureActive;
        }
    }

    private void startGesture(MotionEvent event) {
        gestureActive = true;
        gestureScaling = false;
        // whatever the first finger had started must not turn into a click
        removeCallbacks(absDownRunnable);
        absDownPending = false;
        mouseState.down_pending = false;
        if (mouseState.buttonSent) {
            guessMouseButtonUp(event.getToolType(0), event.getX(0), event.getY(0));
            mouseState.buttonSent = false;
        }
        mouseState.lastMouseButtonDown = -1;
        firstTouch = false;
        pointerAcceleration.clear();
        setBaseline(event, -1);
        gestureStartSpan = gestureLastSpan;
    }

    private void updateGesture(MotionEvent event) {
        if (!measureFingers(event, -1, fingers)) {
            gestureHasBaseline = false; // down to one finger, wait for another
            return;
        }
        if (!gestureHasBaseline) {
            setBaseline(event, -1);
            return;
        }
        float cx = fingers[0];
        float cy = fingers[1];
        float span = fingers[2];
        // small span changes while scrolling must not zoom, so zoom starts after some slop
        if (!gestureScaling && Math.abs(span - gestureStartSpan) > touchSlop)
            gestureScaling = true;
        float factor = (gestureScaling && gestureLastSpan > 0f) ? span / gestureLastSpan : 1f;

        panX += cx - gestureLastX;
        panY += cy - gestureLastY;
        if (factor != 1f)
            zoomAbout(factor, cx, cy);
        gestureLastX = cx;
        gestureLastY = cy;
        gestureLastSpan = span;
        invalidate();
    }

    // Zooms around a point in view coordinates, the picture under that point stays put
    private void zoomAbout(float factor, float fx, float fy) {
        float newZoom = Math.max(1f, Math.min(MAX_ZOOM, zoom * factor));
        float applied = newZoom / zoom;
        panX = applied * panX + (1f - applied) * (fx - getWidth() / 2f);
        panY = applied * panY + (1f - applied) * (fy - getHeight() / 2f);
        zoom = newZoom;
    }

    private void setBaseline(MotionEvent event, int skipIndex) {
        if (measureFingers(event, skipIndex, fingers)) {
            gestureLastX = fingers[0];
            gestureLastY = fingers[1];
            gestureLastSpan = fingers[2];
            gestureHasBaseline = true;
        } else {
            gestureHasBaseline = false;
        }
    }

    // out = {centroid x, centroid y, average finger distance from the centroid}, in view
    // coordinates. False when fewer than two fingers are left. skipIndex is a lifted finger.
    private boolean measureFingers(MotionEvent event, int skipIndex, float[] out) {
        int offsetX = 0;
        int offsetY = 0;
        if (eventsInWindowSpace) {
            getLocationInWindow(tmpLocation);
            offsetX = tmpLocation[0];
            offsetY = tmpLocation[1];
        }
        int n = event.getPointerCount();
        float sx = 0f;
        float sy = 0f;
        int count = 0;
        for (int i = 0; i < n; i++) {
            if (i == skipIndex)
                continue;
            sx += event.getX(i) - offsetX;
            sy += event.getY(i) - offsetY;
            count++;
        }
        if (count < 2)
            return false;
        float cx = sx / count;
        float cy = sy / count;
        float span = 0f;
        for (int i = 0; i < n; i++) {
            if (i == skipIndex)
                continue;
            span += (float) Math.hypot(event.getX(i) - offsetX - cx, event.getY(i) - offsetY - cy);
        }
        out[0] = cx;
        out[1] = cy;
        out[2] = span / count;
        return true;
    }

    // ---- External mouse edge scrolling ----

    // External mouse: absolute position is exact, so scroll when it is pushed to an edge
    private void startEdgeScroll(float x, float y) {
        hoverX = x;
        hoverY = y;
        hoverActive = true;
        if (zoom > 1f && !edgeScrollPosted) {
            edgeScrollPosted = true;
            postOnAnimation(edgeScrollRunnable);
        }
    }

    // +1 at the start edge of the band, -1 at the far edge, 0 elsewhere; the sign is the
    // direction the picture has to move to bring more of it into view
    private static float edgeStrength(float pos, float size, float zone) {
        if (pos < zone)
            return Math.min(1f, (zone - pos) / zone);
        if (pos > size - zone)
            return -Math.min(1f, (pos - (size - zone)) / zone);
        return 0f;
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
                if (firstTouch) {
                // XXX: it seems the panel holds back first slop of movement and sends it
                // as one big delta, which jumps the cursor, so we swallow it here
                    if (Math.hypot(x - mouseState.down_x, y - mouseState.down_y) < touchSlop)
                        return;
                    firstTouch = false;
                    mouseState.old_x = x;
                    mouseState.old_y = y;
                    return;
                }
                // zoomed in, the cursor is drawn bigger too, so it moves slower to keep the same feel
                float divisor = zoomMode ? zoom : 1f;
                float dx = pointerAcceleration.updateDx((x - mouseState.old_x) / divisor);
                float dy = pointerAcceleration.updateDy((y - mouseState.old_y) / divisor);
                QGEActivity.sendRelativeMove(dx, dy);
            } else {
                if (absDownPending && Math.hypot(x - absDownX, y - absDownY) > touchSlop)
                    flushAbsoluteDown(); // finger is dragging, press now
                float[] pt = toFrameBufferPoint(x, y);
                QGEActivity.sendAbsoluteMove(pt[0], pt[1]);
                if (zoomMode && toolType == MotionEvent.TOOL_TYPE_MOUSE)
                    startEdgeScroll(x, y);
            }

            mouseState.old_x = x;
            mouseState.old_y = y;
        }
    }

    private void processMouseButton(MotionEvent event, int action, float x, float y) {
        processPendingMouseButtonDown(action, event.getToolType(0), x, y);
        int mouseButton = getMouseButton(event);

        if (action == MotionEvent.ACTION_UP) {
            flushAbsoluteDown(); // a quick tap still has to be a click
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
            mouseState.buttonSent = false;
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
                if (zoomMode && event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER) {
                    holdAbsoluteDown(mouseButton, x, y);
                } else {
                    QGEActivity.sendMouseEvent(mouseButton, MotionEvent.ACTION_DOWN, event.getToolType(0), x, y);
                    mouseState.buttonSent = true;
                }
            }
            mouseState.lastMouseButtonDown = mouseButton;
        } else if (action == MotionEvent.ACTION_SCROLL) {
            QGEActivity.sendScroll(event.getAxisValue(MotionEvent.AXIS_VSCROLL));
        }
    }

    private void holdAbsoluteDown(int button, float x, float y) {
        absDownPending = true;
        absDownButton = button;
        absDownX = x;
        absDownY = y;
        postDelayed(absDownRunnable, ViewConfiguration.getTapTimeout());
    }

    private void flushAbsoluteDown() {
        if (!absDownPending)
            return;
        removeCallbacks(absDownRunnable);
        absDownPending = false;
        QGEActivity.sendMouseEvent(absDownButton, MotionEvent.ACTION_DOWN, MotionEvent.TOOL_TYPE_FINGER, absDownX, absDownY);
        mouseState.buttonSent = true;
    }

    private void processPendingMouseButtonDown(int action, int toolType, float x, float y) {
        long delta = System.currentTimeMillis() - mouseState.down_event_time;
        if (mouseState.down_pending && QGEActivity.isRelativeMode(toolType)
                && (Math.abs(x - mouseState.down_x) < 20 && Math.abs(y - mouseState.down_y) < 20)
                && ((action == MotionEvent.ACTION_MOVE && delta > 400)
                || action == MotionEvent.ACTION_UP)) {
            QGEActivity.sendMouseEvent(mouseState.down_mouse_button, MotionEvent.ACTION_DOWN, toolType, x, y);
            mouseState.buttonSent = true;
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
        // true once a button press was really sent to the guest, as opposed to only pending
        private boolean buttonSent = false;

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
                        if (zoomMode)
                            startEdgeScroll(event.getX(), event.getY());
                        return true;
                    } else if (event.getActionMasked() == MotionEvent.ACTION_HOVER_EXIT) {
                        hoverActive = false;
                    }
                    break;
                default:
                    break;
            }
            return false;
        }
    }
}
