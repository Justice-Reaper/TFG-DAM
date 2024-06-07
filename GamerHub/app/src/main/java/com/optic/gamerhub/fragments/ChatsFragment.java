package com.optic.gamerhub.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.Query;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.optic.gamerhub.R;
import com.optic.gamerhub.adapters.ChatsAdapter;
import com.optic.gamerhub.models.Chat;
import com.optic.gamerhub.providers.AuthProvider;
import com.optic.gamerhub.providers.ChatsProvider;
import com.optic.gamerhub.providers.UsersProvider;

import java.util.List;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 */
public class ChatsFragment extends Fragment implements MaterialSearchBar.OnSearchActionListener {

    // Declaración de variables
    ChatsAdapter mAdapter;
    ChatsAdapter mChatsAdapterSearch;
    RecyclerView mRecyclerView;
    View mView;
    MaterialSearchBar mSearchBar;
    ChatsProvider mChatsProvider;
    UsersProvider mUsersProvider;
    AuthProvider mAuthProvider;
    Toolbar mToolbar;

    String usernameToSearch;

    // Constructor vació
    public ChatsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflar el diseño para este fragmento
        mView = inflater.inflate(R.layout.fragment_chats, container, false);
        mRecyclerView = mView.findViewById(R.id.recyclerViewChats);
        mToolbar = mView.findViewById(R.id.toolbar);
        mSearchBar = mView.findViewById(R.id.searchBar);

        // Configurar la barra de herramientas
        ((AppCompatActivity) requireActivity()).setSupportActionBar(mToolbar);
        Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setTitle("Chats");

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(linearLayoutManager);

        // Inicializar los proveedores
        mUsersProvider = new UsersProvider();
        mChatsProvider = new ChatsProvider();
        mAuthProvider = new AuthProvider();
        mSearchBar.setOnSearchActionListener(this);

        return mView;
    }

    @Override
    public void onSearchStateChanged(boolean enabled) {
        if (!enabled) {
            getAllChats();
        }
    }

    @Override
    public void onSearchConfirmed(CharSequence text) {
        // Manejar la confirmación de la búsqueda
        usernameToSearch = text.toString().toLowerCase();
        searchForUser(usernameToSearch);
    }

    // Método para buscar usuarios por nombre de usuario
    private void searchForUser(String username) {
        Task<List<String>> searchTask = mUsersProvider.searchUsersByUsername(username);

        searchTask.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<String> userIds = task.getResult();
                for (String i : userIds) {
                    System.out.println("USERID: " + i);
                }
                getFilteredChats(userIds);
            } else {
                // Maneja la excepción
                Exception e = task.getException();
                // Muestra un mensaje de error o realiza alguna acción de recuperación
            }
        });
    }

    // Método para obtener los chats filtrados por usuarios encontrados
    private void getFilteredChats (List<String> userIds) {
        if (userIds != null && !userIds.isEmpty()) {
            Query query = mChatsProvider.getAll(mAuthProvider.getUid())
                    .whereArrayContainsAny("ids", userIds);

            FirestoreRecyclerOptions<Chat> options =
                    new FirestoreRecyclerOptions.Builder<Chat>()
                            .setQuery(query, Chat.class)
                            .build();
            mChatsAdapterSearch = new ChatsAdapter(options, getContext());
            mChatsAdapterSearch.notifyDataSetChanged();
            mRecyclerView.setAdapter(mChatsAdapterSearch);
            mChatsAdapterSearch.startListening();
        }
        else{
            Query query = mChatsProvider.getAll(null);
            FirestoreRecyclerOptions<Chat> options =
                    new FirestoreRecyclerOptions.Builder<Chat>()
                            .setQuery(query, Chat.class)
                            .build();
            mChatsAdapterSearch = new ChatsAdapter(options, getContext());
            mChatsAdapterSearch.notifyDataSetChanged();
            mRecyclerView.setAdapter(mChatsAdapterSearch);
            mChatsAdapterSearch.startListening();
        }
    }

    @Override
    public void onButtonClicked(int buttonCode) {
        // Manejar clics en botones si es necesario
    }

    // Método para obtener todos los chats
    private void getAllChats(){
        Query query = mChatsProvider.getAll(mAuthProvider.getUid());
        FirestoreRecyclerOptions<Chat> options =
                new FirestoreRecyclerOptions.Builder<Chat>()
                        .setQuery(query, Chat.class)
                        .build();
        mAdapter = new ChatsAdapter(options, getContext());
        mAdapter.notifyDataSetChanged();
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.startListening();
    }

    @Override
    public void onStart() {
        super.onStart();
        getAllChats();
    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            mAdapter.stopListening();
            if (mChatsAdapterSearch != null) {
                mChatsAdapterSearch.stopListening();
            }
        }
        catch (Exception e){}
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (mAdapter.getListener() != null) {
                mAdapter.getListener().remove();
            }
        }
        catch (Exception e){}
    }
}
