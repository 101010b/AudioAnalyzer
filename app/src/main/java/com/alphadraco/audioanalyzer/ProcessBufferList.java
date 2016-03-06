package com.alphadraco.audioanalyzer;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

/**
 * Created by aladin on 19.09.2015.
 */
public class ProcessBufferList {

    private ProcessResult lst[];
    private int write;
    private int read;
    public int overflow;
    private Semaphore semaphore;
    final Object monitorObject = new Object();
    int serial=0;

    public ProcessBufferList(int _length) {
        lst=new ProcessResult[_length];
        for (int i=0;i<_length;i++) lst[i]=null;
        semaphore=new Semaphore(1);
        read=write=0;
        overflow=0;
        serial=0;
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

    public ProcessResult addslot() {
        ProcessResult pr=null;
        int wnext=(write + 1 ) % lst.length;
        if (wnext == read) {
            // Overwrite
            semaphore.acquireUninterruptibly();
            read = (read + 1) % lst.length;
            semaphore.release();
            overflow++;
        }
        if (lst[write]==null)
            lst[write]=new ProcessResult();
        pr=lst[write];
        pr.empty=true;
        pr.serial=serial;
        serial++;
        write=wnext;
        return pr;
        // doNotify();
    }

    public void notify_new_buffer() {
        doNotify();
    }

    public ProcessResult retrieve() {
        if (read == write) return null;
        semaphore.acquireUninterruptibly();
        if (lst[read].empty) {
            semaphore.release();
            return null;
        }
        ProcessResult p=lst[read];
        // lst[read]=null;
        read=(read+1) % lst.length;
        semaphore.release();
        return p;
    }

    public int find_serial(int n) {
        for (int i=0;i<lst.length;i++) if ((lst[i] != null) && (!lst[i].empty) && (lst[i].serial==n)) return i;
        return -1;
    }

    public ArrayList<ProcessResult> get_old_results(int from, int to) {
        ArrayList<ProcessResult> a=new ArrayList<ProcessResult>();
        for (int i=from;i<to;i++) {
            semaphore.acquireUninterruptibly();
            int q=find_serial(i);
            if (q >= 0) {
                // a.add(lst[q].duplicate());
                a.add(lst[q]);
                lst[q]=new ProcessResult();
            }
            semaphore.release();
        }
        return a;
    }


    /*
    public void add(float _fs, int _size, short[] _data) {
        int wnext=(write + 1 ) % lst.length;
        if (wnext == read) {
            // Overwrite
            semaphore.acquireUninterruptibly();
            if (lst[read] != null)
                lst[read].invalidate();
            read = (read + 1) % lst.length;
            semaphore.release();
            overflow++;
        }
        if (lst[write]==null)
            lst[write]=new ProcessBuffer(_fs,_size,_data);
        else
            lst[write].reInit(_fs, _size, _data);
        write=wnext;
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
    */


    /*
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
    */



}
