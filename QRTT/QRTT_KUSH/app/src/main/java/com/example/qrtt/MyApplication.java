package com.example.qrtt;

import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.jakewharton.threetenabp.AndroidThreeTen;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        AndroidThreeTen.init(this); // Инициализация ThreeTenABP
    }
}