package com.example.pr_test;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class SnapsModelModule {


    // 모델 파일 인터프리터를 생성하는 공통 함수
    // loadModelFile 함수에 예외가 포함되어 있기 때문에 반드시 try, catch 블록이 필요하다.
    public static Interpreter getTfliteInterpreter(Activity activity,String modelPath) {
        try {
            return new Interpreter(SnapsModelModule.loadModelFile(activity, modelPath));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public static Interpreter getTfliteInterpreter(Activity activity,String modelPath,Boolean useGPU) {
        try {
            if (useGPU==Boolean.TRUE) {
                GpuDelegate delegate = new GpuDelegate();
                Interpreter.Options options = (new Interpreter.Options()).addDelegate(delegate);
                return new Interpreter(SnapsModelModule.loadModelFile(activity, modelPath), options);
            }else{
                return new Interpreter(SnapsModelModule.loadModelFile(activity, modelPath));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 모델을 읽어오는 함수로, 텐서플로 라이트 홈페이지에 있다.
    // MappedByteBuffer 바이트 버퍼를 Interpreter 객체에 전달하면 모델 해석을 할 수 있다.
    public static MappedByteBuffer loadModelFile(Activity activity, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

}
