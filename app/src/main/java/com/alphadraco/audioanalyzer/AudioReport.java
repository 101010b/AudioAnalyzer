package com.alphadraco.audioanalyzer;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.widget.Button;
import android.widget.RelativeLayout;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Random;

/**
 * Created by aladin on 22.11.2015.
 */
public class AudioReport {

    // GUI Connection
    private float txtSize;
    private AudioReportView viewer;
    private AudioWaveView waveViewer;


    public boolean persistant=false;
    private File persistantFile;

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
    public String deviceName;

    public void setup(AudioReportView _viewer, AudioWaveView _waveViewer, String _name) {
        viewer = _viewer;
        waveViewer = _waveViewer;
        data=new ArrayList<ProcessResult>();
        name=_name;
        processed=false;
        energies=null;
        energy_names=null;
        float stdsize = new Button(viewer.root).getTextSize();
        txtSize=0.75f*stdsize;
        deviceName=viewer.root.deviceName;
    }

    public AudioReport(AudioReportView _viewer, AudioWaveView _waveViewer, String _name) {
        setup(_viewer,_waveViewer,_name);
    }

    public AudioReport(AudioReportView _viewer,  AudioWaveView _waveViewer) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
        setup(_viewer,_waveViewer,sdf.format(cal.getTime()));
    }

    public void fetch(DataInputStream dataInputStream) throws IOException {
        String signature=dataInputStream.readUTF();
        if (!signature.equals("AREPO0"))
            throw new IOException("Bad File Format: Signature Error");
        name=dataInputStream.readUTF();
        calMode=dataInputStream.readInt();
        deviceName=dataInputStream.readUTF();
        int size=dataInputStream.readInt();
        data=new ArrayList<ProcessResult>(size);
        for (int i=0;i<size;i++)
            data.add(new ProcessResult(dataInputStream));
        processed=dataInputStream.readBoolean();
        if (processed) {
            specPlot=new XYdata(txtSize, dataInputStream);
            wavePlot=new XYdata(txtSize, dataInputStream);
            recorded=new Date();
            recorded.setTime(dataInputStream.readLong());
            samples=dataInputStream.readInt();
            samplerate=dataInputStream.readFloat();
            int en=dataInputStream.readInt();
            energies=new float[en];
            energy_names=new String[en];
            for (int i=0;i<en;i++)
                energies[i]=dataInputStream.readFloat();
            for (int i=0;i<en;i++)
                energy_names[i]=dataInputStream.readUTF();
        }
    }

    public void store(DataOutputStream dataOutputStream) throws IOException {
        String signature = "AREPO0";
        dataOutputStream.writeUTF(signature);
        dataOutputStream.writeUTF(name);
        dataOutputStream.writeInt(calMode);
        dataOutputStream.writeUTF(deviceName);
        dataOutputStream.writeInt(data.size());
        for (ProcessResult pr:data)
            pr.store(dataOutputStream);
        dataOutputStream.writeBoolean(processed);
        if (processed) {
            specPlot.store(dataOutputStream);
            wavePlot.store(dataOutputStream);
            dataOutputStream.writeLong(recorded.getTime());
            dataOutputStream.writeInt(samples);
            dataOutputStream.writeFloat(samplerate);
            dataOutputStream.writeInt(energies.length);
            for (float f : energies)
                dataOutputStream.writeFloat(f);
            for (String s : energy_names)
                dataOutputStream.writeUTF(s);
        }
    }

    public boolean store(Context ctx, File file) {
        try {
            // BufferedOutputStream bufferedOutputStream=new BufferedOutputStream(new FileOutputStream(file));
            // DataOutputStream dataOutputStream=new DataOutputStream(bufferedOutputStream);
            DataOutputStream dataOutputStream=new DataOutputStream(new FileOutputStream(file));
            store(dataOutputStream);
            dataOutputStream.close();
        } catch (IOException e) {
            return false;
        }
        if (ctx != null)
            MediaScannerConnection.scanFile(ctx, new String[] { file.getAbsolutePath() }, null, null);
        return true;
    }

    public boolean store(File file) {
        return store(null,file);
    }

    public boolean fetch(File file) {
        try {
            DataInputStream dataInputStream=new DataInputStream(new FileInputStream(file));
            fetch(dataInputStream);
            dataInputStream.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public boolean fetchPersistant(File file) {
        if (fetch(file)) {
            persistant = true;
            persistantFile = file;
            return true;
        }
        return false;
    }

    protected String tempName() {
        String t="";
        Random random=new Random();
        for (int i=0;i<8;i++) {
            int rndchar=random.nextInt(36);
            if (rndchar < 26)
                t+=(char) (rndchar+ 'a');
            else
                t+=(char) (rndchar-26+'0');
        }
        return t;
    }

    protected File getTempFile(Context ctx, String ext) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String prefix=sdf.format(Calendar.getInstance().getTime());
        int trys=0;
        while (trys < 10) {
            File f=new File(ctx.getFilesDir(),prefix+tempName()+ext);
            if (!f.exists())
                return f;
            trys++;
        }
        return null; // Not possible
    }

    public void setPersistant(Context ctx, boolean _persistant) {
        if (_persistant == persistant) return;
        if (!persistant) {
            // Store File
            File f=getTempFile(ctx,".aaraw");
            if (f == null)
                return;
            if (store(f)) {
                // Success
                persistantFile=f;
                persistant=true;
            }
        } else {
            // Remove persistance
            persistantFile.delete();
            persistant=false;
        }
    }

    private void txtwrite(FileOutputStream fileOutputStream, String string) throws IOException {
        fileOutputStream.write(string.getBytes());
    }


    public boolean copyResourceToFile(File file, Context ctx, int resourceID) {
        try {
            InputStream in = ctx.getResources().openRawResource(resourceID);
            FileOutputStream af = new FileOutputStream(file);
            byte [] buf= new byte[1024];
            boolean eof=false;
            while (!eof) {
                int l=in.read(buf,0,1024);
                if (l > 0) {
                    af.write(buf,0,l);
                } else
                    eof=true;
            }
            af.close();
            in.close();
            MediaScannerConnection.scanFile(ctx, new String[] { file.getAbsolutePath() }, null, null);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public boolean saveFiles(AudioAnalyzer ctx, File basedir) {
        File logoFile=new File(basedir,"icon.png");
        File qrFile=new File(basedir,"qrcode.png");
        File rootFile=new File(basedir,"index.html");
        File specFile=new File(basedir,"spec.png");
        File waveFile=new File(basedir,"wave.png");
        File audioFile=new File(basedir,"audio.wav");
        File rawFile=new File(basedir,"recording.aaraw");
        try {
            // HTML File
            FileOutputStream html = new FileOutputStream(rootFile);
            txtwrite(html,"<html><head>\n");

            txtwrite(html,"<title>Audio Analyzer Report " + name + "</title>\n");
            txtwrite(html,"</head>\n");
            txtwrite(html,"<body>\n"); //  bgcolor=\"#000000\" text=\"#FFFFFF\" link=\"#00FF00\" alink=\"#00FF00\" text=\"#008000\">\n");

            txtwrite(html,"<table border=\"1\" frame=\"box\" rules=\"rows\">\n");
            txtwrite(html,"<tr><td><img src=\"icon.png\"></td>\n");
            txtwrite(html,"<td width=\"100%\" align=\"center\"><h1>AudioAnalyzer Report</h1><br>" + name + "</td>\n");
            txtwrite(html,"<td><a border=\"0\" href=\"" + ctx.getString(R.string.googlePlayLink) + "\">");
            txtwrite(html,"<img src=\"qrcode.png\"></a></td></tr>");

            txtwrite(html,"<tr><td colspan=\"3\">");
            txtwrite(html,"<h2>Parameters</h2>\n");
            txtwrite(html,"<table border=0>\n");
            txtwrite(html,"<tr><td>Record Device</td><td>" + deviceName + "</td></tr>\n");
            txtwrite(html,"<tr><td>Record Date</td><td>" + dateString() + "</td></tr>\n");
            txtwrite(html,"<tr><td>Record Length</td><td>" + lengthString() + "</td></tr>\n");
            txtwrite(html,"<tr><td>Samples</td><td>" + String.format("%d",samples) + "</td></tr>\n");
            txtwrite(html,"<tr><td>Samplerate</td><td>" + String.format("%1.0f",samplerate) + " Hz</td></tr>\n");
            txtwrite(html,"</table>\n");
            txtwrite(html,"</td></tr>\n");

            txtwrite(html,"<tr><td colspan=\"3\">");
            txtwrite(html,"<h2>Spectrum</h2>\n");
            txtwrite(html,"<img src=\"spec.png\"><br>\n");
            txtwrite(html,"</td></tr>\n");

            txtwrite(html,"<tr><td colspan=\"3\">");
            txtwrite(html,"<h2>Wave</h2>\n");
            txtwrite(html,"<img src=\"wave.png\"><br>\n");
            txtwrite(html,"</td></tr>\n");

            txtwrite(html,"<tr><td colspan=\"3\">");
            txtwrite(html,"<h2>Levels</h2>\n");
            txtwrite(html,"<table border=\"0\">\n");
            txtwrite(html,"<tr><th align=\"left\">Name</th>");
            txtwrite(html,"<th align=\"right\">"+ctx.getCalNote(calMode) + "</th><th width=\"50\"></th>");
            for (int j=0;j<ctx.getCalModes();j++) {
                if (calMode!=j)
                    txtwrite(html,"<th align=\"right\">"+ctx.getCalNote(j)+"</th>");
            }
            txtwrite(html,"</tr>\n");

            txtwrite(html,"<tr><th align=\"right\">Unit</th>");
            txtwrite(html,"<th align=\"right\">" + ctx.getUnit(calMode) + "</th><th></th>");
            for (int j=0;j<ctx.getCalModes();j++) {
                if (calMode!=j)
                    txtwrite(html,"<th  align=\"right\">"+ctx.getUnit(j)+"</th>");
            }
            txtwrite(html,"</tr>\n");

            for (int i=0;i<energy_names.length;i++) {
                txtwrite(html,"<tr><td><b>" + energy_names[i] + "</b></td>");
                txtwrite(html,"<td align=\"right\">"+String.format("%1.2f",energies[i]+ctx.getCalOfs(calMode)) + "</td><td></td>" );
                for (int j=0;j<ctx.getCalModes();j++)
                    if (calMode != j)
                        txtwrite(html,"<td align=\"right\">"+String.format("%1.2f",energies[i]+ctx.getCalOfs(j)) + "</td>");
                txtwrite(html,"</tr>\n");
            }
            txtwrite(html,"</table>\n");
            txtwrite(html,"</td></tr>\n");

            txtwrite(html,"<tr><td colspan=\"3\">");
            txtwrite(html,"<h2>WAV File</h2>\n");
            txtwrite(html,"<a href=\"audio.wav\">WAV File</a><br>\n");
            txtwrite(html,"</td></tr>\n");

            txtwrite(html,"<tr><td colspan=\"3\" align=\"center\">\n");
            txtwrite(html,"<a href=\"" + ctx.getString(R.string.googlePlayLink) + "\">" +
                    "AudioAnalyzer " + ctx.getVersion() + "</a>\n");
            txtwrite(html,"</td></tr>\n");

            txtwrite(html,"<tr><td colspan=\"3\" align=\"center\">\n");
            txtwrite(html,ctx.getString(R.string.reportFooterText) + "\n");
            txtwrite(html,"</td></tr>\n");
            txtwrite(html,"</table\n");
            txtwrite(html,"</body></html>\n");
            html.close();
            MediaScannerConnection.scanFile(ctx, new String[] { rootFile.getAbsolutePath() }, null, null);
        } catch (IOException e) {
            return false;
        }

        // Spectrum Plot
        try {
            ArrayList<XYdata> thisone = new ArrayList<XYdata>();
            thisone.add(specPlot);
            Bitmap bspec=Bitmap.createBitmap(800,600, Bitmap.Config.ARGB_8888);
            Canvas canvas=new Canvas(bspec);
            int colbup=specPlot.color;
            specPlot.color= Color.argb(255,255,0,0);
            ctx.rp_view.drawToCanvas(canvas,thisone,15.0f,true,true);
            specPlot.color=colbup;
            FileOutputStream spec=new FileOutputStream(specFile);
            bspec.compress(Bitmap.CompressFormat.PNG,100,spec);
            spec.close();
            MediaScannerConnection.scanFile(ctx, new String[] { specFile.getAbsolutePath() }, null, null);
        } catch (IOException e) {
            return false;
        }

        // Wave Plot
        try {
            ArrayList<XYdata> thisone = new ArrayList<XYdata>();
            thisone.add(wavePlot);
            Bitmap bspec=Bitmap.createBitmap(800,400, Bitmap.Config.ARGB_8888);
            Canvas canvas=new Canvas(bspec);
            int colbup=wavePlot.color;
            wavePlot.color= Color.argb(255,255,0,0);
            ctx.rp_wave_view.drawToCanvas(canvas,thisone,15.0f,true,true);
            wavePlot.color=colbup;
            FileOutputStream spec=new FileOutputStream(waveFile);
            bspec.compress(Bitmap.CompressFormat.PNG,100,spec);
            spec.close();
            MediaScannerConnection.scanFile(ctx, new String[] { waveFile.getAbsolutePath() }, null, null);
        } catch (IOException e) {
            return false;
        }

        // Wav File
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

        // Raw Data
        // store(ctx,rawFile);

        // Aux Files
        copyResourceToFile(logoFile, ctx, R.raw.icon);
        copyResourceToFile(qrFile, ctx, R.raw.qrcode);

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
        specPlot =new XYdata(txtSize,viewer.getNextColor());
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
        wavePlot =new XYdata(txtSize,waveViewer.getNextColor());
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

    public void setName(Context ctx,String _name) {
        name=_name;
        wavePlot.name=_name;
        specPlot.name=_name;
        if (persistant) {
            store(ctx,persistantFile);
        }
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
