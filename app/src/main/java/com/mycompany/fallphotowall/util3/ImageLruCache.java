package com.mycompany.fallphotowall.util3;

import android.graphics.Bitmap;
import android.util.LruCache;


/**
 * Created by Lenovo on 2017/3/10.
 */
public class ImageLruCache extends LruCache<String, Bitmap> {
    public static final String TAG ="TAG";

    public static int maxMemory =(int) (Runtime.getRuntime().maxMemory() / 1024);
    public  static  int cacheSize = maxMemory / 8;

    public ImageLruCache() {
        super(cacheSize);
    }

    @Override
    protected int sizeOf(String key, Bitmap value) {
        return value.getByteCount() / 1024;
    }


    public void addBitmapToLruCache(String key, Bitmap bitmap){
        if(get(key) == null){
            put(key, bitmap);
        }
    }

    /*
    * 从Lru中获取以hash获取Bitmap
    * */
    public Bitmap loadBitmapFromLruCacheByHash(String key){
        return get(key);
    }

    /*
    * 从Lru中获取以Url获取Bitmap
    * */
    public Bitmap loadBitmapFromLruCacheByUrl(String url){
        final String key = MD5Util.hashKeyFromUrl(url);
        return get(key);
    }



}
