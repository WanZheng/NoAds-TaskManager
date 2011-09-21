package me.cos.taskmanager;

import java.lang.Runnable;
// import java.util.Set;

import android.util.Log;
import android.app.Service;
import android.os.Handler;
import android.os.IBinder;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class KillerService extends Service {
    private ActivityManager mActivityManager;
    private Handler mHandler = new Handler();
    private SharedPreferences mPreferences;

    @Override public void onCreate() {
	Log.d(Config.TAG, "onCreate()");
	mActivityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
	mPreferences = getSharedPreferences(TaskKiller.PREFERENCE_NAME, Context.MODE_PRIVATE);
	Log.d(Config.TAG, "mPreferences = " + mPreferences);
    }

    @Override public IBinder onBind(Intent intent) {
	return null;
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
	postKillingRequest();
	return START_STICKY;
    }

    private void postKillingRequest() {
	// TODO: remove duplicate messages
	mHandler.postDelayed(new Runnable() {
		@Override public void run() {
		    killTasks();
		    postKillingRequest();
		}
	    }, 5000/* TODO */);
    }

    private void killTasks() {
	// level-11
	// Set<String> killList = mPreferences.getStringSet(TaskKiller.PREFERENCE_KILLLIST, null);

	String killListString = mPreferences.getString(TaskKiller.PREFERENCE_KILLLIST, "");
	Log.d(Config.TAG, "killTasks(): string=" + killListString);
	if (killListString.length() == 0) {
	    return;
	}
	String[] killList = killListString.split(":");
	Log.d(Config.TAG, "killList = " + killList + " " + killList.length);

	for (String processName : killList) {
	    Log.d(Config.TAG, "kill " + processName);
	    mActivityManager.killBackgroundProcesses(processName);
	}
    }
}