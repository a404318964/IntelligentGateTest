package com.jiteng.intelligentgatetest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.jiteng.intelligentgatetest.ui.activity.base.BaseAutoLayoutCommonActivity;
import com.jiteng.intelligentgatetest.ui.adapter.DeviceAdapter;
import com.jiteng.intelligentgatetest.utils.ble.BleStatusCallback;
import com.jiteng.intelligentgatetest.utils.ble.BluetoothUtil;
import com.zhy.adapter.recyclerview.MultiItemTypeAdapter;
import com.zwj.customview.progress.ProgressBean;
import com.zwj.customview.progress.ProgressUtil;
import com.zwj.zwjutils.LogUtils;
import com.zwj.zwjutils.ToastUtil;

import java.util.ArrayList;
import java.util.List;

import static com.jiteng.intelligentgatetest.Constant.REQUEST_ENABLE_BT;
import static com.jiteng.intelligentgatetest.MyApplication.getGlobalContext;

public class MainActivity extends BaseAutoLayoutCommonActivity {
    private RecyclerView rvDevcie;
    private DeviceAdapter deviceAdapter;
    private List<BluetoothDevice> deviceList = new ArrayList<>();

    private ImageView ivRefresh;

    private BluetoothAdapter mBluetoothAdapter;
    private static Handler mHandler = new Handler();
    private boolean mScanning;                  // true - 正在扫描设备；

    private static final int delayTime = 5000;

    private int position;

    @Override
    protected int getContentViewId() {
        return R.layout.activity_main;
    }

    @Override
    protected void findViews() {
        rvDevcie = getView(R.id.rv_device);
        ivRefresh = getView(R.id.iv_refresh);
    }

    @Override
    protected void initData(Bundle savedInstanceState) {

        // 检测设备是否支持BLE
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            ToastUtil.toast(mContext, "不支持BLE");
            finish();
        }

        BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            ToastUtil.toast(mContext, "不支持蓝牙");
            finish();
            return;
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constant.ACTION_BLE_SERVICE_CONNECTED);
        registerReceiver(mReceiver, intentFilter);

        // 判断蓝牙是否已经打开，若没有则弹出选择框供用户打开
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            scanDevice(true, true);
        }

        mApp.bindBluetoothService();
    }

    @Override
    protected void setListener() {
        ivRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanDevice(false, false);
                new ProgressBean().setLoadingTip("正在重新扫描设备...").setColor(R.color.color_orange)
                        .startProgress(mContext);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scanDevice(true, false);
                    }
                }, 500);
            }
        });
    }

    /**
     * 开始（停止）扫描设备
     *
     * @param enable true - 开始扫描； false - 停止扫描
     */
    private void scanDevice(boolean enable, boolean showLoading) {
        if (enable) {
            deviceList.clear();
            // Stops scanning after a pre-defined scan period.
            if(showLoading) {
                new ProgressBean().setLoadingTip("正在扫描设备...").setColor(R.color.color_orange)
                        .startProgress(mContext);
            }
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    ProgressUtil.hideProgress();
                }
            }, Constant.SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            if (mScanning) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                mScanning = false;
                ProgressUtil.hideProgress();
            }
        }
    }

    // ble设备扫描回调.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    // 已经绑定过
                    if (device.getName() != null && device.getName().toLowerCase().startsWith(Constant.DEVICE_NAME_PREFIX)
                            && !BluetoothUtil.include(deviceList, device.getAddress())) {
                        deviceList.add(device);
                        refreshDevcieList();
                    }
                }
            };

    // 刷新设备列表
    private void refreshDevcieList() {
        if (deviceAdapter == null) {
            rvDevcie.setLayoutManager(new LinearLayoutManager(mContext));
            rvDevcie.setHasFixedSize(true);

            deviceAdapter = new DeviceAdapter(mContext, deviceList);
            rvDevcie.setAdapter(deviceAdapter);
            rvDevcie.setItemAnimator(new DefaultItemAnimator());

            deviceAdapter.setOnItemClickListener(new MultiItemTypeAdapter.OnItemClickListener<BluetoothDevice>() {

                @Override
                public void onItemClick(View view, RecyclerView.ViewHolder viewHolder, BluetoothDevice device, int i) {
                    LogUtils.i(TAG, "click connect");
                    position = i;
                    // 连接
                    connect(device.getAddress(), true);
                }

                @Override
                public boolean onItemLongClick(View view, RecyclerView.ViewHolder viewHolder, BluetoothDevice device, int i) {
                    return false;
                }
            });
        } else {
            deviceAdapter.setDatasWithRefresh(deviceList);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // 用户选择不开启蓝牙，则退出应用
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                finish();
                return;
            }

            scanDevice(true, true);
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Constant.ACTION_BLE_SERVICE_CONNECTED.equals(action)) {
                MyApplication.getBluetoothBLeService().setBleStatusCallback(mBleStatusCallback);
            } else if (Constant.ACTION_FINISH.equals(action)) {
                finish();
            }
        }
    };

    private boolean isAuth;
    private BleStatusCallback mBleStatusCallback = new BleStatusCallback() {

        @Override
        public void onGattConnected() {
            LogUtils.i(TAG, "onGattConnected");
        }

        @Override
        public void onGattDisconnected(boolean isInitiativeDisconnect) {
            LogUtils.i(TAG, "onGattDisconnected");

            isAuth = false;
            if (!isInitiativeDisconnect) {
                ToastUtil.toast(getGlobalContext(), "连接断开");
            }

//            finish();
        }

        @Override
        public void onGetCharracteristicSuccess(BluetoothGattCharacteristic characteristic) {
            // 发送密码验证命令
            if (BluetoothUtil.sendValidCommand(Constant.DEFAULT_PASS)) {
            } else {
                ToastUtil.toast(getGlobalContext(), "连接断开，请重新连接!");
//                finish();
            }
        }

        @Override
        public void onReconnection() {
            // 重连
            connect(deviceList.get(position).getAddress(), false);
        }

        @Override
        public void onDataAvaliable(byte[] datas) {
//            super.onDataAvaliable(datas);
            ProgressUtil.hideProgress();
            mHandler.removeCallbacks(mRunnable);
            isGetPassValidResult = true;
            String passValidFlag = String.valueOf(datas[0]);
            switch (passValidFlag) {
                case "1":
                    LogUtils.i(TAG, "密码验证成功");
                    BluetoothUtil.sendTestCommand();
                    ToastUtil.toast(MyApplication.getGlobalContext(), "发送测试命令成功");

                    mHandler.postDelayed(disconectRun, 500);
                    break;

                case "2":
                    ToastUtil.toast(getGlobalContext(), "设备密码验证失败!", Toast.LENGTH_LONG);
                    // 弹出设置密码框
//                        showSetPasswordDialog(false);
                    break;

                case "3":
                    // 初始化密码
                    ToastUtil.toast(getGlobalContext(), "密码已被重置，请重新输入初始化密码", Toast.LENGTH_LONG);

                    // 弹出设置密码框
//                    showSetPasswordDialog(true);
                    break;
            }
        }
    };

    private void connect(String address, boolean showLoading) {

        if (showLoading) {
            new ProgressBean().setLoadingTip("正在与设备取得连接...")
                    .setColor(R.color.color_orange)
                    .setCancelable(false)
                    .startProgress(mContext);
        }

        LogUtils.i(TAG, "getBluetoothBLeService --> " + (MyApplication.getBluetoothBLeService() != null));
        if (MyApplication.getBluetoothBLeService() != null && MyApplication.getBluetoothBLeService().connect(address)) {
            mHandler.postDelayed(mRunnable, delayTime);
        } else {
            ProgressUtil.hideProgress();
        }
    }

    private boolean isGetPassValidResult; // true - 成功获取到密码验证返回值
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isGetPassValidResult) {
                ToastUtil.toast(getGlobalContext(), "连接失败，请重新连接!");
//                finish();
            }
        }
    };

    private Runnable disconectRun = new Runnable() {
        @Override
        public void run() {
            // 断开
            MyApplication.disconnect();
            ToastUtil.toast(getGlobalContext(), "断开连接");
            LogUtils.i(TAG, "断开连接");
        }
    };

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacks(mRunnable);
        mHandler.removeCallbacks(disconectRun);
        MyApplication.disconnect();
        mApp.unbindBluetoothService();
        unregisterReceiver(mReceiver);

        super.onDestroy();
    }
}
