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
	// APIAttach entry is the only thing that sets it today; this port has no
	// representation for a target that is a player (enchantSpec's own doc
	// comment) or for more than one target (no ability needing that shape
	// exists yet), so a single CardID is enough rather than a slice or an
	// EntityID.
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
	// spell is SpellAbility.isSpell: this item is a spell (cast, or a copy
	// of one), not an activated or triggered ability. Set by every cast
	// path (castSpell, castspell.go; castWithoutPaying, discovereffect.go)
	// and by copySpell (copyspellabilityeffect.go). A card in the Stack
	// zone is not enough to tell: a "when you cast this spell" trigger's
	// Source is that same card, sitting above it (spellItemOf, stack.go).
	spell bool

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
}

// abilityRefs is what Defined$ can name beyond the host card: the
// ability's own chosen targets and, for a delayed or reflexive trigger's
// ability, what that trigger remembered.
type abilityRefs struct {
	targets           []EntityID
	triggerRemembered []EntityID
	triggered         triggeredObjects
	// replaced is the card a Moved replacement's ReplaceWith$ ability is
	// replacing the entry of (Defined$ ReplacedCard); NoCard otherwise.
	replaced CardID
}

// refs is a's own abilityRefs.
func (a *Ability) refs() abilityRefs {
	return abilityRefs{targets: a.Targets, triggerRemembered: a.TriggerRemembered, triggered: a.triggered,
		replaced: a.replacedCard()}
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
	// (TriggerDamageDone) or the player whose creatures did
	// (TriggerDamageDoneOnceByController). NoEntity when unset.
	source EntityID
	// sourceController is source's controller as the trigger fired --
	// Java stores a last-known-information copy of the damage source
	// (CardCopyService.getLKICopy), so a creature that changes control or
	// dies before the trigger resolves still answers its controller at
	// damage time. For a player source it is that player.
	sourceController PlayerID
	// player is AbilityKey.Player: who became the monarch (Mode$
	// BecomeMonarch), took the initiative (TakesInitiative) or completed a
	// dungeon (DungeonCompleted), whom the Ring tempted (RingTemptsYou), or
	// for whom chaos ensued (ChaosEnsues). NoPlayer when unset.
	player PlayerID
	// blocker is AbilityKey.Blocker for Mode$ AttackerBlockedByCreature
	// (TriggerAttackerBlockedByCreature.setTriggeringObjects), read by
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
	// card is AbilityKey.Card for Mode$ TapsForMana (the permanent tapped
	// for mana, TriggerTapsForMana.java:88) and Mode$ Discarded (the card
	// discarded, TriggerDiscarded.java:72-74), read by Defined$
	// TriggeredCardController. NoCard when unset.
	card CardID
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
