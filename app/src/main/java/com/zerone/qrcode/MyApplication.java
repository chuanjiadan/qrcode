package com.zerone.qrcode;

import android.app.Application;
import android.content.Context;

import com.zerone.qrcode.scaner.ZXingLibrary;

public class MyApplication extends Application {
    public static Context mContext = null;


    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getBaseContext();
        ZXingLibrary.initDisplayOpinion(this);

    }

    @Override
    public Context getBaseContext() {
        return super.getBaseContext();
    }
}
