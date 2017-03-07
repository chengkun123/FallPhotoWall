package com.mycompany.fallphotowall;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

import com.mycompany.fallphotowall.test.MyAdapter;
import com.mycompany.fallphotowall.util.Images;

/**
 * Created by Lenovo on 2017/3/7.
 */
public class TestActivity extends AppCompatActivity{
    private ListView listView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        listView = (ListView) findViewById(R.id.list_view);
        MyAdapter adapter = new MyAdapter(this, 0, Images.imageUrls);
        listView.setAdapter(adapter);

    }
}
