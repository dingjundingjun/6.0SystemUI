package com.android.systemui.statusbar.qscontroll;

import com.android.systemui.R;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.View;

public class BlueToothToggleController extends NotificationToggleController
{

	public final static String TYPE = "BlueToothToggleController";
	public BlueToothToggleController() {
		super();
	}

	public BlueToothToggleController(Context context, INotificationToggle toggle) {
		super(context, toggle);
	}

	@Override
	protected boolean initImpl() {
		return true;
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	protected boolean freeImpl() {
		return true;
	}

	@Override
	public String getLabel() {
		String label = null;
		label = mContext.getResources().getString(R.string.bluetooth_toggle_label);
		return label;
	}

	@Override
	public Drawable getIconDrawable() {
			return mContext.getResources().getDrawable(R.drawable.ic_bluetooth_toggle);
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
		// TODO Auto-generated method stub
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
		int status = getStatus();
		if(status == 0)
		{
			mToggle.setIconLevel(1);
			mToggle.makeSelected(false);
		}
		else
		{
			mToggle.setIconLevel(0);
			mToggle.makeSelected(true);
		}
	}

	@Override
	public void onClick(View view) {
		updateStatus();
	}

	@Override
	public void onLongClick(View view) {
		// TODO Auto-generated method stub
		
	}

}
