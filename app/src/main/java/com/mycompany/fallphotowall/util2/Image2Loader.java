package com.mycompany.fallphotowall.util2;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import com.mycompany.fallphotowall.util.DiskLruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Lenovo on 2017/3/10.
 */
public class Image2Loader {
    private static final String TAG = "Image2Loader";

    private static final int MESSAGE_POST_RESULT = 1;

    /*
    * 线程池相关
    * */
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final long KEEP_ALIVE = 10L;


    private static final int TAG_KEY_URI = 1;
    private static final long DISK_CACHE_SIZE = 1024 * 1024 * 50;
    private boolean mIsDiskLruCacheCreated = false;
    private static final int DISK_CACHE_INDEX = 0;
    private static final int IO_BUFFER_SIZE = 8 * 1024;


    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "ImageLoaderThread#"+mCount.getAndIncrement());
        }
    };

    /*
    * 新建线程池管理异步线程
    * */
    public static final Executor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(CORE_POOL_SIZE
            , MAXIMUM_POOL_SIZE, KEEP_ALIVE
            , TimeUnit.SECONDS
            , new LinkedBlockingQueue<Runnable>()
            , sThreadFactory);


    private static class ImageResult{
        public String uri;
        public ImageView imageView;
        public Bitmap bitmap;

        public ImageResult(String uri, android.widget.ImageView imageView, Bitmap bitmap) {
            this.uri = uri;
            this.imageView = imageView;
            this.bitmap = bitmap;
        }
    }

    /*
    * 一个和主线程绑定的Handler，处理异步线程结果
    * 若异步线程uri和imageView当前绑定的uri相同，更新图片。
    * */
    private Handler mMainHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            ImageResult result = (ImageResult) msg.obj;
            String nowUri = (String) result.imageView.getTag();
            ImageView imageView = result.imageView;
            if(result.uri == nowUri){
                imageView.setImageBitmap(result.bitmap);
            }else{
                Log.w(TAG, "URI改变");
            }
        }
    };

    private Context mContext;
    private LruCache<String, Bitmap> mLruCache;
    private DiskLruCache mDiskLruCache;
    private ImageResizer mImageResizer;

    /*
    * 初始化Lru和DiskLru
    * */
    private Image2Loader(Context context){
        mContext = context.getApplicationContext();
        mImageResizer = new ImageResizer();

        //设置Lru
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;
        mLruCache = new LruCache<String, Bitmap>(cacheSize){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight() / 1024;
            }
        };

        //设置DiskLru
        File diskCacheDir = getDiskCacheDir(mContext, "bitmap");
        if(!diskCacheDir.exists()){
            diskCacheDir.mkdirs();
        }
        if(getFileUsableSpace(diskCacheDir) > DISK_CACHE_SIZE){
            try{
                mDiskLruCache = DiskLruCache.open(diskCacheDir, 1, 1, DISK_CACHE_SIZE);
                mIsDiskLruCacheCreated = true;
                Log.e(TAG, "DiskCache的位置是："+diskCacheDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /*
    * 创建一个Image2Loader实例
    * */
    public static Image2Loader build(Context context){
        return new Image2Loader(context);
    }


    private void addBitmapToLruCache(String key, Bitmap bitmap){
        if(mLruCache.get(key) == null){
            mLruCache.put(key, bitmap);
        }
    }
    private Bitmap getBitmapFromLruCache(String key){
        return mLruCache.get(key);
    }


    public void bindBitmap(final String uri, final ImageView imageView){
        bindBitmap(uri, imageView, 0, 0);
    }

    /*
    * 创建一个异步获取Bitmap的任务
    * */
    public void bindBitmap(final String uri, final ImageView imageView
            , final int reqWidth, final int reqHeight){
        imageView.setTag(uri);
        Bitmap bitmap = loadBitmapFromLruCache(uri);
        if(bitmap != null){
            imageView.setImageBitmap(bitmap);
            return;
        }

        /*
        * 如果无法直接获取到，那么开启一个获取任务
        * */
        Runnable loadTask = new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = loadBitmap(uri, reqWidth, reqHeight);
                if(bitmap != null){
                    ImageResult result = new ImageResult(uri, imageView, bitmap);
                    mMainHandler.obtainMessage(MESSAGE_POST_RESULT, result).sendToTarget();
                }

            }
        };
        THREAD_POOL_EXECUTOR.execute(loadTask);
    }

    /*
    * 同步加载接口，从缓存，硬盘或网络获取Bitmap
    *
    * */
    public Bitmap loadBitmap(String uri, int reqWidth, int reqHeight){
        Bitmap bitmap = loadBitmapFromLruCache(uri);
        if(bitmap != null){
            Log.e(TAG, "从缓存中获取了");
            return bitmap;
        }

        try {
            bitmap = loadBitmapFromDiskCache(uri, reqWidth, reqHeight);
            if(bitmap != null){
                Log.e(TAG, "从硬盘中获取了");
                return bitmap;
            }
            bitmap = loadBitmapFromHttp(uri, reqWidth, reqHeight);
            Log.e(TAG, "从Http中获取了");
        }catch (IOException e){
            e.printStackTrace();
        }
        //没有成功从http获取
        if(bitmap == null && !mIsDiskLruCacheCreated){
            Log.w(TAG, "遇到错误了，DiskLru没创建");
            bitmap = downloadBitmapFromUrl(uri);
        }
        return bitmap;
    }




    /*
    * 从Lru中获取Bitmap,注意是以转换后的url为key
    * */
    private Bitmap loadBitmapFromLruCache(String url){
        final String key = hashKeyForUrl(url);
        return getBitmapFromLruCache(key);
    }

    /*
    * 从DiskLru中获取Bitmap
    * */
    private Bitmap loadBitmapFromDiskCache(String url, int reqWidth, int reqHeight) throws IOException{
        if(Looper.myLooper() == Looper.getMainLooper()){
            Log.w(TAG, "正在主线程中加载Bitmap，不建议！");
        }
        if(mDiskLruCache == null){
            return null;
        }
        Bitmap bitmap = null;
        String key = hashKeyForUrl(url);
        DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
        if(snapshot != null){
            FileInputStream fileInputStream = (FileInputStream) snapshot.getInputStream(DISK_CACHE_INDEX);
            FileDescriptor fileDescriptor = fileInputStream.getFD();
            bitmap = mImageResizer.decodeSampledBitmapFromFileDescriptor(fileDescriptor, reqWidth, reqHeight);
            if(bitmap != null){
                addBitmapToLruCache(key, bitmap);
            }
        }
        return bitmap;
    }
    /*
    * 从Http获取Bitmap,先把原图保存进DiskLru，然后从中获取压缩后的图
    * */
    private Bitmap loadBitmapFromHttp(String url, int reqWidth, int reqHeight) throws IOException{
        if (Looper.myLooper() == Looper.getMainLooper()){
            throw new RuntimeException("不能从主线程访问网络");
        }
        if(mDiskLruCache == null){
            return null;
        }

        //下载完后准备存放进硬盘,注意这里是原大小放入硬盘
        String key = hashKeyForUrl(url);
        DiskLruCache.Editor editor = mDiskLruCache.edit(key);
        if(editor != null){
            OutputStream outputStream = editor.newOutputStream(DISK_CACHE_INDEX);
            if(downloadUrlToStream(url, outputStream)){
                editor.commit();
            }else{
                editor.abort();
            }
            mDiskLruCache.flush();
        }
        return loadBitmapFromDiskCache(url, reqWidth, reqHeight);
    }
    /*
    * 向OutputStream下载
    * */
    private boolean downloadUrlToStream(String url, OutputStream outputStream){
        HttpURLConnection connection = null;
        BufferedOutputStream out = null;
        BufferedInputStream in = null;
        try {
            final URL url1 = new URL(url);
            connection = (HttpURLConnection) url1.openConnection();
            in = new BufferedInputStream(connection.getInputStream());
            out = new BufferedOutputStream(outputStream, IO_BUFFER_SIZE);
            int b;
            while ((b = in.read()) != -1){
                out.write(b);
            }
            return true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if(connection != null){
                    connection.disconnect();
                }
                if(in != null){
                    in.close();
                }
                if (out != null){
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /*
    * 从输入流解析，这里获得的是原图大小
    * */
    private Bitmap downloadBitmapFromUrl(String url){
        Bitmap bitmap = null;
        HttpURLConnection connection = null;
        BufferedInputStream in = null;
        try {
            URL url1 = new URL(url);
            connection = (HttpURLConnection) url1.openConnection();
            in = new BufferedInputStream(connection.getInputStream(),IO_BUFFER_SIZE);
            bitmap = BitmapFactory.decodeStream(in);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try{
                if (connection!= null){
                    connection.disconnect();
                }
                if (in != null){
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bitmap;
    }

    /*
    * 把url转换为MD5或者hashcode
    * */
    private String hashKeyForUrl(String url){
        String hashKey;
        try{
            final MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(url.getBytes());
            hashKey = bytesToHexString(messageDigest.digest());

        } catch (NoSuchAlgorithmException e) {
            hashKey = String.valueOf(url.hashCode());
        }
        return hashKey;
    }

    /*
    * 把字节数组转化为十六进制字符串
    * */
    private String bytesToHexString(byte[] bytes){
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<bytes.length; i++){
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if(hex.length() == 1){
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    /*
    * 通过一个名字在cache根目录下创建一个保存Bitmap的文件夹
    * */
    private File getDiskCacheDir(Context context, String saveName){
        boolean externalStorageAvailable = Environment.getExternalStorageState()
                .equals(Environment.MEDIA_MOUNTED);
        final String cacheDir;
        if(externalStorageAvailable){
            cacheDir = context.getExternalCacheDir().getPath();
        }else{
            cacheDir = context.getCacheDir().getPath();
        }

        return new File(cacheDir + File.separator + saveName);
    }

    /*
    * 获取文件可用空间大小
    * */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private long getFileUsableSpace(File path){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD){
            return path.getUsableSpace();
        }
        final StatFs statFs = new StatFs(path.getPath());
        return (long)statFs.getBlockSize() * (long)statFs.getAvailableBlocks();

    }

}
