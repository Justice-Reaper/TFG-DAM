package com.optic.gamerhub.utils;

import android.app.Application;
import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Date;

public class RelativeTime extends Application {

    // Declaración de variables
    private static final int SECOND_MILLIS = 1000;
    private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final int DAY_MILLIS = 24 * HOUR_MILLIS;

    // Método para obtener el tiempo relativo a partir de un timestamp
    public static String getTimeAgo(long time, Context ctx) {
        if (time < 1000000000000L) {
            // Si el timestamp se proporciona en segundos, convertir a milisegundos
            time *= 1000;
        }

        long now = System.currentTimeMillis();
        if (time > now || time <= 0) {
            return "Hace un momento";
        }

        // Diferencia de tiempo entre el momento actual y el timestamp
        final long diff = now - time;
        if (diff < MINUTE_MILLIS) {
            return "Hace un momento";
        } else if (diff < 2 * MINUTE_MILLIS) {
            return "Hace un minuto";
        } else if (diff < 50 * MINUTE_MILLIS) {
            return "Hace " + diff / MINUTE_MILLIS + " minutos";
        } else if (diff < 90 * MINUTE_MILLIS) {
            return "Hace una hora";
        } else if (diff < 24 * HOUR_MILLIS) {
            return "Hace " + diff / HOUR_MILLIS + " horas";
        } else if (diff < 48 * HOUR_MILLIS) {
            return "Ayer";
        } else {
            return "Hace " + diff / DAY_MILLIS + " dias";
        }
    }

    // Método para formatear la hora en formato AM/PM
    public static String timeFormatAMPM(long time, Context ctx) {

        SimpleDateFormat formatter = new SimpleDateFormat("hh:mm a");


        if (time < 1000000000000L) {
            // Si el timestamp se proporciona en segundos, convertir a milisegundos
            time *= 1000;
        }

        long now = System.currentTimeMillis();
        if (time > now || time <= 0) {
            // Si el tiempo es futuro o nulo, formatear la fecha actual
            String dateString = formatter.format(new Date(time));
            return dateString;
        }

        // Diferencia de tiempo entre el momento actual y el timestamp
        final long diff = now - time;
        if (diff < 24 * HOUR_MILLIS) {
            // Si la diferencia es menor a 24 horas, formatear la hora
            String dateString = formatter.format(new Date(time));
            return dateString;
        } else if (diff < 48 * HOUR_MILLIS) {
            return "Ayer";
        } else {
            return "Hace " + diff / DAY_MILLIS + " dias";
        }

    }

}

