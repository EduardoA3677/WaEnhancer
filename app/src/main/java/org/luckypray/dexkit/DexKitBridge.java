package org.luckypray.dexkit;

import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.result.MethodDataList;
import org.luckypray.dexkit.result.ClassDataList;

import android.util.Log;

/**
 * Clase stub para DexKit
 * Esta clase es un reemplazo mínimo para DexKitBridge que permite que el código compile
 * sin las dependencias reales de DexKit
 */
public class DexKitBridge implements AutoCloseable {
    private static final String TAG = "WaEnhancer-DexKitStub";
    
    private String path;
    
    private DexKitBridge(String path) {
        this.path = path;
    }
    
    /**
     * Crea una instancia de DexKitBridge
     * @param path Ruta al directorio fuente
     * @return Instancia de DexKitBridge
     */
    public static DexKitBridge create(String path) {
        Log.i(TAG, "Creando DexKitBridge stub para: " + path);
        return new DexKitBridge(path);
    }
    
    /**
     * Busca métodos
     */
    public MethodDataList findMethod(FindMethod finder) {
        Log.d(TAG, "Llamada a findMethod stub");
        return new MethodDataList();
    }
    
    /**
     * Busca clases
     */
    public ClassDataList findClass(FindClass finder) {
        Log.d(TAG, "Llamada a findClass stub");
        return new ClassDataList();
    }
    
    /**
     * Cierra los recursos
     */
    @Override
    public void close() {
        Log.d(TAG, "Cerrando DexKitBridge stub");
    }
}
