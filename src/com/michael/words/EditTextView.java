package com.michael.words;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/**
 * The query text field.
 */
public class EditTextView extends EditText {

	private static final boolean DBG = false;
	private static final String TAG = "Michael.EditTextView";

	private CommitCompletionListener mCommitCompletionListener;

	public EditTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setOnKeyListener(mOnLeftCTRListener);
	}

	public EditTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOnKeyListener(mOnLeftCTRListener);
	}

	public EditTextView(Context context) {
		super(context);
		setOnKeyListener(mOnLeftCTRListener);
	}

	/**
	 * Sets the text selection in the query text view.
	 *
	 * @param selectAll If {@code true}, selects the entire query.
	 *        If {@false}, no characters are selected, and the cursor is placed
	 *        at the end of the query.
	 */
	public void setTextSelection(boolean selectAll) {
		if (selectAll) {
			selectAll();
		} else {
			setSelection(length());
		}
	}

	protected void replaceText(CharSequence text) {
		clearComposingText();
		setText(text);
		setTextSelection(false);
	}

	public void setCommitCompletionListener(CommitCompletionListener listener) {
		mCommitCompletionListener = listener;
	}

	private InputMethodManager getInputMethodManager() {
		return (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
	}

	public void showInputMethod() {
		InputMethodManager imm = getInputMethodManager();
		if (imm != null) {
			imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT);
		}
	}

	public void hideInputMethod() {
		InputMethodManager imm = getInputMethodManager();
		if (imm != null) {
			imm.hideSoftInputFromWindow(getWindowToken(), 0);
		}
	}

	@Override
	public void onCommitCompletion(CompletionInfo completion) {
		if (DBG) Log.d(TAG, "onCommitCompletion(" + completion + ")");
		hideInputMethod();
		replaceText(completion.getText());
		if (mCommitCompletionListener != null) {
			mCommitCompletionListener.onCommitCompletion(completion.getPosition());
		}
	}

	public interface CommitCompletionListener {
		void onCommitCompletion(int position);
	}

	private View.OnKeyListener mOnLeftCTRListener = new View.OnKeyListener() {

		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			if(keyCode == KeyEvent.KEYCODE_CTRL_LEFT) {
				setText("");
				return true;
			}
			return false;
		}
	};

}
