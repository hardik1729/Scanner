package com.example.scanner;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.Manifest;
import android.content.ClipData;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.DragEvent;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.opencv.core.Core.flip;
import static org.opencv.core.Core.rotate;

public class CameraPreview extends CameraActivity implements CvCameraViewListener2 {

    private static final String TAG = "OCVSample::Activity";
    private static final String path=Environment.getExternalStorageDirectory()+"/Scanner/";
    private static final String dstFolder="pic/img/";
    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean              mIsJavaCamera = true;
    private MenuItem             mItemSwitchCamera = null;
    private FloatingActionButton crossButton;
    private ImageView imageView;
    private boolean loading=false;

    private Mat mFrame;
    private int maxWidth;
    private int maxHeight;
    private int range=100;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public CameraPreview() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.preview_camera);

        mOpenCvCameraView = findViewById(R.id.CameraPreview);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCameraIndex(0);

        mOpenCvCameraView.setCvCameraViewListener(this);

        crossButton=findViewById(R.id.cross);
        ((ViewGroup)crossButton.getParent()).removeView(crossButton);
        imageView=findViewById(R.id.image_view);
        ((ViewGroup)imageView.getParent()).removeView(imageView);

        createFolder();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        maxHeight=width;
        maxWidth=height;
        mFrame=new Mat();
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat         frame       = inputFrame.rgba();
        rotate(frame, frame, Core.ROTATE_90_CLOCKWISE);
        if(!loading)
            frame.copyTo(mFrame);
        return frame;
    }

    public void createFolder(){
        ActivityCompat.requestPermissions(CameraPreview.this,
                new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE},
                1);
        File folder = new File(path);
        folder.mkdir();
        File folder_pic = new File(path,"pic/");
        folder_pic.mkdir();
        File folder_img = new File(path,dstFolder);
        folder_img.mkdir();
        String[]entries = folder_img.list();
        for(String s: entries){
            File currentFile = new File(folder_img.getPath(),s);
            currentFile.delete();
        }
    }

    private List<Integer> imgName=new ArrayList<>();
    private int index=0;
    private  ImageButton.OnDragListener drag=new View.OnDragListener() {
        @Override
        public boolean onDrag(View v, DragEvent event) {
//            Toast.makeText(getApplicationContext(),"hello"+v.getId()+"x"+imgName.size(),Toast.LENGTH_SHORT).show();
//            Log.d("hola","hola");
            final int action = event.getAction();
            switch (action){
                case DragEvent.ACTION_DRAG_LOCATION:
                    Log.d("hola",v.getId()+"x"+event.getX()+"x"+event.getY()+"loc");
                    break;
                case DragEvent.ACTION_DROP:
//                    Log.d("hola",v.getId()+"x"+event.getX()+"x"+event.getY());
                    break;
                default:
                    break;
            }

            return true;
        }
    };
    private ImageButton.OnLongClickListener longClick=new View.OnLongClickListener(){
        @Override
        public boolean onLongClick(View v){
            View.DragShadowBuilder myShadow=new View.DragShadowBuilder(v);
            v.startDrag(null,myShadow,null,0);
            return true;
        }
    };
    private ImageButton.OnClickListener l=new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final File file = new File(path + dstFolder + v.getId() + ".png");
            Bitmap tempBmp = BitmapFactory.decodeFile(file.getAbsolutePath());
            ViewGroup parent=(ViewGroup) ( (ViewGroup) ( (ViewGroup)(v.getParent()) ).getParent() ).getParent();
            findViewById(R.id.fab_camera).setClickable(false);
            for(int i=0;i<imgName.size();i++){
                if(imgName.get(i)!=v.getId()){
                    findViewById(imgName.get(i)).setAlpha((float)0.25);
                }else {
                    findViewById(imgName.get(i)).setAlpha((float)1.0);
                }
            }
            if(findViewById(R.id.image_view)==null)
                parent.addView(imageView);
            if(findViewById(R.id.cross)==null)
                parent.addView(crossButton);
            imageView.setImageBitmap(tempBmp);
        }
    };

    public void onCapture(View view){
        try {
            File file = new File(path + dstFolder + index + ".png");
            FileOutputStream stream = new FileOutputStream(file);
            Bitmap bmp=Bitmap.createBitmap(mFrame.cols(),mFrame.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mFrame,bmp);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, stream); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
            stream.close();

            Mat mSmallFrame = new Mat();
            Bitmap mSmallBmp;
            if(mFrame.cols()>mFrame.rows()){
                Core.copyMakeBorder(mFrame,mSmallFrame,(mFrame.cols()-mFrame.rows())/2,(mFrame.cols()-mFrame.rows())/2,0,0, Core.BORDER_CONSTANT,new Scalar(0));
            }else{
                Core.copyMakeBorder(mFrame,mSmallFrame,0,0,(mFrame.rows()-mFrame.cols())/2,(mFrame.rows()-mFrame.cols())/2, Core.BORDER_CONSTANT,new Scalar(0));
            }
            Imgproc.resize(mSmallFrame,mSmallFrame,new Size(150,150));
            mSmallBmp=Bitmap.createBitmap(mSmallFrame.cols(),mSmallFrame.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mSmallFrame,mSmallBmp);
            imgName.add(index);
            ImageButton imgButton=new ImageButton(getApplicationContext());
            imgButton.setImageBitmap(mSmallBmp);
            imgButton.setId(index);
            imgButton.setOnClickListener(l);
            imgButton.setOnLongClickListener(longClick);
            imgButton.setOnDragListener(drag);
            if(loading && findViewById(R.id.cross)!=null){
                imgButton.setAlpha((float)0.25);
            }

            LinearLayout imageIconLayout=findViewById(R.id.image_icon_layout);
            imageIconLayout.addView(imgButton);
            imgButton=findViewById(R.id.add_image);
            imageIconLayout.removeView(imgButton);
            imageIconLayout.addView(imgButton);

            final HorizontalScrollView imageIcon=findViewById(R.id.image_icon);
            imageIcon.post(new Runnable() {
                @Override
                public void run() {
                    imageIcon.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
                }
            });
            index++;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed()
    {
        // Example of logic
        if ( findViewById(R.id.cross)!=null ) {
            // ... do your logic ...
            ViewGroup parent = (ViewGroup)findViewById(R.id.cross).getParent();
            findViewById(R.id.fab_camera).setClickable(true);
            parent.removeView(findViewById(R.id.image_view));
            parent.removeView(findViewById(R.id.cross));
            for (int i=0;i<imgName.size();i++){
                findViewById(imgName.get(i)).setAlpha((float)1.0);
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==1 && resultCode==RESULT_OK){
            loading=true;
            ClipData clipData=data.getClipData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };
            for(int i=0;i<clipData.getItemCount();i++){
                Cursor cursor = getContentResolver().query(clipData.getItemAt(i).getUri(), filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String filePath = cursor.getString(columnIndex);
                File file=new File(filePath);
                Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
                if(bmp.getWidth()<4*range || bmp.getHeight()<4*range){
                    Toast.makeText(getApplicationContext(), "Resized too small image",
                            Toast.LENGTH_SHORT).show();
                    Mat img=new Mat();
                    Utils.bitmapToMat(bmp,img);
                    if(bmp.getWidth()>=bmp.getHeight()){
                        Imgproc.resize(img,img,new Size((4*range*bmp.getWidth())/bmp.getHeight(),4*range));
                    }else{
                        Imgproc.resize(img,img,new Size(4*range,(4*range*bmp.getHeight())/bmp.getWidth()));
                    }
                    Utils.matToBitmap(img,bmp);
                }else if(bmp.getWidth()>maxWidth || bmp.getHeight()>maxHeight){
                    Toast.makeText(getApplicationContext(), "Resized too big image",
                            Toast.LENGTH_SHORT).show();
                    Mat img=new Mat();
                    Utils.bitmapToMat(bmp,img);
                    if(bmp.getWidth()>=bmp.getHeight()){
                        Imgproc.resize(img,img,new Size(maxWidth,(maxWidth*bmp.getHeight())/bmp.getWidth()));
                    }else{
                        Imgproc.resize(img,img,new Size((maxHeight*bmp.getWidth())/bmp.getHeight(),maxHeight));
                    }
                    bmp=Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(img,bmp);
                }
                Utils.bitmapToMat(bmp,mFrame);
                this.onCapture(findViewById(R.id.fab_camera));
            }
            loading=false;
        }
    }

    public void onAddImage(View view){
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, 1);
    }

    public void onCross(View view){
        for (int i=0;i<imgName.size();i++){
            if(findViewById(imgName.get(i)).getAlpha()==1.0){
                ((ViewGroup)findViewById(R.id.image_icon_layout)).removeView(findViewById(imgName.get(i)));
                File file = new File(path + dstFolder + imgName.get(i) + ".png");
                file.delete();
                imgName.remove(i);
                if(imgName.size()!=0){
                    if(i==imgName.size()) {
                        i=i-1;
                    }
                    findViewById(imgName.get(i)).setAlpha((float)1.0);
                    file = new File(path + dstFolder + imgName.get(i) + ".png");
                    Bitmap tempBmp = BitmapFactory.decodeFile(file.getAbsolutePath());
                    imageView.setImageBitmap(tempBmp);
                }else {
                    this.onBackPressed();
                }
                break;
            }
        }
    }

    public void onNext(View view){
        if(((LinearLayout)findViewById(R.id.image_icon_layout)).getChildCount()>1){
            Intent intent = new Intent(this,EditActivity.class);
            startActivity(intent);
        }
    }

}