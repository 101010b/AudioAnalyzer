package com.alphadraco.audioanalyzer;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;

/**
 * Created by aladin on 29.10.2015.
 */
public class SweepPlotView extends XYPlot {

    public boolean running;

    // Configuration
    public float SweepFreqStart;
    public float SweepFreqStop;
    public int SweepSteps;
    public boolean SweepLog;
    public float SweepAmp;

    int plotonly=0;

    public void setSweepText() {
        if (root == null) return;
        String text="";
        if (SweepLog) {
            text += "Log Sweep";
        } else {
            text += "Log Sweep";
        }
        text += String.format(" from %1.1f Hz to %1.0f kHz in %d steps at %1.1f dBFS",
                SweepFreqStart,SweepFreqStop/1000.0f,SweepSteps,SweepAmp);
        ((TextView) root.findViewById(R.id.tx_xy_sweep_mode)).setText(text);
    }

    public float getSweepStepFreq(int step) {
        if (step <= 0) return SweepFreqStart;
        if (step >= SweepSteps) return SweepFreqStop;
        if (SweepLog) {
            float a=(float)(SweepSteps-1)/(float)Math.log(SweepFreqStop/SweepFreqStart);
            return SweepFreqStart*(float)Math.exp((float)step/a);
        } else {
            return SweepFreqStart+(float)step*(SweepFreqStop-SweepFreqStart)/(float)(SweepSteps-1);
        }

    }

    public void setPreferences(AudioAnalyzer _root, SharedPreferences prefs) {
        root=_root;
        super.setPreferences(root,prefs,"Sweep");
        if (sharedPreferences != null) {
            try {
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
        }
    }

    protected void setup(Context context) {
        super.setup(context);

        SweepFreqStart=100;
        SweepFreqStop=20000;
        SweepSteps=60;
        SweepLog=true;
        SweepAmp=-6;

        running=false;
    }

    public SweepPlotView(Context context) {
        super(context);
        setup(context);
    }

    public SweepPlotView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context);
    }


    public void install_handlers(ImageButton ib_del, final ImageButton ib_edit, ImageButton ib_zoom) {
        if (ib_zoom != null) ib_zoom.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                autozoom();
            }
        });

        if (ib_del != null) ib_del.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteMenu(v);
            }
        });

        if (ib_edit != null) ib_edit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                colorMenu(v);
            }
        });
    }

}
