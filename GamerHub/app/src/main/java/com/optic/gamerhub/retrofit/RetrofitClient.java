package com.optic.gamerhub.retrofit;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

// Clase para configurar el cliente Retrofit
public class RetrofitClient {

    // Método estático para obtener un cliente Retrofit con la URL especificada
    public static Retrofit getClient(String url) {
        // Construye un objeto Retrofit con la URL base y un convertidor Gson
        Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(url)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        return retrofit;
    }

}
