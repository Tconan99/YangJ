package com.jc.yangj;

import im.fir.sdk.FIR;
import android.app.Application;

public class MyApplication extends Application {

    @Override
    public void onCreate() {

        super.onCreate();
        FIR.init(this);
    }
}