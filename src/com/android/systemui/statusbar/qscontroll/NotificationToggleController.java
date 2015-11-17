package com.android.systemui.statusbar.qscontroll;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import android.util.Slog;

import com.android.systemui.R;

import com.android.systemui.statusbar.BaseStatusBar;
import com.android.systemui.statusbar.CommandQueue;

/**
 * 
 * Notification toggle controller.
 * This is hold by {@link NotificationToggle}. The controller specific the toggle icon, label and implement
 * click(long press) function. But this is a base class, you must implement this by sub class.
 * 
 * </br>
 * 
 * @author hmm@dw.gdbbk.com
 *
 */
public abstract class NotificationToggleController {
	
	// sysetm settings URI, copy from android.provider.Settings$System
    protected final static Uri SETTINGS_SYSTEM_CONTENT_URI = Uri.parse("content://settings/system");
	
	protected final static String SYSTEM_SETTING_ACTION  = "android.settings.SETTINGS";
	protected final static String WIFI_SETTING_ACTION    = "android.settings.WIRELESS_SETTINGS";
	protected final static String SOUND_SETTING_ACTION   = "eebbk.settings.SOUND_SETTINGS";
	protected final static String DISPLAY_SETTING_ACTION = "eebbk.settings.DISPLAY_SETTINGS";
	
	protected Context mContext = null;
    protected BaseStatusBar mBar = null;
	protected boolean mIsInited = false;
	
	protected int mIconLevel = 0;
	protected INotificationToggle mToggle = null;
	
	
    public NotificationToggleController() {
    	this(null, null);
    }
	
    public NotificationToggleController(Context context, INotificationToggle toggle) {
    	mContext = context;
    	mToggle = toggle;
    	mIsInited = false;
    }
    
    /**
     * Initialize the controller.
     * This will call sub class initImpl.
     * 
     * @param context Object of {@link Context}
     * @param toggle Toggle view interface.
     */
	public void initialize(Context context, INotificationToggle toggle) {
    	mContext = context;
    	mToggle = toggle;
		
		if (mIsInited) {
			return;
		}
		
		mIsInited = initImpl();
	}

    /**
     * Set status bar implement.
     *
     * @param bar Object of {@link BaseStatusBar}
     */
    public void setBar(BaseStatusBar bar) {
        mBar = bar;
    }
	
	/**
	 * Release the controller resource.
	 * This will call sub class freeImpl.
	 */
	public void free() {
		if (mIsInited) {
			mIsInited = !freeImpl();
		}
	}
    
	/**
	 * Get current controller icon status(level).
	 * 
	 * @return Icon status.
	 */
    public int getStatus() {
    	return mIconLevel;
    }
    
    /**
     * Update icon show.
     */
    protected void updatIcon() {
    	if (null != mToggle) {
    		mToggle.setIconLevel(mIconLevel);
    	}
    }

    /**
     * Close status bar.
     */
    protected void closeStatusBar() {
        if (null != mBar) {
            if (null == mContext) {
                mBar.animateCollapsePanels(CommandQueue.FLAG_EXCLUDE_NONE);
            } else {
                mBar.animateCollapsePanels(CommandQueue.FLAG_EXCLUDE_RECENTS_PANEL);
                // we force close recents activity manually
//                Intent closeIntent = new Intent(RecentsActivity.CLOSE_RECENTS_INTENT);
//                closeIntent.putExtra(RecentsActivity.FORCE_CLOSE, true);
//                mContext.sendBroadcast(closeIntent);
            }
        }
    }
    
    /**
     * Sub class initialize.
     * 
     * @return True initialize has done, otherwise false.
     */
    protected abstract boolean initImpl();

    /**
     * Sub class specific self type.
     * Caller can use this get the controller type.
     * 
     * @return Controller type.
     */
    public abstract String getType();
    
    /**
     * Sub class free.
     * 
     * @return True free has done, otherwise false.
     */
    protected abstract boolean freeImpl();
    
    /**
     * Sub class specific the label text.
     * Notification toggle will call it to get label.
     * 
     * @return {@link String} of label.
     */
    public abstract String getLabel();
    
    /**
     * Sub class specific the icon drawable.
     * Notification toggle will call it to get icon.
     * 
     * @return {@link Drawable} of icon.
     */
    public abstract Drawable getIconDrawable();

    /**
     * Sub class specific the background drawable.
     * Notification toggle will call it to get background.
     * 
     * @return {@link Drawable} of background.
     */
    public abstract Drawable getBkDrawable();

    /**
     * Sub class specific the icon background drawable.
     * Notification toggle will call it to get icon background.
     * If you don't want the icon background, return null.
     *  
     * @return {@link Drawable} of background.
     */
    public abstract Drawable getIconBkDrawable();

    /**
     * Sub class specific the label color.
     * This is a {@link ColorStateList} which can implement click, checked effect.
     * Notification toggle will call it to get label color.
     * If you don't set(return null), it will use the default color.
     * 
     * @return {@link ColorStateList} of label color.
     */
    public abstract ColorStateList getLabelColor();

    /**
     * Sub class update icon status.
     */
    public abstract void updateStatus();
    
    /**
     * Sub class implement click function.
     * 
     * @param view Click view.
     */
    public abstract void onClick(View view);
    
    /**
     * Sub class implement long press function.
     * 
     * @param view Long press view.
     */
    public abstract void onLongClick(View view);
    
}
