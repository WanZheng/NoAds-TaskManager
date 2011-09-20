package me.cos.taskmanager;

import java.util.List;
import java.util.Iterator;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.util.Log;
import android.util.SparseBooleanArray;

public class TaskKiller extends Activity {
    private ActivityManager mActivityManager;
    private ListView mListView;
    private MyListAdapter mAdapter;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.task_killer);

	mAdapter = new MyListAdapter(this);

	mListView = (ListView) findViewById(R.id.list);
	mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
	// XXX: mListView.setItemsCanFocus(false);
	mListView.setAdapter(mAdapter);
    }

    @Override protected void onResume() {
	super.onResume();

	mActivityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
	refresh();
    }

    private void refresh() {
	mAdapter.clear();

	List<RunningAppProcessInfo> procList = mActivityManager.getRunningAppProcesses();
        
    	for (Iterator<RunningAppProcessInfo> iterator = procList.iterator(); iterator.hasNext();) {
    		mAdapter.add(iterator.next());
    	}

	mAdapter.notifyDataSetChanged();
	// TODO:
	mListView.clearChoices();
    }

    public void onKill(View view) {
	SparseBooleanArray checkedPositions = mListView.getCheckedItemPositions();
	for (int i=0; i<checkedPositions.size(); i++) {
	    if (checkedPositions.valueAt(i)) {
		RunningAppProcessInfo info = (RunningAppProcessInfo) mAdapter.getItem(checkedPositions.keyAt(i));
		Log.d(Config.TAG, "kill " + info.processName);
		mActivityManager.killBackgroundProcesses(info.processName);
	    }
	}
    	
    	refresh();
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