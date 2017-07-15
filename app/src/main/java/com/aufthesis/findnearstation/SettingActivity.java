package com.aufthesis.findnearstation;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Created by yoichi75jp2 on 2015/06/21.
 */
public class SettingActivity extends AppCompatActivity {

    private SharedPreferences   m_preferences;
    private Context             m_context;
    //private boolean             m_flgInit = true;

    private ListView            m_listSettings;
    private BaseAdapter         m_adapter;
    private List<String[]>      m_arrayData = new ArrayList<>();
    private final List<String>  m_listArea = new ArrayList<>();
    private final List<String>  m_listDisp = new ArrayList<>();
    private int                 m_posSelect = 0;
    private int                 m_posSelectItem = 0;

    private int[]				m_index = new int[2];

    private AdView  m_adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        m_adView = (AdView)this.findViewById(R.id.adView);
        TextView txtVersion = (TextView)findViewById(R.id.text_version);
        String sVersion = txtVersion.getText().toString();

        try
        {
            String sPackageName = getPackageName();
            PackageManager packageManager = getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(sPackageName, 0);
            String sVersionName = packageInfo.versionName;
            String sMessage = sVersion + sVersionName;
            txtVersion.setText(sMessage);
        }
        catch(Exception e)
        {
            txtVersion.setText("");
        }


        m_context= this;
        m_preferences = getSharedPreferences("SETTINGS", MODE_PRIVATE);

        String[] arrayArea = getResources().getStringArray(R.array.arealist);
        String[] arrayDisp = getResources().getStringArray(R.array.dispnumlist);
        int i;
        for(i = 0; i < arrayArea.length; i++) m_listArea.add(arrayArea[i]);
        for(i = 0; i < arrayDisp.length; i++) m_listDisp.add(arrayDisp[i]);

        m_arrayData.add(arrayArea);
        m_arrayData.add(arrayDisp);

        Intent intent = getIntent();
        int area_no = intent.getIntExtra(getString(R.string.area), 1);
        int disp_num = intent.getIntExtra(getString(R.string.disp), 10);

        m_listSettings = (ListView)findViewById(R.id.settings);
        m_adapter = new SimpleAdapter(this,
                createData(area_no, disp_num),
                android.R.layout.simple_list_item_2,
                new String[]{"item", "value"},
                new int[]{android.R.id.text1, android.R.id.text2}
        );
        m_listSettings.setAdapter(m_adapter);
        m_listSettings.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                //Map<String, String> conMap = (Map<String, String>) arg0.getItemAtPosition(arg2);
                //String sItem = conMap.get("item");
                m_posSelect = arg2;

                AlertDialog.Builder dialog = new AlertDialog.Builder(m_context);
                //dialog.setIcon(android.R.drawable.ic_dialog_alert);
                dialog.setTitle(getString(R.string.select));
                dialog.setSingleChoiceItems(m_arrayData.get(m_posSelect), m_index[m_posSelect], new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {/**/
                        //⇒アイテムを選択した時のイベント処理
                        String tmp = m_arrayData.get(m_posSelect)[whichButton];
                        m_posSelectItem = whichButton;

                        Toast.makeText(m_context,
                                getString(R.string.selectResult, tmp),
                                Toast.LENGTH_SHORT).show();
                     /**/
                    }
                });
                dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //⇒OKボタンを押下した時のイベント処理
                        //【NOTE】
                        //whichButtonには選択したアイテムのインデックスが入っているわけでは
                        //ないので注意
                        SharedPreferences.Editor editor = m_preferences.edit();
                        if (m_posSelect == 0)
                            editor.putInt(getString(R.string.area), m_posSelectItem);
                        else if (m_posSelect == 1)
                            editor.putInt(getString(R.string.disp), index2DispNum(m_posSelectItem));

                        m_index[m_posSelect] = m_posSelectItem;
                        editor.apply();
/**/
                        m_adapter = new SimpleAdapter(m_context,
                                createData(m_index[0], index2DispNum(m_index[1])),
                                android.R.layout.simple_list_item_2,
                                new String[]{"item", "value"},
                                new int[]{android.R.id.text1, android.R.id.text2}
                        );
                        m_adapter.notifyDataSetChanged();
                        //m_listSettings.invalidateViews();
                        m_listSettings.setAdapter(m_adapter);
 /**/
                    }
                });
                dialog.setNegativeButton(getString(R.string.exitCancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //⇒キャンセルボタンを押下した時のイベント処理
                        //【NOTE】
                        //whichButtonには選択したアイテムのインデックスが入っているわけでは
                        //ないので注意
                    }
                });
                dialog.show();
            }
        });

        try
        {
            android.support.v7.app.ActionBar actionBar = getSupportActionBar();
            if(actionBar != null)
                actionBar.setDisplayHomeAsUpEnabled(true);

            //バナー広告
            AdRequest adRequest = new AdRequest.Builder().build();
            if(m_adView != null) m_adView.loadAd(adRequest);
        }
        catch(Exception e)
        {
        }
    }

    //ListViewに設定するItemリスト
    private List<Map<String, String>> createData(int area_no, int disp_num) {
        List<Map<String, String>> retDataList = new ArrayList<>();

        int pos = 2;
        if(disp_num == 1) pos = 0;
        else if(disp_num == 5) pos = 1;
        else if(disp_num == 10) pos = 2;
        else if(disp_num == 20) pos = 3;

        m_index[0] = area_no;
        m_index[1] = pos;

        String[] arrayItem = {getString(R.string.searchArea), getString(R.string.displayNum)};
        String[] arrayVal = {m_listArea.get(area_no), m_listDisp.get(pos)};
        for(int i = 0; i < 2; i++)
        {
            Map<String, String> data = new HashMap<>();
            data.put("item", arrayItem[i]);
            data.put("value", arrayVal[i]);
            retDataList.add(data);
        }
        return retDataList;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (m_adView != null) {
            m_adView.resume();
        }
        //m_flgInit = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        //m_menu = menu;
        return true;
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
    public void onPause() {
        if (m_adView != null) {
            m_adView.pause();
        }
        super.onPause();
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

    private int index2DispNum(int index)
    {
        int disp_num = 10;
        if (index == 0) disp_num = 1;
        else if (index == 1) disp_num = 5;
        else if (index == 2) disp_num = 10;
        else if (index == 3) disp_num = 20;
        return disp_num;
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
