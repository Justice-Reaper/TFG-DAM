package com.optic.gamerhub.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;
import com.optic.gamerhub.R;
import com.optic.gamerhub.adapters.MessagesAdapter;
import com.optic.gamerhub.models.Chat;
import com.optic.gamerhub.models.FCMBody;
import com.optic.gamerhub.models.FCMResponse;
import com.optic.gamerhub.models.Message;
import com.optic.gamerhub.providers.AuthProvider;
import com.optic.gamerhub.providers.ChatsProvider;
import com.optic.gamerhub.providers.MessagesProvider;
import com.optic.gamerhub.providers.NotificationProvider;
import com.optic.gamerhub.providers.TokenProvider;
import com.optic.gamerhub.providers.UsersProvider;
import com.optic.gamerhub.utils.DarkModeDetector;
import com.optic.gamerhub.utils.RelativeTime;
import com.optic.gamerhub.utils.ViewedMessageHelper;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    // Declaración de variables
    String mExtraIdUser1;
    String mExtraIdUser2;
    String mExtraIdChat;

    long mIdNotificationChat;

    ChatsProvider mChatsProvider;
    MessagesProvider mMessagesProvider;
    UsersProvider mUsersProvider;
    AuthProvider mAuthProvider;
    NotificationProvider mNotificationProvider;
    TokenProvider mTokenProvider;

    EditText mEditTextMessage;
    ImageView mImageViewSendMessage;

    CircleImageView mCircleImageProfile;
    TextView mTextViewUsername;
    TextView mTextViewRelativeTime;
    ImageView mImageViewBack;
    RecyclerView mRecyclerViewMessage;
    Button mGotoUserActivity;

    MessagesAdapter mAdapter;

    View mActionBarView;

    LinearLayoutManager mLinearLayoutManager;

    ListenerRegistration mListener;

    String mMyUsername;
    String mUsernameChat;
    String mImageReceiver = "";
    String mImageSender = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Inicialización de los proveedores de datos
        mChatsProvider = new ChatsProvider();
        mMessagesProvider = new MessagesProvider();
        mAuthProvider = new AuthProvider();
        mUsersProvider = new UsersProvider();
        mNotificationProvider = new NotificationProvider();
        mTokenProvider = new TokenProvider();

        // Obtención de referencias a las vistas
        ImageView messageBackground = findViewById(R.id.messageBackground);
        mGotoUserActivity = findViewById(R.id.goToUserActivity);
        mEditTextMessage = findViewById(R.id.editTextMessage);
        mImageViewSendMessage = findViewById(R.id.imageViewSendMessage);
        mRecyclerViewMessage = findViewById(R.id.recyclerViewMessage);

        // Configuración del layout del RecyclerView
        mLinearLayoutManager = new LinearLayoutManager(ChatActivity.this);
        mLinearLayoutManager.setStackFromEnd(true);
        mRecyclerViewMessage.setLayoutManager(mLinearLayoutManager);

        // Obtención de los extras pasados a través del Intent
        mExtraIdUser1 = getIntent().getStringExtra("idUser1");
        mExtraIdUser2 = getIntent().getStringExtra("idUser2");
        mExtraIdChat  = getIntent().getStringExtra("idChat");

        // Cambiar el fondo del chat
        if (DarkModeDetector.isDarkModeEnabled(this)){
            messageBackground.setBackgroundResource(R.drawable.message_background_dark);
        }

        else {
            messageBackground.setBackgroundResource(R.drawable.message_background);
        }

        showCustomToolbar(R.layout.custom_chat_toolbar);
        getMyInfoUser();

        mImageViewSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });
        checkIfChatExist();
    }

    // Inicio de la escucha de cambios en los datos y actualización del estado de conexión en línea al iniciar la actividad
    @Override
    public void onStart() {
        super.onStart();
        if (mAdapter != null) {
            mAdapter.startListening();
        }
        ViewedMessageHelper.updateOnline(true, ChatActivity.this);
    }

    // Actualización del estado de conexión fuera de línea al pausar la actividad
    @Override
    protected void onPause() {
        super.onPause();
        ViewedMessageHelper.updateOnline(false, ChatActivity.this);
    }

    // Detención de la escucha de cambios en los datos al detener la actividad
    @Override
    public void onStop() {
        super.onStop();
        mAdapter.stopListening();
    }

    // Eliminación del oyente de cambios en los datos al destruir la actividad
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mListener != null) {
            mListener.remove();
        }
    }

    // Método para obtener los mensajes del chat y configurar el adaptador
    private void getMessageChat() {
        // Obtener consulta de mensajes por chat
        Query query = mMessagesProvider.getMessageByChat(mExtraIdChat);

        // Configurar opciones del adaptador con la consulta
        FirestoreRecyclerOptions<Message> options =
                new FirestoreRecyclerOptions.Builder<Message>()
                        .setQuery(query, Message.class)
                        .build();
        // Inicializar el adaptador de mensajes con las opciones y establecerlo en el RecyclerView
        mAdapter = new MessagesAdapter(options, ChatActivity.this);
        mRecyclerViewMessage.setAdapter(mAdapter);

        // Iniciar la escucha del adaptador
        mAdapter.startListening();

        // Registrar un observador de datos del adaptador para manejar la inserción de nuevos mensajes
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);

                // Actualizar el estado de los mensajes vistos
                updateViewed();

                // Obtener el número total de mensajes y la posición del último mensaje visible
                int numberMessage = mAdapter.getItemCount();
                int lastMessagePosition = mLinearLayoutManager.findLastCompletelyVisibleItemPosition();

                // Desplazar el RecyclerView si el último mensaje está visible
                if (lastMessagePosition == -1 || (positionStart >= (numberMessage -1) && lastMessagePosition == (positionStart - 1))) {
                    mRecyclerViewMessage.scrollToPosition(positionStart);
                }
            }
        });
    }

    // Desplaza el RecyclerView al último elemento
    private void scrollToBottom() {
        mRecyclerViewMessage.post(new Runnable() {
            @Override
            public void run() {
                mRecyclerViewMessage.scrollToPosition(mAdapter.getItemCount() - 1);
            }
        });
    }

    // Método para enviar un mensaje
    private void sendMessage() {
        // Obtener el texto del mensaje del campo de texto
        String textMessage = mEditTextMessage.getText().toString();

        if (!textMessage.isEmpty()) {
            // Crear un objeto de mensaje con la información necesaria
            final Message message = new Message();
            message.setIdChat(mExtraIdChat);

            // Establecer el remitente y el destinatario del mensaje según el usuario actual
            if (mAuthProvider.getUid().equals(mExtraIdUser1)) {
                message.setIdSender(mExtraIdUser1);
                message.setIdReceiver(mExtraIdUser2);
            }
            else {
                message.setIdSender(mExtraIdUser2);
                message.setIdReceiver(mExtraIdUser1);
            }

            // Establecer la marca de tiempo actual y otros detalles del mensaje
            message.setTimestamp(new Date().getTime());
            message.setViewed(false);
            message.setIdChat(mExtraIdChat);
            message.setMessage(textMessage);

            // Crear el mensaje en la base de datos y manejar el resultado
            mMessagesProvider.create(message).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        // Limpiar el campo de texto
                        mEditTextMessage.setText("");
                        // Notificar al adaptador sobre el cambio de datos
                        mAdapter.notifyDataSetChanged();
                        // Desplazar hacia abajo para mostrar el nuevo mensaje
                        scrollToBottom();
                        // Llamar a getToken() u otras funciones relevantes después de enviar el mensaje
                        getToken(message);
                    } else {
                        Toast.makeText(ChatActivity.this, "El mensaje no se pudo crear", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void showCustomToolbar(int resource) {
        // Obtener la barra de herramientas y configurarla como la barra de acción
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Obtener la instancia de ActionBar
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;

        // Configurar la barra de acción
        actionBar.setTitle("");
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        // Inflar el diseño personalizado de la barra de herramientas
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mActionBarView = inflater.inflate(resource, null);
        actionBar.setCustomView(mActionBarView);

        // Obtener referencias a los elementos de la barra de herramientas personalizada
        mCircleImageProfile = mActionBarView.findViewById(R.id.circleImageProfile);
        mTextViewUsername = mActionBarView.findViewById(R.id.textViewUsername);
        mTextViewRelativeTime = mActionBarView.findViewById(R.id.textViewRelativeTime);
        mImageViewBack = mActionBarView.findViewById(R.id.imageViewBack);

        // Establecer un Listener de clic para el botón de retroceso
        mImageViewBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        // Obtener información del usuario para mostrar en la barra de herramientas
        getUserInfo();

    }

    private void getUserInfo() {
        // Determinar el ID del usuario del chat opuesto
        String idUserInfo = "";
        if (mAuthProvider.getUid().equals(mExtraIdUser1)) {
            idUserInfo = mExtraIdUser2;
        }
        else {
            idUserInfo = mExtraIdUser1;
        }

        // Escuchar cambios en los datos del usuario en tiempo real
        mListener = mUsersProvider.getUserRealtime(idUserInfo).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {

                // Verificar si el documento existe y contiene datos relevantes
                assert documentSnapshot != null;
                if (documentSnapshot.exists()) {
                    if (documentSnapshot.contains("username")) {
                        mUsernameChat = documentSnapshot.getString("username");
                        mTextViewUsername.setText(mUsernameChat);
                    }

                    // Obtener el estado de conexión del usuario y mostrarlo en la barra de herramientas
                    if (documentSnapshot.contains("online")) {
                        boolean online = Boolean.TRUE.equals(documentSnapshot.getBoolean("online"));
                        if (online) {
                            mTextViewRelativeTime.setText("En linea");
                        }
                        else if (documentSnapshot.contains("lastConnect")) {
                            try {
                                long lastConnect = documentSnapshot.getLong("lastConnect");
                                String relativeTime = RelativeTime.getTimeAgo(lastConnect, ChatActivity.this);
                                mTextViewRelativeTime.setText(relativeTime);
                            } catch (NullPointerException ex) {
                                // Manejar la excepción
                            }

                        }
                    }

                    // Obtener la URL del perfil de imagen del usuario y cargarla en la barra de herramientas
                    if (documentSnapshot.contains("image_profile")) {
                        mImageReceiver = documentSnapshot.getString("image_profile");
                        if (mImageReceiver != null) {
                            if (!mImageReceiver.equals("")) {
                                Picasso.with(ChatActivity.this).load(mImageReceiver).into(mCircleImageProfile);
                            }
                        }
                    }
                }
            }
        });
    }

    private void checkIfChatExist() {
        // Consultar el chat por los IDs de los usuarios
        mChatsProvider.getChatByUser1AndUser2(mExtraIdUser1, mExtraIdUser2).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                // Obtener el número de chats encontrados
                int size = queryDocumentSnapshots.size();

                // Si no se encuentra ningún chat, crear uno nuevo
                if (size == 0) {
                    createChat();
                }
                else {
                    try {
                        // Obtener el ID del chat y la notificación del primer documento encontrado
                        mExtraIdChat = queryDocumentSnapshots.getDocuments().get(0).getId();
                        mIdNotificationChat = queryDocumentSnapshots.getDocuments().get(0).getLong("idNotification");

                        // Obtener y mostrar los mensajes del chat
                        getMessageChat();

                        // Obtener y mostrar los mensajes del chat
                        updateViewed();
                    } catch (NullPointerException ex) {
                        // Manejar la excepción
                    }
                }
            }
        });
    }

    // Método para marcar los mensajes como vistos en el chat
    private void updateViewed() {

        // Determinar el ID del remitente del mensaje no actual
        String idSender = "";

        if (mAuthProvider.getUid().equals(mExtraIdUser1)) {
            idSender = mExtraIdUser2;
        }
        else {
            idSender = mExtraIdUser1;
        }

        // Obtener los mensajes del chat enviados por el remitente y marcarlos como vistos
        mMessagesProvider.getMessagesByChatAndSender(mExtraIdChat, idSender).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                // Recorrer todos los documentos de mensajes del remitente
                for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                    // Marcar el mensaje como visto en la base de datos
                    mMessagesProvider.updateViewed(document.getId(), true);
                }
            }
        });

    }


    private void createChat() {

        // Crear un nuevo objeto Chat
        Chat chat = new Chat();
        chat.setIdUser1(mExtraIdUser1);
        chat.setIdUser2(mExtraIdUser2);
        chat.setWriting(false);
        chat.setTimestamp(new Date().getTime());
        chat.setId(mExtraIdUser1 + mExtraIdUser2);

        // Generar un ID de notificación aleatorio para el chat
        Random random = new Random();
        int n = random.nextInt(1000000);
        chat.setIdNotification(n);
        mIdNotificationChat = n;

        // Crear una lista de IDs de usuarios para el chat
        ArrayList<String> ids = new ArrayList<>();
        ids.add(mExtraIdUser1);
        ids.add(mExtraIdUser2);
        chat.setIds(ids);

        // Guardar el chat en la base de datos
        mChatsProvider.create(chat);

        // Obtener el ID del chat creado
        mExtraIdChat = chat.getId();

        // Mostrar los mensajes del chat
        getMessageChat();
    }

    // Método para obtener el token de notificación del usuario receptor del mensaje
    private void getToken(final Message message) {
        // Determinar el ID del usuario receptor del mensaje
        String idUser = "";
        if (mAuthProvider.getUid().equals(mExtraIdUser1)) {
            idUser = mExtraIdUser2;
        }
        else {
            idUser = mExtraIdUser1;
        }

        // Obtener el token de notificación del usuario receptor
        mTokenProvider.getToken(idUser).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    if (documentSnapshot.contains("token")) {
                        // Obtener el token de notificación del documento
                        String token = documentSnapshot.getString("token");

                        // Obtener los últimos tres mensajes y enviar la notificación
                        getLastThreeMessages(message, token);
                    }
                }
                else {
                    // Mostrar un mensaje si el token de notificaciones del usuario no existe
                    Toast.makeText(ChatActivity.this, "El token de notificaciones del usuario no existe", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Método para obtener los últimos tres mensajes del chat y enviar una notificación
    private void getLastThreeMessages(final Message message, final String token) {
        // Obtener los últimos tres mensajes del chat del remitente actual
        mMessagesProvider.getLastThreeMessagesByChatAndSender(mExtraIdChat, mAuthProvider.getUid()).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                // Crear una lista para almacenar los mensajes
                ArrayList<Message> messageArrayList = new ArrayList<>();

                // Recorrer los documentos de mensajes obtenidos
                for (DocumentSnapshot d: queryDocumentSnapshots.getDocuments()) {
                    // Verificar si el documento existe
                    if (d.exists()) {
                        // Convertir el documento a un objeto Message y añadirlo a la lista
                        Message message = d.toObject(Message.class);
                        messageArrayList.add(message);
                    }
                }

                // Si no se obtienen mensajes, añadir el mensaje actual a la lista
                if (messageArrayList.size() == 0) {
                    messageArrayList.add(message);
                }

                // Invertir el orden de los mensajes en la lista
                Collections.reverse(messageArrayList);

                // Convertir la lista de mensajes a formato JSON
                Gson gson = new Gson();
                String messages = gson.toJson(messageArrayList);

                // Enviar la notificación con los mensajes y el mensaje actual
                sendNotification(token, messages, message);
            }
        });
    }


    private void sendNotification(final String token, String messages, Message message) {
        // Crear un map para almacenar los datos de la notificación
        final Map<String, String> data = new HashMap<>();
        data.put("title", "NUEVO MENSAJE");
        data.put("body", message.getMessage());
        data.put("idNotification", String.valueOf(mIdNotificationChat));
        data.put("messages", messages);
        data.put("usernameSender", mMyUsername.toUpperCase());
        data.put("usernameReceiver", mUsernameChat.toUpperCase());
        data.put("idSender", message.getIdSender());
        data.put("idReceiver", message.getIdReceiver());
        data.put("idChat", message.getIdChat());

        // Verificar si mImageSender y mImageReceiver son null antes de usarlos
        if (mImageSender == null || mImageSender.isEmpty()) {
            mImageSender = "IMAGEN_NO_VALIDA";
        }
        if (mImageReceiver == null || mImageReceiver.isEmpty()) {
            mImageReceiver = "IMAGEN_NO_VALIDA";
        }

        data.put("imageSender", mImageSender);
        data.put("imageReceiver", mImageReceiver);

        // Determinar el ID del remitente para obtener el último mensaje enviado
        String idSender = "";
        if (mAuthProvider.getUid().equals(mExtraIdUser1)) {
            idSender = mExtraIdUser2;
        }
        else {
            idSender = mExtraIdUser1;
        }

        // Obtener el último mensaje enviado por el remitente
        mMessagesProvider.getLastMessageSender(mExtraIdChat, idSender).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                // Obtener el número de documentos
                int size = queryDocumentSnapshots.size();
                String lastMessage = "";
                if (size > 0) {
                    // Obtener el último mensaje
                    lastMessage = queryDocumentSnapshots.getDocuments().get(0).getString("message");
                    data.put("lastMessage", lastMessage);
                }

                // Crear el cuerpo de la notificación
                FCMBody body = new FCMBody(token, "high", "4500s", data);
                // Enviar la notificación utilizando el proveedor de notificaciones
                mNotificationProvider.sendNotification(body).enqueue(new Callback<FCMResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<FCMResponse> call, @NonNull Response<FCMResponse> response) {
                        if (response.body() != null) {
                            if (response.body().getSuccess() == 1) {
                                //Toast.makeText(ChatActivity.this, "La notificacion se envio correcatemente", Toast.LENGTH_SHORT).show();
                            }
                            else {
                                Toast.makeText(ChatActivity.this, "La notificacion no se pudo enviar", Toast.LENGTH_SHORT).show();
                            }
                        }
                        else {
                            Toast.makeText(ChatActivity.this, "La notificacion no se pudo enviar", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<FCMResponse> call, Throwable t) {}
                });
            }
        });

    }

    // Método para obtener la información del usuario actual
    private void getMyInfoUser() {
        // Obtener la información del usuario actual del proveedor de usuarios
        mUsersProvider.getUser(mAuthProvider.getUid()).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                // Verificar si el documento existe
                if (documentSnapshot.exists()) {
                    // Obtener el nombre de usuario si está disponible
                    if (documentSnapshot.contains("username")) {
                        mMyUsername = documentSnapshot.getString("username");
                    }

                    // Obtener la imagen de perfil del usuario si está disponible
                    if (documentSnapshot.contains("image_profile")) {
                        mImageSender = documentSnapshot.getString("image_profile");
                    }
                }
            }
        });
    }

    // Método invocado cuando se hace clic en el botón para mostrar el perfil del usuario
    public void goToShowProfile(View view) {
        // Verificar si el ID del usuario extra no está vacío
        if (!mExtraIdUser2.equals("")) {
            // Crear un intent para abrir la actividad del perfil de usuario
            Intent intent = new Intent(ChatActivity.this, UserProfileActivity.class);
            // Pasar el ID del usuario extra como extra al intent
            intent.putExtra("idUser",mExtraIdUser2);
            // Iniciar la actividad del perfil de usuario
            startActivity(intent);
        } else {
            Toast.makeText(this, "El id del usuario aun no se carga", Toast.LENGTH_SHORT).show();
        }
    }
}
