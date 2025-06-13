package com.wmods.wppenhacer.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.wmods.wppenhacer.R;
import com.wmods.wppenhacer.xposed.core.LSPatchCompat;

/**
 * Preference that shows LSPatch compatibility information
 */
public class LSPatchInfoPreference extends Preference {

    public LSPatchInfoPreference(@NonNull Context context) {
        super(context);
        init();
    }

    public LSPatchInfoPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LSPatchInfoPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayoutResource(R.layout.preference_lspatch_info);
        updateInfo();
        
        setOnPreferenceClickListener(preference -> {
            showDetailedInfo();
            return true;
        });
    }

    private void updateInfo() {
        // This would normally check if we're in Xposed environment
        // For now, we'll show information about LSPatch compatibility
        if (isLSPatchEnvironmentDetected()) {
            setTitle("LSPatch Environment Detected");
            setSummary("Running in LSPatch mode with compatibility layer");
            setIcon(R.drawable.ic_general);
        } else {
            setTitle("Classic Xposed Environment");
            setSummary("Running in traditional Xposed mode");
            setIcon(R.drawable.ic_general);
        }
    }

    private boolean isLSPatchEnvironmentDetected() {
        // Check for LSPatch-specific system properties or classes
        try {
            return Class.forName("org.lsposed.lspatch.loader.LSPApplication") != null;
        } catch (ClassNotFoundException e) {
            try {
                return Class.forName("org.lsposed.lspatch.metaloader.LSPAppComponentFactoryStub") != null;
            } catch (ClassNotFoundException ex) {
                return false;
            }
        }
    }

    private void showDetailedInfo() {
        Context context = getContext();
        StringBuilder info = new StringBuilder();
        
        info.append("LSPatch Compatibility Information:\n\n");
        
        if (isLSPatchEnvironmentDetected()) {
            info.append("• Environment: LSPatch\n");
            info.append("• Compatible: Yes\n");
            info.append("• Resource Hooks: Supported\n");
            info.append("• System Server Hooks: Not Supported\n");
            info.append("• Bridge Service: Available\n");
            info.append("• Signature Bypass: Enabled\n\n");
            info.append("Note: Some features may have limited functionality in LSPatch mode.");
        } else {
            info.append("• Environment: Classic Xposed\n");
            info.append("• Compatible: Full Support\n");
            info.append("• Resource Hooks: Supported\n");
            info.append("• System Server Hooks: Supported\n");
            info.append("• All Features: Available\n");
        }

        new androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("LSPatch Compatibility")
            .setMessage(info.toString())
            .setPositiveButton("OK", null)
            .show();
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        // Update info each time the view is bound
        updateInfo();
    }
}
