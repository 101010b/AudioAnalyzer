package com.alphadraco.audioanalyzer;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
// import android.support.annotation.DimenRes;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;

/**
 * Created by aladin on 22.09.2015.
 */
public class LevelBar  extends View {
    Paint paint_dk_green = new Paint();
    Paint paint_dk_yellow = new Paint();
    Paint paint_dk_red = new Paint();
    Paint paint_green = new Paint();
    Paint paint_yellow = new Paint();
    Paint paint_red = new Paint();
    Paint paint_peak = new Paint();
    Paint paint_levels = new Paint();
    Paint paint_levels_frame = new Paint();
    Paint paint_levels_fixed_frame=new Paint();
    Paint paint_text_current = new Paint();
    Paint paint_text_peak = new Paint();
    Paint paint_text_unit = new Paint();
    Paint paint_text_hint = new Paint();

    // DataConsolidator dataConsolidator;
    PowerTrack powerTrack;
    PowerTrack peakPowerTrack;

    float lmin;
    float lmax;
    int steps;

    float frontSpace;
    float frontY;
    float fontHeight;
    float backSpace;
    float valueWidth;
    float hintWidth;

    int showactual;
    boolean fixedmode;

    int intmode;

    float ofs;
    String unit;

    SharedPreferences LevelPreferences;
    int LevelIndex;
    SharedPreferences.OnSharedPreferenceChangeListener PrefListener;

    AudioAnalyzer root;

    public void setPreferences(AudioAnalyzer _rt, SharedPreferences prefs, int idx) {
        LevelPreferences=prefs;
        LevelIndex=idx;
        root=_rt;
        if (LevelPreferences != null) {
            intmode = prefs.getInt(String.format("LevelView%dMode",LevelIndex), 0);
            showactual = prefs.getInt(String.format("LevelView%dLevel",LevelIndex), 0);
            PrefListener=new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                    if (key.equals(String.format("LevelView%dMode",LevelIndex)))
                        intmode = prefs.getInt(key, 0);
                    if (key.equals(String.format("LevelView%dLevel",LevelIndex)))
                        showactual = prefs.getInt(key, 0);
                }
            };
            LevelPreferences.registerOnSharedPreferenceChangeListener(PrefListener);
        } else
            PrefListener=null;
    }

    public void setup(Context context) {
        // int height=getHeight();
        LevelPreferences=null;
        PrefListener=null;
        LevelIndex=0;


        paint_dk_green.setColor(Color.argb(255, 0, 64, 0));paint_dk_green.setStyle(Paint.Style.FILL);
        paint_green.setColor(Color.argb(255, 0, 255, 0));paint_green.setStyle(Paint.Style.FILL);

        paint_dk_yellow.setColor(Color.argb(255, 64, 64, 0));paint_dk_yellow.setStyle(Paint.Style.FILL);
        paint_yellow.setColor(Color.argb(255, 255, 255, 0));paint_yellow.setStyle(Paint.Style.FILL);

        paint_dk_red.setColor(Color.argb(255, 64, 0, 0));paint_dk_red.setStyle(Paint.Style.FILL);
        paint_red.setColor(Color.argb(255, 255, 0, 0));paint_red.setStyle(Paint.Style.FILL);

        paint_peak.setColor(Color.argb(255, 255, 0, 0));
        paint_peak.setStyle(Paint.Style.STROKE);

        paint_levels.setColor(Color.argb(255, 255, 255, 255));
        paint_levels.setTextAlign(Paint.Align.LEFT);
        float stdsize = new Button(context).getTextSize();
        paint_levels.setTextSize(stdsize);

        paint_levels_frame.setColor(Color.argb(255, 0, 255, 0));
        paint_levels_frame.setStyle(Paint.Style.STROKE);

        paint_levels_fixed_frame.setColor(Color.argb(255, 128, 128, 128));
        paint_levels_fixed_frame.setStyle(Paint.Style.STROKE);

        paint_text_peak.setColor(Color.argb(255, 255, 0, 0));
        paint_text_peak.setTextAlign(Paint.Align.LEFT);
        paint_text_peak.setTextSize(stdsize);

        paint_text_current.setColor(Color.argb(255, 0, 255, 0));
        paint_text_current.setTextAlign(Paint.Align.RIGHT);
        paint_text_current.setTextSize(stdsize);

        paint_text_unit.setColor(Color.argb(255, 0, 255, 0));
        paint_text_unit.setTextAlign(Paint.Align.LEFT);
        paint_text_unit.setTextSize(stdsize/2);

        paint_text_hint.setColor(Color.argb(255, 127, 127, 127));
        paint_text_hint.setTextAlign(Paint.Align.LEFT);
        paint_text_hint.setTextSize(stdsize/2);


        String testtext="A100-20k_";
        Rect bounds=new Rect();
        paint_levels.getTextBounds(testtext, 0, testtext.length(), bounds);
        frontSpace=bounds.width();
        fontHeight=bounds.height();

        testtext="-120.0_";
        bounds=new Rect();
        paint_text_current.getTextBounds(testtext, 0, testtext.length(), bounds);
        valueWidth=bounds.width();

        testtext="Peak Pk Hld_";
        bounds=new Rect();
        paint_text_hint.getTextBounds(testtext, 0, testtext.length(), bounds);
        hintWidth=bounds.width();

        backSpace=valueWidth+hintWidth;


        lmin=-120;
        lmax=0;

        steps=(int)Math.floor((lmax-lmin)/2.5+0.5);
        intmode=0;

        showactual=0;

        fixedmode=false;

        root=null;
        // dataConsolidator=null;
        powerTrack=null;
        peakPowerTrack=null;

        unit="dBFS";
        ofs=0.0f;

    }

    public LevelBar(Context context) {
        super(context);
        setup(context);
    }

    public LevelBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context);
    }

    private void showModeMenu() {
        if (root.dataConsolidator==null) return;
        PopupMenu P = new PopupMenu(getContext(),this);
        Menu menu = P.getMenu();
        for (int i=0;i<root.dataConsolidator.powerTrackLongNames.length;i++) {
            menu.add(0,i+1,Menu.NONE,root.dataConsolidator.powerTrackLongNames[i]);
        }
        P.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id < 1) return false;
                intmode = id - 1;
                powerTrack = root.dataConsolidator.powerTracks[intmode];
                SharedPreferences.Editor e = LevelPreferences.edit();
                e.putInt(String.format("LevelView%dMode", LevelIndex), intmode);
                e.apply();
                return false;
            }
        });
        P.show();
    }

    private void showLevelMenu() {
        if (root.dataConsolidator == null) return;
        PopupMenu P = new PopupMenu(getContext(),this, Gravity.RIGHT);
        Menu menu = P.getMenu();
        menu.add(0,1,Menu.NONE,"RMS, Current");
        menu.add(0,2,Menu.NONE,"RMS, Average");
        menu.add(0, 3, Menu.NONE, "RMS, Peak Hold");
        if (peakPowerTrack != null) {
            menu.add(0, 4, Menu.NONE, "Peak, Current");
            menu.add(0, 5, Menu.NONE, "Peak, Average");
            menu.add(0, 6, Menu.NONE, "Peak, Peak Hold");
        }
        P.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case 1: showactual=0;break;
                    case 2: showactual=1;break;
                    case 3: showactual=2;break;
                    case 4: showactual=3;break;
                    case 5: showactual=4;break;
                    case 6: showactual=5;break;
                }
                SharedPreferences.Editor e = LevelPreferences.edit();
                e.putInt(String.format("LevelView%dLevel", LevelIndex), showactual);
                e.apply();
                return false;
            }
        });
        P.show();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if ((root.dataConsolidator != null) && (e.getAction()==MotionEvent.ACTION_DOWN)) {
            float x=e.getX();
            float y=e.getY();
            if (x < frontSpace) {
                if (!fixedmode) {
                    showModeMenu();
                    /*if (intmode < root.dataConsolidator.powerTracks.length - 1) intmode++;
                    else intmode = 0;
                    powerTrack = root.dataConsolidator.powerTracks[intmode];
                    if (LevelPreferences != null) {
                        SharedPreferences.Editor E=LevelPreferences.edit();
                        E.putInt(String.format("LevelView%dMode",LevelIndex),intmode);
                        E.apply();
                    }*/
                }
            } else if (x < getWidth()-backSpace) {
                if (powerTrack != null) {
                    powerTrack.peak=powerTrack.current;
                }
                if (peakPowerTrack != null) {
                    peakPowerTrack.peak=peakPowerTrack.current;
                }
            } else {
                showLevelMenu();
                /*if (powerTrack == null)
                    showactual=0;
                else if (peakPowerTrack==null) {
                    showactual++;
                    if (showactual > 2) showactual=0;
                } else {
                    showactual++;
                    if (showactual > 5) showactual=0;
                }*/
            }
        }
        return true;
    }

    public String getFstring(float f) {
        float af=Math.abs(f);
        if (f==0) return "0Hz";
        if (af < 0.01) return String.format("%2.0fmHz",f*1000.0f);
        if (af < 0.1) return String.format("%1.2fHz",f);
        if (af < 1) return String.format("%1.2fHz",f);
        if (af < 10) return String.format("%1.1fHz",f);
        if (af < 100) return String.format("%2.0fHz",f);
        if (af < 1000) return String.format("%3.0fHz",f);
        if (af < 10000) return String.format("%1.2fkHz",f/1000.0f);
        if (af < 100000) return String.format("%2.1fkHz",f/1000.0f);
        if (af < 1000000) return String.format("%3.0fkHz",f/1000.0f);
        return String.format("%1.0fMHz",f/1000000.0f);
    }

    public void drawCornerRect(Canvas canvas, int left, int top, int right, int bottom, Paint p) {
        int rw=10;
        float[] pts=
                {
                    left,top+rw,left,top,
                    left,top,left+rw,top,
                    right-rw,top,right,top,
                    right,top,right,top+rw,
                    right,bottom-rw,right,bottom,
                    right,bottom,right-rw,bottom,
                    left+rw,bottom,left,bottom,
                    left,bottom,left,bottom-rw,
                };

        canvas.drawLines(pts,0,pts.length,p);
    }

    @Override
    public void onDraw(Canvas canvas) {
        int width=canvas.getWidth();
        int height=canvas.getHeight();

        float barWidth=(float)((width-frontSpace-backSpace)/(steps+1));

        if ((powerTrack != null)) {
            if (powerTrack.name.equals("Track")) {
                String s="Track";
                if (root.dataConsolidator.trackf > 0) {
                    s=getFstring(root.dataConsolidator.trackf);
                }
                canvas.drawText(s, 10, height / 2.0f + fontHeight / 2.0f, paint_levels);
            } else
                canvas.drawText(powerTrack.name, 10, height / 2.0f + fontHeight / 2.0f, paint_levels);
            if (!fixedmode) {
                drawCornerRect(canvas, 1, 5, (int)frontSpace - 1, height - 5, paint_levels_frame);
                // canvas.drawRect(1, 5, frontSpace - 1, height - 5, paint_levels_frame);
            }

            /*
            if (fixedmode)
                canvas.drawRoundRect(1,5,frontSpace-1,height-5,10,10,paint_levels_fixed_frame);
            else
                canvas.drawRoundRect(1,5,frontSpace-1,height-5,10,10,paint_levels_frame);
            */
            // canvas.drawRoundRect(0,height/2-fontHeight/2-5,frontSpace-1,height/2+fontHeight/2+5,10,10,paint_levels_frame);
        }

        float eol = -1;
        if (powerTrack != null)
            eol = frontSpace+(powerTrack.current-lmin)/(lmax-lmin)*(width - frontSpace-backSpace);
        float sepg=frontSpace;
        float sepgy = frontSpace+(-20-lmin)/(lmax-lmin)*(width - frontSpace-backSpace);
        float sepyr = frontSpace+(-3-lmin)/(lmax-lmin)*(width - frontSpace-backSpace);
        float sepr=width-backSpace;
        float y1=(height-barWidth)/2;
        float y2=(height+barWidth)/2;
        if (peakPowerTrack != null) {
            y2=height/2-1;
        }

        // RMS display
        if (eol <= frontSpace) {
            canvas.drawRect(sepg,y1,sepgy,y2,paint_dk_green);
            canvas.drawRect(sepgy,y1,sepyr,y2,paint_dk_yellow);
            canvas.drawRect(sepyr,y1,sepr,y2,paint_dk_red);
        } else if (eol < sepgy) {
            canvas.drawRect(sepg,y1,eol,y2,paint_green);
            canvas.drawRect(eol,y1,sepgy,y2,paint_dk_green);
            canvas.drawRect(sepgy,y1,sepyr,y2,paint_dk_yellow);
            canvas.drawRect(sepyr,y1,sepr,y2,paint_dk_red);
        } else if (eol < sepyr) {
            canvas.drawRect(sepg,y1,sepgy,y2,paint_green);
            canvas.drawRect(sepgy,y1,eol,y2,paint_yellow);
            canvas.drawRect(eol,y1,sepyr,y2,paint_dk_yellow);
            canvas.drawRect(sepyr,y1,sepr,y2,paint_dk_red);
        } else if (eol < sepr) {
            canvas.drawRect(sepg,y1,sepgy,y2,paint_green);
            canvas.drawRect(sepgy,y1,sepyr,y2,paint_yellow);
            canvas.drawRect(sepyr,y1,eol,y2,paint_red);
            canvas.drawRect(eol,y1,sepr,y2,paint_dk_red);
        } else {
            canvas.drawRect(sepg,y1,sepgy,y2,paint_green);
            canvas.drawRect(sepgy,y1,sepyr,y2,paint_yellow);
            canvas.drawRect(sepyr,y1,sepr,y2,paint_red);
        }

        // RMS Average
        eol=-1;
        if (powerTrack != null)
            eol = frontSpace+(powerTrack.average-lmin)/(lmax-lmin)*(width - frontSpace-backSpace);
        if (eol <= frontSpace)
            canvas.drawRect(sepg-3,y1-8,sepg+3,y1-1,paint_green);
        else if (eol < sepgy)
            canvas.drawRect(eol-3,y1-8,eol+3,y1-1,paint_green);
        else if (eol < sepyr)
            canvas.drawRect(eol-3,y1-8,eol+3,y1-1,paint_yellow);
        else if (eol < sepr)
            canvas.drawRect(eol-3,y1-8,eol+3,y1-1,paint_red);
        else
            canvas.drawRect(sepr-3,y1-8,sepr+3,y1-1,paint_red);

        // RMS Peak follow
        eol=-1;
        if (powerTrack != null)
            eol = frontSpace+(powerTrack.peakFollow-lmin)/(lmax-lmin)*(width - frontSpace-backSpace);
        if (eol <= frontSpace) {
            canvas.drawRect(sepg-1,y1,sepg+1,y2,paint_green);
        } else if (eol < sepgy) {
            canvas.drawRect(eol-1,y1,eol+1,y2,paint_green);
        } else if (eol < sepyr) {
            canvas.drawRect(eol-1,y1,eol+1,y2,paint_yellow);
        } else if (eol < sepr) {
            canvas.drawRect(eol-1,y1,eol+1,y2,paint_red);
        } else {
            canvas.drawRect(sepr-1,y1,sepr+1,y2,paint_red);
        }

        if (peakPowerTrack != null) {
            y1=height/2+1;
            y2=(height+barWidth)/2;
            eol = frontSpace+(peakPowerTrack.current-lmin)/(lmax-lmin)*(width - frontSpace-backSpace);

            // Peak display
            if (eol <= frontSpace) {
                canvas.drawRect(sepg,y1,sepgy,y2,paint_dk_green);
                canvas.drawRect(sepgy,y1,sepyr,y2,paint_dk_yellow);
                canvas.drawRect(sepyr,y1,sepr,y2,paint_dk_red);
            } else if (eol < sepgy) {
                canvas.drawRect(sepg,y1,eol,y2,paint_green);
                canvas.drawRect(eol,y1,sepgy,y2,paint_dk_green);
                canvas.drawRect(sepgy,y1,sepyr,y2,paint_dk_yellow);
                canvas.drawRect(sepyr,y1,sepr,y2,paint_dk_red);
            } else if (eol < sepyr) {
                canvas.drawRect(sepg,y1,sepgy,y2,paint_green);
                canvas.drawRect(sepgy,y1,eol,y2,paint_yellow);
                canvas.drawRect(eol,y1,sepyr,y2,paint_dk_yellow);
                canvas.drawRect(sepyr,y1,sepr,y2,paint_dk_red);
            } else if (eol < sepr) {
                canvas.drawRect(sepg,y1,sepgy,y2,paint_green);
                canvas.drawRect(sepgy,y1,sepyr,y2,paint_yellow);
                canvas.drawRect(sepyr,y1,eol,y2,paint_red);
                canvas.drawRect(eol,y1,sepr,y2,paint_dk_red);
            } else {
                canvas.drawRect(sepg,y1,sepgy,y2,paint_green);
                canvas.drawRect(sepgy,y1,sepyr,y2,paint_yellow);
                canvas.drawRect(sepyr,y1,sepr,y2,paint_red);
            }

            // Peak Average
            eol=-1;
            if (powerTrack != null)
                eol = frontSpace+(peakPowerTrack.average-lmin)/(lmax-lmin)*(width - frontSpace-backSpace);
            if (eol <= frontSpace)
                canvas.drawRect(sepg-3,y2+1,sepg+3,y2+8,paint_green);
            else if (eol < sepgy)
                canvas.drawRect(eol-3,y2+1,eol+3,y2+8,paint_green);
            else if (eol < sepyr)
                canvas.drawRect(eol-3,y2+1,eol+3,y2+8,paint_yellow);
            else if (eol < sepr)
                canvas.drawRect(eol-3,y2+1,eol+3,y2+8,paint_red);
            else
                canvas.drawRect(sepr-3,y2+1,sepr+3,y2+8,paint_red);

            // Peak follow
            eol=-1;
            if (powerTrack != null)
                eol = frontSpace+(peakPowerTrack.peakFollow-lmin)/(lmax-lmin)*(width - frontSpace-backSpace);
            if (eol <= frontSpace) {
                canvas.drawRect(sepg-1,y1,sepg+1,y2,paint_green);
            } else if (eol < sepgy) {
                canvas.drawRect(eol-1,y1,eol+1,y2,paint_green);
            } else if (eol < sepyr) {
                canvas.drawRect(eol-1,y1,eol+1,y2,paint_yellow);
            } else if (eol < sepr) {
                canvas.drawRect(eol-1,y1,eol+1,y2,paint_red);
            } else {
                canvas.drawRect(sepr-1,y1,sepr+1,y2,paint_red);
            }

        }

        if (powerTrack != null) {
            if ((powerTrack.peak >= lmin) && (powerTrack.peak <= lmax)) {
                float xc=frontSpace+(powerTrack.peak-lmin)/(lmax-lmin)*(width-frontSpace-backSpace);
                canvas.drawLine(xc-8, (height-barWidth)/2-10, xc+8, (height-barWidth)/2-10, paint_peak);
                canvas.drawLine(xc-8, (height-barWidth)/2-10, xc, (height-barWidth)/2-2, paint_peak);
                canvas.drawLine(xc, (height-barWidth)/2-2, xc+8, (height-barWidth)/2-10, paint_peak);
            }
            if (peakPowerTrack != null) {
                float xc=frontSpace+(peakPowerTrack.peak-lmin)/(lmax-lmin)*(width-frontSpace-backSpace);
                canvas.drawLine(xc-8, (height+barWidth)/2+10, xc+8, (height+barWidth)/2+10, paint_peak);
                canvas.drawLine(xc-8, (height+barWidth)/2+10, xc, (height+barWidth)/2+2, paint_peak);
                canvas.drawLine(xc, (height+barWidth)/2+2, xc+8, (height+barWidth)/2+10, paint_peak);
            }
        }

        if (powerTrack != null) {
            String hint="";
            String value="";
            switch (showactual) {
                case 0: // RMS Current
                    value=String.format("%2.1f", powerTrack.current+ofs);
                    hint="RMS";
                    break;
                case 1: // RMS Average
                    value=String.format("%2.1f", powerTrack.average+ofs);
                    hint="RMS Avg";
                    break;
                case 2: // RMS Peak hold
                    value=String.format("%2.1f", powerTrack.peak+ofs);
                    hint="RMS Pk Hld";
                    break;
                case 3: // Peak current
                    value=String.format("%2.1f", peakPowerTrack.current+ofs);
                    hint="Peak";
                    break;
                case 4: // Peak Average
                    value=String.format("%2.1f", peakPowerTrack.average+ofs);
                    hint="Peak Avg";
                    break;
                case 5: // Peak hold
                    value=String.format("%2.1f", peakPowerTrack.peak+ofs);
                    hint="Peak Pk Hld";
                    break;
            }
            canvas.drawText(value,width-hintWidth-1,height/2.0f+fontHeight/2.0f,paint_text_current);
            canvas.drawText(unit,width-hintWidth+1,height/2.0f,paint_text_unit);
            canvas.drawText(hint,width-hintWidth+1,height/2.0f+fontHeight/2.0f,paint_text_hint);
            drawCornerRect(canvas, width - (int) backSpace, 5, width - 1, height - 5, paint_levels_frame);

        }
    }

    public void add() {
    }

    public void display() {
        if ((root == null) || (root.dataConsolidator == null)) {
            powerTrack=null;
            peakPowerTrack=null;
            return;
        }
        powerTrack = root.dataConsolidator.powerTracks[intmode];
        peakPowerTrack = null;
        if (fixedmode) {
            for (PowerTrack p:root.dataConsolidator.powerTracks)
                if (p.name.equals("Peak")) {
                    peakPowerTrack=p;
                }
        }
        invalidate();
    }



}
