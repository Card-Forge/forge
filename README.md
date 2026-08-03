# Reforge Commander

Reforge Commander is a **modern, Commander-first** fork of [Card-Forge/forge](https://github.com/Card-Forge/forge). It strips the bloated multi-mode surface area of upstream Forge to deliver a fast, clean client optimized for playing Commander with friends.

Target: a new user should build a deck and start a game in under two minutes. An expert should be able to customize everything.

> Note: Reforge Commander operates independently and is not affiliated with Wizards of the Coast.

---

## Core Focus

- **Commander-first UX**: The entire UI is optimized around Commander/EDH. Non-Commander modes are hidden. Smart defaults reduce setup friction.
- **Multiplayer-first**: Playing with friends is the primary flow. AI games exist but the UX prioritizes lobby, invites, and match setup.
- **Performance at scale**: Flyweight token engine (`StackedTokenCard`) is wired into token creation. Static-eval batching (1c) and GameCopier flyweight (1d) still needed to deliver the full O(1) win in real play. See [Development Status](docs/development.md).
- **Personalization without clutter**: Powerful deck tools, theme options, and preference knobs for power users — surfaced only when needed.
- **Upstream-compatible**: Additive-only code changes. Card scripts and rules updates merge cleanly from `Card-Forge/forge`.

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

# Run Reforge Commander directly from source
mvn -P run-commander exec:java -pl :forge-gui-desktop
```

---

## Development Status

See [docs/development.md](docs/development.md) for the full roadmap, known gaps, and priority fixes.

### 📱 iOS Installation (early stage)
- Build the **IPA** according to Wiki
- No jailbreak needed, only developer mode
- Connect your device to a PC to self-sign and upload the app file, multiple tools exist e.g. [Sideloadly](https://sideloadly.io)

---

## License

Reforge Commander inherits the original project's license: [GPL-3.0](LICENSE).

