package com.zerone.qrcode.cache;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;
import android.util.LruCache;

import com.google.zxing.Result;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class QrResultCache {

    private static QrResultCache sInstance;
    private static final String TAG = "QrResultCache sniper";
    private Context mContext;
    private LruCache<String, Result> mMemoryCache;

    //引用队列
    ReferenceQueue referenceQueue;
    Thread clearReferenceQueue;
    boolean shutDown;
    /**
     * 定义一个复用沲
     */
    public static Set<WeakReference<Result>> reuseablePool;


    public static QrResultCache getsInstance() {
        if (null == sInstance) {
            synchronized (ImageCache.class) {
                if (null == sInstance) {
                    sInstance = new QrResultCache();
                }
            }
        }
        return sInstance;
    }

    public void init(Context mContext) {
        this.mContext = mContext.getApplicationContext();
        //复用池
        reuseablePool = Collections.synchronizedSet(new HashSet<WeakReference<Result>>());

        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        //获取程序最大可用内存 单位是M
        int memoryClass = am.getMemoryClass() / 10;


        Log.d(TAG, "init:大可用内存 单位是M: " + memoryClass);
        //参数表示能够缓存的内存最大值  单位是byte

        mMemoryCache = new LruCache<String, Result>(memoryClass * 1024 * 1024) {

            /**
             * @return value占用的内存大小
             */


            @Override
            protected int sizeOf(String key, Result value) {

                return super.sizeOf(key, value);
            }

            @Override
            public void trimToSize(int maxSize) {
                super.trimToSize(maxSize);
            }

            @Override
            protected Result create(String key) {
                return super.create(key);
            }

            @Override
            protected void entryRemoved(boolean evicted, String key, Result oldValue, Result newValue) {
                super.entryRemoved(evicted, key, oldValue, newValue);
            }
        };

    }
}
