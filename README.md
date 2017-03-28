# FallPhotoWall

- 概述

  使用ScollView+LinearLayout达到瀑布流照片墙效果，在高度最小的LinearLayout中添加ImageView。

  ![picture2.gif](https://github.com/chengkun123/FallPhotoWall/blob/master/ScreenShots/picture2.gif?raw=true)

- 判断是否需要加载Bitmap的条件：是否滑动到了内容最底端以及是否还有在执行的异步加载任务。

  ```java
   scrollY + scrollViewHeight >= contentLayout.getHeight()
                    && mImageLoader.getThreadPoolExecutor().getActiveCount() == 0                 
  ```

  图示如下：

  ![picture1.png](https://github.com/chengkun123/FallPhotoWall/blob/master/ScreenShots/picture1.png?raw=true)

- 利用Handler判断滑动是否停止，只在滑动停止时才可能进行图片加载任务

  ~~~java
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
  ~~~

- 只显示出现在屏幕中的ImageView的Bitmap，为ImageView添加bottom和top两个tag用来判断是否需要显示。

  ~~~java
  for(int i=0; i<mImageViewList.size(); i++){
              ImageView imageView = mImageViewList.get(i);
              int top = (int) imageView.getTag(R.string.border_top);
              int bottom = (int) imageView.getTag(R.string.border_bottom);

              //需要显示
              if(bottom > getScrollY() && top < getScrollY() + scrollViewHeight){
                  String url = (String) imageView.getTag(R.string.image_url);
                  Bitmap bitmap = mImageLoader.getImageLruCache().loadBitmapFromLruCacheByUrl(url);
                  if(bitmap == null){
                      mImageLoader.bindBitmap(url, imageView, imageView.getWidth(), imageView.getHeight());
                  }else{
                      imageView.setImageBitmap(bitmap);
                  }
              }else{//不需要显示
                  imageView.setImageResource(R.drawable.empty_photo);
              }
          }
  ~~~

- 使用了内存缓存和磁盘缓存

  ~~~java
  public static int maxMemory =(int) (Runtime.getRuntime().maxMemory() / 1024);
      public  static  int cacheSize = maxMemory / 8;

      public ImageLruCache() {
          super(cacheSize);
      }
  ~~~

  ~~~java
  public ImageDiskLruCache(Context mContext){
          File diskCacheDir = getDiskCacheDir(mContext, "bitmap");
          if(!diskCacheDir.exists()){
              diskCacheDir.mkdirs();
          }
          if(getFileUsableSpace(diskCacheDir) > DISK_CACHE_SIZE){
              try{
                  mDiskLruCache = DiskLruCache.open(diskCacheDir, 1, 1, DISK_CACHE_SIZE);
                  mIsDiskLruCacheCreated = true;
                  Log.e("新建DiskCache", "DiskCache的位置是：" + diskCacheDir);
              } catch (IOException e) {
                  e.printStackTrace();
              }
          }else{
              mIsDiskLruCacheCreated = false;
          }

      }
  ~~~

  ​

- ImageLoader类封装了存取Bitmap的操作，提供同步接口`getBitmap(String url, int reqWidth, int reqHeight)` （不能在主线程中调用）， 异步接口`bindBitmap(final String uri, final ImageView imageView, final int reqWidth, final int reqHeight)` （绑定ImageView和Bitmap）和`findBitmap(final String url, final BitmapCallback callback, int reqWidth, int reqHeight)`(在主线程中返回异步获取的Bitmap)。

- 问题

  - 虽然在屏幕外面的ImageView不予显示，但是并没有任何ImageView的回收机制，当我增大了url的数量后，出现了OOM，也就是ImageView的实例过多。但是这一块确实不知道应该怎么做，有待研究一下ListView的item的重用机制。
  - 线程池执行异步任务结束时间不确定，所以有序的ImageView的tag中的url顺序并不是数据源中url的顺序。