package com.michael.words;

import java.io.File;
import java.io.IOException;

import com.michael.words.utils.Utils;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;

public class HandleLastRunActivity extends BaseActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_handle_last);
		init();
	}

	private void init() {
		Button deleteResultBtn = (Button) findViewById(R.id.button_delete_result);
		deleteResultBtn.setOnClickListener(mOnDeleteListener);

		Button uploadResultBtn = (Button) findViewById(R.id.button_upload_result);
		uploadResultBtn.setOnClickListener(mOnUploadListener);
		
		Button continueLastButton = (Button) findViewById(R.id.button_continue_last);
		continueLastButton.setOnClickListener(mOnContinueLastListener);
	}

	private View.OnClickListener mOnDeleteListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			File localFile = new File(getFilesDir().getPath() + "/" + "result.txt");
			localFile.delete();
			startActivity(new Intent(HandleLastRunActivity.this, ConfigActivity.class));
			try {
				localFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finish();
		}
	};

	private View.OnClickListener mOnUploadListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			File localFile = new File(getFilesDir().getPath() + "/" + "result.txt");
			Thread upload = new Thread(uploadToFtp);
			upload.start();
			try {
				upload.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			//�ļ����ڣ�ʧ�ܣ����öԻ�����еڶ��γ���
			if(localFile.exists()){
				dialog();
			} else {
				//�ϴ��ɹ����ѱ���޸Ĺ�������ɾ���ļ�����ת����
				SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
				editor.putBoolean("LastRunSuccess", true);
				editor.commit();
				localFile.delete();
				startActivity(new Intent(HandleLastRunActivity.this, ConfigActivity.class));
				finish();
			}
		}
	};

	private View.OnClickListener mOnContinueLastListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			//�ϴ��ɹ����ѱ���޸Ĺ�������ɾ���ļ�����ת����
			SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
			editor.putBoolean("LastRunSuccess", false);
			editor.commit();
			startActivity(new Intent(HandleLastRunActivity.this, ConfigActivity.class));
			finish();
		}
	};
	
	protected void dialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(HandleLastRunActivity.this);
		builder.setCancelable(false);
		builder.setMessage(R.string.upload_result_retry);
		builder.setTitle(R.string.upload_result_failed);
		builder.setPositiveButton(R.string.upload_result_btn_retry, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				File localFile = new File(getFilesDir().getPath() + "/" + "result.txt");
				Thread upload = new Thread(uploadToFtp);
				upload.start();
				try {
					upload.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				//�ļ������ڣ��ɹ�����ʾ���޸ı�ǡ�ɾ�����ؽ���ļ�������ת
				if(!localFile.exists()){
					//�ϴ��ɹ����ѱ���޸Ĺ�������ɾ���ļ�����ת����
					SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
					editor.putBoolean("LastRunSuccess", true);
					editor.commit();
					localFile.delete();
					startActivity(new Intent(HandleLastRunActivity.this, ConfigActivity.class));
					finish();
				}
				dialog.dismiss();
			}
		});
		builder.setNegativeButton(R.string.upload_result_btn_delete, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				File localFile = new File(getFilesDir().getPath() + "/" + "result.txt");
				localFile.delete();
				startActivity(new Intent(HandleLastRunActivity.this, ConfigActivity.class));
				finish();
			}
		});
		builder.create().show();
	}

	private Runnable uploadToFtp = new Runnable() {

		@Override
		public void run() {
			if (!Utils.uploadFile(getApplicationContext(), "10.129.41.70", "imetest", "Sogou7882Imeqa", 
					"/WordCrawler", "result.txt")){
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Utils.showToast(getApplicationContext(), R.string.upload_result_fail_warn);
					}
				});
			} else {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Utils.showToast(getApplicationContext(), R.string.upload_success);
					}
				});
				//�ϴ��ɹ����ѱ���޸Ĺ�������ɾ���ļ�
				SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
				editor.putBoolean("LastRunSuccess", true);
				editor.commit();
				File localFile = new File(getFilesDir().getPath() + "/" + "result.txt");
				localFile.delete();
			}
		}
	};

}
