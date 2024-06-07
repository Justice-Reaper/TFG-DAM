package com.optic.gamerhub.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.optic.gamerhub.R;
import com.optic.gamerhub.fragments.ChatsFragment;
import com.optic.gamerhub.fragments.FiltersFragment;
import com.optic.gamerhub.fragments.HomeFragment;
import com.optic.gamerhub.fragments.ProfileFragment;
import com.optic.gamerhub.providers.AuthProvider;
import com.optic.gamerhub.providers.TokenProvider;
import com.optic.gamerhub.providers.UsersProvider;
import com.optic.gamerhub.utils.ViewedMessageHelper;

public class HomeActivity extends AppCompatActivity {

    // Declaración de variables
    static final String SELECTED_FRAGMENT = "selected_fragment";

    BottomNavigationView bottomNavigation;

    TokenProvider mTokenProvider;
    AuthProvider mAuthProvider;
    UsersProvider mUsersProvider;

    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Inicialización de componentes y proveedores
        sharedPreferences = getSharedPreferences("selected_fragment", MODE_PRIVATE);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnNavigationItemSelectedListener(navigationItemSelectedListener);
        mTokenProvider = new TokenProvider();
        mAuthProvider = new AuthProvider();
        mUsersProvider = new UsersProvider();

        // Manejo del botón de retroceso
        OnBackPressedDispatcher dispatcher = this.getOnBackPressedDispatcher();
        dispatcher.addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Si el fragmento actual es HomeFragment, finalizamos la actividad.
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.container);
                if (currentFragment instanceof HomeFragment) {
                    if (isTaskRoot()) {
                        // Si esta es la última actividad en la pila, finalizamos la aplicación.
                        finishAffinity();
                    } else {
                        // Si no, reemplazamos el fragmento actual por HomeFragment.
                        openFragment(new HomeFragment());
                        bottomNavigation.setSelectedItemId(R.id.itemHome);
                    }
                } else {
                    // Si no, reemplazamos el fragmento actual por HomeFragment.
                    openFragment(new HomeFragment());
                    bottomNavigation.setSelectedItemId(R.id.itemHome);
                }
            }
        });

        // Obtener el fragmento seleccionado del Intent
        String selectedFragment = getIntent().getStringExtra(SELECTED_FRAGMENT);

        if (selectedFragment != null) {
            switch (selectedFragment) {
                case "com.optic.gamerhub.fragments.HomeFragment":
                    openFragment(new HomeFragment());
                    bottomNavigation.setSelectedItemId(R.id.itemHome);
                    break;
                case "com.optic.gamerhub.fragments.ChatsFragment":
                    openFragment(new ChatsFragment());
                    bottomNavigation.setSelectedItemId(R.id.itemChats);
                    break;
                case "com.optic.gamerhub.fragments.FiltersFragment":
                    openFragment(new FiltersFragment());
                    bottomNavigation.setSelectedItemId(R.id.itemFilters);
                    break;
                case "com.optic.gamerhub.fragments.ProfileFragment":
                    openFragment(new ProfileFragment());
                    bottomNavigation.setSelectedItemId(R.id.itemProfile);
                    break;
                default:
                    openFragment(new HomeFragment());
                    bottomNavigation.setSelectedItemId(R.id.itemHome);
                    break;
            }
        } else {
            openFragment(new HomeFragment()); // Fragmento por defecto si no se selecciona ninguno
        }

        // Creación del token de registro de dispositivos
        createToken();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Actualizar estado en línea del usuario
        ViewedMessageHelper.updateOnline(true, HomeActivity.this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Actualizar estado en línea del usuario cuando la actividad está pausada
        ViewedMessageHelper.updateOnline(false, HomeActivity.this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Guardar el fragmento actualmente mostrado en SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.container);
        if (currentFragment instanceof HomeFragment) {
            editor.putString(SELECTED_FRAGMENT, HomeFragment.class.getSimpleName());
        } else if (currentFragment instanceof ChatsFragment) {
            editor.putString(SELECTED_FRAGMENT, ChatsFragment.class.getSimpleName());
        } else if (currentFragment instanceof FiltersFragment) {
            editor.putString(SELECTED_FRAGMENT, FiltersFragment.class.getSimpleName());
        } else if (currentFragment instanceof ProfileFragment) {
            editor.putString(SELECTED_FRAGMENT, ProfileFragment.class.getSimpleName());
        }
        editor.apply();
    }

    public void openFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();

        // Intenta sacar el fragmento de la pila de retroceso
        boolean fragmentPopped = fragmentManager.popBackStackImmediate(fragment.getClass().getName(), 0);

        // Si el fragmento no está en la pila de retroceso, reemplaza el fragmento actual y lo agrega a la pila
        if (!fragmentPopped && fragmentManager.findFragmentByTag(fragment.getClass().getName()) == null) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.container, fragment, fragment.getClass().getName());
            transaction.addToBackStack(fragment.getClass().getName());
            transaction.commit();
        }
    }

    BottomNavigationView.OnNavigationItemSelectedListener navigationItemSelectedListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    // Verifica qué elemento ha sido seleccionado y abre el fragmento correspondiente
                    if (item.getItemId() == R.id.itemHome) {
                        openFragment(new HomeFragment());
                    } else if (item.getItemId() == R.id.itemChats) {
                        openFragment(new ChatsFragment());
                    } else if (item.getItemId() == R.id.itemFilters) {
                        openFragment(new FiltersFragment());
                    } else if (item.getItemId() == R.id.itemProfile) {
                        openFragment(new ProfileFragment());
                    }
                    return true;
                }
            };

    private void createToken() {
        mTokenProvider.create(mAuthProvider.getUid());
    }
}
