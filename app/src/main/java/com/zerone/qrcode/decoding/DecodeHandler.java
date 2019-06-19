/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zerone.qrcode.decoding;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.ArrayMap;
import android.util.Log;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.zerone.qrcode.R;
import com.zerone.qrcode.camera.CameraManager;
import com.zerone.qrcode.camera.PlanarYUVLuminanceSource;
import com.zerone.qrcode.scaner.CaptureFragment;

final class DecodeHandler extends Handler {

    private static final String TAG = "DecodeHandler sniper";

    private final CaptureFragment fragment;
    private final MultiFormatReader multiFormatReader;

    private Result mRawResult = null;
    private byte[] mBytes;
    private byte[] mRotatedData;
    private long mStart;
    private PlanarYUVLuminanceSource mSource;
    private BinaryBitmap mBitmap;
    private HybridBinarizer mHybridBinarizer;

    DecodeHandler(CaptureFragment fragment, ArrayMap<DecodeHintType, Object> hints) {
        multiFormatReader = new MultiFormatReader();
        multiFormatReader.setHints(hints);
        this.fragment = fragment;
    }

    @Override
    public void handleMessage(Message message) {
        Log.d(TAG, "handleMessage: " + message.what);
        if (message.what == R.id.decode) {
            mStart = System.currentTimeMillis();
            mBytes = (byte[]) message.obj;
            Log.d(TAG, "handleMessage: 数组大小：" + mBytes.length);
            if (mRotatedData == null || mRotatedData.length != mBytes.length) {
                Log.d(TAG, "handleMessage: 创建专职数组");
                mRotatedData = new byte[mBytes.length];
            }
            decode(message.arg1, message.arg2);
        } else if (message.what == R.id.quit) {
            Log.d(TAG, "handleMessage: quit 推出");
            mBytes = null;
            mRotatedData = null;
            mRawResult = null;
            Looper.myLooper().quit();
        }
    }

    /**
     * Decode the data within the viewfinder rectangle, and time how long it took. For efficiency,
     * reuse the same reader objects from one decode to the next.
     *
     * @param data   The YUV preview frame.
     * @param width  The width of the preview frame.
     * @param height The height of the preview frame.
     *               <p>
     *               <p>
     *               //todo 内存抖动
     */
    private void decode(int width, int height) {
        //modify here
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++)
                mRotatedData[x * height + height - y - 1] = mBytes[x + y * width];
        }
        int tmp = width; // Here we are swapping, that's the difference to #11
        width = height;
        height = tmp;

        mSource = CameraManager.get().buildLuminanceSource(mRotatedData, width, height);
        mHybridBinarizer = new HybridBinarizer(mSource);
        mBitmap = new BinaryBitmap(mHybridBinarizer);

        try {
            mRawResult = multiFormatReader.decodeWithState(mBitmap);
        } catch (ReaderException re) {
            // continue
        } finally {
            multiFormatReader.reset();
        }

        if (mRawResult != null) {
            long end = System.currentTimeMillis();
            Log.d(TAG, "Found barcode (" + (end - mStart) + " ms):\n" + mRawResult.toString());
            finishScaner(mSource);
        } else {
            Message message = Message.obtain(fragment.getHandler(), R.id.decode_failed);
            message.sendToTarget();
        }
    }

    private void finishScaner(PlanarYUVLuminanceSource source) {
        Message message = Message.obtain(fragment.getHandler(), R.id.decode_succeeded, mRawResult);
        Bundle bundle = new Bundle();
        bundle.putParcelable(DecodeThread.BARCODE_BITMAP, source.renderCroppedGreyscaleBitmap());
        message.setData(bundle);
        Log.d(TAG, "Sending decode succeeded message...");
        message.sendToTarget();
    }

}
