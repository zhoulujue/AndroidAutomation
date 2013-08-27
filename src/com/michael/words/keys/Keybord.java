package com.michael.words.keys;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Locale;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.preference.PreferenceManager;

import com.michael.words.R;
import com.michael.words.utils.Utils;

public class Keybord {
	private HashMap<String, TouchPoint> mKeybordMap;

	public int keybordType;
	
	public static String SCREEN_SIZE_1280_720 = "1280x720";
	public static String SCREEN_SIZE_1280_768 = "1280x768";
	public static String SCREEN_SIZE_2560_1600 = "2560x1600";
	public static String SCREEN_SIZE_480_320 = "480x320";

	public static int KEYBORD_MODEL_QWERTY = 0;
	public static int KEYBORD_MODEL_NINE = 1;

	public static String KEYBORD_CANDIDATE_CORD = "cand";
	public static String KEYBORD_DELETE_BUTTON = "dele";
	public static String KEYBORD_SPLIT_BUTTON = "spli";
	public static String KEYBORD_SYMBOL_BUTTON = "symb";
	public static String KEYBORD_NUMBER_BUTTON = "numb";
	public static String KEYBORD_SPACE_BUTTON = "spac";
	public static String KEYBORD_SWITCH_BUTTON = "swit";
	public static String KEYBORD_ENTER_BUTTON = "ente";
	public static String KEYBORD_COMMA_BUTTON = "comm";
	public static String KEYBORD_PERIOD_BUTTON = "peri";

	public Keybord(Context context) throws IOException{
		mKeybordMap = new HashMap<String, TouchPoint>();
		init(context);
	}

	private void init(Context context) throws IOException {
		String line = "";
		BufferedReader reader = wrapperKeybordFromFile(context);

		while((line = reader.readLine()) != null ) {
			String key = line.split(":")[0];
			String xCord = line.split(":")[1].split(",")[0].split("=")[1];
			String yCord = line.split(":")[1].split(",")[1].split("=")[1];

			int xCordInt = Integer.valueOf(xCord);
			int yCordInt = Integer.valueOf(yCord);
			TouchPoint point = new TouchPoint(xCordInt, yCordInt);
			mKeybordMap.put(key, point);
			keybordType = PreferenceManager.getDefaultSharedPreferences(context).getInt("keybord", 0);
		}
	}

	private BufferedReader wrapperKeybordFromFile(Context context) {
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
				int CurKeybordChoice = sharedPreferences.getInt("keybord", 0);

				//¾Å¼ü
				if (CurKeybordChoice == KEYBORD_MODEL_NINE ) 
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
					//1280x726
					else if (ScreenSize.equals(SCREEN_SIZE_1280_768))
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.sogou_9_1280x768)));
					}
					//2560x1600
					else if (ScreenSize.equals(SCREEN_SIZE_2560_1600))
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.sogou_9_2560x1600)));
					}
				} 
				//26¼üÅÌ
				else if (CurKeybordChoice == KEYBORD_MODEL_QWERTY) 
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
					//1280x726
					else if (ScreenSize.equals(SCREEN_SIZE_1280_768)) 
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.sogou_26_1280x768))); 
					}
					//2560x1600
					else if (ScreenSize.equals(SCREEN_SIZE_2560_1600)) 
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.sogou_26_2560x1650)));
					}
				}

			}
		}
		else if (curImeName.contains("baidu")) 
		{
			if (outSize != null) 
			{
				String ScreenSize = String.valueOf(outSize.x) + "x" + String.valueOf(outSize.y);

				SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
				int CurKeybordChoice = sharedPreferences.getInt("keybord", 0);

				//¾Å¼ü
				if (CurKeybordChoice == KEYBORD_MODEL_NINE ) 
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
					//1280x726
					else if (ScreenSize.equals(SCREEN_SIZE_1280_768)) 
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.baidu_9_1280x768))); 
					}
					//2560x1600
					else if (ScreenSize.equals(SCREEN_SIZE_2560_1600)) 
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.baidu_9_2560x1600))); 
					}
				} 
				//26¼üÅÌ
				else if (CurKeybordChoice == KEYBORD_MODEL_QWERTY) 
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
					//1280x726
					else if (ScreenSize.equals(SCREEN_SIZE_1280_768)) 
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.baidu_26_1280x768))); 
					}
					//2560x1600
					else if (ScreenSize.equals(SCREEN_SIZE_2560_1600)) 
					{
						reader = new BufferedReader(
								new InputStreamReader(context.getResources().openRawResource(R.raw.baidu_26_2560x1600))); 
					}
				}

			}
		}
		return reader;

	}

	public TouchPoint getKeyLocation(String key) {
		return mKeybordMap.get(key);
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


