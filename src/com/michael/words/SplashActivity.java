package com.michael.words;

import java.io.File;
import java.io.IOException;

import com.michael.words.utils.Utils;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Window;
import android.view.WindowManager;


public class SplashActivity extends Activity {

	private static Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
		};
	};

	private SharedPreferences mSharedPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);      
		getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN, WindowManager.LayoutParams. FLAG_FULLSCREEN);

		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		startMain();
	}

	private void startMain() {
		mHandler.postDelayed(new Runnable() {
			public void run() {
/*				download.start();
				try {
					download.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}*/
				
				//TODO: 处理结果文件需要更改，因为结果文件的名字要变
				File localFile = new File(getFilesDir().getPath() + "/" + "result.txt");
				//如果本地没有结果文件，说明已经上传成功
				if (!localFile.exists()) {
					SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
					editor.putBoolean("LastRunSuccess", true);
					editor.commit();
					try {
						localFile.createNewFile();
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
					editor.putBoolean("LastRunSuccess", false);
					editor.commit();
				}
				boolean lastRunSuccess = mSharedPreferences.getBoolean("LastRunSuccess", true);
				if(!lastRunSuccess)
				{
					startActivity(new Intent(SplashActivity.this, HandleLastRunActivity.class));
				}
				else
				{
					startActivity(new Intent(SplashActivity.this, ConfigActivity.class));
				}
				overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
				finish();
				overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
			}

		}, 2000);
	}

	private Thread download = new Thread(new Runnable() {

		@Override
		public void run() {
			Utils.downloadFile(getApplicationContext(), "10.129.41.70", "imetest", "Sogou7882Imeqa", 
					"/WordCrawler", "raw.config");
		}
	});

}
