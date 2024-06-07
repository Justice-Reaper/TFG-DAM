package com.optic.gamerhub.providers;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.optic.gamerhub.models.Chat;

import java.util.ArrayList;

public class ChatsProvider {

    CollectionReference mCollection;

    public ChatsProvider() {
        mCollection = FirebaseFirestore.getInstance().collection("Chats");
    }

    public void create(Chat chat) {
        mCollection.document(chat.getIdUser1() + chat.getIdUser2()).set(chat);
    }

    public Query getAll(String idUser) {
        return mCollection.whereArrayContains("ids", idUser);
    }

    public Query getChatByUser1AndUser2(String idUser1, String idUser2) {
        ArrayList<String> ids = new ArrayList<>();
        ids.add(idUser1 + idUser2);
        ids.add(idUser2 + idUser1);
        return mCollection.whereIn("id", ids);
    }

}
