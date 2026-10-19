# Port Log — Quirks and deviations

- **Parent:** [`game-state.md`](game-state.md) — `internal/engine`'s Java counterpart and `Not ported yet`
- **Go:** [`internal/engine`](../../../../crucible/internal/engine)

Every place the engine knowingly matches a Java oddity (PORT-7) or knowingly differs from Java. Reason: a scenario or
replay that disagrees with the oracle is either one of these rows or a bug; nothing else explains it. A Forge defect
itself lives in [`../forge-java-defects.md`](../forge-java-defects.md) or
[`../card-script-defects.md`](../card-script-defects.md); a row here only says what the engine does about it.

New quirk or deviation → one row here, same commit (DOC-12). Landed work beyond that is recorded in code comments
(`Ported from <File>.java`), tests and `Not ported yet`, not in a narrative note.

## Reproduced from Java (PORT-7)

| Behavior | Java | Go |
| --- | --- | --- |
| FlipOntoBattlefield: a spot at index 0 with a right neighbor takes the right neighbor ("leftmost" quirk) | `FlipOntoBattlefieldEffect.java:125-129` | `flipNeighbor`, `flipontobattlefieldeffect.go` |
| PeekAndReveal clamps `numPeek` to each library in place: a short library shrinks the peek for every later player | `PeekAndRevealEffect` | `peekandrevealeffect.go` |
| Dig moves picks in reverse pick order before owner ordering | `Collections.reverse` in `DigEffect` | `digeffect.go` |
| DigUntil "sequential" mode: found and revealed destinations in one zone move every revealed card in reveal order | `DigUntilEffect` | `diguntileffect.go` |
| ChooseSource: the resolving ability counts as the first stack item, so an `SP$ ChooseSource` offers its own card | `MagicStack.java:572` | `choosesourceeffect.go` |
| ChooseSource with several choosers: only the last pick stays chosen; `RememberChosen$` keeps all | `setChosenCards` (`:137`) | `choosesourceeffect.go` |
| Camouflage reads every attacker in the game, not only the defender's (differs from printed text in 3+ players) | `combat.getAttackers()` | `camouflageeffect.go`, ADR-0035 |
| ChangeTargets with no target at all: resolution ends | `ChangeTargetsEffect` `return` | `changetargetseffect.go` |
| A lost player passes the initiative, then takes it back (no `return` after the recursive call) | `GameAction.java:2568-2573` | `takeinitiativeeffect.go` |
| Block validation: the free-blocker loop takes every able free blocker on its first pass and consumes them; lure lines match by prefix in one check, exactly in the other | `validateBlocks`, `hasStartOfKeyword` vs `hasKeyword` | `validateBlocks`, `attackerLureSatisfied`, `blockvalidation.go`; ADR-0024 |
| A phased-out card has no property unless it starts with `phasedOut`; a bare base type still matches; a negated property reads true | `CardProperty.java:50-56`, `Card.java:5751-5763` | `valid.go` |
| RestartGame clears five phase command lists, never begin-of-combat's: a pending `Combat$` control grant survives | `RestartGameEffect.java:47-51` | `resetForRestart`, `restartgameeffect.go` |
| RestartGame resets extra phases, extra turns and turn number only; the upkeep count runs on | `PhaseHandler.java:826-830` | `restartgameeffect.go` |
| Text change regex: left `\b` only (`Red` rewrites `Reduce`), `non` prefix kept, nothing within 100 characters after `named`, color words lower case then as written, type words case sensitive, SVar/sub-ability names untouched, `LockInText$` locks | `AbilityUtils.java:3090`, `ChangeTextEffect.java:29` | `textwords.go`, `continuous.go` |
| Layer 8 builds after Layer 7 in a pass: a CDA reading a hidden keyword grant sees the previous pass's | `StaticAbilityContinuous` order | `continuousStaticsAllZones`, `continuouslayers.go` |
| `CheckSecondSVar$` read only beside `CheckSVar$`; `CharacteristicDefining$` tested for presence, not value | `StaticAbility.java:470-472` | `continuous.go` |
| Unchosen `ChosenType`/`ChosenColor` token dropped, not the line; `SetColor$ ChosenColor` unchosen adds no color effect | `removeIf` in `StaticAbilityContinuous` | `continuous.go` |
| `Remove*Types$` apply only when `AddType$` is absent or kept a token; `RemoveType$ ChosenType` dropped when unchosen, never substituted | `StaticAbilityContinuous.java:425-426` | `continuous.go` |
| Goad never checks host validity: a host that already left leaves the goad in place (Pump and Animate check) | `PumpEffect.java:271`, `AnimateEffect.java:34`, `GoadEffect` | `registerHostBoundEnd`, `goadeffect.go` |
| Soulbond: leaving frees the partner, but a token keeps its own `pairedWith` | `!c.isRealToken()` | `card.go` |
| `PlayLand` counts the land drop and fires `LandPlayed` whether or not the land entered | `Player.java:1645-1650` | `PlayLand`, `game.go` |
| `Count$Presence_<Type>` counts the battlefield as the amount is read, not as the spell was cast | `AbilityUtils.java:2180-2192` | `presenceCount`, `amountheads.go` |
| `MayFlashSac` sacrifices by card id: a countered and reanimated spell's new object goes too | `SacrificeAllEffect.java:66` | `castoptions.go` |
| `PumpKeywords$` without `PumpDuration$` on a token copy: the keyword outlives the copy (Loose in the Park) | `TokenEffectBase.java:272` | clone path, `TestClonePumpKeywordsWithoutDurationStayPastTheCopy` |
| Dragon's Fire, Monstrous Emergence read the chosen or revealed card's power as the cost paid it | `CostPartWithList.java:136-138` | cost LKI list; `check-card` to confirm against rulings |
| X-named SVar attack cost: Java appends an X shard paid as 0; Go drops the shard, same amount | `CombatUtil.java:266` | `attackcost.go` |
| `RememberingAttacker$` also forgets an attacker the host remembered before | `removeRemembered` | `Memory.Forget`, `attackcost.go` |
| Zero attack cost still asks `ConfirmPayCost` and still skips the pair in `legalAttack` | non-null `Cost` | `attackcost.go` |
| Attack costs already paid are not refunded when the declaration turns illegal | `PhaseHandler.java:577` | `attackcost.go` |
| `ExcessSVar$` unset (ability returned before `dealDamage`) reads 0 | `AbilityUtils.java:415-423` | `defaultExcessSVar`, `excessdamage.go` |
| An entry replaced after enters-prepared keeps the copy castable | leaves-play command only | `castspell.go` |
| Library of Leng, Nephalia Academy shuffle before topdecking (scripts lack `NoShuffle$`) | `ChangeZoneEffect.java:1259` | `changezoneeffect.go` |
| `Defined$ Registered` keeps lost players | no filter | `defined.go` |
| `Count$MaxCombatDamageThisTurn`/`MaxOppDamageThisTurn` read before the `DamageThisTurn` branch | `Player.java:822-831` | `amountheads.go` |
| Target re-check shares one fizzle flag across the chain: one legal target on any link keeps the ability | `MagicStack` resolve | `chaintargets.go` |
| `Surveil` fires with an empty library | `Player.java:1083` | `control.go` |
| Param values never read where Java reads presence only: timing restrictions, `FirstCombat$`, `PlayerTurn$`/`NotPlayerTurn$` | `hasParam` | `turn.go`, `trigger.go` |
| `AddType$ Food,Blood,...` with commas is one subtype named with the commas (Phyrexian Adapter) | `CardType.add` | `continuous.go` |

| `ThisTurnEntered_` splits at the first five underscores, so a valid string holding an underscore is cut short | `AbilityUtils.java:2806` | per-turn ledgers, `valid.go` |
| Activation host snapshot: a cost that moved the card leaves the old object, whose zone still names the zone it left (Laboratory Drudge's `inZoneGraveyard` reads true for a card exiled from the graveyard as its cost) | `Zone.remove` keeps the zone field | activation records, `activateability.go` |
| Hollow Marauder's `Minus.Remembered$Valid ...` subtracts nothing: the operator splits on `.` into three parts | `AbilityUtils.java:3278-3287` | `amount.go` |
## Deviations from Java

| Deviation | Java | Why |
| --- | --- | --- |
| Params Java reads silently or ignores (an unread value, an `Origin` on a known-origin move, a missing trigger key read as empty, `ReplaceDyingCondition$` other than `Kicked`, a second chosen color) are refused with an error | silent default | GO-7: one refusal per card beats a silent wrong answer |
| Collections Java keeps in hash order (`Vote` tally and `EachVote$`, `MultiplePiles` piles, `DigMultiple` categories, `ChooseType` subtypes) use the options' own order, subtypes sorted; `AtRandom$` over such a list is refused | `HashMap`/`HashSet` order | Java's order is undefined |
| `MayPlay$`: two live grants on one card that differ in cost or timing, or a cost-changing grant on a card also castable from hand (Omniscience, Fires of Invention): the cast declines with a pending error and the game stops | the player picks the spell side | No `ChooseMayPlayOption` decision yet; picking for the player would be a guess |
| `ValidAfterStack$ Spell.<props>` checked as the grant is built | checked after the spell is on the stack, cast rolled back | Differs only for a mana value reading X; `continuous.go` |
| Activation pays mana, then tap, then the irreversible parts (sacrifice) last | part-by-part `CostPayment` loop with undo | CR 601.2h allows any order; a failed mana payment never sacrifices for nothing; `activateability.go` |
| `Optional$` Sacrifice (not Devour) sacrifices exactly min(`Amount$`, candidates) once confirmed | 0 to `Amount$` | `sacrificeeffect.go` |
| Unblocked-attacker triggers run last in `DeclareCombatBlockers` | first (`Combat.fireTriggersForUnblockedAttackers`, `PhaseHandler.java:746`) | `block.go` |
| `AttackedTarget$` filter evaluated once as the trigger fires | lazy `Iterable`, re-run on every read | Differs only if the answer changes before resolution (an Aura moved under `Player.EnchantedBy`) |
| Joining combat before the move on the known path: prompt order differs, trigger answers match | deferred check | `changezoneeffect.go` |
| `Secretly$` choices (ChooseCard, GenericChoice, ChoosePlayer, ChooseType) are open choices | hidden display only | No rules difference |
| `Duration$ UntilHostLeavesPlay`: the host's leaves trigger and the returned card's enters trigger may stack in a different order | same commands, different hook | `changezoneeffect.go`, `controlcommands.go` |
| A command firing with no `PlayerController` at hand (zone change, phase out, end of combat) leaves the zone correction to the next SBA pass; only the moment the permanent changes lists differs | `GameAction.java:1205-1207` | `controlcommands.go` |
| Two `SetMaxHandSize$` effects on one player fold in timestamp order | order not pinned in Java | Deliberate choice; `player.go` |
| `MayPlaySnowIgnoreColor$`: the snow source that pays is picked here, colorless first, then WUBRG | left to the payer | `mana.go` |
| Per-turn attack and `LifeGainedThisTurn` records reset at cleanup for every player | `clearAttackedMyTurn` caller not traced | An attack made on an opponent's turn drops at cleanup too |
| Counters placed outside `addCardCounters` (loyalty on entry, `MoveCounter`, replacement-modified placements) are not logged in `CountersAddedThisTurn`; placements writing `Counters.Add` directly (entering with counters, loyalty, defense, `Proliferate`, move/double) ignore `CantPutCounter` | every placement | Gap; `replacement.go`, `staticability.go` |
| Sneaked creature whose defender left: resolution error before it moves | keeps it attacking the CR 506.4c `<Nothing>` card | Unmodelled; `sneak.go` |
| Animate Dead after the host's `ClearRemembered$`: the delayed trigger's own `RememberObjects$` stands in for the host's last-known remembered list | host's LKI remembered list | No remembered-list LKI; `delayedtrigger.go` |
| `Defined$ Remembered` after `RememberLKI$` (Absorb Identity) copies the current values, not an LKI snapshot | LKI copy | Differs only when the remembered permanent was itself a copy; `cloneeffect.go` |
| A `SpellCast` trigger with no `TriggerZones$` on a card in hand, library or graveyard watching other spells is not scanned | scanned | Real scripts name `TriggerZones$ Battlefield`; `appendSpellCastMatches`, `trigger.go` |
| `ReturnFromExile`'s enters trigger during RestartGame waits on the stack through the restart | collected in the waiting list, pushed at the first priority check | Same first window; `restartgameeffect.go` |
| CopySpellAbility `IgnoreFreeze$` is a no-op | freezes the stack | The engine never freezes the stack |
| ChangeX writes the stack item only | writes `tgtSA` and the host's cast SA | No `SpellAbilityStackInstance`; same value |
| Camouflage: every chosen creature leaves the pool | kept while `canBlockAny() \|\| canBlockAdditional() >= blockedCount` | Nothing grants either; `camouflageeffect.go` |
| A vote asks `ChooseNumber` only when an optional vote exists | always `chooseNumber(0, optionalVotes)` | A `[0, 0]` choice decides nothing; `voteeffect.go` |
| `TargetsWithSameController$`: the `ChooseTargets` answer is not re-validated per pick | checks each click | A split pair fizzles whole at resolution; `targeting.go` |
| FlipOntoBattlefield on a non-Aura enchantment spot: error | `FlipOntoBattlefieldEffect.java:109` sweeps every permanent | Forge defect; `flipCandidates` |
| ChaosEnsues `Defined$` naming two distinct planes: error | `TriggerChaosEnsues.java:43-48` fires neither | Suspected defect, no real card; `chaosEnsuesTriggerMatches` |
| An ending `ExchangeTextBox` drops its own rows, so earlier word changes return | `TextBoxExchangeEffect.java:74-76` leaves them wiped | Forge defect; `exchangetextboxeffect.go` |
| Controlled vote with `VotePlayer$ Other`: error | `VoteEffect.java:93-94` removes the wrong player | Forge defect; `voteeffect.go` |
| SwitchBlock stray block or re-block `ValidBlocked$` trigger: error before anything moves | `SwitchBlockEffect.java:22-25,88,97,103,151,157` | Forge defects; `checkStrayBlocks`, `checkReblockTriggerKeys` |
| UnlockDoor offers left then right | `Card.java:7991` `HashSet` order | No outcome depends on it; `unlockdooreffect.go` |
| Power Play with several owners shuffles in seat order | `HashSet` copy shuffled | Java order undefined; `match.go` |
| Opposition Agent's `FoundSearchingLibrary` line runs first; other `Moved` lines apply to the resulting move | one CR 616 walk | Every order exiles the card; library search replacement |
| A copy of a sneaked permanent spell is not sneaked; Sneak offered from the hand only, not under a `MayPlay$` grant | copied `AlternativeCost.Sneak` | No corpus card reaches it; `sneak.go` |
| Animate `Abilities$` grant applied before Layer 6 static grants and removals whatever the timestamps | `StaticAbilityContinuous.java:327-328` removes only older | `animateAbilities`, `animate.go` |
| `Scry` with `n == 0` skips the player | gates on `cause != null` only | `scryeffect.go` |
| Exert asked after attack costs, of attackers that paid | `PhaseHandler.java:564-578` folds Exert into the cost | Same outcome; `exertDeclaredAttackers` |
| Attack re-validation failure returns `*IllegalDeclarationError` | re-prompts in `do ... while` | ADR-0024 |
| Attack cost payment in declaration order | `Combat.getAttackers()` grouped by defender | Same order with one defender |
| Attack requirement re-validation reuses pre-tap constraints | `initConstraints` restriction sets | Rebuilding would make tapped attackers unable to attack |
| Nethergoyf pick short of four types exiles every valid card, no rollback | `HumanCostDecision.java:331-334` cancels the cast | Extra cost paid after mana; `payUnlessParts`, `collectEvidence` |
| An ability a moved card's new object activates reads the old battlefield object for `ExiledWithSource` | host object per `SpellAbility` | `Ability` has no host object; no real line affected; `battlefieldStamp` |
| `TriggeredDefender` reads the live combat | trigger-time defender | `defined.go` |
| SBA deaths after combat damage order in the next APNAP pass | one frozen-stack list (`TriggerHandler.java:264`, `MagicStack.java:599`) | Per-event batches (R9j) |
| A blocker blocking two attackers fires `Blocks` once per matching attacker | once total | Per-pair `checkBlocksTriggers` |
| Ward counter target kept off `Targets` (`wardCounters`) | `Defined$ TriggeredSourceSA` | Keeps becomes-target triggers and 608.2b re-check off the spell |
| Mana pools empty at the top of `beginPhase`; no mana burn | `PhaseHandler.onPhaseEnd` | Same cadence; mana burn left the rules in 2010; `emptyManaPools` |
| Top of library is zone index 0 | design choice, no Java fact | `turn.go` |
| `Planeswalk` `Optional$ True` resolves through `ConfirmEffect` | — | ADR-0029 listed it to reject; 2 real lines |
| `MayLookAt$` resolves to nothing | adds lookers | Engine is omniscient; `lookateffect.go` |
| Aura that loses every ability stays legal; several `Enchant` keywords read the first; a reanimated host keeps the Aura on the same ID, no second `Attached` | `Card.java:6979`, every keyword must hold, new object | `cleanupDanglingAttachments`, `enchantSpec` |
| A permanent under a copy effect cannot transform; Java still flips its backside flag, so the card shows its other face once the copy ends | `Card.changeCardState` | Not reproduced; `setstateeffect.go` |
| A chained sub-ability is never optional (`OptionalDecider$` on a `DB$` SVar) | sub-ability's own "may" | Gap, `resolveSubAbility` |
| A card in a graveyard watches an ETB or dies `ChangesZone` line only when the line names `TriggerZones$ Graveyard`; the engine never walks a graveyard host for a line without it | `TriggerReplacementBase.zonesCheck`, `TriggerReplacementBase.java:61-65` | `graveyardWatcher`, `trigger.go` |
| Non-battlefield zone changes (hand or library origin, any destination but the battlefield) fire `ChangesZone` lines from draw, mill, discard and `moveByEffect` only; a stack, exile or command origin fires nothing, and one mill or discard batch is pushed in one APNAP pass | `GameAction.java:153`, `:601-613`, `CardZoneTable` | Gap, `checkLeftZoneTriggers` |
| `ChangeZone` `WithCountersType$` to a destination other than the battlefield (known origin) adds every kind of a comma list after the move; Java reads the whole string as one `CounterType` there (single kind in every corpus line). The search path still refuses it | `ChangeZoneEffect.java:751-755` | `changeZoneEnter`, `changezoneeffect.go` |
| `Draw` `OptionalDecider$` asks the drawing player whatever its value (`True`, `You`); `Upto$` asks the confirmation first, then the number | `DrawEffect.java:67`, `:75-90` | `drawEffect.Resolve` |
| `Draw` `Reveal$` is ignored: revealing shows information only and no trigger reads it | `DrawEffect.java:91-93`, `GameAction.reveal` | `drawEffect.Resolve` |
| A delve cast clears the host's delved list before the caster picks, so a cast that then pays nothing leaves it empty; the list is filled after the mana is paid and no zone change clears it (it survives stack to battlefield). The `delved` valid property is a `startsWith` match and reads the source's list (Soulflayer, the only corpus line) | `CostAdjustment.java:262`, `:269-274`, `CardProperty.java:1367-1369`, `PlaySpellAbility.java:530-532` | `assistCost`, `costAssist.settle`, `Card.delved`, `valid.go` |
| A static granted by `AddStaticAbility$` joins the layer queue only after Layer 6, so its Layer 1-5 params (`AddType`, `SetColor`, `GainControl`, ...) never apply; the grant is removed and rebuilt every pass. No corpus grant carries one (87 grants, none), so nothing is lost by matching it | `GameAction.java:1111`, `:1143-1191`, `StaticEffect.java:280`, `StaticAbility.java:137-190` | `applyContinuousKeyword`, `traitGrants` |
| `PutSticker` tickets are subtracted from the owner's `TICKET` counters at resolution, after the sticker is chosen, not as a cost; affordability is checked when the options are built. Java's `subtractCounter` also fires the counter-removed trigger; Crucible does not (no corpus trigger reads a ticket counter) | `PutStickerEffect.java:80-84`, `:124-126` | `putStickerEffect.Resolve` |
| `PutSticker` flags are tested by presence, not value: `Optional$ False` is optional, any `NoTicketCost$` skips payment, an unknown `Kind$` filters nothing | `PutStickerEffect.java:70`, `:80`, `:86-87`, `:99` | `putStickerEffect.Resolve` |
| A sheet slot whose SVar is missing or whose `Kind$` names no kind is skipped without a word; a malformed `Tickets$`/`Power$`/`Toughness$` reads 0; an Ability sticker with no body is hidden from the choice | `StickerSheet.java:294-299`, `Sticker.java:27-33`, `:50-60`, `:125-128` | `faceCompiler.stickerSheet`, `Sticker.Implemented` |
| `StickersPlaced` and `StickerUniqueVowels` are SVars of the resolving `PutSticker`, read by its sub-abilities; Crucible writes them as host SVars (`StoreSVar`'s copy), reset to 0 at the start of each resolution | `PutStickerEffect.java:100-101`, `:131`, `:134-135` | `putStickerEffect.Resolve` |
| Stickers survive a move to any public zone and are dropped by one into a hidden zone (Hand, Library, Sideboard, the deck zones, ExtraHand, None, Subgame) | `GameAction.java:268-270`, `ZoneType.java:14-34` | `put`, `hiddenZone` |
| A sticker is rebuilt every pass from the card's list: PT at `LayerSetPT` with its placement timestamp, Name by folding the words in timestamp order (each fills the leftmost `__` blank, else goes in at its chosen position), Ability as keywords plus one trait grant. A `SetName$` static beats a Name sticker whatever the timestamps (Java orders them by timestamp); a Name sticker's name also shows off the battlefield | `Card.java:4634-4709`, `AppliedSticker.java:58-100` | `stickerPT`, `applyStickerNames`, `stickerAbilities` |
| `Discard<N/Random>` as an unless cost draws with `Aggregates.random` (reservoir sampling), the human path; the AI path shuffles (`CardLists.getRandomSubList`) and Crucible has one path. A hand smaller than N cannot pay it; the payer is not asked | `PlaySpellAbility.java:335-339`, `CostDiscard.java:133`, `:192`, `AiCostDecision.java:119-124` | `unlessCost.discardRandomN`, `payUnlessPartsInto` |
| `ExileFromGrave<N/All>` is payable with an empty graveyard (exiling nothing pays it) and ignores N | `CostExile.java:178-179`, `AiCostDecision.java:194-195` | `unlessCost.exileGraveAll` |
| `Card.TopGraveyardCreature` is the last creature of the payer's graveyard (Java reads the owner's graveyard; the same set unless owner and payer differ) | `CardProperty.java:582-587` | `exileGraveOptions` |
| The `DicePTExchanges` set is a `HashSet<Card>`; Crucible keeps insertion order, so the card pick's option order differs from Java's hash order | `RollDiceEffect.java:140`, `:233` | `replacementEvent.exchanges` |
| The exchange sets only the roll's modified value; the natural value (MaxRolls counting) stays the pre-swap roll. A refused stat asks for a roll again, without limit in Java; Crucible errors after 64 refusals (GO-7) | `RollDiceEffect.java:233-262`, `:302-303` | `swapDiceRolls`, `maxSwapDeclines` |
| An exchange writes `Card.addNewPT`: Layer 7b set-P/T at a fresh timestamp, permanent, dropped by any zone change | `Card.java:4390-4398`, `GameAction.java:363` | `Card.newPTs`, `newPTEffects` |
| `Adapt$` skips a creature with any +1/+1 counter unless a `Mode$ CanAdapt` static applies; Crucible reads no CanAdapt (Biomancer's Familiar, one card), so that card's "adapts as though it had no counters" is not modelled | `CountersPutEffect.java:485-487`, `StaticAbilityAdapt.java:10-17` | `putCounterEffect.Resolve` |
| `Exhaust$` and `PowerUp$` are not read by `CountersPutEffect`; both are the activation limit of one a game (`getActivationsThisGame() >= StaticAbilityAdditionalActivations.getLimit`, default 1), which Crucible hard-codes without the additional-activations static (no corpus card) | `SpellAbilityRestriction.java:465-471` | `activationLimitsMet` |
| `Support$` builds its own target restriction: up to N creatures, `Creature.Other` when the host is a permanent, one counter each, no `ValidTgts$` in the script | `CountersPutEffect.java:779-800` | `targetChoiceFor` |
| `RememberLKI$` on ChangeZone remembers the card's copy before the origin and optional checks, so a card that was not moved or whose move was declined is still remembered; the value (`True`, `Targeted`) is never read; Destroy remembers it when destroyed, or when `AlwaysRemember$` and the destroy failed | `ChangeZoneEffect.java:561`, `:1492`, `ChangeZoneAllEffect.java:127-129`, `DestroyEffect.java:102-104` | `Memory.RememberLKI`, `changeZoneKnown`, `destroyEffect.Resolve` |
| An LKI-remembered card is a snapshot in Java; here the ID is remembered and flagged, and only `Remembered*` player lists and `Count$Remembered` amounts read its snapshot (`Game.LKI`). Every other reader of `Defined$ Remembered` cards sees the live card | `CardCopyService.getLKICopy` | `Memory.IsRememberedLKI`, `rememberedPlayers`, `contextCards` |
| `DestinationAlternative$` asks the `AlternativeDecider$` player (default the activator) once before any card moves; the confirm is inverted against its prompt (false picks the alternative), a decider naming nobody is not asked and the primary destination stands. The hidden-origin call (after the search) is not ported | `ChangeZoneEffect.java:485-489`, `:1659-1703` | `changeZoneAltDest` |
| Animate `Triggers$` without `Duration$ Perpetual` grants the compiled triggers while the animate record lives: until cleanup by default (`addUntilCommand`'s end-of-turn fall-through also catches an unknown Duration$ in Java; Crucible refuses it), `Permanent` for good; a zone change drops them with the record. `Replacements$` still needs Perpetual | `AnimateEffect.java:127-130`, `AnimateEffectBase.java:171-174`, `SpellAbilityEffect.java:1030-1032` | `animateTriggerGrants`, `animateAbilities` |
| GenericChoice drops a choice whose `UnlessCost$` the chooser cannot pay, never `UnlessPayer$`/`UnlessSwitched$`; a mana part always passes (`CostPartMana.canPay` is a stub). No choice left resolves `FallbackAbility$`, and does nothing without one (Java only warns). The filter ignores `calculateUnlessCost`, so X and cost adjustments are not applied to it; a cost past `parseUnlessCost` refuses. Corpus costs are all literal | `ChooseGenericEffect.java:69-80`, `:128-137` | `genericchoiceeffect.go` |
