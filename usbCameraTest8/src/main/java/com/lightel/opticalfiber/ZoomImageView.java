package com.lightel.opticalfiber;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class ZoomImageView extends androidx.appcompat.widget.AppCompatImageView implements View.OnTouchListener {

    private Drawable image;
    private int zoomController = 0;
    private int slide = 0;

    public float finger_spacing = 0;
    public int zoom_level = 1;

    enum State {
        ZoomIn,
        Slide
    }

    private State state = State.ZoomIn;

    public ZoomImageView(Context context) {
        super(context);
    }

    public ZoomImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ZoomImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
//        setOnTouchListener(this);
    }

    public void zoomIn(int zoomScale) {
        zoomController += zoomScale;
        state = State.ZoomIn;
        invalidate();
    }

    public void slide(int slideDist) {
        slide += slideDist;
        state = State.Slide;
        invalidate();

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        image = getDrawable();
        if (image != null) {
//            if (state == State.ZoomIn) {
//                //here u can control the width and height of the images........ this line is very important
//                image.setBounds(getLeft() - zoomController,
//                        getTop() - zoomController,
//                        getWidth()+ zoomController,
//                        getHeight() + zoomController);
//                image.draw(canvas);
//            } else {
//                //here u can control the width and height of the images........ this line is very important
//                image.setBounds(getLeft() + slide, getTop(),
//                        getRight() + slide, getBottom());
//                image.draw(canvas);
//            }
        }
    }

    float maxzoom = 200;


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        try {
//            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
//            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId);
//            float maxzoom = (characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM))*10;
//
//            Rect m = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
//            int action = event.getAction();

            float current_finger_spacing;
            Log.d("Andy", "event.getPointerCount() = " + event.getPointerCount());
//            if (event.getPointerCount() > 1) {
//                // Multi touch logic
//                current_finger_spacing = getFingerSpacing(event);
//                if(finger_spacing != 0){
//                    if(current_finger_spacing > finger_spacing && maxzoom > zoom_level){
//                        zoom_level++;
//                    } else if (current_finger_spacing < finger_spacing && zoom_level > 1){
//                        zoom_level--;
//                    }
//                    int minW = (int) (m.width() / maxzoom);
//                    int minH = (int) (m.height() / maxzoom);
//                    int difW = m.width() - minW;
//                    int difH = m.height() - minH;
//                    int cropW = difW /100 *(int)zoom_level;
//                    int cropH = difH /100 *(int)zoom_level;
//                    cropW -= cropW & 3;
//                    cropH -= cropH & 3;
//                    Rect zoom = new Rect(cropW, cropH, m.width() - cropW, m.height() - cropH);
//                    mPreviewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
//                }
//                finger_spacing = current_finger_spacing;
//            } else{
//                if (action == MotionEvent.ACTION_UP) {
//                    //single touch logic
//                }
//            }
//
//            try {
//                mCaptureSession
//                        .setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback, null);
//            } catch (CameraAccessException e) {
//                e.printStackTrace();
//            } catch (NullPointerException ex) {
//                ex.printStackTrace();
//            }
        } catch (Exception e) {
            throw new RuntimeException("can not access camera.", e);
        }
        return true;
    }

    private static float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

//    @Override
//    public boolean onTouch(View v, MotionEvent event) {
//        try {
//            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
//            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId);
//            float maxzoom = (characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM))*10;
//
//            Rect m = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
//            int action = event.getAction();
//            float current_finger_spacing;
//
//            if (event.getPointerCount() > 1) {
//                // Multi touch logic
//                current_finger_spacing = getFingerSpacing(event);
//                if(finger_spacing != 0){
//                    if(current_finger_spacing > finger_spacing && maxzoom > zoom_level){
//                        zoom_level++;
//                    } else if (current_finger_spacing < finger_spacing && zoom_level > 1){
//                        zoom_level--;
//                    }
//                    int minW = (int) (m.width() / maxzoom);
//                    int minH = (int) (m.height() / maxzoom);
//                    int difW = m.width() - minW;
//                    int difH = m.height() - minH;
//                    int cropW = difW /100 *(int)zoom_level;
//                    int cropH = difH /100 *(int)zoom_level;
//                    cropW -= cropW & 3;
//                    cropH -= cropH & 3;
//                    Rect zoom = new Rect(cropW, cropH, m.width() - cropW, m.height() - cropH);
//                    mPreviewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
//                }
//                finger_spacing = current_finger_spacing;
//            } else{
//                if (action == MotionEvent.ACTION_UP) {
//                    //single touch logic
//                }
//            }
//
//            try {
//                mCaptureSession
//                        .setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback, null);
//            } catch (CameraAccessException e) {
//                e.printStackTrace();
//            } catch (NullPointerException ex) {
//                ex.printStackTrace();
//            }
//        } catch (CameraAccessException e) {
//            throw new RuntimeException("can not access camera.", e);
//        }
//        return true;
//    }
}
