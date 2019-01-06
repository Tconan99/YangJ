package com.jc.yangj;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;


@SuppressWarnings("deprecation")
public class MainActivity extends Activity {


    private Switch isServiceSwitch = null;

    private static boolean isHand = true;

    private static MediaPlayer mPlayer = null;
    private static Vibrator mVibrator = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        //启动抢红包服务
        isServiceSwitch = findViewById(R.id.main_isserviceopen_switch);
        isServiceSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isHand) {
                    Intent killIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(killIntent);
                }
                isHand = true;
            }
        });

        // 是否提醒
        Switch isNeedNotifySwitch = findViewById(R.id.main_neednotify_switch);
        isNeedNotifySwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Const.isNeedNotify = isChecked;
            }
        });

        // 是否抢自己的红包
        Switch isNeedOpenSelfSwitch = findViewById(R.id.main_need_open_self_switch);
        isNeedOpenSelfSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Const.isNeedOpenSelf = isChecked;
            }
        });

        mVibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        mPlayer = MediaPlayer.create(this, R.raw.hongbao_arrived);
        mPlayer.setLooping(false);
    }

    public static void playSound() {

        try {

            if (!Const.isNeedNotify) {
                return;
            }

            // 调用震动
            if (mVibrator != null) {
                long[] pattern = {100, 400, 100, 400};   // 停止 开启 停止 开启
                // mVibrator.vibrate(pattern, 2);           //重复两次上面的pattern 如果只想震动一次，index设为-1
            }

            // 播放媒体音乐
            if (mPlayer != null) {
                mPlayer.start();
            }

        } catch (Exception e) {
            Log.e("playSound", "提示错误", e);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        isHand = false;
        isServiceSwitch.setChecked(isOpen());
        isHand = true;
    }

    // 判断辅助功能是否启动
    private boolean isOpen() {
        return Const.isOpen;
    }

}
