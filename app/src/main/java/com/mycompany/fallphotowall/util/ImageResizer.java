package com.mycompany.fallphotowall.util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.FileDescriptor;


public class ImageResizer {
    private final static String TAG = "ImageResizer";

    public ImageResizer(){

    }

    public static Bitmap decodeSampledBitmapFromResource(Resources resources, int resId, int reqWidth, int reqHeight){
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(resources, resId, options);

        options.inSampleSize = calculateInSampledSize(options, reqWidth, reqHeight);
        Log.e(TAG, "缩放倍数是：" + options.inSampleSize);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(resources, resId, options);

    }

    public static Bitmap decodeSampledBitmapFromFileDescriptor(FileDescriptor fd, int reqWidth, int reqheight){
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fd, null, options);

        options.inSampleSize = calculateInSampledSize(options, reqWidth, reqheight);
        Log.e(TAG, "缩放倍数是："+options.inSampleSize);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFileDescriptor(fd, null, options);
    }


    public static int calculateInSampledSize(BitmapFactory.Options options, int reqWidth, int reqHeight){
        if(reqWidth == 0 || reqHeight == 0){
            return 1;
        }
        final int width = options.outWidth;
        final int height = options.outHeight;
        Log.e(TAG, "原始大小是："+width+"*"+height);
        int inSampledSize = 1;
        if(width > reqHeight || height > reqHeight){
            final int widthRatio = Math.round((float) width / (float)reqWidth);
            final int heightRatio = Math.round((float) height / (float)reqHeight);

            inSampledSize = widthRatio > heightRatio ? heightRatio : widthRatio;
        }

        return inSampledSize;
    }
}
