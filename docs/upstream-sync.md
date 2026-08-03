# Upstream Sync Process

## How sync works

A daily GitHub Action (`.github/workflows/sync-upstream.yml`) pulls from `Card-Forge/forge` at 06:00 UTC and opens a PR when the merge is green. The required checks (Java 17/21 tests, CodeQL, doc-status, platform-parity) must pass before merge.

**Never** add a remote literally named `upstream` to any workflow — it makes `gh pr create` misattribute the repo.

## Additive-only rule

Reforge classes extend upstream classes rather than modifying them. Every Reforge class carries a `REFORGE COMMANDER EXTENSION` header. Code markers use `// doc:<item> <STATUS>` format.

## Touched upstream files (sync-conflict risk)

These upstream files are modified directly by Reforge (not extended). A future upstream refactor of any of these will produce a merge conflict. Re-verify this list periodically against `git diff upstream/master..master --name-only`.

| Upstream file | Why touched | Reforge items |
|---------------|-------------|---------------|
| `forge-game/src/main/java/forge/game/card/TokenEffectBase.java` | Token burst suppression wrapper + `tryStackToken` wiring | 1a, 1b |
| `forge-game/src/main/java/forge/game/zone/PlayerZone.java` | `setSuppressViewUpdate` to batch view refreshes | 1b |
| `forge-game/src/main/java/forge/game/zone/PlayerZoneBattlefield.java` | `tryStackToken`, `expandStacks`, `getCardsUnexpanded` | 1b, 1g, 1c |
| `forge-game/src/main/java/forge/game/zone/Zone.java` | Stack-aware card count/iterator | 1b |
| `forge-game/src/main/java/forge/game/Game.java` | `forEachCardInGameUnexpanded` for unexpanded battlefield iteration | 1c |
| `forge-game/src/main/java/forge/game/GameAction.java` | `checkStaticAbilities` uses unexpanded battlefield | 1c |
| `forge-gui/src/main/java/forge/gui/FSkin.java` | `applyCommanderDarkTheme` | 6a |
| `forge-gui-desktop/src/main/java/forge/screens/home/VHomeUI.java` | `reforge.commander.mode` menu gating | commander-mode |
| `forge-gui-desktop/src/main/java/forge/screens/home/EMenuGroup.java` | Reorder PLAY before GAUNTLET | 2c (sync-conflict risk) |
| `forge-gui-desktop/src/main/java/forge/gui/GuiDesktop.java` | Commander-mode init | commander-mode |
| `forge-gui-desktop/src/main/java/forge/util/ForgePreferences.java` | `SUBMENU_PLAY` dead key | 2d |
| `forge-gui-desktop/pom.xml` | FlatLaf dependency | 6a |
| Root `pom.xml` | Java 17 enforcement | build |

**New Reforge-only files** (no sync risk):
- `StackedTokenCard.java`, `ReforgeCommanderApp.java`, `VSubmenuPlayCommander.java`, `CSubmenuPlayCommander.java`, `ReforgeTheme.java`
