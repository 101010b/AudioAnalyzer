package com.alphadraco.audioanalyzer;

import android.graphics.Bitmap;

/**
 * Created by aladin on 13.10.2015.
 */
public class AudioAnalyzerHelper {
    public boolean initialized=false;

    static {
        System.loadLibrary("AudioAnalyzerHelperJNI");
    }

    public native boolean fftProcessorSetup();
    public native boolean fftProcessorSetData(float fs, int window, float trackf, short data[],
                                              float fmin, float fmax, int pixels, boolean logscale,
                                              int terzw);
    public native boolean fftProcessorProcess();
    public native float[]  fftProcessorGetData(int what, int size);
    public native boolean fftProcessorGetDataCopy(float[] result, int what);
    public native boolean fftProcessorResetPeak();

    // public native boolean fftProcessorWaveViewSetup(Bitmap bmp);
    // public native boolean fftProcessorWaveViewProcessData(short [] data);
    public native boolean WaterfallProcessData(int[] bmapin, int width, int height, int[] ctab, int ctablen);

    public native boolean WaveViewProcessData(int [] bmapin, int width, int height, short [] data, int scale);

    public native boolean SignalSetup(int fs);
    public native boolean SignalProg(int param, float value);
    public native boolean SignalSource(short [] erg);
    public native int SignalWavHeader(byte [] header, int samples);

    // float [] erg=new float[4096];

    public Bitmap sharedMap=null;
    int[] sharedMapBuffer=null;
    int sharedMapStride;
    int sharedMapWidth;
    int sharedMapHeight;

    float specFmin=0;
    float specFmax=1;
    boolean specLogScale=false;
    Bitmap specMap=null;
    int specMapWidth=0;
    int specMapHeight=0;
    int[] specMapBuffer=null;
    int[] specColorTable=null;

    AudioAnalyzerHelper() {
        if (fftProcessorSetup())
            initialized=true;
    }

    boolean fftSetData(float fs, int window, float trackf, int terzw, short data[]) {
        if (!initialized) return false;
        if (specMap != null)
            return fftProcessorSetData(fs,window, trackf, data,
                    specFmin,specFmax,specMapWidth,specLogScale,terzw);
        else
            return fftProcessorSetData(fs,window, trackf, data,
                    0,0,0,false,terzw);
    }

    boolean fftProcess() {
        if (!initialized) return false;
        boolean rval=fftProcessorProcess();
        if ((specMap != null) && (specMapBuffer != null) && rval) {
            WaterfallProcessData(specMapBuffer,specMapWidth,specMapHeight,specColorTable,specColorTable.length);
        }
        return rval;
    }

    void fftCopySpecMaptoBitmap() {
        if (!initialized) return;
        if (specMap!=null) // TODO: Make Thread Safe!
            specMap.setPixels(specMapBuffer, 0, specMapWidth, 0, 0, specMapWidth, specMapHeight);
    }


    boolean fftResetPeak() {
        if (!initialized) return false;
        return fftProcessorResetPeak();
    }

    float [] fftGetData(int what, int size) {
        if (!initialized) return null;
        // float [] erg=new float[4096];
        float[] erg=new float[size];
        if (fftProcessorGetDataCopy(erg,what))
            return erg;
        else
            return null;

        /*
        float[] terg=fftProcessorGetData(what,size);
        float[] erg=new float[size];
        System.arraycopy(terg,0,erg,0,size);
        terg=null;
        // erg.clone() .copyOf(terg,4096);
        // if (terg.length < 1) return null;
        return erg;*/
    }

    // Waterfall Viewer
    void SpecViewInit(int width, int height, int[] ctab,
                      float fmin, float fmax, boolean logscale) {
        if (width*height < 1) {
            // Delete it
            specMap=null;
            specMapWidth=0;
            specMapHeight=0;
            specMapBuffer=null;
            return;
        }
        boolean newmap=false;
        if ((width != specMapWidth) || (height != specMapHeight)) {
            specMap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            specMapWidth = width;
            specMapHeight = height;
            specMapBuffer = new int[width * height];
            newmap=true;
        }
        if (ctab != null)
            specColorTable=ctab;

        if (!newmap && ((fmin != specFmin) || (fmax != specFmax) || (specLogScale != logscale)))
            specMapBuffer = new int[width * height];
        specFmin=fmin;
        specFmax=fmax;
        specLogScale=logscale;
    }


    // Wave View Part

    void WaveViewInit(int width, int height) {
        /*if (sharedMap != null) {
            fftProcessorWaveViewSetup(null);
            sharedMap=null;
        }*/
        if ((width < 1) || (height < 1)) return;
        sharedMap=Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        sharedMapWidth=width;
        sharedMapHeight=height;
        sharedMapStride=sharedMap.getRowBytes()/4;
        sharedMapBuffer=new int[sharedMapWidth * sharedMapHeight];
        // sharedMap.copyPixelsToBuffer(sharedMapBuffer);
        /*if (!fftProcessorWaveViewSetup(sharedMap)) {
            sharedMap=null;
            return;
        }*/

    }

    short[] wavedata=null;
    int waveuse;

    void WaveViewCopyMapToBitmap() {
        if (sharedMapBuffer == null)
            return;
        sharedMap.setPixels(sharedMapBuffer, 0, sharedMapWidth, 0, 0, sharedMapWidth, sharedMapHeight);
    }

    void WaverViewProcess(short [] data, int scale) {
        if (sharedMap==null) return;
        if (sharedMapBuffer==null) return;

        if (data.length < sharedMapWidth) {
            // Accumulate
            if ((wavedata == null) || (wavedata.length < sharedMapWidth)) {
                wavedata = new short[sharedMapWidth];
                waveuse = 0;
            }
            if (waveuse+data.length > wavedata.length) {
                System.arraycopy(data,0,wavedata,waveuse,wavedata.length-waveuse);
                waveuse=wavedata.length;
            } else {
                System.arraycopy(data,0,wavedata,waveuse,data.length);
                waveuse+=data.length;
            }
            if (waveuse >= sharedMapWidth) {
                WaveViewProcessData(sharedMapBuffer, sharedMapWidth, sharedMapHeight, wavedata, scale);
                // sharedMap.setPixels(sharedMapBuffer, 0, sharedMapWidth, 0, 0, sharedMapWidth, sharedMapHeight);
                waveuse=0;
            }
        } else {
            WaveViewProcessData(sharedMapBuffer, sharedMapWidth, sharedMapHeight, data, scale);
            // sharedMap.setPixels(sharedMapBuffer, 0, sharedMapWidth, 0, 0, sharedMapWidth, sharedMapHeight);
        }
    }



}
