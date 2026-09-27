# Porter plan: CR 613 Layer 1, "enters as a copy"

Scratch file; deleted in the final commit.

## Real gap (corpus, not the doc's framing)

- `Clone` "becomes a copy" and the `copies` stack on `Card` already exist (cloneeffect.go).
- Missing: "enters as a copy" = CR 614.1c/707.2 replacement on the Moved-to-battlefield event, Java
  `ReplacementLayer.Copy`.
  - `K:ETBReplacement:Copy:<SVar>[:Optional|Mandatory[:Zone[:Valid]]]` -- 68 cards (65 `DB$ Clone`, Mimeoplasm
    ChooseCard chain, Living Lore ChangeZone). Self shape `Choices$ ...` dominant; 3 watcher shapes (Essence of the
    Wild, Thunderbond Vanguard, Infinite Reflection) with `CloneTarget$ ReplacedCard`.
  - `R:Event$ Moved | Layer$ Copy | ReplaceWith$ <Clone>`: Protean Raider, Displaced Dinosaurs (Mystic Reflection is an
    effect-card SVar).
  - The engine expands no `ETBReplacement` keyword at all; `compile` never compiles the SVar it names.

## Steps

1. compile: expand every `K:ETBReplacement` keyword into a `Moved` replacement (CardFactoryUtil.createETBReplacement),
   `Layer$` param carries the mode, `ReplaceWith` sub = compiled SVar. Regenerate golden AST, review. Commit.
2. engine: `checkMovedReplacement` takes a controller; runs Copy layer first (self + battlefield watchers), loops until
   no unapplied candidate (Body Double copying a Clone), then the existing enters-tapped pass over the copied Def.
   ReplaceWith Clone chain resolved through the Registry with `replacing` set; Clone filters `Choices$` by last
   battlefield state (exclude the entering card). `Defined$ ReplacedCard`. Optional$ -> ConfirmEffect. >1 candidate (CR
   616.1 choice) and non-Clone Copy-layer ReplaceWith -> recordPendingError. Tests + scenario. Commit.
3. Clone `AddTriggers$`/`AddAbilities$`/`AddStaticAbilities$` compiled at load (gated on Clone like Effect's), appended
   to the copied face. Tests. Commit.
4. Docs: port-log note `game-state/layer1-enters-as-copy.md`, index row, Not-ported row, effects-clone.md shape table,
   CLAUDE.md/master plan line. gates full. Delete this file. Commit.
