package com.jiteng.intelligentgatetest;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.jiteng.intelligentgatetest.utils.ble.BleStatusCallback;
import com.zwj.customview.progress.ProgressUtil;
import com.zwj.zwjutils.LogUtils;
import com.zwj.zwjutils.ToastUtil;


public class MyApplication extends Application {
    private static final String TAG = "MyApplication";
    private static MyApplication gApp;

    /**
     * true,已经弹出过检测更新的弹窗
     */
    public static boolean isCheckUpdate = false;


    private static BluetoothBLeService mBluetoothLeService;
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothBLeService.LocalBinder) service).getService();
            if (mBluetoothLeService.initialize()) {
                LogUtils.sysout("ble service 连接");

                Intent intent = new Intent(Constant.ACTION_BLE_SERVICE_CONNECTED);
                sendBroadcast(intent);
            } else {
                LogUtils.sysout("ble service initialize fail");
                ToastUtil.toast(getApplicationContext(), "连接失败!");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            LogUtils.sysout("onServiceDisconnected");
            mBluetoothLeService = null;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        gApp = (MyApplication) getApplicationContext();

        //注册全局一场捕获
        Thread.setDefaultUncaughtExceptionHandler(AppException.getAppExceptionHandler(getApplicationContext()));

//        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    public static MyApplication getGlobalContext() {
        return gApp;
    }

    public void bindBluetoothService() {
        if (mBluetoothLeService == null) {
            Intent gattServiceIntent = new Intent(this, BluetoothBLeService.class);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        }
    }

    public void unbindBluetoothService() {
        if (mBluetoothLeService != null) {
            mBluetoothLeService.setBleStatusCallback(null);
            unbindService(mServiceConnection);
            mBluetoothLeService = null;
        }
    }

    public static BluetoothBLeService getBluetoothBLeService() {
        return mBluetoothLeService;
    }

    public static void disconnect() {
        ProgressUtil.hideProgress();
        if (mBluetoothLeService != null) {
            mBluetoothLeService.disconnect();
        }
    }

    public static boolean setBleStatusCallback(BleStatusCallback callback) {
        if (mBluetoothLeService != null) {
            mBluetoothLeService.setBleStatusCallback(callback);
            return true;
        }

        return false;
    }
}
