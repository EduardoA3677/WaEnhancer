package com.wmods.wppenhacer.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.navigation.NavigationBarView;
import com.waseemsabir.betterypermissionhelper.BatteryPermissionHelper;
import com.wmods.wppenhacer.App;
import com.wmods.wppenhacer.BuildConfig;
import com.wmods.wppenhacer.R;
import com.wmods.wppenhacer.activities.base.BaseActivity;
import com.wmods.wppenhacer.adapter.MainPagerAdapter;
import com.wmods.wppenhacer.databinding.ActivityMainBinding;
import com.wmods.wppenhacer.utils.FilePicker;

import java.io.File;

public class MainActivity extends BaseActivity {

    private ActivityMainBinding binding;

    private BatteryPermissionHelper batteryPermissionHelper = BatteryPermissionHelper.Companion.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        App.changeLanguage(this);
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setIcon(R.mipmap.launcher);
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        MainPagerAdapter pagerAdapter = new MainPagerAdapter(this);
        binding.viewPager.setAdapter(pagerAdapter);

        binding.navView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @SuppressLint("NonConstantResourceId")
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                return switch (item.getItemId()) {
                    case R.id.navigation_chat -> {
                        binding.viewPager.setCurrentItem(0);
                        yield true;
                    }
                    case R.id.navigation_privacy -> {
                        binding.viewPager.setCurrentItem(1);
                        yield true;
                    }
                    case R.id.navigation_home -> {
                        binding.viewPager.setCurrentItem(2);
                        yield true;
                    }
                    case R.id.navigation_media -> {
                        binding.viewPager.setCurrentItem(3);
                        yield true;
                    }
                    case R.id.navigation_colors -> {
                        binding.viewPager.setCurrentItem(4);
                        yield true;
                    }
                    default -> false;
                };
            }
        });

        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                binding.navView.getMenu().getItem(position).setChecked(true);
            }
        });
        binding.viewPager.setCurrentItem(2, false);
        createMainDir();
        FilePicker.registerFilePicker(this);
    }


    private void createMainDir() {
        var nomedia = new File(App.getWaEnhancerFolder(), ".nomedia");
        if (nomedia.exists()) {
            nomedia.delete();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.header_menu, menu);
        var powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
            menu.findItem(R.id.batteryoptimization).setVisible(false);
        }
        return true;
    }

    @SuppressLint("BatteryLife")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        } else if (item.getItemId() == R.id.batteryoptimization) {
            if (batteryPermissionHelper.isBatterySaverPermissionAvailable(this, true)) {
                batteryPermissionHelper.getPermission(this, true, true);
            } else {
                var intent = new Intent();
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 0);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public static boolean isXposedEnabled() {
        // Use the new LSPatch-compatible module status detection
        try {
            LSPatchModuleStatus.ModuleStatus status = LSPatchModuleStatus.getCurrentStatus();

            // Log detailed status for debugging
            if (BuildConfig.DEBUG) {
                android.util.Log.d("WaEnhancer-MainActivity",
                        "Module status check:\n" + LSPatchModuleStatus.getDetailedStatus());
            }

            return status.isActive();

        } catch (Exception e) {
            // Fallback to the original detection method
            android.util.Log.w("WaEnhancer-MainActivity",
                    "Error with new status detection, falling back: " + e.getMessage());

            return isXposedEnabledFallback();
        }
    }

    /**
     * Fallback detection method (original implementation enhanced)
     */
    private static boolean isXposedEnabledFallback() {
        // Try to detect LSPatch environment first (most reliable)
        try {
            // Check for LSPatch loader classes
            Class.forName("org.lsposed.lspatch.loader.LSPApplication");
            return true;
        } catch (ClassNotFoundException e1) {
            try {
                Class.forName("org.lsposed.lspatch.metaloader.LSPAppComponentFactoryStub");
                return true;
            } catch (ClassNotFoundException e2) {
                try {
                    Class.forName("org.lsposed.lspatch.service.LocalApplicationService");
                    return true;
                } catch (ClassNotFoundException e3) {
                    try {
                        Class.forName("org.lsposed.lspatch.service.RemoteApplicationService");
                        return true;
                    } catch (ClassNotFoundException e4) {
                        // Check for LSPatch-specific system properties
                        try {
                            String lspatchMarker = System.getProperty("lspatch.enabled");
                            if ("true".equals(lspatchMarker)) {
                                return true;
                            }
                        } catch (Exception e5) {
                            // Ignore property access errors
                        }

                        // Check for LSPatch process markers
                        try {
                            // LSPatch sometimes sets specific environment variables
                            String lspatchEnv = System.getenv("LSPATCH_VERSION");
                            if (lspatchEnv != null && !lspatchEnv.isEmpty()) {
                                return true;
                            }
                        } catch (Exception e6) {
                            // Ignore environment access errors
                        }

                        // Check for classic Xposed
                        try {
                            Class.forName("de.robv.android.xposed.XposedBridge");
                            // Additional check to ensure it's actually working
                            try {
                                Class<?> xposedBridge = Class.forName("de.robv.android.xposed.XposedBridge");
                                java.lang.reflect.Method getXposedVersion = xposedBridge.getMethod("getXposedVersion");
                                Object version = getXposedVersion.invoke(null);
                                return version != null;
                            } catch (Exception e7) {
                                // If we can load the class but not call methods, it might still be active
                                return true;
                            }
                        } catch (ClassNotFoundException e7) {
                            // Check for LSPosed (modern Xposed)
                            try {
                                Class.forName("org.lsposed.lspd.core.Startup");
                                return true;
                            } catch (ClassNotFoundException e8) {
                                // Final fallback: check for known hook indicators
                                try {
                                    // Look for signs that hooks are active
                                    Class<?> thisClass = MainActivity.class;
                                    java.lang.reflect.Method[] methods = thisClass.getDeclaredMethods();
                                    for (java.lang.reflect.Method method : methods) {
                                        if (method.getName().equals("isXposedEnabled")) {
                                            // Check if the method has been hooked by examining stack trace
                                            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
                                            for (StackTraceElement element : stack) {
                                                if (element.getClassName().contains("Xposed") ||
                                                        element.getClassName().contains("LSPatch") ||
                                                        element.getClassName().contains("lspatch")) {
                                                    return true;
                                                }
                                            }
                                            break;
                                        }
                                    }
                                } catch (Exception e9) {
                                    // Ignore reflection errors
                                }

                                // Neither LSPatch nor Xposed detected
                                return false;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Get human-readable module status for display in UI
     */
    public static String getModuleStatusText() {
        try {
            LSPatchModuleStatus.ModuleStatus status = LSPatchModuleStatus.getCurrentStatus();
            return status.getDisplayName();
        } catch (Exception e) {
            return isXposedEnabled() ? "Active" : "Inactive";
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}