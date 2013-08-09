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
			//�������ļ��ϴ����ɹ������Ϊ�ϴ�ʧ�ܣ����´�����ʱʹ��
			if(!Utils.uploadFile(getApplicationContext(), "10.129.41.70", "imetest", "Sogou7882Imeqa", 
					"/WordCrawler", "result.txt")) {
				SharedPreferences.Editor editor = mSharedPreferences.edit();
				editor.putBoolean("LastRunSuccess", false);
				editor.commit();
			} else {
				//�ϴ��ɹ��Ͱѱ��صĽ���ļ�ɾ��
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
			//TODO: �ڱ�����final����ֵ���������ܻ�ȽϿ��٣�ʹ�ó�Ա�����Ļ���cpu����79%���ֲܿ����мɣ�
			final int configChoice = mChoice;
			int curCount = 0;
			String resultToWrite = "";
			mEditView.showInputMethod();
			try {
				mLogcat.read();

				String inputStr = null;
				while ((inputStr = mReader.readLine()) != null) {
					//��ͣ������ʱ������ѭ��ʵ�֣���ѭ�����CPU����ȥ����������
					//TODO: ���Ϸ��ʳ�Ա����������Ҳ���CPU����ȥ
					while(mPause){};
					//������tab������case���������Զ��Ÿ�����case������#��˵����Ҫ���������
					if (inputStr.contains("\t")) {
						String pinyin = inputStr.substring(0, inputStr.indexOf("\t"));
						String hanzi = inputStr.substring(inputStr.indexOf("\t") + 1);
						SendString(pinyin);
						sleepMil(100);
						resultToWrite += readLogcat(pinyin, hanzi, inputStr);
						curCount++;							
					} else if (inputStr.contains(",") && inputStr.contains("\"")) {//������Զ��Ÿ���
						//��ȡ���ŵ��ڶ�������֮ǰ���ַ���a[0]="��,w,9999,21097634"; -> ��,w
						String caseStr = inputStr.substring(inputStr.indexOf("\"") + 1, 
								inputStr.indexOf(",", inputStr.indexOf(",") + 1));
						String pinyin = caseStr.substring(caseStr.indexOf(",") + 1);
						String hanzi = caseStr.substring(0, caseStr.indexOf(","));

						//�������#�����ǵ�����ģʽ����˵����Ҫ����ˣ�����ע�ⲻ�����ÿո����������������
						if (pinyin.equals("#") && configChoice == R.id.config_radio_choice_first_screen) {
							SendKey(KeyEvent.KEYCODE_DEL);
							for (int i = 0; i < 2; i++)
								SendKey(KeyEvent.KEYCODE_SPACE);
							for (int i = 0; i < 2; i++)
								SendKey(KeyEvent.KEYCODE_DEL);
						} else {
							//��������������Ϊ��Rerun
							String resultForThisCase = "";
							int TrialCount = 0;
							while(resultForThisCase.equals("") && TrialCount < 10) {
								//�����Ҫrerun��˵��û�ж�ȡ����ѡ
								if (TrialCount > 0)
									for(int time =0; time < pinyin.length(); time++)
										SendKey(KeyEvent.KEYCODE_DEL);
								
								//����û������ļ����¼��������뷨���ý��ܼ����¼���׼��
								for (int j = 0; j < 3; j++)
									SendKey(KeyEvent.KEYCODE_CTRL_RIGHT);

								SendString(pinyin);

								//Ϊ�˺���һ������������
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
				//**������case������ϵ�ʱ�򣬻���һ����û�м�¼�꣬��ʱӦ��������β����
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
	 * pinyin + hanzi�����һ��case��������ȡlogcat��hook���أ�����һ��case�ķ������log����;
	 * ����ConfigActivity�Ĳ�ͬ���ã����в�ͬ������������������Ĺ�ϵ������
	 * @param pinyin ����case��ƴ�����봮
	 * @param hanzi ����caseҪ�ҵ�Ŀ�꺺��
	 * @return ����case�������log
	 */
	private String readLogcat(String pinyin, String hanzi, String inputStr) {
		//TODO: �ڱ�����final����ֵ���������ܻ�ȽϿ��٣�ʹ�ó�Ա�����Ļ���cpu����79%���ֲܿ����мɣ�
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
				//д���ļ����ַ�����ʾһ��ƴ�����Ŀ�ʼ
				resultToWrite.append("wordstart\n");
				//TODO: ����ʱ������checkʱ�䣬��ʽ���е�ʱ��Ҫɾ��
				resultToWrite.append("time:" + Utils.getDateTime() + "\n");
				resultToWrite.append("count:" + inputStr + "\n");
				resultToWrite.append("pinyin:" + pinyin + "\t" + hanzi + "\n");
				//�õ��˺�ѡ���ں�ѡ����������Ҫѡ�������ĺ�ѡ
				for (int i = startIndex; i < resultlist.length - 1; i+=2) {
					//�����ȡ�����ж���string����ô���Ϻ�ѡ�ʵ����ͣ����Գ����ж��Ǻ�ѡ�ʣ�����ȥ����
					if ((resultlist[i].contains("type=String") && resultlist[i + 1].contains("type=String"))) {
						//�����ѡ�ʣ����ڼ�¼�ͶԱ�
						String word = resultlist[i].substring(
								resultlist[i].indexOf("text:") + "text:".length(), 
								resultlist[i].indexOf("#"));
						//���������֣����ڰ������ּ���ѡ������
						String index = resultlist[i + 1].substring(
								resultlist[i + 1].indexOf("text:") + "text:".length(), 
								resultlist[i + 1].indexOf("#"));

						resultToWrite.append(index + ":");
						resultToWrite.append(word + "\n");
						//TODO: ���Ե�ʱ��򿪣����е�ʱ��ر�
						//Log.e("reading", "The Word is : " + index + ": " + word);
						//�����ѡ�ʺ͵�ǰҪѡ�Ĵ���һ���Ļ���˵�����ζ�������Ҫ�����ĺ�ѡ�ʣ���ôͨ�����̰���index�������
						if (word.equals(hanzi)) {
							targetIndex = index;
						}
					}
				}
				//����configActivity��������ã��ֲ�ͬ�����������������
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
				//��¼�Ƿ����С������0����ôû�����У�����Ϊ���С�
				resultToWrite.append("target:" + targetIndex + "\n");
				//д���ļ����ַ�����ʾһ��ƴ�����Ľ�����
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
