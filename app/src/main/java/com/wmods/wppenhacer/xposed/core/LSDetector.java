package com.wmods.wppenhacer.xposed.core;

import android.content.Context;
import android.util.Log;

/**
 * Utilidad para detectar y gestionar LSPosed y LSPatch
 * 
 * Esta clase proporciona métodos para detectar si la aplicación
 * está ejecutándose en un entorno LSPosed (con root) o LSPatch (sin root)
 */
public class LSDetector {
    private static final String TAG = "WaEnhancer-LSDetector";
    
    // Clases específicas para detectar LSPosed
    private static final String LSPOSED_CLASS = "de.robv.android.xposed.XposedBridge";
    private static final String LSPOSED_BRIDGE = "org.lsposed.lspd.core.Main";
    private static final String LSPOSED_SERVICE = "org.lsposed.lspd.service.LSPosedService";
    
    // Clases específicas para detectar LSPatch
    private static final String LSPATCH_LOADER = "org.lsposed.lspatch.loader.LSPApplication";
    private static final String LSPATCH_META = "org.lsposed.lspatch.metaloader.LSPAppComponentFactoryStub";
    
    // Propiedades del sistema para LSPosed
    private static final String LSPOSED_PROP_VERSION = "ro.lsposed.api";
    private static final String LSPOSED_PROP_VARIANT = "ro.lsposed.variant";
    
    // Propiedades del sistema para LSPatch
    private static final String LSPATCH_PROP = "lspatch.enabled";
    private static final String LSPATCH_ENV = "LSPATCH_ACTIVE";
    
    /**
     * Detecta si el módulo está ejecutándose en un entorno LSPosed con root
     */
    public static boolean isLSPosedEnvironment() {
        // Verificar clases de LSPosed
        if (isClassAvailable(LSPOSED_BRIDGE) || isClassAvailable(LSPOSED_SERVICE)) {
            Log.i(TAG, "LSPosed detectado por presencia de clases específicas");
            return true;
        }
        
        // Verificar propiedades del sistema para LSPosed
        try {
            String lsposedVersion = System.getProperty(LSPOSED_PROP_VERSION);
            if (lsposedVersion != null && !lsposedVersion.isEmpty()) {
                Log.i(TAG, "LSPosed detectado por propiedad del sistema: " + LSPOSED_PROP_VERSION);
                return true;
            }
            
            String lsposedVariant = System.getProperty(LSPOSED_PROP_VARIANT);
            if (lsposedVariant != null && !lsposedVariant.isEmpty()) {
                Log.i(TAG, "LSPosed detectado por propiedad del sistema: " + LSPOSED_PROP_VARIANT);
                return true;
            }
        } catch (Exception e) {
            // Acceso a propiedades puede estar restringido
        }
        
        return false;
    }
    
    /**
     * Detecta si el módulo está ejecutándose en un entorno LSPatch sin root
     */
    public static boolean isLSPatchEnvironment() {
        // Verificar clases de LSPatch
        if (isClassAvailable(LSPATCH_LOADER) || isClassAvailable(LSPATCH_META)) {
            Log.i(TAG, "LSPatch detectado por presencia de clases específicas");
            return true;
        }
        
        // Verificar propiedades del sistema para LSPatch
        try {
            String lspatchEnabled = System.getProperty(LSPATCH_PROP);
            if ("true".equals(lspatchEnabled)) {
                Log.i(TAG, "LSPatch detectado por propiedad del sistema: " + LSPATCH_PROP);
                return true;
            }
        } catch (Exception e) {
            // Acceso a propiedades puede estar restringido
        }
        
        // Verificar variables de entorno para LSPatch
        try {
            String lspatchActive = System.getenv(LSPATCH_ENV);
            if ("1".equals(lspatchActive)) {
                Log.i(TAG, "LSPatch detectado por variable de entorno: " + LSPATCH_ENV);
                return true;
            }
        } catch (Exception e) {
            // Acceso a variables de entorno puede estar restringido
        }
        
        return false;
    }
    
    /**
     * Determina el modo de operación basado en el entorno detectado
     */
    public static LSPatchCompat.LSPatchMode detectOperationMode() {
        // Si es LSPatch, determinar el modo específico
        if (isLSPatchEnvironment()) {
            if (isClassAvailable("org.lsposed.lspatch.service.RemoteApplicationService")) {
                Log.i(TAG, "LSPatch operando en modo gestor");
                return LSPatchCompat.LSPatchMode.LSPATCH_MANAGER;
            } else {
                Log.i(TAG, "LSPatch operando en modo incrustado");
                return LSPatchCompat.LSPatchMode.LSPATCH_EMBEDDED;
            }
        }
        
        // Por defecto o si es LSPosed, usar el modo clásico
        if (isLSPosedEnvironment()) {
            Log.i(TAG, "LSPosed operando en modo clásico");
        } else {
            Log.w(TAG, "No se detectó ningún entorno específico, asumiendo modo clásico");
        }
        
        return LSPatchCompat.LSPatchMode.CLASSIC_XPOSED;
    }
    
    /**
     * Detecta la aplicación está parcheada con LSPatch
     */
    public static boolean isAppPatched(Context context) {
        if (context == null) return false;
        
        try {
            // Verificar metadatos de LSPatch
            if (context.getApplicationInfo().metaData != null && 
                context.getApplicationInfo().metaData.containsKey("lspatch")) {
                Log.i(TAG, "Aplicación parcheada con LSPatch (metadatos)");
                return true;
            }
            
            // Verificar factory component de LSPatch
            if (LSPATCH_META.equals(context.getApplicationInfo().appComponentFactory)) {
                Log.i(TAG, "Aplicación parcheada con LSPatch (component factory)");
                return true;
            }
            
            // Verificar recursos de LSPatch
            try {
                String[] assets = context.getAssets().list("lspatch");
                if (assets != null && assets.length > 0) {
                    Log.i(TAG, "Aplicación parcheada con LSPatch (assets)");
                    return true;
                }
            } catch (Exception e) {
                // El acceso a assets puede fallar
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al verificar si la aplicación está parcheada: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Verifica si una clase está disponible
     */
    private static boolean isClassAvailable(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
