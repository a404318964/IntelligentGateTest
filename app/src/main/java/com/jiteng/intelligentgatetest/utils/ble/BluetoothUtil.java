package com.jiteng.intelligentgatetest.utils.ble;

import android.bluetooth.BluetoothDevice;

import com.jiteng.intelligentgatetest.MyApplication;
import com.zwj.zwjutils.CommonUtil;
import com.zwj.zwjutils.LogUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.jiteng.intelligentgatetest.MyApplication.getBluetoothBLeService;

/**
 * Created by zwj on 2017/4/9.
 */

public class BluetoothUtil {

    /**
     * 判断list中是否包含该设备
     *
     * @return true - 包含
     */
    public static boolean include(List<BluetoothDevice> deviceList, String deviceAddress) {
        for (BluetoothDevice device : deviceList) {
            if (deviceAddress.equals(device.getAddress())) {
                return true;
            }
        }

        return false;
    }

    public static boolean sendCommand(byte[] values) {
        if (getBluetoothBLeService() == null) {
            return false;
        }

        for (int i = 0; i < values.length; i++) {
            LogUtils.sysout("command value " + i + " ---> " + String.valueOf(values[i]));
        }

        boolean flag = MyApplication.getBluetoothBLeService().writeStringToGatt(values);
        LogUtils.sysout("write --> " + flag);
        if (flag) {
            LogUtils.sysout("写入成功!");
        }
        return flag;
    }


    /**
     * 发送测试命令
     */
    public static boolean sendTestCommand() {
        byte[] values = new byte[12];
//        values[0] = (byte) 0xFE;
//        values[1] = (byte) 0xFE;
//        values[2] = 0x01;
//        values[3] = 0x00;
//        values[4] = 0x04;
//        values[5] = 0x00;
//        values[6] = 0x00;
//        values[7] = 0x00;
//        values[8] = 0x00;
//        values[9] = 0x00;
//        values[10] = 0x00;
//        values[11] = 0x0A;

        values[0] = (byte) 0xed;
        values[1] = (byte) 0xed;
        values[2] = (byte) 0xef;

        return sendCommand(values);
    }


    /**
     * 发送验证密码的命令
     *
     * @return
     */
    public static boolean sendValidCommand(String pass) {

        byte[] values = new byte[12];
        values[0] = (byte) 0xFE;
        values[1] = (byte) 0xFE;
        values[2] = 0x04;
        values[3] = 0x00;

        byte[] result = generateDevicePass(pass);
        if (result.length != 7) {
            LogUtils.sysout("length != 7");
            return false;
        }

        for (int i = 0; i < result.length; i++) {
            values[i + 4] = result[i];
        }

        values[11] = 0x0A;

        return sendCommand(values);
    }

    /**
     * 生成加密后的密码验证
     *
     * @return
     */
    public static byte[] generateDevicePass(String pass) {
// 0 	1   2    3    4    5    6    7    8    9    A    B    C     D    E  	F
        // {0x56,0x94,0x68,0x76,0x77,0x36,0x19,0x39,0x59,0x88,0xA5，0xC9,0x80,0x95，0x08，0x33}
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("0", 86);
        map.put("1", 148);
        map.put("2", 104);
        map.put("3", 118);
        map.put("4", 119);
        map.put("5", 54);
        map.put("6", 25);
        map.put("7", 57);
        map.put("8", 89);
        map.put("9", 136);
        map.put("A", 165);
        map.put("B", 201);
        map.put("C", 128);
        map.put("D", 149);
        map.put("E", 8);
        map.put("F", 51);

        StringBuilder sb = new StringBuilder();
        int[] results = new int[6];

        int[] passValues10 = new int[6];
        for (int i = 0; i < passValues10.length; i++) {
            passValues10[i] = Integer.parseInt(pass.substring(i, i + 1));
            System.out.println("passValues10 " + i + " : " + passValues10[i]);
        }

        int[] randomNumber16 = new int[6];
        int[] randomNumber10 = new int[6];
        for (int i = 0; i < 6; i++) {
            randomNumber16[i] = new Random().nextInt(100);

            System.out.println("randomNumber16 " + i + " : " + randomNumber16[i]);
            String key = null;
            if (randomNumber16[i] < 10) {
                key = String.valueOf(randomNumber16[i]);
            } else {
                key = String.valueOf(randomNumber16[i] % 10);
            }
            System.out.println("key ---> " + key);
            results[i] = map.get(key.toUpperCase());

            // 转成10进制
            randomNumber10[i] = Integer.parseInt(String.valueOf(randomNumber16[i]), 16);
            System.out.println("randomNumber10 " + i + " : " + randomNumber10[i]);

            int sum = randomNumber10[i] + passValues10[i];
            String sumStr = Integer.toHexString(sum);
            if (sumStr.length() == 1) {
                sb.append("0");
            }
            sb.append(sumStr);
        }

        int result = results[0];
        System.out.println("result ---> " + result);
        for (int i = 1; i < 6; i++) {
            System.out.println("result ---> " + results[i]);
            result = result ^ results[i];
        }

        String temp = Integer.toHexString(result);
        if (temp.length() == 1) {
            sb.append("0");
        }
        sb.append(temp);

        System.out.println("sb ---> " + sb.toString().toUpperCase());

        return CommonUtil.hex2byte(sb.toString().toUpperCase());
    }

}
