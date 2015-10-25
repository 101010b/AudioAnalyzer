package com.alphadraco.audioanalyzer;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.net.rtp.AudioCodec;
import android.preference.PreferenceManager;
// import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

// public class AudioAnalyzer extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {
public class AudioAnalyzer extends Activity implements PopupMenu.OnMenuItemClickListener {

    private TabHost myTabHost;

    AudioRecord audioRecord;
    AudioTrack audioPlayer;
    AudioSource audioSource;

    private Thread recordingThread = null;
    private Thread processingThread = null;
    private Thread playingThread = null;
    private SpectralView spectralView=null;
    private LevelBar levelBar1=null;
    private LevelBar levelBar2=null;
    private WaveView waveView=null;

    private boolean isRecording = false;
    private boolean pause=false;
    public boolean resetPeak=false;

    private int[] fftsizes = {128,256,512,1024,2048,4096,8192};
    private int fftsize=fftsizes[0];
    private int window=0;
    private int int_sel1=0;
    private int fsample=44100;
    private int recms=0;
    private float trackf=-1;

    private int calmode=0;

    private boolean generator_on=false;

    private ScheduledExecutorService scheduledExecutorService;

    private Semaphore dataAvailable;

    private ProcessBufferList processBufferList;
    private ProcessResultList processResultList;
    private DataConsolidator dataConsolidator;

    public AudioAnalyzerHelper audioAnalyzerHelper;

    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    public SharedPreferences AudioAnalyzerPrefs;

    // Analyzer GUI
    private ImageButton bn_play_pause;
    private ImageButton bn_menu;
    private ImageButton bn_speaker;
    private ImageButton bn_spec_mode;
    private ImageButton bn_cal;
    private ImageButton bn_zoom_all;

    private TextView tx_status;
    private TextView tx_cal;

    // Generator GUI
    public ArrayList<ControlInput> fgen_list;

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

    private void setNewTab(Context context, TabHost tabHost, String tag, String title, int icon, int contentID ){
        TabHost.TabSpec tabSpec = tabHost.newTabSpec(tag);
        tabSpec.setIndicator(getTabIndicator(tabHost.getContext(), title, icon)); // new function to inject our own tab layout
        tabSpec.setContent(contentID);
        tabHost.addTab(tabSpec);
    }

    private View getTabIndicator(Context context, String title, int icon) {
        View view = LayoutInflater.from(context).inflate(R.layout.tab_layout, null);
        ImageView iv = (ImageView) view.findViewById(R.id.imageView);
        iv.setImageResource(icon);
        TextView tv = (TextView) view.findViewById(R.id.textView);
        tv.setText(title);
        return view;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().hide();
        //getSupportActionBar().hide();
        setContentView(R.layout.activity_audio_analyzer);

        myTabHost = (TabHost) findViewById(R.id.tabhost);
        myTabHost.setup();

        /*
        TabHost.TabSpec spec = myTabHost.newTabSpec("tab_analyze");
        spec.setIndicator("Analyzer", getResources().getDrawable(android.R.drawable.ic_menu_add));
        spec.setContent(R.id.analyze);
        myTabHost.addTab(spec);
         */

        setNewTab(this,myTabHost,"tab_analyze","Analyzer",R.mipmap.sink,R.id.analyze);
        setNewTab(this,myTabHost,"tab_generate","Generator",R.mipmap.source,R.id.generate);

        /*
        myTabHost.addTab(myTabHost.newTabSpec("tab_analyze").setIndicator("Analyzer",
                getResources().getDrawable(android.R.drawable.ic_menu_add)).setContent(R.id.analyze));

        myTabHost.addTab(myTabHost.newTabSpec("tab_generate").setIndicator("Generator",
                getResources().getDrawable(android.R.drawable.ic_menu_edit)).setContent(R.id.generate));

        TabWidget tw=myTabHost.getTabWidget();
        */

        // Analyzer GUI
        bn_play_pause=(ImageButton) findViewById(R.id.bn_play_pause);
        bn_menu=(ImageButton) findViewById(R.id.bn_show_menu);
        bn_speaker=(ImageButton) findViewById(R.id.bn_speaker);
        bn_cal=(ImageButton) findViewById(R.id.bn_cal);
        bn_spec_mode=(ImageButton) findViewById(R.id.bn_spec_mode);
        bn_zoom_all=(ImageButton) findViewById(R.id.bn_zoom_all);
        tx_status=(TextView) findViewById(R.id.tx_status);
        tx_cal=(TextView) findViewById(R.id.tx_cal);
        waveView=(WaveView) findViewById(R.id.wv_wave);
        spectralView=(SpectralView) findViewById(R.id.vw_spec);

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

        spectralView.setPreferences(AudioAnalyzerPrefs);
        spectralView.audioAnalyzer=this;

        waveView.setPreferences(AudioAnalyzerPrefs);
        calmode=Integer.parseInt(AudioAnalyzerPrefs.getString("calMode","0"));

        int minBuffSize = AudioRecord.getMinBufferSize(fsample, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        // recbufsize=minBuffSize;
        /* MediaRecorder.AudioSource.VOICE_RECOGNITION has AGC turned off --> calibration is possible
           MediaRecorder.AudioSource.MIC has AGC turned on --> BAD
        */
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,fsample,
                AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,2*8192);

        /* This does nothing on all my devices...
        AutomaticGainControl agc= AutomaticGainControl.create(audioRecord.getAudioSessionId());
        AcousticEchoCanceler aec=AcousticEchoCanceler.create(audioRecord.getAudioSessionId());
        if (agc != null) agc.setEnabled(false);
        if (aec != null) aec.setEnabled(false);
        */

        levelBar1=(LevelBar)findViewById(R.id.bv_bar1);
        levelBar1.intmode=0;
        levelBar1.fixedmode=true;
        levelBar1.setPreferences(AudioAnalyzerPrefs,0);

        levelBar2=(LevelBar)findViewById(R.id.bv_bar2);
        levelBar2.intmode=8;
        levelBar2.setPreferences(AudioAnalyzerPrefs, 1);

        updateCal();

        audioAnalyzerHelper = new AudioAnalyzerHelper();
        waveView.helper=audioAnalyzerHelper;
        spectralView.helper=audioAnalyzerHelper;

        audioSource=new AudioSource(audioAnalyzerHelper);

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
        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED)
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

        playingThread = new Thread(new Runnable() {
            public void run() {
                playAudioData();
            }
        }, "AudioPlaying Thread");
        playingThread.start();

        generator_on=AudioAnalyzerPrefs.getBoolean("Signal_OnOff",false);
        if (generator_on)
            bn_speaker.setImageResource(R.mipmap.speaker_on);
        else
            bn_speaker.setImageResource(R.mipmap.speaker);
        audioAnalyzerHelper.SignalProg(21,(generator_on)?1.0f:0.0f);


        if (AudioAnalyzerPrefs.getBoolean("DisplayWaterfall",false)) {
            bn_spec_mode.setImageResource(R.mipmap.spectral_button);
        }

        // Analyzer GUI Interactions
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

        bn_speaker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (generator_on) {
                    generator_on=false;
                    bn_speaker.setImageResource(R.mipmap.speaker);
                } else {
                    generator_on=true;
                    bn_speaker.setImageResource(R.mipmap.speaker_on);
                }
                audioAnalyzerHelper.SignalProg(21,(generator_on)?1.0f:0.0f);
                SharedPreferences.Editor e=AudioAnalyzerPrefs.edit();
                e.putBoolean("Signal_OnOff",generator_on);
                e.commit();
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
                spectralView.zoomAll();
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
                            case 1:
                                calmode = 0;
                                break;
                            case 2:
                                calmode = 1;
                                break;
                            case 3:
                                calmode = 2;
                                break;
                            case 4:
                                calmode = 3;
                                break;
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
                    spectralView.displaywaterfall = false;
                    bn_spec_mode.setImageResource(R.mipmap.waterfall_button);
                } else {
                    spectralView.displaywaterfall = true;
                    bn_spec_mode.setImageResource(R.mipmap.spectral_button);
                }
                SharedPreferences.Editor E = AudioAnalyzerPrefs.edit();
                E.putBoolean("DisplayWaterfall", spectralView.displaywaterfall);
                E.apply();
            }
        });

        // Generator GUI Interactions
        ArrayList<String> fgen_function_list = new ArrayList<String>();
        fgen_function_list.add("Sine");
        fgen_function_list.add("Triangle");
        fgen_function_list.add("Saw");
        fgen_function_list.add("Rectangle");
        fgen_function_list.add("Noise");
        ArrayList<String> sweep_function_list = new ArrayList<String>();
        sweep_function_list.add("Upwards");
        sweep_function_list.add("Downwards");
        sweep_function_list.add("Up-Down");

        fgen_list=new ArrayList<ControlInput>(0);
        // fgen_list.add(new ControlInput(this,R.id.sw_signal_onoff,"Signal_OnOff",21,false));

        fgen_list.add(new ControlInput(this,R.id.sw_signal_mod_onoff,   "Signal_Mod_OnOff",     4,  false));
        fgen_list.add(new ControlInput(this,R.id.tr_signal_mod_freq, R.id.sb_quick_input,
                                            R.id.nm_signal_mod_freq,    "Signal_Mod_freq",      0,  0.1f,24.0e3f,10.0f,"%1.1f"));
        fgen_list.add(new ControlInput(this,R.id.tr_signal_mod_amp, R.id.sb_quick_input,
                                            R.id.nm_signal_mod_amp,     "Signal_Mod_amp",       1,  -100.0f,0.0f,-20.0f,"%1.1f",true));
        fgen_list.add(new ControlInput(this,R.id.sp_signal_mod_func,    "Signal_Mod_func",      2,  fgen_function_list,0));
        fgen_list.add(new ControlInput(this,R.id.tr_signal_mod_pwm, R.id.sb_quick_input,
                                            R.id.nm_signal_mod_pwm,     "Signal_Mod_pwidth",    3,  0.0f,100.0f,50.0f,"%1.0f",0.01f));
        fgen_list.add(new ControlInput(this,R.id.cb_signal_mod_AM,      "Signal_Mod_AM",        17, false));
        fgen_list.add(new ControlInput(this,R.id.cb_signal_mod_FM,      "Signal_Mod_FM",        18, false));
        fgen_list.add(new ControlInput(this,R.id.cb_signal_mod_PM,      "Signal_Mod_PM",        19, false));
        fgen_list.add(new ControlInput(this,R.id.cb_signal_mod_PWM,     "Signal_Mod_PWM",       20, false));
        fgen_list.add(new ControlInput(this,R.id.cb_signal_mod_ADD,     "Signal_Mod_ADD",       24, false));

        fgen_list.add(new ControlInput(this,R.id.sw_signal_swp_onoff,   "Signal_Sweep_OnOff",   10, false));
        fgen_list.add(new ControlInput(this,R.id.tr_signal_swp_start, R.id.sb_quick_input,
                                            R.id.nm_signal_swp_start,   "Signal_Sweep_start",   5,  0.1f,24.0e3f,100.0f,"%1.1f"));
        fgen_list.add(new ControlInput(this,R.id.tr_signal_swp_stop, R.id.sb_quick_input,
                                            R.id.nm_signal_swp_stop,    "Signal_Sweep_stop",    6,  0.1f,24.0e3f,20.0e3f,"%1.1f"));
        fgen_list.add(new ControlInput(this,R.id.cb_signal_swp_log,     "Signal_Sweep_log",     7,  true));
        fgen_list.add(new ControlInput(this,R.id.tr_signal_swp_time, R.id.sb_quick_input,
                                            R.id.nm_signal_swp_time,    "Signal_Sweep_time",    9,  0.1f,600.0f,10.0f,"%1.1f"));
        fgen_list.add(new ControlInput(this,R.id.sp_signal_swp_func,    "Signal_Sweep_func",    8,  sweep_function_list,0));
        fgen_list.add(new ControlInput(this,R.id.cb_signal_swp_loop,    "Signal_Sweep_loop",    22, true));
        fgen_list.add(new ControlInput(this,R.id.cb_signal_fsweep,      "Signal_Fsweep",        16, false));

        fgen_list.add(new ControlInput(this,R.id.sw_signal_gen_onoff,   "Signal_Gen_OnOff",     15, true));
        fgen_list.add(new ControlInput(this,R.id.tr_signal_gen_freq, R.id.sb_quick_input,
                                            R.id.nm_signal_gen_freq,    "Signal_Gen_freq",      11, 0.1f,24.0e3f,1000.0f,"%1.1f"));
        fgen_list.add(new ControlInput(this,R.id.tr_signal_gen_amp, R.id.sb_quick_input,
                                            R.id.nm_signal_gen_amp,     "Signal_Gen_amp",       12, -100.0f,0.0f,-20.0f,"%1.1f",true));
        fgen_list.add(new ControlInput(this,R.id.sp_signal_gen_func,    "Signal_Gen_func",      13, fgen_function_list,0));
        fgen_list.add(new ControlInput(this,R.id.tr_signal_gen_pwm, R.id.sb_quick_input,
                                            R.id.nm_signal_gen_pwm,     "Signal_Mod_pwidth",    14, 0.0f,100.0f,50.0f,"%1.0f",0.01f));

        fgen_list.add(new ControlInput(this,R.id.bn_signal_swp_trigger,23));

        SeekBar sb=(SeekBar) findViewById(R.id.sb_quick_input);
        // sb.setThumb(getResources().getDrawable(R.drawable.valueselectthumb));

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

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            // Just emulate sound source
            while (isRecording) {
                if (!pause) {
                    recms += fftsize;
                    for (int i=0;i<fftsize;i++) {
                        float t=(float)i/fsample;
                        sData[i]=(short)(32767.0*Math.sin(2*Math.PI*10000*t));
                    }
                    ProcessBuffer pb = new ProcessBuffer(fsample, fftsize, sData);
                    processBufferList.add(pb);
                    try { Thread.sleep(fftsize*1000/fsample,0); }
                    catch (InterruptedException E ){
                        // Ignore
                    }
                }
            }
            return;
        }

        while (isRecording) {
            // gets the voice output from microphone to byte format

            audioRecord.read(sData, 0, fftsize);
            if (!pause) {
                recms += fftsize;
                /*for (int i=0;i<fftsize;i++) {
                    float t=(float)i/fsample;
                    sData[i]=(short)(32767.0*Math.sin(2*Math.PI*10000*t));
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

    // Audio playing Process (Signal Generator)
    private void playAudioData() {
        int bufsize=AudioTrack.getMinBufferSize(44100,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);
        short[] data1 = new short[bufsize/4];
        //short[] data2 = new short[bufsize/4];
        boolean playstarted=false;

        audioPlayer=new AudioTrack(AudioManager.STREAM_MUSIC,
                44100, AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT,
                bufsize,AudioTrack.MODE_STREAM);

        // audioPlayer.play();
        // audioSource.getData(data2);
        audioPlayer.play();

        while (isRecording && (audioPlayer != null)) {
            audioSource.getData(data1);
            audioPlayer.write(data1, 0, data1.length);
            // audioPlayer.flush();
            // try { Thread.sleep(10); } catch (InterruptedException E) { }
            // audioPlayer.write(data2, 0, data2.length);
        }

    }

    private void stopRecording() {
        // stops the recording activity
        if (null != audioRecord) {
            isRecording = false;
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED)
                audioRecord.stop();
            audioPlayer.stop();
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
        TextView tv=(TextView) messageView.findViewById(R.id.tv_about_version);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setTitle("AudioAnalyzer");
        String version="undefined";
        try {
            PackageManager manager = this.getPackageManager();
            PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
            version = info.versionName;
        } catch (PackageManager.NameNotFoundException E) {
            // Ignore
        }
        tv.setText(version);
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
