package me.cos.taskmanager;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MyTaskItemView extends LinearLayout {
    public MyTaskItemView(Context context, AttributeSet attrs) {
	super(context, attrs);
    }

    public void setText(String text) {
	((TextView) findViewById(R.id.text)).setText(text);
    }
}
