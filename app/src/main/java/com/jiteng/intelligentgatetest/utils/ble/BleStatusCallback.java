package com.jiteng.intelligentgatetest.utils.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.jiteng.intelligentgatetest.BluetoothBLeService;


/**
 * Created by zwj on 2017/5/26.
 */

public abstract class BleStatusCallback {

    /**
     * gatt 连接成功
     */
    public abstract void onGattConnected();

    /**
     * gatt 连接断开
     * @param isInitiativeDisconnect  true - 主动断开
     */
    public abstract void onGattDisconnected(boolean isInitiativeDisconnect);

    /**
     * 发现gatt service
     */
    public void onGattServicesDiscovered(BluetoothBLeService mBluetoothLeService) {}

    /**
     * 获取特征值成功
     */
    public abstract void onGetCharracteristicSuccess(BluetoothGattCharacteristic characteristic);

    /**
     * 重连
     */
    public void onReconnection() {}

    /**
     * 有数据返回时回调
     */
    public void onDataAvaliable(byte[] datas){}

    /**
     * 发送数据成功
     */
    public void onWriteDataSuccess(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){};
}

