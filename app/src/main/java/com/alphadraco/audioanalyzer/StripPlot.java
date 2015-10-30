package com.alphadraco.audioanalyzer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.widget.Button;

import java.util.ArrayList;

/**
 * Created by aladin on 29.10.2015.
 */
public class StripPlot {
    public String name;
    public int index;
    public int color;
    public boolean active;
    public StripPlotView plotview;

    public int maxPoints;
    public int points;
    public float[] data;
    public float[] adata;

    public Paint pnt;

    private float[] pointcache;

    public void updatePoints(int _maxPoints) {
        if (_maxPoints == maxPoints) return;
        if (_maxPoints <= 0) {
            points=0;
            maxPoints=0;
            data=null;
            adata=null;
        } else {
            if (_maxPoints > maxPoints) {
                float[] dnew=new float[_maxPoints];
                for (int i=0;i<points;i++)
                    dnew[i]=data[i];
                data=dnew;
                dnew=new float[_maxPoints];
                for (int i=0;i<points;i++)
                    dnew[i]=adata[i];
                adata=dnew;
                maxPoints=_maxPoints;
            } else {
                // _maxPoints <= maxPoints
                float[] dnew =new float[_maxPoints];
                for (int i=0;(i<points) && (i<_maxPoints);i++)
                    dnew[i]=data[i];
                data=dnew;
                dnew =new float[_maxPoints];
                for (int i=0;(i<points) && (i<_maxPoints);i++)
                    dnew[i]=adata[i];
                if (points > _maxPoints) points=_maxPoints;
                adata=dnew;
                maxPoints=_maxPoints;
            }
        }
    }

    public StripPlot(StripPlotView vw, String _name, int _index, int _color, int _maxPoints) {
        name=_name;
        index=_index;
        color=_color;
        maxPoints=_maxPoints;
        points=0;
        plotview=vw;

        data=new float[maxPoints];
        adata=new float[maxPoints];
        pnt=new Paint();
        pnt.setColor(color);
        pnt.setStyle(Paint.Style.STROKE);
        pnt.setTextAlign(Paint.Align.CENTER);
        float stdsize = new Button(plotview.root).getTextSize();
        pnt.setTextSize(0.75f*stdsize);

        pointcache=null;
    }

    public void plot(Canvas canvas, StripPlotView view) {
        if (points < 2) return;

        if ((pointcache == null) || (pointcache.length != maxPoints*4))
            pointcache=new float[maxPoints*4];
        float x0=view.displayWidth-1+view.ofsX;
        float y0=view.displayHeight-1-(data[0]-view.yMin)*(view.displayHeight-1)/(view.yMax-view.yMin)+view.ofsY;
        pnt.setColor(Color.argb(255,((color>>16)&0x000000FF)/2,((color>>8)&0x000000FF)/2,(color&0x000000FF)/2));
        for (int i=1;i<points;i++) {
            float x1=(view.displayWidth-1)-(view.displayWidth-1)*(float)i/(view.displayPoints-1)+view.ofsX;
            float y1=view.displayHeight-1-(data[i]-view.yMin)*(view.displayHeight-1)/(view.yMax-view.yMin)+view.ofsY;
            pointcache[(i-1)*4+0]=x0;
            pointcache[(i-1)*4+1]=y0;
            pointcache[(i-1)*4+2]=x1;
            pointcache[(i-1)*4+3]=y1;
            x0=x1;
            y0=y1;
        }
        canvas.drawLines(pointcache, pnt);
        x0=view.displayWidth-1+view.ofsX;
        y0=view.displayHeight-1-(adata[0]-view.yMin)*(view.displayHeight-1)/(view.yMax-view.yMin)+view.ofsY;
        pnt.setColor(color);
        for (int i=1;i<points;i++) {
            float x1=(view.displayWidth-1)-(view.displayWidth-1)*(float)i/(view.displayPoints-1)+view.ofsX;
            float y1=view.displayHeight-1-(adata[i]-view.yMin)*(view.displayHeight-1)/(view.yMax-view.yMin)+view.ofsY;
            pointcache[(i-1)*4+0]=x0;
            pointcache[(i-1)*4+1]=y0;
            pointcache[(i-1)*4+2]=x1;
            pointcache[(i-1)*4+3]=y1;
            x0=x1;
            y0=y1;
        }
        canvas.drawLines(pointcache, pnt);
    }

    public void add(float f, float f2) {
        if ((data == null) || (maxPoints < 1)) return;
        if (points > 0) {
            if (points < maxPoints) {
                System.arraycopy(data, 0, data, 1, points);
                System.arraycopy(adata, 0, adata, 1, points);
            } else {
                System.arraycopy(data, 0, data, 1, maxPoints - 1);
                System.arraycopy(adata, 0, adata, 1, maxPoints - 1);
            }
        }
        data[0]=f;
        adata[0]=f2;
        if (points < maxPoints) points++;
    }
}
