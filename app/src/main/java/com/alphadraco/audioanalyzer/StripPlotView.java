package com.alphadraco.audioanalyzer;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;

import java.util.ArrayList;

/**
 * Created by aladin on 29.10.2015.
 */
public class StripPlotView extends View {

    // Prefs
    private SharedPreferences sharedPreferences=null;
    public SharedPreferences.OnSharedPreferenceChangeListener PrefListener;
    public AudioAnalyzer root;

    // Settings
    public float yMin,yMax;
    public float ofs;
    public String unit;
    public boolean running;

    private ArrayList<StripPlot> lines;

    // Configuration
    public int displayPoints;

    // Display Coordinates
    public int displayWidth;
    public int displayHeight;
    public int ofsX;
    public int ofsY;

    // Painting
    private Paint paint_frame;
    private Paint paint_grid;
    private Paint paint_unit;
    private Paint paint_gridX;
    private float fctr;
    private float fontspace;
    private float legendwidth;


    // Control
    float storeyMin,storeyMax;
    float PT0X, PT0Y, PT1X, PT1Y;
    int pointers=0;



    public void updatePoints(int dpts){
        if (dpts != displayPoints) {
            if (dpts == 0) {
                if (lines != null) {
                    for (int i = 0; i < lines.size(); i++)
                        lines.get(i).updatePoints(dpts);
                }
            } else {
                if (lines != null) {
                    for (int i=0;i < lines.size();i++)
                        lines.get(i).updatePoints(dpts);
                }
            }
            displayPoints=dpts;
        }
    }

    public void storePreferenceList() {
        if ((root == null) || (sharedPreferences == null)) return;
        if ((lines==null) || (lines.size() < 1)) {
            SharedPreferences.Editor e=sharedPreferences.edit();
            e.putInt("StripLines", 0);
            e.apply();
            return;
        }
        SharedPreferences.Editor e=sharedPreferences.edit();
        e.putInt("StripLines",lines.size());
        for (int i=0;i<lines.size();i++) {
            e.putString(String.format("StripLine%dName", i), lines.get(i).name);
            e.putInt(String.format("StripLine%dColor", i), lines.get(i).color);
        }
        e.apply();
    }

    public void setPreferences(AudioAnalyzer _root, SharedPreferences prefs) {
        root=_root;
        sharedPreferences = prefs;
        if (sharedPreferences != null) {
            yMin = sharedPreferences.getFloat("StripYMin", -120.0f);
            yMax = sharedPreferences.getFloat("StripYMax", 0.0f);
            // int dpts = sharedPreferences.getInt("StripPoints", 64);
            int dpts=Integer.parseInt(sharedPreferences.getString("StripPlotLength","64"));
            if (dpts < 64) dpts=64;
            if (dpts > 1024) dpts=1024;
            updatePoints(dpts);

            int dpl=sharedPreferences.getInt("StripLines",0);
            lines=new ArrayList<StripPlot>(10);
            if (dpl > 0) {
                for (int i=0;i<dpl;i++) {
                    String lineName=sharedPreferences.getString(String.format("StripLine%dName",i),"");
                    int lineColor=sharedPreferences.getInt(String.format("StripLine%dColor", i), 0xFF00FF00);
                    if (lineName.length() > 0) {
                        int found=-1;
                        for (int j=0;j<DataConsolidator.powerTrackNames.length;j++)
                            if (DataConsolidator.powerTrackNames[j].equals(lineName))
                                found=j;
                        if (found >= 0) {
                            lines.add(new StripPlot(StripPlotView.this,DataConsolidator.powerTrackNames[found],found,lineColor,dpts));
                        }
                    }
                }
            }
            if (PrefListener != null)
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(PrefListener);
            PrefListener=new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    if (key.equals("StripPlotLength")) {
                        int dps=Integer.parseInt(sharedPreferences.getString(key,"64"));
                        if (dps < 64) dps=64;
                        if (dps > 1024) dps=1024;
                        updatePoints(dps);
                    }
                }
            };
            sharedPreferences.registerOnSharedPreferenceChangeListener(PrefListener);
        }
    }

    private void setup(Context context) {
        root=null;
        sharedPreferences=null;
        PrefListener=null;

        lines=null;
        displayPoints=64;

        // Init Display
        float stdsize = new Button(context).getTextSize();

        paint_frame=new Paint();
        paint_frame.setColor(Color.WHITE);
        paint_frame.setStyle(Paint.Style.STROKE);
        paint_frame.setStrokeWidth(2);
        paint_frame.setTextAlign(Paint.Align.LEFT);
        paint_frame.setTextSize(stdsize * 0.75f);

        paint_unit=new Paint();
        paint_unit.setColor(Color.WHITE);
        paint_unit.setTextAlign(Paint.Align.LEFT);
        paint_unit.setTextSize(stdsize * 0.75f);


        paint_grid=new Paint();
        paint_grid.setColor(Color.GRAY);
        paint_grid.setStyle(Paint.Style.STROKE);
        paint_grid.setTextSize(stdsize * 0.75f);
        paint_grid.setTextAlign(Paint.Align.RIGHT);

        paint_gridX=new Paint();
        paint_gridX.setColor(Color.GRAY);
        paint_gridX.setStyle(Paint.Style.STROKE);
        paint_gridX.setTextSize(stdsize * 0.75f);
        paint_gridX.setTextAlign(Paint.Align.CENTER);


        String Sw = "-120.0 dB";
        Rect rct=new Rect();
        paint_grid.getTextBounds(Sw, 0, Sw.length(), rct);
        fctr=rct.height();
        fontspace=fctr/5;
        ofsX=rct.width()+5;

        Sw="A10-20k";
        paint_grid.getTextBounds(Sw, 0, Sw.length(), rct);
        legendwidth=rct.width();

        ofsY=2;
        displayWidth=getWidth()-ofsX-2;
        displayHeight=getHeight()-ofsY-2*(int)fctr-2;

        ofs=0;
        unit="dBFS";
        yMin=-120.0f;
        yMax=0.0f;

        running=true;
    }

    public StripPlotView(Context context) {
        super(context);
        setup(context);
    }

    public StripPlotView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                storeyMin=yMin;
                storeyMax=yMax;

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
                    SharedPreferences.Editor E=sharedPreferences.edit();
                    E.putFloat("StripYMin",yMin);
                    E.putFloat("StripYMax",yMax);
                    E.apply();
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
                    float dy = e.getY() - PT0Y;
                    float dl = dy * (storeyMax - storeyMin) / (getHeight() - 1 - ofsY - 1);
                    yMin = storeyMin + dl;
                    yMax = storeyMax + dl;
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
                        float dy = (cy1 - cy0) / (getHeight() - 1 - ofsY - 1);
                        yMin = (storeyMax + storeyMin) / 2 + dy * (storeyMax - storeyMin) - yscale * (storeyMax - storeyMin) / 2;
                        yMax = (storeyMax + storeyMin) / 2 + dy * (storeyMax - storeyMin) + yscale * (storeyMax - storeyMin) / 2;
                    }
                }
                break;
        }
        return true;
    }

    public String getdBstring(float db) {
        return String.format("%1.0f",db);
    }

    public String getTstring(float t) {
        return String.format("%1.0f",t);
    }

    public void drawGridSpectrum(Canvas canvas) {

        float lminX = yMin + ofs;
        float lmaxX = yMax + ofs;

        int i1 = (int) Math.floor(lminX / 10.0);
        int i2 = (int) Math.ceil(lmaxX / 10.0);
        for (int i = i1; i <= i2; i++) {
            int Y = (int) (ofsY + displayHeight - 1) - (int) ((i * 10.0 - lminX) * (displayHeight - 1) / (lmaxX - lminX));
            if ((Y >= ofsY) && (Y < ofsY + displayHeight))
                canvas.drawLine(ofsX, Y, ofsX+displayWidth - 1, Y, paint_grid);

            if ((Y > fctr) && (Y < displayHeight - 1 - ofsY - fctr))
                canvas.drawText(getdBstring(i * 10.0f), ofsX - fontspace, Y + fctr / 2, paint_grid);
        }

        float tmin=0;
        float tmax=10;

        if ((root != null) && (root.dataConsolidator != null))
            tmax=displayPoints*root.dataConsolidator.len/root.dataConsolidator.fs;

        float lstep=100.0f;
        float istep=100.0f;
        String fmt="%1.0f";
        if (tmax < 0.1) {
            lstep = 0.01f;
            istep = 0.05f;
            fmt = "%1.2f";
        } else if (tmax < 0.5) {
            lstep=0.1f;
            istep=0.1f;
            fmt="%1.1f";
        } else if (tmax < 1) {
            lstep=0.1f;
            istep=0.1f;
            fmt="%1.1f";
        } else if (tmax < 5) {
            lstep=0.1f;
            istep=0.5f;
            fmt="%1.1f";
        } else if (tmax < 10) {
            lstep=1;
            istep=1;
            fmt="%1.0f";
        } else if (tmax < 50) {
            lstep=5;
            istep=10;
            fmt="%1.0f";
        } else if (tmax < 100) {
            lstep=10;
            istep=50;
            fmt="%1.0f";
        }
        for (int i=1;i<=(int)Math.floor(tmax/lstep);i++) {
            float X = (displayWidth - 1) - (displayWidth - 1) * (float) i * lstep / tmax + ofsX;
            canvas.drawLine(X, ofsY, X, ofsY + displayHeight, paint_gridX);
        }
        for (int i=1;i<=(int)Math.floor(tmax/istep);i++) {
            float X = (displayWidth - 1) - (displayWidth - 1) * (float) i * istep / tmax + ofsX;
            canvas.drawText(String.format(fmt,(float)i*istep),X,ofsY+displayHeight+fctr+fontspace,paint_gridX);
        }

        canvas.drawText(unit,ofsX+fontspace,ofsY+2*fctr,paint_unit);

    }

    public void drawGrid(Canvas canvas) {

        drawGridSpectrum(canvas);

    }


    @Override
    public void onDraw(Canvas canvas) {
        displayWidth=getWidth()-ofsX-2;
        displayHeight=getHeight()-ofsY-2*(int)fctr-2;

        drawGrid(canvas);
        canvas.drawRect(ofsX-1,ofsY-1,ofsX+displayWidth+1,ofsY+displayHeight-1,paint_frame);

        if (lines == null) return;
        if (lines.size() < 1) return;

        canvas.clipRect(ofsX,ofsY,ofsX+displayWidth-1,ofsY+displayHeight-1);
        for (int i=0;i<lines.size();i++)
            lines.get(i).plot(canvas,this);
        for (int i=0;i<lines.size();i++) {
            canvas.drawText(lines.get(i).name,ofsX+displayWidth-legendwidth,ofsY +2*fctr+fctr*2*i,lines.get(i).pnt);
        }
        canvas.clipRect(0, 0, getWidth() - 1, getHeight() - 1);
    }

    public void add() {
        if ((lines == null) || (lines.size() < 1)) return;
        if ((root == null) || (root.dataConsolidator == null)) return;
        if (!running) return;
        for (int i=0;i<lines.size();i++)
            if ((lines.get(i).index >= 0) && (lines.get(i).index < DataConsolidator.powerTrackNames.length))
                lines.get(i).add(root.dataConsolidator.powerTracks[lines.get(i).index].current,
                        root.dataConsolidator.powerTracks[lines.get(i).index].average);
    }

    public void display() {
        invalidate();
    }

    public void install_handlers(ImageButton ib_add, ImageButton ib_del, final ImageButton ib_edit, ImageButton ib_zoom) {
        ib_zoom.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                yMin=-120.0f;
                yMax=0.0f;
                SharedPreferences.Editor e=sharedPreferences.edit();
                e.putFloat("StripYMin",yMin);
                e.putFloat("StripYMax",yMax);
                e.apply();
            }
        });

        ib_add.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(root, v);
                Menu menu = popup.getMenu();
                for (int i=0;i<DataConsolidator.powerTrackNames.length;i++) {
                    menu.add(0,i+1,Menu.NONE,DataConsolidator.powerTrackNames[i]);
                }
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int i=item.getItemId()-1;
                        if ((i < 0) || (i >= DataConsolidator.powerTrackNames.length)) return false;
                        String srcname=DataConsolidator.powerTrackNames[i];
                        if (lines == null) lines=new ArrayList<StripPlot>(10);
                        lines.add(new StripPlot(StripPlotView.this, srcname, i, 0xFFFF0000, displayPoints));
                        storePreferenceList();
                        return false;
                    }
                });
                popup.show();
            }
        });

        ib_del.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((lines == null) || (lines.size() < 1)) return;
                PopupMenu popup = new PopupMenu(root, v);
                Menu menu = popup.getMenu();
                for (int i=0;i<lines.size();i++) {
                    menu.add(0,i+1,Menu.NONE,lines.get(i).name);
                }
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int i=item.getItemId()-1;
                        if ((i < 0) || (i >= lines.size())) return false;
                        lines.remove(i);
                        storePreferenceList();
                        invalidate();
                        return false;
                    }
                });
                popup.show();
            }
        });

        ib_edit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((lines == null) || (lines.size() < 1)) return;
                PopupMenu popup = new PopupMenu(root, v);
                Menu menu = popup.getMenu();
                for (int i=0;i<lines.size();i++) {
                    menu.add(0,i+1,Menu.NONE,lines.get(i).name);
                }
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int i=item.getItemId()-1;
                        if ((i < 0) || (i >= lines.size())) return false;

                        PopupMenu submenu=new PopupMenu(root, ib_edit);
                        Menu smenu = submenu.getMenu();
                        smenu.add(i,1,Menu.NONE,"Red");
                        smenu.add(i,2,Menu.NONE,"Green");
                        smenu.add(i,3,Menu.NONE,"Blue");
                        smenu.add(i,4,Menu.NONE,"Yellow");
                        smenu.add(i,5,Menu.NONE,"Cyan");
                        smenu.add(i,6,Menu.NONE,"Magenta");
                        smenu.add(i,7,Menu.NONE,"White");
                        submenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                int i = item.getGroupId();
                                switch (item.getItemId()) {
                                    case 1: lines.get(i).color = 0xFFFF0000;break;
                                    case 2: lines.get(i).color = 0xFF00FF00;break;
                                    case 3: lines.get(i).color = 0xFF0000FF;break;
                                    case 4: lines.get(i).color = 0xFFFFFF00;break;
                                    case 5: lines.get(i).color = 0xFF00FFFF;break;
                                    case 6: lines.get(i).color = 0xFFFF00FF;break;
                                    case 7: lines.get(i).color = 0xFFFFFFFF;break;
                                }
                                storePreferenceList();
                                invalidate();
                                return false;
                            }
                        });
                        submenu.show();

                        return false;
                    }
                });
                popup.show();
            }
        });

    }

}
