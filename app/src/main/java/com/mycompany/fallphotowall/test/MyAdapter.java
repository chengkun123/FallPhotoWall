package com.mycompany.fallphotowall.test;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.MessageQueue;
import android.support.v4.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import com.mycompany.fallphotowall.R;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Lenovo on 2017/3/7.
 */
public class MyAdapter extends ArrayAdapter<String>{

    private ListView mListView;
    private LruCache<String, BitmapDrawable> mLruCache;
    private Bitmap mLoadingBitmap;

    public MyAdapter(Context context, int resource, String[] objects) {
        super(context, resource, objects);
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheSize = maxMemory / 8;
        mLruCache = new LruCache<String, BitmapDrawable>(cacheSize){
            @Override
            protected int sizeOf(String key, BitmapDrawable value) {
                return value.getBitmap().getByteCount();
            }
        };
        mLoadingBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.empty_photo);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(mListView == null){
            mListView = (ListView) parent;
        }

        View view;
        if(convertView != null){
            view = convertView;
        }else{
            view = LayoutInflater.from(getContext()).inflate(R.layout.item_test_image, null);
        }

        String url = getItem(position);
        ImageView imageView = (ImageView) view.findViewById(R.id.image);
        imageView.setImageBitmap(mLoadingBitmap);
        //imageView.setTag(url);

        BitmapDrawable bitmapDrawable = mLruCache.get(url);
        if(bitmapDrawable != null){
            imageView.setImageDrawable(bitmapDrawable);
        }else if(cancelAnotherTaskOnThisImageView(url, imageView)){
            LoadTask task = new LoadTask(imageView);
            AsyncDrawable drawable = new AsyncDrawable(getContext().getResources(), mLoadingBitmap, task);
            imageView.setImageDrawable(drawable);
            task.execute(url);
        }
        return view;
    }

    public boolean cancelAnotherTaskOnThisImageView(String url, ImageView imageView){
        LoadTask task = getTask(imageView);
        if(task != null){
            String tempUrl = task.url;
            if(tempUrl == null || !tempUrl.equals(url)){
                task.cancel(true);
            }else{
                return false;
            }
        }
        return true;
    }


    class AsyncDrawable extends BitmapDrawable{
        private WeakReference<LoadTask> mLoadTaskWeakReference;
        public AsyncDrawable(Resources res, Bitmap bitmap, LoadTask task) {
            super(res, bitmap);
            mLoadTaskWeakReference = new WeakReference<LoadTask>(task);
        }

        public LoadTask getLoadTask(){
            return mLoadTaskWeakReference.get();
        }


    }

    /*
    * 返回ImageView当前所关联的task
    *
    * */
    public LoadTask getTask(ImageView imageView){
        if(imageView != null){
            Drawable drawable = imageView.getDrawable();
            if(drawable instanceof AsyncDrawable){
                return ((AsyncDrawable)drawable).getLoadTask();
            }
        }
        return null;
    }


    class LoadTask extends AsyncTask<String, Void, BitmapDrawable>{
        String url;
        private WeakReference<ImageView> mWeakReference;

        public LoadTask(ImageView imageView){
            mWeakReference = new WeakReference<ImageView>(imageView);
        }


        @Override
        protected BitmapDrawable doInBackground(String... params) {
            url = params[0];
            Bitmap bitmap = downloadFromNet(url);
            BitmapDrawable bitmapDrawable = new BitmapDrawable(getContext().getResources(), bitmap);
            if(mLruCache.get(url) == null){
                mLruCache.put(url, bitmapDrawable);
            }
            return bitmapDrawable;
        }


        @Override
        protected void onPostExecute(BitmapDrawable bitmapDrawable) {
            //ImageView imageView = (ImageView) mListView.findViewWithTag(url);
            ImageView imageView = getRightImageView();
            if(imageView != null && bitmapDrawable != null){
                imageView.setImageDrawable(bitmapDrawable);
            }
        }

        /*
        * 判断这个task关联的ImageView此刻是不是也关联着它，
        * 如果不是就返回null
        *
        * */
        private ImageView getRightImageView(){
            ImageView imageView = mWeakReference.get();
            LoadTask task = getTask(imageView);
            if(task == this){
                return imageView;
            }
            return null;
        }
    }

    private Bitmap downloadFromNet(String url){
        HttpURLConnection connection = null;
        Bitmap bitmap = null;
        try{
            URL myUrl = new URL(url);
            connection = (HttpURLConnection) myUrl.openConnection();
            connection.setConnectTimeout(5 * 1000);
            connection.setReadTimeout(10 * 1000);
            bitmap = BitmapFactory.decodeStream(connection.getInputStream());

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (connection != null){
                connection.disconnect();
            }
        }
        return bitmap;
    }

}
