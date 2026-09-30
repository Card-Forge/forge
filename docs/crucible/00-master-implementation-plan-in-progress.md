# Crucible — Implementation Plan: In Progress

Milestones M5-M6, currently underway. Roadmap overview and completed milestones (M0-M4):
[00-master-implementation-plan.md](00-master-implementation-plan.md). Not-yet-started milestones (M7-M9):
[00-master-implementation-plan-open.md](00-master-implementation-plan-open.md).

---

## Milestones

### M5 — Rules kernel — 6–10 wks _(the largest single risk)_

**In progress.**

24. Turn/phase/step loop + priority (`PhaseHandler` port). **Done** — `turn.go`, `phase.go`, including CR 511.3's end of
    combat cleanup (`endCombat`); `PassPriority` (ADR-0019) and the turn driver `Game.Step`/`Game.Run` (`driver.go`,
    ADR-0026), which plays whole steps and games through priority rounds with combat's turn-based actions, the
    no-attackers skip, CR 514.3a's cleanup repeat and a turn cap. Design notes:
    [`## Turn driver: ADR-0026`](porting/port-log/game-state/turn-stack-combat.md#turn-driver-adr-0026).
25. Zone changes + state-based actions + game-over (`GameAction` port — budget the most time here). **Done** for the
    SBAs reached so far (`action.go`: legend rule, World rule, zero toughness/loyalty/defense, lethal damage, Battle
    protector, dangling-attachment cleanup including an Aura's own `Enchant` restriction against its still-present
    host); zone-change machinery itself (`Game.Move`) exists, and now so does LKI tracking (`Game.LKI`, CR 603.6d's own
    "look back in time" -- `Move`'s own battlefield-leaving branch freezes a copy of the card before clearing its own
    `Counters`/`PT`/`TypeMod`/`ColorMod`/`KeywordMod`, so `checkDiesTriggers`/`otherDiesTriggerMatches` (trigger.go,
    item 26) can still match a `ValidCard$` naming the dying card's own power, toughness, type, color, a keyword or a
    counter against what it had the instant before it died rather than the printed-only state `Move` has already reset
    it to by the time either function runs -- 116 of the corpus's own 7,574 real `Mode$ ChangesZone` lines whose
    `Destination$` permits Graveyard name exactly that shape, Retched Wretch's own real "when CARDNAME dies, if it had a
    -1/-1 counter on it..." among them. `Card.Def`/`Card.Controller()` never needed the lookup (`Move`'s own doc comment
    already covers why), so this is a plain struct copy rather than Java's own `CardCopyService.getLKICopy()`'s
    field-by-field reconstruction, overwritten whole -- never merged -- on every subsequent trip off the battlefield;
    `Game.Clone` (M7's own AI lookahead) gives its own copy an independent snapshot, the identical "shares nothing
    writable" contract it already holds for every other per-card ledger. The legend rule's own `ignoreLegendRule`
    exemption is ported too (`ignoreLegendRule`, `staticability.go`, ported from `StaticAbilityIgnoreLegendRule`) — a
    plain `ValidCard` match against every battlefield permanent, needing none of item 27's own layer-folding machinery,
    the same reason `CantBlockBy` (item 28) turned out independently buildable. The legend rule's own Corner Case 2
    (`resolveLegendRule`'s own doc comment, action.go, Java's own name for it) is built too: two or more legendary
    permanents that all carry `HasNonLegendaryCreatureNames` (card.go) -- a new Layer 3 continuous effect,
    `applyContinuousNames` (continuous.go), resolving Spy Kit's own real, and the corpus's only,
    `AddNames$ AllNonLegendaryCreatureNames` line via `Card.AttachedTo()`, this port's existing generic Aura/Equipment/
    Fortification attachment link, for its own `AffectedDefined$ Equipped` shape -- clash with each other even when
    their own printed names differ, grouped and resolved the identical way an ordinary same-name duplicate already is.
    Not reached: Corner Case 1, whether a Corner-Case-2 permanent's own borrowed names collide with some OTHER
    legendary's own literal printed name (`StaticData.instance().getCommonCards().isNonLegendaryCreatureName`,
    GameAction.java) -- needs a lookup across every creature card this game ever printed, not just what is on this
    battlefield, and this port's `*Game` holds no `*carddb.DB` reference to ask; threading one through every `*Game`
    constructor across the whole test suite is a disproportionately large refactor for the one corpus card (Spy Kit) it
    would unlock (PORT-8).

    The "cleanup aura" rule's own Protection/Hexproof gap (CR 702.11h/702.16e — a static-ability "can't be enchanted"
    question, distinct from the `Enchant` restriction itself) is closed too, for both real corpus shapes: Protection's
    own `CantAttach` half is `hostRefusesAttach` (`staticability.go`), reusing `protectionEach` (item 28) against the
    aura card itself rather than a candidate blocker — Java's own Protection branch synthesizes a
    `Mode$ CantAttach | ValidCard$ <valid>` line alongside CantBlockBy's `ValidBlocker$ <valid>`, the identical string —
    checked on every ongoing SBA pass (`cleanupDanglingAttachments`, above). Hexproof, Shroud and Protection's targeting
    half are `cardCantBeTargetedBy` instead (`staticability.go`,
    [`## Hexproof, Shroud and Protection refuse a target`](porting/port-log/game-state/targeting-and-chaining.md#hexproof-shroud-and-protection-refuse-a-target)),
    checked when an Aura is cast (`enchantTargets`, castspell.go, CR 601.2c's own legal-target set) and at the CR 608.2b
    re-check — not on the ongoing SBA pass, since Java's own `cantBeEnchantedByMsg` never re-checks either there.
    Writing the first version of this (since folded into the split above) surfaced a real, separate gap:
    `protectionEach`/`landwalkType` (item 28) read only a card's PRINTED keyword lines, missing one a Layer 6 continuous
    effect grants — closed by a new `Card.KeywordLines` (card.go) both now share with `HasKeyword`. A qualified Hexproof
    (`Hexproof:Black`, `Hexproof:Enchantment`, ...) resolves too: `hexproofValidSource` (staticability.go) ports
    `KeywordWithType.parse`'s own bare-color-word case (a color name is prefixed `Card.` before it reaches `Matches`,
    since it is not itself a recognized valid-string base — `colorFromName`, valid.go, already has the exact name set)
    and its bare-type fallthrough (a type word stays as-is, `baseMatches`'s own ordinary type check). The ability-source
    shape (`Hexproof:Triggered`/`Hexproof:Activated`, `ValidSA$` in Java, 2 real lines) still refuses rather than
    resolves — `Matches` never evaluates a `SpellAbility`, and an Aura's own cast-time targeting is not itself a
    triggered or activated ability doing the targeting anyway, so refusing produces the same observable result here as
    resolving it correctly would.

26. Stack, simultaneous trigger ordering, replacement effects (`MagicStack`, `replacement/`). **Real content for two
    cast shapes, twelve trigger modes, and CR 603.3b's own APNAP ordering now** — `Game.CastSpell` (`castspell.go`) is a
    real (non-test) `PushAbility`/`ResolveStack` caller for both a non-Aura permanent (CR 601 trimmed to nothing left to
    decide, `permanentEffect`) and an Aura (`castAura`: a target chosen from every battlefield permanent `enchantSpec`'s
    parsed `Enchant` restriction matches, `ChooseEnchantTarget` asked only when more than one does, carried on a new
    `Ability.Target` field and read back by `attachEffect` at resolution, CR 601.2c). `checkETBTriggers`/
    `checkDiesTriggers`/`checkAttacksTriggers`/`checkBlocksTriggers`/`checkDamageDoneTriggersToCard`/
    `checkDamageDoneTriggersToPlayer`/`checkDiscardedTriggers`/`checkTapsTriggers`/`checkTapsForManaTriggers`/
    `checkSpellCastTriggers`/`checkAttackersDeclaredTrigger`/`checkDrawnTriggers` (`trigger.go`, fifteen functions in
    all once each mode's own "other" half is counted) cover the corpus's twelve most frequent trigger shapes — a
    permanent's own "enters" (`Mode$ ChangesZone`, `Destination$ Battlefield`), "dies" (`Mode$ ChangesZone`,
    `Origin$ Battlefield`, `Destination$ Graveyard`, CR 700.4), "attacks" (`Mode$ Attacks`, CR 508.3), "blocks"
    (`Mode$ Blocks`, CR 509.2), "deals damage" (`Mode$ DamageDone`, CR 603, `TriggerDamageDone.performTest`), "is
    discarded" (`Mode$ Discarded`, CR 603, `TriggerDiscarded.performTest`), "becomes tapped" (`Mode$ Taps`, CR 603,
    `TriggerTaps.performTest`), "taps for mana" (`Mode$ TapsForMana`, `TriggerTapsForMana.performTest`), "a player casts
    a spell" (`Mode$ SpellCast`, CR 603) and "the beginning of a step or phase" (`Mode$ Phase`, CR 500,
    `checkPhaseTriggers`, added later once corpus-frequency research found it the corpus's SECOND most frequent mode
    after `ChangesZone` — 2,362 real lines, ahead of `Attacks` itself), "a player attacks" (`Mode$ AttackersDeclared`,
    CR 508.1, `checkAttackersDeclaredTrigger`, added once corpus-frequency research turned up 286 real lines, more than
    `SpellCast`'s own unresolved remainder was worth chasing further) and "a player draws a card" (`Mode$ Drawn`, CR
    120.3, `checkDrawnTriggers`, 161 real lines, called from `DrawCards`' own per-card loop, turn.go, already written
    that way before this mode existed to consume it) — the first two also checked against every OTHER battlefield
    permanent's own matching trigger (`otherETBTriggerMatches`/`otherDiesTriggerMatches`); `Attacks`, `Blocks`,
    `DamageDone`, `Taps`, `TapsForMana`, `SpellCast`, `AttackersDeclared` and `Drawn` need no separate "other" loop at
    all, since none of those Java trigger classes special-cases its own host's trigger to begin with — one walk over the
    battlefield covers both "this creature attacks/blocks/deals damage/becomes tapped"/"you cast a spell" and "a
    creature you control attacks/blocks/deals damage/becomes tapped"/"a player casts a spell" alike, and
    `AttackersDeclared` fires at most once per combat regardless of whose battlefield its host sits on to begin with.
    `Discarded` is the one exception: a "Card.Self" shaped line (14 of 105 real lines, the Madness-adjacent "when this
    card is discarded, you may cast it" shape) lives on a card that is never on the battlefield when it fires —
    discarded FROM HAND — so `checkDiscardedTriggers` needed its own explicit "own" half checking the discarded card
    directly, on top of `checkOtherDiscardedTriggers`' battlefield walk (caught by `TestCleanupFiresDiscardedTrigger`
    failing on the first, battlefield-only attempt — a self-caught gap, not a hypothetical one). `Blocks` needs
    `ValidCard` (matched against the declared blocker) and `ValidBlocked$` (8 of 127 real lines, every one an "or
    blocks/becomes blocked by one or more X creatures" description), checked against `blk.Attacker` directly: Java's own
    `performTest` matches it against the FULL collection of attackers one blocker blocks, ANY of which satisfying it
    fires the trigger once, but `checkBlocksTriggers` is already called once per declared `Block` rather than once per
    blocker with every attacker gathered, so checking the one attacker each call already has stands in for "any member
    of the collection" exactly, since a legal declaration has every blocker blocking one attacker (ADR-0024,
    `DeclareCombatBlockers`, block.go). `DamageDone` splits into two functions because the actual damaged object is
    either a `*Card` or a `*Player` (Java's own `DamageTarget` is a `GameEntity`), each needing a different
    `ValidTarget` evaluator — fired from `dealPermanentDamage`/ `dealPlayerDamage` (combatdamage.go), `CombatDamage$`
    checked against a hardcoded `true` since nothing outside combat deals damage in this port yet. `Taps` fires from
    this port's only two real tap sites (`DeclareCombatAttackers`, attack.go; `TapLandForMana`, manaability.go),
    `Attacker$` resolved as the boolean that tells them apart; `TapsForMana` is `Taps`'s own narrower sibling, its own
    separate Java `Trigger` subclass, firing only from the mana-ability site.
    `SpellCast`/`CantBlockBy`/`DamageDone`/`Discarded`/`Taps`/`TapsForMana` all match something other than a `*Card` at
    some point (`ValidActivatingPlayer`, `ValidDefender`, `ValidSource`/`ValidTarget`-as-a-player, `ValidPlayer`,
    `Activator`) — a new `matchesPlayerBase` (`valid.go`) is the shared three-bare-value (`You`/`Opponent`/`Player`)
    dispatch all of them now reuse, factored out once a third caller needed the identical switch two callers had already
    written separately. `matchesPlayerSpec`/`matchesPlayerProperty` (`valid.go`) sit on top of it, the identical
    `Base.Property` split `Player.isValid` (Player.java) itself does, adding `Active`/`NonActive`
    (`Game.ActivePlayer()`) and `Other` (not `sourceController`) — `SpellCast`'s own `matchesActivatingPlayer`,
    `DamageDone`'s own `ValidTarget`-as-a-player and `TapsForMana`'s own `Activator` call it, closing 19 of
    `SpellCast`'s 25 real qualified `ValidActivatingPlayer$` lines, `DamageDone`'s own qualified
    `ValidTarget$ Player.Opponent`/`Player.Other` and `TapsForMana`'s own `Activator$ Player.NonActive`; `CantBlockBy`'s
    `ValidDefender`, `Discarded`'s `ValidPlayer` and `Taps`'s `ValidPlayer` keep calling `matchesPlayerBase` directly,
    verified against the real corpus to carry zero qualified lines for those exact params. `Phase` needs no `ValidCard`
    at all -- there is no object a step or phase change happens TO, only `Phase$` itself (the step/phase name(s),
    resolved against `phase.go`'s own `PhaseType`/`PhaseByName`, built with this mode in mind from the start) and
    `ValidPlayer$` (matched against the ACTIVE player, `AbilityKey.Player` in Java's own `PhaseHandler.onPhaseBegin`,
    not the trigger's own host controller the way every other mode's player-shaped param is) -- and, unique among every
    mode this port checks, an explicit `TriggerZones$` walk rather than an implicit battlefield-only one: `Phase` is
    real from the graveyard, exile and command zone too (a suspend/exiled-with-triggers shape), so `checkPhaseTriggers`
    walks all four real zones (`phaseTriggerZones`, `trigger.go`) rather than assuming battlefield the way every earlier
    mode's own single walk safely could. All twelve modes are keyed off `compile.Face.Triggers` (typed since M3, never
    read by the engine before now), and all twelve now push what they find through `pushTriggeredAbilities`
    (`trigger.go`) rather than `PushAbility` directly — CR 603.3b's own APNAP ordering: each trigger-check function
    collects its own matches (its "own" and "other" halves, where it has both, feeding one `[]Ability`) and calls it
    once at the end. `pushTriggeredAbilities` walks `playersInAPNAPOrder` (`Game.ActivePlayer()` first, then
    `nextPlayerAfter`, `turn.go`'s own seating order) and pushes each player's whole group in that order; since the
    stack is LIFO and `MagicStack.addAllTriggeredAbilitiesToStack` itself pushes the active player's group first, the
    LAST player in APNAP order ends up on top, resolving FIRST — the active player's own group resolves last, ported
    faithfully from `MagicStack.chooseOrderOfSimultaneousStackEntry`'s own player-iteration order. A single player's own
    multiple matches stay in the order they were found rather than a real player choice
    (`Player.orderAndPlaySimultaneousSa`), since no `PlayerController` hook for that exists yet. Caught and proven by
    `TestPlayLandPushesETBTriggersInAPNAPOrder` (trigger_test.go): before this, every trigger-check function pushed each
    match the instant it found one, correct only when a single card's own trigger fires alone. A trigger's `Execute$`
    sub-ability's own params (`Defined$`, `NumCards$`, ...) travel onto the stack now too (`Ability.Params`,
    `ability.go`) — the gap that blocked resolving anything a real trigger pushed until `Draw` (`draweffect.go`) became
    the first of the 203 corpus-frequency APIs `NewRegistry` implements beyond casting itself, and `DealDamage`
    (`dealdamageeffect.go`) the second — 62 of the corpus's 2,219 real `(AB|DB)$ DealDamage` lines naming
    `Defined$ You`/`Player.Opponent`/`Opponent`/`Self` and no other unresolved param (822 name any `Defined$` value at
    all), reusing combat's own damage machinery directly rather than a parallel copy: `dealPermanentDamage`/
    `dealPlayerDamage` (combatdamage.go) gained an `isCombat bool` parameter, every prior call site combat's own and now
    passing `true` explicitly, `DealDamage` the first to pass `false` — threading through to `damagePrevented`/
    `damagePreventedPlayer` (CR 614's own "prevent all of this damage," this item's own replacement-effects paragraph
    below) and to a conditional `FlagCombat` (event.go's own doc comment, "marks damage dealt in combat rather than by
    an effect," dormant until now). `Ability` also gained an `Amounts` field, threaded through all eighteen
    check-triggers call sites' own `Ability{...}` construction (`face.Amounts`, already in scope at each) so
    `resolveNamedAmount` (amount.go) can resolve a named-SVar param value off the stack the identical way a continuous
    effect's own numeric params already do — `NumDmg$`'s own shape for `DealDamage`, and `Draw`'s own `NumCards$`
    upgraded to the same resolver for free once it existed. `definedPlayers` (new `defined.go`) is `drawDefinedPlayers`
    renamed and relocated once `DealDamage` needed the identical `You`/`Opponent`/ `Player.Opponent` resolution its own
    `Defined$` already has — neither effect owns it outright, the same "shared, so neither" reason `resolveAmount`
    (amount.go) sits apart from `ptParam`/`triggerCommonRequirementsMet`. `Defined$ Self` resolves against the ability's
    own host card directly (`a.Source`), `HasKeyword` reading its own `Deathtouch` for `dealPermanentDamage`'s own flag
    the identical way combat's own attacker/blocker already do. `ConditionPresent$`/`ConditionCompare$`/
    `ConditionCheckSVar$`/`ConditionSVarCompare$` (5 of 822, once every other still-unresolved param below is excluded)
    are resolved too, through a new shared `subAbilityConditionMet` (`condition.go`) — `SpellAbilityCondition.areMet`'s
    own gate, the exact same fix a checkland's `DB$ Tap` needed (below); a met condition runs as normal, an unmet one
    returns `nil` rather than an error, `SpellAbilityCondition.areMet`'s own "the ability does nothing" contract.

    Not resolved, each failing loudly by name rather than guessing (PORT-8/GO-7):
    - `DamageSource$` (17 of 822 real `Defined$` lines) — a source other than the ability's own host, needing a
      reference vocabulary this port does not have.
    - `SubAbility$` no longer blocks (`subability.go`, this item's own paragraph below, "SubAbility chaining itself
      landed"): 9 of 316 real SVar-defined `DealDamage` lines naming it chain to an already-built leaf ability and
      resolve end to end.
    - `Condition$` itself — `SpellAbilityCondition`'s own separate Threshold/Metalcraft/... flag switch.
    - `ConditionDefined$` — an arbitrary reference this port has no Defined$-to-objects resolver for.
    - `Planeswalker$`, `UnlessPayer$`, `UnlessCost$`, `UnlessResolveSubs$`, `ValidTgts$`, `TriggeredSpellAbility$`,
      `DamageMap$`, `CounterNum$`, `Optional$`, `TgtPrompt$` — each its own further mechanic.
    - `NoPrevention$` (1) — this port's own `damagePrevented`/`damagePreventedPlayer` would otherwise wrongly apply
      where Java's own `AbilityKey.NoPreventDamage` says not to.

    `ResolveStack` still reports `ErrUnimplemented` for the other 196 once
    `GainLife`/`Pump`/`PumpAll`/`LoseLife`/`PutCounter` (below) are counted alongside it. `Draw` never named
    `SubAbility$` among its own unresolved params, so once `resolveSubAbility` (subability.go, "SubAbility chaining
    itself landed," below) landed it started chaining for free: 170 of the corpus's own 747 real SVar-defined `Draw`
    lines naming `SubAbility$` chain to an already-built leaf ability and resolve end to end — Rousing Read's own real
    "draw two cards, then discard a card" (`DB$ Draw`, chaining into `DB$ Discard`) among them.

    **`GainLife` (`gainlifeeffect.go`) is M6's third script-driven effect, and the corpus's single largest resolvable
    slice past `DealDamage`** — 857 of the corpus's 1,700 real `(AB|DB)$ GainLife` lines that name
    `Defined$ You`/`Player.Opponent` and carry no other unresolved param. `dealDamageEffect`'s own shape reused
    directly: `LifeAmount$` through `resolveNamedAmount` (amount.go), `Defined$` through `definedPlayers` (defined.go),
    `subAbilityConditionMet` (condition.go) gating resolution the identical way it gates `DealDamage`'s and a
    checkland's `DB$ Tap` — `ConditionPresent$`/`ConditionCompare$`/`ConditionCheckSVar$`/`ConditionSVarCompare$`
    resolve, `Condition$` itself and `ConditionDefined$`/`ConditionZone$`/`ConditionOptionalPaid$` still fail loudly by
    name. No `Self` shape (a player gains life, never a card) and no prevention machinery reused, since none exists for
    life yet: CR 119's own "life gain replacement" family (`Event$ GainLife`, 22 real replacement lines) is not built,
    the identical real-gap-not-a-wrong-answer every other unbuilt replacement remainder already is. `Player.Life` gains
    directly; the identical `LifeChanged` event `dealPlayerDamage` already emits for a life LOSS (combatdamage.go) is
    emitted with a positive `Amount` for the gain, reused rather than duplicated. `SubAbility$` no longer blocks
    `GainLife`'s own resolution — removed from its own unresolved-param list once `resolveSubAbility` (subability.go,
    "SubAbility chaining itself landed," below) landed: 18 of the corpus's own 253 real SVar-defined `GainLife` lines
    naming `SubAbility$` chain to an already-built leaf ability and resolve end to end.

    **`Pump` (`pumpeffect.go`) is M6's fourth script-driven effect, and the corpus's own single largest by real line
    count after `ChangeZone`/`Draw`** — 4,103 real `(AB|DB)$ Pump` lines, 1,147 of them resolved here:
    `Defined$ Self`/`Enchanted`/`Equipped` (a new `definedCards`, defined.go — `Self` resolves to the ability's own host
    directly, `Enchanted`/`Equipped` to what the host is attached to, `Card.AttachedTo`) rather than a real target, plus
    `NumAtt$`/`NumDef$` (a plain integer or a named SVar, through `resolveNamedAmount`) and/or `KW$` (a literal,
    `" & "`-separated keyword list, through `keywordTokens`, continuous.go — reused rather than duplicated, the
    identical token split and dynamic-marker rejection `AddKeyword$` already needed), gated by `PumpZone$`'s own zone
    restriction (absent means Battlefield alone, `ZoneType.listValueOf`'s own Java default) and
    `subAbilityConditionMet`'s own Condition-family pair the identical way `DealDamage`'s/`GainLife`'s already are.

    This is the first script-driven effect whose own contribution outlives its `Resolve` call: `Duration$`'s default,
    "until end of turn," is a continuous effect this port never needed a duration for before (`applyContinuousPT`'s own
    doc comment used to name this as the one gap keeping its own blanket per-pass `PT.Clear()` correct only by
    accident). A new `Game.pumps` ledger (`pumpRecord`, game.go) records each resolved Pump's own contribution instead
    of a `Mode$ Continuous` static line, re-added into its target's own `PT`/`KeywordMod` every `CheckStateBasedActions`
    pass by a new `applyPumpEffects` (continuous.go, called right after `applyContinuousPT`/`applyContinuousKeyword` so
    their own per-pass `Clear()` has already run) rather than derived from a card script at all. `cleanupStep` (turn.go)
    drops every non-`Permanent` record at end of turn — CR 514.2's own "until end of turn" effects wearing off, the gap
    its own doc comment used to name too. `Game.Move` also drops every record naming a card the moment it leaves the
    battlefield (`clearPumps`, game.go): `CardID` is stable across zone changes here (ADR-0009), so without this a
    record would silently survive a trip to the graveyard and reapply the moment a Raise Dead-style effect returned the
    same `CardID` to the battlefield — Java's own `applyPump` avoids this with a per-instance game-timestamp check this
    port has no equivalent of, so dropping the record on exit gets the same real-world answer without one.

    Not resolved, each failing loudly by name rather than guessing (PORT-8/GO-7): `Condition$` itself and
    `ConditionDefined$`/`ConditionZone$`/`ConditionPlayerTurn$`/`ConditionActivationLimit$` (0/19/0/4) —
    `SpellAbilityCondition`'s own shapes `subAbilityConditionMet` does not cover, the identical `DealDamage`/`GainLife`
    -shaped gap; `NumAtt$`/`NumDef$` naming the literal `Double`/`Triple` (1 combined) — the target's own power or
    toughness doubled or tripled, a special case rather than a named SVar; a `KW$` token starting with `HIDDEN` (22) — a
    hidden-keyword phrase, its own separate mechanic; `UnlessCost$`/`UnlessPayer$`/`UnlessSwitched$` (6/6/4) — CR
    601.2i's own "unless a cost is paid" branch; `ValidTgts$` (1) — a real target past the `Defined$` card this effect
    already resolves; `AtEOT$` (9) — `registerDelayedTrigger`, a new trigger this effect would silently fail to create;
    `CanBlockAmount$`/`CanBlockAny$`, `DefinedKW$`/`KWChoice$`/`RandomKeyword$`,
    `SharedKeywordsZone$`/`SharedRestrictions$`, `DefinedLandwalk$`, `ImprintCards$`, `NoteCards$`/`NoteCardsFor$`
    /`ClearNotedCardsFor$`/`NoteNumber$`, `IsPresent$`, `Optional$`/`OptionQuestion$`, `Radiance$` (34 combined) — each
    its own further mechanic or an unclear shape not worth guessing at from a handful of real lines. `SubAbility$` no
    longer blocks ("SubAbility chaining itself landed," below): 17 of 571 real SVar-defined `Pump` lines naming it chain
    to an already-built leaf ability and resolve end to end.

    **`PumpAll` (`pumpalleffect.go`) is M6's fifth script-driven effect, `Pump`'s own blanket sibling** — a
    `ValidCards$`-matched set across every player's own battlefield (or, with `Defined$ You`/`Player.Opponent`, only the
    named players', through `definedPlayers`) rather than a single `Defined$` card or a real target — 642 of the
    corpus's 833 real `(AB|DB)$ PumpAll` lines resolve, 818 of them the real corpus's own dominant shape: no `Defined$`
    and no target at all, CR 611 applied blanket, the "every creature you control gets +X/+X" anthem-spell reading
    (Overrun, ...). `NumAtt$`/`NumDef$`/`KW$`/`Duration$`/`PumpZone$`/`subAbilityConditionMet`'s own Condition-family
    pair are `Pump`'s own identical machinery reused outright (`pumpAmount`/`pumpKeywords`, pumpeffect.go, generalized
    to take an effect name for their own error text once `PumpAll` became a second caller); `Game.pumps`/
    `applyPumpEffects`/`cleanupStep`'s own duration tracking (`Pump`'s own paragraph, above) needs no changes at all,
    since a `pumpRecord` never itself distinguishes which effect created it. `PumpZone$`'s own meaning shifts from a
    single-target zone check to the list of zones actually scanned (`pumpAllZones`, new) — `ZoneType.listValueOf`'s own
    default, Battlefield alone, when absent. `SubAbility$` no longer blocks ("SubAbility chaining itself landed,"
    below): 6 of 75 real SVar-defined `PumpAll` lines naming it chain to an already-built leaf ability and resolve end
    to end. Not resolved, each failing loudly by name rather than guessing (PORT-8/GO-7): `Condition$` itself and
    `ConditionDefined$`/`ConditionZone$`/`ConditionPlayerTurn$`/`ConditionManaSpent$`/`ConditionManaNotSpent$`
    (4/5/3/1/4/0) — `SpellAbilityCondition`'s own shapes `subAbilityConditionMet` does not cover, the identical `Pump`
    -shaped gap; `ValidTgts$` (12) — a real target past the blanket `ValidCards$` match; targeting itself now exists
    (targeting's own paragraph, below), `PumpAll` just has not been extended to read `Targeted` back yet;
    `Planeswalker$`/`Ultimate$` (26/13) — unclear semantics on a `PumpAll` line, not worth guessing at;
    `RememberPumped$`/`SharedKeywordsZone$`/`SharedRestrictions$`/`UnlessCost$`/`UnlessPayer$`/`ModeCost$`/`Exhaust$`
    (8/4/4/3/3/3/4) — each its own further mechanic.

    **`LoseLife` (`loselifeeffect.go`) is M6's sixth script-driven effect, `GainLife`'s own mirror image** — a
    plain-or-named-SVar `LifeAmount$` subtracted from a `Defined$` or targeted player rather than added,
    `resolveNamedAmount`/`definedPlayers`/`subAbilityConditionMet` all reused outright. 300 of the corpus's 445 real
    `(AB|DB)$ LoseLife` lines naming `Defined$ You`/`Opponent`/`Player.Opponent` or a resolvable `ValidTgts$`, and
    carrying no other unresolved param, resolve (226 by `Defined$` alone; targeting's own paragraph below closes 74
    more). Unlike `GainLife`, this calls no trigger check at all: Java's own `Player.loseLife` fires
    `TriggerType.LifeLost`, and `LifeLoseEffect.resolve` itself fires `TriggerType.LifeLostAll` again on top of that,
    but `Mode$ LifeLost`/ `LifeLostAll` both carry 0 real `T:` lines corpus-wide (`Mode$ LifeGained`'s own 98, by
    contrast, is why `checkLifeGainedTriggers` exists) — nothing to check, so nothing is built. `Player.Life` decrements
    directly, no "can't lose life" gate (`StaticAbilityCantGainLosePayLife`, symmetric to `GainLife`'s own missing
    "can't gain life" gate) and no CR 119 "life reduced" replacement family (`ReplacementType.LifeReduced`) built, the
    identical real-gap-not-a-wrong-answer `GainLife`'s own unbuilt "life gain replacement" remainder already is. The
    identical `LifeChanged` event `dealPlayerDamage`/`gainLifeEffect` already emit is reused with a negative `Amount`.
    Not resolved, each failing loudly by name rather than guessing (PORT-8/GO-7): `Condition$` itself and
    `ConditionDefined$`/`ConditionZone$` (0/14/1) — `SpellAbilityCondition`'s own shapes `subAbilityConditionMet` does
    not cover, the identical `GainLife`-shaped gap; `Planeswalker$`/`UnlessPayer$`/`UnlessCost$`/`UnlessSwitched$`
    (6/4/4/2 combined across the wider 823-line `Defined$` set) — each its own further mechanic;
    `Ultimate$`/`IsPresent$`/`PresentCompare$`/`NumCards$`/ `ModeCost$` (1/2/2/2/1) — unclear semantics on a `LoseLife`
    line, not worth guessing at from a handful of real lines. `ValidTgts$` itself no longer blocks — targeting's own
    paragraph, next, is why. `SubAbility$` (210 of 445) no longer blocks either, removed from its own unresolved-param
    list once `resolveSubAbility` (subability.go, "SubAbility chaining itself landed," below) landed — Sphinx
    Sovereign's own real "gain 3 life if untapped, otherwise each opponent loses 3" (one `DB$ LoseLife` with a
    `SubAbility$ DB$ GainLife`, the negated condition split across the two) is exactly why that chain runs regardless of
    whether `subAbilityConditionMet` let this effect's own body run. 144 of the corpus's own 382 real SVar-defined
    `LoseLife` lines naming `SubAbility$` chain to an already-built leaf ability and resolve end to end.

    **Targeting itself landed** (`targeting.go`) — CR 601.2c (a spell)/603.3b (a triggered ability)'s own "choose
    targets," this port's own most-cited gap across every M6 effect built before now (`ValidTgts$` sits in every one of
    their own "not resolved" lists). `resolveTargets` runs the moment an ability is about to be pushed onto the stack —
    `pushTriggeredAbilities` (trigger.go), this port's only pusher today, since `CastSpell` (castspell.go) only casts a
    permanent or an Aura, neither of which carries `ValidTgts$` on its own top-level record. It computes `ValidTgts$`'s
    own legal candidates two ways: every player still in the game (`matchesPlayerSpec`, valid.go, reused outright — its
    own `ok` return, "is this spec player-shaped at all," decides which of the two paths runs, tried once rather than
    per candidate since the answer never depends on which player is asked) or every card on any player's battlefield
    (`Matches`, valid.go, reused outright) — never both, since no real corpus line this port has read mixes card and
    player candidates in one `ValidTgts$` string. `TargetMin$`/`TargetMax$` (1/1 when neither is named,
    `TargetRestrictions.java`'s own default) resolve through `resolveNamedAmount` exactly as every other numeric param
    already does, then a new `PlayerController` method, `ChooseTargets` (its twenty-fourth), gets asked for that many. A
    structural shape this port does not parse — `Radiance$` (4 real lines, "and each other permanent that shares a color
    with it," a second, derived candidate set no single `ValidTgts$` evaluation produces),
    `TargetsForEachPlayer$`/`TargetsWithDefinedController$`/`TargetUnique$` (0 each) — folds into CR 603.3c's own "no
    legal targets, doesn't go on the stack" outcome rather than an error: the two are observationally identical from
    outside (the ability does nothing), and GO-7's "fail one game, not the batch" reasoning does not distinguish a card
    the game declined to put on the stack from one this port cannot parse the targeting for.

    Threading a `PlayerController` down to `pushTriggeredAbilities` touched all sixteen of its own callers across
    `trigger.go`, and each of those touched its own external caller in turn — `action.go`'s five state-based-action
    functions (`destroyLethalToughness`/`destroyDamagedCreatures`/`destroyZeroLoyalty`/`destroyZeroDefense`/
    `cleanupDanglingAttachments`, plus `resolveWorldRule`), `attack.go`/`block.go`'s combat-declaration functions,
    `combatdamage.go`'s damage-dealing chain, `manaability.go`'s `TapLandForMana`, `turn.go`'s `drawStep`/`DrawCards`,
    `land.go`'s `PlayLand`, and `castspell.go`'s cast/permanent/Aura effects. Every path bottomed out at a function an
    earlier chunk had already given a controller to (`CheckStateBasedActions`, `DeclareCombatAttackers`,
    `DeclareCombatBlockers`, `DealCombatDamage`, `beginPhase`, `CastSpell`, or `Effect.Resolve`'s own parameter), so the
    cascade stayed contained rather than reaching arbitrarily far up the call graph; `PlayLand`/`TapLandForMana` had no
    internal caller at all (only tests), so gaining the parameter was a clean addition, not a threading exercise.
    `internal/fixture`'s own `actions.go` (the `testdata/scenarios/` walker) picked up the same two calls.

    `definedPlayers`/`definedCards` (defined.go) both gained a `targets []EntityID` parameter and a
    `"Targeted"`/`"TargetedPlayer"`/`"ThisTargetedCard"` case reading it — `AbilityUtils.getDefinedPlayers`'s/
    `getDefinedCards`'s own literal cases for a `Defined$` value that explicitly names what a parent ability (or, for
    cards, `Ability.Target`'s own Aura shape's sibling) targeted, distinct from the effect-level dispatch below.
    `loseLifeEffect` is targeting's first real consumer, and does not route through those new `defined.go` cases at all:
    its own dispatch mirrors `LifeLoseEffect.java`'s `getTargetPlayers(sa)` directly (`SpellAbilityEffect.java`'s own
    base helper) — when `ValidTgts$` is present, read `a.Targets` outright, bypassing `Defined$` entirely, since 0 real
    `LoseLife` lines combine the two (matching `getTargetPlayers`'s own behavior: it never falls through to `Defined$`
    once the ability uses targeting at all). `PutCounter`/`Discard`/`Scry`/`PumpAll`/`Surveil` still block `ValidTgts$`
    outright in their own `Resolve` — the mechanism now exists for any of them to consume, extending each one to
    actually read `Targeted` back is not yet done.

    **SubAbility chaining itself landed** (`subability.go`) — `AbilityUtils.resolveApiAbility`'s own
    `resolveSubAbilities` call, this port's own second-most-cited gap after targeting: `SubAbility$` sits in every M6
    effect's own "not resolved" list above, 16,022 real corpus lines total (12% of the whole corpus), the compiler
    already resolving every reference into a tree at load time (`compile.Ability.Subs`, ADR-0007) with nothing at the
    engine layer ever walking it until now. `Registry.Resolve` (`effect.go`) chains an ability's own `SubAbility$`
    reference, if it names one, right after its own `Effect.Resolve` call succeeds — recursive through that same method
    for a chain more than one hop deep (`compile.Ability.Subs` already holds the whole tree, so no caller needs a loop
    of its own; 1,172 real lines chain exactly two hops past the first, up to 13 deep once) — and runs whether or not
    `subAbilityConditionMet` let the parent's own body run at all: `AbilityUtils.resolveApiAbility`'s own
    `if (sa.metConditions()) { sa.resolve(); } resolveSubAbilities(sa, game);` is unconditional on the second call, the
    exact shape Sphinx Sovereign's own real "you gain 3 life if untapped, otherwise each opponent loses 3" needs — one
    `DB$ LoseLife` (`Condition$ Card.tapped`) with a `SubAbility$ DB$ GainLife` (the identical condition, negated). Only
    the literal `SubAbility$` key auto-chains this way; `PreventionSubAbility$` and every "additional ability" key
    (`WinSubAbility$`, `ChooseSubAbility$`, `Choices$`, ...) name a sub-ability an effect fetches and resolves
    explicitly through its own Go code once that effect exists (`FlipCoinEffect.java`, `ChoosePlayerEffect.java`, ...) —
    none built yet, so this is the only chaining shape with a caller today. The child `Ability` built for the chain
    carries the parent's own `Source`/`Controller`/`Target`/`Targets`/`Amounts` unchanged — a sub-ability naming its own
    `ValidTgts$` (891 of 16,022 real referenced lines, 5.6%) is not targeted separately (`resolveTargets` runs once, on
    the pushed ability, before any of this), so it inherits whatever the parent had and an effect gating on `ValidTgts$`
    presence finds nothing to act on — the identical "unsupported shape observably folds into no legal targets" fold
    targeting's own paragraph above already committed to, not a new wrong-guess category. Chaining into an API this port
    has not built an `Effect` for yet still fails with `ErrUnimplemented` naming it, `Registry.Resolve`'s own existing
    contract for a top-level ability extended for free by the recursion — and the parts of the chain that already
    resolved stay resolved (CR's own sequential "this already happened," not an all-or-nothing rollback):
    riverwise_augur.txt's own real `DB$ Draw | Defined$ You | NumCards$ 3 | SubAbility$ DBChangeZone` still draws its
    three cards before the chain reaches `DB$ ChangeZone` (the single most-referenced `SubAbility$` target in the whole
    corpus, 203 script-driven APIs away from built) and fails naming it. `SubAbility$` no longer blocks any of the ten
    script-driven effects built so far — `Draw` never blocked it in the first place (its own paragraph, above);
    `GainLife`/`LoseLife`/`DealDamage`/`Pump`/`PumpAll`/`PutCounter`/`Discard`/`Scry`/`Surveil` each had it removed from
    their own unresolved-param list, each its own paragraph naming the count. An `APIByName` miss (a `SubAbility$` SVar
    body whose own leading value `ApiType.java` has no constant for) is not reachable against the real corpus today —
    `ApiType.java`'s own generated vocabulary and the apiscan/vocabscan gates (M3) already require every real API string
    to resolve — but errors naming it rather than silently dropping the chain, the identical PORT-8 "a card cannot be
    trusted not to be the first" reasoning `triggerEffectAPI`'s own identical defensive check already used for a
    trigger's `Execute$`.

    **`PutCounter` (`putcountereffect.go`) is M6's seventh script-driven effect, and the corpus's own second-largest
    resolvable slice after `Pump`** — 992 of the corpus's 3,165 real `(AB|DB)$ PutCounter` lines that name a single
    literal `CounterType$` and `Defined$ Self`/`Enchanted`/`Equipped`/`You`, carrying no other unresolved param.
    `CountersPutEffect.java` itself is 800 lines wide (`Bolster$`/`Monstrosity$`/`Adapt$`/`Support$`/`Choices$`/
    `DividedRandomly$`/`PutOnEachOther$`/`PutOnDefined$`/`EachFromSource$`/the `ETB$` counter-table replacement path and
    more, each its own further mechanic this port has nowhere to route through yet); this port keeps only the plain
    shape. `putCounterType` (new) reads `CounterType$` as a single literal name, uppercased —
    `CounterEnumType.getType`'s own `toUpperCase(Locale.ROOT)` canonicalization — so a corpus line writing `Stun` and
    another writing `STUN` (74 and 23 real lines) land on the identical `Counters` key rather than two; a
    comma-separated list (22, an interactive choice among types), `ExistingCounter` (8) and `Any` (a case-insensitive
    nil sentinel in Java, "any kind" rather than a concrete kind) all fail loudly instead. `CounterNum$` defaults to `1`
    (`getParamOrDefault`'s own Java default) and otherwise resolves through `resolveNamedAmount` exactly as
    `NumDmg$`/`LifeAmount$` already do. A new `definedCounterTargets` dispatches `Defined$` to a card (`definedCards`,
    unchanged) or a player (`definedPlayers`, unchanged) by which one the value itself names — the identical dispatch
    `CountersPutEffect.resolvePerType`'s own `obj instanceof Player`/`obj instanceof Card` check makes at the
    resolved-entity level, not by `CounterType$`: a player-only counter kind (energy, poison, ...) is only ever reached
    because `Defined$` itself names a player. `Card.Counters`/`Player.Counters` gain their first script-driven writer;
    `emitCounterChanged` (event.go) gets its first real caller past the hardcoded planeswalker-loyalty-on-entry path,
    and its own documented gap fires for real for the first time too — a script-written `CounterType$` past the eight
    named constants (`ENERGY`, ...) still gets the counter but emits no `CounterChanged` event, `counterDetail`'s own
    closed set unable to encode it. `SubAbility$` no longer blocks ("SubAbility chaining itself landed," above): 56 of
    623 real SVar-defined `PutCounter` lines naming it chain to an already-built leaf ability and resolve end to end.
    Not resolved, each failing loudly by name rather than guessing (PORT-8/GO-7): `ValidTgts$`/`TargetMin$`/`TargetMax$`
    (807/162/162) — a real target; targeting itself now exists (targeting's own paragraph, above), `PutCounter` just has
    not been extended to read `Targeted` back yet; `ETB$` (154) — CR 614's own counters-added-simultaneously replacement
    table (`GameEntityCounterTable`), the identical batching risk `ChangesZoneAll`'s own gap already documents;
    `Choices$` and its own six further params (46 combined) — an interactive multi-card choice this port's own
    `PlayerController` has no hook for; `Monstrosity$`/`Adapt$`/`Bolster$`/`Support$`/`PowerUp$`/`Exhaust$` and a dozen
    more per-target params — each its own further mechanic.

    **`discardEffect` (`discardeffect.go`) is M6's eighth script-driven effect, and the first that has to ask the
    resolving player anything mid-resolution rather than reading game state outright** — 285 of the corpus's 942 real
    `(AB|DB)$ Discard` lines that name `Mode$ TgtChoose` and `Defined$ You`/`Opponent`/`Player`/`Player.Opponent`,
    carrying no other unresolved param. `DiscardEffect.java` itself has eight further `Mode$` values (`Hand`, `Random`,
    `YouChoose`, `LookYouChoose`, `RevealYouChoose`, `RevealTgtChoose`, `RevealDiscardAll`, `Defined`) each its own
    further shape; this port keeps only `TgtChoose`, the corpus's largest at 728 of 942 real lines on its own.
    `TgtChoose`'s own resolution — the discarding player picks their own count of cards out of their own hand — is the
    identical decision shape `DiscardToHandSize` (control.go, CR 514.1's own cleanup discard) already has, but Java
    itself keeps the two as separate `PlayerController` methods (`chooseCardsToDiscardFrom` vs.
    `chooseCardsToDiscardToMaximumHandSize`) because `chooseCardsToDiscardFrom` additionally supports a
    `DiscardValid$`-filtered choice set and a count that need not be exact — neither modeled here, so this port adds a
    new, separately-queued `ChooseCardsToDiscard` (control.go) rather than reusing `DiscardToHandSize` outright.
    `Effect.Resolve` gained a `PlayerController` parameter for it (`effect.go`'s own doc comment) — every effect before
    `discardEffect` reads game state only and ignores the new parameter; `ResolveStack` already threaded one through to
    `CheckStateBasedActions`, so only the dispatch call itself needed the extra argument. `NumCards$` is clamped to the
    discarding player's actual hand size (`Math.min(numCards, numCardsInHand)`, Java's own), and a hand that is already
    empty skips the controller call entirely rather than asking for zero cards — the identical "nothing meaningful to
    decide" reasoning `cleanupStep`'s own doc comment already gives for `DiscardToHandSize`. `definedPlayers`
    (`defined.go`) gained a `"Player"` case: `AbilityUtils.getDefinedPlayers`'s own fallthrough `else` branch (a
    `defined` string matching none of its named cases resolves to every player in the game, unfiltered) — distinct from
    `"Player.Opponent"`, which is Java's dotted-suffix filter applied to that same fallthrough set, the identical
    opponents-only result `"Opponent"` gets directly (so both are one case here, as they already were before this
    chunk). `SubAbility$` no longer blocks ("SubAbility chaining itself landed," below): 11 of 254 real SVar-defined
    `Discard` lines naming it chain to an already-built leaf ability and resolve end to end. Not resolved, each failing
    loudly by name rather than discarding the wrong cards from the wrong player (PORT-8/GO-7): every `Mode$` other than
    `TgtChoose` (each its own further shape, above); `ValidTgts$`/`TargetMin$`/`TargetMax$` (98/3/3) — a real target;
    targeting itself now exists (targeting's own paragraph, below), `Discard` just has not been extended to read
    `Targeted` back yet; `Optional$` (38) — an interactive confirm this port's own `PlayerController` has no hook for;
    `AnyNumber$` (16) — a variable count, a different shape from `ChooseCardsToDiscard`'s own exact-count contract;
    `DiscardValid$`/`DiscardValidDesc$` (18) — a filtered choice set, the identical gap `PutCounter`'s own `Choices$`
    family already documents; `UnlessType$` (14) — a different sub-flow (`chooseCardsToDiscardUnlessType`, Java's own
    separate controller method); `RevealNumber$` — a reveal-then-choose-a-subset step ahead of the discard itself;
    `UnlessCost$`/`UnlessPayer$`/`UnlessSwitched$`/`UnlessResolveSubs$` (10/10/5/0) — "discard unless you pay a cost,"
    each its own further mechanic; `RememberDiscarded$`/`RememberDiscardingPlayers$`/`RememberDiscardingPlayer$` (88
    combined) — no `Defined$ Remembered` resolver exists to ever read the value back (chaining itself existing does not
    help here: `Discard` still blocks `SubAbility$` outright, and even unblocked, `defined.go` has no `"Remembered"`
    case), the identical "blocked outright rather than silently no-op'd" choice `PutCounter`'s own `RememberCards$`
    already made.

    **`scryEffect` (`scryeffect.go`) is M6's ninth script-driven effect, and the first that asks the resolving player to
    reorder a set of cards rather than choose a subset of them.** 332 of the corpus's 415 real `(AB|DB)$ Scry` lines
    resolve — every one naming `Defined$ You`/`Opponent`/`Player`/`Player.Opponent`, or naming no `Defined$` at all: CR
    701.19's own line is the first in M6 where an absent `Defined$` is itself a real, resolvable answer rather than a
    missing param, matching `AbilityUtils.getDefinedPlayers`'s own `changedDef = (def == null) ? "You" : ...` default
    exactly (every other effect so far treats a missing `Defined$` as `definedPlayers`'s own unresolvable-value error).
    `ScryEffect.java` itself is thin (51 lines); the real work is `GameAction.scry`'s own — look at the top `ScryNum$`
    cards of the deciding player's own library, then split them between a chosen top order and a chosen bottom order.
    That split is a genuinely new kind of decision this port had no hook for: `PlayerController` gained a twenty-second
    method, `ArrangeForScry` (control.go), returning the two piles' own orders directly rather than a single chosen
    subset the way `ChooseCardsToDiscard` (M6's eighth, above) does. Applying the "put back on top" half needed a new
    primitive too — `Game.MoveToLibraryTop` (game.go), `Game.Move`'s own mirror for the one end `Move` can never reach:
    `Move` always appends to a zone's own end (the library's own bottom, `mulligan.go`'s own tuck already relies on
    exactly that), so putting a card on top needed `collect.OrderedSet`'s own new `Prepend` (mirror of `Add`) underneath
    it. `MoveToLibraryTop` shares every other part of `Move`'s own behavior, including the Battlefield-transition
    cleanup, so a later tutor-to-top effect (`Destination$ Library | LibraryPosition$ 0`, not built) gets that cleanup
    for free rather than a scry-only shortcut. CR 614's own `Scry` replacement type and `Mode$ Scry` trigger are both
    skipped outright, not merely unresolved: 0 real corpus lines name either, so there is nothing to wire either
    mechanism into yet. `SubAbility$` no longer blocks ("SubAbility chaining itself landed," above): 31 of 57 real
    SVar-defined `Scry` lines naming it chain to an already-built leaf ability and resolve end to end. Not resolved,
    each failing loudly by name (PORT-8/GO-7): `ValidTgts$` (2) — targeting itself now exists (targeting's own
    paragraph, above), `Scry` just has not been extended to read `Targeted` back yet; `Optional$` (4) — an interactive
    confirm this port's own `PlayerController` has no hook for; `Planeswalker$` (8) — its own further mechanic.
    `Condition$` itself and `ConditionDefined$`/`ConditionZone$`/`ConditionPlayerTurn$` (5) skip the whole line via
    `subAbilityConditionMet`'s own unresolved-param list rather than a loud error, the identical silent-skip every other
    effect using it already gets; `ConditionPresent$`/`ConditionCompare$`/ `ConditionCheckSVar$`/`ConditionSVarCompare$`
    are resolved through it exactly as `Discard`'s/`PutCounter`'s own already are.

    **`surveilEffect` (`surveileffect.go`) is M6's tenth script-driven effect, and `scryEffect`'s own sibling decision
    reused wholesale rather than rebuilt.** CR 701.42's own shape is nearly identical to CR 701.19's: look at the top
    `Amount$` cards of the deciding player's own library, then split them between a chosen top order and a chosen "leave
    the top" pile — except Surveil's second pile goes to the graveyard, not the bottom of the library.
    `PlayerController` gained a twenty-third method, `ArrangeForSurveil` (control.go), the identical
    `(toTop, toSecondPile []CardID)` shape `ArrangeForScry` already has, and `ScriptedController`'s own `scryDecision`
    struct (a `toTop`/`toBottom` pair) is shared between the two rather than each declaring its own trivial copy —
    `toBottom` simply means "the graveyard" for `QueueSurveil`'s own answers. Applying `toTop` reuses
    `Game.MoveToLibraryTop` (game.go, `Scry`'s own new primitive) outright, in the identical reverse-then-prepend order;
    applying the graveyard half needs nothing new at all, since `Game.Move` already goes wherever its `kind` argument
    names. 183 of the corpus's 208 real `(AB|DB)$ Scry`-shaped `(AB|DB)$ Surveil` lines resolve — every one naming
    `Defined$ You`/`Opponent`/`Player`/`Player.Opponent` or no `Defined$` at all, the identical "absent `Defined$` means
    `You`" default `Scry`'s own paragraph above already covers, since `SurveilEffect.java` defaults the same way. CR
    702's own Surveil-number static modifier (`StaticAbilitySurveilNum`) is not ported — 0 real lines carry the
    qualifying keyword to trigger it — and CR 603's own `Mode$ Surveil` trigger is, the identical reason `Mode$ Scry`
    is, 0 real corpus lines, so nothing here checks a trigger at all. `SubAbility$` no longer blocks ("SubAbility
    chaining itself landed," above): 2 of 15 real SVar-defined `Surveil` lines naming it chain to an already-built leaf
    ability and resolve end to end. Not resolved, each failing loudly by name (PORT-8/GO-7): `ValidTgts$` — targeting
    itself now exists (targeting's own paragraph, above), `Surveil` just has not been extended to read `Targeted` back
    yet; `Planeswalker$` (5) — its own further mechanic; `RememberMoved$`/`RememberKept$` (2/1) — no
    `Defined$ Remembered` resolver exists to ever read the value back, the identical "blocked outright rather than
    silently no-op'd" choice `PutCounter`'s own `RememberCards$` already made; `Optional$`, present on 0 real `Surveil`
    lines today, is still blocked outright for symmetry with `Scry`'s own identical param, in case a future card adds
    it. `Condition$` itself and `ConditionDefined$`/`ConditionZone$`/`ConditionPlayerTurn$` skip the whole line via
    `subAbilityConditionMet`, the identical silent-skip `Scry`'s own already gets;
    `ConditionPresent$`/`ConditionCompare$`/`ConditionCheckSVar$`/`ConditionSVarCompare$` resolve through it exactly as
    `Scry`'s/`Discard`'s/`PutCounter`'s own already do.

    **`sacrificeEffect` (`sacrificeeffect.go`) is M6's eleventh script-driven effect.** CR 701.20, 465 of the corpus's
    792 real `(AB|DB)$ Sacrifice` lines: an absent `SacValid$` or the literal value `Self` sacrifices the ability's own
    host outright, no choice asked (`SacrificeEffect.java`'s own `valid.equals("Self")` branch); any other `SacValid$`
    value asks each of `Defined$`'s players (default `You`, `AbilityUtils.getDefinedPlayers`'s own null default,
    `Scry`'s own identical shape) to choose `Amount$` of their own matching battlefield permanents through a new
    `PlayerController` method, `ChoosePermanentsToSacrifice` (its twenty-sixth) — `ChooseCardsToDiscard`'s own shape
    reused for a second exactly-N-of-a-set decision. `ValidTgts$` resolves too, `LoseLife`'s own bypass-`Defined$`
    pattern reused (41 real player-shaped lines). `RememberSacrificed$` is this port's first real writer of
    `Memory.Remember` (`memory.go`, dormant scaffolding until now). `SubAbility$` no longer blocks ("SubAbility chaining
    itself landed," above). Not resolved, each failing loudly by name (PORT-8/GO-7): `UnlessPayer$`/ `UnlessCost$` (155
    combined, always co-occurring) — a further "unless a cost is paid" mechanic; `Optional$` (46) — the identical
    ability-body-level "may" gap `Discard`'s/`Pump`'s own already document, distinct from CR 603.3d's own
    `OptionalDecider$`; `Planeswalker$`/`ChangeNum$`/`ConditionDefined$`/`UnlessResolveSubs$`/`UnlessSwitched$`/
    `ValidCard$`/`SorcerySpeed$`/`SacEachValid$`/`Random$`/`Destroy$`/`StrictAmount$`/`Echo$`/`CumulativeUpkeep$` (each
    its own further mechanic or unclear semantics). `ConditionPresent$`/`ConditionCompare$`/`ConditionCheckSVar$`/
    `ConditionSVarCompare$` resolve through `subAbilityConditionMet` exactly as `Scry`'s/`Discard`'s/`PutCounter`'s own
    already do.

    Sacrificing a card also fires CR 701.20's own new `Mode$ Sacrificed` trigger (`checkSacrificedTriggers`,
    `trigger.go`, ported from `TriggerSacrificed.performTest`) right before the zone change —
    `Player. addSacrificedThisTurn`'s own ordering ahead of `sacrificeDestroy`'s own `moveToGraveyard`, both ported
    directly. Unlike every other per-card trigger dispatch this port has, this one walks the battlefield only once: the
    sacrificed card is still physically there at check time, so a separate own-half walk (`checkDiscardedTriggers`'s own
    shape) would fire its own trigger twice — it is already one of the permanents the single walk visits. 106 of 115
    real lines resolve: `ValidCard$` against the sacrificed card and `ValidPlayer$` against its own controller through
    the usual `matchesPlayerBase` dispatch, `PlayerTurn$`/`OptionalDecider$`/the whole `IsPresent$`/ `CheckSVar$`/...
    family through the shared `triggerEffectAPI` gate for free. `ActivationLimit$`/`ResolvedLimit$` (7/1, the identical
    per-turn-cap gap `LifeGained`'s own `ActivationLimit$` already documents) and `WhileKeyword$` (1) skip the whole
    line rather than firing unconditionally (GO-7).

    **`sacrificeAllEffect` (`sacrificealleffect.go`) is M6's twelfth script-driven effect, `Sacrifice`'s own blanket
    sibling.** `pumpAllEffect`'s own shape (pumpalleffect.go) reused for a second blanket effect: an absent `Defined$`
    scans every battlefield in the game, `ValidCards$`-filtered if present (Java's own `game.getCardsIn(Battlefield)`
    then an optional `AbilityUtils.filterListByType`) — 72 of the corpus's own 140 real `(AB|DB)$ SacrificeAll` lines,
    the corpus's own dominant real shape, name no `Defined$` at all — and a present `Defined$` names specific cards
    through `definedCards` (defined.go) instead: `Self`/`Enchanted`/`Equipped`/`Targeted`, an unrecognized value
    (`TriggeredObjectLKICopy`/`ChosenCard`/`Remembered`/... — real corpus values with no resolver) failing loudly rather
    than silently sacrificing nothing. `Controller$`, when present, narrows either set further to cards controlled by
    one of its own resolved players (`definedPlayers`, defined.go) — Java's own "do the controller check after LKI got
    updated" step reordered here since this port takes no LKI snapshot until the actual sacrifice happens
    (`sacrificeCards`, sacrificeeffect.go). 91 of the corpus's own 140 real lines resolve. Not resolved:
    `UnlessCost$`/`UnlessPayer$` (6/6, always co-occurring) — the identical "unless a cost is paid" gap `Sacrifice`'s
    own already documents; `ConditionDefined$` (3) — `SpellAbilityCondition`'s own shape `subAbilityConditionMet` does
    not cover; `Planeswalker$`/`Activator$`/`SorcerySpeed$`/`ImprintSacrificed$` (1 each) — each unclear semantics or
    its own further mechanic, not worth guessing at or building for one real line. `sacrificeCards` (sacrificeeffect.go)
    is shared outright with the plain `Sacrifice` effect, so `RememberSacrificed$` and CR 701.20's own
    `Mode$ Sacrificed` trigger (checkSacrificedTriggers, trigger.go) both fire once per card in the whole blanket set,
    not once for the ability as a whole — a watching permanent's own life-gain trigger fires twice for two sacrificed
    creatures in one `SacrificeAll` resolution.

    **CR 603.6d's own `Mode$ ChangesZoneAll` is real now too** (`checkChangesZoneAllTriggers`, trigger.go, ported from
    `TriggerChangesZoneAll.performTest`) — `Mode$ ChangesZone`'s own batched sibling, firing once for a whole group of
    cards that changed zones together (a board wipe's own "whenever one or more creatures you control die") rather than
    once per card the way the ordinary Dies trigger already does. Called once per batch a single game action moves
    together, every card in the batch sharing the identical origin and destination: `sacrificeCards`
    (sacrificeeffect.go, `Sacrifice`'s and `SacrificeAll`'s own shared caller, `SacrificeEffect.java`'s/
    `SacrificeAllEffect.java`'s own trailing `zoneMovements.triggerChangesZoneAll` call ported directly) and
    `destroyLethalToughness`/`destroyDamagedCreatures` (action.go, CR 704.5f-h's own simultaneous SBA sweeps). This is a
    deliberate simplification of Java's own `CardZoneTable`, which can hold cards with different origins in one table:
    every call site this port has today moves its whole batch the identical way, so a `cards []CardID` triple with one
    shared `origin`/`destination` loses nothing observable yet — a future call site mixing origins within one action
    would need a richer per-card table, not built. Every card has already left the battlefield by the time this runs, so
    unlike `checkSacrificedTriggers` there is no own-half/other-half split to get wrong: a card that was itself part of
    the batch is no longer on the battlefield to be asked about its own trigger. `Destination$`/`Origin$` resolve
    through `hasZoneOrAny` (reused from the ETB/Dies dispatch), `ValidCards$` through `Matches` against each card's own
    `g.LKI` snapshot when one exists (`checkDiesTriggers`'s own pattern, so a `Destination$ Graveyard` line still sees
    the card's pre-move state), and `PlayerTurn$`/`OptionalDecider$`/the whole `IsPresent$`/`CheckSVar$`/... family
    through the shared `triggerEffectAPI` gate. 77 of the corpus's own 126 real lines resolve. Not resolved:
    `ActivationLimit$` (41) — the identical per-turn-cap gap `LifeGained`'s own already documents; `ValidCause$` (4) — a
    `SpellAbility`, not a `Card`, `Matches` cannot evaluate one; `ResolvedLimit$` (3),
    `NoResolvingCheck$`/`InvertValidCause$`/`FirstTime$` (1 each) — each its own further mechanic or unclear semantics.
    A trigger carrying any of these six is skipped entirely, not fired unconditionally (GO-7). Two creatures killed by
    `destroyLethalToughness` and one killed by `destroyDamagedCreatures` in the identical `CheckStateBasedActions` call
    fire two separate `ChangesZoneAll` batches rather than one shared one — this port's own SBA split into one function
    per CR 704.5 clause, rather than Java's single combined pass, is narrower than CR 704.3's own full simultaneity, a
    real, narrow simplification not observable against a corpus with no card that cares which SBA clause killed which
    creature.

    **CR 603's own `Mode$ DamageDoneOnce` is real now too** (`checkDamageDoneOnceTriggers`, trigger.go, ported from
    `TriggerDamageDoneOnce.performTest`) — `Mode$ DamageDone`'s own batched sibling, the corpus's own single largest
    remaining trigger mode at 206 real lines. A new `damageTable` (`[]damageEntry`, trigger.go — `CardDamageTable`'s own
    port, declared alongside the dispatch that consumes it rather than in combatdamage.go where it is built, to avoid an
    enginelint dependency cycle: `combatdamage` already depends on `trigger` to call
    `checkDamageDoneTriggersToCard`/`ToPlayer`) accumulates every `(source, target, amount)` triple a single
    damage-dealing action actually deals — after prevention/replacement, never the raw pre-reduction number — and is
    consumed once, rather than checked per exchange the way `Mode$ DamageDone` already is. CR 510.2's own "all combat
    damage is dealt simultaneously" is the reason: a gang-blocked attacker's own trigger has to see every blocker's
    damage combined into one firing, not one firing per blocker, and several unblocked attackers hitting the same player
    combine the identical way.

    `dealPermanentDamage`/`dealPlayerDamage` (combatdamage.go) each gained a `table *damageTable` parameter, appending
    the actual dealt amount to it whenever the pointer is non-nil rather than forcing every call site to build one;
    `dealAttackerDamage`/`dealAttackTargetDamage` just thread it through. `dealCombatDamageStep` builds one local table
    per first-strike-or-regular damage sub-step and calls `checkDamageDoneOnceTriggers` once after its own loop
    finishes; `dealDamageEffect` (dealdamageeffect.go) builds its own local table per resolution — more than one entry
    when `Defined$` names several players at once — and does the identical thing, so DealDamage's own damage is one
    batch too, not just a combat step's.

    `checkDamageDoneOnceTriggers` groups the table by target first (GO-12: first-seen order), then checks every watching
    permanent's own trigger once per group: `ValidTarget$` against the target itself (`attackedTargetMatches`,
    `AttackersDeclared`'s own dispatch, reused at its one-element case), `CombatDamage$` against `isCombat`, and the
    summed amount against `DamageAmount$` (`damageAmountMatches`, `DamageDone`'s own dispatch, reused) — the sum itself
    filtered first to only the entries whose own `Source` matches `ValidSource$` when the line names one
    (`damageDoneOnceAmount`, `TriggerDamageDoneOnce.getDamageAmount`'s own dispatch, ported directly). Every target's
    own live state is still current when this runs (called before any state-based action can move a lethally damaged
    creature), so no `g.LKI` lookback is needed the way `checkSacrificedTriggers`'/ `checkChangesZoneAllTriggers`' own
    damage-adjacent siblings need one.

    200 of the corpus's own 206 real `T:Mode$ DamageDoneOnce` lines resolve. Not resolved: `ResolvedLimit$` (2) and
    `ActiveZones$` (2) — neither read by `TriggerDamageDoneOnce.performTest` at all, real meaning on the handful of
    lines naming either unclear; `DamageSource$` (1) — an object reference this port has no resolver for; `FirstTime$`
    (1) — `GameEntity.getAssignedDamage`, a per-target running total across the whole turn this port tracks nowhere. A
    trigger carrying any of these four is skipped entirely, not fired unconditionally (GO-7).

    6 new tests (damagedoneonce_test.go) drive the dispatch through the real combat-damage and cast-and-resolve
    pipelines, `checkDamageDoneOnceTriggers` itself being unexported (TEST-1): a double-blocked attacker firing once for
    its combined damage rather than once per blocker, several unblocked attackers hitting one player firing once for
    their combined total, `ValidSource$` filtering the summed amount before `DamageAmount$` is checked against it, a
    `DealDamage` hitting every player firing once per player rather than merging them into one batch, `CombatDamage$`
    rejecting a non-combat `DealDamage` against a `CombatDamage$ True` line, and `ResolvedLimit$` skipping the whole
    line. Regression-verified by temporarily removing the new call from `dealCombatDamageStep` and confirming the
    double-block test fails exactly as expected, then restoring it.

    **`DamageDealtOnce` and `DamageAll` — two of `DamageDoneOnce`'s own three further real siblings sharing the
    identical `damageTable` — are real now too.** A new `checkDamageTableTriggers` (trigger.go) is what
    `dealCombatDamageStep`/`dealDamageEffect` actually call once per damage-dealing action now, running all three
    dispatches off the one table CardDamageTable's own real Java shape already builds it as (`triggerDamageDoneOnce`,
    CardDamageTable.java, fires all of these off a single table rather than one built per mode).
    `checkDamageDealtOnceTriggers` (`Mode$ DamageDealtOnce`, ported from `TriggerDamageDealtOnce.performTest`) is
    `DamageDoneOnce`'s own mirror image: the table grouped by `Source` instead of `Target` -- a gang-blocked attacker
    splitting its power between two blockers is one source hitting two targets, firing this mode once for the combined
    total rather than once per blocker hit. `ValidSource$` matches the source directly (49 of 49 real lines name it, the
    dominant shape being `Card.Self`); `ValidTarget$`, when present, both filters and sums the group's own entries
    (`damageDealtOnceAmount`, `TriggerDamageDealtOnce.getDamageAmount`'s own dispatch, ported directly,
    `attackedTargetMatches` reused at its one-element case for the mixed card-or-player target shape) and gates the
    whole line on that filtered sum being positive. 47 of the corpus's own 49 real lines resolve; `AtLeastOneInstance$`
    (1) -- "at least one single damage instance meets this comparison," a per-instance rather than a summed-amount check
    this dispatch has no evaluator for -- and `ActivationLimit$` (1) skip the whole line rather than firing
    unconditionally (GO-7).

    `checkDamageAllTriggers` (`Mode$ DamageAll`, ported from `TriggerDamageAll.performTest`) fires once for the whole
    damage-dealing action with no grouping at all, whenever the table -- filtered by `ValidSource$` and `ValidTarget$`
    together on the same entry, when either is named -- still has at least one entry left (`damageAllTableMatches`, new,
    ports `CardDamageTable.filteredMap`'s own emptiness check, short-circuiting on the first surviving entry rather than
    building the filtered table Java's own version returns). 9 of the corpus's own 9 real lines resolve: every param
    this mode carries already has a resolver.

    `DamageDoneOnceByController` -- the table's fourth real sibling in Java, grouping by a target's every damaging
    controller -- names 0 real corpus lines and is built as The Initiative's own trigger (`TakeInitiative`,
    `port-log/game-state/effects-monarch-initiative-venture.md`).

    6 new tests (damagetabletriggers_test.go) drive both dispatches through the real combat-damage and cast-and-resolve
    pipelines, both being unexported (TEST-1): a gang-blocked attacker's own split damage firing `DamageDealtOnce` once
    for the combined total, `ValidTarget$` filtering the summed amount down to zero for an Elf blocker dealt no damage
    while a Goblin blocker dealt 5 in the same action (a real negative control: an unfiltered sum of 5 would wrongly
    fire), `ActivationLimit$` skipping `DamageDealtOnce`, `DamageAll` firing once for a double block's own four separate
    exchanges rather than once per exchange, `DamageAll` respecting `ValidSource$`/`ValidTarget$` (rejecting
    `ValidTarget$ Player` against an all-creature combat), and `DamageAll` firing for a non-combat `DealDamage` too.
    `checkDamageTableTriggers` itself is regression-verified by temporarily narrowing it back to just
    `checkDamageDoneOnceTriggers` and confirming the new positive tests fail exactly as expected, then restoring it.

    **`isETBTrigger`/`isDiesTrigger` (trigger.go) now port `TriggerChangesZone.performTest`'s own
    `Origin$`/`Destination$` semantics exactly, closing two real correctness gaps rather than a hypothetical cleanup.**
    A new `hasZoneOrAny` treats a key that is absent, or present naming the literal value `"Any"`, as no restriction at
    all (Java's own `!hasParam(key)` and `getParam(key).equals("Any")`), falling back to `hasZone`'s own comma-list
    membership check only when the param names something else. `isDiesTrigger`'s own `Destination$` used to require the
    literal value `"Graveyard"`, so 253 real `Destination$ Any` lines and 11 more with no `Destination$` at all — CR
    603.6c's own unqualified "leaves the battlefield" — never fired even on an ordinary death; its `Origin$` used to
    require the literal value `"Battlefield"`, so 31 real lines naming only `Destination$ Graveyard` ("put into a
    graveyard from anywhere") missed the battlefield-origin instance of themselves too — 295 real lines combined, all
    now correctly resolved by the same eight `checkDiesTriggers` call sites (`action.go`) that already existed, no new
    call site needed. `isETBTrigger` gained a real `origin ZoneType` parameter for the identical reason on its own
    `Origin$` side: 21 real lines (12 `Origin$ Graveyard` — a reanimation-flavored "enters from a graveyard" — plus a
    handful of `Hand`/`Stack`/`Exile`/`AttractionDeck`) used to fire unconditionally regardless of where the card
    actually came from, an over-firing bug this port had until `checkETBTriggers`/`otherETBTriggerMatches` threaded
    `origin` through from the `origin := c.Zone` local already computed at each of the three real call sites
    (`permanentEffect`/`attachEffect`, castspell.go; `Game.PlayLand`, land.go) for `checkMovedReplacement`, read before
    `Game.Move` overwrites it. A new `changesZoneResolvable` skips a `Mode$ ChangesZone` line naming
    `ValidCause$`/`NotThisAbility$`/`ConditionYouCastThisTurn$`/`CheckOnTriggeredCard$`/`ExcludedOrigins$`/
    `ExcludedDestinations$` (12 of 7,609 real lines combined) rather than firing unconditionally and guessing wrong
    (PORT-8/GO-7) — `TriggerChangesZone.performTest`'s own remaining params, needing a reference vocabulary or a
    per-turn-cast-count tracker this port has neither of.

    **`checkPhaseTriggers`/`checkAttackersDeclaredTrigger` (trigger.go) each carried their own `hasAnyParam` pre-filter
    naming `IsPresent$`/`PresentCompare$`/`CheckSVar$` alongside their own genuinely-unresolved params -- a leftover
    from before `triggerCommonRequirementsMet`'s general resolution of exactly those params (item 26's own
    `meetsCommonRequirements` paragraph, above) existed, never revisited once it landed and started resolving them for
    every other trigger mode through the identical `triggerEffectAPI` choke point both of these functions already call
    last.** Removing the three keys from each pre-filter was the whole fix, since nothing else needed to change: 686 of
    `Phase`'s own real lines (272 `IsPresent$`, 104 `PresentCompare$`, 310 `CheckSVar$`/`SVarCompare$`) and 31 of
    `AttackersDeclared`'s (14, 4, 13) went from unconditionally skipped, regardless of whether their own condition
    actually held, to correctly evaluated. `Condition$` stays in both pre-filters (65 real `Phase` lines, 1
    `AttackersDeclared` line) -- `SpellAbilityCondition`'s own separate gate on the ability itself, distinct from
    `CheckSVar$`/`SVarCompare$`'s own now-resolved `CardTraitBase` shape, still genuinely unresolved.

    `Attacks`'s own `Alone$` (60 real lines, `attacksOtherCount` counting `Combat.Attackers` other than the declared
    one), `DefendingPlayerPoisoned$`/`AttackDifferentPlayers$` (1 each, `defenderOf`'s own `Counters.Count(Poison)` and
    a new `attacksMultiplePlayers`), `Attacked$` (47, `attackedTargetMatches` — built below for `AttackersDeclared`'s
    own `AttackedTarget$`, reused here at its trivial one-element case, `[]EntityID{g.combat.AttackTargets[attacker]}` —
    against `AbilityKey.Attacked`'s own single `GameEntity`), `FirstAttack$` (4, a new `Card.AttacksThisTurn`,
    `CardDamageHistory.getCreatureAttacksThisTurn`'s own per-card counter, incremented for each declared attacker right
    before `checkAttacksTriggers` runs and reset every cleanup alongside `Damage`/`LandsPlayed`/`CardsDrawnThisTurn`,
    checked as `> 1` immediately after that increment) and `DamageDone`'s own `DamageAmount$` (8, a new
    `damageAmountMatches` reusing `compareOp` (`valid.go`), never `AbilityUtils.calculateAmount` — every real line is a
    plain integer or the literal `TargetToughness`) are all resolved now too. `Phase` itself is real now:
    `checkPhaseTriggers` resolves `Phase$` (every real corpus value bar an unrecognized token, which does not occur),
    the `Main`/`PhaseCount$ 2` alias for "second main phase" (29 real lines), and `ValidPlayer$` through
    `matchesPlayerSpec` (2,001 of 2,065 real lines). `AttackersDeclared` is real now too:
    `checkAttackersDeclaredTrigger` resolves `AttackingPlayer$` (`matchesPlayerSpec` against the active player, CR
    508.1's own attacking player — 175 of 286 real lines), `AttackedTarget$` (a new `attackedTargetMatches`, trying both
    `matchesPlayerSpec` and `Matches` against every entity actually attacked this combat, since a real spec mixes
    player-shaped and card-shaped tokens in the same comma list, `You,Planeswalker.YouCtrl` among them — 63 of 286) and
    `ValidAttackers$`/`ValidAttackersAmount$` (a new `validAttackersCountMatches`, counting how many of
    `Combat.Attackers` `Matches` the spec and comparing via the existing `compareOp` — 123 of 286), reusing
    `phaseTriggerZones`'s own four-zone walk (273 real lines carry `TriggerZones$ Battlefield`, but 7 carry `Command`
    and 5 carry `Graveyard`, the identical minority-but-real split `Phase` already needed the walk for).
    `IsPresent$`/`PresentCompare$` (14, 4) and `CheckSVar$` (13) are resolved now too (above, the `hasAnyParam`
    pre-filter fix); Not resolved: `Condition$` (1 — `StaticAbility.java`'s own runtime gate, no equivalent for any
    trigger mode yet); a qualified `AttackedTarget$` `matchesPlayerSpec` cannot resolve (`Player.EnchantedBy`,
    `Player.hasInitiative`, `Player.IsPoisoned`, `Opponent.lifeGTX` — 12 of 286 combined). `Drawn` is real now too:
    `checkDrawnTriggers` resolves `ValidCard$` against the drawn card (an ordinary `Matches`, needing nothing new — 156
    of 161 real lines), `ValidPlayer$` against the drawing player through the existing `matchesPlayerSpec` (13), and
    `Number$` against a new `Player.CardsDrawnThisTurn` (player.go) — `LandsPlayed`'s own per-turn-counter shape,
    incremented once per card in `DrawCards` (turn.go) the same order Java's own `numDrawnThisTurn++` runs before the
    trigger check, reset every cleanup alongside `LandsPlayed` (79 of 161). Not resolved: `FirstCardInDrawStep$` (5) —
    Java's own separate `numDrawnThisDrawStep`, a narrower per-step counter this port tracks nothing for; `ForReveal$`
    (5) — a reveal-while-drawing flag this port's own `DrawCards` has no equivalent state for.

    **`Mode$ LifeGained` is real now too** (CR 119.1's own "whenever you gain life" trigger,
    `TriggerLifeGained.performTest`) — `checkLifeGainedTriggers` (trigger.go), `gainLifeEffect`'s own real caller
    (below), needs no `ValidCard$` at all (the identical no-object shape `Phase`/`Drawn` already have) so it reuses
    `phaseTriggerZones`'s own four-zone walk outright (95 of 98 real lines name `TriggerZones$ Battlefield`, 2
    `Graveyard`, 1 `Command`) and `matchesPlayerSpec` for `ValidPlayer$` (present on every real line — `You`, 95;
    `Opponent`, 2), matched against the gaining player. `ValidPlayer$`'s absence is treated as no match rather than
    unrestricted, since it is this mode's only dispatch key and 0 real lines omit it. `FirstTime$` (6) resolves too — a
    new pre-increment read of `Player.LifeGainedTimesThisTurn` (player.go), incremented once per `gainLifeEffect`
    resolution, the identical pre-increment-count contract `checkLandPlayedTriggers`'s own `NotFirstLand$` already has
    (item 26). `ActivationLimit$` (4) now skips the whole line too — a real correctness fix, not a new resolution: this
    port never checked the key at all before, so those 4 real lines were firing every single time rather than up to
    their own per-turn/per-game cap, a wrong answer rather than a coverage gap (PORT-8/GO-7). 93 of the corpus's own 98
    real lines resolve now (`OptionalDecider$`, 7, every real line "You", resolves too through `triggerEffectAPI`'s own
    `triggerIsOptional`, item 26's own closing paragraph below). Not resolved: `ValidSource$`/`Spell$` (1 line, both
    named together — matched against the triggering `SpellAbility` itself, the identical ability-kind classifier
    `becomesTargetSourceMatches` has for a different mode, not built here since this one real line stays blocked by
    `Spell$` regardless); `ResolvedLimit$` (1 — `Trigger.getResolvedThisTurn`'s own separate per-trigger resolution
    counter).

    **`Mode$ BecomesTarget` is real now too** (CR 115/603.3's own "whenever ~ becomes the target of a spell or ability,"
    `TriggerBecomesTarget.performTest`) — `checkBecomesTargetTriggers` (trigger.go), called from
    `pushTriggeredAbilities` (trigger.go) right after every `PushAbility` and from `castAura` (castspell.go) for an
    Aura's own cast-time attach target, the two places this port ever finishes choosing a target for something today (a
    targeted Instant/Sorcery is not built yet — `CastSpell` only casts a permanent or an Aura, castspell.go's own doc
    comment). `ValidTarget$` is matched with `attackedTargetMatches` (`AttackersDeclared`'s own dispatch, reused — the
    identical one-entity-of-either-kind problem, since a real target can be a player or a card), and
    `Card.AttachedBy`/`EnchantedBy` (Ice Cage's own "enchanted creature becomes the target of a spell or ability") needs
    no new code at all: `Matches` already reads its own source argument as "the object being checked for being attached
    to." `FirstTime$` (Glyph Keeper's own "for the first time each turn") reads a new `Card.BecameTargetThisTurn`
    (card.go) — a plain bool, since every real `FirstTime$` line only ever asks whether the card has been targeted at
    all this turn, never by whom, unlike Java's own per-player `targetedFromThisTurn` set — set the moment the card is
    targeted regardless of whether any trigger's own `ValidTarget$` matches (Java's own `addTargetFromThisTurn` runs
    before any trigger check), reset every cleanup alongside `Card.AttacksThisTurn` (turn.go). `ValidSource$` (71 of 77
    real lines naming it) resolves too — matched against the triggering ability itself, not a `Card`
    (`AbilityKey.SourceSA` in Java, `SpellAbility.isValid`'s own restriction split) — through
    `becomesTargetSourceMatches` (trigger.go, new), a Spell/Triggered ability-kind classifier built from this dispatch's
    own two real call sites rather than a general kind field on `Ability`: `castAura`'s own Aura is always a Spell, and
    a triggered ability pushed through `pushTriggeredAbilities` is always Java's own `isTrigger()`/ `isAbility()` pair
    (this port has no activated-ability targeting built yet, so `Ability`/`Triggered` collapse to the identical "not a
    Spell" check). `SpellAbility` itself matches unconditionally (Java's own "match anything" case);
    `.YouCtrl`/`.OppCtrl` compare the ability's own controller against the watching trigger's own host controller, the
    identical contract every other YouCtrl/OppCtrl property in this port already has; `.Aura` is trivially true once the
    kind itself is Spell, since this port's only Spell source reaching here IS an Aura being cast. 101 of the corpus's
    own 118 real `Mode$ BecomesTarget` lines resolve now (`OptionalDecider$`, 12, every real line "You" and none also
    naming `Valiant$`/`ActivationLimit$`/`Static$`, resolves too through `triggerEffectAPI`'s own `triggerIsOptional`,
    item 26's own closing paragraph below). Not resolved: `Valiant$` (10) — a separate per-activator "have you not
    targeted this before" set `FirstTime$`'s own plain bool cannot answer; `ActivationLimit$` (3) and `Static$` (1) —
    each its own further mechanic; and 6 of the 77 real `ValidSource$` lines —
    silverfur_partisan.txt's/wild_defiance.txt's own real `Instant,Sorcery` (a card-type check neither of this port's
    two sources can ever satisfy) and four more combining a kind with a property past YouCtrl/OppCtrl/Aura
    (`namedGoblin Artisans`, `numTargets EQ1`, `Land+named...`, `Backup`), each its own further mechanic.

    **`Trigger.phasesCheck` itself is real now too** (`triggerPhasesCheck`, trigger.go) — a general gate every trigger
    mode carries regardless of what it fires on, checked before any mode-specific dispatch runs at all
    (`TriggerHandler.isTriggerActive`, called before `canRunTrigger`/`performTest`), kept as its own function rather
    than folded into `triggerCommonRequirementsMet` since the two port genuinely different Java methods on different
    classes. `Phase$` (19 real lines outside `Mode$ Phase`'s own dispatch) restricts a trigger of any mode to firing
    only during named step(s)/phase(s), reusing `phaseTriggerMatches` (`Mode$ Phase`'s own dispatch function)
    generically — confusingly the identical param key `Mode$ Phase` itself reads for a different reason
    (`TriggerPhase.performTest` checks only `ValidPlayer$`; `Phase$` there is this same general gate applied to that one
    mode). `PlayerTurn$` (61) / `NotPlayerTurn$` (0, ported for symmetry) restrict to (or away from) the host's own
    controller's turn; `OpponentTurn$` (23) collapses to `NotPlayerTurn$`'s own check in this port's no-team model
    (`matchesPlayerBase`'s own doc comment). `FirstCombat$` (6, `Attacks`/`AttackersDeclared`, both already built)
    resolves to a hardcoded `true` — this port has no extra-combat mechanism to ever reach a second combat phase in the
    same turn, the identical reasoning `combatdamage.go`'s own `CombatDamage$` check already uses. Closes 43 real lines
    across six already-built modes (`SpellCast` 12+2, `ChangesZone` 9+11, `LifeGained` 5, `Taps` 2, `Discarded` 1,
    `Drawn` 1) that fired **unconditionally** until now — a wrong answer this port had never checked for, not a coverage
    gap (PORT-8/GO-7; sentinel_tower.txt's own real "deals damage... during your turn" among them) — plus the 6 real
    `FirstCombat$` lines above. Not resolved: `FirstUpkeep$`/`FirstUpkeepThisGame$` (1/2, `Mode$ Phase` only, a per-game
    upkeep-step counter this port tracks nowhere); `TurnCount$` (0 real lines, dormant).

    **`Mode$ Untaps` is real now too** (CR 502.3/603's own "becomes untapped," `TriggerUntaps.performTest`) —
    `checkUntapsTriggers` (trigger.go), `Taps`'s own mirror image, called once per card from `untapStep` (turn.go) for
    every card that actually untaps that step — a card already untapped generates no event at all, `Card.untap()`'s own
    early `if (!tapped) return false` ported as a `wasTapped` guard in `untapStep` itself rather than duplicated inside
    the check function. `TriggerUntaps` never special-cases its own host's trigger, so one battlefield walk covers both
    a card's own "Inspired" trigger (`ValidCard$ Card.Self`, the corpus's own dominant real shape) and
    mesmeric_orb.txt's own bare "whenever a permanent becomes untapped" (`ValidCard$ Card`) alike, the identical
    single-walk shape `checkTapsTriggers` already has for its own mirror event. 30 of the corpus's own 30 real
    `Mode$ Untaps` lines resolve now (`Phase$`/`CheckSVar$` fold in through `triggerPhasesCheck`/
    `triggerCommonRequirementsMet` for free, `Secondary$` is a pure display flag no check ever gates on,
    `OptionalDecider$`, 3, every real line "You", resolves too through `triggerEffectAPI`'s own `triggerIsOptional`,
    item 26's own closing paragraph below).

    **`ReplacementEffect.requirementsCheck` is real now too** (`replacementRequirementsCheck`, replacement.go) — a
    general gate every replacement carries regardless of its own `Event$`, checked before its own shape-specific
    `canReplace`, mirroring `triggerPhasesCheck`'s own role for triggers: `PlayerTurn$` (8 real lines combined across
    `DamageDone`/`Draw`/`CreateToken`/`LifeReduced`/`TurnFaceUp`, every one the literal value `True`) checks
    `isPlayerTurn(hostController)` directly; `ActivePhases$` (1, island_sanctuary.txt's own `Draw` shape) reuses
    `phaseTriggerMatches` (trigger.go) at its own key rather than `Phase$`'s, now that function takes a `key` parameter
    instead of hardcoding `"Phase"`; `triggerCommonRequirementsMet` is then called outright, since Java's own
    `ReplacementEffect.requirementsCheck` ends by calling the identical `meetsCommonRequirements` a `Trigger`'s own
    `performTest` already does. Folded into `damagePreventionMatches`/`untapReplacementMatches`/ `replacementTapsOnMove`
    (each widening its own allow-list to admit the newly-resolved keys), closing 7 of 10 previously-skipped real
    `DamageDone`|`Prevent$` lines (`PlayerTurn$` 4, `CheckSVar$`/`SVarCompare$` 2, `IsPresent$` 1 —
    guardian_naga_banishing_coils.txt's own real "can't be dealt damage during your turn" among them) and 5 of 7
    previously-skipped `Untap`|`CantHappen` lines (`IsPresent$` 4, `CheckSVar$`/`SVarCompare$` 1) for free, plus fixing
    a real, if narrow, wrong-firing bug: archelos_lagoon_mystic.txt's own "enters tapped" toggle names
    `IsPresent$ Card.Self+tapped`/`+untapped` restricting its own two replacement lines to only apply while Archelos
    itself is tapped/untapped — unchecked before this, `replacementTapsOnMove` carried no allow-list at all to skip on
    instead of guessing, so the "enters tapped" half matched regardless of Archelos's own state.

    Two new consumers reuse the same general gate directly: **`drawPrevented`/`gainLifePrevented`** (replacement.go)
    resolve CR 120.3's/119's own `Prevent$ True` shape for `Draw`/`GainLife` — 2 of the corpus's own 39 real `Draw`
    lines (possessed_portal.txt's own bare `ValidPlayer$ Player | Prevent$ True`; living_conundrum.txt's own
    `IsPresent$ Card.YouOwn | PresentZone$ Library | PresentCompare$ EQ0`-qualified "while your library has no cards")
    and 1 of 21 real `GainLife` lines (sulfuric_vortex.txt's own bare form) resolve end to end. `drawPrevented` is
    checked from `DrawCards` (turn.go) before the empty-library check runs at all — Java's own `Player.doDraw` checks
    its `Event$ Draw` replacement before ever looking at whether the library is empty, so a prevented draw cannot also
    trigger CR 704.5b's own "attempted to draw from an empty library" loss. `gainLifePrevented` is checked from
    `gainLifeEffect` (gainlifeeffect.go) per player before `Player.Life` is touched at all. The other 36 real `Draw`
    lines and 20 real `GainLife` lines name `ReplaceWith$` instead of `Prevent$` — a real substitution, 7 of the 36 and
    4 of the 20 resolved by `drawReplaced`/`gainLifeReplaced` below.

    `matchesPlayerProperty` (valid.go) resolves two more real `Phase`-mode qualified `ValidPlayer$` forms now, both
    reused for free by every one of its nine existing callers across `trigger.go`/`continuous.go`/`targeting.go`/
    `replacement.go` (each now threading the ability's own host card through as a `source CardID` parameter alongside
    the controller `matchesPlayerSpec` already took): `EnchantedController` (34 lines, righteous_authority.txt's own "at
    the beginning of the draw step of enchanted creature's controller" shape) reads `source.AttachedTo()` — the same
    attachment link Layer 2's own `GainControl$` already reads (item 27) — to find the controller of whatever the
    trigger's own host card enchants; `descended` (10, ruin_lurker_bat.txt's own "if you descended this turn" shape,
    CR's own descend mechanic) reads a new `Player.DescendedThisTurn` (player.go), set in `Game.Move` (game.go) whenever
    a permanent, non-token card moves into a graveyard from any zone — this port has no token-creation effect yet (M6),
    so the token half of Java's own check holds by construction for every card this port can ever move — and reset for
    every player at `cleanupStep` (turn.go) the identical way `LandsPlayed`/`CardsDrawnThisTurn` already are.

    **CR 616's own "the event is replaced by a different one" is real now too, for `Draw`** — `drawReplaced`
    (replacement.go) recognizes a `ReplaceWith$` target naming a plain `DB$ Draw | Defined$ You | NumCards$ N` or
    `DB$ PutCounter | CounterType$ X | CounterNum$ N | Defined$ Self`, run by hand rather than through `drawEffect`/
    `putCounterEffect` — both need a `*Registry` to chain a `SubAbility$` that `DrawCards`' own call chain has no way to
    reach, so a target ability naming one is refused outright rather than run with the chained half silently dropped
    (GO-7). `DrawCards`' own per-card loop body is now a shared primitive, `drawOneCard`, the replacement's own
    substitute draws call directly rather than recursing back through `DrawCards`/`drawPrevented`/`drawReplaced` itself
    — Java's own `ReplacementHandler` guards a replacement effect against reapplying to an event its own resolution
    produced (its `hasRun` set), a per-line recursion guard this port does not build, so reusing the unguarded primitive
    instead sidesteps needing one, at the cost of a real, narrow simplification: the replacement's own draws are not
    themselves checked against any other replacement or prevention effect on the battlefield either — not observable
    against a corpus with no two Draw-replacing permanents on one battlefield today, but not full CR 616 either. 7 of
    the corpus's own 36 real `Event$ Draw | ReplaceWith$` lines resolve end to end: thought_reflection.txt's own bare
    "draw two cards instead," phial_of_galadriel.txt's own `Hellbent$ True`-qualified identical shape,
    ormos_archive_keeper.txt's own `IsPresent$`-qualified line whose own target is `PutCounter` rather than `Draw`, and
    teferis_ageless_insight.txt's/alhammarrets_archive.txt's/bard_king_of_dale.txt's own real "except the first one you
    draw in each of your draw steps, draw two cards instead" (`NotFirstCardInDrawStep$ True`, resolved through a new
    `notFirstCardInDrawStepExempts`/`Player.DrawnThisDrawStep` pair — reset every Draw step, incremented per draw while
    the phase is Draw). notion_thief.txt's own real "except the first one they draw ..., instead you draw a card"
    (`ValidPlayer$ Opponent`) resolves through the identical gate, needing `applyDrawReplacementDraw`'s own
    `Defined$ You` reading corrected from the event's own affected player to the replacement's host controller instead —
    every previously-resolved line's own `ValidPlayer$` happened to be `You` too, so the two had never needed telling
    apart before. Not resolved: reed_richards_smartest_man.txt's own `FirstExtraCardDrawnThisTurn$`; hullbreacher.txt's
    own identical `NotFirstCardInDrawStep$` shape, whose own target is `DB$ Token` rather than `Draw`/`PutCounter`
    (`CreateToken` is not a built `Effect` yet); 4 naming `Defined$ ReplacedPlayer`, a token `definedPlayers` has no
    case for; 1 whose own target chains a further `SubAbility$`; and the 2 already-documented gaps
    (`Player.Chosen`/`Optional$`).

    **`GainLife` gets the same dispatch too now** — `gainLifeReplaced` (replacement.go) is `drawReplaced`'s own sibling,
    but carries both of CR 616's own outcomes rather than just "Replaced," the way `damageReplaced` already does for a
    different `Event$`: a full substitution (`applyGainLifeReplacement`, reporting a gain of 0 the identical way
    `applyDamageReplaceCounter`'s own full substitution already does) or a resized gain (`applyGainLifeReplaceEffect`,
    below, returning the new amount, still granted through the normal path — `gainLifeEffect.Resolve`'s own
    `if gain <= 0 { continue }` folds Java's own `Player.gainLife`, pre- and post-replacement `lifeGain <= 0` checks
    into the one this port's call ordering needs). `ReplaceCount$LifeGained`, "the amount of life that would have been
    gained," reads straight off the raw `LifeAmount$` `gainLifeEffect.Resolve` already has in scope via
    `resolveGainLifeReplacementAmount` (`resolveNamedAmount`'s own sibling for a bare, operator-less `Expression` head)
    for `Draw`'s own `NumCards$`/`LoseLife`'s own `LifeAmount$`, and through the newly generalized
    `resolveReplaceCountAmount` (renamed from `resolveDamageReplaceCountAmount`, item 26's own `DB$ ReplaceEffect`
    paragraph above, read against `"LifeGained"` instead of `"DamageAmount"`) for the operator-carrying
    `DB$ ReplaceEffect` shape, below. 19 of the corpus's own 20 real `Event$ GainLife | ReplaceWith$` lines resolve end
    to end now: lich.txt's/nefarious_lich.txt's own "draw that many cards instead" (`ValidPlayer$ You`, target
    `DB$ Draw | Defined$ You | NumCards$` naming that SVar) and tainted_remedy.txt's/plague_drone.txt's own "that player
    loses that much life instead" (`ValidPlayer$ Opponent`, target `DB$ LoseLife | LifeAmount$` naming it |
    `Defined$ ReplacedPlayer` — read as the replaced player directly), plus 15 more real lines targeting
    `DB$ ReplaceEffect | VarName$ LifeGained | VarValue$ ...` (`applyGainLifeReplaceEffect`) —
    rhox_faithmender.txt's/the_wind_crystal.txt's/selenia_the_cursed_heart.txt's/
    alhammarrets_archive.txt's/doctor_strange_surgeon.txt's/boon_reflection.txt's/phial_of_galadriel.txt's own real
    "gain twice that much life instead" (`Twice`) and angel_of_vitality.txt's/heron_of_hope.txt's/honor_troll.txt's/
    cleric_class.txt's/bilbo_birthday_celebrant.txt's/knight_of_dawns_light.txt's/leyline_of_hope.txt's/
    pest_rescuer.txt's own real "gain that much life plus 1 instead" (`Plus.1`). Not resolved: rain_of_gore.txt's own
    real `ValidSource$ SpellAbility | SourceController$ True` restriction (no `ValidPlayer$` at all — a restriction on
    what caused the event, not who it affects).

    **`Mode$ AttackersDeclaredOneTarget` is real now too** — `checkAttackersDeclaredOneTargetTrigger` (trigger.go) is
    `checkAttackersDeclaredTrigger`'s own sibling: `TriggerType.java`'s own
    `AttackersDeclaredOneTarget(TriggerAttackersDeclared.class)` names the identical Java `Trigger` subclass the plain
    `AttackersDeclared` mode already ports, fired at a different granularity by `PhaseHandler.java`'s own
    `declareAttackersStep` — once per defender that has at least one attacker (`Attackers`/`AttackedTarget` narrowed to
    just that one defender), rather than once per combat with every attacker/every attacked defender gathered. A new
    `attackersDeclaredParamsMatch` factors out the shared param dispatch
    (`Condition$`/`AttackingPlayer$`/`AttackedTarget$`/`ValidAttackers$`) both modes now call, and
    `validAttackersCountMatches` takes the attacker subset as a parameter instead of always reading
    `g.combat.Attackers`, so `ValidAttackers$`/`ValidAttackersAmount$` count only the firing's own defender's own
    attackers under this new mode — `attackersTargeting` (new, `combat.getAttackersOf(ge)`) computes that subset in
    `Combat.Attackers`' own declaration order, the identical order `attackedTargetsOf` (item 26's own
    `AttackersDeclared` paragraph above) already walks to find the defender itself. `DeclareCombatAttackers` (attack.go)
    calls this before the plain `checkAttackersDeclaredTrigger`, `PhaseHandler.java`'s own call order. 35 of the
    corpus's own 35 real `Mode$ AttackersDeclaredOneTarget` lines resolve end to end: every real line's own param
    vocabulary (`TriggerZones$`/`AttackedTarget$`/`ValidAttackers$`/`ValidAttackersAmount$`/`AttackingPlayer$`/
    `Secondary$`) is already resolved by the shared dispatch — 0 real lines name `Condition$`/`OptionalDecider$`/
    `CheckDefinedPlayer$`/`IsPresent$`, the params that stay unresolved for the plain `AttackersDeclared` mode's own
    remainder.

    **CR 603.3d's own "may" triggered ability is real now too** — `Ability` (ability.go) gained an `Optional bool`
    field, true only for `OptionalDecider$ You`, resolved by a new `triggerIsOptional` (trigger.go) folded into
    `triggerEffectAPI`'s own shared gate every one of its twenty-eight call sites already runs through.
    `Registry.Resolve` (effect.go) asks a new `PlayerController.ConfirmOptionalTrigger` (its twenty-fifth method) before
    dispatching to the effect or chaining its own `SubAbility$` at all — `WrappedAbility.resolve()`'s own
    `decider.getController().confirmTrigger(this)`, checked right before its own `playSpellAbilityNoStack` call, ported
    directly: a decline skips the whole ability, chain included, the identical early return. 1,506 of the corpus's own
    1,584 real `OptionalDecider$` lines (95%) name "You" — the ability's own `Controller`, already in scope everywhere
    this is checked, needing no new decider-resolution machinery for the dominant shape; every other real value
    (`TriggeredCardController`, 43; `True`, 11; `TriggeredSourceController`, 5; a dozen more, 1-4 real lines each) skips
    the whole trigger line rather than confirming against the wrong player or firing unconditionally (GO-7) — this
    port's own `ConfirmOptionalTrigger` has nobody correct to ask for those yet. Resolved for free across three
    already-built modes simply by reaching this shared gate for the first time: `Mode$ Untaps`'s own remaining 3 real
    lines (30 of 30 now, item 26's own paragraph above), `Mode$ LifeGained`'s own 7 (93 of 98), `Mode$ BecomesTarget`'s
    own 12 (101 of 118) — and a real correctness fix for `Mode$ LandPlayed`'s own 3 (38 of 42), which
    `checkLandPlayedTriggers`'s own `hasAnyParam` never named at all, so those 3 real lines were firing unconditionally
    before this, not merely unresolved. 83 more real lines name `OptionalDecider$` on a sub-ability's own SVar body
    rather than a `T:` line — a chained `SubAbility$`'s own independent "may" — a smaller, separate gap this change does
    not reach, since `resolveSubAbility` (subability.go) builds its own child `Ability` with no `Optional` field set.

    Still missing: every trigger mode but "enters"/"dies"/"attacks"/"blocks"/ "deals damage"/"is discarded"/"becomes
    tapped"/"becomes untapped"/"taps for mana"/"casts a spell"/"beginning of a step or phase"/"a player attacks"/"a
    player draws a card"/"gains life"/"becomes the target of a spell or ability" (`Countered`, `Exiled`, `Sacrificed`,
    ...); `Phase`'s own `Condition$` (a general conditional-trigger evaluator no mode has — 0 real `Mode$ Phase` lines
    carry the bare key today, unlike `WerewolfTransformCondition$`/`WerewolfUntransformCondition$`'s own unrelated 65,
    which this key was never meant to catch) and the two whole-table comparisons
    (`APlayerHasMoreLifeThanEachOther$`/`APlayerHasMostCardsInHand$`, 2 and 1 real lines), plus its own remaining
    qualified `ValidPlayer$` forms (`Player.EnchantedBy`/`Player.Chosen`/`Opponent.EnchantedBy`/`Player.isMonarch`, 14,
    3, 2 and 1 real lines — each needing its own separate mechanic this port does not have: an Aura enchanting a player
    directly, a chosen-player memory slot, a monarch tracker); `DamageDone`'s own
    `ValidCause$`/`TargetRelativeToCause$`/`TargetRelativeToSource$` (its own qualified
    `ValidTarget$ Player.Opponent`/`Player.Other` are resolved now, `Player.EnchantedBy` is not); `Discarded`'s own
    `ValidCause$`; `Taps`'s own `FirstTime$`/`Teamwork$`; `TapsForMana`'s own `Produced$` (its own qualified
    `Activator$ Player.NonActive` is resolved now); `SpellCast`'s own `Player.EnchantedBy`/ `Player.Chosen` qualified
    `ValidActivatingPlayer$` forms (6 of the original 25 lines) and nine other unresolved params (`ValidSA`,
    `TargetsValid`, `HasXManaCost`, ... — `porting/port-log/game-state.md`'s trigger-firing section has the full list);
    simultaneous-trigger ordering (`addSimultaneousStackEntry`, CR 603.3b's own controller-chosen/APNAP order) is
    resolved now — `pushTriggeredAbilities`/`playersInAPNAPOrder` (`trigger.go`) push each player's own group of matches
    in APNAP order rather than a fixed one, every trigger-check function's own "own" and "other" halves collecting into
    one `[]Ability` first; a single player's own multiple matches still stay in the deterministic order they were found,
    since this port has no `PlayerController` hook for a real player choice among them (`orderAndPlaySimultaneousSa`,
    `MagicStack.java`).

    **CR 614's own replacement-effect system has its first real content now**, the corpus's single largest real shape:
    `checkMovedReplacement` (`replacement.go`) ports `ReplacementHandler`/`ReplaceMoved`/`ReplacementEffect.java`,
    trimmed to CR 614.1's "enters the battlefield already tapped" — `Event$ Moved` naming `ReplaceWith$` pointing at a
    bare `DB$ Tap` (`Defined$ Self` or `Defined$ ReplacedCard`, Java's own distinction between the replacement's host
    and the card actually moving, identical here since this file only ever reaches the moving card either way), 618 of
    the corpus's 969 real `Event$ Moved` lines (587 `Card.Self`-shaped, 31 watching another permanent enter — a static
    "creatures your opponents control enter tapped" effect) and 618 of 2,210 real replacement lines corpus-wide (28%).
    Checked against two sets of `Face.Replacements` (M3's own compiled field, never read by the engine before now,
    compiled the identical way `Face.Triggers`/`Face.Statics` already are — `ReplaceWith$` is one of `subAbilityKeys`,
    so it resolves to `Ability.Subs` for free, no new compiler work needed): the moved card's own, and every OTHER
    battlefield permanent's, the identical own/other split `checkETBTriggers`/`otherETBTriggerMatches` already
    established. `Destination$`/`Origin$`, present on 624 and 2 of the real ETBTapped-named lines respectively, are
    optional restrictions (absence means unrestricted, `ReplaceMoved.java`'s own `hasParam` guard), checked against the
    actual move — `origin` threaded in from each of the three real "enters the battlefield" call sites
    (`permanentEffect`/`attachEffect`, castspell.go; `Game.PlayLand`, land.go), read off the card's own `Zone` field
    before `Game.Move` changes it. Called before `checkETBTriggers`: a replacement changes the event itself, so a
    tapped-on-entry permanent must already be tapped by the time a "when this enters" trigger looks at it, CR 614.1's
    own ordering over CR 603. Unlike a trigger match, no APNAP ordering or collect-then-push step is needed: the one
    outcome this file produces, `Tapped = true`, is idempotent, so CR 616's own "more than one replacement effect could
    apply, the affected player chooses" procedure — needing a `PlayerController` hook this port does not have, the
    identical gap a single player's own multiple simultaneous triggers already has (above) — has no observable answer to
    get wrong here: the first match found in either loop is applied and the search stops.

    **`LandTapped`'s own 140 real `DB$ Tap` lines carrying a Condition-family param are resolved now too** — Rootbound
    Crag's own checkland text, "enters tapped unless you control a Mountain or a Forest." A new `subAbilityConditionMet`
    (`condition.go`) ports `SpellAbilityCondition.areMet`'s own gate, trimmed to the two shapes these lines actually
    use: `ConditionPresent$`/`ConditionCompare$` (106/103 of the 140, a zone-presence count) and `ConditionCheckSVar$`/
    `ConditionSVarCompare$` (34/33, a named-SVar comparison) — the identical shapes
    `CardTraitBase.meetsCommonRequirements` already resolves for a trigger (`isPresentMatches`/`checkSVarMatches`, item
    26's own paragraph above), reused here under `SpellAbilityCondition`'s own different key names rather than
    reimplemented (`checkSVarMatches` gained `checkKey`/`compareKey`/`secondKey` parameters once this became its second
    caller). `tapAbilityResolvesTap` (replacement.go, renamed from `tapAbilityIsPlainTap`) now reports whether a
    `ReplaceWith$` shape is recognized AND whether it actually taps, rather than one collapsed bool. Not resolved:
    `ETBTapped`/`LandTapped` naming a `SubAbility$` chain (15 of 624 real `ETBTapped` lines — a chained counter grant;
    `resolveSubAbility`'s own chaining mechanism, "SubAbility chaining itself landed," item 26's own paragraph above, is
    specific to a stack-resolving `Ability` — a replacement effect applies inline, outside `Registry.Resolve` entirely,
    so it would need its own separate integration this port does not have), `ConditionDefined$` (7 — an arbitrary
    reference, no Defined$-to-objects
    resolver exists), or `ConditionPlayerTurn$`/`ConditionPhases$` (2, each its own
    mechanic) — each skips the whole line rather than tapping unconditionally and guessing wrong (PORT-8/GO-7).

    A third Condition-family shape stays unresolved for the identical reason: the plain `Condition$` flag
    (SpellAbilityCondition's own separate Threshold/Metalcraft/... switch) carries zero real `DB$ Tap` lines. Every
    other `Event$` value (`DamageDone`, `Untap`, `Counter`, `Draw`, ... — 1,241 of 2,210 real replacement lines) and
    every other `Moved` shape (`Exile`, a chained `DBTap`/`DBExile` reference) remain gaps.

    **CR 509.2's own "becomes blocked" family is real now too**, the natural extension of `checkBlocksTriggers` (above)
    to the attacker's own side of the same declare-blockers step. `checkAttackerBlockedTriggers` ports
    `TriggerAttackerBlocked.performTest` — `Mode$ AttackerBlocked`, 127 real lines, fires once per attacker that ended
    up with at least one legal blocker, the whole blocker group gathered first (`DeclareCombatBlockers`, block.go, once
    the declaration passed validation, ADR-0024) rather than once per blocker; `ValidCard$` (74 of 127 real lines carry
    neither `ValidBlocker$` nor `ValidBlockerAmount$`, an unqualified "becomes blocked") matches against the attacker
    directly, and a new `validCardsCountMatches` — `validAttackersCountMatches`'s own shape (item 26's own earlier
    `AttackersDeclared` paragraph) generalized past `g.combat.Attackers` to any `[]CardID` — counts how many of the
    blocker group `ValidBlocker$` matches, compared against `ValidBlockerAmount$`'s own `"GE1"`-defaulted
    operator+operand. `checkAttackerBlockedByCreatureTriggers` ports `TriggerAttackerBlockedByCreature.performTest` —
    `Mode$ AttackerBlockedByCreature`, 102 real lines, `checkBlocksTriggers`'s own exact mirror image: `ValidCard$`
    against the attacker, `ValidBlocker$` against one blocker, both single-card `Matches` calls rather than a counted
    group, fired once per declared `Block` the identical per-pair granularity `checkBlocksTriggers` already has, for the
    identical reason (`DeclareCombatBlockers` has no wider grouping at the point either already runs). Neither mode
    needs a separate own/other loop: like every other trigger class this port has read so far but `Discarded`,
    `TriggerAttackerBlocked`/`TriggerAttackerBlockedByCreature` never special-case the attacker's own trigger, so one
    walk over the battlefield already covers "this creature becomes blocked" and "a creature you control becomes
    blocked" alike. Not resolved: `ValidCard$`/`ValidBlocker$` naming `LessPowerThanBlocker`/`LessPowerThanAttacker` (1
    real line each) — a hardcoded power comparison rather than a valid-string, Skulk's own hardcoded-`X` shape
    (`skulkBlocks`, staticability.go) for a different pairing; explicitly refused rather than left to a bare-word
    valid-string parse that would silently match no card and never fire, for a reason unrelated to the actual gap.
    `Mode$ AttackerBlockedOnce` (3 real lines, a once-per-turn variant neither Java class above is) is not built at all.

    **`CardTraitBase.meetsCommonRequirements` — the one gate Java checks before ANY trigger mode's own `performTest`
    runs — is real now too.** Every check-triggers function in this file used to ignore it entirely: a real card naming
    `IsPresent$`/`CheckSVar$`/etc alongside an already-resolved mode (`Mode$ ChangesZone`, `Attacks`, whatever) fired
    unconditionally, the gate silently never checked. A corpus tally directly against `T:` lines (some of these param
    names are shared with a different switch on `S:`/`A:` lines — `StaticAbility.checkConditions`'s own `Condition$`,
    item 27's own paragraph; `SpellAbilityCondition`'s own `Condition$`/`ConditionPresent$` — neither this gate's
    concern) puts it at ~1,271 real lines. `triggerCommonRequirementsMet` (trigger.go) is called from inside
    `triggerEffectAPI` itself rather than duplicated at each of the eighteen check-triggers call sites, since every one
    of them already funnels through that one function to turn a match into a pushed `Ability` — `triggerEffectAPI`
    gained `g`/`host`/`amounts` params for it, threading `face.Amounts` through from the identical loop every caller
    already has it in.

    Resolved (1,148 of ~1,271): `IsPresent$`/`PresentCompare$`/`PresentZone$`/`PresentPlayer$` and the identical
    `IsPresent2$` pair (624) — `isPresentMatches` ports the zone-scan branch (`PresentZone$` a comma list defaulting to
    Battlefield, `ZoneByName` per entry; `PresentPlayer$` "You" — host's own controller only — or the corpus's own
    default "Any" — every player, Java's own three additive You/Opponent/Allies blocks collapsed to the one partition a
    single-valued param actually produces); `PresentDefined$` (40 of 624) skips, no Defined$-to-cards resolver for an
    arbitrary reference existing yet.

    `CheckSVar$`/`SVarCompare$` (474) — `checkSVarMatches`, both sides resolved through a new `resolveNamedAmount`
    (amount.go) — `ptParam`'s own literal-or-named-SVar shape (continuous.go), factored out once this needed the
    identical resolution against a `*Card` rather than one specific `*compile.Ability` param; `ptParam` itself is now a
    two-line wrapper over it. A line also naming `CheckSecondSVar$` (0 real `T:` lines today) skips: Java ORs a second
    check against the first and nothing forces guessing at that shape blind.

    `Metalcraft$`/`Delirium$`/`Threshold$`/`Hellbent$`/`FatefulHour$` as a `True`/`False` flag (38) — `boolFlagMatches`,
    reusing `continuousConditionMet`'s own underlying predicates (item 27's own `Condition$` paragraph) — the identical
    player-state question, asked as a flag rather than as the whole condition.
    `battlefieldArtifactCount`/`graveyardCoreTypeCount` moved out of continuous.go into a new `playerstate.go`: once
    trigger.go needed them too, leaving them in continuous.go would have made `continuous`→`trigger` (for
    `resolveNamedAmount`) and `trigger`→`continuous` (for these two) a real dependency cycle, `tools/enginelint`'s own
    acyclic-parts rule catching it immediately.

    `LifeTotal$`/`LifeAmount$` (12) — `lifeTotalMatches`, `"You"` (host's own controller) and `"ActivePlayer"`
    (`Game.ActivePlayer()`), the only two real `T:` values; `OpponentSmallest`/`OpponentGreatest` carry none and are not
    resolved.

    Not resolved, each skipped whole rather than treated as met (GO-7): `Revolt$` (25, no
    `Game.leftBattlefieldThisTurn`-equivalent tracked); `WerewolfTransformCondition$`/`WerewolfUntransformCondition$`
    (65, Innistrad's own day/night mechanic, a "spells cast last turn" list this port tracks nowhere);
    `CheckDefinedPlayer$` (20, every real line qualifies it with `isMonarch`, `hasInitiative`, `withMostLife` or
    `withMostType` — mechanics this port has none of, not a shape a general Defined$-to-players resolver could close on
    its own).

    `ManaSpent$`/`ManaNotSpent$` (8, no paying-colors-by-cast tracked); `Adamant$` (1); `Bloodthirst$`, `Monarch$`,
    `EnduringStory$`, `DayTime$` and `ClassLevel$` (0 real `T:` lines each, dormant).

    **CR 614's replacement-effect system has two more real slices now: "doesn't untap" and "prevent all of this damage,"
    both a `Layer$`/`Prevent$` flag rather than a `ReplaceWith$` sub-ability, so neither needed CR 616's own "more than
    one applies" choice built at all — the outcome either produces (blocked, prevented) is idempotent the identical
    reason `checkMovedReplacement`'s own doc comment already gives.** `untapBlocked` (`replacement.go`) ports
    `Card.canUntap`'s own `cantHappenCheck`/`ReplaceUntap.canReplace` — CR 502.3/614.17, `Event$ Untap` naming
    `Layer$ CantHappen` — 149 of the corpus's 158 real `Event$ Untap` lines (156 name `Layer$ CantHappen` at all),
    called from `untapStep` (turn.go) before clearing `Tapped`, summoning sickness clearing regardless since a
    doesn't-untap effect restricts only the untapping action, not CR 302.6's own continuous-control question.
    `ValidStepTurnToController$` (154 of 156, always `"You"`) is not checked at all: `untapStep`'s own loop only ever
    considers cards `g.activePlayer` already controls, so "the untapping player is this card's own controller" already
    holds by construction for every real value the param carries — Java's own `Untap.doUntap` has the identical
    invariant for its own "self" untap pass, the only one this port models (its own "untap a card you don't control"
    branch, `StaticAbilityUntapOtherPlayer`, is not built, no card grants that permission yet).
    `IsPresent$`/`SVarCompare$`/`CheckSVar$`/`EnduringStory$`/`AddSVar$` (7 of 156) skip the whole line rather than
    blocking unconditionally (PORT-8/GO-7); the other 2 of 158 name `ReplaceWith$` instead, a genuine substitution not
    built — skipped the identical way an unresolved `ReplaceWith$` shape already is in `checkMovedReplacement`, the
    plain untap proceeding rather than being blocked defensively.

    `damagePrevented`/`damagePreventedPlayer` (`replacement.go`) port `ReplaceDamage.canReplace`'s own resolvable half
    plus `ReplacementHandler`'s own `Prevent$ True` dispatch (`ReplacementResult.Prevented`, nothing replaces the event,
    it simply does not happen) — CR 614, `Event$ DamageDone` naming `Prevent$ True` — 63 of the corpus's 218 real
    `Event$ DamageDone` lines (72 name `Prevent$ True` at all), called from `dealPermanentDamage`/ `dealPlayerDamage`
    (combatdamage.go) before marking any damage, emitting `DamageDealt`, or checking CR 603's own trigger — a prevented
    instance never happened, the identical "look at the event before it happens" ordering CR 614.1 already has over CR
    603 for `checkMovedReplacement`. `ValidTarget$`/`ValidSource$` are checked the identical way `damageDoneMatches`'s
    own pair already is (item 26), split into a `*Card`/`*Player` pair for the reason
    `checkDamageDoneTriggersToCard`/`ToPlayer` already are. `PlayerTurn$`/`SVarCompare$`/`IsPresent$`/`CheckSVar$`
    resolve generically through `replacementRequirementsCheck` and `DamageAmount$` resolves too now, reusing
    `damageAmountMatches` (item 26) against the original amount about to be dealt; `ValidCause$`/`RelativeToSource$`/
    `CauseIsSource$` (2 of 72, one line naming the first and third together, the other the second alone) still skip the
    whole line. The other 146 of 218 name `ReplaceWith$` instead — most a real sub-ability substitution
    (`Mill`/`ChangeZone`/`Dig`/... — no single shape anywhere near `Moved`'s own 618-line concentration, not built), but
    three do: CR 616's own "Updated" outcome (the event still happens, with a different number) is real too, for both a
    flat reduction and a computed replacement: `damageReplaced`/`damageReplacedPlayer` (`replacement.go`) resolve 18 of
    the 27 real `DB$ ReplaceDamage | Amount$ N` lines this file's own `face.Replacements` walk can even reach ("prevent
    N of that damage," `ReplaceDamageEffect.resolve`'s own two-outcome half this dispatch can compute by hand —
    `applyDrawReplacement`'s own "recognize the one shape" precedent applied to a third `Event$` — without a `*Registry`
    neither call site can reach) and, through the identical two callers, 56 of the 59 real
    `DB$ ReplaceEffect | VarName$ DamageAmount | VarValue$ ...` lines it can reach too (`ReplaceEffect.resolve`'s own
    default "amount" `VarType$` branch — a flat integer, or a named SVar naming `ReplaceCount$DamageAmount/<op>` and one
    of `AbilityUtils.doXMath`'s own `Twice`/`Thrice`/`HalfDown`/`Plus`/`Minus` branches,
    `resolveReplaceCountAmount`/`applyDamageReplaceEffect` — a doubling/tripling/halving/plus/minus rather than
    `ReplaceDamage`'s own flat "prevent N," plus a flat integer `VarValue$` gated by the R: line's own `DamageAmount$`
    threshold the identical way `damagePreventionMatches`'s own new fold-in reads it too). A named-SVar `Amount$`
    (`ShieldAmount`/`X`/`PaidAmount`/`AlchemicX`, 9 lines, each its own further mechanic — a depleting shield counter,
    an X spent on the spell, mana paid), one also chaining its own `SubAbility$` (the identical chained-target refusal
    already given above), stay unresolved. The other 2 of the 27 —
    reidane_god_of_the_worthy_valkmira_protectors_shield.txt's/plated_pegasus.txt's own real
    `ValidTarget$ You,Permanent.YouCtrl`/`Permanent,Player` — resolve too now: `matchesPlayerSpec` (`valid.go`) splits a
    spec on comma the way `valid.Parse` already does for a `*Card`, an alternative whose base is not
    `You`/`Opponent`/`Player` matching nothing rather than aborting the whole spec (`valid.Parse`'s own contract for an
    unrecognized base) — the fix that lets the `You` alternative resolve against a player target even though its own
    `Permanent.YouCtrl` sibling never can. The identical fix closes a real correctness bug in `ReplaceEffect`'s own
    resolved count too: gratuitous_violence.txt's own `ValidTarget$ Permanent,Player` (already counted among the 56
    above, since its card-target half already worked) used to silently skip doubling a hit dealt to a player at all; it
    doubles both now. Of `ReplaceEffect`'s own 59 reachable `VarName$ DamageAmount` lines, 3 stay unresolved — an
    unresolvable `Plus` operand (`Count$CardCounters.FIRE`/`Count$CardPower`) or a bare `Count$CardPower` `VarValue$`,
    each an amount head this port has no evaluator for — and 12 more real `DB$ ReplaceEffect` lines name
    `VarName$ Affected`/`LifeGained`/`Number`/`Ignore` instead of `DamageAmount` — an entirely different substitution,
    not resolved by anything here. Of the corpus's own 39 real `DB$ ReplaceDamage` SVar definitions, the other 12 are
    never named by any literal top-level `R:` line at all: `hedron_field_purists.txt`'s own 2 are referenced only
    through a Layer 6 `AddReplacementEffect$` on a Level-up `Mode$ Continuous` line, and 10 more are created dynamically
    at resolution time by `DB$ Effect`'s own `ReplacementEffects$` param (CR 611.2c) — neither mechanism this port's own
    script-effect dispatch builds, so `face.Replacements` never discovers them regardless of this dispatch's own shape.

    A third real shape resolves too now — `DB$ RemoveCounter`/`DB$ PutCounter` (`applyDamageReplaceCounter`,
    `replacement.go`) — CR 616's own "Replaced" outcome this time, not "Updated": the damage does not happen at all, a
    counter changes on some object instead (`ReplacementHandler.java`'s own default `ReplacementResult.Replaced`, every
    `ApiType` past `ReplaceDamage`/`ReplaceSplitDamage`/`ReplaceEffect`/`ReplaceToken`/`ReplaceMana`). `Defined$`
    resolves three ways: `Self` (every "Phantom" creature's own real "prevent that damage, remove a +1/+1 counter"),
    `Equipped` (panther_habit.txt's own real line, `Card.AttachedTo()` reused), and `ReplacedTarget` (the damaged object
    itself, threaded straight through from `damageReplaced`'s/`damageReplacedPlayer`'s own `target` parameter —
    soul_scar_mage.txt's own "put -1/-1 counters on that creature instead" among them). `CounterNum$` resolves through
    `resolveReplaceCountAmount` (above), now generalized to accept a bare, operator-less `ReplaceCount$DamageAmount` too
    — `doXMath`'s own `operators == null` identity — the dominant real shape for this dispatch specifically. 25 of the
    corpus's own 31 real lines resolve; `SubAbility$` (5, underdark_beholder.txt's own "remove counters, then sacrifice
    if none left" among them) refuses outright, and jared_carthalion_true_heir.txt's own real R: line naming
    `CheckDefinedPlayer$ You.isMonarch` (1, no monarch mechanic this port tracks) is skipped by
    `damageReplacementMatches`'s own allow-list before ever reaching this dispatch.

    All four families share a new `replacementActiveZones`/`hostInActiveZones` (`replacement.go`), generalizing
    `ActiveZones$` past Battlefield alone to the 2 real Command-zone lines each of the shapes carries — the identical
    comma-list zone restriction `checkPhaseTriggers`'s own `TriggerZones$` (item 26) already has, for a replacement's
    own host zone instead of a trigger's.

    **`Mode$ LandPlayed` is real now too** (CR 305/603.5's own "whenever a player plays a land,"
    `TriggerLandPlayed.performTest`) — `checkLandPlayedTriggers` (trigger.go), called from `PlayLand` (land.go) right
    after `checkETBTriggers` — `Player.playLand`'s own real Java ordering (`moveTo`'s own internal ETB firing, then the
    explicit `runTrigger(LandPlayed, ...)` call, then `addLandPlayedThisTurn()`), which is why `PlayLand`'s own
    `LandsPlayed++` now runs last too. `ValidCard$` matched the usual way (`Matches`); `Origin$` resolved through
    `hasZoneOrAny` (ETB triggers' own dispatch, reused) against the land's own origin zone — this port's own `PlayLand`
    only ever moves a card out of Hand (no `MayPlay$` permission to play from elsewhere yet, M6's own remaining
    territory), so 8 of the corpus's 9 real non-`Static$` `Origin$` lines (naming Exile or a Hand-excluding zone list)
    never actually satisfy it today, the identical "mechanically correct, presently unreachable" gap
    `hedron_field_purists.txt`'s own `DB$ ReplaceDamage` lines already have; the 9th, `Origin$ Hand`, fires normally.
    `NotFirstLand$` (1) resolves too: a new pre-increment read of `Player.LandsPlayed` (player.go) — the count of lands
    played strictly before this one, the same value Java's own `performTest` sees since it runs before
    `addLandPlayedThisTurn()` there too. `ValidActivatingPlayer$` (1, "You") resolves through `matchesActivatingPlayer`
    (reused) against the land-playing player. `IsPresent$` (3) resolves generically through `triggerEffectAPI`'s own
    `triggerCommonRequirementsMet` fold-in. 38 of the corpus's own 42 real `T:Mode$ LandPlayed` lines resolve now.
    `Static$`/`ValidSA$` (7 combined — "Once during each of your turns, you may play a historic land..." shapes) skip
    via `hasAnyParam`: `Static$` marks a trigger ability that resolves without going on the stack, a mechanism this
    port's own `pushTriggeredAbilities` does not model, and `ValidSA$` matches a `SpellAbility`, an object `Matches`
    cannot evaluate. `OptionalDecider$` (3, every real line "You") resolves too now, through `triggerEffectAPI`'s own
    `triggerIsOptional` (item 26's own closing paragraph below) — `checkLandPlayedTriggers`'s own `hasAnyParam` never
    named this key at all, so search_the_city.txt's/jokulmorder.txt's/burgeoning.txt's own real "you may..." lines were
    firing unconditionally before this, a real correctness fix (PORT-8/GO-7) rather than only a new resolution.

    **CR's own "unless a cost is paid" is real now too** (`resolveUnlessCost`, effect.go, ported from
    `AbilityUtils.handleUnlessCost`) — a new gate `Registry.Resolve` checks ahead of its own ordinary
    `Effect.Resolve`/`resolveSubAbility` pairing whenever an ability names `UnlessCost$`: each of `UnlessPayer$`'s own
    players (`definedPlayers`, reused; an absent value is Java's own "TargetedController" default, not resolved) is
    asked a new `PlayerController` method, `ConfirmPayCost` (its twenty-seventh), and a yes actually charged through
    `PayManaCost` (manapay.go) — the identical "decide, then pay" split every other mana decision on the interface
    already has. The ability's own body runs when nobody paid (`UnlessSwitched$`'s own presence flips that), and
    `UnlessResolveSubs$` decides whether its own chained `SubAbility$` still runs regardless (absent, Java's own
    "Always") or only on one particular outcome ("WhenPaid"/"WhenNotPaid"). Trimmed to the corpus's own one resolvable
    shape — a pure-mana `UnlessCost$` (a new `cost.Cost.IsPureMana`, `internal/cost`, added once a direct `Tap`/`Untap`
    field read in effect.go collided with `phase.go`'s own `Untap` step constant under enginelint's plain-identifier
    matching, moving the field read into a different Go package sidesteps it) and an explicit `UnlessPayer$` naming
    `You`/`Player`/`Opponent`/`Player.Opponent` (`definedPlayers`, reused) — 55 of the corpus's 727 real `UnlessCost$`
    lines resolve past this gate and are actually reachable by this port at all: nicol_bolas.txt's own real "sacrifice
    CARDNAME unless you pay {U}{B}{R}" shape dominates (51 of `Sacrifice`'s own 155 real lines, all reached through an
    already-built trigger mode -- `T:Mode$ Phase`, mostly -- rather than an activated ability's own `Cost$`, which this
    port cannot activate at all), plus 3 of `DealDamage`'s own 31 (force_of_nature.txt's own real "deals 8 damage to you
    unless you pay {G}{G}{G}{G}") and 1 of `Pump`'s own 15 (spitting_slug.txt's own real "gains first strike... unless
    you pay {1}{G}", chaining `UnlessResolveSubs$ WhenNotPaid` into `PumpAll` when the cost goes unpaid). Of the 672
    real lines that do not: 6 real `Sacrifice` lines that otherwise clear this gate's own filter are a
    `S:Mode$ Continuous | AddTrigger$` line's own dynamically granted trigger instead
    (aura_flux.txt's/magus_of_the_tabernacle.txt's own real "other permanents have 'sacrifice this unless you pay...'"
    among them) — `AddTrigger$` is not a built continuous-effect param, so the trigger it would grant never exists in
    this port's own game at all; the rest name a non-mana cost part (`Sac<.../Discard<.../PayLife<...`), an X shard, an
    unresolvable `UnlessPayer$` value (`TriggeredPlayer`, `EnchantedController`, ...), an activated ability's own
    `Cost$` (general activated-ability casting — real now too, below, but not yet threaded back through this gate's own
    reachability accounting), an instant or sorcery's own top-level line (`CastSpell`'s own doc comment: "an instant or
    sorcery resolves into a script effect this port does not build"), or a line reached only through an unbuilt API's
    own `SubAbility$`/`RepeatSubAbility$`/... chain link (`DB$ Effect`, `DB$ Repeat`, `DB$ GenericChoice`,
    `DB$ DelayedTrigger`, none built). `UnlessCost$`/`UnlessPayer$`/`UnlessResolveSubs$`/`UnlessSwitched$` no longer
    block any of the eight already-built effects that named them in their own unresolved-param lists
    (`sacrificeEffect`/`sacrificeAllEffect`/`dealDamageEffect`/`pumpEffect`/`pumpAllEffect`/`gainLifeEffect`/
    `loseLifeEffect`/`discardEffect`) — `Sacrifice`'s own real corpus count rises from 465 to 516 of 792, `DealDamage`'s
    from 62 to 65 of 2,219, and `Pump`'s own `Defined$`-shape count from 1,147 to 1,148 of 1,335.

    **Activating an ability landed too** (`ActivateAbility`, activateability.go) — CR 602.2, the corpus's own single
    largest still-unbuilt action by real line count (10,879 real `A:AB$` lines, more than any one trigger mode past
    `Mode$ ChangesZone` itself), trimmed to its own two dominant real `Cost$` shapes: pure mana, and pure mana plus a
    single Tap-self token (`cost.Cost.ActivationShape`, `internal/cost` — `IsPureMana`'s own sibling, needed because a
    bare `T` always also parses as its own named `Part` alongside setting the `Tap` flag, `namedParts`' own trailing
    `{name: "T", ...}` entry, so `IsPureMana`'s own flat "no `Parts` at all" contract cannot just add `Tap` to its own
    allowed set). 6,246 of the corpus's 10,879 real `A:AB$` lines carry that shape (2,515 bare `T`, 1,090 bare mana, 995
    two mana symbols, 930 mana-plus-`T`, a long tail past that); excluding `AB$ Mana` itself (1,845 — CR 605.3a's own
    no-stack immediate resolution, a wholly different mechanism this port only has for a basic land's own intrinsic
    ability, `TapLandForMana`, manaability.go) leaves 4,401 real non-mana activated abilities this action can reach at
    the shape level, 1,987 of them already naming one of the twelve already-built effects (`Pump` 993, `PutCounter` 293,
    `DealDamage` 221, `Draw` 196, `PumpAll` 119, `LoseLife` 41, `GainLife` 39, `Scry` 31, `Discard` 27, `Surveil` 27) —
    real lines none of those effects' own previously-published "N of M resolves" counts include yet, since every one was
    computed against cast/trigger reachability alone; recomputing each against activated-ability reachability too is a
    further chunk's own work, not done here.

    Timing collapses to `CastSpell`'s own CR 601.3a simplification (active player, a main phase, an empty stack) since
    this chunk landed before this port had a real priority window. ADR-0019's `PassPriority` later replaced this with CR
    307.1's real split (instant speed by default, sorcery speed only for `SorcerySpeed$`/`Planeswalker$`), documented at
    [`## Interactive priority: CR 117 lands`](porting/port-log/game-state/turn-stack-combat.md#interactive-priority-cr-117-lands).
    A Tap-self cost checks CR 602.5b/302.6 first (`Card.SummonSick`/`HasKeyword("Haste")`, `DeclareCombatAttackers`'s
    own identical gate, `attack.go`, reused) with no side effect yet — already tapped, or summoning-sick without haste,
    both decline outright; the mana half pays through `PayManaCost` exactly as `CastSpell`'s own does, and only once
    that succeeds does the tap itself actually happen (`Card.Tapped` set, `checkTapsTriggers` fired), so a failed mana
    payment never leaves the source tapped for nothing. A successful activation pushes through `pushTriggeredAbilities`
    (trigger.go) with the activating player as its own sole entry — resolving `ValidTgts$` (targeting.go) and firing CR
    115's own "becomes the target" check the identical way a triggered ability's own push already does, APNAP ordering a
    harmless no-op over the one player activating — reusing every one of the twelve already-built effects and the
    general `Registry.Resolve` machinery (`UnlessCost$`/`SubAbility$` chaining/`ConditionCheckSVar$`/...) they already
    carry, with no new effect code at all. `compile.Face.Abilities` (compile.go) already carried every `A:` line's own
    compiled `Ability` since M3 -- both `A:AB$` (`Record` `Activated`) and `A:SP$` (`Record` `Spell`) share the one
    slice, told apart by `Record` alone -- so this needed no new compile-layer work at all, only an engine-side consumer
    for what had sat unread.

    `ActivateAbility` gained a third cost shape too -- mana and/or a Tap-self token plus a single self-sacrifice token,
    `Sac<1/CARDNAME>` ("sacrifice this permanent," fetch lands' and sac outlets' own dominant real shape), through a new
    the same `cost.Cost.ActivationShape` (`internal/cost`), admitting exactly one further `Part` naming the literal
    self-reference `Sac<1/CARDNAME>` and rejecting any chosen count or chosen valid spec the identical way it already
    rejects any other named `Part`. 947 more of the corpus's own non-`AB$ Mana` real `A:AB$` lines are reachable at the
    shape level this way (`ChangeZone` 164, `Draw` 145, `Destroy` 94, `DealDamage` 91, `Pump` 60, `GainLife` 52, `Token`
    44, `PutCounter` 31 among the largest; 441 of the 947 already name one of the twelve already-built effects) --
    recomputing each effect's own "N of M resolves" count against this shape too stays the same deferred further-chunk
    work `ActivationShape`'s own landing already named, not done here. Paying it reuses `sacrificeCards`
    (sacrificeeffect.go) wholesale rather than a new sacrifice primitive -- CR 701.20's own "dies" trigger,
    `RememberSacrificed$`, and the batched `Mode$ ChangesZoneAll` firing all come free, exactly as they already do for
    `Sacrifice`'s own "Self" branch (item 26, above) -- committed last, after the mana and the tap, so a self-sac cost
    never sacrifices a permanent whose own mana or tap half of the same cost went unpaid (CR 601.2h's own "costs may be
    paid in any order" makes this ordering a free choice rather than an approximation of Java's own part-by-part,
    player-cancellable `CostPayment`, which this port does not build). The pushed ability still resolves normally
    afterward even though its own source has already left the battlefield (CR 112.7a) -- the identical "ability survives
    its source" contract a `SubAbility$` chain into a just-sacrificed card's own `Defined$ Self` already relies on
    elsewhere in this port.

    **CR 605.3's own general mana ability landed too** (`ActivateManaAbility`, activatemanaability.go) -- every real
    card printing its own `A:AB$ Mana` line (rocks, dorks, Treasures), not only a basic land's synthesized intrinsic one
    (`TapLandForMana`, manaability.go, CR 305.6). 2,156 real lines exist corpus-wide, 1,946 already matching
    `ActivationShape` (internal/cost) -- the identical predicate `ActivateAbility` already uses, reused outright rather
    than a second one, since `ActivateAbility` itself refuses API `"Mana"` and `ActivateManaAbility` refuses anything
    else. `Produced$`'s own dominant real shape within that 1,946 -- a single literal WUBRG letter or `C` (colorless),
    1,005 lines -- resolves through a new `producedManaColor`; the remaining 108 (past `Any`/`Combo`/ `Chosen`, below)
    name something else or no `Produced$` at all. A new positive allow-list, `manaAbilityAllowedParams`, admits only the
    five real keys this dispatch reads or safely ignores (`AB$`/`Cost$`/`SpellDescription$`/`Produced$`/`Amount$`)
    rather than a growing per-effect blocklist -- `compile.Ability.Params` is directly enumerable, so naming what a mana
    ability's own small real vocabulary needs was shorter than naming the roughly twenty further keys it does not. 850
    of the 1,005 clear it and resolve end to end; the rest name `RestrictValid$` (53, a mana-pool spending restriction
    this port's own `Pool` has no bucket for), `SubAbility$` (28, a further ability this immediate no-stack resolution
    has nowhere to route through `Registry.Resolve`), a bare "activate only if..." restriction
    (`IsPresent$`/`ConditionCheckSVar$`/... 26 combined), a mana-tagging effect
    (`TriggersWhenSpent$`/`AddsKeywords$`/... 16 combined), or AI hinting/a cost-description gate (`AILogic$`/
    `PrecostDesc$`/... 13 combined). Payment order matches `ActivateAbility`'s own exactly -- mana, tap, self-sac,
    reusing `sacrificeCards` wholesale for the last -- and `checkTapsForManaTriggers` (CR 603's own "taps for mana"
    trigger, `TapLandForMana`'s own pairing) fires only when the cost actually has a Tap component.

    **`Produced$ Any` landed too** -- CR 605.3b's own "choose a color," 334 more of the 1,946. A new `PlayerController`
    method, `ChooseManaColor` (its 28th), asks the decider directly, taking an `options mana.Colors` set -- distinct
    from `ChooseHybridManaColor`'s own always-exactly-two contract (CR 601.2h's own hybrid-payment decision). The answer
    is validated (`mana.Colors.Count() == 1`, and a member of `options`) before it ever reaches `Pool.Add`, which panics
    on anything else -- `PlayerController`'s own "not re-checked, trust the controller's answer" contract stops here
    rather than at that panic, since a bad script answer is a testing bug, not an engine invariant breach (GO-7). The
    regression-toggle check for this guard demonstrated the point directly: disabling it turned the expected test
    failure into an actual panic.

    **`Produced$ Combo <letters>` landed too** -- CR 605.3b's own restricted-choice version of "Any," a dual/tri-land's
    own real "Add W or U"/"Add G, U, or R" (`rootbound_crag.txt`'s/`rattleclaw_mystic.txt`'s own real shape), 367 more
    of the 1,946. `ChooseManaColor`'s own `options` parameter -- added for exactly this, not only for "Any" -- narrows
    to a new `parseComboColors`' own parsed letter set (two to four literal WUBRG letters; anything else -- `Combo Any`/
    `Combo AnyDifferent`, 24 combined, CR 605.3b's own "add two mana in any combination of colors," a per-unit
    independent choice this single-color-per-activation dispatch does not model; `ColorIdentity`, 6, Commander's own
    format concept, untracked; a `Chosen` token, `producedManaColor`'s own identical unresolved reference -- fails the
    whole match rather than guessing a subset, PORT-8/GO-7) rather than a second interface method. `ActivateManaAbility`
    checks the answer is both exactly one color AND a member of that narrower set, the identical "trust ends here, not
    at `Pool.Add`'s own panic" contract "Any" already has -- confirmed by its own regression-toggle test, disabling just
    the `options.Has` half of the check failed only the one test built to catch a controller naming a color outside the
    offered set.

    **`ActivateAbility` gained a fourth cost primitive too** -- `Discard<N/Card>`, "discard N cards of your choice," 228
    more real non-`AB$ Mana` `A:AB$` lines -- reusing `PlayerController.ChooseCardsToDiscard` (already built for
    `discardEffect`) and a new shared `discardCards` (discardeffect.go, `sacrificeCards`'s own identical
    "shared-execution-helper" shape) rather than a fourth cost payment path each writing its own move-and-trigger loop.
    This landing also replaced three growing near-identical `cost.Cost` predicates
    (`IsPureManaOrTap`/`IsPureManaTapAndSelfSac`/`SelfSac`) with one `cost.Cost.ActivationShape` decomposition --
    `Discard<N/Card>`'s own count could not fit a bare `bool` the way `Tap`/`SelfSac` could, and three near-identical
    predicates was already the sign a fourth should not be a fourth (Rule of Three). Every feasibility check still runs
    before anything is committed (mana, tap, self-sac, discard, in that order): a `Discard` component first checks the
    activating player's own hand actually holds `DiscardN` cards, declining outright rather than asking
    `ChooseCardsToDiscard` for more cards than the hand has -- a real correctness gap the regression-toggle check itself
    caught: disabling that hand-size guard turned a clean failed-assertion `FAIL` into an actual
    `panic: engine: scripted controller ran out of discard choice decisions`, the identical shape `ChooseManaColor`'s
    own guard's regression-toggle already demonstrated for `Pool.Add`. `ActivateManaAbility` explicitly declines any
    `DiscardN > 0` rather than silently ignoring it -- 0 real `AB$ Mana` lines carry `Discard<...>` at all, so there is
    no execution path to reuse, and letting the shape through unhandled would mean claiming the cost was paid while
    discarding nothing (PORT-8/GO-7).

    **`ActivateAbility` gained a fifth cost primitive too** -- `PayLife<N>`, "pay N life," 108 more real non-`AB$ Mana`
    `A:AB$` lines. `PayLifeN` slotted straight into `ActivationShape` (internal/cost) rather than becoming a fifth
    near-identical predicate the way `Discard$` almost did. A feasibility check first confirms `Player.Life >= PayLifeN`
    (CR 119.4: a life payment can never bring the payer below 0), run before anything else commits the same way the
    Tap-self and Discard feasibility checks already are; committing subtracts `PayLifeN` from `Player.Life` and emits
    the identical `LifeChanged` event `loseLifeEffect` already emits for an ordinary life loss -- verified by reading
    `CostPayLife.java`/`Player.payLife` directly rather than assuming: Java's own life-payment path routes through the
    identical `loseLife` machinery `LifeLoseEffect` uses, so this port's own single `LifeChanged` event kind correctly
    covers both causes. No `Mode$ LifeLost`/`LifeLostAll` trigger check runs either way -- the identical omission
    `loseLifeEffect`'s own doc comment already justifies, since 0 real corpus `T:` lines name that mode regardless of
    what caused the loss. `ActivateManaAbility` declines any `PayLifeN > 0` the identical way it already declines
    `DiscardN > 0` (0 real `AB$ Mana` lines carry `PayLife<...>` either).

    **`ActivateAbility` gained a sixth cost primitive too** -- `PayEnergy<N>`, "pay N energy counters" (CR 122.5), 56
    more real non-`AB$ Mana` `A:AB$` lines. `PayEnergyN` slotted straight into `ActivationShape` (internal/cost) rather
    than becoming a sixth near-identical predicate. A feasibility check first confirms
    `Player.Counters.Count(Energy) >= PayEnergyN`, run before anything else commits the identical way every other cost
    primitive's own feasibility check already is; committing subtracts `PayEnergyN` from the player's own `Energy`
    counter (`Player.Counters`, `counters.go` -- a new named `CounterType` constant, joining `Poison`) and emits the
    identical `CounterChanged` event `putCounterEffect` already emits for a player-level counter (a new
    `CounterDetailEnergy` value, `event.go`'s own closed set, extended alongside it). Verified by reading
    `CostPayEnergy.java`/`Player.payEnergy` directly: unlike `Player.payLife`, `Player.payEnergy` fires no trigger of
    its own at all -- Forge's own `TriggerType` has no `PayEnergy` mode to even skip, a simpler case than `PayLife`'s
    own "the mode exists but 0 real lines use it." Unlike `Discard`/`PayLife`, `ActivateManaAbility` does **not**
    decline `PayEnergyN > 0`: 4 real `AB$ Mana` lines actually carry it (`aether_hub.txt`'s own real "T, Pay one energy
    counter: Add one mana of any color," `Cost$ T PayEnergy<1> | Produced$ Any`), so this is the first `ActivationShape`
    primitive both dispatch functions actually pay rather than one declining what the other executes.

    **`ActivateAbility` gained a seventh cost primitive too** -- `Exile<1/CARDNAME>`, "exile this permanent,"
    `Sac<1/CARDNAME>`'s own sibling shape, 61 more real non-`AB$ Mana` `A:AB$` lines. `SelfExile` slotted into
    `ActivationShape` as a plain bool exactly like `SelfSac`, needing no feasibility check of its own (the source is
    already known to be on the battlefield by the time any cost is paid). This is the first `ActivationShape` primitive
    to need genuinely new engine machinery rather than reusing an existing effect wholesale: exile has no dedicated
    corpus-relevant trigger mode (Forge's own `TriggerType.Exiled` exists, but only 3 real corpus lines name
    `Mode$ Exiled`), so the real question was whether a permanent leaving the battlefield via exile should fire CR
    603.6d's own general "leaves the battlefield" trigger family the identical way dying does -- confirmed by reading
    `GameAction.exile`/`GameAction.moveTo` directly: `moveTo` fires `TriggerType.ChangesZone` for every zone move
    unconditionally, `checkDiesTriggers` (trigger.go) being this port's own specialization of that generic firing for
    the one destination its call sites need (Graveyard). A new `exile.go` builds the identical specialization for Exile:
    `isExiledTrigger`/`checkExiledTriggers`/`otherExiledTriggerMatches` are `isDiesTrigger`/`checkDiesTriggers`/
    `otherDiesTriggerMatches`'s own exact structural copies, `Destination$ Exile` in place of `Graveyard` -- `g.LKI`'s
    own dying-state freeze already applies to any battlefield-leaving move, not the graveyard specifically (`game.go`'s
    own `Move`, `from == Battlefield && kind != Battlefield`), so no change was needed there. A new `exileCards` is
    `sacrificeCards`'s own sibling too, but simpler: no real `Exile<1/CARDNAME>` cost line combines with a
    `RememberExiled$`-shaped param, so it takes no `*Ability` parameter at all, just `ids []CardID`; it still fires the
    individual leaves-the-battlefield check per card and the batched `Mode$ ChangesZoneAll` once for the whole set,
    reusing that function outright since it already takes an origin/destination pair as parameters.
    `ActivateManaAbility` pays `SelfExile` rather than declining it, the identical `PayEnergy` precedent: 1 real
    `AB$ Mana` line needs it (`mirrored_lotus.txt`'s own real "T, Exile CARDNAME: Add three mana of any one color").

    **`ActivateAbility`/`ActivateManaAbility` gained an eighth cost primitive too** -- `tapXType<N/Type>`, CR 602's own
    "tap N untapped permanents of a type" (`CostTapType.java`), 201 real `A:AB$` lines (22 more real `A:AB$ Mana` lines,
    `birchlore_rangers.txt`'s own real "Tap two untapped Elves you control: Add one mana of any color" among them).
    Unlike every primitive before it (Tap/SelfSac/SelfExile/PayLife/PayEnergy), this one is a choice among many rather
    than a self-reference or a hand-wide pick, so `ActivationShape` carries the count AND the raw, unparsed type field
    (`TapTypeN`/`TapTypeSpec`) -- `internal/cost` has no dependency on `internal/valid`, so the spec's own semantic
    resolvability is entirely an engine-layer question, deferred to a new `taptype.go`
    (`tapTypeResolvable`/`tapTypeCandidates`/`tapChosenPermanents`) and a new `PlayerController` method,
    `ChoosePermanentsToTap` (its 29th, `ChoosePermanentsToSacrifice`'s own shape reused for a third exactly-N-of-a-set
    decision). A Cost-syntax `;`-separated type list becomes `valid.Parse`'s own `,`-separated OR (Cost strings use `;`
    specifically because a literal `,` can appear in the part's own trailing description field); `CAN_TAP` becomes a
    plain `!Tapped` read, since this port tracks no CantTap-shaped static ability to consult the way Java's own
    `CardPredicates.CAN_TAP` does. The one real correctness nuance is `CostTapType.java`'s own
    `canTapSource = !costHasTapSource`: the ability's own source is excluded from its own tapXType candidate pool
    whenever the same cost ALSO taps it through a separate plain `T` token, even when the type spec itself does not say
    `.Other` -- verified with a dedicated regression test (a cost naming both `T` and `tapXType<1/Creature>`, with the
    source as the only Creature on the battlefield, correctly declines rather than double-counting the identical
    permanent for two different cost components). `withTotalPowerGE`/`sharesCreatureTypeWith` (3 combined real lines,
    "total power N or greater"/"any two share a creature type" rather than a plain card count) and `OriginalHost` (0
    real lines) are refused explicitly (`tapTypeResolvable`) rather than reaching `Matches` with a spec it has no
    property for -- though a regression-toggle check on this guard found it is not, on its own, load-bearing today:
    `internal/valid`'s own documented fail-safe contract ("a base it does not recognise simply matches nothing") already
    makes an unrecognized Property fail the same way, so `tapTypeCandidates`' own count naturally falls short of
    `TapTypeN` and the caller declines regardless of whether `tapTypeResolvable` runs at all. The explicit guard is kept
    anyway, the same reason every other effect in this port names its own unresolved params rather than trusting an
    implicit fail-safe three files away. `Mode$ TapAll` (2 real lines, CR 603's own batched "these all became tapped
    together" trigger) is not built -- each tapped permanent still fires the ordinary "becomes tapped" trigger
    (`checkTapsTriggers`) individually instead, the identical simplification `Mode$ Exiled`'s own 3-line irrelevance
    already justified for exile.

    **`ActivateAbility` gained its ninth and tenth cost primitives too** -- `Return<1/CARDNAME>` (`SelfSac`'s/
    `SelfExile`'s own third self-reference sibling, CR 602, "return this permanent to its owner's hand," 16 real lines)
    and `Return<N/Type>` (`tapXType`'s own sibling for "return to hand" rather than "tap," 34 real lines). Reading
    `CostReturn.java` directly settled the one real design question: unlike `CostTapType.java`'s own `canTapSource`,
    `CostReturn`'s `canPay`/`getMaxAmountX` never exclude the ability's own source from the type-list branch's own
    candidates -- a permanent already tapped by an earlier `T` component of the same cost is still a legal
    `Return<N/Type>` candidate, since being tapped does not stop it from also being returned. `returnTypeCandidates`
    (new `returncost.go`) therefore takes no `excludeSelf` parameter at all, unlike `tapTypeCandidates`, and no
    tapped-state filter either -- `CostReturn` carries none. Building the self-reference check for `Return<1/CARDNAME>`
    surfaced a real, if narrow, gap in the two earlier self-reference primitives: `CostPart.java`'s own
    `payCostFromSource` has always accepted `NICKNAME` as equally-literal a self-reference token as `CARDNAME` (an
    alternate-name reference some cards carry), but `SelfSac`/`SelfExile`'s own original checks only ever tested for
    `CARDNAME` -- 11 real corpus lines (`Sac<1/NICKNAME>`/`Exile<1/NICKNAME>`) were silently falling through to a
    decline instead of paying the cost. Fixed in the same pass, for all three primitives at once, with a shared
    `isSelfReferenceField` helper (`internal/cost/cost.go`) rather than three separate literal comparisons. `Return`
    needed the identical new trigger machinery `Exile<1/CARDNAME>`'s own landing built (CR 603.6d's "leaves the
    battlefield" family, since `Return` has no dedicated corpus-relevant trigger mode of its own either): a third
    sibling, `isReturnedTrigger`/`checkReturnedTriggers`/`otherReturnedTriggerMatches` (new `returncost.go`), with
    `Destination$ Hand` in place of `Exile`/`Graveyard`. A new `PlayerController` method, `ChoosePermanentsToReturn`
    (its 30th), reuses `ChoosePermanentsToTap`'s own shape for a fourth exactly-N-of-a-set decision. Both shapes are
    declined outright by `ActivateManaAbility`, `Discard`/`PayLife`'s own precedent: the sole real `AB$ Mana` line
    naming `Return<1/CARDNAME>` is already unreachable for an unrelated reason (`SorcerySpeed$`, not in
    `manaAbilityAllowedParams`), and 0 real lines name `Return<N/Type>` at all.

    **`ActivateAbility`/`ActivateManaAbility` gained an eleventh cost primitive too** -- `Exert<1/CARDNAME>` (CR
    701.42a, `SelfSac`'s own fourth self-reference sibling, `Card.exert(Player)` in Java), 36 real lines, every one the
    literal self-reference shape -- 0 real `Exert<N/Type>` lines exist, so unlike Sac/Exile/Return this primitive has no
    chosen-type sibling to build at all. What makes Exert different from every self-reference primitive before it: it
    moves nothing. Paying it sets a new `Card.Exerted bool` (`card.go` -- a single bool rather than Java's own
    per-player `exertedByPlayer` set, since this port's every real activation-cost caller is the card's own controller
    and control does not realistically change before that same player's own next untap step) and fires CR 701.42a's own
    trigger (`checkExertedTriggers`, new `exertcost.go`) -- reusing `checkTapsTriggers`'s own single unified battlefield
    walk rather than building a fourth own/other-split sibling of `checkDiesTriggers`, since exerting stays on the
    battlefield throughout and needs no `g.LKI` lookback the way a zone change does. CR 701.42b's own actual cost -- "it
    doesn't untap during your next untap step" -- is entirely deferred: `untapStep` (`turn.go`) now reads `Card.Exerted`
    before `untapBlocked`, `Card.untap(Player)`'s own `isExertedBy(phase)` ordering in Java ported directly, and clears
    the flag unconditionally every untap step regardless of whether untapping was actually skipped for it or any other
    reason (`Untap.java`'s own separate "remove exerted flags from all things in play" pass, unconditional there too) --
    verified with a dedicated regression test and a regression-toggle pass (disabling the check turned a clean pass into
    a clean failed assertion, not a panic). `Move`'s own battlefield-leaving reset (`game.go`) clears `Exerted`
    alongside `Tapped`/`SummonSick` now too. `ActivateManaAbility` pays this primitive rather than declining it: 1 real
    `AB$ Mana` line needs it with no other unresolved param (a second real line combining `Exert<1/CARDNAME>` with
    `AddsKeywords$`/`AddsKeywordsValid$`/`AddsKeywordsUntil$` stays unreachable regardless, already outside
    `manaAbilityAllowedParams` for a reason unrelated to this landing).

    **`ActivateAbility`/`ActivateManaAbility` gained a twelfth and thirteenth cost primitive too** --
    `AddCounter<N/ Type>` and `SubCounter<N/Type>` (`CostPutCounter.java`/`CostRemoveCounter.java`), the self-reference
    shape only (`CostPart.java`'s own `payCostFromSource`, `isSelfReferenceField` reused), the dominant real cost shape
    of CR 606's own loyalty ability -- a prior pass had deferred `SubCounter<...>`'s own 950 real occurrences as
    "notably larger scope," assuming a whole planeswalker mechanic was missing to build first; re-reading
    `SpellAbility.isPwAbility()` showed `Planeswalker$` is a bare `hasParam` check, not a class of its own, and
    `Card.Counters`/the zero-loyalty SBA (`action.go`) already existed to build on. `AddCounterN`/`AddCounterType` and
    `SubCounterN`/`SubCounterType` join `ActivationShape` (`internal/cost`) as `TapTypeN`/`TapTypeSpec`'s own field-pair
    shape, but `0` is a real value here, not "absent" the way every other `N` field treats it: `AddCounter<0/LOYALTY>`
    (CR 606's own "+0" loyalty ability, 54 real lines) and `SubCounter<0/LOYALTY>` (5 real lines, an oddly-spelled
    version of the identical shape) are both real corpus costs, so `AddCounterType`/`SubCounterType` being non-empty,
    not `N != 0`, is what signals presence. A new `Card.LoyaltyAbilityActivated bool` (CR 606.3's own once-per-turn
    restriction -- `Card.planeswalkerAbilityActivated` in Java collapsed from an `int`, since the only reason Java
    counts past one is `StaticAbilityNumLoyaltyAct`, a limit-raising static ability this port does not build) is checked
    via `ability.Param("Planeswalker")`'s own bare presence (the corpus writes both `Planeswalker$ True` and
    `Planeswalker$ true`) before either caller commits anything, and set once every other part of the cost has actually
    committed, regardless of shape -- CR 606.3 restricts the whole ability, so a "+0" `AddCounter<0/...>` ability sets
    the flag exactly the same as any other. Reset every cleanup (`cleanupStep`, alongside `AttacksThisTurn`/
    `BecameTargetThisTurn`) and on every battlefield-leaving `Move`/`MoveToLibraryTop` (alongside
    `Tapped`/`SummonSick`/`Exerted`) -- CR 400.7's own "a new object remembers nothing." `SubCounter` checks CR 121.5's
    own floor (`CostRemoveCounter.java`'s own `source.getCounters(cntrs) - amount >= 0`,
    `shape.SubCounterN > c.Counters.Count(...)`); `AddCounter` has none (`CostPutCounter.java`'s own
    `getAbilityAmount(ability) == 0` early return -- adding never fails). `ActivateManaAbility` pays both rather than
    declining them, `PayEnergy`/`SelfExile`/ `SelfExert`/`tapXType`'s own "corpus actually needs it" precedent: 27 of
    the corpus's 83 real `AB$ Mana` lines naming either resolve fully (3 `AddCounter`, 24 `SubCounter`, 6 of the 27
    loyalty abilities) once `Produced$`'s own literal-shape gate (`producedManaColor`/`parseComboColors`) is checked too
    -- catching an over-count in this landing's own first pass (31, before checking `Produced$` at all): a bare
    `Produced$ R G`, not `Combo R G`, still declines the identical way every other unresolvable `Produced$` shape
    already does. `manaAbilityAllowedParams` gains `planeswalker`/`ultimate` (`Ultimate$` purely descriptive, admitted
    the same reason `SpellDescription$` already is).

    **A real, separate bug surfaced and fixed in the same pass:** pushing a `Planeswalker$`-carrying ability onto the
    stack for the first time (this port had never been able to before this landing) immediately hit
    `engine: GainLife: Planeswalker$ not resolvable yet` -- nine already-built effects
    (`dealDamageEffect`/`gainLifeEffect`/
    `loseLifeEffect`/`pumpAllEffect`/`putCounterEffect`/`scryEffect`/`sacrificeAllEffect`/`sacrificeEffect`/
    `surveilEffect`) each independently listed `Planeswalker`/`Ultimate` (two of them) in their own unresolved-param
    blocklist, defensively added by an earlier chunk that saw the param on real corpus lines without realizing it is
    purely a cost-side marker with zero bearing on how the effect it cost-gates actually resolves. Removed from all
    nine, each headline corpus count corrected upward: `dealDamageEffect` 65→72, `gainLifeEffect` 857→862,
    `loseLifeEffect` 300→306 (the `Defined$` branch alone), `putCounterEffect` 992→993, `scryEffect` 332→340,
    `surveilEffect` 183→187, `sacrificeAllEffect` 91→92, `sacrificeEffect` 516→522, `pumpAllEffect` 642→668. Not a
    hypothetical found by re-reading old code for its own sake -- found because this was the first thing in the whole
    session to actually try pushing one of these abilities through `Registry.Resolve`, and every one of the nine failed
    identically the first time it was tried.

    **`ActivateAbility`/`ActivateManaAbility` gained a fourteenth cost primitive too** -- `ExileFromGrave<1/CARDNAME>`
    (`CostExile.java`'s own graveyard-origin constructor, the identical class `Exile<1/CARDNAME>` already reads, a
    different `ZoneType` argument), paired with CR 602.2's own `ActivationZone$` generalization past a permanent already
    on the battlefield -- `SpellAbilityRestriction.checkZoneRestrictions`, read directly, since without it this
    primitive alone would pay off nothing (every real corpus line naming it also names `ActivationZone$`, and
    `ActivateAbility` hardcoded `c.Zone != Battlefield` at its own top gate before this). 230 real corpus lines name
    `ActivationZone$ Graveyard` (Escape/Unearth-style abilities), the corpus's own dominant non-Battlefield destination
    -- `Hand`'s 97 (Cycling, Transmute) and `Command`'s 57 (emblems, sagas) stay unbuilt, this landing's own scope cut.
    A Graveyard-zone ability's own "you" is the source's **owner**, not its controller (CR 109.5 -- a card outside the
    battlefield has no controller), so `ActivateAbility`/`ActivateManaAbility` both now switch on
    `ability.Param("ActivationZone")` (fetched right after the ability itself, reordered ahead of the old top-of-
    function zone/controller check, since that check now depends on which ability is being activated): absent or
    `Battlefield` keeps the identical `c.Controller() != pid || c.Zone != Battlefield` gate, `Graveyard` swaps it for
    `c.Owner != pid || c.Zone != Graveyard`, anything else declines outright. `SelfExileFromGrave bool` joined
    `ActivationShape` (`internal/cost`) as a fourteenth primitive; a Graveyard-zone ability naming any other primitive
    (`Tap`/`SelfSac`/`Discard`/...) declines outright too -- 0 real corpus lines combine `ActivationZone$ Graveyard`
    with anything but plain mana or `ExileFromGrave<1/CARDNAME>`, confirmed by grep before writing the guard. Paying it
    calls a new `exileFromGraveyard` (new `exilefromgrave.go`) -- `exileCards`'s (exile.go) own much simpler sibling: a
    plain zone move with no trigger check and no `g.LKI` snapshot, since CR 603.6d's own "leaves the battlefield" family
    is specifically about a permanent leaving the battlefield, which a graveyard card never was for this move (56 real
    corpus `T:Mode$ ChangesZone | Origin$ Graveyard` lines are a separate, unrelated, still-unbuilt "leaves the
    graveyard" trigger family this landing does not reach either). 168 of the corpus's own 220 real
    `ActivationZone$ Graveyard` lines resolve at the cost-shape level (82 pure mana, 86 mana plus `ExileFromGrave`); the
    other 52 name a chosen-type `Sac<.../Discard<.../tapXType<...` component this decomposition does not carry for a
    graveyard ability, correctly declined by the existing `ActivationShape` gate with no new code. Of those 168, 49
    actually run an already-built effect end to end (19 `PutCounter`, 11 `Pump`, 10 `Draw`, 3 `PumpAll`, 2 `GainLife`, 1
    each of `Discard`/`Scry`/`DealDamage`/`Mana`); 87 name `ChangeZone` (Escape's/Unearth's own dominant real effect,
    still this port's own single largest unbuilt API, 6,616 real corpus lines corpus-wide) and the remainder name
    another unbuilt API. `ActivateManaAbility` pays `SelfExileFromGrave` too -- the sole real
    `AB$ Mana | ActivationZone$ Graveyard` line needed `activationzone` added to `manaAbilityAllowedParams` alongside
    the zone-check generalization.

    **`ActivationZone$ Hand` lands too, with two more new cost primitives** -- `Discard<1/CARDNAME>` (CR 702.28's own
    Cycling and its own kin, "discard this card: draw a card," 69 real lines) and `ExileFromHand<1/CARDNAME>`
    (`CostExile.java`'s third real "from" zone, `SelfExileFromGrave`'s own sibling, 14 real lines) -- the corpus's own
    second-largest real `ActivationZone$` destination (97 lines) after Graveyard's own 230; `Command`'s 57 stays
    unbuilt. Both self-reference shapes had been silently unreachable rather than merely undiscovered before this
    landing: `Discard<1/CARDNAME>`'s own literal `1/CARDNAME` shape does not match the existing choose-N-from-hand
    `DiscardN` case's own `p.Field(1) == "Card"` guard, so every real self-discard line was falling through to the
    cost-shape's own `default: false` the entire time `Discard<N/Card>` has existed in this port -- caught by this
    landing's own corpus-frequency research into Cycling, not by any prior audit. `SelfDiscard bool`/
    `SelfExileFromHand bool` join `ActivationShape` (`internal/cost`) as a fifteenth primitive pair,
    `isSelfReferenceField` reused for both. `ActivateAbility`'s own `ActivationZone$` switch grows a third case (`Hand`:
    `c.Owner != pid || c.Zone != Hand`), and the cross-contamination guard generalizes to
    `nonBattlefield := fromGraveyard || fromHand` plus four one-line checks refusing the wrong zone's own self-reference
    primitive -- 0 real corpus lines combine `ActivationZone$ Hand` with any battlefield-only primitive or with
    `ExileFromGrave`, confirmed by grep before writing the guard, the identical discipline the Graveyard landing already
    established. Paying `SelfDiscard` reuses `discardCards` (discardeffect.go) wholesale -- CR 701.8's own
    `Mode$ Discarded` trigger fires the identical way it already does for the unrelated choose-N-from-hand shape, since
    Cycling really is an ordinary discard of a fixed, self-chosen card, the one real difference from
    `SelfExileFromGrave`/`SelfExileFromHand` (both fire no trigger at all, the identical CR 603.6d reasoning). A new
    `exileFromHand` (`exilefromgrave.go`) is `exileFromGraveyard`'s exact sibling. 92 of the corpus's own 95 real
    `ActivationZone$ Hand` lines resolve at the cost-shape level, 41 of those actually running an already-built effect
    end to end (25 `Pump`, 6 `DealDamage`, 3 `PutCounter`, 2 `Draw`, 2 `Mana`, 1 each of
    `Sacrifice`/`GainLife`/`Discard`) and 14 naming `ChangeZone`. `ActivateManaAbility` pays `SelfExileFromHand` -- 2 of
    its own 92 real `AB$ Mana | ActivationZone$ Hand` lines resolve fully -- but declines `SelfDiscard` outright, its
    own real `AB$ Mana` payoff being 0 lines (`DiscardN`/`PayLife`/`SelfReturn`'s own "0 real benefit" precedent, not
    `PayEnergy`/`SelfExile`/`tapXType`'s own "pay it" one).

27. Continuous effects & the layer system (`StaticAbilityContinuous`). **Six real slices of `Mode$ Continuous` now,
    Layer 7a among them, alongside two sibling modes built independently** — `layer.go` has the CR 613 layer _numbers_;
    `pt.go` folds power/toughness through them, and that folding mechanism has a real (non-test) caller for the first
    time: `applyContinuousPT` (`continuous.go`) resolves Layer 7b/7c
    (`SetPower$`/`SetToughness$`/`AddPower$`/`AddToughness$`) matched against a blanket `Affected$` valid-string — the
    anthem/equipment-bonus shape, 2,192 of 2,426 real `S:Mode$ Continuous` lines carrying one of those four keys —
    recomputed from scratch every `CheckStateBasedActions` pass rather than pushed once, matching Java's own
    `applyContinuousAbility` running fresh from `GameAction.checkStateEffects` every time (an anthem has to reach a
    creature that enters after it, and stop the instant it itself leaves). `PTEffect` gained `HasPower`/`HasToughness`
    flags to make this correct: a real corpus `SetPower$`-only or `SetToughness$`-only line (68 and 9 of them) must
    leave the other dimension untouched, which the original bare `int` fields could not express. Layers 4, 5 and 6
    (`applyContinuousType`/`Color`/`Keyword`) fold through `TypeMod`/`TypeEffect`, `ColorMod`/`ColorEffect` (one
    `Overwrite bool` for `SetColor$`) and `KeywordMod`/`KeywordEffect` (`typemod.go`, `colormod.go`, `keywordmod.go`),
    read back by `Card.Type`/`Colors`/`HasKeyword`, so combat's First Strike/Trample/Deathtouch reads and
    `cantBlockByKeywords` see a granted keyword unchanged. Each line goes through `continuouslayers.go`:
    `layerStaticApplies` (`StaticAbility.checkConditions`: `EffectZone$`, `Condition$`, `IsPresent$`,
    `TopCardOfLibraryIs$`, the `CheckSVar$` chain), `layerAffectedCards` (`getAffectedCards`: CDA self,
    `AffectedDefined$` Self/Enchanted/Equipped/AttachedBy Self, `AffectedZone$`, `Affected$`) and the
    TYPE/COLOR/ABILITIES branches' runtime tokens (`ChosenType`, `ImprintedCreatureType`, `All*LandType`,
    `Remove*Types$`, `ChosenColor`, `AllColors`, `CardColors`, `RemoveKeyword$`, `RemoveAllAbilities$`'s keyword half,
    ...). The subtype vocabulary comes off `compile.DB.Types()`, no new `Game` field (GO-2). 254 of 284 real
    `AddType$`/`RemoveType$` lines, 60 of 61 `AddColor$`/`SetColor$` and 1,710 of 1,875 `AddKeyword$` resolve; the rest
    (a `CheckSVar$` amount outside `Count$Valid`, an `EffectZone$` host `traitHosts` does not walk,
    `AddAllCreatureTypes$`, `CardManaCost`, `SharedKeywordsZone$`) and the counting rule:
    `porting/port-log/game-state/layers-4-5-6.md`. Not resolved for Layer 7 (Layers 4-6 resolve both):
    `AffectedDefined$`/`AffectedZone$` (0 and 24) — its own specific missing piece (`porting/port-log/game-state.md`'s
    "Layer 7, Layer 4, Layer 5 and Layer 6" section has the full account), not a reason to have skipped the slices that
    do resolve. `Condition$` is resolved now (below).

    `CharacteristicDefining$` (265 real lines, Layer 7a) and a non-numeric `AddPower$`/`AddToughness$`/`SetPower$`/
    `SetToughness$` naming a named SVar are resolved now, for the one shape both actually need most:
    `Count$Valid [<Zone>...] <spec>` — 2,804 of the corpus's 6,186 real `Count$` expressions (45%),
    `CardLists.getValidCardCount` against a zone, ported as a new `compile.Face.Amounts` (compile.go — every SVar a face
    defines that is not itself an ability, parsed once via `internal/expr.Parse`) and `resolveAmount`/`countValid` (a
    new `amount.go`, `internal/engine` — a `Literal` resolves directly, a `Reference` looks its name up in `Amounts` and
    resolves that in turn, an `Expression` resolves only when its outer head is `Count`, carries no operator suffix, and
    its own inner `Count$` head is one of the "Valid" family, reusing `Matches` (valid.go) and this port's own `Zone`
    (zone.go) the identical way every other valid-string check already does). `ptParam` (continuous.go) tries this once
    a plain `strconv.Atoi` fails; `applyOneCharacteristicDefiningPT` is Layer 7a's own new branch of
    `applyOneContinuousPT`, applying `SetPower$`/`SetToughness$` to host ALONE
    (`StaticAbilityContinuous.getAffectedCards`'s own CharacteristicDefining branch hardcodes the affected set to the
    host card regardless of any `Affected$` a real corpus line also carries) at `LayerCharacteristic` — a layer
    `PTEffect`'s own folding already carried since Layer 7b/7c first landed, unused as a real caller until now.
    `resolveAmount` now covers the real CDA vocabulary past the Valid family too: doXMath suffixes, `SVar$`/`Number$`,
    the measurable `handlePaid` properties (Tarmogoyf's `$CardTypes`, `$GreatestCardManaCost`, ...), Domain,
    YourLifeTotal, Devotion, Chroma, CardCounters and the rest -- 389 of 405 real CDA amount dimensions (no CDA writes
    `xPaid`); the last 16 (`ExiledWith$`, `Remembered$`, `$DifferentCardNames`, `YourTurns`, `Party`) and why are
    `porting/port-log/game-state/layer7a-cda-amounts.md`'s.

    **Layer 8 (`RULES`) has real content now too, this port's first player-facing continuous effect.** A new
    `RulesMod`/`RulesEffect` (`rulesmod.go`) lives on `Player`, not `Card` —
    `Card.PT`/`TypeMod`/`ColorMod`/`KeywordMod`'s own shape, just attached to the other side of `Affected$`'s own two
    real targets (`getAffectedPlayers`, `StaticAbilityContinuous.java`, alongside `getAffectedCards`).
    `applyContinuousRules`/`applyOneContinuousRules` (continuous.go) resolve
    `SetMaxHandSize$`/`RaiseMaxHandSize$`/`AdjustLandPlays$` (75 of 78 real lines) — `Affected$` matched through
    `matchesPlayerSpec` (valid.go, the identical dispatch `SpellCast`'s own `ValidActivatingPlayer$` already reuses,
    here against a static ability rather than a trigger), the numeric case through the existing `ptParam` (no new
    amount-resolution code needed), and `"Unlimited"` checked as its own sentinel before falling to `ptParam`
    (`p.setUnlimitedHandSize`/`addMaxLandPlaysInfinite`'s own literal, ported). Two new `Player` methods fold the
    effects against the printed defaults `turn.go`'s `MaxHandSize` and `land.go`'s `maxLandPlays` were already waiting
    for: `HandSizeLimit` (`SetMaxHandSize$` REPLACES the running limit and the unlimited flag with it,
    `RaiseMaxHandSize$` ADDS to it, both folded in Timestamp order — `foldPT`'s own combine convention, port-log's
    reason this port picked it over rediscovering Java's own iteration order for the rare case of two conflicting
    effects) and `LandPlayLimit` (`AdjustLandPlays$` sums unconditionally, `Player.getMaxLandPlays`'s own contract, no
    order-dependence at all). Both take the printed default as a parameter rather than reading `turn.go`/`land.go`'s own
    constants directly, keeping `player`'s own `enginelint` group acyclic. `cleanupStep`/`PlayLand` (turn.go/land.go)
    now call them instead of comparing against the bare constants. `MayPlay$` resolves as a per-card grant
    `CastSpell`/`PlayLand` consult (421 of 660 real lines; `MayLookAt$` is a no-op in an omniscient engine);
    `AddHiddenKeyword$` resolves per card for the four keyword lines something reads (41 of 53 real lines,
    `port-log/game-state/layers-text-and-rules.md`); the four vote/villainous-choice params resolve into `RulesEffect`
    fields Vote/VillainousChoice read; `ControlOpponentsSearchingLibrary$`/`DeclaresAttackers$`/`DeclaresBlockers$` hand
    a decision to another player's controller, which nothing here can; a qualified `Affected$` `matchesPlayerSpec`
    cannot resolve (`Player.NotedForGreenAnchor`/`Player.Chosen`, 1 real line each).

    The legend rule's own `ignoreLegendRule` exemption (item 25) and `CantBlockBy` (item 28's own combat note) already
    showed a static-ability mode can be independently buildable when it needs no layer-folding of its own —
    `Mode$ Continuous` was always going to be the one mode that could not skip that machinery entirely, and now six of
    its layers partly haven't had to: all six subsets needed only the valid-string evaluator (`valid.go`) every other
    slice already reused, plus (for Layers 4/5) small additions to `cardtype.Line`/`valid.go` themselves. The rest of
    Layers 4/5/6 past a literal token list, Layer 7a's own last 16 CDA amount dimensions, and Layer 8's own remainder
    above are the real remaining size of this item. Layer 3's `GainTextOf$` (1 real line, Volrath's Shapeshifter) swaps
    `Card.Def` for a composite text definition between Layers 2 and 4 (`applyContinuousText`,
    `port-log/game-state/layers-text-and-rules.md`). Layer 1 (copy effects) is not part of
    `StaticAbilityContinuous.java`'s switch either — zero real references to `StaticAbilityLayer.COPY` in it — but a
    resolution-time state swap (`Card.addCloneState`), so it is not this item's job: `Card.copies` holds it (`Clone`'s
    "becomes a copy", `port-log/game-state/effects-clone.md`), and "enters as a copy" is a Copy-layer replacement of the
    card's entry (`K:ETBReplacement:Copy` expanded at compile time, `entersascopy.go`; 50 of the corpus's 69 Copy-layer
    `Clone` replacement lines resolve, 18 are refused with an error, `port-log/game-state/layer1-enters-as-copy.md`).

    **Layer 2 (`CONTROL`) is real now too — this port's first controller-change mechanism.** A new
    `ControlMod`/`ControlEffect` (`controlmod.go`) folds onto `Card.Controller`, which stops being a plain field and
    becomes `Card.Controller()` (card.go): the identical "latest Timestamp wins" pattern `RulesMod`/`PTEffect` already
    use, Java's own `Card.tempControllers` (a `NavigableMap<Long, Player>`, `getController()` returning the
    highest-timestamp entry) collapsed to the one case this port needs, since it builds no equivalent of Java's own
    `setController` (an explicit "gain control permanently" one-shot effect — M6's own remaining territory, not this
    layer). `applyContinuousControl`/`applyOneContinuousControl` (continuous.go) resolve `GainControl$ You` — 43 of the
    corpus's 44 real `S:Mode$ Continuous` lines naming `GainControl$` (distinct from an unrelated
    `DB$ ChangeZone`/`DB$ Dig`'s own one-shot `GainControl$ True`, which shares the param name but is a wholly separate
    effect, M6's own "put onto the battlefield under your control" territory — a naive corpus grep for `GainControl$`
    conflates the two unless `Mode$ Continuous` is checked first) — resolved via `host.Controller()` the same way
    `RulesEffect`'s own player lookup reads the host's controller, against `Affected$`, overwhelmingly
    `Card.EnchantedBy`/`Permanent.EnchantedBy`/`Creature.EnchantedBy` (42 of 44, Control Magic's own shape — the Aura's
    host), needing nothing new: the identical valid-string match `applyOneContinuousPT` already does.
    `applyContinuousControl` runs FIRST among the six appliers (`CheckStateBasedActions`, action.go), ahead of Layers
    4/5/6/7/8, since CR 613.1 puts the control layer before every one of them and their own `Affected$` specs can
    themselves read `Controller()` (a `YouCtrl` property) — a stale value there would evaluate against last pass's
    controller, not this one's, a real ordering bug a dedicated regression test
    (`TestApplyContinuousControlRunsBeforeKeywordSoYouCtrlSeesTheNewController`) proves against. Every other read of
    `Card.Controller` across the engine (roughly eighty call sites, `combatdamage.go`/`staticability.go`/`trigger.go`/
    `valid.go`/`attack.go`/`castspell.go`/`manaability.go`/`land.go`/`action.go`/`continuous.go`, plus
    `internal/fixture`) became a `Controller()` call the same pass, so a stolen creature is controlled by its new
    controller everywhere the engine asks, not just where `applyContinuousControl` itself looks. Not resolved: the
    qualified `GainControl$ Player.isMonarch` (1 of 44) — no monarch mechanic to filter by (PORT-8/GO-7).

    **`Condition$` — the one gate shared by every layer above — is real now too.** A new `continuousConditionMet`
    (continuous.go) ports `StaticAbility.checkConditions`'s own `Condition$` switch, called from all six appliers in
    place of the blanket "any `Condition$` present, skip the line" rule each one had (Layer 4/5/6/7b/7c/8's own doc
    comments each named this the same missing piece). `PlayerTurn`/`NotPlayerTurn` (141, 8 of the corpus's 317 real
    `S:Mode$ Continuous | Condition$` lines) compare `Game.ActivePlayer()` against host's own controller;
    `Threshold`/`Hellbent` (61, 8) are graveyard/hand zone-size checks (`Player.hasThreshold`/`hasHellbent`);
    `Metalcraft` (18) counts battlefield permanents controller controls whose current, Layer-4-folded `Type()` carries
    Artifact (`battlefieldArtifactCount`); `Delirium` (23) unions every graveyard card's own current `Type()` into one
    `cardtype.Line` and counts its distinct core types (`graveyardCoreTypeCount`,
    `AbilityUtils.countCardTypesFromList`'s own `permanentTypes=false` form); `FatefulHour` (3) compares `Player.Life`
    against 5 — 262 of 317 real lines. Not resolved: `MaxSpeed` (40, Alchemy's own speed counter), `Blessing` (9, City's
    Blessing), `EnduringStory` (4, a Saga's own chapter count) and `Monarch` (2) — each its own mechanic this port
    tracks no state for anywhere yet, so (like an unrecognized `Affected$` value already does) the line is skipped
    rather than treated as met (GO-7). Winter, Misanthropic Guide's own `Condition$ Delirium | SetMaxHandSize$ Y` (the
    sole real line pairing a now-resolvable `Condition$` with Layer 8) applies: `Y` is `Number$7/Minus.X` over a
    `$CardTypes` count, both of which `resolveAmount` evaluates.

28. Combat (`combat/`), mana payment (`mana/`), mulligans (`mulligan/`). **Combat further along than "everything but
    static abilities"** (`combat.go`, `attack.go`, `block.go`, `combatdamage.go`, `staticability.go`) — first strike,
    trample, gang blocking, attacking a planeswalker/Battle, a combat split across more than one defending player at
    once (CR 506.4), and block legality's `CantBlockBy` (CR 509.1b): flying/reach, Fear, Horsemanship and Intimidate
    (keyword-synthesized the same way `CardFactoryUtil.java` builds them, `cantBlockByKeywords`); Landwalk and
    Protection separately, each one's own restriction being the keyword's OWN per-card argument rather than a name every
    carrier shares; and every literal `S:Mode$ CantBlockBy` line, walked across every battlefield permanent as a
    possible source, not just the attacker's own card; Menace too (`minMaxBlockers`, a per-attacker blocker count
    `validateBlocks` checks on the whole declaration, hardcoded the same way Forge's own `getMinMaxBlocker` is — not
    routed through `CantBlockBy` at all). Intimidate's own `ValidBlocker$ Creature.nonArtifact+!SharesColorWith` needed
    a new `SharesColorWith` valid-string property (`valid.go`, `c.Colors().HasAny(sourceCard.Colors())`) — left unbuilt
    earlier because the generic `non<Type>` fallthrough it would otherwise reach reads it as a nonexistent type and the
    leading `!` then negates that to an actively wrong "matches everything," not an absent property. Landwalk's own
    `ValidDefender$ Player.controls<Type>` needed a new `matchesValidDefender` (`staticability.go`): a `Player`, not a
    `Card`, matched the same way `SpellCast`'s own `ValidActivatingPlayer` is (item 26) — `You`/`Opponent`/`Player` bare
    forms plus a `"controls<Type>"` battlefield scan, `landwalkType` reading the type argument straight off the keyword
    line (`enchantSpec`'s own precedent). Protection's own restriction (`protectionEach`, staticability.go) needed no
    new property at all: both real corpus shapes — the natural-language "Protection from red" and the colon-structured
    "Protection:Artifact" — resolve through `Matches`/`baseMatches` exactly as written once
    `Protection.getProtectionValid`'s own two branches are reproduced, `keyword.Parse`'s existing space-vs-colon split
    telling them apart. Skulk (`ValidBlocker$ Creature.powerGTX`) closes block legality's last gap: Java's own hardcoded
    `X` (`Count$CardPower` against the ability's own host, always the attacker since `ValidAttacker$` is fixed to
    `Creature.Self`) turns out to be a constant "the attacker's own power" question once read precisely, not a
    `Compare`/SVar one at all — `skulkBlocks` (staticability.go) is a direct `Power()` comparison, the same hardcoded
    shape Menace's own blocker count has. **Block legality's `CantBlockBy` has no remaining gap.** **Combat declarations
    are validated** (ADR-0024, `attackconstraints.go`, `blockvalidation.go`): Java's `AttackConstraints` and
    `validateBlocks`, an illegal declaration an `*IllegalDeclarationError` — except a blocker declaration an
    `Event$ DeclareBlocker` replacement (Camouflage) makes, repaired by Java's steady-state loop instead (ADR-0035).
    **Mulligans done** (`mulligan.go`) — London, free mulligans, tucking. **Mana payment done** (`mana.go`,
    `manapay.go`): a `Pool` per player (twelve buckets — six colors/colorless, each split plain/snow),
    `Pay`/`PayWithSnow` for the plain colored-and-generic case plus snow (a same-color pip or generic unit falls back to
    the snow bucket once the plain one is empty, CR 106.3a; a snow ({S}) symbol spends only the snow bucket, never the
    plain one), CR 500.4's emptying every phase/step, and `PayManaCost` resolving `{X}` via `ChoosePayX` (asked once per
    cost regardless of how many `{X}` symbols it carries, CR 107.3f), snow via `ChoosePaySnow` (asked once per `{S}`
    symbol independently — unlike `{X}`, two can take two different colors), a two-color hybrid shard via
    `ChooseHybridManaColor`, a monocolored hybrid shard via `ChoosePayMonocoloredHybrid`, a colorless hybrid shard via
    `ChoosePayColorlessHybrid`, a single-color Phyrexian shard via `ChoosePayPhyrexian`, a hybrid Phyrexian shard via
    `ChoosePayHybridPhyrexian`, and each unit of a cost's generic amount via `ChoosePayGeneric` — all eight harder
    shapes this port set out to resolve are resolved. A basic land's own intrinsic mana ability (CR 305.6) is:
    `TapLandForMana` (`manaability.go`), `Pool.Add`'s first real (non-test) caller, snow-aware (a land carrying the Snow
    supertype produces snow mana, CR 106.3a) — any other mana ability (a nonbasic land, a creature, an artifact) still
    needs the M6 effect-dispatch machinery this one deliberately bypasses, since CR 305.6's ability is a fixed rule
    keyed off the type line, not script text. **Playing a land done** (`land.go`): `Game.PlayLand`, CR 305 — not casting
    a spell, so no cost and no stack; sorcery-speed timing (CR 305.3) collapsed to active player, a main phase, empty
    stack; CR 305.2's one-per-turn limit via new `Player.LandsPlayed`/`LandsPlayedLastTurn` fields, reset for every
    player each turn by `cleanupStep`. The first card this port moves from hand to the battlefield through a real game
    action rather than `setup.state` placing it there directly.
29. Scenario-parity harness (Layer 2) + ≥300 fixtures. **Fixture count met, coverage still bounded by M5 itself** — the
    harness runs (`TestScenarios`, `testdata/scenarios/`), and 342 fixtures exist today, past the ≥300 floor: combat and
    mana-payment breadth across the real corpus (single-block trades, Vigilance/Haste/First Strike/ Deathtouch/Trample
    against fresh cards, every mana-payment hybrid and Phyrexian branch, every basic land color, casting each permanent
    type including an Aura), on top of the earlier turn-structure/SBA/mulligan set. **Exit gate:** P4 gate — scenario
    suite green (met) on ≥300 fixtures covering every step transition, every layer, every SBA (Plan Section 3.2). The
    count and the step-transition/SBA breadth are met; "every layer" is not — only Layer 7b/7c's own plain-integer
    subset (`applyContinuousPT`, item 27) has a scenario fixture exercising it
    (`equipment-falls-off-without-destroying`, via Sword of Body and Mind's real `AddPower$`/`AddToughness$`); Layer 4's
    own literal-token subset, Layer 5's own and Layer 6's own (`applyContinuousType`/`applyContinuousColor`/
    `applyContinuousKeyword`, item 27) are proven only at the Go module level (`continuous_test.go`), not yet by a
    scenario fixture, and replacement effects and every trigger mode but "enters"/"dies"/"attacks"/"blocks"/"deals
    damage"/"is discarded"/"becomes tapped"/"taps for mana"/"casts a spell" remain gaps too. **Partially reached** —
    blocked on the rest of M5 landing, not on writing more fixtures.

### M6 — Effects, corpus-gated — 6–12 wks _(parallelizable; the long tail)_

**In progress.** 186 of the corpus's 203 script-driven `Effect` APIs resolve (`Draw`, `DealDamage`, `GainLife`, `Pump`,
`PumpAll`, `LoseLife`, `PutCounter`, `Discard`, `Scry`, `Surveil`, `Sacrifice`, `SacrificeAll`, `Destroy`, `Tap`,
`Untap`, `Fight`, `Mill`, `RemoveCounter`, `DamageAll`, `SetLife`, `Shuffle`, `ExchangeLife`, `TapAll`, `UntapAll`,
`PutCounterAll`, `RemoveCounterAll`, `MultiplyCounter`, `Mana`, `MoveCounter`, `Poison`, `Unattach`, `RevealHand`,
`LosesGame`, `WinsGame`, `Radiation`, `RemoveFromCombat`, `Connive`, `Cleanup`, `DestroyAll`, `ChooseCard`,
`ChoosePlayer`, `ChooseColor`, `ChooseNumber`, `Reveal`, `PeekAndReveal`, `TapOrUntap`, `Proliferate`, `ChangeZone`,
`ChangeZoneAll`, `Dig`, `DigUntil`, `RearrangeTopOfLibrary`, `Explore`, `LookAt`, `Branch`, `GenericChoice`, `Repeat`,
`RepeatEach`, `Regenerate`, `Fog`, `AddTurn`, `SkipTurn`, `GainControl`, `ExchangeControl`, `HealDamage`, `EachDamage`,
`DrainMana`, `Token`, `Investigate`, `Amass`, `Incubate`, `Animate`, `AnimateAll`, `Debuff`, `Protection`,
`ProtectionAll`, `DelayedTrigger`, `ImmediateTrigger`, `Charm`, `FlipCoin`, `RollDice`, `Clash`, `Seek`, `StoreSVar`,
`Balance`, `AddPhase`, `SkipPhase`, `BlankLine`, `GameDrawn`, `RemoveFromGame`, `ReverseTurnOrder`, `ChangeSpeed`,
`GainOwnership`, `ReorderZone`, `EndTurn`, `EndCombatPhase`, `ChooseEvenOdd`, `ChooseDirection`, `ExchangeLifeVariant`,
`ExchangePower`, `TapOrUntapAll`, `AddOrRemoveCounter`, `BecomesBlocked`, `Block`, `ChangeCombatants`,
`GainControlVariant`, `Detain`, `Intensify`, `Blight`, `TimeTravel`, `Endure`, `AssignGroup`, `VillainousChoice`,
`TwoPiles`, `ChooseType`, `NameCard`, `PreventDamage`, `DigMultiple`, `Recruit`, `BidLife`, `ExchangeControlVariant`,
`DayTime`, `AlterAttribute`, `Vote`, `MakeCard`, `Learn`, `CopyPermanent`, `Counter`, `Manifest`, `Cloak`,
`ManifestDread`, `SetState`, `Goad`, `RemoveFromMatch`, `ActivateAbility`, `MultiplePiles`, `DamageResolve`,
`ChooseSource`, `Empower`, `Earthbend`, `Airbend`, `Discover`, `Draft`, `Heist`, `ExchangeZone`, `Effect`,
`ReplaceEffect`, `ReplaceDamage`, `ReplaceSplitDamage`, `ReplaceToken`, `ReplaceCounter`, `ReplaceMana`, `Clone`,
`BecomeMonarch`, `TakeInitiative`, `Venture`, `OpenAttraction`, `AssembleContraption`, `Regeneration`, `Play`,
`CopySpellAbility`, `RingTemptsYou`, `Abandon`, `ChangeTargets`, `MustBlock`, `Phases`, `ManaReflected`, `ControlSpell`,
`Planeswalk`, `ChaosEnsues`, `RollPlanarDice`, `RunChaos`, `ChooseSector`, `SetInMotion`, `Subgame`, `UnlockDoor`,
`SwitchBlock`, `LosePerpetual`, `Meld`, `ControlPlayer`, `RestartGame`, `Camouflage`) — see
`docs/crucible/porting/port-log/game-state.md` for the per-API landing notes; items 30-32 below stay in their original
plan-authoring voice (forward-looking, not yet rewritten as a per-item retrospective the way M0-M5 are).

30. Implement APIs in corpus-first, then frequency order (Section 1.5). Keywords, triggers, replacements, cost parts
    alongside.
31. Three scenarios minimum per API. Parity matrix updated continuously.
32. Replay-parity harness (Layer 3) stood up as soon as full games run at all — do not defer this to the end. Full games
    run now (`Game.Run`, ADR-0026) with a scripted controller; the harness itself is not started. **Exit gate:** P5 gate
    — 100% corpus coverage for the active gauntlet; replay parity green over the nightly log corpus.
