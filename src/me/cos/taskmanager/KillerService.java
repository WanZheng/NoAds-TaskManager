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
    private boolean mStopped = false;

    @Override public void onCreate() {
	mActivityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
	mPreferences = getSharedPreferences(AppKiller.PREFERENCE_NAME, Context.MODE_PRIVATE);
    }

    @Override public IBinder onBind(Intent intent) {
	return null;
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
	postKillingRequest();
	mStopped = false;
	return START_STICKY;
    }

    @Override public void onDestroy() {
	super.onDestroy();
	mStopped = true;
	Log.d(Config.TAG, "service onDestroy()");
    }

    private void postKillingRequest() {
	// TODO: remove duplicate messages
	mHandler.postDelayed(new Runnable() {
		@Override public void run() {
		    if (! mStopped) {
			killApplications();
			postKillingRequest();
		    }else{
			Log.d(Config.TAG, "ignore auto kill request");
		    }
		}
	    }, 5000/* TODO */);
    }

    private void killApplications() {
	// level-11
	// Set<String> killList = mPreferences.getStringSet(AppKiller.PREFERENCE_KILLLIST, null);

	String killListString = mPreferences.getString(AppKiller.PREFERENCE_KILLLIST, "");
	if (killListString.length() == 0) {
	    return;
	}
	String[] killList = killListString.split(";");

	for (String packageName : killList) {
	    Log.d(Config.TAG, "auto kill " + packageName);
	    mActivityManager.killBackgroundProcesses(packageName);
	}
    }
}