package com.alphadraco.audioanalyzer;

/**
 * Created by aladin on 20.09.2015.
 */
public class ProcessResult {
    // Orghanisation
    int serial=0;

    // Input Data
    short wave[];
    float fs;
    int len; // Actual Block Length
    int memlen; // Length of the Memory Blocks
    int window;
    int terzw;

    // Final data
    boolean processed;
    boolean empty;
    float trackf;

    float window_amplitude_corr;
    float window_noise_corr;
    float pkmax;
    float trackLevel;  // At trackf
    float trackLevel2; // For Sweep only

    float fres[];
    float f[];
    float y[];
    float yavg[];
    float ypeak[];

    float[] terzf;
    float[] terze;
    float[] terzeavg;
    float[] terzepeak;

    // Reuse already allocated Memory
    boolean reuse;

    public ProcessResult() {
        reuse=false;
        empty=true;
        memlen=-1;
    }

    public ProcessResult duplicate() {
        ProcessResult pr=new ProcessResult();
        pr.serial=serial;
        if (wave != null)
            pr.wave=wave.clone();
        else
            pr.wave=null;
        pr.fs=fs;
        pr.len=len;
        pr.memlen=memlen;
        pr.window=window;
        pr.terzw=terzw;
        pr.processed=processed;
        pr.empty=empty;
        pr.trackf=trackf;
        pr.window_amplitude_corr=window_amplitude_corr;
        pr.window_noise_corr=window_noise_corr;
        pr.pkmax=pkmax;
        pr.trackLevel=trackLevel;
        pr.trackLevel2=trackLevel2;
        if (fres != null)
            pr.fres=fres.clone();
        else
            pr.fres=null;
        if (f != null)
            pr.f=f.clone();
        else
            pr.f=null;
        if (y != null)
            pr.y=y.clone();
        else
            pr.y=null;
        if (yavg != null)
            pr.yavg=yavg.clone();
        else
            pr.yavg=null;
        if (ypeak != null)
            pr.ypeak=ypeak.clone();
        else
            pr.ypeak=null;
        if (terzf != null)
            pr.terzf=terzf.clone();
        else
            pr.terzf=null;
        if (terze != null)
            pr.terze=terze.clone();
        else
            pr.terze=null;
        if (terzeavg != null)
            pr.terzeavg=terzeavg.clone();
        else
            pr.terzeavg=null;
        if (terzepeak != null)
            pr.terzepeak=terzepeak.clone();
        else
            pr.terzepeak=null;
        pr.reuse=reuse;
        return pr;
    }

    public ProcessResult(short[] _wave, int _len, int _fs, float _trackf, int _window, int _terzw) {
        wave=_wave.clone();
        memlen=_len;
        len=_len;

        window=_window;
        trackf=_trackf;
        fs=_fs;
        terzw=_terzw;

        // Init
        reuse=false;
        processed=false;
        empty=false;
    }

    public void ReUse(short[] _wave, int _len, int _fs, float _trackf, int _window, int _terzw) {
        if (_len <= memlen) {
            reuse=true;
            len=_len;
            System.arraycopy(_wave,0,wave,0,len);
            window=_window;
            trackf=_trackf;
            fs=_fs;
            processed=false;
            terzw=_terzw;
        } else {
            reuse=false;
            len=_len;
            memlen=_len;
            wave=_wave.clone();
            window=_window;
            trackf=_trackf;
            fs=_fs;
            processed=false;
            terzw=_terzw;
        }
        processed=false;
        empty=false;
    }

    public void process(AudioAnalyzerHelper audioAnalyzerHelper) {

        audioAnalyzerHelper.fftSetData(fs, window, trackf, terzw, wave, len);
        audioAnalyzerHelper.fftProcess();

        f=audioAnalyzerHelper.fftGetData(0,len/2);
        y=audioAnalyzerHelper.fftGetData(1,len/2);
        fres=audioAnalyzerHelper.fftGetData(2,21);
        yavg=audioAnalyzerHelper.fftGetData(3,len/2);
        ypeak=audioAnalyzerHelper.fftGetData(4,len/2);

        terzf=audioAnalyzerHelper.fftGetData(5,34);
        terze=audioAnalyzerHelper.fftGetData(6,34);
        terzeavg=audioAnalyzerHelper.fftGetData(7,34);
        terzepeak=audioAnalyzerHelper.fftGetData(8,34);

        window_amplitude_corr=fres[0];
        window_noise_corr=fres[1];
        pkmax=fres[10];
        trackLevel=fres[11];
        processed=true;
        trackLevel2=fres[20];
    }




}

