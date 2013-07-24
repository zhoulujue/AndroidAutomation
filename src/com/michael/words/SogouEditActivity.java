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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Point;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.Menu;
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

public class SogouEditActivity extends Activity {
	private EditTextView mEditView;
	private Shell mLogcat;
	private Instrumentation mInstrumentation;
	private BufferedReader mReader;
	private boolean mPause;
	private int mChoice;
	private CandidateMeasure mMeasure;
	private SharedPreferences mSharedPreferences;
	public static int FISRT_SCREEN_THRESHOLD = 12;

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

			mMeasure = new CandidateMeasure();
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
			//��ʼ̽�����뷨
			probeCandidateHeight();

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
					//while(mPause){};
					//������tab������case���������Զ��Ÿ�����case������#��˵����Ҫ���������
					if (inputStr.contains("\t")) {
						String pinyin = inputStr.substring(0, inputStr.indexOf("\t"));
						String hanzi = inputStr.substring(inputStr.indexOf("\t") + 1);
						SendString(pinyin);
						sleepMil(100);
						resultToWrite += readLogcat(pinyin, hanzi);
						curCount++;							
					} else if (inputStr.contains(",") && inputStr.contains("\"")) {//������Զ��Ÿ���
						//��ȡ���ŵ��ڶ�������֮ǰ���ַ���a[0]="��,w,9999,21097634"; -> ��,w
						inputStr = inputStr.substring(inputStr.indexOf("\"") + 1, 
								inputStr.indexOf(",", inputStr.indexOf(",") + 1));
						String pinyin = inputStr.substring(inputStr.indexOf(",") + 1);
						String hanzi = inputStr.substring(0, inputStr.indexOf(","));

						//�������#�����ǵ�����ģʽ����˵���������Case������ע�ⲻ�����ÿո����������������
						if (pinyin.contains("#") && configChoice == R.id.config_radio_choice_first_screen) {
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
								resultForThisCase = readLogcat(pinyin, hanzi);
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
	private String readLogcat(String pinyin, String hanzi) {
		//TODO: �ڱ�����final����ֵ���������ܻ�ȽϿ��٣�ʹ�ó�Ա�����Ļ���cpu����79%���ֲܿ����мɣ�
		final int configChoice = mChoice;
		final double MostYCord = mMeasure.MostYCord;
		String RawResult;
		try{
			RawResult = mLogcat.read();
			String[] resultlist = RawResult.split("\n");
			ArrayList<Candidate> candidateList = new ArrayList<Candidate>();
			int endIndex = -1;

			for (int i = resultlist.length - 1; i >= 0; i--) {
				//ȥ��ƴ���еķָ��
				resultlist[i] = resultlist[i].replaceAll("'", "");
				//�������ƴ�����ˣ�˵����ѡ��ȡ������
				if (resultlist[i].contains("text:" + pinyin +"#")) {
					endIndex = i - 1;
					break;
				} 
			}

			if (endIndex != -1) {
				//ɸ�ú�ѡ������Ϣ��˳���ǵ��ŵ�
				for (int i = endIndex; (i >= 0 && !resultlist[i].contains("text:1#")); i--){
					//ȥ��ƴ���еķָ��
					resultlist[i] = resultlist[i].replaceAll("'", "");

					//ͨ��type=buf��y����ɸѡ��ѡ���Ժ󣬰Ѻ�ѡ��ȡ����
					if (resultlist[i].contains(", type=buf") && resultlist[i].contains("#y:" + MostYCord)
							) {
						String word = resultlist[i].substring(
								resultlist[i].indexOf("text:") + "text:".length(), 
								resultlist[i].indexOf("#"));
						double xCord = Double.valueOf(resultlist[i].substring(
								resultlist[i].indexOf("#x:") + "#x:".length(), 
								resultlist[i].indexOf("#y:")));
						double yCord = Double.valueOf(resultlist[i].substring(
								resultlist[i].indexOf("#y:") + "#y:".length(), 
								resultlist[i].indexOf(", type=")));
						Candidate candidate = new Candidate(word, new Coordinates(xCord, yCord));
						candidateList.add(candidate);
					}
				}

				StringBuilder resultToWrite = new StringBuilder();
				int targetIndex = -1;
				//д���ļ����ַ�����ʾһ��ƴ�����Ŀ�ʼ
				resultToWrite.append("wordstart\n");
				resultToWrite.append("pinyin:" + pinyin + "\t" + hanzi + "\n");

				int indexToWrite = -1;
				//�õ��˺�ѡ���ں�ѡ����������Ҫѡ�������ĺ�ѡ
				for (int i = candidateList.size() - 1; i >=0; i--) {
					indexToWrite = candidateList.size() - i;
					String word = candidateList.get(i).content;
					if (indexToWrite <= FISRT_SCREEN_THRESHOLD){
						resultToWrite.append(indexToWrite + ":");
						resultToWrite.append(word + "\n");
						//TODO: ���Ե�ʱ��򿪣����е�ʱ��ر�
						//Log.e("reading", "The Word is : " + index + ": " + word);
						if (word.equals(hanzi)) {
							//��¼��candidateList����ʵ�����������ں���SendChoiceʹ��
							targetIndex = indexToWrite;
						}
					}//if (index <= FISRT_SCREEN_THRESHOLD)
				}//for

				//����configActivity��������ã��ֲ�ͬ�����������������
				if (configChoice == R.id.config_radio_complete_no_choice) {
					for (int j = 0; j < pinyin.length(); j++) {
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
					if (targetIndex == -1){
						//���û���ҵ�Ŀ��ʣ���ô�ո�����
						SendChoice(KeyEvent.KEYCODE_ENTER);
					} else {
						//���target��0��11֮��
						SendChoice(String.valueOf(targetIndex));
						//SendChoice(candidateList.get(targetIndex).coordinates.x);
					}
				}
				//��¼�Ƿ����С������-1����ôû�����У�����Ϊ���С�
				resultToWrite.append("target:" + targetIndex + "\n");
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
		double QXCord = 0;
		try {
			mLogcat.read();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		mEditView.showInputMethod();
		//��������������¼���׼����������
		for (int j = 0; j < 3; j++){
			try {
				SendKey(KeyEvent.KEYCODE_CTRL_RIGHT);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		//����̽��ƴ����
		try {
			SendKey(KeyEvent.KEYCODE_Q);
		} catch (IOException e) {
			e.printStackTrace();
		}
		//�ȴ����뷨��Ӧ
		//��������������¼���׼����������
		for (int j = 0; j < 30; j++){
			try {
				SendKey(KeyEvent.KEYCODE_CTRL_RIGHT);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		String rawResult = null;
		try {
			rawResult = mLogcat.read();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (rawResult.equals("") || rawResult == null){
			Utils.showToast(getApplicationContext(), R.string.toast_probe_failed);
			finish();
			return;
		} else {
			String[] resultLines = rawResult.split("\n");
			ArrayList<String> resultList = new ArrayList<String>();

			for (String oneLine : resultLines){
				if (oneLine.contains(", type=buf"))
					resultList.add(oneLine);
				//����ؼ��ĸ߶�
				if (oneLine.contains("text:Q#")) {
					int start = oneLine.indexOf("#y:") + "#y:".length();
					int end = oneLine .indexOf(", type=String");	
					String yCordStr = oneLine.substring(start, end);
					singleCtrlHeight = Double.valueOf(yCordStr);

					start = oneLine.indexOf("#x:") + "#x:".length();
					end = oneLine .indexOf("#y:");	
					String xCordStr = oneLine.substring(start, end);
					QXCord = Double.valueOf(xCordStr) * 2.0;
				}
			}
			SparseIntArray array = new SparseIntArray();
			for (String oneBuf : resultList){
				int start = oneBuf.indexOf("#y:") + "#y:".length();
				int end = oneBuf .indexOf(", type=buf");
				String yCordStr = oneBuf.substring(start, end);
				int yCord = Integer.valueOf(yCordStr.substring(0, yCordStr.indexOf(".")));
				array.put(yCord, array.get(yCord, 0) + 1);
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
			try {
				((WindowManager) createPackageContext(
						Utils.getCurrentImeInfo(getApplicationContext()).packageName, 
						Context.CONTEXT_IGNORE_SECURITY)
						.getSystemService(Context.WINDOW_SERVICE))
						.getDefaultDisplay().getRealSize(outSize);
			} catch (NameNotFoundException e1) {
				e1.printStackTrace();
			}
			mMeasure.ScreenHeight = outSize.y;
			mMeasure.ScreenWidth = outSize.x;
			mMeasure.MostYCordInScreen = mMeasure.ScreenHeight - (singleCtrlHeight * 4) - mostYCord/2.0;
			mMeasure.CtrlHeight = singleCtrlHeight;
			mMeasure.MostYCord = mostYCord;
			mMeasure.QxCord = QXCord;
			mMeasure.QyCord = mMeasure.MostYCordInScreen + mostYCord;
			mMeasure.DELx = mMeasure.ScreenWidth - QXCord * 3;
			mMeasure.DELy = mMeasure.ScreenHeight - singleCtrlHeight * 1.5;
			try {
				SendKey(KeyEvent.KEYCODE_DEL);
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}

	private void SendKey(int Keycode) throws IOException{
		mInstrumentation.sendKeyDownUpSync(Keycode);
	}

	private void SendChoice(String Keycode) throws IOException{
		int key = Integer.valueOf(Keycode) + 7;
		//Log.e("Send Choice", "Keycode:" + key);
		mInstrumentation.sendKeyDownUpSync(key);
	}
	
	private void SendChoice(double x) throws IOException{
		int xCord = 
				new BigDecimal(x).setScale(0, BigDecimal.ROUND_HALF_UP).intValue();
		int yCord = 
				new BigDecimal(mMeasure.MostYCordInScreen).setScale(0, BigDecimal.ROUND_HALF_UP).intValue();
		tapScreen(xCord, yCord);
	}

	private void SendString(String text) throws IOException{
		//Log.e("InputKeyEvent", "text:" + text);
		mInstrumentation.sendStringSync(text);
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
