package com.optic.gamerhub.providers;

import com.optic.gamerhub.models.FCMBody;
import com.optic.gamerhub.models.FCMResponse;
import com.optic.gamerhub.retrofit.IFCMApi;
import com.optic.gamerhub.retrofit.RetrofitClient;

import retrofit2.Call;

public class NotificationProvider {

    private String url = "https://fcm.googleapis.com";

    public NotificationProvider() {

    }

    public Call<FCMResponse> sendNotification(FCMBody body) {
        return RetrofitClient.getClient(url).create(IFCMApi.class).send(body);
    }

}
