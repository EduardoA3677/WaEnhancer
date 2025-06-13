package com.wmods.wppenhacer.xposed.core;

import android.content.Context;
import android.util.Log;

import de.robv.android.xposed.XposedBridge;

/**
 * LSPatch Service Integration for WaEnhancer
 * 
 * This class provides direct integration with LSPatch services to enable
 * proper module detection and functionality in LSPatch environments.
 */
public class LSPatchService {
    private static final String TAG = "WaEnhancer-LSPatchService";
    
    private static Object sLocalService = null;
    private static Object sRemoteService = null;
    private static boolean sInitialized = false;
    
    /**
     * Initialize LSPatch service connections
     */
    public static boolean initialize(Context context) {
        if (sInitialized) {
            return sLocalService != null || sRemoteService != null;
        }
        
        sInitialized = true;
        
        if (!LSPatchCompat.isLSPatchEnvironment()) {
            Log.d(TAG, "Not in LSPatch environment, skipping service initialization");
            return false;
        }
        
        Log.i(TAG, "Initializing LSPatch services");
        
        LSPatchCompat.LSPatchMode mode = LSPatchCompat.getCurrentMode();
        
        try {
            switch (mode) {
                case LSPATCH_EMBEDDED:
                    return initializeLocalService(context);
                    
                case LSPATCH_MANAGER:
                    return initializeRemoteService(context);
                    
                default:
                    // Try both services
                    boolean local = initializeLocalService(context);
                    boolean remote = initializeRemoteService(context);
                    return local || remote;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize LSPatch services: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Initialize local service for embedded mode
     */
    private static boolean initializeLocalService(Context context) {
        try {
            Class<?> serviceClass = Class.forName("org.lsposed.lspatch.service.LocalApplicationService");
            sLocalService = serviceClass.getConstructor(Context.class).newInstance(context);
            
            Log.i(TAG, "LocalApplicationService initialized successfully");
            
            // Verify service is working by getting module list
            try {
                Object modulesList = serviceClass.getMethod("getLegacyModulesList").invoke(sLocalService);
                Log.d(TAG, "LocalApplicationService is functional, modules: " + modulesList);
            } catch (Exception e) {
                Log.w(TAG, "LocalApplicationService may not be fully functional: " + e.getMessage());
            }
            
            return true;
            
        } catch (Exception e) {
            Log.w(TAG, "Could not initialize LocalApplicationService: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Initialize remote service for manager mode
     */
    private static boolean initializeRemoteService(Context context) {
        try {
            Class<?> serviceClass = Class.forName("org.lsposed.lspatch.service.RemoteApplicationService");
            sRemoteService = serviceClass.getConstructor(Context.class).newInstance(context);
            
            Log.i(TAG, "RemoteApplicationService initialized successfully");
            
            // Verify service is working
            try {
                Object modulesList = serviceClass.getMethod("getLegacyModulesList").invoke(sRemoteService);
                Log.d(TAG, "RemoteApplicationService is functional, modules: " + modulesList);
            } catch (Exception e) {
                Log.w(TAG, "RemoteApplicationService may not be fully functional: " + e.getMessage());
            }
            
            return true;
            
        } catch (Exception e) {
            Log.w(TAG, "Could not initialize RemoteApplicationService: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if any LSPatch service is available
     */
    public static boolean isServiceAvailable() {
        return sLocalService != null || sRemoteService != null;
    }
    
    /**
     * Get active service (local preferred over remote)
     */
    public static Object getActiveService() {
        return sLocalService != null ? sLocalService : sRemoteService;
    }
    
    /**
     * Get local service specifically
     */
    public static Object getLocalService() {
        return sLocalService;
    }
    
    /**
     * Get remote service specifically
     */
    public static Object getRemoteService() {
        return sRemoteService;
    }
    
    /**
     * Get module list from active service
     */
    public static Object getModulesList() {
        Object service = getActiveService();
        if (service == null) {
            return null;
        }
        
        try {
            return service.getClass().getMethod("getLegacyModulesList").invoke(service);
        } catch (Exception e) {
            Log.w(TAG, "Failed to get modules list: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if WaEnhancer module is loaded
     */
    public static boolean isWaEnhancerLoaded() {
        try {
            Object modulesList = getModulesList();
            if (modulesList == null) {
                return false;
            }
            
            // Check if our module is in the list
            if (modulesList instanceof java.util.List) {
                java.util.List<?> list = (java.util.List<?>) modulesList;
                for (Object module : list) {
                    try {
                        Object packageName = module.getClass().getField("packageName").get(module);
                        if ("com.wmods.wppenhacer".equals(packageName)) {
                            Log.d(TAG, "WaEnhancer module found in LSPatch modules list");
                            return true;
                        }
                    } catch (Exception e) {
                        // Try different field names
                        try {
                            Object apkPath = module.getClass().getField("apkPath").get(module);
                            if (apkPath != null && apkPath.toString().contains("wppenhacer")) {
                                Log.d(TAG, "WaEnhancer module found via APK path");
                                return true;
                            }
                        } catch (Exception e2) {
                            // Ignore field access errors
                        }
                    }
                }
            }
            
            return false;
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to check if WaEnhancer is loaded: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get preferences path from LSPatch service
     */
    public static String getPreferencesPath(String packageName) {
        Object service = getActiveService();
        if (service == null) {
            return null;
        }
        
        try {
            return (String) service.getClass().getMethod("getPrefsPath", String.class).invoke(service, packageName);
        } catch (Exception e) {
            Log.w(TAG, "Failed to get preferences path: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Force module activation status check
     */
    public static boolean forceCheckModuleStatus() {
        if (!sInitialized) {
            return false;
        }
        
        // Check if services are responsive
        boolean localOk = false;
        boolean remoteOk = false;
        
        if (sLocalService != null) {
            try {
                Object result = sLocalService.getClass().getMethod("getLegacyModulesList").invoke(sLocalService);
                localOk = result != null;
            } catch (Exception e) {
                Log.w(TAG, "Local service health check failed: " + e.getMessage());
            }
        }
        
        if (sRemoteService != null) {
            try {
                Object result = sRemoteService.getClass().getMethod("getLegacyModulesList").invoke(sRemoteService);
                remoteOk = result != null;
            } catch (Exception e) {
                Log.w(TAG, "Remote service health check failed: " + e.getMessage());
            }
        }
        
        boolean isHealthy = localOk || remoteOk;
        
        if (isHealthy) {
            XposedBridge.log("LSPatch services are healthy and responsive");
        } else {
            XposedBridge.log("LSPatch services are not responding properly");
        }
        
        return isHealthy;
    }
    
    /**
     * Cleanup services
     */
    public static void cleanup() {
        sLocalService = null;
        sRemoteService = null;
        sInitialized = false;
        Log.d(TAG, "LSPatch services cleaned up");
    }
}
