package com.augmentis.ayp.findlocation;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Created by Apinya on 9/5/2016.
 */
public class LocationPref {
    public static final String PREF_USE_FUSE = "using_fuse";

    public static Boolean getSharedPref(Context context, String prefKey) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(prefKey, false);
    }
}
