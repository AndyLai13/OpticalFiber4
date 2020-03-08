package com.lightel.opticalfiber;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.google.gson.JsonObject;
import com.lightel.opticalfiber.RestApiManager.Battery;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RTSPActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {

    DrawerLayout mDrawerLayout;
    TextureView mTextureView;
    View mLostConnectionLayout;
    TextView mLostConnection;
    Button mBtnRetry;
    private org.videolan.libvlc.MediaPlayer mMediaPlayer;
    private LibVLC mLibVLC = null;
    String rtspUrl0 = "rtsp://192.168.1.1/";
    //
//    String rtspUrl = "rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov";
//    String rtspUrl0 = "rtsp://192.168.1.1/";
//    //    String rtspUrl1 = "rtsp://192.168.1.1/MJPG?W=1280&H=960";
//    String rtspUrl1 = "rtsp://192.168.1.1/MJPG";
//    String rtspUrl2 = "rtsp://192.168.1.1/MJPG?W=640&H=480";
//    String rtspUrl3 = "rtsp://192.168.1.1/H264?W=1280&H=960";
//    String rtspUrl4 = "rtsp://192.168.1.1/H264?W=640&H=480";
//
    ZoomImageView2 imageView;
    TextView mTextState;
    SeekBar seekBarBrightness;
    SeekBar seekBarContrast;
    Button btnOption;
    Button btnBack;
    TextView mTextBattery;

    MutableLiveData<Boolean> isFrozen = new MutableLiveData<>();
    MutableLiveData<Boolean> isLostConnection = new MutableLiveData<>();
    MutableLiveData<Battery> battery = new MutableLiveData<>();

    WeakHandler mHandler = new WeakHandler(this);

    public static final int MSG_CHECK_CAPTURE_BTN_STATE = 0;
    public static final int MSG_CHECK_PLAY_STATE = 1;
    public static final int MSG_CHECK_BATTERS_STATE = 2;

    int oldBtnState = 0;
    boolean forceStop = false;

    static final int ACTION_UP = 0;
    static final int ACTION_DOWN = 1;

    private static class WeakHandler extends Handler {

        private WeakReference<Context> reference;

        public WeakHandler(Context context) {
            reference = new WeakReference<>(context);//这里传入activity的上下文
        }

        @Override
        public void handleMessage(Message msg) {
            if (reference.get() instanceof RTSPActivity) {
                ((RTSPActivity) reference.get()).handleMessage(msg);
            }
        }

    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_CHECK_CAPTURE_BTN_STATE:
                mHandler.removeMessages(MSG_CHECK_CAPTURE_BTN_STATE);
                getCpBtnState();
                break;
            case MSG_CHECK_PLAY_STATE:
                mHandler.removeMessages(MSG_CHECK_PLAY_STATE);
                checkPlayerState();
                mHandler.sendEmptyMessageDelayed(MSG_CHECK_PLAY_STATE, 1000);
                break;
            case MSG_CHECK_BATTERS_STATE:
                mHandler.removeMessages(MSG_CHECK_BATTERS_STATE);
                checkBatteryState();
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rtsp);

        // vlc init
        final ArrayList<String> args = new ArrayList<>();
        args.add("-vvv");
        args.add("--network-caching=100");
        mLibVLC = new LibVLC(this, args);
        mMediaPlayer = new MediaPlayer(mLibVLC);
        // live data init
        isFrozen.setValue(false);
        isLostConnection.setValue(false);
        // view init
        mDrawerLayout = findViewById(R.id.drawer_layout);
        mTextureView = findViewById(R.id.texture_view);
        mLostConnection = findViewById(R.id.lost_connection);
        mBtnRetry = findViewById(R.id.retry);
        mLostConnectionLayout = findViewById(R.id.lost_connection_layout);
        imageView = findViewById(R.id.freeze);
        mTextState = findViewById(R.id.state);
        seekBarBrightness = findViewById(R.id.seekbar_brightness);
        seekBarContrast = findViewById(R.id.seekbar_contrast);
        btnOption = findViewById(R.id.option);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        mTextBattery = findViewById(R.id.text_battery);
        // set landscape due to player is constrained.
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setTextViewSize();

        mTextureView.setSurfaceTextureListener(this);

        mBtnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                retry();
            }
        });

        isFrozen.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isFrozen) {
                // change text
                if (isFrozen) {
                    mTextState.setText(R.string.state_frozen_image);
                    mTextState.setTextColor(Color.RED);
                } else {
                    mTextState.setText(R.string.state_real_time_stream);
                    mTextState.setTextColor(ContextCompat.getColor(RTSPActivity.this, R.color.blue));
                }
                // capture image
                if (isFrozen) {
                    Bitmap bitmap = mTextureView.getBitmap();
                    imageView.setImageBitmap(bitmap);
                }
                // show/hide captured image
                if (isFrozen) {
                    mTextureView.setVisibility(View.INVISIBLE);
                    imageView.setVisibility(View.VISIBLE);
                } else {
                    mTextureView.setVisibility(View.VISIBLE);
                    imageView.setVisibility(View.GONE);
                }
            }
        });

        isLostConnection.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isLostConnection) {
                if (isLostConnection) {
                    mLostConnectionLayout.setVisibility(View.VISIBLE);
                    mTextureView.setVisibility(View.INVISIBLE);
                    stopQueryCapBtnState();
                } else {
                    mLostConnectionLayout.setVisibility(View.GONE);
                    mTextureView.setVisibility(View.VISIBLE);
                    getDefault();
                    startQueryCapBtnState();
                }
            }
        });

        battery.observe(this, new Observer<Battery>() {
            @Override
            public void onChanged(Battery battery) {
                String content = "Battery : ";
                if (battery.Status.equals("Charge")) {
                    content = content.concat("Charge");
                } else {
                    content = content.concat(String.valueOf(battery.Percent));
                }
                mTextBattery.setText(content);
            }
        });

        seekBarBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                setBrightness(seekBar.getProgress());
                seekBarBrightness.setEnabled(false);
            }
        });

        seekBarContrast.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                setContrast(seekBar.getProgress());
                seekBarContrast.setEnabled(false);
            }
        });

        btnOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawerLayout.openDrawer(Gravity.RIGHT);
            }
        });
    }

    public void checkPlayerState() {
//        Log.d("Andy", "" + mMediaPlayer.getPlayerState());
        if (forceStop) {
            isLostConnection.setValue(true);
        } else {
            if (mMediaPlayer.getPlayerState() == Media.State.Playing) {
                isLostConnection.setValue(false);
            } else {
                isLostConnection.setValue(true);
            }
        }
    }

    public void checkBatteryState() {
        RestApiManager.getInstance().getBatteryState(new Callback<Battery>() {
            @Override
            public void onResponse(Call<Battery> call, Response<Battery> response) {
                if (response.isSuccessful()) {
                    Battery object = response.body();
                    battery.setValue(object);
                    mHandler.sendEmptyMessageDelayed(MSG_CHECK_BATTERS_STATE, 1000);
                }
            }

            @Override
            public void onFailure(Call<Battery> call, Throwable t) {

            }
        });
    }

    void startQueryCapBtnState() {
        mHandler.sendEmptyMessage(MSG_CHECK_CAPTURE_BTN_STATE);
    }

    void stopQueryCapBtnState() {
        mHandler.removeMessages(MSG_CHECK_CAPTURE_BTN_STATE);
    }

    void setTextViewSize() {
        if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            int landH = Util.getWidth(this);    // 1080
            int landW = Util.getHeight(this);   // 1920

            int viewHeight = landH;
            int viewWidth = (int) (viewHeight * (1280f / 960));
            mTextureView.setLayoutParams(new ConstraintLayout.LayoutParams(viewWidth, viewHeight));
            mLostConnectionLayout.setLayoutParams(new ConstraintLayout.LayoutParams(viewWidth, viewHeight));
        }
    }

    private void attachViewSurface() {
        final IVLCVout vlcVout = mMediaPlayer.getVLCVout();
        mMediaPlayer.setScale(0);
        vlcVout.detachViews();
        vlcVout.setVideoView(mTextureView);
        vlcVout.setWindowSize(mTextureView.getWidth(), mTextureView.getHeight());
        vlcVout.attachViews();
        mTextureView.setKeepScreenOn(true);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d("Andy", "onSurfaceTextureAvailable");
        attachViewSurface();
        if (mMediaPlayer.hasMedia()) {
            mMediaPlayer.play();
        } else {
            try {
                Media media = new Media(mLibVLC, Uri.parse(rtspUrl0));
                mMediaPlayer.setMedia(media);
                mMediaPlayer.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.d("Andy", "onSurfaceTextureSizeChanged");

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d("Andy", "onSurfaceTextureDestroyed");

        mMediaPlayer.release();
        mLibVLC.release();
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        Log.d("Andy", "onSurfaceTextureUpdated");
    }

    @Override
    protected void onResume() {
        super.onResume();
        retry();
        mHandler.sendEmptyMessage(MSG_CHECK_PLAY_STATE);
        mHandler.sendEmptyMessage(MSG_CHECK_BATTERS_STATE);
    }

    public synchronized void getCpBtnState() {
        RestApiManager.getInstance().getCapBtnState(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    JsonObject object = response.body();
                    int CapBtn = object.get("CapBtn").getAsInt();
                    if (CapBtn == oldBtnState) {
                        if (CapBtn == 0) {
                            // keep not click
//                            Log.d("Andy1", "keep not click ");
                        } else {
                            // keep click
//                            Log.d("Andy1", "keep click ");
                        }
                    } else {
                        if (CapBtn == 1) {
                            // action up
                            Log.d("Andy1", "action down");
                            onKeyEvent(ACTION_DOWN);
                        } else {
                            // action down
                            Log.d("Andy1", "action up");
                            onKeyEvent(ACTION_UP);
                        }
                    }
                    oldBtnState = CapBtn;
                    startQueryCapBtnState();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.d("Andy", "getCpBtnState Throwable = " + t);
                forceStop = true;
                stopQueryCapBtnState();
            }
        });
    }

    void retry() {
        try {
            Media media = new Media(mLibVLC, Uri.parse(rtspUrl0));
            mMediaPlayer.setMedia(media);
            mMediaPlayer.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
        resetForceStop();
    }

    void resetForceStop() {
        forceStop = false;
    }

    void onKeyEvent(int event) {
        switch (event) {
            case ACTION_DOWN:
                break;
            case ACTION_UP:
                if (isFrozen.getValue() != null) {
                    isFrozen.setValue(!isFrozen.getValue());
                }
                break;
        }
    }

    void getDefault() {
        RestApiManager.getInstance().getDefault(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    JsonObject object = response.body();
                    int brightness = object.get("Brightness").getAsInt();
                    int contrast = object.get("Contrast").getAsInt();
                    int sharpness = object.get("Sharpness").getAsInt();
                    int gain = object.get("Gain").getAsInt();
                    int autoExposure = object.get("AutoExposure").getAsInt();
                    int exposureTime = object.get("ExposureTime").getAsInt();

                    Log.d("Andy", "brightness = " + brightness);
                    Log.d("Andy", "contrast = " + contrast);
                    Log.d("Andy", "sharpness = " + sharpness);
                    Log.d("Andy", "gain = " + gain);
                    Log.d("Andy", "autoExposure = " + autoExposure);
                    Log.d("Andy", "exposureTime = " + exposureTime);
//                    Log.d("Andy", "contrast = " + contrast);
                    seekBarBrightness.setProgress(brightness);
                    seekBarContrast.setProgress(contrast);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.d("Andy", "Throwable = " + t);
            }
        });
    }

    // range: 0 ~ 100
    void setBrightness(int brightness) {
        RestApiManager.getInstance().setBrightness(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    JsonObject object = response.body();
                    int Status = object.get("Status").getAsInt();
                    Log.d("Andy", "Status = " + Status);
                    seekBarBrightness.setEnabled(true);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.d("Andy", "Throwable = " + t);
            }
        }, String.valueOf(brightness));
    }

    // range: 0 ~ 100
    void setContrast(int contrast) {
        RestApiManager.getInstance().setContrast(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    JsonObject object = response.body();
                    int Status = object.get("Status").getAsInt();
                    Log.d("Andy", "Status = " + Status);
                    seekBarContrast.setEnabled(true);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.d("Andy", "Throwable = " + t);
            }
        }, String.valueOf(contrast));
    }

    // range: 0 ~ 100
    void setSharpness(int sharpness) {
        RestApiManager.getInstance().setSharpness(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    JsonObject object = response.body();
                    int Status = object.get("Status").getAsInt();
                    Log.d("Andy", "Status = " + Status);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.d("Andy", "Throwable = " + t);
            }
        }, String.valueOf(sharpness));
    }

    // range: 0 ~ 100
    void setGain(int gain) {
        RestApiManager.getInstance().setGain(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    JsonObject object = response.body();
                    int Status = object.get("Status").getAsInt();
                    Log.d("Andy", "Status = " + Status);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.d("Andy", "Throwable = " + t);
            }
        }, String.valueOf(gain));
    }

    // range: 0 or 1
    void setAutoExposure(int autoExposure) {
        RestApiManager.getInstance().setAutoExposure(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    JsonObject object = response.body();
                    int Status = object.get("Status").getAsInt();
                    Log.d("Andy", "Status = " + Status);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.d("Andy", "Throwable = " + t);
            }
        }, String.valueOf(autoExposure));
    }

    // range: -13~-1
    void setExposureTime(int exposureTime) {
        RestApiManager.getInstance().setExposureTime(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    JsonObject object = response.body();
                    int Status = object.get("Status").getAsInt();
                    Log.d("Andy", "Status = " + Status);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.d("Andy", "Throwable = " + t);
            }
        }, String.valueOf(exposureTime));
    }
}