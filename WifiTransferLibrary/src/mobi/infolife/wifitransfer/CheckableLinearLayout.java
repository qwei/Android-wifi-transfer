package mobi.infolife.wifitransfer;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.LinearLayout;

public class CheckableLinearLayout extends LinearLayout implements Checkable {
	
	private boolean mChecked;
	private List<Checkable> mCheckableViewList;

	public CheckableLinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}
	
	public CheckableLinearLayout(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		init(attrs);
	}

	public CheckableLinearLayout(Context context) {
		super(context);
		init(null);
	}

	protected void init(AttributeSet attrs) {
		mChecked = false;
		mCheckableViewList = new ArrayList<Checkable>();
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		
		final int childCount = this.getChildCount();
        for (int i = 0; i < childCount; ++i) {
            findCheckableChildren(this.getChildAt(i));
        }
	}

	@Override
	public void setChecked(boolean checked) {
		mChecked = checked;
		
		for (Checkable c : mCheckableViewList) {
            c.setChecked(mChecked);
        }
	}

	@Override
	public boolean isChecked() {
		return mChecked;
	}

	@Override
	public void toggle() {
		mChecked = !mChecked;
		for (Checkable c : mCheckableViewList) {
			c.toggle();
		}
	}
	
	/**
     * Add to our checkable list all the children of the view that implement the
     * interface Checkable
     */
    private void findCheckableChildren(View v) {
        if (v instanceof Checkable) {
        	mCheckableViewList.add((Checkable) v);
        }

        if (v instanceof ViewGroup) {
            final ViewGroup vg = (ViewGroup) v;
            final int childCount = vg.getChildCount();
            for (int i = 0; i < childCount; ++i) {
                findCheckableChildren(vg.getChildAt(i));
            }
        }
    }

}
