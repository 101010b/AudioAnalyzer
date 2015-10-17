package com.alphadraco.audioanalyzer;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class AudioAnalyzer extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {

    AudioRecord audioRecord;
    private Thread recordingThread = null;
    private Thread processingThread = null;
    private SpectralView spectralView=null;
    private LevelBar levelBar1=null;
    private LevelBar levelBar2=null;
    private WaveView waveView=null;

    private boolean isRecording = false;
    private boolean pause=false;
    public boolean resetPeak=false;

    private int recbufsize=0;
    private int[] fftsizes = {128,256,512,1024,2048,4096,8192};
    private int fftsize=fftsizes[0];
    private int window=0;
    private int int_sel1=0;
    private int fsample=44100;
    private int recms=0;
    private float trackf=-1;

    private int calmode=0;

    private ScheduledExecutorService scheduledExecutorService;

    private Semaphore dataAvailable;

    private ProcessBufferList processBufferList;
    private ProcessResultList processResultList;
    private DataConsolidator dataConsolidator;

    private AudioAnalyzerHelper audioAnalyzerHelper;

    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    private SharedPreferences AudioAnalyzerPrefs;

    private ImageButton bn_play_pause;
    private ImageButton bn_menu;
    private ImageButton bn_spec_mode;
    private ImageButton bn_cal;
    private ImageButton bn_zoom_all;

    private TextView tx_status;
    private TextView tx_cal;

    // Calibration
    float mic_in_MO;
    float mic_in_Vref;
    float mic_in_dBFS;

    float mic_Pref;
    float mic_dBFS;

    public void updateCal() {
        float ofs;
        switch (calmode) {
            case 0:
                // No Calibration
                ofs=0.0f;
                spectralView.ofs=0.0f;
                spectralView.unit="dBFS";
                spectralView.note="direct ADC";
                levelBar1.ofs=0.0f;
                levelBar1.unit = "dBFS";
                levelBar2.ofs = 0.0f;
                levelBar2.unit="dBFS";
                tx_cal.setText("No Calibration");
                break;
            case 1:
                // internal Microphone
                ofs=(float)(mic_Pref-mic_dBFS);
                spectralView.ofs=ofs;
                spectralView.unit="dBSPL";
                spectralView.note="Internal Mic";
                levelBar1.ofs=ofs;
                levelBar1.unit="dBSPL";
                levelBar2.ofs=ofs;
                levelBar2.unit="dBSPL";
                tx_cal.setText("Internal Microphone");
                break;
            case 2:
                // external input
                ofs=(float)(20.0*Math.log10(mic_in_Vref/1000.0)-mic_in_dBFS);
                spectralView.ofs=ofs;
                spectralView.unit="dBV";
                spectralView.note="External In";
                levelBar1.ofs=ofs;
                levelBar1.unit="dBV";
                levelBar2.ofs=ofs;
                levelBar2.unit="dBV";
                tx_cal.setText("External In");
                break;
            case 3:
                // external mic
                ofs=(float)(20.0*Math.log10(mic_in_Vref/1000.0)-mic_in_dBFS-mic_in_MO+94.0f);
                spectralView.ofs=ofs;
                spectralView.unit="dBSPL";
                spectralView.note="External Mic";
                levelBar1.ofs=ofs;
                levelBar1.unit="dBSPL";
                levelBar2.ofs=ofs;
                levelBar2.unit="dBSPL";
                tx_cal.setText("External Mic");
                break;
        }
    }

    public void unitClick() {
        if (calmode == 3) calmode=0; else calmode++;
        updateCal();
        SharedPreferences.Editor E=AudioAnalyzerPrefs.edit();
        E.putInt("CalMode",calmode);
        E.apply();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_audio_analyzer);

        bn_play_pause=(ImageButton) findViewById(R.id.bn_play_pause);
        bn_menu=(ImageButton) findViewById(R.id.bn_show_menu);
        bn_cal=(ImageButton) findViewById(R.id.bn_cal);
        bn_spec_mode=(ImageButton) findViewById(R.id.bn_spec_mode);
        bn_zoom_all=(ImageButton) findViewById(R.id.bn_zoom_all);

        tx_status=(TextView) findViewById(R.id.tx_status);
        tx_cal=(TextView) findViewById(R.id.tx_cal);

        waveView=(WaveView) findViewById(R.id.wv_wave);

        // Load Preferences
        AudioAnalyzerPrefs =  PreferenceManager.getDefaultSharedPreferences(this);

        listener=new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                if (key.equals("FFTSize")) {
                    fftsize=Integer.parseInt(prefs.getString(key,"128"));
                }
                if (key.equals("FFTWindow")) {
                    window=Integer.parseInt(prefs.getString(key, "0"));
                }
                if (key.equals("CalMicIn_MO")) {
                    mic_in_MO = Float.parseFloat(AudioAnalyzerPrefs.getString("CalMicIn_MO", "-38"));
                    updateCal();
                }
                if (key.equals("CalMicIn_V")) {
                    mic_in_Vref = Float.parseFloat(AudioAnalyzerPrefs.getString("CalMicIn_V", "10"));
                    updateCal();
                }
                if (key.equals("CalMicIn_S")) {
                    mic_in_dBFS = Float.parseFloat(AudioAnalyzerPrefs.getString("CalMicIn_S", "-20.0"));
                    updateCal();
                }
                if (key.equals("CalMic_P")) {
                    mic_Pref = Float.parseFloat(AudioAnalyzerPrefs.getString("CalMic_P", "94"));
                    updateCal();
                }
                if (key.equals("CalMic_S")) {
                    mic_dBFS = Float.parseFloat(AudioAnalyzerPrefs.getString("CalMic_S", "-20.0"));
                    updateCal();
                }
            }
        };
        AudioAnalyzerPrefs.registerOnSharedPreferenceChangeListener(listener);

        fftsize = Integer.parseInt(AudioAnalyzerPrefs.getString("FFTSize", "128"));
        window = Integer.parseInt(AudioAnalyzerPrefs.getString("FFTWindow", "0"));

        mic_in_MO=Float.parseFloat(AudioAnalyzerPrefs.getString("CalMicIn_MO", "-38"));
        mic_in_Vref=Float.parseFloat(AudioAnalyzerPrefs.getString("CalMicIn_V", "10"));
        mic_in_dBFS=Float.parseFloat(AudioAnalyzerPrefs.getString("CalMicIn_S","-20.0"));

        mic_Pref=Float.parseFloat(AudioAnalyzerPrefs.getString("CalMic_P","94"));
        mic_dBFS=Float.parseFloat(AudioAnalyzerPrefs.getString("CalMic_S","-20.0"));

        int minBuffSize = AudioRecord.getMinBufferSize(fsample, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        recbufsize=minBuffSize;
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,fsample,
                AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,2*8192);

        spectralView=(SpectralView) findViewById(R.id.vw_spec);
        spectralView.setPreferences(AudioAnalyzerPrefs);
        spectralView.audioAnalyzer=this;

        waveView.setPreferences(AudioAnalyzerPrefs);

        calmode=Integer.parseInt(AudioAnalyzerPrefs.getString("calMode","0"));

        levelBar1=(LevelBar)findViewById(R.id.bv_bar1);
        levelBar1.intmode=0;
        levelBar1.fixedmode=true;
        levelBar1.setPreferences(AudioAnalyzerPrefs,0);

        levelBar2=(LevelBar)findViewById(R.id.bv_bar2);
        levelBar2.intmode=8;
        levelBar2.setPreferences(AudioAnalyzerPrefs,1);

        updateCal();

        audioAnalyzerHelper = new AudioAnalyzerHelper();
        waveView.helper=audioAnalyzerHelper;
        spectralView.helper=audioAnalyzerHelper;

        dataConsolidator = new DataConsolidator();
        dataConsolidator.nft=audioAnalyzerHelper;

        scheduledExecutorService=Executors.newScheduledThreadPool(5);
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tick();
                    }
                });
            }
        }, 50, 50, TimeUnit.DAYS.MILLISECONDS);

        processBufferList=new ProcessBufferList(3);
        processResultList=new ProcessResultList(3);

        recms=0;
        audioRecord.startRecording();
        isRecording=true;

        recordingThread = new Thread(new Runnable() {
            public void run() {
                recordAudioData();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();

        processingThread = new Thread(new Runnable() {
            public void run() {
                processAudioData();
            }
        }, "AudioProcessing Thread");
        processingThread.start();

        bn_menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(AudioAnalyzer.this, v);
                popup.setOnMenuItemClickListener(AudioAnalyzer.this);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.menu_audio_analyzer, popup.getMenu());
                popup.show();
            }
        });

        bn_play_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pause) {
                    pause=false;
                    bn_play_pause.setImageResource(R.mipmap.pause_button);
                } else {
                    pause=true;
                    bn_play_pause.setImageResource(R.mipmap.play_button);
                }
            }
        });

        bn_zoom_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (spectralView.islog) {
                    spectralView.fmin = dataConsolidator.f[1];
                    spectralView.fmax = dataConsolidator.f[dataConsolidator.len/2 - 1];
                    if (!spectralView.displaywaterfall) {
                        spectralView.lmin = -120;
                        spectralView.lmax = 0;
                    }
                } else {
                    spectralView.fmin = 0;
                    spectralView.fmax = dataConsolidator.f[dataConsolidator.len/2 - 1];
                    if (!spectralView.displaywaterfall) {
                        spectralView.lmin = -120;
                        spectralView.lmax = 0;
                    }
                }
            }
        });

        bn_cal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(AudioAnalyzer.this, v);
                Menu menu = popup.getMenu();
                menu.add(0,1,Menu.NONE,"No Calibration");
                menu.add(0,2,Menu.NONE,"Internal Microphone");
                menu.add(0,3,Menu.NONE,"External Input");
                menu.add(0,4,Menu.NONE,"External Microphone");
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case 1: calmode = 0; break;
                            case 2: calmode = 1; break;
                            case 3: calmode = 2; break;
                            case 4: calmode = 3; break;
                        }
                        SharedPreferences.Editor E = AudioAnalyzerPrefs.edit();
                        E.putInt("CalMode", calmode);
                        E.apply();
                        updateCal();
                        return false;
                    }
                });
                popup.show();
            }
        });

        bn_spec_mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (spectralView.displaywaterfall) {
                    spectralView.displaywaterfall=false;
                    bn_spec_mode.setImageResource(R.mipmap.waterfall_button);
                } else {
                    spectralView.displaywaterfall=true;
                    bn_spec_mode.setImageResource(R.mipmap.spectral_button);
                }
                SharedPreferences.Editor E = AudioAnalyzerPrefs.edit();
                E.putBoolean("DisplayWaterfall", spectralView.displaywaterfall);
                E.apply();
            }
        });

    }

    private void tick() {
        if (isRecording) {
            float time=(float)recms/(float)fsample;
            String s=String.format("%4.2f s", time);
            tx_status.setText(s);
            ProcessResult last=null;
            ProcessResult pr;
            while ((pr=processResultList.retrieve())!=null) {
                last=pr;
            }
            if (last!=null) {
                dataConsolidator.add(last);
                spectralView.display(dataConsolidator);
                levelBar1.display(dataConsolidator);
                levelBar2.display(dataConsolidator);
            }
            dataConsolidator.tick();
            if (waveView != null)
                waveView.display(dataConsolidator);
        } else {
            ((TextView) findViewById(R.id.tx_status)).setText("Offline");
        }
    }

/*    private void enableButton(int id, boolean isEnable) {
        ((Button) findViewById(id)).setEnabled(isEnable);
    }

    private void enableButtons(boolean isRecording) {
        enableButton(R.id.bn_stop, isRecording);
    }
*/

    // int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    // int BytesPerElement = 2; // 2 bytes in 16bit format


    // Audio Recording Process
    private void recordAudioData() {
        short sData[] = new short[8192];

        while (isRecording) {
            // gets the voice output from microphone to byte format

            audioRecord.read(sData, 0, fftsize);
            if (!pause) {
                recms += fftsize;
                /*for (int i=0;i<fftsize;i++) {
                    float t=(float)i/fsample;
                    sData[i]=(short)(32767.0*Math.sin(2*Math.PI*1000*t));
                }*/

                ProcessBuffer pb = new ProcessBuffer(fsample, fftsize, sData);
                processBufferList.add(pb);
            }
        }
    }

    // Audio Processing Process
    private void processAudioData() {
        ProcessBuffer pb;
        while (isRecording) {
            while ((pb = processBufferList.retrieve()) != null) {
                if (!pause) {
                    trackf=spectralView.trackf;
                    dataConsolidator.trackf=trackf;
                    ProcessResult pr = new ProcessResult(pb, trackf, window);
                    if (resetPeak) audioAnalyzerHelper.fftResetPeak();
                    resetPeak=false;
                    pr.process(audioAnalyzerHelper);
                    processResultList.add(pr);
                }
            }
            // Wait for next event
            processBufferList.doWait();
        }
    }

    private void stopRecording() {
        // stops the recording activity
        if (null != audioRecord) {
            isRecording = false;
            audioRecord.stop();
            // recorder.release();
            // recorder = null;
            recordingThread = null;
            processBufferList.clear();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            stopRecording();
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_audio_analyzer, menu);
        return true;
    }

    void showAboutWin() {
        View messageView = getLayoutInflater().inflate(R.layout.aboutwin,null,false);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setTitle("AudioAnalyzer");
        builder.setView(messageView);
        builder.create();
        builder.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {

        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent launchSettings = new Intent(this,AudioAnalyzerSettings.class);
            startActivity(launchSettings);
            return true;
        }
        if (id == R.id.action_about) {
            showAboutWin();
            //Intent launchSettings = new Intent(this,AboutWin.class);
            //startActivity(launchSettings);
            return true;
        }
        if (id == R.id.action_quit) {
            stopRecording();
            finish();
            return true;
        }

        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent launchSettings = new Intent(this,AudioAnalyzerSettings.class);
            startActivity(launchSettings);
            return true;
        }
        if (id == R.id.action_about) {
            showAboutWin();
            //Intent launchSettings = new Intent(this,AboutWin.class);
            //startActivity(launchSettings);
            return true;
        }
        if (id == R.id.action_quit) {
            stopRecording();
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
