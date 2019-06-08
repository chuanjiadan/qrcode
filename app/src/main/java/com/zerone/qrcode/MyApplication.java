package com.zerone.qrcode;

import android.app.Application;

import com.zerone.qrcode.scaner.ZXingLibrary;

public class MyApplication extends Application {


    @Override
    public void onCreate() {
        super.onCreate();
        ZXingLibrary.initDisplayOpinion(this);

    }
}
