package com.mycompany.fallphotowall.util3;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;

import com.mycompany.fallphotowall.R;

/**
 * 用于给ImageView绑定Bitmap的Handler
 */
public class BindHandler extends Handler {
    public BindHandler() {
        super(Looper.getMainLooper());
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        TaskResult result = (TaskResult) msg.obj;
        ImageView imageView = result.imageView;
        if(imageView.getTag() == result.uri){
            if(result.bitmap != null){
                imageView.setImageBitmap(result.bitmap);
            }else{
                imageView.setImageResource(R.drawable.ic_error);
            }
        }else{
            Log.e("bind时","此url已过期");
        }

    }
}
