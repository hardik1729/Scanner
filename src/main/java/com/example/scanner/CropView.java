package com.example.scanner;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

public class CropView extends View {

    private Paint mRectPaint;
    private int[] mTopLeft = {100,100};
    private int[] mTopRight = {900,100};
    private int[] mBottomLeft = {100,900};
    private int[] mBottomRight = {900,900};
    private int mBottom = 900;
    private int mRight = 900;
    private boolean cTopLeft = false;
    private boolean cTopRight = false;
    private boolean cBottomLeft = false;
    private boolean cBottomRight = false;
    private boolean cTop = false;
    private boolean cRight = false;
    private boolean cBottom = false;
    private boolean cLeft = false;
    private boolean rBottom = false;
    private boolean rRight = false;
    private boolean rCorner = false;
    private TextPaint mTextPaint = null;
    private float xSwipe=0;

    private OnUpCallback mCallback = null;
    protected int imgWidth;
    protected int imgHeight;
    protected int maxHeight;
    protected int maxWidth;
    protected int range;
    protected boolean mDrawCrop=false;
    protected boolean mDrawResize=false;

    public interface OnUpCallback {
        void onQuadFinished(int[] TopLeft,int[] TopRight,int[] BottomLeft, int[] BottomRight);
        void onRectFinished(int Bottom,int Right);
        void onSwipeFinished(float xSwipe);
    }

    public CropView(final Context context) {
        super(context);
        init();
    }

    public CropView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CropView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * Sets callback for up
     *
     * @param callback {@link OnUpCallback}
     */
    public void setOnUpCallback(OnUpCallback callback) {
        mCallback = callback;
        mCallback.onQuadFinished(mTopLeft, mTopRight, mBottomLeft, mBottomRight);
        mCallback.onRectFinished(mBottom,mRight);
        mCallback.onSwipeFinished(xSwipe);
    }

    /**
     * Inits internal data
     */
    private void init() {
        mRectPaint = new Paint();
        mRectPaint.setColor(getContext().getResources().getColor(android.R.color.holo_green_light));
        mRectPaint.setStyle(Paint.Style.STROKE);
        mRectPaint.setStrokeWidth(5); // TODO: should take from resources

        mTextPaint = new TextPaint();
        mTextPaint.setColor(getContext().getResources().getColor(android.R.color.holo_green_light));
        mTextPaint.setTextSize(40);
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        // TODO: be aware of multi-touches
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                int x = (int) event.getX();
                int y = (int) event.getY();
                if (mDrawCrop && x>0 && y>0 && x < imgWidth && y < imgHeight) {
                    cTopLeft = false;
                    cTopRight = false;
                    cBottomLeft = false;
                    cBottomRight = false;
                    cTop=false;
                    cRight=false;
                    cBottom=false;
                    cLeft=false;
                    cTopLeft = mTopLeft[0] > x - range && mTopLeft[0] < x + range && mTopLeft[1] > y - range && mTopLeft[1] < y + range;
                    cTopRight = mTopRight[0] > x - range && mTopRight[0] < x + range && mTopRight[1] > y - range && mTopRight[1] < y + range;
                    cBottomLeft = mBottomLeft[0] > x - range && mBottomLeft[0] < x + range && mBottomLeft[1] > y - range && mBottomLeft[1] < y + range;
                    cBottomRight = mBottomRight[0] > x - range && mBottomRight[0] < x + range && mBottomRight[1] > y - range && mBottomRight[1] < y + range;
                    cTop= distance(mTopLeft,mTopRight,x,y,1) && mTopLeft[0]<x-range && mTopRight[0]>x+range;
                    cRight=distance(mTopRight,mBottomRight,x,y,1) && mTopRight[1]<y-range && mBottomRight[1]>y+range;
                    cBottom=distance(mBottomLeft,mBottomRight,x,y,1) && mBottomLeft[0]<x-range && mBottomRight[0]>x+range;
                    cLeft=distance(mBottomLeft,mTopLeft,x,y,1) && mTopLeft[1]<y-range && mBottomLeft[1]>y+range;
                }
                else if(mDrawResize && x>0 && y>0 && x<maxWidth && y<maxHeight){
                    rBottom=false;
                    rRight=false;
                    rCorner=false;
                    rBottom = mBottom>y-2*range && mBottom<y+2*range && mRight>x+2*range;
                    rRight = mRight>x-2*range && mRight<x+2*range && mBottom>y+2*range;
                    rCorner = mRight > x - 2*range && mRight <x+2*range && mBottom > y - 2*range && mBottom<y+2*range;
                }
                else {
                    xSwipe=x;
                }
                invalidate();
                break;

            case MotionEvent.ACTION_MOVE:
                final int tempX = (int) event.getX();
                final int tempY = (int) event.getY();
                if(mDrawCrop && tempX<imgWidth && tempY<imgHeight && tempX>0 && tempY>0){
                    if (cTopLeft && distance(mTopRight, mBottomLeft, tempX, tempY,0)) {
                        if(tempX<mBottomLeft[0]+range/5 && tempX>mBottomLeft[0]-range/5
                                && tempY<mTopRight[1]+range/5 && tempY>mTopRight[1]-range/5){
                            mTopLeft[0]=mBottomLeft[0];
                            mTopLeft[1]=mTopRight[1];
                        }else if(tempY<mTopRight[1]+range/5 && tempY>mTopRight[1]-range/5){
                            mTopLeft[0]=tempX;
                            mTopLeft[1]=mTopRight[1];
                        }else if(tempX<mBottomLeft[0]+range/5 && tempX>mBottomLeft[0]-range/5){
                            mTopLeft[0]=mBottomLeft[0];
                            mTopLeft[1]=tempY;
                        }else{
                            mTopLeft[0] = tempX;
                            mTopLeft[1] = tempY;
                        }
                        if(slopeMatch(mTopRight,mBottomRight,new int[]{tempX,tempY},mBottomLeft)){
                            mTopLeft[0]=mBottomLeft[0]+(mTopRight[0]-mBottomRight[0])*(tempY-mBottomLeft[1])/(mTopRight[1]-mBottomRight[1]);
                        }
                        if(slopeMatch(mBottomLeft,mBottomRight,new int[]{tempX,tempY},mTopRight)){
                            mTopLeft[1]=mTopRight[1]+(mBottomLeft[1]-mBottomRight[1])*(tempX-mTopRight[0])/(mBottomLeft[0]-mBottomRight[0]);
                        }
                    }
                    else if (cTopRight && distance(mBottomRight, mTopLeft, tempX, tempY,0)) {
                        if(tempX<mBottomRight[0]+range/5 && tempX>mBottomRight[0]-range/5
                                && tempY<mTopLeft[1]+range/5 && tempY>mTopLeft[1]-range/5){
                            mTopRight[0]=mBottomRight[0];
                            mTopRight[1]=mTopLeft[1];
                        }else if(tempX<mBottomRight[0]+range/5 && tempX>mBottomRight[0]-range/5){
                            mTopRight[0]=mBottomRight[0];
                            mTopRight[1]=tempY;
                        }else if(tempY<mTopLeft[1]+range/5 && tempY>mTopLeft[1]-range/5){
                            mTopRight[0]=tempX;
                            mTopRight[1]=mTopLeft[1];
                        }else{
                            mTopRight[0] = tempX;
                            mTopRight[1] = tempY;
                        }
                        if(slopeMatch(mTopLeft,mBottomLeft,new int[]{tempX,tempY},mBottomRight)){
                            mTopRight[0]=mBottomRight[0]+(mTopLeft[0]-mBottomLeft[0])*(tempY-mBottomRight[1])/(mTopLeft[1]-mBottomLeft[1]);
                        }
                        if(slopeMatch(mBottomRight,mBottomLeft,new int[]{tempX,tempY},mTopLeft)){
                            mTopRight[1]=mTopLeft[1]+(mBottomRight[1]-mBottomLeft[1])*(tempX-mTopLeft[0])/(mBottomRight[0]-mBottomLeft[0]);
                        }
                    }
                    else if (cBottomLeft && distance(mTopLeft, mBottomRight, tempX, tempY,0)) {
                        if(tempX<mTopLeft[0]+range/5 && tempX>mTopLeft[0]-range/5
                                && tempY<mBottomRight[1]+range/5 && tempY>mBottomRight[1]-range/5){
                            mBottomLeft[0]=mTopLeft[0];
                            mBottomLeft[1]=mBottomRight[1];
                        }else if(tempX<mTopLeft[0]+range/5 && tempX>mTopLeft[0]-range/5){
                            mBottomLeft[0]=mTopLeft[0];
                            mBottomLeft[1]=tempY;
                        }else if(tempY<mBottomRight[1]+range/5 && tempY>mBottomRight[1]-range/5){
                            mBottomLeft[0]=tempX;
                            mBottomLeft[1]=mBottomRight[1];
                        }else{
                            mBottomLeft[0] = tempX;
                            mBottomLeft[1] = tempY;
                        }
                        if(slopeMatch(mBottomRight,mTopRight,new int[]{tempX,tempY},mTopLeft)){
                            mBottomLeft[0]=mTopLeft[0]+(mBottomRight[0]-mTopRight[0])*(tempY-mTopLeft[1])/(mBottomRight[1]-mTopRight[1]);
                        }
                        if(slopeMatch(mTopLeft,mTopRight,new int[]{tempX,tempY},mBottomRight)){
                            mBottomLeft[1]=mBottomRight[1]+(mTopLeft[1]-mTopRight[1])*(tempX-mBottomRight[0])/(mTopLeft[0]-mTopRight[0]);
                        }
                    }
                    else if (cBottomRight && distance(mBottomLeft, mTopRight, tempX, tempY,0)) {
                        if(tempX<mTopRight[0]+range/5 && tempX>mTopRight[0]-range/5
                                && tempY<mBottomLeft[1]+range/5 && tempY>mBottomLeft[1]-range/5){
                            mBottomRight[0]=mTopRight[0];
                            mBottomRight[1]=mBottomLeft[1];
                        }else if(tempX<mTopRight[0]+range/5 && tempX>mTopRight[0]-range/5){
                            mBottomRight[0]=mTopRight[0];
                            mBottomRight[1]=tempY;
                        }else if(tempY<mBottomLeft[1]+range/5 && tempY>mBottomLeft[1]-range/5){
                            mBottomRight[0]=tempX;
                            mBottomRight[1]=mBottomLeft[1];
                        }else {
                            mBottomRight[0] = tempX;
                            mBottomRight[1] = tempY;
                        }
                        if(slopeMatch(mBottomLeft,mTopLeft,new int[]{tempX,tempY},mTopRight)){
                            mBottomRight[0]=mTopRight[0]+(mBottomLeft[0]-mTopLeft[0])*(tempY-mTopRight[1])/(mBottomLeft[1]-mTopLeft[1]);
                        }
                        if(slopeMatch(mTopRight,mTopLeft,new int[]{tempX,tempY},mBottomLeft)){
                            mBottomRight[1]=mBottomLeft[1]+(mTopRight[1]-mTopLeft[1])*(tempX-mBottomLeft[0])/(mTopRight[0]-mTopLeft[0]);
                        }
                    }
                    else if(cTop && distance(mBottomRight, mBottomLeft, tempX, tempY,2)){
                        mTopLeft[0]=mBottomLeft[0]+(mTopLeft[0]-mBottomLeft[0])*(tempY-mBottomLeft[1])/(mTopLeft[1]-mBottomLeft[1]);
                        mTopLeft[1]=tempY;
                        mTopRight[0]=mBottomRight[0]+(mTopRight[0]-mBottomRight[0])*(tempY-mBottomRight[1])/(mTopRight[1]-mBottomRight[1]);
                        mTopRight[1]=tempY;
                    }else if(cRight && distance(mBottomLeft,mTopLeft,tempX,tempY,2)){
                        mTopRight[1]=mTopLeft[1]+(mTopRight[1]-mTopLeft[1])*(tempX-mTopLeft[0])/(mTopRight[0]-mTopLeft[0]);
                        mTopRight[0]=tempX;
                        mBottomRight[1]=mBottomLeft[1]+(mBottomRight[1]-mBottomLeft[1])*(tempX-mBottomLeft[0])/(mBottomRight[0]-mBottomLeft[0]);
                        mBottomRight[0]=tempX;
                    }else if(cBottom && distance(mTopLeft, mTopRight, tempX, tempY,2)){
                        mBottomLeft[0]=mTopLeft[0]+(mBottomLeft[0]-mTopLeft[0])*(tempY-mTopLeft[1])/(mBottomLeft[1]-mTopLeft[1]);
                        mBottomLeft[1]=tempY;
                        mBottomRight[0]=mTopRight[0]+(mBottomRight[0]-mTopRight[0])*(tempY-mTopRight[1])/(mBottomRight[1]-mTopRight[1]);
                        mBottomRight[1]=tempY;
                    }else if(cLeft && distance(mTopRight,mBottomRight,tempX,tempY,2)){
                        mTopLeft[1]=mTopRight[1]+(mTopLeft[1]-mTopRight[1])*(tempX-mTopRight[0])/(mTopLeft[0]-mTopRight[0]);
                        mTopLeft[0]=tempX;
                        mBottomLeft[1]=mBottomRight[1]+(mBottomLeft[1]-mBottomRight[1])*(tempX-mBottomRight[0])/(mBottomLeft[0]-mBottomRight[0]);
                        mBottomLeft[0]=tempX;
                    }
                }
                else if(mDrawResize && tempX>0 && tempY>0 && tempX<maxWidth && tempY<maxHeight){
                    if(rBottom && tempY>4*range){
                        if(tempY*9/16.0<mRight+range/4 && tempY*9/16.0>mRight-range/4)
                            mBottom=mRight*16/9;
                        else if(tempY*3/4.0<mRight+range/4 && tempY*3/4.0>mRight-range/4)
                            mBottom=mRight*4/3;
                        else if(tempY*2/3.0<mRight+range/4 && tempY*2/3.0>mRight-range/4)
                            mBottom=mRight*3/2;
                        else
                            mBottom = tempY;
                    }else if(rRight && tempX>4*range){
                        if(tempX*16/9.0<mBottom+range/2 && tempX*16/9.0>mBottom-range/2)
                            mRight=mBottom*9/16;
                        else if(tempX*4/3.0<mBottom+range/2 && tempX*4/3.0>mBottom-range/2)
                            mRight=mBottom*3/4;
                        else if(tempX*3/2.0<mBottom+range/2 && tempX*3/2.0>mBottom-range/2)
                            mRight=mBottom*2/3;
                        else
                            mRight = tempX;
                    }else if(rCorner && tempX>4*range && tempX>4*range){
                        if(tempY>=tempX){
                            if((mRight*tempY)/mBottom<maxWidth) {
                                mRight = (mRight * tempY) / mBottom;
                                mBottom = tempY;
                            }
                        }else{
                            if((mBottom * tempX) / mRight<maxHeight) {
                                mBottom = (mBottom * tempX) / mRight;
                                mRight = tempX;
                            }
                        }
                    }
                    mCallback.onRectFinished(mBottom,mRight);
                }
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
                if (mCallback != null) {
                    mCallback.onQuadFinished(mTopLeft, mTopRight, mBottomLeft, mBottomRight);
                    mCallback.onRectFinished(mBottom, mRight);
                    if (!mDrawCrop && !mDrawResize) {
                        xSwipe = xSwipe - event.getX();
                        mCallback.onSwipeFinished(xSwipe);
                    }
                }
                invalidate();
                break;
            default:
                break;
        }
        return true;
    }

    private boolean distance(int[] p1,int[] p2, int x, int y,int flag){
        int diffX=(p2[0]-p1[0]);
        int diffY=(p2[1]-p1[1]);
        if(flag==0)
            return (-diffY*x+diffX*y+p1[0]*diffY-p1[1]*diffX)/sqrt(diffX*diffX+diffY*diffY)>3*range;
        else if(flag==1)
            return (-diffY*x+diffX*y+p1[0]*diffY-p1[1]*diffX)/sqrt(diffX*diffX+diffY*diffY)<range;
        else if(flag==2)
            return (-diffY*x+diffX*y+p1[0]*diffY-p1[1]*diffX)/sqrt(diffX*diffX+diffY*diffY)>4*range;
        return false;
    }

    private boolean slopeMatch(int[] p11,int[] p12,int[] p21,int[] p22){
        return abs((p12[1]-p11[1])*(p22[0]-p21[0])-(p22[1]-p21[1])*(p12[0]-p11[0]))<4*range*range;
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        int radius=10;
        if(mDrawCrop) {
            canvas.drawCircle(mTopLeft[0], mTopLeft[1], radius, mRectPaint);
            canvas.drawCircle(mTopRight[0], mTopRight[1], radius, mRectPaint);
            canvas.drawCircle(mBottomLeft[0], mBottomLeft[1], radius, mRectPaint);
            canvas.drawCircle(mBottomRight[0], mBottomRight[1], radius, mRectPaint);
            canvas.drawLine(mTopLeft[0], mTopLeft[1], mTopRight[0], mTopRight[1], mRectPaint);
            canvas.drawLine(mTopRight[0], mTopRight[1], mBottomRight[0], mBottomRight[1], mRectPaint);
            canvas.drawLine(mBottomRight[0], mBottomRight[1], mBottomLeft[0], mBottomLeft[1], mRectPaint);
            canvas.drawLine(mBottomLeft[0], mBottomLeft[1], mTopLeft[0], mTopLeft[1], mRectPaint);
        }
        else{
            mTopLeft[0]=range;
            mTopLeft[1]=range;
            mTopRight[0]=imgWidth-range;
            mTopRight[1]=range;
            mBottomLeft[0]=range;
            mBottomLeft[1]=imgHeight-range;
            mBottomRight[0]=imgWidth-range;
            mBottomRight[1]=imgHeight-range;
            mCallback.onQuadFinished(mTopLeft, mTopRight, mBottomLeft, mBottomRight);
        }
        if(mDrawResize){
            canvas.drawLine(mRight,0,mRight,mBottom,mRectPaint);
            canvas.drawLine(0,mBottom,mRight,mBottom,mRectPaint);
            if(abs(mBottom*9-mRight*16)<50)
                canvas.drawText("16:9",maxWidth/2-40,maxHeight+40,mTextPaint);
            if(abs(mBottom*3-mRight*4)<50)
                canvas.drawText("4:3",maxWidth/2-30,maxHeight+40,mTextPaint);
            if(abs(mBottom*2-mRight*3)<50)
                canvas.drawText("3:2",maxWidth/2-30,maxHeight+40,mTextPaint);
            for(int i=0;i<radius;i++){
                canvas.drawRect(mRight-i,mBottom-2*range,mRight,mBottom,mRectPaint);
                canvas.drawRect(mRight-2*range,mBottom-i,mRight,mBottom,mRectPaint);
            }
        }
        else {
            mBottom=imgHeight;
            mRight=imgWidth;
            mCallback.onRectFinished(mBottom,mRight);
        }
    }
}
