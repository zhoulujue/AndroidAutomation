package com.michael.words;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.michael.words.OverlayView.OnTouchFinishListener;
import com.michael.words.keys.Keyboard;
import com.michael.words.utils.Utils;

public class AdaptForKeyboradLayout extends Activity {

	private static final String pointsArrayForProbe =
			"305,494,338,489,537,447,611,433,639,427,661,423,676,420,684,418,686,418,-1,0,"
					+ "282,694,345,678,398,663,454,646,507,632,550,619,588,609,616,601,638,596,652,594,656,594,-1,0,"
					+ "254,879,344,854,418,837,493,817,554,797,604,778,645,761,674,747,693,735,703,725,707,716,707,716,-1,0,"
					+ "477,496,485,617,493,697,499,790,504,877,502,945,495,996,484,1046,470,1077,455,1094,427,1106,391,1105,"
					+ "353,1090,310,1053,278,1014,278,1014,-1,0,"
					+ "-1,-1";
	private int mCurKeyboardChoice;
	private RadioGroup mKeyboardTypeRadioGroup;
	private TextView mTextView;
	private EditText mEditText;
	private boolean AdaptDone = false;
	private WindowManager mWindowManager;
	private OverlayView mOverlayView;
	private Map<String, Point> mCoordsOfKeys;
	private int mCurKeysIndex = 0;;
	private String[] mKeysOnKeyboard;
	private WindowManager.LayoutParams mLayoutParamsOfOverlayView;
	private Instrumentation mInstrumentation;
	
	public static final String[] KEYBOARD_ALL_HW_KEY = {
		Keyboard.KEYBOARD_CANDIDATE_CORD,
		Keyboard.KEYBOARD_DELETE_BUTTON,
		Keyboard.KEYBOARD_SPLIT_BUTTON,
		Keyboard.KEYBOARD_SYMBOL_BUTTON,
		Keyboard.KEYBOARD_NUMBER_BUTTON,
		Keyboard.KEYBOARD_SPACE_BUTTON,
		Keyboard.KEYBOARD_SWITCH_BUTTON,
		Keyboard.KEYBOARD_ENTER_BUTTON,
		Keyboard.KEYBOARD_COMMA_BUTTON,
		Keyboard.KEYBOARD_PERIOD_BUTTON
	};

	public static final String[] KEYBOARD_ALL_NINE_KEY = {
		Keyboard.KEYBOARD_CANDIDATE_CORD,
		"1", "2", "3", "4", "5", "6", "7", "8", "9",
		Keyboard.KEYBOARD_DELETE_BUTTON,
		Keyboard.KEYBOARD_SPLIT_BUTTON,
		Keyboard.KEYBOARD_SYMBOL_BUTTON,
		Keyboard.KEYBOARD_NUMBER_BUTTON,
		Keyboard.KEYBOARD_SPACE_BUTTON,
		Keyboard.KEYBOARD_SWITCH_BUTTON,
		Keyboard.KEYBOARD_ENTER_BUTTON,
		Keyboard.KEYBOARD_COMMA_BUTTON,
		Keyboard.KEYBOARD_PERIOD_BUTTON
	};

	public static final String[] KEYBOARD_ALL_QWERTY_KEY = {
		Keyboard.KEYBOARD_CANDIDATE_CORD,
		"a", "b", "c", "d", "e", "f", "g", "h",
		"i", "j", "k", "l", "m", "n", "o", "p",
		"q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
		Keyboard.KEYBOARD_DELETE_BUTTON,
		Keyboard.KEYBOARD_SPLIT_BUTTON,
		Keyboard.KEYBOARD_SYMBOL_BUTTON,
		Keyboard.KEYBOARD_NUMBER_BUTTON,
		Keyboard.KEYBOARD_SPACE_BUTTON,
		Keyboard.KEYBOARD_SWITCH_BUTTON,
		Keyboard.KEYBOARD_ENTER_BUTTON,
		Keyboard.KEYBOARD_COMMA_BUTTON,
		Keyboard.KEYBOARD_PERIOD_BUTTON
	};

	public class WriteKeyboardConfigFileThread extends Thread {

		private Context mContext;
		private String mConfigString;
		private final String SDCARD_PATH =  Environment.getExternalStorageDirectory().getPath();
		private String mConfigFilePath;

		public WriteKeyboardConfigFileThread(Context context, Map<String, Point> CoordsOfKeys) {
			this.mContext = context;

			this.mConfigString = "";
			Iterator<java.util.Map.Entry<String, Point>> iterator = CoordsOfKeys.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, Point> entry = (Map.Entry<String, Point>) iterator.next();
				//q:x=111,y=111
				Point tmpPoint = entry.getValue();
				mConfigString += entry.getKey() + ":" + "x=" + tmpPoint.x + ",y=" + tmpPoint.y + "\n";
			}

			String curImeName = Utils.getCurrentImeInfo(context).packageName.toLowerCase(Locale.ENGLISH);
			Point outSize = new Point();
			outSize = Utils.getCurScreenSize(context);
			String ScreenSize = String.valueOf(outSize.x > outSize.y ? outSize.x : outSize.y) 
					+ "x" + String.valueOf(outSize.x > outSize.y ? outSize.y : outSize.x);
			mConfigFilePath = 
					SDCARD_PATH + "/" +
							curImeName + "_" + Keyboard.KEYBOARD_MODEL_STR_ARRAY[mCurKeyboardChoice] + "_" + ScreenSize;
		}

		@Override
		public void run() {
			if (mConfigString.toString().isEmpty() || mConfigString.toString().equals("")) {
				return;
			}

			try {
				File KeyboardConfigFile = new File(mConfigFilePath);
				if (!KeyboardConfigFile.exists()) {
					KeyboardConfigFile.createNewFile();
				} else {
					KeyboardConfigFile.delete();
				}
				FileOutputStream os = new FileOutputStream(KeyboardConfigFile);
				os.write(mConfigString.getBytes("UTF-8"));
				os.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			super.run();
			super.run();
		}

	}

	private RadioGroup.OnCheckedChangeListener mOnCheckedChangeListener = new RadioGroup.OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			//User changed keyboard type
			boolean UserPicked = false;
			switch (checkedId) {
			case R.id.adapt_radio_keyboard_hand_writing:
				mCurKeyboardChoice = Keyboard.KEYBOARD_MODEL_HAND_WRITING;
				UserPicked = true;
				break;
			case R.id.adapt_radio_keyboard_nine:
				mCurKeyboardChoice = Keyboard.KEYBOARD_MODEL_NINE;
				UserPicked = true;
				break;
			case R.id.adapt_radio_keyboard_qwerty:
				mCurKeyboardChoice = Keyboard.KEYBOARD_MODEL_QWERTY;
				UserPicked = true;
				break;
			default:
				UserPicked = false;
				break;
			}
			if (mCurKeyboardChoice == Keyboard.KEYBOARD_MODEL_QWERTY) {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			} else {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			}
			if (UserPicked) {
				//开始适配，标记为“未完成适配”
				AdaptDone = false;
				//清空所有中间变量，并且记录坐标到文件
				reset();
				//适配过程中，不让切换键盘
				group.setEnabled(false);
				//开始适配，设置OverlayView可以点击
				//应该是在点击编辑框后,键盘弹起,然后开始适配
				//startAdapt();
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);      
		getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN, WindowManager.LayoutParams. FLAG_FULLSCREEN);
		setContentView(R.layout.adapt);
		
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		mWindowManager = getWindowManager();
		
		mCoordsOfKeys = new HashMap<String, Point>();
		mCurKeysIndex = 0;

		mInstrumentation = new Instrumentation();
		
		mOverlayView = new OverlayView(this);
		mLayoutParamsOfOverlayView = new LayoutParams();
		mLayoutParamsOfOverlayView.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
				| WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
		mLayoutParamsOfOverlayView.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
				| WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
		mLayoutParamsOfOverlayView.gravity = Gravity.BOTTOM | Gravity.LEFT;
		mLayoutParamsOfOverlayView.width = WindowManager.LayoutParams.MATCH_PARENT;
		mLayoutParamsOfOverlayView.format = PixelFormat.RGBA_8888;
		Display display = mWindowManager.getDefaultDisplay();
		Point outSize = new Point();
		display.getRealSize(outSize);
		mLayoutParamsOfOverlayView.height = outSize.y;
		mWindowManager.addView(mOverlayView, mLayoutParamsOfOverlayView);
		
		mOverlayView.setOnTouchFinishListener(mOnTouchFinishListener);
		init();
	}
	
	private void init() {
		mTextView = (TextView) findViewById(R.id.adapt_textview);
		mTextView.setText(R.string.adapt_choose_keyboard_type);

		mEditText = (EditText) findViewById(R.id.adapt_edittext);
		mEditText.setOnClickListener(mOnEditTextClickListener);
		
		mCoordsOfKeys.clear();

		mKeyboardTypeRadioGroup = (RadioGroup) findViewById(R.id.adapt_radioGroup_keyboard);
		mKeyboardTypeRadioGroup.clearCheck();
		mKeyboardTypeRadioGroup.setOnCheckedChangeListener(mOnCheckedChangeListener);
	}

	private void setOverlayViewStartRecord(boolean record) {
		if (record) {
			mLayoutParamsOfOverlayView.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
					| WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
		} else {
			mLayoutParamsOfOverlayView.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
					| WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
					| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
		}
		//mOverlayView.setLayoutParams(mLayoutParamsOfOverlayView);
		mWindowManager.updateViewLayout(mOverlayView, mLayoutParamsOfOverlayView);
	}
	
	private void reset() {
		mCoordsOfKeys.clear();
		mCurKeysIndex = 0;
		AdaptDone = false;
		mTextView.setText(R.string.adapt_choose_keyboard_type);
	}


	private void startAdapt() {
		setOverlayViewStartRecord(true);
		switch (mCurKeyboardChoice) {
		case Keyboard.KEYBOARD_MODEL_HAND_WRITING:
			mKeysOnKeyboard = KEYBOARD_ALL_HW_KEY;
			break;
		case Keyboard.KEYBOARD_MODEL_NINE:
			mKeysOnKeyboard = KEYBOARD_ALL_NINE_KEY;
			break;
		case Keyboard.KEYBOARD_MODEL_QWERTY:
			mKeysOnKeyboard = KEYBOARD_ALL_QWERTY_KEY;
			break;
		default:
			break;
		}
		if (mKeysOnKeyboard == null) {
			Utils.showToast(getApplicationContext(), R.string.adapt_have_you_choosed_keyboard_type);
			reset();
			return;
		}
		String textToBeShown = 	
				getString(R.string.adapt_status_prefix) + 
				Keyboard.KEYBOARD_MODEL_STR_ARRAY[mCurKeyboardChoice] + 
				getString(R.string.adapt_status_suffix) +
				"\n";
//		mTextView.setText(
//				textToBeShown + 					
//				getString(R.string.adapt_guide_prefix) + 
//				mKeysOnKeyboard[0] + 
//				getString(R.string.adapt_guide_suffix)
//				);
		mTextView.setText(textToBeShown + getString(R.string.adapt_guide_candidate_measure));
	}

	private void stopAdapt() {
		setOverlayViewStartRecord(false);
		mTextView.setText(getString(R.string.adapt_guide_complete));
	}

	private OnClickListener mOnEditTextClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			startAdapt();
		}
	};
	
	private OnTouchFinishListener mOnTouchFinishListener = new OnTouchFinishListener() {

		@Override
		public void onTouchFinish() {
			if (AdaptDone)
				return;
			if (mKeysOnKeyboard == null) {
				Utils.showToast(getApplicationContext(), R.string.adapt_have_you_choosed_keyboard_type);
				reset();
				return;
			}
			if (mCurKeysIndex < mKeysOnKeyboard.length) {
				Point tmpPoint = new Point();
				tmpPoint = mOverlayView.getCoord();
				mCoordsOfKeys.put(mKeysOnKeyboard[mCurKeysIndex], tmpPoint);
				//适配完毕
				if (mCurKeysIndex == mKeysOnKeyboard.length - 1) {
					AdaptDone = true;
					stopAdapt();
					if (!mCoordsOfKeys.isEmpty() && mCoordsOfKeys.size() > 0) {
						new WriteKeyboardConfigFileThread(getApplicationContext(), mCoordsOfKeys).start();
					}
					RadioGroup group = (RadioGroup) findViewById(R.id.adapt_radioGroup_keyboard);
					group.setEnabled(true);
					return;
				}
				mCurKeysIndex++;
				String textToBeShown = 	
						getString(R.string.adapt_status_prefix) + 
						Keyboard.KEYBOARD_MODEL_STR_ARRAY[mCurKeyboardChoice] + 
						getString(R.string.adapt_status_suffix) +
						"\n";
				mTextView.setText(
						textToBeShown + 					
						getString(R.string.adapt_guide_prefix) + 
						mKeysOnKeyboard[mCurKeysIndex] + 
						getString(R.string.adapt_guide_suffix)
						);
			} else {
				Utils.showToast(getApplicationContext(), "Index is over max length!");
				reset();
				return;
			}
		}
	};
	
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
	
	protected void SendStringForHW(String points) throws IOException {
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
}
