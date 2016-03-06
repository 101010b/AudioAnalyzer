package com.alphadraco.audioanalyzer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.widget.Button;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by aladin on 29.10.2015.
 */
public class XYdata {
    public String name;
    public int index;
    public int color;
    public boolean hidden;
    // public XYPlot plotview;

    public int maxPoints;
    public int points;
    public float[] X;
    public float[] Y;
    public boolean useminmax;
    public float[] Ymin;
    public float[] Ymax;


    private float[] pointcache;
    private float[] pointcacheminmax;

    public Paint pnt;

    private void setup(XYPlot vw, String _name, int _index, int _color, int _maxPoints) {
        name=_name;
        index=_index;
        color=_color;
        maxPoints=_maxPoints;
        points=0;
        useminmax=false;
        Ymin=null;
        Ymax=null;
        hidden=false;

        pointcache=null;
        pointcacheminmax=null;

        if (maxPoints > 0) {
            X = new float[maxPoints];
            Y = new float[maxPoints];
        } else {
            X=Y=null;
        }

        pnt=new Paint();
        pnt.setColor(color);
        pnt.setStyle(Paint.Style.FILL);
        pnt.setTextAlign(Paint.Align.RIGHT);
        float stdsize = new Button(vw.root).getTextSize();
        pnt.setTextSize(0.75f*stdsize);
    }

    public XYdata(XYPlot vw, String _name, int _index, int _color, int _maxPoints) {
        setup(vw,_name,_index,_color,_maxPoints);
    }

    public XYdata(XYPlot vw, String _name, int _index, int _color) {
        setup(vw,_name,_index,_color,0);
    }

    public XYdata(XYPlot vw, String _name) {
        setup(vw,name,-1,vw.getNextColor(),0);
    }

    public XYdata(XYPlot vw) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        setup(vw,sdf.format(cal.getTime()),-1,vw.getNextColor(),0);
    }

    public RectF getRange() {
        if (points < 1) return null;
        RectF r=new RectF();
        if (useminmax) {
            r.left = X[0];
            r.right = X[0];
            r.top = Ymax[0];
            r.bottom = Ymin[0];
            for (int i = 1; i < points; i++) {
                if (X[i] < r.left) r.left = X[i];
                if (X[i] > r.right) r.right = X[i];
                if (Ymin[i] < r.bottom) r.bottom = Ymin[i];
                if (Ymax[i] > r.top) r.top = Ymax[i];
            }
        } else {
            r.left = X[0];
            r.right = X[0];
            r.top = Y[0];
            r.bottom = Y[0];
            for (int i = 1; i < points; i++) {
                if (X[i] < r.left) r.left = X[i];
                if (X[i] > r.right) r.right = X[i];
                if (Y[i] < r.bottom) r.bottom = Y[i];
                if (Y[i] > r.top) r.top = Y[i];
            }
        }
        return r;
    }

    public void plot(Canvas canvas, PlotAxis AX, PlotAxis AY) {
        plot(canvas,AX,AY,0.0f,1.0f);
    }

    public void plot(Canvas canvas, PlotAxis AX, PlotAxis AY, float ofs) {
        plot(canvas,AX,AY,ofs,1.0f);
    }

    public void plot(Canvas canvas, PlotAxis AX, PlotAxis AY, float ofs, float scale) {
        if (points < 2) return;
        if (hidden) return;

        if ((pointcache == null) || (pointcache.length < maxPoints*4)) {
            pointcache = new float[maxPoints * 4];
            if (useminmax)
                pointcacheminmax=new float[maxPoints * 8];
        }


        int pcps=0;
        if (scale == 1.0f) {
            // No Scaling...
            float x0 = AX.getPos(X[0]);
            float y0 = AY.getPos(Y[0] + ofs);
            float y0min = 0;
            float y0max = 0;
            if (useminmax) {
                y0min = AY.getPos(Ymin[0] + ofs);
                y0max = AY.getPos(Ymax[0] + ofs);
            }

            for (int i = 1; i < points; i++) {
                float x1 = AX.getPos(X[i]);
                float y1 = AY.getPos(Y[i] + ofs);
                pointcache[(i - 1) * 4 + 0] = x0;
                pointcache[(i - 1) * 4 + 1] = y0;
                pointcache[(i - 1) * 4 + 2] = x1;
                pointcache[(i - 1) * 4 + 3] = y1;
                float y1min = 0;
                float y1max = 0;
                if (useminmax) {
                    y1min = AY.getPos(Ymin[i] + ofs);
                    y1max = AY.getPos(Ymax[i] + ofs);
                    pointcacheminmax[(i - 1) * 4 + 0] = x0;
                    pointcacheminmax[(i - 1) * 4 + 1] = y0min;
                    pointcacheminmax[(i - 1) * 4 + 2] = x1;
                    pointcacheminmax[(i - 1) * 4 + 3] = y1min;

                    pointcacheminmax[maxPoints * 8 - 1 - (i - 1) * 4 - 1] = x0;
                    pointcacheminmax[maxPoints * 8 - 1 - (i - 1) * 4 - 0] = y0max;
                    pointcacheminmax[maxPoints * 8 - 1 - (i - 1) * 4 - 3] = x1;
                    pointcacheminmax[maxPoints * 8 - 1 - (i - 1) * 4 - 2] = y1max;
                }
                x0 = x1;
                y0 = y1;
                if (useminmax) {
                    y0min = y1min;
                    y0max = y1max;
                }
            }
        } else {
            // With scaling
            float x0 = AX.getPos(X[0]);
            float y0 = AY.getPos(Y[0]*scale + ofs);
            float y0min = 0;
            float y0max = 0;
            if (useminmax) {
                y0min = AY.getPos(Ymin[0]*scale + ofs);
                y0max = AY.getPos(Ymax[0]*scale + ofs);
            }

            for (int i = 1; i < points; i++) {
                float x1 = AX.getPos(X[i]);
                float y1 = AY.getPos(Y[i]*scale + ofs);
                pointcache[(i - 1) * 4 + 0] = x0;
                pointcache[(i - 1) * 4 + 1] = y0;
                pointcache[(i - 1) * 4 + 2] = x1;
                pointcache[(i - 1) * 4 + 3] = y1;
                float y1min = 0;
                float y1max = 0;
                if (useminmax) {
                    y1min = AY.getPos(Ymin[i]*scale + ofs);
                    y1max = AY.getPos(Ymax[i]*scale + ofs);
                    pointcacheminmax[(i - 1) * 4 + 0] = x0;
                    pointcacheminmax[(i - 1) * 4 + 1] = y0min;
                    pointcacheminmax[(i - 1) * 4 + 2] = x1;
                    pointcacheminmax[(i - 1) * 4 + 3] = y1min;

                    pointcacheminmax[maxPoints * 8 - 1 - (i - 1) * 4 - 1] = x0;
                    pointcacheminmax[maxPoints * 8 - 1 - (i - 1) * 4 - 0] = y0max;
                    pointcacheminmax[maxPoints * 8 - 1 - (i - 1) * 4 - 3] = x1;
                    pointcacheminmax[maxPoints * 8 - 1 - (i - 1) * 4 - 2] = y1max;
                }
                x0 = x1;
                y0 = y1;
                if (useminmax) {
                    y0min = y1min;
                    y0max = y1max;
                }
            }
        }

        pnt.setStyle(Paint.Style.STROKE);
        if (useminmax) {
            pnt.setStrokeWidth(1.0f);
            pnt.setColor(Color.argb(255, ((color >> 16) & 0x000000FF) / 3, ((color >> 8) & 0x000000FF) / 3, (color & 0x000000FF) / 3));
            canvas.drawLines(pointcacheminmax, 0, points * 4, pnt);
            canvas.drawLines(pointcacheminmax, maxPoints*4, points * 4, pnt);
        }
        pnt.setColor(color);
        pnt.setStrokeWidth(3.0f);
        canvas.drawLines(pointcache, 0, points*4, pnt);
        pnt.setStyle(Paint.Style.FILL);
    }

    public void add(float _x, float _y) {
        if (points + 1 > maxPoints) {
            // Extend Array
            int mp=maxPoints*2;
            if (mp == 0) mp=64;
            float[] _X = new float[mp];
            float[] _Y = new float[mp];
            if (points > 0) {
                System.arraycopy(X, 0, _X, 0, points);
                System.arraycopy(Y, 0, _Y, 0, points);
            }
            X=_X;
            Y=_Y;
            maxPoints=mp;
        }
        X[points]=_x;
        Y[points]=_y;
        points++;
    }

    public void add(float[] _x, float[] _y, int n) {
        if (n < 1) return;
        if ((_x == null) || (_y == null)) return;
        if ((_x.length < n) || (_y.length < n)) return;
        if (n == 1) {
            add(_x[0], _y[0]);
            return;
        }
        if (points + n > maxPoints) {
            // Extend Array
            int mp=maxPoints*2;
            if (mp == 0) mp = 64;
            while (points + n > mp) mp=mp*2;
            float[] _X = new float[mp];
            float[] _Y = new float[mp];
            if (points > 0) {
                System.arraycopy(X, 0, _X, 0, points);
                System.arraycopy(Y, 0, _Y, 0, points);
            }
            X=_X;
            Y=_Y;
            maxPoints=mp;
        }
        System.arraycopy(_x,0,X,points,n);
        System.arraycopy(_y,0,Y,points,n);
        points+=n;
    }

    public void add(float[] _x, short[] _y, float scale, int n) {
        if (n < 1) return;
        if ((_x == null) || (_y == null)) return;
        if ((_x.length < n) || (_y.length < n)) return;
        if (n == 1) {
            add(_x[0], _y[0]*scale);
            return;
        }
        if (points + n > maxPoints) {
            // Extend Array
            int mp=maxPoints*2;
            if (mp == 0) mp = 64;
            while (points + n > mp) mp=mp*2;
            float[] _X = new float[mp];
            float[] _Y = new float[mp];
            if (points > 0) {
                System.arraycopy(X, 0, _X, 0, points);
                System.arraycopy(Y, 0, _Y, 0, points);
            }
            X=_X;
            Y=_Y;
            maxPoints=mp;
        }
        System.arraycopy(_x,0,X,points,n);
        for (int i=0;i<n;i++) {
            Y[points]=scale*(float)_y[i];
            points++;
        }
    }

    public void add(float[] _x, float[] _y) {
        if ((_x == null) || (_y == null)) return;
        if (_x.length != _y.length) return;
        if (_x.length == 0) return;
        add(_x,_y,_x.length);
    }

    public void conv_to_lin() {
        for (int i=0;i<points;i++) {
            Y[i]=(float)Math.pow(10.0,Y[i]/10.0f); // Power
        }
    }

    public void scale_Y(float f) {
        for (int i=0;i<points;i++) Y[i]*=f;
    }

    public void conv_to_log() {
        for (int i=0;i<points;i++) {
            if (Y[i] < 1e-14)
                Y[i]=-140.0f;
            else
                Y[i]=10.0f*(float)Math.log10(Y[i]);
        }
    }

    public void add_dsetminmax(float [] _Y) {
        if (_Y.length < points) return;
        if (!useminmax) {
            Ymin=Y.clone();
            Ymax=Y.clone();
            useminmax=true;
        }
        for (int i=0;i<points;i++) {
            if (_Y[i] < Ymin[i]) Ymin[i]=_Y[i];
            if (_Y[i] > Ymax[i]) Ymax[i]=_Y[i];
        }
    }

    public void add_dsetavgpwr(float [] _Y) {
        if (_Y.length < points) return;
        for (int i=0;i<points;i++) Y[i]+=Math.pow(10.0,_Y[i]/10.0);
    }

    public void reset() {
        points=0;
    }



}
