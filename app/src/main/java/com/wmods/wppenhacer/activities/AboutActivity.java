package com.wmods.wppenhacer.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.wmods.wppenhacer.R;
import com.wmods.wppenhacer.activities.base.BaseActivity;
import com.wmods.wppenhacer.databinding.ActivityAboutBinding;
import com.wmods.wppenhacer.xposed.core.LSPatchCompat;

public class AboutActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityAboutBinding binding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Setup button listeners
        binding.btnTelegram.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://t.me/waenhancer"));
            startActivity(intent);
        });
        
        binding.btnGithub.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://github.com/Dev4Mod/waenhancer"));
            startActivity(intent);
        });
        
        binding.btnDonate.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://coindrop.to/dev4mod"));
            startActivity(intent);
        });

        // Setup compatibility status
        setupCompatibilityStatus(binding);
    }
    
    private void setupCompatibilityStatus(ActivityAboutBinding binding) {
        TextView frameworkStatus = binding.tvFrameworkStatus;
        TextView modeStatus = binding.tvModeStatus;
        TextView featuresStatus = binding.tvFeaturesStatus;
        TextView limitationsStatus = binding.tvLimitationsStatus;
        
        try {
            // Check if LSPatch is detected
            boolean isLSPatch = LSPatchCompat.isLSPatchEnvironment();
            
            if (isLSPatch) {
                frameworkStatus.setText("Framework: LSPatch (Rootless)");
                frameworkStatus.setTextColor(ContextCompat.getColor(this, R.color.green));
                
                LSPatchCompat.LSPatchMode mode = LSPatchCompat.getCurrentMode();
                String modeText = "Mode: ";
                switch (mode) {
                    case LSPATCH_EMBEDDED:
                        modeText += "Embedded (Modules in APK)";
                        modeStatus.setTextColor(ContextCompat.getColor(this, R.color.green));
                        break;
                    case LSPATCH_MANAGER:
                        modeText += "Manager (External Manager)";
                        modeStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_light));
                        break;
                    default:
                        modeText += "Unknown";
                        modeStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light));
                        break;
                }
                modeStatus.setText(modeText);
                
                // Check feature availability
                boolean resourceHooks = LSPatchCompat.isFeatureAvailable("RESOURCE_HOOKS");
                boolean systemHooks = LSPatchCompat.isFeatureAvailable("SYSTEM_SERVER_HOOKS");
                
                String featuresText = "Features: ";
                if (resourceHooks && systemHooks) {
                    featuresText += "Full compatibility";
                    featuresStatus.setTextColor(ContextCompat.getColor(this, R.color.green));
                } else if (resourceHooks) {
                    featuresText += "Most features available";
                    featuresStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_light));
                } else {
                    featuresText += "Limited features";
                    featuresStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light));
                }
                featuresStatus.setText(featuresText);
                
                // Show limitations
                String limitationsText = "Limitations: ";
                if (mode == LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
                    limitationsText += "Some resource modifications limited";
                } else if (!systemHooks) {
                    limitationsText += "System server hooks unavailable";
                } else {
                    limitationsText += "None detected";
                }
                limitationsStatus.setText(limitationsText);
                
            } else {
                frameworkStatus.setText("Framework: LSPosed (Root)");
                frameworkStatus.setTextColor(ContextCompat.getColor(this, R.color.blue));
                
                modeStatus.setText("Mode: Classic Xposed");
                modeStatus.setTextColor(ContextCompat.getColor(this, R.color.blue));
                
                featuresStatus.setText("Features: Full compatibility");
                featuresStatus.setTextColor(ContextCompat.getColor(this, R.color.green));
                
                limitationsStatus.setText("Limitations: None");
                limitationsStatus.setTextColor(ContextCompat.getColor(this, R.color.green));
            }
            
        } catch (Exception e) {
            frameworkStatus.setText("Framework: Error detecting (" + e.getMessage() + ")");
            frameworkStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light));
            
            modeStatus.setText("Mode: Unknown");
            modeStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light));
            
            featuresStatus.setText("Features: Unknown");
            featuresStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light));
            
            limitationsStatus.setText("Limitations: Cannot determine");
            limitationsStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light));
        }
    }
}
