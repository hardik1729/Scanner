package com.example.scanner;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCamera2View;
import org.opencv.android.JavaCameraView;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.abs;
import static org.opencv.imgproc.Imgproc.createCLAHE;
import static org.opencv.imgproc.Imgproc.getGaussianKernel;

public class FirstFragment extends Fragment {

    private Point mTopLeft;
    private Point mTopRight;
    private Point mBottomLeft;
    private Point mBottomRight;
    private int mBottom;
    private int mRight;
    private int mContrast=50;
    private int mBrightness=50;
    Bitmap bmp;
    private static final String path = Environment.getExternalStorageDirectory() + "/Scanner/";
    private String srcFolder = "pic/img/";
    private String dstFolder ="pic/ipm/";
    private boolean isResizedRotated=false;
    private boolean mDrawCB=false;
    private List<Bitmap> mBmpList=new ArrayList<>();
    private int idxUndoRedo=0;
    private int size=10;
    private int maxWidth;
    private int maxHeight;
    private int range=100;
    private SeekBar seekBar_contrast;
    private SeekBar seekBar_brightness;
    private TextView contrast;
    private TextView brightness;
    private List<File> imgList=new ArrayList<>();
    private int idx=0;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        Camera mCamera=Camera.open(0);
        Camera.Parameters params = mCamera.getParameters();
        List<android.hardware.Camera.Size> sizes = params.getSupportedPreviewSizes();
        CameraBridgeViewBase.ListItemAccessor accessor=new JavaCameraView.JavaCameraSizeAccessor();
        for (Object size : sizes) {
            int height = accessor.getWidth(size);
            int width = accessor.getHeight(size);
            Display display = getActivity().getWindowManager().getDefaultDisplay();
            if (width <= display.getWidth() && height < display.getHeight()) {
                if (width >= maxWidth && height >= maxHeight) {
                    maxWidth = (int) width;
                    maxHeight = (int) height;
                }
            }
        }
        File folder_src = new File(path,srcFolder);
        String[]entries = folder_src.list();
        for(String s: entries){
            File currentFile = new File(folder_src.getPath(),s);
            imgList.add(currentFile);
        }
        return inflater.inflate(R.layout.fragment_first, container, false);
    }

    public void onViewCreated(@NonNull final View mView, Bundle savedInstanceState) {
        super.onViewCreated(mView, savedInstanceState);

        seekBar_contrast=mView.findViewById(R.id.seek_bar_contrast);
        contrast=mView.findViewById(R.id.contrast);
        ((ViewGroup)mView).removeView(seekBar_contrast);
        ((ViewGroup)mView).removeView(contrast);
        brightness=mView.findViewById(R.id.brightness);
        seekBar_brightness=mView.findViewById(R.id.seek_bar_brightness);
        ((ViewGroup)mView).removeView(seekBar_brightness);
        ((ViewGroup)mView).removeView(brightness);
        final SeekBar.OnSeekBarChangeListener contrast=new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mContrast=progress;
                ((ImageView)mView.findViewById(R.id.image_view)).setImageBitmap(CB());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };
        final SeekBar.OnSeekBarChangeListener brightness=new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mBrightness=progress;
                ((ImageView)mView.findViewById(R.id.image_view)).setImageBitmap(CB());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };
        seekBar_contrast.setOnSeekBarChangeListener(contrast);
        seekBar_brightness.setOnSeekBarChangeListener(brightness);

        final CropView crop_view = (CropView) mView.findViewById(R.id.crop_view);
        crop_view.maxHeight=maxHeight;
        crop_view.maxWidth=maxWidth;
        crop_view.range=range;

        final ImageView imageView = (ImageView) mView.findViewById(R.id.image_view);
        setInitialImage(idx);
        imageView.setImageBitmap(mBmpList.get(mBmpList.size()-1));

        crop_view.imgHeight=(mBmpList.get(mBmpList.size()-1)).getHeight();
        crop_view.imgWidth=(mBmpList.get(mBmpList.size()-1)).getWidth();
        crop_view.setOnUpCallback(new CropView.OnUpCallback() {
            @Override
            public void onQuadFinished(final int[] TopLeft,final int[] TopRight,final int[] BottomLeft,final int[] BottomRight) {
//                Toast.makeText(getContext(), "Quad is (" + "{"+TopLeft[1]+","+TopLeft[0]+"}" + ", " + "{"+TopRight[0]+","+TopRight[1]+"}" + ", " + "{"+BottomLeft[0]+","+BottomLeft[1]+"}" + ", " + "{"+BottomRight[0]+","+BottomRight[1]+"}" + ")",
//                        Toast.LENGTH_SHORT).show();
                mTopLeft=new Point(TopLeft[0],TopLeft[1]);
                mTopRight=new Point(TopRight[0],TopRight[1]);
                mBottomLeft=new Point(BottomLeft[0],BottomLeft[1]);
                mBottomRight=new Point(BottomRight[0],BottomRight[1]);
            }
            @Override
            public void onRectFinished(int Bottom,int Right){
//                Toast.makeText(getContext(), "Rect is (" + "{"+Bottom+","+Right+"}"+ ")",
//                        Toast.LENGTH_SHORT).show();
                mBottom=Bottom;
                mRight=Right;
                if(crop_view.mDrawResize){
                    imageView.setImageBitmap(resize());
                }
            }
            @Override
            public void onSwipeFinished(float xSwipe){
                if (!crop_view.mDrawResize && !crop_view.mDrawCrop && !mDrawCB && abs(xSwipe) > 100) {
                    int idx_old=idx;
                    if (xSwipe > 0) {
                        if(idx+1==imgList.size())
                            Toast.makeText(getContext(), "Last Image", Toast.LENGTH_SHORT).show();
                        else
                            idx++;
                    } else {
                        if(idx==0)
                            Toast.makeText(getContext(), "First Image", Toast.LENGTH_SHORT).show();
                        else
                            idx--;
                    }
                    setInitialImage(idx_old);
                    showImage(crop_view,imageView,mBmpList.size()-1);
                }
            }
        });

        mView.findViewById(R.id.next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setInitialImage(idx);
                for (File file:imgList){
                    File dstFile=new File(file.toString().replace(srcFolder,dstFolder));
                    if(!dstFile.exists()){
                        try {
                            Bitmap tempBmp=BitmapFactory.decodeFile(file.getAbsolutePath());
                            FileOutputStream stream = new FileOutputStream(dstFile);
                            tempBmp.compress(Bitmap.CompressFormat.PNG, 100, stream); // bmp is your Bitmap instance
                            // PNG is a lossless format, the compression factor (100) is ignored
                            stream.close();
                        }catch (IOException e){
                            e.printStackTrace();
                        }
                    }
                }
                Bundle bundle=new Bundle();
                bundle.putString("file",dstFolder);
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment,bundle);
            }
        });
        mView.findViewById(R.id.crop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                falsifyExcept(view.getId(),crop_view,(ViewGroup) mView);
                if(!crop_view.mDrawCrop)
                next(crop());
                showImage(crop_view,imageView,mBmpList.size()-1);
            }
        });
        mView.findViewById(R.id.resize).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                falsifyExcept(view.getId(),crop_view,(ViewGroup) mView);
                if(!crop_view.mDrawResize)
                next(resize());
                showImage(crop_view,imageView,mBmpList.size()-1);
            }
        });
        mView.findViewById(R.id.rotate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                falsifyExcept(view.getId(),crop_view,(ViewGroup) mView);
                next(rotate());
                showImage(crop_view,imageView,mBmpList.size()-1);
            }
        });
        mView.findViewById(R.id.CB).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                falsifyExcept(view.getId(),crop_view,(ViewGroup) mView);
                if(!mDrawCB)
                next(CB());
                showImage(crop_view,imageView,mBmpList.size()-1);
            }
        });
        mView.findViewById(R.id.grayscale).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                falsifyExcept(view.getId(),crop_view,(ViewGroup) mView);
                next(grayscale());
                showImage(crop_view,imageView,mBmpList.size()-1);
            }
        });
        mView.findViewById(R.id.gamma).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                falsifyExcept(view.getId(),crop_view,(ViewGroup) mView);
                next(gamma());
                showImage(crop_view,imageView,mBmpList.size()-1);
            }
        });
        mView.findViewById(R.id.undo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                falsifyExcept(view.getId(),crop_view,(ViewGroup) mView);
                if(idxUndoRedo-1==-1){
                    Toast.makeText(getContext(),"Reached undo limit",Toast.LENGTH_LONG).show();
                }else{
                    idxUndoRedo=idxUndoRedo-1;
                }
                showImage(crop_view,imageView,idxUndoRedo);
            }
        });
        mView.findViewById(R.id.redo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                falsifyExcept(view.getId(),crop_view,(ViewGroup) mView);
                if(idxUndoRedo+1==mBmpList.size()){
                    Toast.makeText(getContext(),"Reached redo limit",Toast.LENGTH_LONG).show();
                }else{
                    idxUndoRedo=idxUndoRedo+1;
                }
                showImage(crop_view,imageView,idxUndoRedo);
            }
        });
        mView.findViewById(R.id.reset).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                falsifyExcept(view.getId(),crop_view,(ViewGroup) mView);
                mBmpList.clear();
                next(bmp);
                showImage(crop_view,imageView,mBmpList.size()-1);
            }
        });
    }

    public void setInitialImage(int idx_old) {
        try {
            File file;
            if(mBmpList.size()!=0){
                file = new File(imgList.get(idx_old).toString().replace(srcFolder, dstFolder));
                FileOutputStream stream = new FileOutputStream(file);
                mBmpList.get(idxUndoRedo).compress(Bitmap.CompressFormat.PNG, 100, stream); // bmp is your Bitmap instance
                // PNG is a lossless format, the compression factor (100) is ignored
                stream.close();
            }
            file = imgList.get(idx);
            bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
            if(!mBmpList.isEmpty())
                mBmpList.clear();
            next(bmp);
            file=new File(imgList.get(idx).toString().replace(srcFolder,dstFolder));
            if(file.exists()){
                next(BitmapFactory.decodeFile(file.getAbsolutePath()));
            }

//            Log.d("hola",(new File(file.toString().replace(srcFolder,dstFolder))).toString());
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void next(Bitmap tempBmp){
        if(mBmpList.size()==size) {
            mBmpList.remove(0);
        }
        mBmpList.add(tempBmp);
        idxUndoRedo=mBmpList.size()-1;
    }

    public void showImage(CropView crop_view, ImageView imageView, int pos){
        crop_view.imgHeight=(mBmpList.get(pos)).getHeight();
        crop_view.imgWidth=(mBmpList.get(pos)).getWidth();
        imageView.setImageBitmap(mBmpList.get(pos));
    }

    public void falsifyExcept(int id,CropView crop_view, ViewGroup mView){
        boolean toggle;
        if(id!=R.id.undo && id!=R.id.redo && id!=R.id.reset){
            if(idxUndoRedo+1<mBmpList.size()){
                mBmpList=mBmpList.subList(0,idxUndoRedo+1);
            }
        }
        switch (id){
            case R.id.crop:
                toggle=crop_view.mDrawCrop;
                break;
            case R.id.rotate:
                toggle=isResizedRotated;
                break;
            case R.id.resize:
                toggle=crop_view.mDrawResize;
                break;
            case R.id.CB:
                toggle=mDrawCB;
                break;
            default:
                toggle=false;
                break;
        }
        if(mView.findViewById(R.id.seek_bar_contrast)!=null && mView.findViewById(R.id.seek_bar_brightness)!=null
                && mView.findViewById(R.id.contrast)!=null && mView.findViewById(R.id.brightness)!=null){
            mView.removeView(seekBar_contrast);
            mView.removeView(seekBar_brightness);
            mView.removeView(contrast);
            mView.removeView(brightness);
        }
        crop_view.mDrawCrop=false;
        isResizedRotated=false;
        crop_view.mDrawResize=false;
        mDrawCB=false;
        switch (id){
            case R.id.crop:
                crop_view.mDrawCrop=!toggle;
                break;
            case R.id.rotate:
                isResizedRotated=toggle;
                break;
            case R.id.resize:
                crop_view.mDrawResize=!toggle;
                break;
            case R.id.CB:
                mDrawCB=!toggle;
                if(mDrawCB) {
                    mContrast=50;
                    seekBar_contrast.setProgress(mContrast);
                    mBrightness=50;
                    seekBar_brightness.setProgress(mBrightness);
                    mView.addView(seekBar_contrast);
                    mView.addView(seekBar_brightness);
                    mView.addView(contrast);
                    mView.addView(brightness);
                }
                break;
        }
        crop_view.invalidate();
    }

    public Bitmap crop(){
        int startX = 0;//(int) Math.min(mTopLeft.x, mBottomLeft.x);
        int startY = 0;//(int) Math.min(mTopLeft.y, mTopRight.y);
        int endX = mBmpList.get(mBmpList.size()-1).getWidth();//(int) Math.max(mTopRight.x, mBottomRight.x);
        int endY = mBmpList.get(mBmpList.size()-1).getHeight();//(int) Math.max(mBottomLeft.y, mBottomRight.y);

        Mat src = new Mat(mBmpList.get(mBmpList.size()-1).getHeight(), mBmpList.get(mBmpList.size()-1).getWidth(), CvType.CV_8U);
        Utils.bitmapToMat(mBmpList.get(mBmpList.size()-1), src);
        List<Point> srcList = new ArrayList<>();
        srcList.add(mTopLeft);
        srcList.add(mTopRight);
        srcList.add(mBottomRight);
        srcList.add(mBottomLeft);
        Mat srcMat = Converters.vector_Point2f_to_Mat(srcList);

        Mat dst = new Mat(src.rows(), src.cols(), src.type());
        List<Point> dstList = new ArrayList<>();
        dstList.add(new Point(startX, startY));
        dstList.add(new Point(endX, startY));
        dstList.add(new Point(endX, endY));
        dstList.add(new Point(startX, endY));
        Mat dstMat = Converters.vector_Point2f_to_Mat(dstList);

        Mat transform = Imgproc.getPerspectiveTransform(srcMat, dstMat);
        Imgproc.warpPerspective(src, dst, transform, src.size());

        Rect roi = new Rect(startX, startY, endX - startX, endY - startY);
        Mat cropped = new Mat(dst, roi);

        Bitmap tempBmp = Bitmap.createBitmap(cropped.cols(), cropped.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(cropped, tempBmp);
        return tempBmp;
    }

    public Bitmap resize(){
        Mat src = new Mat(mBmpList.get(mBmpList.size()-1).getHeight(), mBmpList.get(mBmpList.size()-1).getWidth(), CvType.CV_8U);
        Utils.bitmapToMat(mBmpList.get(mBmpList.size()-1), src);
        Mat dst = new Mat(mBottom,mRight,CvType.CV_8U);
        Imgproc.resize(src,dst,dst.size());
        Bitmap tempBmp = Bitmap.createBitmap(dst.cols(), dst.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dst, tempBmp);
        return tempBmp;
    }

    public Bitmap rotate(){
        int flag=0;
        if(isResizedRotated && mBmpList.size()>1){
            flag=1;
        }
        Mat src = new Mat(mBmpList.get(mBmpList.size()-1-flag).getHeight(), mBmpList.get(mBmpList.size()-1-flag).getWidth(), CvType.CV_8U);
        Utils.bitmapToMat(mBmpList.get(mBmpList.size()-1-flag), src);
        Mat dst = new Mat(src.cols(), src.rows(), src.type());
        if(flag==1){
            Core.rotate(src,dst,Core.ROTATE_180);
            isResizedRotated=false;
        } else
            Core.rotate(src,dst,Core.ROTATE_90_CLOCKWISE);
        if(dst.cols()>maxWidth){
            Imgproc.resize(dst,dst,new Size(maxWidth,(dst.rows()*maxWidth)/dst.cols()));
            isResizedRotated=true;
        }
        Bitmap tempBmp = Bitmap.createBitmap(dst.cols(), dst.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dst, tempBmp);
        return  tempBmp;
    }

    public Bitmap CB(){
        Mat src = new Mat(mBmpList.get(mBmpList.size()-1).getHeight(), mBmpList.get(mBmpList.size()-1).getWidth(), CvType.CV_8U);
        Utils.bitmapToMat(mBmpList.get(mBmpList.size()-1), src);
        Mat dst=new Mat();
        src.convertTo(dst,CvType.CV_8U,mContrast/50.0,(mBrightness-50)/1.0);
        Bitmap tempBmp = Bitmap.createBitmap(dst.cols(), dst.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dst, tempBmp);
        return tempBmp;
    }

    public Bitmap grayscale(){
        Mat src = new Mat(mBmpList.get(mBmpList.size()-1).getHeight(), mBmpList.get(mBmpList.size()-1).getWidth(), CvType.CV_8U);
        Utils.bitmapToMat(mBmpList.get(mBmpList.size()-1), src);
        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY);
        Mat dst = new Mat(src.rows(), src.cols(), src.type());
        Imgproc.cvtColor(src, dst, Imgproc.COLOR_GRAY2BGR);
        Bitmap tempBmp = Bitmap.createBitmap(dst.cols(), dst.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dst, tempBmp);
        return tempBmp;
    }

    public Bitmap gamma(){
        Mat src = new Mat(mBmpList.get(mBmpList.size()-1).getHeight(), mBmpList.get(mBmpList.size()-1).getWidth(), CvType.CV_8U);
        Utils.bitmapToMat(mBmpList.get(mBmpList.size()-1), src);
        Imgproc.cvtColor(src,src,Imgproc.COLOR_BGR2HSV);
        List<Mat> planes= new ArrayList<>();
        Core.split(src,planes);
        planes.get(2).convertTo(planes.get(2),CvType.CV_32F);

        double mean = Core.sumElems(planes.get(2)).val[0] / (planes.get(2).rows() * planes.get(2).cols());

        List<Mat> temp_list=new ArrayList<>();
        temp_list.add(planes.get(2));
        if(Core.minMaxLoc(temp_list.get(0)).maxVal>20 && Core.minMaxLoc(temp_list.get(0)).minVal<235) {
            Mat hist = new Mat();
            Imgproc.calcHist(temp_list, new MatOfInt(0), new Mat(), hist, new MatOfInt(256), new MatOfFloat(0f, 256f), false);
            int below_mode = 0;
            for (int i = 0; i < mean; i++) {
                if (hist.get(i, 0)[0] > below_mode) {
                    below_mode = (int) hist.get(i, 0)[0];
                }
            }
            int above_mode = 0;
            for (int i = (int) mean; i < hist.rows(); i++) {
                if (hist.get(i, 0)[0] > above_mode) {
                    above_mode = (int) hist.get(i, 0)[0];
                }
            }
            double gamma = 2;
            if (below_mode > above_mode) {
                Core.subtract(planes.get(2), new Scalar(255), planes.get(2));
                Core.multiply(planes.get(2), new Scalar(-1), planes.get(2));
                mean = 255 - mean;
            }

            planes.get(2).convertTo(planes.get(2), CvType.CV_32F, 1 / (mean));
            Core.pow(planes.get(2), gamma, planes.get(2));
            planes.get(2).convertTo(planes.get(2), planes.get(2).type(), 255);
            if (below_mode > above_mode) {
                Core.subtract(planes.get(2), new Scalar(255), planes.get(2));
                Core.multiply(planes.get(2), new Scalar(-1), planes.get(2));
                mean = 255 - mean;
            }
            Log.d("hola",mean+"x"+below_mode+"x"+above_mode);
        }
        planes.get(2).convertTo(planes.get(2),planes.get(1).type());

        Core.merge(planes,src);
        Imgproc.cvtColor(src,src,Imgproc.COLOR_HSV2BGR);
        Mat dst = new Mat(src.rows(), src.cols(), src.type());
        src.convertTo(dst,CvType.CV_8U);
        Bitmap tempBmp = Bitmap.createBitmap(dst.cols(), dst.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dst, tempBmp);
        return tempBmp;
    }


    /*
    public Bitmap dehaze(){
        Mat src = new Mat(mBmpList.get(mBmpList.size()-1).getHeight(), mBmpList.get(mBmpList.size()-1).getWidth(), CvType.CV_8U);
        Utils.bitmapToMat(mBmpList.get(mBmpList.size()-1), src);
        List<Mat> planes=new ArrayList<>();
        Core.split(src,planes);
        Core.min(planes.get(0),planes.get(1),planes.get(1));
        Core.min(planes.get(1),planes.get(2),planes.get(2));
        Mat kernel=Imgproc.getStructuringElement(Imgproc.MORPH_RECT,new Size(5,5));
        Mat dark=new Mat();
        Imgproc.erode(planes.get(2),dark,kernel);
        Mat hist = new Mat();
        List<Mat> temp_list=new ArrayList<>();
        temp_list.add(dark);
        Imgproc.calcHist(temp_list,new MatOfInt(0),new Mat(),hist,new MatOfInt(256),new MatOfFloat(0f,256f));
        int thresh_count=(int) (dark.cols()*dark.rows()*0.001);
        int cum_sum=0;
        int thresh=255;
        for(int i=thresh;i>-1;i--){
            cum_sum=cum_sum+(int)hist.get(i,0)[0];
            if(cum_sum>thresh_count) {
                thresh=i;
                break;
            }
        }
        Mat bright_mask=new Mat();
        Imgproc.threshold(dark,bright_mask,Math.min(250,thresh),255,Imgproc.THRESH_BINARY);
        Core.split(src,planes);
        int[] A=new int[3];
        for(int i=0;i<3;i++) {
            Core.MinMaxLocResult max = Core.minMaxLoc(planes.get(i), bright_mask);
            A[i]=(int)max.maxVal;
            Log.d("hola",Core.sumElems(hist).val[0]+"x"+thresh+"x"+A[i]+"x"+cum_sum);
        }

        src.convertTo(src,CvType.CV_32F);
        Core.split(src,planes);
        for(int i=0;i<3;i++){
            Core.multiply(planes.get(i),new Scalar(1.0/A[i]),planes.get(i));
        }
        Core.min(planes.get(0),planes.get(1),planes.get(1));
        Core.min(planes.get(1),planes.get(2),planes.get(2));
        Mat t=new Mat();
        Imgproc.erode(planes.get(2),t,kernel);
        Core.multiply(t,new Scalar(0.95),t);
        Core.subtract(Mat.ones(t.size(),t.type()),t,t);

        Mat gray=new Mat();
        Imgproc.cvtColor(src,gray,Imgproc.COLOR_BGR2GRAY);
        gray.convertTo(gray,CvType.CV_32F,1/255.0);
        Mat mean_gray=new Mat();
        Imgproc.boxFilter(gray,mean_gray,CvType.CV_32F,new Size(60,60));
        Mat mean_t=new Mat();
        Imgproc.boxFilter(t,mean_t,CvType.CV_32F,new Size(60,60));
        Mat mean_gray_t=new Mat();
        Core.multiply(gray,t,mean_gray_t);
        Imgproc.boxFilter(mean_gray_t,mean_gray_t,CvType.CV_32F,new Size(60,60));
        Mat cov_gray_t=new Mat();
        Core.multiply(mean_gray,mean_t,cov_gray_t);
        Core.subtract(mean_gray_t,cov_gray_t,cov_gray_t);

        Mat mean_gray_gray=new Mat();
        Core.multiply(gray,gray,mean_gray_gray);
        Imgproc.boxFilter(mean_gray_gray,mean_gray_gray,CvType.CV_32F,new Size(60,60));
        Mat var_gray=new Mat();
        Core.multiply(mean_gray,mean_gray,var_gray);
        Core.subtract(mean_gray_gray,var_gray,var_gray);

        Mat a=new Mat();
        Core.add(var_gray,new Scalar(0.0001),a);
        Core.divide(cov_gray_t,a,a);
        Mat b=new Mat();
        Core.multiply(a,mean_gray,b);
        Core.subtract(mean_t,b,b);
        Imgproc.boxFilter(a,a,CvType.CV_32F,new Size(60,60));
        Imgproc.boxFilter(b,b,CvType.CV_32F,new Size(60,60));

        Core.multiply(a,gray,t);
        Core.add(t,b,t);

        Core.max(t,new Scalar(0.1),t);
        src.convertTo(src,CvType.CV_32F);
        Core.split(src,planes);
        for(int i=0;i<3;i++){
            Core.subtract(planes.get(i),new Scalar(A[i]),planes.get(i));
            Core.divide(planes.get(i),t,planes.get(i));
            Core.add(planes.get(i),new Scalar(A[i]),planes.get(i));
        }
        Core.merge(planes,src);

        for(int i=0;i<3;i++) {
            Core.MinMaxLocResult max = Core.minMaxLoc(temp_list.get(i), Mat.ones(temp_list.get(i).size(),temp_list.get(i).type()));
            Log.d("hola1",max.maxVal+"x"+max.minVal);
        }

        Mat dst = new Mat();
        src.convertTo(dst,CvType.CV_8U);
        Bitmap tempBmp = Bitmap.createBitmap(dst.cols(), dst.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dst, tempBmp);
        return tempBmp;
    }

    public Bitmap dft(){
        Bitmap tempBmp=Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.ARGB_8888);;
        Mat src = new Mat(mBmpList.get(mBmpList.size()-1).getHeight(), mBmpList.get(mBmpList.size()-1).getWidth(), CvType.CV_8U);
        Utils.bitmapToMat(mBmpList.get(mBmpList.size()-1), src);
        Imgproc.cvtColor(src,src,Imgproc.COLOR_BGR2Lab);
        List<Mat> lab_planes=new ArrayList<>(3);
        Core.split(src,lab_planes);
        Mat singleChannel=lab_planes.get(0).clone();
        singleChannel.convertTo(singleChannel, CvType.CV_64FC1);

        List<Mat> planes = new ArrayList<Mat>();
        planes.add(singleChannel);
        planes.add(Mat.zeros(singleChannel.rows(), singleChannel.cols(), CvType.CV_64FC1));

        Mat complexI = Mat.zeros(singleChannel.rows(), singleChannel.cols(), CvType.CV_64FC2);
        Mat complexO = Mat.zeros(singleChannel.rows(), singleChannel.cols(), CvType.CV_64FC2);
        Mat complexP = Mat.zeros(singleChannel.rows(), singleChannel.cols(), CvType.CV_64FC2);

        Core.merge(planes, complexI);
        Core.dft(complexI, complexO);
        Core.split(complexO, planes);
        Mat mag = new Mat(planes.get(0).size(), planes.get(0).type());
        Core.magnitude(planes.get(0), planes.get(1), mag);
        Mat magR=Mat.zeros(mag.size(),mag.type());

        Mat mask=Mat.zeros(complexO.size(),CvType.CV_8U);
        int outer_radius=200/2;
        int inner_radius=100/2;
        int center_radius=100/2;

//        Imgproc.circle(mask,new Point(0,0),outer_radius,new Scalar(255),2*outer_radius);
//        Imgproc.circle(mask,new Point(complexO.cols()-1,0),outer_radius,new Scalar(255),2*outer_radius);
//        Imgproc.circle(mask,new Point(complexO.cols()-1,complexO.rows()-1),outer_radius,new Scalar(255),2*outer_radius);
//        Imgproc.circle(mask,new Point(0,complexO.rows()-1),outer_radius,new Scalar(255),2*outer_radius);
//
//        Imgproc.circle(mask,new Point(0,0),inner_radius,new Scalar(0),2*inner_radius);
//        Imgproc.circle(mask,new Point(complexO.cols()-1,0),inner_radius,new Scalar(0),2*inner_radius);
//        Imgproc.circle(mask,new Point(complexO.cols()-1,complexO.rows()-1),inner_radius,new Scalar(0),2*inner_radius);
//        Imgproc.circle(mask,new Point(0,complexO.rows()-1),inner_radius,new Scalar(0),2*inner_radius);

        Imgproc.circle(mask,new Point(0,0),center_radius,new Scalar(255),2*center_radius);
        Imgproc.circle(mask,new Point(complexO.cols()-1,0),center_radius,new Scalar(255),2*center_radius);
        Imgproc.circle(mask,new Point(complexO.cols()-1,complexO.rows()-1),center_radius,new Scalar(255),2*center_radius);
        Imgproc.circle(mask,new Point(0,complexO.rows()-1),center_radius,new Scalar(255),2*center_radius);

        Imgproc.GaussianBlur(mask,mask,new Size(49,49),2,2);
        mask.convertTo(mask,CvType.CV_64FC1,1.0/255.0);
        Core.multiply(mag,mask,magR);
        planes.set(0,mask);
        planes.set(1,mask);
        Core.merge(planes,mask);

        Core.multiply(complexO,mask,complexP);
//            complexO.copyTo(complexP,mask);
//            mag.copyTo(magR,mask);

        Core.idft(complexP,complexO);
        Core.split(complexO, planes);
        Core.magnitude(planes.get(0), planes.get(1), mag);

        Core.normalize(mag, mag, 0, 255, Core.NORM_MINMAX);
        mag.convertTo(mag, lab_planes.get(0).type());
        lab_planes.set(0,mag);
        Core.merge(lab_planes,src);
        Mat dst=new Mat();
        Imgproc.cvtColor(src,dst,Imgproc.COLOR_Lab2BGR);
        tempBmp = Bitmap.createBitmap(dst.cols(), dst.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dst, tempBmp);


        Core.add(magR, Mat.ones(magR.rows(), magR.cols(), CvType.CV_64FC1), magR);
        Core.log(magR, magR);

        int cx = magR.cols() / 2;
        int cy = magR.rows() / 2;

        Rect q0Rect = new Rect(0, 0, cx, cy);
        Rect q1Rect = new Rect(cx, 0, cx, cy);
        Rect q2Rect = new Rect(0, cy, cx, cy);
        Rect q3Rect = new Rect(cx, cy, cx, cy);
        Mat q0 = new Mat(magR, q0Rect); // Top-Left - Create a ROI per quadrant
        Mat q1 = new Mat(magR, q1Rect); // Top-Right
        Mat q2 = new Mat(magR, q2Rect); // Bottom-Left
        Mat q3 = new Mat(magR, q3Rect); // Bottom-Right

        Mat tmp = new Mat(); // swap quadrants (Top-Left with Bottom-Right)
        q0.copyTo(tmp);
        q3.copyTo(q0);
        tmp.copyTo(q3);
        q1.copyTo(tmp); // swap quadrant (Top-Right with Bottom-Left)
        q2.copyTo(q1);
        tmp.copyTo(q2);

        Core.normalize(magR, magR, 0, 255, Core.NORM_MINMAX);
        Mat dft = new Mat(magR.size(), CvType.CV_8UC1);
        magR.convertTo(dft, CvType.CV_8UC1);

        tempBmp = Bitmap.createBitmap(dft.cols(), dft.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dft, tempBmp);

        return tempBmp;

//        try {
//            File file = new File(path + dstFolder + "name" + ".png");
//            FileOutputStream stream = new FileOutputStream(file);
//            bmp.compress(Bitmap.CompressFormat.PNG, 100, stream); // bmp is your Bitmap instance
//            // PNG is a lossless format, the compression factor (100) is ignored
//            stream.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
*/
}