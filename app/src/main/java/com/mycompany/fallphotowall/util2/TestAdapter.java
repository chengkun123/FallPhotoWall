package com.mycompany.fallphotowall.util2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import com.mycompany.fallphotowall.R;

/**
 * Created by Lenovo on 2017/3/10.
 */
public class TestAdapter extends ArrayAdapter<String>{

    private Image2Loader mImage2Loader;
    private Bitmap mLoadingBitmap;
    private String[] list;

    public TestAdapter(Context context, int resource, String[] objects, Image2Loader image2Loader) {
        super(context, resource, objects);
        list = objects;
        mImage2Loader = image2Loader;
        mLoadingBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.empty_photo);
    }

    @Override
    public int getCount() {
        return list.length;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
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
        mImage2Loader.bindBitmap(url, imageView, imageView.getWidth(), imageView.getHeight());

        return view;
    }
}
