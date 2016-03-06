package com.alphadraco.audioanalyzer;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;

import java.util.ArrayList;

/**
 * Created by aladin on 02.03.2016.
 */
public class AudioWaveView extends XYPlot {

    public AudioReportView audioReportView;

    public void setPreferences(AudioAnalyzer _root, SharedPreferences prefs) {
        super.setPreferences(_root,prefs,"ReportWave");
        if (sharedPreferences != null) {
            /*try {
                SweepFreqStart=Float.parseFloat(sharedPreferences.getString("SweepFreqStart", "100.0"));
            } catch (Exception E) {
                SweepFreqStart = 100.0f;
            }
            try {
                SweepFreqStop=Float.parseFloat(sharedPreferences.getString("SweepFreqStop", "20.0"))*1000;
            } catch (Exception E) {
                SweepFreqStop = 20000.0f;
            }
            try {
                SweepSteps=Integer.parseInt(sharedPreferences.getString("SweepSteps", "60"));
            } catch (Exception E) {
                SweepSteps = 60;
            }
            try {
                SweepAmp=Float.parseFloat(sharedPreferences.getString("SweepAmp", "-6.0"));
            } catch (Exception E) {
                SweepAmp = -6.0f;
            }
            SweepLog=sharedPreferences.getBoolean("SweepLog", true);
            setSweepText();

            if (PrefListener != null)
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(PrefListener);
            PrefListener=new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    if (key.equals("SweepFreqStart")) {
                        try {
                            SweepFreqStart = Float.parseFloat(sharedPreferences.getString("SweepFreqStart", "100.0"));
                        } catch (Exception E) {
                            SweepFreqStart = 100;
                        }
                        if (SweepFreqStart < 10) SweepFreqStart=10;
                        if (SweepFreqStart > 22000) SweepFreqStart=22000;
                        setSweepText();
                    }
                    if (key.equals("SweepFreqStop")) {
                        try {
                            SweepFreqStop=Float.parseFloat(sharedPreferences.getString("SweepFreqStop", "20.0"))*1000;
                        } catch (Exception E) {
                            SweepFreqStop = 20000;
                        }
                        if (SweepFreqStop < 10) SweepFreqStop=10;
                        if (SweepFreqStop > 22000) SweepFreqStop=22000;
                        setSweepText();
                    }
                    if (key.equals("SweepLog")) {
                        SweepLog=sharedPreferences.getBoolean("SweepLog", true);
                        setSweepText();
                    }
                    if (key.equals("SweepSteps")) {
                        try {
                            SweepSteps=Integer.parseInt(sharedPreferences.getString("SweepSteps", "60"));
                        } catch (Exception E) {
                            SweepSteps = 60;
                        }
                        if (SweepSteps < 2) SweepFreqStop=2;
                        if (SweepSteps > 200) SweepFreqStop=200;
                        setSweepText();
                    }
                    if (key.equals("SweepAmp")) {
                        try {
                            SweepAmp=Float.parseFloat(sharedPreferences.getString("SweepAmp", "-6.0"));
                        } catch (Exception E) {
                            SweepAmp = -6.0f;
                        }
                        if (SweepAmp < -100) SweepAmp=-100.0f;
                        if (SweepAmp > 0) SweepAmp=0.0f;
                        setSweepText();
                    }
                }
            };
            sharedPreferences.registerOnSharedPreferenceChangeListener(PrefListener);
            */
        }
    }



    protected void setup(Context context) {
        super.setup(context);
        audioReportView=null;

        defaultxMin=-2;
        defaultxMax=2;
        defaultyMin=-1.0f;
        defaultyMax=1.0f;

        canLogX=false;
        canLogY=false;

        AX.aMin=-2;
        AX.aMax=2;

        AY.aMin=-1.0f;
        AY.aMax=1.0f;

        unit="FS";

        displayOnlyOne=true;
        displayOne=-1;
    }

    public AudioWaveView(Context context) {
        super(context);
        setup(context);
    }

    public AudioWaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context);
    }





}
