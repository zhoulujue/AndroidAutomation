package com.michael.words;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;

public class ConfigActivity extends BaseActivity {
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);      
		getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN, WindowManager.LayoutParams. FLAG_FULLSCREEN);
		setContentView(R.layout.activity_config);
		init();
	}

	private void init() {
		Button connectBluetoothButton = (Button) findViewById(R.id.config_button_connect);
		connectBluetoothButton.setOnClickListener(mOnConnectBluetoothListener);
		
		Button switchImeButton = (Button) findViewById(R.id.config_button_switch_ime);
		switchImeButton.setOnClickListener(mOnSwitchImeListener);
		
		Button nextStepButton = (Button) findViewById(R.id.config_button_next_step);
		nextStepButton.setOnClickListener(mOnNextStepListener);
	}
	
	private View.OnClickListener mOnConnectBluetoothListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			 Intent intent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
			 startActivity(intent);
		}
	};
	
	private View.OnClickListener mOnSwitchImeListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS);
			startActivity(intent);
		}
	};
	
	private View.OnClickListener mOnNextStepListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			CheckBox checkBox = (CheckBox) findViewById(R.id.config_checkBox);
			boolean checked = !checkBox.isChecked();
			SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
			editor.putBoolean("choice", checked);
			editor.commit();
			startActivity(new Intent(ConfigActivity.this, EditActivity.class));
			finish();
		}
	};
}
