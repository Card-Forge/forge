// The API vocabulary and the value that names one resolvable ability,
// kept apart from effect.go's dispatch machinery on purpose: dispatch
// (Effect.Resolve, Registry) needs *Game, and Game.stack (stack.go) needs
// to name Ability, so Ability and the APIType it carries have to sit below
// both in the dependency graph or the two would depend on each other
// (enginelint).

package engine

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/expr"
)

//go:generate go run ../../tools/genapitype -apitype ../../../forge-game/src/main/java/forge/game/ability/ApiType.java

// APIType names an ability API. The constants are generated from Forge's
// ApiType enum, so the set cannot drift from upstream without a build failure.
type APIType uint16

// String returns the API as a card script spells it.
func (a APIType) String() string {
	if int(a) >= numAPITypes {
		return fmt.Sprintf("APIType(%d)", uint16(a))
	}
	return apiNames[a]
}

// APIByName looks an API up by the name a script writes, and reports whether
// it is one. Exact match: an API that silently resolves to something else is
// a card doing the wrong thing rather than nothing.
func APIByName(name string) (APIType, bool) {
	for i, n := range apiNames {
		if n == name {
			return APIType(i), true
		}
	}
	return 0, false
}

// Ability is one resolvable ability on the stack. The cost already paid and
// everything else CR 601-609 tracks per stack object beyond API/Source/
// Controller/Target land here once casting grows enough to fill them; today
// it carries just enough for dispatch, for the stack to know whose it is,
// and for the one target CastSpell's own Aura branch chooses at cast time
// (CR 601.2c).
type Ability struct {
	// ID is this stack item's own identity (ADR-0018, StackItemID's doc
	// comment, id.go) -- set by PushAbility, NoStackItem until then. Distinct
	// from Source: CopySpellAbility's copy and the spell it copies share no
	// CardID host once the original has left the stack, but need to be told
	// apart, and a triggered ability's own Defined$ TriggeredSpellAbility
	// (checkSpellCastTriggers, trigger.go) names a specific one of possibly
	// several spells cast this turn.
	ID StackItemID
	// API decides which Effect resolves this.
	API APIType
	// Source is the card the ability came from.
	Source CardID
	// Controller is who is resolving it, which is not always the source's
	// controller once control-changing effects are involved.
	Controller PlayerID
	// Target is the single card this ability was announced against at cast
	// time (CR 601.2c) -- NoCard when the ability has none. An Aura's own
	// APIAttach entry is the only thing that sets it today, and only for an
	// Aura that enchants a permanent: an Aura that enchants a player
	// (castPlayerAura) carries it in Targets instead and leaves this NoCard.
	Target CardID
	// Targets is the general CR 601.2c/603.3b "choose targets" answer --
	// resolveTargets (targeting.go) fills it from a ValidTgts$/TargetMin$/
	// TargetMax$ triple right before this ability is pushed onto the stack
	// (pushTriggeredAbilities, trigger.go, the only pusher this port has),
	// asking the controller once via ChooseTargets. Nil for an ability
	// naming no ValidTgts$ at all -- the overwhelming majority -- and for
	// Target's own Aura shape above, which predates this field and is not
	// migrated onto it: two callers, two shapes, no single caller needing
	// both. A card or a player target is carried the same way here
	// (EntityID, unlike Target's own CardID-only shape), matching
	// AbilityUtils.getTargetCards/getTargetPlayers' own split -- an effect
	// naming Defined$ reads either half back through definedCards's/
	// definedPlayers's own "Targeted" case (defined.go); one naming
	// ValidTgts$ on its own line instead reads this field directly
	// (targetedOrDefinedCards, defined.go, SpellAbilityEffect.
	// getTargetCards(sa)'s own contract -- destroyEffect's/tapEffect's/
	// untapEffect's own first callers), since it IS that ability's own
	// resolveTargets answer, already populated against these same Params
	// before Resolve is ever called.
	Targets []EntityID
	// Params is the compiled sub-ability record backing this API call --
	// Defined$, NumCards$, and every other key an Effect's own Resolve reads
	// (compile.Ability.Param). Nil for a cast ability
	// (APIPermanentCreature/APIPermanentNoncreature/APIAttach): nothing
	// reads a param off casting itself, only off a card script's own
	// sub-ability -- a trigger's Execute$ (triggerEffectAPI, trigger.go)
	// today, an activated ability's own cost-paid effect once that exists.
	Params *compile.Ability
	// Amounts is the compile.Face's own SVar-defined amounts (compile.go)
	// Params' own host face carries -- resolveNamedAmount (amount.go) needs
	// it to resolve a named-SVar param value (dealDamageEffect's own
	// NumDmg$, dealdamageeffect.go, the first Effect to need one) the
	// identical way a trigger's own numeric params already do
	// (triggerCommonRequirementsMet, trigger.go). Nil wherever Params is,
	// for the identical reason.
	Amounts map[string]expr.Amount
	// Optional is CR 603.3d's own "may" triggered ability -- a trigger's own
	// OptionalDecider$ (WrappedAbility.java's own decider field), true only
	// for the "You" case (triggerEffectAPI's own doc comment, trigger.go,
	// has the corpus accounting). Registry.Resolve (effect.go) asks
	// PlayerController.ConfirmOptionalTrigger before running this ability's
	// body OR chaining its own SubAbility$ at all -- WrappedAbility
	// .resolve()'s own early return on a declined confirmTrigger, ported
	// directly. False for every ability this port pushes any other way (a
	// cast spell, a chained SubAbility$'s own child, resolveSubAbility,
	// subability.go) -- neither is ever optional on its own, a trigger's own
	// OptionalDecider$ never carrying onto what it chains into.
	Optional bool
	// costPaid marks an activated ability whose Cost$ ActivateAbility already
	// paid. An AB$ line reaching the stack any other way -- a trigger's
	// Execute$ -- is paid when it resolves (resolveTriggeredCost).
	costPaid bool
	// paid are the cards the activation cost used up, read back by Sacrificed$,
	// Exiled$ and Discarded$ amounts. Sub-abilities inherit them.
	paid paidLists
	// parentTargets are the targets of the nearest ancestor ability that chose
	// some (Defined$ ParentTarget); a sub-ability that chooses its own keeps
	// the parent's here while Targets holds its own.
	parentTargets []EntityID
	// ancestorTargeting is SpellAbility.usesTargeting on some ancestor of this
	// sub-ability: one of them names ValidTgts$. An empty parentTargets then
	// is Java's empty getParentTargetingCard answer -- the targets were
	// dropped as illegal (CR 608.2b) or TargetMin$ 0 chose none -- rather than
	// a reference this port does not carry (definedCards' ParentTarget).
	ancestorTargeting bool
	// uniqueTargets is SpellAbility.getUniqueTargets: every target the
	// ancestors of a sub-ability link chose, which a TargetUnique$ link may
	// not choose again (targetChoiceFor). Set only on the link
	// resolveChainTargets asks about.
	uniqueTargets []EntityID
	// radiance is the ValidTgts$ of the nearest ancestor that targets a card
	// and names Radiance$, "" when none does (radianceSpec).
	radiance string
	// chainTargets are the targets chosen for the sub-abilities of this
	// ability that name their own ValidTgts$ (resolveChainTargets).
	chainTargets []chainTarget
	// divided is the DividedAsYouChoose$ division announced with Targets
	// (CR 601.2d, divideAmongTargets, dividedtargets.go): Java's
	// TargetChoices.dividedMap. Looked up by target, never by position --
	// dropIllegalTargets rebuilds Targets, and a dropped target's share is
	// simply never read. Replaced, never written in place: a stack item
	// copied by value (copySpell, Game.Clone) shares it.
	divided []dividedShare
	// evolve is the creature that entered for a trigger naming Condition$
	// Evolve: WrappedAbility.resolve checks CR 702.100c again as the
	// ability resolves, against the creatures' values by then. NoCard for
	// every other ability.
	evolve CardID
	// TriggerRemembered is what a delayed or reflexive trigger remembered when
	// it was created (RememberObjects$, Java's Trigger.addRemembered),
	// carried onto the ability it runs and every sub-ability that ability
	// chains -- Java reads it off the root ability. Defined$
	// DelayTriggerRemembered[LKI] reads it (defined.go).
	TriggerRemembered []EntityID
	// triggered is what the trigger that put this ability on the stack
	// recorded about its event (SpellAbility.setTriggeringObject), carried
	// onto every sub-ability it chains the way TriggerRemembered is: Java
	// reads it off the root ability. Zero for any ability no trigger made.
	triggered triggeredObjects
	// targetStamps is each card target's zoneStamp as the ability went on
	// the stack (stampTargets, targeting.go) -- Java's
	// equalsWithGameTimestamp identity (MagicStack.java:716-722): a card
	// that changed zones since is a new object (CR 400.7) and no longer a
	// legal target. A Charm mode carries its own.
	targetStamps []targetStamp
	// Modes is a Charm's chosen modes, each with its own targets, picked as
	// the Charm was put on the stack (chooseCharmModes, charmeffect.go).
	Modes []Ability
	// charmRoot is the Charm line a mode belongs to, nil elsewhere:
	// SpellAbility.getRootAbility for a mode (askOf, staticability.go).
	charmRoot *compile.Ability
	// casting is true only while a spell's targets are being chosen as it is
	// cast, before it is on the stack (castInstantOrSorcery); askOf reads it.
	casting bool
	// spell is SpellAbility.isSpell: this item is a spell (cast, or a copy
	// of one), not an activated or triggered ability. Set by every cast
	// path (castSpell, castspell.go; castWithoutPaying, discovereffect.go)
	// and by copySpell (copyspellabilityeffect.go). A card in the Stack
	// zone is not enough to tell: a "when you cast this spell" trigger's
	// Source is that same card, sitting above it (spellItemOf, stack.go).
	spell bool
	// probing marks a spell not yet cast asked about as though one optional
	// cost were paid: SpellAbility.canPlay(checkOptionalCosts) tries the spell
	// with each one added (SpellAbility.java:467-491). probeTeamwork is that
	// cost being Teamwork, what the Teamwork property reads before the cast
	// records anything (castsWithFlashOptional).
	probing       bool
	probeTeamwork bool

	// hostTransforms is the host's transform count when this ability went
	// on the stack -- or, for a delayed trigger, when it was created: Java's
	// StoredTransform SVar, read by SetState (CR 701.28f).
	hostTransforms    int
	hasHostTransforms bool

	// xManaCostPaid is SpellAbility.xManaCostPaid: the value of X announced
	// for this ability's own mana cost (CR 601.2b, CR 602.2b), set by
	// castSpell and ActivateAbility from payManaCostX (manapay.go) and
	// rewritten by ChangeX (changexeffect.go). Java's field is a nullable
	// Integer, null when no cost part carried an X or the cost was not paid
	// (PlaySpellAbility.announceValuesLikeX, a WithoutManaCost$ cast), and
	// ChangeXEffect.java writes only a non-null one -- hasXManaCostPaid is
	// that null. A value, so Clone and a CopySpellAbility copy carry it
	// (CR 707.10).
	xManaCostPaid    int
	hasXManaCostPaid bool

	// damageMap is SpellAbility.getDamageMap: damage recorded under a
	// DamageMap$ ability for a later DamageResolve, shared down the
	// sub-ability chain.
	damageMap *pendingDamage
	// replacing is the event a ReplaceWith$ ability edits in place:
	// SpellAbility.getReplacingObject and its OriginalParams map
	// (replaceeffect.go). Set only on the Ability a replacement dispatch
	// (replacement.go) builds and resolves on the spot, never on one pushed
	// to the stack, so no stacked Ability carries a live one into Clone.
	replacing *replacementEvent
	// modesErr is why chooseCharmModes could not pick modes for a Charm it
	// was asked about; the Charm is still pushed and fails with this error
	// when it resolves, since pushing has no error path of its own (GO-7).
	modesErr error
	// targetsErr is why resolveTargets could not ask for this ability's
	// targets (targetChoice.err, targeting.go): a legal target it has no
	// EntityID for. The ability is still pushed and fails with this error
	// when it resolves, modesErr's own deferral.
	targetsErr error
	// targeter is the player who chose the targets (TargetingPlayer$,
	// SpellAbility.getTargetingPlayer), NoPlayer before they were chosen.
	targeter PlayerID
	// forEachControllers is each card target's controller as it was chosen under
	// TargetsForEachPlayer$ (TargetChoices.cardControllers).
	forEachControllers map[CardID]PlayerID
	// stackTargets is what each ability on the stack this ability targets
	// (StackItemEntity, ADR-0041) was as the CR 608.2b check found it still
	// legal, just before resolution (recordStackTargets): Java's target is the
	// SpellAbility object itself, which keeps its activating player and host
	// after a Counter removed it, so Deny the Witch's Defined$
	// TargetedController still names the countered ability's controller.
	// Sub-abilities inherit it. Replaced, never written in place: a stack item
	// copied by value (Game.Clone) shares it.
	stackTargets []stackTargetRef
	// wardCounters is checkWardTriggers' own answer to Forge's
	// Defined$ TriggeredSourceSA (trigger.go, ADR-0028): the spell Ward's
	// built Counter ability counters. Kept off Targets on purpose -- a real
	// Targets entry would run the spell through
	// pushTriggeredAbilities' own post-push checkBecomesTargetTriggers scan
	// (wrongly marking the spell BecameTargetThisTurn and letting an
	// unrelated watcher's ValidTarget$ fire against a stack card) and
	// through resolveTop's own CR 608.2b targetsStillLegal re-check
	// (cardCantBeTargetedBy has no business running against a spell).
	// Forge's own Defined$ reference is exempt from both, since CR 702.21a
	// names no target at all; counterEffect (countereffect.go) reads this
	// field instead, and its own `c.Zone != Stack` check is the identical
	// "left the stack already" answer Forge's own
	// getInstanceMatchingSpellAbilityID null-check gives.
	wardCounters EntityID
	// wardItem is the same stand-in for an activated or triggered ability
	// that targeted the warded permanent (checkWardTriggersForAbility): the
	// stack item Ward's Counter removes, without moving any card. 0 for none.
	wardItem StackItemID
	// isTrigger is SpellAbility.isTrigger: pushTriggeredAbilities, the only
	// pusher of a trigger, sets it. hasSourceOnStack reads it (CR 704.5v's
	// "source of an ability that has triggered but not yet left the stack").
	isTrigger bool
	// stackRoot marks the ability resolveTop popped off the stack, never a
	// sub-ability, Charm mode or choice it goes on to resolve: the only one
	// MagicStack.resolveStack hands to AbilityUtils.handleRemembering
	// (MagicStack.java:646). Registry.resolve refuses a RememberTargets$ line
	// that is not one (rememberTargets, stack.go).
	stackRoot bool
	// activated marks an ability put on the stack by ActivateAbility, which
	// pushes through pushTriggeredAbilities for its targets: it is not a
	// trigger (isTrigger stays false).
	activated bool
	// targetsChosen marks an ability whose modes and targets were chosen before
	// its costs were paid (chooseAbilityTargets): pushTriggeredAbilities pushes
	// it as is.
	targetsChosen bool
	// staticTrigger marks an ability of a T: line carrying Static$ True: it
	// resolves at its trigger site and never uses the stack
	// (pushTriggeredAbilities hands it to resolveStaticTriggers, ADR-0020). Only
	// the trigger checks that set it get that treatment.
	staticTrigger bool
}

// chainTarget is the targets chosen for one sub-ability of a chain
// (chaintargets.go), keyed by the link's compiled ability: immutable and
// shared, so a pointer names it.
type chainTarget struct {
	Params  *compile.Ability
	Targets []EntityID
	// Stamps are the zoneStamps of the card targets as the ability went on the
	// stack (stampTargets), for the CR 608.2b re-check.
	Stamps []targetStamp
	// Divided is the link's own DividedAsYouChoose$ division, Ability.divided's
	// twin.
	Divided []dividedShare
	// Err is why the link's targets or division could not be asked for
	// (Ability.targetsErr's twin): the link fails with it when it resolves.
	Err error
	// Targeter is who chose the link's targets (Ability.targeter's twin):
	// Java stores the targeting player on each link as setupTargets walks
	// the chain (SpellAbility.java:2158-2161) and reads it at fizzle time
	// for TargetingPlayerControls$ (:1474-1476). NoPlayer when not chosen.
	Targeter PlayerID
}

// stackTargetRef is one targeted ability on the stack as it last was: its
// stack item, controller (SpellAbility.getActivatingPlayer) and host card.
type stackTargetRef struct {
	item       StackItemID
	controller PlayerID
	host       CardID
}

// dividedShare is one target's part of a DividedAsYouChoose$ amount.
type dividedShare struct {
	target EntityID
	amount int
}

// allTargetsOf is SpellAbility.getAllTargetChoices (MagicStack.java:410):
// a's own Targets, its SubAbility$ links' (chainTargets), then each chosen
// Charm mode's and that mode's links', in chain order (CharmEffect's
// chainAbilities appends the modes to the tail). checkBecomesTargetTriggers
// and checkWardTriggers read it: a mode's or a link's target is on neither
// a.Targets nor a.Target.
func allTargetsOf(a Ability) []EntityID {
	targets := append([]EntityID(nil), a.Targets...)
	for _, ct := range a.chainTargets {
		targets = append(targets, ct.Targets...)
	}
	for _, mode := range a.Modes {
		targets = append(targets, mode.Targets...)
		for _, ct := range mode.chainTargets {
			targets = append(targets, ct.Targets...)
		}
	}
	return targets
}

// abilityRefs is what Defined$ can name beyond the host card: the
// ability's own chosen targets and, for a delayed or reflexive trigger's
// ability, what that trigger remembered.
type abilityRefs struct {
	targets           []EntityID
	parentTargets     []EntityID
	ancestorTargeting bool
	triggerRemembered []EntityID
	triggered         triggeredObjects
	paid              paidLists
	// stackTargets is Ability.stackTargets: the targeted abilities on the
	// stack as last seen, for a reader that runs after one left the stack.
	stackTargets []stackTargetRef
	// replaced is the card a Moved replacement's ReplaceWith$ ability is
	// replacing the entry of (Defined$ ReplacedCard); NoCard otherwise.
	replaced CardID
	// replacedPlayer and replacedDefendingPlayer are a DeclareBlocker
	// replacement's Player and DefendingPlayer (Defined$ ReplacedPlayer,
	// Defined$ ReplacedDefendingPlayer); NoPlayer otherwise.
	replacedPlayer          PlayerID
	replacedDefendingPlayer PlayerID
	// replacedTarget is a DamageDone replacement's Target, the damaged object
	// (ReplaceDamage.setReplacingObjects, ReplaceDamage.java:145: AbilityKey.
	// Target from Affected; Defined$ ReplacedTarget); NoEntity otherwise.
	replacedTarget EntityID
}

// refs is a's own abilityRefs.
func (a *Ability) refs() abilityRefs {
	r := abilityRefs{targets: a.Targets, parentTargets: a.parentTargets, ancestorTargeting: a.ancestorTargeting,
		triggerRemembered: a.TriggerRemembered, triggered: a.triggered, paid: a.paid, replaced: a.replacedCard(),
		stackTargets: a.stackTargets}
	if a.replacing != nil {
		r.replacedPlayer, r.replacedDefendingPlayer = a.replacing.player, a.replacing.defendingPlayer
		if a.replacing.amountName == "DamageAmount" {
			r.replacedTarget = a.replacing.affected
		}
	}
	return r
}

// replacedCard is the card entering the battlefield when a runs as a Moved
// replacement's ReplaceWith$ ability (entersascopy.go), NoCard otherwise.
func (a *Ability) replacedCard() CardID {
	if a.replacing == nil {
		return NoCard
	}
	return a.replacing.card
}

// triggeredObjects is the slice of Java's triggering-objects map
// (SpellAbility.getTriggeringObject) a ported trigger mode records, read by
// Defined$ Triggered<Key> (definedPlayers, defined.go). Only the keys some
// mode sets are here; a Defined$ naming one its trigger did not record is
// an error rather than an empty answer (GO-7), so a mode that never learned
// to set a key fails loudly instead of acting on nobody.
type triggeredObjects struct {
	// source is AbilityKey.Source: the card that dealt the damage
	// (TriggerDamageDone, TriggerDamageDealtOnce) or the player whose creatures did
	// (TriggerDamageDoneOnceByController). NoEntity when unset.
	source EntityID
	// sourceController is source's controller as the trigger fired --
	// Java stores a last-known-information copy of the damage source
	// (CardCopyService.getLKICopy), so a creature that changes control or
	// dies before the trigger resolves still answers its controller at
	// damage time. For a player source it is that player.
	sourceController PlayerID
	// player is AbilityKey.Player: the turn's player for Mode$ Phase (card
	// and delayed lines alike, PhaseHandler.java:438), who drew (Drawn), who
	// became the monarch (BecomeMonarch), took the initiative
	// (TakesInitiative) or completed a dungeon (DungeonCompleted), whom the
	// Ring tempted (RingTemptsYou), or for whom chaos ensued (ChaosEnsues).
	// NoPlayer when unset.
	player PlayerID
	// blocker is AbilityKey.Blocker for Mode$ AttackerBlockedByCreature
	// (TriggerAttackerBlockedByCreature.setTriggeringObjects) and Blocks
	// (TriggerBlocks.java:71), read by
	// Defined$ TriggeredBlocker/TriggeredBlockerLKICopy (definedCards). A
	// CardID is stable across zone changes, so the LKI spelling names the
	// same card. NoCard when unset.
	blocker CardID
	// attacker is AbilityKey.Attacker for Mode$ Attacks
	// (TriggerAttacks.setTriggeringObjects), read by Defined$
	// TriggeredAttacker/TriggeredAttackerLKICopy (definedCards). NoCard when
	// unset.
	attacker CardID
	// spellAbility is AbilityKey.SpellAbility for Mode$ SpellCast: the
	// stack item of the spell just cast (checkSpellCastTriggers), read by
	// Defined$ TriggeredSpellAbility (copyspellabilityeffect.go). An ID,
	// not a copy of the Ability: the spell can change (a copy retargets
	// nothing of it, but a fizzle or counter removes it) between the
	// trigger and its resolution, and the stack is the one place to ask.
	// NoStackItem when unset.
	spellAbility StackItemID
	// card is AbilityKey.Card: the permanent tapped for mana (Mode$
	// TapsForMana, TriggerTapsForMana.java:88), the card discarded
	// (Discarded, TriggerDiscarded.java:72-74), drawn (Drawn), tapped (Taps),
	// untapped (Untaps), sacrificed (Sacrificed) or played as a land
	// (LandPlayed), and the card a ChangesZone line -- a card's own, a
	// watcher's or a delayed one -- saw move. Read by Defined$ TriggeredCard
	// and TriggeredCardController. NoCard when unset.
	card CardID
	// cardStamp is card's zoneStamp as a "this card is put into a graveyard"
	// trigger fired (checkDiesTriggers' own card, checkHaunterSpellTriggers):
	// the graveyard object AbilityKey.NewCard names. Haunt reads it for
	// HauntEffect.java:25's equalsWithGameTimestamp. Zero when unset.
	cardStamp uint64
	// cardLKIController and cardLKIOwner are card's controller and owner as
	// the trigger fired, for the modes whose AbilityKey.Card is a frozen copy
	// rather than the live card: ChangesZone with Origin$ Battlefield (the
	// CardLKI, TriggerChangesZone.java:192-194), Exiled from the battlefield
	// (the pre-move object, GameAction.java:933-942) and Sacrificed (the LKI
	// copy, Player.java:2276-2280). Defined$ TriggeredCardController/Owner
	// read them (AbilityUtils.java:1029-1030, :1057-1058), so a creature that
	// died under a borrowed controller answers the borrower, as Java does.
	// NoPlayer when the mode passes the live card (Taps, Drawn, Discarded,
	// CounterAdded, ...): the reader then asks the card as it is now, Java's
	// own reading of a live object.
	cardLKIController PlayerID
	cardLKIOwner      PlayerID
	// exploited is AbilityKey.Exploited of Mode$ Exploited: the creature the
	// exploiter (card) sacrificed, read from its last-known information
	// (SacrificeEffect.java:171-173). Read by the TriggeredExploited amount
	// head. NoCard when unset.
	exploited CardID
	// devoured is AbilityKey.Devoured of Mode$ Devoured: the permanent a
	// Devour sacrifice devoured, read from its last-known information
	// (SacrificeEffect.java:164-168). Read by the TriggeredDevoured amount
	// head. NoCard when unset.
	devoured CardID
	// activator is AbilityKey.Activator for Mode$ TapsForMana: the player
	// who activated the mana ability, read by Defined$ TriggeredActivator.
	// NoPlayer when unset.
	activator PlayerID
	// produced is AbilityKey.Produced for Mode$ TapsForMana: the mana the
	// ability made after ProduceMana replacements (manaReplaced) -- Java
	// passes produceMana's own after-replacement string on
	// (AbilityManaPart.java:174, :211, :224; ManaEffect.java:187-193). No
	// ported effect reads it yet; ManaReflected's ReflectProperty$ Produced
	// will. A value, so a stacked Ability copies it with no aliasing. Zero
	// amount when unset.
	produced producedMana
	// originalController is AbilityKey.OriginalController for Mode$
	// ChangesController (TriggerChangesController.setTriggeringObjects): the
	// player the card changed controller away from, read by Defined$
	// TriggeredOriginalController and TriggerController$. NoPlayer when
	// unset.
	originalController PlayerID
	// attackingPlayer is AbilityKey.AttackingPlayer for Mode$
	// AttackersDeclared/AttackersDeclaredOneTarget
	// (TriggerAttackersDeclared.java:98) and AttackerUnblockedOnce: the
	// player whose creatures attack; for DamageDoneOnce the one controller of
	// the damage sources (damageDoneOnceObjects). Read by Defined$
	// TriggeredAttackingPlayer. NoPlayer when unset.
	attackingPlayer PlayerID
	// scheme is AbilityKey.Scheme for Mode$ SetInMotion
	// (TriggerSetInMotion.setTriggeringObjects): the scheme just set in
	// motion, read by SetInMotion's Again$ (setinmotioneffect.go). NoCard
	// when unset.
	scheme CardID
	// grant is not an AbilityKey but the trigger itself, as far as
	// LosePerpetual asks (SpellAbility.getTrigger): the id of the source
	// card's grant row (grantedTriggers, card.go) the trigger that put this
	// ability on the stack came from; 0 for a printed trigger or an ability
	// no trigger put there. Kept here so it travels down the SubAbility$
	// chain with the rest of the triggering objects, as Java's getTrigger
	// walks up getParent to the root (SpellAbility.java:1354-1359).
	grant uint64
	// counts are the integers the trigger mode recorded, read by
	// TriggerCount$<Key> (AbilityUtils.java:638).
	counts triggerCounts
	// cards is AbilityKey.Cards: the batch a Mode$ DiscardedAll or
	// ChangesZoneAll trigger fired for, read by Defined$ TriggeredCards.
	cards []CardID
	// attackers is AbilityKey.Attackers: the attackers a Mode$
	// AttackersDeclared/AttackersDeclaredOneTarget trigger fired for, narrowed
	// by its ValidAttackers$ (TriggerAttackersDeclared.java:82-91), the
	// attacker a Mode$ Blocks blocker blocks (TriggerBlocks.java:71), the
	// blocked attackers of Mode$ BlockersDeclared (PhaseHandler.java:750-760)
	// or AttackerBlockedOnce (PhaseHandler.java:778-820). Read by Defined$
	// TriggeredAttackers, the TriggeredAttackers valid property and the
	// TriggerObjectsAttackers$ amount head. Its own slice, never combat's: the
	// list is fixed as the trigger fires and is never edited in place, so a
	// stacked Ability and a Clone share it safely.
	attackers []CardID
	// attackersRecorded is whether the mode recorded attackers: a recorded
	// list can be empty (no attacker matched ValidAttackers$ under an EQ0
	// amount), which reads 0, while an unrecorded one is unresolved (GO-7) --
	// Java's TriggerObjects reader defaults a missing key to an empty list
	// (AbilityUtils.java:697-700), which this port does not guess at.
	attackersRecorded bool
	// blockers is AbilityKey.Blockers of Mode$ BlockersDeclared: every
	// declared blocker (combat.getAllBlockers, PhaseHandler.java:748-761).
	// Read by Defined$ TriggeredBlockers. nil when unset; the mode fires only
	// for a non-empty list, so nil is never a recorded value. Shared like
	// attackers.
	blockers []CardID
	// attackedTargets is AbilityKey.AttackedTarget of Mode$ AttackersDeclared/
	// AttackersDeclaredOneTarget (TriggerAttackersDeclared.java:80-96): the
	// defenders attacked, or the defenders of the ValidAttackers$ attackers,
	// kept to those AttackedTarget$ names. Players and cards (planeswalkers,
	// battles) in one list. Read by Defined$ TriggeredAttackedTarget and the
	// "attacking TriggeredAttackedTarget" valid property. Shared like
	// attackers.
	attackedTargets []EntityID
	// attackedTargetsRecorded is whether the mode recorded attackedTargets: a
	// recorded list can be empty (AttackedTarget$ kept none of the
	// ValidAttackers$ defenders), an unrecorded one is unresolved (GO-7).
	attackedTargetsRecorded bool
	// target is AbilityKey.Target: what the attachment became attached to
	// (Mode$ Attached, TriggerAttached.setTriggeringObjects), the damaged
	// card or player (DamageDone, DamageDoneOnce), or what became the target
	// (BecomesTarget). Read by Defined$ TriggeredTarget/TriggeredTargetLKICopy.
	// NoEntity when unset.
	target EntityID
}

// triggerCounts is the integer half of Java's triggering-objects map: the
// keys a ported trigger mode records (DamageAmount, LifeAmount, Amount, the
// storm count). A key a mode did not record is unresolved, not 0, so a mode
// that never learned it fails loudly instead of counting nothing (GO-7).
type triggerCounts struct {
	damage, life, amount, storm int
	scryNum, scryBottom         int
	set                         triggerCountKeys
}

// triggerCountKeys is which triggerCounts fields were recorded.
type triggerCountKeys uint8

const (
	countDamage triggerCountKeys = 1 << iota
	countLife
	countAmount
	countStorm
	countScry
)

// count is the value of the AbilityKey name, and whether the trigger recorded
// it.
func (c triggerCounts) count(name string) (int, bool) {
	switch name {
	case "DamageAmount":
		return c.damage, c.set&countDamage != 0
	case "LifeAmount":
		return c.life, c.set&countLife != 0
	case "Amount":
		return c.amount, c.set&countAmount != 0
	case "CurrentStormCount":
		return c.storm, c.set&countStorm != 0
	case "ScryNum":
		return c.scryNum, c.set&countScry != 0
	case "ScryBottom":
		return c.scryBottom, c.set&countScry != 0
	}
	return 0, false
}

// targetStamp is one card target's zoneStamp as recorded by stampTargets.
type targetStamp struct {
	card  CardID
	stamp uint64
}

// stampOf is the zoneStamp stampTargets recorded for card, if any. An
// ability resolved without going on the stack (a test calling
// Registry.Resolve directly) has none, and only the other checks apply.
func (a *Ability) stampOf(card CardID) (uint64, bool) {
	for _, s := range a.targetStamps {
		if s.card == card {
			return s.stamp, true
		}
	}
	return 0, false
}

// paidLists are the cards an ability's cost used up, which its effects read
// back as `Sacrificed$`, `Exiled$` and `Discarded$` (SpellAbility.getPaidList).
// They are IDs: a sacrificed card is read from its last-known information.
//
// recorded is true once the cost that paid for the ability wrote them. A cost
// path that does not (a spell's additional cost, an unless cost, a trigger's
// Cost$) leaves them unrecorded, and the amount stays unresolved instead of
// reading an empty list as 0.
type paidLists struct {
	sacrificed, exiled, discarded []CardID
	recorded                      bool
}
