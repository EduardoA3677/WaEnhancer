package com.wmods.wppenhacer;

import android.annotation.SuppressLint;
import android.content.ContextWrapper;
import android.content.res.XModuleResources;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.wmods.wppenhacer.activities.MainActivity;
import com.wmods.wppenhacer.xposed.AntiUpdater;
import com.wmods.wppenhacer.xposed.bridge.ScopeHook;
import com.wmods.wppenhacer.xposed.core.FeatureLoader;
import com.wmods.wppenhacer.xposed.core.LSPatchCompat;
import com.wmods.wppenhacer.xposed.core.LSPatchPreferences;
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
            XSharedPreferences xprefs = new XSharedPreferences(BuildConfig.APPLICATION_ID, BuildConfig.APPLICATION_ID + "_preferences");
            xprefs.makeWorldReadable();
            xprefs.reload();
            pref = new LSPatchPreferences(xprefs);
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

        ScopeHook.hook(lpparam);

        //  AndroidPermissions.hook(lpparam); in tests
        if ((packageName.equals(FeatureLoader.PACKAGE_WPP) && App.isOriginalPackage()) || packageName.equals(FeatureLoader.PACKAGE_BUSINESS)) {
            XposedBridge.log("[•] This package: " + lpparam.packageName);

            // Initialize LSPatch compatibility first
            LSPatchCompat.init();
            if (LSPatchCompat.isLSPatchEnvironment()) {
                XposedBridge.log("[LSPatch] Running in LSPatch mode: " + LSPatchCompat.getLSPatchMode());
                
                // Apply LSPatch specific setup
                if (LSPatchCompat.getLSPatchMode() == LSPatchCompat.LSPatchMode.LSPATCH_EMBEDDED) {
                    XposedBridge.log("[LSPatch] Using embedded mode optimizations");
                } else if (LSPatchCompat.getLSPatchMode() == LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
                    XposedBridge.log("[LSPatch] Using manager mode with bridge service");
                }
                
                // Check feature availability
                if (!LSPatchCompat.isFeatureAvailable("RESOURCE_HOOKS")) {
                    XposedBridge.log("[LSPatch] Warning: Resource hooks may not work properly");
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

}
