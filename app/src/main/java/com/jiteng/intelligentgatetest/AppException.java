package com.jiteng.intelligentgatetest;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.jiteng.intelligentgatetest.utils.Utils;
import com.zwj.zwjutils.CommonUtil;
import com.zwj.zwjutils.DateUtil;

import org.json.JSONException;
import org.json.JSONObject;
import org.xutils.ex.HttpException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;



/**
 * 应用程序异常类：用于捕获异常和提示错误信息
 */

public class AppException extends Exception implements UncaughtExceptionHandler {
    /**
     * 定义异常类型
     */
    public final static byte TYPE_NETWORK = 0x01;
    public final static byte TYPE_SOCKET = 0x02;
    public final static byte TYPE_HTTP_CODE = 0x03;
    public final static byte TYPE_HTTP_ERROR = 0x04;
    public final static byte TYPE_IO = 0x06;
    public final static byte TYPE_RUN = 0x07;
    public final static byte TYPE_JSON = 0x08;
    private byte type;
    private int code;
    private Context mContext;

    /**
     * 系统默认的UncaughtException处理类
     */
    private UncaughtExceptionHandler mDefaultHandler;

    private AppException(Context context) {
        this.mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        this.mContext = context;
    }

    private AppException(byte type, int code, Exception excp, Context context) {
        super(excp);
        this.type = type;
        this.code = code;
        this.mContext = context;
        if (BuildConfig.DEBUG) {
            this.saveErrorLog(excp);
        }
    }

    public int getCode() {
        return this.code;
    }

    public int getType() {
        return this.type;
    }

    /*
     * 保存异常日志
     *
     * @param excp
     */
    public void saveErrorLog(Exception excp) {
        String errorlog = "imcc_errorlog.txt";
        String savePath = "";
        String logFilePath = "";
        FileWriter fw = null;
        PrintWriter pw = null;
        try {
            // 判断是否挂载了SD卡
            String storageState = Environment.getExternalStorageState();
            if (storageState.equals(Environment.MEDIA_MOUNTED)) {
                savePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/intech_imcc/Log/";
                File file = new File(savePath);
                if (!file.exists()) {
                    file.mkdirs();
                }
                logFilePath = savePath + errorlog;
            }
            // 没有挂载SD卡，无法写文件
            if (logFilePath == "") {
                return;
            }
            File logFile = new File(logFilePath);
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            fw = new FileWriter(logFile, true);
            pw = new PrintWriter(fw);
            pw.println("--------------------" + (new Date().toLocaleString()) + "---------------------");
            excp.printStackTrace(pw);
            pw.close();
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (pw != null) {
                pw.close();
            }
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {

                }
            }
        }

    }

    public static AppException http(int code) {
        return new AppException(TYPE_HTTP_CODE, code, null, null);
    }

    public static AppException http(Exception e) {
        return new AppException(TYPE_HTTP_ERROR, 0, e, null);
    }

    public static AppException socket(Exception e) {
        return new AppException(TYPE_SOCKET, 0, e, null);
    }

    public static AppException io(Exception e) {
        if (e instanceof UnknownHostException || e instanceof ConnectException) {
            return new AppException(TYPE_NETWORK, 0, e, null);
        } else if (e instanceof IOException) {
            return new AppException(TYPE_IO, 0, e, null);
        }
        return run(e);
    }

    public static AppException json(Exception e) {
        return new AppException(TYPE_JSON, 0, e, null);
    }

    public static AppException network(Exception e) {
        if (e instanceof UnknownHostException || e instanceof ConnectException) {
            return new AppException(TYPE_NETWORK, 0, e, null);
        } else if (e instanceof HttpException) {
            return http(e);
        } else if (e instanceof SocketException) {
            return socket(e);
        }
        return http(e);
    }

    public static AppException run(Exception e) {
        return new AppException(TYPE_RUN, 0, e, null);
    }

    /**
     * 获取APP异常崩溃处理对象
     */
    public static AppException getAppExceptionHandler(Context context) {
        return new AppException(context);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (!handleException(ex) && mDefaultHandler != null) {
            // 如果用户没有处理则让系统默认的异常处理器来处理
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.e("AppException", "error : ", e);
            }
            //退出程序
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
//            restart();
        }
    }

    /**
     * 自定义异常处理:收集错误信息&发送错误报告
     *
     * @param ex
     * @return true:处理了该异常信息;否则返回false
     */
    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }
        if (mContext == null) {
            return false;
        }
        Log.e("AppException", "UnCaughtException", ex);
        final String crashReport = getCrashReport(mContext, ex);
        // 显示异常信息&发送报告
        new Thread() {
            public void run() {
                Looper.prepare();
                Toast.makeText(mContext, "很抱歉，应用程序出现错误", Toast.LENGTH_SHORT).show();
                Utils.saveInfo(mContext, crashReport, "crashReport.txt");//错误保存到本地文件
                // 直接回到首页
//                MyApplication.getGlobalContext().init();
//                AppManager.getAppManager().finishAllActivity();
//                Intent intent = new Intent(mContext, MainActivity.class);//跳转到App最开始的页面
//                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                mContext.startActivity(intent);
                Looper.loop();
            }
        }.start();
        return true;
    }

    /**
     * 重启应用
     */
//    private void restart() {
//        Intent intent = new Intent(mContext, WelcomeActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        PendingIntent restartIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//        AlarmManager mgr = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
//        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 2000,
//                restartIntent); // 2秒钟后重启应用
//        AppManager.getAppManager().AppExit(mContext);
//    }

    /**
     * 获取APP崩溃异常报告
     *
     * @param ex
     * @return
     */
    private String getCrashReport(Context context, Throwable ex) {
        JSONObject json = new JSONObject();
        try {
            PackageInfo pinfo = CommonUtil.getPackageInfo(context);
            json.put("version", pinfo.versionName + "(" + pinfo.versionCode + ")");
            StringBuffer exceptionStr = new StringBuffer();
            exceptionStr.append("Time: " + DateUtil.getCurDateStr() + ";  ");
            exceptionStr.append("Android: " + android.os.Build.VERSION.RELEASE + "(" + android.os.Build.MODEL + ");  ");
            String str = Log.getStackTraceString(ex);
            int index = str.indexOf("Caused by:");
            if (index == -1) {
                exceptionStr.append("Exception: " + str);
            } else {
                exceptionStr.append("Exception: " + str.substring(index));
            }
            json.put("log", exceptionStr.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }

//    private void sendCrashReport(final Context context, final String report) {
//        if (Utils.isNetworkConnected(AppContext.getGlobalAppContext())) {
//            WebParameters parameters = new WebParameters();
//            parameters.addParam("url", Constant.getBaseUrl() + Constant.POST_UPDATE_RACK_INFO);
//            parameters.addParam("requestype", WebUtils.METHOD_POST);
//            //param
//            parameters.addParam("Sn", DeviceUtil.getSerialNumber());
//            parameters.addParam("Status", "3");
//            parameters.addParam("Message", report);
//            Log.e("AppException", "发起请求");
//            new WebRequest().dorequest(parameters, new WebResponseListener() {
//                @Override
//                public void onComplete(String json, String string) {
//                    Log.e("AppException", "错误上传服务器成功");
//                    FileUtils.writeContent2File(null, "LOG", "错误上传服务器成功" + report, true);
//                }
//
//                @Override
//                public void onException(Exception exception, String string) {
//                    Log.e("AppException", "错误上传服务器异常");
//                    FileUtils.writeContent2File(null, "LOG", "错误上传服务器异常" + report, true);
//                }
//            }, null, null);
//        }
//    }
}