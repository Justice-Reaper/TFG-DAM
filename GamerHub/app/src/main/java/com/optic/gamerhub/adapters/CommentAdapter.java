package com.optic.gamerhub.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.optic.gamerhub.R;
import com.optic.gamerhub.models.Comment;
import com.optic.gamerhub.providers.UsersProvider;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class CommentAdapter extends FirestoreRecyclerAdapter<Comment, CommentAdapter.ViewHolder> {

    // Declaración de variables
    Context context;
    UsersProvider mUsersProvider;

    // Constructor
    public CommentAdapter(FirestoreRecyclerOptions<Comment> options, Context context) {
        super(options);
        this.context = context;
        mUsersProvider = new UsersProvider();
    }

    // Método para vincular los datos de la lista con la vista de cada elemento
    @Override
    protected void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull Comment comment) {

        DocumentSnapshot document = getSnapshots().getSnapshot(position);
        final String commentId = document.getId();
        String idUser = document.getString("idUser");

        holder.textViewComment.setText(comment.getComment());
        getUserInfo(idUser, holder);
    }

    // Método para obtener y mostrar la información del usuario que realizó el comentario
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
                                Picasso.with(context).load(imageProfile).into(holder.circleImageComment);
                            }
                        }
                    }
                }
            }
        });
    }

    // Método para crear una nueva vista para cada elemento de la lista
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_comment, parent, false);
        return new ViewHolder(view);
    }

    // Clase ViewHolder para representar cada elemento de la lista
    public class ViewHolder extends RecyclerView.ViewHolder {

        // Decleración de variables
        TextView textViewUsername;
        TextView textViewComment;
        CircleImageView circleImageComment;
        View viewHolder;

        // Constructor ViewHolder que recibe la vista de un elemento de la lista
        public ViewHolder(View view) {
            super(view);
            // Inicializa las vistas de los elementos de la tarjeta de comentario
            textViewUsername = view.findViewById(R.id.textViewUsername);
            textViewComment = view.findViewById(R.id.textViewComment);
            circleImageComment = view.findViewById(R.id.circleImageComment);
            viewHolder = view;
        }
    }

}
