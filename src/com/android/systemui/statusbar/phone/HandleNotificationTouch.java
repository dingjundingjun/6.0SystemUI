package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.keyguard.KeyguardStatusView;
import com.android.systemui.R;
import com.android.systemui.statusbar.StatusBarState;
import com.android.systemui.statusbar.qscontroll.BlurControlView;
import com.android.systemui.statusbar.qscontroll.QuickSettingGridView;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import com.dingjun.debug.Debug;

/**
 * @author dingj
 * 处理下拉通知栏和锁屏通知栏的各种触摸状态、动画等
 */
public class HandleNotificationTouch {
	private Context mContext;
	private int VELOCITY_DY = 1500;
	/**通知栏状态，主要是判断锁屏和非锁屏状态*/
	private int mStatusBarState = StatusBarState.SHADE;
	/**通知栏收缩动画结束**/
	private final int NOTIFICATION_COLLOPSE_END = 1;
	/**通知栏展开动画结束*/
	private final int NOTIFICATION_EXPAND_END = 2;
	/**点击了右边的区域，相机*/
	private final int TOUCH_IN_RIGHT_AREA = 1;
	/**点击了左边的区域，通话*/
	private final int TOUCH_IN_LEFT_AREA = 2;
	/**这个是整个expandLayout*/
	private NotificationPanelView mNotificationPanelView;
    /**这个是下拉通知栏里的通知栏*/
    private NotificationStackScrollLayout mNotificationStackScroller;
	private float mLastNotificationPositionY;
    private float mNotificationContainerInitY;
    private ImageView mPullStatusBar;
    private QuickSettingGridView mQuickSettingGridView;
    private View mNotificationClockLayout;
    private View mNotificationBottomLayout;
    private NotificationsQuickSettingsContainer mNotificationContainerParent;
    private NotificationExpandAnimateUpdateListener mNotificationExpandAnimateUpdateListener = new NotificationExpandAnimateUpdateListener();
    private NotificationCollopseAnimateUpdateListener mNotificationCollopseAnimateUpdateListener = new NotificationCollopseAnimateUpdateListener(); 
    private ImageView mCameraBtn;
    private ImageView mPhoneCallBtn;
    private TextView mUpToUnlock;
    /**快速进入app的提示，锁屏界面上拉通知栏，会出现*/
    private ImageView mGoToAPPTipView;
    PanelBar mBar;
    /**动画Handle处理*/
    private AnimateHandle mAnimateHandle;
    /**锁屏 触摸的区域，判断是打开相机还是通话*/
    private int mTouchInMode = -1;
    /**锁屏界面点击了相机或者通话*/
    private boolean isTouchInQuickApp = false;
    /**加速度*/
    private VelocityTracker mVTracker = null;
    /**显示锁屏时钟*/
    private KeyguardStatusView mKeyguardStatusView;
    /**毛玻璃view*/
    private BlurControlView mBlurControlView;
    
	public HandleNotificationTouch(Context mContext) {
		this.mContext = mContext;
	}

	public boolean onTouchEvent(MotionEvent event)
	{
		return setContainerParentTransLationY(event);
	}
	
	public void setNotificationView(NotificationPanelView view)
	{
		mNotificationPanelView = view;
		mNotificationContainerParent = (NotificationsQuickSettingsContainer)mNotificationPanelView.findViewById(R.id.notification_container_parent);
		mNotificationStackScroller = (NotificationStackScrollLayout)mNotificationPanelView.findViewById(R.id.notification_stack_scroller);
		mNotificationClockLayout = mNotificationPanelView.findViewById(R.id.notification_container_clock_layout);
        mNotificationBottomLayout = mNotificationPanelView.findViewById(R.id.notification_container_bottom_layout);
        mUpToUnlock = (TextView)mNotificationPanelView.findViewById(R.id.up_to_unlock);
        mCameraBtn = (ImageView)mNotificationPanelView.findViewById(R.id.btn_camera);
        mPullStatusBar = (ImageView)mNotificationPanelView.findViewById(R.id.btn_status_bar_pull);
        mPhoneCallBtn = (ImageView)mNotificationPanelView.findViewById(R.id.btn_phone);
        mGoToAPPTipView = (ImageView)mNotificationPanelView.findViewById(R.id.go_to_app_tip);
        mAnimateHandle = new AnimateHandle();
	}
	
	public void setBar(PanelBar bar)
	{
		this.mBar = bar;
	}
	
	public void setStatusBarState(int state)
	{
		mStatusBarState = state;
	}
	
	public void updateState() {
  	  if(mStatusBarState == StatusBarState.KEYGUARD)
  	  {
  		  Debug.d("updateQsState = " + Thread.currentThread().getStackTrace()[2].getLineNumber());
  		  mNotificationClockLayout.setVisibility(View.INVISIBLE);
  		  mNotificationBottomLayout.setVisibility(View.INVISIBLE);
  		  mPullStatusBar.setVisibility(View.INVISIBLE);
  		  
  		  mCameraBtn.setVisibility(View.VISIBLE);
  		  mPhoneCallBtn.setVisibility(View.VISIBLE);
  		  mUpToUnlock.setVisibility(View.VISIBLE);
  		  mNotificationContainerParent.setY(getNotificationContainerParentMaxPosition());
  	  }
  	  else
  	  {
  		  Debug.d("updateQsState = " + Thread.currentThread().getStackTrace()[2].getLineNumber());
  		  mNotificationClockLayout.setVisibility(View.VISIBLE);
  		  mNotificationBottomLayout.setVisibility(View.VISIBLE);
  		  mPullStatusBar.setVisibility(View.VISIBLE);
  		  mCameraBtn.setVisibility(View.GONE);
  		  mPhoneCallBtn.setVisibility(View.GONE);
  		  mUpToUnlock.setVisibility(View.GONE);
  	  }
  }
	
	private boolean setContainerParentTransLationY(MotionEvent event)
    {
		final float y = event.getY();
		final float x = event.getX();
		switch (event.getActionMasked()) {
		case MotionEvent.ACTION_DOWN: {
//			if(mStatusBarState != StatusBarState.KEYGUARD)
//			{
//				mNotificationPanelView.mNotificationStackScroller.showShadeModeNotification();
//			}
			if(mVTracker == null){    
				mVTracker = VelocityTracker.obtain();    
            }else{    
            	mVTracker.clear();    
            }    
			mVTracker.addMovement(event);   
			mNotificationContainerParent.animate().cancel();
			mLastNotificationPositionY = mNotificationContainerParent.getY();
			mNotificationContainerInitY = y;
			Debug.d("ACTION_DOWN mLastNotificationPositionY = " + mLastNotificationPositionY + " mNotificationContainerInitY = " + mNotificationContainerInitY);
			if(!mNotificationPanelView.isShown())
			{
				mNotificationPanelView.setVisibility(View.VISIBLE);
			}
			
			if(!mNotificationStackScroller.isShown())
			{
				mNotificationStackScroller.setVisibility(View.VISIBLE);
			}
			else
			{
//				Debug.d("mNotificationStackScroller height = " + mNotificationStackScroller.getHeight());
//				Debug.d("mNotificationStackScroller width = " + mNotificationStackScroller.getWidth());
			}
			if(mStatusBarState == StatusBarState.KEYGUARD)
			{
				isTouchInQuickApp = touchInQuickApp(x,y);
				if(isTouchInQuickApp)
				{
					mGoToAPPTipView.setVisibility(View.VISIBLE);
					mGoToAPPTipView.setY(mNotificationContainerParent.getY() + mNotificationContainerParent.getHeight());
				}
				else
				{
					mGoToAPPTipView.setVisibility(View.GONE);
				}
			}
			if(isContainerParentExpanded())
			{
				return false;
			}
			break;
		}
		case MotionEvent.ACTION_MOVE: {
			Debug.d("ACTION_MOVE");
			mVTracker.addMovement(event);
			mVTracker.computeCurrentVelocity(1000); 
			Debug.d("vTracker y = " + mVTracker.getYVelocity());
			if(mStatusBarState == StatusBarState.KEYGUARD)
			{
				mNotificationPanelView.positionClockAndNotifications(y - mNotificationContainerInitY);
			}
			mNotificationContainerParent.setVisibility(View.VISIBLE);
//			mQsContainer.setVisibility(View.INVISIBLE);
			if(!mNotificationPanelView.isShown())
			{
//				Log.d(TAG, "ACTION_MOVE NotificationView is not shown");
				mNotificationPanelView.setVisibility(View.VISIBLE);
			}
			if(!mNotificationStackScroller.isShown())
			{
//				Log.d("dingjun", "ACTION_MOVE mNotificationStackScroller visibility ");
				mNotificationStackScroller.setVisibility(View.VISIBLE);
			}
//			Log.d(TAG, "mNotificationContainer height = " + mNotificationContainerParent.getHeight());
//			Log.d(TAG, "mNotificationContainerInitY " + " mLastNotificationPositionY = "
//					+ mLastNotificationPositionY
//					+ " mNotificationContainerInitY = "
//					+ mNotificationContainerInitY + " y = " + y + " mNotifiHeight = " + mNotificationContainerParent.getHeight());
			if(isContainerParentExpanded() && y - mNotificationContainerInitY > 0)
    		{
				Debug.d("isContainerParentExpanded y = " + y + " mNotificationContainerInitY = " + mNotificationContainerInitY);
    			return true;
    		}else if(isContainerParentCollapsed() && y - mNotificationContainerInitY < 0)
    		{
    			Debug.d("isContainerParentCollapsed y = " + y + " mNotificationContainerInitY = " + mNotificationContainerInitY);
    			return true;
    		}
			float disY = mLastNotificationPositionY + y - mNotificationContainerInitY;
//			Debug.d(" disY = " + disY + " mLastNotificationPositionY = "
//					+ mLastNotificationPositionY
//					+ " mNotificationContainerInitY = "
//					+ mNotificationContainerInitY + " y = " + y);
    		disY = disY > 0 ? 0 : disY;
    		mNotificationContainerParent.setY(disY);
    		setBlurFraction(1 - disY / getNotificationContainerParentMinPosition());
    		if(isTouchInQuickApp)
    		{
    			mGoToAPPTipView.setY(mNotificationContainerParent.getY() + mNotificationContainerParent.getHeight());
    		}
			break;
		}
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL: {
			Debug.d("ACTION_UP OR ACTION_CANCEL");
			Debug.d("vTracker y = " + mVTracker.getYVelocity());
			float yveloc = mVTracker.getYVelocity();
			mVTracker.clear(); 
			if(yveloc > VELOCITY_DY)
			{
				Debug.d("setMaxPosition");
				startAnimateExpand();
			}
			else if(yveloc < -1 * VELOCITY_DY)
			{
				Debug.d("setMinPosition");
				startAnimateCollopse();
			}
			else
			{
				if(isContainerParentOverHalf())
				{
					Debug.d("setMaxPosition");
					startAnimateExpand();
				}
				else
				{
					Debug.d("setMinPosition");
					startAnimateCollopse();
				}
			}
			break;
		}
		}
		return true;
    }
	
	/**
	 * 判断是否点击了相机或者电话
	 * @return
	 */
	private boolean touchInQuickApp(float x,float y) {
		boolean bTouch = false;
		if (mCameraBtn.isShown()) 
		{
				Debug.d("judge left bottom container");
				bTouch |= (x > mCameraBtn.getX()
						&& x < (mCameraBtn.getX() + mCameraBtn.getWidth())
						&& y > mCameraBtn.getY() && y < (mCameraBtn.getY() + mCameraBtn
						.getHeight()));
				if(bTouch)
				{
					mTouchInMode = TOUCH_IN_RIGHT_AREA;
					return true;
				}
		}
		if(mPhoneCallBtn.isShown())
		{
			Debug.d("judge right bottom container");
			bTouch |= (x > mPhoneCallBtn.getX()
					&& x < (mPhoneCallBtn.getX() + mPhoneCallBtn.getWidth())
					&& y > mPhoneCallBtn.getY() && y < (mPhoneCallBtn.getY() + mPhoneCallBtn
					.getHeight()));
			if(bTouch)
			{
				mTouchInMode = TOUCH_IN_LEFT_AREA;
			}
		}
		return bTouch;
	}

	private boolean isContainerParentOverHalf()
    {
    	if(mNotificationContainerParent.getY() > getNotificationContainerParentMinPosition() / 2)
    	{
    		return true;
    	}
    	else
    	{
    		return false;
    	}
    }
    
    private float getNotificationContainerParentMinPosition()
    {
    	return -1*mNotificationPanelView.getMaxPanelHeight();
    }
    
    private float getNotificationContainerParentMaxPosition()
    {
    	return 0;
    }
    private boolean isContainerParentExpanded()
    {
    	
    	return mNotificationContainerParent.getY() == 0;
    }
    
    private boolean isContainerParentCollapsed()
    {
    	return mNotificationContainerParent.getY() == getNotificationContainerParentMinPosition();
    }
    
    private void collseContainerParent()
    {
    	mNotificationContainerParent.setY(getNotificationContainerParentMinPosition());
    }
    
    /**
     * 展开的动画
     */
    private void startAnimateExpand()
    {
		mNotificationContainerParent.animate()
				.translationY(getNotificationContainerParentMaxPosition())
				.setUpdateListener(mNotificationExpandAnimateUpdateListener).withEndAction(new Runnable() {
					@Override
					public void run() {
						mAnimateHandle
						.sendEmptyMessage(NOTIFICATION_EXPAND_END);
						setBlurFraction(1.0f);
					}
				});
    }
    
    /**
     * 收缩的动画
     */
    private void startAnimateCollopse()
    {
		mNotificationContainerParent.animate().
				translationY(getNotificationContainerParentMinPosition())
				.withEndAction(new Runnable() {
					@Override
					public void run() {
						mAnimateHandle
								.sendEmptyMessage(NOTIFICATION_COLLOPSE_END);
						setBlurFraction(0.0f);
					}
				}).setUpdateListener(mNotificationCollopseAnimateUpdateListener);
    }
   
    /**
     * @author dingj
     * 展开通知栏动画监听
     */
    public class NotificationExpandAnimateUpdateListener  implements ValueAnimator.AnimatorUpdateListener
    {
		@Override
		public void onAnimationUpdate(ValueAnimator arg0) {
			Debug.d("expaned onAnimationUpdate y = " + mNotificationContainerParent.getY());
			if(mStatusBarState == StatusBarState.KEYGUARD)
			{
				mNotificationPanelView
						.positionClockAndNotifications(mNotificationContainerParent
								.getY() - getNotificationContainerParentMaxPosition());
			}
			if(isTouchInQuickApp)
			{
				mGoToAPPTipView.setY(mNotificationContainerParent.getY()
						+ mNotificationContainerParent.getHeight());
			}
			setBlurFraction(1 - mNotificationContainerParent.getY() / getNotificationContainerParentMinPosition());
		}
    	
    }
    
    /**
     * @author dingj
     * 收缩通知栏动画监听
     */
    public class NotificationCollopseAnimateUpdateListener  implements ValueAnimator.AnimatorUpdateListener
    {
		@Override
		public void onAnimationUpdate(ValueAnimator arg0) {
			Debug.d("Collopse onAnimationUpdate y = " + mNotificationContainerParent.getY());
			if(isTouchInQuickApp)
			{
				mGoToAPPTipView.setY(mNotificationContainerParent.getY()
						+ mNotificationContainerParent.getHeight());
			}
			setBlurFraction(1 - mNotificationContainerParent.getY() / getNotificationContainerParentMinPosition());
		}
    }
    
    public class AnimateHandle extends Handler
    {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch(msg.what)
			{
			case NOTIFICATION_COLLOPSE_END:
			{
				mBar.onAllPanelsCollapsed();
				mKeyguardStatusView.setVisibility(View.INVISIBLE);
				if(isTouchInQuickApp)
				{
					mGoToAPPTipView.setVisibility(View.GONE);
					if(mTouchInMode == TOUCH_IN_RIGHT_AREA)
					{
						goToCamera();
					}
					else if(mTouchInMode == TOUCH_IN_LEFT_AREA)
					{
						goToPhoneCall();
					}
				}
				break;
			}
			case NOTIFICATION_EXPAND_END:
			{
				if(isTouchInQuickApp)
				{
					mGoToAPPTipView.setVisibility(View.GONE);
				}
				break;
			}
			}
		}
    }
    
    private void goToCamera()
    {
    	Debug.d("进入相机 111111111111111111111111111111111111");
    }
    
    private void goToPhoneCall()
    {
    	Debug.d("进入电话 111111111111111111111111111111111111");
    }
    
    public void free()
    {
    	if(mVTracker != null)
    	{
    		mVTracker.recycle();
    	}
    }

	public void setKeyguardStatusView(KeyguardStatusView keyguardStatusView) {
		mKeyguardStatusView = keyguardStatusView;
	}

	public void setBlurView(BlurControlView mBlurView) {
		mBlurControlView = mBlurView;
	}
	
	private void setBlurFraction(float f)
	{
		Debug.d("setBlurFraction f = " + f);
		if(mBlurControlView != null && mBlurControlView.isBlurEnable())
		{
			mBlurControlView.setFraction(f);
		}
	}
}
