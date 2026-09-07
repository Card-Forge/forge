# REV — Commit, Branch, Review

- **Status:** Active
- **Applies to:** all work in this fork
- **Rule IDs:** cite as `REV-n`

---

## REV-1 — Fork hygiene comes first

This repo is a fork of `Card-Forge/forge` and keeps pulling upstream. Merge pain is the default failure mode.

| Rule                      | Detail                                                                             |
| ------------------------- | ---------------------------------------------------------------------------------- |
| Go code                   | `crucible/` only                                                                   |
| Crucible docs             | `docs/crucible/` only. Never edit upstream `docs/*.md`                             |
| Java edits                | **None**, except additive test-scoped oracle tooling in `crucible/oracle-java/`    |
| Unavoidable upstream edit | Log it in `docs/crucible/porting/upstream-patches.md` with the reason, same commit |

Reason: every line touched outside `crucible/` and `docs/crucible/` is a future rebase conflict. ADR-0001.

---

## REV-2 — Branches

`master` tracks upstream. Never commit directly.

```text
port/<unit>        port/game-action, port/card-rules-reader
feat/<thing>       feat/telemetry-recorder
docs/<topic>       docs/adr-0009-state-model
fix/<issue>        fix/layer-timestamp-order
```

---

## REV-3 — Commit messages

Conventional Commits. Subject ≤ 50 chars, imperative, no trailing period.

```text
port(engine): translate checkStateEffects to Go

Splits Java's interleaved SBA + layer pass into applyLayers() and
checkSBA() so layer ordering is testable in isolation (CR 613).

Behavior pinned by 14 fixtures under testdata/scenarios/cr613-layers/.
Differential harness green against Java for all of them.

Deviations recorded in docs/crucible/porting/port-log/game-action.md.
```

Body required when the "why" is not obvious from the subject. Body explains **why**, not what — the diff already says
what.

Types: `port`, `feat`, `fix`, `docs`, `test`, `refactor`, `perf`, `chore`.

---

## REV-4 — What blocks a merge

| Check                                            | Blocking                                     |
| ------------------------------------------------ | -------------------------------------------- |
| `gofmt -s`, `golangci-lint`                      | Yes                                          |
| `go test -race ./...` (L1–L3)                    | Yes                                          |
| `docgate` — module-map row, ADR existence        | Yes                                          |
| Coverage floor for the touched package (TEST-12) | Yes                                          |
| L4 differential parity                           | No — opens a bug                             |
| L5 fuzz / soak                                   | No — opens a bug                             |
| L6 benchmark regression                          | Yes, if over threshold on a hot path (GO-16) |

Checks whose tooling does not exist yet — `docgate`, the coverage floor, L4-L6 — are enforced by the reviewer until it
does. A gate listed here is binding either way; only the mechanism differs.

---

## REV-5 — Review checklist

Reviewer checks, in this order:

1. **Docs landed?** port-log note, module-map row, ADR if ADRP-1 applies. Missing → request changes, stop reading.
2. **Tests at the right level?** Module-level default; internal test carries a why-comment (TEST-1, TEST-2).
3. **Rules behavior in fixtures, not Go funcs?** (TEST-5)
4. **Engine bans respected?** No package-level mutable state, mutex, reflection, `any` (GO-2, GO-3, GO-4, GO-8).
5. **Java shapes leaked?** Accessor walls, deep hierarchies, string re-interpretation (PORT-1, PORT-2).
6. Then the actual logic.

Order matters. Reviewing logic first means the structural problems get found after the author has stopped caring.

---

## REV-6 — Review comment style

One line: location, problem, fix. Cite the rule ID.

```text
sba.go:88 — package-level `var layerCache` breaks goroutine-per-game (GO-2).
Move onto Game.
```

Not: "I was wondering whether it might perhaps be better if we considered moving this."

Follows [00-documentation-style](00-documentation-style.md).

---

## REV-7 — Upstream sync

Merge upstream `master` on a cadence, never rebase Crucible work onto a moving upstream.

After each sync:

1. Re-run L2 corpus golden. Upstream card-script changes are absorbed for free — the diff tells you if the parser missed
   a new key.
2. Re-run `crucible corpus-coverage`. New scripts may use new APIs.
3. Regenerate `parity-matrix.md`.

Upstream Java changes to `forge-game` do **not** require Go changes unless L4 differential goes red.

---

## Related

- [00-documentation-style](00-documentation-style.md)
- [01-go-coding-standards](01-go-coding-standards.md)
- [03-testing-standards](03-testing-standards.md)
