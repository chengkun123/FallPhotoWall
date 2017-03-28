package com.mycompany.fallphotowall.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import com.mycompany.fallphotowall.R;

import java.io.IOException;
import java.util.concurrent.ThreadPoolExecutor;


public class ImageLoader {
    private static volatile ImageLoader instance = null;
    private Context mContext;
    private static ImageLruCache imageLrucache;
    private static ImageDiskLruCache imageDiskLrucache;
    //创建一个静态的线程池对象
    private static ThreadPoolExecutor THREAD_POOL_EXECUTOR = null;
    //创建一个更新ImageView的UI的Handler
    private static BindHandler mMainHandler;
    public static ImageLoader getInstance(Context context){
        if(instance == null){
            synchronized (ImageLoader.class){
                if(instance == null){
                    instance = new ImageLoader(context);
                }
            }
        }
        return instance;
    }
    //私有的构造方法，防止在外部实例化该ImageLoader
    private ImageLoader(Context context){
        mContext = context.getApplicationContext();
        THREAD_POOL_EXECUTOR = ImageThreadPoolExecutor.getInstance();
        imageLrucache = new ImageLruCache();
        imageDiskLrucache = new ImageDiskLruCache(context);
        mMainHandler = new BindHandler();
    }


    /*
    * 同步接口
    *
    * */
    public Bitmap getBitmap(String url, int reqWidth, int reqHeight){
        Bitmap bitmap = imageLrucache.loadBitmapFromLruCacheByUrl(url);
        if(bitmap != null){
            return bitmap;
        }

        try {
            bitmap = imageDiskLrucache.loadBitmapFromDiskCache(url, reqWidth, reqHeight);
            if(bitmap != null){
                imageLrucache.addBitmapToLruCache(MD5Util.hashKeyFromUrl(url), bitmap);
                return bitmap;
            }

            imageDiskLrucache.downloadBitmapToDiskCache(url);
            bitmap = imageDiskLrucache.loadBitmapFromDiskCache(url, reqWidth, reqHeight);
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
        * 还没获取到bitmap而且DiskLru没被创建，直接从网络下载原图。
        * */
        if(bitmap == null && !imageDiskLrucache.isDiskLruCacheCreated()){
            bitmap = NetUtil.downloadBitmapFromUrl(url);
        }
        return bitmap;
    }


    /*
    * 异步接口,用于绑定Bitmap和ImageView
    * */
    public void bindBitmap(final String uri, final ImageView imageView, final int reqWidth, final int reqHeight){
        imageView.setImageResource(R.drawable.empty_photo);

        imageView.setTag(uri);
        //首先从内存缓存中尝试获取
        Bitmap bitmap = imageLrucache.loadBitmapFromLruCacheByUrl(uri);
        if(bitmap != null){
            imageView.setImageBitmap(bitmap);
            return;
        }
        //如果不能获取到，执行异步任务
        LoadBitmapTask loadBitmapTask = new LoadBitmapTask(mContext,mMainHandler,imageView,uri,reqWidth,reqHeight);
        //使用线程池去执行Runnable对象
        THREAD_POOL_EXECUTOR.execute(loadBitmapTask);
    }

    /*
    * 异步接口，仅返回Bitmap
    * */
    public void findBitmap(final String url, final BitmapCallback callback, int reqWidth, int reqHeight){
        //从内存缓存中获取bitmap
        final Bitmap bitmap = imageLrucache.loadBitmapFromLruCacheByUrl(url);
        if(bitmap != null){
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onResponse(bitmap, url);
                }
            });
            return;
        }
        LoadBitmapTask loadBitmapTask = new LoadBitmapTask(mContext, callback, url, reqWidth, reqHeight);
        //使用线程池去执行Runnable对象
        THREAD_POOL_EXECUTOR.execute(loadBitmapTask);
    }

    //返回本地缓存类
    public static ImageDiskLruCache getImageDiskLruCache(Context context){
        if(imageDiskLrucache==null){
            imageDiskLrucache = new ImageDiskLruCache(context);
        }
        return imageDiskLrucache;
    }


    //返回内存缓存类
    public static ImageLruCache getImageLruCache(){
        if(imageLrucache==null){
            imageLrucache = new ImageLruCache();
        }
        return imageLrucache;
    }

    public interface BitmapCallback{
        public void onResponse(Bitmap bitmap, String url);
    }

    public static ThreadPoolExecutor getThreadPoolExecutor() {
        return THREAD_POOL_EXECUTOR;
    }

    public static void setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor) {
        THREAD_POOL_EXECUTOR = threadPoolExecutor;
    }
}
