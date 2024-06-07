package com.optic.gamerhub.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.optic.gamerhub.R;
import com.optic.gamerhub.models.Message;
import com.optic.gamerhub.providers.AuthProvider;
import com.optic.gamerhub.providers.UsersProvider;
import com.optic.gamerhub.utils.DarkModeDetector;
import com.optic.gamerhub.utils.RelativeTime;

public class MessagesAdapter extends FirestoreRecyclerAdapter<Message, MessagesAdapter.ViewHolder> {

    // Declaración de variables
    Context context;
    UsersProvider mUsersProvider;
    AuthProvider mAuthProvider;

    // Constructor
    public MessagesAdapter(FirestoreRecyclerOptions<Message> options, Context context) {
        super(options);
        this.context = context;
        mUsersProvider = new UsersProvider();
        mAuthProvider = new AuthProvider();
    }

    // Método para vincular los datos de cada mensaje con la vista de cada elemento
    @Override
    protected void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull final Message message) {
        DocumentSnapshot document = getSnapshots().getSnapshot(position);
        final String messageId = document.getId();
        holder.textViewMessage.setText(message.getMessage());

        // Obtiene y establece la fecha del mensaje en el TextView correspondiente
        String relativeTime = RelativeTime.timeFormatAMPM(message.getTimestamp(), context);
        holder.textViewDate.setText(relativeTime);

        // Establece el diseño y la alineación del mensaje dependiendo de si fue enviado por el usuario actual o por otro usuario
        if (message.getIdSender().equals(mAuthProvider.getUid())) {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.setMargins(150, 0, 0,0);
            holder.linearLayoutMessage.setLayoutParams(params);
            holder.linearLayoutMessage.setPadding(30, 20, 0, 20);
            holder.linearLayoutMessage.setBackground(context.getResources().getDrawable(R.drawable.rounded_linear_layout));
            holder.imageViewViewed.setVisibility(View.VISIBLE);
            holder.textViewMessage.setTextColor(Color.WHITE);
            holder.textViewDate.setTextColor(Color.LTGRAY);
        }
        else {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            params.setMargins(0, 0, 150,0);
            holder.linearLayoutMessage.setLayoutParams(params);
            holder.linearLayoutMessage.setPadding(30, 20, 30, 20);
            holder.linearLayoutMessage.setBackground(context.getResources().getDrawable(R.drawable.rounded_linear_layout_grey));
            holder.imageViewViewed.setVisibility(View.GONE);

            // Verifica si está activado el modo oscuro para establecer el color del texto del mensaje
            if (DarkModeDetector.isDarkModeEnabled(context)) {
                holder.textViewMessage.setTextColor(Color.WHITE);
            } else {
                holder.textViewMessage.setTextColor(Color.DKGRAY);
            }

            holder.textViewDate.setTextColor(Color.LTGRAY);
        }

        // Establece el icono de visualización del mensaje dependiendo de si fue visto o no
        if (message.isViewed()) {
            holder.imageViewViewed.setImageResource(R.drawable.icon_check_blue_light);
        }
        else {
            holder.imageViewViewed.setImageResource(R.drawable.icon_check_grey);
        }

    }

    // Método para crear una nueva vista para cada elemento de la lista
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Infla el diseño de la tarjeta de mensaje para crear la vista
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_message, parent, false);
        return new ViewHolder(view);
    }

    // Clase ViewHolder para representar cada elemento de la lista
    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMessage;
        TextView textViewDate;
        ImageView imageViewViewed;
        LinearLayout linearLayoutMessage;
        View viewHolder;

        // Constructor ViewHolder que recibe la vista de un elemento de la lista
        public ViewHolder(View view) {
            super(view);
            // Inicializa las vistas de los elementos de la tarjeta de mensaje
            textViewMessage = view.findViewById(R.id.textViewMessage);
            textViewDate = view.findViewById(R.id.textViewDateMessage);
            imageViewViewed = view.findViewById(R.id.imageViewViewedMessage);
            linearLayoutMessage = view.findViewById(R.id.linearLayoutMessage);
            viewHolder = view;
        }
    }

}
