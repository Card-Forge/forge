# Porter plan: Layer 3 GainTextOf$ + Layer 8 remainder

Scratch plan for whoever resumes this branch. Deleted in the final commit.

## Assigned

- Layer 3: `GainTextOf$` (Volrath's Shapeshifter, the one real line).
- Layer 8 remainder: `MayLookAt$`, `MayPlay$`, `AddHiddenKeyword$`, vote/villainous-choice params.

## Order (commit after each)

1. Layer 3: compile `GainTextAbilities$` as static Subs (golden AST regen, only volraths_shapeshifter changes); Card
   text-change record; `clearContinuousText` before Layer 2, `applyContinuousText` after it; composite Def (copied
   slices, blank faces 1+, zero SplitType); strip on battlefield exit (after LKI), before addCopy / turnFaceDown;
   copiableValues/UncopiedDef read pre-text Def. Tests + scenario fixture.
2. `AddHiddenKeyword$`: per-card hidden keyword list recomputed in the Rules pass, read by
   hasKeywordText/hasKeywordTextPrefix; AffectedDefined$ Self/Enchanted/Equipped; only the four strings the engine
   reads, skip the rest.
3. Votes: AdditionalVote$/AdditionalOptionalVote$/AdditionalVillainousChoice$ as RulesEffect fields; wire voteeffect.go
   and villainouschoiceeffect.go; ControlVote stays fail-closed unless trivial.
4. MayLookAt$: documented no-op (engine is omniscient, lookateffect.go).
5. MayPlay$ (optional narrow slice): plain `MayPlay$ True` (+WithoutManaCost/WithFlash) grants consulted by
   CastSpell/PlayLand; skip Limit/AltManaCost/IgnoreType/IgnoreColor/MayPlayText/MayPlayPlayer.
6. Docs: port-log note, "Not ported yet" row correction, gates full, delete this file.
