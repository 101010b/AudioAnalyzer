package com.alphadraco.audioanalyzer;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
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
    public AudioAnalyzer root;
    // public AudioAnalyzerHelper helper;
    // private DataConsolidator dataConsolidator;
    private int scale;
    private float fontyofs;
    private int xofs;




    public void setPreferences(AudioAnalyzer _rt,  SharedPreferences prefs) {
        WavePreferences=prefs;
        root=_rt;
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
        if ((root.dataConsolidator != null) && (e.getAction()==MotionEvent.ACTION_DOWN)) {
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
        paint_text.setTextAlign(Paint.Align.RIGHT);
        paint_text.setTextSize(stdsize * 0.75f);
        paint_text.setStyle(Paint.Style.STROKE);
        fontyofs=stdsize*0.75f;

        Rect rct=new Rect();
        String Sw="-0.0000";
        paint_text.getTextBounds(Sw, 0, Sw.length(), rct);
        xofs=rct.width()+rct.height()/10;
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
        if (root == null) return;
        if (root.audioAnalyzerHelper==null) return;
        // if (root.audioAnalyzerHelper.sharedMap==null) return;
        if (root.dataConsolidator==null) return;
        if (root.dataConsolidator.wave == null) return;
        if (root.audioAnalyzerHelper.sharedMap == null) return;

        int width=canvas.getWidth();
        int height=canvas.getHeight();

        /*
        if (root.audioAnalyzerHelper.sharedMap == null) {
            // Initialize
            root.audioAnalyzerHelper.WaveViewInit(width-2-xofs,height-2);
            if (root.audioAnalyzerHelper.sharedMap == null)
                return;
        }

        root.audioAnalyzerHelper.WaverViewProcess(root.dataConsolidator.wave, scale);
        */
        root.audioAnalyzerHelper.WaveViewCopyMapToBitmap();

        canvas.drawBitmap(root.audioAnalyzerHelper.sharedMap, xofs + 1, 1, null);
        canvas.drawRect(xofs, 0, width - 1, height - 1, paint_text);


        //canvas.drawLine(0, 0, 20f, 0, paint_text);
        //canvas.drawLine(width-20f, 0, width-1f, 0, paint_text);

        //canvas.drawLine(0, height / 2, 20f, height / 2, paint_text);
        //canvas.drawLine(width-20f, height / 2, width-1f, height / 2, paint_text);

        //canvas.drawLine(0,height-1,20f,height-1,paint_text);
        //canvas.drawLine(width-20f,height-1,width-1f,height-1,paint_text);

        canvas.drawText(String.format("%1.3f", 1.0f / (float) scale), xofs-5, fontyofs, paint_text);
        canvas.drawText(String.format("%1.3f",-1.0f/(float)scale),xofs-5,height-1,paint_text);
    }

    public void add() {
        if (root == null) return;
        if (root.audioAnalyzerHelper==null) return;
        // if (root.audioAnalyzerHelper.sharedMap==null) return;
        if (root.dataConsolidator==null) return;
        if (root.dataConsolidator.wave == null) return;

        int width=getWidth();
        int height=getHeight();

        if (root.audioAnalyzerHelper.sharedMap == null) {
            // Initialize
            root.audioAnalyzerHelper.WaveViewInit(width-2-xofs,height-2);
            if (root.audioAnalyzerHelper.sharedMap == null)
                return;
        }

        root.audioAnalyzerHelper.WaverViewProcess(root.dataConsolidator.wave, scale);

    }

    public void display() {
        invalidate();
    }



}
