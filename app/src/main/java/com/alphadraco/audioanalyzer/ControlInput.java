package com.alphadraco.audioanalyzer;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TableRow;

import com.alphadraco.audioanalyzer.AudioAnalyzer;
import com.alphadraco.audioanalyzer.AudioSource;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by aladin on 22.10.2015.
 */

public class ControlInput {
    public enum FieldType { IN_SWITCH, IN_NUMERIC, IN_CHECK, IN_SPIN, IN_BUTTON };

    private NumberFormat nf;

    private FieldType fieldType;
    private Switch iSwitch;
    private EditText iEditText;
    private CheckBox iCheckBox;
    private Spinner iSpinner;
    private Button iButton;

    private int normBkg;
    private int highlightBkg;
    private TableRow tableRow;
    private SeekBar quickSeekBar;
    private ArrayList<ControlInput> CtrlList;

    private AudioAnalyzer root;

    private String name;
    private int tgtID;
    private float fmin, fmax;
    private boolean noupdate;

    private boolean bool_val;
    private float float_val;
    private float float_val_save;
    private int sel_val;
    private int sel_max;
    private boolean is_db_float;
    private float float_scale;
    private String float_format;





    public void init(AudioAnalyzer aa, int rowid, int seekid, int id, String nme, final int tgt, boolean bool_def,  float float_def, float min, float max, boolean db_float, float float_sc,
                     String float_fmt,
                     ArrayList<String> sl, int spin_def) {
        root=aa;
        tgtID=tgt;
        name=nme;

        if ((rowid > 0) && (seekid > 0)) {
            tableRow=(TableRow) root.findViewById(rowid);
            quickSeekBar=(SeekBar) root.findViewById(seekid);
            quickSeekBar.setMax(1000);
            CtrlList=root.fgen_list;
            normBkg=Color.TRANSPARENT;
            highlightBkg=Color.argb(255, 0, 30, 0);
        }

        View v=root.findViewById(id);
        if (v instanceof Switch) {
            fieldType=FieldType.IN_SWITCH;
            iSwitch=(Switch) v;
            bool_val=root.AudioAnalyzerPrefs.getBoolean(name,bool_def);
            root.audioAnalyzerHelper.SignalProg(tgtID,(bool_val)?1.0f:0.0f);
            iSwitch.setChecked(bool_val);
            iSwitch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean n = iSwitch.isChecked();
                    if (n != bool_val) {
                        bool_val = n;
                        SharedPreferences.Editor e = root.AudioAnalyzerPrefs.edit();
                        e.putBoolean(name, bool_val);
                        e.apply();
                        root.audioAnalyzerHelper.SignalProg(tgtID, (bool_val) ? 1.0f : 0.0f);
                    }
                }
            });
            iSwitch.setTextAppearance(root,R.style.SwitchTextAppearance);
        } else if (v instanceof EditText) {
            nf=NumberFormat.getInstance();
            nf.setMaximumFractionDigits(32);
            nf.setMinimumFractionDigits(0);
            nf.setParseIntegerOnly(false);
            nf.setRoundingMode(RoundingMode.HALF_UP);

            fieldType=FieldType.IN_NUMERIC;
            iEditText=(EditText)v;
            fmin=min;
            fmax=max;
            float_val=root.AudioAnalyzerPrefs.getFloat(name,float_def);
            if (float_val < fmin) float_val=fmin;
            if (float_val > fmax) float_val=fmax;
            is_db_float=db_float;
            float_scale=float_sc;
            float_format=float_fmt;
            float_val_save=float_val;
            noupdate=false;
            if (is_db_float)
                root.audioAnalyzerHelper.SignalProg(tgtID,(float)Math.pow(10.0f,float_val/20.0f));
            else
                root.audioAnalyzerHelper.SignalProg(tgtID,float_val*float_scale);
            iEditText.setText(String.format(float_format,float_val));
            if (tableRow != null) {
                iEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (hasFocus) {
                            tableRow.setBackgroundColor(highlightBkg);
                            for (int i=0;i<CtrlList.size();i++) {
                                if ((CtrlList.get(i) != ControlInput.this) && (CtrlList.get(i).tableRow!=null))
                                    CtrlList.get(i).tableRow.setBackgroundColor(normBkg);
                            }
                            quickSeekBar.setOnSeekBarChangeListener(null);
                            quickSeekBar.setProgress(500);
                            quickSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                @Override
                                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                    float pval=(float)(progress-500)/500.0f;
                                    float f;
                                    if (is_db_float || (fmin == 0.0f)) {
                                        float fullscale=fmax-fmin;
                                        f=float_val_save+pval*fullscale*0.3f;
                                    } else {
                                        float fullscale=(float)Math.log(fmax / fmin);
                                        f=(float)Math.exp(Math.log(float_val_save)+pval*fullscale*0.3f);
                                    }
                                    if (f < fmin) f=fmin;
                                    if (f > fmax) f=fmax;
                                    float_val=f;
                                    noupdate=true;
                                    iEditText.setText(String.format(float_format,f));
                                    noupdate=false;

                                    SharedPreferences.Editor e = root.AudioAnalyzerPrefs.edit();
                                    e.putFloat(name, float_val);
                                    e.apply();
                                    if (is_db_float)
                                        root.audioAnalyzerHelper.SignalProg(tgtID, (float) Math.pow(10.0f, float_val / 20.0f));
                                    else
                                        root.audioAnalyzerHelper.SignalProg(tgtID, float_val * float_scale);

                                }

                                @Override
                                public void onStartTrackingTouch(SeekBar seekBar) {
                                    float_val_save=float_val;
                                    InputMethodManager imm = (InputMethodManager)root.getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                                    imm.hideSoftInputFromWindow(iEditText.getWindowToken(), 0);
                                }

                                @Override
                                public void onStopTrackingTouch(SeekBar seekBar) {
                                    float_val_save=float_val;
                                    seekBar.setProgress(500);
                                }
                            });
                        }
                    }
                });
            }
            iEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (noupdate) return;
                    String st = iEditText.getText().toString();
                    float f;
                    try {
                        // f = Float.parseFloat(st);
                        f=nf.parse(st).floatValue();
                    } catch (ParseException ex) {
                        iEditText.setTextColor(Color.RED);
                        return;
                    }
                    if ((f < fmin) || (f > fmax)) {
                        iEditText.setTextColor(Color.RED);
                        return;
                    } else
                        iEditText.setTextColor(0xFF00FF00);
                    if (f != float_val) {
                        float_val = f;
                        SharedPreferences.Editor e = root.AudioAnalyzerPrefs.edit();
                        e.putFloat(name, float_val);
                        e.apply();
                        if (is_db_float)
                            root.audioAnalyzerHelper.SignalProg(tgtID, (float) Math.pow(10.0f, float_val / 20.0f));
                        else
                            root.audioAnalyzerHelper.SignalProg(tgtID, float_val * float_scale);
                    }
                }
            });
        } else if (v instanceof CheckBox) {
            fieldType=FieldType.IN_CHECK;
            iCheckBox=(CheckBox)v;
            bool_val=root.AudioAnalyzerPrefs.getBoolean(name,bool_def);
            iCheckBox.setChecked(bool_val);
            root.audioAnalyzerHelper.SignalProg(tgtID, (bool_val) ? 1.0f : 0.0f);
            iCheckBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean n = iCheckBox.isChecked();
                    if (n != bool_val) {
                        bool_val = n;
                        SharedPreferences.Editor e = root.AudioAnalyzerPrefs.edit();
                        e.putBoolean(name, bool_val);
                        e.apply();
                        root.audioAnalyzerHelper.SignalProg(tgtID, (bool_val) ? 1.0f : 0.0f);
                    }
                }
            });
        } else if (v instanceof Spinner) {
            fieldType=FieldType.IN_SPIN;
            iSpinner=(Spinner)v;
            //ArrayAdapter<String> adapter = new ArrayAdapter<String>(root,
            //        android.R.layout.simple_spinner_item,sl);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(root,
                R.layout.asimple_spinner_item,sl);
            // adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            adapter.setDropDownViewResource(R.layout.asimple_spinner_list_item);
            iSpinner.setAdapter(adapter);
            sel_val=root.AudioAnalyzerPrefs.getInt(name,spin_def);
            sel_max=sl.size()-1;
            if (sel_val < 0) sel_val=0;
            if (sel_val > sel_max) sel_val=0;
            iSpinner.setSelection(sel_val);
            root.audioAnalyzerHelper.SignalProg(tgtID, (float) sel_val);
            iSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position < 0) position=0;
                    if (position > sel_max) position=sel_max;
                    if (position != sel_val) {
                        sel_val=position;
                        SharedPreferences.Editor e = root.AudioAnalyzerPrefs.edit();
                        e.putInt(name,sel_val);
                        e.apply();
                        root.audioAnalyzerHelper.SignalProg(tgtID,(float)sel_val);
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        } else if (v instanceof Button) {
            fieldType=FieldType.IN_BUTTON;
            iButton=(Button)v;
            iButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    root.audioAnalyzerHelper.SignalProg(tgtID,1.0f);
                }
            });
        }
    }

    // Button (trigger only)
    public ControlInput(AudioAnalyzer aa, int id, int tgt) {
        init(aa,0,0,id,null,tgt,false,0.0f,0.0f,1.0f,false,1.0f,null,null,0);
    }

    // CheckBox, Swicth
    public ControlInput(AudioAnalyzer aa, int id, String nme, int tgt, boolean def) {
        init(aa,0,0,id,nme,tgt,def,0.0f,0.0f,1.0f,false,1.0f,null,null,0);
    }

    // EditText
    public ControlInput(AudioAnalyzer aa, int rowid, int seekid, int id, String nme, int tgt, float min, float max, float def, String fmt) {
        init(aa,rowid,seekid,id,nme,tgt,false,def,min,max,false,1.0f,fmt,null,0);
    }

    // EditText with db
    public ControlInput(AudioAnalyzer aa, int rowid, int seekid, int id, String nme, int tgt, float min, float max, float def, String fmt, boolean db_float) {
        init(aa,rowid,seekid,id,nme,tgt,false,def,min,max,db_float,1.0f,fmt,null,0);
    }

    // EditText with scale
    public ControlInput(AudioAnalyzer aa, int rowid, int seekid, int id, String nme, int tgt, float min, float max, float def, String fmt, float float_scl) {
        init(aa,rowid,seekid,id,nme,tgt,false,def,min,max,false,float_scl,fmt,null,0);
    }

    // EditText with scale and db
    public ControlInput(AudioAnalyzer aa, int rowid, int seekid, int id, String nme, int tgt, float min, float max, float def, String fmt, float float_scl, boolean db_float) {
        init(aa,rowid,seekid,id,nme,tgt,false,def,min,max,db_float,float_scl,fmt,null,0);
    }

    // Spinner
    public ControlInput(AudioAnalyzer aa, int id, String nme, int tgt, ArrayList<String> content, int def) {
        init(aa,0,0,id,nme,tgt,false,0.0f,0.0f,1.0f,false,1.0f,null,content,def);
    }


}
