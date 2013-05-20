package com.michael.words;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class NativeShell {

	private FileDescriptor fd = null;
	private int[] id = new int[1];
	BufferedInputStream stdin = null;
	BufferedOutputStream stdout = null;

	static {
		System.loadLibrary("jackpal-androidterm3");
    }

	public NativeShell() throws IOException, InterruptedException {
		this.fd = NativeShell.createSubprocess("/system/bin/sh", new String[] { "-" }, null, this.id);

		this.stdin = new BufferedInputStream(new FileInputStream(this.fd));
		this.stdout = new BufferedOutputStream(new FileOutputStream(this.fd));

		this.write("cd /data/data/com.michael.words");
		this.read();
	}

    public static native FileDescriptor createSubprocess(String cmd, String[] args, String[] envVars, int[] processId);
    public static native void setPtyWindowSize(FileDescriptor fd, int row, int col, int xpixel, int ypixel);
    public static native int waitFor(int processId);
    public static native void close(FileDescriptor fd);
    public static native void hangupProcessGroup(int processId);
    
    public void close() {
    	NativeShell.close(this.fd);
    }

	public String read() throws IOException, InterruptedException {
		StringBuffer value = new StringBuffer();

		while(this.stdin.available() > 0) {
			for(int i=0; i<this.stdin.available(); i++) {
				int c = this.stdin.read();

				value.append((char)c);
			}

			Thread.sleep(50);
		}

		return value.toString();
	}
    
    public void write(String value) throws IOException {
		this.stdout.write((value + "\n").getBytes());
		this.stdout.flush();
		//this.stdin.read();
		//for(int i=0; i<value.length(); i++)
		//	this.stdin.read();
	}
    
}