# macOS App Bundle Build

This directory contains the configuration to build a native macOS application bundle (.app) for Forge.

## Building

From the project root:
```bash
mvn clean package -Pmacos -DskipTests
```

## Output

The build creates:
- `target/Forge.app` - Native macOS application bundle (main game)
- `target/Forge Adventure.app` - Native macOS application bundle (adventure mode)
- `target/forge-installer-*.dmg` - Disk image for distribution (contains both apps, only created on macOS)

## App Bundle Structure

```
Forge.app/
├── Contents/
│   ├── Info.plist          # App metadata
│   ├── MacOS/
│   │   └── forge           # Launcher script
│   └── Resources/          # Application resources
│       ├── forge-gui-desktop-*.jar
│       ├── res/            # Game resources
│       └── *.txt           # Documentation files
```

## Requirements

- macOS 10.9 (Mavericks) or later
- Java 17 or later (bundled JRE not included)
- Maven 3.6+

## Installation

Users can:
1. Mount the DMG file
2. Drag Forge.app to Applications folder
3. Launch from Applications or Launchpad

## Notes

- The app bundles are created on any OS and require Java to be installed on the target system
- Icon conversion from PNG to ICNS happens automatically if available
- DMG creation only works on macOS (requires `hdiutil` command) but is optional - the .app bundles are always created