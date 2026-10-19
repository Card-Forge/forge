---
name: add-scenario
description:
  Add a whole-engine rules test as a fixture directory under crucible/testdata/scenarios (setup.state, actions.log,
  expect.state) and run it. Use when a rules behavior (CR section, SBA, layer, combat, trigger, casting) needs a test,
  when asked to "add a scenario/fixture", or for the P4 gate's per-layer/per-SBA coverage.
---

# Add a scenario

TEST-5: rules tests are data. Adding a test = adding a directory; never a new Go func. `TestScenarios`
(`crucible/internal/engine/scenario_test.go`) walks `crucible/testdata/scenarios/` automatically.

## 1. Name it

Directory name = what it proves, kebab-case, flat under `crucible/testdata/scenarios/`: `battle-zero-defense-destroyed`,
`aura-falls-off-when-host-dies-sent-to-graveyard`. Check it does not exist:
`ls crucible/testdata/scenarios | grep <word>`.

## 2. Write the files

| File | Content |
| --- | --- |
| `setup.state` | Forge `GameState` text format, same one the Java oracle reads. Do not invent keys |
| `actions.log` | Ordered scripted decisions, one per line; `#` comments explain the CR rule and why each step exists |
| `expect.state` | The post-state, same format as `setup.state` |

Grammar references: `setup.state` / `expect.state` keys in `docs/crucible/porting/port-log/game-state-fixture.md`
(`Parse`/`Write`), `actions.log` verbs in its `## Scenarios: actions.log and the harness` section. Copy the closest
existing scenario and edit it; `grep -l <keyword> crucible/testdata/scenarios/*/setup.state` finds one.

Example (`battle-zero-defense-destroyed`):

```text
# setup.state
humanlife=20
ailife=20
humanbattlefield=Invasion of Belenon|Id:1

# actions.log
# CR 704.5v: a Battle at zero or less defense goes to its owner's graveyard.
queue battleprotector ai
startturn human

# expect.state
turn=1
activeplayer=human
activephase=Untap
humanlife=20
humangraveyard=Invasion of Belenon
ailife=20
```

Cards are named by their real corpus name (`forge-gui/res/cardsfolder`); `|Id:n` gives the handle `actions.log` refers
to. Every controller decision the engine will ask for needs a `queue ...` line first, or the run fails.

## 3. Run it

```bash
cd crucible && go test -race -count=1 -run 'TestScenarios/<name>$' ./internal/engine/
```

Takes about 60 s even for one case (the card DB loads first). A failing expectation prints the state diff. Fix the
fixture only when the fixture is wrong; if the engine is wrong, that is the bug to fix (bug fix -> this scenario is its
regression test, TEST-2).

## 4. Commit

No doc change needed for a scenario alone. If it pins a Java quirk, add a row to `port-log/quirks.md` (fixture-format
quirks: `game-state-fixture.md`'s `## Pinned quirks (PORT-7)`). There is no Java-oracle scenario
runner yet (ADR-0010); do not claim oracle parity.
