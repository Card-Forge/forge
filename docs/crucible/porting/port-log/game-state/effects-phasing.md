# Phasing: the ADR-0021 primitive, `Phases` and `K:Phasing`

## Phasing primitive lands (ADR-0021)

CR 702.26b: a phased-out permanent is treated as though it does not exist, yet stays in the battlefield zone. ADR-0021
decided where that lives; this section records what was built.

| Piece | Where | Java |
| --- | --- | --- |
| Filtered enumeration | `Zone.Cards` (`zone.go`) | `PlayerZoneBattlefield.getCards(true)`, `PlayerZoneBattlefield.java:76-100` |
| Opt-in full battlefield | `Zone.CardsIncludingPhasedOut` | `Game.getCardsIncludePhasingIn`, `Game.java:618` |
| Card state | `Card.phasedOut`/`directlyPhasedOut`/`wontPhaseInNormal` | `Card.phasedOut`, `directlyPhasedOut`, `wontPhaseInNormal`, `:252-254` |
| Only writer | `Game.setPhasedOut` (`game.go`) | `Card.setPhasedOut`, `:5581-5585` |
| Phase operation | `Game.phase`/`switchPhaseState` (`phasing.go`) | `Card.phase`/`switchPhaseState`, `:5589-5675` |
| Untap-step phasing | `untapStepPhasing` | `Untap.doPhasing`, `Untap.java:198-249` |
| Event | `Phased`, `Detail` = `PhaseDetailOut`/`PhaseDetailIn` | `GameEventCardPhased` |
| Fixture key | `PhasedOut:P<seat>` (`internal/fixture`) | `GameState.java:309-311`, `:1302-1304` |

**Storage.** `Zone` cannot see the card arena, so it keeps its own phased-out subset (`Zone.phasedOut`, an `OrderedSet`,
nil until something phases out). `Cards` returns the zone's own slice when the subset is empty -- every zone but the
battlefield, and the battlefield nearly always -- and a fresh slice in the same relative order otherwise (GO-12). `Len`
and `Contains` stay unfiltered: Java's `Zone.size()` reads the full list, and a phased-out permanent never left the zone
(CR 702.26d). `Game.setPhasedOut` writes the card field and the zone subset together; `Move` and `MoveToLibraryTop` drop
a card from the subset as it leaves (`Zone.remove`) and clear the card fields after the LKI snapshot, so last-known
information keeps `IsPhasedOut` (`GameAction.java:977` reads it). `Game.Clone` copies the subset only when non-empty
(`Zone.clone`); card fields ride the struct copy.

**Opt-in callers.** Each cites the Java opt-in it mirrors (ADR-0021 decision 2):

| Caller | Java |
| --- | --- |
| `untapStepPhasing` | `Untap.java:202` |
| `phasesValidCards` under `PhaseInOrOut$` | `PhasesEffect.java:47-48` |
| `cleanupStep` damage and resets | `PhaseHandler.java:400`, `Game.java:1236` |
| `fixture.dumpZone` | `GameState.java:182`, `:223` |
| `compareZoneCards` (scenario harness) | same fixture opt-in, so a phased-out card is compared, not skipped |

**The operation.** Phasing is never `Move`: no `ZoneChanged`, no ETB/LTB trigger. Phasing out records the controller
whose untap step returns it (`Card.java:5645`) and removes it from combat (`:5647-5650`); phasing in unattaches it from
a host no longer on the battlefield (CR 702.26g, `:5652-5665`). Attachments follow with `direct` false when their
phasing matches the host's before the flip, unless one that is phased in cannot phase out (`:5602-5611`).
`directlyPhasedOut` is set only on phasing out.

**Triggers.** Java holds every `PhaseIn`/`PhaseOut` trigger (`runTrigger(..., true)`) and collects it after the
operation against the trigger set active before it began; `ValidCard$` is read after the flip, which is how
`Card.phasedOutSelf` matches its own host. `phasing` records that set (`beginPhasing`) and the events in order;
`endPhasing` fires `PhaseOutAll` first (run at once in Java, `PhasesEffect.java:123`, `Untap.java:241`), then each held
event -- `PhaseOut` against the pre-operation hosts, `PhaseIn` against those plus every card that phased in
(`registerActiveTrigger`, `Card.java:5668`). All seven corpus lines (`Mode$ PhaseIn` 4, `PhaseOut` 2, `PhaseOutAll` 1)
resolve.

**`Matches`.** `CardProperty.java:50-56`: a phased-out card has no property unless the property starts with `phasedOut`,
which is stripped (`phasedOutSelf` reads `Self`). The base type is not a property, so a bare `Creature` still matches
(`Card.java:5751-5763`), and a negated property reads true (`hasProperty`'s negation) -- both reproduced (PORT-7). A
phased-in card reads `phasedOut*` false and `phasedIn*` true (`:1051-1057`). Three properties the corpus's phasing lines
need were added with it: `Permanent` (`:90`), `token` (`:1336`, exact form) and `EffectSource` (`:424`, the card that
created an effect card).

**Targets (CR 608.2b).** A chosen target is a per-card reference, not an enumeration. `dropIllegalTargets`
(`targeting.go`) is `MagicStack.hasFizzled` (`MagicStack.java:704-752`); a card target that phased out after it was
chosen is one of the causes it re-checks, since `canBeTargetedBy` refuses it (`Card.java:6829-6831`). Such targets are
removed from the ability's `Targets` and from each Charm mode's; the ability fizzles when at least one target was chosen
and none is left, unless it or a chosen mode names `CantFizzle$`. `auraTargetStillLegal` refuses a phased-out host.
Guardian of Faith or Slip Out the Back in response to removal now counters the removal.

**By-ID re-adders.** `applyContinuousPT` and its siblings clear layer mods over the filtered enumeration, so a
phased-out card is never cleared. `applyPumpEffects` and `applyAnimateEffects` re-add by `CardID`; both skip a
phased-out card, or a `Duration$ Permanent` pump would stack one more copy every state-based pass. The record is kept
and applies again once the card phases in, as Java's boost stored on the card does. A phased-out card's static-derived
mods stay as they were when it phased out -- hidden while it is out, rebuilt by the next pass after it phases in.

### `c.Zone == Battlefield` sites (ADR-0021 decision 3)

Guarded here, each citing the Java line it mirrors:

| Go site | Java guard |
| --- | --- |
| `canBeDestroyed` (`destroyeffect.go`) | `Card.canBeDestroyed`, `Card.java:6816` |
| `sacrificeEffect` self, `sacrificeCards` | `Card.canBeSacrificedBy`, `:6909` |
| `changeZoneKnown` (`changezoneeffect.go`) | `ChangeZoneEffect.java:557` |
| `tapEffect` | `TapEffect.java:56` |
| `untapEffect` | `UntapEffect.java:57` |
| `animateCards` (`animate.go`) | `AnimateEffect.java:168`, `:176` |
| `applyPumpEffects`, `applyAnimateEffects` | see "By-ID re-adders" |
| `auraTargetStillLegal`, `targetStillLegal` | `Card.canBeTargetedBy`, `:6829` |

Not guarded yet. A targeted reference is covered by `targetStillLegal`; these matter only for a `Defined$` that names a
phased-out card without targeting it (`Remembered`, `Self`, `Enchanted`, ...):

| Java guard | Go site |
| --- | --- |
| `canReceiveCounters`/`canRemoveCounters`, `Card.java:1719`, `:1730` | `putcountereffect.go`, `removecountereffect.go` |
| `changeCardState`, `Card.java:664` | `setstateeffect.go` |
| `PumpEffect.java:444`, `DebuffEffect.java:75`, `ProtectEffect.java:150` | `pumpeffect.go` (Debuff/Protection share `animateCards`, guarded) |
| `ControlGainEffect.java:138`, `ControlExchangeEffect.java:76` | `gaincontroleffect.go`, `exchangecontroleffect.go` |
| `CloneEffect.java:139`, `AlterAttributeEffect.java:50`, `AirbendEffect.java:60` | `cloneeffect.go`, `alterattributeeffect.go`, `airbendeffect.go` |
| `DamageDealEffect.java:254`, `DamageEachEffect.java:98`, `FightEffect.java:109` | `dealdamageeffect.go`, `eachdamageeffect.go`, `fighteffect.go` |
| `MustBlockEffect.java:72` | not ported (ADR-0024) |
| `CombatUtil.java:208` (can attack), `:474` (can block) | `attack.go`, `block.go` -- declarations read the filtered enumeration; a scripted controller naming a phased-out creature is not refused |
| `SpellAbilityRestriction.java:548` (activate) | `activateability.go`, `activatemanaability.go` |
| `ChangeZoneEffect.java:1006` (hidden origin) | `changeZoneHidden` reads filtered zones |
| `SpellAbilityEffect.java:1049`, `:1060` (`AsLongAsControl`, tapped durations) | durations not ported |
| `StaticAbility.java:341`, `TriggerReplacementBase.java:62` (host phased out) | covered: `traitHosts` and the replacement walks read the filtered enumeration |

## `Phases` and `K:Phasing` land

`phasesEffect` (`phaseseffect.go`) is `PhasesEffect.resolve`. It resolves every corpus param:

| Param | Behavior |
| --- | --- |
| `AllValid$` | every battlefield permanent matching; phased-out ones too under `PhaseInOrOut$` |
| `Defined$` | `getDefinedCardsOrTargeted`: `Defined$` first (each `" & "` part; `Valid <spec>` read locally, as `cloneeffect.go` does), else targets, else `Self` |
| `ValidTgts$` | the chosen targets |
| `PhaseInOrOut$` | each permanent phases the other way; directions decided before any phases (`PhasesEffect.java:63-73`) |
| `WontPhaseInNormal$` | the untap step does not phase it back in (`Card.switchPhaseState`'s first check) |
| `Tapped$`/`Untapped$` | set as `PhaseInOrOut$` phases a permanent in, no tap/untap trigger |
| `AnyNumber$` | `ChooseCardsForEffect`, 0 to all |
| `RememberAffected$`/`RememberValids$` | remembered on the host |

`TargetedPlayerCtrl` (`CardProperty.java:277-280`, 4 corpus lines) needs the ability's targets, which `Matches` does not
see; `phasesValidCards` answers it from `Ability.Targets`, phased-in cards only. Negated, it is rejected
(`not resolvable yet`). A `Defined$` value `definedCards` does not know is an error, as elsewhere: the corpus's
`TriggeredTargetLKICopy` (a `Mode$ BecomesTarget` trigger) and `TriggeredAttackerLKICopy`
(`Mode$ AttackerBlockedByCreature`), 1 line each, fail loudly -- neither mode records that object for `Defined$` yet.
`TriggeredBlockerLKICopy` (the same card's blocker half) resolves: `Mode$ AttackerBlockedByCreature` records the blocker
(`triggeredObjects.blocker`). `DelayTriggerRememberedLKI`, `ChosenCard`, `Enchanted`, `Equipped` resolve. A permanent no
longer on the battlefield is skipped -- Java's `equalsWithGameTimestamp` check less its timestamp half, since a `CardID`
survives a zone change (ADR-0009).

**`K:Phasing` (13 cards).** `untapStepPhasing` runs first in the untap step, before day/night and untapping
(`Untap.executeAt`, `Untap.java:69-77`): every permanent the active player phased out directly phases in, every
`K:Phasing` permanent they control phases out (or in, if already out on its own), CR 702.26h's attachment case phasing
out indirectly with its host. `WontPhaseInNormal$` and a `CantPhaseIn`/`CantPhaseOut` static leave a permanent where it
is. A permanent that phases in untaps with the rest. Java re-derives statics between phasing and untapping
(`checkStaticAbilities`); here continuous effects are rebuilt by the pass after the step, so a permanent that just
phased in untaps under the characteristics it phased out with.

**`CantPhaseIn`/`CantPhaseOut` (7 lines).** `cantPhase` is `StaticAbilityCantPhase`: a static of that mode on a trait
host whose `IsPresent$` holds (`isPresentMatches`, the only condition the corpus lines carry) and whose `ValidCard$`
matches. `the_pandorica.txt` reads `IsPresent$ Card.EffectSource+tapped`, which is why `EffectSource` landed with it.

**`ForgetOnPhasedIn$` (6 lines).** `Effect` no longer rejects it. `effectCardsSeePhaseIn` is
`addForgetOnPhasedInTrigger`: an effect card remembering a card that phases in forgets it, and is exiled once it
remembers no card. No effect is created when nothing is remembered (`EffectEffect.java:94`).

**`ChangeZone` `Origin$ Command`.** Oubliette, Out of Time and The Moment end their return chain with
`DB$ ChangeZone | Origin$ Command | Destination$ Exile | Defined$ Self` -- the effect exiling itself, one of ~180 corpus
lines of that shape. `ChangeZone` refused `Origin$ Command`, so the chain failed after phasing the creature back in.
`changeZoneKnown` now resolves it for an effect card going to exile: `GameAction.changeZone`'s immutable branch
(`GameAction.java:100-106`) removes the card from the Command zone with no zone-change event or trigger, which is
`exileEffect`. Any other destination, or a non-effect card in the Command zone (a commander), is still rejected with
`not resolvable yet`, checked before any card moves. `TestOublietteReturnsItsCreatureTapped` runs `oubliette.txt` from
the corpus end to end.

**Blocked elsewhere.** Teferi's Protection and Perch Protection reach `DB$ Phases` only through
`DB$ Pump | Defined$ You | Duration$ UntilYourNextTurn | KW$ Protection from everything`, which `Pump` rejects
(`Duration$ "UntilYourNextTurn" not resolvable yet`): the chain fails loudly before phasing. Player keywords and that
duration are `Pump`'s gap, not `Phases`'.

**Tests.** `phasing_test.go` (engine) and `phasing_test.go` (fixture) cover each branch; scenario
`phasing-untap-step-phases-in-and-out` proves the untap step through the fixture harness: `K:Phasing` phases out, a
permanent phased out for the active player phases in and untaps, one phased out for the other player stays out, order
kept.

### Not modeled

| Java | Why |
| --- | --- |
| `clearEncodedCards` | cipher not ported |
| `Combat.saveLKI` | combat LKI not ported |
| Phasing in unattaches from a player no longer in the game | this port attaches only to cards |
| `PhasesEffect.java:75-79` timestamp half | `CardID` survives a zone change (ADR-0009) |
