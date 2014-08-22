package com.michael.words;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class OverlayView extends View {

	public interface OnTouchFinishListener {
		public abstract void onTouchFinish();
	}

	public void setOnTouchFinishListener(OnTouchFinishListener l) {
		mOnTouchFinishListener = l;
	}
	
	private OnTouchFinishListener mOnTouchFinishListener;
	private Context mContext;
	private float mXCoord;
	private float mYCoord;
	private int mTouchStatus;
	/** 等待用户点击状态，此时不能获取记录的点击坐标 */
	public static final int TOUCH_STATUS_WAITING = 0;
	/** 用户正在点击，此时不能获取记录的点击坐标 */
	public static final int TOUCH_STATUS_TOUCHING = 1;
	/** 用户点击完毕，此时能够获取记录的点击坐标 */
	public static final int TOUCH_STATUS_FINISH = 2;

	public OverlayView(Context context) {
		super(context);
		mContext = context;
		init();
	}
	
	public OverlayView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;

	}
	
	public OverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mContext = context;
		init();
	}

	private void init() {
		mXCoord = -1;
		mYCoord = -1;
		mTouchStatus = TOUCH_STATUS_WAITING;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		synchronized(this) {
			//进入正在点击状态
			mTouchStatus = TOUCH_STATUS_TOUCHING;

			int iAction = event.getAction();
			if (iAction == MotionEvent.ACTION_CANCEL 
					|| iAction == MotionEvent.ACTION_DOWN
					|| iAction == MotionEvent.ACTION_MOVE)
			{
				//点击完毕，但是没有获取到坐标，所以接着等待
				mTouchStatus = TOUCH_STATUS_WAITING;
				return false;
			}
			mXCoord = (int) event.getX();
			mYCoord = (int) event.getY();
			//获取坐标成功，将状态设置为完成状态，表示有数据可以取
			mTouchStatus = TOUCH_STATUS_FINISH;
			((Activity) mContext).dispatchTouchEvent(event);
			mOnTouchFinishListener.onTouchFinish();
			return false;
			//return super.onTouchEvent(event);
		}
	}

	private void resetPoint() {
		mXCoord = -1;
		mYCoord = -1;
	}
	
	public int getTouchStatus() {
		return mTouchStatus;
	}
	
	public Point getCoord() {
		synchronized(this) {
			if (mTouchStatus == TOUCH_STATUS_FINISH) {
				//构造返回数据，准备将用户点击的坐标返回
				Point tmpPoint = new Point((int)mXCoord, (int)mYCoord);
				//重置用来记录坐标的变量，为下一次记录做好准备
				resetPoint();
				//将状态置为等待，等待用户下一次点击
				mTouchStatus = TOUCH_STATUS_WAITING;
				return tmpPoint;
			} else {
				return null;
			}
		}
	}

}
