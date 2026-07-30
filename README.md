# Reforge Commander

Reforge Commander is a **modern, Commander-first** fork of [Card-Forge/forge](https://github.com/Card-Forge/forge). It strips the bloated multi-mode surface area of upstream Forge to deliver a fast, clean client optimized for playing Commander with friends.

Target: a new user should build a deck and start a game in under two minutes. An expert should be able to customize everything.

> Note: Reforge Commander operates independently and is not affiliated with Wizards of the Coast.

---

## Core Focus

- **Commander-first UX**: The entire UI is optimized around Commander/EDH. Non-Commander modes are hidden. Smart defaults reduce setup friction.
- **Multiplayer-first**: Playing with friends is the primary flow. AI games exist but the UX prioritizes lobby, invites, and match setup.
- **Performance at scale**: Flyweight token engine (`StackedTokenCard`) keeps large board states (Scute Swarm, Krenko) snappy. O(1) memory for identical token populations.
- **Personalization without clutter**: Powerful deck tools, theme options, and preference knobs for power users — surfaced only when needed.
- **Upstream-compatible**: Additive-only code changes. Card scripts and rules updates merge cleanly from `Card-Forge/forge`.

> **Note:** The flyweight optimization class exists but is not yet wired into the game engine. See [Development Status](docs/development.md) for the integration roadmap.

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

---

## License

Reforge Commander inherits the original project's license: [GPL-3.0](LICENSE).

