package com.android.systemui.statusbar.qscontroll;

import java.util.ArrayList;
import java.util.List;

import com.android.systemui.R;
import com.android.systemui.statusbar.BaseStatusBar;
import com.dingjun.debug.Debug;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;

public class QuickSettingGridView extends LinearLayout{
	private final String TAG = "QuickSettingGridView";
	private final int COLUMN_NUM = 5;
	private Context mContext;
	private BaseStatusBar mBar;
	private boolean isDraging = false;
	private float mPrimayHeight;
	private int mExpandedHeight = 800;
	private int mCollopHeight = 300;
	private List<View> mChildViews = new ArrayList<View>();
	public QuickSettingGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		init(context);
	}
	
	public void init(Context context)
	{
		mContext = context;
		this.setOrientation(LinearLayout.VERTICAL);
		String[] qsItems = mContext.getResources().getStringArray(R.array.status_bar_qs_items);
		LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		NotificationToggleController controller = null;
		NotificationToggle toggle;
		for(int i = 0;i < qsItems.length;i++)
		{
			controller = (NotificationToggleController)getObject(qsItems[i]);
			if(controller == null)
			{
				Log.e(TAG,"instance " + qsItems[i] + " failed!");
				continue;
			}
			toggle = (NotificationToggle)inflater.inflate(R.layout.notification_toggle, null);
			toggle.setupController(controller);
			mChildViews.add(toggle);
		}
		composeChild();
	}
	
	private void composeChild()
	{
		int size = mChildViews.size();
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.weight = 1;
		LinearLayout oneRow = null;
		for(int i = 0;i < size;i++)
		{
			if(i % COLUMN_NUM == 0)
			{
				oneRow = new LinearLayout(mContext);
				oneRow.setOrientation(LinearLayout.HORIZONTAL);
				this.addView(oneRow);
			}
			oneRow.addView(mChildViews.get(i), params);
		}
	}
	
	private Object getObject(String className)
	{
		Object obj = null;
		try {
			obj = Class.forName(className).newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return obj;
	}
	
	public void setBar(BaseStatusBar bar) {
		mBar = bar;
	}

	public void changedHeight(float dy)
	{
		Debug.d("changedHeigt primayHeight = " + mPrimayHeight + " dy = " + dy);
		LinearLayout.LayoutParams params = (LayoutParams) this.getLayoutParams();
		if(mPrimayHeight + dy > mExpandedHeight)
		{
			params.height = mExpandedHeight;
		}
		else if(mPrimayHeight + dy < mCollopHeight)
		{
			params.height = mCollopHeight;
			return;
		}
		else
		{
			params.height = (int) (mPrimayHeight + dy);
		}
		this.setLayoutParams(params);
	}
	
	public void setDraging(boolean b)
	{
		isDraging = b;
		mPrimayHeight = getHeight();
	}
	
	public void expandOrCollop()
	{
		if(this.getHeight() > (mExpandedHeight - mCollopHeight)/2)
		{
			expend();
		}
		else
		{
			collop();
		}
	}
	
	public void expend()
	{
		LinearLayout.LayoutParams params = (LayoutParams) this.getLayoutParams();
		params.height = mExpandedHeight;
		this.setLayoutParams(params);
	}
	
	public void collop()
	{
		LinearLayout.LayoutParams params = (LayoutParams) this.getLayoutParams();
		params.height = mCollopHeight;
		this.setLayoutParams(params);
	}
	
	/*public class QSAdapter extends BaseAdapter
	{
		@Override
		public int getCount() {
			return mChildViews.size();
		}

		@Override
		public Object getItem(int position) {
			if (null == mChildViews || mChildViews.size() <= 0
					|| position >= mChildViews.size()) {
				return null;
			}
			return mChildViews.get(position);
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup arg2) 
		{
			if (null == mChildViews || position < 0
					|| position >= mChildViews.size()) {
				Log.d(TAG, "index over range: pos: " + position + " total: "
						+ mChildViews.size());
				return convertView;
			}
			if (convertView == null) {
				convertView = mChildViews.get(position);
			}

			NotificationToggle toggle = null;
			try {
				toggle = (NotificationToggle) convertView;
			} catch (Exception e) {
				e.printStackTrace();
				toggle = null;
			}
			if (null != toggle) {
				toggle.setBar(mBar);
			}
			return convertView;
		}
	}*/

}
