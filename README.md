# Reforge Commander

> [!WARNING]
> **This is a personal project, not a community one.**
>
> Reforge Commander is a private fork of [Card-Forge/forge](https://github.com/Card-Forge/forge) that I
> built to play Magic: The Gathering Commander with friends. It is **not** a general-use release, and it
> is **not** a supported community fork of the original project with the same rules and standards.
>
> - **AI-generated code.** Most of the code here was written with AI assistance. It probably contains
>   bugs or errors. Expect the unexpected.
> - **Use at your own risk.** The project is provided as-is, with no warranty of any kind. I take no
>   responsibility for anything that happens as a result of building, running, or using it.
> - **Not affiliated** with the Forge project/team or with Wizards of the Coast. Don't confuse this with
>   the original Forge project or expect it to follow their standards.
> - **Contributions are welcome, but nothing is guaranteed.** You may open issues or pull requests, and
>   I may or may not get to them. There is no review schedule, no merge promise, and no maintenance
>   commitment.

---

## What it is

Reforge Commander is a **modern, Commander-first** fork of [Card-Forge/forge](https://github.com/Card-Forge/forge).
It strips the bloated multi-mode surface area of upstream Forge to deliver a fast, clean client optimized
for playing Commander with friends.

Target: a new user should build a deck and start a game in under two minutes. An expert should be able to
customize everything.

## Core Focus

- **Commander-first UX**: The entire UI is optimized around Commander/EDH. Non-Commander modes are hidden. Smart defaults reduce setup friction.
- **Multiplayer-first**: Playing with friends is the primary flow. AI games exist but the UX prioritizes lobby, invites, and match setup.
- **Performance at scale**: Flyweight token engine (`StackedTokenCard`) is wired into token creation. Static-eval batching (1c) and GameCopier flyweight (1d) still needed to deliver the full O(1) win in real play. See [Development Status](docs/development.md).
- **Personalization without clutter**: Powerful deck tools, theme options, and preference knobs for power users — surfaced only when needed.
- **Upstream-compatible**: New code prefers extending upstream classes to stay mergeable with `Card-Forge/forge`; direct edits to upstream files are allowed when a small, marked change beats a fragile workaround.

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

### iOS Installation (early stage)
- Build the **IPA** according to the [iOS Builds guide](docs/Development/iOS-Builds.md)
- No jailbreak needed, only developer mode
- Connect your device to a PC to self-sign and upload the app file, multiple tools exist e.g. [Sideloadly](https://sideloadly.io)

---

## License

Reforge Commander inherits the original project's license: [GPL-3.0](LICENSE).
