/*
 *  UVCCamera
 *  library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *  All files in the folder are under this Apache License, Version 2.0.
 *  Files in the libjpeg-turbo, libusb, libuvc, rapidjson folder
 *  may have a different license, see the respective files.
 */

package com.lightel.opticalfiber;

import android.animation.Animator;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.lightel.opticalfiber.ProbeManager.Probe;
import com.serenegiant.common.BaseActivity;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usbcameracommon.UVCCameraHandler;
import com.serenegiant.utils.ViewAnimationHelper;
import com.serenegiant.widget.CameraViewInterface;
import com.serenegiant.widget.UVCCameraTextureView;

import static com.lightel.opticalfiber.MainActivity.PROBE_TYPE_UVC_CAMERA;
import static com.lightel.opticalfiber.SharedPreferenceHelper.PREF_KEY_BRIGHTNESS;
import static com.lightel.opticalfiber.SharedPreferenceHelper.PREF_KEY_CONTRAST;

public final class UVCCameraActivity extends BaseActivity implements CameraDialog.CameraDialogParent,
        View.OnClickListener {
    private static final boolean DEBUG = true;
    private static final String TAG = "MainActivity";

    /**
     * set true if you want to record movie using MediaSurfaceEncoder
     * (writing frame data into Surface camera from MediaCodec
     * by almost same way as USBCameratest2)
     * set false if you want to record movie using MediaVideoEncoder
     */
    private static final boolean USE_SURFACE_ENCODER = false;

    /**
     * preview resolution(width)
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     */
    private static final int PREVIEW_WIDTH = 1280;
    /**
     * preview resolution(height)
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     */
    private static final int PREVIEW_HEIGHT = 960;
    /**
     * preview mode
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     * 0:YUYV, other:MJPEG
     */
    private static final int PREVIEW_MODE = 1;

    protected static final int SETTINGS_HIDE_DELAY_MS = 2500;

    /**
     * for accessing USB
     */
    private USBMonitor mUSBMonitor;
    /**
     * Handler to execute camera related methods sequentially on private thread
     */
    private UVCCameraHandler mCameraHandler;
    /**
     * for camera preview display
     */
    private CameraViewInterface mUVCCameraView;
    /**
     * for open&start / stop&close camera preview
     */
    private ToggleButton mCameraButton;
    /**
     * button for start/stop recording
     */
    private ImageButton mCaptureButton;
    private View mBtnBack, mBrightnessButton, mContrastButton, mBtnSettings, mBtnSave;
    private View mResetButton;
    private View mToolsLayout, mValueLayout;
    private SeekBar mSettingSeekbar;
    private RadioGroup mCaptureTypeRadioGroup;
    private ImageView mFrameImage;

    String[] fiberType = {"SM", "MM", "MPO"};

    Probe mProbe;
    int mContrast;
    int mBrightness;

    public final static int DEFAULT_CONTRAST_D1000 = 0;
    public final static int DEFAULT_BRIGHTNESS_D1000 = 100;
    public final static int DEFAULT_CONTRAST_D2000 = 50;
    public final static int DEFAULT_BRIGHTNESS_D2000 = 50;

    UsbDevice mUsbDevice;

    Probe DI1000 = ProbeManager.getInstance().DI1000;
    Probe DI1000L = ProbeManager.getInstance().DI1000L;
    Probe DI2000 = ProbeManager.getInstance().DI2000;
    Probe DI3000 = ProbeManager.getInstance().DI3000;
    Probe DI5000 = ProbeManager.getInstance().DI5000;

    enum CaptureType {
        Image,Video
    }

    CaptureType mCaptureType = CaptureType.Image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uvccamera);

        mCameraButton = findViewById(R.id.camera_button);
        mCaptureButton = findViewById(R.id.capture_button);
        mBtnBack = findViewById(R.id.btnBack);
        mBrightnessButton = findViewById(R.id.btnBrightness);
        mContrastButton = findViewById(R.id.btnContrast);
        mResetButton = findViewById(R.id.reset_button);
        mToolsLayout = findViewById(R.id.tools_layout);
        mValueLayout = findViewById(R.id.value_layout);
        mSettingSeekbar = findViewById(R.id.setting_seekbar);
        mUVCCameraView = findViewById(R.id.camera_view);
        mBtnSettings = findViewById(R.id.btnSettings);
//        mBtnSave = findViewById(R.id.btnSaveImageAndReport);
//        mSpinnerFiberType = findViewById(R.id.spinnerFiberType);
        mCaptureTypeRadioGroup = findViewById(R.id.captureType);

        mBtnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        mCaptureButton.setOnClickListener(this);
        mBrightnessButton.setOnClickListener(this);
        mContrastButton.setOnClickListener(this);
        mResetButton.setOnClickListener(this);
        mBtnSettings.setOnClickListener(this);
//        mBtnSave.setOnClickListener(this);

        mCameraButton.setOnCheckedChangeListener(mOnCheckedChangeListener);
        ((UVCCameraTextureView) mUVCCameraView).setOnLongClickListener(mOnLongClickListener);
        mSettingSeekbar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
        mCaptureTypeRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int id) {
                switch (id) {
                    case R.id.capture_image:
                        mCaptureType = CaptureType.Image;
                        break;
                    case R.id.capture_video:
                        mCaptureType = CaptureType.Video;
                        break;
                }
            }
        });
        mFrameImage = findViewById(R.id.frame_image);


        mUVCCameraView.setAspectRatio(PREVIEW_WIDTH / (float) PREVIEW_HEIGHT);
        mCaptureButton.setVisibility(View.INVISIBLE);
//        mToolsLayout.setVisibility(View.INVISIBLE);
        mValueLayout.setVisibility(View.INVISIBLE);

        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
        mCameraHandler = UVCCameraHandler.createHandler(this, mUVCCameraView,
                USE_SURFACE_ENCODER ? 0 : 1, PREVIEW_WIDTH, PREVIEW_HEIGHT, PREVIEW_MODE);

        setCurrentProbe();
        mValueLayout.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                Log.d("Andy", "keyEvent = " + keyEvent);

                return false;
            }
        });

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                mCameraButton.performClick();
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d("Andy", "keyCode = " + keyCode);
        Log.d("Andy", "event = " + event);
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.d("Andy", "event = " + event);
        return super.dispatchKeyEvent(event);
    }

    void setCaptureStillImage() {
        Log.d("Andy", "setCaptureStillImage");
        mFrameImage.setVisibility(View.VISIBLE);
        ((UVCCameraTextureView) mUVCCameraView).setVisibility(View.GONE);
        mFrameImage.setImageBitmap(((UVCCameraTextureView) mUVCCameraView).getBitmap());

        AlertDialog.Builder builder = new AlertDialog.Builder(UVCCameraActivity.this);
        builder.setMessage("Save Image?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mFrameImage.setVisibility(View.GONE);
                        ((UVCCameraTextureView) mUVCCameraView).setVisibility(View.VISIBLE);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .create()
                .show();

    }

    void setCurrentProbe() {
        if (getIntent() != null) {
            String probeType = getIntent().getStringExtra(PROBE_TYPE_UVC_CAMERA);
            if (probeType.equals(DI1000.probeName)) {
                mProbe = DI1000;
            } else if (probeType.equals(DI1000L.probeName)) {
                mProbe = DI1000L;
            } else {
                mProbe = DI2000;
            }
        }

        mContrast = SharedPreferenceHelper.getData(this, mProbe.probeName, PREF_KEY_CONTRAST);
        mBrightness = SharedPreferenceHelper.getData(this, mProbe.probeName, PREF_KEY_BRIGHTNESS);

        if (mContrast == -1) {
            if (mProbe == DI2000) {
                mContrast = DEFAULT_CONTRAST_D2000;
            } else {
                mContrast = DEFAULT_CONTRAST_D1000;
            }
        }

        if (mBrightness == -1) {
            if (mProbe == DI2000) {
                mBrightness = DEFAULT_BRIGHTNESS_D2000;
            } else {
                mBrightness = DEFAULT_BRIGHTNESS_D1000;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (DEBUG) Log.v(TAG, "onStart:");
        mUSBMonitor.register();
        if (mUVCCameraView != null)
            mUVCCameraView.onResume();
    }

    @Override
    protected void onStop() {
        if (DEBUG) Log.v(TAG, "onStop:");
        mCameraHandler.close();
        if (mUVCCameraView != null)
            mUVCCameraView.onPause();
        setCameraButton(false);
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.v(TAG, "onDestroy:");
        if (mCameraHandler != null) {
            mCameraHandler.release();
            mCameraHandler = null;
        }
        if (mUSBMonitor != null) {
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }
        mUVCCameraView = null;
        mCameraButton = null;
        mCaptureButton = null;
        super.onDestroy();
    }

    /**
     * event handler when click camera / capture button
     */
    @Override
    public void onClick(final View view) {
        switch (view.getId()) {
            case R.id.capture_button:
                if (mCaptureType == CaptureType.Image) {
                    captureImage();
                } else {
                    captureVideo();
                }
                break;
            case R.id.btnBrightness:
                showSettings(UVCCamera.PU_BRIGHTNESS);
                break;
            case R.id.btnContrast:
                showSettings(UVCCamera.PU_CONTRAST);
                break;
            case R.id.reset_button:
                resetSettings();
                break;
            case R.id.btnSettings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
//            case R.id.btnSavd
        }
    }

    void captureImage() {
        if (mCameraHandler.isOpened()) {
            if (checkPermissionWriteExternalStorage()) {
                mCameraHandler.captureStill();
                setCaptureStillImage();
            }
        }
    }

    void captureVideo() {
        if (mCameraHandler.isOpened()) {
            if (checkPermissionWriteExternalStorage() && checkPermissionAudio()) {
                if (!mCameraHandler.isRecording()) {
                    mCaptureButton.setColorFilter(0xffff0000);    // turn red
                    mCameraHandler.startRecording();
                } else {
                    mCaptureButton.setColorFilter(0);    // return to default color
                    mCameraHandler.stopRecording();
                }
            }
        }
    }

    private final CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener
            = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(final CompoundButton compoundButton, final boolean isChecked) {
            switch (compoundButton.getId()) {
                case R.id.camera_button:
                    compoundButton.setText(isChecked ? "On" :"Off");

                    if (isChecked && !mCameraHandler.isOpened()) {
                        CameraDialog.showDialog(UVCCameraActivity.this);
                    } else {

                        mCameraHandler.close();
                        setCameraButton(false);
                    }
                    break;
            }
        }
    };

    /**
     * capture still image when you long click on preview image(not on buttons)
     */
    private final View.OnLongClickListener mOnLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(final View view) {
            Log.d("Andy", "onLongClick");
//            switch (view.getId()) {
//                case R.id.camera_view:
//
//                    if (mCameraHandler.isOpened()) {
//                        if (checkPermissionWriteExternalStorage()) {
//                            mCameraHandler.captureStill();
//                        }
//                        return true;
//                    }
//            }
            return false;
        }
    };

    private void setCameraButton(final boolean isOn) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mCameraButton != null) {
                    try {
                        mCameraButton.setOnCheckedChangeListener(null);
                        mCameraButton.setChecked(isOn);
                    } finally {
                        mCameraButton.setOnCheckedChangeListener(mOnCheckedChangeListener);
                    }
                }
                if (!isOn && (mCaptureButton != null)) {
                    mCaptureButton.setVisibility(View.INVISIBLE);
                }
            }
        }, 0);
        updateItems();
    }

    private void startPreview() {
        final SurfaceTexture st = mUVCCameraView.getSurfaceTexture();
        mCameraHandler.startPreview(new Surface(st));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCaptureButton.setVisibility(View.VISIBLE);
            }
        });
        updateItems();
    }

    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            Toast.makeText(UVCCameraActivity.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
            Toast.makeText(UVCCameraActivity.this, "device.getVendorId() = " + device.getVendorId(),
                    Toast.LENGTH_SHORT).show();
            Toast.makeText(UVCCameraActivity.this, "device.getVendorId() = " + device.getProductId(),
                    Toast.LENGTH_SHORT).show();
            Log.v(TAG, "device.getVendorId() = " + Integer.toHexString(device.getVendorId()));
            Log.v(TAG, "device.getProductId() = " + device.getProductId());


        }

        @Override
        public void onConnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock, final boolean createNew) {
            if (DEBUG) Log.v(TAG, "onConnect:");
            mCameraHandler.open(ctrlBlock);
            startPreview();
            updateItems();
        }

        @Override
        public void onDisconnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock) {
            if (DEBUG) Log.v(TAG, "onDisconnect:");
            if (mCameraHandler != null) {
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        mCameraHandler.close();
                    }
                }, 0);
                setCameraButton(false);
                updateItems();
            }
        }

        @Override
        public void onDettach(final UsbDevice device) {
            showDialogAndFinish("Probe has been removed, close this camera activity.");
        }

        @Override
        public void onCancel(final UsbDevice device) {
            setCameraButton(false);
        }
    };

    void showDialogAndFinish(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(UVCCameraActivity.this);
        builder.setMessage(message)
                .setPositiveButton("OK", (dialog, id) -> finish())
                .create()
                .show();
    }

    /**
     * to access from CameraDialog
     *
     * @return
     */
    @Override
    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }

    @Override
    public void onDialogResult(boolean canceled) {
        if (DEBUG) Log.v(TAG, "onDialogResult:canceled=" + canceled);
        if (canceled) {
            setCameraButton(false);
        }
    }

    //================================================================================
    private boolean isActive() {
        return mCameraHandler != null && mCameraHandler.isOpened();
    }

    private boolean checkSupportFlag(final int flag) {
        return mCameraHandler != null && mCameraHandler.checkSupportFlag(flag);
    }

    private int getValue(final int flag) {
        return mCameraHandler != null ? mCameraHandler.getValue(flag) : 0;
    }

    private int setValue(final int flag, final int value) {
        return mCameraHandler != null ? mCameraHandler.setValue(flag, value) : 0;
    }

    private int resetValue(final int flag) {
        return mCameraHandler != null ? mCameraHandler.resetValue(flag) : 0;
    }

    private void updateItems() {
        runOnUiThread(mUpdateItemsOnUITask, 100);
    }

    private final Runnable mUpdateItemsOnUITask = new Runnable() {
        @Override
        public void run() {
            if (isFinishing()) return;
            final int visible_active = isActive() ? View.VISIBLE : View.INVISIBLE;
//            mToolsLayout.setVisibility(visible_active);
//            mBrightnessButton.setVisibility(
//                    checkSupportFlag(UVCCamera.PU_BRIGHTNESS)
//                            ? visible_active : View.INVISIBLE);
//            mContrastButton.setVisibility(
//                    checkSupportFlag(UVCCamera.PU_CONTRAST)
//                            ? visible_active : View.INVISIBLE);
        }
    };

    private int mSettingMode = -1;

    /**
     * 設定画面を表示
     *
     * @param mode
     */
    private final void showSettings(final int mode) {
        if (DEBUG) Log.v(TAG, String.format("showSettings:%08x", mode));
        hideSetting(false);
        if (isActive()) {
            switch (mode) {
                case UVCCamera.PU_BRIGHTNESS:
                    mSettingMode = mode;
                    mSettingSeekbar.setProgress(mBrightness);
                    break;
                case UVCCamera.PU_CONTRAST:
                    mSettingMode = mode;
                    mSettingSeekbar.setProgress(mContrast);
                    Log.d("Andy", "showSettings mContrast = " + mContrast);

                    break;
            }
            ViewAnimationHelper.fadeIn(mValueLayout, -1, 0, mViewAnimationListener);

        }
    }

    private void resetSettings() {
        if (isActive()) {
            switch (mSettingMode) {
                case UVCCamera.PU_BRIGHTNESS:
                    if (mProbe == DI1000 || mProbe == DI1000L) {
                        mBrightness = DEFAULT_BRIGHTNESS_D1000;
                    } else {
                        mBrightness = DEFAULT_BRIGHTNESS_D2000;
                    }
                    mSettingSeekbar.setProgress(mBrightness);
                    SharedPreferenceHelper.setData(UVCCameraActivity.this,
                            mProbe.probeName, PREF_KEY_BRIGHTNESS, mBrightness);
                    break;
                case UVCCamera.PU_CONTRAST:
                    if (mProbe == DI1000 || mProbe == DI1000L) {
                        mContrast = DEFAULT_CONTRAST_D1000;
                    } else {
                        mContrast = DEFAULT_CONTRAST_D2000;
                    }
                    mSettingSeekbar.setProgress(mContrast);
                    SharedPreferenceHelper.setData(UVCCameraActivity.this,
                            mProbe.probeName, PREF_KEY_CONTRAST, mContrast);
                    break;
            }
        }
        mSettingMode = -1;
        ViewAnimationHelper.fadeOut(mValueLayout, -1, 0, mViewAnimationListener);
    }

    /**
     * 設定画面を非表示にする
     *
     * @param fadeOut trueならばフェードアウトさせる, falseなら即座に非表示にする
     */
    protected final void hideSetting(final boolean fadeOut) {
        removeFromUiThread(mSettingHideTask);
        if (fadeOut) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ViewAnimationHelper.fadeOut(mValueLayout, -1, 0, mViewAnimationListener);
                }
            }, 0);
        } else {
            try {
                mValueLayout.setVisibility(View.GONE);
            } catch (final Exception e) {
                // ignore
            }
            mSettingMode = -1;
        }
    }

    protected final Runnable mSettingHideTask = new Runnable() {
        @Override
        public void run() {
            hideSetting(true);
        }
    };

    /**
     * 設定値変更用のシークバーのコールバックリスナー
     */
    private final SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
            // 設定が変更された時はシークバーの非表示までの時間を延長する
            if (fromUser) {
                runOnUiThread(mSettingHideTask, SETTINGS_HIDE_DELAY_MS);
            }
        }

        @Override
        public void onStartTrackingTouch(final SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(final SeekBar seekBar) {
            // シークバーにタッチして値を変更した時はonProgressChangedへ
            // 行かないみたいなのでここでも非表示までの時間を延長する
            runOnUiThread(mSettingHideTask, SETTINGS_HIDE_DELAY_MS);
            if (isActive() && checkSupportFlag(mSettingMode)) {
                switch (mSettingMode) {
                    case UVCCamera.PU_BRIGHTNESS:
                        mBrightness = seekBar.getProgress();
                        SharedPreferenceHelper.setData(UVCCameraActivity.this,
                                mProbe.probeName, PREF_KEY_BRIGHTNESS, mBrightness);
                        setValue(mSettingMode, seekBar.getProgress());
                        break;
                    case UVCCamera.PU_CONTRAST:
                        mContrast = seekBar.getProgress();
                        SharedPreferenceHelper.setData(UVCCameraActivity.this,
                                mProbe.probeName, PREF_KEY_CONTRAST, mContrast);
                        setValue(mSettingMode, seekBar.getProgress());
                        break;
                }
            }    // if (active)
        }
    };

    private final ViewAnimationHelper.ViewAnimationListener
            mViewAnimationListener = new ViewAnimationHelper.ViewAnimationListener() {
        @Override
        public void onAnimationStart(@NonNull final Animator animator, @NonNull final View target, final int animationType) {
//			if (DEBUG) Log.v(TAG, "onAnimationStart:");
        }

        @Override
        public void onAnimationEnd(@NonNull final Animator animator, @NonNull final View target, final int animationType) {
            final int id = target.getId();
            switch (animationType) {
                case ViewAnimationHelper.ANIMATION_FADE_IN:
                case ViewAnimationHelper.ANIMATION_FADE_OUT: {
                    final boolean fadeIn = animationType == ViewAnimationHelper.ANIMATION_FADE_IN;
                    if (id == R.id.value_layout) {
                        if (fadeIn) {
                            runOnUiThread(mSettingHideTask, SETTINGS_HIDE_DELAY_MS);
                        } else {
                            mValueLayout.setVisibility(View.GONE);
                            mSettingMode = -1;
                        }
                    } else if (!fadeIn) {
//					target.setVisibility(View.GONE);
                    }
                    break;
                }
            }
        }

        @Override
        public void onAnimationCancel(@NonNull final Animator animator, @NonNull final View target, final int animationType) {
//			if (DEBUG) Log.v(TAG, "onAnimationStart:");
        }
    };
}
