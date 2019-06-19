package com.zerone.qrcode.cache;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;
import android.util.LruCache;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * 管理内存中的图片
 */
public class ImageCache {

    private static final String TAG = "ImageCache sniper";
    private static ImageCache instance;
    private Context context;
    private LruCache<String, Bitmap> memoryCache;

    //引用队列
    ReferenceQueue referenceQueue;
    Thread clearReferenceQueue;
    boolean shutDown;

    BitmapFactory.Options options = new BitmapFactory.Options();

    /**
     * 定义一个复用沲
     */
    public static Set<WeakReference<Bitmap>> reuseablePool;


    public static ImageCache getInstance() {
        if (null == instance) {
            synchronized (ImageCache.class) {
                if (null == instance) {
                    instance = new ImageCache();
                }
            }
        }
        return instance;
    }


    private ReferenceQueue<Bitmap> getReferenceQueue() {
        if (null == referenceQueue) {
            //当弱用引需要被回收的时候，会进到这个队列中
            referenceQueue = new ReferenceQueue<Bitmap>();
            //单开一个线程，去扫描引用队列中GC扫到的内容，交到native层去释放
            clearReferenceQueue = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!shutDown) {
                        try {
                            //remove是阻塞式的
                            Reference<Bitmap> reference = referenceQueue.remove();
                            Bitmap bitmap = reference.get();
                            if (null != bitmap && !bitmap.isRecycled()) {
                                bitmap.recycle();
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            clearReferenceQueue.start();
        }
        return referenceQueue;
    }

    //dir是用来存放图片文件的路径
    public void init(Context context) {
        this.context = context.getApplicationContext();

        //复用池
        reuseablePool = Collections.synchronizedSet(new HashSet<WeakReference<Bitmap>>());

        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        //获取程序最大可用内存 单位是M
        int memoryClass = am.getMemoryClass();
        Log.d(TAG, "init:大可用内存 单位是M: " + memoryClass);
        //参数表示能够缓存的内存最大值  单位是byte
        memoryCache = new LruCache<String, Bitmap>(memoryClass / 8 * 1024 * 1024) {
            /**
             * @return value占用的内存大小
             */
            @Override
            protected int sizeOf(String key, Bitmap value) {
                //19之前   必需同等大小，才能复用  inSampleSize=1
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                    return value.getAllocationByteCount();
                }
                return value.getByteCount();
            }

            /**
             * 当lru满了，bitmap从lru中移除对象时，会回调
             */
            @Override
            protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
                if (oldValue.isMutable()) {//如果是设置成能复用的内存块，拉到java层来管理
                    //3.0以下   Bitmap   native
                    //3.0以后---8.0之前  java
                    //8。0开始      native
                    //把这些图片放到一个复用沲中
                    reuseablePool.add(new WeakReference<Bitmap>(oldValue, referenceQueue));
                } else {
                    //oldValue就是移出来的对象
                    oldValue.recycle();
                }


            }
        };
        //valueCount:表示一个key对应valueCount个文件


        getReferenceQueue();
    }

    public void putBitmapToMemeory(String key, Bitmap bitmap) {
        memoryCache.put(key, bitmap);
    }

    public Bitmap getBitmapFromMemory(String key) {
        return memoryCache.get(key);
    }

    public void clearMemoryCache() {
        memoryCache.evictAll();
    }

    //获取复用池中的内容
    public Bitmap getReuseable(int w, int h, int inSampleSize) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            return null;
        }
        Bitmap reuseable = null;
        Iterator<WeakReference<Bitmap>> iterator = reuseablePool.iterator();
        while (iterator.hasNext()) {
            Bitmap bitmap = iterator.next().get();
            if (null != bitmap) {
                //可以复用
                if (checkInBitmap(bitmap, w, h, inSampleSize)) {
                    reuseable = bitmap;
                    iterator.remove();
                    break;
                } else {
                    iterator.remove();
                }
            }
        }
        return reuseable;

    }

    private boolean checkInBitmap(Bitmap bitmap, int w, int h, int inSampleSize) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return bitmap.getWidth() == w && bitmap.getHeight() == h && inSampleSize == 1;
        }
        if (inSampleSize >= 1) {
            w /= inSampleSize;
            h /= inSampleSize;
        }
        int byteCount = w * h * getPixelsCount(bitmap.getConfig());
        return byteCount <= bitmap.getAllocationByteCount();
    }

    private int getPixelsCount(Bitmap.Config config) {
        if (config == Bitmap.Config.ARGB_8888) {
            return 4;
        }
        return 2;
    }


}












