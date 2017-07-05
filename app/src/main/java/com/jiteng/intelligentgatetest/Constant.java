package com.jiteng.intelligentgatetest;

/**
 * Created by zwj on 2017/4/3.
 */

public interface Constant {


    int REQUEST_ENABLE_BT = 1113;

    String DEFAULT_PASS = "000000";

    /**
     * 蓝牙逻辑服务连接成功广播事件
     */
    String ACTION_BLE_SERVICE_CONNECTED = "ckeyom.erling.bleservice.connected";

    String ACTION_FINISH = "action_finish";
    // 获取特征值成功
    String ACTION_GET_CHARACTERISTIC_SUCCESS = "action_get_characteristic_success";
    String ACTION_PASS_VALID = "action_pass_valid";     // 密码验证后
    String ACTION_RECONNECTION = "action_reconnection";     // 重连
    String ACTION_REFRESH = "action_refresh";     // 刷新

    String APP_FOLDER_APK = "/apk"; // apk文件在sdcard上的文件目录
    String APP_URL = "appUrl";
    String SP_KEY_IGNORE_VERSION_CODE = "ignoreVersionCode";

    int SCAN_PERIOD = 3000;      // 扫描设备时长，单位毫秒

    /**
     * 设备名称前缀
     */
    String DEVICE_NAME_PREFIX = "intelligentgate";


}
