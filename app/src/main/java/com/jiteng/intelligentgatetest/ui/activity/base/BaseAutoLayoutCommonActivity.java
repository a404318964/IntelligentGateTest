package com.jiteng.intelligentgatetest.ui.activity.base;

/**
 * Created by zhy on 15/11/19.
 * 在自适配的屏幕大小的基础上，不包含fragment，单纯的activity使用的基类
 * 说明: 特定提出此类是由于使用友盟统计，activity和fragment需要调用不同的方法继续统计;
 * 分离开来后会更加的灵活，其他对应包含fragment的activity和单纯activity需要不同处理的时候就可以
 * 继承不同的基类
 */
public abstract class BaseAutoLayoutCommonActivity extends BaseAutoLayoutActivity {

	@Override
	protected void onResume() {
		super.onResume();
//		MobclickAgent.onPageStart(TAG);
//		MobclickAgent.onResume(this); // 友盟统计
	}

	@Override
	protected void onPause() {
		super.onPause();
//		MobclickAgent.onPageEnd(TAG);
//		MobclickAgent.onPause(this); // 友盟统计
	}
}
