package com.alphadraco.audioanalyzer;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * Created by aladin on 12.03.2016.
 */
public class AudioPlayer {

    private int len;
    private int memLen;

    private short[] buffer;

    private int read=0;

    public AudioPlayer(int _len) {
        len=0;
        read=0;
        if (_len <= 0)
            memLen=0;
        else {
            memLen=_len;
            buffer=new short[memLen];
        }
    }

    public void addBlock(short [] src, int samples) {
        if (src == null) return;
        if (samples < 1) return;
        if (samples + len > memLen) {
            // Reallocation necessary
            if ((memLen == 0) || (buffer == null)) {
                // New
                if (samples < 2048)
                    memLen=2048;
                else
                    memLen=(samples/2048+1)*2048;
                buffer=new short[memLen];
                len=0;
            } else {
                // Reallocate
                memLen=((samples+len)/2048+1)*2048;
                short [] newbuf = new short[memLen];
                if (len > 0)
                    System.arraycopy(buffer,0,newbuf,0,len);
                buffer=newbuf;
            }
        }
        System.arraycopy(src,0,buffer,len,samples);
        len+=samples;
    }

    public void addBlock(short [] src) {
        if (src == null) return;
        int samples=src.length;
        if (samples < 1) return;
        addBlock(src,samples);
    }


    // Read a block of samples
    // Returns false in case of an error or when the buffer is empty
    public boolean getBlock(short [] blk) {
        if (blk==null) return false;;
        int samples=blk.length;
        if (samples < 1) return false;
        if (read >= len) {
            Arrays.fill(blk,(short)0);
            return false;
        }
        if (read+samples > len) {
            // Partial read
            System.arraycopy(buffer,read,blk,0,len-read);
            Arrays.fill(blk,len-read,samples-1,(short)0);
            read=len;
            return true;
        }
        // Full Block fits
        System.arraycopy(buffer,read,blk,0,samples);
        read+=samples;
        return true;
    }

    public void reset() {
        read=0;
    }

}
