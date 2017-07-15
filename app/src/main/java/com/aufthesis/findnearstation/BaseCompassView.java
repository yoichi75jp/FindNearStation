package com.aufthesis.findnearstation;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import java.math.BigDecimal;
import java.text.NumberFormat;

/**
// Created by yoichi75jp2 on 2015/06/24.
 */
public class BaseCompassView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private final static int ONE_FRAME_TICK = 1000 / 25;    // 1フレームの時間
    private final static int MAX_FRAME_SKIPS = 5;           // 時間が余ったとき最大何回フレームをスキップするか

    private SurfaceHolder mSurfaceHolder;   // サーフェイスホルダー
    private Thread mMainLoop;   // メインのゲームループの様なモノ
    private Context m_context;

    // 画像を表示するためのモノ
    private final Resources mRes = this.getContext().getResources();
    private Bitmap 	mBitmap;
    private float	m_ArrowDir;    // 矢印の方向

    private float 	m_swsize;
    private float 	m_shsize;

    private float 	m_bitmapWidth;
    private float 	m_bitmapHeight;

    // ////////////////////////////////////////////////////////////
    // コンストラクタ
    public BaseCompassView(Context context) {
        super(context);
        m_context = context;
        // Bitmapをロードする
        this.loadBitmap();
        // SurfaceViewの初期化
        this.initSurfaceView(context);
    }

    // ////////////////////////////////////////////////////////////
    // コンストラクタ
    public BaseCompassView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        m_context = context;
        // Bitmapをロードする
        this.loadBitmap();
        // SurfaceViewの初期化
        this.initSurfaceView(context);
    }

    // ////////////////////////////////////////////////////////////
    // コンストラクタ
    public BaseCompassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        m_context = context;
        // Bitmapをロードする
        this.loadBitmap();
        // SurfaceViewの初期化
        this.initSurfaceView(context);
    }

    // ////////////////////////////////////////////////////////////
    // Bitmapをロードする
    private void loadBitmap() {
        //画像のロード
            mBitmap = BitmapFactory.decodeResource(this.mRes, R.drawable.basecompass);
            m_bitmapWidth = mBitmap.getWidth();
            m_bitmapHeight = mBitmap.getHeight();
}

    // ////////////////////////////////////////////////////////////
    /**/
    // 回転角度設定
    public void setArrowDir(float dir) {
        m_ArrowDir = dir;
    }
    /**/
    // ////////////////////////////////////////////////////////////
    // SurfaceViewの初期化
    private void initSurfaceView(Context context) {
        // サーフェイスホルダーを取り出す
        this.mSurfaceHolder = this.getHolder();
        // コールバック関数を登録する
        this.mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setFormat(PixelFormat.TRANSLUCENT);

        setDispSize();
        setZOrderOnTop(true);
    }

    private void setDispSize()
    {
        // スマートフォンの液晶のサイズを取得を開始
        // ウィンドウマネージャのインスタンス取得
        WindowManager wm = (WindowManager)m_context.getSystemService(Context.WINDOW_SERVICE);
        // ディスプレイのインスタンス生成
        Display disp = wm.getDefaultDisplay();
        // mod start 2016/08/18 getWidth, getHeightが非推奨につき改修
        /*
        m_swsize = disp.getWidth();
        m_shsize = disp.getHeight();
        /**/
        Point point = new Point();
        disp.getSize(point);
        m_swsize = point.x;
        m_shsize = point.y;
         /**/
        // mod end 2016/08/18
    }

    public void onDraw() {
        Canvas canvas = this.mSurfaceHolder.lockCanvas();
        this.draw(canvas);
        this.mSurfaceHolder.unlockCanvasAndPost(canvas);
    }
    // ////////////////////////////////////////////////////////////
    // 描画処理
    public void draw(final Canvas canvas) {
        super.draw(canvas);
        // Arrowを表示
        Paint paint = new Paint();
        canvas.save();

        float fRatio = ((m_swsize/m_bitmapWidth)*0.9f);
        canvas.rotate(m_ArrowDir, m_swsize / 2.f, m_shsize / 2.f);
        canvas.translate((m_swsize/2.f) - (m_bitmapWidth*fRatio/2.f), (m_shsize/2.f)-(m_bitmapHeight*fRatio/2.f));
        canvas.scale(fRatio, fRatio);
        canvas.drawBitmap(mBitmap, 0, 0, paint);
        canvas.restore();
    }

//private int[] mScreenCenter = { 0, 0 };
    // ////////////////////////////////////////////////////////////
    // サーフェイスサイズの変更があったときとかに呼ばれる
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // センター位置
        //this.mScreenCenter[0] = width/2;
        //this.mScreenCenter[1] = height/2;

        setDispSize();
    }

    // ////////////////////////////////////////////////////////////
    // サーフェイスが作られたときに呼ばれる
    public void surfaceCreated(SurfaceHolder holder) {
        // ワーカースレッドを作る
        this.mMainLoop = new Thread(this);
        this.mMainLoop.start();
    }

    // ////////////////////////////////////////////////////////////
    // サーフェイスが破棄された時に呼ばれる
    public void surfaceDestroyed(SurfaceHolder holder) {
        this.mMainLoop = null;
    }

    // ////////////////////////////////////////////////////////////
    // 毎フレーム呼ばれるやつ
    public void move() {
    }

    //////////////////////////////////////////////////////////////
    //毎フレーム呼ばれるやつ
    //ちょっと上等なメインループ

    public void run() {
        Canvas canvas;
        long beginTime; // 処理開始時間
        long pastTick;  // 経過時間
        int sleep;
        int frameSkipped;   // 何フレーム分スキップしたか

        // フレームレート関連
        //int frameCount = 0;
        long beforeTick = 0;
        long currTime;
        //String tmp = "";

        // 文字書いたり
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setAntiAlias(true);
        paint.setTextSize(70);
        /**/
        //int count = 0;
        // スレッドが消滅していない間はずっと処理し続ける
        while (this.mMainLoop != null) {
            canvas = null;

            // フレームレートの表示
            //frameCount++;
            currTime = System.currentTimeMillis();
            if (beforeTick + 1000 < currTime) {
                beforeTick = currTime;
                //tmp = "" + frameCount;
                //frameCount = 0;
            }
            try {
                synchronized (this.mSurfaceHolder) {
                    canvas = this.mSurfaceHolder.lockCanvas();
                    // キャンバスとれなかった
                    if (canvas == null)
                        continue;

                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                    // 現在時刻
                    beginTime = System.currentTimeMillis();
                    frameSkipped = 0;

                    // ////////////////////////////////////////////////////////////
                    // ↓アップデートやら描画やら
                    this.move();
                    canvas.save();
                    this.draw(canvas);
                    canvas.restore();
                    // ////////////////////////////////////////////////////////////

                    // 経過時間
                    pastTick = System.currentTimeMillis() - beginTime;

                    // 余っちゃった時間
                    sleep = (int)(ONE_FRAME_TICK - pastTick);

                    // 余った時間があるときは待たせる
                    if (0 < sleep) {
                        try {
                            Thread.sleep(sleep);
                        } catch (Exception e) {}
                    }

                    // 描画に時間係過ぎちゃった場合は更新だけ回す
                    while (sleep < 0 && frameSkipped < MAX_FRAME_SKIPS) {
                        // ////////////////////////////////////////////////////////////
                        // 遅れた分だけ更新をかける
                        this.move();
                        // ////////////////////////////////////////////////////////////
                        sleep += ONE_FRAME_TICK;
                        frameSkipped++;
                    }
                }
            } finally {
                // キャンバスの解放し忘れに注意
                if (canvas != null) {
                    this.mSurfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }
}
