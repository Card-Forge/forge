# UI Guidelines

Forge is a volunteer project and creativity is encouraged! This page is not a hard rule-book — the guidelines below are intended to give some direction to contributors submitting UI changes and help maintain a consistent UI approach across the project.

## Contents

- [UI Design Principles](#ui-design-principles)
- [Where Should a New Setting or Option Live?](#where-should-a-new-setting-or-option-live)
  - [Desktop](#desktop)
    - [Game Menu](#game-menu)
    - [Dock Panel](#dock-panel)
    - [Preferences Menu](#preferences-menu)
  - [Mobile](#mobile)
    - [Game Menu](#game-menu-1)
    - [Settings](#settings)

---

## UI Design Principles

- Aim to be intuitive and user-friendly.
- Involve a good mix of dynamic and static elements.
- Use reasonable, context-driven default options.
- User-facing elements should always use localisation keys, not hard-coded strings.
- UI elements should be compatible with theme skinning — prefer skin-aware components and colours over hard-coded styles.
- The default Forge skin should favour accessibility.

---

## Where Should a New Setting or Option Live?

### Desktop

#### Game Menu

- **Purpose:** detailed in-match configuration options.
- **Heuristic:** if it affects in-match gameplay or display and the user might want to change it mid-match, it should live in the Game menu for easy access.

The in-match menu bar is divided into the following top-level submenus:

- **Game** — how the user interacts with game state (e.g. yield options, auto yields and triggers, concede).
- **Layout** — visual UI and layout (e.g. XML layout files, theme selection, UI panel options).
- **Display** — how game state is displayed to the user (e.g. card overlays, targeting arcs, stack/group token options, separate combatants toggle).
- **Audio** — sound and music settings.

Conventions:

- If a new feature involves suboptions (not just a single checkbox), group them together in a submenu.
- Avoid nesting menu items more than 3 levels deep (e.g. `Forge > Game > Stack/Group options` is as deep as we want to go).

#### Dock Panel

- **Purpose:** quick shortcuts and toggles for the most commonly used gameplay functions.
- **Heuristic:** if the user would need to navigate the game menu multiple times per match to change a setting, it should have a button here.
- The dock is only ever a mirror of a setting or option — never the exclusive entry point.

#### Preferences Menu

- **Purpose:** comprehensive access to all available user preferences and options.
- The search filter makes this menu cheap to add to — don't be overly concerned about menu length.

### Mobile

#### Game Menu

Physical space is at a premium on mobile, so only the most essential settings a user needs to access in-match should appear in the game menu. Generally these will be settings that involve interacting with game state, not just display options.

#### Settings

- **Purpose:** comprehensive access to all available user preferences and options.
- The search filter makes this menu cheap to add to — don't be overly concerned about menu length.
