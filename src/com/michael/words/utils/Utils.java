package com.michael.words.utils;

import it.sauronsoftware.ftp4j.FTPAbortedException;
import it.sauronsoftware.ftp4j.FTPDataTransferException;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Instrumentation;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Point;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.michael.words.EditTextView;

public class Utils {

	public static String UPLOAD_PATH = "/Temp/PinZhuan/raw";
	public static String FTP_HOST_NAME = "10.12.9.184";
	public static int FTP_PORT = 21;
	public static String CONFIG_FILE_SUFFIX = "gz.txt";
	public static String TEMP_RESULT_FILE = "result.txt";

	public static class ReadFromFile {
		/**
		 * 以字节为单位读取文件，常用于读二进制文件，如图片、声音、影像等文件。
		 */
		public static void readFileByBytes(String fileName) {
			File file = new File(fileName);
			InputStream in = null;
			try {
				// 一次读一个字节
				in = new FileInputStream(file);
				int tempbyte;
				while ((tempbyte = in.read()) != -1) {
					System.out.write(tempbyte);
				}
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			try {
				// 一次读多个字节
				byte[] tempbytes = new byte[100];
				int byteread = 0;
				in = new FileInputStream(fileName);
				ReadFromFile.showAvailableBytes(in);
				// 读入多个字节到字节数组中，byteread为一次读入的字节数
				while ((byteread = in.read(tempbytes)) != -1) {
					System.out.write(tempbytes, 0, byteread);
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e1) {
					}
				}
			}
		}

		/**
		 * 以字符为单位读取文件，常用于读文本，数字等类型的文件
		 */
		public static void readFileByChars(String fileName) {
			File file = new File(fileName);
			Reader reader = null;
			try {
				// 一次读一个字符
				reader = new InputStreamReader(new FileInputStream(file));
				int tempchar;
				while ((tempchar = reader.read()) != -1) {
					// 对于windows下，\r\n这两个字符在一起时，表示一个换行。
					// 但如果这两个字符分开显示时，会换两次行。
					// 因此，屏蔽掉\r，或者屏蔽\n。否则，将会多出很多空行。
					if (((char) tempchar) != '\r') {
						System.out.print((char) tempchar);
					}
				}
				reader.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				// 一次读多个字符
				char[] tempchars = new char[30];
				int charread = 0;
				reader = new InputStreamReader(new FileInputStream(fileName));
				// 读入多个字符到字符数组中，charread为一次读取字符数
				while ((charread = reader.read(tempchars)) != -1) {
					// 同样屏蔽掉\r不显示
					if ((charread == tempchars.length)
							&& (tempchars[tempchars.length - 1] != '\r')) {
						System.out.print(tempchars);
					} else {
						for (int i = 0; i < charread; i++) {
							if (tempchars[i] == '\r') {
								continue;
							} else {
								System.out.print(tempchars[i]);
							}
						}
					}
				}

			} catch (Exception e1) {
				e1.printStackTrace();
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e1) {
					}
				}
			}
		}

		/**
		 * @param fileName 文件的绝对路径
		 * @return lines 存储着每一行的ArrayList
		 * 以行为单位读取文件，常用于读面向行的格式化文件
		 */
		public static ArrayList<String> readFileByLines(String fileName) {
			File file = new File(fileName);
			ArrayList<String> lines = new ArrayList<String>();
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(file));
				String tempString = null;
				int line = 0;
				// 一次读入一行，直到读入null为文件结束
				while ((tempString = reader.readLine()) != null) {
					// 显示行号
					//System.out.println("line " + line + ": " + tempString);
					lines.add(line, tempString);
					line++;
				}
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e1) {
					}
				}
			}
			return lines;
		}

		/**
		 * @param fileName 文件的绝对路径
		 * @return lines 存储着每一行的ArrayList
		 * 以行为单位读取文件，常用于读面向行的格式化文件
		 */
		public static ArrayList<String> readFileByLines(String fileName, String charset) {
			File file = new File(fileName);
			ArrayList<String> lines = new ArrayList<String>();
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(file));
				String tempString = null;
				int line = 0;
				// 一次读入一行，直到读入null为文件结束
				while ((tempString = reader.readLine()) != null) {
					// 显示行号
					//System.out.println("line " + line + ": " + tempString);
					lines.add(line, tempString);
					line++;
				}
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e1) {
					}
				}
			}
			return lines;
		}

		/**
		 * 随机读取文件内容
		 */
		public static void readFileByRandomAccess(String fileName) {
			RandomAccessFile randomFile = null;
			try {
				// 打开一个随机访问文件流，按只读方式
				randomFile = new RandomAccessFile(fileName, "r");
				// 文件长度，字节数
				long fileLength = randomFile.length();
				// 读文件的起始位置
				int beginIndex = (fileLength > 4) ? 4 : 0;
				// 将读文件的开始位置移到beginIndex位置。
				randomFile.seek(beginIndex);
				byte[] bytes = new byte[10];
				int byteread = 0;
				// 一次读10个字节，如果文件内容不足10个字节，则读剩下的字节。
				// 将一次读取的字节数赋给byteread
				while ((byteread = randomFile.read(bytes)) != -1) {
					System.out.write(bytes, 0, byteread);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (randomFile != null) {
					try {
						randomFile.close();
					} catch (IOException e1) {
					}
				}
			}
		}

		/**
		 * 显示输入流中还剩的字节数
		 */
		public static void showAvailableBytes(InputStream in) {
			try {
				System.out.println("当前字节输入流中的字节数为:" + in.available());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/*	    public static void main(String[] args) {
	        String fileName = "C:/temp/newTemp.txt";
	        ReadFromFile.readFileByBytes(fileName);
	        ReadFromFile.readFileByChars(fileName);
	        ReadFromFile.readFileByLines(fileName);
	        ReadFromFile.readFileByRandomAccess(fileName);
	    }*/
	}

	public static class AppendToFile {
		/**
		 * A方法追加文件：使用RandomAccessFile
		 */
		public static void appendMethodA(String fileName, String content) {
			try {
				// 打开一个随机访问文件流，按读写方式
				RandomAccessFile randomFile = new RandomAccessFile(fileName, "rw");
				// 文件长度，字节数
				long fileLength = randomFile.length();
				//将写文件指针移到文件尾。
				randomFile.seek(fileLength);
				randomFile.writeBytes(content);
				randomFile.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * B方法追加文件：使用FileWriter
		 */
		public static void appendMethodB(String fileName, String content) {
			try {
				//打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
				FileWriter writer = new FileWriter(fileName, true);
				writer.write(content);
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/*	    public static void main(String[] args) {
	        String fileName = "C:/temp/newTemp.txt";
	        String content = "new append!";
	        //按方法A追加文件
	        AppendToFile.appendMethodA(fileName, content);
	        AppendToFile.appendMethodA(fileName, "append end. \n");
	        //显示文件内容
	        ReadFromFile.readFileByLines(fileName);
	        //按方法B追加文件
	        AppendToFile.appendMethodB(fileName, content);
	        AppendToFile.appendMethodB(fileName, "append end. \n");
	        //显示文件内容
	        ReadFromFile.readFileByLines(fileName);
	    }*/
	}

	/**
	 * 使当前线程睡眠指定时间，以秒为单位
	 * @param sec 睡眠的秒数
	 */
	public static void sleep(int sec)
	{
		try {
			Thread.currentThread();
			Thread.sleep(sec*1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 将十六进制数转换成十进制。高位和低位分开，高位转换后左移16位，和低位相加。
	 * @param HighHex 十六进制数的高位
	 * @param LowHex 十六进制的低位
	 * @return 转换完成的十进制数
	 */
	public static int getDexFromHex(String HighHex, String LowHex)
	{
		int high = Integer.parseInt(HighHex, 16);
		int low = Integer.parseInt(LowHex, 16);
		int result = (high << 16) + low;
		return result;
	}

	public static String getDateTime()
	{
		//设置日期格式
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.CHINA);
		return df.format(new Date());
	}

	public static String getContent(ArrayList<String> lines)
	{
		String content = "";
		for(int i = 0; i < lines.size(); i++)
		{
			content = content + lines.get(i) + "\r\n";
		}
		return content;
	}

	public static boolean IsFileExist(String path){
		if(path == null || "".equals(path))
			return false;
		File file = new File(path);
		return file.exists();
	}

	public static String getTimeStamp()
	{
		Date now = new Date();
		return String.valueOf(now.getTime()).substring(0, 10);
	}

	public static void showToast(Context context, String string) {
		Toast.makeText(context, string, Toast.LENGTH_LONG).show();
	}

	public static void showToast(Context context, int stringId) {
		Toast.makeText(context, stringId, Toast.LENGTH_LONG).show();
	}

	/**
	 * 把FTP上的文件下载到本地，本地的文件名字和服务器上文件的名字是一样的。
	 * @param host FTP服务器的IP地址，或者FTP服务器的域名
	 * @param username 登陆FTP服务器需要的用户名
	 * @param passwd 登陆FTP服务器需要的密码
	 * @param remoteDir 文件在FTP服务器上的路径，不含文件名
	 * @param filename	文件的名字
	 * @return true:下载成功；false:下载失败
	 */
	public static boolean downloadFile(Context context, String host, String username, String passwd, String remoteDir, String filename) {
		File localFile = new File(context.getFilesDir().getPath() + "/" + filename);

		it.sauronsoftware.ftp4j.FTPClient client = new it.sauronsoftware.ftp4j.FTPClient();
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

	public static boolean uploadFile(Context context, String host, String username, String passwd, String remoteDir, String filename) {
		File file = new File(context.getFilesDir().getPath() + "/" + filename);
		if (!file.exists())
			return false;

		it.sauronsoftware.ftp4j.FTPClient client = new it.sauronsoftware.ftp4j.FTPClient();
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
		} catch (Exception e) {
			return false;
		}
	}

	public static PackageInfo getCurrentImeInfo(Context context){
		InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
		List<InputMethodInfo> mInputMethodProperties = imm.getEnabledInputMethodList();

		final int N = mInputMethodProperties.size();

		for (int i = 0; i < N; i++) {
			InputMethodInfo imi = mInputMethodProperties.get(i);
			if (imi.getId().equals(Settings.Secure.getString(context.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD))) {
				String packageName = imi.getPackageName();
				PackageInfo pInfo = null;
				try {
					pInfo = context.getPackageManager().getPackageInfo(packageName, 0);
				} catch (NameNotFoundException e) {
					e.printStackTrace();
				}
				return pInfo;
			}
		}
		return null;
	}

	public static Point getCurScreenSize(Context context) {
		Point outSize = new Point();
		/*			((WindowManager) context.createPackageContext(
							Utils.getCurrentImeInfo(context).packageName, 
							Context.CONTEXT_IGNORE_SECURITY)
							.getSystemService(Context.WINDOW_SERVICE))
							.getDefaultDisplay().getRealSize(outSize);*/
		WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		manager.getDefaultDisplay().getRealSize(outSize);

		return outSize;
	}

	public static Point getCurDisplaySize(Context context) {
		Point outSize = new Point();
		/*			((WindowManager) context.createPackageContext(
							Utils.getCurrentImeInfo(context).packageName, 
							Context.CONTEXT_IGNORE_SECURITY)
							.getSystemService(Context.WINDOW_SERVICE))
							.getDefaultDisplay().getRealSize(outSize);*/
		WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		manager.getDefaultDisplay().getSize(outSize);

		return outSize;
	}
	
	public static boolean isChineseCharacter(String str) {
		return str.matches("[\\u4e00-\\u9fa5]+");
	}

	public static boolean isNumber(String str) {
		return str.matches("[0-9]+");
	}

	public static boolean isLetter(String str) {
		return str.matches("[a-z]+");
	}

	public static void showDialog(final Context context, int messageId, int titleId, int positiveBtnStr, int negativeBtnStr,
			boolean cancelable, final Runnable jobRunnable) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setCancelable(cancelable);
		builder.setMessage(messageId);
		builder.setTitle(titleId);
		builder.setPositiveButton(positiveBtnStr, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (jobRunnable != null) {
					Thread job = new Thread(jobRunnable);
					job.start();
					try {
						job.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				dialog.dismiss();
			}

		});
		builder.setNegativeButton(positiveBtnStr, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();

			}
		});
		builder.create().show();
	}

	public static ArrayList<File> getSuffixFiles(Context context, String suffix) {
		ArrayList<File> files = new ArrayList<File>();
		File dir = context.getFilesDir();
		File[] configFiles = dir.listFiles();
		for (File file : configFiles) {
			if (file.getAbsolutePath().endsWith("gz.txt")){
				files.add(file);
			}
		}
		return files;
	}

	public static void renameResultTxt(File rawConfig, Context context) {
		File tempResultFile = new File(context.getFilesDir() + "/" + TEMP_RESULT_FILE);
		String rawConfigName = rawConfig.getName().split("\\.")[0]; 
		File newFile = new File(context.getFilesDir() + "/" + "result-" + rawConfigName + ".txt");
		//重命名result.txt文件
		tempResultFile.renameTo(newFile);
		//重命名评测集case文件
		rawConfig.renameTo(new File(context.getFilesDir() + "/" + rawConfigName + ".txt"));

		//重命名以后，创建新的result.txt
		File resultFile = new File(context.getFilesDir() + "/" + TEMP_RESULT_FILE);
		try {
			resultFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}

	public static int getLastCaseCountFromResult(Context context) {
		int caseCount = -1;
		try {
			File tempResultFile = new File(context.getFilesDir() + "/" + TEMP_RESULT_FILE);

			if (tempResultFile.exists()) {
				BufferedReader reader = new BufferedReader(new FileReader(tempResultFile));
				String caseline = null;
				while ( (caseline = reader.readLine()) != null ) {
					if (caseline.contains("count:")) {
						caseCount = Integer.valueOf(caseline.split("\\[")[1].split("\\]")[0]);
					}
				}
				reader.close();
				return caseCount;
			} else {
				return caseCount;
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return caseCount;
		} catch (IOException e) {
			e.printStackTrace();
			return caseCount;
		}
	}

	public static void clearImeContext(Instrumentation instrumentation) {
		for (int i =0; i < 20; i++) 
			instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DEL);
	}

	@SuppressLint("SdCardPath")
	public static void clearImeData(String packageName, Context context) {
		ActivityManager am = (ActivityManager)context.getSystemService(
				Context.ACTIVITY_SERVICE);
		PackageManager pm = (PackageManager)context.getPackageManager();
		Method forceStopPackage = null;
		try {
			forceStopPackage = am.getClass().getMethod("forceStopPackage", String.class);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		try {
			forceStopPackage.invoke(am, packageName);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

		Method deleteApplicationCacheFiles = null;
		try {
			Class<?> type = Class.forName("android.content.pm.IPackageDataObserver");
			deleteApplicationCacheFiles = pm.getClass().getMethod("deleteApplicationCacheFiles", new Class[]{String.class, type});
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		try {
			deleteApplicationCacheFiles.invoke(pm, new Object[]{packageName, null});
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

		Method clearApplicationUserData = null;
		try {
			Class<?> type = Class.forName("android.content.pm.IPackageDataObserver");
			clearApplicationUserData = pm.getClass().getMethod("clearApplicationUserData", new Class[]{String.class, type});
		} catch (NoSuchMethodException e1) {
			e1.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		try {
			clearApplicationUserData.invoke(pm, new Object[]{packageName, null});
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		//am.forceStopPackage(packageName);
		//pm.deleteApplicationCacheFiles(packageName, null);
		//pm.clearApplicationUserData(packageName, null);
	}

	public static boolean deleteDir(File dir) {
		if (dir != null && dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}

		return dir.delete();
	}

	public static void showInputMethodPicker(Context context) {
		InputMethodManager imeManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE); 
		if (imeManager != null) {
			imeManager.showInputMethodPicker();
		}
	}

	public static boolean showSoftInput(EditTextView editView, Context context) {
		if (editView != null) {
			editView.requestFocus();
			InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
			return imm.showSoftInput(editView, InputMethodManager.SHOW_IMPLICIT);
		}
		return false;
	}

	public static boolean isIMEActive(EditTextView view, Context context) {
		InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
		return imm.isActive() && imm.showSoftInput(view, InputMethodManager.SHOW_FORCED);
	}

	public static void execCommand(String... command) {
		Process process = null;
		try {
			process = new ProcessBuilder().command(command).start();
			//对于命令的执行结果我们可以通过流来读取
			// InputStream in = process.getInputStream();
			// OutputStream out = process.getOutputStream();
			// InputStream err = process.getErrorStream();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (process != null)
				process.destroy();
		}
	}

	 /**
     * 从 Unicode 形式的字符串转换成对应的编码的特殊字符串。 如 "\u9EC4" to "黄".
     * Converts encoded \\uxxxx to unicode chars
     * and changes special saved chars to their original forms
     * 
     * @param in
     *        Unicode编码的字符数组。
     * @param off
     *        转换的起始偏移量。
     * @param len
     *        转换的字符长度。
     * @param convtBuf
     *        转换的缓存字符数组。
     * @return 完成转换，返回编码前的特殊字符串。
     */
    public static String fromEncodedUnicode(char[] in, int off, int len) {
        char aChar;
        char[] out = new char[len]; // 只短不长
        int outLen = 0;
        int end = off + len;

        while (off < end) {
            aChar = in[off++];
            if (aChar == '\\') {
                aChar = in[off++];
                if (aChar == 'u') {
                    // Read the xxxx
                    int value = 0;
                    for (int i = 0; i < 4; i++) {
                        aChar = in[off++];
                        switch (aChar) {
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            value = (value << 4) + aChar - '0';
                            break;
                        case 'a':
                        case 'b':
                        case 'c':
                        case 'd':
                        case 'e':
                        case 'f':
                            value = (value << 4) + 10 + aChar - 'a';
                            break;
                        case 'A':
                        case 'B':
                        case 'C':
                        case 'D':
                        case 'E':
                        case 'F':
                            value = (value << 4) + 10 + aChar - 'A';
                            break;
                        default:
                            throw new IllegalArgumentException("Malformed \\uxxxx encoding.");
                        }
                    }
                    out[outLen++] = (char) value;
                } else {
                    if (aChar == 't') {
                        aChar = '\t';
                    } else if (aChar == 'r') {
                        aChar = '\r';
                    } else if (aChar == 'n') {
                        aChar = '\n';
                    } else if (aChar == 'f') {
                        aChar = '\f';
                    }
                    out[outLen++] = aChar;
                }
            } else {
                out[outLen++] = (char) aChar;
            }
        }
        return new String(out, 0, outLen);
    }
	
}
