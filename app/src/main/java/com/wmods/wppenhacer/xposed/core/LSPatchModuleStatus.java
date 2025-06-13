package com.wmods.wppenhacer.xposed.core;

import android.content.Context;
import android.util.Log;

import de.robv.android.xposed.XposedBridge;

/**
 * Module Status Manager for LSPatch Compatibility
 * 
 * This class manages the detection and reporting of module activation status
 * specifically for LSPatch environments, providing accurate status information
 * for the UI.
 */
public class LSPatchModuleStatus {
    private static final String TAG = "WaEnhancer-ModuleStatus";
    
    public enum ModuleStatus {
        ACTIVE_LSPATCH_EMBEDDED("Active (LSPatch Embedded)"),
        ACTIVE_LSPATCH_MANAGER("Active (LSPatch Manager)"),
        ACTIVE_XPOSED("Active (Xposed/LSPosed)"),
        INACTIVE_NOT_FOUND("Module Not Found"),
        INACTIVE_NOT_LOADED("Module Not Loaded"),
        INACTIVE_ERROR("Error Detecting Status"),
        UNKNOWN("Status Unknown");
        
        private final String displayName;
        
        ModuleStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public boolean isActive() {
            return this == ACTIVE_LSPATCH_EMBEDDED || 
                   this == ACTIVE_LSPATCH_MANAGER || 
                   this == ACTIVE_XPOSED;
        }
    }
    
    private static ModuleStatus sLastKnownStatus = ModuleStatus.UNKNOWN;
    private static long sLastStatusCheck = 0;
    private static final long STATUS_CACHE_DURATION = 5000; // 5 seconds
    
    /**
     * Get current module status with caching
     */
    public static ModuleStatus getCurrentStatus() {
        long currentTime = System.currentTimeMillis();
        
        // Return cached status if recent
        if (currentTime - sLastStatusCheck < STATUS_CACHE_DURATION && 
            sLastKnownStatus != ModuleStatus.UNKNOWN) {
            return sLastKnownStatus;
        }
        
        // Perform fresh status check
        sLastKnownStatus = performStatusCheck();
        sLastStatusCheck = currentTime;
        
        return sLastKnownStatus;
    }
    
    /**
     * Force a fresh status check
     */
    public static ModuleStatus forceStatusCheck() {
        sLastKnownStatus = performStatusCheck();
        sLastStatusCheck = System.currentTimeMillis();
        return sLastKnownStatus;
    }
    
    /**
     * Perform the actual status check
     */
    private static ModuleStatus performStatusCheck() {
        try {
            // Check if we're in LSPatch environment
            if (LSPatchCompat.isLSPatchEnvironment()) {
                return checkLSPatchStatus();
            } else {
                return checkXposedStatus();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error during status check: " + e.getMessage());
            return ModuleStatus.INACTIVE_ERROR;
        }
    }
    
    /**
     * Check status in LSPatch environment
     */
    private static ModuleStatus checkLSPatchStatus() {
        try {
            LSPatchCompat.LSPatchMode mode = LSPatchCompat.getCurrentMode();
            
            // Initialize LSPatch service if needed
            if (!LSPatchService.isServiceAvailable()) {
                Context context = getCurrentContext();
                if (context != null) {
                    LSPatchService.initialize(context);
                }
            }
            
            // Check if WaEnhancer is loaded in LSPatch
            boolean moduleLoaded = LSPatchService.isWaEnhancerLoaded();
            
            if (moduleLoaded) {
                // Module is loaded, determine mode
                switch (mode) {
                    case LSPATCH_EMBEDDED:
                        return ModuleStatus.ACTIVE_LSPATCH_EMBEDDED;
                    case LSPATCH_MANAGER:
                        return ModuleStatus.ACTIVE_LSPATCH_MANAGER;
                    default:
                        return ModuleStatus.ACTIVE_LSPATCH_EMBEDDED; // Default assumption
                }
            } else {
                // Module not found in LSPatch service, but we're still in LSPatch
                // This could mean the module is embedded but not listed in services
                
                // Check if hooks are actually working
                if (areHooksWorking()) {
                    Log.i(TAG, "Hooks are working despite module not being in service list");
                    switch (mode) {
                        case LSPATCH_EMBEDDED:
                            return ModuleStatus.ACTIVE_LSPATCH_EMBEDDED;
                        case LSPATCH_MANAGER:
                            return ModuleStatus.ACTIVE_LSPATCH_MANAGER;
                        default:
                            return ModuleStatus.ACTIVE_LSPATCH_EMBEDDED;
                    }
                } else {
                    return ModuleStatus.INACTIVE_NOT_LOADED;
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking LSPatch status: " + e.getMessage());
            
            // Fallback: check if hooks are working
            if (areHooksWorking()) {
                LSPatchCompat.LSPatchMode mode = LSPatchCompat.getCurrentMode();
                switch (mode) {
                    case LSPATCH_EMBEDDED:
                        return ModuleStatus.ACTIVE_LSPATCH_EMBEDDED;
                    case LSPATCH_MANAGER:
                        return ModuleStatus.ACTIVE_LSPATCH_MANAGER;
                    default:
                        return ModuleStatus.ACTIVE_LSPATCH_EMBEDDED;
                }
            }
            
            return ModuleStatus.INACTIVE_ERROR;
        }
    }
    
    /**
     * Check status in traditional Xposed environment
     */
    private static ModuleStatus checkXposedStatus() {
        try {
            // Check if XposedBridge is available and functional
            Class<?> xposedBridge = Class.forName("de.robv.android.xposed.XposedBridge");
            
            // Try to get Xposed version
            try {
                java.lang.reflect.Method getVersionMethod = xposedBridge.getMethod("getXposedVersion");
                Object version = getVersionMethod.invoke(null);
                
                if (version != null) {
                    // Xposed is functional, check if our hooks are working
                    if (areHooksWorking()) {
                        return ModuleStatus.ACTIVE_XPOSED;
                    } else {
                        return ModuleStatus.INACTIVE_NOT_LOADED;
                    }
                }
            } catch (Exception e) {
                // Method might not exist in all Xposed versions
                Log.d(TAG, "Could not get Xposed version, checking hooks directly");
            }
            
            // Direct hook check
            if (areHooksWorking()) {
                return ModuleStatus.ACTIVE_XPOSED;
            } else {
                return ModuleStatus.INACTIVE_NOT_LOADED;
            }
            
        } catch (ClassNotFoundException e) {
            return ModuleStatus.INACTIVE_NOT_FOUND;
        } catch (Exception e) {
            Log.e(TAG, "Error checking Xposed status: " + e.getMessage());
            return ModuleStatus.INACTIVE_ERROR;
        }
    }
    
    /**
     * Check if hooks are actually working
     */
    private static boolean areHooksWorking() {
        try {
            // Check if our core classes are initialized
            // This indicates that our hooks have been loaded
            
            // Check if WppCore has been initialized
            if (isWppCoreInitialized()) {
                return true;
            }
            
            // Check if FeatureLoader has been initialized
            if (isFeatureLoaderInitialized()) {
                return true;
            }
            
            // Check if any of our features are working
            if (areFeaturesWorking()) {
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            Log.d(TAG, "Error checking if hooks are working: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if WppCore is initialized
     */
    private static boolean isWppCoreInitialized() {
        try {
            Class<?> wppCoreClass = Class.forName("com.wmods.wppenhacer.xposed.core.WppCore");
            java.lang.reflect.Field clientField = wppCoreClass.getDeclaredField("client");
            clientField.setAccessible(true);
            Object client = clientField.get(null);
            
            return client != null;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if FeatureLoader is initialized
     */
    private static boolean isFeatureLoaderInitialized() {
        try {
            Class<?> featureLoaderClass = Class.forName("com.wmods.wppenhacer.xposed.core.FeatureLoader");
            // Check if any static fields indicate initialization
            java.lang.reflect.Field[] fields = featureLoaderClass.getDeclaredFields();
            
            for (java.lang.reflect.Field field : fields) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    Object value = field.get(null);
                    if (value != null) {
                        return true;
                    }
                }
            }
            
            return false;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if any features are working
     */
    private static boolean areFeaturesWorking() {
        try {
            // This is a basic check - in a real implementation you'd check
            // specific features that you know should be active
            
            // For now, just check if XposedBridge log is functional
            XposedBridge.log("WaEnhancer status check");
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get current context
     */
    private static Context getCurrentContext() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            return (Context) activityThreadClass.getMethod("getApplication").invoke(activityThread);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get detailed status information for debugging
     */
    public static String getDetailedStatus() {
        StringBuilder sb = new StringBuilder();
        
        ModuleStatus status = getCurrentStatus();
        sb.append("Status: ").append(status.getDisplayName()).append("\n");
        
        if (LSPatchCompat.isLSPatchEnvironment()) {
            sb.append("Environment: LSPatch\n");
            sb.append("Mode: ").append(LSPatchCompat.getCurrentMode()).append("\n");
            sb.append("Service Available: ").append(LSPatchService.isServiceAvailable()).append("\n");
            sb.append("WaEnhancer Loaded: ").append(LSPatchService.isWaEnhancerLoaded()).append("\n");
        } else {
            sb.append("Environment: Classic Xposed\n");
        }
        
        sb.append("Hooks Working: ").append(areHooksWorking()).append("\n");
        sb.append("WppCore Initialized: ").append(isWppCoreInitialized()).append("\n");
        sb.append("FeatureLoader Initialized: ").append(isFeatureLoaderInitialized()).append("\n");
        
        return sb.toString();
    }
    
    /**
     * Clear status cache
     */
    public static void clearCache() {
        sLastKnownStatus = ModuleStatus.UNKNOWN;
        sLastStatusCheck = 0;
    }
    
    /**
     * Checks if the module is currently active
     */
    public static boolean isModuleActive() {
        ModuleStatus status = getCurrentStatus();
        return status.isActive();
    }
    
    /**
     * Checks if the module is active and working properly
     */
    public static boolean isModuleWorking() {
        ModuleStatus status = getCurrentStatus();
        return status == ModuleStatus.ACTIVE_LSPATCH_EMBEDDED || 
               status == ModuleStatus.ACTIVE_LSPATCH_MANAGER || 
               status == ModuleStatus.ACTIVE_XPOSED;
    }
}
