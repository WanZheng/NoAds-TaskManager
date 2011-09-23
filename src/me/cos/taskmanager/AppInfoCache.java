package me.cos.taskmanager;

import java.util.Map;
import java.util.HashMap;

import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.util.Log;

public class AppInfoCache {
    private Map<String, ApplicationInfo> mCache = new HashMap();
    PackageManager mPackageManager;

    public AppInfoCache(PackageManager packageManager) {
	mPackageManager = packageManager;
    }

    public ApplicationInfo getInfo(String packageName) {
	ApplicationInfo info = mCache.get(packageName);
	if (info == null) {
	    try {
		info = mPackageManager.getApplicationInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
		if (info != null) {
		    mCache.put(packageName, info);
		}
	    } catch (PackageManager.NameNotFoundException e) {
		Log.d(Config.TAG, e + packageName);
	    }
	}
	return info;
    }

    public String getLabel(ApplicationInfo info) {
	return info.loadLabel(mPackageManager).toString();
    }
}