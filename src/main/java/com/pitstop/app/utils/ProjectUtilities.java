package com.pitstop.app.utils;

public class ProjectUtilities {
    public static String getRealUsername(String modifiedUsername) {
        return modifiedUsername.substring(modifiedUsername.indexOf('_') + 1);
    }
}
