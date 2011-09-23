package me.cos.taskmanager;

import java.util.Collection;
import java.util.List;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.text.Collator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Bundle;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.widget.ListView;
import android.widget.AbsListView;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.content.SharedPreferences;

public class AppList extends Activity {
    private ActivityManager mActivityManager;
    private PackageManager mPackageManager;
    private ListView mListView;
    private AppInfoCache mAppInfoCache;
    private AppInfoAdapter mAdapter;
    private Set<String> mKillList = new HashSet<String>();
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
	mActivityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
	mPackageManager = getPackageManager();
	mAppInfoCache = new AppInfoCache(mPackageManager);
	mAdapter = new AppInfoAdapter(this, mAppInfoCache);

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

	stopService(new Intent(this, KillerService.class));

	List<String> list = restoreStringList(mPreferences, PREFERENCE_KILLLIST);
	if (list != null) {
	    mKillList = new HashSet<String>(list);
	}
	
	list = restoreStringList(mPreferences, PREFERENCE_IGNORELIST);
	if (list != null) {
	    mIgnoreList = new HashSet<String>(list);
	}

	refresh();
    }

    @Override protected void onPause() {
	super.onPause();

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

    public static List<String> restoreStringList(SharedPreferences preference, String name) {
	// level-11
	// Set<String> killList = mPreferences.getStringSet(AppKiller.PREFERENCE_KILLLIST, null);

	String listString = preference.getString(name, "");
	if (listString.length() == 0) {
	    return null;
	}
	return Arrays.asList(listString.split(";"));
    }

    public void saveStrings(SharedPreferences preference, String name, Collection<String> collection) {
	String listString = "";
	for (String s : collection) {
	    listString += s + ";";
	}
	preference.edit().putString(name, listString).commit();
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
		    mAdapter.add(pkgName);
		}
	    }
    	}

	mAdapter.sort(Collator.getInstance());

	mAdapter.notifyDataSetChanged();

	mListView.clearChoices();
	int count = mAdapter.getCount();
	for (int i=0; i<count; i++) {
	    String item = mAdapter.getItem(i);
	    if (mKillList.contains(item)) {
		mListView.setItemChecked(i, true);
	    }
	}
    }

    private void printList(String title, Collection<String> c) {
	Log.d(Config.TAG, "== " + title + " ==");
	for (String s : c) {
	    Log.d(Config.TAG, s);
	}
	Log.d(Config.TAG, " ");
    }

    public void onKill(View view) {
	onKill();
    }

    private void updateCollectionAccordingToSelected(Collection<String> collection) {
	SparseBooleanArray checkedPositions = mListView.getCheckedItemPositions();

	for (int i=0; i<checkedPositions.size(); i++) {
	    String item = mAdapter.getItem(checkedPositions.keyAt(i));
	    if (checkedPositions.valueAt(i)) {
		collection.add(item);
	    }else{
		collection.remove(item);
	    }
	}
    }

    private void onKill() {
	updateCollectionAccordingToSelected(mKillList);
	saveStrings(mPreferences, PREFERENCE_KILLLIST, mKillList);

	doKill();
    }

    private void onIgnore() {
	boolean modified = false;
	updateCollectionAccordingToSelected(mIgnoreList);
	saveStrings(mPreferences, PREFERENCE_IGNORELIST, mIgnoreList);

	for (String name : mIgnoreList) {
	    mKillList.remove(name);
	    modified = true;
	}
	if (modified) {
	    saveStrings(mPreferences, PREFERENCE_KILLLIST, mKillList);
	}

	refresh();
    }

    private void doKill() {
	for (String packageName : mKillList) {
	    Log.d(Config.TAG, "manual kill " + packageName);
	    mActivityManager.killBackgroundProcesses(packageName);
	}

	mHandler.post(new Runnable() {
		@Override public void run() {
		    refresh();
		}
	    });
    }
}