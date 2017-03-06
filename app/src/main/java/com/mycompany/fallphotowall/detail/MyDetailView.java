package com.mycompany.fallphotowall.detail;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Lenovo on 2017/2/21.
 */
public class MyDetailView extends View{

    private int mWidth;
    private int mHeight;


    private Bitmap mSourceBitmap;
    private int mCurrentStatus;
    private final static int STATUS_INIT = 1;
    private final static int STATUS_MOVE = 2;
    private final static int STATUS_ZOOM_OUT = 3;
    private final static int STATUS_ZOOM_IN = 4;

    private Matrix mMatrix = new Matrix();
    private float mInitRatio;
    private float mMatrixRatio;
    private float totalTranslateX;
    private float totalTranslateY;
    private float currentBitmapWidth;
    private float currentBitmapHeight;

    private float lastXMove = -1;
    private float lastYMove = -1;
    private float movedDistanceX;
    private float movedDistanceY;

    private double lastFingersDis;
    private float centerPointX;
    private float centerPointY;
    private float mScaledRatio;


    public MyDetailView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mCurrentStatus = STATUS_INIT;
    }

    public void setSourceBitmap(Bitmap bitmap){
        mSourceBitmap = bitmap;
        invalidate();
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if(changed){
            mWidth = getWidth();
            mHeight = getHeight();
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()){
            case MotionEvent.ACTION_POINTER_DOWN:
                if(event.getPointerCount() == 2){
                    lastFingersDis = calculateFingersDis(event);
                    Log.e("两只手指之间的距离是：", lastFingersDis+"");
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if(event.getPointerCount() == 1){
                    confirmMovedDis(event);
                    invalidate();
                }else if(event.getPointerCount() == 2){
                    confirmModeAndRatio(event);
                    invalidate();
                }
            case MotionEvent.ACTION_POINTER_UP:
                if (event.getPointerCount() == 2) {
                    // 手指离开屏幕时将临时值还原
                    lastXMove = -1;
                    lastYMove = -1;
                }
                break;
            case MotionEvent.ACTION_UP:
                // 手指离开屏幕时将临时值还原
                lastXMove = -1;
                lastYMove = -1;
                break;
        }
        return true;
    }


    /*
    * 确定移动的距离
    * */
    private void confirmMovedDis(MotionEvent event){
        Log.e("在Move","+++++");
        mCurrentStatus = STATUS_MOVE;
        float x = event.getX();
        float y = event.getY();
        if(lastXMove == -1 && lastYMove == -1){
            lastXMove = x;
            lastYMove = y;
        }
        movedDistanceX = x - lastXMove;
        movedDistanceY = y - lastYMove;
        Log.e("x方向上移动的距离是",""+movedDistanceX);
        Log.e("y方向上移动的距离是",""+movedDistanceY);

        if (totalTranslateX + movedDistanceX > 0){
            movedDistanceX = 0;
        }else if(mWidth - (totalTranslateX + movedDistanceX) > currentBitmapWidth){
            movedDistanceX = 0;
        }
        if(totalTranslateY + movedDistanceY > 0){
            movedDistanceY = 0;
        } else if(mHeight - (totalTranslateY + movedDistanceY) > currentBitmapHeight){
            movedDistanceY = 0;
        }

        lastXMove = x;
        lastYMove = y;
    }

    /*
    * 确定缩放的大小
    * */
    private void confirmModeAndRatio(MotionEvent event){
        calculateFingersCenter(event);
        double fingersDis = calculateFingersDis(event);
        if(fingersDis > lastFingersDis){
            mCurrentStatus = STATUS_ZOOM_OUT;
        }else{
            mCurrentStatus = STATUS_ZOOM_IN;
        }
        if((mCurrentStatus == STATUS_ZOOM_OUT && mMatrixRatio < 4 * mInitRatio)
                || (mCurrentStatus == STATUS_ZOOM_IN && mMatrixRatio > mInitRatio)){
            //用于计算translate
            mScaledRatio = (float) (fingersDis / lastFingersDis);
            //用于计算最后的大小
            mMatrixRatio = mMatrixRatio * mScaledRatio;
            if(mMatrixRatio > 4*mInitRatio){
                mMatrixRatio = 4*mInitRatio;
            }else if(mMatrixRatio < mInitRatio){
                mMatrixRatio = mInitRatio;
            }
        }

        lastFingersDis = fingersDis;
    }

    /*
    * 根据不同的状态进行相应的动作
    * */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        switch (mCurrentStatus){
            case STATUS_INIT:
                initBitmap(canvas);
                break;
            case STATUS_MOVE:
                move(canvas);
                break;
            case STATUS_ZOOM_IN:
            case STATUS_ZOOM_OUT:
                zoom(canvas);
                break;
            default:
                canvas.drawBitmap(mSourceBitmap, mMatrix, null);
                break;
        }

    }

    /*
    * 初始化图片，需要计算出translate和scale
    *
    * */
    private void initBitmap(Canvas canvas){
        if(mSourceBitmap != null){
            mMatrix.reset();
            int bitmapWidth = mSourceBitmap.getWidth();
            int bitmapHeight = mSourceBitmap.getHeight();
            Log.e("bitmap的尺寸是",bitmapWidth + "*" + bitmapHeight);
            //如果bitmap宽高有超过View的宽高
            if(bitmapWidth > mWidth || bitmapHeight > mHeight){
                //如果宽超过的更多,宽压缩成View宽，Y方向上移动保证居中
                if((bitmapWidth - mWidth) > (bitmapHeight - mHeight)){
                    float ratio = mWidth / (1f * bitmapWidth);
                    mMatrix.postScale(ratio, ratio);

                    //注意这里计算的时候不要考虑正负号
                    float translateY = (mHeight - bitmapHeight * ratio) / 2;
                    mMatrix.postTranslate(0, translateY);
                    totalTranslateY = translateY;
                    Log.e("压缩了宽","高的位移是" + totalTranslateY);
                    mMatrixRatio = mInitRatio = ratio;
                    Log.e("压缩宽的比例是",""+mMatrixRatio);
                }else{
                    float ratio = mHeight / (1f * bitmapHeight);
                    mMatrix.postScale(ratio, ratio);
                    float translateX = (mWidth - bitmapWidth * ratio) / 2;
                    mMatrix.postTranslate(translateX, 0);
                    totalTranslateX = translateX;
                    Log.e("压缩了高","宽的位移是" + totalTranslateX);
                    mMatrixRatio = mInitRatio = ratio;
                    Log.e("压缩高的比例是",""+mMatrixRatio);
                }
                currentBitmapWidth = bitmapWidth * mMatrixRatio;
                currentBitmapHeight = bitmapHeight * mMatrixRatio;
                Log.e("压缩后的宽高是",currentBitmapWidth + "*" + currentBitmapHeight);

            }else{//如果bitmap宽高没超过View宽高，无需压缩，直接居中
                float translateX = (mWidth - bitmapWidth) * 1f / 2;
                float translateY = (mHeight - bitmapHeight) * 1f / 2;
                mMatrix.postTranslate(translateX, translateY);
                currentBitmapHeight = bitmapHeight;
                currentBitmapWidth = bitmapWidth;
                mMatrixRatio = mInitRatio = 1f;
                totalTranslateX = translateX;
                totalTranslateY = translateY;
                Log.e("没有压缩，宽高是",currentBitmapWidth + "*" + currentBitmapHeight);
                Log.e("位移是","宽的位移是："+totalTranslateX+","+"高的位移是："+totalTranslateY);
            }
            canvas.drawBitmap(mSourceBitmap, mMatrix, null);
        }
    }

    /*
    * 移动，需要知道translate
    * */
    private void move(Canvas canvas){
        Log.e("进行了移动重绘", "++++");
        mMatrix.reset();
        float translateX = totalTranslateX + movedDistanceX;
        float translateY = totalTranslateY + movedDistanceY;
        mMatrix.postScale(mMatrixRatio, mMatrixRatio);

        mMatrix.postTranslate(translateX, translateY);

        totalTranslateX = translateX;
        totalTranslateY = translateY;

        canvas.drawBitmap(mSourceBitmap, mMatrix, null);
    }


    /*
    *
    * 缩放，注意translate的计算
    * */
    private void zoom(Canvas canvas){
        mMatrix.reset();
        //确定缩放大小
        mMatrix.postScale(mMatrixRatio, mMatrixRatio);

        //确定translate
        float scaledWidth = mSourceBitmap.getWidth() * mMatrixRatio;
        float scaledHeight = mSourceBitmap.getHeight() * mMatrixRatio;
        float translateX = 0f;
        float translateY = 0f;
        if(scaledWidth < mWidth){
            translateX = (mWidth - scaledWidth) * 1f / 2;
        }else{
            translateX = totalTranslateX * mScaledRatio + (centerPointX - centerPointX * mScaledRatio);

            //这两句话是保证在原本图片宽度大于View宽的情况下，我们缩小的时候最多让边和View边重合。
            if(translateX > 0){
                translateX = 0;
            }
            else if( -translateX + mWidth > scaledWidth){
                translateX = mWidth - scaledWidth;
            }

        }

        if(scaledHeight < mHeight){
            translateY = (mWidth - scaledHeight) * 1f / 2;
        }else{
            translateY = totalTranslateY * mScaledRatio + (centerPointY - centerPointY * mScaledRatio);

            if(translateY > 0){
                translateY = 0;
            }
            else if(- translateY + mHeight > scaledHeight){
                translateY = mHeight - scaledHeight;
            }
        }
        mMatrix.postTranslate(translateX, translateY);
        currentBitmapWidth = scaledWidth;
        currentBitmapHeight = scaledHeight;
        totalTranslateX = translateX;
        totalTranslateY = translateY;
        canvas.drawBitmap(mSourceBitmap, mMatrix, null);

    }

    private double calculateFingersDis(MotionEvent event){
        float xDis = Math.abs(event.getX(0) - event.getX(1));
        float yDis = Math.abs(event.getY(0) - event.getY(1));

        return Math.sqrt(xDis * xDis + yDis * yDis);
    }

    private void calculateFingersCenter(MotionEvent event){
        centerPointX = (event.getX(0) + event.getX(1)) / 2;
        centerPointY = (event.getY(0) + event.getY(1)) / 2;
    }

}
