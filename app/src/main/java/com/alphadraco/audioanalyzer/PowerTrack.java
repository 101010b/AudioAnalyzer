package com.alphadraco.audioanalyzer;

/**
 * Created by aladin on 26.09.2015.
 */
public class PowerTrack {
    float current;
    float peakFollow;
    float peak;
    float average;
    float tPeak;
    String name;
    String longName;
    int id;

    public PowerTrack(PowerTrack p) {
        current=p.current;
        peakFollow=p.peakFollow;
        peak=p.peak;
        average=p.average;
        tPeak=p.tPeak;
        name=p.name;
        longName=p.longName;
    }

    public PowerTrack(String _name, String _longName, int _id) {
        name=_name;
        longName=_longName;
        id=_id;
        reset();
    }

    public void reset() {
        current=peakFollow=peak=average=tPeak=-150.0f;
    }

    public void tick() {
        if (peakFollow > -150.0f)
            peakFollow-=0.5f;
        else
            peakFollow=-150.0f;
    }

    public void add(float val) {
        current=val;
        if (current > peakFollow)
            peakFollow=current;
        if (current > peak)
            peak = current;
        average=10.0f*(float)Math.log10(Math.pow(10.0, average / 10.0f)*0.95+Math.pow(10.0,current/10.0f)*0.05);
    }

    public void add(float val, float tpk) {
        add(val);
        if (tpk > tPeak)
            tPeak=tpk;
    }


}
