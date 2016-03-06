package com.alphadraco.audioanalyzer;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Shader;
import android.provider.ContactsContract;
// import android.support.annotation.DimenRes;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;

/**
 * Created by aladin on 19.09.2015.
 */




public class SpectralView  extends View {
    Paint paint_grid = new Paint();
    Paint paint_gridy = new Paint();
    Paint paint_subgrid = new Paint();
    Paint paint_fft = new Paint();
    Paint paint_avg = new Paint();
    Paint paint_max = new Paint();
    Paint paint_fft_mark = new Paint();
    Paint paint_avg_mark = new Paint();
    Paint paint_max_mark = new Paint();
    Paint paint_frame = new Paint();
    Paint paint_note = new Paint();
    Paint paint_colorBarFrame = new Paint();
    Paint paint_mark=new Paint();
    Paint paint_bar=null;
    Paint paint_bar_avg=new Paint();
    Paint paint_bar_peak=new Paint();


    Paint textY = new Paint();
    Paint textX = new Paint();
    Paint textYL = new Paint();

    // Cache for Points
    float [] line1;
    float [] line2;
    float [] line3;

    AudioAnalyzer root;

    float[] logf;

    float fmin;
    float fmax;
    float lmin;
    float lmax;

    float trackf;
    Path trackfPath=new Path();

    int pointers;
    int trackmode;
    float PT0X,PT0Y;
    float PT1X,PT1Y;
    float storeLmin,storeLmax;
    float storeFmin,storeFmax;

    boolean gridredraw;

    int xofs,yofs;
    float fctr;
    Rect unitrect;
    Rect maxrect;
    int gridfontheight;
    int fontspace;
    int levelBarHeight;
    int buttonFontHeight;
    int buttonHeight;
    public enum DisplayType { SPEK, WFALL, TERZ };
    DisplayType displayType=DisplayType.SPEK;
    String[] colorTabStrings;
    String colorTabString;
    ColorTable colorTable;

    // Configuration
    boolean islog;
    boolean showAvg;
    boolean showPeak;
    boolean showFFT;
    float ofs=0.0f;
    String unit;
    String note;
    SharedPreferences SpectralPrefs;
    SharedPreferences.OnSharedPreferenceChangeListener PrefListener;

    float terzminf;
    int terzminidx;

    public void setPreferences(AudioAnalyzer _rt, SharedPreferences prefs) {
        SpectralPrefs=prefs;
        root=_rt;
        if (SpectralPrefs != null) {
            lmin = SpectralPrefs.getFloat("LMIN",-120.0f);
            lmax = SpectralPrefs.getFloat("LMAX", 0.0f);
            fmin = SpectralPrefs.getFloat("FMIN", 100.0f);
            fmax = SpectralPrefs.getFloat("FMAX", 20000.0f);
            islog = SpectralPrefs.getBoolean("SpecDisplayLog", true);
            showAvg = SpectralPrefs.getBoolean("SpecDisplayAvg", true);
            showPeak = SpectralPrefs.getBoolean("SpecDisplayPeak", true);
            showFFT = SpectralPrefs.getBoolean("SpecDisplayFFT", true);
            trackf = SpectralPrefs.getFloat("TrackF", -1.0f);
            colorTabString = SpectralPrefs.getString("WaterFallColor", colorTabStrings[0]);
            displayType=DisplayType.values()[SpectralPrefs.getInt("DisplayType", 0)];
            root.terzw=SpectralPrefs.getInt("SpecDisplayTerzW", 0);
            PrefListener=new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                    if (key.equals("SpecDisplayLog")) {
                        boolean isl=prefs.getBoolean(key, true);
                        changeMode(isl);
                    }
                    if (key.equals("SpecDisplayAvg"))
                        showAvg = prefs.getBoolean(key, true);
                    if (key.equals("SpecDisplayPeak"))
                        showPeak = prefs.getBoolean(key, true);
                    if (key.equals("WaterFallColor"))
                        colorTabString=prefs.getString(key,"KrYW");
                }
            };
            SpectralPrefs.registerOnSharedPreferenceChangeListener(PrefListener);
        } else
            PrefListener=null;
    }

    public void setup(Context context) {
        float stdsize = new Button(context).getTextSize();
        PrefListener=null;
        SpectralPrefs=null;
        root=null;

        paint_mark.setColor(Color.BLUE);
        paint_mark.setStyle(Paint.Style.STROKE);
        paint_mark.setPathEffect(new DashPathEffect(new float[]{20, 20}, 0));
        paint_mark.setStrokeWidth(2);

        paint_frame.setColor(Color.WHITE);
        paint_frame.setStyle(Paint.Style.STROKE);
        paint_frame.setTextAlign(Paint.Align.LEFT);

        paint_colorBarFrame.setColor(Color.GRAY);
        paint_colorBarFrame.setStyle(Paint.Style.STROKE);
        paint_colorBarFrame.setTextAlign(Paint.Align.LEFT);

        paint_note.setColor(Color.WHITE);
        paint_note.setStyle(Paint.Style.STROKE);
        paint_note.setTextAlign(Paint.Align.CENTER);
        paint_note.setTextSize(stdsize);

        paint_grid.setColor(Color.GRAY);
        paint_grid.setTextAlign(Paint.Align.CENTER);
        paint_grid.setTextSize(stdsize * 0.75f);

        paint_gridy.setColor(Color.GRAY);
        paint_gridy.setTextAlign(Paint.Align.LEFT);
        paint_gridy.setTextSize(stdsize * 0.75f);

        paint_subgrid.setColor(Color.DKGRAY);

        paint_fft.setColor(Color.GREEN);paint_fft.setStyle(Paint.Style.STROKE);
        paint_fft.setTextAlign(Paint.Align.CENTER);
        paint_avg.setColor(Color.GRAY);paint_avg.setStyle(Paint.Style.STROKE);
        paint_avg.setTextAlign(Paint.Align.CENTER);
        paint_max.setColor(Color.RED);paint_max.setStyle(Paint.Style.STROKE);
        paint_max.setTextAlign(Paint.Align.CENTER);

        paint_fft_mark.setColor(Color.GREEN);paint_fft_mark.setStyle(Paint.Style.STROKE);
        paint_fft_mark.setTextSize(stdsize * 0.75f);paint_fft_mark.setTextAlign(Paint.Align.LEFT);
        paint_avg_mark.setColor(Color.GRAY);paint_avg_mark.setStyle(Paint.Style.STROKE);
        paint_avg_mark.setTextSize(stdsize * 0.75f);paint_avg_mark.setTextAlign(Paint.Align.LEFT);
        paint_max_mark.setColor(Color.RED);paint_max_mark.setStyle(Paint.Style.STROKE);
        paint_max_mark.setTextSize(stdsize * 0.75f);paint_max_mark.setTextAlign(Paint.Align.LEFT);

        paint_bar_avg.setColor(Color.GREEN);
        paint_bar_avg.setStyle(Paint.Style.FILL);
        paint_bar_avg.setTextSize(stdsize);
        paint_bar_avg.setTextAlign(Paint.Align.CENTER);

        paint_bar_peak.setColor(Color.RED);
        paint_bar_peak.setStyle(Paint.Style.STROKE);
        // paint_bar_peak.setStyle(Paint.Style.FILL);
        paint_bar_peak.setTextSize(stdsize);
        paint_bar_peak.setTextAlign(Paint.Align.CENTER);

        textY.setColor(Color.GRAY);
        textY.setTextAlign(Paint.Align.RIGHT);
        textY.setTextSize(stdsize*0.75f);
        String Sw="-100xx";

        textYL.setColor(Color.GRAY);
        textYL.setTextAlign(Paint.Align.LEFT);
        textYL.setTextSize(stdsize*0.75f);

        textX.setColor(Color.GRAY);
        textX.setTextAlign(Paint.Align.CENTER);
        textX.setTextSize(stdsize*0.75f);
        // String Sh="20k";

        Rect rct=new Rect();
        textX.getTextBounds(Sw,0,Sw.length(),rct);
        fctr=rct.height();
        gridfontheight=rct.height();
        fontspace=gridfontheight/5;
        xofs=rct.width()+fontspace;
        yofs =gridfontheight+fontspace;
        levelBarHeight=3*gridfontheight;

        paint_frame.setTextSize(stdsize);
        paint_max.setTextSize(stdsize);
        paint_avg.setTextSize(stdsize);
        paint_fft.setTextSize(stdsize);

        Sw="dBSPL(C)";
        paint_frame.getTextBounds(Sw, 0, Sw.length(), rct);
        unitrect=new Rect(rct);

        unitrect.offset((int) xofs, 5 + rct.height());
        paint_max.getTextBounds("_MAX_", 0, 5, rct);
        buttonHeight=rct.height()*3;
        buttonFontHeight=rct.height();
        maxrect=new Rect(rct);
        maxrect.offset(0, 5 + rct.height());

        if (isInEditMode()) {
            colorTabStrings=new String[1];
            colorTabStrings[0]="krYW";
        } else {
            colorTabStrings = getResources().getStringArray(R.array.pref_waterfall_colorscheme);
        }
        colorTabString=colorTabStrings[0];
        colorTable=new ColorTable(256,colorTabString);

        fmin=100;
        fmax=22050;
        lmin=-100;
        lmax=0;
        islog=true;
        showAvg=true;
        showPeak=true;
        showFFT=true;

        ofs=0.0f;
        unit="dBFS";

        trackf=-1;

        gridredraw=true;

        pointers=0;
        PT0X=PT0Y=PT1X=PT1Y=0;
        storeLmin=storeLmax=storeFmin=storeFmax=0;
        note="";

        line1=line2=line3=null;

        // displayType=DisplayType.SPEK;

    }

    public SpectralView(Context context) {
        super(context);
        setup(context);
    }

    public SpectralView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context);
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

        canvas.drawLines(pts, 0, pts.length, p);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (root == null) return true;

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                storeLmin=lmin;
                storeLmax=lmax;
                storeFmin=fmin;
                storeFmax=fmax;
                float trackfpos=0;
                if (islog) {
                    trackfpos= (float) (xofs + (float) (Math.log(trackf/fmin) / Math.log(fmax / fmin) * (getWidth() - 1 - xofs)));
                } else {
                    trackfpos= (float) xofs + (float) ((trackf - fmin) / (fmax - fmin) * (getWidth() - 1 - xofs));
                }

                // First down
                PT0X = e.getX(0);
                PT0Y = e.getY(0);
                // Check for active Areas first
                switch (displayType) {
                    case WFALL:
                        // Color Bar
                        if (PT0Y < levelBarHeight) {
                            showColorMenu();
                            return true;
                        }
                        // X-Axis
                        if (PT0Y > getHeight()-2*yofs) {
                            islog=!islog;
                            if (islog && (fmin < 1)) {
                                fmin = 1;
                                if (fmax < fmin+100)
                                    fmax=fmin+100;
                            }
                            // Store setup
                            SharedPreferences.Editor E=SpectralPrefs.edit();
                            E.putBoolean("SpecDisplayLog",islog);
                            E.putFloat("LMIN", lmin);
                            E.putFloat("LMAX",lmax);
                            E.putFloat("FMIN",fmin);
                            E.putFloat("FMAX",fmax);
                            E.apply();
                            return true;
                        }
                        break;
                    case SPEK:
                        // Line Selection
                        if (PT0X > getWidth()-maxrect.width()) {
                            if (PT0Y < 5 + buttonHeight) {
                                showPeak = !showPeak;
                                if (showPeak)
                                    root.dataConsolidator.reset();
                                SharedPreferences.Editor E=SpectralPrefs.edit();
                                E.putBoolean("SpecDisplayPeak",showPeak);
                                E.apply();
                                return true;
                            } else if (PT0Y < 5 + buttonHeight * 2) {
                                showAvg = !showAvg;
                                SharedPreferences.Editor E=SpectralPrefs.edit();
                                E.putBoolean("SpecDisplayAvg",showAvg);
                                E.apply();
                                return true;
                            } else if (PT0Y < 5 + buttonHeight * 3) {
                                showFFT=!showFFT;
                                SharedPreferences.Editor E=SpectralPrefs.edit();
                                E.putBoolean("SpecDisplayFFT",showFFT);
                                E.apply();
                                return true;
                            }
                        }
                        // X-Axis
                        if (PT0Y > getHeight()-2*yofs) {
                            islog=!islog;
                            if (islog && (fmin < 1)) {
                                fmin = 1;
                                if (fmax < fmin+100)
                                    fmax=fmin+100;
                            }
                            // Store setup
                            SharedPreferences.Editor E=SpectralPrefs.edit();
                            E.putBoolean("SpecDisplayLog",islog);
                            E.putFloat("LMIN", lmin);
                            E.putFloat("LMAX",lmax);
                            E.putFloat("FMIN",fmin);
                            E.putFloat("FMAX",fmax);
                            E.apply();
                            return true;
                        }
                        break;
                    case TERZ:
                        if ((PT0X > unitrect.left) && (PT0X < unitrect.right+40) &&
                                (PT0Y > 5) && (PT0Y < 5 + buttonHeight)) {
                            switch (root.terzw) {
                                case 0: root.terzw=1;break;
                                case 1: root.terzw=2;break;
                                case 2: root.terzw=3;break;
                                case 3: root.terzw=0;break;
                            }
                            SharedPreferences.Editor E=SpectralPrefs.edit();
                            E.putInt("SpecDisplayTerzW",root.terzw);
                            E.apply();
                            return true;
                        } else if (PT0X > getWidth()-maxrect.width()) {
                            if (PT0Y < 5 + buttonHeight) {
                                showPeak=!showPeak;
                                if (showPeak)
                                    root.dataConsolidator.reset();
                                SharedPreferences.Editor E=SpectralPrefs.edit();
                                E.putBoolean("SpecDisplayPeak",showPeak);
                                E.apply();
                                return true;
                            }
                            if (PT0Y < 5 + buttonHeight * 2) {
                                showAvg = !showAvg;
                                SharedPreferences.Editor E=SpectralPrefs.edit();
                                E.putBoolean("SpecDisplayAvg", showAvg);
                                E.apply();
                                return true;
                            }
                        }
                        break;

                }
                pointers++;

                // Track Mode
                if ((displayType == DisplayType.SPEK) && (e.getX() > trackfpos-buttonHeight/2) && (e.getX() < trackfpos+buttonHeight/2)) {
                    // Special Case: Drag frequency Cursor
                    if (e.getPointerCount() == 1) {
                        trackmode = 1;
                        return true;
                    }
                }

                // Standard Mode
                trackmode=0;
                if (e.getPointerCount()==2) {
                    PT1X=e.getX(1);
                    PT1Y=e.getY(1);
                    pointers++;
                }
                if (e.getPointerCount() > 2)
                    pointers=0;
                break;

            case MotionEvent.ACTION_UP:
                if (pointers==1) {
                    if (trackmode == 1) {
                        // Cursor Track
                        SharedPreferences.Editor E = SpectralPrefs.edit();
                        E.putFloat("TrackF", trackf);
                        E.apply();
                        pointers=0;
                        return true;
                    }
                }
                if ((pointers > 0) && (SpectralPrefs != null)) {
                    // Store Data
                    SharedPreferences.Editor E=SpectralPrefs.edit();
                    E.putFloat("LMIN",lmin);
                    E.putFloat("LMAX",lmax);
                    E.putFloat("FMIN",fmin);
                    E.putFloat("FMAX",fmax);
                    E.apply();
                }
            case MotionEvent.ACTION_CANCEL:
                pointers=0;
                break;



            case MotionEvent.ACTION_MOVE:
                // Break when more than two fingers...
                if (e.getPointerCount() > 2)
                    pointers=0;

                // Add more pointers?
                if ((e.getPointerCount()==2) && (pointers == 1)) {
                    if (trackmode == 0) {
                        PT1X = e.getX(1);
                        PT1Y = e.getY(1);
                        pointers = 2;
                    } else {
                        pointers=0; // break it
                    }
                }

                if (pointers==1) {
                    // One Finger Track
                    if (trackmode == 0) {
                        // Move Screen Track
                        float dx = e.getX() - PT0X;
                        float dy = e.getY() - PT0Y;
                        float dl;
                        switch (displayType) {
                            case SPEK:
                                if (islog) {
                                    float df = (float) (-dx * (Math.log(storeFmax) - Math.log(storeFmin)) / (getWidth() - 1 - xofs - 1));
                                    fmin = (float) Math.exp(Math.log(storeFmin) + df);
                                    fmax = (float) Math.exp(Math.log(storeFmax) + df);
                                } else {
                                    float df = -dx * (storeFmax - storeFmin) / (getWidth() - 1 - xofs - 1);
                                    fmin = storeFmin + df;
                                    fmax = storeFmax + df;
                                }
                                dl = dy * (storeLmax - storeLmin) / (getHeight() - 1 - yofs - 1);
                                lmin = storeLmin + dl;
                                lmax = storeLmax + dl;
                                break;
                            case WFALL:
                                if (islog) {
                                    float df = (float) (-dx * (Math.log(storeFmax) - Math.log(storeFmin)) / (getWidth() - 1 - xofs - 1));
                                    fmin = (float) Math.exp(Math.log(storeFmin) + df);
                                    fmax = (float) Math.exp(Math.log(storeFmax) + df);
                                } else {
                                    float df = -dx * (storeFmax - storeFmin) / (getWidth() - 1 - xofs - 1);
                                    fmin = storeFmin + df;
                                    fmax = storeFmax + df;
                                }
                                break;
                            case TERZ:
                                dl = dy * (storeLmax - storeLmin) / (getHeight() - 1 - yofs - 1);
                                lmin = storeLmin + dl;
                                lmax = storeLmax + dl;
                                paint_bar=null;
                                break;
                        }
                    } else {
                        // Move Frequency Cursor
                        float ttf=0;
                        if (islog) {
                            ttf=(float)Math.exp((e.getX()-xofs)/(getWidth() - 1 - xofs)*Math.log(fmax / fmin))*fmin;
                        } else {
                            ttf=(e.getX()-xofs)/(getWidth() - 1 - xofs)*(fmax - fmin)+fmin;
                        }
                        if (ttf < root.dataConsolidator.f[1])
                            ttf=root.dataConsolidator.f[1];
                        if (ttf > root.dataConsolidator.f[root.dataConsolidator.len/2-1])
                            ttf=root.dataConsolidator.f[root.dataConsolidator.len/2-1];
                        trackf=ttf;
                    }
                } else if (pointers==2) {
                    // Two FInger Track
                    if (e.getPointerCount() < 2) {
                        // Break
                        pointers=0;
                        return true;
                    }
                    // Two pointers
                    float x0 = e.getX(0);
                    float y0 = e.getY(0);
                    float x1 = e.getX(1);
                    float y1 = e.getY(1);
                    float cx0 = (PT0X + PT1X) / 2;
                    float cy0 = (PT0Y + PT1Y) / 2;
                    float cx1 = (e.getX(0) + e.getX(1)) / 2;
                    float cy1 = (e.getY(0) + e.getY(1)) / 2;

                    switch (displayType) {
                        case SPEK:
                            if (Math.abs(x1 - x0) > Math.abs(y1 - y0)) {
                                // horizontal scale and shift
                                float xscale = (PT1X - PT0X)/(x1 - x0);
                                float dx = -(cx1 - cx0)/(getWidth()-1-xofs-1);
                                if (islog) {
                                    fmin = (float)Math.exp((Math.log(storeFmax) + Math.log(storeFmin)) / 2 +
                                            dx * (Math.log(storeFmax) - Math.log(storeFmin))
                                            - xscale * (Math.log(storeFmax) - Math.log(storeFmin)) / 2);
                                    fmax = (float)Math.exp((Math.log(storeFmax) + Math.log(storeFmin)) / 2 +
                                            dx * (Math.log(storeFmax) - Math.log(storeFmin))
                                            + xscale * (Math.log(storeFmax) - Math.log(storeFmin)) / 2);
                                } else {
                                    fmin = (storeFmax + storeFmin) / 2 + dx * (storeFmax - storeFmin) - xscale * (storeFmax - storeFmin) / 2;
                                    fmax = (storeFmax + storeFmin) / 2 + dx * (storeFmax - storeFmin) + xscale * (storeFmax - storeFmin) / 2;
                                }
                            } else {
                                // vertical scale and shift
                                float yscale = (PT1Y - PT0Y) / (y1 - y0);
                                float dy = (cy1 - cy0) / (getHeight() - 1 - yofs - 1);
                                lmin = (storeLmax + storeLmin) / 2 + dy * (storeLmax - storeLmin) - yscale * (storeLmax - storeLmin) / 2;
                                lmax = (storeLmax + storeLmin) / 2 + dy * (storeLmax - storeLmin) + yscale * (storeLmax - storeLmin) / 2;
                            }
                            break;

                        case WFALL:
                            if (Math.abs(x1 - x0) > Math.abs(y1 - y0)) {
                                // horizontal scale and shift
                                float xscale = (PT1X - PT0X)/(x1 - x0);
                                float dx = -(cx1 - cx0)/(getWidth()-1-xofs-1);
                                if (islog) {
                                    fmin = (float)Math.exp((Math.log(storeFmax) + Math.log(storeFmin)) / 2 +
                                            dx * (Math.log(storeFmax) - Math.log(storeFmin))
                                            - xscale * (Math.log(storeFmax) - Math.log(storeFmin)) / 2);
                                    fmax = (float)Math.exp((Math.log(storeFmax) + Math.log(storeFmin)) / 2 +
                                            dx * (Math.log(storeFmax) - Math.log(storeFmin))
                                            + xscale * (Math.log(storeFmax) - Math.log(storeFmin)) / 2);
                                } else {
                                    fmin = (storeFmax + storeFmin) / 2 + dx * (storeFmax - storeFmin) - xscale * (storeFmax - storeFmin) / 2;
                                    fmax = (storeFmax + storeFmin) / 2 + dx * (storeFmax - storeFmin) + xscale * (storeFmax - storeFmin) / 2;
                                }
                            }
                            break;

                        case TERZ:
                            // vertical scale and shift
                            float yscale = (PT1Y - PT0Y) / (y1 - y0);
                            float dy = (cy1 - cy0) / (getHeight() - 1 - yofs - 1);
                            lmin = (storeLmax + storeLmin) / 2 + dy * (storeLmax - storeLmin) - yscale * (storeLmax - storeLmin) / 2;
                            lmax = (storeLmax + storeLmin) / 2 + dy * (storeLmax - storeLmin) + yscale * (storeLmax - storeLmin) / 2;
                            paint_bar=null;
                            break;
                    }
                }
                break;
        }
        return true;
    }

    public void zoomAll() {
        if (root == null) return;
        switch (displayType) {
            case SPEK:
                if (islog) {
                    fmin = root.dataConsolidator.f[1];
                    fmax = root.dataConsolidator.f[root.dataConsolidator.len / 2 - 1];
                } else {
                    fmin = 0;
                    fmax = root.dataConsolidator.f[root.dataConsolidator.len / 2 - 1];
                }
                lmin = -120;
                lmax = 0;
                break;
            case WFALL:
                if (islog) {
                    fmin = root.dataConsolidator.f[1];
                    fmax = root.dataConsolidator.f[root.dataConsolidator.len / 2 - 1];
                } else {
                    fmin = 0;
                    fmax = root.dataConsolidator.f[root.dataConsolidator.len / 2 - 1];
                }
                break;
            case TERZ:
                lmin=-120f;
                lmax=0;
                break;
        }
        SharedPreferences.Editor E=SpectralPrefs.edit();
        E.putBoolean("SpecDisplayLog",islog);
        E.putFloat("LMIN",lmin);
        E.putFloat("LMAX",lmax);
        E.putFloat("FMIN",fmin);
        E.putFloat("FMAX",fmax);
        E.apply();
    }

    public String getFstring(float f) {
        float af=Math.abs(f);
        if (f==0) return "0Hz";
        if (af < 0.01) return String.format("%2.0fmHz",f*1000.0f);
        if (af < 0.1) return String.format("%1.2fHz",f);
        if (af < 1) return String.format("%1.1fHz",f);
        if (af < 10) return String.format("%1.0fHz",f);
        if (af < 100) return String.format("%2.0fHz",f);
        if (af < 1000) return String.format("%3.0fHz",f);
        if (af < 10000) return String.format("%1.0fkHz",f/1000.0f);
        if (af < 100000) return String.format("%2.0fkHz",f/1000.0f);
        if (af < 1000000) return String.format("%3.0fkHz",f/1000.0f);
        return String.format("%1.0fMHz",f/1000000.0f);
    }

    public String getdBstringx(float db) {
        return String.format("%1.1f",db);
    }

    public String getdBstring(float db) {
        return String.format("%1.0f",db);
    }

    public void changeMode(boolean newmode) {
        if (newmode == islog) return;
        if (newmode) {
            // Make sure fmin and fmax are set in the right way
            if (fmin <= 0) fmin=10;
            if (fmax <= fmin+100) fmax=fmin+100;
        }
        logf=null;
        islog=newmode;
    }

    private void showColorMenu() {
        if (root==null) return;
        if (displayType!=DisplayType.WFALL) return;
        if (root.dataConsolidator==null) return;
        PopupMenu P = new PopupMenu(getContext(),this,Gravity.LEFT+Gravity.FILL_VERTICAL+Gravity.BOTTOM);
        Menu menu = P.getMenu();
        for (int i=0;i<colorTabStrings.length;i++) {
            menu.add(0,i+1,Menu.NONE,colorTabStrings[i]);
        }
        P.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id < 1) return false;
                id--;
                if (id >= colorTabStrings.length) return false;
                colorTabString=colorTabStrings[id];
                SharedPreferences.Editor e = SpectralPrefs.edit();
                e.putString("WaterFallColor", colorTabString);
                e.apply();
                return false;
            }
        });
        P.show();
    }

    public void drawFreqGrid(Canvas canvas, int hofs, int vofs, int width, int height, float fmin, float fmax, boolean islog) {

        if (islog) {
            // Logarithmic scale
            int i1 = (int) Math.floor(Math.log10(fmin));
            int i2 = (int) Math.ceil(Math.log10(fmax));
            for (int i = i1; i <= i2; i++) {
                float f = (float) Math.pow(10, i);
                float X = hofs + (float) ((Math.log(f / fmin)) / (Math.log(fmax / fmin)) * (width-1));
                float XN = hofs + (float) ((Math.log(10.0 * f / fmin)) / (Math.log(fmax / fmin)) * (width-1));
                if ((X > hofs) && (X < hofs+width - 1))
                    canvas.drawLine(X, vofs, X, vofs + height - 1, paint_grid);
                if ((X > hofs) && (X < hofs+width-hofs))
                    canvas.drawText(getFstring(f), X, vofs+height+gridfontheight+fontspace, textX);

                if ((XN - X) > 50) {
                    // Subgrid find
                    for (int j = 2; j < 10; j++) {
                        float XY = X + (float) ((Math.log10(j) - 1.0) * (XN - X));
                        if ((XY > hofs) && (XY < hofs+width - 1))
                            canvas.drawLine(XY, vofs, XY, vofs+height - 1, paint_subgrid);
                    }
                } else if (XN - X > 20) {
                    // Subgrid rough
                    float XY = X + (float) (Math.log10(2) - 1.0) * (XN - X);
                    if ((XY > hofs) && (XY < hofs+width - 1))
                        canvas.drawLine(XY, vofs, XY, vofs+height - 1, paint_subgrid);
                    XY = X + (float) (Math.log10(5) - 1.0) * (XN - X);
                    if ((XY > hofs) && (XY < hofs+width - 1))
                        canvas.drawLine(XY, vofs, XY, vofs+height - 1, paint_subgrid);
                }
            }
        } else {
            // Linear scale
            float range=fmax-fmin;
            float gridstep=1;
            float i1,i2;
            if (range < 0.1) gridstep=0.001f;
            else if (range < 1) gridstep=0.01f;
            else if (range < 10) gridstep=0.1f;
            else if (range < 100) gridstep=1f;
            else if (range < 1000) gridstep=10f;
            else if (range < 10000) gridstep=100f;
            else if (range < 100000) gridstep=1000f;
            else gridstep=10000f;
            i1=(float)Math.floor(fmin / gridstep)*gridstep;
            i2=(float)Math.ceil(fmax/gridstep)*gridstep;
            float biggrid=gridstep*10f;


            for (float i=i1;i<=i2;i+=gridstep) {
                float X=hofs+(i-fmin)*(width-1)/(fmax-fmin);
                if (Math.floor(i/biggrid+0.5)==i/biggrid) {
                    if ((X > hofs) && (X < hofs + width - 1))
                        canvas.drawLine(X, vofs, X, vofs+height - 1, paint_grid);
                    if ((X >= hofs) && (X < hofs + width-1))
                        canvas.drawText(getFstring(i),X,vofs+height+gridfontheight+fontspace,textX);
                } else {
                    if ((X > hofs) && (X < hofs + width-1))
                        canvas.drawLine(X, vofs, X, vofs+height - 1, paint_subgrid);
                }
            }
        }
    }

    public void drawGridWaterfall(Canvas canvas, int hofs, int vofs, int width, int height) {

        drawFreqGrid(canvas, hofs, vofs, width, height, fmin, fmax, islog);

        float tmax=(height - 1)/2*root.dataConsolidator.len/root.dataConsolidator.fs;
        int step=1;
        if (tmax > 50)
            step=5;
        else if (tmax > 20)
            step=2;
        else
            step=1;
        for (int i=step;i<tmax;i+=step) {
            float Y=vofs+height-1-(float)i/tmax*(height-1);
            canvas.drawLine(hofs, Y, hofs + width - 1, Y, paint_grid);
            if ((Y > fctr) && (Y < height - 1 - fctr))
                canvas.drawText(String.format("%d", i), hofs- fontspace, Y + fctr / 2, textY);
        }

    }

    public void drawGridSpectrum(Canvas canvas, int hofs, int vofs, int width, int height) {

        drawFreqGrid(canvas, hofs, vofs, width, height, fmin, fmax, islog);

        float lminX = lmin + ofs;
        float lmaxX = lmax + ofs;

        int i1 = (int) Math.floor(lminX / 10.0);
        int i2 = (int) Math.ceil(lmaxX / 10.0);
        for (int i = i1; i <= i2; i++) {
            int Y = (int) (vofs + height - 1) - (int) ((i * 10.0 - lminX) * (height - 1) / (lmaxX - lminX));
            if ((Y >= vofs) && (Y < vofs + height))
                canvas.drawLine(hofs, Y, hofs+width - 1, Y, paint_grid);
            if ((Y > fctr) && (Y < height - 1 - yofs - fctr))
                canvas.drawText(getdBstring(i * 10.0f), xofs - fontspace, Y + fctr / 2, textY);
        }

    }

    public void drawGridTerz(Canvas canvas, int hofs, int vofs, int width, int height) {

        drawFreqGrid(canvas, hofs, vofs, width, height, terzminf, 25298.0f, true);

        float lminX = lmin + ofs;
        float lmaxX = lmax + ofs;

        int i1 = (int) Math.floor(lminX / 10.0);
        int i2 = (int) Math.ceil(lmaxX / 10.0);
        for (int i = i1; i <= i2; i++) {
            int Y = (int) (vofs + height - 1) - (int) ((i * 10.0 - lminX) * (height - 1) / (lmaxX - lminX));
            if ((Y >= vofs) && (Y < vofs + height))
                canvas.drawLine(hofs, Y, hofs+width - 1, Y, paint_grid);
            if ((Y > fctr) && (Y < height - 1 - yofs - fctr))
                canvas.drawText(getdBstring(i * 10.0f), xofs - fontspace, Y + fctr / 2, textY);
        }
    }



    public void drawGrid(Canvas canvas) {
        int width=canvas.getWidth();
        int height=canvas.getHeight();

        int gwidth=width-(int)xofs-2;
        int gheight=height-(int)yofs-2;

        int hofs=(int)xofs+1;
        int vofs=1;

        switch (displayType) {
            case SPEK:
                drawGridSpectrum(canvas, hofs, vofs, gwidth, gheight);
                break;
            case WFALL:
                gheight=height-(int)yofs-2-levelBarHeight;
                vofs=1+levelBarHeight;
                drawGridWaterfall(canvas,hofs,vofs,gwidth,gheight);
                break;
            case TERZ:
                drawGridTerz(canvas, hofs, vofs, gwidth, gheight);
                break;
        }

        canvas.drawRect(hofs - 1, vofs - 1, hofs + gwidth - 1 + 1, vofs + gheight - 1 + 1, paint_frame);
        gridredraw=false;

        // Unit
        switch (displayType) {
            case SPEK:
                canvas.drawText(unit, unitrect.left, unitrect.bottom, paint_frame);

                canvas.drawText("MAX", width - 5-(maxrect.width()+15)/2, 5+buttonHeight/2+buttonFontHeight/2,paint_max);
                canvas.drawText("AVG", width - 5-(maxrect.width()+15)/2, 5+buttonHeight/2+buttonFontHeight/2+buttonHeight,paint_avg);
                canvas.drawText("FFT", width - 5 - (maxrect.width() + 15) / 2, 5 + buttonHeight / 2 + buttonFontHeight / 2 + 2 * buttonHeight, paint_fft);

                drawCornerRect(canvas, width - maxrect.width() - 20, 5 + 2, width - 5, 5 + buttonHeight - 2, paint_max);
                drawCornerRect(canvas, width - maxrect.width() - 20, 5 + 2 + buttonHeight, width - 5, 5 + buttonHeight - 2 + buttonHeight, paint_avg);
                drawCornerRect(canvas, width - maxrect.width() - 20, 5 + 2 + 2 * buttonHeight, width - 5, 5 + buttonHeight - 2 + 2 * buttonHeight, paint_fft);

                //canvas.drawRect(width - maxrect.width() - 20, 5 + 2, width - 5, 5 + buttonHeight - 2, paint_max);
                //canvas.drawRect(width - maxrect.width() - 20, 5+2+buttonHeight, width - 5, 5+buttonHeight-2+buttonHeight, paint_avg);
                //canvas.drawRect(width - maxrect.width() - 20, 5+2+2*buttonHeight, width - 5, 5+buttonHeight-2+2*buttonHeight, paint_fft);
                break;
            case WFALL:
                canvas.drawText("s", unitrect.left, unitrect.bottom, paint_frame);
                if ((colorTable != null) && (colorTable.bar != null)) {
                    int bstart=hofs+hofs;
                    int bstop=hofs+gwidth-1-hofs-hofs;
                    int barwidth=gridfontheight*7/10;
                    int bary1=gridfontheight/2-barwidth/2;
                    int bary2=gridfontheight/2+barwidth/2;
                    Rect sr=new Rect(0,0,colorTable.table.length-1,1);
                    Rect dr=new Rect(bstart,bary1,bstop,bary2);
                    canvas.drawBitmap(colorTable.bar,sr,dr,null);
                    canvas.drawRect(bstart - 1, bary1 - 1, bstop, bary2, paint_colorBarFrame);
                    float lmin=-120+ofs;
                    float lmax=0+ofs;
                    for (float lvl=(float)Math.floor(lmin/10)*10f;lvl < lmax;lvl+=20.0f) {
                        float x=bstart+(lvl-lmin)*(bstop-bstart)/(lmax-lmin);
                        if ((x >= bstart) && (x <= bstop)) {
                            canvas.drawLine(x,bary2,x,bary2+fontspace,paint_gridy);
                            canvas.drawText(getdBstring(lvl),x,bary2+2*fontspace+gridfontheight,textX);
                        }
                    }
                    canvas.drawText(unit,bstop+fontspace,gridfontheight,textYL);
                }
                break;
            case TERZ:
                String un=unit;
                switch (root.dataConsolidator.TERZw) {
                    case 0: break;
                    case 1: un=un + "(A)";break;
                    case 2: un=un + "(B)";break;
                    case 3: un=un + "(C)";break;
                }
                drawCornerRect(canvas, unitrect.left+2, 5+2,
                        unitrect.left+2+unitrect.width()+40, 5 + buttonHeight - 2, paint_frame);
                canvas.drawText(un, unitrect.left+19, 5+buttonHeight/2+buttonFontHeight/2, paint_frame);

                canvas.drawText("MAX", width - 5-(maxrect.width()+15)/2, 5+buttonHeight/2+buttonFontHeight/2,paint_bar_peak);
                canvas.drawText("AVG", width - 5-(maxrect.width()+15)/2, 5+buttonHeight/2+buttonFontHeight/2+buttonHeight,paint_bar_avg);
                drawCornerRect(canvas, width - maxrect.width() - 20, 5 + 2, width - 5, 5 + buttonHeight - 2, paint_bar_peak);
                drawCornerRect(canvas, width - maxrect.width() - 20, 5 + 2 + buttonHeight, width - 5, 5 + buttonHeight - 2 + buttonHeight, paint_bar_avg);

                break;
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        int width=canvas.getWidth();
        int height=canvas.getHeight();
        int trackidx=-1;

        if (root == null) return;

        if ((root.dataConsolidator!=null) && (root.dataConsolidator.f != null) && (root.dataConsolidator.len > 0)) {

            switch (displayType) {
                case SPEK:
                    drawGrid(canvas);
                    canvas.clipRect(xofs+1,1,width-1-1,height-1-yofs-1);

                    if ((root.audioAnalyzerHelper != null) && (root.audioAnalyzerHelper.specMap != null)) {
                        root.audioAnalyzerHelper.SpecViewInit(0,0,null,0,0,false);
                    }
                    if (trackf > 0) {
                        trackidx = (int) Math.floor(trackf / root.dataConsolidator.fs * root.dataConsolidator.len + 0.5);
                    }
                    int q1, q2, q3;
                    q1 = q2 = q3 = 0;

                    if ((line1 == null) || (line1.length != (root.dataConsolidator.len / 2 - 1) * 4)) {
                        line1 = new float[(root.dataConsolidator.f.length - 1) * 4];
                        line2 = new float[(root.dataConsolidator.f.length - 1) * 4];
                        line3 = new float[(root.dataConsolidator.f.length - 1) * 4];
                    }
                    ;

                    if (islog) {
                        // Logarithmic
                        if ((logf == null) || (logf.length != root.dataConsolidator.len / 2) ||
                                (logf[0] != (float) Math.log(root.dataConsolidator.f[0] / fmin))) {
                            if ((logf == null) || (logf.length != root.dataConsolidator.len / 2))
                                logf = new float[root.dataConsolidator.len / 2];
                            for (int i = 1; i < logf.length; i++)
                                logf[i] = (float) Math.log(root.dataConsolidator.f[i] / fmin);
                        }
                        float logfmaxmin = (float) Math.log(fmax / fmin);
                        float y = root.dataConsolidator.y[1];
                        float yavg = root.dataConsolidator.yavg[1];
                        float ypeak = root.dataConsolidator.ypeak[1];
                        float X = (float) xofs + (float) (logf[1] / logfmaxmin * (width - 1 - xofs));
                        float Y = (height - 1 - yofs) - (y - lmin) / (lmax - lmin) * (height - 1 - yofs);
                        float Yavg = (height - 1 - yofs) - (yavg - lmin) / (lmax - lmin) * (height - 1 - yofs);
                        float Ypeak = (height - 1 - yofs) - (ypeak - lmin) / (lmax - lmin) * (height - 1 - yofs);
                        if (1 == trackidx) {
                            if (showPeak) {
                                canvas.drawCircle(X, Ypeak, 10f, paint_max);
                                canvas.drawText(getdBstringx(ypeak+ofs),X,Ypeak,paint_max_mark);
                            }
                            if (showAvg) {
                                canvas.drawCircle(X, Yavg, 10f, paint_avg);
                                canvas.drawText(getdBstringx(yavg+ofs),X,Yavg,paint_avg_mark);
                            }
                            if (showFFT) {
                                canvas.drawCircle(X, Y, 10f, paint_fft);
                                // canvas.drawText(getdBstringx(y),X,Y,paint_fft_mark);
                            }
                        }
                        for (int i = 2; i < logf.length; i++) {
                            float y2 = root.dataConsolidator.y[i];
                            float yavg2 = root.dataConsolidator.yavg[i];
                            float ypeak2 = root.dataConsolidator.ypeak[i];
                            float X2 = (float) xofs + (float) (logf[i] / logfmaxmin * (width - 1 - xofs));
                            float Y2 = (height - 1 - yofs) - (y2 - lmin) / (lmax - lmin) * (height - 1 - yofs);
                            float Yavg2 = (height - 1 - yofs) - (yavg2 - lmin) / (lmax - lmin) * (height - 1 - yofs);
                            float Ypeak2 = (height - 1 - yofs) - (ypeak2 - lmin) / (lmax - lmin) * (height - 1 - yofs);
                            line1[q1++] = X;
                            line1[q1++] = Y;
                            line1[q1++] = X2;
                            line1[q1++] = Y2;
                            line2[q2++] = X;
                            line2[q2++] = Ypeak;
                            line2[q2++] = X2;
                            line2[q2++] = Ypeak2;
                            line3[q3++] = X;
                            line3[q3++] = Yavg;
                            line3[q3++] = X2;
                            line3[q3++] = Yavg2;

                            //if (showPeak) canvas.drawLine(X, Ypeak, X2, Ypeak2, paint_max);
                            //if (showAvg) canvas.drawLine(X, Yavg, X2, Yavg2, paint_avg);
                            //canvas.drawLine(X, Y, X2, Y2, paint_fft);
                            if (i == trackidx) {
                                if (showPeak) {
                                    canvas.drawCircle(X2, Ypeak2, 10f, paint_max);
                                    canvas.drawText(getdBstringx(ypeak2+ofs), X2, Ypeak2, paint_max_mark);
                                }
                                if (showAvg)  {
                                    canvas.drawCircle(X2, Yavg2, 10f, paint_avg);
                                    canvas.drawText(getdBstringx(yavg2+ofs), X2, Yavg2, paint_avg_mark);
                                }
                                if (showFFT) {
                                    canvas.drawCircle(X2, Y2, 10f, paint_fft);
                                    // canvas.drawText(getdBstringx(y2), X2, Y2, paint_fft_mark);
                                }
                            }
                            X = X2;
                            Y = Y2;
                            Yavg = Yavg2;
                            Ypeak = Ypeak2;
                        }
                        X = (float) xofs + (float) (Math.log(trackf/fmin) / logfmaxmin * (width - 1 - xofs));
                        trackfPath.reset();
                        trackfPath.moveTo(X, gridfontheight);
                        trackfPath.lineTo(X, height - 1 - yofs - gridfontheight);
                        canvas.drawPath(trackfPath, paint_mark);
                        // canvas.drawLine(X,gridfontheight,X,height-1-yofs-gridfontheight,paint_mark);
                    } else {
                        // Linear
                        float f = root.dataConsolidator.f[1];
                        float y = root.dataConsolidator.y[1];
                        float yavg = root.dataConsolidator.yavg[1];
                        float ypeak = root.dataConsolidator.ypeak[1];
                        float X = (float) xofs + (float) ((f - fmin) / (fmax - fmin) * (width - 1 - xofs));
                        float Y = (height - 1 - yofs) - (y - lmin) / (lmax - lmin) * (height - 1 - yofs);
                        float Yavg = (height - 1 - yofs) - (yavg - lmin) / (lmax - lmin) * (height - 1 - yofs);
                        float Ypeak = (height - 1 - yofs) - (ypeak - lmin) / (lmax - lmin) * (height - 1 - yofs);
                        if (1 == trackidx) {
                            if (showPeak) {
                                canvas.drawCircle(X, Ypeak, 10f, paint_max);
                                canvas.drawText(getdBstringx(ypeak+ofs),X,Ypeak,paint_max_mark);
                            }
                            if (showAvg) {
                                canvas.drawCircle(X, Yavg, 10f, paint_avg);
                                canvas.drawText(getdBstringx(yavg+ofs),X,Yavg,paint_avg_mark);
                            }
                            if (showFFT) {
                                canvas.drawCircle(X, Y, 10f, paint_fft);
                                // canvas.drawText(getdBstringx(y),X,Y,paint_fft_mark);
                            }
                        }
                        for (int i = 2; i < root.dataConsolidator.len / 2; i++) {
                            float f2 = root.dataConsolidator.f[i];
                            float y2 = root.dataConsolidator.y[i];
                            float yavg2 = root.dataConsolidator.yavg[i];
                            float ypeak2 = root.dataConsolidator.ypeak[i];
                            float X2 = (float) xofs + (float) ((f2 - fmin) / (fmax - fmin) * (width - 1 - xofs));
                            float Y2 = (height - 1 - yofs) - (y2 - lmin) / (lmax - lmin) * (height - 1 - yofs);
                            float Yavg2 = (height - 1 - yofs) - (yavg2 - lmin) / (lmax - lmin) * (height - 1 - yofs);
                            float Ypeak2 = (height - 1 - yofs) - (ypeak2 - lmin) / (lmax - lmin) * (height - 1 - yofs);
                            line1[q1++] = X;
                            line1[q1++] = Y;
                            line1[q1++] = X2;
                            line1[q1++] = Y2;
                            line2[q2++] = X;
                            line2[q2++] = Ypeak;
                            line2[q2++] = X2;
                            line2[q2++] = Ypeak2;
                            line3[q3++] = X;
                            line3[q3++] = Yavg;
                            line3[q3++] = X2;
                            line3[q3++] = Yavg2;
                            //if (showPeak) canvas.drawLine(X, Ypeak, X2, Ypeak2, paint_max);
                            //if (showAvg) canvas.drawLine(X, Yavg, X2, Yavg2, paint_avg);
                            //canvas.drawLine(X, Y, X2, Y2, paint_fft);
                            if (i == trackidx) {
                                if (showPeak) {
                                    canvas.drawCircle(X2, Ypeak2, 10f, paint_max);
                                    canvas.drawText(getdBstringx(ypeak2+ofs), X2, Ypeak2, paint_max_mark);
                                }
                                if (showAvg)  {
                                    canvas.drawCircle(X2, Yavg2, 10f, paint_avg);
                                    canvas.drawText(getdBstringx(yavg2+ofs), X2, Yavg2, paint_avg_mark);
                                }
                                if (showFFT) {
                                    canvas.drawCircle(X2, Y2, 10f, paint_fft);
                                    // canvas.drawText(getdBstringx(y2), X2, Y2, paint_fft_mark);
                                }
                            }
                            X = X2;
                            Y = Y2;
                            Yavg = Yavg2;
                            Ypeak = Ypeak2;
                        }
                        X = (float) xofs + (float) ((trackf - fmin) / (fmax - fmin) * (width - 1 - xofs));
                        trackfPath.reset();
                        trackfPath.moveTo(X, gridfontheight);
                        trackfPath.lineTo(X, height - 1 - yofs - gridfontheight);
                        canvas.drawPath(trackfPath, paint_mark);
                        // canvas.drawLine(X,gridfontheight,X,height-1-yofs-gridfontheight,paint_mark);
                    }
                    if (showPeak) canvas.drawLines(line2, paint_max);
                    if (showAvg) canvas.drawLines(line3, paint_avg);
                    if (showFFT) canvas.drawLines(line1, paint_fft);
                    break;
                case WFALL:
                    if (root.audioAnalyzerHelper != null) {
                        int wd = (int)width-(int)xofs-2;
                        int ht = (int)height-(int)yofs-2-levelBarHeight;
                        boolean renewed=false;
                        if ((root.audioAnalyzerHelper.specMap == null) ||
                                (root.audioAnalyzerHelper.specMapWidth != wd) ||
                                (root.audioAnalyzerHelper.specMapHeight != ht)) {
                            colorTable = new ColorTable(256,colorTabString);
                            renewed=true;
                            root.audioAnalyzerHelper.SpecViewInit(wd, ht, colorTable.table, fmin, fmax, islog);
                        }
                        if (!renewed && (
                                (fmin != root.audioAnalyzerHelper.specFmin) || (fmax != root.audioAnalyzerHelper.specFmax) ||
                                        (islog != root.audioAnalyzerHelper.specLogScale) ||
                                        !colorTabString.equals(colorTable.id))) {
                            if (!colorTabString.equals(colorTable.id)) {
                                colorTable = new ColorTable(256,colorTabString);
                                root.audioAnalyzerHelper.SpecViewInit(wd, ht, colorTable.table, fmin, fmax, islog);
                            } else
                                root.audioAnalyzerHelper.SpecViewInit(wd, ht, null, fmin, fmax, islog);
                        }

                        root.audioAnalyzerHelper.fftCopySpecMaptoBitmap();
                        canvas.drawBitmap(root.audioAnalyzerHelper.specMap, xofs + 1, 1 + levelBarHeight, null);
                    }
                    drawGrid(canvas);
                    break;
                case TERZ:
                    if (paint_bar == null) {
                        int[] colors={Color.argb(255,128,0,0),
                                    Color.argb(255,255,0,0),
                                    Color.argb(255,255,255,0)};
                        float[] positions={0,0.25f,1.0f};
                        paint_bar=new Paint();
                        // paint_bar.setColor(Color.GREEN);
                        float y1=(height - 1 - yofs) - (-120.0f - lmin) / (lmax - lmin) * (height - 1 - yofs);
                        float y2=(height - 1 - yofs) - (0.0f - lmin) / (lmax - lmin) * (height - 1 - yofs);
                        paint_bar.setShader(new LinearGradient(0,y1,0,y2,colors,positions, Shader.TileMode.CLAMP));
                        // Color.argb(255,255,0,0),Color.argb(255, 255, 255, 0), Shader.TileMode.CLAMP));
                        paint_bar.setStyle(Paint.Style.FILL);
                    }
                    float fftmin=(float)root.dataConsolidator.fs/root.dataConsolidator.len;
                    terzminidx=(int)Math.floor(3.0*Math.log(fftmin/500.0)/Math.log(2.0)+17);
                    if (terzminidx < 0) terzminidx=0;
                    terzminf=(float)(500.0*Math.pow(2.0,(terzminidx-17-1)/3.0));
                    drawGrid(canvas);
                    canvas.clipRect(xofs+1,1,width-1-1,height-1-yofs-1);
                    int N=root.dataConsolidator.TERZn-terzminidx;
                    float bw=1.0f* (width - 1 - xofs)/((float)N+1.0f);
                    if (showPeak) {
                        if ((line1==null) || (line1.length < (N-1)*4))
                            line1=new float[(N-1)*4];
                        float X1=(float) xofs + (float)(0+1.0f) * (width - 1 - xofs)/(N+1);
                        float Y1 = (height - 1 - yofs) - (root.dataConsolidator.TERZepeak[terzminidx] - lmin) / (lmax - lmin) * (height - 1 - yofs);
                        for (int i=terzminidx+1;i<root.dataConsolidator.TERZn;i++) {
                            float X2=(float) xofs + (float)(i-terzminidx+1.0f) * (width - 1 - xofs)/(N+1);
                            float Y2 = (height - 1 - yofs) - (root.dataConsolidator.TERZepeak[i] - lmin) / (lmax - lmin) * (height - 1 - yofs);
                            line1[(i-terzminidx-1)*4+0]=X1;
                            line1[(i-terzminidx-1)*4+1]=Y1;
                            line1[(i-terzminidx-1)*4+2]=X2;
                            line1[(i-terzminidx-1)*4+3]=Y2;
                            X1=X2;
                            Y1=Y2;
                        }
                        canvas.drawLines(line1,0,(N-1)*4,paint_max);
                    }
                    for (int i=terzminidx;i<root.dataConsolidator.TERZn;i++) {
                        float X=(float) xofs + (float)(i-terzminidx+1.0f) * (width - 1 - xofs)/(N+1);
                        float X1 = X - bw*0.2f;
                        float X2 = X + bw*0.2f;
                        float y=root.dataConsolidator.TERZe[i];
                        if (y > lmin) {
                            float Y = (height - 1 - yofs) - (y - lmin) / (lmax - lmin) * (height - 1 - yofs);
                            canvas.drawRect(X1,Y,X2,height-1-yofs,paint_bar);
                        }
                        X1 = X - bw*0.4f;
                        X2 = X + bw*0.4f;
                        y=root.dataConsolidator.TERZeavg[i];
                        if ((y > lmin) && showAvg) {
                            float Y = (height - 1 - yofs) - (y - lmin) / (lmax - lmin) * (height - 1 - yofs);
                            canvas.drawRect(X1,Y,X2,Y+5.0f,paint_bar_avg);
                        }
                        /*
                        y=root.dataConsolidator.TERZepeak[i];
                        if (y > lmin) {
                            float Y = (height - 1 - yofs) - (y - lmin) / (lmax - lmin) * (height - 1 - yofs);
                            canvas.drawLine(X1,Y-bw/2,X,Y,paint_max);
                            canvas.drawLine(X, Y, X2, Y - bw / 2, paint_max);
                        }*/
                    }
                    break;
            }
            canvas.clipRect(0, 0, width - 1, height - 1);
        }
    }

    public void display() {
        invalidate();
    }

}
