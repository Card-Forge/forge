---
name: port-java-unit
description:
  Port a Forge Java unit that is not an ApiType effect (a rules primitive, cost, trigger mode, replacement, static
  ability, parser or new package) to Crucible Go, following PORT-3 order - read Java, write the port-log note, API,
  tests, implementation, docs. Use for "port GameAction.X", "port the Y trigger mode", "add package Z".
---

# Port a Java unit

For ApiType effects use the `port-effect` skill instead. Rules: `docs/crucible/guidelines/02-java-to-go-translation.md`
(PORT-n), `01-go-coding-standards.md` (GO-n), `03-testing-standards.md` (TEST-n).

## PORT-3 order - do not skip or reorder

1. **Read the Java end to end.** Delegate the reading to `forge-oracle` ("forge-oracle: GameAction.checkStateEffects").
   Read inline only what it could not answer.
2. **Port-log note first.** Decide where it goes:

   | Unit                           | Note                                                                                                                          |
   | ------------------------------ | ----------------------------------------------------------------------------------------------------------------------------- |
   | Part of `internal/engine`      | New `##` section in the matching `docs/crucible/porting/port-log/game-state/<topic>.md`, plus an index row in `game-state.md` |
   | New package or standalone unit | New `docs/crucible/porting/port-log/<unit>.md` in PORT-4 format                                                               |

   Topic files: `state-based-actions`, `layers`, `turn-stack-combat`, `mana-and-casting`, `triggers`, `trigger-modes`,
   `replacement`, `activation`, `activation-costs`, `targeting-and-chaining`, `effects-*`. Content: what it does, real
   inputs/outputs, Java scaffolding dropped, deviations, pinned quirks (PORT-7), `path:line` for every Java claim.

3. **Public API** in the target package. Identity by ID (GO-9), no package-level mutable state (GO-2), no mutex (GO-3),
   no reflection (GO-4), no `any` (GO-8), ordered collections where order is visible (GO-12).
4. **Tests before the body**, at module level (`package x_test`, TEST-1). Whole-engine rules behavior goes in a fixture
   directory (`add-scenario` skill), not a Go func (TEST-5). Parsers get table tests plus `testing.F` fuzz (TEST-2).
5. **Implement.** Card-script-reachable failure returns `error`; `panic` only on engine invariant breach (GO-7). A Forge
   bug stops the port: report file and line, never compensate (PORT-8).
6. **Wire gates:**
   - New file in `internal/engine` -> group + allow-list in `internal/engine/enginelint.json`.
   - New package -> row in `docs/crucible/architecture/module-map.md` linking its port-log note (`docgate`, PORT-4).
   - New non-stdlib import -> needs an ADR first (GO-14). Stop and ask.
7. **Docs in the same commit** (DOC-12), in the compressed style (`guidelines/00-documentation-style.md`); run
   `prettier --write` on touched `.md`.
8. **Review and commit.** `rules-review` skill on the diff, fix, commit (hook runs `gates.sh full`).

## Stop and ask when

- The unit needs an upstream edit outside `crucible/` and `docs/crucible/` (REV-1; the guard hook will ask too).
- The Java behavior contradicts the Comprehensive Rules and parity is unclear (PORT-7 decision).
- A design choice is not covered by an accepted ADR (ADRP-1).
