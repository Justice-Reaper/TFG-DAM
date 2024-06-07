package com.optic.gamerhub.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.optic.gamerhub.R;
import com.optic.gamerhub.activities.ChatActivity;
import com.optic.gamerhub.models.Chat;
import com.optic.gamerhub.providers.AuthProvider;
import com.optic.gamerhub.providers.ChatsProvider;
import com.optic.gamerhub.providers.MessagesProvider;
import com.optic.gamerhub.providers.UsersProvider;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatsAdapter extends FirestoreRecyclerAdapter<Chat, ChatsAdapter.ViewHolder> {

    // Variables de contexto y proveedores de datos
    Context context;
    UsersProvider mUsersProvider;
    AuthProvider mAuthProvider;
    ChatsProvider mChatsProvider;
    MessagesProvider mMessagesProvider;
    ListenerRegistration mListener;
    ListenerRegistration mListenerLastMessage;

    // Constructor del adaptador
    public ChatsAdapter(FirestoreRecyclerOptions<Chat> options, Context context) {
        super(options);
        this.context = context;
        mUsersProvider = new UsersProvider();
        mAuthProvider = new AuthProvider();
        mChatsProvider = new ChatsProvider();
        mMessagesProvider = new MessagesProvider();
    }

    // Método para enlazar los datos a la vista
    @Override
    protected void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull final Chat chat) {
        // Obtener el documento actual y el ID del chat
        DocumentSnapshot document = getSnapshots().getSnapshot(position);
        final String chatId = document.getId();

        // Obtener información del usuario del chat
        if (mAuthProvider.getUid().equals(chat.getIdUser1())) {
            getUserInfo(chat.getIdUser2(), holder);
        }
        else {
            getUserInfo(chat.getIdUser1(), holder);
        }

        // Configurar clic en el chat para abrir la actividad de chat
        holder.viewHolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToChatActivity(chatId, chat.getIdUser1(), chat.getIdUser2());
            }
        });

        getLastMessage(chatId, holder.textViewLastMessage);

        // Obtener el número de mensajes no leídos y el último mensaje del chat
        String idSender = "";
        if (mAuthProvider.getUid().equals(chat.getIdUser1())) {
            idSender = chat.getIdUser2();
        }
        else {
            idSender = chat.getIdUser1();
        }
        getMessageNotRead(chatId, idSender, holder.textViewMessageNotRead, holder.frameLayoutMessageNotRead);

    }

    // Método para obtener el número de mensajes no leídos
    private void getMessageNotRead(String chatId, String idSender, final TextView textViewMessageNotRead, final FrameLayout frameLayoutMessageNotRead) {

        mListener = mMessagesProvider.getMessagesByChatAndSender(chatId, idSender).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (queryDocumentSnapshots != null) {
                    int size = queryDocumentSnapshots.size();
                    if (size > 0) {
                        frameLayoutMessageNotRead.setVisibility(View.VISIBLE);
                        textViewMessageNotRead.setText(String.valueOf(size));
                    }
                    else {
                        frameLayoutMessageNotRead.setVisibility(View.GONE);
                    }
                }
            }
        });

    }

    public ListenerRegistration getListener() {
        return mListener;
    }

    public ListenerRegistration getListenerLastMessage() {
        return mListenerLastMessage;
    }

    // Método para obtener el último mensaje del chat
    private void getLastMessage(String chatId, final TextView textViewLastMessage) {
        mListenerLastMessage = mMessagesProvider.getLastMessage(chatId).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (queryDocumentSnapshots != null) {
                    int size = queryDocumentSnapshots.size();
                    if (size > 0) {
                        String lastMessage = queryDocumentSnapshots.getDocuments().get(0).getString("message");
                        textViewLastMessage.setText(lastMessage);
                    }
                }
            }
        });
    }

    // Método para abrir la actividad de chat
    private void goToChatActivity(String chatId, String idUser1, String idUser2) {
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra("idChat", chatId);
        intent.putExtra("idUser1", idUser1);
        intent.putExtra("idUser2", idUser2);
        intent.putExtra("fondo", R.drawable.message_background_dark);
        context.startActivity(intent);
    }

    // Método para obtener información del usuario
    private void getUserInfo(String idUser, final ViewHolder holder) {
        mUsersProvider.getUser(idUser).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    if (documentSnapshot.contains("username")) {
                        String username = documentSnapshot.getString("username");
                        assert username != null;
                        holder.textViewUsername.setText(username.toUpperCase());
                    }
                    if (documentSnapshot.contains("image_profile")) {
                        String imageProfile = documentSnapshot.getString("image_profile");
                        if (imageProfile != null) {
                            if (!imageProfile.isEmpty()) {
                                Picasso.with(context).load(imageProfile).into(holder.circleImageChat);
                            }
                        }
                    }
                }
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflar el diseño del elemento de la lista desde XML
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_chats, parent, false);
        // Devolver una nueva instancia del ViewHolder
        return new ViewHolder(view);
    }

    // Clase ViewHolder que contiene las vistas de cada elemento de la lista
    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewUsername;
        TextView textViewLastMessage;
        TextView textViewMessageNotRead;
        CircleImageView circleImageChat;
        FrameLayout frameLayoutMessageNotRead;
        View viewHolder;

        // Constructor que asigna las vistas a los elementos correspondientes
        public ViewHolder(View view) {
            super(view);
            textViewUsername = view.findViewById(R.id.textViewUsernameChat);
            textViewLastMessage = view.findViewById(R.id.textViewLastMessageChat);
            textViewMessageNotRead = view.findViewById(R.id.textViewMessageNotRead);
            circleImageChat = view.findViewById(R.id.circleImageChat);
            frameLayoutMessageNotRead = view.findViewById(R.id.frameLayoutMessageNotRead);
            viewHolder = view;
        }
    }

}
