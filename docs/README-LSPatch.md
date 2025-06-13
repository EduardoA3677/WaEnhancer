# WaEnhancer + LSPatch Integration

[English](README.md) | [Português](README.pt-BR.md) | **LSPatch Guide**

## Quick Start with LSPatch

LSPatch allows you to use WaEnhancer without root access by patching WhatsApp directly.

### Step 1: Download Requirements

- [LSPatch Manager](https://github.com/LSPosed/LSPatch/releases) (latest version)
- [WaEnhancer APK](../../releases) (this module)
- WhatsApp APK (original from Google Play or APKMirror)

### Step 2: Patch WhatsApp

1. Install LSPatch Manager
2. Open LSPatch Manager
3. Select "New Patch"
4. Choose your WhatsApp APK
5. Add WaEnhancer module
6. Select patch options:
   - ✅ Signature bypass level 2 (recommended)
   - ✅ Debuggable (for troubleshooting)
   - Choose embedded mode for full features
7. Create patch

### Step 3: Install Patched App

1. Uninstall original WhatsApp (backup chats first!)
2. Install the patched WhatsApp APK
3. Restore your chats
4. Open WaEnhancer settings to configure features

## Features Status in LSPatch

| Feature Category     | LSPatch Support | Notes                                |
| -------------------- | --------------- | ------------------------------------ |
| 🔒 Privacy Features  | ✅ Full         | Hide seen, Anti-revoke, etc.         |
| 📱 Media Features    | ✅ Full         | Download status, View once, etc.     |
| 🎨 Customization     | ⚠️ Partial      | Resource-dependent features may vary |
| ⚙️ General Features  | ✅ Full         | Chat limits, New chat, etc.          |
| 🔧 Advanced Features | ✅ Full         | Tasker, Debug tools, etc.            |

## Troubleshooting

### Common Issues

**Q: WaEnhancer not working after patch**
A: Check that:

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
