package com.alphadraco.audioanalyzer;

import java.util.concurrent.Semaphore;

/**
 * Created by aladin on 19.09.2015.
 */
public class ProcessBufferList {

    private ProcessBuffer lst[];
    private int write;
    private int read;
    public int overflow;
    private Semaphore semaphore;
    final Object monitorObject = new Object();

    public ProcessBufferList(int _length) {
        lst=new ProcessBuffer[_length];
        semaphore=new Semaphore(1);
        read=write=0;
        overflow=0;
    }

    public void doWait() {
        synchronized (monitorObject) {
            try {
                monitorObject.wait();
            } catch (InterruptedException e) {
                // Just ignore
            }
        }
    }

    public void doNotify() {
        synchronized (monitorObject) {
            monitorObject.notify();
        }
    }

    public void clear() {
        semaphore.acquireUninterruptibly();
        while (read != write) {
            lst[read]=null;
            read=(read+1)%lst.length;
        }
        read=write=0;
        overflow=0;
        semaphore.release();
    }

    public void add(ProcessBuffer p) {
        int wnext=(write + 1 ) % lst.length;
        if (wnext == read) {
            // Overwrite
            semaphore.acquireUninterruptibly();
                lst[read]=null;
                read=(read +1) % lst.length;
            semaphore.release();
            overflow++;
            lst[write]=p;
            write=wnext;
        } else {
            lst[write]=p;
            write=wnext;
        }
        doNotify();
    }

    public ProcessBuffer retrieve() {
        if (read == write) return null;
        semaphore.acquireUninterruptibly();
            ProcessBuffer p=lst[read];
            lst[read]=null;
            read=(read+1) % lst.length;
        semaphore.release();
        return p;
    }

}
