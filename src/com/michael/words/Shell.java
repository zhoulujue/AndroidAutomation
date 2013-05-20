package com.michael.words;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;

public class Shell {

	private FileDescriptor fd = null;
	private int[] id = new int[1];
	private BufferedInputStream stdin = null;
	private BufferedOutputStream stdout = null;
	private Process mProcess;

	public Shell() throws IOException, InterruptedException {

		mProcess = new ProcessBuilder()
			.command("su")
			.redirectErrorStream(true)
			.start();

		this.stdin = new BufferedInputStream(mProcess.getInputStream());
		this.stdout = new BufferedOutputStream(mProcess.getOutputStream());

		this.read();
	}

	public static native FileDescriptor createSubprocess(String cmd, String[] args, String[] envVars, int[] processId);
	public static native void setPtyWindowSize(FileDescriptor fd, int row, int col, int xpixel, int ypixel);
	public static native int waitFor(int processId);
	public static native void close(FileDescriptor fd);
	public static native void hangupProcessGroup(int processId);

	public void close() {
		mProcess.destroy();
	}

	public void  flushRead() throws IOException {
		this.stdin.read();
	}

	public String read() throws IOException, InterruptedException {
		/*		StringBuffer value = new StringBuffer();
		while(this.stdin.available() > 0) {
			for(int i=0; i<this.stdin.available(); i++) {
				int c = this.stdin.read();

				value.append((char)c);
			}

			Thread.sleep(50);
		}
		return value.toString();*/
		StringBuffer value = new StringBuffer();
		while (this.stdin.available() > 0) {
			byte[] buffer = new byte[this.stdin.available()];
			this.stdin.read(buffer);

			String temp = new String(buffer, "UTF-8");
			value.append(temp);

			Thread.sleep(50);
		}

		return value.toString();
	}

	public void exit() throws IOException {
		write("exit");
	}

	public void write(String value) throws IOException {
		this.stdout.write((value + "\n").getBytes());
		this.stdout.flush();
		for(int i=0; i<value.length(); i++)
			this.stdin.read();
	}

}
