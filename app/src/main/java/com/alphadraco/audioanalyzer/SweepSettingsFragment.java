package com.alphadraco.audioanalyzer;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by aladin on 27.02.2016.
 */

public class SweepSettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.sweepsetup);
    }

}

