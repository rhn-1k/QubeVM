package com.max2idea.android.qube.server;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Lightweight HTTP server that shares the host's Download folder with the VM guest
 * Guests can upload files via curl -T
 * Pure Java (NanoHTTPD), no external deps
 * Accessible at 10.0.2.2 over QEMU's SLIRP networking
 */
public class SharedFolderServer extends NanoHTTPD {

    public static final String TAG = "SharedFolderServer";
    public static final int PORT = 19000;

    private static SharedFolderServer instance;

    public static String getSharedDir() {
        return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
    }

    public static String getHostUrl() {
        return "http://10.0.2.2:" + PORT;
    }

    public static String getUploadExample() {
        return "curl -T myfile.txt " + getHostUrl() + "/myfile.txt";
    }

    public static boolean isRunning() {
        return instance != null && instance.isAlive();
    }

    // Starts the server if not already running
    // Returns true if server is running after this call
    public static synchronized boolean startServer() {
        if (isRunning()) {
            return true;
        }
        try {
            instance = new SharedFolderServer();
            instance.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "startServer: failed to bind port " + PORT, e);
            instance = null;
            return false;
        }
    }

    public static synchronized void stopServer() {
        if (instance != null) {
            instance.stop();
            instance = null;
        }
    }

    private SharedFolderServer() {
        super(PORT);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        // Strip the leading slash to get a relative path under the shared dir
        String relativePath = uri.startsWith("/") ? uri.substring(1) : uri;

        // Guard against path traversal (e.g. "../../etc")
        File requested = new File(getSharedDir(), relativePath).getAbsoluteFile();
        File sharedRoot = new File(getSharedDir()).getAbsoluteFile();
        if (!requested.getPath().startsWith(sharedRoot.getPath())) {
            return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Forbidden");
        }

        switch (session.getMethod()) {
            case PUT:
            case POST:
                return handleUpload(session, requested);
            case GET:
            default:
                return handleDownload(requested);
        }
    }

    private Response handleDownload(File requested) {
        if (requested.isDirectory()) {
            return newFixedLengthResponse(Response.Status.OK, "text/html", listDirectory(requested));
        }
        if (!requested.isFile()) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found");
        }
        try {
            InputStream stream = new FileInputStream(requested);
            String mime = getMimeTypeForFile(requested.getName());
            return newFixedLengthResponse(Response.Status.OK, mime, stream, requested.length());
        } catch (IOException e) {
            Log.e(TAG, "handleDownload: " + requested, e);
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Read error");
        }
    }

    private Response handleUpload(IHTTPSession session, File destination) {
        if (destination.isDirectory()) {
            return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Cannot upload over a directory");
        }
        try {
            Map<String, String> files = new java.util.HashMap<>();
            // NanoHTTPD buffers the raw body to a temp file for us
            session.parseBody(files);
            String tmpPath = files.get("content");
            File tmpFile = tmpPath != null ? new File(tmpPath) : null;

            File parent = destination.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Could not create destination folder");
            }

            if (tmpFile != null && tmpFile.exists()) {
                copyFile(tmpFile, destination);
            } else {
                // Fallback, stream raw request body directly for clients like curl -T
                writeStreamToFile(session.getInputStream(), destination,
                        Long.parseLong(session.getHeaders().getOrDefault("content-length", "-1")));
            }

            return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "OK");
        } catch (Exception e) {
            Log.e(TAG, "handleUpload: " + destination, e);
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Upload failed");
        }
    }

    private static void copyFile(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dst)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }

    private static void writeStreamToFile(InputStream in, File dst, long expectedLength) throws IOException {
        try (OutputStream out = new FileOutputStream(dst)) {
            byte[] buffer = new byte[8192];
            long remaining = expectedLength;
            int read;
            while ((remaining < 0 || remaining > 0)
                    && (read = in.read(buffer, 0, remaining < 0 ? buffer.length : (int) Math.min(buffer.length, remaining))) != -1) {
                out.write(buffer, 0, read);
                if (remaining > 0) remaining -= read;
            }
        }
    }

    private String listDirectory(File dir) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body><h3>Index of ").append(dir.getName()).append("</h3><ul>");
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                String name = child.getName() + (child.isDirectory() ? "/" : "");
                html.append("<li><a href=\"").append(name).append("\">").append(name).append("</a></li>");
            }
        }
        html.append("</ul></body></html>");
        return html.toString();
    }
}
