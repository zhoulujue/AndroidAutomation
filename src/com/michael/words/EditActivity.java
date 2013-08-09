package com.michael.words;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.michael.shell.Shell;

public class EditActivity extends Activity {
	private EditTextView mEditView;
	private Shell mLogcat;
	private Instrumentation mInstrumentation;
	private BufferedReader mReader;
	private boolean mPause;
	private int mChoice;
	private SharedPreferences mSharedPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);      
		getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN, WindowManager.LayoutParams. FLAG_FULLSCREEN);
		setContentView(R.layout.activity_edit);
		init();
		try {
			File rawFile = new File(getFilesDir() + "/" + "raw.config");
			if(!rawFile.exists()) {
				Utils.showToast(getApplicationContext(), R.string.toast_config_missing);
				finish();
			}

			mLogcat = new Shell("su");
			sleepSec(2);
			mLogcat.write("logcat CanvasDrawText:E *:S");

			mReader = new BufferedReader(new FileReader(getFilesDir() + "/" + "raw.config"));

			mInstrumentation = new Instrumentation();

			mPause = false;

			mChoice = mSharedPreferences.getInt("choice", 0);

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

		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
	}

	private void writeInfoHead() {
		PackageInfo pInfo = Utils.getCurrentImeInfo(getApplicationContext());
		if (pInfo != null) {
				new WriteFileThread(getApplicationContext(), 
						"IMEName:" + pInfo.packageName + "\n" +
								"IMEVersionName:" + pInfo.versionName + "\n" +
								"IMEVersionCode:" + pInfo.versionCode + "\n"
						).start();
		}
	}

	private Runnable uploadToFtp = new Runnable() {

		@Override
		public void run() {
			//如果结果文件上传不成功，标记为上传失败，供下次启动时使用
			if(!Utils.uploadFile(getApplicationContext(), "10.129.41.70", "imetest", "Sogou7882Imeqa", 
					"/WordCrawler", "result.txt")) {
				SharedPreferences.Editor editor = mSharedPreferences.edit();
				editor.putBoolean("LastRunSuccess", false);
				editor.commit();
			} else {
				//上传成功就把本地的结果文件删掉
				SharedPreferences.Editor editor = mSharedPreferences.edit();
				editor.putBoolean("LastRunSuccess", true);
				editor.commit();
				File localFile = new File(getFilesDir().getPath() + "/" + "result.txt");
				localFile.deleteOnExit();
			}
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
			//TODO: 在本地用final记下值，这样性能会比较快速，使用成员变量的话，cpu会上79%，很恐怖，切忌！
			final int configChoice = mChoice;
			int curCount = 0;
			String resultToWrite = "";
			mEditView.showInputMethod();
			try {
				mLogcat.read();

				String inputStr = null;
				while ((inputStr = mReader.readLine()) != null) {
					//暂停功能暂时采用死循环实现，死循环会把CPU带上去，这样不好
					//TODO: 不断访问成员变量，这样也会把CPU带上去
					while(mPause){};
					//运行以tab隔开的case，或者是以逗号隔开的case，遇到#则说明是要清空上下文
					if (inputStr.contains("\t")) {
						String pinyin = inputStr.substring(0, inputStr.indexOf("\t"));
						String hanzi = inputStr.substring(inputStr.indexOf("\t") + 1);
						SendString(pinyin);
						sleepMil(100);
						resultToWrite += readLogcat(pinyin, hanzi, inputStr);
						curCount++;							
					} else if (inputStr.contains(",") && inputStr.contains("\"")) {//如果是以逗号隔开
						//提取引号到第二个逗号之前的字符：a[0]="我,w,9999,21097634"; -> 我,w
						String caseStr = inputStr.substring(inputStr.indexOf("\"") + 1, 
								inputStr.indexOf(",", inputStr.indexOf(",") + 1));
						String pinyin = caseStr.substring(caseStr.indexOf(",") + 1);
						String hanzi = caseStr.substring(0, caseStr.indexOf(","));

						//如果遇到#号且是第三种模式，则说明需要清空了，但是注意不能先敲空格那样会把联想上屏
						if (pinyin.equals("#") && configChoice == R.id.config_radio_choice_first_screen) {
							SendKey(KeyEvent.KEYCODE_DEL);
							for (int i = 0; i < 2; i++)
								SendKey(KeyEvent.KEYCODE_SPACE);
							for (int i = 0; i < 2; i++)
								SendKey(KeyEvent.KEYCODE_DEL);
						} else {
							//这两个参数都是为了Rerun
							String resultForThisCase = "";
							int TrialCount = 0;
							while(resultForThisCase.equals("") && TrialCount < 10) {
								//如果需要rerun，说明没有读取到候选
								if (TrialCount > 0)
									for(int time =0; time < pinyin.length(); time++)
										SendKey(KeyEvent.KEYCODE_DEL);
								
								//发送没有意义的键盘事件，让输入法做好接受键盘事件的准备
								for (int j = 0; j < 3; j++)
									SendKey(KeyEvent.KEYCODE_CTRL_RIGHT);

								SendString(pinyin);

								//为了和下一次输入间隔开来
								for (int j = 0; j < (pinyin.length() < 4 ? 10:5); j++)
									SendKey(KeyEvent.KEYCODE_CTRL_RIGHT);

								sleepMil(100);
								resultForThisCase = readLogcat(pinyin, hanzi, inputStr);
								TrialCount++;
							}
							resultToWrite += resultForThisCase;
							curCount++;
						}

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

	/**
	 * pinyin + hanzi组成了一次case，函数读取logcat的hook返回，并将一次case的分析结果log返回;
	 * 根据ConfigActivity的不同配置，进行不同的上屏或者清除上下文关系操作。
	 * @param pinyin 本次case的拼音输入串
	 * @param hanzi 本次case要找的目标汉字
	 * @return 本次case分析结果log
	 */
	private String readLogcat(String pinyin, String hanzi, String inputStr) {
		//TODO: 在本地用final记下值，这样性能会比较快速，使用成员变量的话，cpu会上79%，很恐怖，切忌！
		final int configChoice = mChoice;
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
				String targetIndex = "-1";
				//写进文件的字符，表示一个拼音串的开始
				resultToWrite.append("wordstart\n");
				//TODO: 插入时间用于check时间，正式运行的时候要删除
				resultToWrite.append("time:" + Utils.getDateTime() + "\n");
				resultToWrite.append("count:" + inputStr + "\n");
				resultToWrite.append("pinyin:" + pinyin + "\t" + hanzi + "\n");
				//得到了候选，在候选词里面挑出要选择上屏的候选
				for (int i = startIndex; i < resultlist.length - 1; i+=2) {
					//如果读取的两行都是string，那么符合候选词的类型，可以初步判读是候选词，算是去噪音
					if ((resultlist[i].contains("type=String") && resultlist[i + 1].contains("type=String"))) {
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
						//TODO: 测试的时候打开，运行的时候关闭
						//Log.e("reading", "The Word is : " + index + ": " + word);
						//如果候选词和当前要选的词是一样的话，说明本次读到的是要上屏的候选词，那么通过键盘按下index这个数字
						if (word.equals(hanzi)) {
							targetIndex = index;
						}
					}
				}
				//根据configActivity里面的配置，分不同情况上屏，或者清屏
				if (configChoice == R.id.config_radio_complete_no_choice) {
					for (int i = 0; i < pinyin.length(); i++) {
						SendKey(KeyEvent.KEYCODE_DEL);
					}
				} else if (configChoice == R.id.config_radio_choice_first_candidate) {
					SendChoice("1");
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							mEditView.setText("");
						}
					});
					SendKey(KeyEvent.KEYCODE_SEMICOLON);
					SendKey(KeyEvent.KEYCODE_DEL);
					SendKey(KeyEvent.KEYCODE_SPACE);
					SendKey(KeyEvent.KEYCODE_DEL);
				} else if (configChoice == R.id.config_radio_choice_first_screen) {
					SendChoice(targetIndex.equals("-1") ? "1" : targetIndex);
				}
				//记录是否命中。如果是0，那么没有命中；否则即为命中。
				resultToWrite.append("target:" + targetIndex + "\n");
				//写进文件的字符，表示一个拼音串的结束。
				resultToWrite.append("wordend\n");
				return resultToWrite.toString();
			} else {
				return "";
			}
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		} catch (InterruptedException e) {
			e.printStackTrace();
			return "";
		}
	}

	private void SendKey(int Keycode) throws IOException{
		//Log.e("InputKeyEvent", "Keycode:" + Keycode);
		mInstrumentation.sendKeyDownUpSync(Keycode);
	}

	private void SendChoice(String Keycode) throws IOException{
		int key = Integer.valueOf(Keycode) + 7;
		//Log.e("Send Choice", "Keycode:" + key);
		mInstrumentation.sendKeyDownUpSync(key);
	}

	private void SendString(String text) throws IOException{
		//Log.e("InputKeyEvent", "text:" + text);
		mInstrumentation.sendStringSync(text);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.edit, menu);
		return true;
	}

	private static void sleepSec(int second) {
		try {
			Thread.sleep(second * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static void sleepMil(int millisecond) {
		try {
			Thread.sleep(millisecond);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
