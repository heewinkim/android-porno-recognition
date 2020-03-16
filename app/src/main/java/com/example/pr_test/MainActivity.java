package com.example.pr_test;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ArrayBlockingQueue;

public class MainActivity extends AppCompatActivity {

    String[] permission_list = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    private static final String DEBUG_TAG = "SNAPS";
    private TextView textViewResult1;
    private TextView textViewResult2;
    private ImageView imgCapture;
    private TextView textViewTestResult1;
    private TextView textViewTestResult2;
    private ProgressBar progressBarTest;
    private Switch switchGPU;
    private NumberPicker numberPicker;
    Interpreter model;

    private static final int CAMERA_REQUEST_CODE = 1;
    private static final int GALLERY_REQUEST_CODE = 2;
    private static final int image_width= 224;
    private static final int image_height= 224;
    private static final int[] VGG_MEAN = {104, 117, 123};

    private static final String dataset_path= "/Screenshots/";
    ArrayBlockingQueue<File> filelist;
    private static int DATASET_LENGTH;
    private static boolean TEST_RUN= false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission();

        // view configuration
        textViewResult1= (TextView) findViewById(R.id.textViewResult1);
        textViewResult2= (TextView) findViewById(R.id.textViewResult2);
        imgCapture = (ImageView) findViewById(R.id.capturedImage);
        textViewTestResult1 = (TextView) findViewById(R.id.textViewTestResult1);
        textViewTestResult2 = (TextView) findViewById(R.id.textViewTestResult2);
        progressBarTest = (ProgressBar) findViewById(R.id.progressBarTest);
        switchGPU = (Switch) findViewById(R.id.switchGPU);

        try{
            String load_path = Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_DCIM ).toString() + dataset_path;
            filelist = SnapsImageModule.loadDataset(load_path);
            DATASET_LENGTH = filelist.size();
        }catch (Exception e){
            e.printStackTrace();
        }


        // model init
        model = SnapsModelModule.getTfliteInterpreter(this, "pr_m.1.0.0.tflite",Boolean.FALSE);

        numberPicker= (NumberPicker) findViewById(R.id.numberPicker);
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(8);
        numberPicker.setDescendantFocusability(NumberPicker.FOCUS_AFTER_DESCENDANTS);
        numberPicker.setWrapSelectorWheel(false);
        numberPicker.setValue(1);


        // allocate onclick function
        Button buttonCamera = (Button) findViewById(R.id.btnCamera);
        buttonCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = SnapsImageModule.getImageFromCameraIntent();
                startActivityForResult(intent,CAMERA_REQUEST_CODE);
            }
        });

        Button buttonGallery = (Button) findViewById(R.id.btnGallery);
        buttonGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = SnapsImageModule.getImageFromGalleryIntent();
                startActivityForResult(intent,GALLERY_REQUEST_CODE);
            }
        });

        Button buttonTest = (Button) findViewById(R.id.buttonTest);
        buttonTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TEST_RUN=true;
                int nThread = numberPicker.getValue();
                for (int i=0;i<nThread;i++) {
                    new Evaluate().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
                }
            }
        });
        switchGPU.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    model.close();
                    model = SnapsModelModule.getTfliteInterpreter(MainActivity.this, "pr_m.1.0.0.tflite",Boolean.TRUE);
                    Toast.makeText(getApplicationContext(),"GPU Activate",Toast.LENGTH_SHORT).show();
                }
                else {
                    model.close();
                    model = SnapsModelModule.getTfliteInterpreter(MainActivity.this, "pr_m.1.0.0.tflite",Boolean.FALSE);
                    Toast.makeText(getApplicationContext(),"CPU Activate",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Bitmap bitmap;
        ByteBuffer x;
        float[][] prediction = new float[1][2];

        if(resultCode!=RESULT_OK){return;}

        switch (requestCode) {
            case CAMERA_REQUEST_CODE:
                bitmap = (Bitmap) data.getExtras().get("data");
                bitmap = Bitmap.createScaledBitmap(bitmap, image_width, image_height, false);
                x = (ByteBuffer) preprocessing_input(bitmap);
                SnapsDebugMoudle.tic();
                model.run(x, prediction);
                SnapsDebugMoudle.toc(DEBUG_TAG);
                break;

            case GALLERY_REQUEST_CODE:
                bitmap = loadBitmapFromUri(data.getData());
                bitmap = Bitmap.createScaledBitmap(bitmap, image_width, image_height, false);
                x = (ByteBuffer) preprocessing_input(bitmap);
                SnapsDebugMoudle.tic();
                model.run(x, prediction);
                SnapsDebugMoudle.toc(DEBUG_TAG);
                break;

            case RESULT_CANCELED:
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
                return;
            default:
                Toast.makeText(this, "Stopped", Toast.LENGTH_LONG).show();
                return;
        }
        textViewResult1.setText(String.format("%.2f %%", prediction[0][0] * 100));
        textViewResult2.setText(String.format("%.2f %%", prediction[0][1] * 100));
        imgCapture.setImageBitmap(bitmap);
    }

    // load bitmap from uri which come from Gallery
    private Bitmap loadBitmapFromUri(Uri data){
        Bitmap bm=null;
        try {
            bm = MediaStore.Images.Media.getBitmap(getContentResolver(),  data);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "파일 없음", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "오류", Toast.LENGTH_SHORT).show();
        }
        return bm;
    }

    // bitmap Convert
    private static ByteBuffer preprocessing_input(Bitmap bitmap) {

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int[] pixels = new int[w * h];
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
        ByteBuffer input_img = ByteBuffer.allocateDirect(w * h * 3 * 4);
        input_img.order(ByteOrder.nativeOrder());

        for (int i = 0; i < w * h; i++) {
            int pixel = pixels[i];        // ARGB : ff4e2a2a

            float r = ((pixel >> 16) & 0xff)-VGG_MEAN[0];
            float g = ((pixel >> 8) & 0xff)-VGG_MEAN[1];
            float b = ((pixel >> 0) & 0xff)-VGG_MEAN[2];

            // save to BGR color_model
            input_img.putFloat(b);
            input_img.putFloat(g);
            input_img.putFloat(r);
        }

        return input_img;
    }

    private class Evaluate extends AsyncTask<Void, Object, Void> {

        long startTime;


        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            progressBarTest.setMax(DATASET_LENGTH);
            progressBarTest.setProgress(0);
            textViewTestResult1.setText("총 이미지 : 0 / " + DATASET_LENGTH);
            startTime = System.currentTimeMillis();

        }

        @Override
        protected Void doInBackground(Void... voids) {
            while (filelist.size()!=0 && TEST_RUN){
                try{
                    File file = filelist.take();

                    if( file.toString().matches(".*jpg") ){

                        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                        bitmap = Bitmap.createScaledBitmap(bitmap, image_width, image_height, false);
                        ByteBuffer x = (ByteBuffer) preprocessing_input(bitmap);
                        float[][] prediction = new float[1][2];
                        model.run(x, prediction);
                        Log.d(DEBUG_TAG, "file path : " + file.toString());
                        Log.d(DEBUG_TAG, "pred info : " + String.format("sfw : %.2f%% nsfw : %.2f%%",prediction[0][0],prediction[0][1]));
                        publishProgress(DATASET_LENGTH-filelist.size(),bitmap,prediction);

                    }
                }catch (InterruptedException e){
                    e.printStackTrace();

                }
            }
            return null;
        }
        @Override
        protected void onProgressUpdate(Object... values) {
            int i = (int) values[0];
            Bitmap bitmap = (Bitmap) values[1];
            float[][] prediction = (float[][]) values[2];

            progressBarTest.setProgress((int)values[0]);

            if (numberPicker.getValue()==1) {
                imgCapture.setImageBitmap(bitmap);
                textViewTestResult1.setText(String.format("총 이미지 : %d / %d", i, DATASET_LENGTH));
                textViewTestResult2.setText(String.format("sfw : %.2f%% nsfw : %.2f%%", prediction[0][0], prediction[0][1]));
            }else{
                textViewTestResult1.setText(String.format("총 이미지 : %d / %d" ,i, DATASET_LENGTH));
                textViewTestResult2.setText(String.format("sfw : %.2f%% nsfw : %.2f%%",prediction[0][0],prediction[0][1]));
            }


        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            TEST_RUN=false;

            long difftime = System.currentTimeMillis()-startTime;
            float average_time = difftime/DATASET_LENGTH;
            textViewTestResult2.setText(String.format("평균 응답시간 : %.2f ms",average_time));

            try{
                filelist.clear();
                String load_path = Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_DCIM ).toString() + dataset_path;
                filelist = SnapsImageModule.loadDataset(load_path);
                DATASET_LENGTH = filelist.size();
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }

    public void checkPermission(){
        //현재 안드로이드 버전이 6.0미만이면 메서드를 종료한다.
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return;

        for(String permission : permission_list){
            //권한 허용 여부를 확인한다.
            int chk = checkCallingOrSelfPermission(permission);

            if(chk == PackageManager.PERMISSION_DENIED){
                //권한 허용을여부를 확인하는 창을 띄운다
                requestPermissions(permission_list,0);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==0)
        {
            for(int i=0; i<grantResults.length; i++)
            {
                //허용됬다면
                if(grantResults[i]==PackageManager.PERMISSION_GRANTED){

                }
                else {
                    Toast.makeText(getApplicationContext(),"앱권한설정하세요", Toast.LENGTH_LONG).show();
                    finish();

                }
            }
        }
    }

}
