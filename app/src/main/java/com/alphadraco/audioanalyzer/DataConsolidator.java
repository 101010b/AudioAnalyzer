package com.alphadraco.audioanalyzer;

/**
 * Created by aladin on 26.09.2015.
 */
public class DataConsolidator {


    float f[];
    float y[];
    float yavg[];
    float ypeak[];
    short wave[];

    AudioAnalyzerHelper nft;

    int window;
    float fs;
    int len;
    float trackf;

    final static String[] powerTrackNames = {"Flat","Flat10k","Flat20k","1kHz","A10-20k","A100-20k","A10-10k","A100-10k","Track","Peak"};
    String[] powerTrackLongNames = {"AC RMS", "0-10kHz","0-20kHz","1kHz",
            "A-Weighted, 10Hz-20kHz","A-Weighted, 100Hz-20kHz","A-Weighted, 10Hz-10kHz","A-Weighted, 100Hz-10kHz","Track","Peak"};
    int[] powerTrackIds = {2,3,4,5,6,9,7,8,11,10};

    PowerTrack[] powerTracks = new PowerTrack[powerTrackIds.length];

    public void initPowerTracks() {
        for (int i=0;i<powerTrackNames.length;i++)
            powerTracks[i]=new PowerTrack(powerTrackNames[i],powerTrackLongNames[i],powerTrackIds[i]);
    }

    public void reset() {
        f=y=yavg=ypeak=null;
        wave=null;
        if (nft != null) nft.fftResetPeak();
        window=len=-1;
        fs=-1.0f;
        trackf=-1;
        for (PowerTrack p:powerTracks)
            p.reset();
    }

    public DataConsolidator() {
        f=y=yavg=ypeak=null;
        wave=null;
        initPowerTracks();
        reset();
    }

    public void tick() {
        for (PowerTrack p:powerTracks) p.tick();
    }

    public void add(ProcessResult pr) {
        if ((f == null) || (pr.fs != fs) || (pr.len != len)) {
            // New Package
            f=pr.f.clone();
            y=pr.y.clone();
            yavg=pr.yavg.clone();
            ypeak=pr.ypeak.clone();
            for (PowerTrack p:powerTracks) {
                p.reset();
                p.add(pr.fres[p.id]);
            }
            window=pr.window;
            len=pr.len;
            fs=pr.fs;
            return;
        }
        // Accumulate
        y=pr.y;
        yavg=pr.yavg;
        ypeak=pr.ypeak;
        wave=pr.wave;
        /*for (int i=0;i<y.length;i++) {
            if (y[i] > ypeak[i])
                ypeak[i]=y[i];
        }*/
        for (PowerTrack p:powerTracks)
            p.add(pr.fres[p.id]);
    }


}
