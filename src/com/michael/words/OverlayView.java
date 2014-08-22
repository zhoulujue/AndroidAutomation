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
	/** �ȴ��û����״̬����ʱ���ܻ�ȡ��¼�ĵ������ */
	public static final int TOUCH_STATUS_WAITING = 0;
	/** �û����ڵ������ʱ���ܻ�ȡ��¼�ĵ������ */
	public static final int TOUCH_STATUS_TOUCHING = 1;
	/** �û������ϣ���ʱ�ܹ���ȡ��¼�ĵ������ */
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
			//�������ڵ��״̬
			mTouchStatus = TOUCH_STATUS_TOUCHING;

			int iAction = event.getAction();
			if (iAction == MotionEvent.ACTION_CANCEL 
					|| iAction == MotionEvent.ACTION_DOWN
					|| iAction == MotionEvent.ACTION_MOVE)
			{
				//�����ϣ�����û�л�ȡ�����꣬���Խ��ŵȴ�
				mTouchStatus = TOUCH_STATUS_WAITING;
				return false;
			}
			mXCoord = (int) event.getX();
			mYCoord = (int) event.getY();
			//��ȡ����ɹ�����״̬����Ϊ���״̬����ʾ�����ݿ���ȡ
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
				//���췵�����ݣ�׼�����û���������귵��
				Point tmpPoint = new Point((int)mXCoord, (int)mYCoord);
				//����������¼����ı�����Ϊ��һ�μ�¼����׼��
				resetPoint();
				//��״̬��Ϊ�ȴ����ȴ��û���һ�ε��
				mTouchStatus = TOUCH_STATUS_WAITING;
				return tmpPoint;
			} else {
				return null;
			}
		}
	}

}
