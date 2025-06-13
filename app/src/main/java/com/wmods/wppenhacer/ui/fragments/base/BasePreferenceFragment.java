package com.wmods.wppenhacer.ui.fragments.base;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.wmods.wppenhacer.App;
import com.wmods.wppenhacer.BuildConfig;
import com.wmods.wppenhacer.preference.LSPatchPreferenceManager;
import com.wmods.wppenhacer.xposed.core.LSPatchCompat;
import com.wmods.wppenhacer.xposed.utils.Utils;

import java.util.Objects;

import rikka.material.preference.MaterialSwitchPreference;

public abstract class BasePreferenceFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    protected SharedPreferences mPrefs;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                } else {
                    requireActivity().finish();
                }
            }
        });
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        
        // Apply LSPatch compatibility filtering after preferences are loaded
        applyLSPatchFiltering();
        
        chanceStates(null);
        monitorPreference();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String s) {
        Intent intent = new Intent(BuildConfig.APPLICATION_ID + ".MANUAL_RESTART");
        App.getInstance().sendBroadcast(intent);
        chanceStates(s);
    }

    private void setPreferenceState(String key, boolean enabled) {
        var pref = findPreference(key);
        if (pref != null) {
            pref.setEnabled(enabled);
            if (pref instanceof MaterialSwitchPreference && !enabled) {
                ((MaterialSwitchPreference) pref).setChecked(false);
            }
        }
    }

    private void monitorPreference() {
        var downloadstatus = (MaterialSwitchPreference) findPreference("downloadstatus");

        if (downloadstatus != null) {
            downloadstatus.setOnPreferenceChangeListener((preference, newValue) -> checkStoragePermission(newValue));
        }

        var downloadviewonce = (MaterialSwitchPreference) findPreference("downloadviewonce");
        if (downloadviewonce != null) {
            downloadviewonce.setOnPreferenceChangeListener((preference, newValue) -> checkStoragePermission(newValue));
        }
    }

    private boolean checkStoragePermission(Object newValue) {
        if (newValue instanceof Boolean && (Boolean) newValue) {
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) || (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
                App.showRequestStoragePermission(requireActivity());
                return false;
            }
        }
        return true;
    }

    @SuppressLint("ApplySharedPref")
    private void chanceStates(String key) {

        // Check for LSPatch environment and disable incompatible features
        checkLSPatchCompatibility();

        var lite_mode = mPrefs.getBoolean("lite_mode", false);

        if (lite_mode) {
            setPreferenceState("wallpaper", false);
            setPreferenceState("custom_filters", false);
        }

        if (Objects.equals(key, "thememode")) {
            var mode = Integer.parseInt(mPrefs.getString("thememode", "0"));
            App.setThemeMode(mode);
        }

        if (Objects.equals(key, "force_english")) {
            mPrefs.edit().commit();
            Utils.doRestart(requireContext());
        }

        var igstatus = mPrefs.getBoolean("igstatus", false);
        setPreferenceState("oldstatus", !igstatus);

        var oldstatus = mPrefs.getBoolean("oldstatus", false);
        setPreferenceState("verticalstatus", !oldstatus);
        setPreferenceState("channels", !oldstatus);
        setPreferenceState("removechannel_rec", !oldstatus);
        setPreferenceState("status_style", !oldstatus);
        setPreferenceState("igstatus", !oldstatus);

        var channels = mPrefs.getBoolean("channels", false);
        setPreferenceState("removechannel_rec", !channels && !oldstatus);

        var freezelastseen = mPrefs.getBoolean("freezelastseen", false);
        setPreferenceState("show_freezeLastSeen", !freezelastseen);


        var separategroups = mPrefs.getBoolean("separategroups", false);
        setPreferenceState("filtergroups", !separategroups);

        var filtergroups = mPrefs.getBoolean("filtergroups", false);
        setPreferenceState("separategroups", !filtergroups);


        var callBlockContacts = findPreference("call_block_contacts");
        var callWhiteContacts = findPreference("call_white_contacts");
        if (callBlockContacts != null && callWhiteContacts != null) {
            var callType = Integer.parseInt(mPrefs.getString("call_privacy", "0"));
            switch (callType) {
                case 3:
                    callBlockContacts.setEnabled(true);
                    callWhiteContacts.setEnabled(false);
                    break;
                case 4:
                    callWhiteContacts.setEnabled(true);
                    callBlockContacts.setEnabled(false);
                    break;
                default:
                    callWhiteContacts.setEnabled(false);
                    callBlockContacts.setEnabled(false);
                    break;
            }

        }
    }

    /**
     * Applies LSPatch compatibility filtering to preference screen
     */
    private void applyLSPatchFiltering() {
        try {
            if (getPreferenceScreen() != null) {
                LSPatchPreferenceManager.filterPreferences(getPreferenceScreen(), requireContext());
            }
        } catch (Exception e) {
            // LSPatch filtering failed, continue without it
        }
    }

    /**
     * Disables preferences that are incompatible with LSPatch
     */
    private void checkLSPatchCompatibility() {
        try {
            LSPatchCompat.init();
            if (LSPatchCompat.isLSPatchEnvironment()) {
                // Disable features that require system server hooks
                setPreferenceState("bootloader_spoofer", false);
                setPreferenceState("bootloader_spoofer_custom", false);

                // Features with limited functionality in LSPatch manager mode
                if (LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
                    // Resource-related features may have limited functionality
                    setPreferenceState("custom_css", false);
                    setPreferenceState("wallpaper", false);
                }

                // Mark feature as having limitations
                markFeatureAsLimited("custom_filters", "May have limited functionality in LSPatch");
            }
        } catch (Exception e) {
            // LSPatch classes may not be available in UI context, ignore
        }
    }

    /**
     * Marks a preference as having limited functionality
     */
    private void markFeatureAsLimited(String key, String warning) {
        var pref = findPreference(key);
        if (pref != null) {
            String currentSummary = pref.getSummary() != null ? pref.getSummary().toString() : "";
            if (!currentSummary.contains("LSPatch")) {
                pref.setSummary(currentSummary + " ⚠️ " + warning);
            }
        }
    }

    public void setDisplayHomeAsUpEnabled(boolean enabled) {
        if (getActivity() == null) return;
        var actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(enabled);
        }
    }
}
