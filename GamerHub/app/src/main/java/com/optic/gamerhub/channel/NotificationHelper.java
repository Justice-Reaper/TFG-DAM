package com.optic.gamerhub.channel;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;
import androidx.core.graphics.drawable.IconCompat;

import com.optic.gamerhub.R;
import com.optic.gamerhub.models.Message;

import java.util.Date;

// Clase auxiliar para ayudar en la creación y gestión de notificaciones
public class NotificationHelper extends ContextWrapper {

    // Declaración de variables
    private static final String CHANNEL_ID = "com.optic.gamerhub";
    private static final String CHANNEL_NAME = "gamerhub";

    private NotificationManager manager;

    // Constructor de la clase
    public NotificationHelper(Context context) {
        super(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannels();
        }
    }

    // Método para crear el canal de notificación
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createChannels() {
        NotificationChannel notificationChannel = new NotificationChannel(
          CHANNEL_ID,
          CHANNEL_NAME,
          NotificationManager.IMPORTANCE_HIGH
        );
        notificationChannel.enableLights(true);
        notificationChannel.enableVibration(true);
        notificationChannel.setLightColor(Color.GRAY);
        notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        getManager().createNotificationChannel(notificationChannel);
    }

    // Método para obtener el gestor de notificaciones
    public NotificationManager getManager() {
        if (manager == null) {
            manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return manager;
    }

    // Método para obtener un constructor de notificaciones simple
    public NotificationCompat.Builder getNotification(String title, String body) {
        return new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.notification_icon)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body).setBigContentTitle(title));
    }

    // Método para obtener un constructor de notificaciones de mensaje
    public NotificationCompat.Builder getNotificationMessage(
            Message[] messages,
            String usernameSender,
            String usernameReceiver,
            String lastMessage,
            Bitmap bitmapSender,
            Bitmap bitmapReceiver,
            NotificationCompat.Action action) {

        // Define las personas involucradas en la conversación
        Person person1 = null;
        if (bitmapReceiver == null) {
            person1 = new Person.Builder()
                    .setName(usernameReceiver)
                    .setIcon(IconCompat.createWithResource(getApplicationContext(), R.drawable.ic_person_grey))
                    .build();
        }
        else {
            person1 = new Person.Builder()
                    .setName(usernameReceiver)
                    .setIcon(IconCompat.createWithBitmap(bitmapReceiver))
                    .build();
        }

        Person person2 = null;

        if (bitmapSender == null) {
            person2 = new Person.Builder()
                    .setName(usernameSender)
                    .setIcon(IconCompat.createWithResource(getApplicationContext(), R.drawable.ic_person_grey))
                    .build();
        }
        else {
            person2 = new Person.Builder()
                    .setName(usernameSender)
                    .setIcon(IconCompat.createWithBitmap(bitmapSender))
                    .build();
        }

        // Crea el estilo de la notificación de mensajería
        NotificationCompat.MessagingStyle messagingStyle = new NotificationCompat.MessagingStyle(person1);
        NotificationCompat.MessagingStyle.Message message1 = new
                                    NotificationCompat.MessagingStyle.Message(
                                            lastMessage,
                                            new Date().getTime(),
                                            person1);
        messagingStyle.addMessage(message1);

        for (Message m: messages) {
            NotificationCompat.MessagingStyle.Message message2 = new
                    NotificationCompat.MessagingStyle.Message(
                    m.getMessage(),
                    m.getTimestamp(),
                    person2);
            messagingStyle.addMessage(message2);
        }

        // Construye la notificación y la devuelve
        return new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setStyle(messagingStyle)
                .addAction(action);
    }
}
