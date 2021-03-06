package com.michael.words;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

public class Shell {

	private BufferedInputStream stdin = null;
	private BufferedOutputStream stdout = null;
	private static Process mProcess = null;

	public Shell() throws IOException, InterruptedException {

		mProcess = Runtime.getRuntime().exec("su");

		this.stdin = new BufferedInputStream(mProcess.getInputStream());
		this.stdout = new BufferedOutputStream(mProcess.getOutputStream());

		this.read();
	}

	public void close() throws IOException {
		this.stdin.close();
		this.stdout.close();
		mProcess.destroy();
	}

	public void  flushRead() throws IOException {
		this.stdin.read();
	}

	public String read() throws IOException, InterruptedException {
		StringBuffer value = new StringBuffer();
		int length = 0;
		while ((length = this.stdin.available()) > 0) {
			byte[] buffer = new byte[length];
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
	}

}
