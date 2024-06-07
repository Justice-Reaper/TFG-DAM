package com.optic.gamerhub.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.optic.gamerhub.R;
import com.optic.gamerhub.models.User;
import com.optic.gamerhub.providers.AuthProvider;
import com.optic.gamerhub.providers.UsersProvider;
import com.optic.gamerhub.utils.DarkModeDetector;

import java.io.File;
import java.util.Date;
import java.util.Objects;

import dmax.dialog.SpotsDialog;

public class CompleteProfileActivity extends AppCompatActivity {

    // Declaración de variables
    TextInputEditText mTextInputUsername;
    TextInputEditText mTextInputPhone;
    Button mButtonRegister;
    AuthProvider mAuthProvider;
    UsersProvider mUsersProvider;
    AlertDialog mDialog;
    ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_profile);

        // Inicialización de vistas y proveedores
        mTextInputUsername = findViewById(R.id.textInputUsername);
        mTextInputPhone = findViewById(R.id.textInputPhone);
        mButtonRegister = findViewById(R.id.btnRegister);
        mImageView = findViewById(R.id.image);

        mAuthProvider = new AuthProvider();
        mUsersProvider = new UsersProvider();

        // Configuración del diálogo de espera
        mDialog = new SpotsDialog.Builder()
                .setContext(this)
                .setMessage("Espere un momento")
                .setCancelable(false).build();

        // Verificar el modo oscuro y cambiar la imagen
        if (DarkModeDetector.isDarkModeEnabled(this)) {
            mImageView.setImageResource(R.drawable.ic_person);
        } else {
            mImageView.setImageResource(R.drawable.ic_person_grey);
        }

        // Configurar el callback para el botón de retroceso
        OnBackPressedDispatcher dispatcher = this.getOnBackPressedDispatcher();
        dispatcher.addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Acciones al presionar el botón de retroceso
                logOut();
                deleteUserAccount();
                clearAppCache();
            }
        });

        // Configurar el listener para el botón de registro
        mButtonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                register();
            }
        });

    }

    // Borrar la caché de la aplicación
    public void clearAppCache() {
        try {
            File cache = getApplicationContext().getCacheDir();
            deleteDir(cache);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Método recursivo para eliminar un directorio y sus subdirectorios
    public boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < Objects.requireNonNull(children).length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if(dir!= null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }

    // Método para cerrar sesión
    private void logOut() {
        // Cerrar sesión del proveedor de autenticación
        mAuthProvider.logout();
        // Crear un intent para ir a la actividad principal
        Intent intent = new Intent(CompleteProfileActivity.this, MainActivity.class);
        // Iniciar la actividad principal
        startActivity(intent);
        // Cerrar todas las actividades abiertas en la pila de actividades
        finishAffinity();
    }

    // Método para eliminar la cuenta del usuario
    private void deleteUserAccount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            user.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(CompleteProfileActivity.this, "Error al iniciar sesión", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(CompleteProfileActivity.this, "Error al iniciar sesión, se asignó un nombre de usuario y teléfono por defecto", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    // Método para registrar al usuario
    private void register() {
        String username = Objects.requireNonNull(mTextInputUsername.getText()).toString().toLowerCase();
        String phone = Objects.requireNonNull(mTextInputPhone.getText()).toString();
        if (!username.isEmpty()) {
            updateUser(username, phone);
        }
        else {
            Toast.makeText(this, "Para continuar inserta todos los campos", Toast.LENGTH_SHORT).show();
        }
    }

    // Método para actualizar la información del usuario en la base de datos
    private void updateUser(final String username, final String phone) {
        // Obtener el ID del usuario actualmente autenticado
        String id = mAuthProvider.getUid();

        // Crear un nuevo objeto User con la información actualizada
        User user = new User();
        user.setUsername(username);
        user.setId(id);
        user.setPhone(phone);
        user.setTimestamp(new Date().getTime());

        // Mostrar un diálogo de carga mientras se realiza la actualización de usuario
        mDialog.show();

        // Llamar al método update() de UsersProvider para actualizar la información del usuario en la base de datos
        mUsersProvider.update(user).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                // Ocultar el diálogo de carga después de completar la operación
                mDialog.dismiss();
                if (task.isSuccessful()) {
                    // Si la actualización fue exitosa, redirigir al usuario a la actividad HomeActivity
                    Intent intent = new Intent(CompleteProfileActivity.this, HomeActivity.class);
                    startActivity(intent);
                }
                else {
                    Toast.makeText(CompleteProfileActivity.this, "No se pudo almacenar el usuario en la base de datos", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
