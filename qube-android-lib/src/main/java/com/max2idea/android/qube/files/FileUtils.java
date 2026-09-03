/*
Copyright (C) Max Kastanas 2012
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.files;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.documentfile.provider.DocumentFile;

import com.qube.emu.lib.R;
import com.max2idea.android.qube.dialog.DialogUtils;
import com.max2idea.android.qube.machine.Machine.FileType;
import com.max2idea.android.qube.main.Config;
import com.max2idea.android.qube.main.QubeApplication;
import com.max2idea.android.qube.main.QubeSettingsManager;
import com.max2idea.android.qube.toast.ToastUtils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;

/**
 * @author dev
 */
public class FileUtils {
    private final static String TAG = "FileUtils";
    private static final Object fdsLock = new Object();
    private static HashMap<Integer, FileInfo> fds = new HashMap<Integer, FileInfo>();

    public static String getNativeLibDir(Context context) {
        return context.getApplicationInfo().nativeLibraryDir;
    }

    public static String getFullPathFromDocumentFilePath(String filePath) {

        filePath = filePath.replaceAll("%3A", "^3A");
        int index = filePath.lastIndexOf("^3A");
        if (index > 0)
            filePath = filePath.substring(index + 3);
        if (!filePath.startsWith("/"))
            filePath = "/" + filePath;

        //remove any spaces encoded by the ASF
        try {
            filePath = URLDecoder.decode(filePath, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return filePath;
    }

    public static String getFilenameFromPath(String filePath) {
        filePath = filePath.replaceAll("%2F", "/");
        filePath = filePath.replaceAll("%3A", "/");
        filePath = filePath.replaceAll("\\^2F", "/");
        filePath = filePath.replaceAll("\\^3A", "/");


        int index = filePath.lastIndexOf("/");
        if (index > 0)
            return filePath.substring(index + 1);
        return filePath;
    }

    public static String decodeDocumentFilePath(String filePath) {
        if (filePath != null && filePath.startsWith("/content//")) {
            filePath = filePath.replace("/content//", "content://");
            filePath = filePath.replaceAll("\\^\\^\\^", "%");
        }
        return filePath;
    }

    /**
     * Encode the path % chars to ^
     *
     * @param filePath
     * @return
     */
    public static String encodeDocumentFilePath(String filePath) {
        if (filePath != null && filePath.startsWith("content://")) {
            filePath = filePath.replace("content://", "/content//");
            filePath = filePath.replaceAll("%", "\\^\\^\\^");
            ;

        }
        return filePath;
    }

    /**
     * Resolve a Shared Folder location to a real, on-disk filesystem path.
     * <p>
     * QEMU's "fat:rw:" virtual FAT driver needs to opendir/stat/mkstemp directly
     * on the target folder. It cannot do this through a SAF content:// tree URI
     * (only single-file fopen/open/stat are bridged to the content resolver in
     * the native compat layer), so passing a content URI straight through
     * causes QEMU to fail creating its temp file and abort the whole process.
     * <p>
     * Since this app already requests MANAGE_EXTERNAL_STORAGE / legacy storage
     * access, most SAF tree URIs picked from a real, mounted storage volume can
     * be mapped back to a real absolute path that QEMU can use directly.
     *
     * @return a real, writable directory path, or null if the folder can't be
     * resolved to one (in which case the caller should skip Shared Folder
     * instead of crashing QEMU).
     */
    public static String getRealPathForSharedFolder(Context context, String folderPath) {
        if (folderPath == null || folderPath.trim().isEmpty())
            return null;

        try {
            if (!folderPath.startsWith("content://")) {
                // Already a plain filesystem path (e.g. legacy file manager)
                File dir = new File(folderPath);
                return (dir.isDirectory() && dir.canWrite()) ? dir.getAbsolutePath() : null;
            }

            Uri treeUri = Uri.parse(folderPath);
            String docId = DocumentsContract.getTreeDocumentId(treeUri);
            if (docId == null)
                return null;

            String volumeId;
            String relativePath;
            int colonIndex = docId.indexOf(':');
            if (colonIndex >= 0) {
                volumeId = docId.substring(0, colonIndex);
                relativePath = docId.substring(colonIndex + 1);
            } else {
                volumeId = docId;
                relativePath = "";
            }

            String basePath;
            if ("primary".equalsIgnoreCase(volumeId)) {
                basePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            } else {
                basePath = findStorageVolumeRoot(context, volumeId);
            }

            if (basePath == null)
                return null;

            String fullPath = relativePath.isEmpty() ? basePath : basePath + "/" + relativePath;
            File dir = new File(fullPath);
            if (dir.isDirectory() && dir.canWrite())
                return dir.getAbsolutePath();

        } catch (Exception e) {
            Log.e(TAG, "Could not resolve real path for shared folder: " + folderPath, e);
        }

        return null;
    }

    /** Like getRealPathForSharedFolder but for single document URIs. Returns null if unresolvable. */
    public static String getRealPathForFile(Context context, String filePath) {
        if (filePath == null || filePath.trim().isEmpty())
            return null;

        String uriStr = filePath.startsWith("/content//") ? decodeDocumentFilePath(filePath) : filePath;

        if (!uriStr.startsWith("content://")) {
            File f = new File(uriStr);
            return f.exists() ? f.getAbsolutePath() : null;
        }

        try {
            Uri uri = Uri.parse(uriStr);
            String authority = uri.getAuthority();
            if (!"com.android.externalstorage.documents".equals(authority))
                return null;

            String docId = DocumentsContract.getDocumentId(uri);
            if (docId == null)
                return null;

            int colonIndex = docId.indexOf(':');
            if (colonIndex < 0)
                return null;

            String volumeId = docId.substring(0, colonIndex);
            String relativePath = docId.substring(colonIndex + 1);

            String basePath;
            if ("primary".equalsIgnoreCase(volumeId)) {
                basePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            } else {
                basePath = findStorageVolumeRoot(context, volumeId);
            }

            if (basePath == null)
                return null;

            String fullPath = relativePath.isEmpty() ? basePath : basePath + "/" + relativePath;
            File f = new File(fullPath);
            if (f.exists())
                return f.getAbsolutePath();

        } catch (Exception e) {
            Log.e(TAG, "Could not resolve real path for file: " + filePath, e);
        }

        return null;
    }

    /**
     * Find the real mount root (e.g. "/storage/1234-5678") for a non-primary
     * storage volume id, using the app-specific directories Android exposes on
     * every mounted volume.
     */
    private static String findStorageVolumeRoot(Context context, String volumeId) {
        try {
            File[] externalDirs = context.getExternalFilesDirs(null);
            if (externalDirs == null)
                return null;
            for (File dir : externalDirs) {
                if (dir == null)
                    continue;
                String path = dir.getAbsolutePath();
                int androidDataIndex = path.indexOf("/Android/data");
                if (androidDataIndex <= 0)
                    continue;
                String root = path.substring(0, androidDataIndex);
                if (root.endsWith("/" + volumeId)) {
                    return root;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not resolve storage volume root for: " + volumeId, e);
        }
        return null;
    }

    // Convert a drive image path (plain filesystem or SAF content://) into a
    // form that QEMU can open.
    // path so the native compat layer can handle it directly (or fallback
    // to the real path if needed).
    public static String getQemuFilePath(String imagePath) {
        if (imagePath == null || imagePath.equals("None") || imagePath.trim().isEmpty())
            return null;

        // Already a plain filesystem path – use the standard encode so the
        // native compat layer handles it correctly.
        if (!imagePath.startsWith("content://") && !imagePath.startsWith("/content//"))
            return encodeDocumentFilePath(imagePath);

        // SAF content:// URI: return encoded path
        Log.d(TAG, "getQemuFilePath: returning encoded path for SAF uri: " + imagePath);
        return encodeDocumentFilePath(imagePath);
    }

    public static void saveFileContents(String filePath, String contents) {
        // TODO: we assume that the contents are of small size so we keep in an array
        byteArrayToFile(contents.getBytes(), new File(filePath));
    }

    public static void byteArrayToFile(byte[] byteData, File filePath) {

        try {
            FileOutputStream fos = new FileOutputStream(filePath);
            fos.write(byteData);
            fos.close();

        } catch (FileNotFoundException ex) {
            System.out.println("FileNotFoundException : " + ex);
        } catch (IOException ioe) {
            System.out.println("IOException : " + ioe);
        }

    }

    public static InputStream getStreamFromFilePath(String filePath) throws FileNotFoundException {
        if (filePath.startsWith("content://")) {
            Uri uri = Uri.parse(filePath);
            String mode = "rw";
            ParcelFileDescriptor pfd = QubeApplication.getInstance().getContentResolver().openFileDescriptor(uri, mode);
            return new FileInputStream(pfd.getFileDescriptor());
        } else {
            return new FileInputStream(filePath);
        }
    }

    public static void closeFileDescriptor(String filePath) throws IOException {
        if (filePath.startsWith("content://")) {
            Uri uri = Uri.parse(filePath);
            String mode = "rw";
            ParcelFileDescriptor pfd = QubeApplication.getInstance().getContentResolver().openFileDescriptor(uri, mode);
            pfd.close();
        }
    }

    public static String getFileContents(String filePath) {
        File file = new File(filePath);
        if (!file.exists())
            return "";
        StringBuilder builder = new StringBuilder("");
        try {
            FileInputStream stream = new FileInputStream(file);
            byte[] buff = new byte[32768];
            int bytesRead = 0;
            while ((bytesRead = stream.read(buff, 0, buff.length)) > 0) {
                //FIXME: log file can be too large appending might fail with out of memory
                builder.append(new String(buff, 0, bytesRead));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String contents = builder.toString();
        return contents;
    }

    public static boolean fileValid(String path) {
        if (path == null || path.equals(""))
            return true;
        if (path.startsWith("content://") || path.startsWith("/content/")) {
            int fd = get_fd(path);
            if (fd <= 0)
                return false;
        } else {
            File file = new File(path);
            file.setWritable(true);
            return file.exists();
        }
        return true;
    }

    //TODO: we should pass the modes from the backend and translate them
    // instead of blindly using "rw". ie ISOs should be read only.
    public static int get_fd(String path) {
        synchronized (fdsLock) {
            int fd = 0;
            if (path == null)
                return 0;
            if (path.startsWith("/content//") || path.startsWith("content://")) {
                String npath = decodeDocumentFilePath(path);
                try {
                    Uri uri = Uri.parse(npath);
                    String mode = "rw";
                    if (path.toLowerCase().endsWith(".iso"))
                        mode = "r";
                    ParcelFileDescriptor pfd = QubeApplication.getInstance().getContentResolver().openFileDescriptor(uri, mode);
                    fd = pfd.getFd();
                    fds.put(fd, new FileInfo(path, npath, pfd));
                    Log.d(TAG, "Opening Content Uri: " + npath + ", FD: " + fd);
                } catch (Exception e) {
                    String msg = QubeApplication.getInstance().getString(R.string.CouldNotOpenDocFile) + " "
                            + FileUtils.getFullPathFromDocumentFilePath(npath)
                            + "\n" + QubeApplication.getInstance().getString(R.string.PleaseReassingYourDiskFiles);
                    ToastUtils.toastLong(QubeApplication.getInstance(), msg);
                    e.printStackTrace();
                }
            } else {
                try {
                    int mode = ParcelFileDescriptor.MODE_READ_WRITE;
                    if (path.toLowerCase().endsWith(".iso"))
                        mode = ParcelFileDescriptor.MODE_READ_ONLY;
                    File file = new File(path);
                    if (!file.exists())
                        file.createNewFile();
                    ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, mode);
                    fd = pfd.getFd();
                    fds.put(fd, new FileInfo(path, path, pfd));
                    Log.d(TAG, "Opening File: " + path + ", FD: " + fd);
                } catch (Exception e) {
                    Log.e(TAG, "Could not open File: " + path + ", FD: " + fd);
                    if (Config.debug)
                        e.printStackTrace();
                }
            }
            return fd;
        }
    }

    public static void close_fds() {
        synchronized (fds) {
            Integer[] fds = FileUtils.fds.keySet().toArray(new Integer[FileUtils.fds.keySet().size()]);
            for (int i = 0; i < fds.length; i++) {
                FileUtils.close_fd(fds[i]);
            }
        }
    }

    /**
     * Closing File Descriptors
     *
     * @param fd File Descriptro to be closed
     * @return Returns 0 if fd is closed successfully otherwise -1
     */
    public static int close_fd(int fd) {
        if (!Config.closeFileDescriptors) {
            return 0;
        }
        synchronized (fds) {
            if (FileUtils.fds.containsKey(fd)) {
                FileInfo info = FileUtils.fds.get(fd);
                try {
                    ParcelFileDescriptor pfd = info.pfd;
                    if(Config.syncFilesOnClose) {
                        try {
                            pfd.getFileDescriptor().sync();
                        } catch (IOException e) {
                            if (Config.debug) {
                                Log.w(TAG, "Syncing DocumentFile: " + info.path + ": " + fd + " : " + e);
                                e.printStackTrace();
                            }
                        }
                    }
                    pfd.close();
                    FileUtils.fds.remove(fd);
                    return 0;
                } catch (IOException e) {
                    Log.e(TAG, "Error Closing DocumentFile: " + info.path + ": " + fd + " : " + e);
                    if (Config.debug)
                        e.printStackTrace();
                }
            } else {
                ParcelFileDescriptor pfd = null;

                try {
                    String path = "";
                    FileInfo info = FileUtils.fds.get(fd);
                    if (info != null) {
                        pfd = info.pfd;
                        path = info.path;
                    }
                    if (pfd == null)
                        pfd = ParcelFileDescriptor.fromFd(fd);
                    if(Config.syncFilesOnClose) {
                        try {
                            pfd.getFileDescriptor().sync();
                        } catch (IOException e) {
                            if (Config.debug) {
                                Log.w(TAG, "Error Syncing File: " + path + ": " + fd + " : " + e);
                                e.printStackTrace();
                            }
                        }
                    }
                    pfd.close();
                    return 0;
                } catch (Exception e) {
                    Log.e(TAG, "Error Closing File FD: " + fd + " : " + e);
                    if (Config.debug)
                        e.printStackTrace();
                }
            }
            return -1;
        }
    }


    public static void startLogging() {
        if (Config.logFilePath == null) {
            Log.w(TAG, "Log file is not ready");
            return;
        }
        Thread t = new Thread(new Runnable() {
            public void run() {
                FileOutputStream os = null;
                File logFile = null;
                try {
                    logFile = new File(Config.logFilePath);
                    if (logFile.exists()) {
                        if (!logFile.delete()) {
                            Log.w(TAG, "Could not delete previous log file!");
                        }
                    }
                    logFile.createNewFile();
                    Runtime.getRuntime().exec("logcat -c");
                    Process process = Runtime.getRuntime().exec("logcat v main");
                    os = new FileOutputStream(logFile);
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                    StringBuilder log = new StringBuilder("");
                    String line = "";
                    while ((line = bufferedReader.readLine()) != null) {
                        log.setLength(0);
                        log.append(line).append("\n");
                        os.write(log.toString().getBytes("UTF-8"));
                        os.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (os != null) {
                            os.flush();
                            os.close();
                        }
                    } catch (IOException e) {

                        e.printStackTrace();
                    }

                }

            }
        });
        t.setName("QubeLogger");
        t.start();
    }

    public static String LoadFile(Context context, String fileName, boolean loadFromRawFolder) throws IOException {
        // Create a InputStream to read the file into
        InputStream iS;
        if (loadFromRawFolder) {
            // get the resource id from the file name
            int rID = context.getResources().getIdentifier(QubeApplication.getInstance().getClass().getPackage().getName() + ":raw/" + fileName,
                    null, null);
            // get the file as a stream
            iS = context.getResources().openRawResource(rID);
        } else {
            // get the file as a stream
            iS = context.getResources().getAssets().open(fileName);
        }

        ByteArrayOutputStream oS = new ByteArrayOutputStream();
        byte[] buffer = new byte[iS.available()];
        int bytesRead = 0;
        while ((bytesRead = iS.read(buffer)) > 0) {
            oS.write(buffer);
        }
        oS.close();
        iS.close();

        // return the output stream as a String
        return oS.toString();
    }

    public static Uri saveLogFileSDCard(Activity activity, Uri destDir) {
        DocumentFile destFileF;
        OutputStream os = null;
        Uri uri = null;

        try {
            String logFileContents = getFileContents(Config.logFilePath);
            DocumentFile dir = DocumentFile.fromTreeUri(activity, destDir);
            if (dir == null)
                throw new Exception("Could not get log path directory");

            //Create the file if doesn't exist
            destFileF = dir.findFile(Config.destLogFilename);
            if (destFileF == null) {
                destFileF = dir.createFile(MimeTypeMap.getSingleton().getMimeTypeFromExtension("txt"), Config.destLogFilename);
            }

            //Write to the dest
            os = activity.getContentResolver().openOutputStream(destFileF.getUri());
            os.write(logFileContents.getBytes());

            //success
            uri = destFileF.getUri();

        } catch (Exception ex) {
            ToastUtils.toastShort(activity, activity.getString(R.string.FailedToSaveLogFile) + ex.getMessage());
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        return uri;
    }

    public static String saveLogFileLegacy(Activity activity,
                                           String destLogFilePath) {

        String filePath = null;
        File destFileF = new File(destLogFilePath, Config.destLogFilename);

        try {
            String logFileContents = getFileContents(Config.logFilePath);
            FileUtils.saveFileContents(destFileF.getAbsolutePath(), logFileContents);

            //success
            filePath = destFileF.getAbsolutePath();

        } catch (Exception ex) {
            ToastUtils.toastShort(activity, activity.getString(R.string.FailedSaveLogFile) + ": " + destFileF.getAbsolutePath() + ", " + activity.getString(R.string.Error)+ ": " + ex.getMessage());
        } finally {
        }
        return filePath;
    }


    public static String getFileUriFromIntent(Activity activity, Intent data, boolean write) {
        if (data == null)
            return null;

        Uri uri = data.getData();
        DocumentFile pickedFile = DocumentFile.fromSingleUri(activity, uri);
        String file = uri.toString();
        if (!file.contains("com.android.externalstorage.documents")) {
            showFileNotSupported(activity);
            return null;
        }
        activity.grantUriPermission(activity.getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (write)
            activity.grantUriPermission(activity.getPackageName(), uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        activity.grantUriPermission(activity.getPackageName(), uri, Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

        int takeFlags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
        if (write)
            takeFlags = takeFlags | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

        activity.getContentResolver().takePersistableUriPermission(uri, takeFlags);
        return file;
    }

    public static String getDirPathFromIntent(Activity activity, Intent data) {
        if (data == null)
            return null;
        Bundle b = data.getExtras();
        String file = b.getString("currDir");
        return file;
    }


    public static String getFilePathFromIntent(Activity activity, Intent data) {
        if (data == null)
            return null;
        Bundle b = data.getExtras();
        String file = b.getString("file");
        return file;
    }

    public static FileType getFileTypeFromIntent(Activity activity, Intent data) {
        if (data == null)
            return null;
        Bundle b = data.getExtras();
        FileType fileType = (FileType) b.getSerializable("fileType");
        return fileType;
    }

    public static void showFileNotSupported(Activity context) {
        DialogUtils.UIAlert(
            context,
            context.getString(R.string.Error),
            context.getString(R.string.FilePathNotSupportedWarning),
            0,
            true,
            true,
            R.drawable.error_24px,
            context.getString(android.R.string.ok),
            null,
            null,
            null,
            null,
            null
        );
    }

    public static void saveLogToFile(final Activity activity, final String logFileDestDir) {
        new Thread(new Runnable() {
            public void run() {

                String displayName = null;
                if (logFileDestDir.startsWith("content://")) {
                    Uri exportDirUri = Uri.parse(logFileDestDir);
                    Uri fileCreatedUri = FileUtils.saveLogFileSDCard(activity,
                            exportDirUri);
                    displayName = FileUtils.getFullPathFromDocumentFilePath(fileCreatedUri.toString());
                } else {
                    String filePath = FileUtils.saveLogFileLegacy(activity, logFileDestDir);
                    displayName = filePath;
                }

                if (displayName != null) {

                    ToastUtils.toastShort(activity, activity.getString(R.string.LogfileSaved));
                }
            }
        }).start();
    }

    public static String convertFilePath(String text, int position) {
        try {
            text = FileUtils.getFullPathFromDocumentFilePath(text);
        } catch (Exception ex) {
            if (Config.debug)
                ex.printStackTrace();
        }

        return text;
    }

    public static class FileInfo {
        public String path;
        public String npath;
        public ParcelFileDescriptor pfd;

        public FileInfo(String path, String npath, ParcelFileDescriptor pfd) {
            this.npath = npath;
            this.path = path;
            this.pfd = pfd;
        }
    }

    public static String createImgFromTemplate(Context context, String templateImage, String destImage, FileType imgType) {

        String imagesDir = QubeSettingsManager.getImagesDir(context);
        String displayName = null;
        String filePath = null;
        if (imagesDir.startsWith("content://")) {
            Uri imagesDirUri = Uri.parse(imagesDir);
            Uri fileCreatedUri = FileInstaller.installImageTemplateToSDCard(context, templateImage,
                    imagesDirUri, "hdtemplates", destImage);
            if (fileCreatedUri != null) {
                displayName = FileUtils.getFullPathFromDocumentFilePath(fileCreatedUri.toString());
                filePath = fileCreatedUri.toString();
            }
        } else {
            filePath = FileInstaller.installImageTemplateToExternalStorage(context, templateImage, imagesDir, "hdtemplates", destImage);
            displayName = filePath;
        }
        if (displayName != null) {
            ToastUtils.toastShort(context, context.getString(R.string.ImageCreated) + ": " + displayName);
            return filePath;
        }
        return null;
    }

    // Browse for files/folders using Storage Access Framework (SAF)
    // This replaces the old FileManager activity
    public static void browse(Activity activity, FileType fileType, int requestCode) {
        String lastDir = getLastDirForBrowse(activity, fileType);

        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            ToastUtils.toastShort(activity, activity.getResources().getString(R.string.sdcardNotMounted));
            return;
        }

        try {
            promptOpenFileASF(activity, fileType, getASFFileManagerRequestCode(requestCode), lastDir);
        } catch (Exception ex) {
            Log.e(TAG, "Error opening SAF file picker: " + ex.getMessage());
            ToastUtils.toastShort(activity, "Error: " + ex.getMessage());
        }
    }

    private static String getLastDirForBrowse(Context context, FileType fileType) {
        if (fileType == FileType.SHARED_DIR) {
            return QubeSettingsManager.getSharedDir(context);
        } else if (fileType == FileType.IMAGE_DIR) {
            return QubeSettingsManager.getImagesDir(context);
        }
        return QubeSettingsManager.getLastDir(context);
    }

    private static int getASFFileManagerRequestCode(int requestCode) {
        switch (requestCode) {
            case Config.OPEN_IMAGE_FILE_REQUEST_CODE:
                return Config.OPEN_IMAGE_FILE_ASF_REQUEST_CODE;
            case Config.OPEN_IMAGE_DIR_REQUEST_CODE:
                return Config.OPEN_IMAGE_DIR_ASF_REQUEST_CODE;
            case Config.OPEN_SHARED_DIR_REQUEST_CODE:
                return Config.OPEN_SHARED_DIR_ASF_REQUEST_CODE;
            case Config.OPEN_LOG_FILE_DIR_REQUEST_CODE:
                return Config.OPEN_LOG_FILE_DIR_ASF_REQUEST_CODE;
            default:
                return requestCode;
        }
    }

    protected static void promptOpenFileASF(Activity context, FileType fileType, int requestCode, String lastDir) {
        Intent intent;

        if (isFileTypeDirectory(fileType)) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        }

        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);

        if (!isFileTypeDirectory(fileType)) {
            String[] fileMimeTypes = getFileMimeTypes(fileType);
            if (fileMimeTypes != null && fileMimeTypes.length > 0) {
                intent.setType(fileMimeTypes[0]);
            }
        }

        if (lastDir != null && lastDir.startsWith("content://")) {
            Uri uri = Uri.parse(lastDir);
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);
        }

        context.startActivityForResult(intent, requestCode);
    }

    private static boolean isFileTypeDirectory(FileType fileType) {
        return (fileType == FileType.SHARED_DIR
                || fileType == FileType.IMAGE_DIR || fileType == FileType.LOG_DIR);
    }

    private static String[] getFileMimeTypes(FileType fileType) {
        return new String[]{"*/*"};
    }
}
