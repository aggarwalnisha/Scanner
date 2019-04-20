package com.nisha.scanner;

/**
 * Created by PC on 4/19/2019.
 */

import java.io.FileOutputStream;
import java.util.List;

import org.opencv.android.JavaCameraView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;

public class CamView extends JavaCameraView implements PictureCallback {

    private static final String TAG = "Sample::CamView";
    private String mPictureFileName;

    public static int minWidthQuality = 400;
    private Context context;

    public CamView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    @Override
    protected void AllocateCache() {
        super.AllocateCache();
        setPictureSize();
    }

    protected boolean setPictureSize() {
        try {
            Camera.Parameters params = mCamera.getParameters();
            Log.d(TAG, "getSupportedPictureSizes()");
            List<Camera.Size> sizes = params.getSupportedPictureSizes();
            if (sizes == null) {
                Log.w(TAG, "getSupportedPictureSizes() = null, cannot set a custom size");
                return false;
            }

            int maxSize = 0;
            // choose the largest size that matches the preview AR
            for (android.hardware.Camera.Size size : sizes) {
//                if (size.height * mFrameWidth != size.width * mFrameHeight) {
//                    continue; // the picture size doesn't match
//                }
                if (maxSize > size.width * size.height) {
                    continue; // don't need this size
                }
                params.setPictureSize(size.width, size.height);
                maxSize = size.width * size.height;
            }
            if (maxSize == 0) {
                Log.w(TAG, "getSupportedPictureSizes() has no matches for " + mFrameWidth + 'x' + mFrameHeight);
                return false;
            }
            Log.d(TAG, "try Picture size " + params.getPictureSize().width + 'x' + params.getPictureSize().height);
            mCamera.setParameters(params);
        } catch (Exception e) {
            Log.e(TAG, "setPictureSize for " + mFrameWidth + 'x' + mFrameHeight, e);
            return false;
        }
        return true;
    }






    public List<String> getEffectList() {
        return mCamera.getParameters().getSupportedColorEffects();
    }

    public boolean isEffectSupported() {
        return (mCamera.getParameters().getColorEffect() != null);
    }

    public String getEffect() {
        return mCamera.getParameters().getColorEffect();
    }

    public void setEffect(String effect) {
        Camera.Parameters params = mCamera.getParameters();
        params.setColorEffect(effect);
        mCamera.setParameters(params);
    }

    public List<Size> getResolutionList() {
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    public void setResolution(Size resolution) {
        disconnectCamera();
        mMaxHeight = resolution.height;
        mMaxWidth = resolution.width;
        connectCamera(getWidth(), getHeight());
    }

    public Size getResolution() {
        return mCamera.getParameters().getPreviewSize();
    }

    public void takePicture(final String fileName) {
        Log.i(TAG, "Taking picture");
        this.mPictureFileName = fileName;
        // Postview and jpeg are sent in the same buffers if the queue is not empty when performing a capture.
        // Clear up buffers to avoid mCamera.takePicture to be stuck because of a memory issue
        mCamera.setPreviewCallback(null);

        // PictureCallback is implemented by the current class
        mCamera.takePicture(null, null, this);
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Log.i(TAG, "Saving a bitmap to file");
        // The camera preview was automatically stopped. Start it again.
        mCamera.startPreview();
        mCamera.setPreviewCallback(this);

        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        Uri uri = Uri.parse(mPictureFileName);

        Log.d(TAG, "selectedImage: " + uri);
        Bitmap bm = null;
        bm = rotate(bitmap, 90);

        // Write the image in a file (in jpeg format)
        try {
            FileOutputStream fos = new FileOutputStream(mPictureFileName);

            fos.write(data);
            fos.close();

        } catch (java.io.IOException e) {
            Log.e("PictureDemo", "Exception in photoCallback", e);
        }

    }

    private static Bitmap rotate(Bitmap bm, int rotation) {
        if (rotation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            Bitmap bmOut = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
            return bmOut;
        }
        return bm;
    }
}
