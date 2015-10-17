package com.alphadraco.audioanalyzer;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v7.internal.view.menu.MenuBuilder;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;

/**
 * Created by aladin on 08.10.2015.
 */
public class WaveView extends View {


    private SharedPreferences WavePreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener PrefListener;
    private Paint paint_text;
    public AudioAnalyzerHelper helper;
    private DataConsolidator dataConsolidator;
    private int scale;
    private float fontyofs;


    public void setPreferences(SharedPreferences prefs) {
        WavePreferences=prefs;
        if (WavePreferences != null) {
            scale = prefs.getInt("WaveViewScale", 1);
            PrefListener=new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                    if (key.equals("WaveViewScale"))
                        scale = prefs.getInt(key, 0);
                }
            };
            WavePreferences.registerOnSharedPreferenceChangeListener(PrefListener);
        } else
            PrefListener=null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if ((dataConsolidator != null) && (e.getAction()==MotionEvent.ACTION_DOWN)) {
            float x=e.getX();
            float y=e.getY();

            PopupMenu P = new PopupMenu(getContext(),this);
            Menu menu = P.getMenu();
            menu.add(0,1,Menu.NONE," 0 dB (Fullscale)");
            menu.add(0,2,Menu.NONE," 6 dB");
            menu.add(0,3,Menu.NONE,"14 dB");
            menu.add(0,4,Menu.NONE,"20 dB");
            menu.add(0,5,Menu.NONE,"26 dB");
            menu.add(0,6,Menu.NONE,"34 dB");
            menu.add(0,7,Menu.NONE,"40 dB");
            menu.add(0,8,Menu.NONE,"46 dB");
            menu.add(0,9,Menu.NONE,"54 dB");
            menu.add(0,10,Menu.NONE,"60 dB");
            P.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case 1:
                            scale = 1;
                            break;
                        case 2:
                            scale = 2;
                            break;
                        case 3:
                            scale = 5;
                            break;
                        case 4:
                            scale = 10;
                            break;
                        case 5:
                            scale = 20;
                            break;
                        case 6:
                            scale = 50;
                            break;
                        case 7:
                            scale = 100;
                            break;
                        case 8:
                            scale = 200;
                            break;
                        case 9:
                            scale = 500;
                            break;
                        case 10:
                            scale = 1000;
                            break;
                    }
                    SharedPreferences.Editor e = WavePreferences.edit();
                    e.putInt("WaveViewScale", scale);
                    e.apply();
                    return false;
                }
            });
            P.show();
        }
        return true;
    }


    public void setup(Context context) {
        // int height=getHeight();

        float stdsize = new Button(context).getTextSize();
        scale=1;
        WavePreferences=null;
        PrefListener=null;
        paint_text=new Paint();
        paint_text.setColor(Color.WHITE);
        paint_text.setTextAlign(Paint.Align.LEFT);
        paint_text.setTextSize(stdsize * 0.75f);
        fontyofs=stdsize*0.75f;
    }

    public WaveView(Context context) {
        super(context);
        setup(context);
    }

    public WaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context);
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (helper==null) return;

        int width=canvas.getWidth();
        int height=canvas.getHeight();

        if (helper.sharedMap == null) {
            // Initialize
            helper.WaveViewInit(width,height);
            if (helper.sharedMap == null)
                return;
        }

        helper.WaverViewProcess(dataConsolidator.wave, scale);

        canvas.drawBitmap(helper.sharedMap, 0, 0, null);
        canvas.drawLine(0, 0, 20f, 0, paint_text);
        canvas.drawLine(width-20f, 0, width-1f, 0, paint_text);

        canvas.drawLine(0, height / 2, 20f, height / 2, paint_text);
        canvas.drawLine(width-20f, height / 2, width-1f, height / 2, paint_text);

        canvas.drawLine(0,height-1,20f,height-1,paint_text);
        canvas.drawLine(width-20f,height-1,width-1f,height-1,paint_text);

        canvas.drawText(String.format("%1.3f", 1.0f / (float) scale), 20f, fontyofs, paint_text);
        canvas.drawText(String.format("%1.3f",-1.0f/(float)scale),20f,height-1,paint_text);
    }

    public void display(DataConsolidator dc) {
        dataConsolidator=dc;
        invalidate();
    }



}