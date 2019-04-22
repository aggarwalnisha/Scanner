package com.nisha.scanner;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOError;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class CapturedImageActivity extends AppCompatActivity {

    private static final String TAG = "CapturedImageActivity";
    Bitmap bitmap;
    private int numClicks = 0;
    private Scalar CONTOUR_COLOR;

    private double maxArea = 0;
    private int maxAreaIdx = 0;

    private final int threshold_level = 2;

    private Mat rgba, image, blurred, edges;
    Mat startM, endM, inputMat, outputMat;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_captured_image);

        CONTOUR_COLOR = new Scalar(255,0,0,255);

        final ImageView capturedImage = (ImageView)findViewById(R.id.capturedImage);
        ImageView okBtn = (ImageView)findViewById(R.id.OkBtn);

        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                numClicks += 1;
                // Start Image Processing
                Log.i(TAG, "Starting Image processing");







                if (numClicks == 1) {
                    //rgba Image
                    rgba = new Mat();
                    Utils.bitmapToMat(bitmap, rgba);

                    image = new Mat();
                    Utils.bitmapToMat(bitmap, image);

                    //Edge Detection
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

                    Imgproc.GaussianBlur(edges, edges, new Size(9, 9), 9);

                    Bitmap edged_bitmap = null;

                    try {
                        edged_bitmap = Bitmap.createBitmap(edges.cols(), edges.rows(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(edges, edged_bitmap);

                        capturedImage.setImageBitmap(edged_bitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }

                else if (numClicks == 2) {
                    //Contour Detection
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
//                        if(iterator.hasNext()) {
//                            iterator.next();
//                        }
                        while (iterator.hasNext()){

                            MatOfPoint contour = iterator.next();
                            Log.i(TAG, String.valueOf(Imgproc.contourArea(contour)));
                            double epsilon = 0.02*Imgproc.arcLength(new MatOfPoint2f(contour.toArray()),true);
                            approx = new MatOfPoint2f();
                            Imgproc.approxPolyDP(new MatOfPoint2f(contour.toArray()),approx,epsilon,true);
                            if(approx.toArray().length == 4){
                                list.add(contour);
                                break;
                            }
                        }


                        Log.i(TAG, list.toString());
                        Imgproc.drawContours(rgba, list, -1, CONTOUR_COLOR, 20);

                        Bitmap contour_bitmap = null;

                        try {
                            contour_bitmap = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888);
                            Utils.matToBitmap(rgba, contour_bitmap);

                            capturedImage.setImageBitmap(contour_bitmap);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                else if(numClicks == 3){


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


                        Bitmap warp_bitmap = null;


                        warp_bitmap = Bitmap.createBitmap(resultWidth, resultHeight, Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(outputMat, warp_bitmap);

                        capturedImage.setImageBitmap(warp_bitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                else if(numClicks == 4){
                    try{
                    Mat finalMat = outputMat;
                    Imgproc.cvtColor(outputMat, finalMat, Imgproc.COLOR_BGR2GRAY);
                    Imgproc.adaptiveThreshold(finalMat, finalMat, 240, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 15);
                    Bitmap thresh_bitmap = null;


                    thresh_bitmap = Bitmap.createBitmap(resultWidth, resultHeight, Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(finalMat, thresh_bitmap);

                    capturedImage.setImageBitmap(thresh_bitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }



            }




        });

        ContextWrapper cw = new ContextWrapper(getApplicationContext());

        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        File myImagePath = new File(directory, "myImage.jpg");

        try {

            bitmap = BitmapFactory.decodeStream(new FileInputStream(myImagePath));
            capturedImage.setImageBitmap(bitmap);
            Log.i(TAG, "Image set");
        }catch(java.io.FileNotFoundException e){
            Log.i(TAG, "File not found");
        }
    }
}
