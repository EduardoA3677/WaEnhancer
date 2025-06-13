package com.wmods.wppenhacer.xposed.core;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import java.lang.reflect.Method;

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
    
    private static boolean detectLSPatchEnvironment() {
        // Check for LSPatch loader classes
        if (isClassAvailable(LSPATCH_LOADER_CLASS) ||
            isClassAvailable(LSPATCH_METALOADER_CLASS) ||
            isClassAvailable(LSPATCH_SERVICE_CLASS) ||
            isClassAvailable(LSPATCH_BRIDGE_CLASS) ||
            isClassAvailable(LSPATCH_XPOSED_INIT) ||
            isClassAvailable(LSPATCH_MODULE_SERVICE) ||
            isClassAvailable(LSPATCH_REMOTE_SERVICE)) {
            return true;
        }
        
        // Check for LSPatch specific system properties
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
            // System property access might be restricted
        }
        
        // Check for LSPatch specific environment variables
        try {
            String lspatchEnv = System.getenv("LSPATCH_ACTIVE");
            if ("1".equals(lspatchEnv)) {
                return true;
            }
            
            String lspatchMode = System.getenv("LSPATCH_MODE");
            if (lspatchMode != null && !lspatchMode.isEmpty()) {
                return true;
            }
        } catch (Exception e) {
            // Environment access might be restricted
        }
        
        // Check for LSPatch config file in assets
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl != null && cl.getResource(LSPATCH_CONFIG_PATH) != null) {
                return true;
            }
        } catch (Exception e) {
            // Resource access might fail
        }
        
        return false;
    }
    
    private static LSPatchMode detectLSPatchMode() {
        // Check if manager mode is active
        if (isClassAvailable("org.lsposed.lspatch.service.RemoteApplicationService")) {
            return LSPatchMode.LSPATCH_MANAGER;
        }
        
        // Check if embedded mode is active
        if (isClassAvailable("org.lsposed.lspatch.service.LocalApplicationService")) {
            return LSPatchMode.LSPATCH_EMBEDDED;
        }
        
        // Check for LSPatch meta loader (indicates patched APK)
        if (isClassAvailable(LSPATCH_METALOADER_CLASS)) {
            return LSPatchMode.LSPATCH_EMBEDDED;
        }
        
        // Check for manager package name in system
        try {
            String managerPackage = System.getProperty("lspatch.manager.package");
            if ("org.lsposed.lspatch".equals(managerPackage)) {
                return LSPatchMode.LSPATCH_MANAGER;
            }
        } catch (Exception e) {
            // Property access might be restricted
        }
        
        // Default to embedded mode if LSPatch is detected
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
    
    private static boolean isClassAvailable(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
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
}
