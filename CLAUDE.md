# Forge - Magic: The Gathering Rules Engine

## Project Overview
Forge is an open-source, cross-platform Magic: The Gathering rules engine written in Java. Started in 2007, it provides a complete MTG game experience with AI opponents, multiple game modes (Draft, Sealed, Commander, Cube, Adventure/Shandalar), and network play support.

**License:** GPL-3.0
**Repository:** https://github.com/Card-Forge/forge

## Tech Stack
- **Language:** Java 17+
- **Build:** Maven 3.8.1+
- **GUI:** Java Swing (desktop), Libgdx (mobile)
- **Testing:** TestNG
- **Key Libraries:** Guava (collections), JGraphT (graph algorithms), Sentry (error tracking)

## Project Structure

```
forge/
├── forge-core/          # Core engine: card definitions, dec1k management, utilities
├── forge-game/          # Game session: rules engine, combat, mana, zones, phases
├── forge-ai/            # AI opponent: decision algorithms, game simulation
├── forge-gui/           # Shared GUI components and resources
│   └── res/             # Game resources (card scripts, editions, AI decks)
│       └── cardsfolder/ # Individual card implementations (2000+ files)
├── forge-gui-desktop/   # Desktop GUI (Java Swing)
├── forge-gui-mobile/    # Mobile GUI framework (Libgdx)
├── forge-gui-android/   # Android implementation
├── forge-gui-ios/       # iOS implementation
├── forge-gui-mobile-dev/# Mobile dev testing on desktop
├── adventure-editor/    # Adventure mode content editor
└── docs/                # Comprehensive documentation
```

### Key Directories in forge-game
- `ability/` - Spell ability implementations
- `card/` - Card game state management
- `combat/` - Combat system logic
- `cost/` - Mana and ability costs
- `event/` - Game event system
- `keyword/` - Keyword ability handling
- `mana/` - Mana pool and production
- `phase/` - Turn phase management
- `player/` - Player state and actions
- `replacement/` - Replacement effects
- `trigger/` - Triggered abilities
- `zone/` - Game zones (hand, library, battlefield, graveyard)

## Build Commands

**Maven Path (Windows):** `"C:\Program Files\Apache\maven\bin\mvn.cmd"`

```bash
# Clean build
mvn -U -B clean install

# Windows/Linux profile build
mvn -U -B clean -P windows-linux install

# Run tests
mvn clean test

# Build specific module
mvn -pl forge-game clean install
```

## Running
Desktop GUI requires display. CI uses Xvfb for headless testing.

## Key Entry Points
- Card definitions: `forge-gui/res/cardsfolder/` (one file per card)
- Effect implementations: `forge-game/src/main/java/forge/game/ability/effects/`
- AI decisions: `forge-ai/src/main/java/forge/ai/ability/`
- Game state: `forge-game/src/main/java/forge/game/card/Card.java`
- Trigger system: `forge-game/src/main/java/forge/game/trigger/TriggerHandler.java`

## Adding New Cards
1. Create card script in `forge-gui/res/cardsfolder/[first_letter]/`
2. Implement any new effects in `forge-game/.../ability/effects/`
3. Add AI logic in `forge-ai/.../ability/`

See: `docs/Card-scripting-API/`, `docs/Creating-a-custom-Card.md`

## Important Patterns
- **No DI framework** - Manual constructor injection throughout
- **View separation** - Mutable game objects have immutable `*View` counterparts
- **Effect pattern** - All abilities extend `SpellAbilityEffect` (forge-game:43-50)
- **Predicates** - Filter logic in `*Predicates.java` factory classes
- **Handlers** - Centralized event processing (`TriggerHandler`, `ReplacementHandler`)

## Current Branch Context

**Important**: When working on this repository, check the current git branch. If on a feature branch, read the corresponding documentation:

- **NetworkPlay branch**: Read `Branch_Documentation.md` for details on delta synchronization, reconnection support, and recent changes. This file tracks the implementation status and architectural decisions for network play optimizations.

## Additional Documentation

### Architecture & Patterns
- `.claude/docs/architectural_patterns.md` - Design patterns, conventions, code organization

### Official Documentation (docs/)
**Development:**
- `docs/Card-scripting-API/` - Complete card scripting reference
- `docs/Creating-a-custom-Card.md` - Card creation tutorial
- `docs/Creating-a-custom-Set.md` - Set creation guide
- `docs/File-Formats.md` - Data file specifications
- `docs/Development/` - Developer-specific documentation

**Adventure Mode:**
- `docs/Adventure/` - Adventure mode docs (~20 files)
- `docs/Adventure/Create-new-Maps.md`, `Create-Enemies.md`, `Create-Rewards.md`
- `docs/Adventure/Modding.md` - Custom content creation

**User & Setup:**
- `docs/User-Guide.md` - Main gameplay manual
- `docs/Network-Play.md`, `docs/Network-FAQ.md` - Multiplayer setup
- `docs/Docker-Setup.md` - Docker containerization

### Contributing
- `CONTRIBUTING.md` - Contribution guidelines and IDE setup
- `checkstyle.xml` - Code style rules (enforced by Maven)

## Quick Reference

| What | Where |
|------|-------|
| Card scripts | `forge-gui/res/cardsfolder/` |
| Effect code | `forge-game/.../ability/effects/` |
| AI behavior | `forge-ai/.../ability/` |
| Game state | `forge-game/.../game/` |
| Desktop UI | `forge-gui-desktop/.../screens/` |
| Test base | `forge-gui-desktop/.../gamesimulationtests/BaseGameSimulationTest.java` |
