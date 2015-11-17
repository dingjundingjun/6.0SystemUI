package com.android.systemui.statusbar.qscontroll;

import com.android.systemui.R;
import com.android.systemui.statusbar.BaseStatusBar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class QuickSettingLayout extends LinearLayout
{
	private Context mContext;
	private final int QS_CONTROL_EXPEND = 0;
    private final int QS_CONTROL_COLLOP = 1;
    private QuickSettingGridView mQuickSettingGridView;
    private ImageView mBtnQSControl;
    private BaseStatusBar mStatusBar;
    private float mInitY;
    private boolean isDrag = false;
	public QuickSettingLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	
	private void init(Context context)
	{
		mContext = context;
	}
	
	@Override
    protected void onFinishInflate() {
		mQuickSettingGridView = (QuickSettingGridView)findViewById(R.id.qs_grid);
		mBtnQSControl = (ImageView)findViewById(R.id.btn_qs_control);
        mBtnQSControl.setImageLevel(QS_CONTROL_EXPEND);
    }
	
	public void setBar(BaseStatusBar bar)
	{
		mStatusBar = bar;
		if(mQuickSettingGridView != null)
		{
			mQuickSettingGridView.setBar(mStatusBar);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float x = event.getX();
		float y = event.getY();
		if(!isInControl(x, y))
		{
			return super.onTouchEvent(event);
		}
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN: {
			mInitY = y;
			isDrag = true;
			break;
		}
		case MotionEvent.ACTION_MOVE: {
			float dy = y - mInitY;
			break;
		}
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL: {
			break;
		}
		}
		return true;
	}
	
	private boolean isInControl(float x,float y)
	{
		if(y > mBtnQSControl.getY())
		{
			return true;
		}
		return false;
	}

	
}