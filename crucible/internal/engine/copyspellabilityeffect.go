package engine

//enginelint:allow ability action amount card castspell condition control defined effecthelpers game id parts player stack targeting trigger zone

import (
	"fmt"
	"strings"
)

// copySpellAbilityEffect is CopySpellAbilityEffect.java (CR 707.10): for
// each Controller$ player (default You), Optional$ lets them decline each
// named spell, then Amount$ (default 1) copies of it go on the stack under
// that player, above whatever is resolving, so they resolve first. A copy
// is not cast (no SpellCast, no SpellsCastThisTurn, no cast triggers), pays
// no cost, and keeps the original's modes and targets unless
// MayChooseTarget$ lets the copier choose new ones (CR 707.10c). Each copy
// is a new stack object with its own StackItemID, on a new card
// (copySpellHost, Java's COPIED_SPELL) that ceases to exist when it leaves
// the stack (ceaseCopiedSpell, game.go) or becomes a token when it resolves
// as a permanent (copyBecomesToken, castspell.go).
//
// The spells are Defined$ TriggeredSpellAbility -- the stack item a Mode$
// SpellCast trigger recorded (triggeredObjects.spellAbility; 164 of 255
// lines) -- or the ability's own targets (ValidTgts$, spells on the stack,
// targetChoiceFor) and Defined$ Targeted. RememberNewCard$ remembers each
// copy's card on the host; IgnoreFreeze$ and Secondary$ change nothing
// (this port never freezes the stack; Secondary$ is a description flag).
//
// Ported from forge-game/src/main/java/forge/game/ability/effects/CopySpellAbilityEffect.java's
// resolve (CopySpellAbilityEffect.java:65-210) and CardFactory.java's
// copySpellHost/copySpellAbilityAndPossiblyHost (CardFactory.java:80-167).
type copySpellAbilityEffect struct{}

// copySpellUnresolvedParams are CopySpellAbilityEffect params this port does
// not resolve, each rejected before anything is copied (PORT-8, GO-7):
// CopyForEachCanTarget$/ChooseOnlyOne$ (CR 707.10d, one copy per other
// legal target), DefinedTarget$ (CR 707.10e), SingleChoice$, the copy's
// Layer 1 changes (NonLegendary$, SetPower$, SetToughness$, SetColor$,
// AddTypes$), Epic$, UseOriginalHost$, RememberCopies$ (remembers
// SpellAbility objects, which Memory cannot hold), TargetValidTargeting$,
// and Condition$/ConditionDefined$, which subAbilityConditionMet reads as
// never met, silently.
var copySpellUnresolvedParams = [...]string{
	"CopyForEachCanTarget", "ChooseOnlyOne", "DefinedTarget", "SingleChoice", "NonLegendary", "SetPower",
	"SetToughness", "SetColor", "AddTypes", "Epic", "UseOriginalHost", "RememberCopies", "TargetValidTargeting",
	"Condition", "ConditionDefined",
}

func (copySpellAbilityEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectParams(a, "CopySpellAbility", copySpellUnresolvedParams[:]...); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	amount, err := optionalAmount(g, a, "CopySpellAbility", "Amount", 1)
	if err != nil {
		return err
	}
	spells, err := copySpellTargets(g, a)
	if err != nil {
		return err
	}
	if len(spells) == 0 || amount == 0 {
		return nil
	}
	if battlefieldReplacementEvent(g, "CopySpell") || battlefieldStaticMode(g, "CantBeCopied") {
		return fmt.Errorf("engine: CopySpellAbility: CopySpell replacements or CantBeCopied statics not resolvable yet")
	}
	if g.copyTriggerWatching() {
		return fmt.Errorf("engine: CopySpellAbility: Mode$ SpellCopy/SpellCastOrCopy/SpellAbilityCopy triggers not resolvable yet")
	}
	copierSpec, ok := a.Params.Param("Controller")
	if !ok {
		copierSpec = "You"
	}
	copiers, err := definedPlayers(g, a.Controller, a.Source, copierSpec, a.refs())
	if err != nil {
		return fmt.Errorf("engine: CopySpellAbility: Controller$: %w", err)
	}
	optional := hasParam(a, "Optional")
	mayChoose := hasParam(a, "MayChooseTarget")
	// Every copy goes on the stack before any BecomesTarget trigger a copy's
	// targets raise: Java's MagicStack.add only hands those to the trigger
	// handler, whose abilities reach the stack when a player next receives
	// priority (addSimultaneousStackEntry), so under Amount$ 2 both copies
	// sit below every such trigger.
	var targeted []copiedTargets
	for _, copier := range copiers {
		for _, orig := range spells {
			if optional && !controller.ConfirmEffect(g, copier, a.Source) {
				continue
			}
			for i := 0; i < amount; i++ {
				host, tg, err := g.copySpell(controller, orig, copier, mayChoose)
				if err != nil {
					return err
				}
				if len(tg.targets) > 0 {
					targeted = append(targeted, tg)
				}
				if hasParam(a, "RememberNewCard") {
					// Re-read: NewCard (inside copySpell) may have grown the
					// arena and moved g.cards, so source (taken before the
					// loop) can point into a stale backing array whose
					// lazily-allocated Memory.remembered writes are lost
					// (effecteffect.go:143's own convention).
					g.Card(a.Source).Memory.Remember(CardEntity(host))
				}
			}
		}
	}
	for _, tg := range targeted {
		// isSpellSource as castInstantOrSorcery/castAura pass it: true only
		// for an Aura (becomesTargetSourceMatches reads .Aura as always true
		// under a spell source).
		g.pushTriggeredAbilities(controller, g.checkBecomesTargetTriggers(tg.targets, tg.aura, tg.copier))
	}
	return nil
}

// copiedTargets is one pushed copy's targets, held until every copy of the
// resolution is on the stack, for its BecomesTarget check.
type copiedTargets struct {
	targets []EntityID
	aura    bool
	copier  PlayerID
}

// copySpellTargets is getTargetSpells(sa): the spells a names, as stack
// items by value, in order, CantCopy$ ones dropped (SpellAbility.
// cantBeCopied). Each must still be on the stack.
func copySpellTargets(g *Game, a *Ability) ([]Ability, error) {
	var ids []StackItemID
	if _, ok := a.Params.Param("ValidTgts"); ok {
		if tt, ok := a.Params.Param("TargetType"); ok && tt != "Spell" {
			return nil, fmt.Errorf("engine: CopySpellAbility: TargetType$ %q not resolvable yet", tt)
		}
		ids = spellItemsOf(g, a.Targets)
	} else {
		defined, _ := a.Params.Param("Defined")
		switch defined {
		case "TriggeredSpellAbility":
			if a.triggered.spellAbility == NoStackItem {
				return nil, fmt.Errorf("engine: CopySpellAbility: Defined$ TriggeredSpellAbility: no triggering spell recorded")
			}
			ids = []StackItemID{a.triggered.spellAbility}
		case "Targeted":
			ids = spellItemsOf(g, a.Targets)
		default:
			// Parent (10 lines) needs the resolving root spell, which a
			// chained sub-ability does not carry; Remembered, Imprinted,
			// ValidStack and TriggeredSourceSA are getDefinedSpellAbilities
			// shapes of their own.
			return nil, fmt.Errorf("engine: CopySpellAbility: Defined$ %q not resolvable yet", defined)
		}
	}
	var out []Ability
	for _, id := range ids {
		item, ok := g.stackItem(id)
		if !ok {
			// Java copies from the spell object it still holds (last-known
			// information); this port keeps no stack item past its leaving.
			return nil, fmt.Errorf("engine: CopySpellAbility: copying a spell no longer on the stack not resolvable yet")
		}
		if item.Params != nil {
			if _, ok := item.Params.Param("CantCopy"); ok {
				continue
			}
		}
		out = append(out, *item)
	}
	return out, nil
}

// spellItemsOf is the stack items of the spells whose cards targets name,
// in target order. A target that is no longer a spell on the stack is
// skipped: it has resolved or left, and there is nothing to copy.
func spellItemsOf(g *Game, targets []EntityID) []StackItemID {
	var ids []StackItemID
	for _, t := range targets {
		card, ok := t.AsCard()
		if !ok {
			continue
		}
		if id, ok := g.spellItemOf(card); ok {
			ids = append(ids, id)
		}
	}
	return ids
}

// copyTriggerWatching reports whether any trait host has a trigger copying
// a spell would have to fire (MagicStack.add's TriggerType.SpellCopy,
// SpellAbilityCopy and SpellCastOrCopy runs, MagicStack.java:442-450) --
// none of the three modes is built.
func (g *Game) copyTriggerWatching() bool {
	for _, pid := range g.Players() {
		for _, id := range g.traitHosts(pid) {
			h := g.Card(id)
			if h.Def == nil {
				continue
			}
			for _, face := range h.Def.Faces {
				for _, t := range face.Triggers {
					switch strings.ToLower(t.Name) {
					case "spellcopy", "spellcastorcopy", "spellabilitycopy":
						return true
					}
				}
			}
		}
	}
	return false
}

// copySpell is CardFactory.copySpellAbilityAndPossiblyHost plus
// orderAndPlaySimultaneousSa's stack add, for one copy of orig under
// copier: a new card from the original's definition, owned by copier,
// straight into the stack zone and marked IsCopiedSpell (copySpellHost,
// CardFactory.java:80-118); the original's API, params, amounts, Charm modes
// and targets carried over (CR 707.10); no cost; then MayChooseTarget$'s new
// targets (CR 707.10c) and the push, which hands the copy its own
// StackItemID. Nothing cast-shaped runs (CR 707.10: not cast); the copy's
// targets come back for the caller's BecomesTarget check, which
// MagicStack.add raises for every added item.
//
// The original's card never carries a Layer 1 copy effect here: copy
// effects end when a permanent leaves the battlefield (endCopiesOnLeave), so
// its printed Def is its copiable values.
func (g *Game) copySpell(controller PlayerController, orig Ability, copier PlayerID, mayChoose bool) (CardID, copiedTargets, error) {
	host := g.NewCard(g.Card(orig.Source).Def, copier, Stack)
	g.Card(host).IsCopiedSpell = true
	cp := orig
	cp.ID = NoStackItem
	cp.Source, cp.Controller = host, copier
	cp.spell, cp.Optional = true, false
	cp.hostTransforms, cp.hasHostTransforms = 0, false
	cp.damageMap, cp.replacing = nil, nil
	cp.Targets = append([]EntityID(nil), orig.Targets...)
	if orig.Modes != nil {
		cp.Modes = make([]Ability, len(orig.Modes))
		for i, m := range orig.Modes {
			m.Source, m.Controller = host, copier
			m.Targets = append([]EntityID(nil), m.Targets...)
			cp.Modes[i] = m
		}
	}
	if mayChoose {
		if err := g.chooseCopyTargets(controller, &cp); err != nil {
			return NoCard, copiedTargets{}, err
		}
		for i := range cp.Modes {
			if err := g.chooseCopyTargets(controller, &cp.Modes[i]); err != nil {
				return NoCard, copiedTargets{}, err
			}
		}
	}
	g.PushAbility(cp)
	targets := append([]EntityID(nil), cp.Targets...)
	for _, m := range cp.Modes {
		targets = append(targets, m.Targets...)
	}
	if cp.Target != NoCard {
		targets = append(targets, CardEntity(cp.Target))
	}
	return host, copiedTargets{targets: targets, aura: cp.API == APIAttach, copier: copier}, nil
}

// chooseCopyTargets is SpellAbility.setupNewTargets for one targeting part
// of a copy (SpellAbility.java:2133-2152, CR 707.10c): the copier may keep
// its targets or choose new ones, from the same scan and bounds casting
// chose from (targetChoiceFor), evaluated for the copier. Two existing
// decisions, no new one: ConfirmEffect ("choose new targets?"), then
// ChooseTargets on a yes. An Aura copy's own target is the same choice over
// its enchant targets (ChooseEnchantTarget). A part with no targets, or no
// legal candidate now, keeps what it has.
func (g *Game) chooseCopyTargets(controller PlayerController, m *Ability) error {
	if m.API == APIAttach && m.Target != NoCard {
		spec, ok := enchantSpec(g.Card(m.Source))
		if !ok {
			return nil
		}
		eligible := g.enchantTargets(spec, m.Controller, m.Source)
		if len(eligible) == 0 || !controller.ConfirmEffect(g, m.Controller, m.Source) {
			return nil
		}
		chosen := controller.ChooseEnchantTarget(g, m.Controller, m.Source, eligible)
		if err := checkChoice([]CardID{chosen}, eligible, 1, 1); err != nil {
			return fmt.Errorf("engine: CopySpellAbility: new Aura target: %w", err)
		}
		m.Target = chosen
		return nil
	}
	if len(m.Targets) == 0 || m.Params == nil {
		return nil
	}
	choice, named, ok := g.targetChoiceFor(m)
	if choice.err != nil {
		return choice.err
	}
	if !named || !ok || !controller.ConfirmEffect(g, m.Controller, m.Source) {
		return nil
	}
	chosen := controller.ChooseTargets(g, m.Controller, choice.candidates, choice.min, choice.max)
	if err := checkChoice(chosen, choice.candidates, choice.min, choice.max); err != nil {
		return fmt.Errorf("engine: CopySpellAbility: new targets: %w", err)
	}
	m.Targets = chosen
	return nil
}
