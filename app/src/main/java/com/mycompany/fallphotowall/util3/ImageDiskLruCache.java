package com.mycompany.fallphotowall.util3;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.StatFs;
import android.util.Log;

import com.mycompany.fallphotowall.util.DiskLruCache;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Lenovo on 2017/3/10.
 */
public class ImageDiskLruCache {
    private DiskLruCache mDiskLruCache;
    private  boolean mIsDiskLruCacheCreated;
    private static final long DISK_CACHE_SIZE = 1024*1024*50;
    private static final int DISK_CACHE_INDEX = 0;

    public ImageDiskLruCache(Context mContext){
        File diskCacheDir = getDiskCacheDir(mContext, "bitmap");
        if(!diskCacheDir.exists()){
            diskCacheDir.mkdirs();
        }
        if(getFileUsableSpace(diskCacheDir) > DISK_CACHE_SIZE){
            try{
                mDiskLruCache = DiskLruCache.open(diskCacheDir, 1, 1, DISK_CACHE_SIZE);
                mIsDiskLruCacheCreated = true;
                Log.e("新建DiskCache", "DiskCache的位置是：" + diskCacheDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            mIsDiskLruCacheCreated = false;
        }

    }


    /*
    * 从DiskLru中获取Bitmap,并没有添加到Lru中
    * */
    public Bitmap loadBitmapFromDiskCache(String url, int reqWidth, int reqHeight) throws IOException{
        if(Looper.myLooper() == Looper.getMainLooper()){
            Log.w("DiskLru下载中", "正在主线程中加载Bitmap，不建议！");
        }
        if(mDiskLruCache == null){
            return null;
        }
        Bitmap bitmap = null;
        String key = MD5Util.hashKeyFromUrl(url);
        DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
        if(snapshot != null){
            FileInputStream fileInputStream = (FileInputStream) snapshot.getInputStream(DISK_CACHE_INDEX);
            FileDescriptor fileDescriptor = fileInputStream.getFD();
            bitmap = Image3Resizer.decodeSampledBitmapFromFileDescriptor(fileDescriptor, reqWidth, reqHeight);

            /*if(bitmap != null){
                addBitmapToLruCache(key, bitmap);
            }*/
        }
        return bitmap;
    }

    /*
    *
    * 从网络下载保存到DiskLru中
    * */
    public void downloadBitmapToDiskCache(String urlString) throws IOException {
        if(mDiskLruCache == null){
            return ;
        }
        String key = MD5Util.hashKeyFromUrl(urlString);
        DiskLruCache.Editor editor = mDiskLruCache.edit(key);
        if(editor != null){
            OutputStream outputStream = editor.newOutputStream(DISK_CACHE_INDEX);
            if(NetUtil.downloadUrlToStream(urlString, outputStream)){
                //提交数据，并是释放连接
                editor.commit();
            }else{
                //释放连接
                editor.abort();
            }
            mDiskLruCache.flush();
        }
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
