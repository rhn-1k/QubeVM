/*
 *
 * * Original Qube and current QubeVM comments retained during Java migration.
 * * A simple QMP Client that is needed for communicating with QEMU. You can use it for
 * * changing VNC password, checking VM status, and changing removable drives.
 * * Deliberately no "format" argument: older QEMU (<7.1) doesn't understand it at all, and even on
 * * newer QEMU, asking for PPM (the universal default) means this works identically regardless of
 * * the QEMU version in use. We convert PPM -> PNG ourselves right after the dump.
 * 
 */
package com.max2idea.android.qube.qmp;

import android.graphics.Bitmap;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;
import com.max2idea.android.qube.main.Config;
import com.max2idea.android.qube.main.QubeApplication;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import org.json.JSONObject;

/** Small QMP client used for QEMU control commands. */
public final class QmpClient {
    private static final String TAG = "QmpClient";
    private static final String REQUEST_COMMAND_MODE = "{ \"execute\": \"qmp_capabilities\" }";
    private static final int SOCKET_TIMEOUT_MS = 5000;
    private static final int RESPONSE_TRIES = 3;
    private static final long RESPONSE_RETRY_DELAY_MS = 1000L;
    private static boolean external;

    private QmpClient() {
    }

    public static void setExternal(boolean value) {
        external = value;
    }

    public static synchronized String sendCommand(String command) {
        String response = null;
        Socket tcpSocket = null;
        LocalSocket localSocket = null;
        PrintWriter out = null;
        BufferedReader input = null;
        try {
            if (external) {
                tcpSocket = new Socket(Config.QMPServer, Config.QMPPort);
                tcpSocket.setSoTimeout(SOCKET_TIMEOUT_MS);
                out = new PrintWriter(tcpSocket.getOutputStream(), true);
                input = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
            } else {
                localSocket = new LocalSocket();
                LocalSocketAddress address = new LocalSocketAddress(
                        QubeApplication.getLocalQMPSocketPath(),
                        LocalSocketAddress.Namespace.FILESYSTEM
                );
                localSocket.connect(address);
                localSocket.setSoTimeout(SOCKET_TIMEOUT_MS);
                out = new PrintWriter(localSocket.getOutputStream(), true);
                input = new BufferedReader(new InputStreamReader(localSocket.getInputStream()));
            }
            sendRequest(out, REQUEST_COMMAND_MODE);
            tryGetResponse(input);
            sendRequest(out, command);
            response = tryGetResponse(input);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (out != null) {
                out.close();
            }
            try {
                if (input != null) input.close();
                if (tcpSocket != null) tcpSocket.close();
                if (localSocket != null) localSocket.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        if (Config.debugQmp) {
            Log.d(TAG, "Response: " + response);
        }
        return response;
    }

    private static String tryGetResponse(BufferedReader input) throws InterruptedException {
        String response = getResponse(input);
        int trial = 0;
        while (response.isEmpty() && trial < RESPONSE_TRIES) {
            Thread.sleep(RESPONSE_RETRY_DELAY_MS);
            trial++;
            response = getResponse(input);
        }
        return response;
    }

    private static void sendRequest(PrintWriter out, String request) {
        if (Config.debugQmp) {
            Log.d(TAG, "QMP request" + request);
        }
        out.println(request);
    }

    private static String getResponse(BufferedReader input) {
        StringBuilder response = new StringBuilder();
        try {
            while (true) {
                String line = input.readLine();
                if (line == null) {
                    break;
                }
                if (Config.debugQmp) {
                    Log.d(TAG, "QMP response: " + line);
                }
                JSONObject json = new JSONObject(line);
                boolean hasReturn = line.contains("return") && json.has("return");
                boolean hasError = line.contains("error") && json.has("error");
                response.append(line).append('\n');
                if (hasReturn || hasError) {
                    break;
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "Could not get Response: " + ex.getMessage());
            if (Config.debugQmp) {
                ex.printStackTrace();
            }
        }
        return response.toString();
    }


    public static String getEjectDeviceCommand(String dev) {
        return "{ \"execute\": \"eject\", \"arguments\": { \"device\": \"" + dev + "\" } }";
    }

    public static String getChangeDeviceCommand(String dev, String value) {
        return "{ \"execute\": \"blockdev-change-medium\", \"arguments\": { \"device\": \""
                + dev + "\", \"filename\": \"" + value + "\" } }";
    }

    public static String getPowerDownCommand() {
        return "{ \"execute\": \"system_powerdown\" }";
    }

    public static String getResetCommand() {
        return "{ \"execute\": \"system_reset\" }";
    }

    public static String getStateCommand() {
        return "{ \"execute\": \"query-status\" }";
    }

    public static String getScreendumpCommand(String filename) {
        // Deliberately no "format" argument: older QEMU (<7.1) doesn't understand it.
        // We convert PPM -> PNG ourselves right after the dump.
        return "{ \"execute\": \"screendump\", \"arguments\": { \"filename\": \""
                + filename + "\" } }";
    }

    /**
     * Asks QEMU to dump the current display contents and converts the result to PNG.
     */
    public static String screendump(String pngFilename) {
        File ppmFile = new File(pngFilename + ".tmp.ppm");
        String response = sendCommand(getScreendumpCommand(ppmFile.getAbsolutePath()));
        if (response == null) {
            return null;
        }
        if (response.contains("\"error\"")) {
            Log.w(TAG, "screendump error: " + response);
            ppmFile.delete();
            return null;
        }
        boolean converted = false;
        try {
            converted = convertPpmToPng(ppmFile, new File(pngFilename));
        } catch (Exception ex) {
            Log.e(TAG, "PPM to PNG conversion failed: " + ex.getMessage());
            if (Config.debugQmp) {
                ex.printStackTrace();
            }
        } finally {
            ppmFile.delete();
        }
        return converted ? response : null;
    }

    /** Reads a binary PPM (P6) file and writes it out as a PNG file. */
    private static boolean convertPpmToPng(File ppmFile, File pngFile) throws Exception {
        if (!ppmFile.exists()) {
            Log.e(TAG, "PPM file not found: " + ppmFile.getAbsolutePath());
            return false;
        }
        try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(ppmFile))) {
            String magic = readPpmToken(input);
            if (!"P6".equals(magic)) {
                Log.e(TAG, "Unsupported PPM magic number: " + magic);
                return false;
            }
            int width = Integer.parseInt(readPpmToken(input));
            int height = Integer.parseInt(readPpmToken(input));
            int maxVal = Integer.parseInt(readPpmToken(input));
            if (maxVal < 1 || maxVal > 255) {
                Log.e(TAG, "Unsupported PPM maxval: " + maxVal);
                return false;
            }
            int pixelCount = width * height;
            byte[] rgb = new byte[pixelCount * 3];
            int offset = 0;
            byte[] buffer = new byte[8192];
            while (offset < rgb.length) {
                int read = input.read(buffer, 0, Math.min(buffer.length, rgb.length - offset));
                if (read == -1) {
                    Log.e(TAG, "Unexpected end of PPM pixel data");
                    return false;
                }
                System.arraycopy(buffer, 0, rgb, offset, read);
                offset += read;
            }
            int[] pixels = new int[pixelCount];
            int rgbIndex = 0;
            for (int i = 0; i < pixelCount; i++) {
                int r = rgb[rgbIndex] & 0xFF;
                int g = rgb[rgbIndex + 1] & 0xFF;
                int b = rgb[rgbIndex + 2] & 0xFF;
                rgbIndex += 3;
                pixels[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
            Bitmap bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
            try (FileOutputStream output = new FileOutputStream(pngFile)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
            }
            bitmap.recycle();
            return true;
        }
    }

    /** Reads a whitespace-delimited PPM token and skips PPM comments. */
    private static String readPpmToken(BufferedInputStream input) throws Exception {
        ByteArrayOutputStream token = new ByteArrayOutputStream();
        int b;
        // Skip leading whitespace/comments.
        while (true) {
            b = input.read();
            if (b == -1) {
                throw new IllegalStateException("Unexpected end of PPM header");
            }
            if (b == '#') {
                while (b != -1 && b != '\n') {
                    b = input.read();
                }
                continue;
            }
            if (!Character.isWhitespace((char) b)) {
                break;
            }
        }
        // Read the token itself.
        while (b != -1 && !Character.isWhitespace((char) b)) {
            token.write(b);
            b = input.read();
        }
        return token.toString("US-ASCII");
    }
}
