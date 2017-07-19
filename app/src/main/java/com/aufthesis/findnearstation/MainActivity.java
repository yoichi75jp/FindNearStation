package com.aufthesis.findnearstation;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class MainActivity extends AppCompatActivity implements LocationListener, Runnable, OnMapReadyCallback {

    private Context m_context;
    //private DBOpenHelper m_DbHelper;
    private SQLiteDatabase m_db;
    private LocationManager m_locationMgr;
    private Location m_Loc;
    private ListView m_list_station;
    private GoogleMap m_map;
    private Spinner m_spinner;
    //private Menu        m_menu;
    private double m_Area = 1000;
    private double m_Latitude = 35.681382;
    private double m_Longitude = 139.766084;
    private String m_Provider = LocationManager.NETWORK_PROVIDER;

    private static ProgressDialog m_waitDialog = null;
    private int m_timeSleep = 4000;
    private int m_area_no = 1;
    private int m_disp_num = 10;

    private boolean m_changeArea = false;
    private boolean m_flgLocOn = false;
    //private boolean m_flgMovable = true;

    private List<Marker> m_listMarker = new ArrayList<>();
    private List<Map<String, String>> m_stationDataList = new ArrayList<>();
    private float m_zoom = 0.f;
    private boolean m_isLocationChanged = false;
    private boolean m_showDialog = true;

    //private List<Integer> m_listSubNemu = new ArrayList<>();

    private AdView  m_adView;
    private static InterstitialAd m_InterstitialAd = null;
    //private static final String _MEDIA_CODE_ADMOB_INTERSTITIAL = "ca-app-pub-1485554329820885/8115720256";

    //private FirebaseAnalytics m_Analytics;

    private boolean PERMISSION_GRANTED = true;

    // setMyLocationEnabled対応のため 2016/08/18
    static final int RC_LOCATION_PERMISSIONS = 0x01;

    static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private FirebaseAnalytics m_FirebaseAnalytics;

    private SharedPreferences m_prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //m_Analytics = FirebaseAnalytics.getInstance(this);

        // Obtain the FirebaseAnalytics instance.
        m_FirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Bundle fireLogBundle = new Bundle();
        fireLogBundle.putString("TEST", "MyApp MainActivity.onCreate() is called.");
        MyApp.getFirebaseAnalytics().logEvent(FirebaseAnalytics.Event.APP_OPEN, fireLogBundle);

        m_prefs = getSharedPreferences("SETTINGS", MODE_PRIVATE);
        int count = m_prefs.getInt(getString(R.string.count_induce), 0);
        count++;
        SharedPreferences.Editor editor = m_prefs.edit();
        editor.putInt(getString(R.string.count_induce), count);
        editor.apply();

        m_context = this;
        DBOpenHelper dbHelper = new DBOpenHelper(this);
        m_db = dbHelper.getDataBase();
        m_locationMgr = (LocationManager) getSystemService(LOCATION_SERVICE);
        m_list_station = findViewById(R.id.list_station);
        m_adView = this.findViewById(R.id.adView);

        //APIレベル23以降でパーミッション確認関数
        if(Build.VERSION.SDK_INT >= 23)
        {
            //Permission確認のため追加 2016/09/26
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED)
                PERMISSION_GRANTED = false;
        }

        // MapFragmentオブジェクトを取得
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map_fragment);
        try {
            // GoogleMapオブジェクトの取得
            //m_map = mapFragment.getMap(); //MapFragment.getMap()メソッドは非推奨になったため、getMapAsync()を使う
            mapFragment.getMapAsync(this);
            // MapFragmentのオブジェクトをセット
            mapFragment.setRetainInstance(true);
            /*
             // Activityが初回で生成されたとき
             if (savedInstanceState == null) {
             // MapFragmentのオブジェクトをセット
             mapFragment.setRetainInstance(true);
             // 地図タイプ設定
             m_map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
             m_map.getUiSettings().setRotateGesturesEnabled(true);
             m_map.getUiSettings().setZoomControlsEnabled(true);
             setMap();
             }
             /**/
        }
        // GoogleMapが使用不可のときのためにtry catchで囲っています。
        catch (Exception e) {
            String exp = e.getMessage();
        }
        m_spinner = findViewById(R.id.spinner_area);
        //spinner.setAdapter(adapter);
        m_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        m_Area = 500;
                        m_zoom = 15.5f;
                        break;
                    case 1:
                        m_Area = 1000;
                        m_zoom = 14.5f;
                        break;
                    case 2:
                        m_Area = 2000;
                        m_zoom = 13.0f;
                        break;
                    case 3:
                        m_Area = 5000;
                        m_zoom = 11.5f;
                        break;
                    case 4:
                        m_Area = 10000;
                        m_zoom = 11.0f;
                        break;
                    case 5:
                        m_Area = 20000;
                        m_zoom = 10.0f;
                        break;
                }
                m_showDialog = true;
                m_changeArea = true;
                //m_flgMovable = true;

                //APIレベル23以降でパーミッション確認関数
                if(Build.VERSION.SDK_INT >= 23)
                {
                    //Permission確認のため追加 2016/11/17
                    PERMISSION_GRANTED = !(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED);
                }

                //Permission確認のため追加 2016/09/26
                if(!PERMISSION_GRANTED) return;

                if(setLocation(true) || m_flgLocOn)
                {
                    getNearStation();
                    setMap();/**/
                }
                else
                {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(m_context);
                    dialog.setTitle(getString(R.string.notPositioningTitle));
                    dialog.setMessage(getString(R.string.notPositioningMessage));
                    dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            m_flgLocOn = true;
                            startActivity(new Intent("android.settings.LOCATION_SOURCE_SETTINGS"));
                        }
                    });
                    dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {}
                    });
                    dialog.show();
                }/**/
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                setLocation(false);
            }
        });
        setMapEvent();

        //バナー広告
        AdRequest adRequest = new AdRequest.Builder().build();
        if(m_adView != null) m_adView.loadAd(adRequest);

        // AdMobインターステイシャル
        m_InterstitialAd = new InterstitialAd(this);
        m_InterstitialAd.setAdUnitId("ca-app-pub-1485554329820885/8115720256");
        m_InterstitialAd.loadAd(new AdRequest.Builder().build());
    }

    //GoogleMapのイベント関連設定
    private void setMapEvent()
    {
        /*
         m_map.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener(){
        @Override
        public void onMyLocationChange(Location loc)
        {
        if(m_flgMovable)
        {
        LatLng curr = new LatLng(loc.getLatitude(), loc.getLongitude());
        m_map.animateCamera(CameraUpdateFactory.newLatLng(curr));
        }
        }
        });
         /**/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        //m_menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        /*
         if(id == R.id.action_train)
         {
         return true;
         }
         **/
        if(id == R.id.action_cancel)
        {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(getString(R.string.exitTile));
            dialog.setMessage(getString(R.string.exitMessage));
            dialog.setPositiveButton(getString(R.string.exitOK), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            dialog.setNegativeButton(getString(R.string.exitCancel), new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            dialog.show();
        }
        if(id == R.id.action_refresh)
        {
            m_showDialog = true;
            //m_flgMovable = true;
            setLocation(false);
            setMap();
            return true;
        }
        if(id == R.id.action_settings)
        {
            Intent intent = new Intent(this, SettingActivity.class);
            intent.putExtra(getString(R.string.area), m_area_no);
            intent.putExtra(getString(R.string.disp), m_disp_num);
            int requestCode = 2;
            startActivityForResult(intent, requestCode);
            //startActivity(intent);
            // アニメーションの設定
            overridePendingTransition(R.animator.slide_in_right, R.animator.slide_out_left);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();

        //default
        getPreferenceData();
        //m_isLocationChanged =false;
        //m_locationMgr.requestLocationUpdates(m_Provider, 30000, 10, this);
        if (m_locationMgr != null) {
            if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                m_locationMgr.requestLocationUpdates(m_Provider, 30000, 10, this);
            }
        }

        if(m_showDialog) setWait(4000);
        setMap();
        if(m_adView == null)
        {
            m_adView = this.findViewById(R.id.adView);
            //バナー広告
            AdRequest adRequest = new AdRequest.Builder().build();
            if(m_adView != null) m_adView.loadAd(adRequest);/**/
        }
        else
            m_adView.resume();
    }

    @Override
    public void onStart() {
        super.onStart();
        //測定
        //setLocation(false);
    }

    private boolean setLocation(boolean useGPS) {
        m_list_station.setAdapter(null);
        Criteria criteria = new Criteria();
        List<String> enableProv = m_locationMgr.getProviders(true);
        m_Provider = m_locationMgr.getBestProvider(criteria, true);
        if (enableProv.size() == 0) {
            //showDialog("Error", getString(R.string.disablePositioningMessage));
            m_showDialog = false;
            return false;
        }
        m_Provider = LocationManager.NETWORK_PROVIDER;
        /*  // GPS機能が怪しいので一時封印 2017/05/02
        boolean isPGS = m_locationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNET = m_locationMgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if(!useGPS && isNET)
            m_Provider = LocationManager.NETWORK_PROVIDER;
        else if(isPGS || useGPS)
            m_Provider = LocationManager.GPS_PROVIDER;
        else
        {
            m_showDialog = false;
            return false;
        }
        */
        //m_locationMgr.requestLocationUpdates(m_Provider, 30000, 10, this);
        if (m_locationMgr != null) {
            if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                m_locationMgr.requestLocationUpdates(m_Provider, 30000, 10, this);
            }
        }
        m_Loc = m_locationMgr.getLastKnownLocation(m_Provider);
        if (m_Loc == null) {
            m_showDialog = false;
            return false;
        }
        if(m_showDialog) setWait(4000);
        return true;
    }

    @Override
    public void onPause() {
        if (m_adView != null) {
            m_adView.pause();
        }
        super.onPause();
        //位置情報の更新を止める
        //m_locationMgr.removeUpdates(this);
        if (m_locationMgr != null) {
            if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                m_locationMgr.removeUpdates(this);
            }
        }
        m_Loc = null;
    }

    @Override
    public void onStop() {
        super.onStop();
        //位置情報の更新を止める
        //m_locationMgr.removeUpdates(this);
        if (m_locationMgr != null) {
            if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                m_locationMgr.removeUpdates(this);
            }
        }
        m_Loc = null;
    }

    @Override
    public void onDestroy() {
        if (m_adView != null) {
            m_adView.destroy();
        }
        super.onDestroy();
        //位置情報の更新を止める
        //m_locationMgr.removeUpdates(this);
        if (m_locationMgr != null) {
            if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                m_locationMgr.removeUpdates(this);
            }
        }
        m_Loc = null;

        //Intent intent = new Intent(this, AdvertisementActivity.class);
        //startActivity(intent);
    }

    @SuppressWarnings("unchecked")
    public void getNearStation() {
        try {
            if (m_Loc == null) //テスト用暫定処置
            {
                //緯度
                m_Latitude = 35.66390344;
                //経度
                m_Longitude = 139.75136385;
                showDialog("Error", getString(R.string.notGetPositioningInfoMessage) + "\nProvider:" + m_Provider);
                return;
            } else {
                m_isLocationChanged = true;
                //精度
                //float fAccuracy = m_Loc.getAccuracy();
                //緯度
                m_Latitude = m_Loc.getLatitude();
                //m_Latitude = 36.153277;
                //経度
                m_Longitude = m_Loc.getLongitude();
                //m_Longitude = 140.507626;
            }/*
             //緯度
             m_Latitude = 35.66390344;
             //経度
             m_Longitude = 139.75136385;/**/
            /*
             //緯度
             m_Latitude = 41.9054;
             //経度
             m_Longitude = 140.646525;/**/
            String sLat = String.valueOf(m_Latitude);
            String sLon = String.valueOf(m_Longitude);

            //およそ２５ｋｍ圏内の駅を全て取得するようにしている
            String sSQL = "select tbl_station.station_name, tbl_station.lon, tbl_station.lat, Line.line_name from tbl_station " +
                    "left join tbl_line as Line on Line.line_cd = tbl_station.line_cd " +
                    "where tbl_station.lon >= " + sLon + " - 0.2 and tbl_station.lon <= " + sLon + " + 0.2 and " +
                    "tbl_station.lat >= " + sLat + " - 0.2 and tbl_station.lat <= " + sLat + " + 0.2 and " +
                    "not tbl_station.e_status = 2";
            Cursor cursor = m_db.rawQuery(sSQL, null);
            cursor.moveToFirst();
            if (cursor.getCount() == 0) {
                showDialog("Not Found Near Station", getString(R.string.notFoundStation));
            } else {
                BaseAdapter adapter = new SimpleAdapter(this,
                        createData(cursor),
                        android.R.layout.simple_list_item_2,
                        new String[]{"station", "distance"},
                        new int[]{android.R.id.text1, android.R.id.text2}
                );
                m_list_station.setAdapter(adapter);
                m_list_station.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    //@SuppressWarnings("unchecked")
                    @Override
                    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                        Map<String, String> conMap = (Map<String, String>) arg0.getItemAtPosition(arg2);
                        //m_txtLine.setText(conMap.get("line"));
                        String sStation = conMap.get("station");
                        //m_flgMovable = false;
                        setMapCenter(sStation);
                    }
                });
                //長押しイベント
                m_list_station.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    //リストの項目が長押しされた場合の処理
                    //@SuppressWarnings("unchecked")
                    @Override
                    public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                        displayInterstitial();
                        Map<String, String> conMap = (Map<String, String>) arg0.getItemAtPosition(arg2);
                        setMapCenter(conMap.get("station"));
                        Intent intent = new Intent(m_context, CompassActivity.class);
                        intent.putExtra("station", conMap.get("station"));
                        intent.putExtra("latitude", conMap.get("latitude"));
                        intent.putExtra("longitude", conMap.get("longitude"));
                        intent.putExtra("locateLat", m_Latitude);
                        intent.putExtra("locateLon", m_Longitude);
                        //startActivity(intent);
                        int requestCode = 1;
                        startActivityForResult(intent, requestCode);
                        // アニメーションの設定
                        overridePendingTransition(R.animator.slide_in_right, R.animator.slide_out_left);
                        return true;
                    }
                });
            }
            cursor.close();
            if (m_changeArea) setMap();
            m_changeArea = false;
            setMerker();
        } catch (Exception e) {
            Log.v("Error:getNearStation", e.toString());
            showDialog("Error", e.toString());
        }
    }

    //入力データから路線名を取得
    private List<Map<String, String>> createData(Cursor cursor) {
        Set<Map<String, String>> retDataList = new HashSet<>();
        int i, j, nCount = 0;

        for (i = 0; i < cursor.getCount(); i++) {
            Map<String, String> data = new HashMap<>();

            String sStation = (cursor.getString(0));
            double dLon = (cursor.getDouble(1));
            double dLat = (cursor.getDouble(2));
            String sLine = (cursor.getString(3));

            float[] results = new float[3];
            Location.distanceBetween(m_Latitude, m_Longitude, dLat, dLon, results);

            BigDecimal bd = new BigDecimal(results[0]);
            float fDistance = bd.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();    //小数点第2位で四捨五入
            NumberFormat nf = NumberFormat.getNumberInstance();                         //3桁区切りでカンマ表示するように

            //指定範囲内の駅(100件以内)のみをリスト化
            if (fDistance <= m_Area) {
                //if(nCount <= 20)
                {
                    data.put("value", String.valueOf(fDistance));
                    data.put("station", sStation + " (" + sLine + ")");
                    data.put("distance", String.valueOf(nf.format(fDistance)) + " m");
                    data.put("latitude", String.valueOf(dLat));
                    data.put("longitude", String.valueOf(dLon));
                    retDataList.add(data);
                }
                nCount++;
            }
            cursor.moveToNext();
        }

        for (i = 0; i < m_listMarker.size(); i++) m_listMarker.get(i).remove();
        m_listMarker.clear();
        m_stationDataList.clear();

        List<Map<String, String>> listTmp = new ArrayList<>();
        listTmp.addAll(retDataList);

        //ソート処理(距離が近い順に)
        for (i = 0; i < listTmp.size() - 1; i++) {
            for (j = listTmp.size() - 1; j > i; j--) {
                Map<String, String> data1 = listTmp.get(j);
                Map<String, String> data2 = listTmp.get(j - 1);
                float fVal1 = Float.valueOf(data1.get("value"));
                float fVal2 = Float.valueOf(data2.get("value"));
                if (fVal1 < fVal2) {
                    Map<String, String> tmp = listTmp.get(j);
                    listTmp.set(j, listTmp.get(j - 1));
                    listTmp.set(j - 1, tmp);
                }
            }
        }
        if (listTmp.size() == 0)
            Toast.makeText(this, getString(R.string.notFoundStation2), Toast.LENGTH_SHORT).show();
        else {
            String sMsg = getString(R.string.foundStation, nCount);
            if (nCount > m_disp_num) sMsg += "\n(" + getString(R.string.omit, m_disp_num) +")";
            Toast.makeText(this, sMsg, Toast.LENGTH_SHORT).show();
        }
        int nEnd = listTmp.size();
        if (nEnd >= m_disp_num) nEnd = m_disp_num;
        m_stationDataList.addAll(listTmp.subList(0, nEnd));
        return m_stationDataList;
    }

    //---以下、LocationListenerを実装した際に必要となるメソッド---
    @Override
    public void onProviderEnabled(String provider) {
        m_Provider = provider;
        Toast.makeText(this, "Provider : " + getString(R.string.available, "[" + m_Provider + "]"), Toast.LENGTH_LONG).show();
        //m_Loc = m_locationMgr.getLastKnownLocation(m_Provider);
        if (m_locationMgr != null) {
            if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                m_Loc = m_locationMgr.getLastKnownLocation(m_Provider);
            }
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        //Toast.makeText(this, "Provider : [" + provider + "]が利用できなくなりました", Toast.LENGTH_LONG).show();
        m_Loc = null;
    }

    @Override
    public void onLocationChanged(Location location) {
        m_Loc = location;
        //setWait(1000);
        m_flgLocOn= false;
        getNearStation();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }
    //---ここまで---

    private void showDialog(String title, final String message) {
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

    private void setWait(int nSleep) {
        if (m_waitDialog != null) return;
        // プログレスダイアログの設定
        m_waitDialog = new ProgressDialog(this);
        // プログレスダイアログのメッセージを設定します
        m_waitDialog.setMessage(getString(R.string.positioning));
        // 円スタイル（くるくる回るタイプ）に設定します
        m_waitDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        // プログレスダイアログを表示
        m_waitDialog.show();

        m_timeSleep = nSleep;
        Thread thread = new Thread(this);
        /* show()メソッドでプログレスダイアログを表示しつつ、
        * 別スレッドを使い、裏で重い処理を行う。
        */
        thread.start();
    }

    @Override
    public void run() {
        try {
            //ダイアログがしっかり見えるように少しだけスリープ
            //（nnn：任意のスリープ時間・ミリ秒単位）
            Thread.sleep(m_timeSleep);
            //m_thread.join();
        } catch (InterruptedException e) {
            //スレッドの割り込み処理を行った場合に発生、catchの実装は割愛
        }
        //run内でUIの操作をしてしまうと、例外が発生する為、
        //Handlerにバトンタッチ
        m_handler.sendEmptyMessage(0);
    }

    private Handler m_handler = new Handler() {
        public void handleMessage(Message msg) {
            // プログレスダイアログ終了
            m_waitDialog.dismiss();
            m_waitDialog = null;
        }
    };

    // 地図の初期設定メソッド
    private void setMap() {
        float fZoom = m_zoom;
        if (!m_isLocationChanged) fZoom = 1.0f;
        if(m_map == null) return;
        // 現在位置ボタンの表示を行なう
        // mod start 2016/08/18 GoogleMap.setMyLocationEnabled() はruntime permissionsを要求するようになりました
        //m_map.setMyLocationEnabled(true);
        this.setMyLocationEnabled();
        // mod end 2016/08/18
        //位置、ズーム設定
        /*
         //緯度
         m_Latitude = 35.66390344;
         //経度
         m_Longitude = 139.75136385;/**/
        CameraPosition camerapos = new CameraPosition.Builder()
                .target(new LatLng(m_Latitude, m_Longitude)).zoom(fZoom).build();

        // 地図の中心の変更する
        //if(IsInit) m_map.moveCamera(CameraUpdateFactory.newCameraPosition(camerapos));
        m_map.animateCamera(CameraUpdateFactory.newCameraPosition(camerapos), 2000, null);
    }

    // add 2016/08/18
    void setMyLocationEnabled() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, RC_LOCATION_PERMISSIONS);
            return;
        }
        m_map.setMyLocationEnabled(true);
    }

    //マーカーの設定
    private void setMerker() {
        int i;
        for (i = 0; i < m_listMarker.size(); i++) m_listMarker.get(i).remove();
        m_listMarker.clear();

        MarkerOptions options = new MarkerOptions();
        for (i = 0; i < m_stationDataList.size(); i++) {
            String sLat = m_stationDataList.get(i).get("latitude");
            String sLon = m_stationDataList.get(i).get("longitude");
            LatLng loc = new LatLng(Double.valueOf(sLat), Double.valueOf(sLon));
            options.position(loc);
            options.title(m_stationDataList.get(i).get("station"));
            m_listMarker.add(m_map.addMarker(options));
        }
    }

    private void setMapCenter(String station) {
        for (int i = 0; i < m_stationDataList.size(); i++) {
            if (m_stationDataList.get(i).get("station").equals(station)) {
                String sLat = m_stationDataList.get(i).get("latitude");
                String sLon = m_stationDataList.get(i).get("longitude");
                LatLng loc = new LatLng(Double.valueOf(sLat), Double.valueOf(sLon));
                //位置、ズーム設定
                CameraPosition camerapos = new CameraPosition.Builder()
                        .target(loc).zoom(17.f).build();
                // 地図の中心の変更する
                m_map.animateCamera(CameraUpdateFactory.newCameraPosition(camerapos), 2000, null);
                for (int j = 0; j < m_listMarker.size(); j++) {
                    Marker marker = m_listMarker.get(j);
                    String title = marker.getTitle();
                    if (title.equals(station)) {
                        marker.showInfoWindow();
                        break;
                    }
                }
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1:
            case 2:
                this.induceReview();
                m_showDialog = false;
                setMap();
                break;
        }
    }

    //プリファセンス値を取得してメンバに設定
    private void getPreferenceData()
    {
        m_area_no = m_prefs.getInt(getString(R.string.area), m_area_no);
        m_disp_num = m_prefs.getInt(getString(R.string.disp), m_disp_num);

        if(m_area_no < 0)
        {
            m_area_no = 1;
            showDialog("error", String.valueOf(m_area_no));
        }

        m_spinner.setSelection(m_area_no, false);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(getString(R.string.exitTile));
            dialog.setMessage(getString(R.string.exitMessage));
            dialog.setPositiveButton(getString(R.string.exitOK), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            dialog.setNegativeButton(getString(R.string.exitCancel), new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            dialog.show();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void displayInterstitial() {
        int count = m_prefs.getInt(getString(R.string.count_use), 0);
        count++;
        if(count >= 3)
        {
            count = 0;
            if (m_InterstitialAd.isLoaded()) {
                m_InterstitialAd.show();
            }
        }
        SharedPreferences.Editor editor = m_prefs.edit();
        editor.putInt(getString(R.string.count_use), count);
        editor.apply();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        if (m_map == null) {
            m_map = map;

            // 地図タイプ設定
            m_map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            m_map.getUiSettings().setRotateGesturesEnabled(true);
            m_map.getUiSettings().setZoomControlsEnabled(true);
            setMap();
        }
    }

    //Permission確認のため追加 2016/09/26
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        PERMISSION_GRANTED = requestCode == PackageManager.PERMISSION_GRANTED;
    }

    private void induceReview()
    {
        int count = m_prefs.getInt(getString(R.string.count_induce), 0);
        if(count >= 50)
        {
            try
            {
                Thread.sleep(500);
            }
            catch(Exception e){}
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(getString(R.string.induce_title));
            dialog.setMessage(getString(R.string.induce_message));
            dialog.setPositiveButton(getString(R.string.induce_ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    SharedPreferences.Editor editor = m_prefs.edit();
                    editor.putInt(getString(R.string.count_induce), 0);
                    editor.apply();
                    Intent googlePlayIntent = new Intent(Intent.ACTION_VIEW);
                    googlePlayIntent.setData(Uri.parse("market://details?id=com.aufthesis.findnearstation"));
                    startActivity(googlePlayIntent);
                }
            });
            dialog.setNegativeButton(getString(R.string.induce_cancel), new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            dialog.setCancelable(false);
            dialog.show();
        }
    }

}


