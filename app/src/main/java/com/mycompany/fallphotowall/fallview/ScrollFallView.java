package com.mycompany.fallphotowall.fallview;

import android.content.Context;
import android.graphics.Bitmap;
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

import com.mycompany.fallphotowall.util.Images;
import com.mycompany.fallphotowall.util.ImageLoader;

import java.util.ArrayList;
import java.util.List;


public class ScrollFallView extends ScrollView implements View.OnTouchListener, ImageLoader.BitmapCallback{

    private ImageLoader mImageLoader;
    private Context mContext;

    private int columnWidth;
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


    /*
    * 用Handler判断滑动是否停止
    * */
    private Handler sHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            ScrollFallView fallScrollView = (ScrollFallView) msg.obj;
            int scrollY = fallScrollView.getScrollY();

            //如果滑动停止了
            if(lastScrollY == scrollY){
                //如果滑动到最底部，再加载15个
                if(scrollY + scrollViewHeight >= contentLayout.getHeight()
                        && mImageLoader.getThreadPoolExecutor().getActiveCount() == 0){
                    fallScrollView.loadOnePageImages();
                }
                //保证只有可视范围内的ImageView显示Bitmap
                fallScrollView.checkAndSetVisibility();

            }else{//如果没有停止，5ms后继续发送消息以检查
                lastScrollY = scrollY;
                Message m = new Message();
                m.obj = fallScrollView;
                //延迟发送，若还在滑动则必然不会相等
                sHandler.sendMessageDelayed(m, 5);
            }

        }
    };


    public ScrollFallView(Context context, AttributeSet attrs) {
        super(context, attrs);
        //mImageLoader = ImageLoader.getInstance();
        mImageLoader = ImageLoader.getInstance(context);
        mContext = context;
        //initDiskLruCache(context);
        //mTaskCollection = new HashSet<>();
        setOnTouchListener(this);
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
        Log.e("进入了加载的函数", ".......");
        int start = page * PAGE_SIZE;
        int end = start + PAGE_SIZE;

        if(start < Images.imageUrls.length) {
            if (end > Images.imageUrls.length) {
                end = Images.imageUrls.length;
            }
            for (int i = start; i < end; i++) {
                mImageLoader.findBitmap(Images.imageUrls[i], this, columnWidth, 1);
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
                Bitmap bitmap = mImageLoader.getImageLruCache().loadBitmapFromLruCacheByUrl(url);
                if(bitmap == null){//如果Lru中没有了
                    mImageLoader.bindBitmap(url, imageView, imageView.getWidth(), imageView.getHeight());
                }else{
                    imageView.setImageBitmap(bitmap);
                }
            }else{//不需要显示
                imageView.setImageResource(R.drawable.empty_photo);
            }
        }
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_UP){
            Message msg = new Message();
            msg.obj = this;
            sHandler.sendMessageDelayed(msg, 5);
        }
        return false;
    }

    @Override
    public void onResponse(Bitmap bitmap, String url) {
        addImage(bitmap, url, columnWidth, bitmap.getHeight());
    }

    /*
    *  新建ImageView并且添加照片
    * */
    private void addImage(Bitmap bitmap, String url, int width, int height){
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);

        ImageView imageView = new ImageView(getContext());
        imageView.setPadding(5, 5, 5, 5);
        imageView.setImageBitmap(bitmap);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        imageView.setLayoutParams(params);
        imageView.setTag(R.string.image_url, url);
        imageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnPhotoClickListner.onPhotoClick(mImageViewList.size());
            }
        });
        LinearLayout resultColumn = findRightColumn(imageView, height);
        resultColumn.addView(imageView);

        //将ImageView按照完成顺序添加在
        mImageViewList.add(imageView);
    }

    /*
    * 找到当前高度最小的column
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
