# Porter plan: ChangeX

Assigned: `ChangeX` (`ChangeXEffect.java`). Batch doc slug `changex`. Not ours: `ControlSpell`.

Corpus: 2 lines, both `Defined$ TriggeredSpellAbility` (Unbound Flourishing, `Mode$ SpellCast` + `HasXManaCost$ True`,
`Value$ TriggeredSpellAbility>Count$xPaid/Twice`; Glava, `Mode$ SpellAbilityCast`, unported mode, `Value$ 5`).

Steps, one commit each:

1. Primitive: `Ability.xManaCostPaid`/`hasXManaCostPaid` (Java `SpellAbility.xManaCostPaid`, nullable Integer);
   `payManaCostX` in manapay.go, `PayManaCost` wraps it; set on cast (permanent, Aura, instant/sorcery) and activated
   abilities before `PushAbility`; `withoutManaCost` leaves X unannounced. Tests.
2. `HasXManaCost$` in `checkSpellCastTriggers` (TriggerSpellAbilityCastOrCopy.java:171-181, spell branch). Tests.
3. `changexeffect.go`: Defined$ TriggeredSpellAbility only; Value$ literal or `TriggeredSpellAbility>Count$xPaid[/op]`
   via `doXMath`. Registry regen, tests, coverage.
4. Docs: `effects-changex.md`, index row, counts 170 -> 171. Scenario fixture if expressible. Full gates.
5. Delete this file.
