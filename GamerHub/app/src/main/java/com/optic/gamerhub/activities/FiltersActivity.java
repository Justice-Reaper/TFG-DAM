package com.optic.gamerhub.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.Query;
import com.optic.gamerhub.R;
import com.optic.gamerhub.adapters.PostsAdapter;
import com.optic.gamerhub.models.Post;
import com.optic.gamerhub.providers.AuthProvider;
import com.optic.gamerhub.providers.PostProvider;
import com.optic.gamerhub.utils.ViewedMessageHelper;

import java.util.Objects;

public class FiltersActivity extends AppCompatActivity {

    // Declaración de variables
    String mExtraCategory;

    AuthProvider mAuthProvider;
    RecyclerView mRecyclerView;
    PostProvider mPostProvider;
    PostsAdapter mPostsAdapter;

    TextView mTextViewNumberFilter;
    Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filters);

        // Inicialización de vistas
        mRecyclerView = findViewById(R.id.recyclerViewFilter);
        mToolbar = findViewById(R.id.toolbar);
        mTextViewNumberFilter = findViewById(R.id.textViewNumberFilter);

        // Configuración de la barra de herramientas
        setSupportActionBar(mToolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Filtros");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Configuración del RecyclerView con un diseño de cuadrícula
        mRecyclerView.setLayoutManager(new GridLayoutManager(FiltersActivity.this, 2));

        // Obtener la categoría extraída del Intent
        mExtraCategory = getIntent().getStringExtra("category");

        // Inicialización de proveedores de autenticación y publicaciones
        mAuthProvider = new AuthProvider();
        mPostProvider = new PostProvider();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Obtener la consulta de publicaciones por categoría y marca de tiempo
        Query query = mPostProvider.getPostByCategoryAndTimestamp(mExtraCategory);

        // Configurar las opciones del adaptador FirestoreRecyclerOptions
        FirestoreRecyclerOptions<Post> options =
                new FirestoreRecyclerOptions.Builder<Post>()
                        .setQuery(query, Post.class)
                        .build();

        // Inicializar el adaptador de publicaciones y establecerlo en el RecyclerView
        mPostsAdapter = new PostsAdapter(options, FiltersActivity.this, mTextViewNumberFilter);
        mRecyclerView.setAdapter(mPostsAdapter);

        // Iniciar la escucha de cambios en la base de datos
        mPostsAdapter.startListening();

        // Actualizar el estado en línea del usuario
        ViewedMessageHelper.updateOnline(true, FiltersActivity.this);
    }

    @Override
    public void onStop() {
        super.onStop();
        // Detener la escucha de cambios en la base de datos
        mPostsAdapter.stopListening();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Actualizar el estado en línea del usuario
        ViewedMessageHelper.updateOnline(false, FiltersActivity.this);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Manejar el evento de hacer clic en el botón de inicio
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }
}
