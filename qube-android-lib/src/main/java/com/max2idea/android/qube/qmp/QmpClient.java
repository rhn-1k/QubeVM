/*
Copyright (C) Max Kastanas 2012
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.qmp;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;
import com.max2idea.android.qube.main.Config;
import com.max2idea.android.qube.main.QubeApplication;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import org.json.JSONObject;

/** A simple QMP CLient that is needed for communicating with QEMU. You can use it for
 * changing removable drives, power and reset.
  */
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
}
