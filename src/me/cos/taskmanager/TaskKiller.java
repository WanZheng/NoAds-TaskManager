package me.cos.taskmanager;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.content.SharedPreferences;

public class TaskKiller extends Activity {
    private ActivityManager mActivityManager;
    private ListView mListView;
    private MyListAdapter mAdapter;
    private Map<String, RunningAppProcessInfo> mKillList = new HashMap<String, RunningAppProcessInfo>();
    private Handler mHandler = new Handler();
    private SharedPreferences mPreferences;

    public static final String PREFERENCE_NAME = "me.cos.taskmanager";
    public static final String PREFERENCE_KILLLIST = "kill_list";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.task_killer);

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

	startService(new Intent(this, KillerService.class));
    }

    @Override protected void onResume() {
	super.onResume();

	mActivityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
	refresh();
    }

    private void refresh() {
	mAdapter.clear();

	List<RunningAppProcessInfo> procList = mActivityManager.getRunningAppProcesses();
        
	for (RunningAppProcessInfo info : procList) {
    		mAdapter.add(info);
    	}

	mAdapter.notifyDataSetChanged();

	mListView.clearChoices();
	int count = mAdapter.getCount();
	for (int i=0; i<count; i++) {
	    RunningAppProcessInfo item = mAdapter.getItem(i);
	    if (mKillList.containsKey(item.processName)) {
		Log.d(Config.TAG, "select " + item.processName + " at " + i);
		mListView.setItemChecked(i, true);
	    }
	}
    }

    public void onKill(View view) {
	SparseBooleanArray checkedPositions = mListView.getCheckedItemPositions();
	for (int i=0; i<checkedPositions.size(); i++) {
	    RunningAppProcessInfo item = (RunningAppProcessInfo) mAdapter.getItem(checkedPositions.keyAt(i));
	    if (checkedPositions.valueAt(i)) {
		mKillList.put(item.processName, item);
	    }else{
		Log.d(Config.TAG, "Need to remove " + item.processName);
		mKillList.remove(item.processName);
	    }
	}

	doKill();
    }

    private void doKill() {
	String killListString = "";
	for (String processName : mKillList.keySet()) {
	    Log.d(Config.TAG, "kill " + processName);
	    mActivityManager.killBackgroundProcesses(processName);
	    killListString += processName + ":";
	}
	mPreferences.edit().putString(PREFERENCE_KILLLIST, killListString).commit();

	mHandler.post(new Runnable() {
		@Override public void run() {
		    refresh();
		}
	    });
    }

    private class MyListAdapter extends ArrayAdapter<RunningAppProcessInfo> {
	public MyListAdapter(Context context) {
	    super(context, android.R.layout.simple_list_item_multiple_choice);
	}

	@Override public View getView(int position, View convertView, ViewGroup parent) {
	    RunningAppProcessInfo info = (RunningAppProcessInfo) getItem(position);
	    MyTaskItemView view = (MyTaskItemView) convertView;

	    if (view == null) {
		view = (MyTaskItemView) getLayoutInflater().inflate(R.layout.task_item, parent, false); /* XXX: do not attach to root */
	    }
	    view.setText(info.processName);

	    return view;
	}
    }
}