package com.jc.yangj;

import android.util.Log;

public class LogUtils {
    public static void log(String log) {
        if (BuildConfig.DEBUG) {
            Log.i("YangJ", log);
        }
    }
}
