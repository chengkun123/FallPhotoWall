package com.mycompany.fallphotowall.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import java.io.IOException;


/*
* 异步任务，在线程池中执行
* */
public class LoadBitmapTask implements Runnable {

    public static final String TAG ="LoadBitmapTask";
    public static final int MESSAGE_POST_RESULT = 1;
    private boolean mIsDiskLruCacheCreated = false;
    private Context mContext;
    private String uri;
    private int reqWidth;
    private int reqHeight;
    private Handler mMainHandler;
    private ImageView imageView;
    private ImageLoader.BitmapCallback callback;

    //用于Handler处理的构造函数
    public LoadBitmapTask(Context context
            ,Handler handler,ImageView imageview,String uri
            ,int reqWidth,int reqHeight) {
        this.mMainHandler =handler;
        this.uri=uri;
        this.reqHeight =reqHeight;
        this.reqWidth =reqWidth;
        this.imageView =imageview;
        mContext =context.getApplicationContext();
    }


    public LoadBitmapTask(Context context
            ,ImageLoader.BitmapCallback callback,String uri,int reqWidth,int reqHeight) {
        this.callback =callback;
        this.uri=uri;
        this.reqHeight =reqHeight;
        this.reqWidth =reqWidth;
        mContext =context.getApplicationContext();
    }


    //从本地或者网络获取bitmap
    @Override
    public void run() {

        final Bitmap bitmap = loadBitmap(uri, reqWidth, reqHeight);

        /*
        * 将结果发送给绑定在主线程的Handler
        * */
        if(mMainHandler != null){
            TaskResult result = new TaskResult(uri, imageView, bitmap);
            mMainHandler.obtainMessage(MESSAGE_POST_RESULT, result).sendToTarget();
        }
        /*
        * 将结果回调给实现了BitmapCallback的接口
        *
        * */
        if(callback != null){
            //在主线程中回调
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onResponse(bitmap, uri);
                }
            });
        }

    }

    private Bitmap loadBitmap(String uri,int reqWidth,int reqHeight){
        Bitmap bitmap = null;
        try {
            //从磁盘缓存中获取bitmap
            bitmap = ImageLoader.getImageDiskLruCache(mContext).loadBitmapFromDiskCache(uri, reqWidth, reqHeight);

            if(bitmap != null){
                Log.e("直接从硬盘","从本地缓存中获取到了bitmap");
                //添加到内存缓存中
                ImageLoader.getImageLruCache().addBitmapToLruCache(MD5Util.hashKeyFromUrl(uri), bitmap);
                return bitmap;
            }else{
                //从网络下载bitmap到本地缓存，并从本地缓存中获取bitmap
                bitmap = loadBitmapFromHttp(uri,reqWidth,reqHeight);
                if(bitmap != null){
                    Log.e("先下载到硬盘，在提取","从网络下载并保存到本地并从中读取bitmap成功");
                    //添加到内存缓存中
                    ImageLoader.getImageLruCache().addBitmapToLruCache(MD5Util.hashKeyFromUrl(uri), bitmap);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(bitmap == null && !mIsDiskLruCacheCreated ){
            //如果sd卡已满，无法使用本地缓存，则通过网络下载bitmap，一般不会调用这一步
            bitmap = NetUtil.downloadBitmapFromUrl(uri);
            Log.i(TAG, "sd卡满了，直接从网络获取");
        }
        return bitmap;
    }

    //从网络获取bitmap，并放入本地缓存中
    private Bitmap loadBitmapFromHttp(String url,int reqWidth,int reqHeight) throws IOException {
        //通过url从网络获取图片的字节流保存到磁盘缓存
        ImageLoader.getImageDiskLruCache(mContext).downloadBitmapToDiskCache(url);
        //从网络保存到本地缓存中后，直接从本地缓存中获取bitmap;
        return ImageLoader.getImageDiskLruCache(mContext).loadBitmapFromDiskCache(url, reqWidth, reqHeight);
    }
}
