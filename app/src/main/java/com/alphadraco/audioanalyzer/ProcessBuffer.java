package com.alphadraco.audioanalyzer;

/**
 * Created by aladin on 19.09.2015.
 */

import java.util.Arrays;

public class ProcessBuffer {

    public float fs;
    public int size;
    public short data[];

    public ProcessBuffer (float _fs, int _size, short _data[]) {
        size=_size;
        // data=new short[size];
        data=Arrays.copyOfRange(_data,0,size);
        // data=_data;
        fs=_fs;
    }



}
