package com.michael.words;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

public class CountTextView extends TextView {
	public CountTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setOnKeyListener(mOnF1Listener);
	}

	public CountTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOnKeyListener(mOnF1Listener);
	}

	public CountTextView(Context context) {
		super(context);
		setOnKeyListener(mOnF1Listener);
	}
	
	private View.OnKeyListener mOnF1Listener = new View.OnKeyListener() {

		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			if(keyCode == KeyEvent.KEYCODE_F1) {
				setText("");
				return true;
			}
			return false;
		}
	};
}
