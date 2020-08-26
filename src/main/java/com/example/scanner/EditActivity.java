package com.example.scanner;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EditActivity extends AppCompatActivity {
    private static final String path = Environment.getExternalStorageDirectory() + "/Scanner/";
    private String dstFolder1 ="pic/ipm/";
    private String dstFolder2="pic/resize";
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        getSupportActionBar().hide();
        File folder_dst = new File(path,dstFolder1);
        folder_dst.mkdir();
        String[]entries = folder_dst.list();
        for(String s: entries){
            File currentFile = new File(folder_dst.getPath(),s);
            currentFile.delete();
        }
        folder_dst = new File(path,dstFolder2);
        folder_dst.mkdir();
        entries = folder_dst.list();
        for(String s: entries){
            File currentFile = new File(folder_dst.getPath(),s);
            currentFile.delete();
        }
    }
}
