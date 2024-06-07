package com.optic.gamerhub.utils;

import android.content.Context;
import android.content.res.Configuration;

public class DarkModeDetector {

    // Método para verificar si el tema oscuro está activado
    public static boolean isDarkModeEnabled(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
}
