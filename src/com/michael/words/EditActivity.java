package com.michael.words;

import it.sauronsoftware.ftp4j.FTPAbortedException;
import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferException;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
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
	private StringBuilder mResult;

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

			mResult = new StringBuilder();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void onBackPressed() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				if (mResult.toString().isEmpty() || mResult.toString().equals("")) {
					return;
				}
				
				try {
					FileOutputStream os = openFileOutput("result.txt", Context.MODE_PRIVATE);
					os.write(mResult.toString().getBytes("UTF-8"));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				uploadFile("10.129.41.70", "imetest", "Sogou7882Imeqa", "/WordCrawler", "result.txt");
			}
		}).start();
		super.onBackPressed();
	}

	private void init() {
		mEditView = (EditTextView) findViewById(R.id.editText1);
		mEditView.setOnKeyListener(mOnLeftCTRListener);
		mEditView.requestFocus();

		Button startButton = (Button) findViewById(R.id.button_start);
		startButton.setOnClickListener(mOnButtonStartListener);
	}

	private View.OnKeyListener mOnLeftCTRListener = new View.OnKeyListener() {
		
		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			if(keyCode == KeyEvent.KEYCODE_CTRL_LEFT) {
				Log.e("reading", "#############"  + "reading" + "#############");
				readLogcat();
				return true;
			}
			return false;
		}
	};
	
	private View.OnClickListener mOnButtonStartListener = new View.OnClickListener() {

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
				mResult.append("\nwordstart\n");
				for (int i = startIndex; i < resultlist.length; i++) {
					if (resultlist[i].contains("type=String")) {
						String raw = resultlist[i].substring(
								resultlist[i].indexOf("text:") + "text:".length(), 
								resultlist[i].indexOf("#"));
						mResult.append(raw);
						Log.e("reading", "@#@#@#@#@#@# One Line : " + resultlist[i]);
					}
				}
				mResult.append("\nwordend\n");
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

	private boolean uploadFile(String host, String username, String passwd, String remoteDir, String filename) {
		File file = new File(getFilesDir().getPath() + "/" + filename);
		if (!file.exists())
			return false;

		FTPClient client = new FTPClient();
		try {
			client.connect(host);
			client.login(username, passwd);
			client.changeDirectory(remoteDir);
			client.upload(file);
			client.rename(filename, "result-" + Utils.getDateTime() + ".txt");
			client.disconnect(false);

			return true;
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (FTPIllegalReplyException e) {
			e.printStackTrace();
			return false;
		} catch (FTPException e) {
			e.printStackTrace();
			return false;
		} catch (FTPDataTransferException e) {
			e.printStackTrace();
			return false;
		} catch (FTPAbortedException e) {
			e.printStackTrace();
			return false;
		} 
	}

}
