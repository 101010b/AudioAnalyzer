package com.alphadraco.audioanalyzer;

/**
 * Created by aladin on 21.10.2015.
 */
public class AudioSource {

    AudioAnalyzerHelper helper;

    public AudioSource(AudioAnalyzerHelper hlp) {
        helper=hlp;
        boolean ok;

        ok=helper.SignalSetup(44100);
        //ok=ok &&  helper.SignalProg(12,0.5f);
        //ok=ok && helper.SignalProg(15,1.0f);
        //ok=ok && helper.SignalProg(21,1.0f);
    }

    void getData(short[] data) {
        //for (int i=0;i<data.length;i++)
        //   data[i]=(short) (32000 * Math.sin(2*Math.PI*1000*i/44100));
        helper.SignalSource(data);
    }



}
