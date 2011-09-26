package me.cos.taskmanager;

import java.util.Set;
import java.util.HashSet;
import java.text.Collator;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.AdapterView;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;

public class IgnoreList extends Activity {
    private ListView mListView;
    private PackageManager mPackageManager;
    private AppInfoCache mAppInfoCache;
    private AppInfoAdapter mAdapter;
    private Set<String> mIgnoreList;
    private SharedPreferences mPreferences;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

	mPreferences = getSharedPreferences(Config.PREFERENCE_NAME, Context.MODE_PRIVATE);
	mPackageManager = getPackageManager();
	mAppInfoCache = new AppInfoCache(mPackageManager);
	mAdapter = new AppInfoAdapter(this, mAppInfoCache);

        setContentView(R.layout.ignore_list);
	mListView = (ListView) findViewById(R.id.list);
	mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
		@Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		    String item = mAdapter.getItem(position);
		    mIgnoreList.remove(item);
		    mAdapter.remove(item);
		    mAdapter.notifyDataSetChanged();

		    AppList.saveStrings(mPreferences, Config.PREFERENCE_IGNORELIST, mIgnoreList);
		}
	    });
	mListView.setAdapter(mAdapter);
    }

    @Override protected void onResume() {
	super.onResume();

	List<String> list = AppList.restoreStringList(mPreferences, Config.PREFERENCE_IGNORELIST);
	mIgnoreList = new HashSet<String>(list);

	refresh();
    }

    private void refresh() {
	mAdapter.clear();

	for (String pkgName : mIgnoreList) {
	    mAdapter.add(pkgName);
    	}

	mAdapter.sort(Collator.getInstance());
	mAdapter.notifyDataSetChanged();
    }
}