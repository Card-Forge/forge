# ADR-0022 — "As Enters" Replacements Resolve Before the Permanent Lands

- **Status:** Accepted
- **Date:** 2026-09-26
- **Deciders:** `mc@archlab.pl`

## Context

CR 614.1c/614.12: an effect that says how a permanent enters ("as ~ enters, choose a creature type", "you may have ~
enter as a copy of …") is a replacement effect applied to the event of it entering. The permanent is on the battlefield
only once that effect has been applied; nothing sees an intermediate state.

Forge writes most of these as a keyword, `K:ETBReplacement:<Layer>:<SVar>[:Optional[:Zone[:Valid]]]` — 421 corpus lines,
`Other` 353 and `Copy` 68 (Cavern of Souls, Phantasmal Image, Phyrexian Metamorph, Clone). `CardFactoryUtil` expands the
keyword at card creation into an `Event$ Moved | Destination$ Battlefield | ReplacementResult$ Updated` replacement
whose overriding ability is the SVar's ability (`CardFactoryUtil.java:2595-2606`, `:515-542`), placed in its CR 616.1
replacement layer (`ReplacementLayer.java:8-13`: CantHappen, Control, Copy, Transform, Other). The ability resolves on
the entering card during the move, before it lands.

This port has four battlefield-entry paths, and each applies Moved replacements _after_ landing:

| Path                        | Order today                                                          |
| --------------------------- | -------------------------------------------------------------------- |
| `permanentEffect`           | `Move` → `checkMovedReplacement` → ETB triggers (`castspell.go:289`) |
| `attachEffect`              | Same (`castspell.go:315`)                                            |
| `PlayLand`                  | Same (`land.go:81`)                                                  |
| `moveByEffect` (and tokens) | Same (`zonemove.go:35`; `token.go` routes through it)                |

That works only because the one resolved outcome, `Tapped = true` (`replacement.go:93`), is an idempotent flag. The
engine expands no `ETBReplacement` keyword, and `compile` does not compile the SVar it names. This blocks Clone's
largest shape — 62 of the 124 unresolved `Clone` lines (`effects-clone.md` (git `624d87dba16`), "Shapes not resolved") —
and every as-enters choice.

## Decision Drivers

- CR 614.12 and parity: a copy or a chosen type must be in place before ETB triggers, statics and SBAs look.
- PORT-2: the keyword and its SVar are expanded and compiled once at load, never at the moment of entering.
- One entry path, so a fifth caller cannot forget the step.
- TEST-1: the existing tapped-on-entry fixtures must stay byte-identical.

## Considered Options

1. **Keep applying after `Move`, before ETB triggers.** Rejected: between `Move` and the replacement the card is on the
   battlefield with its printed characteristics; a copy effect applied there changes the object after statics and the
   legend rule can already have seen it.
2. **A per-path pre-landing call at each of the four sites.** Rejected: four copies of an ordering rule.
3. **One battlefield-entry function all four paths call, applying Moved replacements before the zone change.** Chosen.

## Decision

1. **`compile` expands `K:ETBReplacement` into the same compiled replacement shape as an `R:Event$ Moved` line**, with
   its SVar compiled as the replacement's ability (the `effectTraitKeys` precedent, `compile.go:570-582`) and its CR
   616.1 layer recorded. The engine never sees the keyword text.
2. **Every battlefield entry goes through one engine function** that, before the card joins the battlefield zone,
   applies the Moved replacements that match it in CR 616.1 layer order — copy before other — resolving each
   `ReplaceWith$`/overriding ability on the entering card through the game's registry (ADR-0020), then lands it, then
   fires ETB triggers. `permanentEffect`, `attachEffect`, `PlayLand` and `moveByEffect` become callers of it.
3. **`Optional$` asks the entering card's controller** through the existing `ConfirmOptionalTrigger`-shaped confirm, and
   a choice the ability makes (creature type, what to copy) goes through its existing `PlayerController` method.
4. **Within one CR 616.1 layer, more than one applicable replacement keeps today's deterministic order** (GO-12). CR
   616's "affected player chooses" hook stays the separate M5 gap it already is.
5. **The existing tapped-on-entry replacement moves into this path unchanged in effect.** `Move` resets `Tapped`
   (`game.go:439`), so a replacement is _decided_ before landing and its result applied to the entering object as it
   lands, before ETB triggers — the same observable order as today.

## Consequences

**Good:** unblocks 421 `ETBReplacement` lines, including Clone's 62-line "enters as a copy" shape and Cavern of
Souls-style type choices; Layer 1 copies land correct from the first moment. One place owns battlefield entry.

**Bad:** four call sites change at once, and a replacement that fails (`ErrUnimplemented`) now stops a permanent from
entering — the entry path gains an error return and its callers must pass it on (GO-7).

**Neutral:** `checkMovedReplacement` is absorbed into the new function; fixtures without an `ETBReplacement` card see
the same `Tapped` result and the same events.

## Related

ADR-0017 (registry), ADR-0020 (registry reachable from any entry point), `effects-clone.md` (git `624d87dba16`),
[04-adr-process](../guidelines/04-adr-process.md)
