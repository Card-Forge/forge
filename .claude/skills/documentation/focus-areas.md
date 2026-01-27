# Documentation Focus Areas

## Focus Area Mappings

| Focus | Primary File | Key Sections |
|-------|--------------|--------------|
| testing | .documentation/Testing.md | Test results, components, usage |
| delta | .documentation/NetworkPlay.md | Delta Synchronization section |
| reconnection | .documentation/NetworkPlay.md | Reconnection Support section |
| bugs | .documentation/Debugging.md | Active bugs, resolved status |
| architecture | .documentation/NetworkPlay.md | Architecture, class hierarchies |
| all | All files | Comprehensive review |

## Source Locations for Verification

| Component | Path |
|-----------|------|
| Network code | `forge-gui/src/main/java/forge/gamemodes/net/` |
| Test code | `forge-gui-desktop/src/test/java/forge/net/` |
| Delta sync | `DeltaSyncManager.java`, `NetworkGuiGame.java`, `DeltaPacket.java` |
| Headless testing | `HeadlessGuiDesktop.java`, `NoOpGuiGame.java` |

## Documentation Scope

### Include

- Feature descriptions and capabilities
- Architecture and class relationships
- How to use features/tests
- Bug symptoms and solutions
- Configuration and setup

### Exclude

- Development methodology or approach
- Implementation phase discussions
- Planning notes or future considerations
- Day-by-day debugging progress
- Tool usage during development

**Exception**: .documentation/Debugging.md may reference development process to explain debugging steps and how solutions were reached.

## Cross-Reference Checks

### .documentation/NetworkPlay.md
- Class hierarchy descriptions match actual inheritance
- File paths in "Files Modified" sections exist
- Feature descriptions match implementation
- Configuration options exist

### .documentation/Testing.md
- Test class names exist
- Test command examples work
- Test result counts are recent
- Component descriptions match code

### .documentation/Debugging.md
- "Under Investigation" bugs resolved?
- "Fix Applied - Pending Verification" bugs tested?
- File references valid?
