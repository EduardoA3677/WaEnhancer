# Compatibilidad con LSPosed y LSPatch en WaEnhancer

Este documento describe la implementación de compatibilidad con LSPosed (con root) y LSPatch (sin root) en el módulo WaEnhancer.

## Arquitectura de compatibilidad

WaEnhancer ahora es compatible con dos modos principales de operación:

1. **LSPosed (con root)** - El modo tradicional de Xposed que requiere acceso root.
2. **LSPatch (sin root)** - Método sin root que utiliza el parcheo de APKs.

### Componentes principales

- `LSPatchCompat.java` - Capa principal de compatibilidad que detecta y adapta el comportamiento según el entorno.
- `LSDetector.java` - Utilidad para detectar y distinguir entre entornos LSPosed y LSPatch.
- `LSPatchDexKitCompat.java` - Implementación compatible para inicializar y configurar el entorno sin dependencias externas.

## Modos de LSPatch

LSPatch puede operar en dos modos distintos:

1. **Modo incrustado (Embedded)** - Los módulos están integrados directamente en el APK objetivo.
2. **Modo gestor (Manager)** - Los módulos son gestionados a través de una aplicación externa (LSPatch Manager).

### Características soportadas

| Característica                 | LSPosed (root) | LSPatch Incrustado | LSPatch Gestor |
| ------------------------------ | -------------- | ------------------ | -------------- |
| Hooks básicos                  | ✓              | ✓                  | ✓              |
| Hooks de recursos              | ✓              | ✓                  | Parcial        |
| Hooks del servidor del sistema | ✓              | ✗                  | ✗              |
| Bypass de firma                | ✓              | ✓                  | ✓              |
| Servicio puente                | ✓              | ✓                  | ✗              |

## Detección de entorno

El módulo utiliza varias técnicas para detectar el entorno en el que está operando:

1. **Detección de clases** - Verifica la presencia de clases específicas de LSPosed o LSPatch.
2. **Propiedades del sistema** - Comprueba propiedades del sistema específicas de cada plataforma.
3. **Variables de entorno** - Examina variables de entorno establecidas por LSPatch.
4. **Archivos de recursos** - Busca archivos de configuración específicos en los recursos de la aplicación.

## Optimizaciones específicas

### Para LSPosed (root)

- Utiliza hooks tradicionales de Xposed
- Acceso completo a recursos y servidor del sistema

### Para LSPatch (sin root)

- Estabilidad mejorada para hooks en entornos sin root
- Manejo optimizado de recursos según el modo
- Mecanismos de recuperación de errores
- Fallbacks para servicios no disponibles

## Uso para desarrolladores

```java
// Detectar entorno LSPatch
if (LSPatchCompat.isLSPatchEnvironment()) {
    // Código específico para LSPatch
}

// Verificar modo específico
LSPatchCompat.LSPatchMode mode = LSPatchCompat.getCurrentMode();
if (mode == LSPatchCompat.LSPatchMode.LSPATCH_EMBEDDED) {
    // Código para modo incrustado
} else if (mode == LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
    // Código para modo gestor
}

// Verificar características disponibles
if (LSPatchCompat.isFeatureAvailable("RESOURCE_HOOKS")) {
    // Usar hooks de recursos
}
```

## Solución de problemas

### Errores comunes en LSPatch

1. **ClassNotFoundException** - Algunas clases pueden no estar disponibles en entornos LSPatch. Usa `isClassAvailable()` para verificar.
2. **Errores de recursos** - Los hooks de recursos pueden fallar en modo gestor. Implementa alternativas.
3. **SecurityException** - Acceso a propiedades del sistema o variables de entorno puede estar restringido.

### Verificación de entorno

Para verificar el entorno de ejecución actual:

1. Revisa los logs con el tag "WaEnhancer-LSPatch" o "WaEnhancer-LSDetector"
2. Usa `LSPatchCompat.logCompatibilityInfo()` para imprimir información detallada
3. Verifica si la aplicación está parcheada con `LSPatchCompat.isApplicationPatched(context)`
