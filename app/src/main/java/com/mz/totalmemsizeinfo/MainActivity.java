package com.mz.totalmemsizeinfo;

import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.storage.StorageManager;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MAZHUANG";
    private TextView sizeTxt;
    private TextView memTxt;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sizeTxt = findViewById(R.id.sizeTxt);
        memTxt = findViewById(R.id.memTxt);

        // 1. 查询存储空间 (由于这部分涉及反射，只在启动时调用一次)
        queryStorageSize();

        // 2. 开启运行内存定时刷新
        startMemoryUpdate();
    }

    private void startMemoryUpdate() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                int totalMem = SystemMemory.getTotalMemory(MainActivity.this);
                String availMem = SystemMemory.getAvailMemory(MainActivity.this);

                String info = "【运行内存 (RAM)】\n"
                        + "总共大小: " + totalMem + " GB\n"
                        + "当前可用: " + availMem + " GB";

                memTxt.setText(info);

                // 每2秒刷新一次
                mHandler.postDelayed(this, 2000);
            }
        });
    }

    private void queryStorageSize() {
        StorageManager storageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
        try {
            Method getVolumes = StorageManager.class.getDeclaredMethod("getVolumes");
            List<Object> getVolumeInfo = (List<Object>) getVolumes.invoke(storageManager);
            long total = 0L, used = 0L;

            for (Object obj : getVolumeInfo) {
                Field getType = obj.getClass().getField("type");
                int type = getType.getInt(obj);

                if (type == 1) { // TYPE_PRIVATE
                    Method getFsUuid = obj.getClass().getDeclaredMethod("getFsUuid");
                    String fsUuid = (String) getFsUuid.invoke(obj);
                    long totalSize = getTotalSize(fsUuid);

                    Method isMountedReadable = obj.getClass().getDeclaredMethod("isMountedReadable");
                    boolean readable = (boolean) isMountedReadable.invoke(obj);
                    if (readable) {
                        Method getPath = obj.getClass().getDeclaredMethod("getPath");
                        File f = (File) getPath.invoke(obj);
                        if (totalSize <= 0) totalSize = f.getTotalSpace();

                        used += (totalSize - f.getFreeSpace());
                        total += totalSize;
                    }
                } else if (type == 0) { // TYPE_PUBLIC (SD Card)
                    Method isMountedReadable = obj.getClass().getDeclaredMethod("isMountedReadable");
                    if ((boolean) isMountedReadable.invoke(obj)) {
                        Method getPath = obj.getClass().getDeclaredMethod("getPath");
                        File f = (File) getPath.invoke(obj);
                        used += (f.getTotalSpace() - f.getFreeSpace());
                        total += f.getTotalSpace();
                    }
                }
            }

            String storageInfo = "【存储空间 (ROM)】\n"
                    + "总存储: " + getUnit(total, 1000) + "\n"
                    + "已使用: " + getUnit(used, 1000) + "\n"
                    + "剩余可用: " + getUnit(total - used, 1000);

            sizeTxt.setText(storageInfo);

        } catch (Exception e) {
            Log.e(TAG, "Storage query failed", e);
            sizeTxt.setText("存储信息获取失败 (请检查权限)");
        }
    }

    private long getTotalSize(String fsUuid) {
        try {
            StorageStatsManager stats = getSystemService(StorageStatsManager.class);
            UUID id = (fsUuid == null) ? StorageManager.UUID_DEFAULT : UUID.fromString(fsUuid);
            return stats.getTotalBytes(id);
        } catch (Exception e) {
            return -1;
        }
    }

    private String getUnit(float size, int unit) {
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int index = 0;
        while (size > unit && index < 4) {
            size = size / unit;
            index++;
        }
        return String.format(Locale.getDefault(), "%.2f %s", size, units[index]);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null); // 防止内存泄漏
    }
}