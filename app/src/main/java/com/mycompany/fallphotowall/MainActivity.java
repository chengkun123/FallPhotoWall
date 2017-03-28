package com.mycompany.fallphotowall;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;


import com.mycompany.fallphotowall.detail.DetailActivity;
import com.mycompany.fallphotowall.fallview.ScrollFallView;


public class MainActivity extends AppCompatActivity implements ScrollFallView.OnPhotoClickListner{
    //private FallScrollView mFallScrollView;
    private ScrollFallView mScrollFallView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        mScrollFallView = (ScrollFallView) findViewById(R.id.my_scroll_view);
        mScrollFallView.setOnPhotoClickListner(this);

    }

    @Override
    public void onPhotoClick(int pos) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra("image_pos", pos);
        startActivity(intent);
    }
}
