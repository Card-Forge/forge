# Shandalar Old Border Mod - Quick Reference

Forge Adventure mode using only Old Border cards (≤ Scourge 2003-05-26). 6,103 legal cards across 46 editions.

## Git Commits

**Do NOT add Claude Code attribution to commit messages:**
- ❌ No "🤖 Generated with [Claude Code](https://claude.com/claude-code)"
- ❌ No "Co-Authored-By: Claude <noreply@anthropic.com>"
- ✅ Write clean, descriptive commit messages without AI attribution

## ⚠️ CRITICAL: Start-in-Battle Cards

**NEVER add `startBattleWith` or `effect` fields to `world/enemies.json`!**

The `EnemyData` class does NOT support these fields. Adding them will cause:
- Complete game failure on load
- No enemies spawning on overworld
- All maps refusing to load
- Cryptic serialization errors: `Field not found: startBattleWith (forge.adventure.data.EnemyData)`

**Correct approach:**
1. ✅ Add `effect` property with `startBattleWithCard` in **TMX map files** (enemy object or map properties)
2. ✅ Add `[StartInBattle_Reference]` section in **deck files** (.dck) for documentation
3. ❌ NEVER add `startBattleWith` to **enemies.json**
4. ❌ NEVER use equipment-based `startBattleWithCard` in **items.json** - it doesn't work

**Example (TMX only):**
```xml
<property name="effect">{ "startBattleWithCard": ["Card|SET", "Card2|SET2"] }</property>
```

**Note:** Equipment items with `startBattleWithCard` effects (e.g., "Slime Mother Starting Setup") do not function. Always use TMX file effects instead.

See "Start-in-Battle Cards: TMX + DCK Sync" section for full details.

## Map Override Workflow

**CRITICAL: Check for existing local map FIRST:**
```bash
ls "forge-gui/res/adventure/Shandalar Old Border/maps/map/[path]/[mapname].tmx"
```
If exists locally, edit that file directly. Only copy from common if map doesn't exist.

**Steps:**
1. Copy: `common/maps/map/...` → `Shandalar Old Border/maps/map/...` (preserve structure)
2. Edit local copy (replace illegal cards, remove boss abilities, adjust rewards)
3. Update `world/points_of_interest.json`: `"map": "../Shandalar Old Border/maps/map/..."`

## POI Map Path Resolution

**Required format:** `../Shandalar Old Border/maps/map/...` (game prepends `common/` to all paths)
- ✅ `../Shandalar Old Border/maps/map/fort/example.tmx` → loads from local
- ❌ `maps/map/fort/example.tmx` → loads from common (wrong)

## TMX Map Structure

**Map properties** (top of file, affects all battles):
```xml
<property name="dungeonEffect">{ "startBattleWithCard": [ "Card|SET" ] }</property>
```

**Enemy-specific effects** (in object properties):
```xml
<object template="../../../../../../common/obj/enemy.tx">
  <properties>
    <property name="enemy" value="Boss Name"/>
    <property name="effect">{ "startBattleWithCard": [ "Card|SET" ] }</property>
  </properties>
</object>
```

**CRITICAL:** Effects ONLY work in TMX files, NOT in enemies.json (no `effect` field in `EnemyData` class).

## Start-in-Battle Cards: TMX + DCK Sync

**MUST update BOTH files when editing startBattleWithCard:**

1. **TMX (functional)** - Game reads from here
2. **DCK (reference)** - Add `[StartInBattle_Reference]` section with same cards

Example DCK section:
```
[StartInBattle_Reference]
1 Mox Pearl|LEA
1 White Knight|LEA
```
DCK is documentation only but must match TMX for consistency.

**Other start locations:**
- `startBattleWithCardInCommandZone` - REMOVED (causes bugs)
- `startBattleWithCardInGraveyard`, `startBattleWithCardInExile` - Available

## Tileset/Object Template Paths

Maps loaded via `common/../Shandalar Old Border/...` require extra `../` levels:

**3 levels deep** (`maps/map/fort/`): `../../../../../../common/maps/tileset/main.tsx`
**4 levels deep** (`maps/map/main_story/castles/`): `../../../../../../../common/maps/tileset/main.tsx`

Explanation: 6 `../` escape path: `maps/map` → `maps` → `Shandalar Old Border` → `..` → `common` → `adventure`

## Card Legality

**Cutoff:** Scourge (SCG) 2003-05-26 | **Total:** 6,103 cards
**Format:** `"Card Name|SET"` using oldest legal printing

**Verify before adding cards:** Check edition files in `forge-gui/res/editions/*.txt`

**Special sets** (legal but excluded from allowedEditions):
- UGL (Unglued), PGRU/PELP/PALP (promo lands) - can be used for boss/miniboss decks, start-in-battle cards, and specific rewards

**Easter eggs:** Chicken Egg, Goblin Polka Band (excluded from validation)

**Boss rewards** (`world/enemies.json`):
```json
{"name": "Boss", "boss": true, "rewards": [{"type": "card", "cardName": "Card|SET"}]}
```
All rewards MUST include set codes.

## Item Sprites

**System:** `items.json` → `items.atlas` → `items.png` (480x1008, 16x16 sprites)

**Process:**
1. Add to `sprites/items.atlas`: `ItemName\n  xy: x, y\n  size: 16, 16`
2. Reference in `items.json`: `"iconName": "ItemName"`

**Naming:** PascalCase without spaces/apostrophes (e.g., "Tawnos's Wand" → `TawnossWand`)
**Grid conversion:** x = col×16, y = row×16

## Build & Test

```bash
# Build
mvn install -pl forge-core,forge-gui-mobile-dev -am -DskipTests -q

# Fix .command file (required after each build)
# Edit forge-gui-mobile-dev/target/forge-adventure.command:
# - Change: cd $(dirname "${0}") → cd "$(dirname "${0}")/.."
# - Change: -jar forge-gui-mobile-dev-...jar → -jar target/forge-gui-mobile-dev-...jar

chmod +x forge-gui-mobile-dev/target/forge-adventure.command
./forge-gui-mobile-dev/target/forge-adventure.command
```

## Key Scripts

- `check_old_border_maps.py` - Scan for illegal cards
- `remove_boss_abilities.py` - Remove command zone abilities
- `fix_boss_rewards_systematic.py` - Fix boss rewards with oldest legal editions
- `fix_all_boss_maps.py` - Fix boss maps via teleport tracing

## File Locations

```
Shandalar Old Border/
├── maps/map/              # Local map overrides
├── world/
│   ├── points_of_interest.json
│   ├── enemies.json
│   └── items.json
├── decks/standard/        # Enemy decks
└── sprites/items.atlas
```

Common assets: `forge-gui/res/adventure/common/maps/`
Editions: `forge-gui/res/editions/*.txt`

## Quick Tips

- Always edit local copies, never modify common/
- Preserve JSON formatting (single-line strings in TMX)
- Use git status to track changes
- Test in-game after modifications
