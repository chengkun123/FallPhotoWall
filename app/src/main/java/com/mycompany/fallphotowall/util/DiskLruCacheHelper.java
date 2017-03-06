package com.mycompany.fallphotowall.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

import java.io.File;

/**
 * Created by Lenovo on 2017/3/5.
 */
public class DiskLruCacheHelper {
    public File getDiskCacheDir(Context context, String uniqueName){
        String cachePath;
        cachePath = context.getCacheDir().getPath();

        return new File(cachePath + File.separator + uniqueName);
    }

    public int getAppVersion(Context context){
        try{
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return 1;
    }
}
