package com.mycompany.fallphotowall;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

import com.mycompany.fallphotowall.test.MyAdapter;
import com.mycompany.fallphotowall.util.Images;
import com.mycompany.fallphotowall.util2.Image2Loader;
import com.mycompany.fallphotowall.util2.ImageResizer;
import com.mycompany.fallphotowall.util2.TestAdapter;

/**
 * Created by Lenovo on 2017/3/10.
 */
public class Test2Activity extends AppCompatActivity{
    private ImageResizer mImageResizer;
    private Image2Loader mImage2Loader;
    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_2_test);
        /*mImageResizer = new ImageResizer();
        Bitmap bitmap = mImageResizer
                .decodeSampledBitmapFromResource(getResources(), R.drawable.image2, 100, 100);*/
        mImage2Loader = Image2Loader.build(this);
        mListView = (ListView) findViewById(R.id.list_view);
        TestAdapter adapter =  new TestAdapter(this, 0, Images.imageUrls, mImage2Loader);
        mListView.setAdapter(adapter);
    }
}
