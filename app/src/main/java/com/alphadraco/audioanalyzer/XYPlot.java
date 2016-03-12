package com.alphadraco.audioanalyzer;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.text.Html;
import android.text.Spanned;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by aladin on 28.02.2016.
 */
public class XYPlot extends View {

    // Prefs
    public String sharedPrefPrefix="undefined";
    public SharedPreferences sharedPreferences=null;
    public SharedPreferences.OnSharedPreferenceChangeListener PrefListener;
    public AudioAnalyzer root;

    // Defaults (Configuration, must not change after creation of the object)
    float defaultxMin=10;
    float defaultxMax=22050;
    float defaultyMin=-120;
    float defaultyMax=0;
    boolean canLogX=true;
    boolean canLogY=false;
    public boolean displayOnlyOne=false;
    public int displayOne=-1;

    // Axis Data
    PlotAxis AX;
    PlotAxis AY;

    // Plot Config
    float displayOfs=0.0f;
    float displayScale=1.0f;
    public String unit;

    // The Data to Plot
    protected ArrayList<XYdata> lines;

    // Display Coordinates
    //public int displayWidth;
    //public int displayHeight;
    //public int ofsX;
    //public int ofsY;

    // Painting
    private class PaintConfig {
        public Paint paint_frame;
        public Paint paint_grid;
        public Paint paint_unit;
        public Paint paint_gridX;
        public Paint paint_subgrid;
        public Paint textX;
        public Paint textY;

        public float xAxisHeight;
        public float yAxisWidth;

        public float xLableWidth;
        public float yLableHeight;

        public float fontspace;
        public boolean antiAlias;
        public boolean inverted;

        public float stdFontSize;



        public PaintConfig(float _stdFontSize, boolean _antiAlias, boolean _inverted) {
            stdFontSize=_stdFontSize;
            antiAlias=_antiAlias;
            inverted=_inverted;

            paint_frame=new Paint();
            if (inverted)
                paint_frame.setColor(Color.BLACK);
            else
                paint_frame.setColor(Color.WHITE);
            paint_frame.setStyle(Paint.Style.STROKE);
            paint_frame.setStrokeWidth(2);
            paint_frame.setTextAlign(Paint.Align.LEFT);
            paint_frame.setTextSize(stdFontSize);
            paint_frame.setAntiAlias(antiAlias);

            paint_unit=new Paint();
            if (inverted)
                paint_unit.setColor(Color.BLACK);
            else
                paint_unit.setColor(Color.WHITE);
            paint_unit.setTextAlign(Paint.Align.LEFT);
            paint_unit.setTextSize(stdFontSize);
            paint_unit.setStyle(Paint.Style.FILL);
            paint_unit.setAntiAlias(antiAlias);

            paint_grid=new Paint();
            if (inverted)
                paint_grid.setColor(Color.DKGRAY);
            else
                paint_grid.setColor(Color.GRAY);
            paint_grid.setStyle(Paint.Style.STROKE);
            paint_grid.setTextSize(stdFontSize);
            paint_grid.setTextAlign(Paint.Align.RIGHT);
            paint_grid.setAntiAlias(antiAlias);

            textX=new Paint();
            if (inverted)
                textX.setColor(Color.DKGRAY);
            else
                textX.setColor(Color.GRAY);
            textX.setStyle(Paint.Style.STROKE);
            textX.setTextSize(stdFontSize);
            textX.setTextAlign(Paint.Align.CENTER);
            textX.setStyle(Paint.Style.FILL);
            textX.setAntiAlias(antiAlias);

            paint_subgrid=new Paint();
            if (inverted)
                paint_subgrid.setColor(Color.GRAY);
            else
                paint_subgrid.setColor(Color.DKGRAY);
            paint_subgrid.setStyle(Paint.Style.STROKE);
            paint_subgrid.setTextSize(stdFontSize);
            paint_subgrid.setTextAlign(Paint.Align.RIGHT);
            paint_subgrid.setAntiAlias(antiAlias);

            textY=new Paint();
            if (inverted)
                textY.setColor(Color.DKGRAY);
            else
                textY.setColor(Color.GRAY);
            textY.setStyle(Paint.Style.FILL);
            textY.setTextSize(stdFontSize);
            textY.setTextAlign(Paint.Align.RIGHT);
            textY.setAntiAlias(antiAlias);

            paint_gridX=new Paint();
            if (inverted)
                paint_gridX.setColor(Color.DKGRAY);
            else
                paint_gridX.setColor(Color.GRAY);
            paint_gridX.setStyle(Paint.Style.STROKE);
            paint_gridX.setTextSize(stdFontSize);
            paint_gridX.setTextAlign(Paint.Align.CENTER);
            paint_gridX.setAntiAlias(antiAlias);

            String Sw = "-120.0 dB";
            Rect rct=new Rect();
            paint_grid.getTextBounds(Sw, 0, Sw.length(), rct);
            //fctr=rct.height();
            yLableHeight=rct.height();
            fontspace=yLableHeight/5;

            yAxisWidth=rct.width()+5;
            xAxisHeight=rct.height()+5;

            Sw="-10.00k";
            paint_grid.getTextBounds(Sw, 0, Sw.length(), rct);
            xLableWidth=rct.width();


        }


    };

    public PaintConfig pc;

    // Control
    float storeyMin,storeyMax;
    float storexMin,storexMax;
    float PT0X, PT0Y, PT1X, PT1Y;
    int pointers=0;

    public int getColor(int idx) {
        switch (idx % 12) {
            case 0: return Color.argb(255,0,255,0);
            case 1: return Color.argb(255,255,0,0);
            case 2: return Color.argb(255,0,0,255);
            case 3: return Color.argb(255,0,255,255);
            case 4: return Color.argb(255,255,0,255);
            case 5: return Color.argb(255,255,255,0);
            case 6: return Color.argb(255,0,0,127);
            case 7: return Color.argb(255,0,127,0);
            case 8: return Color.argb(255,127,0,0);
            case 9: return Color.argb(255,0,127,127);
            case 10: return Color.argb(255,127,0,127);
            case 11: return Color.argb(255,127,127,0);
        }
        return 0;
    }

    public String getColorName(int idx) {
        switch (idx % 12) {
            case 0: return "Green";
            case 1: return "Red";
            case 2: return "Blue";
            case 3: return "Cyan";
            case 4: return "Magenta";
            case 5: return "Yellow";
            case 6: return "Dark Blue";
            case 7: return "Dark Green";
            case 8: return "Dark Red";
            case 9: return "Dark Cyan";
            case 10: return "Dark Magenta";
            case 11: return "Dark Yellow";
        }
        return "";
    }

    public String getColorHtml(int col) {
        int alpha=Color.alpha(col);
        int red=Color.red(col);
        int green=Color.green(col);
        int blue=Color.blue(col);
        return String.format("#%02X%02X%02X",red,green,blue);
    }

    public Spanned getColoredText(int col, String txt) {
        return Html.fromHtml("<font color='" + getColorHtml(col) + "'>" + txt + "</font>");
    }

    public Spanned getColoredColorName(int idx) {
        return getColoredText(getColor(idx),getColorName(idx));
    }


    public int getColorNum() {
        return 12;
    }

    public int getNextColor() {
        if (lines==null) return getColor(0);
        return getColor(lines.size());
    }

    public String prefName(String s) {
        return sharedPrefPrefix + "_" + s;
    }

    public void storePreferences() {
        if ((root == null) || (sharedPreferences == null)) return;
        SharedPreferences.Editor e=sharedPreferences.edit();
        e.putFloat(prefName("XYPlotXMin"),AX.aMin);
        e.putFloat(prefName("XYPlotYMin"),AY.aMin);
        e.putFloat(prefName("XYPlotXMax"),AX.aMax);
        e.putFloat(prefName("XYPlotYMax"),AY.aMax);
        e.putBoolean(prefName("XYPlotXLog"),AX.logScale);
        e.putBoolean(prefName("XYPlotYLog"),AY.logScale);
        e.apply();
    }

    public void setPreferences(AudioAnalyzer _root, SharedPreferences prefs, String prefix) {
        root=_root;
        sharedPrefPrefix = prefix;
        sharedPreferences = prefs;
        if (sharedPreferences != null) {
            AX.aMin = sharedPreferences.getFloat(prefName("XYPlotXMin"), defaultxMin);
            AX.aMax = sharedPreferences.getFloat(prefName("XYPlotXMax"), defaultxMax);
            AY.aMin = sharedPreferences.getFloat(prefName("XYPlotYMin"), defaultyMin);
            AY.aMax = sharedPreferences.getFloat(prefName("XYPlotYMax"), defaultyMax);
            AX.logScale = sharedPreferences.getBoolean(prefName("XYPlotXLog"),false);
            AY.logScale = sharedPreferences.getBoolean(prefName("XYPlotYLog"),false);
            // Verify X
            if ((AX.aMin >= AX.aMax) || (AX.logScale && (AX.aMin <= 0.0f))) {
                // Reset Scaling to defaults
                AX.aMin=defaultxMin;
                AX.aMax=defaultxMax;
            }
            // Verify Y
            if ((AY.aMin >= AY.aMax) || (AY.logScale && (AY.aMin <= 0.0f))) {
                // Reset Scaling to defaults
                AY.aMin=defaultyMin;
                AY.aMax=defaultyMax;
            }
        }
    }

    protected void setup(Context context) {
        root=null;
        sharedPrefPrefix="undefined";
        sharedPreferences=null;
        PrefListener=null;

        lines=null;

        // Init Display
        float stdsize = new Button(context).getTextSize();
        pc=new PaintConfig(0.75f*stdsize,false,false); // No AntiAliasing on High Resolution screens

        AX=new PlotAxis(0,22050,false,pc.yAxisWidth+1,getWidth()-1,pc.xLableWidth);
        AY=new PlotAxis(-120.0f,0.0f,false,getHeight()-1-pc.xAxisHeight,1,1.5f*pc.yLableHeight);

        displayOfs=0;
        unit="dBFS";

        displayScale=1.0f;
        displayOnlyOne=false;
        displayOne=-1;
    }

    public XYPlot(Context context) {
        super(context);
        setup(context);
    }

    public XYPlot(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Check fixed positions first
                if (canLogX) {
                    // Switch Log Lin on X-Axis
                    if (e.getY(0) > getHeight() - 2 * pc.xAxisHeight) {
                        if (!AX.logScale) {
                            // Ensure valid Parameters for log plot
                            if (AX.aMin <= 0) AX.aMin = defaultxMin;
                            if (AX.aMax > defaultxMax) AX.aMax = defaultxMax;
                            if (AX.aMax <= AX.aMin) AX.aMax = AX.aMin + 1;
                        } else {
                            if (AX.aMin < -defaultxMax) AX.aMin = -defaultxMax;
                            if (AX.aMax > defaultxMax) AX.aMax = defaultxMax;
                            if (AX.aMin >= AX.aMax) {
                                AX.aMax = AX.aMin + 1;
                            }
                        }
                        AX.logScale = !AX.logScale;
                        invalidate();
                        storePreferences();
                        return true;
                    }
                }
                if (canLogY) {
                    // Switch Log/lin on Y-Axis
                    if (e.getX(0) < pc.yAxisWidth) {
                        if (!AY.logScale) {
                            // Ensure valid Parameters for log plot
                            if (AY.aMin <= 0) AY.aMin = defaultyMin;
                            if (AY.aMax > defaultyMax) AY.aMax = defaultyMax;
                            if (AY.aMax <= AY.aMin) AY.aMax = AY.aMin + 1;
                        } else {
                            if (AY.aMin < -defaultyMax) AY.aMin = -defaultyMax;
                            if (AY.aMax > defaultyMax) AY.aMax = defaultyMax;
                            if (AY.aMin >= AY.aMax) {
                                AY.aMax = AY.aMin + 1;
                            }
                        }
                        AY.logScale = !AY.logScale;
                        invalidate();
                        storePreferences();
                        return true;
                    }
                }

                // Dynamic scaling/shifting
                storeyMin=AY.aMin;
                storeyMax=AY.aMax;
                storexMin=AX.aMin;
                storexMax=AX.aMax;

                // First down
                PT0X = e.getX(0);
                PT0Y = e.getY(0);

                pointers++;

                if (e.getPointerCount()==2) {
                    PT1X=e.getX(1);
                    PT1Y=e.getY(1);
                    pointers++;
                }

                if (e.getPointerCount() > 2)
                    pointers=0;
                break;

            case MotionEvent.ACTION_UP:
                if (pointers > 0) {
                    // Store Data
                    storePreferences();
                }
            case MotionEvent.ACTION_CANCEL:
                pointers=0;
                break;

            case MotionEvent.ACTION_MOVE:
                if (e.getPointerCount() > 2)
                    pointers=0;
                if ((e.getPointerCount() == 2) && (pointers == 1)) {
                    PT1X = e.getX(1);
                    PT1Y = e.getY(1);
                    pointers++;
                }

                if (pointers == 1) {
                    if (AY.logScale) {
                        float dy = e.getY() - PT0Y;
                        float dl = -dy * (float)Math.log(storeyMax/storeyMin) / AY.px();
                        AX.aMin = storeyMin*(float)Math.exp(dl);
                        AX.aMax = storeyMax*(float)Math.exp(dl);
                    } else {
                        float dy = e.getY() - PT0Y;
                        float dl = dy * (storeyMax - storeyMin) / AY.px();
                        AY.aMin = storeyMin + dl;
                        AY.aMax = storeyMax + dl;
                    }

                    if (AX.logScale) {
                        float dx = e.getX() - PT0X;
                        float dl = -dx * (float)Math.log(storexMax/storexMin) / AX.px();
                        AX.aMin = storexMin*(float)Math.exp(dl);
                        AX.aMax = storexMax*(float)Math.exp(dl);
                    } else {
                        float dx = e.getX() - PT0X;
                        float dl = dx * (storexMax - storexMin) / AX.px();
                        AX.aMin = storexMin - dl;
                        AX.aMax = storexMax - dl;
                    }

                    // Redraw
                    invalidate();

                } else if (pointers==2) {
                    if (e.getPointerCount() < 2) {
                        pointers=0;
                    } else {
                        // Two pointers
                        float y0 = e.getY(0);
                        float y1 = e.getY(1);
                        float cy0 = (PT0Y + PT1Y) / 2;
                        float cy1 = (e.getY(0) + e.getY(1)) / 2;
                        // vertical scale and shift
                        float yscale = (PT1Y - PT0Y) / (y1 - y0);
                        float dy = (cy1 - cy0) / AY.px();

                        float x0 = e.getX(0);
                        float x1 = e.getX(1);
                        float cx0 = (PT0X + PT1X) / 2;
                        float cx1 = (e.getX(0) + e.getX(1)) / 2;
                        // vertical scale and shift
                        float xscale = (PT1X - PT0X) / (x1 - x0);
                        float dx = -(cx1 - cx0) / AX.px();

                        if (Math.abs(PT0X-PT1X) < Math.abs(PT0Y-PT1Y)) {
                            // Vertical Scale and Shift
                            if (AY.logScale) {
                                AY.aMin = (float) Math.exp((Math.log(storeyMax) + Math.log(storeyMin)) / 2 + dy * (Math.log(storeyMax) - Math.log(storexMin)) - yscale * (Math.log(storeyMax) - Math.log(storeyMin)) / 2);
                                AY.aMax = (float) Math.exp((Math.log(storeyMax) + Math.log(storeyMin)) / 2 + dy * (Math.log(storeyMax) - Math.log(storexMin)) + yscale * (Math.log(storeyMax) - Math.log(storeyMin)) / 2);
                            } else {
                                AY.aMin = (storeyMax + storeyMin) / 2 + dy * (storeyMax - storeyMin) - yscale * (storeyMax - storeyMin) / 2;
                                AY.aMax = (storeyMax + storeyMin) / 2 + dy * (storeyMax - storeyMin) + yscale * (storeyMax - storeyMin) / 2;
                            }
                        } else {
                            // Horizontal Scale and Shift
                            if (AX.logScale) {
                                AX.aMin = (float) Math.exp((Math.log(storexMax) + Math.log(storexMin)) / 2 + dx * (Math.log(storexMax) - Math.log(storexMin)) - xscale * (Math.log(storexMax) - Math.log(storexMin)) / 2);
                                AX.aMax = (float) Math.exp((Math.log(storexMax) + Math.log(storexMin)) / 2 + dx * (Math.log(storexMax) - Math.log(storexMin)) + xscale * (Math.log(storexMax) - Math.log(storexMin)) / 2);
                            } else {
                                AX.aMin = (storexMax + storexMin) / 2 + dx * (storexMax - storexMin) - xscale * (storexMax - storexMin) / 2;
                                AX.aMax = (storexMax + storexMin) / 2 + dx * (storexMax - storexMin) + xscale * (storexMax - storexMin) / 2;
                            }
                        }
                        invalidate();
                    }
                }
                break;
        }
        return true;
    }

    protected void drawGrid(Canvas canvas, PlotAxis PAX, PlotAxis PAY, PaintConfig p) {
        // Horizontal
        PAX.realize();
        for (int i=0;i<PAX.gridLength;i++) {
            float pos=PAX.gridPositions[i];
            int style=PAX.gridStyles[i];
            float val=PAX.gridValues[i];
            if ((style & 0x01) != 0) {
                // Major
                canvas.drawLine(pos, PAY.pxMin, pos, PAY.pxMax, p.paint_grid);
            } else {
                // Minor
                canvas.drawLine(pos, PAY.pxMin, pos, PAY.pxMax, p.paint_subgrid);
            }
            if ((style & 0x02) != 0) {
                if (pos < PAX.pxMax-p.xLableWidth/2)
                    canvas.drawText(PAX.getString(val), pos, PAY.pxMin+p.xAxisHeight,p.textX);
            }
        }

        // Vertical
        PAY.realize();
        for (int i=0;i<PAY.gridLength;i++) {
            float pos=PAY.gridPositions[i];
            int style=PAY.gridStyles[i];
            float val=PAY.gridValues[i];
            if ((style & 0x01) != 0) {
                // Major
                canvas.drawLine(PAX.pxMin, pos,PAX.pxMax,pos, p.paint_grid);
            } else {
                // Minor
                canvas.drawLine(PAX.pxMin, pos,PAX.pxMax,pos, p.paint_subgrid);
            }
            if ((style & 0x02) != 0) {
                if ((pos > PAY.pxMax+p.yLableHeight/2) && (pos < PAY.pxMin-p.yLableHeight/2))
                    canvas.drawText(AY.getString(val), p.yAxisWidth-p.fontspace, pos + p.yLableHeight / 2, p.textY);
            }
        }
        canvas.drawText(unit, PAX.pxMin+p.fontspace, PAY.pxMax+p.fontspace+p.yLableHeight, p.paint_unit);
        canvas.drawRect(PAX.pxMin-1,PAY.pxMax-1,PAX.pxMax+1,PAY.pxMin+1,p.paint_frame);
    }

    protected RectF getRange(ArrayList<XYdata> xy, boolean logScaleX, boolean logScaleY) {
        RectF r=new RectF();
        r.left=defaultxMin;
        r.right=defaultxMax;
        r.bottom=defaultyMin;
        r.top=defaultyMax;
        if ((xy != null) && (xy.size() > 0)) {
            boolean foundone = false;
            for (int i = 0; i < xy.size(); i++) {
                if (xy.get(i).points > 1) {
                    RectF rn = xy.get(i).getRange();
                    if (!foundone) {
                        r.set(rn);
                        foundone = true;
                    } else {
                        r.union(rn);
                    }
                }
            }
        }
        if (r.left >= r.right)
            r.right=r.left+1;
        if (r.bottom >= r.top)
            r.top=r.bottom+1;
        if (logScaleX) {
            if (r.left <= 0)
                r.left=defaultxMin;
            if (r.left >= r.right)
                r.right=r.left+1;
        }
        if (logScaleY) {
            if (r.bottom <= 0)
                r.bottom=defaultyMin;
            if (r.bottom >= r.top)
                r.top=r.bottom+1;
        }
        r.top*=displayScale;
        r.bottom*=displayScale;
        r.top += displayOfs;
        r.bottom += displayOfs;
        return r;
    }

    public void drawToCanvas(Canvas canvas, ArrayList<XYdata> xy, float fontsize, boolean autoZoom, boolean inverted) {
        PlotAxis QX=new PlotAxis(AX);
        PlotAxis QY=new PlotAxis(AY);
        PaintConfig p=new PaintConfig(fontsize,true,inverted); // Anti Alias on
        if (inverted) canvas.drawARGB(255,255,255,255);
        QX.pxMin=p.yAxisWidth+2;
        QX.pxMax=canvas.getWidth()-2;
        QY.pxMin=canvas.getHeight()-p.xAxisHeight-2;
        QY.pxMax=2;
        if (autoZoom) {
            RectF r=getRange(xy,QX.logScale,QY.logScale);
            QX.aMin=r.left;
            QX.aMax=r.right;
            QY.aMin=r.bottom;
            QY.aMax=r.top;
        }
        drawGrid(canvas,QX,QY,p);

        if ((xy == null) || (xy.size() < 1))
            return;

        canvas.clipRect(QX.pxMin,QY.pxMax,QX.pxMax,QY.pxMin);
        if (p.antiAlias) {
            for (XYdata l:xy)
                l.pnt.setAntiAlias(true);
        }

        // Draw Lines
        for (XYdata l:xy)
            l.plot(canvas,QX,QY, displayOfs,displayScale);

        // Legend
        int n=0;
        for (XYdata l:xy) {
            l.pnt.setTextSize(p.stdFontSize);
            canvas.drawText(l.name, QX.pxMax-p.fontspace, QY.pxMax + p.fontspace + p.yLableHeight +
                    p.yLableHeight * 2 * n, l.pnt);
            n++;
        }

        if (!pc.antiAlias) {
            for (XYdata l:xy)
                l.pnt.setAntiAlias(false);
        }

        canvas.clipRect(0, 0, canvas.getWidth() - 1, canvas.getHeight() - 1);
    }

    @Override
    public void onDraw(Canvas canvas) {
        //displayWidth=getWidth()-ofsX-2;
        //displayHeight=getHeight()-ofsY-2*(int)fctr-2;
        AX.pxMin=pc.yAxisWidth+2;
        AX.pxMax=getWidth()-2;
        AY.pxMin=getHeight()-pc.xAxisHeight-2;
        AY.pxMax=2;

        drawGrid(canvas,AX,AY,pc);
        // canvas.drawRect(ofsX-1,ofsY-1,ofsX+displayWidth+1,ofsY+displayHeight-1,paint_frame);

        if (lines == null) return;

        canvas.clipRect(AX.pxMin,AY.pxMax,AX.pxMax,AY.pxMin);

        if (displayOnlyOne) {
            if ((displayOne>=0) && (displayOne < lines.size())) {
                lines.get(displayOne).plot(canvas, AX,AY, displayOfs, displayScale);
            }
        } else {
            for (XYdata l:lines)
                l.plot(canvas, AX,AY, displayOfs, displayScale);

            // Legend
            int n = 0;
            for (XYdata l:lines)
                if (!l.hidden) {
                    l.pnt.setTextSize(pc.stdFontSize);
                    canvas.drawText(l.name, AX.pxMax-pc.fontspace, AY.pxMax + pc.fontspace + pc.yLableHeight +
                            pc.yLableHeight * 2 * n, l.pnt);
                    n++;
                }
        }
        canvas.clipRect(0, 0, getWidth() - 1, getHeight() - 1);
    }

    public XYdata addPlot(String name) {
        if (name == null) name="undefined";
        if (lines==null)
            lines=new ArrayList<XYdata>();
        float stdsize = new Button(root).getTextSize();
        XYdata xydata=new XYdata(0.75f*stdsize, name, lines.size(), getColor(lines.size()) ,64);
        lines.add(xydata);
        return xydata;
    }

    public XYdata addPlot(XYdata xydata) {
        if (lines == null) lines=new ArrayList<XYdata>();
        xydata.index=lines.size();
        lines.add(xydata);
        return xydata;
    }

    public void display() {
        invalidate();
    }

    public void autozoom() {
        if ((lines == null) || (lines.size() < 1)) {
            AY.aMin = defaultyMin*displayScale+displayOfs;
            AY.aMax = defaultyMax*displayScale+displayOfs;
            AX.aMin = defaultxMin;
            AX.aMax = defaultxMax;
        } else {
            if (displayOnlyOne) {
                // Display only one Line at a time
                if ((displayOne >= 0) && (displayOne < lines.size()) && (lines.get(displayOne).points > 1)) {
                    RectF r = lines.get(displayOne).getRange();
                    r.top*=displayScale;
                    r.bottom*=displayScale;
                    r.top += displayOfs;
                    r.bottom += displayOfs;
                    AX.aMin = r.left;
                    AX.aMax = r.right;
                    AY.aMin = r.bottom;
                    AY.aMax = r.top;
                } else {
                    AY.aMin = defaultyMin*displayScale+displayOfs;
                    AY.aMax = defaultyMax*displayScale+displayOfs;
                    AX.aMin = defaultxMin;
                    AX.aMax = defaultxMax;
                }
            } else {
                // Display all
                boolean foundone = false;
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).points > 1) {
                        RectF r = lines.get(i).getRange();
                        r.top*=displayScale;
                        r.bottom*=displayScale;
                        r.top += displayOfs;
                        r.bottom += displayOfs;
                        if (!foundone) {
                            AX.aMin = r.left;
                            AX.aMax = r.right;
                            AY.aMin = r.bottom;
                            AY.aMax = r.top;
                            foundone = true;
                        } else {
                            if (r.left < AX.aMin) AX.aMin = r.left;
                            if (r.right > AX.aMax) AX.aMax = r.right;
                            if (r.bottom < AY.aMin) AY.aMin = r.bottom;
                            if (r.top > AY.aMax) AY.aMax = r.top;
                        }
                    }
                }
                if (!foundone) {
                    AY.aMin = defaultyMin*displayScale + displayOfs;
                    AY.aMax = defaultyMax*displayScale + displayOfs;
                    AX.aMin = defaultxMin;
                    AX.aMax = defaultxMax;
                }
            }

            // Verify Data
            if (AX.aMin >= AX.aMax) {
                AX.aMax=AX.aMin+1;
            }
            if (AY.aMin >= AY.aMax) {
                AY.aMax = AY.aMin + 1;
            }
            if (AX.logScale && (AX.aMin <= 0)) {
                AX.aMin=defaultxMin;
                if (AX.aMax <= AX.aMin) AX.aMax=AX.aMin+1;
            }
            if (AY.logScale && (AY.aMin <= 0)) {
                AY.aMin=defaultyMin+displayOfs;
                if (AY.aMax <= AY.aMin) AY.aMax=AY.aMin+1;
            }
        }
        storePreferences();
        invalidate();
    }

    public void deleteMenu(View v) {
        if ((lines == null) || (lines.size() < 1)) return;
        PopupMenu popup = new PopupMenu(root, v);
        Menu menu = popup.getMenu();
        menu.add(0,0,Menu.NONE,"Remove ALL");
        for (int i=0;i<lines.size();i++) {
            menu.add(0,i+1,Menu.NONE,lines.get(i).name);
        }
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int i=item.getItemId();
                if (i == 0) {
                    // Remove all
                    lines.clear();
                    if (displayOnlyOne)
                        displayOne=-1;
                } else {
                    i--;
                    if ((i < 0) || (i >= lines.size()))
                        return false;
                    lines.remove(i);
                    if (displayOnlyOne) {
                        if (displayOne > i) displayOne--;
                    }
                }
                invalidate();
                return false;
            }
        });
        popup.show();
    }

    public void colorMenu(View v) {
        if ((lines == null) || (lines.size() < 1)) return;
        PopupMenu popup = new PopupMenu(root, v);
        final Menu menu = popup.getMenu();
        // final XYPlot xySave=this;
        final View vsave=v;
        for (int i=0;i<lines.size();i++) {
            menu.add(0,i+1,Menu.NONE,lines.get(i).name);
        }
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int i=item.getItemId()-1;
                if ((i < 0) || (i >= lines.size())) return false;
                PopupMenu submenu=new PopupMenu(root, vsave);
                Menu smenu = submenu.getMenu();
                for (int j=0;j<getColorNum();j++) {
                    MenuItem it=menu.add(i, j + 1, Menu.NONE,
                            getColoredColorName(j));
                }
                submenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int i = item.getGroupId();
                        lines.get(i).color=getColor(item.getItemId()-1);
                        XYPlot.this.invalidate();
                        //xySave.invalidate();
                        return false;
                    }
                });
                submenu.show();
                return false;
            }
        });
        popup.show();
    }


}
