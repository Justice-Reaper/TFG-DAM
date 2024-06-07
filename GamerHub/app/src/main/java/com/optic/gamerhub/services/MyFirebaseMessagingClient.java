package com.optic.gamerhub.services;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.optic.gamerhub.R;
import com.optic.gamerhub.channel.NotificationHelper;
import com.optic.gamerhub.models.Message;
import com.optic.gamerhub.receivers.MessageReceiver;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.Map;
import java.util.Random;

// Servicio para manejar los mensajes recibidos de Firebase Cloud Messaging
public class MyFirebaseMessagingClient extends FirebaseMessagingService {

    // Declaración de variables
    public static final String NOTIFICATION_REPLY = "NotificationReply";

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
    }


    // Método llamado cuando se recibe un mensaje de Firebase Cloud Messaging
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Obtiene los datos del mensaje
        Map<String, String> data = remoteMessage.getData();
        String title = data.get("title");
        String body = data.get("body");

        // Verifica el título del mensaje
        if (title != null) {
            // Si es un nuevo mensaje, muestra la notificación del mensaje
            if (title.equals("NUEVO MENSAJE")) {
                showNotificationMessage(data);
            }
            else {
                // De lo contrario, muestra la notificación con el título y el cuerpo recibidos
                showNotification(title, body);
            }
        }
    }

    // Método para mostrar una notificación simple
    private void showNotification(String title, String body) {
        NotificationHelper notificationHelper = new NotificationHelper(getBaseContext());
        NotificationCompat.Builder builder = notificationHelper.getNotification(title, body);

        // Genera un ID aleatorio para la notificación
        Random random = new Random();
        int n = random.nextInt(10000);

        // Muestra la notificación
        notificationHelper.getManager().notify(n, builder.build());
    }

    // Método para manejar la notificación de un nuevo mensaje
    private void showNotificationMessage(Map<String, String> data) {
        final String imageSender = data.get("imageSender");
        final String imageReceiver = data.get("imageReceiver");
        Log.d("ENTRO", "NUEVO MENSAJE");
        // Obtiene la imagen del remitente del mensaje
        getImageSender(data, imageSender, imageReceiver);
    }

    // Método para obtener la imagen del remitente del mensaje
    private void getImageSender(final Map<String, String> data, final String imageSender, final String imageReceiver) {

        new Handler(Looper.getMainLooper())
                .post(new Runnable() {
                    @Override
                    public void run() {
                        // Carga la imagen del remitente usando Picasso
                        Picasso.with(getApplicationContext())
                                .load(imageSender)
                                .into(new Target() {
                                    @Override
                                    public void onBitmapLoaded(final Bitmap bitmapSender, Picasso.LoadedFrom from) {
                                        // Una vez cargada la imagen del remitente, obtiene la imagen del destinatario
                                        getImageReceiver(data, imageReceiver, bitmapSender);
                                    }

                                    @Override
                                    public void onBitmapFailed(Drawable errorDrawable) {
                                        // En caso de error al cargar la imagen del remitente, continúa con la siguiente acción
                                        getImageReceiver(data, imageReceiver, null);
                                    }

                                    @Override
                                    public void onPrepareLoad(Drawable placeHolderDrawable) {
                                        // Método vacío necesario para la implementación de Target
                                    }
                                });
                    }
                });

    }

    // Método para obtener la imagen del destinatario del mensaje
    private void getImageReceiver(final Map<String, String> data, String imageReceiver, final Bitmap bitmapSender) {

        // Carga la imagen del destinatario usando Picasso
        Picasso.with(getApplicationContext())
                .load(imageReceiver)
                .into(new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmapReceiver, Picasso.LoadedFrom from) {
                        // Una vez cargada la imagen del destinatario, notifica el mensaje
                        notifyMessage(data, bitmapSender, bitmapReceiver);
                    }

                    @Override
                    public void onBitmapFailed(Drawable errorDrawable) {
                        // En caso de error al cargar la imagen del destinatario, continúa con la siguiente acción
                        notifyMessage(data, bitmapSender, null);
                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {
                        // Método vacío necesario para la implementación de Target
                    }
                });

    }

    // Método para notificar el mensaje al usuario
    private void notifyMessage(Map<String, String> data, Bitmap bitmapSender, Bitmap bitmapReceiver) {
        final String usernameSender = data.get("usernameSender");
        final String usernameReceiver = data.get("usernameReceiver");
        final String lastMessage = data.get("lastMessage");
        String messagesJSON = data.get("messages");
        final String imageSender = data.get("imageSender");

        final String imageReceiver = data.get("imageReceiver");
        final String idSender = data.get("idSender");
        final String idReceiver = data.get("idReceiver");
        final String idChat = data.get("idChat");
        final int idNotification = Integer.parseInt(data.get("idNotification"));

        // Crea un intent para la recepción del mensaje
        Intent intent = new Intent(this, MessageReceiver.class);
        intent.putExtra("idSender", idSender);
        intent.putExtra("idReceiver", idReceiver);
        intent.putExtra("idChat", idChat);
        intent.putExtra("idNotification", idNotification);
        intent.putExtra("usernameSender", usernameSender);
        intent.putExtra("usernameReceiver", usernameReceiver);
        intent.putExtra("imageSender", imageSender);
        intent.putExtra("imageReceiver", imageReceiver);

        // Crea un PendingIntent para el intent
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

        // Crea un RemoteInput para la entrada de texto
        RemoteInput remoteInput = new RemoteInput.Builder(NOTIFICATION_REPLY).setLabel("Tu mensaje...").build();

        // Crea una acción para responder al mensaje
        final NotificationCompat.Action action = new NotificationCompat.Action.Builder(
                R.drawable.notification_icon,
                "Responder",
                pendingIntent)
                .addRemoteInput(remoteInput)
                .build();

        // Convierte el JSON de los mensajes en un arreglo de objetos Message usando Gson
        Gson gson = new Gson();
        final Message[] messages = gson.fromJson(messagesJSON, Message[].class);

        // Crea una instancia de NotificationHelper para construir la notificación
        NotificationHelper notificationHelper = new NotificationHelper(getBaseContext());
        // Crea y muestra la notificación con el estilo de mensajería
        NotificationCompat.Builder builder =
                notificationHelper.getNotificationMessage(
                        messages,
                        usernameSender,
                        usernameReceiver,
                        lastMessage,
                        bitmapSender,
                        bitmapReceiver,
                        action
                );
        // Muestra la notificación
        notificationHelper.getManager().notify(idNotification, builder.build());
    }

}
