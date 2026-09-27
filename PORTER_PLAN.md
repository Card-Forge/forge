# Porter plan: ControlSpell

API: `ControlSpell` (ControlSpellEffect.java). Batch slug `controlspell`.

1. Research Java resolve + corpus shapes; verify stale blocker in effects-batch-b.md against current stack/targeting.
2. Decide routine vs blocked. If routine: `controlspelleffect.go`, regenerate registry, tests, docs
   `effects-controlspell.md` + index row + counts.
3. If blocked: re-defer with accurate blocker in `effects-controlspell.md`.
4. gates.sh full, commit, delete this file in final commit.

Not mine: ChangeX (sibling porter). Any ability.go edit must be additive.
