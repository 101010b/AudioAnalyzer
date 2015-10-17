package com.alphadraco.audioanalyzer;

import android.graphics.Bitmap;

/**
 * Created by aladin on 16.10.2015.
 */
public class ColorTable {

    public String id;
    public int[] table;
    public Bitmap bar;

    public ColorTable(int n, String s) {
        table = new int[n];
        bar=null;
        id="";
        build(s);
    }

    public void buildBar() {
        if ((table == null) || (table.length < 1)) {
            bar=null;
            return;
        }
        bar=Bitmap.createBitmap(table.length,1, Bitmap.Config.ARGB_8888);
        bar.setPixels(table,0,table.length,0,0,table.length,1);
    }

    private boolean getcolor(char c, float[] col)
    {
        char U = Character.toUpperCase(c);
        float r,g,b;
        switch (U)
        {
            default:
                return false;
            case 'K': r = 0; g = 0; b = 0; break;

            case 'R': r = 1; g = 0; b = 0; break;
            case 'G': r = 0; g = 1; b = 0; break;
            case 'B': r = 0; g = 0; b = 1; break;

            case 'Y': r = 1; g = 1; b = 0; break;
            case 'M': r = 1; g = 0; b = 1; break;
            case 'C': r = 0; g = 1; b = 1; break;

            case 'W': r = 1; g = 1; b = 1; break;

        }
        if (U != c)
        {
            r /= 2.0;
            g /= 2.0;
            b /= 2.0;
        }
        col[0]=r;
        col[1]=g;
        col[2]=b;
        return true;
    }

    private float getspacer(char c)
    {
        float w;
        switch (c)
        {
            default:
                return -1.0f;
            case ' ': w = 0.0f; break;
            case '-': w = 1.0f; break;
            case '.': w = 0.5f; break;
        }
        return w;
    }

    private boolean isspacer(char c)
    {
        return getspacer(c) >= 0.0f;
    }

    private boolean iscolor(char c)
    {
        float[] f=new float[3];
        return getcolor(c, f);
    }

    public int col(int x) {
        if (table==null) return 0;
        if (table.length < 1) return 0;
        if (x <= 0)
            return table[0];
        if (x >= table.length)
            return table[table.length-1];
        return table[x];
    }

    public int col(float x)
    {
        return col((int)Math.floor(x*(table.length-1.0f)+0.5f));
    }

    private void build()
    {
        if (table == null) return;
        for (int i = 0; i < table.length; i++)
            table[i]=0;
        id="";
        bar=null;
    }

    private int to_int(float f) {
        if (f <= 0.0f) return 0;
        if (f >= 1.0f) return 255;
        return (int)Math.floor(f*255.0+0.5f);
    }

    private int to_color(float cr, float cg, float cb) {
        return 0xFF000000 + to_int(cb)+(to_int(cg)<<8)+(to_int(cr)<<16);
    }

    private int to_color(float[] f) {
        if (f == null) return 0;
        if (f.length < 3) return 0;
        return to_color(f[0], f[1], f[2]);
    }

    private void build(char c)
    {
        if (table == null) return;
        int icol=0;
        float[] col=new float[3];
        if (getcolor(c, col))
            icol=to_color(col);
        for (int i = 0; i < table.length; i++)
            table[i]=icol;
        id="" + c;
        buildBar();
    }

    private void build(String s)
    {
        while ((s.length() > 0) && isspacer(s.charAt(0)))
            s = s.substring(1);
        while (s.length() > 0 && isspacer(s.charAt(s.length()-1)))
            s=s.substring(0, s.length() - 1);

        int M = s.length();
        if (M <= 0)
        {
            // Bad
            build('R');
            return;
        }

        int clrs = 0;
        int sprs = 0;

        // Check chain
        for (int i=0;i<M;i++) {
            if (iscolor(s.charAt(i)))
                clrs++;
            else if (isspacer(s.charAt(i)))
                sprs++;
            else {
                // Bad
                build();
                return;
            }
        }

        // Less than two colors?
        if (clrs < 2) {
            for (int i=0;i<M;i++)
                if (iscolor(s.charAt(i)))
                {
                    build(s.charAt(i));
                    return;
                }
        }

        float[] rx = new float[clrs];
        float[] cr = new float[clrs];
        float[] cg = new float[clrs];
        float[] cb = new float[clrs];

        int idx = 0;
        float ofs = 0;
        float[] wcol=new float[3];
        for (int i = 0; i < M; i++)
        {
            float w=getspacer(s.charAt(i));
            if (w >= 0.0f) {
                ofs+=w;
            } else if (getcolor(s.charAt(i), wcol)) {
                rx[idx]=ofs;
                cr[idx]=wcol[0];
                cg[idx]=wcol[1];
                cb[idx]=wcol[2];
                ofs+=1.0;
                idx++;
            }
        }

        for (int i=0;i<clrs;i++) {
            rx[i]=rx[i]/rx[clrs-1];
        }

        build(rx, cr, cg, cb);
        id = s;
        buildBar();
    }

    private void build(float[] rx, float[] cr, float[] cg, float[] cb)
    {
        // Special Cases
        int M=rx.length;
        if (M < 1)
        {
            for (int i = 0; i < table.length; i++)
                table[i]=0;
            return;
        }
        if (M == 1)
        {
            int wcol=to_color(cr[0],cg[0],cb[0]);
            for (int i = 0; i < table.length; i++)
                table[i]=wcol;
            return;
        }

        // Sort
        for (int i = 0;i < M-1; i++)
            for (int j = i + 1; j < M; j++)
            {
                if (rx[j] < rx[i])
                {
                    float t;
                    t = rx[i]; rx[i] = rx[j]; rx[j] = t;
                    t = cr[i]; cr[i] = cr[j]; cr[j] = t;
                    t = cg[i]; cg[i] = cg[j]; cg[j] = t;
                    t = cb[i]; cb[i] = cb[j]; cb[j] = t;
                }
            }

        for (int i = 0; i < table.length; i++)
        {
            float x = (float)i / ((float)table.length - 1.0f);
            int left, right;
            if (x <= rx[0])
                table[i]=to_color(cr[0],cg[0],cb[0]);
            else if (x >= rx[M - 1])
                table[i]=to_color(cr[M-1],cg[M-1],cb[M-1]);
            else
            {
                right=1;
                while (rx[right] < x) right++;
                left=right-1;
                float f2 = (x-rx[left])/(rx[right]-rx[left]);
                float f1 = 1.0f-f2;
                table[i]=to_color(cr[left]*f1 + cr[right]*f2,
                        cg[left]*f1 + cg[right]*f2,
                        cb[left]*f1 + cb[right]*f2);
            }
        }
    }


}


