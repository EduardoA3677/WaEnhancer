# LSPatch Compatibility Guide for WaEnhancer

## Overview

WaEnhancer now includes full compatibility with LSPatch, allowing it to run without root access by patching WhatsApp APKs directly. LSPatch is a rootless implementation of the Xposed framework that works by modifying app APKs to include the framework and modules.

## Features

### LSPatch Compatibility Layer

- **Automatic Detection**: WaEnhancer automatically detects when running in an LSPatch environment
- **Mode Detection**: Supports both embedded mode (modules included in APK) and manager mode (modules managed externally)
- **Optimized Hooks**: Applies LSPatch-specific optimizations for better stability
- **Error Recovery**: Includes enhanced error handling for LSPatch environments

### Supported LSPatch Features

- ✅ **Signature Bypass**: Built-in signature verification bypass
- ✅ **Embedded Mode**: Modules embedded directly in the patched APK
- ✅ **Manager Mode**: Modules managed through LSPatch Manager app
- ✅ **Resource Hooks**: Support for resource modification (mode-dependent)
- ❌ **System Server Hooks**: Not supported (LSPatch limitation)

### WaEnhancer Features in LSPatch

All WaEnhancer features are compatible with LSPatch:

- Privacy features (Hide seen, Anti-revoke, etc.)
- Media features (Download status, View once, etc.)
- Customization features (Custom themes, Bubble colors, etc.)
- General features (Chat limits, New chat, etc.)

## Installation Methods

### Method 1: Using LSPatch Manager

1. Install LSPatch Manager from GitHub
2. Install WaEnhancer APK normally
3. Use LSPatch Manager to patch WhatsApp with WaEnhancer module
4. Install the patched WhatsApp APK

### Method 2: Command Line LSPatch

1. Download LSPatch CLI tools
2. Use command: `java -jar lspatch.jar --embed waenhancer.apk whatsapp.apk`
3. Install the generated patched APK

### Method 3: Pre-patched APK

1. Download a pre-patched WhatsApp APK with WaEnhancer
2. Install directly (no additional steps required)

## Configuration

### LSPatch-Specific Settings

WaEnhancer includes several LSPatch-specific optimizations that are automatically applied:

- **Hook Stability**: Enhanced hook stability for non-root environments
- **Memory Conservation**: Reduced memory usage in LSPatch environments
- **Error Recovery**: Graceful handling of LSPatch-specific errors
- **Resource Fallbacks**: Alternative resource handling when standard resource hooks are unavailable

### Debug Information

Enable debug logging to see LSPatch-specific information:

1. Open WaEnhancer settings
2. Go to General → Advanced
3. Enable "Enable Logs"
4. Check logcat for LSPatch-related messages tagged with `[LSPatch]`

## Troubleshooting

### Common Issues

**Issue**: WaEnhancer features not working after LSPatch installation
**Solution**:

- Ensure you're using a compatible LSPatch version (0.6+)
- Check that WaEnhancer was properly embedded in the patch
- Verify that the patched APK was signed correctly

**Issue**: Resource-related features not working
**Solution**:

- Some LSPatch modes have limited resource hook support
- Check logs for resource hook availability warnings
- Consider using embedded mode for full resource support

**Issue**: App crashes on startup
**Solution**:

- Ensure WhatsApp version is supported by WaEnhancer
- Check that signature bypass is enabled in LSPatch
- Try regenerating the patch with different settings

### Verification

To verify LSPatch compatibility is working:

1. Open WhatsApp (patched with WaEnhancer)
2. Check logcat for: `[WaEnhancer] Running in LSPatch mode`
3. Look for LSPatch mode detection: `LSPATCH_EMBEDDED` or `LSPATCH_MANAGER`
4. Verify feature availability messages

## Technical Details

### Architecture

```
WaEnhancer Module
├── LSPatchCompat.java (Environment detection)
├── LSPatchPreferences.java (Preferences handling)
├── LSPatchBridge.java (Service integration)
├── LSPatchConfig.java (Configuration management)
├── LSPatchHookWrapper.java (Hook optimizations)
└── FeatureLoader.java (Main initialization)
```

### Detection Mechanism

WaEnhancer detects LSPatch through multiple methods:

1. Class availability checks (LSPatch-specific classes)
2. System property checks (`lspatch.enabled`)
3. Environment variable checks (`LSPATCH_ACTIVE`)
4. Asset file checks (`assets/lspatch/config.json`)

### Hook Optimizations

- **Priority Setting**: Higher priority for LSPatch hooks
- **Verification**: Enhanced hook verification
- **Timeout Handling**: Longer timeouts for hook operations
- **Error Wrapping**: LSPatch-specific error handling

## Version Compatibility

### LSPatch Versions

- **Minimum Required**: 0.6
- **Recommended**: Latest stable release
- **Tested With**: 0.6, 0.7, 0.8+

### WhatsApp Versions

All WhatsApp versions supported by WaEnhancer are compatible with LSPatch:

- **WhatsApp**: 2.23.25.xx and newer
- **WhatsApp Business**: 2.23.25.xx and newer

## Support

For LSPatch-specific issues:

1. Check the troubleshooting section above
2. Enable debug logging and check for LSPatch-related messages
3. Report issues on the WaEnhancer GitHub repository with LSPatch version and logs
4. For LSPatch framework issues, refer to the LSPatch project documentation

## Development Notes

For developers working with WaEnhancer and LSPatch:

- Use `LSPatchCompat.isLSPatchEnvironment()` to detect LSPatch
- Use `LSPatchHookWrapper` for optimized hooks
- Check feature availability with `LSPatchCompat.isFeatureAvailable()`
- Handle resource limitations gracefully in manager mode
- Test with both embedded and manager modes
