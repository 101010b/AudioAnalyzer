package com.alphadraco.audioanalyzer;

import android.app.Activity;
import android.os.Bundle;


/**
 * Created by aladin on 27.09.2015.
 */
public class AudioAnalyzerSettings extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new AudioAnalyzerSettingsFragment())
                .commit();
    }


}
