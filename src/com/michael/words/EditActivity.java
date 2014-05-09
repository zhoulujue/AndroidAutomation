package com.michael.words;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.os.Bundle;
import android.util.Log;

import com.michael.shell.Shell;

public class EditActivity extends BaseActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		copyAssetFile("gram.bin", "/data/data/com.baidu.input/files/gram.bin");
		try {
			Shell rootShell = new Shell("su");
			sleepSec(2);
			rootShell.write("chmod 777 /data/data/com.baidu.input/files/gram.bin");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		super.onCreate(savedInstanceState);
		SetFilter(", type=String");
	}

	private void copyAssetFile(String pathInAsset, String toPath) {
		try {
			InputStream ins = getAssets().open(pathInAsset);
			File tempFile = createFileFromInputStream(ins);
			String pathToTempFile = tempFile.getAbsolutePath();
			
			String cmd = "cp " + pathToTempFile + " " + toPath + "\n";
			
			Shell rootShell = new Shell("su");
			
			rootShell.write("id");
			String echo = rootShell.read();
			
			rootShell.write(cmd);
			echo = rootShell.read();
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private File createFileFromInputStream(InputStream inputStream) {
		try{
			File f = new File(getFilesDir().getPath() + "/" + "temp");
			if (f.exists()) {
				f.delete();
				f.createNewFile();
			}
			OutputStream outputStream = new FileOutputStream(f);
			byte buffer[] = new byte[1024];
			int length = 0;

			while((length=inputStream.read(buffer)) > 0) {
				outputStream.write(buffer,0,length);
			}

			outputStream.close();
			inputStream.close();

			return f;
		}catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

}
