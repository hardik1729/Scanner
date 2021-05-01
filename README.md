# Scanner
Scan Document with Android device using camera

Scanner UI:  
<img src="https://user-images.githubusercontent.com/36228523/91321531-58642180-e7dc-11ea-8a21-423fe41ad497.png" width="200"> <img src="https://user-images.githubusercontent.com/36228523/91321543-5c903f00-e7dc-11ea-884d-123e6a8405ce.png" width="200">  <img src="https://user-images.githubusercontent.com/36228523/91321540-5b5f1200-e7dc-11ea-8bc5-2c6d31594bd4.png" width="200"> <img src="https://user-images.githubusercontent.com/36228523/91321537-5a2de500-e7dc-11ea-85ad-4d66df548853.png" width="200">

Clone this repo in OpenCV-android-sdk/samples
Add Scanner in settings.gradle of samples
Add ndk location, for example "ndk.dir=/home/hardik/Android/Sdk/ndk/21.0.6113669" in local.properties of samples

Make following changes in CameraBridgeViewBase in sdk/java/src/org/opencv/android
Edit onEnterStartedState function
	//Scanner : exchanged height and width
        if (!connectCamera(getHeight(), getWidth())) {
Edit deliverAndDrawFrame function
	//Scanner : shifted destination up
                if (mScale != 0) {
                    canvas.drawBitmap(mCacheBitmap, new Rect(0,0,mCacheBitmap.getWidth(), mCacheBitmap.getHeight()),
                         new Rect((int)((canvas.getWidth() - mScale*mCacheBitmap.getWidth()) / 2),
                         (int)(0),
                         (int)((canvas.getWidth() - mScale*mCacheBitmap.getWidth()) / 2 + mScale*mCacheBitmap.getWidth()),
                         (int)( mScale*mCacheBitmap.getHeight())), null);
                } else {
                     canvas.drawBitmap(mCacheBitmap, new Rect(0,0,mCacheBitmap.getWidth(), mCacheBitmap.getHeight()),
                         new Rect((canvas.getWidth() - mCacheBitmap.getWidth()) / 2,
                         0,
                         (canvas.getWidth() - mCacheBitmap.getWidth()) / 2 + mCacheBitmap.getWidth(),
                          mCacheBitmap.getHeight()), null);
                }
Edit AllocateCache function
	//Scanner : exchanged height and width
        mCacheBitmap = Bitmap.createBitmap(mFrameHeight, mFrameWidth, Bitmap.Config.ARGB_8888);