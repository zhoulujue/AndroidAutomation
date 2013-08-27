package com.michael.words.utils;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import android.content.Context;

public abstract class AbstractTool implements ToolsConstants{

	protected boolean succTag=false;
	
	protected boolean createFileFromAsset(String paramString, Context context) {
		try {
			File file = context.getFilesDir();
			File tempFile = new File(file.getAbsoluteFile().toString()
					+ File.separator + paramString);
			if (tempFile.exists()) {
				return false;
			}

			InputStream localInputStream = context.getAssets()
					.open(paramString);
			FileOutputStream localFileOutputStream = context.openFileOutput(
					paramString, 2);
			byte[] arrayOfByte = new byte[1000];
			while (true) {
				int i = localInputStream.read(arrayOfByte);
				if (i == -1) {
					localInputStream.close();
					localFileOutputStream.close();
					break;
				}
				localFileOutputStream.write(arrayOfByte, 0, i);
			}
			arrayOfByte = null;
			succTag=chmodToolbox(paramString, context);
			Utils.sleep(1);
		} catch (Exception localException) {
			return false;
		}
		return true;
	}
	
	public boolean getTag(){
		return succTag;
	}

	protected boolean chmodToolbox(String paramString, Context context) {
		try {
			String cmd[] = { "chmod 777 /data/data/" + context.getPackageName()
					+ "/files/" + paramString + "\n" };
			consoleExec(cmd);
		} catch (Exception ex) {

		}
		return true;
	}

	protected void consoleExec(String[] cmd) throws IOException {
		Process process = Runtime.getRuntime().exec("su");
		DataOutputStream os = new DataOutputStream(process.getOutputStream());
		for (int i = 0; i < cmd.length; i++) {
			os.writeBytes(cmd[i] + "\n");
		}
		os.writeBytes("exit\n");
		os.flush();
		os.close();
	}

	public abstract boolean doAction(String tag, String value);

}
