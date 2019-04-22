package com.nisha.scanner;

/**
 * Created by PC on 4/19/2019.
 */

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import org.opencv.android.JavaCameraView;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

public class CamView extends JavaCameraView implements PictureCallback {

    private static final String TAG = "Sample::CamView";
    private String mPictureFileName;

    private static boolean isFlashLightON = false;

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

    public void takePicture() {
        Log.i(TAG, "Taking picture");

        mPictureFileName = "myImage.jpg";

        mCamera.setPreviewCallback(null);

        Log.i(TAG, "Calling mCamera's takePicture");

        Toast.makeText(getContext().getApplicationContext(), "Saving photo", Toast.LENGTH_SHORT).show();

        mCamera.takePicture(null, null, this);
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Log.i(TAG, "Saving a bitmap to file");

        mCamera.startPreview();
        mCamera.setPreviewCallback(this);

        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

        Log.d(TAG, "selectedImage: " + mPictureFileName);

        Bitmap bm = null;
        bm = rotate(bitmap, 90);

        ContextWrapper cw = new ContextWrapper(getContext().getApplicationContext());
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        File mypath = new File(directory, mPictureFileName);

        FileOutputStream fos = null;
        try{
            fos = new FileOutputStream(mypath);
            bm.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        Toast.makeText(getContext().getApplicationContext(), "Save in OnPictureCallback", Toast.LENGTH_SHORT).show();

        //mCamera.release();
        Intent intent = new Intent(getContext().getApplicationContext(), CapturedImageActivity.class);
        getContext().startActivity(intent);


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

    public void setupCameraFlashLight() {
        Camera  camera = mCamera;
        if (camera != null) {
            Log.i(TAG, "Camera is not null");
            Camera.Parameters params = camera.getParameters();

            if (params != null) {
                Log.i(TAG, "Parameters is not null");
                if (isFlashLightON) {
                    isFlashLightON = false;
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    camera.setParameters(params);
                    camera.startPreview();
                } else {
                    isFlashLightON = true;
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    camera.setParameters(params);
                    camera.startPreview();

                }
            }
        }else{
            Log.i(TAG, "Camera is null");
        }

    }
}


