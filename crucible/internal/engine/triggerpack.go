// Trigger modes fired by effects this port already has: Scry, Surveil,
// Transformed and TurnFaceUp, each a few params tested against the event,
// fired through scanTriggers (triggerscan.go).

package engine

//enginelint:allow id card ability trigger staticability triggerscan game control valid stack

import (
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// effectEventTriggerParams are the general params a trigger of these four
// modes may carry besides its own; the intervening-if and limit params are
// read by triggerEffectAPI, so naming them needs no check here. Anything else
// (Scry's ToBottom$, Surveil's FirstTime$, TurnFaceUp's ValidCause$) is a
// check this port has no evidence for, so the line is not fired (GO-7).
var effectEventTriggerParams = map[string]bool{
	"mode": true, "execute": true, "triggerdescription": true, "triggerzones": true, "optionaldecider": true,
	"secondary": true, "activationlimit": true, "resolvedlimit": true, "gameactivationlimit": true,
	"ispresent": true, "presentdefined": true, "presentcompare": true, "presentzone": true, "presentplayer": true,
	"playerturn": true,
}

func effectEventParams(own ...string) map[string]bool {
	m := make(map[string]bool, len(effectEventTriggerParams)+len(own))
	for k, v := range effectEventTriggerParams {
		m[k] = v
	}
	for _, k := range own {
		m[k] = true
	}
	return m
}

var (
	scryTriggerParams    = effectEventParams("validplayer", "tobottom")
	surveilTriggerParams = effectEventParams("validplayer", "firsttime")
	transformedParams    = effectEventParams("validcard")
	turnedFaceUpParams   = effectEventParams("validcard", "validcause")
)

// checkScryTriggers is Mode$ Scry (TriggerScry.performTest, 24 real lines),
// run once per player that scried (GameAction.java:2652-2655): ValidPlayer$
// against the scrying player; ToBottom$ needs at least one card put on the
// bottom. looked is ScryNum (cards put anywhere), bottom is ScryBottom, both
// read by TriggerCount$.
func (g *Game) checkScryTriggers(controller PlayerController, pid PlayerID, looked, bottom int) {
	matches := g.scanTriggers([]string{"Scry"}, func(h *Card, _ triggerFace, t *compile.Ability) (triggeredObjects, bool) {
		if !paramsResolvable(t, scryTriggerParams) || !g.triggerPlayerMatches(h, t, "ValidPlayer", pid) {
			return triggeredObjects{}, false
		}
		if _, ok := t.Param("ToBottom"); ok && bottom <= 0 {
			return triggeredObjects{}, false
		}
		return triggeredObjects{player: pid,
			counts: triggerCounts{scryNum: looked, scryBottom: bottom, set: countScry}}, true
	})
	g.pushTriggeredAbilities(controller, matches)
}

// checkSurveilTriggers is Mode$ Surveil (TriggerSurveil.performTest, 16 real
// lines, Player.java:1083-1088): ValidPlayer$ against the surveiling player;
// FirstTime$ needs first, the player's first surveil this turn.
func (g *Game) checkSurveilTriggers(controller PlayerController, pid PlayerID, first bool) {
	matches := g.scanTriggers([]string{"Surveil"}, func(h *Card, _ triggerFace, t *compile.Ability) (triggeredObjects, bool) {
		if !paramsResolvable(t, surveilTriggerParams) || !g.triggerPlayerMatches(h, t, "ValidPlayer", pid) {
			return triggeredObjects{}, false
		}
		if _, ok := t.Param("FirstTime"); ok && !first {
			return triggeredObjects{}, false
		}
		return triggeredObjects{player: pid}, true
	})
	g.pushTriggeredAbilities(controller, matches)
}

var collectEvidenceParams = effectEventParams("validplayer")

// checkCollectEvidenceTriggers is Mode$ CollectEvidence
// (TriggerCollectEvidence.performTest, CostCollectEvidence.java:78): ValidPlayer$
// against the player who paid the cost; the player is TriggeredPlayer.
func (g *Game) checkCollectEvidenceTriggers(controller PlayerController, pid PlayerID) {
	g.pushTriggeredAbilities(controller, g.playerEventMatches("CollectEvidence", collectEvidenceParams, pid))
}

// commitCrimeParams are the params a Mode$ CommitCrime trigger may carry
// (TriggerCommitCrime.performTest reads ValidPlayer$ only; the 21 corpus
// lines add ActivationLimit$ and PlayerTurn$, read by triggerEffectAPI).
var commitCrimeParams = effectEventParams("validplayer")

// crimeTriggers is the Mode$ CommitCrime run of Player.commitCrime
// (Player.java:3962-3968, TriggerCommitCrime.java:17-28) for pushed, the
// stack item just added: none when pushing it was no crime (isCrime, the
// check noteCrime counted), else every trigger whose ValidPlayer$ admits its
// controller, who is TriggeredPlayer. A caller appends these to the push's
// BecomesTarget batch, after it, as MagicStack.add runs the crime check after
// the BecomesTarget triggers (MagicStack.java:408-451).
func (g *Game) crimeTriggers(pushed Ability) []Ability {
	if !g.isCrime(pushed) {
		return nil
	}
	return g.playerEventMatches("CommitCrime", commitCrimeParams, pushed.Controller)
}

func (g *Game) playerEventMatches(mode string, params map[string]bool, pid PlayerID) []Ability {
	return g.scanTriggers([]string{mode}, func(h *Card, _ triggerFace, t *compile.Ability) (triggeredObjects, bool) {
		if !paramsResolvable(t, params) || !g.triggerPlayerMatches(h, t, "ValidPlayer", pid) {
			return triggeredObjects{}, false
		}
		return triggeredObjects{player: pid}, true
	})
}

// checkTransformedTriggers is Mode$ Transformed (TriggerTransformed, 35 real
// lines): ValidCard$ against the permanent that just transformed.
func (g *Game) checkTransformedTriggers(controller PlayerController, card CardID) {
	g.pushTriggeredAbilities(controller, g.cardEventMatches("Transformed", transformedParams, card))
}

// checkTurnedFaceUpTriggers is Mode$ TurnFaceUp (TriggerTurnFaceUp, 126 real
// lines): ValidCard$ against the permanent just turned face up, ValidCause$
// against the ability that turned it (nil for a special action, which matches
// no ValidCause$ -- matchesValidParam is false for a null object).
func (g *Game) checkTurnedFaceUpTriggers(controller PlayerController, card CardID, cause *Ability) {
	c := g.Card(card)
	matches := g.scanTriggers([]string{"TurnFaceUp"}, func(h *Card, face triggerFace, t *compile.Ability) (triggeredObjects, bool) {
		if !paramsResolvable(t, turnedFaceUpParams) || !g.triggerCardMatches(h, t, "ValidCard", c) {
			return triggeredObjects{}, false
		}
		if spec, ok := t.Param("ValidCause"); ok {
			if cause == nil {
				return triggeredObjects{}, false
			}
			if m, known := g.spellAbilityMatches(cause, spec, h, h.Controller(), face.Amounts); !known || !m {
				return triggeredObjects{}, false
			}
		}
		return triggeredObjects{card: card}, true
	})
	g.pushTriggeredAbilities(controller, matches)
}

var attachedTriggerParams = effectEventParams("validsource", "validtarget", "targetrelativetosource")

// checkAttachedTriggers is Mode$ Attached (TriggerAttached, 4 real lines),
// fired by Card.attachToEntity after the Event$ Attached replacements
// (Card.java:3949-3953): ValidSource$ against the attachment, ValidTarget$ and
// TargetRelativeToSource$ against what it became attached to -- a card or a
// player. TargetRelativeToSource$ is matchesValidParam(param, target, source)
// with the attachment as the source card, so a `cmcLEX` operand is the
// attachment's own value, read through the trigger host's SVar X
// (Game.relativeAmounts): Eriette, the Beguiler's `Count$CardManaCost` is the
// Aura's mana value. The attachment is Defined$ TriggeredSource, the host
// TriggeredTarget (TriggerAttached.setTriggeringObjects).
func (g *Game) checkAttachedTriggers(controller PlayerController, attachment CardID, target EntityID) {
	src := g.Card(attachment)
	matches := g.scanTriggers([]string{"Attached"}, func(h *Card, face triggerFace, t *compile.Ability) (triggeredObjects, bool) {
		if !paramsResolvable(t, attachedTriggerParams) || !g.triggerCardMatches(h, t, "ValidSource", src) {
			return triggeredObjects{}, false
		}
		if spec, ok := t.Param("ValidTarget"); ok && !g.attachTargetMatches(h, spec, h.ID, target) {
			return triggeredObjects{}, false
		}
		if spec, ok := t.Param("TargetRelativeToSource"); ok && !g.attachTargetRelative(h, face, spec, attachment, target) {
			return triggeredObjects{}, false
		}
		return face.objects(triggeredObjects{source: CardEntity(attachment), sourceController: src.Controller(), target: target}), true
	})
	g.pushTriggeredAbilities(controller, matches)
}

// attachTargetMatches is matchesValid(target, spec, srcCard) on a trigger of
// h with src as the source card: a card target against the valid string, a
// player target against the player spec. A player spec this port cannot read
// does not match (GO-7).
func (g *Game) attachTargetMatches(h *Card, spec string, src CardID, target EntityID) bool {
	if id, ok := target.AsCard(); ok {
		return Matches(g, g.Card(id), valid.Parse(spec), h.Controller(), src)
	}
	pid, _ := target.AsPlayer()
	matched, recognized := matchesPlayerSpec(g, pid, h.Controller(), src, spec)
	return recognized && matched
}

// attachTargetRelative is TargetRelativeToSource$: attachTargetMatches with
// the attachment as the source card and the trigger host's SVars answering a
// comparison operand (AbilityUtils.calculateAmount(source, "X", trigger)
// reads the SVar off the trigger, the amount off the source).
func (g *Game) attachTargetRelative(h *Card, face triggerFace, spec string, attachment CardID, target EntityID) bool {
	prev := g.relativeAmounts
	g.relativeAmounts = face.Amounts
	defer func() { g.relativeAmounts = prev }()
	return g.attachTargetMatches(h, spec, attachment, target)
}

func (g *Game) cardEventMatches(mode string, params map[string]bool, card CardID) []Ability {
	c := g.Card(card)
	return g.scanTriggers([]string{mode}, func(h *Card, _ triggerFace, t *compile.Ability) (triggeredObjects, bool) {
		if !paramsResolvable(t, params) || !g.triggerCardMatches(h, t, "ValidCard", c) {
			return triggeredObjects{}, false
		}
		return triggeredObjects{card: card}, true
	})
}
