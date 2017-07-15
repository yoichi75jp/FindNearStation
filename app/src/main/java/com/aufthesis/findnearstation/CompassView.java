package com.aufthesis.findnearstation;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.util.AttributeSet;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.math.BigDecimal;
import java.text.NumberFormat;

/**
 // Created by yoichi75jp2 on 2015/06/20.
 */
//サーフェイスビューの拡張
public class CompassView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private final static int ONE_FRAME_TICK = 1000 / 25;    // 1フレームの時間
    private final static int MAX_FRAME_SKIPS = 5;           // 時間が余ったとき最大何回フレームをスキップするか

    private SurfaceHolder mSurfaceHolder;   // サーフェイスホルダー
    private Thread mMainLoop;   // メインのゲームループの様なモノ
    private Context m_context;
    private Vibrator m_vibrator;

    // 画像を表示するためのモノ
    private final Resources mRes = this.getContext().getResources();
    private Bitmap 	mBitmap;
    private float	m_ArrowDir;     // 矢印の方向
    private float   m_StationDir;   // 駅の方向

    private float 	m_swsize;
    private float 	m_shsize;

    private float 	m_bitmapWidth;
    private float 	m_bitmapHeight;

    private String 	m_station = "";
    private String  m_distance = "";

    private final String[] m_Direction = getResources().getStringArray(R.array.direction);

    // ////////////////////////////////////////////////////////////
    // コンストラクタ
    public CompassView(Context context) {
        super(context);
        m_context = context;
        // Bitmapをロードする
        this.loadBitmap();
        // SurfaceViewの初期化
        this.initSurfaceView(context);
    }

    // ////////////////////////////////////////////////////////////
    // コンストラクタ
    public CompassView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        m_context = context;
        // Bitmapをロードする
        this.loadBitmap();
        // SurfaceViewの初期化
        this.initSurfaceView(context);
    }

    // ////////////////////////////////////////////////////////////
    // コンストラクタ
    public CompassView(Context context, AttributeSet attrs) {
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
        // 画像のロード
        mBitmap = BitmapFactory.decodeResource(this.mRes, R.drawable.compass);
        m_bitmapWidth = mBitmap.getWidth();
        m_bitmapHeight = mBitmap.getHeight();
    }
    // ////////////////////////////////////////////////////////////

    // ////////////////////////////////////////////////////////////
    // SurfaceViewの初期化
    private void initSurfaceView(Context context) {
        // サーフェイスホルダーを取り出す
        this.mSurfaceHolder = this.getHolder();
        // コールバック関数を登録する
        this.mSurfaceHolder.addCallback(this);

        mSurfaceHolder.setFormat(PixelFormat.TRANSLUCENT);
        setDispSize();
        m_vibrator = (Vibrator)m_context.getSystemService(Context.VIBRATOR_SERVICE);
        setZOrderOnTop(true);
    }

    private void setDispSize()
    {
        // スマートフォンの液晶のサイズを取得を開始
        // ウィンドウマネージャのインスタンス取得
        WindowManager wm = (WindowManager)m_context.getSystemService(Context.WINDOW_SERVICE);
        // ディスプレイのインスタンス生成
        Display disp = wm.getDefaultDisplay();
        // スマートフォンの画面のサイズ
        /*
        m_swsize = disp.getWidth();
        m_shsize = disp.getHeight();
         /**/
        Point point = new Point();
        disp.getSize(point);
        m_swsize = point.x;
        m_shsize = point.y;
        /**/
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

        float fRatio = (m_swsize/m_bitmapWidth)/1.3f;

        canvas.rotate(m_ArrowDir, m_swsize/2.f, m_shsize/2.f);
        canvas.translate((m_swsize/2.f) - (m_bitmapWidth*fRatio/2.f), (m_shsize/2.f)-(m_bitmapHeight*fRatio/2.f));
        canvas.scale(fRatio, fRatio);

        canvas.drawBitmap(mBitmap, 0, 0, paint);
        canvas.restore();

/*
        // 文字を表示
        paint.setColor(Color.BLACK);
        paint.setAntiAlias(true);
        paint.setTextSize(70);
        canvas.save();
        canvas.rotate(this.m_ArrowDir, m_swsize/2.f, m_shsize/2.f);
        canvas.translate(m_swsize/2.f, m_shsize/2.f - m_bitmapHeight*fRatio/1.75f);
        canvas.drawText("駅", 0, 0, paint);
        canvas.restore();
 /**/
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
        paint.setTextSize(50);

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

                    //canvas.drawColor(Color.parseColor("#CACACA"));
                    //canvas.drawColor(Color.parseColor("#FFFFEE"));
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
                    canvas.drawText(mRes.getString(R.string.toStation, m_station), 1, 100, paint);
                    canvas.drawText(mRes.getString(R.string.direction, getDirectionString(m_StationDir)), 1, 200, paint);
                    canvas.drawText(mRes.getString(R.string.distance, m_distance), 1, 300, paint);
                }
            } finally {
                // キャンバスの解放し忘れに注意
                if (canvas != null) {
                    this.mSurfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    //位置情報を設定
    public void setLocationInfo(float iDegree1,float iDegree2, float iDistance, String iStation)
    {
        m_ArrowDir = iDegree1 + iDegree2;
        m_StationDir = iDegree2;
        m_station = iStation;

        BigDecimal bd = new BigDecimal(iDistance);
        float fDistance = bd.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();    //小数点第2位で四捨五入
        NumberFormat nf = NumberFormat.getNumberInstance();                         //3桁区切りでカンマ表示するように
        m_distance = String.valueOf(nf.format(fDistance));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                if((x <= m_swsize/2.f+100f && x >= m_swsize/2.f-100f) && (y <= m_shsize/2.f+100f && y >= m_shsize/2.f-100f))
                {
                    m_vibrator.vibrate(100);
                }
                break;
        }
        long lTime = event.getEventTime() - event.getDownTime();
        if(lTime >= 1000) {
            /*
            m_vibrator.vibrate(100);
            Toast.makeText(m_context, String.valueOf(lTime), Toast.LENGTH_LONG).show();
            Intent intent = new Intent(m_context, CatalogActivity.class);
            m_context.startActivity(intent);
             **/
        }
        return true;
    }

    private String getDirectionString(float ArrowDir)
    {
        float ratio = 360f/32f;
        String sDir = "";
        if((ArrowDir >= 360-(ratio) && ArrowDir <= 360 || ArrowDir >= 0 && ArrowDir <= ratio) ||
                ArrowDir >= -ratio && ArrowDir <= 0 || ArrowDir <= -(360-ratio) && ArrowDir >= -(360))
        {
            sDir = m_Direction[0];
        }
        else {
            for (int i = 1; i < 16; i++)
                if (ArrowDir >= ratio * (i*2-1) && ArrowDir <= ratio * (i*2+1) ||
                        ArrowDir >= -(ratio)*(32-(i*2-1)) && ArrowDir <= -(ratio)*(32-(i*2+1)))
                {
                    sDir = m_Direction[i];
                    break;
                }
        }
        return sDir;
    }

}

