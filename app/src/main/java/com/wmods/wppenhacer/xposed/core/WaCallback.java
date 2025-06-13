package com.wmods.wppenhacer.xposed.core;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class WaCallback implements Application.ActivityLifecycleCallbacks {
    private static void triggerActivityState(@NonNull Activity activity, WppCore.ActivityChangeState.ChangeType type) {
        WppCore.listenerAcitivity.forEach((listener) -> listener.onChange(activity, type));
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
        try {
            // Enhanced LSPatch compatibility check
            if (LSPatchCompat.isLSPatchEnvironment() && !verifyLSPatchContext(activity)) {
                return; // Skip if LSPatch context verification fails
            }

            WppCore.mCurrentActivity = activity;
            triggerActivityState(activity, WppCore.ActivityChangeState.ChangeType.CREATED);
            WppCore.activities.add(activity);

            // LSPatch specific logging
            if (LSPatchCompat.isLSPatchEnvironment()) {
                android.util.Log.d("WaEnhancer-LSPatch", "Activity created in LSPatch mode: " + activity.getClass().getSimpleName());
            }
        } catch (Exception e) {
            android.util.Log.e("WaEnhancer-WaCallback", "Error in onActivityCreated: " + e.getMessage());
        }
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        try {
            // Enhanced LSPatch compatibility check
            if (LSPatchCompat.isLSPatchEnvironment() && !verifyLSPatchContext(activity)) {
                return; // Skip if LSPatch context verification fails
            }

            WppCore.mCurrentActivity = activity;
            triggerActivityState(activity, WppCore.ActivityChangeState.ChangeType.STARTED);
            WppCore.activities.add(activity);
        } catch (Exception e) {
            android.util.Log.e("WaEnhancer-WaCallback", "Error in onActivityStarted: " + e.getMessage());
        }
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        try {
            // Enhanced LSPatch compatibility check
            if (LSPatchCompat.isLSPatchEnvironment() && !verifyLSPatchContext(activity)) {
                return; // Skip if LSPatch context verification fails
            }

            WppCore.mCurrentActivity = activity;
            WppCore.activities.add(activity);
            triggerActivityState(activity, WppCore.ActivityChangeState.ChangeType.RESUMED);

            // Additional LSPatch status check when WhatsApp is resumed
            if (LSPatchCompat.isLSPatchEnvironment()) {
                verifyWaEnhancerFunctionality(activity);
            }
        } catch (Exception e) {
            android.util.Log.e("WaEnhancer-WaCallback", "Error in onActivityResumed: " + e.getMessage());
        }
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        try {
            triggerActivityState(activity, WppCore.ActivityChangeState.ChangeType.PAUSED);
        } catch (Exception e) {
            android.util.Log.e("WaEnhancer-WaCallback", "Error in onActivityPaused: " + e.getMessage());
        }
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        try {
            triggerActivityState(activity, WppCore.ActivityChangeState.ChangeType.ENDED);
            WppCore.activities.remove(activity);
        } catch (Exception e) {
            android.util.Log.e("WaEnhancer-WaCallback", "Error in onActivityStopped: " + e.getMessage());
        }
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        WppCore.activities.remove(activity);
    }

    /**
     * Verify LSPatch context for activity lifecycle callbacks
     */
    private boolean verifyLSPatchContext(@NonNull Activity activity) {
        try {
            // Verify we're in WhatsApp context
            String packageName = activity.getPackageName();
            if (!"com.whatsapp".equals(packageName) && !"com.whatsapp.w4b".equals(packageName)) {
                android.util.Log.w("WaEnhancer-WaCallback", "Activity not in WhatsApp context: " + packageName);
                return false;
            }
            
            // Verify LSPatch integrity if in LSPatch environment
            if (!LSPatchCompat.validateLSPatchIntegrity()) {
                android.util.Log.w("WaEnhancer-WaCallback", "LSPatch integrity validation failed");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            android.util.Log.e("WaEnhancer-WaCallback", "LSPatch context verification failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Verify WaEnhancer functionality in LSPatch environment
     */
    private void verifyWaEnhancerFunctionality(@NonNull Activity activity) {
        try {
            // Check if WaEnhancer is still functional
            boolean functional = LSPatchService.isWaEnhancerLoaded();
            if (!functional) {
                android.util.Log.w("WaEnhancer-WaCallback", "WaEnhancer functionality verification failed in activity: " + 
                                  activity.getClass().getSimpleName());
            } else {
                android.util.Log.d("WaEnhancer-WaCallback", "WaEnhancer functionality verified in LSPatch");
            }
        } catch (Exception e) {
            android.util.Log.e("WaEnhancer-WaCallback", "Error verifying WaEnhancer functionality: " + e.getMessage());
        }
    }
}
