package com.nisha.scanner;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity implements CvCameraViewListener2, OnTouchListener {
    private static final String TAG = "OCVSample::Activity";

    private CamView mOpenCvCameraView;
    private List<Size> mResolutionList;
    private MenuItem[] mEffectMenuItems;
    private SubMenu mColorEffectsMenu;
    private MenuItem[] mResolutionMenuItems;
    private SubMenu mResolutionMenu;

    private Mat mRgba, mRgbaT, mRgbaF;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);

//        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mOpenCvCameraView = (CamView) findViewById(R.id.main_activity_java_surface_view);

//        mOpenCvCameraView.setMinimumHeight(720);
//        mOpenCvCameraView.setMinimumWidth(1280);
//        mOpenCvCameraView.setMaxFrameSize(2340, 4160);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);

        mOpenCvCameraView.disableFpsMeter();

        ImageView captureBtn = (ImageView)findViewById(R.id.captureBtn);
        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                String outPicture = Constants.SCAN_IMAGE_LOCATION + File.separator + Utilities.generateFilename();
//                FolderUtil.createDefaultFolder(Constants.SCAN_IMAGE_LOCATION);
//                mOpenCvCameraView.takePicture(outPicture);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                String currentDateandTime = sdf.format(new Date());
                String fileName = Environment.getExternalStorageDirectory().getPath() +
                        "/sample_picture_" + currentDateandTime + ".jpg";
                mOpenCvCameraView.takePicture(fileName);


                Toast.makeText(MainActivity.this, "Picture has been taken ", Toast.LENGTH_LONG).show();
                Log.d(TAG, "Path " + fileName);
            }
        });
       // captureBtn.setOnTouchListener(MainActivity.this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
//        mRgba = new Mat(height, width, CvType.CV_8UC4);
//        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
//        mRgbaT = new Mat(width, width, CvType.CV_8UC4);
    }

    public void onCameraViewStopped() {
//        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
//        mRgba = inputFrame.rgba();
//        Core.transpose(mRgba, mRgbaT);
//        Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0,0, 0);
//        Core.flip(mRgbaT, mRgba, 1 );
//        return mRgba;
        return inputFrame.rgba();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        super.onCreateOptionsMenu(menu);
        MenuInflater menuInflater = new MenuInflater(this);
        menuInflater.inflate(R.menu.camera_menu, menu);
//
//        List<String> effects = mOpenCvCameraView.getEffectList();
//
//        if (effects == null) {
//            Log.e(TAG, "Color effects are not supported by device!");
//            return true;
//        }
//
//        mColorEffectsMenu = menu.addSubMenu("Color Effect");
//        mEffectMenuItems = new MenuItem[effects.size()];
//
//        int idx = 0;
//        ListIterator<String> effectItr = effects.listIterator();
//        while(effectItr.hasNext()) {
//            String element = effectItr.next();
//            mEffectMenuItems[idx] = mColorEffectsMenu.add(1, idx, Menu.NONE, element);
//            idx++;
//        }
//
//        mResolutionMenu = menu.addSubMenu("Resolution");
//        mResolutionList = mOpenCvCameraView.getResolutionList();
//        mResolutionMenuItems = new MenuItem[mResolutionList.size()];
//
//        ListIterator<Size> resolutionItr = mResolutionList.listIterator();
//        idx = 0;
//        while(resolutionItr.hasNext()) {
//            Size element = resolutionItr.next();
//            mResolutionMenuItems[idx] = mResolutionMenu.add(2, idx, Menu.NONE,
//                    Integer.valueOf(element.width).toString() + "x" + Integer.valueOf(element.height).toString());
//            idx++;
//        }

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item.getGroupId() == 1)
        {
            mOpenCvCameraView.setEffect((String) item.getTitle());
            Toast.makeText(this, mOpenCvCameraView.getEffect(), Toast.LENGTH_SHORT).show();
        }
        else if (item.getGroupId() == 2)
        {
            int id = item.getItemId();
            Size resolution = mResolutionList.get(id);
            mOpenCvCameraView.setResolution(resolution);
            resolution = mOpenCvCameraView.getResolution();
            String caption = Integer.valueOf(resolution.width).toString() + "x" + Integer.valueOf(resolution.height).toString();
            Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
        }

        return true;
    }

    @SuppressLint("SimpleDateFormat")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.i(TAG,"onTouch event");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String currentDateandTime = sdf.format(new Date());
        String fileName = Environment.getExternalStorageDirectory().getPath() +
                "/sample_picture_" + currentDateandTime + ".jpg";
        mOpenCvCameraView.takePicture(fileName);
        Toast.makeText(this, fileName + " saved", Toast.LENGTH_SHORT).show();
        return false;
    }
}
