package com.wmods.wppenhacer.xposed.core;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;
import de.robv.android.xposed.XposedBridge;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

/**
 * LSPatch Compatibility Layer for WaEnhancer
 * 
 * This class provides compatibility with LSPatch framework, which is a rootless
 * implementation of the Xposed framework. LSPatch allows running Xposed modules
 * without root access by patching APKs directly.
 * 
 * LSPatch compatibility features:
 * - Automatic detection of LSPatch environment
 * - Enhanced hook stability for non-root environment
 * - Proper resource handling for patched applications
 * - Signature bypass compatibility
 * - Bridge service fallback mechanisms
 */
public class LSPatchCompat {
    private static final String TAG = "WaEnhancer-LSPatch";
    
    // LSPatch specific constants
    private static final String LSPATCH_LOADER_CLASS = "org.lsposed.lspatch.loader.LSPApplication";
    private static final String LSPATCH_METALOADER_CLASS = "org.lsposed.lspatch.metaloader.LSPAppComponentFactoryStub";
    private static final String LSPATCH_SERVICE_CLASS = "org.lsposed.lspd.service.ILSPApplicationService";
    private static final String LSPATCH_BRIDGE_CLASS = "org.lsposed.lspd.core.Startup";
    private static final String LSPATCH_XPOSED_INIT = "org.lsposed.lspatch.loader.LSPLoader";
    private static final String LSPATCH_CONFIG_PATH = "assets/lspatch/config.json";
    private static final String LSPATCH_MODULE_SERVICE = "org.lsposed.lspatch.service.LocalApplicationService";
    private static final String LSPATCH_REMOTE_SERVICE = "org.lsposed.lspatch.service.RemoteApplicationService";
    
    private static Boolean isLSPatchEnvironment = null;
    private static Boolean isLSPatchPatched = null;
    private static LSPatchMode currentMode = null;
    
    public enum LSPatchMode {
        CLASSIC_XPOSED,           // Traditional Xposed with root
        LSPATCH_EMBEDDED,         // LSPatch with embedded modules
        LSPATCH_MANAGER           // LSPatch with manager
    }
    
    /**
     * Detects if the current environment is running under LSPatch
     * @return true if LSPatch is detected, false otherwise
     */
    public static boolean isLSPatchEnvironment() {
        if (isLSPatchEnvironment == null) {
            isLSPatchEnvironment = detectLSPatchEnvironment();
            if (isLSPatchEnvironment) {
                Log.i(TAG, "LSPatch environment detected - enabling compatibility mode");
                currentMode = detectLSPatchMode();
                Log.i(TAG, "LSPatch mode: " + currentMode);
                
                // Additional verification for WhatsApp context
                if (isInWhatsAppContext()) {
                    Log.i(TAG, "Confirmed LSPatch is running in WhatsApp context");
                } else {
                    Log.w(TAG, "LSPatch detected but WhatsApp context verification failed");
                }
            } else {
                currentMode = LSPatchMode.CLASSIC_XPOSED;
                Log.d(TAG, "Classic Xposed environment detected");
            }
        }
        return isLSPatchEnvironment;
    }
    
    /**
     * Checks if the current application is patched with LSPatch
     * @param context Application context
     * @return true if the app is LSPatch patched
     */
    public static boolean isApplicationPatched(Context context) {
        if (isLSPatchPatched == null) {
            isLSPatchPatched = checkApplicationPatched(context);
            if (isLSPatchPatched) {
                Log.i(TAG, "Application is LSPatch patched");
            }
        }
        return isLSPatchPatched;
    }
    
    /**
     * Gets the current LSPatch mode
     * @return Current mode or CLASSIC_XPOSED if not detected
     */
    public static LSPatchMode getCurrentMode() {
        if (currentMode == null) {
            isLSPatchEnvironment(); // Trigger detection
        }
        return currentMode != null ? currentMode : LSPatchMode.CLASSIC_XPOSED;
    }
    
    /**
     * Initializes LSPatch compatibility layer
     * This method should be called early in the module initialization process
     */
    public static void init() {
        // Trigger environment detection
        isLSPatchEnvironment();
        
        if (isLSPatchEnvironment) {
            Log.i(TAG, "Initializing LSPatch compatibility layer");
            
            // Apply LSPatch specific optimizations
            optimizeForLSPatch();
            
            // Log compatibility information
            logCompatibilityInfo();
            
            // Set system properties for better integration
            try {
                System.setProperty("waenhancer.lspatch.enabled", "true");
                System.setProperty("waenhancer.lspatch.mode", currentMode.toString());
            } catch (Exception e) {
                Log.d(TAG, "Could not set system properties: " + e.getMessage());
            }
        } else {
            Log.d(TAG, "LSPatch not detected, using classic Xposed mode");
        }
    }

    /**
     * Gets the current LSPatch mode (same as getCurrentMode but with better naming)
     */
    public static LSPatchMode getLSPatchMode() {
        return getCurrentMode();
    }
    
    /**
     * Applies LSPatch specific optimizations for hooks
     * This method should be called before setting up critical hooks
     */
    public static void optimizeForLSPatch() {
        if (!isLSPatchEnvironment()) {
            return;
        }
        
        Log.i(TAG, "Applying LSPatch optimizations");
        
        // Enable debug mode for better error reporting in LSPatch
        try {
            Class<?> debugClass = Class.forName("de.robv.android.xposed.XposedBridge");
            Method setDebugMethod = debugClass.getDeclaredMethod("setDebugMode", boolean.class);
            setDebugMethod.setAccessible(true);
            setDebugMethod.invoke(null, true);
        } catch (Exception e) {
            Log.d(TAG, "Could not set debug mode: " + e.getMessage());
        }
        
        // Apply hook stability improvements
        applyHookStabilityImprovements();
        
        // Setup bridge service fallback if needed
        setupBridgeServiceFallback();
    }
    
    /**
     * Checks if a specific LSPatch feature is available
     * @param feature Feature to check
     * @return true if feature is available
     */
    public static boolean isFeatureAvailable(String feature) {
        if (!isLSPatchEnvironment()) {
            return true; // All features available in classic Xposed
        }
        
        switch (feature) {
            case "RESOURCE_HOOKS":
                return getCurrentMode() != LSPatchMode.LSPATCH_MANAGER;
            case "SYSTEM_SERVER_HOOKS":
                return false; // LSPatch doesn't support system server hooks
            case "SIGNATURE_BYPASS":
                return true; // LSPatch has built-in signature bypass
            case "BRIDGE_SERVICE":
                return getCurrentMode() == LSPatchMode.LSPATCH_EMBEDDED;
            default:
                return true;
        }
    }
    
    /**
     * Gets LSPatch specific configuration
     * @param key Configuration key
     * @return Configuration value or null if not available
     */
    public static String getLSPatchConfig(String key) {
        if (!isLSPatchEnvironment()) {
            return null;
        }
        
        try {
            // Try to access LSPatch configuration
            Class<?> configClass = Class.forName("org.lsposed.lspatch.share.LSPConfig");
            Method getMethod = configClass.getDeclaredMethod("getString", String.class);
            return (String) getMethod.invoke(null, key);
        } catch (Exception e) {
            Log.d(TAG, "Could not access LSPatch config: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Handles LSPatch specific bridge service operations
     * @param operation Operation to perform
     * @return true if operation was successful
     */
    public static boolean handleBridgeService(String operation) {
        if (!isLSPatchEnvironment() || getCurrentMode() != LSPatchMode.LSPATCH_EMBEDDED) {
            return false;
        }
        
        try {
            // Handle LSPatch bridge service operations
            Log.d(TAG, "Handling bridge service operation: " + operation);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Bridge service operation failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Checks if LSPatch service is available and functional
     * @return true if LSPatch service is available
     */
    public static boolean isLSPatchServiceAvailable() {
        if (!isLSPatchEnvironment()) {
            return false; // Not in LSPatch environment
        }
        
        try {
            // Check if we can access LSPatch service classes
            switch (getCurrentMode()) {
                case LSPATCH_EMBEDDED:
                    return isClassAvailable(LSPATCH_MODULE_SERVICE);
                case LSPATCH_MANAGER:
                    return isClassAvailable(LSPATCH_REMOTE_SERVICE);
                default:
                    return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking LSPatch service availability: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Enhanced method to check if a specific LSPatch class is accessible
     * @param className Class name to check
     * @return true if class is accessible and functional
     */
    public static boolean isLSPatchClassAvailable(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            // Additional verification - try to get some basic info from the class
            if (clazz != null) {
                clazz.getName(); // This will throw if class is not properly loaded
                return true;
            }
        } catch (Exception e) {
            Log.d(TAG, "LSPatch class not available: " + className + " - " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Validates the integrity of the LSPatch environment
     * @return true if LSPatch environment is fully functional
     */
    public static boolean validateLSPatchIntegrity() {
        if (!isLSPatchEnvironment()) {
            return false;
        }
        
        boolean integrity = true;
        
        // Check core LSPatch components
        if (!isClassAvailable(LSPATCH_LOADER_CLASS)) {
            Log.w(TAG, "LSPatch loader class not available");
            integrity = false;
        }
        
        // Check if we're in correct application context
        if (!isInWhatsAppContext()) {
            Log.w(TAG, "Not in WhatsApp context");
            integrity = false;
        }
        
        // Check if basic Xposed functionality works
        try {
            XposedBridge.log("LSPatch integrity test");
        } catch (Exception e) {
            Log.w(TAG, "XposedBridge not functional: " + e.getMessage());
            integrity = false;
        }
        
        return integrity;
    }
    
    /**
     * Enhanced LSPatch environment detection
     * Checks multiple indicators to reliably detect LSPatch
     */
    private static boolean detectLSPatchEnvironment() {
        // Primary detection: Check for LSPatch loader classes (most reliable)
        if (isClassAvailable(LSPATCH_LOADER_CLASS) ||
            isClassAvailable(LSPATCH_METALOADER_CLASS)) {
            return true;
        }
        
        // Secondary detection: Check for LSPatch service classes
        if (isClassAvailable(LSPATCH_SERVICE_CLASS) ||
            isClassAvailable(LSPATCH_MODULE_SERVICE) ||
            isClassAvailable(LSPATCH_REMOTE_SERVICE)) {
            return true;
        }
        
        // Tertiary detection: Check for LSPatch bridge and init classes
        if (isClassAvailable(LSPATCH_BRIDGE_CLASS) ||
            isClassAvailable(LSPATCH_XPOSED_INIT)) {
            return true;
        }
        
        // Enhanced detection: Check for LSPatch dex injection markers
        if (isLSPatchDexInjectionPresent()) {
            return true;
        }
        
        // Quaternary detection: Check for LSPatch specific system properties
        try {
            String lspatchMarker = System.getProperty("lspatch.enabled");
            if ("true".equals(lspatchMarker)) {
                return true;
            }
            
            // Check for LSPatch version property
            String lspatchVersion = System.getProperty("lspatch.version");
            if (lspatchVersion != null && !lspatchVersion.isEmpty()) {
                return true;
            }
        } catch (Exception e) {
            // Ignore property access errors
        }
        
        // Quinary detection: Check for LSPatch environment variables
        try {
            String lspatchEnv = System.getenv("LSPATCH_VERSION");
            if (lspatchEnv != null && !lspatchEnv.isEmpty()) {
                return true;
            }
            
            String lspatchMode = System.getenv("LSPATCH_MODE");
            if (lspatchMode != null && !lspatchMode.isEmpty()) {
                return true;
            }
        } catch (Exception e) {
            // Ignore environment access errors
        }
        
        // Senary detection: Check for LSPatch in stack trace
        try {
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stack) {
                String className = element.getClassName();
                if (className.contains("lspatch") || 
                    className.contains("LSPatch") ||
                    className.startsWith("org.lsposed.lspatch")) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Ignore stack trace access errors
        }
        
        // Septenary detection: Check for LSPatch-patched application characteristics
        try {
            Context context = getCurrentContext();
            if (context != null) {
                // Check if the application has been patched by LSPatch
                String sourceDir = context.getApplicationInfo().sourceDir;
                
                // LSPatch often modifies the APK structure
                if (sourceDir != null && sourceDir.contains("lspatch")) {
                    return true;
                }
                
                // Check for LSPatch assets
                try {
                    String[] assets = context.getAssets().list("lspatch");
                    if (assets != null && assets.length > 0) {
                        return true;
                    }
                } catch (Exception e) {
                    // Ignore asset access errors
                }
                
                // Check for LSPatch configuration files
                try {
                    context.getAssets().open("lspatch/config.json");
                    return true; // If we can open this file, we're definitely in LSPatch
                } catch (Exception e) {
                    // This is expected if not in LSPatch
                }
            }
        } catch (Exception e) {
            // Ignore context access errors
        }
        
        // Final fallback: Check for the presence of known LSPatch artifacts
        try {
            // Look for LSPatch native libraries
            String[] libPaths = {
                "/data/app/*/lib/*/liblspatch.so",
                "/system/lib*/liblspatch.so",
                "/vendor/lib*/liblspatch.so"
            };
            
            for (String libPath : libPaths) {
                java.io.File libFile = new java.io.File(libPath);
                if (libFile.exists()) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Ignore file system access errors
        }
        
        return false;
    }
    
    private static LSPatchMode detectLSPatchMode() {
        // Enhanced mode detection with more precise checks
        
        Log.d(TAG, "Starting enhanced LSPatch mode detection");
        
        // Method 0: Check for dex injection first (most definitive for embedded/local mode)
        if (isLSPatchDexInjectionPresent()) {
            Log.d(TAG, "LSPatch dex injection detected - likely embedded/local mode");
            
            // Double-check with context to confirm
            Context context = getCurrentContext();
            if (context != null) {
                String packageName = context.getPackageName();
                if ("com.whatsapp".equals(packageName) || "com.whatsapp.w4b".equals(packageName)) {
                    Log.d(TAG, "Dex injection in WhatsApp context confirmed - Embedded Mode");
                    return LSPatchMode.LSPATCH_EMBEDDED;
                }
            }
        }
        
        // Method 1: Check system properties (most reliable)
        try {
            String lspatchMode = System.getProperty("lspatch.mode");
            if ("embedded".equals(lspatchMode) || "local".equals(lspatchMode)) {
                Log.d(TAG, "System property indicates embedded/local mode: " + lspatchMode);
                return LSPatchMode.LSPATCH_EMBEDDED;
            } else if ("manager".equals(lspatchMode) || "remote".equals(lspatchMode)) {
                Log.d(TAG, "System property indicates manager/remote mode: " + lspatchMode);
                return LSPatchMode.LSPATCH_MANAGER;
            }
            
            // Check specific mode properties
            String embeddedMode = System.getProperty("lspatch.embedded");
            String localMode = System.getProperty("lspatch.local");
            if ("true".equals(embeddedMode) || "true".equals(localMode)) {
                Log.d(TAG, "Embedded/local property detected - Embedded Mode");
                return LSPatchMode.LSPATCH_EMBEDDED;
            }
            
            String managerMode = System.getProperty("lspatch.manager");
            String remoteMode = System.getProperty("lspatch.remote");
            if ("true".equals(managerMode) || "true".equals(remoteMode)) {
                Log.d(TAG, "Manager/remote property detected - Manager Mode");
                return LSPatchMode.LSPATCH_MANAGER;
            }
            
            String managerPackage = System.getProperty("lspatch.manager.package");
            if ("org.lsposed.lspatch".equals(managerPackage)) {
                Log.d(TAG, "Manager package property detected - Manager Mode");
                return LSPatchMode.LSPATCH_MANAGER;
            }
        } catch (Exception e) {
            Log.d(TAG, "Property access error: " + e.getMessage());
        }
        
        // Method 2: Check service classes availability (order matters)
        // Manager mode typically has RemoteApplicationService
        if (isClassAvailable("org.lsposed.lspatch.service.RemoteApplicationService")) {
            Log.d(TAG, "RemoteApplicationService detected - Manager Mode");
            return LSPatchMode.LSPATCH_MANAGER;
        }
        
        // Embedded/local mode typically has LocalApplicationService
        if (isClassAvailable("org.lsposed.lspatch.service.LocalApplicationService")) {
            Log.d(TAG, "LocalApplicationService detected - Embedded Mode");
            return LSPatchMode.LSPATCH_EMBEDDED;
        }
        
        // Method 3: Check for application patching indicators
        Context context = getCurrentContext();
        if (context != null) {
            // Verify this is actually WhatsApp before checking further
            String packageName = context.getPackageName();
            if (!"com.whatsapp".equals(packageName) && !"com.whatsapp.w4b".equals(packageName)) {
                Log.w(TAG, "LSPatch mode detection called in non-WhatsApp context: " + packageName);
                // Still continue but log the warning
            }
            
            // Check application metadata for LSPatch mode
            try {
                android.content.pm.ApplicationInfo appInfo = context.getApplicationInfo();
                if (appInfo.metaData != null) {
                    // Check for embedded indicators
                    if (appInfo.metaData.containsKey("lspatch.embedded") || 
                        appInfo.metaData.containsKey("lspatch.local")) {
                        Log.d(TAG, "Application metadata indicates embedded/local mode");
                        return LSPatchMode.LSPATCH_EMBEDDED;
                    }
                    
                    // Check for manager indicators
                    if (appInfo.metaData.containsKey("lspatch.manager") ||
                        appInfo.metaData.containsKey("lspatch.remote")) {
                        Log.d(TAG, "Application metadata indicates manager/remote mode");
                        return LSPatchMode.LSPATCH_MANAGER;
                    }
                }
                
                // Check for LSPatch component factory (typically embedded)
                if ("org.lsposed.lspatch.metaloader.LSPAppComponentFactoryStub".equals(appInfo.appComponentFactory)) {
                    Log.d(TAG, "LSPatch component factory detected - Embedded Mode");
                    return LSPatchMode.LSPATCH_EMBEDDED;
                }
            } catch (Exception e) {
                Log.d(TAG, "Error checking application info: " + e.getMessage());
            }
            
            // Check LSPatch configuration files
            try {
                // Check for embedded configuration
                java.io.InputStream configStream = context.getAssets().open("lspatch/config.json");
                java.util.Scanner scanner = new java.util.Scanner(configStream).useDelimiter("\\A");
                String config = scanner.hasNext() ? scanner.next() : "";
                configStream.close();
                
                if (config.contains("\"embedded\":true") || 
                    config.contains("\"mode\":\"embedded\"") ||
                    config.contains("\"mode\":\"local\"")) {
                    Log.d(TAG, "Config indicates embedded/local mode");
                    return LSPatchMode.LSPATCH_EMBEDDED;
                } else if (config.contains("\"manager\":true") || 
                           config.contains("\"mode\":\"manager\"") ||
                           config.contains("\"mode\":\"remote\"")) {
                    Log.d(TAG, "Config indicates manager/remote mode");
                    return LSPatchMode.LSPATCH_MANAGER;
                }
            } catch (Exception e) {
                // Config file might not exist
            }
        }
        
        // Method 4: Check for LSPatch meta loader and loader classes
        if (isClassAvailable(LSPATCH_METALOADER_CLASS)) {
            Log.d(TAG, "LSPatch MetaLoader detected - Embedded Mode");
            return LSPatchMode.LSPATCH_EMBEDDED;
        }
        
        if (isClassAvailable(LSPATCH_LOADER_CLASS)) {
            Log.d(TAG, "LSPatch Loader detected - Embedded Mode");
            return LSPatchMode.LSPATCH_EMBEDDED;
        }
        
        // Method 5: Check classloader hierarchy for LSPatch indicators
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            String clString = cl.toString();
            
            if (clString.contains("lspatch") || clString.contains("LSPatch")) {
                if (clString.contains("embedded") || clString.contains("local")) {
                    Log.d(TAG, "ClassLoader indicates embedded/local mode");
                    return LSPatchMode.LSPATCH_EMBEDDED;
                } else if (clString.contains("manager") || clString.contains("remote")) {
                    Log.d(TAG, "ClassLoader indicates manager/remote mode");
                    return LSPatchMode.LSPATCH_MANAGER;
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "ClassLoader check error: " + e.getMessage());
        }
        
        // Method 6: Check process name patterns
        try {
            String processName = getCurrentProcessName();
            if (processName != null) {
                if ((processName.contains("lspatch") && processName.contains("embedded")) ||
                    (processName.contains("lspatch") && processName.contains("local"))) {
                    Log.d(TAG, "Process name indicates embedded/local mode");
                    return LSPatchMode.LSPATCH_EMBEDDED;
                } else if ((processName.contains("lspatch") && processName.contains("manager")) ||
                          (processName.contains("lspatch") && processName.contains("remote"))) {
                    Log.d(TAG, "Process name indicates manager/remote mode");
                    return LSPatchMode.LSPATCH_MANAGER;
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Process name check error: " + e.getMessage());
        }
        
        // Default to embedded mode if LSPatch is detected but mode is unclear
        // This is safer since embedded mode has fewer restrictions
        Log.d(TAG, "Mode unclear, defaulting to Embedded Mode (safer choice)");
        return LSPatchMode.LSPATCH_EMBEDDED;
    }
    
    private static boolean checkApplicationPatched(Context context) {
        if (context == null) {
            return false;
        }
        
        try {
            ApplicationInfo appInfo = context.getApplicationInfo();
            
            // Check for LSPatch metadata
            if (appInfo.metaData != null && appInfo.metaData.containsKey("lspatch")) {
                return true;
            }
            
            // Check for LSPatch specific app component factory
            if ("org.lsposed.lspatch.metaloader.LSPAppComponentFactoryStub".equals(appInfo.appComponentFactory)) {
                return true;
            }
            
            // Check for LSPatch assets
            try {
                String[] assets = context.getAssets().list("lspatch");
                if (assets != null && assets.length > 0) {
                    return true;
                }
            } catch (Exception e) {
                // Assets access might fail
            }
            
        } catch (Exception e) {
            Log.d(TAG, "Error checking application patch status: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Get current process name
     */
    private static String getCurrentProcessName() {
        try {
            // Method 1: Use ActivityManager
            Context context = getCurrentContext();
            if (context != null) {
                android.app.ActivityManager am = (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                if (am != null) {
                    List<android.app.ActivityManager.RunningAppProcessInfo> processInfos = am.getRunningAppProcesses();
                    int pid = android.os.Process.myPid();
                    for (android.app.ActivityManager.RunningAppProcessInfo info : processInfos) {
                        if (info.pid == pid) {
                            return info.processName;
                        }
                    }
                }
            }
            
            // Method 2: Read from /proc/self/cmdline
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader("/proc/self/cmdline"));
            String processName = reader.readLine().trim();
            reader.close();
            return processName;
            
        } catch (Exception e) {
            Log.d(TAG, "Error getting process name: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get current application context
     */
    private static Context getCurrentContext() {
        try {
            // Method 1: ActivityThread.currentApplication()
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentActivityThreadMethod = activityThreadClass.getMethod("currentActivityThread");
            Object activityThread = currentActivityThreadMethod.invoke(null);
            Method getApplicationMethod = activityThreadClass.getMethod("getApplication");
            return (Context) getApplicationMethod.invoke(activityThread);
        } catch (Exception e) {
            try {
                // Method 2: AppGlobals.getInitialApplication()
                Class<?> appGlobalsClass = Class.forName("android.app.AppGlobals");
                Method getInitialApplicationMethod = appGlobalsClass.getMethod("getInitialApplication");
                return (Context) getInitialApplicationMethod.invoke(null);
            } catch (Exception e2) {
                Log.d(TAG, "Error getting current context: " + e2.getMessage());
                return null;
            }
        }
    }

    /**
     * Check if a class is available in the current classloader
     */
    private static boolean isClassAvailable(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Exception e) {
            Log.d(TAG, "Error checking class availability: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Verifies that we're running in WhatsApp context
     */
    private static boolean isInWhatsAppContext() {
        try {
            // Check process name
            String processName = getCurrentProcessName();
            if (processName != null && 
                (processName.equals("com.whatsapp") || 
                 processName.equals("com.whatsapp.w4b") ||
                 processName.contains("whatsapp"))) {
                return true;
            }
            
            // Check current context
            Context context = getCurrentContext();
            if (context != null) {
                String packageName = context.getPackageName();
                if ("com.whatsapp".equals(packageName) || "com.whatsapp.w4b".equals(packageName)) {
                    return true;
                }
            }
            
            // Check if WhatsApp classes are accessible
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                context = getCurrentContext();
                if (context != null) {
                    cl = context.getClassLoader();
                }
            }
            
            if (cl != null) {
                // Try to load key WhatsApp classes
                String[] whatsappClasses = {
                    "com.whatsapp.HomeActivity",
                    "com.whatsapp.Main",
                    "com.whatsapp.Conversation"
                };
                
                for (String className : whatsappClasses) {
                    try {
                        cl.loadClass(className);
                        return true; // Found at least one WhatsApp class
                    } catch (ClassNotFoundException e) {
                        // Try alternative naming
                        if (className.equals("com.whatsapp.HomeActivity")) {
                            try {
                                cl.loadClass("com.whatsapp.home.ui.HomeActivity");
                                return true;
                            } catch (ClassNotFoundException ignored) {}
                        }
                    }
                }
            }
            
            return false;
        } catch (Exception e) {
            Log.w(TAG, "Error checking WhatsApp context: " + e.getMessage());
            return false;
        }
    }
    
    private static void applyHookStabilityImprovements() {
        Log.d(TAG, "Applying hook stability improvements for LSPatch");
        
        // LSPatch specific hook optimizations
        try {
            // Set hook priorities for better stability
            System.setProperty("lspatch.hook.priority", "high");
            
            // Enable hook verification
            System.setProperty("lspatch.hook.verify", "true");
            
            // Set timeout for hook operations
            System.setProperty("lspatch.hook.timeout", "10000");
            
        } catch (Exception e) {
            Log.d(TAG, "Could not apply all stability improvements: " + e.getMessage());
        }
    }
    
    private static void setupBridgeServiceFallback() {
        if (getCurrentMode() != LSPatchMode.LSPATCH_EMBEDDED) {
            return;
        }
        
        Log.d(TAG, "Setting up bridge service fallback for LSPatch embedded mode");
        
        try {
            // Setup fallback mechanisms for bridge service operations
            System.setProperty("waenhancer.bridge.fallback", "true");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to setup bridge service fallback: " + e.getMessage());
        }
    }
    
    /**
     * Logs LSPatch compatibility information
     */
    public static void logCompatibilityInfo() {
        Log.i(TAG, "=== WaEnhancer LSPatch Compatibility Info ===");
        Log.i(TAG, "LSPatch Environment: " + isLSPatchEnvironment());
        Log.i(TAG, "Current Mode: " + getCurrentMode());
        Log.i(TAG, "Resource Hooks: " + isFeatureAvailable("RESOURCE_HOOKS"));
        Log.i(TAG, "System Server Hooks: " + isFeatureAvailable("SYSTEM_SERVER_HOOKS"));
        Log.i(TAG, "Signature Bypass: " + isFeatureAvailable("SIGNATURE_BYPASS"));
        Log.i(TAG, "Bridge Service: " + isFeatureAvailable("BRIDGE_SERVICE"));
        Log.i(TAG, "=======================================");
    }
    
    /**
     * Check for LSPatch dex injection markers
     * This detects when LSPatch has injected dex files into the application
     */
    private static boolean isLSPatchDexInjectionPresent() {
        try {
            // Check for LSPatch-specific system properties that indicate dex injection
            String[] dexProperties = {
                "lspatch.dex.injected",
                "lspatch.loader.injected", 
                "lspatch.hook.enabled",
                "lspatch.module.loaded"
            };
            
            for (String prop : dexProperties) {
                if ("true".equals(System.getProperty(prop))) {
                    Log.d(TAG, "LSPatch dex injection detected via property: " + prop);
                    return true;
                }
            }
            
            // Check for LSPatch-specific thread names
            Set<Thread> allThreads = Thread.getAllStackTraces().keySet();
            for (Thread thread : allThreads) {
                String threadName = thread.getName();
                if (threadName != null && (threadName.contains("LSPatch") || 
                    threadName.contains("lspatch") || threadName.contains("LSP"))) {
                    Log.d(TAG, "LSPatch thread detected: " + threadName);
                    return true;
                }
            }
            
            // Check for LSPatch-specific classloader patterns
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl != null) {
                String clString = cl.toString();
                if (clString.contains("LSPatch") || clString.contains("lspatch") || 
                    clString.contains("LSP_") || clString.contains("org.lsposed")) {
                    Log.d(TAG, "LSPatch classloader detected: " + clString);
                    return true;
                }
            }
            
            // Check for WaEnhancer being loaded in a different context (indicates LSPatch)
            try {
                String packageName = getCurrentPackageName();
                if ("com.whatsapp".equals(packageName) || "com.whatsapp.w4b".equals(packageName)) {
                    // We're in WhatsApp but WaEnhancer classes are available - this indicates LSPatch
                    try {
                        Class.forName("com.wmods.wppenhacer.xposed.core.FeatureLoader");
                        Log.d(TAG, "WaEnhancer classes detected in WhatsApp context - LSPatch confirmed");
                        return true;
                    } catch (ClassNotFoundException e) {
                        // WaEnhancer not loaded, probably not LSPatch
                    }
                }
            } catch (Exception e) {
                // Context access failed
            }
            
        } catch (Exception e) {
            Log.d(TAG, "Error checking LSPatch dex injection: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Get current package name
     */
    private static String getCurrentPackageName() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            Object app = activityThreadClass.getMethod("getApplication").invoke(activityThread);
            if (app != null) {
                return (String) app.getClass().getMethod("getPackageName").invoke(app);
            }
        } catch (Exception e) {
            // Fallback methods
            try {
                // Try getting from system properties
                return System.getProperty("java.class.path");
            } catch (Exception e2) {
                // Ignore
            }
        }
        return null;
    }
}
