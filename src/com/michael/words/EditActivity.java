package com.michael.words;

import it.sauronsoftware.ftp4j.FTPAbortedException;
import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferException;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import com.michael.shell.Shell;

public class EditActivity extends Activity {
	private EditTextView mEditView;
	private Shell mLogcat;
	private Instrumentation mInstrumentation;
	private BufferedReader mReader;
	private boolean mPause;

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

			mLogcat = new Shell("su");
			sleep(2);
			mLogcat.write("logcat CanvasDrawText:E *:S");

			mReader = new BufferedReader(new FileReader(getFilesDir() + "/" + "raw.config"));

			mInstrumentation = new Instrumentation();

			mPause = false;

			writeInfoHead();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void onBackPressed() {
		new Thread(uploadToFtp).start();
		super.onBackPressed();
	}

	private void init() {
		mEditView = (EditTextView) findViewById(R.id.editText1);
		//mEditView.setOnKeyListener(mOnLeftCTRListener);
		mEditView.requestFocus();

		Button startButton = (Button) findViewById(R.id.button_start);
		startButton.setOnClickListener(mOnButtonStartListener);

		Button pauseButton = (Button) findViewById(R.id.button_pause);
		pauseButton.setOnClickListener(mOnButtonPauseListener);

		Button deleteButton = (Button) findViewById(R.id.button_delete);
		deleteButton.setOnClickListener(mOnButtonDeleteListener);
	}

	private void writeInfoHead() {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		List<InputMethodInfo> mInputMethodProperties = imm.getEnabledInputMethodList();

		final int N = mInputMethodProperties.size();

		for (int i = 0; i < N; i++) {
			InputMethodInfo imi = mInputMethodProperties.get(i);
			if (imi.getId().equals(Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD))) {
				String packageName = imi.getPackageName();
				PackageInfo pInfo;
				String versionName = "Not kown";
				int versionCode = 0;
				try {
					pInfo = getPackageManager().getPackageInfo(packageName, 0);
					versionName = pInfo.versionName;
					versionCode = pInfo.versionCode;
				} catch (NameNotFoundException e) {
					e.printStackTrace();
				}

				new WriteFileThread(getApplicationContext(), 
						"IMEName:" + packageName + "\n" +
								"IMEVersionName:" + versionName + "\n" +
								"IMEVersionCode:" + versionCode + "\n"
						).start();
				break;
			}
		}
	}

	private Runnable uploadToFtp = new Runnable() {

		@Override
		public void run() {
			uploadFile("10.129.41.70", "imetest", "Sogou7882Imeqa", "/WordCrawler", "result.txt");
			File localFile = new File(getFilesDir().getPath() + "/" + "result.txt");
			localFile.deleteOnExit();
		}
	};

	private View.OnClickListener mOnButtonStartListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			new Thread(mSendRunnable).start();
		}
	};

	private View.OnClickListener mOnButtonPauseListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			if(mPause){
				mPause = false;
				((Button) findViewById(R.id.button_pause)).setText(R.string.pause);
			} else {
				mPause = true;
				((Button) findViewById(R.id.button_pause)).setText(R.string.conti);
			}

		}
	};

	private View.OnClickListener mOnButtonDeleteListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			File localFile = new File(getFilesDir().getPath() + "/" + "result.txt");
			localFile.delete();
			writeInfoHead();
		}
	};

	private Runnable mSendRunnable = new Runnable() {

		@Override
		public void run() {
			int curCount = 0;
			String resultToWrite = "";
			mEditView.showInputMethod();
			try {
				mLogcat.read();

				String inputStr = null;
				while ((inputStr = mReader.readLine()) != null) {
					//暂停功能暂时采用死循环实现，死循环会把CPU带上去，这样不好
					while(mPause){};
					//如果是以tab键隔开的case
					if (inputStr.contains("\t")) {
						String pinyin = inputStr.substring(0, inputStr.indexOf("\t"));
						SendString(pinyin);
						for (int i = 0; i < (pinyin.length()==1?20:5); i++)
							SendKey(KeyEvent.KEYCODE_CTRL_RIGHT);
						resultToWrite += readLogcat(inputStr);
						curCount++;							
					} else if (inputStr.contains(",") && inputStr.contains("\"")) {//如果是以逗号隔开
						inputStr = inputStr.substring(inputStr.indexOf("\"") + 1, 
								inputStr.indexOf(",", inputStr.indexOf(",") + 1));
						String pinyin = inputStr.substring(inputStr.indexOf(",") + 1);
						inputStr = inputStr.substring(inputStr.indexOf(",") + 1) + 
								"\t" + inputStr.substring(0, inputStr.indexOf(","));
						SendString(pinyin);
						for (int i = 0; i < (pinyin.length()==1?20:5); i++)
							SendKey(KeyEvent.KEYCODE_CTRL_RIGHT);
						resultToWrite += readLogcat(inputStr);
						curCount++;	
					}
					if (curCount % 20 == 0) {
						final int count = curCount;
						SendKey(KeyEvent.KEYCODE_CTRL_RIGHT);
						SendKey(KeyEvent.KEYCODE_CTRL_RIGHT);
						SendKey(KeyEvent.KEYCODE_CTRL_RIGHT);
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								mEditView.setText("");
								((TextView) findViewById(R.id.textView_cur_count)).setText(String.valueOf(count));
							}
						});
						new WriteFileThread(getApplicationContext(), resultToWrite.toString()).start();
						resultToWrite = "";
						SendKey(KeyEvent.KEYCODE_CTRL_RIGHT);
						SendKey(KeyEvent.KEYCODE_CTRL_RIGHT);
						SendKey(KeyEvent.KEYCODE_CTRL_RIGHT);
					}
				}
				//**当所有case运行完毕的时候，还有一部分没有记录完，此时应该做好收尾工作
				final int count = curCount;
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mEditView.setText("");
						((TextView) findViewById(R.id.textView_cur_count)).setText(String.valueOf(count));
					}
				});
				new WriteFileThread(getApplicationContext(), resultToWrite.toString()).start();
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
			mLogcat.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		super.onStop();
	}

	private String readLogcat(String currentStr) {
		String RawResult;
		try {
			RawResult = mLogcat.read();
			String[] resultlist = RawResult.split("\n");
			int startIndex = -1;
			for (int i = resultlist.length - 1; i >=0; i--) {
				if (resultlist[i].contains("text:1#")) {
					startIndex = i - 1;
					break;
				}
			}
			if (startIndex != -1) {
				StringBuilder resultToWrite = new StringBuilder();
				String targetIndex = "0";
				String choice = currentStr;
				String choiceWords = choice.substring(choice.indexOf("\t") + 1);
				//写进文件的字符，表示一个拼音串的开始
				resultToWrite.append("wordstart\n");
				resultToWrite.append("pinyin:" + choice + "\n");
				//得到了候选，在候选词里面挑出要选择上屏的候选
				for (int i = startIndex; i < resultlist.length - 1; i+=2) {
					//如果读取的两行都是string，那么符合候选词的类型，可以初步判读是候选词，算是去噪音
					if (resultlist[i].contains("type=String") && resultlist[i + 1].contains("type=String")) {
						//计算候选词，用于记录和对比
						String word = resultlist[i].substring(
								resultlist[i].indexOf("text:") + "text:".length(), 
								resultlist[i].indexOf("#"));
						//计算标号数字，用于按下数字键来选择上屏
						String index = resultlist[i + 1].substring(
								resultlist[i + 1].indexOf("text:") + "text:".length(), 
								resultlist[i + 1].indexOf("#"));

						resultToWrite.append(index + ":");
						resultToWrite.append(word + "\n");
						Log.e("reading", "The Word is : " + index + ": " + word);
						//如果候选词和当前要选的词是一样的话，说明本次读到的是要上屏的候选词，那么通过键盘按下index这个数字
						if (word.equals(choiceWords)) {
							targetIndex = index;
						}
					}
				}
				//选择数字键，进行上屏。
				SendChoice(targetIndex.equals("0") ? "1" : targetIndex);
				//记录是否命中。如果是0，那么没有命中；否则即为命中。
				resultToWrite.append("target:" + targetIndex + "\n");
				//写进文件的字符，表示一个拼音串的结束。
				resultToWrite.append("wordend\n");
				return resultToWrite.toString();
				//new WriteFileThread(getApplicationContext(), resultToWrite.toString()).start();
			} else {
				return null;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}

	private void SendKey(int Keycode) throws IOException{
		Log.e("InputKeyEvent", "Keycode:" + Keycode);
		mInstrumentation.sendKeyDownUpSync(Keycode);
	}

	private void SendChoice(String Keycode) throws IOException{
		int key = Integer.valueOf(Keycode) + 7;
		Log.e("Send Choice", "Keycode:" + key);
		mInstrumentation.sendKeyDownUpSync(key);
	}

	private void SendString(String text) throws IOException{
		Log.e("InputKeyEvent", "text:" + text);
		mInstrumentation.sendStringSync(text);
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
