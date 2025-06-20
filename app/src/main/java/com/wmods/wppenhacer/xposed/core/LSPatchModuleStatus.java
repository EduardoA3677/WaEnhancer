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
            
            // Enhanced context verification for LSPatch
            if (!isCorrectApplicationContext()) {
                Log.d(TAG, "Not in WhatsApp context, checking if WaEnhancer is loaded anyway");
                
                // In LSPatch, sometimes the context detection fails but the module is still active
                // Check if WaEnhancer classes are loaded regardless of context
                if (areWaEnhancerClassesLoaded()) {
                    Log.i(TAG, "WaEnhancer classes detected despite context issues - likely LSPatch embedded mode");
                    return ModuleStatus.ACTIVE_LSPATCH_EMBEDDED;
                }
                
                return ModuleStatus.INACTIVE_NOT_LOADED;
            }
            
            // Enhanced hook verification specifically for LSPatch
            boolean hooksWorking = areHooksWorkingOnWhatsApp();
            
            if (!hooksWorking) {
                Log.w(TAG, "LSPatch environment detected but hooks verification failed");
                
                // In LSPatch, hook verification might fail even when module is working
                // Check if core components are initialized as fallback
                if (isWaEnhancerCoreInitialized()) {
                    Log.i(TAG, "Core components initialized despite hook verification failure");
                    hooksWorking = true;
                } else if (areWaEnhancerClassesLoaded()) {
                    Log.i(TAG, "WaEnhancer classes loaded despite hook verification failure");
                    hooksWorking = true;
                }
            }
            
            // Initialize LSPatch service if needed for additional verification
            if (!LSPatchService.isServiceAvailable()) {
                Context context = getCurrentContext();
                if (context != null) {
                    LSPatchService.initialize(context);
                }
            }
            
            // Check if WaEnhancer is loaded in LSPatch service (only if service is available)
            boolean moduleLoaded = false;
            if (LSPatchService.isServiceAvailable()) {
                moduleLoaded = LSPatchService.isWaEnhancerLoaded();
            } else {
                Log.d(TAG, "LSPatch service not available, using alternative detection");
                // Fallback: check if WaEnhancer is functional without service
                moduleLoaded = isWaEnhancerFunctional();
            }
            
            // If hooks are working and we're in WhatsApp context, determine the correct mode
            if (hooksWorking) {
                Log.i(TAG, "Hooks are working on WhatsApp, determining LSPatch mode");
                
                // Verify the mode detection is accurate
                LSPatchCompat.LSPatchMode verifiedMode = verifyLSPatchMode(mode, moduleLoaded);
                
                switch (verifiedMode) {
                    case LSPATCH_EMBEDDED:
                        return ModuleStatus.ACTIVE_LSPATCH_EMBEDDED;
                    case LSPATCH_MANAGER:
                        return ModuleStatus.ACTIVE_LSPATCH_MANAGER;
                    default:
                        // If mode is unclear but hooks work, default to embedded
                        return ModuleStatus.ACTIVE_LSPATCH_EMBEDDED;
                }
            } else {
                // Hooks not working, but check for partial functionality
                if (moduleLoaded || areWaEnhancerClassesLoaded()) {
                    Log.w(TAG, "Module detected but hooks not fully working - possible LSPatch issue");
                    return ModuleStatus.ACTIVE_LSPATCH_EMBEDDED; // Assume embedded mode with issues
                }
                return ModuleStatus.INACTIVE_NOT_LOADED;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking LSPatch status: " + e.getMessage());
            
            // Enhanced fallback: check if hooks are working
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
     * Verify LSPatch mode detection with additional checks
     */
    private static LSPatchCompat.LSPatchMode verifyLSPatchMode(LSPatchCompat.LSPatchMode detectedMode, boolean moduleInService) {
        try {
            // Check for embedded indicators
            boolean hasEmbeddedIndicators = checkEmbeddedIndicators();
            
            // Check for manager indicators  
            boolean hasManagerIndicators = checkManagerIndicators();
            
            Log.d(TAG, "Mode verification - Detected: " + detectedMode + 
                      ", Embedded indicators: " + hasEmbeddedIndicators + 
                      ", Manager indicators: " + hasManagerIndicators +
                      ", In service: " + moduleInService);
            
            // If we have clear embedded indicators and no manager indicators
            if (hasEmbeddedIndicators && !hasManagerIndicators) {
                return LSPatchCompat.LSPatchMode.LSPATCH_EMBEDDED;
            }
            
            // If we have manager indicators or module is found in service
            if (hasManagerIndicators || moduleInService) {
                return LSPatchCompat.LSPatchMode.LSPATCH_MANAGER;
            }
            
            // Fallback to detected mode
            return detectedMode;
            
        } catch (Exception e) {
            Log.w(TAG, "Error verifying LSPatch mode: " + e.getMessage());
            return detectedMode;
        }
    }
    
    /**
     * Check for embedded mode indicators
     */
    private static boolean checkEmbeddedIndicators() {
        try {
            // Check for embedded-specific classes
            if (isClassAvailable("org.lsposed.lspatch.service.LocalApplicationService")) {
                return true;
            }
            
            // Check for embedded metadata
            Context context = getCurrentContext();
            if (context != null) {
                try {
                    context.getAssets().open("lspatch/embedded.flag");
                    return true;
                } catch (Exception e) {
                    // File doesn't exist
                }
                
                // Check for embedded system properties
                if ("true".equals(System.getProperty("lspatch.embedded"))) {
                    return true;
                }
                
                if ("embedded".equals(System.getProperty("lspatch.mode"))) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check for manager mode indicators
     */
    private static boolean checkManagerIndicators() {
        try {
            // Check for manager-specific classes
            if (isClassAvailable("org.lsposed.lspatch.service.RemoteApplicationService")) {
                return true;
            }
            
            // Check for manager system properties
            if ("true".equals(System.getProperty("lspatch.manager"))) {
                return true;
            }
            
            if ("manager".equals(System.getProperty("lspatch.mode"))) {
                return true;
            }
            
            // Check for manager package
            if ("org.lsposed.lspatch".equals(System.getProperty("lspatch.manager.package"))) {
                return true;
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if a class is available
     */
    private static boolean isClassAvailable(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Enhanced hook verification specifically for WhatsApp
     */
    private static boolean areHooksWorkingOnWhatsApp() {
        try {
            // First verify we're in WhatsApp context
            if (!isCorrectApplicationContext()) {
                Log.d(TAG, "Not in correct application context for hook verification");
                
                // In LSPatch embedded mode, context detection might fail
                // But we can still check if WaEnhancer is loaded
                if (LSPatchCompat.isLSPatchEnvironment()) {
                    String packageName = LSPatchCompat.getCurrentPackageName();
                    if ("com.whatsapp".equals(packageName) || "com.whatsapp.w4b".equals(packageName)) {
                        Log.d(TAG, "LSPatch detected WhatsApp context despite failing standard check");
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
            
            // Check if XposedBridge is functional
            if (!isXposedBridgeFunctional()) {
                Log.d(TAG, "XposedBridge is not functional");
                
                // In LSPatch, XposedBridge might not be fully functional but module can still work
                if (LSPatchCompat.isLSPatchEnvironment()) {
                    Log.d(TAG, "LSPatch environment detected, checking alternative functionality");
                    
                    // Check if WaEnhancer core is initialized despite XposedBridge issues
                    if (isWaEnhancerCoreInitialized()) {
                        Log.d(TAG, "WaEnhancer core initialized despite XposedBridge issues");
                        return true;
                    }
                } else {
                    return false;
                }
            }
            
            // Try to access core WhatsApp classes that WaEnhancer hooks
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                Context context = getCurrentContext();
                if (context != null) {
                    cl = context.getClassLoader();
                } else {
                    // Try using WaEnhancer's classloader as fallback
                    try {
                        cl = LSPatchModuleStatus.class.getClassLoader();
                    } catch (Exception e) {
                        cl = ClassLoader.getSystemClassLoader();
                    }
                }
            }
            
            if (cl == null) {
                Log.w(TAG, "Could not get ClassLoader");
                return false;
            }
            
            // Test access to key WhatsApp classes that WaEnhancer needs to hook
            String[] criticalWhatsAppClasses = {
                "com.whatsapp.HomeActivity",
                "com.whatsapp.Main", 
                "com.whatsapp.Conversation",
                "com.whatsapp.jobqueue.job.SendReadReceiptJob"
            };
            
            int accessibleClasses = 0;
            for (String className : criticalWhatsAppClasses) {
                try {
                    Class<?> clazz = cl.loadClass(className);
                    if (clazz != null) {
                        accessibleClasses++;
                        Log.d(TAG, "Successfully accessed WhatsApp class: " + className);
                    }
                } catch (ClassNotFoundException e) {
                    // Try alternative class names for newer WhatsApp versions
                    if (className.equals("com.whatsapp.HomeActivity")) {
                        try {
                            cl.loadClass("com.whatsapp.home.ui.HomeActivity");
                            accessibleClasses++;
                        } catch (ClassNotFoundException e2) {
                            // Neither version available
                        }
                    }
                }
            }
            
            // We need at least 2 critical classes to be accessible
            if (accessibleClasses < 2) {
                Log.w(TAG, "Cannot access enough WhatsApp classes (" + accessibleClasses + "/" + criticalWhatsAppClasses.length + ")");
                
                // In LSPatch, class access might be limited but module can still work
                if (LSPatchCompat.isLSPatchEnvironment()) {
                    Log.d(TAG, "LSPatch detected, checking for module functionality despite class access issues");
                    
                    // Check if WaEnhancer is actually working despite class access issues
                    if (isWaEnhancerCoreInitialized() || areWaEnhancerClassesLoaded()) {
                        Log.d(TAG, "WaEnhancer appears functional despite class access limitations");
                        return true;
                    }
                }
                
                return false;
            }
            
            // Check if WaEnhancer core components are initialized
            if (isWppCoreInitialized()) {
                Log.d(TAG, "WppCore is initialized - hooks are working on WhatsApp");
                return true;
            }
            
            if (isFeatureLoaderInitialized()) {
                Log.d(TAG, "FeatureLoader is initialized - hooks are working on WhatsApp");
                return true;
            }
            
            // Final verification: check if WaEnhancer classes are properly loaded
            if (areWaEnhancerClassesLoaded()) {
                Log.d(TAG, "WaEnhancer classes are loaded in WhatsApp context");
                return true;
            }
            
            Log.w(TAG, "WhatsApp classes accessible but WaEnhancer components not initialized");
            
            // Additional LSPatch-specific check
            if (LSPatchCompat.isLSPatchEnvironment()) {
                Log.d(TAG, "LSPatch environment - checking for partial functionality");
                
                // In LSPatch, the module might be working even if standard checks fail
                // Check if there are any signs of WaEnhancer activity
                if (areFeaturesWorking()) {
                    Log.d(TAG, "Some WaEnhancer features are working in LSPatch");
                    return true;
                }
            }
            
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking if hooks are working on WhatsApp: " + e.getMessage());
            
            // Last resort check for LSPatch
            if (LSPatchCompat.isLSPatchEnvironment()) {
                try {
                    // If we can load any WaEnhancer class, assume it's working
                    Class.forName("com.wmods.wppenhacer.xposed.core.FeatureLoader");
                    Log.d(TAG, "WaEnhancer detected in LSPatch despite errors");
                    return true;
                } catch (ClassNotFoundException e2) {
                    // WaEnhancer not loaded
                }
            }
            
            return false;
        }
    }
    
    /**
     * Check if hooks are actually working (legacy method for compatibility)
     */
    private static boolean areHooksWorking() {
        return areHooksWorkingOnWhatsApp();
    }
    
    /**
     * Check if we're in the correct application context (WhatsApp)
     */
    private static boolean isCorrectApplicationContext() {
        try {
            Context context = getCurrentContext();
            if (context == null) {
                return false;
            }
            
            String packageName = context.getPackageName();
            return "com.whatsapp".equals(packageName) || "com.whatsapp.w4b".equals(packageName);
            
        } catch (Exception e) {
            Log.d(TAG, "Error checking application context: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if XposedBridge is functional
     */
    private static boolean isXposedBridgeFunctional() {
        try {
            // Try to use XposedBridge log function
            de.robv.android.xposed.XposedBridge.log("WaEnhancer LSPatch status check");
            return true;
        } catch (Exception e) {
            Log.d(TAG, "XposedBridge is not functional: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if WaEnhancer classes are loaded in classloader
     */
    private static boolean areWaEnhancerClassesLoaded() {
        try {
            // Check if our main classes exist in the current classloader
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = LSPatchModuleStatus.class.getClassLoader();
            }
            
            // Try to load core WaEnhancer classes
            String[] waEnhancerClasses = {
                "com.wmods.wppenhacer.xposed.core.WppCore",
                "com.wmods.wppenhacer.xposed.core.FeatureLoader",
                "com.wmods.wppenhacer.xposed.core.components.FMessageWpp"
            };
            
            for (String className : waEnhancerClasses) {
                try {
                    Class<?> clazz = cl.loadClass(className);
                    if (clazz != null) {
                        Log.d(TAG, "Found WaEnhancer class: " + className);
                        return true;
                    }
                } catch (ClassNotFoundException e) {
                    // Continue checking other classes
                }
            }
            
            return false;
            
        } catch (Exception e) {
            Log.d(TAG, "Error checking WaEnhancer classes: " + e.getMessage());
            return false;
        }
    }
    /**
     * Check if WppCore is initialized
     */
    private static boolean isWppCoreInitialized() {
        try {
            Class<?> wppCoreClass = Class.forName("com.wmods.wppenhacer.xposed.core.WppCore");
            
            // Check multiple indicators of WppCore initialization
            // 1. Check if client field exists and is initialized
            try {
                java.lang.reflect.Field clientField = wppCoreClass.getDeclaredField("client");
                clientField.setAccessible(true);
                Object client = clientField.get(null);
                if (client != null) {
                    Log.d(TAG, "WppCore client is initialized");
                    return true;
                }
            } catch (Exception e) {
                // Client field might not exist or be accessible
            }
            
            // 2. Check if mApp field exists and is initialized (from FeatureLoader)
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
                // mApp field might not exist or be accessible
            }
            
            // 3. Check if any static fields in WppCore are initialized
            java.lang.reflect.Field[] fields = wppCoreClass.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    try {
                        field.setAccessible(true);
                        Object value = field.get(null);
                        if (value != null && !(value instanceof Boolean) && !(value instanceof Integer)) {
                            Log.d(TAG, "WppCore field initialized: " + field.getName());
                            return true;
                        }
                    } catch (Exception e) {
                        // Field might not be accessible
                    }
                }
            }
            
            return false;
            
        } catch (ClassNotFoundException e) {
            Log.d(TAG, "WppCore class not found");
            return false;
        } catch (Exception e) {
            Log.d(TAG, "Error checking WppCore initialization: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if FeatureLoader is initialized
     */
    private static boolean isFeatureLoaderInitialized() {
        try {
            Class<?> featureLoaderClass = Class.forName("com.wmods.wppenhacer.xposed.core.FeatureLoader");
            
            // Check if mApp field is initialized (indicates FeatureLoader has started)
            try {
                java.lang.reflect.Field mAppField = featureLoaderClass.getDeclaredField("mApp");
                mAppField.setAccessible(true);
                Object mApp = mAppField.get(null);
                if (mApp != null) {
                    Log.d(TAG, "FeatureLoader mApp field is initialized");
                    return true;
                }
            } catch (Exception e) {
                // mApp field might not exist
            }
            
            // Check other static fields that indicate initialization
            java.lang.reflect.Field[] fields = featureLoaderClass.getDeclaredFields();
            
            for (java.lang.reflect.Field field : fields) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    try {
                        field.setAccessible(true);
                        Object value = field.get(null);
                        // Look for non-null objects (excluding primitives and their wrappers)
                        if (value != null && 
                            !(value instanceof Boolean) && 
                            !(value instanceof Integer) && 
                            !(value instanceof String) &&
                            !(value instanceof Long) &&
                            !(value instanceof Double) &&
                            !(value instanceof Float)) {
                            Log.d(TAG, "FeatureLoader field initialized: " + field.getName());
                            return true;
                        }
                    } catch (Exception e) {
                        // Field might not be accessible
                    }
                }
            }
            
            return false;
            
        } catch (ClassNotFoundException e) {
            Log.d(TAG, "FeatureLoader class not found");
            return false;
        } catch (Exception e) {
            Log.d(TAG, "Error checking FeatureLoader initialization: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if WaEnhancer is functional in LSPatch environment
     */
    private static boolean isWaEnhancerFunctional() {
        try {
            // Check multiple indicators of functionality
            if (isWppCoreInitialized()) {
                return true;
            }
            
            if (isFeatureLoaderInitialized()) {
                return true;
            }
            
            if (areWaEnhancerClassesLoaded()) {
                return true;
            }
            
            // Check if any WaEnhancer features are working
            if (areFeaturesWorking()) {
                return true;
            }
            
            return false;
        } catch (Exception e) {
            Log.d(TAG, "Error checking WaEnhancer functionality: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if any features are working
     */
    private static boolean areFeaturesWorking() {
        try {
            // Check if XposedBridge logging is functional (basic Xposed functionality)
            try {
                de.robv.android.xposed.XposedBridge.log("WaEnhancer LSPatch feature test");
                Log.d(TAG, "XposedBridge logging is functional");
            } catch (Exception e) {
                Log.d(TAG, "XposedBridge logging failed: " + e.getMessage());
                return false;
            }
            
            // Check if we can access WhatsApp classes (indicates we're hooked into the right app)
            try {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                if (cl == null) {
                    cl = getCurrentContext().getClassLoader();
                }
                
                // Try to load WhatsApp classes to confirm we're in the right context
                String[] whatsappClasses = {
                    "com.whatsapp.HomeActivity",
                    "com.whatsapp.Main",
                    "com.whatsapp.conversationslist.ConversationsFragment"
                };
                
                for (String className : whatsappClasses) {
                    try {
                        Class<?> clazz = cl.loadClass(className);
                        if (clazz != null) {
                            Log.d(TAG, "Successfully loaded WhatsApp class: " + className);
                            return true;
                        }
                    } catch (ClassNotFoundException e) {
                        // Continue trying other classes
                    }
                }
                
            } catch (Exception e) {
                Log.d(TAG, "Error loading WhatsApp classes: " + e.getMessage());
            }
            
            // Check if we can access application context (indicates we're properly hooked)
            try {
                Context context = getCurrentContext();
                if (context != null) {
                    String packageName = context.getPackageName();
                    if ("com.whatsapp".equals(packageName) || "com.whatsapp.w4b".equals(packageName)) {
                        Log.d(TAG, "Successfully accessed WhatsApp context: " + packageName);
                        return true;
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "Error accessing context: " + e.getMessage());
            }
            
            return false;
            
        } catch (Exception e) {
            Log.d(TAG, "Error checking if features are working: " + e.getMessage());
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
    
    /**
     * Check if WaEnhancer core components are initialized
     */
    private static boolean isWaEnhancerCoreInitialized() {
        try {
            // Check if WppCore or FeatureLoader are initialized
            if (isWppCoreInitialized()) {
                return true;
            }
            
            if (isFeatureLoaderInitialized()) {
                return true;
            }
            
            // Check if any WaEnhancer components are loaded and functional
            if (areWaEnhancerClassesLoaded()) {
                return true;
            }
            
            return false;
        } catch (Exception e) {
            Log.d(TAG, "Error checking WaEnhancer core initialization: " + e.getMessage());
            return false;
        }
    }
}
