# Reforge Commander

Reforge Commander is a Commander-focused fork of [Card-Forge/forge](https://github.com/Card-Forge/forge).

It aims to optimize engine performance for large token board states (e.g. Scute Swarm, Krenko), streamline the UI around Commander/EDH gameplay, and maintain zero-conflict upstream synchronization with official Forge card releases.

> Note: Reforge Commander operates independently and is not affiliated with Wizards of the Coast or the core Card-Forge development team.

---

## Core Focus & Differences

- **Token Engine Optimization**: Introduces flyweight token handling (`StackedTokenCard`) and optimized AI state cloning to eliminate lag and timeouts on heavy token battlefields.
- **UI Streamlining**: Isolates interface submenus to focus directly on Commander matches and deck building.
- **Java 17 Bytecode Baseline**: Global Maven compiler targeting for cross-platform Java 17+ compatibility.
- **Upstream Synchronization**: Maintained in structural alignment with `Card-Forge/forge` main repository to merge card scripts and rules updates effortlessly.

---

## Building from Source

### Prerequisites
- JDK 17 or higher
- Apache Maven 3.9+

### Build Commands

```bash
# Build complete reactor skipping tests
mvn clean install -DskipTests

# Build desktop client module only
mvn clean install -pl :forge-gui-desktop -am -DskipTests
```

---

## License

Reforge Commander inherits the original project's license: [GPL-3.0](LICENSE).

