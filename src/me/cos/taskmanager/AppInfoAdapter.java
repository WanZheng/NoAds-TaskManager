package me.cos.taskmanager;

import android.app.Activity;
import android.widget.ArrayAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.content.pm.ApplicationInfo;
import android.util.Log;

public class AppInfoAdapter extends ArrayAdapter<String> {
    private AppInfoCache mCache;
    private Activity mActivity;

    public AppInfoAdapter(Activity activity, AppInfoCache cache) {
	super(activity, android.R.layout.simple_list_item_multiple_choice);

	mActivity = activity;
	mCache = cache;
    }

    public boolean hasPackage(String packageName) {
	int count = getCount();
	for (int i=0; i<count; i++) {
	    String item = getItem(i);
	    if (item.equals(packageName)) {
		return true;
	    }
	}
	return false;
    }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
	ApplicationInfo info = (ApplicationInfo) mCache.getInfo(getItem(position));
	if (info == null) {
	    return null;
	}

	AppItemView view = (AppItemView) convertView;

	if (view == null) {
	    view = (AppItemView) mActivity.getLayoutInflater().inflate(R.layout.app_item, parent, false); /* XXX: do not attach to root */
	}
	CharSequence label = mCache.getLabel(info);
	if (label == null) {
	    Log.d(Config.TAG, "can't resolve label for " + info.packageName);
	    view.setText(label);
	}else{
	    view.setText(info.packageName);
	}

	return view;
    }
}