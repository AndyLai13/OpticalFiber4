package com.lightel.opticalfiber;

import android.app.Activity;
import android.util.DisplayMetrics;

@SuppressWarnings("SuspiciousNameCombination")
public final class Util {
    public static int getWidth(Activity activity) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().
                getDefaultDisplay().
                getMetrics(displayMetrics);

        return displayMetrics.heightPixels;
    }

    public static int getHeight(Activity activity) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().
                getDefaultDisplay().
                getMetrics(displayMetrics);

        return displayMetrics.widthPixels;
    }
}
