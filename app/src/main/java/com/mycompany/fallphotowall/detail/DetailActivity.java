package com.mycompany.fallphotowall.detail;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.mycompany.fallphotowall.R;
import com.mycompany.fallphotowall.util.Images;

import java.io.File;

/**
 * Created by Lenovo on 2017/2/21.
 */
public class DetailActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener{

    private ViewPager mViewPager;
    private TextView mTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_detail);

        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mTextView = (TextView) findViewById(R.id.page_text);

        int imagePos = getIntent().getIntExtra("image_pos", 0);
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter();
        mViewPager.setAdapter(viewPagerAdapter);
        mViewPager.setCurrentItem(imagePos);
        mViewPager.addOnPageChangeListener(this);
        mTextView.setText((imagePos + 1) + "/" + Images.imageUrls.length);

    }

    class ViewPagerAdapter extends PagerAdapter{
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            //获得View的Bitmap

            String url = Images.imageUrls[position];
            String path = getImagePath(url);
            //从路径解析，没有就解析默认空图片
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            if(bitmap == null){
                bitmap = BitmapFactory.decodeResource(getResources()
                        , R.drawable.empty_photo);
            }

            //设置View

            View view = LayoutInflater.from(DetailActivity.this)
                    .inflate(R.layout.item_zoom_view, null);
            MyDetailView myDetailView = (MyDetailView) view.findViewById(R.id.zoom_image_view);
            myDetailView.setSourceBitmap(bitmap);
            container.addView(view);

            return view;
        }

        @Override
        public int getCount() {
            return Images.imageUrls.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View view = (View) object;
            container.removeView(view);
        }
    }

    private String getImagePath(String url){
        int lastSlashIndex = url.lastIndexOf("/");
        String imageName = url.substring(lastSlashIndex);
        String saveDir = getFilesDir().getPath() + "/PhotoWallFalls/";

        File file = new File(saveDir);
        if(!file.exists()){
            file.mkdirs();
        }
        return saveDir + imageName;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        mTextView.setText((position + 1) + "/" + Images.imageUrls.length);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
