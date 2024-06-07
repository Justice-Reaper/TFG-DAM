package com.optic.gamerhub.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.optic.gamerhub.R;
import com.optic.gamerhub.adapters.CommentAdapter;
import com.optic.gamerhub.adapters.SliderAdapter;
import com.optic.gamerhub.models.Comment;
import com.optic.gamerhub.models.FCMBody;
import com.optic.gamerhub.models.FCMResponse;
import com.optic.gamerhub.models.SliderItem;
import com.optic.gamerhub.providers.AuthProvider;
import com.optic.gamerhub.providers.CommentsProvider;
import com.optic.gamerhub.providers.LikesProvider;
import com.optic.gamerhub.providers.NotificationProvider;
import com.optic.gamerhub.providers.PostProvider;
import com.optic.gamerhub.providers.TokenProvider;
import com.optic.gamerhub.providers.UsersProvider;
import com.optic.gamerhub.utils.RelativeTime;
import com.optic.gamerhub.utils.ViewedMessageHelper;
import com.smarteist.autoimageslider.IndicatorAnimations;
import com.smarteist.autoimageslider.SliderAnimations;
import com.smarteist.autoimageslider.SliderView;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostDetailActivity extends AppCompatActivity {

    // Declaración de variables
    SliderView mSliderView;
    SliderAdapter mSliderAdapter;
    List<SliderItem> mSliderItems = new ArrayList<>();

    PostProvider mPostProvider;
    UsersProvider mUsersProvider;
    CommentsProvider mCommentsProvider;
    AuthProvider mAuthProvider;
    LikesProvider mLikesProvider;
    NotificationProvider mNotificationProvider;
    TokenProvider mTokenProvider;

    CommentAdapter mAdapter;

    String mExtraPostId;

    TextView mTextViewTitle;
    TextView mTextViewDescription;
    TextView mTextViewUsername;
    TextView mTextViewPhone;
    TextView mTextViewNameCategory;
    TextView mTextViewRelativeTime;
    TextView mTextViewLikes;
    ImageView mImageViewCategory;
    CircleImageView mCircleImageViewProfile;
    Button mButtonShowProfile;
    FloatingActionButton mFabComment;
    RecyclerView mRecyclerView;
    Toolbar mToolbar;

    String mIdUser = "";

    ListenerRegistration mListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        // Inicializar componentes de la interfaz de usuario
        mSliderView = findViewById(R.id.imageSlider);
        mTextViewTitle = findViewById(R.id.textViewTitle);
        mTextViewDescription = findViewById(R.id.textViewDescription);
        mTextViewUsername = findViewById(R.id.textViewUsername);
        mTextViewPhone = findViewById(R.id.textViewPhone);
        mTextViewNameCategory = findViewById(R.id.textViewNameCategory);
        mTextViewRelativeTime = findViewById(R.id.textViewRelativeTime);
        mTextViewLikes = findViewById(R.id.textViewLikes);
        mImageViewCategory = findViewById(R.id.imageViewCategory);
        mCircleImageViewProfile = findViewById(R.id.circleImageProfile);
        mButtonShowProfile = findViewById(R.id.btnShowProfile);
        mFabComment = findViewById(R.id.fabComment);
        mRecyclerView = findViewById(R.id.recyclerViewComments);
        mToolbar = findViewById(R.id.toolbar);

        // Configurar la barra de herramientas
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("");
        Drawable drawable = getResources().getDrawable(R.drawable.ic_arrow_with_background);
        getSupportActionBar().setHomeAsUpIndicator(drawable);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Configurar el administrador de diseño para el RecyclerView de comentarios
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(PostDetailActivity.this);
        mRecyclerView.setLayoutManager(linearLayoutManager);

        // Inicializar proveedores
        mPostProvider = new PostProvider();
        mUsersProvider = new UsersProvider();
        mCommentsProvider = new CommentsProvider();
        mAuthProvider = new AuthProvider();
        mLikesProvider = new LikesProvider();
        mNotificationProvider = new NotificationProvider();
        mTokenProvider = new TokenProvider();

        // Obtener el ID de la publicación del intento
        mExtraPostId = getIntent().getStringExtra("id");

        // Establecer la imagen de perfil predeterminada si aún no se ha cargado
        if (mCircleImageViewProfile.getDrawable() == null) {
            mCircleImageViewProfile.setImageResource(R.drawable.cover_image);
        }

        // Establecer los eventos onClickListener
        mFabComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialogComment();
            }
        });

        mButtonShowProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToShowProfile();
            }
        });

        // Cargar detalles de la publicación y contar los likes
        getPost();
        getNumberLikes();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Manejar el clic en el botón de inicio de la barra de herramientas
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }

    // Obtener el número de Me gusta para la publicación
    private void getNumberLikes() {
        // Escuchar cambios en la colección de Me gusta para la publicación actual
        mListener = mLikesProvider.getLikesByPost(mExtraPostId).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (queryDocumentSnapshots != null) {
                    int numberLikes = queryDocumentSnapshots.size();
                    if (numberLikes == 1) {
                        mTextViewLikes.setText(numberLikes + " Me gusta");
                    } else {
                        mTextViewLikes.setText(numberLikes + " Me gustas");
                    }
                }

            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        // Obtener los comentarios de la publicación y mostrarlos en el RecyclerView
        Query query = mCommentsProvider.getCommentsByPost(mExtraPostId);
        FirestoreRecyclerOptions<Comment> options =
                new FirestoreRecyclerOptions.Builder<Comment>()
                        .setQuery(query, Comment.class)
                        .build();
        mAdapter = new CommentAdapter(options, PostDetailActivity.this);
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.startListening();
        // Actualizar el estado en línea del usuario
        ViewedMessageHelper.updateOnline(true, PostDetailActivity.this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Detener la escucha de cambios en los comentarios cuando la actividad ya no está visible
        mAdapter.stopListening();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Actualizar el estado en línea del usuario cuando la actividad está en pausa
        ViewedMessageHelper.updateOnline(false, PostDetailActivity.this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Detener la escucha de cambios en los Me gusta para evitar fugas de memoria
        if (mListener != null) {
            mListener.remove();
        }
    }

    private void showDialogComment() {
        // Crear un diálogo de alerta para que el usuario ingrese un comentario
        AlertDialog.Builder alert = new AlertDialog.Builder(PostDetailActivity.this);
        alert.setTitle("¡COMENTARIO!");
        alert.setMessage("Ingresa tu comentario");

        // Crear un EditText para que el usuario escriba su comentario
        final EditText editText = new EditText(PostDetailActivity.this);
        editText.setHint("Texto");

        // Configurar los parámetros del EditText
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(36, 0, 36, 36);
        editText.setLayoutParams(params);

        // Agregar el EditText al contenedor del diálogo
        RelativeLayout container = new RelativeLayout(PostDetailActivity.this);
        RelativeLayout.LayoutParams relativeParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        container.setLayoutParams(relativeParams);
        container.addView(editText);

        // Establecer el contenido del diálogo como el contenedor que contiene el EditText
        alert.setView(container);

        // Configurar el botón "OK" para enviar el comentario ingresado por el usuario
        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String value = editText.getText().toString();
                if (!value.isEmpty()) {
                    createComment(value);
                } else {
                    Toast.makeText(PostDetailActivity.this, "Debe ingresar el comentario", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Configurar el botón "Cancelar" para cerrar el diálogo sin realizar ninguna acción
        alert.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        // Mostrar el diálogo de alerta
        alert.show();
    }

    private void createComment(final String value) {
        // Crear un objeto Comment con los datos del comentario
        Comment comment = new Comment();
        comment.setComment(value);
        comment.setIdPost(mExtraPostId);
        comment.setIdUser(mAuthProvider.getUid());
        comment.setTimestamp(new Date().getTime());

        // Guardar el comentario en la base de datos
        mCommentsProvider.create(comment).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    // Enviar notificación al usuario del post sobre el nuevo comentario
                    sendNotification(value);
                    Toast.makeText(PostDetailActivity.this, "El comentario se creo correctamente", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PostDetailActivity.this, "No se pudo crear el comentario", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void sendNotification(final String comment) {
        if (mIdUser == null) {
            return;
        }
        // Obtener el token de notificación del usuario del post
        mTokenProvider.getToken(mIdUser).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    if (documentSnapshot.contains("token")) {
                        String token = documentSnapshot.getString("token");
                        // Configurar los datos de la notificación
                        Map<String, String> data = new HashMap<>();
                        data.put("title", "NUEVO COMENTARIO");
                        data.put("body", comment);
                        // Crear el cuerpo de la notificación FCM
                        FCMBody body = new FCMBody(token, "high", "4500s", data);
                        // Enviar la notificación
                        mNotificationProvider.sendNotification(body).enqueue(new Callback<FCMResponse>() {
                            @Override
                            public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                                if (response.body() != null) {
                                    if (response.body().getSuccess() == 1) {
                                        Toast.makeText(PostDetailActivity.this, "La notificacion se envio correcatemente", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(PostDetailActivity.this, "La notificacion no se pudo enviar", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(PostDetailActivity.this, "La notificacion no se pudo enviar", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<FCMResponse> call, Throwable t) {

                            }
                        });
                    }
                } else {
                    Toast.makeText(PostDetailActivity.this, "El token de notificaciones del usuario no existe", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void goToShowProfile() {
        // Verificar si el ID del usuario está disponible
        if (!mIdUser.equals("")) {
            // Abrir la actividad de perfil de usuario con el ID de usuario correspondiente
            Intent intent = new Intent(PostDetailActivity.this, UserProfileActivity.class);
            intent.putExtra("idUser", mIdUser);
            startActivity(intent);
        } else {
            Toast.makeText(this, "El id del usuario aun no se carga", Toast.LENGTH_SHORT).show();
        }
    }

    private void instanceSlider() {
        // Configurar el adaptador y las propiedades del SliderView para mostrar las imágenes
        mSliderAdapter = new SliderAdapter(PostDetailActivity.this, mSliderItems);
        mSliderView.setSliderAdapter(mSliderAdapter);
        mSliderView.setIndicatorAnimation(IndicatorAnimations.THIN_WORM);
        mSliderView.setSliderTransformAnimation(SliderAnimations.SIMPLETRANSFORMATION);
        mSliderView.setAutoCycleDirection(SliderView.AUTO_CYCLE_DIRECTION_RIGHT);
        mSliderView.setIndicatorSelectedColor(Color.WHITE);
        mSliderView.setIndicatorUnselectedColor(Color.GRAY);
        mSliderView.setScrollTimeInSec(5);
        mSliderView.setAutoCycle(true);
        mSliderView.startAutoCycle();
    }

    private void getPost() {
        // Obtener los detalles de la publicación mediante su ID
        mPostProvider.getPostById(mExtraPostId).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {

                    // Verificar y mostrar la primera imagen de la publicación
                    if (documentSnapshot.contains("image1")) {
                        String image1 = documentSnapshot.getString("image1");
                        SliderItem item = new SliderItem();
                        item.setImageUrl(image1);
                        mSliderItems.add(item);
                    }

                    // Verificar y mostrar la segunda imagen de la publicación
                    if (documentSnapshot.contains("image2")) {
                        String image2 = documentSnapshot.getString("image2");
                        SliderItem item = new SliderItem();
                        item.setImageUrl(image2);
                        mSliderItems.add(item);
                    }

                    // Mostrar el título de la publicación
                    if (documentSnapshot.contains("title")) {
                        String title = documentSnapshot.getString("title");
                        assert title != null;
                        mTextViewTitle.setText(title.toUpperCase());
                    }

                    // Mostrar la descripción de la publicación
                    if (documentSnapshot.contains("description")) {
                        String description = documentSnapshot.getString("description");
                        mTextViewDescription.setText(description);
                    }

                    // Mostrar la categoría de la publicación
                    if (documentSnapshot.contains("category")) {
                        String category = documentSnapshot.getString("category");
                        mTextViewNameCategory.setText(category);

                        // Asignar la imagen de la categoría correspondiente

                        assert category != null;
                        if (category.equals("PS4")) {
                            mImageViewCategory.setImageResource(R.drawable.icon_ps4);
                        } else if (category.equals("XBOX")) {
                            mImageViewCategory.setImageResource(R.drawable.icon_xbox);
                        } else if (category.equals("PC")) {
                            mImageViewCategory.setImageResource(R.drawable.icon_pc);
                        } else if (category.equals("NINTENDO")) {
                            mImageViewCategory.setImageResource(R.drawable.icon_nintendo);
                        }
                    }

                    // Obtener el ID del usuario que realizó la publicación
                    if (documentSnapshot.contains("idUser")) {
                        mIdUser = documentSnapshot.getString("idUser");
                        // Obtener y mostrar la información del usuario
                        getUserInfo(mIdUser);
                    }

                    // Mostrar el tiempo relativo desde la publicación
                    if (documentSnapshot.contains("timestamp")) {
                        try {
                            long timestamp = documentSnapshot.getLong("timestamp");
                            String relativeTime = RelativeTime.getTimeAgo(timestamp, PostDetailActivity.this);
                            mTextViewRelativeTime.setText(relativeTime);
                        }

                        catch (Exception e){

                        }
                    }

                    // Inicializar el SliderView para mostrar las imágenes
                    instanceSlider();
                }
            }
        });
    }

    private void getUserInfo(String idUser) {
        // Obtener la información del usuario mediante su ID
        mUsersProvider.getUser(idUser).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    // Mostrar el nombre de usuario del usuario
                    if (documentSnapshot.contains("username")) {
                        String username = documentSnapshot.getString("username");
                        mTextViewUsername.setText(username);
                    }

                    // Mostrar el número de teléfono del usuario
                    if (documentSnapshot.contains("phone")) {
                        String phone = documentSnapshot.getString("phone");
                        mTextViewPhone.setText(phone);
                    }

                    // Mostrar la imagen de perfil del usuario, si está disponible
                    if (documentSnapshot.contains("image_profile")) {
                        String imageProfile = documentSnapshot.getString("image_profile");
                        if (imageProfile != null && !imageProfile.isEmpty()) {
                            // Si hay una imagen de perfil, cargarla con Picasso
                            Picasso.with(PostDetailActivity.this).load(imageProfile).into(mCircleImageViewProfile);
                        } else {
                            // Si no hay imagen de perfil, establecer la imagen predeterminada desde los recursos drawable
                            mCircleImageViewProfile.setImageResource(R.drawable.cover_image);
                        }
                    } else {
                        // Si el campo "image_profile" no está presente, establecer la imagen predeterminada desde los recursos drawable
                        mCircleImageViewProfile.setImageResource(R.drawable.cover_image);
                    }
                }
            }
        });
    }
}
