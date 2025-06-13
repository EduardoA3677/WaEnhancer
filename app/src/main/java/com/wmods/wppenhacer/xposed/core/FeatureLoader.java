package com.wmods.wppenhacer.xposed.core;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.wmods.wppenhacer.App;
import com.wmods.wppenhacer.BuildConfig;
import com.wmods.wppenhacer.UpdateChecker;
import com.wmods.wppenhacer.xposed.core.components.AlertDialogWpp;
import com.wmods.wppenhacer.xposed.core.components.FMessageWpp;
import com.wmods.wppenhacer.xposed.core.components.SharedPreferencesWrapper;
import com.wmods.wppenhacer.xposed.core.devkit.Unobfuscator;
import com.wmods.wppenhacer.xposed.core.devkit.UnobfuscatorCache;
import com.wmods.wppenhacer.xposed.features.customization.BubbleColors;
import com.wmods.wppenhacer.xposed.features.customization.CustomThemeV2;
import com.wmods.wppenhacer.xposed.features.customization.CustomTime;
import com.wmods.wppenhacer.xposed.features.customization.CustomToolbar;
import com.wmods.wppenhacer.xposed.features.customization.CustomView;
import com.wmods.wppenhacer.xposed.features.customization.FilterGroups;
import com.wmods.wppenhacer.xposed.features.customization.HideSeenView;
import com.wmods.wppenhacer.xposed.features.customization.HideTabs;
import com.wmods.wppenhacer.xposed.features.customization.IGStatus;
import com.wmods.wppenhacer.xposed.features.customization.SeparateGroup;
import com.wmods.wppenhacer.xposed.features.customization.ShowOnline;
import com.wmods.wppenhacer.xposed.features.general.AntiRevoke;
import com.wmods.wppenhacer.xposed.features.general.CallType;
import com.wmods.wppenhacer.xposed.features.general.ChatLimit;
import com.wmods.wppenhacer.xposed.features.general.DeleteStatus;
import com.wmods.wppenhacer.xposed.features.general.LiteMode;
import com.wmods.wppenhacer.xposed.features.general.MenuStatus;
import com.wmods.wppenhacer.xposed.features.general.NewChat;
import com.wmods.wppenhacer.xposed.features.general.Others;
import com.wmods.wppenhacer.xposed.features.general.PinnedLimit;
import com.wmods.wppenhacer.xposed.features.general.SeenTick;
import com.wmods.wppenhacer.xposed.features.general.ShareLimit;
import com.wmods.wppenhacer.xposed.features.general.ShowEditMessage;
import com.wmods.wppenhacer.xposed.features.general.Tasker;
import com.wmods.wppenhacer.xposed.features.media.DownloadProfile;
import com.wmods.wppenhacer.xposed.features.media.DownloadViewOnce;
import com.wmods.wppenhacer.xposed.features.media.MediaPreview;
import com.wmods.wppenhacer.xposed.features.media.MediaQuality;
import com.wmods.wppenhacer.xposed.features.media.StatusDownload;
import com.wmods.wppenhacer.xposed.features.others.ActivityController;
import com.wmods.wppenhacer.xposed.features.others.AudioTranscript;
import com.wmods.wppenhacer.xposed.features.others.Channels;
import com.wmods.wppenhacer.xposed.features.others.ChatFilters;
import com.wmods.wppenhacer.xposed.features.others.CopyStatus;
import com.wmods.wppenhacer.xposed.features.others.DebugFeature;
import com.wmods.wppenhacer.xposed.features.others.GoogleTranslate;
import com.wmods.wppenhacer.xposed.features.others.GroupAdmin;
import com.wmods.wppenhacer.xposed.features.others.MenuHome;
import com.wmods.wppenhacer.xposed.features.others.Stickers;
import com.wmods.wppenhacer.xposed.features.others.TextStatusComposer;
import com.wmods.wppenhacer.xposed.features.others.ToastViewer;
import com.wmods.wppenhacer.xposed.features.privacy.AntiWa;
import com.wmods.wppenhacer.xposed.features.privacy.CallPrivacy;
import com.wmods.wppenhacer.xposed.features.privacy.CustomPrivacy;
import com.wmods.wppenhacer.xposed.features.privacy.DndMode;
import com.wmods.wppenhacer.xposed.features.privacy.FreezeLastSeen;
import com.wmods.wppenhacer.xposed.features.privacy.HideChat;
import com.wmods.wppenhacer.xposed.features.privacy.HideReceipt;
import com.wmods.wppenhacer.xposed.features.privacy.HideSeen;
import com.wmods.wppenhacer.xposed.features.privacy.TagMessage;
import com.wmods.wppenhacer.xposed.features.privacy.TypingPrivacy;
import com.wmods.wppenhacer.xposed.features.privacy.ViewOnce;
import com.wmods.wppenhacer.xposed.spoofer.HookBL;
import com.wmods.wppenhacer.xposed.utils.DesignUtils;
import com.wmods.wppenhacer.xposed.utils.ReflectionUtils;
import com.wmods.wppenhacer.xposed.utils.ResId;
import com.wmods.wppenhacer.xposed.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class FeatureLoader {
    public static Application mApp;

    public final static String PACKAGE_WPP = "com.whatsapp";
    public final static String PACKAGE_BUSINESS = "com.whatsapp.w4b";

    private static final ArrayList<ErrorItem> list = new ArrayList<>();
    private static List<String> supportedVersions;
    private static String currentVersion;

    /**
     * Initialize LSPatch compatibility layer
     * This method sets up LSPatch specific configurations and feature filtering
     */
    private static void initializeLSPatchCompatibility(XSharedPreferences pref) {
        // Initialize LSPatch compatibility
        LSPatchCompat.init();
        
        if (LSPatchCompat.isLSPatchEnvironment()) {
            XposedBridge.log("LSPatch environment detected - Mode: " + LSPatchCompat.getCurrentMode());
            
            // Initialize LSPatch bridge if needed
            try {
                // This will be called later when context is available
                pref.edit().putBoolean("lspatch_compatibility_enabled", true).apply();
            } catch (Exception e) {
                XposedBridge.log("Failed to set LSPatch preference: " + e.getMessage());
            }
        }
    }

    /**
     * Initialize LSPatch bridge with application context
     */
    private static void initializeLSPatchBridge(Application app) {
        if (LSPatchCompat.isLSPatchEnvironment()) {
            boolean success = LSPatchBridge.initialize(app);
            if (success) {
                XposedBridge.log("LSPatch bridge initialized successfully");
            } else {
                XposedBridge.log("LSPatch bridge initialization failed");
            }
        }
    }

    /**
     * Checks if a feature is compatible with current environment
     */
    private static boolean isFeatureCompatible(String featureName) {
        if (!LSPatchCompat.isLSPatchEnvironment()) {
            return true; // All features work in classic Xposed
        }
        
        // Features that require system server hooks are not compatible with LSPatch
        String[] systemServerFeatures = {
            "ScopeHook", // Requires system server hooks
            "AndroidPermissions", // Requires system server hooks
            "HookBL" // Bootloader spoofer requires system-level access
        };
        
        for (String systemFeature : systemServerFeatures) {
            if (featureName.contains(systemFeature) || featureName.equals(systemFeature)) {
                XposedBridge.log("Feature " + featureName + " skipped - not compatible with LSPatch");
                return false;
            }
        }
        
        // Features that have limited functionality in LSPatch
        if (LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
            String[] limitedFeatures = {
                "resource_hooks", // Limited in manager mode
                "custom_themes" // Limited in manager mode
            };
            
            for (String limitedFeature : limitedFeatures) {
                if (featureName.contains(limitedFeature)) {
                    XposedBridge.log("Feature " + featureName + " has limited functionality in LSPatch manager mode");
                }
            }
        }
        
        return true;
    }

    public static void start(@NonNull ClassLoader loader, @NonNull XSharedPreferences pref, String sourceDir) {

        // Initialize LSPatch compatibility layer
        initializeLSPatchCompatibility(pref);

        if (!Unobfuscator.initWithPath(sourceDir)) {
            XposedBridge.log("Can't init dexkit");
            return;
        }
        Feature.DEBUG = pref.getBoolean("enablelogs", true);
        Utils.xprefs = pref;

        XposedHelpers.findAndHookMethod(Instrumentation.class, "callApplicationOnCreate", Application.class, new XC_MethodHook() {
            @SuppressWarnings("deprecation")
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                mApp = (Application) param.args[0];

                // Initialize LSPatch bridge with application context
                initializeLSPatchBridge(mApp);

                // Inject Booloader Spoofer (only if compatible with current environment)
                if (pref.getBoolean("bootloader_spoofer", false) && isFeatureCompatible("bootloader_spoofer")) {
                    HookBL.hook(loader, pref);
                    XposedBridge.log("Bootloader Spoofer is Injected");
                } else if (pref.getBoolean("bootloader_spoofer", false)) {
                    XposedBridge.log("Bootloader Spoofer skipped - not compatible with LSPatch");
                }

                PackageManager packageManager = mApp.getPackageManager();
                pref.registerOnSharedPreferenceChangeListener((sharedPreferences, s) -> pref.reload());
                PackageInfo packageInfo = packageManager.getPackageInfo(mApp.getPackageName(), 0);
                XposedBridge.log(packageInfo.versionName);
                currentVersion = packageInfo.versionName;
                supportedVersions = Arrays.asList(mApp.getResources().getStringArray(Objects.equals(mApp.getPackageName(), FeatureLoader.PACKAGE_WPP) ? ResId.array.supported_versions_wpp : ResId.array.supported_versions_business));
                mApp.registerActivityLifecycleCallbacks(new WaCallback());
                registerReceivers();
                try {
                    var timemillis = System.currentTimeMillis();
                    boolean isSupported = supportedVersions.stream().anyMatch(s -> packageInfo.versionName.startsWith(s.replace(".xx", "")));
                    if (!isSupported) {
                        disableExpirationVersion(mApp.getClassLoader());
                        if (!pref.getBoolean("bypass_version_check", false)) {
                            StringBuilder sb = new StringBuilder()
                                    .append("Unsupported version: ")
                                    .append(packageInfo.versionName)
                                    .append("\n")
                                    .append("Only the function of ignoring the expiration of the WhatsApp version has been applied!");
                            throw new Exception(sb.toString());
                        }
                    }
                    SharedPreferencesWrapper.hookInit(mApp.getClassLoader());
                    UnobfuscatorCache.init(mApp);
                    WppCore.Initialize(loader, pref);
                    DesignUtils.setPrefs(pref);
                    initComponents(loader, pref);
                    plugins(loader, pref, packageInfo.versionName);
                    sendEnabledBroadcast(mApp);
//                    XposedHelpers.setStaticIntField(XposedHelpers.findClass("com.whatsapp.util.Log", loader), "level", 5);
                    var timemillis2 = System.currentTimeMillis() - timemillis;
                    XposedBridge.log("Loaded Hooks in " + timemillis2 + "ms");
                } catch (Throwable e) {
                    XposedBridge.log(e);
                    var error = new ErrorItem();
                    error.setPluginName("MainFeatures[Critical]");
                    error.setWhatsAppVersion(packageInfo.versionName);
                    error.setModuleVersion(BuildConfig.VERSION_NAME);
                    error.setMessage(e.getMessage());
                    error.setError(Arrays.toString(Arrays.stream(e.getStackTrace()).filter(s -> !s.getClassName().startsWith("android") && !s.getClassName().startsWith("com.android")).map(StackTraceElement::toString).toArray()));
                    list.add(error);
                }

            }
        });

        XposedHelpers.findAndHookMethod(WppCore.getHomeActivityClass(loader), "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                if (!list.isEmpty()) {
                    var activity = (Activity) param.thisObject;
                    var msg = String.join("\n", list.stream().map(item -> item.getPluginName() + " - " + item.getMessage()).toArray(String[]::new));

                    new AlertDialogWpp(activity)
                            .setTitle(activity.getString(ResId.string.error_detected))
                            .setMessage(activity.getString(ResId.string.version_error) + msg + "\n\nCurrent Version: " + currentVersion + "\nSupported Versions:\n" + String.join("\n", supportedVersions))
                            .setPositiveButton(activity.getString(ResId.string.copy_to_clipboard), (dialog, which) -> {
                                var clipboard = (ClipboardManager) mApp.getSystemService(Context.CLIPBOARD_SERVICE);
                                ClipData clip = ClipData.newPlainText("text", String.join("\n", list.stream().map(ErrorItem::toString).toArray(String[]::new)));
                                clipboard.setPrimaryClip(clip);
                                Toast.makeText(mApp, ResId.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            })
                            .show();
                }
            }
        });
    }

    private static void disableExpirationVersion(ClassLoader classLoader) {
        var expirationClass = Unobfuscator.loadExpirationClass(classLoader);
        var method = ReflectionUtils.findMethodUsingFilter(expirationClass, m -> m.getReturnType().equals(Date.class));
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                var calendar = Calendar.getInstance();
                calendar.set(2099, 12, 31);
                param.setResult(calendar.getTime());
            }
        });
    }

    private static void initComponents(ClassLoader loader, XSharedPreferences pref) throws Exception {
        AlertDialogWpp.initDialog(loader);
        FMessageWpp.initialize(loader);
        Utils.init(loader);
        WppCore.addListenerActivity((activity, state) -> {

            if (state == WppCore.ActivityChangeState.ChangeType.RESUMED) {
                checkUpdate(activity);
            }

            // Check for WAE Update
            //noinspection ConstantValue
            if (App.isOriginalPackage() && pref.getBoolean("update_check", true)) {
                if (activity.getClass().getSimpleName().equals("HomeActivity") && state == WppCore.ActivityChangeState.ChangeType.CREATED) {
                    CompletableFuture.runAsync(new UpdateChecker(activity));
                }
            }
        });
    }


    private static void checkUpdate(@NonNull Activity activity) {
        if (WppCore.getPrivBoolean("need_restart", false)) {
            WppCore.setPrivBoolean("need_restart", false);
            try {
                new AlertDialogWpp(activity).
                        setMessage(activity.getString(ResId.string.restart_wpp)).
                        setPositiveButton(activity.getString(ResId.string.yes), (dialog, which) -> {
                            if (!Utils.doRestart(activity))
                                Toast.makeText(activity, "Unable to rebooting activity", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton(activity.getString(ResId.string.no), null)
                        .show();
            } catch (Throwable ignored) {
            }
        }
    }

    private static void registerReceivers() {
        try {
            // Enhanced receiver registration with LSPatch compatibility
            if (LSPatchCompat.isLSPatchEnvironment()) {
                XposedBridge.log("Registering receivers with LSPatch compatibility");
                registerLSPatchCompatibleReceivers();
            } else {
                XposedBridge.log("Registering standard receivers");
                registerStandardReceivers();
            }
        } catch (Exception e) {
            XposedBridge.log("Failed to register receivers: " + e.getMessage());
            XposedBridge.log(e);
        }
    }
    
    /**
     * Register receivers with LSPatch-specific adaptations
     */
    private static void registerLSPatchCompatibleReceivers() {
        try {
            // Reboot receiver with LSPatch adaptations
            BroadcastReceiver restartReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        if (context.getPackageName().equals(intent.getStringExtra("PKG"))) {
                            var appName = context.getPackageManager().getApplicationLabel(context.getApplicationInfo());
                            String message = "LSPatch: " + context.getString(ResId.string.rebooting) + " " + appName + "...";
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                            
                            // LSPatch-specific restart mechanism
                            if (!performLSPatchRestart(context)) {
                                Toast.makeText(context, "Unable to restart " + appName + " in LSPatch mode", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (Exception e) {
                        XposedBridge.log("LSPatch restart receiver error: " + e.getMessage());
                    }
                }
            };
            
            // WPP receiver with enhanced LSPatch status reporting
            BroadcastReceiver wppReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        sendLSPatchEnabledBroadcast(context);
                    } catch (Exception e) {
                        XposedBridge.log("LSPatch WPP receiver error: " + e.getMessage());
                    }
                }
            };
            
            // Manual restart receiver with LSPatch support
            BroadcastReceiver restartManualReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        WppCore.setPrivBoolean("need_restart", true);
                        WppCore.setPrivBoolean("lspatch_restart_requested", true);
                    } catch (Exception e) {
                        XposedBridge.log("LSPatch manual restart receiver error: " + e.getMessage());
                    }
                }
            };
            
            // Register receivers with LSPatch-compatible flags
            int receiverFlags = ContextCompat.RECEIVER_EXPORTED;
            
            ContextCompat.registerReceiver(mApp, restartReceiver, 
                new IntentFilter(BuildConfig.APPLICATION_ID + ".WHATSAPP.RESTART"), receiverFlags);
            ContextCompat.registerReceiver(mApp, wppReceiver, 
                new IntentFilter(BuildConfig.APPLICATION_ID + ".CHECK_WPP"), receiverFlags);
            ContextCompat.registerReceiver(mApp, restartManualReceiver, 
                new IntentFilter(BuildConfig.APPLICATION_ID + ".MANUAL_RESTART"), receiverFlags);
                
            XposedBridge.log("LSPatch-compatible receivers registered successfully");
            
        } catch (Exception e) {
            XposedBridge.log("Failed to register LSPatch receivers: " + e.getMessage());
            // Fallback to standard receivers
            registerStandardReceivers();
        }
    }
    
    /**
     * Register standard receivers for traditional Xposed
     */
    private static void registerStandardReceivers() {
        // Reboot receiver
        BroadcastReceiver restartReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (context.getPackageName().equals(intent.getStringExtra("PKG"))) {
                    var appName = context.getPackageManager().getApplicationLabel(context.getApplicationInfo());
                    Toast.makeText(context, context.getString(ResId.string.rebooting) + " " + appName + "...", Toast.LENGTH_SHORT).show();
                    if (!Utils.doRestart(context))
                        Toast.makeText(context, "Unable to rebooting " + appName, Toast.LENGTH_SHORT).show();
                }
            }
        };
        ContextCompat.registerReceiver(mApp, restartReceiver, new IntentFilter(BuildConfig.APPLICATION_ID + ".WHATSAPP.RESTART"), ContextCompat.RECEIVER_EXPORTED);

        /// Wpp receiver
        BroadcastReceiver wppReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                sendEnabledBroadcast(context);
            }
        };
        ContextCompat.registerReceiver(mApp, wppReceiver, new IntentFilter(BuildConfig.APPLICATION_ID + ".CHECK_WPP"), ContextCompat.RECEIVER_EXPORTED);

        // Dialog receiver restart
        BroadcastReceiver restartManualReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                WppCore.setPrivBoolean("need_restart", true);
            }
        };
        ContextCompat.registerReceiver(mApp, restartManualReceiver, new IntentFilter(BuildConfig.APPLICATION_ID + ".MANUAL_RESTART"), ContextCompat.RECEIVER_EXPORTED);
    }
    
    /**
     * Perform LSPatch-specific restart
     */
    private static boolean performLSPatchRestart(Context context) {
        try {
            // In LSPatch, we may need different restart mechanisms
            if (LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
                // For manager mode, log restart request and use fallback
                if (LSPatchBridge.isInitialized()) {
                    XposedBridge.log("[LSPatch] Restart requested, using fallback method");
                }
            }
            
            // Fallback to standard restart
            return Utils.doRestart(context);
            
        } catch (Exception e) {
            XposedBridge.log("LSPatch restart failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Send enhanced broadcast with LSPatch status information
     */
    private static void sendLSPatchEnabledBroadcast(Context context) {
        try {
            Intent wppIntent = new Intent(BuildConfig.APPLICATION_ID + ".RECEIVER_WPP");
            wppIntent.putExtra("VERSION", context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName);
            wppIntent.putExtra("PKG", context.getPackageName());
            
            // Add LSPatch-specific information
            wppIntent.putExtra("LSPATCH_MODE", LSPatchCompat.isLSPatchEnvironment());
            if (LSPatchCompat.isLSPatchEnvironment()) {
                wppIntent.putExtra("LSPATCH_TYPE", LSPatchCompat.getCurrentMode().toString());
                wppIntent.putExtra("LSPATCH_SERVICE", LSPatchCompat.isLSPatchServiceAvailable());
                wppIntent.putExtra("LSPATCH_BRIDGE", LSPatchBridge.isInitialized());
            }
            
            wppIntent.setPackage(BuildConfig.APPLICATION_ID);
            context.sendBroadcast(wppIntent);
            
            XposedBridge.log("LSPatch status broadcast sent successfully");
            
        } catch (Exception e) {
            XposedBridge.log("Failed to send LSPatch broadcast: " + e.getMessage());
            // Fallback to standard broadcast
            sendEnabledBroadcast(context);
        }
    }

    /**
     * Standard broadcast method for compatibility
     */
    private static void sendEnabledBroadcast(Context context) {
        try {
            Intent wppIntent = new Intent(BuildConfig.APPLICATION_ID + ".RECEIVER_WPP");
            wppIntent.putExtra("VERSION", context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName);
            wppIntent.putExtra("PKG", context.getPackageName());
            wppIntent.setPackage(BuildConfig.APPLICATION_ID);
            context.sendBroadcast(wppIntent);
        } catch (Exception ignored) {
        }
    }
    
    /**
     * Add error to the error list for reporting
     */
    private static void addError(String pluginName, String error, String message) {
        try {
            ErrorItem errorItem = new ErrorItem();
            errorItem.setPluginName(pluginName);
            errorItem.setError(error);
            errorItem.setMessage(message);
            errorItem.setWhatsAppVersion(currentVersion);
            errorItem.setModuleVersion(BuildConfig.VERSION_NAME);
            list.add(errorItem);
        } catch (Exception e) {
            XposedBridge.log("Failed to add error to list: " + e.getMessage());
        }
    }

    private static void plugins(@NonNull ClassLoader loader, @NonNull XSharedPreferences pref, @NonNull String versionWpp) throws Exception {

        var classes = new Class<?>[]{
                DebugFeature.class,
                MenuStatus.class,
                ShowEditMessage.class,
                AntiRevoke.class,
                CustomToolbar.class,
                CustomView.class,
                SeenTick.class,
                BubbleColors.class,
                CallPrivacy.class,
                ActivityController.class,
//                CustomTheme.class,
                CustomThemeV2.class,
                ChatLimit.class,
                SeparateGroup.class,
                ShowOnline.class,
                DndMode.class,
                FreezeLastSeen.class,
                TypingPrivacy.class,
                HideChat.class,
                HideReceipt.class,
                HideSeen.class,
                HideSeenView.class,
                TagMessage.class,
                HideTabs.class,
                IGStatus.class,
                LiteMode.class,
                MediaQuality.class,
                NewChat.class,
                Others.class,
                PinnedLimit.class,
                CustomTime.class,
                ShareLimit.class,
                StatusDownload.class,
                ViewOnce.class,
                CallType.class,
                MediaPreview.class,
                FilterGroups.class,
                Tasker.class,
                DeleteStatus.class,
                DownloadViewOnce.class,
                Channels.class,
                DownloadProfile.class,
                ChatFilters.class,
                GroupAdmin.class,
                Stickers.class,
                CopyStatus.class,
                TextStatusComposer.class,
                ToastViewer.class,
                MenuHome.class,
                AntiWa.class,
                CustomPrivacy.class,
                AudioTranscript.class,
                GoogleTranslate.class
        };
        
        // Filter classes based on LSPatch compatibility
        List<Class<?>> compatibleClasses = filterLSPatchCompatibleFeatures(classes);
        
        int filteredCount = classes.length - compatibleClasses.size();
        XposedBridge.log("Loading " + compatibleClasses.size() + " Plugins");
        if (filteredCount > 0) {
            XposedBridge.log("LSPatch Compatibility: " + filteredCount + " features filtered due to incompatibility");
        }
        
        var executorService = Executors.newWorkStealingPool(Math.min(Runtime.getRuntime().availableProcessors(), 4));
        var times = new ArrayList<String>();
        for (var classe : compatibleClasses) {
            CompletableFuture.runAsync(() -> {
                var timemillis = System.currentTimeMillis();
                try {
                    var constructor = classe.getConstructor(ClassLoader.class, XSharedPreferences.class);
                    var plugin = (Feature) constructor.newInstance(loader, pref);
                    
                    // Use LSPatch-aware hooking if available
                    if (LSPatchCompat.isLSPatchEnvironment()) {
                        doLSPatchSafeHook(plugin);
                    } else {
                        plugin.doHook();
                    }
                } catch (Throwable e) {
                    XposedBridge.log(e);
                    var error = new ErrorItem();
                    error.setPluginName(classe.getSimpleName());
                    error.setWhatsAppVersion(versionWpp);
                    error.setModuleVersion(BuildConfig.VERSION_NAME);
                    error.setMessage(e.getMessage());
                    error.setError(Arrays.toString(Arrays.stream(e.getStackTrace()).filter(s -> !s.getClassName().startsWith("android") && !s.getClassName().startsWith("com.android")).map(StackTraceElement::toString).toArray()));
                    list.add(error);
                }
                var timemillis2 = System.currentTimeMillis() - timemillis;
                times.add("* Loaded Plugin " + classe.getSimpleName() + " in " + timemillis2 + "ms");
            }, executorService);
        }
        executorService.shutdown();
        executorService.awaitTermination(15, TimeUnit.SECONDS);
        if (DebugFeature.DEBUG) {
            for (var time : times) {
                if (time != null)
                    XposedBridge.log(time);
            }
        }
    }

    /**
     * Filters features based on LSPatch compatibility
     */
    private static List<Class<?>> filterLSPatchCompatibleFeatures(Class<?>[] allFeatures) {
        List<Class<?>> compatibleFeatures = new ArrayList<>();
        
        if (!LSPatchCompat.isLSPatchEnvironment()) {
            // All features are compatible with classic Xposed
            return Arrays.asList(allFeatures);
        }
        
        // Features that are NOT compatible with LSPatch (require system server hooks)
        String[] incompatibleFeatures = {
            "AntiWa", // Requires system server access for deep hooks
            "CustomPrivacy" // Some functionality requires system server hooks
        };
        
        // Features with limited functionality in LSPatch manager mode
        String[] limitedInManagerMode = {
            "CustomThemeV2", // Limited resource hook capabilities
            "CustomView", // Limited resource modifications
            "BubbleColors" // Limited styling capabilities
        };
        
        for (Class<?> feature : allFeatures) {
            String featureName = feature.getSimpleName();
            boolean isCompatible = true;
            
            // Check if feature is completely incompatible
            for (String incompatible : incompatibleFeatures) {
                if (featureName.equals(incompatible)) {
                    XposedBridge.log("Feature " + featureName + " disabled - incompatible with LSPatch");
                    isCompatible = false;
                    break;
                }
            }
            
            // Check if feature has limitations in manager mode
            if (isCompatible && LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
                for (String limited : limitedInManagerMode) {
                    if (featureName.equals(limited)) {
                        XposedBridge.log("Feature " + featureName + " loaded with limitations in LSPatch manager mode");
                        break;
                    }
                }
            }
            
            if (isCompatible) {
                compatibleFeatures.add(feature);
            }
        }
        
        return compatibleFeatures;
    }
    
    /**
     * Performs LSPatch-safe hook initialization for a feature
     */
    private static void doLSPatchSafeHook(Feature plugin) throws Throwable {
        try {
            // Set flag to indicate LSPatch mode for the feature
            if (plugin.prefs != null) {
                // Check if feature supports LSPatch mode
                String featureName = plugin.getClass().getSimpleName();
                boolean lspatchMode = true;
                
                // Use reflection to set LSPatch mode if feature supports it
                try {
                    java.lang.reflect.Field lspatchField = plugin.getClass().getDeclaredField("isLSPatchMode");
                    lspatchField.setAccessible(true);
                    lspatchField.setBoolean(plugin, lspatchMode);
                } catch (NoSuchFieldException e) {
                    // Feature doesn't have LSPatch mode support, that's fine
                }
            }
            
            // Apply hook with LSPatch optimizations
            plugin.doHook();
            
        } catch (Throwable e) {
            XposedBridge.log("LSPatch-safe hook failed for " + plugin.getClass().getSimpleName() + ": " + e.getMessage());
            throw e;
        }
    }

    /**
     * Load a feature with enhanced error handling and validation
     */
    private static void loadFeatureSafely(Feature feature) {
        if (feature == null) {
            XposedBridge.log("Attempted to load null feature");
            return;
        }
        
        try {
            // Use the enhanced doHook method with validation
            boolean success = feature.safeDoHook();
            
            if (success) {
                String logMessage = String.format("✓ %s loaded successfully", feature.getPluginName());
                if (LSPatchCompat.isLSPatchEnvironment()) {
                    logMessage += " (LSPatch compatible)";
                }
                XposedBridge.log(logMessage);
            } else {
                String logMessage = String.format("⚠ %s failed to load", feature.getPluginName());
                if (LSPatchCompat.isLSPatchEnvironment()) {
                    logMessage += " (LSPatch compatibility issue)";
                }
                XposedBridge.log(logMessage);
                
                // Add to error list for reporting
                addError(feature.getPluginName(), "Feature initialization failed", "Compatibility issue");
            }
            
        } catch (Throwable e) {
            String errorMessage = String.format("✗ %s failed with exception: %s", 
                feature.getPluginName(), e.getMessage());
                
            XposedBridge.log(errorMessage);
            XposedBridge.log(e);
            
            addError(feature.getPluginName(), e.getClass().getSimpleName(), e.getMessage());
        }
    }
    
    /**
     * Enhanced feature loading with LSPatch-specific optimizations
     */
    private static void loadAllFeatures(ClassLoader loader, XSharedPreferences pref) {
        XposedBridge.log("Starting feature loading process...");
        
        if (LSPatchCompat.isLSPatchEnvironment()) {
            XposedBridge.log("Loading features with LSPatch optimizations");
            
            // Apply LSPatch-specific optimizations
            LSPatchCompat.optimizeForLSPatch();
        }
        
        int totalFeatures = 0;
        int successfulFeatures = 0;
        
        try {
            // Core features (highest priority)
            Feature[] coreFeatures = {
                new Others(loader, pref),
                new AntiRevoke(loader, pref),
                new SeenTick(loader, pref),
                new ShowOnline(loader, pref)
            };
            
            XposedBridge.log("Loading core features...");
            for (Feature feature : coreFeatures) {
                totalFeatures++;
                loadFeatureSafely(feature);
                if (feature.validateFeatureCompatibility()) {
                    successfulFeatures++;
                }
            }
            
            // Privacy features
            Feature[] privacyFeatures = {
                new CustomPrivacy(loader, pref),
                new HideChat(loader, pref),
                new CallPrivacy(loader, pref),
                new TagMessage(loader, pref),
                new AntiWa(loader, pref)
            };
            
            XposedBridge.log("Loading privacy features...");
            for (Feature feature : privacyFeatures) {
                totalFeatures++;
                loadFeatureSafely(feature);
                if (feature.validateFeatureCompatibility()) {
                    successfulFeatures++;
                }
            }
            
            // Media features
            Feature[] mediaFeatures = {
                new MediaPreview(loader, pref),
                new DownloadViewOnce(loader, pref),
                new MediaQuality(loader, pref)
            };
            
            XposedBridge.log("Loading media features...");
            for (Feature feature : mediaFeatures) {
                totalFeatures++;
                loadFeatureSafely(feature);
                if (feature.validateFeatureCompatibility()) {
                    successfulFeatures++;
                }
            }
            
            // Customization features (may have more limitations in LSPatch)
            Feature[] customFeatures = {
                new CustomThemeV2(loader, pref),
                new BubbleColors(loader, pref),
                new CustomView(loader, pref),
                new CustomToolbar(loader, pref),
                new FilterGroups(loader, pref),
                new SeparateGroup(loader, pref)
            };
            
            XposedBridge.log("Loading customization features...");
            for (Feature feature : customFeatures) {
                totalFeatures++;
                // Check LSPatch compatibility for customization features
                if (LSPatchCompat.isLSPatchEnvironment() && 
                    LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_EMBEDDED) {
                    // Some customization features may not work in embedded mode
                    XposedBridge.log("Skipping " + feature.getPluginName() + " in LSPatch embedded mode");
                    continue;
                }
                loadFeatureSafely(feature);
                if (feature.validateFeatureCompatibility()) {
                    successfulFeatures++;
                }
            }
            
            // Other features
            Feature[] otherFeatures = {
                new ChatFilters(loader, pref),
                new CopyStatus(loader, pref),
                new GoogleTranslate(loader, pref),
                new ToastViewer(loader, pref),
                new Tasker(loader, pref),
                new NewChat(loader, pref),
                new CallType(loader, pref),
                new ShareLimit(loader, pref),
                new PinnedLimit(loader, pref),
                new ChatLimit(loader, pref),
                new ShowEditMessage(loader, pref),
                new Channels(loader, pref),
                new MenuStatus(loader, pref),
                new DeleteStatus(loader, pref),
                new TextStatusComposer(loader, pref),
                new IGStatus(loader, pref),
                new HideTabs(loader, pref),
                new HideSeenView(loader, pref),
                new CustomTime(loader, pref),
                new LiteMode(loader, pref)
            };
            
            XposedBridge.log("Loading additional features...");
            for (Feature feature : otherFeatures) {
                totalFeatures++;
                loadFeatureSafely(feature);
                if (feature.validateFeatureCompatibility()) {
                    successfulFeatures++;
                }
            }
            
        } catch (Exception e) {
            XposedBridge.log("Error during feature loading: " + e.getMessage());
            XposedBridge.log(e);
        }
        
        // Log summary
        XposedBridge.log(String.format("Feature loading completed: %d/%d features loaded successfully", 
            successfulFeatures, totalFeatures));
            
        if (LSPatchCompat.isLSPatchEnvironment()) {
            XposedBridge.log("LSPatch compatibility mode active");
        }
    }

    private static class ErrorItem {
        private String pluginName;
        private String whatsAppVersion;
        private String error;
        private String moduleVersion;
        private String message;

        @NonNull
        @Override
        public String toString() {
            return "pluginName='" + getPluginName() + '\'' +
                    "\nmoduleVersion='" + getModuleVersion() + '\'' +
                    "\nwhatsAppVersion='" + getWhatsAppVersion() + '\'' +
                    "\nMessage=" + getMessage() +
                    "\nerror='" + getError() + '\'';
        }

        public String getWhatsAppVersion() {
            return whatsAppVersion;
        }

        public void setWhatsAppVersion(String whatsAppVersion) {
            this.whatsAppVersion = whatsAppVersion;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public String getPluginName() {
            return pluginName;
        }

        public void setPluginName(String pluginName) {
            this.pluginName = pluginName;
        }

        public String getModuleVersion() {
            return moduleVersion;
        }

        public void setModuleVersion(String moduleVersion) {
            this.moduleVersion = moduleVersion;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
