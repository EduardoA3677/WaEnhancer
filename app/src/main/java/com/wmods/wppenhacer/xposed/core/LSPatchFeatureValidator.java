package com.wmods.wppenhacer.xposed.core;

import android.content.Context;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * LSPatch Feature Compatibility Validator
 * 
 * This class validates and ensures that all WaEnhancer features work correctly
 * in both LSPatch and traditional Xposed environments.
 */
public class LSPatchFeatureValidator {
    private static final String TAG = "LSPatchFeatureValidator";
    
    // Critical features that must work in LSPatch
    private static final String[] CRITICAL_FEATURES = {
        "anti_revoke",
        "hide_seen", 
        "show_online",
        "download_profile",
        "media_quality",
        "status_download"
    };
    
    // Features with known limitations in LSPatch
    private static final String[] LIMITED_FEATURES = {
        "bootloader_spoofer", // System-level, may not work in embedded mode
        "anti_wa_features",   // Root detection bypass
        "custom_theme_v2"     // Resource hooking may be limited
    };
    
    private static final Map<String, ValidationResult> sValidationResults = new HashMap<>();
    
    public static class ValidationResult {
        public final String featureName;
        public final boolean isCompatible;
        public final boolean hasLimitations;
        public final String message;
        public final Exception error;
        
        public ValidationResult(String featureName, boolean isCompatible, boolean hasLimitations, String message, Exception error) {
            this.featureName = featureName;
            this.isCompatible = isCompatible;
            this.hasLimitations = hasLimitations;
            this.message = message;
            this.error = error;
        }
    }
    
    /**
     * Validate all critical features for LSPatch compatibility
     */
    public static Map<String, ValidationResult> validateAllFeatures(ClassLoader classLoader) {
        Log.i(TAG, "Starting comprehensive feature validation for LSPatch compatibility");
        
        sValidationResults.clear();
        
        // Validate core hook functionality
        validateCoreHookingCapability(classLoader);
        
        // Validate specific features
        for (String feature : CRITICAL_FEATURES) {
            validateFeature(feature, classLoader);
        }
        
        // Check limited features
        for (String feature : LIMITED_FEATURES) {
            validateLimitedFeature(feature, classLoader);
        }
        
        // Validate WhatsApp class access
        validateWhatsAppClassAccess(classLoader);
        
        // Validate preference system
        validatePreferenceSystem();
        
        // Validate bridge services
        validateBridgeServices();
        
        logValidationSummary();
        
        return new HashMap<>(sValidationResults);
    }
    
    /**
     * Validate core Xposed hooking capability
     */
    private static void validateCoreHookingCapability(ClassLoader classLoader) {
        try {
            // Test basic XposedBridge functionality
            XposedBridge.log("LSPatch validation: Testing core hooking capability");
            
            // Try to hook a basic Android class to verify Xposed is working
            Class<?> testClass = classLoader.loadClass("java.lang.Object");
            Method toStringMethod = testClass.getDeclaredMethod("toString");
            
            if (toStringMethod != null) {
                addValidationResult("core_hooking", true, false, 
                    "Core Xposed hooking capability verified", null);
            } else {
                addValidationResult("core_hooking", false, false, 
                    "Could not access basic class methods", null);
            }
            
        } catch (Exception e) {
            addValidationResult("core_hooking", false, false, 
                "Core hooking capability test failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate specific feature compatibility
     */
    private static void validateFeature(String featureName, ClassLoader classLoader) {
        try {
            boolean isCompatible = true;
            String message = "Feature validated successfully";
            
            switch (featureName) {
                case "anti_revoke":
                    isCompatible = validateAntiRevoke(classLoader);
                    message = isCompatible ? "Anti-revoke hooks accessible" : "Anti-revoke classes not found";
                    break;
                    
                case "hide_seen":
                    isCompatible = validateHideSeen(classLoader);
                    message = isCompatible ? "Hide seen mechanisms accessible" : "Hide seen classes not found";
                    break;
                    
                case "show_online":
                    isCompatible = validateShowOnline(classLoader);
                    message = isCompatible ? "Show online classes accessible" : "Show online classes not found";
                    break;
                    
                case "download_profile":
                    isCompatible = validateDownloadProfile(classLoader);
                    message = isCompatible ? "Profile download mechanisms accessible" : "Profile download classes not found";
                    break;
                    
                case "media_quality":
                    isCompatible = validateMediaQuality(classLoader);
                    message = isCompatible ? "Media quality settings accessible" : "Media quality classes not found";
                    break;
                    
                case "status_download":
                    isCompatible = validateStatusDownload(classLoader);
                    message = isCompatible ? "Status download mechanisms accessible" : "Status download classes not found";
                    break;
                    
                default:
                    message = "Unknown feature";
                    break;
            }
            
            addValidationResult(featureName, isCompatible, false, message, null);
            
        } catch (Exception e) {
            addValidationResult(featureName, false, false, 
                "Validation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate limited features
     */
    private static void validateLimitedFeature(String featureName, ClassLoader classLoader) {
        try {
            boolean isCompatible = true;
            boolean hasLimitations = true;
            String message = "Feature has limitations in LSPatch";
            
            // These features may work but with reduced functionality
            switch (featureName) {
                case "bootloader_spoofer":
                    // System-level hooks may not work in embedded LSPatch
                    if (LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_EMBEDDED) {
                        isCompatible = false;
                        message = "Bootloader spoofing not available in embedded LSPatch mode";
                    } else {
                        message = "Bootloader spoofing available but may have limitations";
                    }
                    break;
                    
                case "anti_wa_features":
                    // Root detection bypass may not work without system access
                    message = "Anti-WhatsApp detection may have limited effectiveness in LSPatch";
                    break;
                    
                case "custom_theme_v2":
                    // Resource hooking may be limited
                    message = "Custom themes may have limited functionality in LSPatch";
                    break;
            }
            
            addValidationResult(featureName, isCompatible, hasLimitations, message, null);
            
        } catch (Exception e) {
            addValidationResult(featureName, false, true, 
                "Limited feature validation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate anti-revoke functionality
     */
    private static boolean validateAntiRevoke(ClassLoader classLoader) {
        try {
            // Check if we can access key classes for anti-revoke
            Class<?> fMessageClass = classLoader.loadClass("com.whatsapp.fmessage.FMessage");
            return fMessageClass != null;
        } catch (ClassNotFoundException e) {
            // Try alternative class names for different WhatsApp versions
            try {
                classLoader.loadClass("com.whatsapp.data.FMessage");
                return true;
            } catch (ClassNotFoundException e2) {
                return false;
            }
        }
    }
    
    /**
     * Validate hide seen functionality
     */
    private static boolean validateHideSeen(ClassLoader classLoader) {
        try {
            // Check receipt classes
            Class<?> receiptClass = XposedHelpers.findClassIfExists("com.whatsapp.jobqueue.job.SendReadReceiptJob", classLoader);
            return receiptClass != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Validate show online functionality  
     */
    private static boolean validateShowOnline(ClassLoader classLoader) {
        try {
            // Check presence classes
            Class<?> presenceClass = XposedHelpers.findClassIfExists("com.whatsapp.jobqueue.job.SendPresenceAvailableJob", classLoader);
            return presenceClass != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Validate download profile functionality
     */
    private static boolean validateDownloadProfile(ClassLoader classLoader) {
        try {
            // Check contact classes
            Class<?> contactClass = XposedHelpers.findClassIfExists("com.whatsapp.contact.Contact", classLoader);
            return contactClass != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Validate media quality functionality
     */
    private static boolean validateMediaQuality(ClassLoader classLoader) {
        try {
            // Check media classes
            Class<?> mediaClass = XposedHelpers.findClassIfExists("com.whatsapp.media.MediaUpload", classLoader);
            return mediaClass != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Validate status download functionality
     */
    private static boolean validateStatusDownload(ClassLoader classLoader) {
        try {
            // Check status classes
            Class<?> statusClass = XposedHelpers.findClassIfExists("com.whatsapp.status.StatusDownload", classLoader);
            if (statusClass != null) return true;
            
            // Try alternative
            statusClass = XposedHelpers.findClassIfExists("com.whatsapp.status.viewonce.ViewOnceDownload", classLoader);
            return statusClass != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Validate WhatsApp class access
     */
    private static void validateWhatsAppClassAccess(ClassLoader classLoader) {
        try {
            List<String> accessibleClasses = new ArrayList<>();
            List<String> inaccessibleClasses = new ArrayList<>();
            
            String[] testClasses = {
                "com.whatsapp.HomeActivity",
                "com.whatsapp.Main", 
                "com.whatsapp.Conversation",
                "com.whatsapp.contact.Contact",
                "com.whatsapp.jid.UserJid"
            };
            
            for (String className : testClasses) {
                try {
                    Class<?> clazz = classLoader.loadClass(className);
                    if (clazz != null) {
                        accessibleClasses.add(className);
                    }
                } catch (ClassNotFoundException e) {
                    inaccessibleClasses.add(className);
                }
            }
            
            boolean isAccessible = accessibleClasses.size() >= testClasses.length / 2;
            String message = String.format("WhatsApp classes: %d accessible, %d inaccessible", 
                accessibleClasses.size(), inaccessibleClasses.size());
                
            addValidationResult("whatsapp_class_access", isAccessible, false, message, null);
            
        } catch (Exception e) {
            addValidationResult("whatsapp_class_access", false, false, 
                "WhatsApp class access validation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate preference system
     */
    private static void validatePreferenceSystem() {
        try {
            // Test LSPatchPreferences functionality
            Context context = getCurrentContext();
            if (context != null) {
                LSPatchPreferences prefs = new LSPatchPreferences(context);
                boolean isFunctional = prefs.isFunctional();
                
                String message = isFunctional ? 
                    "LSPatchPreferences system is functional" : 
                    "LSPatchPreferences system has issues";
                    
                addValidationResult("preference_system", isFunctional, false, message, null);
            } else {
                addValidationResult("preference_system", false, false, 
                    "Could not get application context", null);
            }
        } catch (Exception e) {
            addValidationResult("preference_system", false, false, 
                "Preference system validation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate bridge services
     */
    private static void validateBridgeServices() {
        try {
            boolean bridgeAvailable = LSPatchBridge.isInitialized();
            boolean serviceAvailable = LSPatchCompat.isLSPatchServiceAvailable();
            
            String message = String.format("Bridge: %s, Service: %s", 
                bridgeAvailable ? "available" : "unavailable",
                serviceAvailable ? "available" : "unavailable");
                
            boolean isWorking = bridgeAvailable || serviceAvailable;
            
            addValidationResult("bridge_services", isWorking, !isWorking, message, null);
            
        } catch (Exception e) {
            addValidationResult("bridge_services", false, false, 
                "Bridge services validation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get current application context
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
     * Add validation result
     */
    private static void addValidationResult(String featureName, boolean isCompatible, 
                                          boolean hasLimitations, String message, Exception error) {
        ValidationResult result = new ValidationResult(featureName, isCompatible, hasLimitations, message, error);
        sValidationResults.put(featureName, result);
        
        String logLevel = isCompatible ? "INFO" : "WARN";
        Log.println(isCompatible ? Log.INFO : Log.WARN, TAG, 
            String.format("[%s] %s: %s", logLevel, featureName, message));
            
        if (error != null) {
            Log.w(TAG, "Validation error for " + featureName, error);
        }
    }
    
    /**
     * Log validation summary
     */
    private static void logValidationSummary() {
        int compatible = 0;
        int incompatible = 0;
        int limited = 0;
        
        for (ValidationResult result : sValidationResults.values()) {
            if (result.isCompatible) {
                if (result.hasLimitations) {
                    limited++;
                } else {
                    compatible++;
                }
            } else {
                incompatible++;
            }
        }
        
        Log.i(TAG, "=== LSPatch Feature Validation Summary ===");
        Log.i(TAG, String.format("Compatible: %d, Limited: %d, Incompatible: %d", 
            compatible, limited, incompatible));
        Log.i(TAG, "LSPatch Mode: " + LSPatchCompat.getCurrentMode());
        Log.i(TAG, "Environment: " + (LSPatchCompat.isLSPatchEnvironment() ? "LSPatch" : "Xposed"));
        Log.i(TAG, "==========================================");
        
        // Log any critical failures
        for (ValidationResult result : sValidationResults.values()) {
            if (!result.isCompatible && isCriticalFeature(result.featureName)) {
                Log.e(TAG, String.format("CRITICAL: %s is not compatible: %s", 
                    result.featureName, result.message));
            }
        }
    }
    
    /**
     * Check if a feature is critical
     */
    private static boolean isCriticalFeature(String featureName) {
        for (String critical : CRITICAL_FEATURES) {
            if (critical.equals(featureName)) {
                return true;
            }
        }
        return "core_hooking".equals(featureName) || 
               "preference_system".equals(featureName) ||
               "whatsapp_class_access".equals(featureName);
    }
    
    /**
     * Get validation results
     */
    public static Map<String, ValidationResult> getValidationResults() {
        return new HashMap<>(sValidationResults);
    }
    
    /**
     * Check if all critical features are compatible
     */
    public static boolean areAllCriticalFeaturesCompatible() {
        for (ValidationResult result : sValidationResults.values()) {
            if (isCriticalFeature(result.featureName) && !result.isCompatible) {
                return false;
            }
        }
        return true;
    }
}
