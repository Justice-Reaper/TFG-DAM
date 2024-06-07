package com.optic.gamerhub.utils;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;

import com.optic.gamerhub.providers.AuthProvider;
import com.optic.gamerhub.providers.UsersProvider;

import java.util.List;

public class ViewedMessageHelper {

    // Método para actualizar el estado en línea del usuario
    public static void updateOnline(boolean status, final Context context) {

        // Proveedor de usuarios y proveedor de autenticación
        UsersProvider usersProvider = new UsersProvider();
        AuthProvider authProvider = new AuthProvider();

        // Verificar si el usuario está autenticado
        if (authProvider.getUid() != null) {
            // Verificar si la aplicación está en segundo plano
            if (isApplicationSentToBackground(context)) {
                // Actualizar el estado en línea del usuario si la aplicación está en segundo plano
                usersProvider.updateOnline(authProvider.getUid(), status);
            }
            else if (status){
                // Actualizar el estado en línea del usuario si la aplicación está en primer plano
                usersProvider.updateOnline(authProvider.getUid(), status);
            }
        }
    }

    // Método para verificar si la aplicación está en segundo plano
    public static boolean isApplicationSentToBackground(final Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(1);
        if (!tasks.isEmpty()) {
            ComponentName topActivity = tasks.get(0).topActivity;
            assert topActivity != null;
            // Verificar si la actividad superior no pertenece al paquete de la aplicación
            if (!topActivity.getPackageName().equals(context.getPackageName())) {
                return true;
            }
        }
        return false;
    }

}
