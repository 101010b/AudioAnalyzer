package com.alphadraco.audioanalyzer;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TabHost;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

// public class AudioAnalyzer extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {
public class AudioAnalyzer extends Activity implements PopupMenu.OnMenuItemClickListener {

    private TabHost tabHost;

    AudioRecord audioRecord;    // Android Java Class for Audio Recording
    AudioTrack audioTrack;      // Android Java Class for Audio Playing

    AudioSource audioSource;    // Signal Generator
    AudioPlayer audioPlayer=null;// Recorded Sound Player, only != null when playing

    private Thread recordingThread = null;
    private Thread processingThread = null;
    private Thread playingThread = null;
    public SpectralView spectralView=null;
    private LevelBar levelBar1=null;
    private LevelBar levelBar2=null;
    private WaveView waveView=null;

    private boolean isRecording = false;
    private boolean quitting=false;
    private int quittick=0;
    private boolean pause=false;
    public boolean resetPeak=false;

    public boolean audioPlayerStopped=false;

    private int[] fftsizes = {128,256,512,1024,2048,4096,8192};
    private int fftsize=fftsizes[0];
    private int window=0;
    private int fsample=44100;
    private int recms=0;

    public String deviceName=""; // Device Name, to be stored in Reports

    public int terzw=0;

    private int calmode=0;

    private boolean generator_on=false;

    private ScheduledExecutorService scheduledExecutorService;
    // private Semaphore dataAvailable;

    private ProcessBufferList processBufferList;
    public DataConsolidator dataConsolidator;

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
    private ImageButton bn_sync;

    private TextView tx_status;
    private TextView tx_cal;

    // StripPlot GUI
    public StripPlotView sp_plot;
    private ImageButton bn_sp_play_pause;
    private ImageButton bn_sp_cal;
    private ImageButton bn_sp_plus;
    private ImageButton bn_sp_minus;
    private ImageButton bn_sp_zoom_all;
    private ImageButton bn_sp_style;

    // Generator GUI
    public ArrayList<ControlInput> fgen_list;

    // Sweep GUI
    private ImageButton bn_xy_cal;
    private ImageButton bn_xy_delete;
    private ImageButton bn_xy_zoom_all;
    private ImageButton bn_xy_setup;
    private TextView tx_xy_sweep_mode;
    private ImageButton bn_xy_play;
    private SweepPlotView xy_plot;
    private ImageButton bn_xy_style;

    private boolean Sweeping=false;
    private int SweepStep=0;
    private int SweepPhase=0;
    private float SweepFreq=0.0f;
    private XYdata SweepPlot=null;
    private boolean SweepRenew=false;

    // Report GUI
    private ImageButton bn_rp_setup;
    private ImageButton bn_rp_cal;
    // private ImageButton bn_rp_minus;
    // private ImageButton bn_rp_style;
    private ImageButton bn_rp_save;
    private ImageButton bn_rp_zoom;
    private ListView lv_rp_items;
    public AudioReportView rp_view;
    public AudioWaveView rp_wave_view;

    private boolean reporting=false;
    private int reportingctr=0;
    private int reportend;
    private int lastserial;
    private boolean reportRenew=false;
    private AudioReport report=null;

    // Calibration
    float mic_in_MO;
    float mic_in_Vref;
    float mic_in_dBFS;

    float mic_Pref;
    float mic_dBFS;

    // Overflow
    boolean overflow;
    int tickctr;

    public int getCalModes() { return 4; }

    public String getUnit(int idx ){
        switch (idx) {
            case 0: // No Calibration
                return "dBFS";
            case 1: // internal Microphone
                return "dBSPL";
            case 2: // external Input
                return "dBV";
            case 3: // external Microphone
                return "dBSPL";
        }
        return null;
    }

    public String getLinearUnit(int idx) {
        switch (idx) {
            case 0: // No Calibration
                return "FS";
            case 1: // internal Microphone
                return "Pa";
            case 2: // external Input
                return "V";
            case 3: // external Microphone
                return "Pa";
        }
        return null;
    }

    public float getCalOfs(int idx) {
        switch (idx) {
            case 0: // No Calibration
                return 0.0f;
            case 1: // internal Microphone
                return (float)(mic_Pref-mic_dBFS);
            case 2: // external Input
                return (float)(20.0*Math.log10(mic_in_Vref/1000.0)-mic_in_dBFS);
            case 3: // external Microphone
                return (float)(20.0*Math.log10(mic_in_Vref/1000.0)-mic_in_dBFS-mic_in_MO+94.0f);
        }
        return 0.0f;
    }

    public float getCalScale(int idx) {
        switch (idx) {
            case 0: // No Calibration
                return 1.0f;
            case 1: // internal Microphone
                return (float)Math.pow(10.0,getCalOfs(idx)/20.0)*20e-6f;
            case 2: // external Input
                return (float)Math.pow(10.0,getCalOfs(idx)/20.0);
            case 3: // external Microphone
                return (float)Math.pow(10.0,getCalOfs(idx)/20.0)*20e-6f;
        }
        return 1.0f;
    }

    public String getCalNote(int idx) {
        switch (idx) {
            case 0: // No Calibration
                return "direct ADC";
            case 1: // internal Microphone
                return "Internal Mic";
            case 2: // external Input
                return "External In";
            case 3: // external Microphone
                return "External Mic";
        }
        return null;
    }

    public void updateCal() {
        float ofs=0.0f;
        String unit="dBFS";
        String linUnit="FS";
        float linScale=1.0f;
        String note="direct ADC";

        ofs=getCalOfs(calmode);
        linScale=getCalScale(calmode);
        unit=getUnit(calmode);
        linUnit=getLinearUnit(calmode);
        note=getCalNote(calmode);
        spectralView.ofs=ofs;
        spectralView.unit=unit;
        spectralView.note=note;
        levelBar1.ofs=ofs;
        levelBar1.unit = unit;
        levelBar2.ofs = ofs;
        levelBar2.unit=unit;
        sp_plot.ofs=ofs;
        sp_plot.unit=unit;
        xy_plot.displayOfs=ofs;
        xy_plot.unit=unit;
        xy_plot.invalidate();
        rp_view.displayOfs=ofs;
        rp_view.unit=unit;
        rp_view.invalidate();
        rp_wave_view.unit=linUnit;
        rp_wave_view.displayScale=linScale;
        rp_wave_view.invalidate();
        tx_cal.setText(note);
    }

    // Tab Management Help Functions
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
        // TextView tv = (TextView) view.findViewById(R.id.textView);
        // tv.setText(title);
        return view;
    }

    // Create Function (Main initialization)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar=getActionBar();
        if (actionBar != null) actionBar.hide();
        // getActionBar().hide();

        setContentView(R.layout.activity_audio_analyzer);

        // Setup Tabs
        tabHost = (TabHost) findViewById(R.id.tabhost);
        tabHost.setup();
        setNewTab(this, tabHost, "tab_analyze", "FFT", R.mipmap.sink, R.id.analyze);
        setNewTab(this, tabHost, "tab_stripplot","Time",R.mipmap.strip,R.id.stripplot);
        setNewTab(this, tabHost, "tab_generate","Sig",R.mipmap.source,R.id.generate);
        setNewTab(this, tabHost, "tab_sweep","Swp",R.mipmap.sweep,R.id.sweep);
        setNewTab(this, tabHost, "tab_report","Rprt",R.mipmap.report,R.id.report);

        // General GUI
        bn_speaker=(ImageButton) findViewById(R.id.bn_speaker);
        bn_sync=(ImageButton) findViewById(R.id.bn_sync);

        // Analyzer GUI
        bn_play_pause=(ImageButton) findViewById(R.id.bn_play_pause);
        bn_menu=(ImageButton) findViewById(R.id.bn_show_menu);
        bn_cal=(ImageButton) findViewById(R.id.bn_cal);
        bn_spec_mode=(ImageButton) findViewById(R.id.bn_spec_mode);
        bn_zoom_all=(ImageButton) findViewById(R.id.bn_zoom_all);
        tx_status=(TextView) findViewById(R.id.tx_status);
        tx_cal=(TextView) findViewById(R.id.tx_cal);
        waveView=(WaveView) findViewById(R.id.wv_wave);
        spectralView=(SpectralView) findViewById(R.id.vw_spec);

        // StripView GUI
        bn_sp_play_pause=(ImageButton) findViewById(R.id.bn_sp_play_pause);
        bn_sp_cal=(ImageButton) findViewById(R.id.bn_sp_cal);
        bn_sp_plus=(ImageButton) findViewById(R.id.bn_sp_plus);
        bn_sp_minus=(ImageButton) findViewById(R.id.bn_sp_minus);
        bn_sp_style=(ImageButton) findViewById(R.id.bn_sp_style);
        bn_sp_zoom_all=(ImageButton) findViewById(R.id.bn_sp_zoom_all);
        sp_plot=(StripPlotView) findViewById(R.id.sp_plot);

        // Sweep GUI
        bn_xy_cal = (ImageButton) findViewById(R.id.bn_xy_cal);
        bn_xy_style = (ImageButton) findViewById(R.id.bn_xy_style);
        bn_xy_delete = (ImageButton) findViewById(R.id.bn_xy_delete);
        bn_xy_zoom_all = (ImageButton) findViewById(R.id.bn_xy_zoom_all);
        bn_xy_setup = (ImageButton) findViewById(R.id.bn_xy_setup);
        tx_xy_sweep_mode = (TextView) findViewById(R.id.tx_xy_sweep_mode);
        bn_xy_play = (ImageButton) findViewById(R.id.bn_xy_play);
        xy_plot = (SweepPlotView) findViewById(R.id.xy_plot);

        // Report GUI
        bn_rp_cal = (ImageButton) findViewById(R.id.bn_rp_cal);
        bn_rp_save = (ImageButton) findViewById(R.id.bn_rp_save);
        bn_rp_zoom = (ImageButton)findViewById(R.id.bn_rp_zoom);
        lv_rp_items = (ListView) findViewById(R.id.lv_rp_items);
        rp_view = (AudioReportView) findViewById(R.id.xy_rp_plot);
        rp_wave_view = (AudioWaveView) findViewById(R.id.xy_rp_wave);

        // JNI Helper
        audioAnalyzerHelper = new AudioAnalyzerHelper();

        // Load Preferences
        AudioAnalyzerPrefs =  PreferenceManager.getDefaultSharedPreferences(this);

        listener=new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                if (key.equals("DeviceName")) {
                    deviceName=prefs.getString(key,"My Device");
                }
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
        deviceName=AudioAnalyzerPrefs.getString("DeviceName","My Device");

        fftsize = Integer.parseInt(AudioAnalyzerPrefs.getString("FFTSize", "2048"));
        window = Integer.parseInt(AudioAnalyzerPrefs.getString("FFTWindow", "0"));

        mic_in_MO=Float.parseFloat(AudioAnalyzerPrefs.getString("CalMicIn_MO", "-38"));
        mic_in_Vref=Float.parseFloat(AudioAnalyzerPrefs.getString("CalMicIn_V", "10"));
        mic_in_dBFS=Float.parseFloat(AudioAnalyzerPrefs.getString("CalMicIn_S","-20.0"));

        mic_Pref=Float.parseFloat(AudioAnalyzerPrefs.getString("CalMic_P","94"));
        mic_dBFS=Float.parseFloat(AudioAnalyzerPrefs.getString("CalMic_S","-20.0"));

        calmode=AudioAnalyzerPrefs.getInt("CalMode", 0);

        spectralView.setPreferences(this, AudioAnalyzerPrefs);
        waveView.setPreferences(this, AudioAnalyzerPrefs);


        // Assume thisActivity is the current activity
        //int permissionCheck = ContextCompat.checkSelfPermission(thisActivity,
        //        Manifest.permission.RECORD_AUDIO);
        //if (permissionCheck == PackageManager.PERMISSION_DENIED) {

        //}

        int minBuffSize = AudioRecord.getMinBufferSize(fsample, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
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
        levelBar1.setPreferences(this, AudioAnalyzerPrefs, 0);

        levelBar2=(LevelBar)findViewById(R.id.bv_bar2);
        levelBar2.intmode=8;
        levelBar2.setPreferences(this, AudioAnalyzerPrefs, 1);

        updateCal();

        audioSource=new AudioSource(audioAnalyzerHelper);

        dataConsolidator = new DataConsolidator(this);

        processBufferList=new ProcessBufferList(32);

        recms=0;
        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED)
            audioRecord.startRecording();
        isRecording=true;

        generator_on=AudioAnalyzerPrefs.getBoolean("Signal_OnOff",false);
        if (generator_on)
            bn_speaker.setImageResource(R.mipmap.speaker_on);
        else
            bn_speaker.setImageResource(R.mipmap.speaker);
        audioAnalyzerHelper.SignalProg(21,(generator_on)?1.0f:0.0f);

        switch (spectralView.displayType) {
            case SPEK:
                bn_spec_mode.setImageResource(R.mipmap.waterfall_button);
                break;
            case WFALL:
                bn_spec_mode.setImageResource(R.mipmap.terz_button);
                break;
            case TERZ:
                bn_spec_mode.setImageResource(R.mipmap.spectral_button);
                break;
        }

        // StripPlot GUI
        sp_plot.setPreferences(this, AudioAnalyzerPrefs);
        sp_plot.install_handlers(bn_sp_plus, bn_sp_minus, bn_sp_style, bn_sp_zoom_all);

        bn_sp_play_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sp_plot.running) {
                    sp_plot.running=false;
                    bn_sp_play_pause.setImageResource(R.mipmap.play_button);
                } else {
                    sp_plot.running=true;
                    bn_sp_play_pause.setImageResource(R.mipmap.pause_button);
                }
            }
        });

        // Sweep GUI
        xy_plot.setPreferences(this, AudioAnalyzerPrefs);
        xy_plot.install_handlers(bn_xy_delete, bn_xy_style, bn_xy_zoom_all);

        bn_xy_setup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent launchSettings = new Intent(AudioAnalyzer.this, SweepSettings.class);
                startActivity(launchSettings);
            }
        });

        bn_xy_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Sweeping) {
                    Sweeping=false;
                    bn_xy_play.setImageResource(R.mipmap.play_button);
                    audioAnalyzerHelper.SignalProg(25,0);
                } else {
                    if (fftsize != 4096) {
                        AlertDialog alertDialog = new AlertDialog.Builder(AudioAnalyzer.this).create();
                        alertDialog.setTitle("Info");
                        alertDialog.setMessage("FFT Size must be 4096 to use this feature. Set this Value now?");
                        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        SharedPreferences.Editor E = AudioAnalyzerPrefs.edit();
                                        E.putString("FFTSize","4096");
                                        E.apply();
                                        // fftsize=4096;
                                        dialog.dismiss();
                                    }
                                });
                        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "NO",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });

                        alertDialog.show();
                        return;
                    }
                    SweepStep = 0;
                    SweepPhase = 0;
                    Sweeping = true;
                    bn_xy_play.setImageResource(R.mipmap.pause_button);
                    audioAnalyzerHelper.SignalProg(25, 0);
                }
            }
        });

        // Report GUI
        rp_view.setPreferences(this, AudioAnalyzerPrefs);
        rp_wave_view.setPreferences(this, AudioAnalyzerPrefs);
        rp_view.install_handlers(null,bn_rp_zoom);
        rp_view.initListView(lv_rp_items);
        rp_view.initWaveView(rp_wave_view);

        bn_rp_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (reporting) {
                    return;
                }
                reportingctr=0;
                reporting=true;
                reportend=lastserial+16;
                bn_rp_save.setImageResource(R.mipmap.recording);

            }
        });

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
                audioAnalyzerHelper.SignalProg(21, (generator_on) ? 1.0f : 0.0f);
                SharedPreferences.Editor e=AudioAnalyzerPrefs.edit();
                e.putBoolean("Signal_OnOff", generator_on);
                e.apply();
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

        final View.OnClickListener calChanger=new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(AudioAnalyzer.this, v);
                Menu menu = popup.getMenu();
                menu.add(0, 1, Menu.NONE, "No Calibration");
                menu.add(0, 2, Menu.NONE, "Internal Microphone");
                menu.add(0, 3, Menu.NONE, "External Input");
                menu.add(0, 4, Menu.NONE, "External Microphone");
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
        };
        bn_cal.setOnClickListener(calChanger);
        bn_sp_cal.setOnClickListener(calChanger);
        bn_xy_cal.setOnClickListener(calChanger);
        bn_rp_cal.setOnClickListener(calChanger);

        bn_spec_mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (spectralView.displayType) {
                    case SPEK:
                        spectralView.displayType=SpectralView.DisplayType.WFALL;
                        bn_spec_mode.setImageResource(R.mipmap.terz_button);
                        break;
                    case WFALL:
                        spectralView.displayType=SpectralView.DisplayType.TERZ;
                        bn_spec_mode.setImageResource(R.mipmap.spectral_button);
                        break;
                    case TERZ:
                        spectralView.displayType=SpectralView.DisplayType.SPEK;
                        bn_spec_mode.setImageResource(R.mipmap.waterfall_button);
                        break;
                }
                SharedPreferences.Editor E = AudioAnalyzerPrefs.edit();
                E.putInt("DisplayType", spectralView.displayType.ordinal());
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

        rp_view.readPersistantRecords();

        // ////////////////////////////////////////////////////
        // Start the worker Threads
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

        // ////////////////////////////////////////////////////
        // Finally, start the Screen Update Function, triggered every 50 ms to give a smooth picture
        tickctr=0;
        overflow=false;
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

    }

    // Cyclic call within the GUI Thread --> Can modify GUI Elements
    private void tick() {
        // Initaite Application Exit from the Main GUI Thread
        if (quitting) {
            if (isRecording) {
                stopRecording();
                return;
            }
            quittick++; // Leaves Time fro the worker threads to finish gracefully.
            if (quittick > 3)
                finish();
            return;
        }

        if (isRecording) {
            float time=(float)recms/(float)fsample;
            String s=String.format("%4.2f s", time);
            tx_status.setText(s);

            // Manage Out-of-Sync Flag
            if (tickctr >= 10) {
                if (processBufferList.overflow > 0) {
                    if (!overflow)
                        bn_sync.setImageResource(R.mipmap.async);
                    overflow=true;
                } else {
                    if (overflow) {
                        bn_sync.setImageResource(R.mipmap.sync);
                    }
                    overflow=false;
                }
                tickctr=0;
                processBufferList.overflow=0;
            }
            tickctr++;

            // Manage Sweep
            if (SweepRenew) {
                xy_plot.invalidate();
                if (!Sweeping)
                    bn_xy_play.setImageResource(R.mipmap.play_button);
                SweepRenew=false;
            }

            if (audioPlayerStopped) {
                if (rp_view.playingButton != null) {
                    rp_view.playingButton.setImageResource(R.mipmap.play_button);
                }
                audioPlayerStopped=false;
            }

            // Update Display
            if (!pause) {
                if (spectralView != null) spectralView.display();
                if (levelBar1 != null) levelBar1.display();
                if (levelBar2 != null) levelBar2.display();
                if (waveView != null) waveView.display();
            }
            if (sp_plot != null) sp_plot.display();

            // Report
            if (reporting) {
                if (reportingctr==5) {
                    bn_rp_save.setImageResource(R.mipmap.record);
                } else if (reportingctr>=10) {
                    bn_rp_save.setImageResource(R.mipmap.recording);
                    reportingctr=0;
                }
                reportingctr++;
             }
            if (reportRenew) {
                if (!reporting)
                    bn_rp_save.setImageResource(R.mipmap.record);
                reportRenew=false;
                if (report != null) {
                    report.process();
                    rp_view.add(report);
                    report=null;
                }
            }

        } else {
            ((TextView) findViewById(R.id.tx_status)).setText("Offline");
        }
    }

    // Audio Recording Process
    private void recordAudioData() {
        short sData[] = new short[8192];
        int state=audioRecord.getState();

        if (state != AudioRecord.STATE_INITIALIZED) {
            // Just emulate sound source delivering a 10kHz Sine for testing purposes
            while (isRecording) {
                recms += fftsize;
                for (int i=0;i<fftsize;i++) {
                    float t=(float)i/fsample;
                    sData[i]=(short)(32767.0*Math.sin(2*Math.PI*10000*t));
                }
                ProcessResult pr=processBufferList.addslot();
                pr.ReUse(sData,fftsize,fsample,spectralView.trackf,window,terzw);

                processBufferList.notify_new_buffer();
                try { Thread.sleep(fftsize*1000/fsample,0); }
                catch (InterruptedException E ){
                    // Ignore
                }
            }
            return;
        }

        // Regular Working --> Pickup Data and add it to the processBufferList
        while (isRecording) {
            audioRecord.read(sData, 0, fftsize);
            recms += fftsize;
            // Dummy Data for numeric Calibration/verification
            /*for (int i=0;i<fftsize;i++) {
                float t=(float)i/fsample;
                sData[i]=(short)(32767.0*Math.sin(2*Math.PI*10000*t));
            }*/

            // Add to list
            ProcessResult pr=processBufferList.addslot();
            pr.ReUse(sData,fftsize,fsample,spectralView.trackf,window,terzw);

            processBufferList.notify_new_buffer();
        }
    }

    // Audio Processing Process
    private void processAudioData() {
        ProcessResult pr;
        float tracklvl=-120.0f;
        boolean valid=false;

        while (isRecording) {
            while ((pr = processBufferList.retrieve()) != null) {
                if (resetPeak) audioAnalyzerHelper.fftResetPeak();
                resetPeak = false;

                pr.process(audioAnalyzerHelper);
                lastserial=pr.serial;
                tracklvl = pr.trackLevel2; // For the sweep only

                dataConsolidator.add(pr);

                levelBar1.add();
                levelBar2.add();
                waveView.add();
                sp_plot.add();
                dataConsolidator.tick();
                valid = true;

                if (reporting && lastserial>=reportend) {
                    report = new AudioReport(rp_view, rp_wave_view);
                    report.addData(processBufferList.get_old_results(reportend-30,reportend));
                    report.calMode=calmode;
                    if (report.data.size() == 0)
                        report=null;
                    reporting=false;
                    reportRenew=true;
                }
            }
            if (Sweeping && valid) {
                switch (SweepPhase) {
                    case 0: // Turn Sine On
                        if (SweepStep==0) {
                            SweepFreq=xy_plot.getSweepStepFreq(SweepStep);
                            audioAnalyzerHelper.SignalProg(26,(float)Math.pow(10,xy_plot.SweepAmp/20));
                            audioAnalyzerHelper.SignalProg(27,SweepFreq);
                            audioAnalyzerHelper.SignalProg(25,1);
                            Calendar cal = Calendar.getInstance();
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            SweepPlot=xy_plot.addPlot(sdf.format(cal.getTime()));
                        } else {
                            SweepFreq=xy_plot.getSweepStepFreq(SweepStep);
                            audioAnalyzerHelper.SignalProg(27,SweepFreq);
                        }
                        SweepPhase++;
                        break;
                    case 1: // delay Phase
                        SweepPhase++;
                        break;
                    case 2: // delay Phase
                        SweepPhase++;
                        break;
                    case 3: // delay Phase
                        SweepPhase++;
                        break;
                    case 4: // delay Phase
                        SweepPhase++;
                        break;
                    case 5: // Measurment phase
                        SweepPhase++;
                        break;
                    case 6: // Get Data
                        SweepPlot.add(SweepFreq,tracklvl);
                        //SweepPlot.add(SweepFreq,-10);
                        SweepPhase=0;
                        SweepStep++;
                        if (SweepStep >= xy_plot.SweepSteps) {
                            SweepStep = 0;
                            Sweeping = false;
                            audioAnalyzerHelper.SignalProg(25,0);
                            // bn_xy_play.setImageResource(R.mipmap.play_button);
                        }
                        SweepRenew=true;
                        break;
                }
            }
            // Wait for next event
            processBufferList.doWait();
        }
    }

    // Audio playing Process (Signal Generator)
    private void playAudioData() {
        // return;
        int bufsize=AudioTrack.getMinBufferSize(44100,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);
        short[] data1 = new short[bufsize/4];
        boolean playstarted=false;

        audioTrack =new AudioTrack(AudioManager.STREAM_MUSIC,
                44100, AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT,
                bufsize,AudioTrack.MODE_STREAM);

        audioTrack.play();

        while (isRecording && (audioTrack != null)) {
            if (audioPlayer != null) {
                if (!audioPlayer.getBlock(data1)) {
                    audioPlayer=null;
                    audioPlayerStopped=true;
                }
            } else
                audioSource.getData(data1);
            audioTrack.write(data1, 0, data1.length);
        }
    }

    private void stopRecording() {
        // stops the recording activity
        if (null != audioRecord) {
            isRecording = false;
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED)
                audioRecord.stop();
            if (audioTrack != null) audioTrack.stop();
            recordingThread = null;
            processBufferList.clear();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            quitting=true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_audio_analyzer, menu);
        return true;
    }

    public String getVersion() {
        String version="undefined";
        try {
            PackageManager manager = this.getPackageManager();
            PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
            version = info.versionName;
        } catch (PackageManager.NameNotFoundException E) {
            // Ignore
        }
        return version;
    }

    void showAboutWin() {
        View messageView = getLayoutInflater().inflate(R.layout.aboutwin,null,false);
        TextView tv=(TextView) messageView.findViewById(R.id.tv_about_version);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setTitle("AudioAnalyzer");
        tv.setText(getVersion());
        builder.setView(messageView);
        builder.create();
        builder.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent launchSettings = new Intent(this,AudioAnalyzerSettings.class);
            startActivity(launchSettings);
            return true;
        }
        if (id == R.id.action_about) {
            showAboutWin();
            return true;
        }
        if (id == R.id.action_quit) {
            quitting=true;
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

        if (id == R.id.action_settings) {
            Intent launchSettings = new Intent(this,AudioAnalyzerSettings.class);
            startActivity(launchSettings);
            return true;
        }
        if (id == R.id.action_about) {
            showAboutWin();
            return true;
        }
        if (id == R.id.action_quit) {
            quitting=true;
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
