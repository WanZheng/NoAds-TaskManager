package me.cos.taskmanager;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Bundle;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.content.SharedPreferences;

public class AppList extends Activity {
    private ActivityManager mActivityManager;
    private PackageManager mPackageManager;
    private ListView mListView;
    private MyListAdapter mAdapter;
    private Map<String, ApplicationInfo> mKillList = new HashMap<String, ApplicationInfo>();
    private Set<String> mIgnoreList = new HashSet<String>();
    private Handler mHandler = new Handler();
    private SharedPreferences mPreferences;

    public static final String PREFERENCE_NAME = "me.cos.taskmanager";
    public static final String PREFERENCE_KILLLIST = "kill_list";
    public static final String PREFERENCE_IGNORELIST = "ignore_list";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_list);

	mPreferences = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);

	mAdapter = new MyListAdapter(this);

	mListView = (ListView) findViewById(R.id.list);
	mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
	// XXX: mListView.setItemsCanFocus(false);

	// TODO: require Android 3.0
	// mListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
	// 	void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
	// 	    RunningAppProcessInfo item = mAdapter.getItem(position);
	// 	    for (RunningAppProcessInfo info : mKillList) {
	// 		if (info.processName.equals(item.processName)) {
	// 		    mKillList.remove(info);
	// 		    break;
	// 		}
	// 	    }
	// 	}
	//     });
	mListView.setAdapter(mAdapter);
    }

    @Override protected void onResume() {
	super.onResume();

	mActivityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
	mPackageManager = getPackageManager();
	refresh();

	Log.d(Config.TAG, "stop service");
	stopService(new Intent(this, KillerService.class));
    }

    @Override protected void onPause() {
	super.onPause();

	Log.d(Config.TAG, "start service");
	startService(new Intent(this, KillerService.class));
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
	MenuInflater inflater = getMenuInflater();
	inflater.inflate(R.menu.app_list_menu, menu);
	return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
	switch (item.getItemId()) {
	case R.id.kill:
	    onKill();
	    return true;
	case R.id.ignore:
	    onIgnore();
	    return true;
	default:
	    return super.onOptionsItemSelected(item);
	}
    }

    private void refresh() {
	mAdapter.clear();

	List<RunningAppProcessInfo> procList = mActivityManager.getRunningAppProcesses();
        
	for (RunningAppProcessInfo info : procList) {
	    for (String pkgName: info.pkgList) {
		if (mIgnoreList.contains(pkgName)) {
		    continue;
		}

		if (! mAdapter.hasPackage(pkgName)) {
		    try {
			mAdapter.add(mPackageManager.getApplicationInfo(pkgName, PackageManager.GET_UNINSTALLED_PACKAGES));
		    } catch (PackageManager.NameNotFoundException e) {
			Log.d(Config.TAG, e + pkgName);
		    }
		}
	    }
    	}

	mAdapter.sort(new ApplicationInfo.DisplayNameComparator(mPackageManager));

	mAdapter.notifyDataSetChanged();

	mListView.clearChoices();
	int count = mAdapter.getCount();
	for (int i=0; i<count; i++) {
	    ApplicationInfo item = mAdapter.getItem(i);
	    if (mKillList.containsKey(item.packageName)) {
		Log.d(Config.TAG, "select " + item.packageName + " at " + i);
		mListView.setItemChecked(i, true);
	    }
	}
    }

    public void onKill(View view) {
	onKill();
    }

    private void updateMapAccordingToSelected(Map<String, ApplicationInfo> map) {
	SparseBooleanArray checkedPositions = mListView.getCheckedItemPositions();

	for (int i=0; i<checkedPositions.size(); i++) {
	    ApplicationInfo item = (ApplicationInfo) mAdapter.getItem(checkedPositions.keyAt(i));
	    if (checkedPositions.valueAt(i)) {
		map.put(item.packageName, item);
	    }else{
		map.remove(item.packageName);
	    }
	}
    }

    private void updateSetAccordingToSelected(Set<String> set) {
	SparseBooleanArray checkedPositions = mListView.getCheckedItemPositions();

	for (int i=0; i<checkedPositions.size(); i++) {
	    ApplicationInfo item = (ApplicationInfo) mAdapter.getItem(checkedPositions.keyAt(i));
	    if (checkedPositions.valueAt(i)) {
		set.add(item.packageName);
	    }else{
		set.remove(item.packageName);
	    }
	}
    }

    private void saveMap(String name, Map<String, ApplicationInfo> map) {
	String listString = "";
	for (String packageName : map.keySet()) {
	    listString += packageName + ";";
	}
	mPreferences.edit().putString(name, listString).commit();
    }

    private void onKill() {
	updateMapAccordingToSelected(mKillList);
	saveMap(PREFERENCE_KILLLIST, mKillList);

	doKill();
    }

    private void onIgnore() {
	boolean modified = false;
	updateSetAccordingToSelected(mIgnoreList);
	for (String name : mIgnoreList) {
	    mKillList.remove(name);
	    modified = true;
	}
	if (modified) {
	    saveMap(PREFERENCE_KILLLIST, mKillList);
	}

	refresh();
    }

    private void doKill() {
	for (String packageName : mKillList.keySet()) {
	    Log.d(Config.TAG, "manual kill " + packageName);
	    mActivityManager.killBackgroundProcesses(packageName);
	}

	mHandler.post(new Runnable() {
		@Override public void run() {
		    refresh();
		}
	    });
    }

    private class MyListAdapter extends ArrayAdapter<ApplicationInfo> {
    	public MyListAdapter(Context context) {
    	    super(context, android.R.layout.simple_list_item_multiple_choice);
    	}

    	public boolean hasPackage(String packageName) {
	    int count = getCount();
	    for (int i=0; i<count; i++) {
		ApplicationInfo item = (ApplicationInfo) getItem(i);
		if (item.packageName.equals(packageName)) {
		    return true;
		}
	    }
	    return false;
    	}

    	@Override public View getView(int position, View convertView, ViewGroup parent) {
    	    ApplicationInfo item = (ApplicationInfo) getItem(position);
    	    AppItemView view = (AppItemView) convertView;

    	    if (view == null) {
    		view = (AppItemView) getLayoutInflater().inflate(R.layout.app_item, parent, false); /* XXX: do not attach to root */
    	    }
	    CharSequence label = item.loadLabel(mPackageManager);
	    Log.d(Config.TAG, "item=" + item + ", name=" + item.packageName);
	    if (label == null) {
		view.setText(label);
	    }else{
		view.setText(item.packageName);
	    }

    	    return view;
    	}
    }
}