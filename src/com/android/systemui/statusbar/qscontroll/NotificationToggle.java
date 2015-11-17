package com.android.systemui.statusbar.qscontroll;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.Slog;

import com.android.systemui.R;
import com.android.systemui.statusbar.BaseStatusBar;

/**
 * 
 * Notification toggle view.
 * It contain a status icon and label text. And each have a {@link NotificationToggleController}.
 * The status change, click and long press function is implement by controller.
 * 
 * </br>
 * 
 * @author hmm@dw.gdbbk.com
 *
 */
public class NotificationToggle extends LinearLayout implements INotificationToggle, 
	View.OnClickListener, View.OnLongClickListener {
	private final String TAG = "NotificationToggle";
	// flag record has been initialization
	private boolean mIsInited = false;
	
	private Context mContext = null;
	private NotificationToggleController mController = null;
	
	private ImageView mIvIcon = null;
	private TextView mTvLabel = null;
	
	
    public NotificationToggle(Context context) {
    	super(context);
    	init(context);
    }
    
    public NotificationToggle(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public NotificationToggle(Context context, AttributeSet attrs, int defStyle) {
    	super(context, attrs, defStyle);
    	init(context);
    }
    
    private void init(Context context) {
    	mContext = context;
    	mIsInited = false;
    	mController = null;
    	
    	setOnClickListener(this);
    	setOnLongClickListener(this);
    }
    
    @Override
    protected void onFinishInflate() {
    	mIvIcon = (ImageView) findViewById(R.id.notification_toggle_icon);
    	mTvLabel = (TextView) findViewById(R.id.notification_toggle_label);
    	initFromController();
    }
    
	@Override
	public void onClick(View view) {
		if (null != mController) {
			mController.onClick(view);
		}
	}
	
	@Override
	public boolean onLongClick(View view) {
		if (null != mController) {
			mController.onLongClick(view);
			return true;
		}
		
		return false;
	}

    @Override
    public void setIcon(Drawable icon) {
        if (null != mIvIcon) {
            mIvIcon.setImageDrawable(icon);
        }
    }

    @Override
    public void setLabel(String label) {
        if (null != mTvLabel) {
            mTvLabel.setText(label);
        }
    }

    @Override
    public void setIconLevel(int iconLevel) {
        if (null != mIvIcon) {
            mIvIcon.setImageLevel(iconLevel);
        }
    }

    @Override
    public void makeSelected(boolean selected) {
        if (null != mIvIcon) {
            mIvIcon.setSelected(selected);
        }
        if (null != mTvLabel) {
            // er ... setSelected, some time is chaos and suck.
            // use setEnabled replace it, whatever we don't click the text.
            //mTvLabel.setSelected(selected);
            mTvLabel.setEnabled(selected);
        }
        setSelected(selected);
    }

	/**
	 * Set the controller, and initiate toggle view with the controller.
	 * 
	 * @param controller Target {@link NotiifcationController}
	 */
	public void setupController(NotificationToggleController controller) {
		// free the old controller
		freeController();
		
		mController = controller;
		mController.initialize(mContext, this);
		initFromController();
	}

    /**
     * Reset the controller.
     */
    public void resetController() {
        if (null != mController) {
			mController.free();
            mController.initialize(mContext, this);
            initFromController();
        }
    }
	
	/**
	 * Release hold controller resource.
	 */
	public void freeController() {
		if (null != mController) {
			mController.free();
		}
	}
	
	/**
	 * Update toggle status show.
	 */
	public void updateStatus() {
		if (null != mController) {
			mController.updateStatus();
		}
	}

    /**
     * Get toggle type, in fact is controller type.
     * 
     * @return Toggle type.
     */
    public String getType() {
        if (null == mController) {
            return null;
        }

        return mController.getType();
    }

    /**
     * Get toggle holder controller.
     * 
     * @return Object of {@link NotificationToggleController}
     */
    public NotificationToggleController getController() {
        return mController;
    }

    /**
     * Set status bar implement.
     *
     * @param bar Object of {@link BaseStatusBar}
     */
    public void setBar(BaseStatusBar bar) {
		if (null != mController) {
            mController.setBar(bar);
        }
    }
	
	private void initFromController() {
		if (mIsInited) {
			return;
		}
		
		if (null == mTvLabel || null == mIvIcon || 
				null == mController) {
			return;
		}
		
		mController.updateStatus();
		mIvIcon.setImageDrawable(mController.getIconDrawable());
		mIvIcon.setImageLevel(mController.getStatus());
        mIvIcon.setBackground(mController.getIconBkDrawable());
        
		mTvLabel.setText(mController.getLabel());
        if (null != mController.getLabelColor()) {
            mTvLabel.setTextColor(mController.getLabelColor());
        }
		
		mIsInited = true;
	}

}
