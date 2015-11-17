package com.android.systemui.statusbar.qscontroll;

import java.util.ArrayList;
import java.util.List;

import com.android.systemui.R;
import com.android.systemui.statusbar.BaseStatusBar;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;

public class QuickSettingGridView extends GridView{
	private final String TAG = "QuickSettingGridView";
	private Context mContext;
	private BaseStatusBar mBar;
	private QSAdapter mQSAdapter;
	private List<View> mChildViews = new ArrayList<View>();
	public QuickSettingGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	
	public void init(Context context)
	{
		mContext = context;
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
		mQSAdapter = new QSAdapter();
		this.setAdapter(mQSAdapter);
		mQSAdapter.notifyDataSetChanged();
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
		if (null != mQSAdapter) {
			mQSAdapter.notifyDataSetChanged();
		}
	}

	 
	public class QSAdapter extends BaseAdapter
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
	}

}
