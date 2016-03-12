package com.alphadraco.audioanalyzer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Created by aladin on 20.09.2015.
 */
public class ProcessResult {
    // Organisation
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

    public float[] readArrayFloat(DataInputStream dataInputStream) throws IOException {
        int len=dataInputStream.readInt();
        if (len < 0) {
            return null;
        }
        ByteBuffer bb=ByteBuffer.allocate(len*4);
        byte [] bu=bb.array();
        dataInputStream.read(bu);
        float [] reta = new float[len];
        for (int i=0;i<len;i++) reta[i]=bb.getFloat();
        // for (float f:reta) f=bb.getFloat();
        return reta;
    }

    public short[] readArrayShort(DataInputStream dataInputStream) throws IOException {
        int len=dataInputStream.readInt();
        if (len < 0) {
            return null;
        }
        ByteBuffer bb=ByteBuffer.allocate(len*2);
        byte [] bu=bb.array();
        dataInputStream.read(bu);
        short [] reta=new short[len];
        for (int i=0;i<len;i++) reta[i]=bb.getShort();
        // for (short s:reta) s=bb.getShort();
        return reta;
    }

    public ProcessResult(DataInputStream dataInputStream) throws IOException {
        String signature=dataInputStream.readUTF();
        if (!signature.equals("ARES0"))
            throw new IOException("Bad File Format: Bad Signature");
        serial=dataInputStream.readInt();
        len=dataInputStream.readInt();
        wave=readArrayShort(dataInputStream);
        fs=dataInputStream.readFloat();
        memlen=len;
        window=dataInputStream.readInt();
        terzw=dataInputStream.readInt();

        processed=dataInputStream.readBoolean();
        empty=dataInputStream.readBoolean();
        trackf=dataInputStream.readFloat();

        window_amplitude_corr=dataInputStream.readFloat();
        window_noise_corr=dataInputStream.readFloat();
        pkmax=dataInputStream.readFloat();
        trackLevel=dataInputStream.readFloat();
        trackLevel2=dataInputStream.readFloat();

        fres=readArrayFloat(dataInputStream);
        f=readArrayFloat(dataInputStream);
        y=readArrayFloat(dataInputStream);
        yavg=readArrayFloat(dataInputStream);
        ypeak=readArrayFloat(dataInputStream);

        terzf=readArrayFloat(dataInputStream);
        terze=readArrayFloat(dataInputStream);
        terzeavg=readArrayFloat(dataInputStream);
        terzepeak=readArrayFloat(dataInputStream);

        reuse=false;
    }

    public void writeArray(DataOutputStream dataOutputStream, float [] fa) throws IOException {
        if (fa==null) {
            dataOutputStream.writeInt(-1);
            return;
        }
        dataOutputStream.writeInt(fa.length);
        ByteBuffer bb=ByteBuffer.allocate(fa.length*4);
        for (float f:fa) bb.putFloat(f);
        /*FloatBuffer fb=bb.asFloatBuffer();
        System.arraycopy(fa,0,fb.array(),0,fa.length);*/
        dataOutputStream.write(bb.array());
        //for (float q:fa)
        //    dataOutputStream.writeFloat(q);
    }

    public void writeArray(DataOutputStream dataOutputStream, short [] fa) throws IOException {
        if (fa==null) {
            dataOutputStream.writeInt(-1);
            return;
        }
        dataOutputStream.writeInt(fa.length);
        ByteBuffer bb=ByteBuffer.allocate(fa.length*2);
        for (short s:fa) bb.putShort(s);
        /*ShortBuffer sb=bb.asShortBuffer();
        System.arraycopy(fa,0,sb.array(),0,fa.length);*/
        dataOutputStream.write(bb.array());
        //for (short q:fa)
        //    dataOutputStream.writeShort(q);
    }


    public void store(DataOutputStream dataOutputStream) throws IOException {
        String signature = "ARES0";
        dataOutputStream.writeUTF(signature);
        dataOutputStream.writeInt(serial);
        dataOutputStream.writeInt(len);
        writeArray(dataOutputStream,wave);
        dataOutputStream.writeFloat(fs);
        dataOutputStream.writeInt(window);
        dataOutputStream.writeInt(terzw);
        dataOutputStream.writeBoolean(processed);
        dataOutputStream.writeBoolean(empty);
        dataOutputStream.writeFloat(trackf);
        dataOutputStream.writeFloat(window_amplitude_corr);
        dataOutputStream.writeFloat(window_noise_corr);
        dataOutputStream.writeFloat(pkmax);
        dataOutputStream.writeFloat(trackLevel);
        dataOutputStream.writeFloat(trackLevel2);

        writeArray(dataOutputStream,fres);
        writeArray(dataOutputStream,f);
        writeArray(dataOutputStream,y);
        writeArray(dataOutputStream,yavg);
        writeArray(dataOutputStream,ypeak);
        writeArray(dataOutputStream,terzf);
        writeArray(dataOutputStream,terze);
        writeArray(dataOutputStream,terzeavg);
        writeArray(dataOutputStream,terzepeak);

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

