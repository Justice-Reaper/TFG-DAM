package com.optic.gamerhub.receivers;

import static com.optic.gamerhub.services.MyFirebaseMessagingClient.NOTIFICATION_REPLY;

import android.app.NotificationManager;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.gson.Gson;
import com.optic.gamerhub.models.FCMBody;
import com.optic.gamerhub.models.FCMResponse;
import com.optic.gamerhub.models.Message;
import com.optic.gamerhub.providers.MessagesProvider;
import com.optic.gamerhub.providers.NotificationProvider;
import com.optic.gamerhub.providers.TokenProvider;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageReceiver extends BroadcastReceiver {

    // Declaración de variables
    String mExtraIdSender;
    String mExtraIdReceiver;
    String mExtraIdChat;
    String mExtraUsernameSender;
    String mExtraUsernameReceiver;
    String mExtraImageSender;
    String mExtraImageReceiver;
    int mExtraIdNotification;

    TokenProvider mTokenProvider;
    NotificationProvider mNotificationProvider;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Obtener datos extras del intent
        mExtraIdSender = Objects.requireNonNull(intent.getExtras()).getString("idSender");
        mExtraIdReceiver = intent.getExtras().getString("idReceiver");
        mExtraIdChat = intent.getExtras().getString("idChat");
        mExtraUsernameSender = intent.getExtras().getString("usernameSender");
        mExtraUsernameReceiver = intent.getExtras().getString("usernameReceiver");
        mExtraImageSender = intent.getExtras().getString("imageSender");
        mExtraImageReceiver = intent.getExtras().getString("imageReceiver");

        mExtraIdNotification = intent.getExtras().getInt("idNotification");

        // Inicialización de proveedores
        mTokenProvider = new TokenProvider();
        mNotificationProvider = new NotificationProvider();

        // Cancelar la notificación
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(mExtraIdNotification);

        // Obtener el mensaje de la notificación
        String message = Objects.requireNonNull(getMessageText(intent)).toString();

        // Enviar el mensaje
        sendMessage(message);
    }

    // Método para enviar el mensaje recibido
    private void sendMessage(String messageText) {
        // Crear objeto mensaje
        final Message message = new Message();
        message.setIdChat(mExtraIdChat);
        message.setIdSender(mExtraIdReceiver);
        message.setIdReceiver(mExtraIdSender);
        message.setTimestamp(new Date().getTime());
        message.setViewed(false);
        message.setIdChat(mExtraIdChat);
        message.setMessage(messageText);

        // Proveedor de mensajes
        MessagesProvider messagesProvider = new MessagesProvider();
        // Crear el mensaje en la base de datos
        messagesProvider.create(message).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    // Obtener el token del remitente
                    getToken(message);
                }
            }
        });
    }

    // Método para obtener el token del remitente
    private void getToken(final Message message) {
        mTokenProvider.getToken(mExtraIdSender).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    if (documentSnapshot.contains("token")) {
                        // Obtener el token
                        String token = documentSnapshot.getString("token");
                        // Convertir el mensaje a JSON
                        Gson gson = new Gson();
                        ArrayList<Message> messagesArray = new ArrayList<>();
                        messagesArray.add(message);
                        String messages = gson.toJson(messagesArray);
                        // Enviar la notificación al destinatario
                        sendNotification(token, messages, message);
                    }
                }
            }
        });
    }

    // Método para enviar la notificación al destinatario
    private void sendNotification(final String token, String messages, Message message) {
        // Crear datos para la notificación
        final Map<String, String> data = new HashMap<>();
        data.put("title", "NUEVO MENSAJE");
        data.put("body", message.getMessage());
        data.put("idNotification", String.valueOf(mExtraIdNotification));
        data.put("messages", messages);
        data.put("usernameSender", mExtraUsernameReceiver.toUpperCase());
        data.put("usernameReceiver", mExtraUsernameSender.toUpperCase());
        data.put("idSender", message.getIdSender());
        data.put("idReceiver", message.getIdReceiver());
        data.put("idChat", message.getIdChat());
        data.put("imageSender", mExtraImageReceiver);
        data.put("imageReceiver", mExtraImageSender);

        // Crear el cuerpo de la notificación FCM
        FCMBody body = new FCMBody(token, "high", "4500s", data);
        mNotificationProvider.sendNotification(body).enqueue(new Callback<FCMResponse>() {
            @Override
            public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                // Manejar respuesta
            }

            @Override
            public void onFailure(Call<FCMResponse> call, Throwable t) {
                Log.d("ERROR", "El error fue: " + t.getMessage());
            }
        });

    }

    // Método para obtener el texto del mensaje desde el intent
    private CharSequence getMessageText(Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            return remoteInput.getCharSequence(NOTIFICATION_REPLY);
        }
        return null;
    }
}
