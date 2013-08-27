package com.michael.words.utils;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

public class InstallTool extends AbstractTool {

	private static final String UNINSTALL_ACTION_CMD = " exec app_process /system/bin sogou.installpackage.jar.StartMain uninstall ";
	private static final String INSTALL_ACTION_CMD = " exec app_process /system/bin sogou.installpackage.jar.StartMain reinstall ";
	private static final String CLEAR_ACTION_CMD = " exec app_process /system/bin sogou.installpackage.jar.StartMain clear ";
	private String exportClassPath;

	public InstallTool(Context context) {
		if (context == null) {
			return;
		}

		Log.i("file", "install file 2");
		this.createFileFromAsset(INSTALLER, context);
		exportClassPath = "export CLASSPATH=/data/data/"
				+ context.getPackageName() + "/files/installpackagejar.jar";
	}

	@Override
	public boolean doAction(String tag, String value) {
		if (tag == null)
			return false;
		if (tag.equals(INSTALL)) {
			boolean installTag=false;
			File temp = new File(value);
			Log.i("install", "value->"+value);
			if (!temp.exists()){
				Log.i("install", "->file not exist");
				return false;
			}
				
			Log.i("install", "->install app");
			String cmd[] = { exportClassPath, INSTALL_ACTION_CMD + value };
			try {
				consoleExec(cmd);				
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			} 
			long start=SystemClock.uptimeMillis();
			
			while(!installTag){
				Utils.sleep(1);
				if(SystemClock.uptimeMillis()>(start+10000))
					break;
			}
			
			if(!installTag){
				Log.i("install", "install tag 2="+installTag);
				String cmd2[] = {"cp -f \""+value+"\" /data/app" };
				try {
					consoleExec(cmd2);				
				} catch (IOException e) {
					e.printStackTrace();
					return false;
				}
			}
			
			//BootReceiver.tag=false;
			start=SystemClock.uptimeMillis();
			while(!installTag){
				Utils.sleep(1);
				if(SystemClock.uptimeMillis()>(start+10000))
					break;
			}
			
			if(!installTag){
				Log.i("install", "install tag 3="+installTag);
				String name=value.split("/")[value.split("/").length-1];
				String cmd2[] = {"cat \""+value+"\" > \"/data/app/"+name+"\"" };
				try {
					consoleExec(cmd2);				
				} catch (IOException e) {
					e.printStackTrace();
					return false;
				}
			}
			
			start=SystemClock.uptimeMillis();
			Log.i("install", "install 3 start="+start);
			while(!installTag){
				Utils.sleep(1);
				if(SystemClock.uptimeMillis()>(start+10000))
					break;
			}
			Log.i("install", "install 3 end="+SystemClock.uptimeMillis());
			
			return installTag;
		} else if (tag.equals(UNINSTALL)) {
			String cmd[] = { exportClassPath, UNINSTALL_ACTION_CMD + value };
			try {
				consoleExec(cmd);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			
			return true;
		}
		else if(tag.equals(CLEAR)){
			String cmd[] = { exportClassPath, CLEAR_ACTION_CMD + value };
			try {
				consoleExec(cmd);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			
			return true;
		}
		return false;
	}

}
