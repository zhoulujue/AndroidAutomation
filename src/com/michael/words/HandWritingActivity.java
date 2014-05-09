package com.michael.words;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import android.graphics.Point;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.Button;

import com.michael.shell.Shell;
import com.michael.words.keys.Keybord;
import com.michael.words.utils.Utils;

public class HandWritingActivity extends BaseActivity {

	private static final String pointsArrayForProbe =
			"305,494,338,489,537,447,611,433,639,427,661,423,676,420,684,418,686,418,-1,0,"
					+ "282,694,345,678,398,663,454,646,507,632,550,619,588,609,616,601,638,596,652,594,656,594,-1,0,"
					+ "254,879,344,854,418,837,493,817,554,797,604,778,645,761,674,747,693,735,703,725,707,716,707,716,-1,0,"
					+ "477,496,485,617,493,697,499,790,504,877,502,945,495,996,484,1046,470,1077,455,1094,427,1106,391,1105,"
					+ "353,1090,310,1053,278,1014,278,1014,-1,0,"
					+ "-1,-1";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//���ݲ�ͬ�����뷨�����ò�ͬ�Ĺ���
		String ImeName = Utils.getCurrentImeInfo(getApplicationContext()).packageName;
		if (ImeName.contains("sogou")) {
			SetFilter(", type=subSequence");
		} else {
			SetFilter(", type=String");
		}
		Button startBtn = (Button) findViewById(R.id.button_start);
		startBtn.setOnClickListener(mOnButtonStartHWListener);
	}

	@Override
	protected void probeCandidateHeight() {
		//���������
		double singleCtrlHeight = 0;
		try {
			mLogcat.read();
			sleepSec(10);
			//����̽��ƴ����
			SendString(pointsArrayForProbe);
			sleepSec(3);
			
			String rawResult = null;
			rawResult = mLogcat.read();
			sleepSec(1);

			if (rawResult.equals("") || rawResult == null){
				finish();
				return;
			} else {
				//�����ѡ��#y:�����ֵ��������ַ����ڵ�λ�ã�
				String[] resultLines = rawResult.split("\n");
				ArrayList<String> resultList = new ArrayList<String>();
				for (String oneLine : resultLines){
					if (oneLine.contains(", type=String") || oneLine.contains(", type=buf") || oneLine.contains(", type=subSequence"))
						resultList.add(oneLine);
				}
				SparseIntArray array = new SparseIntArray();
				for (String oneBuf : resultList){
					String text = oneBuf.split("text:")[1].split("#")[0];
					if (!text.equals("") && text != null && Utils.isChineseCharacter(text)) {
						int start = oneBuf.indexOf("#y:") + "#y:".length();
						int end = oneBuf .indexOf(", type=");
						String yCordStr = oneBuf.substring(start, end);
						int yCord = Integer.valueOf(yCordStr.substring(0, yCordStr.indexOf(".")));
						array.put(yCord, array.get(yCord, 0) + 1);
					}
				}
				int MaxCount = 0;
				int mostYCord = (int)mKeybord.getKeyLocation(Keybord.KEYBORD_CANDIDATE_CORD).y;
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

				SendKey(Keybord.KEYBORD_DELETE_BUTTON);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void SendString(String points) throws IOException {
		String[] pointsArray = points.split(",");
		int preX = -1;
		int preY = 0;
		for(int i = 0; i < pointsArray.length; i+=2) {
			int xCoor = 0;
			int yCoor = 0;
			try {
				xCoor = Integer.valueOf(pointsArray[i]).intValue();
				yCoor = Integer.valueOf(pointsArray[i + 1]).intValue();
			} catch (NumberFormatException e) {
				//�����ǰ�����꣨x,y������������ô��������㣬����������
				e.printStackTrace();
				continue;
			}
			if (-1 == xCoor && 0 == yCoor) {
				tapUp(preX, preY);
			} else if(-1 == xCoor && -1 == yCoor) {
				break;
			} else if (-1 == preX && 0 == preY) {
				tapDown(xCoor, yCoor);
			} else {
				tapMove(xCoor, yCoor);
			}
			preX = xCoor;
			preY = yCoor;
		}
	}

	private View.OnClickListener mOnButtonStartHWListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			new Thread(mSendRunnable).start();
		}
	};

	private Runnable mSendRunnable = new Runnable() {

		@Override
		public void run() {
			//��ʼ̽�����뷨
			probeCandidateHeight();

			//TODO: �ڱ�����final����ֵ���������ܻ�ȽϿ��٣�ʹ�ó�Ա�����Ļ���cpu����79%���ֲܿ����мɣ�
			final int configChoice = mChoice;

			mCurCount = 0;
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
						if (inputStr.contains(",") && inputStr.contains(";")) {//������Զ��źͷֺŸ���
							//��ȡ����ͺ���,�����ȡ��������ͺ���,��ô��������case
							String points = "";
							String hanzi = "";
							String cmd = "";
							try {
								String[] caseStrs = inputStr.split("=")[1].split(";");
								points = caseStrs[0];
								hanzi = caseStrs[1];//TODO: hanzi��Unicode����Ҫת��
								if(!hanzi.contains("\\")) {
									hanzi = "\\" + hanzi;
								} else if (hanzi.contains("\\\\")) {
									hanzi = hanzi.replace("\\\\", "\\");
								}
								hanzi = Utils.fromEncodedUnicode(hanzi.toCharArray(), 0, hanzi.length());
								//cmd = caseStrs[2];
							} catch (IndexOutOfBoundsException e) {
								e.printStackTrace();
								continue;
							}
							//�������#�����ǵ�����ģʽ����˵���������Case������ע�ⲻ�����ÿո����������������
							if (inputStr.contains("#") && configChoice == R.id.config_radio_choice_first_screen) {
								SendKey(Keybord.KEYBORD_DELETE_BUTTON);
								for (int i = 0; i < 2; i++)
									SendKey(Keybord.KEYBORD_SPACE_BUTTON);
								for (int i = 0; i < 2; i++)
									SendKey(Keybord.KEYBORD_DELETE_BUTTON);

								mLogcat.read();

							} else if (inputStr.contains("*")) {
								SendKey(Keybord.KEYBORD_DELETE_BUTTON);
								mLogcat.read();
								mCurCount++;
							} else if (inputStr.contains("&") && configChoice == R.id.config_radio_choice_first_screen) {
								sleepMil(100);
								findCompletion(hanzi, GetFilter());
								sleepMil(100);
								mLogcat.read();
								mCurCount++;
							} else {
								//��������������Ϊ��Rerun
								String resultForThisCase = "";
								int TrialCount = 0;
								while(resultForThisCase.equals("") && TrialCount < 2) {
									mLogcat.read();
									sleepMil(50);
									SendString(points);
									//Ϊ�˺���һ������������
									sleepSec(2);
									resultForThisCase = readLogcat(hanzi, hanzi, inputStr, GetFilter());
									TrialCount++;
								}
								resultToWrite += resultForThisCase;
								mCurCount++;
							}
						}
						if (NextCase != null) {
							if (mCurCount % 20 == 0 && !NextCase.contains(",&,")) {
								sleepMil(50);
								for(int i=0;i<30;i++)SendKey(Keybord.KEYBORD_DELETE_BUTTON);
								//mHandler.obtainMessage(MSG_CLEAR_EDITTEXT).sendToTarget();
								//mHandler.obtainMessage(MSG_UPDATE_CUR_COUNT, mCurCount, 0).sendToTarget();
								sleepMil(50);
								new WriteFileThread(getApplicationContext(), resultToWrite.toString()).start();
								resultToWrite = "";
								//��ʼ̽�����뷨
								//probeCandidateHeight();
							}
						}
					}
					//**������case������ϵ�ʱ�򣬻���һ����û�м�¼�꣬��ʱӦ��������β����
					mHandler.obtainMessage(MSG_CLEAR_EDITTEXT).sendToTarget();
					mHandler.obtainMessage(MSG_UPDATE_CUR_COUNT, mCurCount, 0).sendToTarget();
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
}
