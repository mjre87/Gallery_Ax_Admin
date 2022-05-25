package com.gallery_ax.admin;

import android.app.Application;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.zxy.tiny.Tiny;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // غیر فعال کردن حالت آفلاین Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false)
                .build();
        db.setFirestoreSettings(settings);

        // مقداردهی فشرده ساز تصویر
        Tiny.getInstance().init(this);
    }
}
