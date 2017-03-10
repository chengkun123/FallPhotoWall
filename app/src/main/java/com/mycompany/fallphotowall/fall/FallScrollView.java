package com.mycompany.fallphotowall.fall;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.mycompany.fallphotowall.R;
import com.mycompany.fallphotowall.util.DiskLruCache;
import com.mycompany.fallphotowall.util.ImageLoader;
import com.mycompany.fallphotowall.util.Images;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Lenovo on 2017/2/19.
 */
public class FallScrollView extends ScrollView implements View.OnTouchListener{
    //工具
    private ImageLoader mImageLoader;
    private DiskLruCache mDiskLruCache;


    private int columnWidth;
    private static Set<LoadImageTask> mTaskCollection;
    private List<ImageView> mImageViewList = new ArrayList<>();
    private boolean loadOnce;
    private static int scrollViewHeight;
    private static View contentLayout;

    private int page;
    private final static int PAGE_SIZE = 15;

    private int firstColumnHeight;
    private int secondColumnHeight;
    private int thirdColumnHeight;
    private LinearLayout mFirstColumn;
    private LinearLayout mSecondColumn;
    private LinearLayout mThirdColumn;

    private static int lastScrollY = -1;

    private OnPhotoClickListner mOnPhotoClickListner;

    public interface OnPhotoClickListner{
        void onPhotoClick(int pos);
    }

    public void setOnPhotoClickListner(OnPhotoClickListner onPhotoClickListner){
        mOnPhotoClickListner = onPhotoClickListner;
    }

    private static Handler sHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            FallScrollView fallScrollView = (FallScrollView) msg.obj;
            int scrollY = fallScrollView.getScrollY();

            if(lastScrollY == scrollY){
                if(scrollY + scrollViewHeight >= contentLayout.getHeight()
                        && mTaskCollection.isEmpty()){
                    fallScrollView.loadOnePageImages();
                }
                fallScrollView.checkAndSetVisibility();

            }else{
                lastScrollY = scrollY;
                Message m = new Message();
                m.obj = fallScrollView;
                //延迟发送，若还在滑动则必然不会相等
                sHandler.sendMessageDelayed(m, 5);
            }

        }
    };


    public FallScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mImageLoader = ImageLoader.getInstance();
        initDiskLruCache(context);
        mTaskCollection = new HashSet<>();
        setOnTouchListener(this);
    }


    /*
    * 初始化DiskLruCache的缓存根目录
    * */
    private void initDiskLruCache(Context context){
        try{
            File saveFile = getDiskCacheDir(context, "thumb");
            if(!saveFile.exists()){
                saveFile.mkdirs();
            }
            mDiskLruCache = DiskLruCache.open(saveFile, 1, 1, 10 * 1024 *1024);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if(changed && !loadOnce){
            scrollViewHeight = getHeight();
            contentLayout = getChildAt(0);
            mFirstColumn = (LinearLayout) findViewById(R.id.first_column);
            mSecondColumn = (LinearLayout) findViewById(R.id.second_column);
            mThirdColumn = (LinearLayout) findViewById(R.id.third_column);
            columnWidth = mFirstColumn.getWidth();
            loadOnce = true;
            loadOnePageImages();
        }

    }


    /*
    * 加载15张图片
    * */
    private void loadOnePageImages(){
        Log.e("进入了加载的函数",".......");
        int start = page * PAGE_SIZE;
        int end = start + PAGE_SIZE;
        /*int start = page * 3;
        int end = start + 3;*/

        if(start < Images.imageUrls.length) {
            if (end > Images.imageUrls.length) {
                end = Images.imageUrls.length;
            }
            for (int i = start; i < end; i++) {
                LoadImageTask task = new LoadImageTask();
                mTaskCollection.add(task);
                task.execute(i);
            }
            page++;
        }else{
            Log.e("加载完了", "没有更多图片");
        }

    }

    public void checkAndSetVisibility(){
        for(int i=0; i<mImageViewList.size(); i++){
            ImageView imageView = mImageViewList.get(i);
            int top = (int) imageView.getTag(R.string.border_top);
            int bottom = (int) imageView.getTag(R.string.border_bottom);

            /*
            * 需要显示
            * */
            if(bottom > getScrollY() && top < getScrollY() + scrollViewHeight){
                String url = (String) imageView.getTag(R.string.image_url);
                Bitmap bitmap = mImageLoader.getBitmapFromMemoryCache(url);
                if(bitmap == null){//如果Lru中没有了
                    LoadImageTask task = new LoadImageTask(imageView);
                    task.execute(i);
                }else{
                    imageView.setImageBitmap(bitmap);
                }
            }else{//不需要显示
                imageView.setImageResource(R.drawable.empty_photo);
            }
        }
    }


    /*private boolean hasSDCard(){
        return Environment.MEDIA_MOUNTED.equals(Environment
                .getExternalStorageState());
    }*/

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_UP){
            Message msg = new Message();
            msg.obj = this;
            sHandler.sendMessageDelayed(msg, 5);
        }

        return false;
    }


    private File getDiskCacheDir(Context context, String fileName){
        String fileDir;
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)
                || !Environment.isExternalStorageRemovable()) {
            fileDir = context.getExternalCacheDir().getPath();
            Log.e("保存在了SD卡", "。。。");
        }else{
            fileDir = context.getCacheDir().getPath();
            Log.e("保存在了内部存储", "。。。");
        }
        return new File(fileDir + File.separator + fileName);
    }



    class LoadImageTask extends AsyncTask<Integer, Void, Bitmap>{

        //private String mImageUrl;
        private int mImagePos;

        private ImageView mImageView;

        public LoadImageTask(){
            Log.e("创建了一个Task","+++++");
        }

        public LoadImageTask(ImageView imageView){
            mImageView = imageView;
        }

        /*
        * 根据url找到bitmap，如果不存在直接下载并保存
        * */
        @Override
        protected Bitmap doInBackground(Integer... params) {
            mImagePos = params[0];
            String url = Images.imageUrls[mImagePos];
            Bitmap bitmap = mImageLoader.getBitmapFromMemoryCache(url);
            if(bitmap == null){
                Log.e("Lru中没有，在内部存储和网络中获取","......");
                bitmap = loadImageFromDiskOrInternet(url);
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if(bitmap != null){
                Log.e("onPost中成功的到bitmap","....");
                //以宽度
                /*double ratio = bitmap.getWidth() / (columnWidth * 1.0);
                int height = (int) (bitmap.getHeight() / ratio);*/
                addImage(bitmap, columnWidth, bitmap.getHeight());
            }
            mTaskCollection.remove(this);
        }



        /*
        * 如果图片不存在，下载并缓存到内存、手机。
        * */
        private Bitmap loadImageFromDiskOrInternet(String imageUrl){
            //获取图片应该保存在的路径
            //String imagePath = getImagePath(imageUrl);

            FileDescriptor fileDescriptor = null;
            FileInputStream fileInputStream = null;
            DiskLruCache.Snapshot snapShot = null;
            try{
                String key = getKeyForDiskCache(imageUrl);
                Log.e("文件保存的路径是",key);
                snapShot = mDiskLruCache.get(key);

                //如果手机中不存在，下载并保存在手机存储
                if(snapShot == null){
                    Log.e("正在从网络获取","...");
                    DiskLruCache.Editor editor = mDiskLruCache.edit(key);
                    if(editor != null){
                        //0代表保存文件的下标，这里因为设置每个文件大小为1，所以是0
                        OutputStream os = editor.newOutputStream(0);
                        if(downloadFromInternet(os, imageUrl)){
                            editor.commit();
                        }else{
                            editor.abort();
                        }
                    }
                    snapShot = mDiskLruCache.get(key);
                }
                //如果手机中存在，从输入流中解析
                if(snapShot != null){
                    Log.e("正在从手机内部存储获取","...");
                    fileInputStream = (FileInputStream) snapShot.getInputStream(0);
                    fileDescriptor = fileInputStream.getFD();
                }
                Bitmap bitmap = null;
                //这里直接获取的是压缩后的图片
                if(fileDescriptor != null){
                    bitmap = ImageLoader.decodeSampledBitmapFromDescriptor(fileDescriptor, columnWidth);
                }

                //把小图缓存到内存
                if (bitmap != null){
                    mImageLoader.addBitmapToMemoryCache(imageUrl, bitmap);
                }
                return bitmap;

            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                if (fileDescriptor == null && fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e) {

                    }
                }
            }
            return null;



            /*//如果文件不存在，从网络下载并保存在Lru中
            File file = new File(imagePath);
            if(!file.exists()){
                Log.e("该路径下文件并不存在","从网络获取图片");
                LoadImageFromInternet(imageUrl);
            }else{//如果文件是存在的，只是Lru中没有，就从文件中获取Bitmap
                Log.e("该路径下文件存在","从文件中解析Bitmap");
                Bitmap bitmap = Image2Loader.decodeSampledBitmapFromResource(imagePath, columnWidth);
                Log.e("从文件解析的bitmap空",""+(bitmap == null));
                mImageLoader.addBitmapToMemoryCache(imageUrl, bitmap);
            }
            return mImageLoader.getBitmapFromMemoryCache(imageUrl);*/
        }

        /*
        * 把url用MD5转化为文件名用于保存
        *
        * */
        private String getKeyForDiskCache(String url){
            String key;
            try {
                MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                messageDigest.update(url.getBytes());
                key = bytesToHexString(messageDigest.digest());
            } catch (NoSuchAlgorithmException e) {
                key = String.valueOf(url.hashCode());
            }
            return key;
        }

        /*
        * 把字节数组中的每一项转化为16进制并用字符串表示，连接这些字符串
        * */
        private String bytesToHexString(byte[] byteArray){
            StringBuilder sb = new StringBuilder();
            for(int i=0; i<byteArray.length; i++){
                String temp = Integer.toHexString(0XFF & byteArray[i]);
                if(temp.length() == 1){
                    sb.append('0');
                }
                sb.append(temp);
            }
            return sb.toString();
        }


        /*
        * 根据Url获得照片的名称并返回保存照片的最终路径
        * */
        private String getImagePath(String url){
            int index = url.lastIndexOf("/");
            String imageName = url.substring(index + 1);

            //由于Nexus6没有SD卡，这里保存在内部存储
            String imageDir = getContext().getFilesDir().getPath() + "/PhotoWallFalls/";
            //以路径imageDir新建一个File实例并检测内部存储中该路径是否是存在文件
            File imageFile = new File(imageDir);
            //如果文件不存在，以这个路径为根路径创建一个文件
            if(!imageFile.exists()){
                imageFile.mkdirs();
            }

            String imagePath = imageDir + imageName;
            return imagePath;
        }

        private boolean downloadFromInternet(OutputStream os, String url){
            HttpURLConnection connection = null;
            BufferedInputStream bis = null;
            BufferedOutputStream bos = null;
            try {
                URL url1 = new URL(url);
                connection = (HttpURLConnection) url1.openConnection();
                bis = new BufferedInputStream(connection.getInputStream(), 8*1024);
                bos = new BufferedOutputStream(os, 8*1024);
                int count;
                while ((count = bis.read()) != -1){
                    bos.write(count);
                }
                return true;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                if(connection != null){
                    connection.disconnect();
                }
                try {
                    if(bis != null){
                        bis.close();
                    }
                    if(bos != null){
                        bos.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return false;
        }


        /*
        * 根据url下载图片并保存到ImageLoader中
        * */
        private void LoadImageFromInternet(String imageUrl){
            Log.e("开始下载", ".....");
            HttpURLConnection con = null;
            FileOutputStream fos = null;
            BufferedInputStream bis = null;
            BufferedOutputStream bos = null;
            File imageFile = null;
            try {
                URL url = new URL(imageUrl);
                con = (HttpURLConnection) url.openConnection();
                con.setConnectTimeout(5 * 1000);
                con.setReadTimeout(15 * 1000);
                //将con的输入输出都使能
                con.setDoInput(true);
                con.setDoOutput(true);
                //从con的InputStream建立BufferedInputStream
                bis = new BufferedInputStream(con.getInputStream());
                //向imageFile的OutputStream建立BufferedOutputStream
                imageFile = new File(getImagePath(imageUrl));
                bos = new BufferedOutputStream(new FileOutputStream(imageFile));

                byte[] b = new byte[1024];
                int length;
                while((length = bis.read(b)) != -1){
                    bos.write(b, 0, length);
                    bos.flush();
                }
            }catch (Exception e){
                Log.e("下载出错",".....");
                e.printStackTrace();
            }finally {
                try {
                    if(bis != null){
                        bis.close();
                    }
                    if (bos != null) {
                        bos.close();
                    }
                    if (con != null) {
                        con.disconnect();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            if(imageFile != null){
                Bitmap bitmap = ImageLoader
                        .decodeSampledBitmapFromResource(imageFile.getPath(), columnWidth);
                if(bitmap != null){
                    Log.e("下载成功","保存到Lru中");
                    mImageLoader.addBitmapToMemoryCache(imageUrl, bitmap);
                }
            }

        }

        /*
        * 在某个column中添加图片，如果ImageView存在，直接添加；不存在新建一个ImageView
        * */
        private void addImage(Bitmap bitmap, int width, int height){
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
            if(mImageView != null){
                mImageView.setImageBitmap(bitmap);
            }else{
                //第一次添加ImageView的时候为其添加3个Tag
                ImageView imageView = new ImageView(getContext());
                imageView.setPadding(5, 5, 5, 5);
                imageView.setImageBitmap(bitmap);
                imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                imageView.setLayoutParams(params);
                imageView.setTag(R.string.image_url, Images.imageUrls[mImagePos]);
                imageView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mOnPhotoClickListner.onPhotoClick(mImagePos);
                    }
                });
                LinearLayout resultColumn = findRightColumn(imageView, height);
                resultColumn.addView(imageView);
                mImageViewList.add(imageView);
            }

        }

        /*
        * 找到合适的column
        * */
        private LinearLayout findRightColumn(ImageView imageView, int height){
            if(firstColumnHeight <= secondColumnHeight && firstColumnHeight <= thirdColumnHeight){
                imageView.setTag(R.string.border_top, firstColumnHeight);
                firstColumnHeight += height;
                imageView.setTag(R.string.border_bottom, firstColumnHeight);
                return mFirstColumn;
            }else if(secondColumnHeight <= firstColumnHeight && secondColumnHeight <= thirdColumnHeight){
                imageView.setTag(R.string.border_top, secondColumnHeight);
                secondColumnHeight += height;
                imageView.setTag(R.string.border_bottom, secondColumnHeight);
                return mSecondColumn;
            }else{
                imageView.setTag(R.string.border_top, thirdColumnHeight);
                thirdColumnHeight += height;
                imageView.setTag(R.string.border_bottom, thirdColumnHeight);
                return mThirdColumn;
            }
        }
    }




}
