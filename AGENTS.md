# Forge - AI Coding Agent Configuration

## Project Overview

Forge is an open-source, cross-platform Magic: The Gathering rules engine written in Java. Started in 2007, it provides a complete MTG game experience with AI opponents, multiple game modes, and network play support.

**License:** GPL-3.0 | **Repository:** https://github.com/Card-Forge/forge

Forge is maintained by a small group of part-time volunteers. Contributions should be well-documented, self-contained, and respectful of reviewer time: small PRs, minimal diffs, and clear descriptions.

## Tech Stack

- **Language:** Java 17+ | **Build:** Maven 3.8.1+ | **Testing:** TestNG
- **GUI:** Java Swing (desktop), Libgdx (mobile)
- **Libraries:** Guava, JGraphT, Sentry

## Project Structure

```
forge/
├── forge-core/          # Core engine: card definitions, utilities
├── forge-game/          # Rules engine: combat, mana, zones, phases
├── forge-ai/            # AI opponent: decision algorithms
├── forge-gui/           # Shared GUI and resources (platform-neutral)
├── forge-gui-desktop/   # Desktop GUI (Swing)
├── forge-gui-mobile/    # Mobile GUI (Libgdx)
├── forge-gui-android/   # Android backend (Libgdx)
├── forge-gui-ios/       # iOS backend (Libgdx)
└── docs/                # Project documentation
    └── Development/
        ├── Guidelines.md    # Code review guidelines
        └── Architecture.md  # GUI and network architecture reference
```

For detailed architecture documentation (GUI hierarchy, layer responsibilities, network infrastructure), see [docs/Development/Architecture.md](docs/Development/Architecture.md).

**Card scripting:** Card definitions use a text-based scripting DSL and live in `forge-gui/res/cardsfolder/`. See the [Card Scripting API](https://github.com/Card-Forge/forge/wiki/Card-scripting-API) wiki for reference.

## Before Making Changes

You MUST read and follow these documents before writing or planning code:

- **[docs/Development/Guidelines.md](docs/Development/Guidelines.md)** — Code review guidelines distilled from PR feedback. These are the most common cause of PR rejection when violated. Covers general principles, code style, architecture rules, network patterns, and testing requirements.
- **[docs/Development/Architecture.md](docs/Development/Architecture.md)** — GUI inheritance hierarchy, layer responsibilities, and network infrastructure. Read before modifying any GUI or network code.

Do not treat these as optional reference material. Review the relevant sections before designing an approach, and verify your implementation against them before submitting.

## Build & Test Commands

```bash
# Incremental build (default for iterative development)
mvn -pl forge-gui -am install -DskipTests

# Desktop build (for testing)
mvn -pl forge-gui-desktop -am install -DskipTests

# Quick compile check (fastest, syntax checking only)
mvn -pl forge-gui -am compile

# Compile a single module (fastest feedback loop)
mvn -pl forge-game compile

# Run a single test (IMPORTANT: use verify, not test)
mvn -pl forge-gui-desktop -am verify -Dtest="TestClassName#methodName" -Dsurefire.failIfNoSpecifiedTests=false

# Checkstyle (checks RedundantImport and UnusedImports only)
mvn checkstyle:check

# Install dependencies (also happens implicitly during compile/install)
mvn dependency:resolve

# Full distributable installer (Windows/Linux)
mvn -U -B clean -P windows-linux install -DskipTests

# Full distributable installer (Mac)
mvn -U -B clean -P osx install -DskipTests
```

**Key notes:**
- Always use incremental builds (`-pl forge-gui -am`) for development iteration. Full installer builds are slow.
- Use `-pl <module>` to scope commands to a single module for faster feedback.
- Use `verify` (not `test`) when running individual tests.
- Desktop GUI tests require a display. CI uses Xvfb for headless testing.

## CI Environment

**Workflow:** `.github/workflows/test-build.yaml` runs on `ubuntu-latest` with Java 17 and 21 (Temurin), Xvfb virtual framebuffer, on every push and pull request.

**CI command:** `mvn -U -B clean test`

**What CI checks:**
- Unit tests (`testDeckLoaderHasPrecons`, `testGameResultInitialization`, `testConfigurationParsing`, etc.)
- Checkstyle: `RedundantImport` and `UnusedImports` only (see `checkstyle.xml`)

**Note:** Fork PRs require maintainer approval before CI workflows run. This is a GitHub Actions security feature, not a test failure.

## Development Principles

These are the most critical principles. See [docs/Development/Guidelines.md](docs/Development/Guidelines.md) for the complete set.

1. **Ask clarifying questions before starting work.** When a task has ambiguities in scope, approach, or trade-offs, surface options and let the maintainers decide before proceeding.
2. **Search for existing code before creating new functionality.** Before implementing, search the codebase for existing mechanisms that solve the same or similar problem. Enhance existing code rather than creating parallel implementations.
3. **Avoid over-engineering.** Solve the problem with the simplest approach that works. Don't introduce new classes, event types, or abstractions when existing infrastructure can be reused. Prefer modifying 3 files over creating 10 new ones.
4. **Prototype the minimal version.** Before building new infrastructure, ask: "Can this be done by composing existing methods with a few lines at the call site?" If yes, do that.
5. **Read code before modifying it.** Understand existing code before suggesting changes. Search for all callers of a method before changing its behavior.
6. **Trace execution contexts.** Before implementing, enumerate the runtime contexts where the affected code will execute: local single-player, local multiplayer, network host, network client, AI opponent. Verify the change is appropriate in each.
7. **Minimal diff.** Prefer small, focused changes. Do not make cosmetic fixes (whitespace, formatting, style) to code that isn't otherwise being changed for functional reasons.

## Code Review Guidelines

See [docs/Development/Guidelines.md](docs/Development/Guidelines.md) for the complete code review guidelines. These are distilled from PR reviewer feedback and cover general principles, code style, architecture rules, network-specific patterns, and testing requirements. Following them avoids the most common causes of PR rejection.

## Git Conventions

- Do not commit files listed in `.gitignore`.
- Write focused commits with clear, descriptive messages.
- Fork PRs require maintainer approval before CI workflows run. This is a GitHub Actions security feature, not a test failure.

## Further Reading

- **[docs/Development/Guidelines.md](docs/Development/Guidelines.md)** - Complete code review guidelines distilled from PR feedback
- **[docs/Development/Architecture.md](docs/Development/Architecture.md)** - GUI hierarchy, layer responsibilities, and network architecture
- **[CONTRIBUTING.md](CONTRIBUTING.md)** - Contributor setup guide (IDE, SDK, platform builds)
- **[Card Scripting API](https://github.com/Card-Forge/forge/wiki/Card-scripting-API)** - Card scripting reference
