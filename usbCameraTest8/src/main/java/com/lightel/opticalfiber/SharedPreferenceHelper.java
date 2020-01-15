package com.lightel.opticalfiber;

import android.content.Context;

import static android.content.Context.MODE_PRIVATE;

public class SharedPreferenceHelper {

    public static final String PREF_NAME_D1000 = ProbeManager.getInstance().DI1000.probeName;
    public static final String PREF_NAME_D1000L = ProbeManager.getInstance().DI1000L.probeName;
    public static final String PREF_NAME_D2000 = ProbeManager.getInstance().DI2000.probeName;
    public static final String PREF_NAME_D3000 = ProbeManager.getInstance().DI3000.probeName;
    public static final String PREF_NAME_D5000 = ProbeManager.getInstance().DI5000.probeName;

    public static final String PREF_KEY_CONTRAST = "PREF_KEY_CONTRAST";
    public static final String PREF_KEY_BRIGHTNESS = "PREF_KEY_BRIGHTNESS";

    public static int getData(Context context, String prefName, String prefKey) {
        return context.getSharedPreferences(prefName, MODE_PRIVATE).getInt(prefKey, -1);
    }

    public static void setData(Context context, String prefName, String prefKey, int value) {
        context.getSharedPreferences(prefName, MODE_PRIVATE).edit().putInt(prefKey, value).apply();
    }
}
