# Contributing to Forge

Forge is an open-source project run by volunteers, and contributions are welcome! This page explains how to get set up and how to avoid the most common problems in pull requests.

[Official repo](https://github.com/Card-Forge/forge.git). Questions and discussion happen on [Discord](https://discord.gg/HcPJNyD66a).

## Contents

- [Setup](#setup)
- [Card scripting](#card-scripting)
- [Using AI coding agents](#using-ai-coding-agents)
- [Tests](#tests)
- [Opening a pull request](#opening-a-pull-request)
- [Art and UI](#art-and-ui)
- [Project hierarchy](#project-hierarchy)

## Setup

You need:

- Java JDK 17 or later
- [Maven](https://maven.apache.org/install.html)
- Git and a GitHub account
- A Java IDE. IntelliJ is recommended. Eclipse also works and includes Maven.

To get started, fork the project on GitHub and clone your fork. Then run Maven from the project folder to download the dependencies and build a snapshot:

```
mvn -U -B clean -P windows-linux install
```

Add `-DskipTests` for a faster build. The [IntelliJ setup guide](https://github.com/Card-Forge/forge/wiki/IntelliJ-setup) explains how to run the desktop and mobile clients. To test a change in a game, you can turn on Developer Mode in Preferences.

### Android

Install these through the Android SDK Manager (in IntelliJ: Tools > Android > Android SDK Manager):

- Android SDK Build-tools 35.0.0
- Android 15 (API 35) SDK Platform

Shared code also runs on Android, so avoid Java APIs that Android doesn't support, such as `StringBuilder.isEmpty()`. Google's documentation for these is sometimes inaccurate.

Proguard 7.6.0 is included under `forge-gui-android/tools` (`proguard.jar`). It supports up to Java 23.

## Card scripting

Card scripts are in `forge-gui/res/cardsfolder`. The [Card scripting API](https://github.com/Card-Forge/forge/wiki/Card-scripting-API) page on the wiki explains the format.

The easiest way to write a new script is to use a similar card as a template. Prefer one from a recent set, since older scripts can use outdated forms. Test the card in a game before you open a PR.

## Using AI coding agents

Use of AI coding agents such as Claude Code, Codex or Copilot is acceptable. However, ultimately you are the author responsible for any change, so make sure you understand it and can explain it. 

If an agent wrote a substantial part of your contribution, say so in the PR body or add it as a co-author.

Agent-written PRs tend to go wrong in the same few ways:

- Work with Forge's existing architecture. Before adding something new, check whether existing code already does the job and either re-use or adapt it, so the change fits how the rest of Forge works rather than adding a parallel system.
- Keep the code simple. Aim for the clearest, smallest change that solves the problem.
- Fix the root cause, not the symptom. Trace a bug back to where it starts and fix it there, rather than hiding the bug and leaving other cases broken.
- Keep PRs as small as possible. Break a large feature into a minimal first version that works on its own, followed by smaller PRs, because one large PR takes much longer to review than two small ones.
- Keep code comments short. Comment only what isn't obvious from the code, such as a hidden constraint or the reason for an unusual choice. Don't use a code comment to describe the bug you fixed or the change you made. That belongs in the PR description.
- Make sure you run your change before you open a PR. Reproduce a bug before you fix it, check in a game that the fix works, and state in the PR description what you have verified.

If your change touches the rules engine, the AI or another complex system, it can help to have your agent run an adversarial review before you open the PR. Ask it to look for ways the change could be wrong, not just to confirm that it works.

Try and answer review comments in your own words, or make clear when you are copying and pasting text from an agent.

Agents also tend to add more tests than a change needs. See [Tests](#tests).

## Tests

Tests run on every push and build, so every future contributor pays for them. Add the fewest tests that protect behaviour a later change could break.

- Don't add tests for card script changes that don't touch engine or AI code, or wiring tests that only confirm your code runs, such as checking that a method was called.
- Tests may be added for engine or AI behaviour that many cards share, and to protect against regressions between systems. A useful test checks a generalisable rule or behaviour, not just one very specific card or situation.

A test should fail without your change and protect a behaviour a player would notice. Add to existing test classes and reuse their setup where you can, rather than writing new test infrastructure. Keep tests fast and deterministic, with fixed seeds and no sleeps or full games. One test per behaviour is usually enough.

## Opening a pull request

- Check GitHub for open PRs and issues on the same topic first. 
- For a large change, it can be helpful to seek feedback on Discord before opening a PR.
- Keep each PR to one goal or feature. Avoid lumping different features that could be separated into the same PR.
- For a change that may affect performance, you should profile the impact on different board states (e.g. 1v1, 4v4 Commander, wide token decks) and give before and after performance numbers in the PR description.
- If your PR involves a visual or UI change, include a screenshot or video in the PR description.
- Build and run the tests before you open a PR. CI runs `mvn clean test`, and Checkstyle fails the build on unused or redundant imports.

## Art and UI

- Art must be copyright-free and in the public domain. Add credits to [Credit and Thanks](https://github.com/Card-Forge/forge/wiki/Credit-and-Thanks).
- New UI elements should follow the [UI guidelines](https://github.com/Card-Forge/forge/wiki/UI-Guidelines).

## Project hierarchy

Forge is divided into 4 primary projects with additional projects that target specific platform releases. The primary projects are:

- forge-ai
- forge-core
- forge-game
- forge-gui

The platform-specific projects are:

- forge-gui-android
- forge-gui-desktop
- forge-gui-ios
- forge-gui-mobile
- forge-gui-mobile-dev

#### forge-ai

The forge-ai project contains the computer opponent logic for gameplay. It includes decision-making algorithms for specific abilities, cards and turn phases.

#### forge-core

The forge-core project contains the card database, card and deck data, and shared utilities used by the other projects.

#### forge-game

The forge-game project contains the rules engine. It implements the Magic: The Gathering rules, card interactions, and game state, including zones, abilities, triggers and combat.

#### forge-gui

The forge-gui project contains the GUI logic shared by the desktop and mobile clients, including game modes, player interactions and network play. It also holds the resources in the res/ path, such as card scripts, editions and translations.

#### forge-gui-android

Libgdx-based backend targeting Android. Requires Android SDK and relies on forge-gui-mobile for GUI logic.

#### forge-gui-desktop

Java Swing based GUI targeting desktop machines.

Screen layout and game logic revolving around the GUI is found here. For example, the overlay arrows (when enabled) that indicate attackers and blockers, or the targets of the stack are defined and drawn by this.

#### forge-gui-ios

Libgdx-based backend targeting iOS. Relies on forge-gui-mobile for GUI logic.

#### forge-gui-mobile

Mobile GUI game logic utilizing [libgdx](https://libgdx.badlogicgames.com/) library. Screen layout and game logic revolving around the GUI for the mobile platforms is found here.

#### forge-gui-mobile-dev

Libgdx backend for desktop development for mobile backends. Utilizes LWJGL. Relies on forge-gui-mobile for GUI logic.
