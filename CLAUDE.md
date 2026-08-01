# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Forge — an open-source Magic: The Gathering rules engine in Java 17, built with Maven as a multi-module reactor. Cards are **not** Java classes: they are ~33,500 plain-text scripts under `forge-gui/res/cardsfolder/`, interpreted at runtime by the engine.

## Build & test

```bash
mvn -U -B clean -P windows-linux install          # full build (what CONTRIBUTING documents)
mvn -pl forge-gui-desktop -am install -DskipTests # fast build of one module + deps
mvn -U -B clean test                              # what CI runs (Java 17 and 21)
```

Running a single test (TestNG; nearly all game-logic tests live in `forge-gui-desktop`):

```bash
mvn -pl forge-gui-desktop -am test \
    -Dtest=DamageDealAiTest -Dsurefire.failIfNoSpecifiedTests=false
mvn -pl forge-gui-desktop -am test \
    -Dtest='BecomeMonarchAiTest#takesTheCrownFromAnOpponent' -Dsurefire.failIfNoSpecifiedTests=false
```

`-Dsurefire.failIfNoSpecifiedTests=false` is needed because `-am` pulls in modules that contain no matching test.

Tests touch AWT, so on a headless Linux box run them under a virtual framebuffer (CI does exactly this):

```bash
export DISPLAY=:1 && Xvfb :1 -screen 0 800x600x8 &
```

Checkstyle runs in the `validate` phase and **fails the build** — but only enforces two rules: no redundant imports, no unused imports.

Network stress tests are gated behind `-Drun.stress.tests=true` and skip otherwise; see `docs/Development/Network-Testing.md` for the full matrix of entry points and `-Dtest.*` properties.

## Running the app

- Desktop (Swing): main class `forge.view.Main` in `forge-gui-desktop`, working directory = the module dir. VM options matter (large `--add-opens` list) — copy them from `docs/Development/IntelliJ-setup/IntelliJ-setup.md`.
- Adventure mode / mobile GUI on desktop: main class `forge.app.Main` in `forge-gui-mobile-dev`.
- Dev Mode (in-game cheats: view zones, set game state, generate mana) is toggled in Game Settings → Preferences; documented in `docs/Development/DevMode.md`.

## Module layout and dependency direction

```
forge-core   → card DB, CardRules, editions, decks, no game state
forge-game   → the rules engine: Game, GameAction, Card, Player, stack, triggers, statics
forge-ai     → computer opponent; depends on forge-game
forge-gui    → GUI-agnostic app layer: game modes (quest, limited, draft, adventure data), lobby, netplay
  ├── forge-gui-desktop     Swing GUI + the bulk of the test suite
  ├── forge-gui-mobile      libGDX GUI logic (source root is `src`, not `src/main/java`); Adventure mode lives here
  │     ├── forge-gui-mobile-dev  LWJGL desktop backend for the mobile/Adventure GUI
  │     ├── forge-gui-android     Android backend (SDK 35, proguard in tools/)
  │     └── forge-gui-ios         RoboVM backend — deliberately NOT in the default reactor (needs locally-built deps; use `mvn -Pios`)
adventure-editor → standalone editor for Adventure content
forge-lda        → deck-generation data
```

`forge-gui/res/` is the single source of truth for all game data (card scripts, editions, formats, decks, quest data, Adventure content, localization). Platform builds copy/zip it into their distributions.

## How a card becomes behavior

1. `CardStorageReader` (forge-core) walks `res/cardsfolder/` and parses each `.txt` into `CardRules`/`CardFace`. Files in `cardsfolder/upcoming/` are only loaded on development builds — that's where unreleased-set cards go.
2. `CardFactory` (forge-game) turns `CardRules` into a live `Card`.
3. Script lines `A:`/`SVar:`/`T:`/`R:`/`ST:` are parsed by `AbilityFactory` into `SpellAbility`s. The `DB$`/`AB$`/`SP$` token maps to an `ApiType` enum constant, and each enum constant is bound to a `SpellAbilityEffect` subclass in `forge-game/.../ability/effects/` (~206 of them).
4. `SpellApiToAi` (forge-ai) maps the same `ApiType` to a `SpellAbilityAi` subclass in `forge-ai/.../ability/`, which decides whether/how the AI plays it.

So **adding a new effect API means touching three places**: a new `ApiType` constant, a new `…Effect` class, and an entry in `SpellApiToAi` pointing at a `…Ai` class. Most card work needs none of that — reuse existing APIs from a similar existing script.

Card scripting reference lives in `docs/Card-scripting-API/` (`AbilityFactory.md` for effect parameters, plus `Triggers.md`, `Replacements.md`, `Statics.md`, `Costs.md`, `Restrictions.md`, `Targeting.md`).

Script file naming: lowercase, accents folded, punctuation → underscores (`Dáin, Lord of the Iron Hills` → `dain_lord_of_the_iron_hills.txt`), filed under the first-letter directory. A card also needs an entry in the relevant `res/editions/<Set Name>.txt` (`[cards]` section: `number rarity Name @Artist`).

## Engine ↔ GUI boundary

The engine never talks to a GUI directly. Two seams matter:

- **`PlayerController`** (forge-game) — every decision the engine needs from a player goes through it. `PlayerControllerAi` (forge-ai) and `PlayerControllerHuman` (forge-gui) implement it.
- **View objects** — `GameView`/`PlayerView`/`CardView` are the read-only projections handed to UIs; GUIs must not reach into `Game`/`Card` state. `IGuiGame` / `IGuiBase` (`forge-gui/.../gui/interfaces/`) are the interfaces each platform implements.

`HostedMatch` and `GameLobby` (`forge-gui/.../gamemodes/match/`) orchestrate a match; netplay lives in `forge-gui/.../gamemodes/net/` with a delta-sync protocol (toggle with `-Dforge.deltasync=false`).

## Conventions worth knowing

- UI strings go through `Localizer` and `res/languages/en-US.properties`; other locales are community-maintained — add the `en-US` key and leave the rest.
- Android compatibility: avoid newer JDK API even when the docs claim it's available (e.g. `StringBuilder.isEmpty()`), since it breaks the Android build.
- The vast majority of commits are data-only (`res/cardsfolder/`, `res/editions/`, `res/formats/`) and touch no Java at all.lll
- Per CONTRIBUTING: do **not** add new unit or wiring tests to the CI suite unless they catch a real integration regression, and disclose AI-agent authorship in the PR (co-author trailer or a note in the body).

## Launch with

cd /home/sg/repos/forge/forge-gui && java -Xms768m -Xmx4096m -XX:+UseParallelGC \
 -Dsun.java2d.xrender=false -Dio.netty.tryReflectionSetAccessible=true -Dfile.encoding=UTF-8 \
 --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED \
 --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.text=ALL-UNNAMED \
 --add-opens java.base/jdk.internal.misc=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED \
 --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/java.math=ALL-UNNAMED \
 --add-opens java.base/java.util.concurrent=ALL-UNNAMED --add-opens java.base/java.net=ALL-UNNAMED \
 --add-opens java.desktop/java.awt=ALL-UNNAMED --add-opens java.desktop/java.awt.font=ALL-UNNAMED \
 --add-opens java.desktop/java.awt.image=ALL-UNNAMED --add-opens java.desktop/java.awt.color=ALL-UNNAMED \
 --add-opens java.desktop/javax.swing=ALL-UNNAMED --add-opens java.desktop/javax.swing.border=ALL-UNNAMED \
 --add-opens java.desktop/javax.swing.event=ALL-UNNAMED --add-opens java.desktop/java.beans=ALL-UNNAMED \
 --add-opens java.desktop/sun.swing=ALL-UNNAMED --add-opens java.desktop/sun.awt.image=ALL-UNNAMED \
 -cp ../forge-gui-desktop/target/forge-gui-desktop-2.0.14-SNAPSHOT-jar-with-dependencies.jar \
 forge.view.Main

# Kill the game with

pkill -f '[f]orge.view.Main'
