package com.jc.yangj;

import im.fir.sdk.FIR;
import im.fir.sdk.VersionCheckCallback;

import java.io.File;
import java.io.FileInputStream;

import org.json.JSONObject;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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

	private KeyguardLock keyguardLock;
	private final String LOCK_TAG = "Charge";

	private Switch isServiceSwitch = null;
	private Switch isSelfSwitch = null;
	private Switch isPageSwitch = null;
	private Switch isHaveNoPersonSwitch = null;
	private static Switch isSoundSwitch = null;
	private static Switch isVibratorSwitch = null;

	private static MainActivity mainActivity;
	private static boolean isHand = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mainActivity = this;

		setContentView(R.layout.activity_main);

		KeyguardManager keyguardManager = (KeyguardManager)getSystemService(KEYGUARD_SERVICE);
		keyguardLock = keyguardManager.newKeyguardLock(LOCK_TAG);
		keyguardLock.disableKeyguard();

		//启动抢红包服务
		isServiceSwitch = (Switch)findViewById(R.id.main_isserviceopen_switch);
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

		// 抢自己红包
		isSelfSwitch = (Switch)findViewById(R.id.main_isselfopen_switch);
		isSelfSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Const.isNeedOpenSelf = isChecked;
			}
		});

		// 在页面抢红包
		isPageSwitch = (Switch)findViewById(R.id.main_ispageopen_switch);
		isPageSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Const.isNeedInPage = isChecked;
			}
		});

		isSoundSwitch = (Switch)findViewById(R.id.main_issound_switch);
		isVibratorSwitch = (Switch)findViewById(R.id.main_isvibratorh_switch);

		// 无人模式
		isHaveNoPersonSwitch = (Switch)findViewById(R.id.main_havenoperson_switch);
		isHaveNoPersonSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Const.isHaveNoPerson = isChecked;
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

		//playSound();
	}

	public static void playSound() {

		try {

			if (mainActivity==null) {
				return;
			}

			if (!isSoundSwitch.isChecked() && !isVibratorSwitch.isChecked()) {
				return;
			}

			Context context = mainActivity;

			if (isVibratorSwitch.isChecked()) {
				// 调用震动
				Vibrator vibrator = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
				long [] pattern = {100, 400, 100, 400};   // 停止 开启 停止 开启
				vibrator.vibrate(pattern, -1);           //重复两次上面的pattern 如果只想震动一次，index设为-1
			}

			if (isSoundSwitch.isChecked()) {
				// 调用提示音
//		        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//		        Ringtone r = RingtoneManager.getRingtone(context.getApplicationContext(), notification);
//		        r.play();

				initAudioTrack();
				//audioFile = new File(Environment.getExternalStorageDirectory(),"test.wav");
				audioFile = new File("file:///android_asset/","hongbao_arrived.wav");
				System.out.println(audioFile.length());
				try {
					fileInputStream = new FileInputStream(audioFile);
					fileInputStream.skip(0x2c);
				} catch (Exception e) {
				}

				writePCMThread = new Thread(new Runnable(){
					public void run() {
						try {
							while(fileInputStream.read(buffer)>=0)
							{
								System.out.println("write pcm data");
								audioTrack.write(buffer, 0, buffer.length);
							}
						}
						catch (Exception e) {
							e.printStackTrace();
						}

					}
				});
			}

		} catch (Exception e) {
			Log.e("playSound", "提示错误", e);
		}

	}

	private static AudioTrack audioTrack = null;
	private static Thread writePCMThread = null;
	private static File audioFile = null;
	private static FileInputStream fileInputStream = null;
	private static byte buffer[] = new byte[16*10000];

	private static void initAudioTrack()
	{
		int minBufferSize = AudioTrack.getMinBufferSize(0xac44, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
		System.out.println("minBufferSize = "+minBufferSize);
		audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 0xac44,
				AudioFormat.CHANNEL_CONFIGURATION_STEREO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize*2,AudioTrack.MODE_STREAM);
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
		FIR.checkForUpdateInFIR("33f15f26bd0a1bf5693c34ef5c70cb44" , new VersionCheckCallback() {
			@Override
			public void onSuccess(String versionJson) {
				Log.i("fir","check from fir.im success! " + "\n" + versionJson);

				if (true) {
					//AlertDialog.Builder builder = new Builder(MainActivity.this);
					try {
						//远程版本号
						int remoteVersionCode = 3;
						JSONObject object = new JSONObject(versionJson);
						remoteVersionCode = Integer.valueOf(object.get("version").toString());

						// 本地版本号
						int versionCode = MainActivity.this.getPackageManager().getPackageInfo(MainActivity.this.getPackageName(), 0).versionCode;
						findViewById(R.id.main_update_btn).setEnabled(versionCode<remoteVersionCode);

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
		if (keyguardLock!=null) {
			keyguardLock.reenableKeyguard();
		}
	}

}