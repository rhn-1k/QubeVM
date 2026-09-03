/*
Copyright (C) Max Kastanas 2012
Copyright (C) Rhn 2026
 */
package com.max2idea.android.qube.machine;

import java.util.ArrayList;

public class MachineFilePaths {

    public static void insertRecentFilePath(Machine.FileType fileType, String filePath) {
        if (fileType == null || filePath == null || filePath.equals(""))
            return;
        if (!isRecentFilePathStored(fileType, filePath)) {
            FavOpenHelper.getInstance().insertFav(fileType.name().toLowerCase(), filePath);
        }
    }

    public static boolean isRecentFilePathStored(Machine.FileType type, String filePath) {
        return FavOpenHelper.getInstance().getFavSeq(type.toString().toLowerCase(), filePath) >= 0;
    }
    synchronized
    public static  ArrayList<String> getRecentFilePaths(Machine.FileType fileType) {
        return FavOpenHelper.getInstance().getFav(fileType.toString().toLowerCase());
    }

    public static synchronized boolean clearAllRecentFilePaths() {
        return FavOpenHelper.getInstance().deleteAllFavs();
    }

    public static synchronized boolean clearRecentFilePathsByType(Machine.FileType fileType) {
        return FavOpenHelper.getInstance().deleteFavsByType(fileType.toString().toLowerCase());
    }

}
