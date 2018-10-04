package com.jc.yangj;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;

import im.fir.sdk.FIR;
import im.fir.sdk.VersionCheckCallback;

@SuppressWarnings("deprecation")
public class MainActivity extends Activity {

    private KeyguardLock keyguardLock;
    private final String LOCK_TAG = "Charge";

    private Switch isServiceSwitch = null;

    private static boolean isHand = true;

    private static MediaPlayer mPlayer = null;
    private static Vibrator mVibrator = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        keyguardLock = keyguardManager.newKeyguardLock(LOCK_TAG);
        keyguardLock.disableKeyguard();

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

        // 无人模式
        Switch isHaveNoPersonSwitch = findViewById(R.id.main_havenoperson_switch);
        isHaveNoPersonSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Const.isHaveNoPerson = isChecked;
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

        //更新按钮
        findViewById(R.id.main_update_btn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("http://fir.im/YangJ"));
                startActivity(intent);
            }
        });

        mVibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        mPlayer = MediaPlayer.create(this, R.raw.hongbao_arrived);
        mPlayer.setLooping(false);

        findViewById(R.id.main_tips_btn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                playSound();
            }
        });
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

    private static Thread writePCMThread = null;
    private static File audioFile = null;
    private static FileInputStream fileInputStream = null;
    private static byte buffer[] = new byte[16 * 10000];

    private static void initAudioTrack() {
        int minBufferSize = AudioTrack.getMinBufferSize(0xac44, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
        System.out.println("minBufferSize = " + minBufferSize);
        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 0xac44,
                AudioFormat.CHANNEL_CONFIGURATION_STEREO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize * 2, AudioTrack.MODE_STREAM);
        audioTrack.setStereoVolume(1.0f, 1.0f);// 设置当前音量大小
        System.out.println("initAudioTrack over");
        audioTrack.play();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isHand = false;
        isServiceSwitch.setChecked(isOpen());
        isHand = true;
        findViewById(R.id.main_update_btn).setEnabled(false);
        FIR.checkForUpdateInFIR("33f15f26bd0a1bf5693c34ef5c70cb44", new VersionCheckCallback() {
            @Override
            public void onSuccess(String versionJson) {
                Log.i("fir", "check from fir.im success! " + "\n" + versionJson);

                if (true) {
                    //AlertDialog.Builder builder = new Builder(MainActivity.this);
                    try {
                        //远程版本号
                        int remoteVersionCode = 3;
                        JSONObject object = new JSONObject(versionJson);
                        remoteVersionCode = Integer.valueOf(object.get("version").toString());

                        // 本地版本号
                        int versionCode = MainActivity.this.getPackageManager().getPackageInfo(MainActivity.this.getPackageName(), 0).versionCode;
                        findViewById(R.id.main_update_btn).setEnabled(versionCode < remoteVersionCode);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }

        });
    }

    // 判断辅助功能是否启动
    private boolean isOpen() {
        return Const.isOpen;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (keyguardLock != null) {
            keyguardLock.reenableKeyguard();
        }
    }

}
