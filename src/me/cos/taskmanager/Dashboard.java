package me.cos.taskmanager;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;

public class Dashboard extends Activity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard);

	Intent intent = new Intent(this, AppList.class);
	startActivity(intent);
    }
}