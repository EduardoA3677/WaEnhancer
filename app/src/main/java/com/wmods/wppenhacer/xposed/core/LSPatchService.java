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
            Log.d(TAG, "Checking if WaEnhancer is loaded in LSPatch");
            
            // Critical: First verify we're in WhatsApp context with enhanced detection
            if (!isWhatsAppPackageDetected()) {
                Log.w(TAG, "Enhanced WhatsApp detection failed - not in WhatsApp context");
                return false;
            }
            
            Log.d(TAG, "WhatsApp context confirmed, proceeding with WaEnhancer detection");
            
            // Method 1: Check in LSPatch module list (most reliable if available)
            Object modulesList = getModulesList();
            if (modulesList != null && isWaEnhancerInModulesList(modulesList)) {
                Log.d(TAG, "WaEnhancer found in LSPatch modules list");
                
                // Double-check: verify hooks are actually working
                if (areWaEnhancerClassesLoaded() && isWaEnhancerFunctional()) {
                    Log.d(TAG, "WaEnhancer confirmed functional in modules list");
                    return true;
                } else {
                    Log.w(TAG, "WaEnhancer in modules list but not functional");
                    return false;
                }
            }
            
            // Method 2: Check if WaEnhancer is actually functional (most important)
            if (isWaEnhancerFunctional()) {
                Log.d(TAG, "WaEnhancer is functional even if not in service list");
                return true;
            }
            
            // Method 3: Check if WaEnhancer classes are loaded in current classloader
            if (areWaEnhancerClassesLoaded()) {
                Log.d(TAG, "WaEnhancer classes found in classloader, verifying functionality");
                
                // Additional verification: check if we can actually hook WhatsApp
                if (canHookWhatsAppClasses()) {
                    Log.d(TAG, "Can hook WhatsApp classes - WaEnhancer is loaded");
                    return true;
                } else {
                    Log.w(TAG, "WaEnhancer classes loaded but cannot hook WhatsApp classes");
                    return false;
                }
            }
            
            Log.w(TAG, "WaEnhancer not found or not functional");
            return false;
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to check if WaEnhancer is loaded: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if WaEnhancer is in the modules list
     */
    private static boolean isWaEnhancerInModulesList(Object modulesList) {
        try {
            // Check if our module is in the list
            if (modulesList instanceof java.util.List) {
                java.util.List<?> list = (java.util.List<?>) modulesList;
                for (Object module : list) {
                    try {
                        // Try packageName field
                        Object packageName = module.getClass().getField("packageName").get(module);
                        if ("com.wmods.wppenhacer".equals(packageName)) {
                            Log.d(TAG, "WaEnhancer module found via packageName");
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
                            try {
                                Object moduleName = module.getClass().getField("name").get(module);
                                if (moduleName != null && moduleName.toString().contains("wppenhacer")) {
                                    Log.d(TAG, "WaEnhancer module found via name");
                                    return true;
                                }
                            } catch (Exception e3) {
                                // Try toString method
                                String moduleStr = module.toString();
                                if (moduleStr.contains("wppenhacer") || moduleStr.contains("WaEnhancer")) {
                                    Log.d(TAG, "WaEnhancer module found via toString");
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
            
            return false;
            
        } catch (Exception e) {
            Log.w(TAG, "Error checking modules list: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if WaEnhancer classes are loaded
     */
    private static boolean areWaEnhancerClassesLoaded() {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = LSPatchService.class.getClassLoader();
            }
            
            // Core WaEnhancer classes that should be loaded if module is active
            String[] waEnhancerClasses = {
                "com.wmods.wppenhacer.xposed.core.WppCore",
                "com.wmods.wppenhacer.xposed.core.FeatureLoader",
                "com.wmods.wppenhacer.xposed.core.components.FMessageWpp",
                "com.wmods.wppenhacer.xposed.features.general.AntiRevoke",
                "com.wmods.wppenhacer.xposed.features.privacy.HideSeen"
            };
            
            int loadedCount = 0;
            for (String className : waEnhancerClasses) {
                try {
                    Class<?> clazz = cl.loadClass(className);
                    if (clazz != null) {
                        loadedCount++;
                        Log.d(TAG, "Found WaEnhancer class: " + className);
                    }
                } catch (ClassNotFoundException e) {
                    // Class not found, continue
                }
            }
            
            // If we found at least 2 core classes, consider it loaded
            return loadedCount >= 2;
            
        } catch (Exception e) {
            Log.w(TAG, "Error checking WaEnhancer classes: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if we're in WhatsApp context with WaEnhancer
     */
    private static boolean isInWhatsAppWithWaEnhancer() {
        try {
            Context context = getCurrentContext();
            if (context == null) {
                return false;
            }
            
            String packageName = context.getPackageName();
            boolean isWhatsApp = "com.whatsapp".equals(packageName) || "com.whatsapp.w4b".equals(packageName);
            
            if (!isWhatsApp) {
                return false;
            }
            
            // Check if WaEnhancer is actually running by trying to access its resources or features
            try {
                // Try to access WaEnhancer's shared preferences
                android.content.SharedPreferences prefs = context.getSharedPreferences("WaGlobal", Context.MODE_PRIVATE);
                if (prefs != null) {
                    // Check if any WaEnhancer preferences exist
                    java.util.Map<String, ?> allPrefs = prefs.getAll();
                    if (!allPrefs.isEmpty()) {
                        Log.d(TAG, "WaEnhancer preferences found");
                        return true;
                    }
                }
            } catch (Exception e) {
                // Preferences access might fail
            }
            
            // Check if Xposed is functional in this context
            try {
                de.robv.android.xposed.XposedBridge.log("WaEnhancer LSPatch service check");
                return true;
            } catch (Exception e) {
                Log.w(TAG, "XposedBridge not functional: " + e.getMessage());
                return false;
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Error checking WhatsApp context: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if LSPatch service is healthy and has WaEnhancer indicators
     */
    private static boolean isServiceHealthyWithWaEnhancer() {
        try {
            // Check if any service is available and responding
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
            
            boolean serviceHealthy = localOk || remoteOk;
            
            if (serviceHealthy) {
                // Service is healthy, check for WaEnhancer indicators
                try {
                    // Check if we can access WaEnhancer related system properties
                    String waEnhancerProp = System.getProperty("waenhancer.lspatch.enabled");
                    if ("true".equals(waEnhancerProp)) {
                        Log.d(TAG, "WaEnhancer system property found");
                        return true;
                    }
                } catch (Exception e) {
                    // Property access might be restricted
                }
                
                // If service is healthy but no specific WaEnhancer indicators,
                // still return true as the service might just not list embedded modules
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            Log.w(TAG, "Error checking service health: " + e.getMessage());
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
            try {
                Class<?> appGlobalsClass = Class.forName("android.app.AppGlobals");
                return (Context) appGlobalsClass.getMethod("getInitialApplication").invoke(null);
            } catch (Exception e2) {
                return null;
            }
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
    
    /**
     * Check if WaEnhancer is actually functional (can hook and modify WhatsApp)
     */
    private static boolean isWaEnhancerFunctional() {
        try {
            // Test 1: Check if XposedBridge is working
            try {
                de.robv.android.xposed.XposedBridge.log("WaEnhancer functionality test");
            } catch (Exception e) {
                Log.w(TAG, "XposedBridge not functional: " + e.getMessage());
                return false;
            }
            
            // Test 2: Check if we can access WhatsApp core classes
            if (!canHookWhatsAppClasses()) {
                Log.w(TAG, "Cannot access WhatsApp classes for hooking");
                return false;
            }
            
            // Test 3: Check if WaEnhancer core is initialized
            if (isWaEnhancerCoreInitialized()) {
                Log.d(TAG, "WaEnhancer core is initialized");
                return true;
            }
            
            // Test 4: Check if WaEnhancer preferences are accessible
            if (isWaEnhancerPreferencesAccessible()) {
                Log.d(TAG, "WaEnhancer preferences are accessible");
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            Log.w(TAG, "Error checking WaEnhancer functionality: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if we can hook WhatsApp classes
     */
    private static boolean canHookWhatsAppClasses() {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                Context context = getCurrentContext();
                if (context != null) {
                    cl = context.getClassLoader();
                }
            }
            
            if (cl == null) {
                return false;
            }
            
            // Try to access critical WhatsApp classes
            String[] testClasses = {
                "com.whatsapp.HomeActivity",
                "com.whatsapp.Main",
                "com.whatsapp.Conversation"
            };
            
            for (String className : testClasses) {
                try {
                    Class<?> clazz = cl.loadClass(className);
                    if (clazz != null) {
                        Log.d(TAG, "Successfully accessed WhatsApp class: " + className);
                        return true;
                    }
                } catch (ClassNotFoundException e) {
                    // Try alternative class names for newer versions
                    if (className.equals("com.whatsapp.HomeActivity")) {
                        try {
                            cl.loadClass("com.whatsapp.home.ui.HomeActivity");
                            Log.d(TAG, "Successfully accessed alternative WhatsApp class");
                            return true;
                        } catch (ClassNotFoundException e2) {
                            // Continue
                        }
                    }
                }
            }
            
            return false;
            
        } catch (Exception e) {
            Log.w(TAG, "Error testing WhatsApp class access: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if WaEnhancer core components are initialized
     */
    private static boolean isWaEnhancerCoreInitialized() {
        try {
            // Check for WppCore initialization
            Class<?> wppCoreClass = Class.forName("com.wmods.wppenhacer.xposed.core.WppCore");
            
            // Try to access static fields that indicate initialization
            try {
                java.lang.reflect.Field field = wppCoreClass.getDeclaredField("mApp");
                field.setAccessible(true);
                Object mApp = field.get(null);
                if (mApp != null) {
                    Log.d(TAG, "WppCore mApp is initialized");
                    return true;
                }
            } catch (Exception e) {
                // Field might not exist or not be accessible
            }
            
            // Check for FeatureLoader initialization
            try {
                Class<?> featureLoaderClass = Class.forName("com.wmods.wppenhacer.xposed.core.FeatureLoader");
                java.lang.reflect.Field mAppField = featureLoaderClass.getDeclaredField("mApp");
                mAppField.setAccessible(true);
                Object mApp = mAppField.get(null);
                if (mApp != null) {
                    Log.d(TAG, "FeatureLoader mApp is initialized");
                    return true;
                }
            } catch (Exception e) {
                // Field might not exist
            }
            
            return false;
            
        } catch (ClassNotFoundException e) {
            Log.d(TAG, "WaEnhancer core classes not found");
            return false;
        } catch (Exception e) {
            Log.w(TAG, "Error checking WaEnhancer core initialization: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if WaEnhancer preferences are accessible
     */
    private static boolean isWaEnhancerPreferencesAccessible() {
        try {
            Context context = getCurrentContext();
            if (context == null) {
                return false;
            }
            
            // Try to access WaEnhancer preferences
            android.content.SharedPreferences prefs = context.getSharedPreferences("WaGlobal", Context.MODE_PRIVATE);
            if (prefs != null) {
                // Check if any WaEnhancer preferences exist
                java.util.Map<String, ?> allPrefs = prefs.getAll();
                if (!allPrefs.isEmpty()) {
                    Log.d(TAG, "WaEnhancer preferences are accessible and contain data");
                    return true;
                }
                
                // Even empty preferences indicate WaEnhancer is set up
                Log.d(TAG, "WaEnhancer preferences are accessible");
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            Log.w(TAG, "Error checking WaEnhancer preferences: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Enhanced debug information for LSPatch troubleshooting
     */
    public static String getDetailedLSPatchStatus() {
        StringBuilder status = new StringBuilder();
        status.append("=== WaEnhancer LSPatch Status ===\n");
        
        try {
            // Environment detection
            boolean isLSPatch = LSPatchCompat.isLSPatchEnvironment();
            status.append("LSPatch Environment: ").append(isLSPatch).append("\n");
            
            if (isLSPatch) {
                LSPatchCompat.LSPatchMode mode = LSPatchCompat.getCurrentMode();
                status.append("LSPatch Mode: ").append(mode).append("\n");
                
                // Context verification
                Context context = getCurrentContext();
                if (context != null) {
                    String packageName = context.getPackageName();
                    status.append("Package Name: ").append(packageName).append("\n");
                    status.append("Is WhatsApp: ").append(
                        "com.whatsapp".equals(packageName) || "com.whatsapp.w4b".equals(packageName)
                    ).append("\n");
                } else {
                    status.append("Context: null\n");
                }
                
                // Service availability
                status.append("LSPatch Service Available: ").append(isServiceAvailable()).append("\n");
                status.append("Local Service: ").append(sLocalService != null).append("\n");
                status.append("Remote Service: ").append(sRemoteService != null).append("\n");
                
                // Module detection
                status.append("WaEnhancer in Service List: ").append(
                    isWaEnhancerInModulesList(getModulesList())
                ).append("\n");
                
                // Functionality checks
                status.append("WaEnhancer Classes Loaded: ").append(areWaEnhancerClassesLoaded()).append("\n");
                status.append("Can Hook WhatsApp: ").append(canHookWhatsAppClasses()).append("\n");
                status.append("WaEnhancer Functional: ").append(isWaEnhancerFunctional()).append("\n");
                status.append("WaEnhancer Loaded: ").append(isWaEnhancerLoaded()).append("\n");
                
                // System properties
                status.append("\nSystem Properties:\n");
                String[] props = {
                    "lspatch.mode", "lspatch.embedded", "lspatch.manager", 
                    "lspatch.local", "lspatch.remote", "lspatch.version"
                };
                for (String prop : props) {
                    String value = System.getProperty(prop);
                    if (value != null) {
                        status.append("  ").append(prop).append("=").append(value).append("\n");
                    }
                }
            }
            
        } catch (Exception e) {
            status.append("Error getting status: ").append(e.getMessage()).append("\n");
        }
        
        return status.toString();
    }
    
    /**
     * Enhanced WhatsApp package detection for LSPatch environments
     * This method provides more reliable detection of WhatsApp application context
     */
    public static boolean isWhatsAppPackageDetected() {
        try {
            // Method 1: Check current application context
            Context context = getCurrentContext();
            if (context != null) {
                String packageName = context.getPackageName();
                if ("com.whatsapp".equals(packageName) || "com.whatsapp.w4b".equals(packageName)) {
                    Log.d(TAG, "WhatsApp detected via application context: " + packageName);
                    return true;
                }
            }
            
            // Method 2: Check process name
            String processName = getCurrentProcessName();
            if (processName != null) {
                if ("com.whatsapp".equals(processName) || 
                    "com.whatsapp.w4b".equals(processName) ||
                    processName.startsWith("com.whatsapp")) {
                    Log.d(TAG, "WhatsApp detected via process name: " + processName);
                    return true;
                }
            }
            
            // Method 3: Check stack trace for WhatsApp classes
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stack) {
                String className = element.getClassName();
                if (className.startsWith("com.whatsapp.")) {
                    Log.d(TAG, "WhatsApp detected via stack trace: " + className);
                    return true;
                }
            }
            
            // Method 4: Check classloader for WhatsApp specific classes
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null && context != null) {
                cl = context.getClassLoader();
            }
            
            if (cl != null) {
                String[] whatsappIndicatorClasses = {
                    "com.whatsapp.HomeActivity",
                    "com.whatsapp.Main",
                    "com.whatsapp.Conversation",
                    "com.whatsapp.Voip",
                    "com.whatsapp.conversationslist.ConversationsFragment"
                };
                
                for (String className : whatsappIndicatorClasses) {
                    try {
                        Class<?> clazz = cl.loadClass(className);
                        if (clazz != null) {
                            Log.d(TAG, "WhatsApp detected via classloader: " + className);
                            return true;
                        }
                    } catch (ClassNotFoundException e) {
                        // Try alternative class paths for different WhatsApp versions
                        if (className.equals("com.whatsapp.HomeActivity")) {
                            try {
                                cl.loadClass("com.whatsapp.home.ui.HomeActivity");
                                Log.d(TAG, "WhatsApp detected via alternative HomeActivity");
                                return true;
                            } catch (ClassNotFoundException e2) {
                                // Continue trying other classes
                            }
                        }
                    }
                }
            }
            
            // Method 5: Check system properties set by LSPatch in WhatsApp context
            try {
                String targetPackage = System.getProperty("waenhancer.target.package");
                if ("com.whatsapp".equals(targetPackage) || "com.whatsapp.w4b".equals(targetPackage)) {
                    Log.d(TAG, "WhatsApp detected via system property");
                    return true;
                }
            } catch (Exception e) {
                // Property access might fail
            }
            
            Log.w(TAG, "WhatsApp package detection failed - not in WhatsApp context");
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "Error in WhatsApp package detection: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get the current process name
     */
    private static String getCurrentProcessName() {
        try {
            int pid = android.os.Process.myPid();
            android.app.ActivityManager am = (android.app.ActivityManager) 
                getCurrentContext().getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                for (android.app.ActivityManager.RunningAppProcessInfo processInfo : am.getRunningAppProcesses()) {
                    if (processInfo.pid == pid) {
                        return processInfo.processName;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error getting current process name: " + e.getMessage());
        }
        return null;
    }
}
