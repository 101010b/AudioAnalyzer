package com.alphadraco.audioanalyzer;

/**
 * Created by aladin on 20.09.2015.
 */
public class ProcessResult {
    // Temporary data
    ProcessBuffer src;

    // Final data
    boolean processed;
    int window;
    float trackf;
    float fs;
    int len;

    float window_amplitude_corr;
    float window_noise_corr;
    float pkmax;
    float trackLevel;

    float fres[];
    float f[];
    float y[];
    float yavg[];
    float ypeak[];
    short wave[];

    int terzw;
    float[] terzf;
    float[] terze;
    float[] terzeavg;
    float[] terzepeak;


    public ProcessResult(ProcessBuffer pb, float tf, int win, int _terzw) {
        src=pb;
        window=win;
        trackf=tf;
        fs=pb.fs;
        len=pb.data.length;
        processed=false;
        terzw=_terzw;
    }

    public void process(AudioAnalyzerHelper audioAnalyzerHelper) {

        audioAnalyzerHelper.fftSetData(fs, window, trackf, terzw, src.data);

        audioAnalyzerHelper.fftProcess();

        f=audioAnalyzerHelper.fftGetData(0,len/2);
        y=audioAnalyzerHelper.fftGetData(1,len/2);
        fres=audioAnalyzerHelper.fftGetData(2,20);
        yavg=audioAnalyzerHelper.fftGetData(3,len/2);
        ypeak=audioAnalyzerHelper.fftGetData(4,len/2);

        terzf=audioAnalyzerHelper.fftGetData(5,34);
        terze=audioAnalyzerHelper.fftGetData(6,34);
        terzeavg=audioAnalyzerHelper.fftGetData(7,34);
        terzepeak=audioAnalyzerHelper.fftGetData(8,34);

        /*
        f=audioAnalyzerHelper.fftGetData(0,len/2).clone();
        y=audioAnalyzerHelper.fftGetData(1,len/2).clone();
        fres=audioAnalyzerHelper.fftGetData(2,20).clone();
        yavg=audioAnalyzerHelper.fftGetData(3,len/2).clone();
        ypeak=audioAnalyzerHelper.fftGetData(4,len/2).clone();
        */

        wave=src.data;

        src=null;

        window_amplitude_corr=fres[0];
        window_noise_corr=fres[1];
        pkmax=fres[10];
        trackLevel=fres[11];
        processed=true;
    }




}

