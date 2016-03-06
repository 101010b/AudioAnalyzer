package com.alphadraco.audioanalyzer;

/**
 * Created by aladin on 27.02.2016.
 */

import android.app.Activity;
import android.os.Bundle;


/**
 * Created by aladin on 27.09.2015.
 */
public class SweepSettings extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SweepSettingsFragment())
                .commit();
    }


}


