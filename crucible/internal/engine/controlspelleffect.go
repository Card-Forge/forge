package engine

//enginelint:allow ability card condition control defined effecthelpers game id stack gaincontroleffect parts zone player

import "fmt"

// controlSpellEffect is ControlSpellEffect.java: a player gains control of
// a spell on the stack (CR 110.2a: an effect can change a spell's
// controller). The new controller resolves it -- every "you" in its text,
// its SubAbility$ chain and each chosen Charm mode now means them -- and a
// permanent spell enters the battlefield under their control (CR 608.3a).
//
// Mode$ Gain (Aethersnatch, Commandeer, Invert Polarity) hands each named
// spell to NewController$'s first player, default the activator. Mode$
// Exchange (Perplexing Chimera) swaps the spell with the host: the host
// goes to the spell's controller and the spell to the host's controller,
// both under one timestamp, skipped when the host is no longer on the
// battlefield, is phased out, or the spell has left the stack. Remember$
// remembers the host (Exchange) and then the spell on the host.
//
// The spells are the ability's own targets (ValidTgts$ with TargetType$
// Spell, the card on the stack), Defined$ Targeted (a sub-ability sharing
// its parent's targets) or Defined$ TriggeredSpellAbility (the spell a
// Mode$ SpellCast trigger recorded).
//
// Card.runChangeControllerCommands (ControlSpellEffect.java:88, :96) has
// nothing to run here: the only change-controller command this port has is
// losing the Ring-bearer designation, which changeControllerAt already
// covers for the exchanged host, and a spell is never a Ring-bearer.
//
// Ported from forge-game/src/main/java/forge/game/ability/effects/ControlSpellEffect.java's
// resolve (ControlSpellEffect.java:54-101).
type controlSpellEffect struct{}

// controlSpellUnresolvedParams are rejected before anything changes (PORT-8,
// GO-7): RememberTargets$ (AbilityUtils.java:1517's generic pre-resolve
// remember, which this port does not run) and TargetValidTargeting$ (a
// canTargetSpellAbility filter targetChoiceFor reads for ChangeTargets
// only) are Chef's Kiss's line; DefinedExchange$ names an exchange object
// other than the host, which no corpus line does. Condition$ and
// ConditionDefined$ (Sudden Substitution's `ConditionDefined$ Remembered |
// ConditionPresent$ Card`) are SpellAbilityCondition shapes
// subAbilityConditionMet reads as never met, which would skip the steal
// silently.
var controlSpellUnresolvedParams = [...]string{
	"RememberTargets", "TargetValidTargeting", "DefinedExchange", "Condition", "ConditionDefined",
}

// namedSpell is one spell getTargetSpells names: its stack item, or
// NoStackItem once it has left the stack (Java's null stack instance), and
// its card, NoCard when only the vanished stack item is known.
type namedSpell struct {
	id   StackItemID
	card CardID
}

func (controlSpellEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if a.targetsErr != nil {
		return a.targetsErr
	}
	if err := rejectParams(a, "ControlSpell", controlSpellUnresolvedParams[:]...); err != nil {
		return err
	}
	mode, ok := a.Params.Param("Mode")
	if !ok {
		// ControlSpellEffect.java:58 calls equals on the missing param.
		return fmt.Errorf("engine: ControlSpell: Mode$ is required")
	}
	if mode != "Gain" && mode != "Exchange" {
		return fmt.Errorf("engine: ControlSpell: Mode$ %q not resolvable yet", mode)
	}
	exchange := mode == "Exchange"
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	if exchange && battlefieldStaticMode(g, "CantGainControl") {
		// Card.canBeControlledBy's StaticAbilityCantGainControl half.
		return fmt.Errorf("engine: ControlSpell: CantGainControl statics not resolvable yet")
	}
	newController, err := controlSpellNewController(g, a)
	if err != nil {
		return err
	}
	spells, err := controlSpellTargets(g, a)
	if err != nil {
		return err
	}
	remember := hasParam(a, "Remember")
	for _, s := range spells {
		g.timestamp++
		ts := g.timestamp
		item, onStack := g.stackItem(s.id)
		if exchange {
			// ControlSpellEffect.java:69-90: the host is the exchange
			// object (DefinedExchange$ absent defaults to Self).
			if source.Zone != Battlefield || source.IsPhasedOut() || !onStack {
				continue
			}
			if g.Player(item.Controller).Lost {
				// canBeControlledBy's isInGame half.
				continue
			}
			if remember {
				source.Memory.Remember(CardEntity(a.Source))
			}
			newController = source.Controller()
			g.changeControllerAt(a.Source, item.Controller, ts)
		}
		if !onStack {
			// ControlSpellEffect.java:99 dereferences the missing stack
			// instance; no corpus chain reaches it, since every Gain line's
			// spell is a target its own parent fizzles without.
			return fmt.Errorf("engine: ControlSpell: the named spell is no longer on the stack")
		}
		if remember {
			source.Memory.Remember(CardEntity(s.card))
		}
		g.giveSpell(item, s.card, newController, ts)
	}
	return nil
}

// controlSpellNewController is getDefinedPlayersOrTargeted(sa,
// "NewController")'s first player, else the activator: a targeting
// ability naming no NewController$ reads its own player targets.
func controlSpellNewController(g *Game, a *Ability) (PlayerID, error) {
	if spec, ok := a.Params.Param("NewController"); ok {
		players, err := definedPlayers(g, a.Controller, a.Source, spec, a.refs())
		if err != nil {
			return NoPlayer, fmt.Errorf("engine: ControlSpell: NewController$: %w", err)
		}
		if len(players) > 0 {
			return players[0], nil
		}
		return a.Controller, nil
	}
	if hasParam(a, "ValidTgts") {
		for _, t := range a.Targets {
			if pid, ok := t.AsPlayer(); ok {
				return pid, nil
			}
		}
	}
	return a.Controller, nil
}

// controlSpellTargets is getTargetSpells(sa): a's own spell targets (a card
// on the stack, spellItemOf), or Defined$ Targeted (the same list, a
// sub-ability sharing its parent's targets) or TriggeredSpellAbility (the
// spell a Mode$ SpellCast trigger recorded, named even after it left the
// stack -- getDefinedSpellAbilities returns the SpellAbility itself).
func controlSpellTargets(g *Game, a *Ability) ([]namedSpell, error) {
	if !hasParam(a, "ValidTgts") {
		defined, _ := a.Params.Param("Defined")
		switch defined {
		case "Targeted":
		case "TriggeredSpellAbility":
			id := a.triggered.spellAbility
			if id == NoStackItem {
				return nil, fmt.Errorf("engine: ControlSpell: Defined$ TriggeredSpellAbility: no triggering spell recorded")
			}
			spell := namedSpell{id: NoStackItem, card: NoCard}
			if item, ok := g.stackItem(id); ok {
				spell = namedSpell{id: id, card: item.Source}
			}
			return []namedSpell{spell}, nil
		default:
			return nil, fmt.Errorf("engine: ControlSpell: Defined$ %q not resolvable yet", defined)
		}
	}
	var spells []namedSpell
	for _, t := range a.Targets {
		card, ok := t.AsCard()
		if !ok {
			continue
		}
		id, ok := g.spellItemOf(card)
		if !ok {
			id = NoStackItem
		}
		spells = append(spells, namedSpell{id: id, card: card})
	}
	return spells, nil
}

// giveSpell is the spell half of ControlSpellEffect.java:92-99: the card
// gains a timestamped controller (Card.addTempController -- it rides along
// onto the battlefield, Game.Move leaving a Stack card's control alone
// there, and is cleared anywhere else), and the stack item and every chosen
// Charm mode resolve as to's (SpellAbilityStackInstance.setActivatingPlayer,
// which trickles into sub-abilities; this port builds a SubAbility$ child
// from its parent's Controller at resolution, so only the stored modes need
// rewriting). Modes is copied first: Game.Clone shares the stack's Modes
// backing array.
func (g *Game) giveSpell(item *Ability, card CardID, to PlayerID, ts uint64) {
	c := g.Card(card)
	c.tempControllers = append(c.tempControllers, ControlEffect{Timestamp: ts, Controller: to})
	item.Controller = to
	if item.Modes != nil {
		item.Modes = append([]Ability(nil), item.Modes...)
		for i := range item.Modes {
			item.Modes[i].Controller = to
		}
	}
}
