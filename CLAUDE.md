# Forge - Magic: The Gathering Rules Engine

## Project Overview
Forge is an open-source, cross-platform Magic: The Gathering rules engine written in Java. Started in 2007, it provides a complete MTG game experience with AI opponents, multiple game modes (Draft, Sealed, Commander, Cube, Adventure/Shandalar), and network play support.

**Development:** Forge is maintained by a small group of volunteers contributing on an intermittent, part-time basis. Code reviews and merges may take time; contributions should be well-documented and self-contained where possible.

**License:** GPL-3.0 | **Repository:** https://github.com/Card-Forge/forge

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
├── forge-gui/           # Shared GUI and resources
│   └── res/cardsfolder/ # Card scripts (2000+ files)
├── forge-gui-desktop/   # Desktop GUI (Swing)
├── forge-gui-mobile/    # Mobile GUI (Libgdx)
└── docs/                # Documentation
```

## Build Commands

**Maven Path (Windows):** `"C:\Program Files\Apache\maven\bin\mvn.cmd"`

```bash
# Default: Incremental build (use for iterative development)
mvn -pl forge-gui -am install -DskipTests

# Desktop build (for network play testing)
mvn -pl forge-gui-desktop -am install -DskipTests

# Quick compile check (fastest, syntax checking only)
mvn -pl forge-gui -am compile

# Full build with installer (slow, only when needed)
mvn -U -B clean -P windows-linux install -DskipTests

# Run single test (IMPORTANT: use verify, not test)
mvn -pl forge-gui-desktop -am verify -Dtest="TestClassName#methodName" -Dsurefire.failIfNoSpecifiedTests=false
```

**Windows Bash Note:** Use `-f` flag for pom.xml path:
```bash
"C:\Program Files\Apache\maven\bin\mvn.cmd" -f "/path/to/forge/pom.xml" -pl forge-gui-desktop -am install -DskipTests
```

## Quick Reference

| What | Where |
|------|-------|
| Card scripts | `forge-gui/res/cardsfolder/` |
| Effect implementations | `forge-game/.../ability/effects/` |
| AI decisions | `forge-ai/.../ability/` |
| Game state | `forge-game/.../game/card/Card.java` |
| Trigger system | `forge-game/.../trigger/TriggerHandler.java` |
| Network tests | `forge-gui-desktop/src/test/java/forge/net/` |

## Important Patterns
- **No DI framework** - Manual constructor injection throughout
- **View separation** - Mutable game objects have immutable `*View` counterparts
- **Effect pattern** - All abilities extend `SpellAbilityEffect`
- **Predicates** - Filter logic in `*Predicates.java` factory classes
- **Handlers** - Centralized event processing (`TriggerHandler`, `ReplacementHandler`)

## IMPORTANT: Development Guidelines

- **YOU MUST** use incremental builds (`-pl forge-gui -am`) by default
- **YOU MUST** minimize core code changes on feature branches - use subclasses and hooks
- **YOU MUST** read files before editing - never propose changes to unread code
- Avoid over-engineering: only make changes directly requested or clearly necessary
- Desktop GUI requires display; CI uses Xvfb for headless testing

## IMPORTANT: TASKS LIST

-**YOU MUST** prompt user whether Claude should continue implementing tasks in `tasks.md` when a new session is started.
-**YOU MUST** update `tasks.md` when a task is complete to ensure continuity between sessions.

## CRITICAL: Test Results Documentation Integrity

**STRICT REQUIREMENT:** When updating `.documentation/Testing.md` with test results:

1. **YOU MUST** archive test artifacts to `testlogs/` directory BEFORE updating documentation:
   - Copy `comprehensive-test-results-*.md` to `testlogs/`
   - Copy all `network-debug-runBATCHID-*.log` files from that run to `testlogs/`
2. **YOU MUST** read the actual test results file BEFORE writing any metrics
3. **YOU MUST** copy numbers EXACTLY from the results file - never estimate, interpolate, or generate numbers
4. **YOU MUST** cite the specific results file used (filename with timestamp)
5. **NEVER** hallucinate, fabricate, or approximate test metrics - if a number is not in the results file, do not include it
6. If the results file is missing data, state "Not recorded" rather than inventing values

**Verification checklist before updating test documentation:**
- [ ] Test artifacts archived to `testlogs/` for GitHub verification
- [ ] Read the actual `comprehensive-test-results-YYYYMMDD-HHMMSS.md` file
- [ ] Every number in documentation matches the source file exactly
- [ ] Results file timestamp is cited in documentation
- [ ] Any discrepancies between expected and actual game counts are explained

## Context Files (consult as needed)

| File                                     | Purpose |
|------------------------------------------|---------|
| `.claude/docs/architectural_patterns.md` | Design patterns, conventions, code organization |
| `.claude/docs/tasks.md`                  | Current tasks and priorities |
| `.claude/docs/scriptReferences.md`       | **CONSULT FIRST** when looking for NetworkPlay scripts - lists all classes with descriptions |
| `.documentation/NetworkPlay.md`          | Implementation status, architectural decisions, branch workflows |
| `.documentation/Testing.md`              | Test infrastructure and validation results |
| `.documentation/Debugging.md`            | Known bugs, debugging progress, resolutions (active bugs have full descriptions; resolved bugs go in summary table) |
| `.documentation/FeatureDependencies.md`  | Feature categories, dependencies, PR sequencing guide |
| `.documentation/RefactorOptions.md`      | Future refactoring considerations |

## Custom Skills

| Skill | Purpose |
|-------|---------|
| `/improve` | Analyze and improve CLAUDE.md and architectural_patterns.md |
| `/reviewlogs` | Review network debug logs for errors and propose fixes |
| `/documentation` | Update .documentation/NetworkPlay.md, .documentation/Testing.md, .documentation/Debugging.md (auto-triggers before commits) |
| `/batchtest` | Run comprehensive 100-game network test with monitoring, analysis, and documentation updates |

## Adding New Cards
1. Create card script in `forge-gui/res/cardsfolder/[first_letter]/`
2. Implement any new effects in `forge-game/.../ability/effects/`
3. Add AI logic in `forge-ai/.../ability/`

See: `docs/Card-scripting-API/`, `docs/Creating-a-custom-Card.md`
