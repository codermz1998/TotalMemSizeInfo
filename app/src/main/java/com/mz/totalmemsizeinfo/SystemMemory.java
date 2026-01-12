package com.mz.totalmemsizeinfo;

import android.app.ActivityManager;
import android.content.Context;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;

public class SystemMemory {

    /**
     * 获取当前可用运行内存 (单位: GB)
     */
    public static String getAvailMemory(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);

        // 1024进制转换
        double availGb = mi.availMem / (1024.0 * 1024.0 * 1024.0);
        DecimalFormat df = new DecimalFormat("0.00");
        return df.format(availGb);
    }

    /**
     * 获取总运行内存 (单位: GB)
     */
    public static int getTotalMemory(Context context) {
        String str1 = "/proc/meminfo";
        String str2;
        String[] arrayOfString;
        long initial_memory = 0;
        try {
            FileReader localFileReader = new FileReader(str1);
            BufferedReader localBufferedReader = new BufferedReader(localFileReader, 8192);
            str2 = localBufferedReader.readLine();
            arrayOfString = str2.split("\\s+");

            // 获得KB
            long i = Long.parseLong(arrayOfString[1]);
            // 转换为Byte再转为GB
            double initial_memory_double = (i * 1024.0) / (1024.0 * 1024.0 * 1024.0);

            localBufferedReader.close();
            // 四舍五入取整
            return (int) Math.ceil(initial_memory_double);
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }
}