package com.mycompany.fallphotowall.util3;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

import com.mycompany.fallphotowall.R;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by Lenovo on 2017/3/10.
 */
public class Image3Loader {
    private static Image3Loader instance = null;
    private Context mContext;
    private static ImageLruCache imageLrucache;
    private static ImageDiskLruCache imageDiskLrucache;
    //创建一个静态的线程池对象
    private static ThreadPoolExecutor THREAD_POOL_EXECUTOR = null;
    //创建一个更新ImageView的UI的Handler
    private static BindHandler mMainHandler;
    public static Image3Loader getInstance(Context context){
        if(instance == null){
            synchronized (Image3Loader.class){
                if(instance == null){
                    instance = new Image3Loader(context);
                }
            }
        }
        return instance;
    }
    //私有的构造方法，防止在外部实例化该ImageLoader
    private Image3Loader(Context context){
        mContext =context.getApplicationContext();
        THREAD_POOL_EXECUTOR = ImageThreadPoolExecutor.getInstance();
        imageLrucache = new ImageLruCache();
        imageDiskLrucache = new ImageDiskLruCache(context);
        mMainHandler = new BindHandler();
    }


    public void bindBitmap(final String url, final ImageView imageView){
        bindBitmap(url, imageView, 0, 0);
    }

    public void bindBitmap(final String uri
            ,final ImageView imageView
            ,final int reqWidth
            ,final int reqHeight){
        imageView.setImageResource(R.drawable.empty_photo);

        imageView.setTag(uri);

        Bitmap bitmap = imageLrucache.loadBitmapFromLruCacheByUrl(uri);
        if(bitmap != null){
            imageView.setImageBitmap(bitmap);
            return;
        }
        LoadBitmapTask loadBitmapTask = new LoadBitmapTask(mContext,mMainHandler,imageView,uri,reqWidth,reqHeight);
        //使用线程池去执行Runnable对象
        THREAD_POOL_EXECUTOR.execute(loadBitmapTask);

    }

    //返回本地缓存类
    public static ImageDiskLruCache getImageDiskLrucache(Context context){
        if(imageDiskLrucache==null){
            imageDiskLrucache = new ImageDiskLruCache(context);
        }
        return imageDiskLrucache;
    }


    //返回内存缓存类
    public static ImageLruCache getImageLrucache(){
        if(imageLrucache==null){
            imageLrucache = new ImageLruCache();
        }
        return imageLrucache;
    }

    public interface BitmapCallback{
        public void onResponse(Bitmap bitmap);
    }

}
