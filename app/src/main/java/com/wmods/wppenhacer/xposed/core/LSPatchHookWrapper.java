package com.wmods.wppenhacer.xposed.core;

import android.util.Log;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Hook wrapper that provides LSPatch compatibility
 * 
 * This class provides wrapper methods for Xposed hooks that automatically
 * apply LSPatch optimizations and handle LSPatch-specific edge cases.
 */
public class LSPatchHookWrapper {
    private static final String TAG = "WaEnhancer-HookWrapper";
    
    /**
     * Hook a method with LSPatch optimizations
     * @param clazz Target class
     * @param methodName Method name
     * @param parameterTypes Parameter types (Object array including callback)
     * @return Hook object or null if failed
     */
    public static XC_MethodHook.Unhook hookMethod(Class<?> clazz, String methodName, Object... parameterTypes) {
        try {
            if (LSPatchCompat.isLSPatchEnvironment()) {
                return hookMethodLSPatch(clazz, methodName, parameterTypes);
            } else {
                return XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypes);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to hook method " + clazz.getSimpleName() + "." + methodName + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Hook a method with LSPatch-specific optimizations
     */
    private static XC_MethodHook.Unhook hookMethodLSPatch(Class<?> clazz, String methodName, Object... parameterTypes) {
        try {
            // Extract callback from parameters
            XC_MethodHook callback = null;
            Object[] cleanParams = new Object[parameterTypes.length];
            int callbackIndex = -1;
            
            for (int i = 0; i < parameterTypes.length; i++) {
                if (parameterTypes[i] instanceof XC_MethodHook) {
                    callback = (XC_MethodHook) parameterTypes[i];
                    callbackIndex = i;
                } else {
                    cleanParams[i] = parameterTypes[i];
                }
            }
            
            if (callback == null) {
                // No callback found, use standard hook
                return XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypes);
            }
            
            // Remove callback from parameters for method finding
            Object[] methodParams = new Object[cleanParams.length - 1];
            System.arraycopy(cleanParams, 0, methodParams, 0, callbackIndex);
            if (callbackIndex < cleanParams.length - 1) {
                System.arraycopy(cleanParams, callbackIndex + 1, methodParams, callbackIndex, 
                               cleanParams.length - callbackIndex - 1);
            }
            
            // Create a wrapper callback that handles LSPatch specifics
            XC_MethodHook finalCallback = callback;
            XC_MethodHook lspatchCallback = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        // Use safe callback invocation
                        LSPatchHookWrapper.callSafeBefore(finalCallback, param);
                    } catch (Exception e) {
                        handleLSPatchHookError("beforeHookedMethod", clazz, methodName, e);
                        // Re-throw only if in manager mode or if it's a critical error
                        if (LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
                            throw e;
                        }
                    }
                }
                
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        // Use safe callback invocation
                        LSPatchHookWrapper.callSafeAfter(finalCallback, param);
                    } catch (Exception e) {
                        handleLSPatchHookError("afterHookedMethod", clazz, methodName, e);
                        // Re-throw only if in manager mode or if it's a critical error
                        if (LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
                            throw e;
                        }
                    }
                }
            };
            
            // Combine method parameters with LSPatch callback
            Object[] finalParams = new Object[methodParams.length + 1];
            System.arraycopy(methodParams, 0, finalParams, 0, methodParams.length);
            finalParams[finalParams.length - 1] = lspatchCallback;
            
            return XposedHelpers.findAndHookMethod(clazz, methodName, finalParams);
            
        } catch (Exception e) {
            Log.e(TAG, "LSPatch hook failed for " + clazz.getSimpleName() + "." + methodName + ": " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Replace a method with LSPatch optimizations
     * @param clazz Target class
     * @param methodName Method name
     * @param parameterTypes Parameter types (Object array including replacement)
     * @return Hook object or null if failed
     */
    public static XC_MethodHook.Unhook replaceMethod(Class<?> clazz, String methodName, Object... parameterTypes) {
        try {
            if (LSPatchCompat.isLSPatchEnvironment()) {
                return replaceMethodLSPatch(clazz, methodName, parameterTypes);
            } else {
                return XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypes);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to replace method " + clazz.getSimpleName() + "." + methodName + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Replace a method with LSPatch-specific optimizations
     */
    private static XC_MethodHook.Unhook replaceMethodLSPatch(Class<?> clazz, String methodName, Object... parameterTypes) {
        try {
            // Extract replacement from parameters
            XC_MethodReplacement replacement = null;
            Object[] cleanParams = new Object[parameterTypes.length];
            int replacementIndex = -1;
            
            for (int i = 0; i < parameterTypes.length; i++) {
                if (parameterTypes[i] instanceof XC_MethodReplacement) {
                    replacement = (XC_MethodReplacement) parameterTypes[i];
                    replacementIndex = i;
                } else {
                    cleanParams[i] = parameterTypes[i];
                }
            }
            
            if (replacement == null) {
                // No replacement found, use standard hook
                return XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypes);
            }
            
            // Remove replacement from parameters for method finding
            Object[] methodParams = new Object[cleanParams.length - 1];
            System.arraycopy(cleanParams, 0, methodParams, 0, replacementIndex);
            if (replacementIndex < cleanParams.length - 1) {
                System.arraycopy(cleanParams, replacementIndex + 1, methodParams, replacementIndex, 
                               cleanParams.length - replacementIndex - 1);
            }
            
            // Create a wrapper replacement that handles LSPatch specifics
            XC_MethodReplacement finalReplacement = replacement;
            XC_MethodReplacement lspatchReplacement = new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        // Use safe replacement invocation
                        return LSPatchHookWrapper.callSafeReplacement(finalReplacement, param);
                    } catch (Exception e) {
                        handleLSPatchHookError("replaceHookedMethod", clazz, methodName, e);
                        
                        // In LSPatch, provide fallback behavior for critical methods
                        if (LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_EMBEDDED) {
                            Log.w(TAG, "Using original method due to replacement error in embedded mode");
                            return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                        } else {
                            throw e;
                        }
                    }
                }
            };
            
            // Combine method parameters with LSPatch replacement
            Object[] finalParams = new Object[methodParams.length + 1];
            System.arraycopy(methodParams, 0, finalParams, 0, methodParams.length);
            finalParams[finalParams.length - 1] = lspatchReplacement;
            
            return XposedHelpers.findAndHookMethod(clazz, methodName, finalParams);
            
        } catch (Exception e) {
            Log.e(TAG, "LSPatch replacement failed for " + clazz.getSimpleName() + "." + methodName + ": " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Hook all methods with a given name
     * @param clazz Target class
     * @param methodName Method name
     * @param callback Hook callback
     * @return Set of hook objects
     */
    public static java.util.Set<XC_MethodHook.Unhook> hookAllMethods(Class<?> clazz, String methodName, XC_MethodHook callback) {
        try {
            if (LSPatchCompat.isLSPatchEnvironment()) {
                return hookAllMethodsLSPatch(clazz, methodName, callback);
            } else {
                return XposedBridge.hookAllMethods(clazz, methodName, callback);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to hook all methods " + clazz.getSimpleName() + "." + methodName + ": " + e.getMessage());
            return new java.util.HashSet<>();
        }
    }
    
    /**
     * Hook all methods with LSPatch-specific optimizations
     */
    private static java.util.Set<XC_MethodHook.Unhook> hookAllMethodsLSPatch(Class<?> clazz, String methodName, XC_MethodHook callback) {
        // Create LSPatch-optimized callback
        XC_MethodHook lspatchCallback = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    // Use safe callback invocation
                    LSPatchHookWrapper.callSafeBefore(callback, param);
                } catch (Exception e) {
                    handleLSPatchHookError("beforeHookedMethod", clazz, methodName, e);
                    if (LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
                        throw e;
                    }
                }
            }
            
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    // Use safe callback invocation
                    LSPatchHookWrapper.callSafeAfter(callback, param);
                } catch (Exception e) {
                    handleLSPatchHookError("afterHookedMethod", clazz, methodName, e);
                    if (LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
                        throw e;
                    }
                }
            }
        };
        
        return XposedBridge.hookAllMethods(clazz, methodName, lspatchCallback);
    }
    
    /**
     * Hook all constructors of a class
     * @param clazz Target class
     * @param callback Hook callback
     * @return Set of hook objects
     */
    public static java.util.Set<XC_MethodHook.Unhook> hookAllConstructors(Class<?> clazz, XC_MethodHook callback) {
        try {
            if (LSPatchCompat.isLSPatchEnvironment()) {
                return hookAllConstructorsLSPatch(clazz, callback);
            } else {
                return XposedBridge.hookAllConstructors(clazz, callback);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to hook all constructors " + clazz.getSimpleName() + ": " + e.getMessage());
            return new java.util.HashSet<>();
        }
    }
    
    /**
     * Hook all constructors with LSPatch-specific optimizations
     */
    private static java.util.Set<XC_MethodHook.Unhook> hookAllConstructorsLSPatch(Class<?> clazz, XC_MethodHook callback) {
        // Create LSPatch-optimized callback
        XC_MethodHook lspatchCallback = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    // Use safe callback invocation
                    LSPatchHookWrapper.callSafeBefore(callback, param);
                } catch (Exception e) {
                    handleLSPatchHookError("beforeHookedMethod", clazz, "<init>", e);
                    if (LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
                        throw e;
                    }
                }
            }
            
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    // Use safe callback invocation
                    LSPatchHookWrapper.callSafeAfter(callback, param);
                } catch (Exception e) {
                    handleLSPatchHookError("afterHookedMethod", clazz, "<init>", e);
                    if (LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
                        throw e;
                    }
                }
            }
        };
        
        return XposedBridge.hookAllConstructors(clazz, lspatchCallback);
    }
    
    /**
     * Safely call beforeHookedMethod with error handling
     */
    public static void callSafeBefore(XC_MethodHook callback, XC_MethodHook.MethodHookParam param) throws Throwable {
        if (callback == null) return;
        
        try {
            // Use reflection to access protected method
            Method beforeMethod = XC_MethodHook.class.getDeclaredMethod("beforeHookedMethod", XC_MethodHook.MethodHookParam.class);
            beforeMethod.setAccessible(true);
            beforeMethod.invoke(callback, param);
        } catch (Exception e) {
            // Fallback: try direct call if available
            if (e.getCause() instanceof Throwable) {
                throw (Throwable) e.getCause();
            }
            throw e;
        }
    }
    
    /**
     * Safely call afterHookedMethod with error handling
     */
    public static void callSafeAfter(XC_MethodHook callback, XC_MethodHook.MethodHookParam param) throws Throwable {
        if (callback == null) return;
        
        try {
            // Use reflection to access protected method
            Method afterMethod = XC_MethodHook.class.getDeclaredMethod("afterHookedMethod", XC_MethodHook.MethodHookParam.class);
            afterMethod.setAccessible(true);
            afterMethod.invoke(callback, param);
        } catch (Exception e) {
            // Fallback: try direct call if available
            if (e.getCause() instanceof Throwable) {
                throw (Throwable) e.getCause();
            }
            throw e;
        }
    }
    
    /**
     * Safely call replaceHookedMethod with error handling
     */
    public static Object callSafeReplacement(XC_MethodReplacement replacement, XC_MethodHook.MethodHookParam param) throws Throwable {
        if (replacement == null) return null;
        
        try {
            // Use reflection to access protected method
            Method replaceMethod = XC_MethodReplacement.class.getDeclaredMethod("replaceHookedMethod", XC_MethodHook.MethodHookParam.class);
            replaceMethod.setAccessible(true);
            return replaceMethod.invoke(replacement, param);
        } catch (Exception e) {
            // Fallback: try direct call if available
            if (e.getCause() instanceof Throwable) {
                throw (Throwable) e.getCause();
            }
            throw e;
        }
    }
    
    /**
     * Handle LSPatch-specific hook errors
     */
    private static void handleLSPatchHookError(String phase, Class<?> clazz, String methodName, Exception e) {
        String errorMsg = String.format("LSPatch hook error in %s for %s.%s: %s", 
                                       phase, clazz.getSimpleName(), methodName, e.getMessage());
        Log.w(TAG, errorMsg);
        
        // Log additional debug info for LSPatch troubleshooting
        if (LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_EMBEDDED) {
            Log.d(TAG, "LSPatch embedded mode - continuing with degraded functionality");
        } else if (LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
            Log.d(TAG, "LSPatch manager mode - strict error handling");
        }
    }
    
    /**
     * Check if a hook is safe to apply in current LSPatch environment
     */
    public static boolean isHookSafe(Class<?> clazz, String methodName) {
        if (!LSPatchCompat.isLSPatchEnvironment()) {
            return true; // All hooks are safe in classic Xposed
        }
        
        // Check for known problematic hooks in LSPatch
        String className = clazz.getSimpleName();
        
        // System server hooks are not supported in LSPatch
        if (className.contains("SystemServer") || className.contains("ActivityManager") || 
            className.contains("PackageManager")) {
            return false;
        }
        
        // Resource hooks have limitations in manager mode
        if (LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
            if (className.contains("Resources") || className.contains("Theme") || 
                methodName.contains("getColor") || methodName.contains("getDrawable")) {
                Log.w(TAG, "Resource hook has limitations in LSPatch manager mode: " + className + "." + methodName);
            }
        }
        
        return true;
    }
}
