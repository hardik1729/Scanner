package com.example.scanner;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class SecondFragment extends Fragment {

    private static final String path= Environment.getExternalStorageDirectory() + "/Scanner/";
    private String srcFolder;
    private String dstFolder="/pic/resize";
    private int idx=1;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_second, container, false);
    }

    public void onViewCreated(@NonNull View mView, Bundle savedInstanceState) {
        super.onViewCreated(mView, savedInstanceState);
        srcFolder=getArguments().getString("file");
        File folder_src = new File(path,srcFolder);
        String[]entries = folder_src.list();
        try {
            final File file = new File(path+dstFolder, "sample.pdf");
            file.createNewFile();
            FileOutputStream fOut = new FileOutputStream(file);
            PdfDocument document = new PdfDocument();
            for(String s: entries) {
                File currentFile = new File(folder_src.getPath(), s);
                Bitmap bmp = BitmapFactory.decodeFile(currentFile.getAbsolutePath());
                PdfDocument.PageInfo pageInfo = new
                        PdfDocument.PageInfo.Builder(bmp.getWidth(), bmp.getHeight(), idx).create();
                PdfDocument.Page page = document.startPage(pageInfo);
                Canvas canvas = page.getCanvas();
                canvas.drawBitmap(bmp,0,0,null);
                document.finishPage(page);
                idx++;
            }
            document.writeTo(fOut);
            document.close();
        }catch (IOException e){
//            Log.i("error",e.getLocalizedMessage());
        }
    }
}