package com.alphadraco.audioanalyzer;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Created by aladin on 04.10.2015.
 */
public class ADEditTextPreference extends android.preference.EditTextPreference {
    public ADEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ADEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ADEditTextPreference(Context context) {
        super(context);
    }

    @Override
    public CharSequence getSummary() {
        String summary = super.getSummary().toString();
        return String.format(summary, getText());
    }
}
