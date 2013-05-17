package com.michael.words;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import android.R.integer;

public class Utils {

	public static String UPLOAD_PATH = "/Temp/PinZhuan/raw";
	public static String FTP_HOST_NAME = "10.12.9.184";
	public static int FTP_PORT = 21;

	public static class ReadFromFile {
		/**
		 * ���ֽ�Ϊ��λ��ȡ�ļ��������ڶ��������ļ�����ͼƬ��������Ӱ����ļ���
		 */
		public static void readFileByBytes(String fileName) {
			File file = new File(fileName);
			InputStream in = null;
			try {
				// һ�ζ�һ���ֽ�
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
				// һ�ζ�����ֽ�
				byte[] tempbytes = new byte[100];
				int byteread = 0;
				in = new FileInputStream(fileName);
				ReadFromFile.showAvailableBytes(in);
				// �������ֽڵ��ֽ������У�bytereadΪһ�ζ�����ֽ���
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
		 * ���ַ�Ϊ��λ��ȡ�ļ��������ڶ��ı������ֵ����͵��ļ�
		 */
		public static void readFileByChars(String fileName) {
			File file = new File(fileName);
			Reader reader = null;
			try {
				// һ�ζ�һ���ַ�
				reader = new InputStreamReader(new FileInputStream(file));
				int tempchar;
				while ((tempchar = reader.read()) != -1) {
					// ����windows�£�\r\n�������ַ���һ��ʱ����ʾһ�����С�
					// ������������ַ�ֿ���ʾʱ���ỻ�����С�
					// ��ˣ����ε�\r����������\n�����򣬽������ܶ���С�
					if (((char) tempchar) != '\r') {
						System.out.print((char) tempchar);
					}
				}
				reader.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				// һ�ζ�����ַ�
				char[] tempchars = new char[30];
				int charread = 0;
				reader = new InputStreamReader(new FileInputStream(fileName));
				// �������ַ��ַ������У�charreadΪһ�ζ�ȡ�ַ���
				while ((charread = reader.read(tempchars)) != -1) {
					// ͬ�����ε�\r����ʾ
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
		 * @param fileName �ļ��ľ��·��
		 * @return lines �洢��ÿһ�е�ArrayList
		 * ����Ϊ��λ��ȡ�ļ��������ڶ������еĸ�ʽ���ļ�
		 */
		public static ArrayList<String> readFileByLines(String fileName) {
			File file = new File(fileName);
			ArrayList<String> lines = new ArrayList<String>();
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(file));
				String tempString = null;
				int line = 0;
				// һ�ζ���һ�У�ֱ������nullΪ�ļ�����
				while ((tempString = reader.readLine()) != null) {
					// ��ʾ�к�
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
		 * ����ȡ�ļ�����
		 */
		public static void readFileByRandomAccess(String fileName) {
			RandomAccessFile randomFile = null;
			try {
				// ��һ���������ļ�������ֻ����ʽ
				randomFile = new RandomAccessFile(fileName, "r");
				// �ļ����ȣ��ֽ���
				long fileLength = randomFile.length();
				// ���ļ�����ʼλ��
				int beginIndex = (fileLength > 4) ? 4 : 0;
				// �����ļ��Ŀ�ʼλ���Ƶ�beginIndexλ�á�
				randomFile.seek(beginIndex);
				byte[] bytes = new byte[10];
				int byteread = 0;
				// һ�ζ�10���ֽڣ�����ļ����ݲ���10���ֽڣ����ʣ�µ��ֽڡ�
				// ��һ�ζ�ȡ���ֽ����byteread
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
		 * ��ʾ�������л�ʣ���ֽ���
		 */
		public static void showAvailableBytes(InputStream in) {
			try {
				System.out.println("��ǰ�ֽ��������е��ֽ���Ϊ:" + in.available());
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
		 * A����׷���ļ���ʹ��RandomAccessFile
		 */
		public static void appendMethodA(String fileName, String content) {
			try {
				// ��һ���������ļ���������д��ʽ
				RandomAccessFile randomFile = new RandomAccessFile(fileName, "rw");
				// �ļ����ȣ��ֽ���
				long fileLength = randomFile.length();
				//��д�ļ�ָ���Ƶ��ļ�β��
				randomFile.seek(fileLength);
				randomFile.writeBytes(content);
				randomFile.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * B����׷���ļ���ʹ��FileWriter
		 */
		public static void appendMethodB(String fileName, String content) {
			try {
				//��һ��д�ļ��������캯���еĵڶ�������true��ʾ��׷����ʽд�ļ�
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
	        //������A׷���ļ�
	        AppendToFile.appendMethodA(fileName, content);
	        AppendToFile.appendMethodA(fileName, "append end. \n");
	        //��ʾ�ļ�����
	        ReadFromFile.readFileByLines(fileName);
	        //������B׷���ļ�
	        AppendToFile.appendMethodB(fileName, content);
	        AppendToFile.appendMethodB(fileName, "append end. \n");
	        //��ʾ�ļ�����
	        ReadFromFile.readFileByLines(fileName);
	    }*/
	}

	/**
	 * ʹ��ǰ�߳�˯��ָ��ʱ�䣬����Ϊ��λ
	 * @param sec ˯�ߵ�����
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
	 * ��ʮ�������ת����ʮ���ơ���λ�͵�λ�ֿ�����λת��������16λ���͵�λ��ӡ�
	 * @param HighHex ʮ�������ĸ�λ
	 * @param LowHex ʮ����Ƶĵ�λ
	 * @return ת����ɵ�ʮ������
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
		//�������ڸ�ʽ
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
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

	public static InputStreamReader getInputStreamReaderFromFtp(String url,int port,String username,
			String password, String path, String filename) {
		FTPClient ftp = new FTPClient();
		int reply;
		try {
			ftp.connect(url, port);
			ftp.login(username, password);
			reply = ftp.getReplyCode();
			if (!FTPReply.isPositiveCompletion(reply)) {
				ftp.disconnect();
				return null;
			}
			ftp.changeWorkingDirectory(path);
			InputStream inputStream = ftp.retrieveFileStream(filename);
			InputStreamReader reader = new InputStreamReader(inputStream);
			return reader;

		} catch (SocketException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			if (ftp.isConnected()) {
				try {
					ftp.disconnect();
				} catch (IOException ioe) {
				}
			}
		}
	}
	
	public static OutputStreamWriter getOutputStreamWriterFromFtp(String url,int port,String username,
			String password, String path, String filename) {
		FTPClient ftp = new FTPClient();
		int reply;
		try {
			ftp.connect(url, port);
			ftp.login(username, password);
			reply = ftp.getReplyCode();
			if (!FTPReply.isPositiveCompletion(reply)) {
				ftp.disconnect();
				return null;
			}
			ftp.changeWorkingDirectory(path);
			OutputStream outputStream = ftp.appendFileStream(filename);
			OutputStreamWriter writer = new OutputStreamWriter(outputStream);
			return writer;

		} catch (SocketException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			if (ftp.isConnected()) {
				try {
					ftp.disconnect();
				} catch (IOException ioe) {
				}
			}
		}

	}

	public static boolean uploadFile(String filename)
	{
		File resultFile = new File(filename);
		if(!resultFile.exists())
			return false;
		FileInputStream in = null;
		try {
			in = new FileInputStream(resultFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}

		//TODO: ���úϳ��ļ�
		//String fileNameOnFTP = "result" + getTimeStamp() + ".txt";
		String fileNameOnFTP = "result.txt";
		return uploadFile(FTP_HOST_NAME, FTP_PORT, "setest", "setest", UPLOAD_PATH, fileNameOnFTP, in);

	}

	/**
	 * Description: ��FTP�������ϴ��ļ�
	 * @param url FTP������HostName
	 * @param port FTP�������˿�
	 * @param username FTP��¼�˺�
	 * @param password FTP��¼����
	 * @param path FTP����������Ŀ¼
	 * @param filename �ϴ���FTP�������ϵ��ļ���
	 * @param input ������
	 * @return �ɹ�����true�����򷵻�false
	 */
	public static boolean uploadFile(String url,int port,String username, String password, String path, String filename, InputStream input) {
		boolean success = false;
		FTPClient ftp = new FTPClient();
		try {
			int reply;
			ftp.connect(url, port);//����FTP������
			//������Ĭ�϶˿ڣ�����ʹ��ftp.connect(url)�ķ�ʽֱ������FTP������
			ftp.login(username, password);//��¼
			reply = ftp.getReplyCode();
			if (!FTPReply.isPositiveCompletion(reply)) {
				ftp.disconnect();
				return success;
			}
			ftp.changeWorkingDirectory(path);
			//TODO: ���úϳ��ļ�
			//ftp.storeFile(filename, input);			
			ftp.appendFile(filename, input);

			input.close();
			ftp.logout();
			success = true;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (ftp.isConnected()) {
				try {
					ftp.disconnect();
				} catch (IOException ioe) {
				}
			}
		}
		return success;
	}

	/*	public static void main(String[] args){
		System.out.println(uploadFile(OutputFileName));
	}*/

}
