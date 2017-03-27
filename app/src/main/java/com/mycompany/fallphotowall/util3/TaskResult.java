package com.mycompany.fallphotowall.util3;

import android.graphics.Bitmap;
import android.widget.ImageView;

/**
 * 用于发送结果给主线程进行ImageView的更新
 */
public class TaskResult {
    public String uri;
    public ImageView imageView;
    public Bitmap bitmap;

    public TaskResult(String uri, ImageView imageView, Bitmap bitmap) {
        this.uri = uri;
        this.imageView = imageView;
        this.bitmap = bitmap;
    }
}
