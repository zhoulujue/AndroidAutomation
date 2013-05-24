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

import com.michael.shell.Shell;

import android.R.bool;
import android.app.Activity;
import android.content.Context;
import android.graphics.Path;
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
	private Shell mSendChoice;
	private StringBuilder mResult;
	
	private ArrayList<String> input;
	private int mCurIndex;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit);
		init();
		try {
			Thread download = new Thread(new Runnable() {
				
				@Override
				public void run() {
					downloadFile("10.129.41.70", "imetest", "Sogou7882Imeqa", "/WordCrawler", "raw.config");
				}
			});
			download.start();
			download.join();
			File rawFile = new File(getFilesDir() + "/" + "raw.config");
			
			if(!rawFile.exists()) {
				Utils.showToast(getApplicationContext(), "没有获取到配置文件，退出！");
				finish();
			}
			
			input = Utils.ReadFromFile.readFileByLines(getFilesDir() + "/" + "raw.config");
			
			mLogcat = new Shell();
			sleep(2);
			mLogcat.write("logcat CanvasDrawText:E *:S");

			mInputShell = new Shell();
			sleep(2);

			mSendChoice = new Shell();
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
				mCurIndex ++;
				return true;
			}
			return false;
		}
		
	};
	
	private View.OnClickListener mOnButtonStartListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			mEditView.showInputMethod();
			mCurIndex = 0;
			try {
				mLogcat.read();

				for (String inputStr : input) {
					synchronized (inputStr) {
						String pinyin = inputStr.substring(0, inputStr.indexOf(" "));
						SendString(mInputShell, pinyin);

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
				String lastWords = "";
				String choice = input.get(mCurIndex);
				String choiceWords = choice.substring(choice.indexOf(" ") + 1);
				//写进文件的字符，表示一个拼音串的开始
				mResult.append("\nwordstart\n");
				mResult.append(input.get(mCurIndex) + "\n");
				//得到了候选，在候选词里面挑出要选择上屏的候选
				for (int i = startIndex; i < resultlist.length; i++) {
					if (resultlist[i].contains("type=String")) {
						String raw = resultlist[i].substring(
								resultlist[i].indexOf("text:") + "text:".length(), 
								resultlist[i].indexOf("#"));
						mResult.append(raw);
						Log.e("reading", "@#@#@#@#@#@# One Line : " + resultlist[i]);
						//如果上一个候选词和当前要选的词是一样的话，说明本次读到的是候选词的序号，那么通过键盘按下这个数字
						if (lastWords == choiceWords) {
							SendKey(mSendChoice, raw);
						}
						lastWords = raw;
					}
				}
				//写进文件的字符，表示一个拼音串的结束
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

	private void SendKey(Shell shell, String Keycode) throws IOException{
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

	/**
	 * 把FTP上的文件下载到本地，本地的文件名字和服务器上文件的名字是一样的。
	 * @param host FTP服务器的IP地址，或者FTP服务器的域名
	 * @param username 登陆FTP服务器需要的用户名
	 * @param passwd 登陆FTP服务器需要的密码
	 * @param remoteDir 文件在FTP服务器上的路径，不含文件名
	 * @param filename	文件的名字
	 * @return true:下载成功；false:下载失败
	 */
	private boolean downloadFile(String host, String username, String passwd, String remoteDir, String filename) {
		File localFile = new File(getFilesDir().getPath() + "/" + filename);
		
		FTPClient client = new FTPClient();
		try {
			client.connect(host);
			client.login(username, passwd);
			client.download(remoteDir + "/" + filename, localFile);
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
