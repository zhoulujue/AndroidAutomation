package com.michael.words;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.graphics.Point;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.michael.shell.Shell;
import com.michael.words.candidate.Candidate;
import com.michael.words.candidate.CandidateMeasure;
import com.michael.words.candidate.Coordinates;
import com.michael.words.keys.Keybord;
import com.michael.words.utils.Utils;

public class SogouEditActivity extends Activity {
	private EditTextView mEditView;
	private Shell mLogcat;
	private Instrumentation mInstrumentation;
	private boolean mPause;
	private int mChoice;
	private CandidateMeasure mMeasure;
	private SharedPreferences mSharedPreferences;
	private Keybord mKeybord;
	private WakeLock mWakeLock;
	public static int FISRT_SCREEN_THRESHOLD = 12;

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

			mKeybord = new Keybord(getApplicationContext());
			
			writeInfoHead();
			
			if (mKeybord.keybordType == Keybord.KEYBORD_MODEL_NINE) {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			}
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
			//��ʼ̽�����뷨
			probeCandidateHeight();

			//TODO: �ڱ�����final����ֵ���������ܻ�ȽϿ��٣�ʹ�ó�Ա�����Ļ���cpu����79%���ֲܿ����мɣ�
			final int configChoice = mChoice;
			final int keybordType = mKeybord.keybordType;

			int curCount = 0;
			String resultToWrite = "";
			mEditView.showInputMethod();
			Utils.showSoftInput(mEditView, getApplicationContext());
			try {
				mLogcat.read();

				String inputStr = null;
				ArrayList<File> rawFiles = Utils.getSuffixFiles(getApplicationContext(), Utils.CONFIG_FILE_SUFFIX);
				boolean NeedRerun = !PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
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
								reader.readLine();
								shadowReader.readLine();
							}
					}

					Utils.clearImeContext(mInstrumentation);
					mEditView.showInputMethod();
					tapScreen(370.0f, 75.0f);
					sleepSec(10);
					tapScreen(370.0f, 75.0f);
					sleepSec(10);
					tapScreen(370.0f, 75.0f);
					sleepSec(10);
					
					mLogcat = new Shell("su");
					sleepSec(2);
					mLogcat.write("logcat CanvasDrawText:E *:S");
					
					//����м���
					curCount = 0;
					resultToWrite = "";

					while ((inputStr = reader.readLine()) != null) {
						String NextCase = shadowReader.readLine();
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
							//ȥ������case�е�ƴ���ָ��
							inputStr = inputStr.replaceAll("'", "");
							//��ȡƴ���ͺ���,�����ȡ����ƴ���ͺ���,��ô��������case
							String pinyin = "";
							String hanzi = "";
							try {
								String[] caseStrs = inputStr.split("\"")[1].split(",");
								pinyin = caseStrs[keybordType + 1];
								hanzi = caseStrs[0];
							} catch (IndexOutOfBoundsException e) {
								e.printStackTrace();
								continue;
							}
							//�������#�����ǵ�����ģʽ����˵���������Case������ע�ⲻ�����ÿո����������������
							if (pinyin.contains("#") && configChoice == R.id.config_radio_choice_first_screen) {
								SendKey(KeyEvent.KEYCODE_DEL);
								for (int i = 0; i < 2; i++)
									SendKey(KeyEvent.KEYCODE_SPACE);
								for (int i = 0; i < 2; i++)
									SendKey(KeyEvent.KEYCODE_DEL);
								
								mLogcat.read();
								
							} else if (pinyin.contains("*")) {
								SendKey(KeyEvent.KEYCODE_DEL);
							} else if (pinyin.contains("&") && configChoice == R.id.config_radio_choice_first_screen) {
								for (int j = 0; j < 3; j++)
									SendKey(KeyEvent.KEYCODE_CTRL_RIGHT);

								findCompletion(hanzi);

								for (int j = 0; j < 3; j++)
									SendKey(KeyEvent.KEYCODE_CTRL_RIGHT);
								
								mLogcat.read();
								
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

									mLogcat.read();
									SendString(pinyin);

									//Ϊ�˺���һ������������
									for (int j = 0; j < (pinyin.length() < 4 ? 10:5); j++)
										SendKey(KeyEvent.KEYCODE_CTRL_RIGHT);

									sleepMil(200);
									resultForThisCase = readLogcat(pinyin, hanzi, inputStr);
									TrialCount++;
								}
								if (resultForThisCase.equals("") && TrialCount == 10)
									for(int time =0; time < pinyin.length(); time++)
										SendKey(KeyEvent.KEYCODE_DEL);
								resultToWrite += resultForThisCase;
								curCount++;
							}
						}
						if (NextCase != null) {
							if (curCount % 20 == 0 && !NextCase.contains("&")) {
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
					new WriteFileThread(getApplicationContext(), resultToWrite.toString()).run();
					//�ر�case�ļ���������
					reader.close();
					shadowReader.close();
					sleepSec(2);
					Utils.renameResultTxt(rawconfig, getApplicationContext());
					writeInfoHead();
					Utils.clearImeData(Utils.getCurrentImeInfo(getApplicationContext()).packageName, getBaseContext());
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

	private void findCompletion(String hanzi) {
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
				if (resultlist[i].contains("#y:" + MostYCord) && resultlist[i].contains(", type=buf")) {
					endIndex = i;
					break;
				}
			}

			if (endIndex != -1) {
				double lastX = Double.MAX_VALUE;
				for (int i = endIndex; ( i >= 0 && resultlist[i].contains("#y:" + MostYCord) ); i--){
					//ȥ��ƴ���еķָ��
					resultlist[i] = resultlist[i].replaceAll("'", "");
					if (resultlist[i].contains(", type=buf") && resultlist[i].contains("#y:" + MostYCord)) {
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
	private String readLogcat(String pinyin, String hanzi, String inputStr) {
		//TODO: �ڱ�����final����ֵ���������ܻ�ȽϿ��٣�ʹ�ó�Ա�����Ļ���cpu����79%���ֲܿ����мɣ�
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
					if (resultlist[i].contains("#y:" + MostYCord) && resultlist[i].contains(", type=buf")) {
					endIndex = i;
					break;
				}
			}

			if (endIndex != -1) {
				double lastX = Double.MAX_VALUE;
				for (int i = endIndex; ( i >= 0 && resultlist[i].contains("#y:" + MostYCord) ) ; i--){
					//ȥ��ƴ���еķָ��
					resultlist[i] = resultlist[i].replaceAll("'", "");
					if (resultlist[i].contains(", type=buf") && resultlist[i].contains("#y:" + MostYCord)) {
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
					if (indexToWrite <= FISRT_SCREEN_THRESHOLD){
						resultToWrite.append(indexToWrite + ":");
						resultToWrite.append(word + "\n");
						if (word.equals(hanzi)) {
							//��¼��candidateList����ʵ�����������ں���SendChoiceʹ��
							targetIndex = i;
						}
					}//if (index <= FISRT_SCREEN_THRESHOLD)
				}//for

				//����configActivity��������ã��ֲ�ͬ�����������������
				if (configChoice == R.id.config_radio_complete_no_choice) {
					for (int j = 0; j < pinyin.length(); j++) {
						SendKey(KeyEvent.KEYCODE_DEL);
					}
				} else if (configChoice == R.id.config_radio_choice_first_candidate) {
					if (candidateList.size() < 1)
						return "";
					//SendChoice("1");
					SendChoice(candidateList.get(candidateList.size() - 1).coordinates.x);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							mEditView.setText("");
						}
					});
					//SendKey(KeyEvent.KEYCODE_SEMICOLON);
					SendKey(KeyEvent.KEYCODE_DEL);
					SendKey(KeyEvent.KEYCODE_DEL);
					SendKey(KeyEvent.KEYCODE_SPACE);
					SendKey(KeyEvent.KEYCODE_DEL);
				} else if (configChoice == R.id.config_radio_choice_first_screen) {
					if (candidateList.size() < 1)
						return "";

					if (targetIndex == -1){
						//���û���ҵ�Ŀ��ʣ���ô�ո�����
						SendChoice(candidateList.get(candidateList.size() - 1).coordinates.x);
					} else {
						//���target��0��11֮��
						//SendChoice(String.valueOf(targetIndex));
						SendChoice(candidateList.get(targetIndex).coordinates.x);
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

	private void probeCandidateHeight() {
		//���������
		double singleCtrlHeight = 0;
		//double QXCord = 0;
		try {
			mLogcat.read();
			mEditView.showInputMethod();
			//��������������¼���׼����������
			for (int j = 0; j < 3; j++){
				SendKey(KeyEvent.KEYCODE_CTRL_RIGHT);
			}
			//����̽��ƴ����
			if (mKeybord.keybordType == Keybord.KEYBORD_MODEL_NINE)
				SendString("2");
			else if (mKeybord.keybordType == Keybord.KEYBORD_MODEL_QWERTY)
				SendString("q");

			//�ȴ����뷨��Ӧ
			//��������������¼���׼����������
			for (int j = 0; j < 30; j++){
				SendKey(KeyEvent.KEYCODE_CTRL_RIGHT);
			}

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
					if (oneLine.contains(", type=String") || oneLine.contains(", type=buf"))
						resultList.add(oneLine);
				}
				SparseIntArray array = new SparseIntArray();
				for (String oneBuf : resultList){
					String text = oneBuf.split("text:")[1].split("#")[0];
					if (!text.equals("") && text != null) {
						int start = oneBuf.indexOf("#y:") + "#y:".length();
						int end = oneBuf .indexOf(", type=");
						String yCordStr = oneBuf.substring(start, end);
						int yCord = Integer.valueOf(yCordStr.substring(0, yCordStr.indexOf(".")));
						array.put(yCord, array.get(yCord, 0) + 1);
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
				mMeasure.MostYCordInScreen = mKeybord.getKeyLocation(Keybord.KEYBORD_CANDIDATE_CORD).y;
				mMeasure.CtrlHeight = singleCtrlHeight;
				mMeasure.MostYCord = mostYCord;
				mMeasure.DELx = mKeybord.getKeyLocation(Keybord.KEYBORD_DELETE_BUTTON).x;
				mMeasure.DELy = mKeybord.getKeyLocation(Keybord.KEYBORD_DELETE_BUTTON).y;

				SendKey(KeyEvent.KEYCODE_DEL);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void SendKey(int Keycode) throws IOException{
		mInstrumentation.sendKeyDownUpSync(Keycode);
	}

	/**
	 * ����һЩ�����ϵĿ��Ʒ�
	 * ������dele��ɾ������spli���ָ�������symb�����ţ���numb�����֣���
	 * spac���ո񣩡�swit����Ӣ���л�����ente���س�����comm�����ţ���peri����ţ�
	 * @param Keycode
	 * @throws IOException
	 */
	public void SendKey(String KeyCode) throws IOException {
		Keybord.TouchPoint point = null; 
		point = mKeybord.getKeyLocation(KeyCode);
		if (point != null) {
			tapScreen(point.x, point.y);
		}

	}

	private void SendChoice(double x) throws IOException{
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
	private void SendString(String text) throws IOException{
		//Log.e("InputKeyEvent", "text:" + text);
		for (int i = 0; i < text.length(); i ++){
			String letter = String.valueOf(text.charAt(i));
			Keybord.TouchPoint point = null;
			point = mKeybord.getKeyLocation(letter);
			if (point != null) {
				tapScreen(point.x, point.y);
				sleepMil(10);
			}

		}
	}

	private void tapScreen(float x, float y){
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.edit, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.about:
			Utils.showDialog(SogouEditActivity.this, 
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
