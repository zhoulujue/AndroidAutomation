package com.michael.words;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import android.content.Context;

public class WriteFileThread extends Thread {
	private Context mContext;
	private String mResult;
	
	public WriteFileThread(Context context, String result) {
		mContext = context;
		mResult = result;
	}
		
	@Override
	public void run() {
		if (mResult.toString().isEmpty() || mResult.toString().equals("")) {
			return;
		}

		try {
			FileOutputStream os = mContext.openFileOutput("result.txt", Context.MODE_APPEND);
			os.write(mResult.getBytes("UTF-8"));
			os.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		super.run();
	}
	
	
}
