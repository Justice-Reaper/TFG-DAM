package com.optic.gamerhub.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.firestore.DocumentSnapshot;
import com.optic.gamerhub.R;
import com.optic.gamerhub.models.User;
import com.optic.gamerhub.providers.AuthProvider;
import com.optic.gamerhub.providers.UsersProvider;

import java.util.Objects;

import dmax.dialog.SpotsDialog;

public class MainActivity extends AppCompatActivity {

    // Declaración de variables
    TextView mTextViewRegister;
    TextInputEditText mTextInputEmail;
    TextInputEditText mTextInputPassword;
    Button mButtonLogin;
    AuthProvider mAuthProvider;
    SignInButton mButtonGoogle;
    GoogleSignInClient mGoogleSignInClient;
    UsersProvider mUsersProvider;
    private final int REQUEST_CODE_GOOGLE = 1;
    AlertDialog mDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicialización de vistas
        mTextViewRegister = findViewById(R.id.textViewRegister);
        mTextInputEmail = findViewById(R.id.textInputEmail);
        mTextInputPassword = findViewById(R.id.textInputPassword);
        mButtonLogin = findViewById(R.id.btnLogin);
        mButtonGoogle = findViewById(R.id.btnLoginGoogle);

        // Inicialización de proveedores y diálogo de carga
        mAuthProvider = new AuthProvider();
        mDialog = new SpotsDialog.Builder()
                .setContext(this)
                .setMessage("Espere un momento")
                .setCancelable(false).build();

        // Configuración del botón de inicio de sesión con Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mUsersProvider = new UsersProvider();

        // Configuración de listeners para los botones
        mButtonGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signInGoogle();
            }
        });

        mButtonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                login();
            }
        });

        mTextViewRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Verificar si hay una sesión de usuario activa
        if (mAuthProvider.getUserSession() != null) {
            // Si hay una sesión activa, redirigir a la actividad principal y limpiar la pila de actividades anteriores
            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    private void signInGoogle() {
        // Obtener el intent para iniciar sesión con Google
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        // Iniciar la actividad para el resultado esperando el resultado del inicio de sesión con Google
        startActivityForResult(signInIntent, REQUEST_CODE_GOOGLE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Verificar si el resultado proviene del inicio de sesión con Google
        if (requestCode == REQUEST_CODE_GOOGLE) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Obtener la cuenta de Google desde la tarea completada con éxito
                GoogleSignInAccount account = task.getResult(ApiException.class);
                // Autenticar con Firebase utilizando la cuenta de Google obtenida
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // El inicio de sesión con Google falló, actualizar la interfaz de usuario adecuadamente
                Log.w("ERROR", "Google sign in failed", e);
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        // Mostrar el diálogo de carga
        mDialog.show();
        // Autenticar con Firebase utilizando la cuenta de Google proporcionada
        mAuthProvider.googleLogin(acct).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // Verificar si la autenticación fue exitosa
                        if (task.isSuccessful()) {
                            // Obtener el ID del usuario autenticado
                            String id = mAuthProvider.getUid();
                            // Verificar si el usuario ya existe en la base de datos
                            checkUserExist(id);
                        }
                        else {
                            // Si la autenticación falla, ocultar el diálogo de carga y mostrar un mensaje al usuario
                            mDialog.dismiss();
                            Log.w("ERROR", "signInWithCredential:failure", task.getException());
                            Toast.makeText(MainActivity.this, "No se pudo iniciar sesion con google", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void checkUserExist(final String id) {
        // Verificar si el usuario ya existe en la base de datos
        mUsersProvider.getUser(id).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                // Si el usuario existe, iniciar sesión y dirigirlo a la actividad principal
                if (documentSnapshot.exists()) {
                    mDialog.dismiss();
                    Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                    startActivity(intent);
                }

                // Si el usuario no existe, crear un nuevo usuario en la base de datos
                else {
                    String email = mAuthProvider.getEmail();
                    User user = new User();
                    user.setEmail(email);
                    user.setId(id);

                    // Crear el nuevo usuario en la base de datos
                    mUsersProvider.create(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            mDialog.dismiss();

                            // Verificar si la creación del usuario fue exitosa
                            if (task.isSuccessful()) {

                                // Si la creación del usuario fue exitosa, dirigir al usuario a completar su perfil
                                Intent intent = new Intent(MainActivity.this, CompleteProfileActivity.class);
                                startActivity(intent);
                            }
                            else {
                                Toast.makeText(MainActivity.this, "No se pudo almacenar la informacion del usuario", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });
    }

    private void login() {
        // Obtener el correo electrónico y la contraseña ingresados por el usuario
        String email = Objects.requireNonNull(mTextInputEmail.getText()).toString();
        String password = Objects.requireNonNull(mTextInputPassword.getText()).toString();

        // Verificar si los campos de correo electrónico y contraseña están vacíos
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(MainActivity.this, "Por favor ingresa un correo electrónico y una contraseña", Toast.LENGTH_LONG).show();
            return; // Detener el proceso de inicio de sesión
        }

        // Mostrar el diálogo de carga mientras se procesa el inicio de sesión
        mDialog.show();

        // Iniciar sesión con el correo electrónico y la contraseña proporcionados
        mAuthProvider.login(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                // Ocultar el diálogo de carga después de que se complete el inicio de sesión
                mDialog.dismiss();
                if (task.isSuccessful()) {
                    // Si el inicio de sesión fue exitoso, dirigir al usuario a la actividad principal
                    Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
                else {
                    Toast.makeText(MainActivity.this, "El email o la contraseña que ingresaste no son correctas", Toast.LENGTH_LONG).show();
                }
            }
        });
        Log.d("CAMPO", "email: " + email);
        Log.d("CAMPO", "password: " + password);
    }

}
