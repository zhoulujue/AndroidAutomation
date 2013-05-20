package com.michael.words;

import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class EditActivity extends Activity {
	private EditTextView mEditView;
	private Shell mLogcat;
	private Shell mInputShell;
	
	private static final ArrayList<String> input = new ArrayList<String>();
	static {
		input.add("yigerenkang");//1
		input.add("yigerenzou");//2
		input.add("yiqiesuiyuan");//3
		input.add("yihaizuche");//4
		input.add("yishenbeipan");//5
		input.add("yiweiliuying");//6
		input.add("yifanfengshun");//7
		input.add("yinianzuoyou");//8
		input.add("yiyiguxing");//9
		input.add("yizhihongfeng");//10
		input.add("yishishijian");//11
		input.add("yibeikafei");//12
		input.add("yipaihuyan");//13
		input.add("yishengyishi");//14
		input.add("yishengwuhui");//15
		input.add("yizhiyilai");//16
		input.add("yizhizaizhang");//17
		input.add("yizhandaodi");//18
		input.add("yimiyangguang");//19
		input.add("yizhiduiwai");//20
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit);
		init();
		try {
			mLogcat = new Shell();
			sleep(2);
			mLogcat.write("logcat CanvasDrawText:E *:S");

			mInputShell = new Shell();
			sleep(2);
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}


	private void init() {
		mEditView = (EditTextView) findViewById(R.id.editText1);
		mEditView.requestFocus();

		Button startButton = (Button) findViewById(R.id.button_start);
		startButton.setOnClickListener(onButtonStartListener);
	}

	private View.OnClickListener onButtonStartListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			mEditView.showInputMethod();

			try {
				mLogcat.read();

				for (String inputStr : input) {
					synchronized (inputStr) {
						SendString(mInputShell, inputStr);

						SendKey(mInputShell, KeyEvent.KEYCODE_CTRL_RIGHT);
						SendKey(mInputShell, KeyEvent.KEYCODE_CTRL_LEFT);
						SendKey(mInputShell, KeyEvent.KEYCODE_SPACE);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} 

		}
	};

	@Override
	protected void onStop() {
		try {
			mInputShell.close();
			mLogcat.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		super.onStop();
	}
	
	private void readLogcat() {
		String result;
		StringBuilder writer = new StringBuilder();
		try {
			result = mLogcat.read();

			String[] resultlist = result.split("\n");
			int startIndex = -1;
			for (int i = resultlist.length - 1; i >=0; i--) {
				if (resultlist[i].contains("text:1#")) {
					startIndex = i - 1;
					break;
				}
			}
			if (startIndex != -1) {

				for (int i = startIndex; i < resultlist.length; i++) {
					writer.append(resultlist[i]);
					Log.e("reading", "@#@#@#@#@#@# One Line : " + resultlist[i]);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void SendKey(Shell shell, int Keycode) throws IOException{
		Log.e("InputKeyEvent", "Keycode:" + Keycode);
		shell.write("input keyevent " + Keycode);
	}

	private void SendString(Shell shell, String text) throws IOException{
		Log.e("InputKeyEvent", "text:" + text);
		String cmdString = "input text " + "\"" + text + "\"";
		shell.write(cmdString);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.edit, menu);
		return true;
	}

	private static void sleep(int second) {
		try {
			Thread.sleep(second * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_CTRL_LEFT) {
			Log.e("reading", "#############"  + "reading" + "#############");
			readLogcat();
		}
		return super.onKeyDown(keyCode, event);
	}

}
