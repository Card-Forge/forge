# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Forge is an open-source Magic: The Gathering rules engine and game client written in Java. It supports cross-platform play (Windows, Mac, Linux, Android) and includes single-player modes (Adventure, Quest) and multiplayer formats (Sealed, Draft, Commander, Cube).

## Build Commands

### Initial Setup
```bash
# Download dependencies and build snapshot (Windows & Linux)
mvn -U -B clean -P windows-linux install

# For other platforms, use appropriate profile
```

### Common Build Commands
```bash
# Clean build
mvn clean install

# Run tests
mvn test

# Build without tests
mvn install -DskipTests

# Build desktop version
mvn clean install -pl forge-gui-desktop -am

# Build Android version
mvn clean install -pl forge-gui-android -am
```

### Running the Application
- **Desktop (Swing GUI)**: Run `forge.view.Main` from `forge-gui-desktop` module
- **Mobile Dev (Adventure Mode)**: Run `forge.app.Main` from `forge-gui-mobile-dev` module

VM options required (Java 17+):
```
-Xms768m -XX:+UseParallelGC -Dsun.java2d.xrender=false --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.text=ALL-UNNAMED --add-opens java.desktop/java.awt.font=ALL-UNNAMED --add-opens java.base/jdk.internal.misc=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/java.math=ALL-UNNAMED --add-opens java.base/java.util.concurrent=ALL-UNNAMED --add-opens java.desktop/java.awt=ALL-UNNAMED --add-opens java.base/java.net=ALL-UNNAMED --add-opens java.desktop/javax.swing=ALL-UNNAMED --add-opens java.desktop/java.beans=ALL-UNNAMED --add-opens java.desktop/javax.swing.border=ALL-UNNAMED -Dio.netty.tryReflectionSetAccessible=true
```

## Project Architecture

### Module Hierarchy

Forge uses a multi-module Maven structure with four primary modules and several platform-specific modules:

#### Primary Modules

**forge-core**: Core game engine containing fundamental game logic, card mechanics, rules engine, and game state management. Includes the implementation of MTG rules and card interactions.

**forge-game**: Handles game session management, player interactions, and game flow control. Bridges the core engine with UI and networking components. Contains:
- `forge.game.ability`: Ability system and effects (AbilityFactory, ApiType, effects/*)
- `forge.game.card`: Card objects and card-related logic
- `forge.game.combat`: Combat system
- `forge.game.player`: Player state and actions
- `forge.game.spellability`: Spell and ability framework
- `forge.game.trigger`: Triggered ability system
- `forge.game.replacement`: Replacement effect system
- `forge.game.staticability`: Static ability system
- `forge.game.zone`: Game zones (hand, battlefield, graveyard, etc.)

**forge-ai**: Computer opponent logic including decision-making algorithms for abilities, cards, and turn phases. Key files:
- `AiController.java`: Main AI controller
- `ComputerUtil*.java`: Utility classes for AI decisions
- `SpellAbilityAi.java`: AI logic for casting spells/abilities
- `simulation/`: Game state simulation for planning

**forge-gui**: User interface components and scripting resources. Contains the card scripting definitions in `res/` directory:
- `res/cardsfolder/`: Card script files (organized alphabetically)
- `res/adventure/`: Adventure mode data
- `res/editions/`: Set/edition definitions
- `release-files/`: Distribution files

#### Platform-Specific Modules

- **forge-gui-desktop**: Java Swing-based GUI for desktop (main class: `forge.view.Main`)
- **forge-gui-mobile**: Libgdx-based mobile GUI logic shared by Android/iOS
- **forge-gui-mobile-dev**: Libgdx desktop backend for mobile development (main class: `forge.app.Main`)
- **forge-gui-android**: Android-specific backend (requires Android SDK)
- **forge-gui-ios**: iOS-specific backend (RoboVM)

## Card Scripting

Card definitions are text files in `forge-gui/res/cardsfolder/` organized alphabetically by first letter. Each card is a `.txt` file with properties:

### Basic Card Structure
```
Name:Card Name
ManaCost:2 G
Types:Creature Beast
PT:2/2
K:Keyword abilities
A:Ability effect
T:Triggered ability
S:Static ability
R:Replacement effect
SVar:String variables
Oracle:Oracle text
```

### Key Properties
- `Name`: Card name (filename must match: lowercase, no special chars, underscores for spaces)
- `ManaCost`: Mana cost (e.g., `2 G G` for {2}{G}{G}, `no cost` for uncastable)
- `Types`: Card types and subtypes (space-separated)
- `PT`: Power/toughness for creatures
- `K:` Keyword abilities (one per line)
- `A:` Ability effects (uses AbilityFactory API)
- `T:` Triggered abilities (uses Trigger API)
- `S:` Static abilities (uses Static API)
- `R:` Replacement effects
- `SVar:` String variables for scripting
- `AI:` AI hints (`RemoveDeck:All`, `RemoveDeck:Random`, `RemoveDeck:NonCommander`)
- `DeckHints:` AI deck building hints (improves synergy)
- `DeckNeeds:` Required card types for AI deck inclusion
- `DeckHas:` Abilities this card provides (Token, Counters, Graveyard)
- `Oracle:` Current Oracle text (auto-generated by Python script)

### File Conventions
- Filename: lowercase, no special characters, underscore for spaces
- Unix (LF) line endings
- Empty lines only between multiple faces (ALTERNATE keyword)
- AI SVars immediately before Oracle text
- Avoid writing default parameters to keep scripts concise

### Documentation
See `docs/Card-scripting-API/` for detailed scripting documentation:
- `Card-scripting-API.md`: Main API reference
- `AbilityFactory.md`: Ability effects
- `Triggers.md`: Triggered abilities
- `Statics.md`: Static abilities
- `Replacements.md`: Replacement effects
- `Costs.md`: Cost definitions
- `Targeting.md`: Targeting restrictions
- `Restrictions.md`: Play restrictions

## Code Quality

### Checkstyle
Checkstyle validation runs during the `validate` phase and will fail the build on violations. Configuration is in `checkstyle.xml`.

### Android API Compatibility
Be careful about unsupported API calls (e.g., `StringBuilder.isEmpty()`). Google's documentation can be inaccurate. Current requirements:
- Android SDK Build-tools 35.0.0
- Android 15 (API 35) SDK Platform
- Proguard 7.6.0 (supports up to Java 23)

## Development Notes

### Java Requirements
- Java 17 or later required
- Maven 3.8.1+ required

### IDE Setup
IntelliJ is the recommended IDE. See `docs/Development/IntelliJ-setup/IntelliJ-setup.md` for setup guide.

### Libgdx
Mobile platforms (Android, iOS) use the Libgdx framework. Familiarity with Libgdx is helpful for mobile development.

### Art Files
Art files must be copyright-free and in the public domain.

### Git Workflow
- Main branch: `master`
- Fork the repository and create pull requests
- Commit message format should follow existing conventions (see recent commits)

## Key Implementation Details

### Ability System
The ability system is built around `AbilityFactory` which parses card scripts and creates executable abilities. Effects are in `forge-game/src/main/java/forge/game/ability/effects/`.

### AI System
AI decisions are made through:
1. `AiController` orchestrates AI actions
2. `ComputerUtil*` classes evaluate game states
3. `SpellAbilityAi` determines which spells/abilities to use
4. `simulation/` package simulates future game states

### Card Interactions
Card interactions are managed through:
- **Triggers**: Event-based actions (ETB, dies, etc.)
- **Static abilities**: Continuous effects
- **Replacement effects**: Modify events before they happen
- **State-based actions**: Automatic game rule enforcement

### Multi-face Cards
For double-faced cards, use `AlternateMode:{CardStateName}` in the front face and separate faces with "ALTERNATE" on a new line.

## Additional Resources
@C:\Users\BEBENEDE\Documents\Mtg Rogue Contents_v2.0.0.pdf
@C:\Users\BEBENEDE\Documents\Mtg Rogue Rules_v2.0.0.pdf

