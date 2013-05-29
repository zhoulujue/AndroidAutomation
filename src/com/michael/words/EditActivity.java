package com.michael.words;

import it.sauronsoftware.ftp4j.FTPAbortedException;
import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferException;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

import com.michael.shell.NativeShell;
import com.michael.shell.Shell;

public class EditActivity extends Activity {
	private EditTextView mEditView;
	private Shell mLogcat;
	private NativeShell mInputShell;
	private NativeShell mSendChoice;
	
	private ArrayList<String> input;
	private int mCurIndex;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit);
		init();
		try {
			Thread download = new Thread(new Runnable() {

				@Override
				public void run() {
					downloadFile("10.129.41.70", "imetest", "Sogou7882Imeqa", "/WordCrawler", "raw.config");
				}
			});
			download.start();
			download.join();
			File rawFile = new File(getFilesDir() + "/" + "raw.config");

			if(!rawFile.exists()) {
				Utils.showToast(getApplicationContext(), "û�л�ȡ�������ļ����˳���");
				finish();
			}

			input = Utils.ReadFromFile.readFileByLines(getFilesDir() + "/" + "raw.config");

			mLogcat = new Shell("su");
			sleep(2);
			mLogcat.write("logcat CanvasDrawText:E *:S");

			mInputShell = new NativeShell();
			sleep(2);

			mSendChoice = new NativeShell();
			sleep(2);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void onBackPressed() {
		new Thread(uploadToFtp).start();
		super.onBackPressed();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_CTRL_LEFT) {
			Log.e("reading", "#############"  + "reading" + "#############");
			readLogcat();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	private void init() {
		mEditView = (EditTextView) findViewById(R.id.editText1);
		//mEditView.setOnKeyListener(mOnLeftCTRListener);
		mEditView.requestFocus();

		Button startButton = (Button) findViewById(R.id.button_start);
		startButton.setOnClickListener(mOnButtonStartListener);
	}

	private Runnable uploadToFtp = new Runnable() {

		@Override
		public void run() {
			uploadFile("10.129.41.70", "imetest", "Sogou7882Imeqa", "/WordCrawler", "result.txt");
			File localFile = new File(getFilesDir().getPath() + "/" + "result.txt");
			localFile.deleteOnExit();
		}
	};

	private View.OnKeyListener mOnLeftCTRListener = new View.OnKeyListener() {

		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			if(keyCode == KeyEvent.KEYCODE_CTRL_LEFT) {
				Log.e("reading", "#############"  + "reading" + "#############");
				readLogcat();
				return true;
			}
			return false;
		}

	};

	private View.OnClickListener mOnButtonStartListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			mEditView.showInputMethod();
			mCurIndex = 0;
			try {
				mLogcat.read();

				for (String inputStr : input) {
					synchronized (inputStr) {
						//��������ļ��������һ���ո�����������ո��keyevent
						if (inputStr.equals(" ")){
							SendKey(mInputShell, KeyEvent.KEYCODE_SPACE);
						}
						String pinyin = inputStr.substring(0, inputStr.indexOf(" "));
						SendString(mInputShell, pinyin);

						SendKey(mInputShell, KeyEvent.KEYCODE_CTRL_LEFT);
						//SendKey(mInputShell, KeyEvent.KEYCODE_SPACE);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} 

		}
	};

	@Override
	protected void onStop() {
		try {
			mInputShell.close();
			mLogcat.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		super.onStop();
	}

	private void readLogcat() {
		String RawResult;
		try {
			RawResult = mLogcat.read();

			String[] resultlist = RawResult.split("\n");
			int startIndex = -1;
			for (int i = resultlist.length - 1; i >=0; i--) {
				if (resultlist[i].contains("text:1#")) {
					startIndex = i - 1;
					break;
				}
			}
			if (startIndex != -1) {
				StringBuilder resultToWrite = new StringBuilder();
				String targetIndex = "0";
				String choice = input.get(mCurIndex);
				String choiceWords = choice.substring(choice.indexOf(" ") + 1);
				//д���ļ����ַ�����ʾһ��ƴ�����Ŀ�ʼ
				resultToWrite.append("wordstart\n");
				resultToWrite.append("pinyin:" + input.get(mCurIndex) + "\n");
				//�õ��˺�ѡ���ں�ѡ����������Ҫѡ�������ĺ�ѡ
				for (int i = startIndex; i < resultlist.length - 1; i+=2) {
					//�����ȡ�����ж���string����ô���Ϻ�ѡ�ʵ����ͣ����Գ����ж��Ǻ�ѡ�ʣ�����ȥ����
					if (resultlist[i].contains("type=String") && resultlist[i + 1].contains("type=String")) {
						//�����ѡ�ʣ����ڼ�¼�ͶԱ�
						String word = resultlist[i].substring(
								resultlist[i].indexOf("text:") + "text:".length(), 
								resultlist[i].indexOf("#"));
						//���������֣����ڰ������ּ���ѡ������
						String index = resultlist[i + 1].substring(
								resultlist[i + 1].indexOf("text:") + "text:".length(), 
								resultlist[i + 1].indexOf("#"));

						resultToWrite.append(index + ":");
						resultToWrite.append(word + "\n");
						Log.e("reading", "The Word is : " + index + ": " + word);
						//�����ѡ�ʺ͵�ǰҪѡ�Ĵ���һ���Ļ���˵�����ζ�������Ҫ�����ĺ�ѡ�ʣ���ôͨ�����̰���index�������
						if (word.equals(choiceWords)) {
							targetIndex = index;
						}
					}
				}
				//ѡ�����ּ�������������
				SendChoice(mSendChoice, targetIndex.equals("0") ? "1" : targetIndex);
				//��¼�Ƿ����С������0����ôû�����У�����Ϊ���С�
				resultToWrite.append("tartget:" + targetIndex + "\n");
				//д���ļ����ַ�����ʾһ��ƴ�����Ľ�����
				resultToWrite.append("wordend\n");
				mCurIndex ++;
				new WriteFileThread(getApplicationContext(), resultToWrite.toString()).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void SendKey(NativeShell shell, int Keycode) throws IOException{
		Log.e("InputKeyEvent", "Keycode:" + Keycode);
		shell.write("input keyevent " + Keycode);
	}

	private void SendChoice(NativeShell shell, String Keycode) throws IOException{
		int key = Integer.valueOf(Keycode) + 7;
		Log.e("Send Choice", "Keycode:" + key);
		shell.write("input keyevent " + key);
	}

	private void SendString(NativeShell shell, String text) throws IOException{
		Log.e("InputKeyEvent", "text:" + text);
		String cmdString = "input text " + "\"" + text + "\"";
		shell.write(cmdString);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.edit, menu);
		return true;
	}

	private static void sleep(int second) {
		try {
			Thread.sleep(second * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * ��FTP�ϵ��ļ����ص����أ����ص��ļ����ֺͷ��������ļ���������һ���ġ�
	 * @param host FTP��������IP��ַ������FTP������������
	 * @param username ��½FTP��������Ҫ���û���
	 * @param passwd ��½FTP��������Ҫ������
	 * @param remoteDir �ļ���FTP�������ϵ�·���������ļ���
	 * @param filename	�ļ�������
	 * @return true:���سɹ���false:����ʧ��
	 */
	private boolean downloadFile(String host, String username, String passwd, String remoteDir, String filename) {
		File localFile = new File(getFilesDir().getPath() + "/" + filename);

		FTPClient client = new FTPClient();
		try {
			client.connect(host);
			client.login(username, passwd);
			client.download(remoteDir + "/" + filename, localFile);
			client.disconnect(false);

			return true;
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (FTPIllegalReplyException e) {
			e.printStackTrace();
			return false;
		} catch (FTPException e) {
			e.printStackTrace();
			return false;
		} catch (FTPDataTransferException e) {
			e.printStackTrace();
			return false;
		} catch (FTPAbortedException e) {
			e.printStackTrace();
			return false;
		} 


	}

	private boolean uploadFile(String host, String username, String passwd, String remoteDir, String filename) {
		File file = new File(getFilesDir().getPath() + "/" + filename);
		if (!file.exists())
			return false;

		FTPClient client = new FTPClient();
		try {
			client.connect(host);
			client.login(username, passwd);
			client.changeDirectory(remoteDir);
			client.upload(file);
			client.rename(filename, "result-" + Utils.getDateTime() + ".txt");
			client.disconnect(false);

			return true;
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (FTPIllegalReplyException e) {
			e.printStackTrace();
			return false;
		} catch (FTPException e) {
			e.printStackTrace();
			return false;
		} catch (FTPDataTransferException e) {
			e.printStackTrace();
			return false;
		} catch (FTPAbortedException e) {
			e.printStackTrace();
			return false;
		} 
	}

}
