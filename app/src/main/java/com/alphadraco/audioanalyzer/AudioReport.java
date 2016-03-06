package com.alphadraco.audioanalyzer;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.media.MediaScannerConnection;
import android.widget.RelativeLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by aladin on 22.11.2015.
 */
public class AudioReport {

    // GUI Connection
    private AudioReportView viewer;
    private AudioWaveView waveViewer;

    // Stored Data
    public String name;
    public ArrayList<ProcessResult> data;
    private boolean processed;
    XYdata specPlot;
    XYdata wavePlot;
    public Date recorded;
    public float rectime;
    public int samples;
    public float samplerate;
    public float[] energies;
    public String[] energy_names;
    public int calMode;


    public AudioReport(AudioReportView _viewer, AudioWaveView _waveViewer, String _name) {
        viewer = _viewer;
        waveViewer = _waveViewer;
        data=new ArrayList<ProcessResult>();
        name=name;
        processed=false;
        energies=null;
        energy_names=null;
    }

    public AudioReport(AudioReportView _viewer,  AudioWaveView _waveViewer) {
        viewer = _viewer;
        waveViewer = _waveViewer;
        data=new ArrayList<ProcessResult>();
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
        name=sdf.format(cal.getTime());
        processed=false;
    }

    private void txtwrite(FileOutputStream fileOutputStream, String string) throws IOException {
        fileOutputStream.write(string.getBytes());
    }

    public boolean saveFiles(AudioAnalyzer ctx, File basedir) {
        File logoFile=new File(basedir,"logo.png");
        File rootFile=new File(basedir,"index.html");
        File specFile=new File(basedir,"spec.png");
        File waveFile=new File(basedir,"wave.png");
        File audioFile=new File(basedir,"audio.wav");
        try {
            FileOutputStream html = new FileOutputStream(rootFile);
            txtwrite(html,"<html><head>\n");
            txtwrite(html,"<title>Audio Analyzer Report " + name + "</title>\n");
            txtwrite(html,"</head><body bgcolor=\"#000000\" text=\"#FFFFFF\" link=\"#00FF00\" alink=\"#00FF00\" text=\"#008000\">\n");
            txtwrite(html,"<h1><img src=\"logo.png\">AudioAnalyzer " + name + "</h1>\n");
            txtwrite(html,"<h2>Overview</h2>");
            txtwrite(html,"<table border=0>\n");
            txtwrite(html,"<tr><td>Record Date</td><td>" + dateString() + "</td></tr>\n");
            txtwrite(html,"<tr><td>Record Length</td><td>" + lengthString() + "</td></tr>\n");
            txtwrite(html,"<tr><td>Samples</td><td>" + String.format("%d",samples) + "</td></tr>\n");
            txtwrite(html,"<tr><td>Samplerate</td><td>" + String.format("%1.0f",samplerate) + " Hz</td></tr>\n");
            txtwrite(html,"</table>\n");
            txtwrite(html,"<h2>Spectrum</h2>\n");
            txtwrite(html,"<img src=\"spec.png\"><br>\n");
            txtwrite(html,"<h2>Wave</h2>\n");
            txtwrite(html,"<img src=\"wave.png\"><br>\n");
            txtwrite(html,"<h2>Levels</h2>\n");
            txtwrite(html,"<table border=\"0\">\n");
            txtwrite(html,"<tr><th>Name</th>");
            for (int j=0;j<ctx.getCalModes();j++) {
                if (calMode==j)
                    txtwrite(html,"<th bgcolor=\"#300000\">"+ctx.getCalNote(j)+"</i></th>");
                else
                    txtwrite(html,"<th>"+ctx.getCalNote(j)+"</th>");
            }
            txtwrite(html,"</tr>\n");
            txtwrite(html,"<tr><th></th>");
            for (int j=0;j<ctx.getCalModes();j++) {
                if (calMode==j)
                    txtwrite(html,"<th bgcolor=\"#300000\">"+ctx.getUnit(j)+"</th>");
                else
                    txtwrite(html,"<th>"+ctx.getUnit(j)+"</th>");
            }
            txtwrite(html,"</tr>\n");
            for (int i=0;i<energy_names.length;i++) {
                txtwrite(html,"<tr><td>" + energy_names[i] + "</td>");
                for (int j=0;j<ctx.getCalModes();j++)
                    txtwrite(html,"<td>"+String.format("%1.2f",energies[i]+ctx.getCalOfs(j)) + "</td>");
                txtwrite(html,"</tr>\n");
            }
            txtwrite(html,"</table>\n");
            txtwrite(html,"<h2>WAV File</h2>\n");
            txtwrite(html,"<a href=\"audio.wav\">WAV File</a><br>\n");
            txtwrite(html,"<hr>");
            txtwrite(html,"AudioAnalyzer " + ctx.getVersion() + "<br>\n");
            txtwrite(html,ctx.getString(R.string.reportFooterText));
            html.write("</body></html>\n".getBytes());
            html.close();
            MediaScannerConnection.scanFile(ctx, new String[] { rootFile.getAbsolutePath() }, null, null);
        } catch (IOException e) {
            return false;
        }
        try {
            ArrayList<XYdata> thisone = new ArrayList<XYdata>();
            thisone.add(specPlot);
            Bitmap bspec=Bitmap.createBitmap(800,600, Bitmap.Config.ARGB_8888);
            Canvas canvas=new Canvas(bspec);
            ctx.rp_view.drawToCanvas(canvas,thisone,true);
            FileOutputStream spec=new FileOutputStream(specFile);
            bspec.compress(Bitmap.CompressFormat.PNG,100,spec);
            spec.close();
            MediaScannerConnection.scanFile(ctx, new String[] { specFile.getAbsolutePath() }, null, null);
        } catch (IOException e) {
            return false;
        }
        try {
            ArrayList<XYdata> thisone = new ArrayList<XYdata>();
            thisone.add(wavePlot);
            Bitmap bspec=Bitmap.createBitmap(800,400, Bitmap.Config.ARGB_8888);
            Canvas canvas=new Canvas(bspec);
            ctx.rp_wave_view.drawToCanvas(canvas,thisone,true);
            FileOutputStream spec=new FileOutputStream(waveFile);
            bspec.compress(Bitmap.CompressFormat.PNG,100,spec);
            spec.close();
            MediaScannerConnection.scanFile(ctx, new String[] { waveFile.getAbsolutePath() }, null, null);
        } catch (IOException e) {
            return false;
        }
        try {
            byte[] header=new byte[128];
            int headsize=ctx.audioAnalyzerHelper.SignalWavHeader(header,samples);
            FileOutputStream af=new FileOutputStream(audioFile);
            af.write(header,0,headsize);
            short[] wavdata=new short[samples];
            byte[] wavbdata=new byte[samples*2];
            int ofs=0;
            for (int i=0;i<data.size();i++) {
                System.arraycopy(data.get(i).wave, 0, wavdata, ofs, data.get(i).len);
                ofs += data.get(i).len;
            }
            ctx.audioAnalyzerHelper.SignalWavData(wavdata,wavbdata);
            af.write(wavbdata,0,samples*2);
            af.close();
            MediaScannerConnection.scanFile(ctx, new String[] { audioFile.getAbsolutePath() }, null, null);
        } catch (IOException e) {
            return false;
        }
        try {
            FileOutputStream af= new FileOutputStream(logoFile);
            Bitmap b= BitmapFactory.decodeResource(ctx.getResources(),R.mipmap.ic_launcher);
            b.compress(Bitmap.CompressFormat.PNG,100,af);
            af.close();
            MediaScannerConnection.scanFile(ctx, new String[] { logoFile.getAbsolutePath() }, null, null);
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public void addData(ArrayList<ProcessResult> pr) {
        if (pr.size() < 1) return;
        recorded=Calendar.getInstance().getTime();
        ProcessResult ref=null;
        for (int i=0;i<pr.size();i++) {
            ProcessResult cand=pr.get(i);
            if (cand.processed && !cand.empty) {
                if (ref == null) {
                    ref = cand;
                    data.add(ref);
                } else {
                    if ((cand.len == ref.len) && (cand.fs == ref.fs)) {
                        data.add(cand);
                    }
                }
             }
        }
        processed=false;
    }

    public void process() {
        if (data.size() < 1) return; // Nothing to do...
        specPlot =new XYdata(viewer);
        specPlot.name=name;
        ProcessResult pr=data.get(0);
        specPlot.add(pr.f,pr.y,pr.len/2);
        if (data.size() > 1) {
            for (int i=1;i<data.size();i++) {
                pr = data.get(i);
                specPlot.add_dsetminmax(pr.y);
            }
            specPlot.conv_to_lin();
            for (int i=1;i<data.size();i++) {
                pr=data.get(i);
                specPlot.add_dsetminmax(pr.y);
                specPlot.add_dsetavgpwr(pr.y);
            }
            specPlot.scale_Y(1.0f/(float)data.size());
            specPlot.conv_to_log();
        }
        rectime=0.0f;
        samples=0;
        samplerate=data.get(0).fs;
        for (int i=0;i<data.size();i++ ) {
            samples += data.get(i).len;
            rectime += (float) data.get(i).len / (float) data.get(i).fs;
        }
        energies=new float[DataConsolidator.powerTrackNames.length];
        energy_names=DataConsolidator.powerTrackNames.clone();
        for (int i=0;i<energies.length;i++)
            energies[i]=0.0f;
        for (int i=0;i<data.size();i++) {
            for (int j=0;j<energies.length;j++) {
                int idx=DataConsolidator.powerTrackIds[j];
                energies[j]+=(float)Math.pow(10.0,data.get(i).fres[idx]/10.0f);
            }
        }
        for (int i=0;i<energies.length;i++)
            energies[i]=(float)10.0*(float)Math.log10(energies[i]/(float)data.size());

        // Wave Plot
        wavePlot =new XYdata(waveViewer);
        wavePlot.name=name;
        wavePlot.color= specPlot.color;
        float t=-rectime/2;
        for (int i=0;i<data.size();i++) {
            ProcessResult prt=data.get(i);
            float[] tbase=new float[prt.len];
            for (int j=0;j<prt.len;j++) {
                tbase[j] = t;
                t+=1/prt.fs;
            }
            wavePlot.add(tbase,prt.wave,1.0f/32768.0f,prt.len);
        }

        processed=true;
    }

    public void setName(String _name) {
        name=_name;
        wavePlot.name=_name;
        specPlot.name=_name;
    }

    public String description() {
        if (processed) {
            return String.format("%1.1f s, %1.1f dBFS(A)",
                    rectime, energies[0]);
        } else {
            return "undefined";
        }
    }

    public String dateString() {
        if (processed) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.format(recorded);
        }
        return "undefined";
    }


    public String lengthString() {
        if (processed)
            return String.format("%1.1f s",rectime);
        else
            return "undefined";
    }

}
