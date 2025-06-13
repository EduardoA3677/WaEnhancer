# WaEnhancer LSPatch Compatibility Guide

## Overview

WaEnhancer has been updated to provide full compatibility with LSPatch, a rootless implementation of the Xposed framework. This guide explains the changes made and how to use WaEnhancer with LSPatch.

## What is LSPatch?

LSPatch is a framework that allows running Xposed modules without root access by directly patching APK files. It provides two main modes:

1. **Embedded Mode**: The module is embedded directly into the target APK
2. **Manager Mode**: Uses an external LSPatch manager application

## WaEnhancer LSPatch Compatibility Features

### 1. Enhanced Detection System

WaEnhancer now includes multiple detection methods to reliably identify LSPatch environments:

- **Class Availability Checks**: Detects LSPatch loader and service classes
- **System Properties**: Checks for LSPatch-specific system properties
- **Environment Variables**: Looks for LSPatch environment markers
- **Asset Detection**: Scans for LSPatch configuration files and assets
- **Stack Trace Analysis**: Examines call stacks for LSPatch signatures
- **Native Library Detection**: Searches for LSPatch native libraries

### 2. Adaptive Service Management

The bridge service system has been enhanced to work with both LSPatch modes:

- **Embedded Mode**: Prioritizes ProviderClient and LocalApplicationService
- **Manager Mode**: Prioritizes BridgeClient and RemoteApplicationService
- **Fallback Mechanisms**: Automatically falls back to working alternatives

### 3. LSPatch-Compatible Preferences

A new `LSPatchPreferences` wrapper handles shared preferences access:

- **Traditional Xposed**: Uses `XSharedPreferences` normally
- **LSPatch Embedded**: Uses file-based preference access
- **LSPatch Manager**: Uses LSPatch service for preference access
- **Automatic Conversion**: Converts XSharedPreferences to compatible format

### 4. Optimized Hook Management

LSPatch-specific optimizations for better hook stability:

- **Conservative Memory Usage**: Reduced memory footprint
- **Enhanced Error Recovery**: Better handling of hook failures
- **Priority-based Hook Registration**: Optimized hook order for LSPatch

### 5. Feature Compatibility Matrix

| Feature | Classic Xposed | LSPatch Embedded | LSPatch Manager |
|---------|----------------|------------------|-----------------|
| Core Features | ✅ Full | ✅ Full | ✅ Full |
| Resource Hooks | ✅ Full | ⚠️ Limited | ✅ Full |
| System Hooks | ✅ Full | ❌ Disabled | ❌ Disabled |
| Bridge Service | ✅ Full | ⚠️ Fallback | ✅ Full |
| Preferences | ✅ Full | ✅ Adapted | ✅ Full |
| Bootloader Spoofer | ✅ Full | ✅ Compatible | ✅ Compatible |

## Installation and Usage

### Using with LSPatch

1. **Patch WhatsApp with LSPatch**:
   - Download LSPatch manager
   - Patch WhatsApp APK with WaEnhancer embedded
   - Install the patched APK

2. **Verify Installation**:
   - Open WaEnhancer settings
   - Check the module status (should show "Active (LSPatch Embedded)" or "Active (LSPatch Manager)")

3. **Configure Features**:
   - All core features work the same as in traditional Xposed
   - Some advanced features may have limitations (see compatibility matrix)

### Troubleshooting LSPatch Issues

#### Module Shows as "Inactive"

1. **Check LSPatch Installation**:
   ```
   - Ensure WhatsApp was properly patched
   - Verify LSPatch manager is installed (for manager mode)
   - Check that WaEnhancer is embedded in the patched APK
   ```

2. **Force Status Refresh**:
   - Open WaEnhancer settings
   - The status should update automatically
   - Look for detailed status in logs

3. **Check Logs**:
   ```
   adb logcat | grep -E "(WaEnhancer|LSPatch)"
   ```

#### Features Not Working

1. **Check Feature Compatibility**:
   - Some system-level features are disabled in LSPatch
   - Resource hooks may have limitations
   - Check the compatibility matrix above

2. **Verify Bridge Service**:
   - The bridge service may use fallback methods in LSPatch
   - Check if ProviderClient is working as alternative

3. **Preference Issues**:
   - LSPatch uses different preference access methods
   - Settings should automatically adapt

## Technical Implementation Details

### LSPatch Detection Flow

```java
// Primary detection: LSPatch classes
if (isClassAvailable("org.lsposed.lspatch.loader.LSPApplication")) {
    return LSPatchMode.LSPATCH_EMBEDDED;
}

// Secondary detection: Service classes
if (isClassAvailable("org.lsposed.lspatch.service.LocalApplicationService")) {
    return LSPatchMode.LSPATCH_EMBEDDED;
}

// Additional checks: properties, environment, assets...
```

### Service Initialization Priority

```java
// Embedded Mode Priority
1. LocalApplicationService
2. ProviderClient
3. BridgeClient

// Manager Mode Priority  
1. RemoteApplicationService
2. BridgeClient
3. ProviderClient
```

### Preference Access Adaptation

```java
// LSPatch environment
if (LSPatchCompat.isLSPatchEnvironment()) {
    // Use LSPatch-compatible preference access
    preferences = new LSPatchPreferences(context);
} else {
    // Use traditional XSharedPreferences
    preferences = new XSharedPreferences(packageName);
}
```

## Code Changes Summary

### New Classes Added

1. **`LSPatchCompat`**: Main compatibility detection and management
2. **`LSPatchService`**: Direct integration with LSPatch services
3. **`LSPatchPreferences`**: Preferences wrapper for LSPatch compatibility
4. **`LSPatchPreferencesImpl`**: File-based preferences implementation
5. **`LSPatchBridge`**: Bridge functionality for LSPatch
6. **`LSPatchHookWrapper`**: Hook optimization for LSPatch
7. **`LSPatchModuleStatus`**: Enhanced module status detection
8. **`LSPatchConfig`**: Configuration management for LSPatch

### Modified Classes

1. **`MainActivity`**: Enhanced module detection
2. **`WppXposed`**: LSPatch initialization and compatibility checks
3. **`WppCore`**: Bridge service adaptation for LSPatch
4. **`FeatureLoader`**: LSPatch-aware feature loading

### Configuration Files

1. **`lspatch_config.json`**: Feature compatibility matrix
2. **Updated manifests**: LSPatch-specific service declarations

## Migration Guide

### For Users

No migration is needed - the app automatically detects and adapts to the environment.

### For Developers

1. **Use New Detection APIs**:
   ```java
   // Old way
   if (isXposedActive()) { ... }
   
   // New way
   LSPatchModuleStatus.ModuleStatus status = LSPatchModuleStatus.getCurrentStatus();
   if (status.isActive()) { ... }
   ```

2. **Check Feature Availability**:
   ```java
   if (LSPatchCompat.isFeatureAvailable("SYSTEM_HOOKS")) {
       // Safe to use system hooks
   }
   ```

3. **Use Compatible Preferences**:
   ```java
   // Automatically adapts to environment
   LSPatchPreferences prefs = new LSPatchPreferences(context);
   ```

## Limitations and Known Issues

### LSPatch Embedded Mode
- System hooks (ScopeHook, AndroidPermissions) are disabled for security
- Resource hooks may have limited functionality
- Bridge service uses fallback mechanisms

### LSPatch Manager Mode
- Requires LSPatch manager app to be installed
- Some advanced features may need additional permissions
- Bridge service should work normally

### General
- Bootloader spoofer requires additional verification
- Some Xposed APIs may behave differently
- Performance may be slightly different from traditional Xposed

## Support and Debugging

### Debug Information

Enable debug mode to get detailed status information:

```java
String detailedStatus = LSPatchModuleStatus.getDetailedStatus();
Log.d("WaEnhancer", detailedStatus);
```

### Common Issues

1. **"Module Desactivado" Message**: 
   - Check that WhatsApp was properly patched with LSPatch
   - Verify WaEnhancer is embedded in the patched APK
   - Check LSPatch manager installation (for manager mode)

2. **Features Not Working**:
   - Check feature compatibility matrix
   - Verify bridge service is connected
   - Check logs for specific error messages

3. **Settings Not Saving**:
   - LSPatch uses different preference mechanisms
   - Check preference adapter initialization
   - Verify file permissions for embedded mode

## Conclusion

WaEnhancer now provides comprehensive LSPatch compatibility while maintaining full functionality with traditional Xposed frameworks. The automatic detection and adaptation systems ensure a seamless experience regardless of the underlying framework.

For additional support or to report LSPatch-specific issues, please include the detailed status information from `LSPatchModuleStatus.getDetailedStatus()` in your bug reports.
