package com.wmods.wppenhacer.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.wmods.wppenhacer.R;
import com.wmods.wppenhacer.xposed.core.LSPatchCompat;

/**
 * Dialog that shows detailed information about LSPatch compatibility and limitations
 */
public class LSPatchInfoDialog extends DialogFragment {
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Context context = requireContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_lspatch_info, null);
        
        setupDialogContent(view, context);
        
        return new AlertDialog.Builder(context)
                .setTitle(R.string.lspatch_environment_detected)
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }
    
    private void setupDialogContent(View view, Context context) {
        TextView modeText = view.findViewById(R.id.lspatch_mode);
        TextView featuresText = view.findViewById(R.id.lspatch_features);
        TextView limitationsText = view.findViewById(R.id.lspatch_limitations);
        
        try {
            LSPatchCompat.init();
            
            if (LSPatchCompat.isLSPatchEnvironment()) {
                LSPatchCompat.LSPatchMode mode = LSPatchCompat.getCurrentMode();
                
                // Set mode information
                String modeInfo = getModeDescription(mode, context);
                modeText.setText(modeInfo);
                
                // Set available features
                String availableFeatures = getAvailableFeatures(mode, context);
                featuresText.setText(availableFeatures);
                
                // Set limitations
                String limitations = getLimitations(mode, context);
                limitationsText.setText(limitations);
                
            } else {
                // Classic Xposed
                modeText.setText(context.getString(R.string.classic_xposed_detected));
                featuresText.setText("• All WaEnhancer features available\n• Full Xposed API access\n• System server hooks supported");
                limitationsText.setText("No limitations");
            }
            
        } catch (Exception e) {
            modeText.setText("Environment detection failed");
            featuresText.setText("Unable to determine available features");
            limitationsText.setText("Unknown limitations");
        }
    }
    
    private String getModeDescription(LSPatchCompat.LSPatchMode mode, Context context) {
        switch (mode) {
            case LSPATCH_EMBEDDED:
                return "LSPatch Embedded Mode\n\nThe module is embedded directly into the WhatsApp APK. This provides better performance and stability compared to manager mode.";
            case LSPATCH_MANAGER:
                return "LSPatch Manager Mode\n\nThe module is loaded through the LSPatch manager app. This mode has more limitations but allows easier module management.";
            case CLASSIC_XPOSED:
                return context.getString(R.string.classic_xposed_detected);
            default:
                return "Unknown LSPatch mode detected";
        }
    }
    
    private String getAvailableFeatures(LSPatchCompat.LSPatchMode mode, Context context) {
        StringBuilder features = new StringBuilder();
        features.append("✓ App-level hooks\n");
        features.append("✓ UI modifications\n");
        features.append("✓ Message interception\n");
        features.append("✓ Media download features\n");
        features.append("✓ Privacy features\n");
        features.append("✓ Customization options\n");
        
        if (mode != LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
            features.append("✓ Resource modifications\n");
            features.append("✓ Theme customization\n");
        }
        
        return features.toString();
    }
    
    private String getLimitations(LSPatchCompat.LSPatchMode mode, Context context) {
        StringBuilder limitations = new StringBuilder();
        
        // Common LSPatch limitations
        limitations.append("✗ System server hooks disabled\n");
        limitations.append("✗ Bootloader spoofer unavailable\n");
        limitations.append("✗ Some anti-detection features limited\n");
        limitations.append("✗ Deep system modifications blocked\n");
        
        if (mode == LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
            limitations.append("✗ Resource hooks limited\n");
            limitations.append("✗ Theme customization reduced\n");
            limitations.append("✗ Performance overhead\n");
        }
        
        limitations.append("\nNote: These limitations are due to LSPatch's rootless architecture and are intended to maintain system security.");
        
        return limitations.toString();
    }
    
    public static LSPatchInfoDialog newInstance() {
        return new LSPatchInfoDialog();
    }
}
