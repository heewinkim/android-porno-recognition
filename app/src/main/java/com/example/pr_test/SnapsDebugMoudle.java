package com.example.pr_test;

import android.util.Log;

public class SnapsDebugMoudle {

    private static long tic;
    private static long toc;

    public static void tic(){
        tic = System.currentTimeMillis();
    }
    public static void toc(){
        toc = System.currentTimeMillis();
        long diff_millis = toc-tic;
        Log.d("SNAPS", String.format("time difference == %d ms",diff_millis));
    }
    public static void toc(String tag){
        toc = System.currentTimeMillis();
        long diff_millis = toc-tic;
        Log.d(tag, String.format("time difference == %d ms",diff_millis));
    }
}
