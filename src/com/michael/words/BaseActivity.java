package com.michael.words;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.michael.shell.Shell;
import com.michael.words.candidate.Candidate;
import com.michael.words.candidate.CandidateMeasure;
import com.michael.words.candidate.Coordinates;
import com.michael.words.keys.Keyboard;
import com.michael.words.utils.Utils;

public class BaseActivity extends Activity {

	protected static final int MSG_CLEAR_EDITTEXT = 0;
	protected static final int MSG_UPDATE_CUR_COUNT = 1;
	protected static final String IME_GUIDENCE_CONFIRM = "\u786e\u5b9a";
	protected static final String IME_GUIDENCE_I_KNOW = "\u77e5\u9053";

	private String mFilterOfType;
	protected static EditTextView mEditView;
	private static CountTextView mCurCountTextView;
	protected Shell mLogcat;
	protected Instrumentation mInstrumentation;
	protected boolean mPause;
	protected int mChoice;
	protected CandidateMeasure mMeasure;
	private SharedPreferences mSharedPreferences;
	protected Keyboard mKeyboard;
	private WakeLock mWakeLock;
	protected static int mCurCount = 0;
	protected ArrayList<Candidate> mLastSuccCandidateList;
	public static double FISRT_SCREEN_THRESHOLD = 1086;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);      
		getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN, WindowManager.LayoutParams. FLAG_FULLSCREEN);
		setContentView(R.layout.activity_edit);
		init();
		PowerManager powerManager = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
		mWakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Lock");
		try {
			ArrayList<File> rawFiles = Utils.getSuffixFiles(getApplicationContext(), Utils.CONFIG_FILE_SUFFIX);
			if (rawFiles.isEmpty() || rawFiles == null) {
				Utils.showToast(getApplicationContext(), R.string.toast_config_missing);
				finish();
			}

			mLogcat = new Shell("su");
			sleepSec(2);
			mLogcat.write("logcat CanvasDrawText:E *:S");

			mInstrumentation = new Instrumentation();

			mPause = false;

			mChoice = mSharedPreferences.getInt("choice", 0);

			mMeasure = new CandidateMeasure();

			mKeyboard = new Keyboard(getApplicationContext());

			//writeInfoHead();

			if (mKeyboard.KeyboardType == Keyboard.KEYBOARD_MODEL_NINE || 
					mKeyboard.KeyboardType == Keyboard.KEYBOARD_MODEL_HAND_WRITING) {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			}
			//mLastSuccCandidateList = new ArrayList<Candidate>();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	public void SetFilter(String filterOfType) {
		this.mFilterOfType = filterOfType;
	}

	public String GetFilter() {
		return this.mFilterOfType;
	}

	protected static Handler mHandler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_CLEAR_EDITTEXT:
				mEditView.setText("");
				break;
			case MSG_UPDATE_CUR_COUNT:
				mCurCountTextView.setText(String.valueOf(mCurCount));
			default:
				break;
			}
		};
	};

	@Override
	public void onBackPressed() {
		new Thread(uploadToFtp).start();
		super.onBackPressed();
	}

	@Override
	protected void onResume() {
		mWakeLock.acquire();
		super.onResume();
	}

	@Override
	protected void onPause() {
		mWakeLock.release();
		super.onPause();
	}

	private void init() {
		mEditView = (EditTextView) findViewById(R.id.editText1);
		//mEditView.setOnKeyListener(mOnLeftCTRListener);
		mEditView.requestFocus();

		mCurCountTextView = (CountTextView) findViewById(R.id.textView_cur_count);

		Button startButton = (Button) findViewById(R.id.button_start);
		startButton.setOnClickListener(mOnButtonStartListener);

		Button pauseButton = (Button) findViewById(R.id.button_pause);
		pauseButton.setOnClickListener(mOnButtonPauseListener);

		Button deleteButton = (Button) findViewById(R.id.button_delete);
		deleteButton.setOnClickListener(mOnButtonDeleteListener);

		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

	}

//	protected void writeInfoHead() {
//		PackageInfo pInfo = Utils.getCurrentImeInfo(getApplicationContext());
//		if (pInfo != null) {
//			new WriteFileThread(getApplicationContext(), 
//					"IMEName:" + pInfo.packageName + "\n" +
//							"IMEVersionName:" + pInfo.versionName + "\n" +
//							"IMEVersionCode:" + pInfo.versionCode + "\n"
//					).start();
//		}
//	}

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
			//writeInfoHead();
		}
	};

	private Runnable mSendRunnable = new Runnable() {

		@Override
		public void run() {
			//��ʼ̽�����뷨
			probeCandidateHeight();

			//TODO: �ڱ�����final����ֵ���������ܻ�ȽϿ��٣�ʹ�ó�Ա�����Ļ���cpu����79%���ֲܿ����мɣ�
			final int configChoice = mChoice;
			final int KeyboardType = mKeyboard.KeyboardType;
			final boolean NeedClearImeData = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
					.getBoolean("clearcontext", true);

			mCurCount = 0;
			String resultToWrite = "";
			mEditView.showInputMethod();
			Utils.showSoftInput(mEditView, getApplicationContext());
			try {
				mLogcat.read();

				String inputStr = null;
				ArrayList<File> rawFiles = Utils.getSuffixFiles(getApplicationContext(), Utils.CONFIG_FILE_SUFFIX);
				final boolean NeedRerun = !PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
						.getBoolean("LastRunSuccess", true);

				//ÿ��case�ļ���һ��
				for(File rawconfig : rawFiles) {
					BufferedReader reader = new BufferedReader(new FileReader(rawconfig));
					BufferedReader shadowReader = new BufferedReader(new FileReader(rawconfig));
					shadowReader.readLine();
					//check �Ƿ���rerun���ָ��ֳ�
					if (NeedRerun) {
						int RanCount = Utils.getLastCaseCountFromResult(getApplicationContext());
						if (RanCount != -1)
							for (int ranindex = 0; ranindex < RanCount; ranindex ++) {
								String temp = reader.readLine();
								shadowReader.readLine();
								if (temp.contains("a[" + RanCount + "]"))
									break;
							}
					}

					//NOTICE: Shouldn't clear IME data here, because we need RERUN!!! 
					//NOTICE: So clear the IME data after every pingceji is done.
					//Utils.clearImeData(Utils.getCurrentImeInfo(getApplicationContext()).packageName, getBaseContext());
					//sleepSec(10);
					Utils.clearImeContext(mInstrumentation);
					mEditView.showInputMethod();

					tapOnEditTextAndWait(3, 5);
					sleepSec(5);
					tapOnEditTextAndWait(3, 5);

					mLogcat = new Shell("su");
					sleepSec(2);
					mLogcat.write("logcat CanvasDrawText:E *:S");

					tapConfirmBtnIfThereIsAny(IME_GUIDENCE_CONFIRM);
					tapConfirmBtnIfThereIsAny(IME_GUIDENCE_I_KNOW);

					//����м���
					mCurCount = 0;
					resultToWrite = "";

					while ((inputStr = reader.readLine()) != null) {
						String NextCase = shadowReader.readLine();
						//��ͣ������ʱ������ѭ��ʵ�֣���ѭ�����CPU����ȥ����������
						//TODO: ���Ϸ��ʳ�Ա����������Ҳ���CPU����ȥ
						while(mPause){};
						//������tab������case���������Զ��Ÿ�����case������#��˵����Ҫ���������
						if (inputStr.contains("\t")) {
							//String pinyin = inputStr.substring(0, inputStr.indexOf("\t"));
							String pinyin = inputStr.split("\t")[0];
							//String hanzi = inputStr.substring(inputStr.indexOf("\t") + 1);
							String hanzi = inputStr.split("\t")[1];
							mLogcat.read();
							sleepMil(50);
							//Log.e("Performance", "Case Start, Case : " + inputStr);
							SendString(pinyin, inputStr);
							//Ϊ�˺��¸�Case������
							sleepMil(200);
							resultToWrite += readLogcat(pinyin, hanzi, inputStr, mFilterOfType);
							mCurCount++;						
						} else if (inputStr.contains(",") && inputStr.contains("\"")) {//������Զ��Ÿ���
							//ȥ������case�е�ƴ���ָ��
							inputStr = inputStr.replaceAll("'", "");
							//��ȡƴ���ͺ���,�����ȡ����ƴ���ͺ���,��ô��������case
							String pinyin = "";
							String hanzi = "";
							try {
								String[] caseStrs = inputStr.split("\"")[1].split(",");
								pinyin = caseStrs[1 + KeyboardType];
								hanzi = caseStrs[0];
							} catch (IndexOutOfBoundsException e) {
								e.printStackTrace();
								continue;
							}
							//�������#�����ǵ�����ģʽ����˵���������Case������ע�ⲻ�����ÿո����������������
							if (pinyin.contains("#") && configChoice == R.id.config_radio_choice_first_screen) {
								SendKey(Keyboard.KEYBOARD_DELETE_BUTTON);
								for (int i = 0; i < 2; i++)
									SendKey(Keyboard.KEYBOARD_SPACE_BUTTON);
								for (int i = 0; i < 2; i++)
									SendKey(Keyboard.KEYBOARD_DELETE_BUTTON);

								mLogcat.read();

							} else if (pinyin.contains("*")) {
								SendKey(Keyboard.KEYBOARD_DELETE_BUTTON);
								mLogcat.read();
								mCurCount++;
							} else if (pinyin.contains("&") && configChoice == R.id.config_radio_choice_first_screen) {
								sleepMil(100);
								findCompletion(hanzi, mFilterOfType);
								sleepMil(100);
								mLogcat.read();
								mCurCount++;
							} else {
								//��������������Ϊ��Rerun
								String resultForThisCase = "";
								int TrialCount = 0;
								while(resultForThisCase.equals("") && TrialCount < 10) {
									//�����Ҫrerun��˵��û�ж�ȡ����ѡ
									if (TrialCount > 0)
										for(int time =0; time < pinyin.length(); time++)
											SendKey(Keyboard.KEYBOARD_DELETE_BUTTON);

									mLogcat.read();
									sleepMil(50);
									//Log.e("Performance", "Case Start, Case : " + inputStr);
									SendString(pinyin, inputStr);
									//Ϊ�˺���һ������������
									sleepMil(200);
									resultForThisCase = readLogcat(pinyin, hanzi, inputStr, mFilterOfType);
									TrialCount++;
								}
								if (resultForThisCase.equals("") && TrialCount == 10)
									for(int time =0; time < pinyin.length(); time++)
										SendKey(Keyboard.KEYBOARD_DELETE_BUTTON);
								resultToWrite += resultForThisCase;
								mCurCount++;
							}
						}
						if (NextCase != null) {
							if (mCurCount % 20 == 0 && !NextCase.contains(",&,")) {
								sleepMil(50);
								mHandler.obtainMessage(MSG_CLEAR_EDITTEXT).sendToTarget();
								mHandler.obtainMessage(MSG_UPDATE_CUR_COUNT, mCurCount, 0).sendToTarget();
								sleepMil(50);
								//new WriteFileThread(getApplicationContext(), resultToWrite.toString()).start();
								resultToWrite = "";
							}
						}
					}
					//**������case������ϵ�ʱ�򣬻���һ����û�м�¼�꣬��ʱӦ��������β����
					mHandler.obtainMessage(MSG_CLEAR_EDITTEXT).sendToTarget();
					mHandler.obtainMessage(MSG_UPDATE_CUR_COUNT, mCurCount, 0).sendToTarget();
					//new WriteFileThread(getApplicationContext(), resultToWrite.toString()).run();
					//�ر�case�ļ���������
					reader.close();
					shadowReader.close();
					sleepSec(2);
					//Utils.renameResultTxt(rawconfig, getApplicationContext());
					//writeInfoHead();
					if (NeedClearImeData) {
						Utils.clearImeData(Utils.getCurrentImeInfo(getApplicationContext()).packageName, getBaseContext());
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
			mLogcat.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		super.onStop();
	}

	protected void findCompletion(String hanzi, String filterOfType) {
		if (null == mLastSuccCandidateList)
			return;
		if (mLastSuccCandidateList.size() < 1)
			return;

		final int MostYCord = mMeasure.MostYCord;


		String RawResult;
		try{
			RawResult = mLogcat.read();
			String[] resultlist = RawResult.split("\n");
			ArrayList<Candidate> candidateList = new ArrayList<Candidate>();
			int endIndex = -1;

			for (int i = resultlist.length - 1; i >= 0; i--) {
				//ȥ��ƴ���еķָ��
				resultlist[i] = resultlist[i].replaceAll("'", "");
				if (resultlist[i].contains("#y:" + MostYCord) && resultlist[i].contains(filterOfType)) {
					endIndex = i;
					break;
				}
			}

			if (endIndex != -1) {
				double lastX = Double.MAX_VALUE;
				for (int i = endIndex; ( i >= 0 && resultlist[i].contains("#y:" + MostYCord) ); i--){
					//ȥ��ƴ���еķָ��
					resultlist[i] = resultlist[i].replaceAll("'", "");
					if (resultlist[i].contains(filterOfType) && resultlist[i].contains("#y:" + MostYCord)) {
						//text:�����ǿյģ�������index,��ô��Ҫ��
						String text = resultlist[i].split("text:")[1].split("#")[0];
						if (text.equals("") || Utils.isNumber(text)){
							continue;
						}

						String word = resultlist[i].substring(
								resultlist[i].indexOf("text:") + "text:".length(), 
								resultlist[i].indexOf("#x"));
						double xCord = Double.valueOf(resultlist[i].substring(
								resultlist[i].indexOf("#x:") + "#x:".length(), 
								resultlist[i].indexOf("#y:")));
						double yCord = Double.valueOf(resultlist[i].substring(
								resultlist[i].indexOf("#y:") + "#y:".length(), 
								resultlist[i].indexOf(", type=")));
						if (xCord >= lastX) 
							break;
						lastX = xCord;
						Candidate candidate = new Candidate(word, new Coordinates(xCord, yCord));
						candidateList.add(candidate);
					} else if (!resultlist[i - 1].contains("#y:" + MostYCord)) {
						break;
					}
				}

				if (candidateList.equals(mLastSuccCandidateList))
					return;

				int targetIndex = -1;
				int indexToWrite = -1;
				//�õ��˺�ѡ���ں�ѡ����������Ҫѡ�������ĺ�ѡ
				for (int i = candidateList.size() - 1; i >=0; i--) {
					indexToWrite = candidateList.size() - i;
					String word = candidateList.get(i).content;
					if (indexToWrite <= FISRT_SCREEN_THRESHOLD){
						if (word.equals(hanzi)) {
							//��¼��candidateList����ʵ�����������ں���SendChoiceʹ��
							targetIndex = i;
						}
					}//if (index <= FISRT_SCREEN_THRESHOLD)
				}//for

				if (candidateList.size() < 1)
					return;

				if (targetIndex == -1){
					return;
				} else {
					//���target��0��11֮��
					SendChoice(candidateList.get(targetIndex).coordinates.x);
				}

			} else {//if (endIndex != -1)
				return;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return;
		}
	}

	/**
	 * pinyin + hanzi�����һ��case��������ȡlogcat��hook���أ�����һ��case�ķ������log����;
	 * ����ConfigActivity�Ĳ�ͬ���ã����в�ͬ������������������Ĺ�ϵ������
	 * @param pinyin ����case��ƴ�����봮
	 * @param hanzi ����caseҪ�ҵ�Ŀ�꺺��
	 * @return ����case�������log
	 */
	protected String readLogcat(String pinyin, String hanzi, String inputStr, String filterOfType) {
		//TODO: �ڱ�����final����ֵ���������ܻ�ȽϿ��٣�ʹ�ó�Ա�����Ļ���cpu����79%���ֲܿ�����?ɣ?
		final int configChoice = mChoice;
		final int MostYCord = mMeasure.MostYCord;

		String RawResult;
		try{
			RawResult = mLogcat.read();
			String[] resultlist = RawResult.split("\n");
			ArrayList<Candidate> candidateList = new ArrayList<Candidate>();
			int endIndex = -1;

			for (int i = resultlist.length - 1; i >= 0; i--) {
				//ȥ��ƴ���еķָ��
				resultlist[i] = resultlist[i].replaceAll("'", "");
				//String text = resultlist[i].split("text:")[1].split("#")[0];
				//�������ƴ�����ˣ�˵����ѡ��ȡ������
				//				if (resultlist[i].contains("text:" + pinyin +"#")) {
				//					endIndex = i - 1;
				//					break;
				//				} else 
				if (resultlist[i].contains("#y:" + MostYCord) && resultlist[i].contains(filterOfType)) {
					endIndex = i;
					break;
				}
			}

			if (endIndex != -1) {
				double lastX = Double.MAX_VALUE;
				for (int i = endIndex; ( i >= 0 && resultlist[i].contains("#y:" + MostYCord) ) ; i--){
					//ȥ��ƴ���еķָ��
					resultlist[i] = resultlist[i].replaceAll("'", "");
					if (resultlist[i].contains(filterOfType) && resultlist[i].contains("#y:" + MostYCord)) {
						//text:�����ǿյģ�������index,��ô��Ҫ��
						String text = resultlist[i].split("text:")[1].split("#")[0];
						if (text.equals("") || Utils.isNumber(text)){
							continue;
						}

						String word = resultlist[i].substring(
								resultlist[i].indexOf("text:") + "text:".length(), 
								resultlist[i].indexOf("#x"));
						double xCord = Double.valueOf(resultlist[i].substring(
								resultlist[i].indexOf("#x:") + "#x:".length(), 
								resultlist[i].indexOf("#y:")));
						double yCord = Double.valueOf(resultlist[i].substring(
								resultlist[i].indexOf("#y:") + "#y:".length(), 
								resultlist[i].indexOf(", type=")));
						if (xCord >= lastX) 
							break;
						lastX = xCord;
						Candidate candidate = new Candidate(word, new Coordinates(xCord, yCord));
						candidateList.add(candidate);
					} else if (!resultlist[((i - 1) < 0 ? 0 : (i - 1))].contains("#y:" + MostYCord)) {
						break;
					}
				}

				StringBuilder resultToWrite = new StringBuilder();
				int targetIndex = -1;
				//д���ļ����ַ�����ʾһ��ƴ�����Ŀ�ʼ
				resultToWrite.append("wordstart\n");
				resultToWrite.append("time:" + Utils.getDateTime() + "\n");
				resultToWrite.append("count:" + inputStr + "\n");
				resultToWrite.append("pinyin:" + pinyin + "\t" + hanzi + "\n");

				int indexToWrite = -1;
				//�õ��˺�ѡ���ں�ѡ����������Ҫѡ�������ĺ�ѡ
				for (int i = candidateList.size() - 1; i >=0; i--) {
					indexToWrite = candidateList.size() - i;
					String word = candidateList.get(i).content;
					//if (indexToWrite <= FISRT_SCREEN_THRESHOLD){
					if(candidateList.get(i).coordinates.x < FISRT_SCREEN_THRESHOLD) {
						resultToWrite.append(indexToWrite + ":");
						resultToWrite.append(word + "\n");
						if (word.equals(hanzi)) {
							//��¼��candidateList����ʵ�����������ں���SendChoiceʹ��
							targetIndex = i;
						}
					}//if (index <= FISRT_SCREEN_THRESHOLD)
				}//for
				if(candidateList.size() < 1) {
					//û����ȷ�Ķ�ȡ����ѡ����������������ʵ�case��Ҳ��û�����壬�����findCompletion�Ҳ���
					mLogcat.read();
					return "";
				}

				//����configActivity��������ã��ֲ�ͬ�����������������
				if (configChoice == R.id.config_radio_complete_no_choice) {
					for (int j = 0; j < pinyin.length(); j++) {
						SendKey(Keyboard.KEYBOARD_DELETE_BUTTON);
					}
					mLogcat.read();
				} else if (configChoice == R.id.config_radio_choice_first_candidate) {
					SendChoice(candidateList.get(candidateList.size() - 1).coordinates.x);
					SendKey(Keyboard.KEYBOARD_DELETE_BUTTON);
					SendKey(Keyboard.KEYBOARD_DELETE_BUTTON);
					SendKey(Keyboard.KEYBOARD_SPACE_BUTTON);
					SendKey(Keyboard.KEYBOARD_DELETE_BUTTON);
					mLogcat.read();
				} else if (configChoice == R.id.config_radio_choice_first_screen) {
					if (targetIndex == -1){
						//���û���ҵ�Ŀ��ʣ���ôɾ���Ѿ�������֣�������
						for(int time =0; time < pinyin.length(); time++)
							SendKey(Keyboard.KEYBOARD_DELETE_BUTTON);
						//���logcat��û���ҵ�����������������ʵ�case��Ҳ��û�����壬�����findCompletion�Ҳ���
						mLogcat.read();
					} else {
						//ѡ�˺�ѡ�Ժ�������룬Ϊ��findCompletion��ȡ�ĸ�׼ȷ�������logcat
						mLogcat.read();
						//���target��0��11֮�䣬ѡ��Ŀ�������
						SendChoice(candidateList.get(targetIndex).coordinates.x);
						mLastSuccCandidateList = new ArrayList<Candidate>(candidateList);
					}
				}
				//��¼�Ƿ����С������-1����ôû�����У�����Ϊ���С�
				resultToWrite.append("target:" + (targetIndex == -1 ? -1 : (candidateList.size() - targetIndex)) + "\n");
				//д���ļ����ַ�����ʾһ��ƴ�����Ľ�����
				resultToWrite.append("wordend\n");
				return resultToWrite.toString();
			} else {//if (endIndex != -1)
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

	protected void probeCandidateHeight() {
		//���������
		try {
			mLogcat.read();
			mEditView.showInputMethod();
			sleepMil(500);

			//����̽��ƴ����
			if (mKeyboard.KeyboardType == Keyboard.KEYBOARD_MODEL_NINE)
				SendString("2");
			else if (mKeyboard.KeyboardType == Keyboard.KEYBOARD_MODEL_QWERTY)
				SendString("q");

			sleepMil(100);
			String rawResult = null;
			rawResult = mLogcat.read();

			if (rawResult.equals("") || rawResult == null){
				Utils.showToast(getApplicationContext(), R.string.toast_probe_failed);
				finish();
				return;
			} else {
				//�����ѡ��#y:�����ֵ��������ַ����ڵ�λ�ã�
				String[] resultLines = rawResult.split("\n");
				ArrayList<String> resultList = new ArrayList<String>();
				for (String oneLine : resultLines){
					if (oneLine.contains(mFilterOfType))
						resultList.add(oneLine);
				}
				SparseIntArray array = new SparseIntArray();
				ArrayList<Point> coordinates = new ArrayList<Point>();
				for (String oneBuf : resultList){
					String text = oneBuf.split("text:")[1].split("#")[0];
					if (!text.equals("") && text != null) {
						int start = oneBuf.indexOf("#y:") + "#y:".length();
						int end = oneBuf .indexOf(", type=");
						String yCordStr = oneBuf.substring(start, end);
						int yCord = Integer.valueOf(yCordStr.substring(0, yCordStr.indexOf(".")));
						array.put(yCord, array.get(yCord, 0) + 1);
						double xCord = Double.valueOf(oneBuf.substring(
								oneBuf.indexOf("#x:") + "#x:".length(), 
								oneBuf.indexOf("#y:")));
						coordinates.add(new Point((int)xCord, (int)yCord));
					}
				}
				int MaxCount = 0;
				int mostYCord = 62;
				for (int i = 0; i < array.size(); i++){
					if (array.valueAt(i) > MaxCount){
						MaxCount = array.valueAt(i);
						mostYCord = array.keyAt(i);
					}
				}

				Point outSize = new Point();
				outSize = Utils.getCurScreenSize(getApplicationContext());
				mMeasure.ScreenHeight = outSize.y;
				mMeasure.ScreenWidth = outSize.x;
				mMeasure.MostYCordInScreen = mKeyboard.getKeyLocation(Keyboard.KEYBOARD_CANDIDATE_CORD).y;
				mMeasure.MostYCord = mostYCord;

				ArrayList<Point> candCoordinates = new ArrayList<Point>();
				for (int index = 0; index < coordinates.size(); index++) {
					Point point = coordinates.get(index);
					if(point.y == mMeasure.MostYCord)
						candCoordinates.add(point);
				}
				mMeasure.oneWordCandWidth = Math.abs(candCoordinates.get(1).x - candCoordinates.get(0).x); 
				mMeasure.FisrtScreenThreshold = Utils.getCurDisplaySize(getApplicationContext()).x
						- mMeasure.oneWordCandWidth * 1.5;
				FISRT_SCREEN_THRESHOLD = mMeasure.FisrtScreenThreshold;

				SendKey(Keyboard.KEYBOARD_DELETE_BUTTON);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * ����һЩ�����ϵĿ��Ʒ�
	 * ������dele��ɾ������spli���ָ�������symb�����ţ���numb�����֣���
	 * spac���ո񣩡�swit����Ӣ���л�����ente���س�����comm�����ţ���peri����ţ�
	 * @param Keycode
	 * @throws IOException
	 */
	public void SendKey(String KeyCode) throws IOException {
		Keyboard.TouchPoint point = null; 
		point = mKeyboard.getKeyLocation(KeyCode);
		if (point != null) {
			tapScreen(point.x, point.y);
		}

	}

	protected void SendChoice(double x) throws IOException{
		int xCord = 
				new BigDecimal(x).setScale(0, BigDecimal.ROUND_HALF_UP).intValue();
		int yCord = 
				new BigDecimal(mMeasure.MostYCordInScreen).setScale(0, BigDecimal.ROUND_HALF_UP).intValue();
		tapScreen(xCord, yCord);
	}

	/**
	 * ͨ��������Ļ�ķ�ʽ�����֣�����sogou������һ��һ����char����ѯλ�ã�Ȼ������tapScreen��
	 * @param text Ҫ���͵ļ����¼������硰sogou��������Сд��
	 * @throws IOException
	 */
	protected void SendString(String text, String testCase) throws IOException{
		for (int i = 0; i < text.length(); i ++){
			String letter = String.valueOf(text.charAt(i));
			Keyboard.TouchPoint point = null;
			point = mKeyboard.getKeyLocation(letter);
			if (point != null) {
				tapScreen(point.x, point.y, letter, testCase);

			}
		}
	}

	/**
	 * ͨ��������Ļ�ķ�ʽ�����֣�����sogou������һ��һ����char����ѯλ�ã�Ȼ������tapScreen��
	 * @param text Ҫ���͵ļ����¼������硰sogou��������Сд��
	 * @throws IOException
	 */
	protected void SendString(String text) throws IOException{
		for (int i = 0; i < text.length(); i ++){
			String letter = String.valueOf(text.charAt(i));
			Keyboard.TouchPoint point = null;
			point = mKeyboard.getKeyLocation(letter);
			if (point != null) {
				tapScreen(point.x, point.y);

			}
		}
	}

	protected void tapScreen(float x, float y){
		MotionEvent tapDownEvent = MotionEvent.obtain(
				SystemClock.uptimeMillis(), 
				SystemClock.uptimeMillis(), 
				MotionEvent.ACTION_DOWN, 
				x, 
				y, 
				0);
		MotionEvent tapUpEvent = MotionEvent.obtain(
				SystemClock.uptimeMillis(), 
				SystemClock.uptimeMillis(), 
				MotionEvent.ACTION_UP, 
				x, 
				y, 
				0);
		mInstrumentation.sendPointerSync(tapDownEvent);
		mInstrumentation.sendPointerSync(tapUpEvent);

		tapDownEvent.recycle();
		tapUpEvent.recycle();
	}

	protected void tapScreen(float x, float y, String letter, String testCase){
		MotionEvent tapDownEvent = MotionEvent.obtain(
				SystemClock.uptimeMillis(), 
				SystemClock.uptimeMillis(), 
				MotionEvent.ACTION_DOWN, 
				x, 
				y, 
				0);
		MotionEvent tapUpEvent = MotionEvent.obtain(
				SystemClock.uptimeMillis(), 
				SystemClock.uptimeMillis(), 
				MotionEvent.ACTION_UP, 
				x, 
				y, 
				0);
		long letterDownStartTime = SystemClock.elapsedRealtime();
		Log.i("clock", "Case: " + testCase + ", letter: " + letter + ", down: " + letterDownStartTime);
		mInstrumentation.sendPointerSync(tapDownEvent);
		long letterMiddleTime = SystemClock.elapsedRealtime();
		Log.i("clock", "Case: " + testCase + ", letter: " + letter + ", mid: " + letterMiddleTime);
		mInstrumentation.sendPointerSync(tapUpEvent);
		long letterUpEndTime = SystemClock.elapsedRealtime();
		Log.i("clock", "Case: " + testCase + ", letter: " + letter + ", up: " + letterUpEndTime);

		String tempLog = "Case:" + testCase + 
				"\t" + letter +   
				"\t" + (letterMiddleTime - letterDownStartTime) +
				"\t" + (letterUpEndTime - letterMiddleTime + "\n");
		Log.e("PerformanceCal",  tempLog);

		try {
			FileOutputStream os = openFileOutput("result-performance.txt", Context.MODE_APPEND);
			os.write(tempLog.getBytes("UTF-8"));
			os.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		tapDownEvent.recycle();
		tapUpEvent.recycle();
	}

	protected void tapDown(float x, float y){
		MotionEvent tapDownEvent = MotionEvent.obtain(
				SystemClock.uptimeMillis(), 
				SystemClock.uptimeMillis(), 
				MotionEvent.ACTION_DOWN, 
				x, 
				y, 
				0);
		mInstrumentation.sendPointerSync(tapDownEvent);
		tapDownEvent.recycle();
	}

	protected void tapUp(float x, float y){
		MotionEvent tapUpEvent = MotionEvent.obtain(
				SystemClock.uptimeMillis(), 
				SystemClock.uptimeMillis(), 
				MotionEvent.ACTION_UP, 
				x, 
				y, 
				0);
		mInstrumentation.sendPointerSync(tapUpEvent);
		tapUpEvent.recycle();
	}

	protected void tapMove(float x, float y){
		MotionEvent tapUpEvent = MotionEvent.obtain(
				SystemClock.uptimeMillis(), 
				SystemClock.uptimeMillis(), 
				MotionEvent.ACTION_MOVE, 
				x, 
				y, 
				0);
		mInstrumentation.sendPointerSync(tapUpEvent);
		tapUpEvent.recycle();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.edit, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.about:
			Utils.showDialog(BaseActivity.this, 
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

	protected static void sleepSec(int second) {
		try {
			Thread.sleep(second * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	protected static void sleepMil(int millisecond) {
		try {
			Thread.sleep(millisecond);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	protected void tapOnEditTextAndWait(int tapCount, int waitTime) {
		for(int count = 0; count < tapCount; count++) {
			for(int tapindex = 0; tapindex < 10; tapindex++) {
				tapScreen(370.0f, 65.0f);
				sleepMil(10);
			}
			sleepSec(waitTime);
		}
	}

	protected void tapConfirmBtnIfThereIsAny(String confirm) {
		double x = -1f;
		double y = -1f;
		try {
			String RawResult = mLogcat.read();
			String[] resultlist = RawResult.split("\n");
			for (int index = resultlist.length - 1; index >= 0; index--) {
				String eachLine = resultlist[index];
				if (eachLine.contains(confirm)) {
					x = Double.valueOf(eachLine.substring(
							eachLine.indexOf("#x:") + "#x:".length(), 
							eachLine.indexOf("#y:")));
					y = Double.valueOf(eachLine.substring(
							eachLine.indexOf("#y:") + "#y:".length(), 
							eachLine.indexOf(", type=")));
					break;
				}
			}
		} catch (IOException e) {
			tapScreen(384.0f, 1084.0f);
			e.printStackTrace();
		} catch (InterruptedException e) {
			tapScreen(384.0f, 1084.0f);
			e.printStackTrace();
		}
		if (x > 0 && y > 0) {
			tapScreen((float)x, (float)y);
			Log.d("EditActivity.tapConfirmBtnIfThereIsAny", "===taped confirm! calculated! x : " + x +", y : " + y + "===");
		} else {
			tapScreen(384.0f, 1084.0f);
			Log.d("EditActivity.tapConfirmBtnIfThereIsAny", "===taped confirm! specified! x : " + x +", y : " + y + "===");
		}
	}

}
