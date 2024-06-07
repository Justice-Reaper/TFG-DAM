package com.optic.gamerhub.retrofit;

import com.optic.gamerhub.models.FCMBody;
import com.optic.gamerhub.models.FCMResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

// Interfaz para la API de FCM (Firebase Cloud Messaging)
public interface IFCMApi {

    // Anotaciones para establecer las cabeceras y el tipo de solicitud HTTP
    @Headers({
            "Content-Type:application/json",
            "Authorization:key=AAAAVt4J9ms:APA91bEWpns6Y46kgRdTCY_2AlzRkvFLoJD0Fsf1EeOYhhssj3dDWbSKhu79ILJ143QsHh9U9gz5GsRYpQpf5kJ7soqCJ0bCBn7QXrw_mI3-B7vVwxTKhYPQAStpvqQLi-F_sGteA8EZ"

    })

    // Método para enviar una solicitud POST al endpoint "fcm/send"
    @POST("fcm/send")
    Call<FCMResponse> send(@Body FCMBody body);
}
