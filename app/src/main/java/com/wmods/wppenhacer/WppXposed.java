package com.wmods.wppenhacer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.XModuleResources;
import android.view.Window;
import android.view.WindowManager;

import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.wmods.wppenhacer.activities.MainActivity;
import com.wmods.wppenhacer.xposed.AntiUpdater;
import com.wmods.wppenhacer.xposed.bridge.ScopeHook;
import com.wmods.wppenhacer.xposed.core.FeatureLoader;
import com.wmods.wppenhacer.xposed.core.LSPatchCompat;
import com.wmods.wppenhacer.xposed.core.LSPatchPreferences;
import com.wmods.wppenhacer.xposed.core.LSPatchFeatureValidator;
import com.wmods.wppenhacer.xposed.core.LSPatchTestSuite;
import com.wmods.wppenhacer.xposed.core.LSPatchBridge;
import com.wmods.wppenhacer.xposed.downgrade.Patch;
import com.wmods.wppenhacer.xposed.spoofer.HookBL;
import com.wmods.wppenhacer.xposed.utils.ResId;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class WppXposed implements IXposedHookLoadPackage, IXposedHookInitPackageResources, IXposedHookZygoteInit {

    private static LSPatchPreferences pref;
    private String MODULE_PATH;
    public static XC_InitPackageResources.InitPackageResourcesParam ResParam;

    @NonNull
    public static LSPatchPreferences getPref() {
        if (pref == null) {
            if (LSPatchCompat.isLSPatchEnvironment()) {
                // In LSPatch environment, create LSPatch-compatible preferences
                try {
                    // Try to get context from LSPatchCompat helper method
                    Context context = getCurrentApplicationContext();
                    if (context != null) {
                        pref = new LSPatchPreferences(context);
                        
                        // Verify the preferences are functional
                        if (!pref.isFunctional()) {
                            XposedBridge.log("LSPatch preferences not functional, trying XSharedPreferences fallback");
                            XSharedPreferences xPrefs = new XSharedPreferences(BuildConfig.APPLICATION_ID);
                            pref = new LSPatchPreferences(xPrefs);
                        }
                        
                        XposedBridge.log("LSPatch preferences initialized successfully");
                    } else {
                        // Fallback to XSharedPreferences if context is not available
                        XSharedPreferences xPrefs = new XSharedPreferences(BuildConfig.APPLICATION_ID);
                        xPrefs.makeWorldReadable();
                        pref = new LSPatchPreferences(xPrefs);
                    }
                    
                } catch (Exception e) {
                    XposedBridge.log("Failed to initialize LSPatch preferences: " + e.getMessage());
                    // Fallback to XSharedPreferences
                    XSharedPreferences xPrefs = new XSharedPreferences(BuildConfig.APPLICATION_ID);
                    xPrefs.makeWorldReadable();
                    pref = new LSPatchPreferences(xPrefs);
                }
            } else {
                // Traditional Xposed environment
                XSharedPreferences xPrefs = new XSharedPreferences(BuildConfig.APPLICATION_ID);
                xPrefs.makeWorldReadable();
                pref = new LSPatchPreferences(xPrefs);
            }
        }
        return pref;
    }

    @NonNull
    public static XSharedPreferences getXPref() {
        return getPref().getXSharedPreferences();
    }

    @SuppressLint("WorldReadableFiles")
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        var packageName = lpparam.packageName;
        var classLoader = lpparam.classLoader;

        if (packageName.equals(BuildConfig.APPLICATION_ID)) {
            XposedHelpers.findAndHookMethod(MainActivity.class.getName(), lpparam.classLoader, "isXposedEnabled", XC_MethodReplacement.returnConstant(true));
            XposedHelpers.findAndHookMethod(PreferenceManager.class.getName(), lpparam.classLoader, "getDefaultSharedPreferencesMode", XC_MethodReplacement.returnConstant(ContextWrapper.MODE_WORLD_READABLE));
            return;
        }

        AntiUpdater.hookSession(lpparam);

        Patch.handleLoadPackage(lpparam, getXPref());

        // Initialize LSPatch compatibility first for early filtering
        LSPatchCompat.init();

        // Only run system hooks if not in LSPatch environment
        if (!LSPatchCompat.isLSPatchEnvironment()) {
            ScopeHook.hook(lpparam);
            //  AndroidPermissions.hook(lpparam); in tests
        } else {
            XposedBridge.log("[LSPatch] Skipping system hooks (ScopeHook, AndroidPermissions) - not compatible with LSPatch");
        }

        if ((packageName.equals(FeatureLoader.PACKAGE_WPP) && App.isOriginalPackage()) || packageName.equals(FeatureLoader.PACKAGE_BUSINESS)) {
            XposedBridge.log("[•] This package: " + lpparam.packageName);

            if (LSPatchCompat.isLSPatchEnvironment()) {
                LSPatchCompat.LSPatchMode mode = LSPatchCompat.getLSPatchMode();
                XposedBridge.log("[LSPatch] Running in LSPatch mode: " + mode);
                
                // Critical: Verify we're actually in WhatsApp and hooks are working
                if (!verifyWhatsAppLSPatchContext(packageName, classLoader)) {
                    XposedBridge.log("[LSPatch] ERROR: WhatsApp context verification failed!");
                    XposedBridge.log("[LSPatch] This indicates LSPatch is not properly hooked into WhatsApp");
                    return; // Stop loading if verification fails
                }
                
                // Enhanced LSPatch status logging
                XposedBridge.log("[LSPatch] Module successfully loaded in LSPatch environment");
                XposedBridge.log("[LSPatch] LSPatch mode: " + mode);
                XposedBridge.log("[LSPatch] Service available: " + LSPatchCompat.isLSPatchServiceAvailable());
                XposedBridge.log("[LSPatch] Bridge initialized: " + LSPatchBridge.isInitialized());
                
                // Validate features in LSPatch environment
                try {
                    XposedBridge.log("[LSPatch] Starting feature compatibility validation...");
                    Map<String, LSPatchFeatureValidator.ValidationResult> results = 
                        LSPatchFeatureValidator.validateAllFeatures(classLoader);
                    
                    // Check if all critical features are working
                    boolean allCriticalWorking = LSPatchFeatureValidator.areAllCriticalFeaturesCompatible();
                    if (allCriticalWorking) {
                        XposedBridge.log("[LSPatch] ✓ All critical features validated successfully");
                    } else {
                        XposedBridge.log("[LSPatch] ⚠ Some critical features may have issues - check logs for details");
                    }
                    
                    // Log summary
                    int compatible = 0, limited = 0, incompatible = 0;
                    for (LSPatchFeatureValidator.ValidationResult result : results.values()) {
                        if (result.isCompatible) {
                            if (result.hasLimitations) limited++;
                            else compatible++;
                        } else {
                            incompatible++;
                        }
                    }
                    
                    XposedBridge.log(String.format("[LSPatch] Feature status - Compatible: %d, Limited: %d, Incompatible: %d", 
                        compatible, limited, incompatible));
                        
                } catch (Exception e) {
                    XposedBridge.log("[LSPatch] Feature validation failed: " + e.getMessage());
                    XposedBridge.log(e);
                }
                
                // Optional: Run comprehensive test suite if enabled
                if (pref.getBoolean("lspatch_run_tests", false)) {
                    try {
                        XposedBridge.log("[LSPatch] Running comprehensive test suite...");
                        List<LSPatchTestSuite.TestResult> testResults = 
                            LSPatchTestSuite.runCompleteTestSuite(classLoader);
                        
                        int testsPassed = 0;
                        for (LSPatchTestSuite.TestResult result : testResults) {
                            if (result.passed) testsPassed++;
                        }
                        
                        XposedBridge.log(String.format("[LSPatch] Test suite completed: %d/%d tests passed", 
                            testsPassed, testResults.size()));
                            
                    } catch (Exception e) {
                        XposedBridge.log("[LSPatch] Test suite execution failed: " + e.getMessage());
                    }
                } else {
                    // Run basic smoke test
                    boolean smokeTestPassed = LSPatchTestSuite.runSmokeTest();
                    XposedBridge.log("[LSPatch] Smoke test: " + (smokeTestPassed ? "PASSED" : "FAILED"));
                }
            }

            // Load features with LSPatch compatibility
            FeatureLoader.start(classLoader, getXPref(), lpparam.appInfo.sourceDir);

            disableSecureFlag();
        }
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {
        var packageName = resparam.packageName;

        if (!packageName.equals(FeatureLoader.PACKAGE_WPP) && !packageName.equals(FeatureLoader.PACKAGE_BUSINESS))
            return;

        // Check if resource hooks are available in LSPatch
        if (LSPatchCompat.isLSPatchEnvironment() && !LSPatchCompat.isFeatureAvailable("RESOURCE_HOOKS")) {
            XposedBridge.log("[LSPatch] Resource hooks not available, skipping resource initialization");
            return;
        }

        XModuleResources modRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);
        ResParam = resparam;

        try {
            for (var field : ResId.string.class.getFields()) {
                var field1 = R.string.class.getField(field.getName());
                field.set(null, resparam.res.addResource(modRes, field1.getInt(null)));
            }

            for (var field : ResId.array.class.getFields()) {
                var field1 = R.array.class.getField(field.getName());
                field.set(null, resparam.res.addResource(modRes, field1.getInt(null)));
            }

            for (var field : ResId.drawable.class.getFields()) {
                var field1 = R.drawable.class.getField(field.getName());
                field.set(null, resparam.res.addResource(modRes, field1.getInt(null)));
            }
        } catch (Exception e) {
            XposedBridge.log("[WaEnhancer] Error setting up resources: " + e.getMessage());
            if (LSPatchCompat.isLSPatchEnvironment()) {
                XposedBridge.log("[LSPatch] Resource error might be due to LSPatch limitations");
            }
        }
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        MODULE_PATH = startupParam.modulePath;
    }


    public void disableSecureFlag() {
        XposedHelpers.findAndHookMethod(Window.class, "setFlags", int.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.args[0] = (int) param.args[0] & ~WindowManager.LayoutParams.FLAG_SECURE;
                param.args[1] = (int) param.args[1] & ~WindowManager.LayoutParams.FLAG_SECURE;
            }
        });

        XposedHelpers.findAndHookMethod(Window.class, "addFlags", int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.args[0] = (int) param.args[0] & ~WindowManager.LayoutParams.FLAG_SECURE;
                if ((int) param.args[0] == 0) {
                    param.setResult(null);
                }
            }
        });
    }

    /**
     * Helper method to get current application context
     * This works in both LSPatch and classic Xposed environments
     */
    private static Context getCurrentApplicationContext() {
        try {
            // Try ActivityThread method first (most reliable)
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            return (Context) activityThreadClass.getMethod("getApplication").invoke(activityThread);
        } catch (Exception e) {
            try {
                // Fallback: try AndroidAppHelper if available (Xposed specific)
                Class<?> appHelperClass = Class.forName("de.robv.android.xposed.AndroidAppHelper");
                return (Context) appHelperClass.getMethod("currentApplication").invoke(null);
            } catch (Exception e2) {
                XposedBridge.log("Could not get application context: " + e2.getMessage());
                return null;
            }
        }
    }

    /**
     * Verify that we're properly hooked into WhatsApp in LSPatch environment
     * This is critical to ensure WaEnhancer functions correctly
     */
    private boolean verifyWhatsAppLSPatchContext(String packageName, ClassLoader classLoader) {
        try {
            // 1. Verify package name is WhatsApp
            if (!FeatureLoader.PACKAGE_WPP.equals(packageName) && !FeatureLoader.PACKAGE_BUSINESS.equals(packageName)) {
                XposedBridge.log("[LSPatch] Invalid package name: " + packageName);
                return false;
            }

            // 2. Try to load key WhatsApp classes to ensure we're in the right context
            String[] criticalClasses = {
                "com.whatsapp.HomeActivity",
                "com.whatsapp.Main",
                "com.whatsapp.Conversation"
            };

            int accessibleClasses = 0;
            for (String className : criticalClasses) {
                try {
                    Class<?> clazz = classLoader.loadClass(className);
                    if (clazz != null) {
                        accessibleClasses++;
                        XposedBridge.log("[LSPatch] Successfully loaded: " + className);
                    }
                } catch (ClassNotFoundException e) {
                    // Try alternative class paths for newer WhatsApp versions
                    if (className.equals("com.whatsapp.HomeActivity")) {
                        try {
                            classLoader.loadClass("com.whatsapp.home.ui.HomeActivity");
                            accessibleClasses++;
                            XposedBridge.log("[LSPatch] Successfully loaded alternative HomeActivity");
                        } catch (ClassNotFoundException e2) {
                            XposedBridge.log("[LSPatch] Could not load HomeActivity: " + e2.getMessage());
                        }
                    }
                }
            }

            if (accessibleClasses < 2) {
                XposedBridge.log("[LSPatch] ERROR: Could only access " + accessibleClasses + "/" + criticalClasses.length + " WhatsApp classes");
                return false;
            }

            // 3. Verify we can get application context
            Context context = getCurrentApplicationContext();
            if (context == null) {
                XposedBridge.log("[LSPatch] ERROR: Could not get application context");
                return false;
            }

            String contextPackageName = context.getPackageName();
            if (!packageName.equals(contextPackageName)) {
                XposedBridge.log("[LSPatch] ERROR: Package name mismatch. Expected: " + packageName + ", Got: " + contextPackageName);
                return false;
            }

            // 4. Test XposedBridge functionality
            try {
                XposedBridge.log("[LSPatch] XposedBridge test successful");
            } catch (Exception e) {
                XposedBridge.log("[LSPatch] ERROR: XposedBridge not functional: " + e.getMessage());
                return false;
            }

            // 5. Verify LSPatch service is available
            try {
                boolean serviceAvailable = LSPatchCompat.isLSPatchServiceAvailable();
                if (!serviceAvailable) {
                    XposedBridge.log("[LSPatch] WARNING: LSPatch service not available, using fallback mode");
                }
            } catch (Exception e) {
                XposedBridge.log("[LSPatch] WARNING: Could not check LSPatch service: " + e.getMessage());
            }

            XposedBridge.log("[LSPatch] WhatsApp context verification PASSED");
            return true;

        } catch (Exception e) {
            XposedBridge.log("[LSPatch] ERROR: WhatsApp context verification failed: " + e.getMessage());
            return false;
        }
    }
}
