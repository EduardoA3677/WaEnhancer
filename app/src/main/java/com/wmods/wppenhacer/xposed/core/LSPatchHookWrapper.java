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
     * @param parameterTypes Parameter types
     * @param callback Hook callback
     * @return Hook object or null if failed
     */
    public static XC_MethodHook.Unhook hookMethod(Class<?> clazz, String methodName, 
                                                  Object[] parameterTypes, XC_MethodHook callback) {
        try {
            if (LSPatchCompat.isLSPatchEnvironment()) {
                return hookMethodLSPatch(clazz, methodName, parameterTypes, callback);
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
    private static XC_MethodHook.Unhook hookMethodLSPatch(Class<?> clazz, String methodName, 
                                                          Object[] parameterTypes, XC_MethodHook callback) {
        try {
            // Create a wrapper callback that handles LSPatch specifics
            XC_MethodHook lspatchCallback = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        callback.beforeHookedMethod(param);
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
                        callback.afterHookedMethod(param);
                    } catch (Exception e) {
                        handleLSPatchHookError("afterHookedMethod", clazz, methodName, e);
                        // Re-throw only if in manager mode or if it's a critical error
                        if (LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
                            throw e;
                        }
                    }
                }
            };
            
            return XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypes);
            
        } catch (Exception e) {
            Log.e(TAG, "LSPatch hook failed for " + clazz.getSimpleName() + "." + methodName + ": " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Replace a method with LSPatch optimizations
     * @param clazz Target class
     * @param methodName Method name
     * @param parameterTypes Parameter types
     * @param replacement Replacement callback
     * @return Hook object or null if failed
     */
    public static XC_MethodHook.Unhook replaceMethod(Class<?> clazz, String methodName, 
                                                     Object[] parameterTypes, XC_MethodReplacement replacement) {
        try {
            if (LSPatchCompat.isLSPatchEnvironment()) {
                return replaceMethodLSPatch(clazz, methodName, parameterTypes, replacement);
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
    private static XC_MethodHook.Unhook replaceMethodLSPatch(Class<?> clazz, String methodName, 
                                                             Object[] parameterTypes, XC_MethodReplacement replacement) {
        try {
            // Create a wrapper replacement that handles LSPatch specifics
            XC_MethodReplacement lspatchReplacement = new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        return replacement.replaceHookedMethod(param);
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
            
            return XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypes);
            
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
                    callback.beforeHookedMethod(param);
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
                    callback.afterHookedMethod(param);
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
     * Hook a constructor with LSPatch optimizations
     * @param clazz Target class
     * @param parameterTypes Parameter types
     * @param callback Hook callback
     * @return Hook object or null if failed
     */
    public static XC_MethodHook.Unhook hookConstructor(Class<?> clazz, Object[] parameterTypes, XC_MethodHook callback) {
        try {
            if (LSPatchCompat.isLSPatchEnvironment()) {
                return hookConstructorLSPatch(clazz, parameterTypes, callback);
            } else {
                return XposedHelpers.findAndHookConstructor(clazz, parameterTypes);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to hook constructor " + clazz.getSimpleName() + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Hook a constructor with LSPatch-specific optimizations
     */
    private static XC_MethodHook.Unhook hookConstructorLSPatch(Class<?> clazz, Object[] parameterTypes, XC_MethodHook callback) {
        // Similar pattern as method hooks but for constructors
        XC_MethodHook lspatchCallback = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    callback.beforeHookedMethod(param);
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
                    callback.afterHookedMethod(param);
                } catch (Exception e) {
                    handleLSPatchHookError("afterHookedMethod", clazz, "<init>", e);
                    if (LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
                        throw e;
                    }
                }
            }
        };
        
        return XposedHelpers.findAndHookConstructor(clazz, parameterTypes);
    }
    
    /**
     * Handle LSPatch-specific hook errors
     */
    private static void handleLSPatchHookError(String hookType, Class<?> clazz, String methodName, Exception e) {
        String identifier = clazz.getSimpleName() + "." + methodName + " (" + hookType + ")";
        
        Log.w(TAG, "Hook error in LSPatch mode for " + identifier + ": " + e.getMessage());
        
        // Log additional context for LSPatch debugging
        if (LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_EMBEDDED) {
            Log.d(TAG, "Error occurred in embedded mode, continuing with reduced functionality");
        } else if (LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
            Log.d(TAG, "Error occurred in manager mode, may need bridge service");
        }
        
        // Update error statistics
        incrementErrorCount(identifier);
    }
    
    private static final java.util.Map<String, Integer> sErrorCounts = new java.util.HashMap<>();
    
    /**
     * Increment error count for monitoring
     */
    private static void incrementErrorCount(String identifier) {
        synchronized (sErrorCounts) {
            Integer count = sErrorCounts.get(identifier);
            sErrorCounts.put(identifier, count == null ? 1 : count + 1);
        }
    }
    
    /**
     * Get error statistics
     */
    public static java.util.Map<String, Integer> getErrorStats() {
        synchronized (sErrorCounts) {
            return new java.util.HashMap<>(sErrorCounts);
        }
    }
    
    /**
     * Clear error statistics
     */
    public static void clearErrorStats() {
        synchronized (sErrorCounts) {
            sErrorCounts.clear();
        }
    }
}
