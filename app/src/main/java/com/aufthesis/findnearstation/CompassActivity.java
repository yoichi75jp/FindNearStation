package com.aufthesis.findnearstation;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.Surface;
import android.view.ViewGroup;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

/**
// Created by yoichi75jp2 on 2015/06/21.
 */
public class CompassActivity extends AppCompatActivity implements SensorEventListener, LocationListener
{
    private CompassView     m_compassView;
    private BaseCompassView m_baseCompassView;
    private SensorManager   m_SensorManager;  // センサマネージャ
    private Sensor          m_Accelerometer;  // 加速度センサ
    private Sensor          m_MagneticField;  // 磁気センサ

    private LocationManager m_locationMgr;
    private Location        m_Loc;
    private String          m_Provider;

    private String          m_sStation;
    private double          m_dStationLat;
    private double          m_dStationLon;

    private float[] 		m_results = new float[3];

    private AdView m_adView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);

        try
        {
            Intent intent = getIntent();
            m_sStation = intent.getExtras().getString("station");
            String sLatitude = intent.getExtras().getString("latitude");
            String sLongitude = intent.getExtras().getString("longitude");
            m_dStationLat = Double.valueOf(sLatitude);
            m_dStationLon = Double.valueOf(sLongitude);

            double dInitLat = intent.getExtras().getDouble("locateLat");
            double dInitLon = intent.getExtras().getDouble("locateLon");

            //
            m_locationMgr = (LocationManager)getSystemService(LOCATION_SERVICE);
            if(!checkGPS())
            {
                finish();
                return;
            }

            m_Provider = LocationManager.NETWORK_PROVIDER;
            //m_locationMgr.requestLocationUpdates(m_Provider, 1000, 1, this);
            if (m_locationMgr != null) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    m_locationMgr.requestLocationUpdates(m_Provider, 1000, 10, this);
                }
            }

            // センサーを取り出す
            m_SensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
            m_Accelerometer = m_SensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            m_MagneticField = m_SensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

            DiskView diskView = new DiskView(this);
            m_baseCompassView = new BaseCompassView(this);
            m_compassView = new CompassView(this);
            //setContentView(m_baseCompassView);
            setContentView(diskView);
            //オーバーレイビューの追加
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            addContentView(m_baseCompassView, params);
            addContentView(m_compassView, params);

            /*
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            {
                m_baseCompassView.setTranslationZ(2);
                m_compassView.setTranslationZ(2);
            }
            */
            //2点間の距離・方位角の初期値を取得
            Location.distanceBetween(dInitLat, dInitLon, m_dStationLat, m_dStationLon, m_results);

            ActionBar actionBar = getSupportActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);

            //バナー広告
            AdRequest adRequest = new AdRequest.Builder().build();
            if(m_adView != null) m_adView.loadAd(adRequest);
        }
        catch(Exception e)
        {
            showDialog("error", e.toString());
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        // リスナーの登録
        m_SensorManager.registerListener(this, m_Accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        m_SensorManager.registerListener(this, m_MagneticField, SensorManager.SENSOR_DELAY_NORMAL);

        //m_locationMgr.requestLocationUpdates(m_Provider, 1000, 1, this);
        if (m_locationMgr != null) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                m_locationMgr.requestLocationUpdates(m_Provider, 1000, 1, this);
            }
        }
        if(m_adView == null)
        {
            m_adView = (AdView)this.findViewById(R.id.adView);
            //バナー広告
            AdRequest adRequest = new AdRequest.Builder().build();
            if(m_adView != null) m_adView.loadAd(adRequest);/**/
        }
        else
            m_adView.resume();
    }
    @Override
    public void onPause()
    {
        if (m_adView != null) {
            m_adView.pause();
        }
        super.onPause();
        // リスナーの登録解除
        m_SensorManager.unregisterListener(this);
        //位置情報の更新を止める
        //m_locationMgr.removeUpdates(this);
        if (m_locationMgr != null) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                m_locationMgr.removeUpdates(this);
            }
        }
        m_Loc = null;
    }

    // ////////////////////////////////////////////////////////////
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    // 加速度センサの値
    private float[] m_AccelerometerValue = new float[3];
    // 磁気センサの値
    private float[] m_MagneticFieldValue = new float[3];
    // 磁気センサの更新がすんだか
    private boolean m_ValidMagneticFiled = false;
    // センサーの値が変更された
    public void onSensorChanged(SensorEvent event) {

        // センサーごとの処理
        switch (event.sensor.getType()) {
            // 加速度センサー
            case Sensor.TYPE_ACCELEROMETER:
                // cloneで配列がコピーできちゃうんだね。へえ
                m_AccelerometerValue = event.values.clone();
                break;
            // 磁気センサー
            case Sensor.TYPE_MAGNETIC_FIELD:
                m_MagneticFieldValue = event.values.clone();
                m_ValidMagneticFiled = true;
                break;
        }

        // 値が更新された角度を出す準備ができた
        if (m_ValidMagneticFiled) {
            // 方位を出すための変換行列
            float[] rotate = new float[16]; // 傾斜行列？
            float[] inclination = new float[16];    // 回転行列

            // うまいこと変換行列を作ってくれるらしい
            SensorManager.getRotationMatrix(
                    rotate, inclination,
                    m_AccelerometerValue,
                    m_MagneticFieldValue);

            // 方向を求める
            float[] orientation = new float[3];
            this.getOrientation(rotate, orientation);

            // デグリー角に変換する
            float degreeDir = (float)Math.toDegrees(orientation[0]);
            //Log.i("onSensorChanged", "角度:" + degreeDir);

            if(m_Loc != null)
            {
                //緯度
                double dLocateLat = m_Loc.getLatitude();
                //経度
                double dLocateLon = m_Loc.getLongitude();
/*
                //緯度
                m_dLocateLat = 35.66390344;
                //経度
                m_dLocateLon = 139.75136385;/**/
                //2点間の距離・方位角を取得
                Location.distanceBetween(dLocateLat, dLocateLon, m_dStationLat, m_dStationLon, m_results);
            }
            // 位置情報をViewに設定する
            m_baseCompassView.setArrowDir(degreeDir);
            m_compassView.setLocationInfo(degreeDir, m_results[1], m_results[0], m_sStation);
            //m_diskView.setLocationInfo(degreeDir, m_results[1], m_results[0], m_sStation);
        }
    }

    // 画面が回転していることを考えた方角の取り出し
    public void getOrientation(float[] rotate, float[] out)
    {
        // ディスプレイの回転方向を求める(縦もちとか横持ちとか)
        Display disp = this.getWindowManager().getDefaultDisplay();
        // ↓コレを使うためにはAPIレベルを8にする必要がある
        int dispDir = disp.getRotation();

        // 画面回転してない場合はそのまま
        if (dispDir == Surface.ROTATION_0)
            SensorManager.getOrientation(rotate, out);
            // 回転している
        else
        {
            float[] outR = new float[16];
            // 90度回転
            int rot1 = SensorManager.AXIS_Y;
            int rot2 = SensorManager.AXIS_MINUS_X;
            int rot3 = SensorManager.AXIS_MINUS_Y;
            if(dispDir == Surface.ROTATION_90)
            {
                SensorManager.remapCoordinateSystem(
                        rotate, rot1, rot2, outR);
            }
            // 180度回転
            else if (dispDir == Surface.ROTATION_180)
            {
                float[] outR2 = new float[16];
                SensorManager.remapCoordinateSystem(
                        rotate, rot1, rot2, outR2);
                SensorManager.remapCoordinateSystem(
                        outR2, rot1, rot2, outR);
            }
            // 270度回転
            else if (dispDir == Surface.ROTATION_270)
            {
                SensorManager.remapCoordinateSystem(
                        rotate, rot3, rot2, outR);
            }
            SensorManager.getOrientation(outR, out);
        }
    }

    //---以下、LocationListenerを実装した際に必要となるメソッド---
    @Override
    public void onProviderEnabled(String provider)
    {
        m_Provider = provider;
        //m_Loc = m_locationMgr.getLastKnownLocation(m_Provider);
        if (m_locationMgr != null) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                m_Loc = m_locationMgr.getLastKnownLocation(m_Provider);
            }
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        m_Loc = null;
        if(!checkGPS()) finish();
    }

    @Override
    public void onLocationChanged(Location location) {
        m_Loc = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras){}
    //---ここまで---

    //メッセージボックス
    private void showDialog(String title, String message)
    {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        dialog.show();
    }

    //GPSチェック
    private boolean checkGPS()
    {
        boolean isPGS = m_locationMgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if(!isPGS) {
            showDialog("GPS Error",getString(R.string.gps));
        }
        return isPGS;
    }

    @Override
    public void onDestroy()
    {
        if (m_adView != null) {
            m_adView.destroy();
        }
        super.onDestroy();
        setResult(RESULT_OK);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                // アニメーションの設定
                overridePendingTransition(R.animator.slide_in_left, R.animator.slide_out_right);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            // アニメーションの設定
            overridePendingTransition(R.animator.slide_in_left, R.animator.slide_out_right);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

}

