package com.jiteng.intelligentgatetest.ui.activity.base;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.zhy.autolayout.AutoDrawerLayout;
import com.zhy.autolayout.AutoFrameLayout;
import com.zhy.autolayout.AutoLinearLayout;
import com.zhy.autolayout.AutoNavigationView;
import com.zhy.autolayout.AutoRelativeLayout;
import com.zwj.zwjutils.LogUtils;


/**
 * Created by zhy on 15/11/19.
 * 自适配屏幕的基础Activity父类
 */
public abstract class BaseAutoLayoutActivity extends BaseActivity {
	private static final String LAYOUT_LINEARLAYOUT = "LinearLayout";
	private static final String LAYOUT_FRAMELAYOUT = "FrameLayout";
	private static final String LAYOUT_RELATIVELAYOUT = "RelativeLayout";
	private static final String LAYOUT_DRAWERLAYOUT = "android.support.v4.widget.DrawerLayout";
	private static final String LAYOUT_NAVIGATIONVIEW = "android.support.design.widget.NavigationView";
	private static final String LAYOUT_COLLAPSINGTOOLBARLAYOUT = "android.support.design.widget.CollapsingToolbarLayout";
	private static final String LAYOUT_COORDINATORLAYOUT = "android.support.design.widget.CoordinatorLayout";


	@Override
	public View onCreateView(String name, Context context, AttributeSet attrs) {
		View view = null;
		if (name.equals(LAYOUT_FRAMELAYOUT)) {
			view = new AutoFrameLayout(context, attrs);
		}

		if (name.equals(LAYOUT_LINEARLAYOUT)) {
			view = new AutoLinearLayout(context, attrs);
		}

		if (name.equals(LAYOUT_RELATIVELAYOUT)) {
			view = new AutoRelativeLayout(context, attrs);
		}

		if (name.equals(LAYOUT_DRAWERLAYOUT)) {
			LogUtils.sysout("view drawerlayout =========");
			view = new AutoDrawerLayout(context, attrs);
		}

		if (name.equals(LAYOUT_NAVIGATIONVIEW)) {
			view = new AutoNavigationView(context, attrs);
		}

//		if (name.equals(LAYOUT_COLLAPSINGTOOLBARLAYOUT)) {
//			view = new AutoCollapsingToolbarLayout(context, attrs);
//		}
//
//		if (name.equals(LAYOUT_COORDINATORLAYOUT)) {
//			view = new AutoCoordinatorLayout(context, attrs);
//		}

//		LogUtils.sysout("view name --> "+name);

		if (view != null)
			return view;

		return super.onCreateView(name, context, attrs);
	}

}
