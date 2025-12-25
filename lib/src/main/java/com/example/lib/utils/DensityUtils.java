package com.example.lib.utils;

import android.content.res.Resources;

public class DensityUtils {
    public static int dpToPx(float dpValue) {
        final float density = Resources.getSystem().getDisplayMetrics().density;
        int value = Math.round(dpValue * density);
        if (value == 0 && dpValue != 0) {
            value = 1;
        }

        return value;
    }
}
