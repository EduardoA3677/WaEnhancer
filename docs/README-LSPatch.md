# WaEnhancer LSPatch Compatibility

WaEnhancer is fully compatible with LSPatch, providing rootless Xposed functionality for WhatsApp enhancement without requiring root access.

## What is LSPatch?

LSPatch is a rootless implementation of the Xposed framework that allows running Xposed modules by patching APKs directly. This means you can use WaEnhancer without rooting your device.

## Supported LSPatch Modes

### 1. Embedded Mode (Recommended)

- **Description**: The module is embedded directly into the WhatsApp APK
- **Advantages**: Better performance, more stable, better feature support
- **Limitations**: Requires repatching APK for updates
- **Status**: ✅ Fully supported with most features available

### 2. Manager Mode

- **Description**: Module is loaded through LSPatch manager app
- **Advantages**: Easier module management, no APK repatching needed
- **Limitations**: More restrictions, limited resource modifications
- **Status**: ⚠️ Supported with limitations

## Feature Compatibility

### ✅ Fully Compatible Features

- **UI Modifications**: All interface customizations work normally
- **Message Features**: Anti-revoke, read receipts, typing indicators
- **Media Features**: Download status, view once bypass, media quality
- **Privacy Features**: Hide online status, freeze last seen, ghost mode
- **Customization**: Bubble colors, custom time, hide tabs
- **General Features**: Chat limits, pinned limits, new chat features

### ⚠️ Limited Functionality

- **Theme Customization**: Limited in manager mode, full support in embedded mode
- **Resource Modifications**: May have reduced functionality in manager mode
- **Custom CSS**: Limited resource hook capabilities in manager mode

### ❌ Not Available in LSPatch

- **Bootloader Spoofer**: Requires system-level access not available in LSPatch
- **System Server Hooks**: Deep system modifications are blocked for security
- **Android Permissions Modification**: System permission changes not supported
- **Anti-Detection (Advanced)**: Some deep anti-detection features unavailable

## Automatic Feature Filtering

WaEnhancer automatically:

- **Detects LSPatch environment** and adjusts functionality accordingly
- **Hides incompatible settings** from the UI to avoid confusion
- **Shows clear status** indicating LSPatch mode and limitations
- **Provides warnings** for features with limited functionality

- LSPatch version is 0.6 or newer
- WaEnhancer was properly embedded
- Signature bypass is enabled

**Q: Some features missing**
A: Resource-dependent features may not work in manager mode. Use embedded mode for full compatibility.

**Q: App crashes on startup**
A: Ensure WhatsApp version is supported and try regenerating the patch with signature bypass level 2.

### Debug Information

Enable logging in WaEnhancer settings and check logcat for messages tagged with `[LSPatch]`.

## Advanced Usage

### Command Line Patching

```bash
java -jar lspatch.jar \
  --embed waenhancer.apk \
  --sigbypasslv 2 \
  --debuggable \
  whatsapp.apk
```

### Multiple Modules

You can embed multiple Xposed modules in a single patch:

```bash
java -jar lspatch.jar \
  --embed waenhancer.apk \
  --embed other-module.apk \
  whatsapp.apk
```

## Technical Details

WaEnhancer includes a comprehensive LSPatch compatibility layer:

- Automatic environment detection
- Optimized hook management
- Enhanced error handling
- Resource fallback mechanisms
- Service integration for both embedded and manager modes

For complete technical documentation, see [LSPatch-Compatibility.md](LSPatch-Compatibility.md).

## Version Compatibility

| Component  | Minimum Version | Recommended   |
| ---------- | --------------- | ------------- |
| LSPatch    | 0.6             | Latest        |
| WaEnhancer | 1.5.0           | Latest        |
| WhatsApp   | 2.23.25.xx      | Latest stable |
| Android    | 8.0 (API 26)    | 10+           |

## Support

For LSPatch-related issues:

1. Check this guide first
2. Enable debug logging
3. Check our [Issues](../../issues) page
4. For LSPatch framework issues, visit [LSPatch GitHub](https://github.com/LSPosed/LSPatch)

---

**Note**: Using modified WhatsApp may violate WhatsApp's Terms of Service. Use at your own risk.
