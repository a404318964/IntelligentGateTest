package com.jiteng.intelligentgatetest.ui.activity.base;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.jiteng.intelligentgatetest.MyApplication;
import com.zwj.zwjutils.AppManager;
import com.zwj.zwjutils.LogUtils;


/**
 * 快速开发的activity基础框架类，提供了getView来简化findViewById,并且提供了构建框架.
 *
 * @author zwj
 * @date 2015/5/24
 */
public abstract class BaseActivity extends AppCompatActivity {
    protected String TAG = getClass().getSimpleName();
    protected Context mContext;
    protected Activity mActivity;
    protected MyApplication mApp;
//    protected ViewDataBinding mBinding;
//    protected Callback.Cancelable cancelable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LogUtils.d(TAG, TAG + " ---> onCreate");
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mContext = this;
        mActivity = this;
        mApp = (MyApplication) getApplication();
        AppManager.getAppManager().addActivity(this);
        beforeSetContentView();
        if (getContentViewId() != 0) {
            setContentView();
            if(isUseButterKnife()) {
//                ButterKnife.bind(this);
            }
        }
//        Glide.get(mContext).clearMemory();
        findViews();
        initData(savedInstanceState);
        setListener();
    }

    /**
     * 是否使用databinding
     * @return 默认返回false
     */
    protected boolean isUseDataBinding() {
        return false;
    }

    /**
     * 是否使用ButterKnife
     * @return 默认返回false
     */
    protected boolean isUseButterKnife() {
        return false;
    }

    protected void setContentView() {
//        if(isUseDataBinding()) {
//            mBinding = DataBindingUtil.setContentView(this, getContentViewId());
//        }else {
            setContentView(getContentViewId());
//        }
    }

    /**
     * 在setContentView之前触发的方法
     */
    protected void beforeSetContentView() {

    }

    /**
     * 如果没有布局，那么就返回0
     *
     * @return activity的布局文件
     */
    protected abstract int getContentViewId();

    /**
     * 找到所有的views
     */
    protected abstract void findViews();

    /**
     * 在这里初始化设置view的各种资源，比如适配器或各种变量
     */
    protected abstract void initData(Bundle savedInstanceState);

    /**
     * 设置所有的监听事件
     */
    protected abstract void setListener();

    /**
     * 通过泛型来简化findViewById
     */
    protected final <E extends View> E getView(int id) {
        try {
            return (E) findViewById(id);
        } catch (ClassCastException ex) {
            LogUtils.e(TAG, "Could not cast View to concrete class.", ex);
            throw ex;
        }
    }

    @Override
    protected void onDestroy() {
        LogUtils.d(TAG, TAG + " ---> onDestroy");
//        if(cancelable != null && !cancelable.isCancelled()) {
//            cancelable.cancel();
//        }
        super.onDestroy();

        AppManager.getAppManager().finishActivity(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LogUtils.d(TAG, TAG + " ---> onResume");
    }

    @Override
    protected void onStart() {
        super.onStart();
        LogUtils.d(TAG, TAG + " ---> onStart");
    }

    @Override
    protected void onPause() {
        super.onPause();
        LogUtils.d(TAG, TAG + " ---> onPause");
    }
}
