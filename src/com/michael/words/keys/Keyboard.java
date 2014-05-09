package com.michael.words.keys;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Locale;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Point;
import android.preference.PreferenceManager;

import com.michael.words.utils.Utils;

public class Keyboard {
	private HashMap<String, TouchPoint> mKeyboardMap;

	public String KeyboardType;

	public static final String SCREEN_SIZE_1280_720 = "1280x720";
	public static final String SCREEN_SIZE_1280_768 = "1280x768";
	public static final String SCREEN_SIZE_1280_800 = "1280x800";
	public static final String SCREEN_SIZE_2560_1600 = "2560x1600";
	public static final String SCREEN_SIZE_480_320 = "480x320";

	public static final String KEYBOARD_MODEL_QWERTY = "26";
	public static final String KEYBOARD_MODEL_NINE = "9";
	public static final String KEYBOARD_MODEL_HAND_WRITING = "hw";

	public static final String KEYBOARD_CANDIDATE_CORD = "cand";
	public static final String KEYBOARD_DELETE_BUTTON = "dele";
	public static final String KEYBOARD_SPLIT_BUTTON = "spli";
	public static final String KEYBOARD_SYMBOL_BUTTON = "symb";
	public static final String KEYBOARD_NUMBER_BUTTON = "numb";
	public static final String KEYBOARD_SPACE_BUTTON = "spac";
	public static final String KEYBOARD_SWITCH_BUTTON = "swit";
	public static final String KEYBOARD_ENTER_BUTTON = "ente";
	public static final String KEYBOARD_COMMA_BUTTON = "comm";
	public static final String KEYBOARD_PERIOD_BUTTON = "peri";

	public Keyboard(Context context) throws IOException{
		mKeyboardMap = new HashMap<String, TouchPoint>();
		init(context);
	}

	private void init(Context context) throws IOException {
		String line = "";
		//BufferedReader reader = wrapperKeyboardFromFile(context);
		BufferedReader reader = renderKeyBoard(context);
		KeyboardType = PreferenceManager.getDefaultSharedPreferences(context).getString("keyboard", "");
		while((line = reader.readLine()) != null) {
			String key = line.split(":")[0];
			String xCord = line.split(":")[1].split(",")[0].split("=")[1];
			String yCord = line.split(":")[1].split(",")[1].split("=")[1];

			int xCordInt = Integer.valueOf(xCord);
			int yCordInt = Integer.valueOf(yCord);
			TouchPoint point = new TouchPoint(xCordInt, yCordInt);
			mKeyboardMap.put(key, point);
		}
	}
	
	private BufferedReader renderKeyBoard(Context context) {
		BufferedReader reader = null;
		String KeyboardConfigFileName = "";
		
		Point outSize = new Point();
		outSize = Utils.getCurScreenSize(context);
		String ScreenSize = String.valueOf(outSize.x > outSize.y ? outSize.x : outSize.y) 
				+ "x" + String.valueOf(outSize.x > outSize.y ? outSize.y : outSize.x);
		
		String curImeName = Utils.getCurrentImeInfo(context).packageName.toLowerCase(Locale.ENGLISH);
		if (curImeName == null || curImeName.isEmpty() || curImeName.equals("")) {
			Utils.showToast(context, "Ã»ÓÐÄ¬ÈÏµÄÊäÈë·¨£¬ÎÞ·¨½øÐÐÆÀ²â£¡");
			return null;
		}
		
		AssetManager assetManager = context.getAssets();
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		String CurKeyboardChoice = sharedPreferences.getString("keyboard", "");
		if (CurKeyboardChoice == null || CurKeyboardChoice.isEmpty() || CurKeyboardChoice.equals("")) {
			Utils.showToast(context, "Didn't choose keyboard, ÎÞ·¨½øÐÐÆÀ²â£¡");
			return null;
		}
		
		KeyboardConfigFileName = curImeName + "_" + CurKeyboardChoice + "_" + ScreenSize;
		
		try {
			reader = new BufferedReader(new InputStreamReader(assetManager.open(KeyboardConfigFileName)));
		} catch (IOException e) {
			Utils.showToast(context, "Keyboard configuration failed due to the absence of config file£¡");
			e.printStackTrace();
		}
		return reader;
	}
	
/*	private BufferedReader wrapperKeyboardFromFile(Context context) {
		BufferedReader reader = null;
		Point outSize = new Point();
		outSize = Utils.getCurScreenSize(context);

		String curImeName = Utils.getCurrentImeInfo(context).packageName.toLowerCase(Locale.ENGLISH);

		if (curImeName.contains("sogou"))
		{
			if (outSize != null) 
			{
				String ScreenSize = String.valueOf(outSize.x > outSize.y ? outSize.x : outSize.y) 
						+ "x" + String.valueOf(outSize.x > outSize.y ? outSize.y : outSize.x);

				SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
				int CurKeyboardChoice = sharedPreferences.getInt("keyboard", 0);

				//¾Å¼ü
				if (CurKeyboardChoice == KEYBOARD_MODEL_NINE ) 
				{
					//1280x720
					if (ScreenSize.equals(SCREEN_SIZE_1280_720)) 
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.sogou_9_1280x720))); 
					} 
					//480x320
					else if (ScreenSize.equals(SCREEN_SIZE_480_320)) 
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.sogou_9_480x320))); 
					}
					//1280x768
					else if (ScreenSize.equals(SCREEN_SIZE_1280_768))
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.sogou_9_1280x768)));
					}
					//1280x800
					else if (ScreenSize.equals(SCREEN_SIZE_1280_800))
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.sogou_9_1280x800)));
					}
					//2560x1600
					else if (ScreenSize.equals(SCREEN_SIZE_2560_1600))
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.sogou_9_2560x1600)));
					}
				} 
				//26¼üÅÌ
				else if (CurKeyboardChoice == KEYBOARD_MODEL_QWERTY) 
				{
					//1280x720
					if (ScreenSize.equals(SCREEN_SIZE_1280_720)) 
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.sogou_26_1280x720))); 
					} 
					//480x320
					else if (ScreenSize.equals(SCREEN_SIZE_480_320)) 
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.sogou_26_480x320))); 
					}
					//1280x768
					else if (ScreenSize.equals(SCREEN_SIZE_1280_768)) 
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.sogou_26_1280x768))); 
					}
					//1280x800
					else if (ScreenSize.equals(SCREEN_SIZE_1280_800)) 
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.sogou_26_1280x800))); 
					}
					//2560x1600
					else if (ScreenSize.equals(SCREEN_SIZE_2560_1600)) 
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.sogou_26_2560x1650)));
					}
				}
				//ÊÖÐ´¼üÅÌ
				else if (CurKeyboardChoice == KEYBOARD_MODEL_HAND_WRITING) 
				{
					//1280x720
					if (ScreenSize.equals(SCREEN_SIZE_1280_720)) 
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.sogou_hw_1280x720))); 
					} 
					//480x320
					else if (ScreenSize.equals(SCREEN_SIZE_480_320)) 
					{
						//reader = new BufferedReader(
						//		new InputStreamReader(context.getResources().openRawResource(R.raw.sogou_26_480x320))); 
					}
					//1280x768
					else if (ScreenSize.equals(SCREEN_SIZE_1280_768)) 
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.sogou_hw_1280x768))); 
					}
					//1280x800
					else if (ScreenSize.equals(SCREEN_SIZE_1280_800)) 
					{
						//reader = new BufferedReader(
						//		new InputStreamReader(context.getResources().openRawResource(R.raw.sogou_26_1280x800))); 
					}
					//2560x1600
					else if (ScreenSize.equals(SCREEN_SIZE_2560_1600)) 
					{
						//reader = new BufferedReader(
						//		new InputStreamReader(context.getResources().openRawResource(R.raw.sogou_26_2560x1650)));
					}
				}

			}
		}
		else if (curImeName.contains("baidu")) 
		{
			if (outSize != null) 
			{
				String ScreenSize = String.valueOf(outSize.x > outSize.y ? outSize.x : outSize.y) 
						+ "x" + String.valueOf(outSize.x > outSize.y ? outSize.y : outSize.x);

				SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
				int CurKeyboardChoice = sharedPreferences.getInt("keyboard", 0);

				//¾Å¼ü
				if (CurKeyboardChoice == KEYBOARD_MODEL_NINE ) 
				{
					//1280x720
					if (ScreenSize.equals(SCREEN_SIZE_1280_720)) 
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.baidu_9_1280x720))); 
					} 
					//480x320
					else if (ScreenSize.equals(SCREEN_SIZE_480_320)) 
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.baidu_9_480x320))); 
					}
					//1280x768
					else if (ScreenSize.equals(SCREEN_SIZE_1280_768)) 
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.baidu_9_1280x768))); 
					}
					//1280x800
					else if (ScreenSize.equals(SCREEN_SIZE_1280_800))
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.baidu_9_1280x800)));
					}
					//2560x1600
					else if (ScreenSize.equals(SCREEN_SIZE_2560_1600)) 
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.baidu_9_2560x1600))); 
					}
				} 
				//26¼üÅÌ
				else if (CurKeyboardChoice == KEYBOARD_MODEL_QWERTY) 
				{
					//1280x720
					if (ScreenSize.equals(SCREEN_SIZE_1280_720)) 
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.baidu_26_1280x720))); 
					} 
					//480x320
					else if (ScreenSize.equals(SCREEN_SIZE_480_320)) 
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.baidu_26_480x320))); 
					}
					//1280x768
					else if (ScreenSize.equals(SCREEN_SIZE_1280_768)) 
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.baidu_26_1280x768))); 
					}
					//1280x800
					else if (ScreenSize.equals(SCREEN_SIZE_1280_800))
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.baidu_26_1280x800)));
					}
					//2560x1600
					else if (ScreenSize.equals(SCREEN_SIZE_2560_1600)) 
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.baidu_26_2560x1600))); 
					}
				}
				//ÊÖÐ´¼üÅÌ
				else if (CurKeyboardChoice == KEYBOARD_MODEL_HAND_WRITING) 
				{
					//1280x720
					if (ScreenSize.equals(SCREEN_SIZE_1280_720)) 
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.baidu_hw_1280x720))); 
					} 
					//480x320
					else if (ScreenSize.equals(SCREEN_SIZE_480_320)) 
					{
						//reader = new BufferedReader(
						//		new InputStreamReader(context.getResources().openRawResource(R.raw.baidu_hw_480x320))); 
					}
					//1280x768
					else if (ScreenSize.equals(SCREEN_SIZE_1280_768)) 
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.baidu_hw_1280x768))); 
					}
					//1280x800
					else if (ScreenSize.equals(SCREEN_SIZE_1280_800))
					{
						//reader = new BufferedReader(
						//		new InputStreamReader(context.getResources().openRawResource(R.raw.baidu_hw_1280x800)));
					}
					//2560x1600
					else if (ScreenSize.equals(SCREEN_SIZE_2560_1600)) 
					{
						//reader = new BufferedReader(
						//		new InputStreamReader(context.getResources().openRawResource(R.raw.baidu_hw_2560x1600))); 
					}
				}
				
			}
		}
		else if (curImeName.contains("qq")) 
		{
			if (outSize != null) 
			{
				String ScreenSize = String.valueOf(outSize.x > outSize.y ? outSize.x : outSize.y) 
						+ "x" + String.valueOf(outSize.x > outSize.y ? outSize.y : outSize.x);

				SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
				int CurKeyboardChoice = sharedPreferences.getInt("keyboard", 0);

				//¾Å¼ü
				if (CurKeyboardChoice == KEYBOARD_MODEL_NINE ) 
				{
					//1280x720
					if (ScreenSize.equals(SCREEN_SIZE_1280_720)) 
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.qq_9_1280x720))); 
					} 
					//480x320
					else if (ScreenSize.equals(SCREEN_SIZE_480_320)) 
					{
						//reader = new BufferedReader(
						//		new InputStreamReader(context.getResources().openRawResource(R.raw.qq_9_480x320))); 
					}
					//1280x768
					else if (ScreenSize.equals(SCREEN_SIZE_1280_768)) 
					{
						//reader = new BufferedReader(
						//		new InputStreamReader(context.getResources().openRawResource(R.raw.qq_9_1280x768))); 
					}
					//1280x800
					else if (ScreenSize.equals(SCREEN_SIZE_1280_800))
					{
						//reader = new BufferedReader(
						//		new InputStreamReader(context.getResources().openRawResource(R.raw.qq_9_1280x800)));
					}
					//2560x1600
					else if (ScreenSize.equals(SCREEN_SIZE_2560_1600)) 
					{
						//reader = new BufferedReader(
						//		new InputStreamReader(context.getResources().openRawResource(R.raw.qq_9_2560x1600))); 
					}
				} 
				//26¼üÅÌ
				else if (CurKeyboardChoice == KEYBOARD_MODEL_QWERTY) 
				{
					//1280x720
					if (ScreenSize.equals(SCREEN_SIZE_1280_720)) 
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.qq_26_1280x720))); 
					} 
					//480x320
					else if (ScreenSize.equals(SCREEN_SIZE_480_320)) 
					{
						//reader = new BufferedReader(
						//		new InputStreamReader(context.getResources().openRawResource(R.raw.qq_26_480x320))); 
					}
					//1280x768
					else if (ScreenSize.equals(SCREEN_SIZE_1280_768)) 
					{
						//reader = new BufferedReader(
						//		new InputStreamReader(context.getResources().openRawResource(R.raw.qq_26_1280x768))); 
					}
					//1280x800
					else if (ScreenSize.equals(SCREEN_SIZE_1280_800))
					{
						//reader = new BufferedReader(
						//		new InputStreamReader(context.getResources().openRawResource(R.raw.qq_26_1280x800)));
					}
					//2560x1600
					else if (ScreenSize.equals(SCREEN_SIZE_2560_1600)) 
					{
						//reader = new BufferedReader(
						//		new InputStreamReader(context.getResources().openRawResource(R.raw.qq_26_2560x1600))); 
					}
				}
				//ÊÖÐ´¼üÅÌ
				else if (CurKeyboardChoice == KEYBOARD_MODEL_HAND_WRITING) 
				{
					//1280x720
					if (ScreenSize.equals(SCREEN_SIZE_1280_720)) 
					{
						//reader = new BufferedReader(
						//		new InputStreamReader(context.getResources().openRawResource(R.raw.qq_26_1280x720))); 
					} 
					//480x320
					else if (ScreenSize.equals(SCREEN_SIZE_480_320)) 
					{
						//reader = new BufferedReader(
						//		new InputStreamReader(context.getResources().openRawResource(R.raw.qq_26_480x320))); 
					}
					//1280x768
					else if (ScreenSize.equals(SCREEN_SIZE_1280_768)) 
					{
						//reader = new BufferedReader(
						//		new InputStreamReader(context.getResources().openRawResource(R.raw.qq_26_1280x768))); 
					}
					//1280x800
					else if (ScreenSize.equals(SCREEN_SIZE_1280_800))
					{
						//reader = new BufferedReader(
						//		new InputStreamReader(context.getResources().openRawResource(R.raw.qq_26_1280x800)));
					}
					//2560x1600
					else if (ScreenSize.equals(SCREEN_SIZE_2560_1600)) 
					{
						//reader = new BufferedReader(
						//		new InputStreamReader(context.getResources().openRawResource(R.raw.qq_26_2560x1600))); 
					}
				}

			}
		}
		else if (curImeName.contains("iflytek")) 
		{
			if (outSize != null) 
			{
				String ScreenSize = String.valueOf(outSize.x > outSize.y ? outSize.x : outSize.y) 
						+ "x" + String.valueOf(outSize.x > outSize.y ? outSize.y : outSize.x);

				SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
				int CurKeyboardChoice = sharedPreferences.getInt("keyboard", 0);

				//¾Å¼ü
				if (CurKeyboardChoice == KEYBOARD_MODEL_NINE ) 
				{
					//1280x720
					if (ScreenSize.equals(SCREEN_SIZE_1280_720)) 
					{
						//reader = new BufferedReader(
						//		new InputStreamReader(context.getResources().openRawResource(R.raw.qq_9_1280x720))); 
					} 
					//480x320
					else if (ScreenSize.equals(SCREEN_SIZE_480_320)) 
					{
						//reader = new BufferedReader(
						//		new InputStreamReader(context.getResources().openRawResource(R.raw.qq_9_480x320))); 
					}
					//1280x768
					else if (ScreenSize.equals(SCREEN_SIZE_1280_768)) 
					{
						//reader = new BufferedReader(
						//		new InputStreamReader(context.getResources().openRawResource(R.raw.qq_9_1280x768))); 
					}
					//1280x800
					else if (ScreenSize.equals(SCREEN_SIZE_1280_800))
					{
						//reader = new BufferedReader(
						//		new InputStreamReader(context.getResources().openRawResource(R.raw.qq_9_1280x800)));
					}
					//2560x1600
					else if (ScreenSize.equals(SCREEN_SIZE_2560_1600)) 
					{
						//reader = new BufferedReader(
						//		new InputStreamReader(context.getResources().openRawResource(R.raw.qq_9_2560x1600))); 
					}
				} 
				//26¼üÅÌ
				else if (CurKeyboardChoice == KEYBOARD_MODEL_QWERTY) 
				{
					//1280x720
					if (ScreenSize.equals(SCREEN_SIZE_1280_720)) 
					{
						//reader = new BufferedReader(
						//		new InputStreamReader(context.getResources().openRawResource(R.raw.qq_26_1280x720))); 
					} 
					//480x320
					else if (ScreenSize.equals(SCREEN_SIZE_480_320)) 
					{
						//reader = new BufferedReader(
						//		new InputStreamReader(context.getResources().openRawResource(R.raw.qq_26_480x320))); 
					}
					//1280x768
					else if (ScreenSize.equals(SCREEN_SIZE_1280_768)) 
					{
						//reader = new BufferedReader(
						//		new InputStreamReader(context.getResources().openRawResource(R.raw.qq_26_1280x768))); 
					}
					//1280x800
					else if (ScreenSize.equals(SCREEN_SIZE_1280_800))
					{
						//reader = new BufferedReader(
						//		new InputStreamReader(context.getResources().openRawResource(R.raw.qq_26_1280x800)));
					}
					//2560x1600
					else if (ScreenSize.equals(SCREEN_SIZE_2560_1600)) 
					{
						//reader = new BufferedReader(
						//		new InputStreamReader(context.getResources().openRawResource(R.raw.qq_26_2560x1600))); 
					}
				}
				//ÊÖÐ´¼üÅÌ
				else if (CurKeyboardChoice == KEYBOARD_MODEL_HAND_WRITING) 
				{
					//1280x720
					if (ScreenSize.equals(SCREEN_SIZE_1280_720)) 
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.iflytek_hw_1280x720))); 
					} 
					//480x320
					else if (ScreenSize.equals(SCREEN_SIZE_480_320)) 
					{
						//reader = new BufferedReader(
						//		new InputStreamReader(context.getResources().openRawResource(R.raw.qq_26_480x320))); 
					}
					//1280x768
					else if (ScreenSize.equals(SCREEN_SIZE_1280_768)) 
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.iflytek_hw_1280x768))); 
					}
					//1280x800
					else if (ScreenSize.equals(SCREEN_SIZE_1280_800))
					{
						//reader = new BufferedReader(
						//		new InputStreamReader(context.getResources().openRawResource(R.raw.qq_26_1280x800)));
					}
					//2560x1600
					else if (ScreenSize.equals(SCREEN_SIZE_2560_1600)) 
					{
						//reader = new BufferedReader(
						//		new InputStreamReader(context.getResources().openRawResource(R.raw.qq_26_2560x1600))); 
					}
				}

			}
		}
		else if(curImeName.contains("scutgpen")) {
			reader = new BufferedReader(
					new InputStreamReader(context.getResources().openRawResource(R.raw.scutgpen_hw_1280x768)));
		}
		else if(curImeName.contains("gbime")) {
			reader = new BufferedReader(
					new InputStreamReader(context.getResources().openRawResource(R.raw.gbime_hw_1280x768)));
		}
		return reader;

	}*/

	public TouchPoint getKeyLocation(String key) {
		return mKeyboardMap.get(key);
	}

	public class TouchPoint {
		public float x;
		public float y;

		public TouchPoint(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

}


