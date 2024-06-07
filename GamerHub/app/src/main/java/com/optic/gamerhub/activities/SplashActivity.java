package com.optic.gamerhub.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    // Tiempo de espera antes de cargar el tema del splash screen (en milisegundos)
    private static final long SPLASH_SCREEN_DELAY = 2000; // 3 segundos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Verificar si la versión de Android es mayor o igual a Android 12 (S)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            // Retraso antes de cargar el tema del splash screen
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    goToMainActivity();
                }
            }, SPLASH_SCREEN_DELAY);
        } else {
            // Método para iniciar la actividad principal (MainActivity)
            goToMainActivity();
        }
    }

    // Método para iniciar la actividad principal (MainActivity)
    private void goToMainActivity() {
        // Crear un intent para iniciar la actividad MainActivity
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        // Iniciar la actividad MainActivity
        startActivity(intent);
        // Finalizar la actividad del splash screen para evitar que el usuario pueda volver a ella
        finish();
    }
}
