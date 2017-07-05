/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jiteng.intelligentgatetest;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.jiteng.intelligentgatetest.utils.ble.BleStatusCallback;
import com.zwj.zwjutils.LogUtils;

import java.util.List;
import java.util.UUID;


/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothBLeService extends Service {
    private final static String TAG = BluetoothBLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private BleStatusCallback mBleStatusCallback;
    // TODO 不做重连
    public static int maxConnectionCount = 0;     // 最大连接次数
    private boolean isInitiativeDisconnect;  // true - 主动断开连接
    // 当前重连次数
    private int connectionCount;

    private BluetoothGattCharacteristic writeCharacteristic;

    private static Handler mHandler = new Handler(Looper.getMainLooper());


    public static UUID SERVICE_UUID = UUID.fromString("0000dd00-0000-1000-8000-00805f9b34fb");
    public static UUID RX_CHAR_UUID = UUID.fromString("0000dd23-0000-1000-8000-00805f9b34fb");

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // 连接成功
                mConnectionState = STATE_CONNECTED;
                connectionCount = 0;

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mBleStatusCallback != null) {
                            mBleStatusCallback.onGattConnected();
                        }
                    }
                });

                LogUtils.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                LogUtils.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                mConnectionState = STATE_DISCONNECTED;

                LogUtils.i(TAG, "isInitiativeDisconnect ----> "+isInitiativeDisconnect);
                LogUtils.i(TAG, "maxConnectionCount --> "+maxConnectionCount);
                LogUtils.i(TAG, "connectionCount ---> "+connectionCount);
                if (!isInitiativeDisconnect && maxConnectionCount > 0 && connectionCount < maxConnectionCount) {
                    connectionCount++;
                    LogUtils.i(TAG, "connectionCount ---> " + connectionCount);
                    // 重连
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mBleStatusCallback != null) {
                                mBleStatusCallback.onReconnection();
                            }
                        }
                    });
                } else {
                    connectionCount = 0;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mBleStatusCallback != null) {
                                mBleStatusCallback.onGattDisconnected(isInitiativeDisconnect);
                            }
                        }
                    });
                }

                LogUtils.i(TAG, "Disconnected from GATT server.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            LogUtils.sysout("onServicesDiscovered");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // 查询到服务
                LogUtils.sysout("开始查询服务 ---> start");

                if (RX_CHAR_UUID != null) {
                    BluetoothGattService gattService = getSupportedGattService(BluetoothBLeService.SERVICE_UUID);
                    writeCharacteristic = gattService.getCharacteristic(RX_CHAR_UUID);

                    LogUtils.sysout("查询到服务 ---> " + writeCharacteristic);
                    setCharacteristicNotification(writeCharacteristic, true);

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mBleStatusCallback != null) {
                                mBleStatusCallback.onGetCharracteristicSuccess(writeCharacteristic);
                            }
                        }
                    });
                } else {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mBleStatusCallback != null) {
                                mBleStatusCallback.onGattServicesDiscovered(BluetoothBLeService.this);
                            }
                        }
                    });
                }
            } else {
                LogUtils.e(TAG, "onServicesDiscovered received: " + status);
                // TODO 未发现服务
//                broadcastUpdate(Constant.ACTION_FINISH);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         final BluetoothGattCharacteristic characteristic,
                                         int status) {

            System.out.println("onCharacteristicRead --> " + status);
            if (status == BluetoothGatt.GATT_SUCCESS && RX_CHAR_UUID.equals(characteristic.getUuid())) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mBleStatusCallback != null) {
                            mBleStatusCallback.onDataAvaliable(characteristic.getValue());
                        }
                    }
                });
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            final BluetoothGattCharacteristic characteristic) {
            System.out.println("onCharacteristicChanged");
            if (mBleStatusCallback != null && RX_CHAR_UUID.equals(characteristic.getUuid())) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mBleStatusCallback != null) {
                            mBleStatusCallback.onDataAvaliable(characteristic.getValue());
                        }
                    }
                });
            }
        }

        @Override
        public void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic,
                                          final int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            System.out.println("onCharacteristicWrite");

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mBleStatusCallback != null) {
                        mBleStatusCallback.onWriteDataSuccess(gatt, characteristic, status);
                    }
                }
            });
        }
    };

    public class LocalBinder extends Binder {
        public BluetoothBLeService getService() {
            return BluetoothBLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mBleStatusCallback = null;
//        mHandler = null;
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        // 重置主动断开标志
        isInitiativeDisconnect = false;

        if (mBluetoothAdapter == null || address == null) {
            LogUtils.i(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            LogUtils.i(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                LogUtils.i(TAG, "conect fail");
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            LogUtils.i(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        // 主动断开连接，不需要进行重连
        isInitiativeDisconnect = true;
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        LogUtils.sysout("readCharacteristic");
        if (characteristic == null) {
            characteristic = writeCharacteristic;
        }
        boolean flag = mBluetoothGatt.readCharacteristic(characteristic);
        LogUtils.sysout("read ---> " + flag);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        if (mBluetoothGatt.setCharacteristicNotification(characteristic, true)) {
//            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CCCD);
//            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//            mBluetoothGatt.writeDescriptor(descriptor);

            LogUtils.sysout("setCharacteristicNotification success");
        } else {
            // Failed to register notification;
            LogUtils.sysout("setCharacteristicNotification fail");
        }
    }

    public boolean writeStringToGatt(BluetoothGattCharacteristic characteristic) {
        boolean flag = mBluetoothGatt.writeCharacteristic(characteristic);
        if (flag) {
            Log.i("test", "send data success");
        } else {
            Log.i("test", "send data fail");
        }
        return flag;
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    public BluetoothGattService getSupportedGattService(UUID uuid) {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getService(uuid);
    }

    public BleStatusCallback getBleStatusCallback() {
        return mBleStatusCallback;
    }

    public void setBleStatusCallback(BleStatusCallback bleStatusCallback) {
        this.mBleStatusCallback = bleStatusCallback;
    }

    public BluetoothGattCharacteristic getWriteCharacteristic() {
        return writeCharacteristic;
    }

    public void setWriteCharacteristic(BluetoothGattCharacteristic writeCharacteristic) {
        this.writeCharacteristic = writeCharacteristic;
    }

    public boolean writeStringToGatt(byte[] values) {

        if (writeCharacteristic == null) {
            return false;
        }

        int charaProp = writeCharacteristic.getProperties();
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {

            writeCharacteristic.setValue(values);
            boolean flag = writeStringToGatt(writeCharacteristic);
            LogUtils.sysout("write --> " + flag);
            if (flag) {
                LogUtils.e(TAG, "写入成功!");
            } else {
                LogUtils.e(TAG, "写入失败!");
            }
            return flag;
        }

        return false;
    }
}
