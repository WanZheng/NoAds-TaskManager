package me.cos.taskmanager;

import android.util.Log;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Checkable;

public class AppItemView extends LinearLayout implements Checkable {
    private boolean mChecked = false;

    public AppItemView(Context context, AttributeSet attrs) {
	super(context, attrs);
    }

    public void setText(String text) {
	((TextView) findViewById(R.id.text)).setText(text);
    }

    public void setText(CharSequence text) {
	((TextView) findViewById(R.id.text)).setText(text);
    }

    @Override public void setChecked(boolean checked) {
	mChecked = checked;
	((TextView) findViewById(R.id.check)).setText(checked ? "* " : "  ");
    }

    @Override public boolean isChecked() {
	return mChecked;
    }

    @Override public void toggle() {
	setChecked(! mChecked);
    }
}