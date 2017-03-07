package com.mycompany.fallphotowall.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.LruCache;

import java.io.FileDescriptor;

/**
 * Created by Lenovo on 2017/2/18.
 */
public class ImageLoader {
    private static LruCache<String, Bitmap> mMemoryCache;

    //单例模式
    private static ImageLoader mImageloader;

    private ImageLoader(){
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        //用程序最大可用内存1/8来缓存图片
        int cacheSize = maxMemory / 8;
        Log.e("可缓存的大小是","" + cacheSize / 1024 / 1024);
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
    }

    public static ImageLoader getInstance(){
        if(mImageloader == null){
            mImageloader = new ImageLoader();
        }
        return mImageloader;
    }

    public Bitmap getBitmapFromMemoryCache(String key){
        return mMemoryCache.get(key);
    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap){
        if(getBitmapFromMemoryCache(key) == null){
            mMemoryCache.put(key, bitmap);
        }
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth){
        final int width = options.outWidth;
        int inSampleSize = 1;
        if(width > reqWidth){
            //四舍五入
            final int widthRatio = Math.round((float)width / (float)reqWidth);
            inSampleSize = widthRatio;
        }
        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromResource(String path, int reqWidth){
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        //可以在获得图片的时候选择缩放比例
        options.inSampleSize = calculateInSampleSize(options, reqWidth);
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(path, options);
    }

    public static Bitmap decodeSampledBitmapFromDescriptor(FileDescriptor fd, int reqWidth){
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fd, null, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFileDescriptor(fd, null, options);

    }

}
