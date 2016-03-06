package com.alphadraco.audioanalyzer;

/**
 * Created by aladin on 28.02.2016.
 */

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Environment;
import android.text.Html;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

public class AudioReportView extends XYPlot {

    public ArrayList<AudioReport> reports;
    public ArrayList<View> ListEntries;
    public ListView lv;
    public AudioWaveView wv;

    private class ReportAdaptor extends ArrayAdapter<AudioReport> {
        private final Context context;
        private ArrayList<AudioReport> ar;
        public ReportAdaptor(Context _context, ArrayList<AudioReport> objects) {
            super(_context,android.R.layout.simple_list_item_1,objects);
            ar = objects;
            context=_context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.audio_report_item, parent, false);
            TextView header=(TextView)rowView.findViewById(R.id.tx_rp_header);
            TextView desc=(TextView)rowView.findViewById(R.id.tx_rp_desc);
            CheckBox shown=(CheckBox)rowView.findViewById(R.id.cb_rp_shown);
            header.setText(ar.get(position).name);
            header.setTextColor(lines.get(position).color);
            while (ListEntries.size()<position+1)
                ListEntries.add(null);
            ListEntries.set(position,rowView);

            desc.setText(ar.get(position).description());
            if (ar.get(position).specPlot.hidden)
                shown.setChecked(false);
            else
                shown.setChecked(true);
            final AudioReport arpointer=ar.get(position);
            shown.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked)
                        arpointer.specPlot.hidden=false;
                    else
                        arpointer.specPlot.hidden=true;
                    root.rp_view.display();
                }
            });
            return rowView;
        }
    }

    public void setPreferences(AudioAnalyzer _root, SharedPreferences prefs) {
        super.setPreferences(_root,prefs,"Report");
        if (sharedPreferences != null) {
            /*try {
                SweepFreqStart=Float.parseFloat(sharedPreferences.getString("SweepFreqStart", "100.0"));
            } catch (Exception E) {
                SweepFreqStart = 100.0f;
            }
            try {
                SweepFreqStop=Float.parseFloat(sharedPreferences.getString("SweepFreqStop", "20.0"))*1000;
            } catch (Exception E) {
                SweepFreqStop = 20000.0f;
            }
            try {
                SweepSteps=Integer.parseInt(sharedPreferences.getString("SweepSteps", "60"));
            } catch (Exception E) {
                SweepSteps = 60;
            }
            try {
                SweepAmp=Float.parseFloat(sharedPreferences.getString("SweepAmp", "-6.0"));
            } catch (Exception E) {
                SweepAmp = -6.0f;
            }
            SweepLog=sharedPreferences.getBoolean("SweepLog", true);
            setSweepText();

            if (PrefListener != null)
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(PrefListener);
            PrefListener=new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    if (key.equals("SweepFreqStart")) {
                        try {
                            SweepFreqStart = Float.parseFloat(sharedPreferences.getString("SweepFreqStart", "100.0"));
                        } catch (Exception E) {
                            SweepFreqStart = 100;
                        }
                        if (SweepFreqStart < 10) SweepFreqStart=10;
                        if (SweepFreqStart > 22000) SweepFreqStart=22000;
                        setSweepText();
                    }
                    if (key.equals("SweepFreqStop")) {
                        try {
                            SweepFreqStop=Float.parseFloat(sharedPreferences.getString("SweepFreqStop", "20.0"))*1000;
                        } catch (Exception E) {
                            SweepFreqStop = 20000;
                        }
                        if (SweepFreqStop < 10) SweepFreqStop=10;
                        if (SweepFreqStop > 22000) SweepFreqStop=22000;
                        setSweepText();
                    }
                    if (key.equals("SweepLog")) {
                        SweepLog=sharedPreferences.getBoolean("SweepLog", true);
                        setSweepText();
                    }
                    if (key.equals("SweepSteps")) {
                        try {
                            SweepSteps=Integer.parseInt(sharedPreferences.getString("SweepSteps", "60"));
                        } catch (Exception E) {
                            SweepSteps = 60;
                        }
                        if (SweepSteps < 2) SweepFreqStop=2;
                        if (SweepSteps > 200) SweepFreqStop=200;
                        setSweepText();
                    }
                    if (key.equals("SweepAmp")) {
                        try {
                            SweepAmp=Float.parseFloat(sharedPreferences.getString("SweepAmp", "-6.0"));
                        } catch (Exception E) {
                            SweepAmp = -6.0f;
                        }
                        if (SweepAmp < -100) SweepAmp=-100.0f;
                        if (SweepAmp > 0) SweepAmp=0.0f;
                        setSweepText();
                    }
                }
            };
            sharedPreferences.registerOnSharedPreferenceChangeListener(PrefListener);
            */
        }
    }

    protected void setup(Context context) {
        super.setup(context);
        reports=new ArrayList<AudioReport>();
        ListEntries=new ArrayList<View>();
    }

    public AudioReportView(Context context) {
        super(context);
        setup(context);
    }

    public AudioReportView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context);
    }

    public void install_handlers(final ImageButton ib_edit, ImageButton ib_zoom) {
        if (ib_zoom != null) ib_zoom.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                autozoom();
                wv.autozoom();
            }
        });

        if (ib_edit != null) ib_edit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                colorMenu(v);
            }
        });
    }

    public void add(AudioReport ar) {
        reports.add(ar);
        addPlot(ar.specPlot);
        wv.addPlot(ar.wavePlot);
        wv.displayOne=wv.lines.size()-1;
        display();
        wv.display();

        ((BaseAdapter) lv.getAdapter()).notifyDataSetChanged();
    }

    public void initWaveView(AudioWaveView _wv) {
        wv=_wv;
        wv.audioReportView=this;
    }

    // Rename a Report
    public void renameReport(int idx) {
        if ((idx < 0) || (idx >= reports.size())) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(root);
        LayoutInflater inflater=root.getLayoutInflater();
        View sv=inflater.inflate(R.layout.rename_wave_win, null);
        final EditText txe=(EditText) sv.findViewById(R.id.et_name);
        txe.setText(reports.get(idx).name);

        ((TextView)sv.findViewById(R.id.tv_rename_date)).setText(reports.get(idx).dateString());
        ((TextView)sv.findViewById(R.id.tv_rename_length)).setText(reports.get(idx).lengthString());

        builder.setView(sv);
        final int saveid=idx;
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName=txe.getText().toString();
                reports.get(saveid).setName(newName);
                ((TextView)ListEntries.get(saveid).findViewById(R.id.tx_rp_header)).setText(newName);
                ListEntries.get(saveid).invalidate();
                invalidate();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        AlertDialog dialog=builder.create();
        dialog.show();
    }

    public void alertwin(String title, String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(root).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    // Check if external storage is writable
    // Taken from http://developer.android.com/training/basics/data-storage/files.html
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }


    public void saveReport(int idx) {
        if ((idx < 0) || (idx >= reports.size())) return;
        AudioReport r=reports.get(idx);
        if (r == null) return;
        if (r.name.length() < 1) {
            alertwin("Error","Name Not Set or empty");
            return;
        }
        String basename=r.name;
        File basedir= Environment.getExternalStoragePublicDirectory("AudioAnalyzer");
        basedir.mkdirs();

        File file=new File(basedir,basename + File.separator);
        if (file.exists()) {
            alertwin("Error","A File with the name " + basename + " exists already.");
            return;
        }

        file.mkdir();
        if (!file.isDirectory()) {
            alertwin("Error","Cannot create directory " + basename + " to hold the data to be saved.");
            return;
        }
        // Go and save the data
        if (r.saveFiles(root,file)) {
            alertwin("Info","Files Stored in Directory AudioAnalyzer/" + basename +" successfully.");
        } else {
            alertwin("Error","File write Error Directory AudioAnalyzer/" + basename +".");
        }
    }

    public void initListView(ListView _lv) {
        lv=_lv;
        lv.setAdapter(new ReportAdaptor(root,reports));
        // root.registerForContextMenu(lv);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                wv.displayOne=position;
                wv.invalidate();
            }
        });
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                PopupMenu popup = new PopupMenu(root, view);
                Menu menu = popup.getMenu();
                menu.add(position,1,Menu.NONE,"Rename");
                menu.add(position,2,Menu.NONE,"Recolor");
                menu.add(position,3,Menu.NONE,"Delete");
                menu.add(position,4,Menu.NONE,"Save");
                final View vsave = view;
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int pos=item.getGroupId();
                        if ((pos < 0) || (pos >= reports.size())) return false;
                        switch(item.getItemId()) {
                            case 1: // Rename
                                renameReport(item.getGroupId());
                                break;
                            case 2: // Recolor
                                PopupMenu submenu=new PopupMenu(root, vsave);
                                Menu smenu = submenu.getMenu();
                                for (int j=0;j<getColorNum();j++)
                                    smenu.add(pos,j+1,Menu.NONE, getColoredColorName(j));
                                submenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(MenuItem item) {
                                        int i = item.getGroupId();
                                        int tgtcol=getColor(item.getItemId()-1);
                                        lines.get(i).color=tgtcol;
                                        wv.lines.get(i).color=tgtcol;
                                        invalidate();
                                        wv.invalidate();
                                        TextView v=(TextView)ListEntries.get(i).findViewById(R.id.tx_rp_header);
                                        v.setTextColor(tgtcol);
                                        v.invalidate();
                                        return false;
                                    }
                                });
                                submenu.show();
                                break;
                            case 3: // Delete
                                int idx=item.getGroupId();
                                reports.remove(idx);
                                ListEntries.remove(idx);
                                lines.remove(idx);
                                wv.lines.remove(idx);
                                if (wv.displayOne > idx) wv.displayOne--;
                                ((BaseAdapter) lv.getAdapter()).notifyDataSetChanged();
                                display();
                                wv.display();
                                break;
                            case 4: // Save
                                int idxx=item.getGroupId();
                                saveReport(idxx);
                                break;
                        }
                        return false;
                    }
                });
                popup.show();
                return false;
            }
        });
    }

}
