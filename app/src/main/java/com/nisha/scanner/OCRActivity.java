package com.nisha.scanner;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class OCRActivity extends AppCompatActivity {

    private static final String TAG = "Sample::OCRActivity";
    Bitmap image;
    private TessBaseAPI mTess;
    String datapath = "";

    private void copyFiles() {
        try {
            String filepath = datapath + "/tessdata/eng.traineddata";
            AssetManager assetManager = getAssets();
            InputStream instream = assetManager.open("tessdata/eng.traineddata");
            OutputStream outstream = new FileOutputStream(filepath);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }
            outstream.flush();
            outstream.close();
            instream.close();
            File file = new File(filepath);
            if (!file.exists()) {
                throw new FileNotFoundException();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkFile(File dir) {
        try {
            if (!dir.exists() && dir.mkdirs()) {
                copyFiles();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        try {
            if (dir.exists()) {
                String datafilepath = datapath + "/tessdata/eng.traineddata";
                File datafile = new File(datafilepath);
                if (!datafile.exists()) {
                    copyFiles();
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

//    public void runOCR(View view){
//
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "OnCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr);

        TextView textView = (TextView) findViewById(R.id.tv_OCR_Result);

        Log.i(TAG, "Loading Bitmap");

        ContextWrapper cw = new ContextWrapper(getApplicationContext());

        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        File myImagePath = new File(directory, "myImage.jpg");


        try {

            image = BitmapFactory.decodeStream(new FileInputStream(myImagePath));

            Log.i(TAG, "Into tesseract");
            String language = "eng";
            datapath = getFilesDir()+ "/tesseract/";
            mTess = new TessBaseAPI();
            checkFile(new File(datapath + "tessdata/"));
            mTess.init(datapath, language);

            Log.i(TAG, "Initialization Done");

        }catch(Exception e){
           e.printStackTrace();
        }

        Button ocrBtn = (Button)findViewById(R.id.ocrBtn);
        ocrBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "OCR demanded");
                view.setVisibility(View.INVISIBLE);
                Log.i(TAG, "View invisible");
                try {


                    String OCRresult = null;
                    mTess.setImage(image);
                    OCRresult = mTess.getUTF8Text();
                    TextView tv_OCR_Result = (TextView) findViewById(R.id.tv_OCR_Result);
                    tv_OCR_Result.setText(OCRresult);
                    Toast.makeText(getApplicationContext(), "OCR done!", Toast.LENGTH_SHORT).show();
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        });



    }
}
