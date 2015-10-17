package com.alphadraco.audioanalyzer;

import java.util.concurrent.Semaphore;

/**
 * Created by aladin on 20.09.2015.
 */
public class ProcessResultList {
    private ProcessResult lst[];
    private int write;
    private int read;
    public boolean overflow;
    private Semaphore semaphore;
    final Object monitorObject = new Object();

    public ProcessResultList(int _length) {
        lst=new ProcessResult[_length];
        semaphore=new Semaphore(1);
        read=write=0;
        overflow=false;
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
        overflow=false;
        semaphore.release();
    }

    public void add(ProcessResult p) {
        int wnext=(write + 1 ) % lst.length;
        if (wnext == read) {
            // Overwrite
            semaphore.acquireUninterruptibly();
            lst[read]=null;
            read=(read +1) % lst.length;
            semaphore.release();
            overflow=true;
            lst[write]=p;
            write=wnext;
        } else {
            lst[write]=p;
            write=wnext;
        }
        doNotify();
    }

    public ProcessResult retrieve() {
        if (read == write) return null;
        semaphore.acquireUninterruptibly();
        ProcessResult p=lst[read];
        lst[read]=null;
        read=(read+1) % lst.length;
        semaphore.release();
        return p;
    }


}
