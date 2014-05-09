package com.michael.words;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioGroup;

import com.michael.words.keys.Keybord;
import com.michael.words.utils.Utils;

public class ConfigActivity extends Activity {


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
			Utils.showInputMethodPicker(getApplicationContext());
		}
	};

	private View.OnClickListener mOnNextStepListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			RadioGroup choiceRadio = (RadioGroup) findViewById(R.id.config_radioGroup_choice);
			int choice = choiceRadio.getCheckedRadioButtonId();
			
			RadioGroup keybordRadio = (RadioGroup) findViewById(R.id.config_radioGroup_keybord);
			int keybord = keybordRadio.getCheckedRadioButtonId();
			
			SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
			editor.putInt("choice", choice);
			
			switch (keybord) {
			case R.id.config_radio_keybord_nine:
				editor.putInt("keybord", Keybord.KEYBORD_MODEL_NINE);
				break;
			case R.id.config_radio_keybord_qwerty:
				editor.putInt("keybord", Keybord.KEYBORD_MODEL_QWERTY);
				break;
			case R.id.config_radio_keybord_hand_writing:
				editor.putInt("keybord", Keybord.KEYBORD_MODEL_HAND_WRITING);
				break;
			default:
				break;
			}
			
			editor.commit();
			String ImeName = Utils.getCurrentImeInfo(getApplicationContext()).packageName;
			if (keybord == R.id.config_radio_keybord_hand_writing) {
				startActivity(new Intent(ConfigActivity.this, HandWritingActivity.class));
				finish();
				return;
			}
			if (ImeName.contains("sogou")){
				startActivity(new Intent(ConfigActivity.this, SogouEditActivity.class));
				finish();
			} else {
				startActivity(new Intent(ConfigActivity.this, EditActivity.class));
				finish();
			}

		}
	};
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.about:
			Utils.showDialog(ConfigActivity.this, 
					R.string.app_version, 
					R.string.dialog_about_title, 
					R.string.dialog_confirm, 
					R.string.dialog_cancel, 
					true,
					null);
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
}
