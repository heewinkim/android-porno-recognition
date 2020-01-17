package com.example.pr_test;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;

public class SnapsImageModule {

    public static Intent getImageFromCameraIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        return intent;
    }

    public static Intent getImageFromGalleryIntent() {

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
        return Intent.createChooser(intent ,"Choose an image");
    }

    public static Bitmap RotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        Bitmap temp = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
        temp = ThumbnailUtils.extractThumbnail(temp, 1080, 1080);

        return temp;
    }
    public static ArrayBlockingQueue<File> loadDataset(String dataset_path) throws Exception{
        File file = new File( dataset_path );
        File[] listfiles = file.listFiles();

        ArrayBlockingQueue<File> filelist = new ArrayBlockingQueue<File>(listfiles.length);
        for (File f:listfiles) {
            filelist.put(f);
        }
        return filelist;
    }
}
