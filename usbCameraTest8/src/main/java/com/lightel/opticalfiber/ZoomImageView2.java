package com.lightel.opticalfiber;


import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

public class ZoomImageView2 extends android.support.v7.widget.AppCompatImageView
        implements ScaleGestureDetector.OnScaleGestureListener,
        View.OnTouchListener, ViewTreeObserver.OnGlobalLayoutListener {
    /**
     * 縮放手勢的監測
     */
    private ScaleGestureDetector mScaleGestureDetector;
    /**
     * 監聽手勢
     */
    private GestureDetector mGestureDetector;
    /**
     * 對圖片進行縮放平移的Matrix
     */
    private Matrix mScaleMatrix;
    /**
     * 第一次載入圖片時調整圖片縮放比例，使圖片的寬或者高充滿螢幕
     */
    private boolean mFirst;
    /**
     * 圖片的初始化比例
     */
    private float mInitScale;
    /**
     * 圖片的最大比例
     */
    private float mMaxScale;
    /**
     * 雙擊圖片放大的比例
     */
    private float mMidScale;

    /**
     * 是否正在自動放大或者縮小
     */
    private boolean isAutoScale;

    //-----------------------------------------------
    /**
     * 上一次觸控點的數量
     */
    private int mLastPointerCount;
    /**
     * 是否可以拖動
     */
    private boolean isCanDrag;
    /**
     * 上一次滑動的x和y座標
     */
    private float mLastX;
    private float mLastY;
    /**
     * 可滑動的臨界值
     */
    private int mTouchSlop;
    /**
     * 是否用檢查左右邊界
     */
    private boolean isCheckLeftAndRight;
    /**
     * 是否用檢查上下邊界
     */
    private boolean isCheckTopAndBottom;


    public ZoomImageView2(Context context) {
        this(context, null, 0);
    }

    public ZoomImageView2(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZoomImageView2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //一定要將圖片的ScaleType設定成Matrix型別的
        setScaleType(ScaleType.MATRIX);
        //初始化縮放手勢監聽器
        mScaleGestureDetector = new ScaleGestureDetector(context, this);
        //初始化矩陣
        mScaleMatrix = new Matrix();
        setOnTouchListener(this);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        //初始化手勢檢測器，監聽雙擊事件
        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                //如果是正在自動縮放，則直接返回，不進行處理
                if (isAutoScale)
                    return true;
                //得到點選的座標
                float x = e.getX();
                float y = e.getY();
                Log.d("Andy" ,"onDoubleTap");
//                post(new AutoScaleRunnable(mMidScale, x, y));
//                //如果當前圖片的縮放值小於指定的雙擊縮放值
                if (getScale() < mMidScale) {
                    //進行自動放大
                    post(new AutoScaleRunnable(mMidScale, x, y));
                    Log.i("Andy", "根據圖片的縮放值小於指定的雙擊縮放值------FD");
                } else {
                    //當前圖片的縮放值大於初試縮放值，則自動縮小
                    Log.i("Andy", "根據圖片的縮放值小於指定的雙擊縮放值------zdsx");
                    post(new AutoScaleRunnable(mInitScale, x, y));
                }
                return true;
            }
        });


    }

    /**
     * 當view新增到window時呼叫，早於onGlobalLayout，因此可以在這裡註冊監聽器
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    /**
     * 當view從window上移除時呼叫，因此可以在這裡移除監聽器
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeGlobalOnLayoutListener(this);
    }

    /**
     * 當佈局樹發生變化時會呼叫此方法，我們可以在此方法中獲得控制元件的寬和高
     */
    @Override
    public void onGlobalLayout() {
        Log.d("Andy", "onGlobalLayout mFirst = " + mFirst);
        //只有當第一次載入圖片的時候才會進行初始化，用一個變數mFirst控制
        if (!mFirst) {
            //得到控制元件的寬和高
            int width = getWidth();
            int height = getHeight();
            if (width == 0 || height == 0) return;
            Log.i("Andy", "得倒控制元件的寬高" + "" + width + "" + height);
            //得到當前ImageView中載入的圖片
            Drawable d = getDrawable();
            if (d == null) {//如果沒有圖片，則直接返回
                return;
            }
            //得到當前圖片的寬和高，圖片的寬和高不一定等於控制元件的寬和高
            //因此我們需要將圖片的寬和高與控制元件寬和高進行判斷
            //將圖片完整的顯示在螢幕中
            int dw = d.getIntrinsicWidth();
            int dh = d.getIntrinsicHeight();
            Log.i("Andy", "得倒圖片的寬高" + "" + dw + "" + dh);
            if (dw == 0 || dh == 0) return;

            //我們定義一個臨時變數，根據圖片與控制元件的寬高比例，來確定這個最終縮放值
            float scale = 1.0f;
            //如果圖片寬度大於控制元件寬度，圖片高度小於控制元件高度
            if (dw > width && dh < height) {
                Log.i("Andy", "圖片寬度大於控制元件寬度，圖片高度小於控制元件高度");
                //我們需要將圖片寬度縮小，縮小至控制元件的寬度
                //至於為什麼要這樣計算，我們可以這樣想
                //我們呼叫matrix.postScale（scale,scale）時，寬和高都要乘以scale的
                //當前我們的圖片寬度是dw，dw*scale=dw*（width/dw）=width,這樣就等於控制元件寬度了
                //我們的高度同時也乘以scale，這樣能夠保證圖片的寬高比不改變，圖片不變形
                scale = width * 1.0f / dw;

            }
            //如果圖片的寬度小於控制元件寬度，圖片高度大於控制元件高度
            if (dw < width && dh > height) {
                Log.i("Andy", "圖片的寬度小於控制元件寬度，圖片高度大於控制元件高度");
                //我們就應該將圖片的高度縮小，縮小至控制元件的高度，計算方法同上
                scale = height * 1.0f / dh;
            }
            //如果圖片的寬度小於控制元件寬度，高度小於控制元件高度時，我們應該將圖片放大
            //比如圖片寬度是控制元件寬度的1/2 ，圖片高度是控制元件高度的1/4
            //如果我們將圖片放大4倍，則圖片的高度是和控制元件高度一樣了，但是圖片寬度就超出控制元件寬度了
            //因此我們應該選擇一個最小值，那就是將圖片放大2倍，此時圖片寬度等於控制元件寬度
            //同理，如果圖片寬度大於控制元件寬度，圖片高度大於控制元件高度，我們應該將圖片縮小
            //縮小的倍數也應該為那個最小值
            if ((dw < width && dh < height) || (dw > width && dh > height)) {
                scale = Math.min(width * 1.0f / dw, height * 1.0f / dh);
                Log.i("Andy", "圖片的寬度小於控制元件寬度，高度小於控制元件高度時");
            }

            //我們還應該對圖片進行平移操作，將圖片移動到螢幕的居中位置
            //控制元件寬度的一半減去圖片寬度的一半即為圖片需要水平移動的距離
            //高度同理，大家可以畫個圖看一看
            int dx = width / 2 - dw / 2;
            int dy = height / 2 - dh / 2;
            //對圖片進行平移，dx和dy分別表示水平和豎直移動的距離
            mScaleMatrix.postTranslate(dx, dy);
            //對圖片進行縮放，scale為縮放的比例，後兩個引數為縮放的中心點
            mScaleMatrix.postScale(scale, scale, width / 2, height / 2);
            //將矩陣作用於我們的圖片上，圖片真正得到了平移和縮放
            setImageMatrix(mScaleMatrix);

            //初始化一下我們的幾個縮放的邊界值
            mInitScale = scale;
            //最大比例為初始比例的4倍
            mMaxScale = mInitScale * 4;
            //雙擊放大比例為初始化比例的2倍
            mMidScale = mInitScale * 2;

            mFirst = true;

        }
    }

    /**
     * 獲得圖片當前的縮放比例值
     */
    private float getScale() {
        //Matrix為一個3*3的矩陣，一共9個值
        float[] values = new float[9];
        //將Matrix的9個值對映到values陣列中
        mScaleMatrix.getValues(values);
        //拿到Matrix中的MSCALE_X的值，這個值為圖片寬度的縮放比例，因為圖片高度
        //的縮放比例和寬度的縮放比例一致，我們取一個就可以了
        //我們還可以 return values[Matrix.MSCALE_Y];
        Log.i("Andy", "圖片的縮放值------" + values[Matrix.MSCALE_X]);
        return values[Matrix.MSCALE_X];


    }

    /**
     * 獲得縮放後圖片的上下左右座標以及寬高
     */
    private RectF getMatrixRectF() {
        //獲得當錢圖片的矩陣
        Matrix matrix = mScaleMatrix;
        //建立一個浮點型別的矩形
        RectF rectF = new RectF();
        //得到當前的圖片
        Drawable d = getDrawable();
        if (d != null) {
            //使這個矩形的寬和高同當前圖片一致
            rectF.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            //將矩陣對映到矩形上面，之後我們可以通過獲取到矩陣的上下左右座標以及寬高
            //來得到縮放後圖片的上下左右座標和寬高
            matrix.mapRect(rectF);
        }
        return rectF;
    }

    /**
     * 當縮放時檢查邊界並且使圖片居中
     */
    private void checkBorderAndCenterWhenScale() {
        if (getDrawable() == null) {
            return;
        }
        //初始化水平和豎直方向的偏移量
        float deltaX = 0.0f;
        float deltaY = 0.0f;
        //得到控制元件的寬和高
        int width = getWidth();
        int height = getHeight();
        //拿到當前圖片對應的矩陣
        RectF rectF = getMatrixRectF();
        //如果當前圖片的寬度大於控制元件寬度，當前圖片處於放大狀態
        if (rectF.width() >= width) {
            //如果圖片左邊座標是大於0的，說明圖片左邊離控制元件左邊有一定距離，
            //左邊會出現一個小白邊
            if (rectF.left > 0) {
                //我們將圖片向左邊移動
                deltaX = -rectF.left;
            }
            //如果圖片右邊座標小於控制元件寬度，說明圖片右邊離控制元件右邊有一定距離，
            //右邊會出現一個小白邊
            if (rectF.right < width) {
                //我們將圖片向右邊移動
                deltaX = width - rectF.right;
            }
        }
        //上面是調整寬度，這是調整高度
        if (rectF.height() >= height) {
            //如果上面出現小白邊，則向上移動
            if (rectF.top > 0) {
                deltaY = -rectF.top;
            }
            //如果下面出現小白邊，則向下移動
            if (rectF.bottom < height) {
                deltaY = height - rectF.bottom;
            }
        }
        //如果圖片的寬度小於控制元件的寬度，我們要對圖片做一個水平的居中
        if (rectF.width() < width) {
            Log.i("Andy", "圖片的寬度小於控制元件的寬度，我們要對圖片做一個水平的居中");
            deltaX = width / 2f - rectF.right + rectF.width() / 2f;
        }

        //如果圖片的高度小於控制元件的高度，我們要對圖片做一個豎直方向的居中
        if (rectF.height() < height) {
            Log.i("Andy", "圖片的高度小於控制元件的高度，我們要對圖片做一個豎直方向的居中");
            deltaY = height / 2f - rectF.bottom + rectF.height() / 2f;
        }
        //將平移的偏移量作用到矩陣上
        mScaleMatrix.postTranslate(deltaX, deltaY);
    }

    /**
     * 平移時檢查上下左右邊界
     */
    private void checkBorderWhenTranslate() {
        //獲得縮放後圖片的相應矩形
        RectF rectF = getMatrixRectF();
        //初始化水平和豎直方向的偏移量
        float deltaX = 0.0f;
        float deltaY = 0.0f;
        //得到控制元件的寬度
        int width = getWidth();
        //得到控制元件的高度
        int height = getHeight();
        //如果是需要檢查左和右邊界
        if (isCheckLeftAndRight) {
            //如果左邊出現的白邊
            if (rectF.left > 0) {
                //向左偏移
                deltaX = -rectF.left;
            }
            //如果右邊出現的白邊
            if (rectF.right < width) {
                //向右偏移
                deltaX = width - rectF.right;
            }
        }
        //如果是需要檢查上和下邊界
        if (isCheckTopAndBottom) {
            //如果上面出現白邊
            if (rectF.top > 0) {
                //向上偏移
                deltaY = -rectF.top;
            }
            //如果下面出現白邊
            if (rectF.bottom < height) {
                //向下偏移
                deltaY = height - rectF.bottom;
            }
        }

        mScaleMatrix.postTranslate(deltaX, deltaY);
    }


    /**
     * 自動放大縮小，自動縮放的原理是使用View.postDelay()方法，每隔16ms呼叫一次
     * run方法，給人視覺上形成一種動畫的效果
     */
    private class AutoScaleRunnable implements Runnable {
        //放大或者縮小的目標比例
        private float mTargetScale;
        //可能是BIGGER,也可能是SMALLER
        private float tempScale;
        //放大縮小的中心點
        private float x;
        private float y;
        //比1稍微大一點，用於放大
        private final float BIGGER = 1.07f;
        //比1稍微小一點，用於縮小
        private final float SMALLER = 0.93f;

        //構造方法，將目標比例，縮放中心點傳入，並且判斷是要放大還是縮小
        public AutoScaleRunnable(float targetScale, float x, float y) {
            this.mTargetScale = targetScale;
            this.x = x;
            this.y = y;
            Log.i("Andy", "-----" + mTargetScale);
            //如果當前縮放比例小於目標比例，說明要自動放大
            if (getScale() < mTargetScale) {
                Log.i("Andy", "如果當前縮放比例小於目標比例，說明要自動放大");
                //設定為Bigger
                tempScale = BIGGER;
            }
            //如果當前縮放比例大於目標比例，說明要自動縮小
            if (getScale() > mTargetScale) {
                Log.i("Andy", "當前縮放比例大於目標比例，說明要自動縮小");
                //設定為Smaller
                tempScale = SMALLER;
            }
        }

        @Override
        public void run() {
            //這裡縮放的比例非常小，只是稍微比1大一點或者比1小一點的倍數
            //但是當每16ms都放大或者縮小一點點的時候，動畫效果就出來了
            mScaleMatrix.postScale(tempScale, tempScale, x, y);
            //每次將矩陣作用到圖片之前，都檢查一下邊界
            checkBorderAndCenterWhenScale();
            //將矩陣作用到圖片上
            setImageMatrix(mScaleMatrix);
            //得到當前圖片的縮放值
            float currentScale = getScale();
            //如果當前想要放大，並且當前縮放值小於目標縮放值
            //或者 當前想要縮小，並且當前縮放值大於目標縮放值
            if ((tempScale > 1.0f) && currentScale < mTargetScale
                    || (tempScale < 1.0f) && currentScale > mTargetScale) {
                //每隔16ms就呼叫一次run方法
                postDelayed(this, 16);
            } else {
                //current*scale=current*(mTargetScale/currentScale)=mTargetScale
                //保證圖片最終的縮放值和目標縮放值一致
                float scale = mTargetScale / currentScale;
                mScaleMatrix.postScale(scale, scale, x, y);
                checkBorderAndCenterWhenScale();
                setImageMatrix(mScaleMatrix);
                //自動縮放結束，置為false
                isAutoScale = false;
            }
        }
    }

    /**
     * 這個是OnScaleGestureListener中的方法，在這個方法中我們可以對圖片進行放大縮小
     */
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        //當我們兩個手指進行分開操作時，說明我們想要放大，這個scaleFactor是一個稍微大於1的數值
        //當我們兩個手指進行閉合操作時，說明我們想要縮小，這個scaleFactor是一個稍微小於1的數值
        float scaleFactor = detector.getScaleFactor();
        //獲得我們圖片當前的縮放值
        float scale = getScale();
        //如果當前沒有圖片，則直接返回
        if (getDrawable() == null) {
            return true;
        }
        //如果scaleFactor大於1，說明想放大，當前的縮放比例乘以scaleFactor之後小於
        //最大的縮放比例時，允許放大
        //如果scaleFactor小於1，說明想縮小，當前的縮放比例乘以scaleFactor之後大於
        //最小的縮放比例時，允許縮小
        if ((scaleFactor > 1.0f && scale * scaleFactor < mMaxScale)
                || scaleFactor < 1.0f && scale * scaleFactor > mInitScale) {
            //邊界控制，如果當前縮放比例乘以scaleFactor之後大於了最大的縮放比例
            if (scale * scaleFactor > mMaxScale + 0.01f) {
                //則將scaleFactor設定成mMaxScale/scale
                //當再進行matrix.postScale時
                //scale*scaleFactor=scale*(mMaxScale/scale)=mMaxScale
                //最後圖片就會放大至mMaxScale縮放比例的大小
                scaleFactor = mMaxScale / scale;
            }
            //邊界控制，如果當前縮放比例乘以scaleFactor之後小於了最小的縮放比例
            //我們不允許再縮小
            if (scale * scaleFactor < mInitScale + 0.01f) {
                //計算方法同上
                scaleFactor = mInitScale / scale;

            }
            //前兩個引數是縮放的比例，是一個稍微大於1或者稍微小於1的數，形成一個隨著手指放大
            //或者縮小的效果
            //detector.getFocusX()和detector.getFocusY()得到的是多點觸控的中點
            //這樣就能實現我們在圖片的某一處區域性放大的效果
            mScaleMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
            //因為圖片的縮放點不是圖片的中心點了，所以圖片會出現偏移的現象，所以進行一次邊界的檢查和居中操作
            checkBorderAndCenterWhenScale();
            //將矩陣作用到圖片上
            setImageMatrix(mScaleMatrix);
        }
        return true;
    }

    /**
     * 一定要返回true
     */
    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //當雙擊操作時，不允許移動圖片，直接返回true
        if (mGestureDetector.onTouchEvent(event)) {
            return true;
        }
        //將事件傳遞給ScaleGestureDetector
        mScaleGestureDetector.onTouchEvent(event);
        //用於儲存多點觸控產生的座標
        float x = 0.0f;
        float y = 0.0f;
        //得到多點觸控的個數
        int pointerCount = event.getPointerCount();
        //將所有觸控點的座標累加起來
        for (int i = 0; i < pointerCount; i++) {
            x += event.getX(i);
            y += event.getY(i);
        }
        //取平均值，得到的就是多點觸控後產生的那個點的座標
        x /= pointerCount;
        y /= pointerCount;
        //如果觸控點的數量變了，則置為不可滑動
        if (mLastPointerCount != pointerCount) {
            isCanDrag = false;
            mLastX = x;
            mLastY = y;
        }
        mLastPointerCount = pointerCount;
        RectF rectF = getMatrixRectF();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isCanDrag = false;
                //當圖片處於放大狀態時，禁止ViewPager攔截事件，將事件傳遞給圖片，進行拖動
                if (rectF.width() > getWidth() + 0.01f || rectF.height() > getHeight() + 0.01f) {
                    if (getParent() instanceof ViewPager) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                //當圖片處於放大狀態時，禁止ViewPager攔截事件，將事件傳遞給圖片，進行拖動
                if (rectF.width() > getWidth() + 0.01f || rectF.height() > getHeight() + 0.01f) {
                    if (getParent() instanceof ViewPager) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }
                //得到水平和豎直方向的偏移量
                float dx = x - mLastX;
                float dy = y - mLastY;
                //如果當前是不可滑動的狀態，判斷一下是否是滑動的操作
                if (!isCanDrag) {
                    isCanDrag = isMoveAction(dx, dy);
                }
                //如果可滑動
                if (isCanDrag) {
                    if (getDrawable() != null) {
                        isCheckLeftAndRight = true;
                        isCheckTopAndBottom = true;
                        //如果圖片寬度小於控制元件寬度
                        if (rectF.width() < getWidth()) {
                            //左右不可滑動
                            dx = 0;
                            //左右不可滑動，也就不用檢查左右的邊界了
                            isCheckLeftAndRight = false;
                        }
                        //如果圖片的高度小於控制元件的高度
                        if (rectF.height() < getHeight()) {
                            //上下不可滑動
                            dy = 0;
                            //上下不可滑動，也就不用檢查上下邊界了
                            isCheckTopAndBottom = false;
                        }
                    }
                    mScaleMatrix.postTranslate(dx, dy);
                    //當平移時，檢查上下左右邊界
                    checkBorderWhenTranslate();
                    setImageMatrix(mScaleMatrix);
                }
                mLastX = x;
                mLastY = y;
                break;
            case MotionEvent.ACTION_UP:
                //當手指擡起時，將mLastPointerCount置0，停止滑動
                mLastPointerCount = 0;
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
        }
        return true;
    }


    /**
     * 判斷是否是移動的操作
     */
    private boolean isMoveAction(float dx, float dy) {
        //勾股定理，判斷斜邊是否大於可滑動的一個臨界值
        return Math.sqrt(dx * dx + dy * dy) > mTouchSlop;
    }
}