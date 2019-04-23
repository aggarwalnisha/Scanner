package com.nisha.scanner;

import com.nisha.scanner.libraries.PolygonView;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOError;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CapturedImageActivity extends AppCompatActivity {

    PolygonView polygonView;

    private static final String TAG = "CapturedImageActivity";

    private int numClicks = -1;
    private Scalar CONTOUR_COLOR;

    //image will contain unchanged image
    private Mat rgba, image, blurred, edges, startM, endM, inputMat, outputMat, finalMat, morphMat;
    Bitmap bitmap, thresh_bitmap, edged_bitmap, warp_bitmap,contour_bitmap, morph_bitmap;
    MatOfPoint2f approx;
    int resultWidth, resultHeight;

    void extractChannel(Mat source, Mat out, int channelNum) {
        List<Mat> sourceChannels=new ArrayList<Mat>();
        List<Mat> outChannel=new ArrayList<Mat>();

        Core.split(source, sourceChannels);

        outChannel.add(new Mat(sourceChannels.get(0).size(),sourceChannels.get(0).type()));

        Core.mixChannels(sourceChannels, outChannel, new MatOfInt(channelNum,0));

        Core.merge(outChannel, out);
    }

    private Point[] sortPoints( Point[] src ) {

        ArrayList<Point> srcPoints = new ArrayList<>(Arrays.asList(src));

        Point[] result = { null , null , null , null };

        Comparator<Point> sumComparator = new Comparator<Point>() {
            @Override
            public int compare(Point lhs, Point rhs) {
                return Double.valueOf(lhs.y + lhs.x).compareTo(rhs.y + rhs.x);
            }
        };

        Comparator<Point> diffComparator = new Comparator<Point>() {

            @Override
            public int compare(Point lhs, Point rhs) {
                return Double.valueOf(lhs.y - lhs.x).compareTo(rhs.y - rhs.x);
            }
        };

        // top-left corner = minimal sum
        result[0] = Collections.min(srcPoints, sumComparator);

        // bottom-right corner = maximal sum
        result[2] = Collections.max(srcPoints, sumComparator);

        // top-right corner = minimal diference
        result[1] = Collections.min(srcPoints, diffComparator);

        // bottom-left corner = maximal diference
        result[3] = Collections.max(srcPoints, diffComparator);

        return result;
    }

    private boolean insideArea(Point[] rp, Size size) {

        int width = Double.valueOf(size.width).intValue();
        int height = Double.valueOf(size.height).intValue();
        int baseMeasure = height/4;

        int bottomPos = height-baseMeasure;
        int topPos = baseMeasure;
        int leftPos = width/2-baseMeasure;
        int rightPos = width/2+baseMeasure;

        return (
                rp[0].x <= leftPos && rp[0].y <= topPos
                        && rp[1].x >= rightPos && rp[1].y <= topPos
                        && rp[2].x >= rightPos && rp[2].y >= bottomPos
                        && rp[3].x <= leftPos && rp[3].y >= bottomPos

        );
    }

    private Map<Integer, PointF> getOutlinePoints(Bitmap tempBitmap) {
        Log.v("aashari-tag", "getOutlinePoints");
        Map<Integer, PointF> outlinePoints = new HashMap<>();
        outlinePoints.put(0, new PointF(0, 0));
        outlinePoints.put(1, new PointF(tempBitmap.getWidth(), 0));
        outlinePoints.put(2, new PointF(0, tempBitmap.getHeight()));
        outlinePoints.put(3, new PointF(tempBitmap.getWidth(), tempBitmap.getHeight()));
        return outlinePoints;
    }

    private Map<Integer, PointF> orderedValidEdgePoints(Bitmap tempBitmap, List<PointF> pointFs) {
        Log.v("aashari-tag", "orderedValidEdgePoints");
        Map<Integer, PointF> orderedPoints = polygonView.getOrderedPoints(pointFs);
        if (!polygonView.isValidShape(orderedPoints)) {
            orderedPoints = getOutlinePoints(tempBitmap);
        }
        return orderedPoints;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_captured_image);

        CONTOUR_COLOR = new Scalar(255,0,0,255);

      //  polygonView = (com.nisha.scanner.libraries.PolygonView)findViewById(R.id.polygonView);

        final ImageView capturedImage = (ImageView)findViewById(R.id.capturedImage);
        ImageView nextBtn = (ImageView)findViewById(R.id.nextBtn);
        ImageView backBtn = (ImageView)findViewById(R.id.backBtn);
        final TextView textView1 = (TextView)findViewById(R.id.textView1);
        textView1.setText("Captured Image");

        //Getting image from internal memory to ImageView
        ContextWrapper cw = new ContextWrapper(getApplicationContext());

        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        File myImagePath = new File(directory, "myImage.jpg");

        try {
            bitmap = BitmapFactory.decodeStream(new FileInputStream(myImagePath));
            capturedImage.setImageBitmap(bitmap);

//            capturedImage.setAdjustViewBounds(true);
//            capturedImage.setScaleType(ImageView.ScaleType.CENTER_CROP);

            Log.i(TAG, "Image set");
        }catch(java.io.FileNotFoundException e){
            Log.i(TAG, "File not found");
        }

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                numClicks += 1;
                // Start Image Processing
                Log.i(TAG, "Starting Image processing");

                if(numClicks == 0){

                    textView1.setText("Resized Image");
                    Size maxImageSize = new Size(1700, 1700);
                    float ratio = Math.min(
                            (float) maxImageSize.width / bitmap.getWidth(),
                            (float) maxImageSize.height / bitmap.getHeight());
                    int width = Math.round((float) ratio * bitmap.getWidth());
                    int height = Math.round((float) ratio * bitmap.getHeight());

                    bitmap = Bitmap.createScaledBitmap(bitmap, width,
                            height, true);

                    capturedImage.setImageBitmap(bitmap);
                }
                else if (numClicks == 1) {

                    textView1.setText("Detected edges");
                    //rgba Image
                    rgba = new Mat();
                    Utils.bitmapToMat(bitmap, rgba);

                    image = new Mat();
                    Utils.bitmapToMat(bitmap, image);

                    //edges will contain Canny Edged image
                    edges = rgba;
                    Imgproc.cvtColor(rgba, edges, Imgproc.COLOR_BGR2GRAY);

                   // Imgproc.Canny(edges, edges, 50, 50);

                    MatOfDouble mean = new MatOfDouble();
                    MatOfDouble std = new MatOfDouble();
                    Core.meanStdDev(edges, mean, std);
                    double[] means = mean.get(0, 0);
                    double[] stds = std.get(0, 0);

                    Imgproc.Canny(edges, edges, means[0]-stds[0], means[0]-stds[0]);

//                    Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(3,3));
//                    Imgproc.dilate(edges, edges, kernel);
//
//                    Mat im = new Mat(rgba.size(), CvType.CV_8UC1);
//                    double otsu_thresh_val = Imgproc.threshold(
//                            edges,im , 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU
//                    );
//
//                    double high_thresh_val  = otsu_thresh_val,
//                            lower_thresh_val = otsu_thresh_val * 0.5;
//
//                    Imgproc.Canny(edges, edges, lower_thresh_val, high_thresh_val);

                    Imgproc.GaussianBlur(edges, edges, new Size(5, 5), 5);

                    edged_bitmap = null;

                    try {
                        edged_bitmap = Bitmap.createBitmap(edges.cols(), edges.rows(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(edges, edged_bitmap);

                        capturedImage.setImageBitmap(edged_bitmap);

//                        capturedImage.setAdjustViewBounds(true);
//                        capturedImage.setScaleType(ImageView.ScaleType.CENTER_CROP);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }

                else if (numClicks == 2) {
                    //Contour Detection

                    textView1.setText("Detected contour");

                    Log.i(TAG, "Contour Detection");

                    List<MatOfPoint> contours = new ArrayList<>();
                    List<MatOfPoint> mcontours = new ArrayList<>();

                    Log.i(TAG, "Finding contours");


                    try {
                        Imgproc.findContours(edges, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

                        Log.i(TAG, String.valueOf(contours.size()));

                        //Sort the contours
                        Collections.sort(contours, new Comparator<MatOfPoint>(){
                            public int compare(MatOfPoint o1, MatOfPoint o2) {
                                double area1 = Imgproc.contourArea(o1);
                                double area2 = Imgproc.contourArea(o2);
                                if (area2>area1)
                                    return 1;
                                else if (area2<area1)
                                    return -1;
                                else
                                    return  0;
                            }
                        });

                        List<MatOfPoint> list = new ArrayList<MatOfPoint>();
                        Iterator<MatOfPoint> iterator = contours.iterator();
                        while (iterator.hasNext()){

                            MatOfPoint contour = iterator.next();
                            Log.i(TAG, String.valueOf(Imgproc.contourArea(contour)));
                            double epsilon = 0.02*Imgproc.arcLength(new MatOfPoint2f(contour.toArray()),true);
                            approx = new MatOfPoint2f();
                            Imgproc.approxPolyDP(new MatOfPoint2f(contour.toArray()),approx,epsilon,true);
                            if(approx.total() == 4){
                                list.add(contour);
                                break;
                            }
                        }


                        Log.i(TAG, list.toString());

                        Imgproc.drawContours(rgba, list, -1, CONTOUR_COLOR, 20);

                        contour_bitmap = null;

                        try {
                            contour_bitmap = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888);
                            Utils.matToBitmap(rgba, contour_bitmap);

                            capturedImage.setImageBitmap(contour_bitmap);

//                            capturedImage.setAdjustViewBounds(true);
//                            capturedImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                else if(numClicks == 3){

                    textView1.setText("Perspective Transformation");
                    try {
                        double[] temp_double;                       // find four points
                        Point top_left, top_right, bottom_left, bottom_right;

                        temp_double = approx.get(0, 0);
                        Point p1 = new Point(temp_double[0], temp_double[1]);

                        temp_double = approx.get(1, 0);
                        Point p2 = new Point(temp_double[0], temp_double[1]);

                        temp_double = approx.get(2, 0);
                        Point p3 = new Point(temp_double[0], temp_double[1]);

                        temp_double = approx.get(3, 0);
                        Point p4 = new Point(temp_double[0], temp_double[1]);

                        List<Point> list = new ArrayList<Point>();
                        list.add(p1);
                        list.add(p2);
                        list.add(p3);
                        list.add(p4);



                        //Sort by x-xoordinates
                        Collections.sort(list, new Comparator<Point>() {
                            @Override
                            public int compare(Point p1, Point p2) {

                                if (p2.x < p1.x) {
                                    return 1;
                                } else if (p2.x > p1.x) {
                                    return -1;
                                } else {
                                    return 0;
                                }
                            }
                        });

                        List<Point> leftList = new ArrayList<Point>();
                        leftList.add(list.get(0));
                        leftList.add(list.get(1));

                        Collections.sort(leftList, new Comparator<Point>() {
                            @Override
                            public int compare(Point p1, Point p2) {
                                if (p2.y < p1.y) {
                                    return 1;
                                } else if (p2.y > p1.y) {
                                    return -1;
                                } else {
                                    return 0;
                                }
                            }
                        });
                        top_left = leftList.get(0);
                        bottom_left = leftList.get(1);

                        List<Point> rightList = new ArrayList<Point>();
                        rightList.add(list.get(2));
                        rightList.add(list.get(3));

                        Collections.sort(rightList, new Comparator<Point>() {
                            @Override
                            public int compare(Point p1, Point p2) {
                                if (p2.y < p1.y) {
                                    return 1;
                                } else if (p2.y > p1.y) {
                                    return -1;
                                } else {
                                    return 0;
                                }
                            }
                        });
                        top_right = rightList.get(0);
                        bottom_right = rightList.get(1);

                        //Show the 4 points and allow them to be dragged (topleft, topright, bottomright, bottomleft)

/*
                        FrameLayout frameLayout = (FrameLayout)findViewById(R.id.frameLayout);


                        ImageView imageView = new ImageView(getApplicationContext());
                        imageView.setImageResource(R.drawable.circle);

                        imageView.setX(50);
                        imageView.setY(50);
//                        imageView.setMinimumHeight(1);
//                        imageView.setMaxHeight(2);
//                        imageView.setMinimumWidth(1);
//                        imageView.setMaxWidth(2);

//                        imageView.setX((float)top_left.x);
//                        imageView.setY((float)top_left.y);
                        Log.i(TAG, "ImageView Set");

                        LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(3,3);
                        imageView.setLayoutParams(parms);


                        frameLayout.addView(imageView);







//                        List<PointF> res = new ArrayList<>();
//                        res.add(new PointF((float)top_left.x, (float)top_left.y));
//                       res.add(new PointF((float)top_right.x, (float)top_right.y));
//                        res.add(new PointF((float)bottom_right.x, (float)bottom_right.y));
//                        res.add(new PointF((float)bottom_left.x, (float)bottom_left.y));
//
//                        Map<Integer, PointF> pointFs = orderedValidEdgePoints(bitmap, res);
//
//
//
//                        polygonView.setPoints(pointFs);
//                        polygonView.setVisibility(View.VISIBLE);
//
//                       // int padding = (int) getResources().getDimension(R.dimen.scanPadding);
//                        int padding = 0;
//
//                        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(bitmap.getWidth() + 2 * padding, bitmap.getHeight() + 2 * padding);
//                        layoutParams.gravity = Gravity.CENTER;
//
//                        polygonView.setLayoutParams(layoutParams);



*/

                        List<Point> source = new ArrayList<>();
                        source.add(top_left);
                        source.add(top_right);
                        source.add(bottom_right);
                        source.add(bottom_left);


                        Log.i(TAG, Double.toString(top_left.x));
                        Log.i(TAG, top_left.toString());
                        Log.i(TAG, top_right.toString());
                        Log.i(TAG, bottom_right.toString());
                        Log.i(TAG, bottom_left.toString());


                        startM = Converters.vector_Point2f_to_Mat(source);

                        resultWidth = Math.abs((int) (top_right.x - top_left.x));
                        int bottomWidth = Math.abs((int) (bottom_right.x - bottom_left.x));
                        if (bottomWidth > resultWidth)
                            resultWidth = bottomWidth;

                        resultHeight = (int) (bottom_left.y - top_left.y);
                        int bottomHeight = (int) (bottom_right.y - top_right.y);
                        if (bottomHeight > resultHeight)
                            resultHeight = bottomHeight;

                        inputMat = image;
                        outputMat = new Mat(resultWidth, resultHeight, CvType.CV_8UC1);

                        Point ocvPOut1 = new Point(0, 0);
                        Point ocvPOut2 = new Point(resultWidth, 0);
                        Point ocvPOut3 = new Point(resultWidth, resultHeight);
                        Point ocvPOut4 = new Point(0, resultHeight);

                        List<Point> dest = new ArrayList<>();
                        dest.add(ocvPOut1);
                        dest.add(ocvPOut2);
                        dest.add(ocvPOut3);
                        dest.add(ocvPOut4);
                        endM = Converters.vector_Point2f_to_Mat(dest);

                        Mat perspectiveTransform = Imgproc.getPerspectiveTransform(startM, endM);

                        Imgproc.warpPerspective(inputMat, outputMat, perspectiveTransform, new Size(resultWidth, resultHeight));

                        Core.copyMakeBorder(outputMat, outputMat, 5, 5, 5, 5, Core.BORDER_CONSTANT);
                        warp_bitmap = null;

                        resultHeight+=10;
                        resultWidth +=10;

                        warp_bitmap = Bitmap.createBitmap(resultWidth, resultHeight, Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(outputMat, warp_bitmap);

                        capturedImage.setImageBitmap(warp_bitmap);

//                        capturedImage.setAdjustViewBounds(true);
//                        capturedImage.setScaleType(ImageView.ScaleType.CENTER_CROP);


                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                else if(numClicks == 4){
                    textView1.setText("Adaptive Threshold");
                    try{
                        finalMat = outputMat;
                        Imgproc.cvtColor(outputMat, finalMat, Imgproc.COLOR_BGR2GRAY);
//
                     //   Imgproc.medianBlur(finalMat, finalMat, 3);
                      //  Imgproc.threshold(finalMat, finalMat, 0, 255, Imgproc.THRESH_OTSU);

                        Imgproc.GaussianBlur(finalMat, finalMat, new Size(3, 3), 0);
                  //      Imgproc.threshold(finalMat, finalMat, 0, 255, Imgproc.THRESH_OTSU);

                        Imgproc.adaptiveThreshold(finalMat, finalMat, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 7, 3);

                    //    Imgproc.adaptiveThreshold(finalMat, finalMat, 230, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 19, 5);
                        thresh_bitmap = null;

                        thresh_bitmap = Bitmap.createBitmap(resultWidth, resultHeight, Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(finalMat, thresh_bitmap);

                        capturedImage.setImageBitmap(thresh_bitmap);

//                        capturedImage.setAdjustViewBounds(true);
//                        capturedImage.setScaleType(ImageView.ScaleType.CENTER_CROP);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                else if(numClicks == 5) {
                    textView1.setText("Morphological Transformation");
                    Log.i(TAG, "Morphological Operations");
                    try{
                        morphMat = finalMat;
                        Mat kernel = new Mat(new Size(1, 1), CvType.CV_8UC1, new Scalar(255));
                        Imgproc.morphologyEx(finalMat, morphMat, Imgproc.MORPH_OPEN, kernel);
                        Imgproc.morphologyEx(morphMat, morphMat, Imgproc.MORPH_CLOSE, kernel);

                        morph_bitmap = null;

                        morph_bitmap = Bitmap.createBitmap(resultWidth, resultHeight, Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(morphMat, morph_bitmap);

                        capturedImage.setImageBitmap(morph_bitmap);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }

                else if(numClicks == 6){
                    Log.i(TAG, "Opening directory");
                    ContextWrapper cw = new ContextWrapper(getApplicationContext());
                    File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
                    File mypath = new File(directory, "myImage.jpg");

                    FileOutputStream fos = null;
                    try{
                        fos = new FileOutputStream(mypath);

                        morph_bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);

                        /*
                        Size maxImageSize = new Size(1200, 1200);
                        float ratio = Math.min(
                                (float) maxImageSize.width / thresh_bitmap.getWidth(),
                                (float) maxImageSize.height / thresh_bitmap.getHeight());
                        int width = Math.round((float) ratio * thresh_bitmap.getWidth());
                        int height = Math.round((float) ratio * thresh_bitmap.getHeight());

                        Bitmap newBitmap = Bitmap.createScaledBitmap(thresh_bitmap, width,
                                height, true);

                        newBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        */
                        fos.close();
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    Toast.makeText(getApplicationContext(), "OCR Activity", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "Sent to OCR activity");
                    Intent intent = new Intent(getApplicationContext(), OCRActivity.class);
                    startActivity(intent);
                }

            }




        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                numClicks -= 1;
                if(numClicks == 0){
                    textView1.setText("Captured Image");
                    capturedImage.setImageBitmap(bitmap);



                }else if(numClicks == 1){
                    textView1.setText("Detected edges");
                    capturedImage.setImageBitmap(edged_bitmap);



                } else if(numClicks == 2){
                    textView1.setText("Detected contour");
                    capturedImage.setImageBitmap(contour_bitmap);



                } else if(numClicks == 3){
                    textView1.setText("Perspective Transformation");
                    capturedImage.setImageBitmap(warp_bitmap);



                } else if(numClicks == 4){
                    textView1.setText("Adaptive Threshold");
                    capturedImage.setImageBitmap(thresh_bitmap);



                } else if(numClicks == 5){
                    textView1.setText("Morphological Transform");
                    capturedImage.setImageBitmap(morph_bitmap);



                }

            }
        });


    }
}
