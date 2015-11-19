package com.android.systemui.statusbar.qscontroll;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration;
import android.view.View;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;

import java.util.List;

import com.android.systemui.R;


/**
 * 
 * Wifi Notification toggle controller.
 * 
 * </br>
 * 
 * @author hmm@dw.gdbbk.com
 *
 */
public class WifiToggleController extends NotificationToggleController {
	
    public final static String TYPE = "WifiToggleController";

	private final static String DEFAULT_LABEL = "WLAN";
	
    public final static int WIFI_STATUS_DISABLING = WifiManager.WIFI_STATE_DISABLING;  // 0
    public final static int WIFI_STATUS_DISABLED = WifiManager.WIFI_STATE_DISABLED;    // 1
    public final static int WIFI_STATUS_ENABLING = WifiManager.WIFI_STATE_ENABLING;    // 2
    public final static int WIFI_STATUS_ENABLED = WifiManager.WIFI_STATE_ENABLED;      // 3
    public final static int WIFI_STATUS_UNKNOWN = WifiManager.WIFI_STATE_UNKNOWN;      // 4
	
    
    private boolean mClickFromDisabled = false;
	private WifiManager mWifiMgr = null;
	private WifiStatusReceiver mReceiver = null;
	
	
    public WifiToggleController() {
    	this(null, null);
    }
	
    public WifiToggleController(Context context, INotificationToggle toggle) {
    	super(context, toggle);
    }
    
	@Override
	protected boolean initImpl() {
		mWifiMgr = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		mReceiver = new WifiStatusReceiver();
		return mReceiver.startListening();
	}
	
	@Override
	protected boolean freeImpl() {
		return mReceiver.stopListening();
	}

    @Override
    public String getType() {
        return TYPE;
    }
	
	@Override
	public String getLabel() {
		String label = null;
		try {
			label = mContext.getResources().getString(
					R.string.wifi_toggle_label);
		} catch (Exception e) {
			e.printStackTrace();
			label = null;
		}
		
		if (null == label) {
			label = DEFAULT_LABEL;
		}
		return label;
	}
	
	@Override
	public Drawable getIconDrawable() {
		try {
			Log.d("dingjun","getIconDrawable 111111111111111111 context = " + mContext);
			return mContext.getResources().
					getDrawable(R.drawable.ic_wifi_toggle);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

    @Override
    public Drawable getBkDrawable() {
        try {
            return mContext.getResources().
                getDrawable(R.drawable.notification_toggle_bk);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Drawable getIconBkDrawable() {
        return null;
    }

    @Override
    public ColorStateList getLabelColor() {
        try {
            return mContext.getResources().
                getColorStateList(R.color.notification_label_color_list);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

	@Override
	public void updateStatus() {
		onStatusChanged(mWifiMgr.getWifiState());
	}
	
	private void onStatusChanged(int status) {
		mIconLevel = status;
		
		if (null != mToggle) {
			mToggle.setIconLevel(status);
		}

        // if user toggle the wifi on and there have not connected wifi, 
        // we go to the wifi settings ui.
        if (WIFI_STATUS_ENABLED == status) {
            if (mClickFromDisabled && !haveConfiguredWifi()) {
                onLongClick(null);
            }
            mClickFromDisabled = false;
        }

        if (WIFI_STATUS_DISABLED == status 
                || WIFI_STATUS_DISABLING == status) {
            mToggle.makeSelected(false);
        } else {
            mToggle.makeSelected(true);
        }
    }

    @Override
    public void onClick(View view) {
        switch (mIconLevel) {
            case WIFI_STATUS_ENABLED:
            mClickFromDisabled = false;
			mWifiMgr.setWifiEnabled(false);
			break;
			
		case WIFI_STATUS_UNKNOWN:
		case WIFI_STATUS_DISABLED:
            mClickFromDisabled = true;
			mWifiMgr.setWifiEnabled(true);
			break;
		
		case WIFI_STATUS_ENABLING:
		case WIFI_STATUS_DISABLING:
		default:
			break;
		}
	}
	
	@Override
	public void onLongClick(View view) {
		// long press go to wifi setting.
		Intent intent = new Intent();
		intent.setAction(WIFI_SETTING_ACTION);
		//intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		try {
            mContext.startActivityAsUser(intent, new UserHandle(UserHandle.USER_CURRENT));
            closeStatusBar();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    private boolean haveConfiguredWifi() {
        List<WifiConfiguration> list = mWifiMgr.getConfiguredNetworks();
        if (null == list || list.isEmpty()) {
            return false;
        } else {
            return true;
        }
    }
	
    public class WifiStatusReceiver extends BroadcastReceiver {
    	
    	// avoid duplication of registration
    	private boolean mIsRegistered = false; 
    	private IntentFilter mIntentFilter;
    	
    	protected WifiStatusReceiver() {
    		mIsRegistered = false;
    		mIntentFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
    		mIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
    	}
    	
    	public boolean startListening() {
    		if (!mIsRegistered) {
    			try {
    				mContext.registerReceiver(this, mIntentFilter);
    				mIsRegistered = true;
    			} catch (Exception e) {
    				e.printStackTrace();
    				mIsRegistered = false;
    			}
    		}
    		
    		return mIsRegistered;
    	}
    	
    	public boolean stopListening() {
    		if (mIsRegistered) {
    			try {
    				mContext.unregisterReceiver(this);
    				mIsRegistered = false;
    			} catch (Exception e) {
    				e.printStackTrace();
    			}
    		}
    		
    		return !mIsRegistered;
    	}
    	
    	@SuppressWarnings("deprecation")
		@Override
    	public void onReceive(Context context, Intent intent) {
    		String action = intent.getAction();
    		// listen in WIFI status
    		if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
    			onStatusChanged(mWifiMgr.getWifiState());
    		} else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
    			// listen in connection status
    			NetworkInfo networkInfo = intent.getParcelableExtra(
    					ConnectivityManager.EXTRA_NETWORK_INFO);
    			if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    int status = mWifiMgr.getWifiState();
    				if (networkInfo.isConnected()) {
                        // someting the status will be change from disabling to enabled(but the action is turn off the wifi)
                        // the disabled status will coming soon. is there our wifi bug ??
                        if (WIFI_STATUS_DISABLING != status) {
                            onStatusChanged(WIFI_STATUS_ENABLED);
                        } else {
                            Slog.d(TYPE, "receive a enabled staus from disabling, this is not normal, we ignore it !");
                        }
    				} else {
                        if (WIFI_STATUS_ENABLING != status) {
                            onStatusChanged(WIFI_STATUS_DISABLED);
                        } else {
                            Slog.d(TYPE, "receive a disabled staus from enabling, this is not normal, we ignore it !");
                        }
    				}
    			}
    		}
    	}
    }
    
}
