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
		//根据不同的输入法，设置不同的过滤
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
		//清空输入流
		double singleCtrlHeight = 0;
		try {
			mLogcat.read();
			sleepSec(10);
			//发送探测拼音串
			SendString(pointsArrayForProbe);
			sleepSec(3);
			
			String rawResult = null;
			rawResult = mLogcat.read();
			sleepSec(1);

			if (rawResult.equals("") || rawResult == null){
				finish();
				return;
			} else {
				//计算候选的#y:后面的值（大多数字符所在的位置）
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
				//如果当前的坐标（x,y）解析错误，那么抛弃这个点，当它不存在
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
			//开始探测输入法
			probeCandidateHeight();

			//TODO: 在本地用final记下值，这样性能会比较快速，使用成员变量的话，cpu会上79%，很恐怖，切忌！
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

				//每个case文件跑一遍
				for(File rawconfig : rawFiles) {
					BufferedReader reader = new BufferedReader(new FileReader(rawconfig));
					BufferedReader shadowReader = new BufferedReader(new FileReader(rawconfig));
					shadowReader.readLine();
					//check 是否是rerun，恢复现场
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

					//清空中间结果
					mCurCount = 0;
					resultToWrite = "";

					while ((inputStr = reader.readLine()) != null) {
						String NextCase = shadowReader.readLine();
						//暂停功能暂时采用死循环实现，死循环会把CPU带上去，这样不好
						//TODO: 不断访问成员变量，这样也会把CPU带上去
						while(mPause){};
						if (inputStr.contains(",") && inputStr.contains(";")) {//如果是以逗号和分号隔开
							//提取点阵和汉字,如果提取不到点阵和汉字,那么抛弃这条case
							String points = "";
							String hanzi = "";
							String cmd = "";
							try {
								String[] caseStrs = inputStr.split("=")[1].split(";");
								points = caseStrs[0];
								hanzi = caseStrs[1];//TODO: hanzi是Unicode，需要转换
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
							//如果遇到#号且是第三种模式，则说明遇到清空Case，但是注意不能先敲空格，那样会把联想上屏
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
								//这两个参数都是为了Rerun
								String resultForThisCase = "";
								int TrialCount = 0;
								while(resultForThisCase.equals("") && TrialCount < 2) {
									mLogcat.read();
									sleepMil(50);
									SendString(points);
									//为了和下一次输入间隔开来
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
								//开始探测输入法
								//probeCandidateHeight();
							}
						}
					}
					//**当所有case运行完毕的时候，还有一部分没有记录完，此时应该做好收尾工作
					mHandler.obtainMessage(MSG_CLEAR_EDITTEXT).sendToTarget();
					mHandler.obtainMessage(MSG_UPDATE_CUR_COUNT, mCurCount, 0).sendToTarget();
					new WriteFileThread(getApplicationContext(), resultToWrite.toString()).run();
					//关闭case文件的输入流
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
