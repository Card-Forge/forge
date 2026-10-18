package engine

//enginelint:allow id card game ability defined condition control amount parts zone valid effecthelpers scheduledaction

import (
	"fmt"
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/valid"
)

// chooseCardUnresolvedParams are ChooseCardEffect.java's params this port
// cannot honour yet: the alternative selection loops (ChooseEach$/
// EachBasicType$/WithTotalPower$/WithDifferentPowers$/EachDifferentPower$/
// ControlAndNot$), the non-default choice pools (AllCards$,
// IncludeSpellsOnStack$, TargetControls$) and ChosenMap$'s per-player map.
// 399 real (AB|DB)$ lines; the default chooseCardsForEffect shape covers
// most of them. QuasiLibrarySearch$ resolves (chooseCardQuasiSearch);
// AtRandom$ is Aggregates.random(pChoices, Amount) over the pool in its scan
// order (ChooseCardEffect.java:225-228); Secretly$ only moves Reveal$'s
// display to after every chooser (:275-288), no state.
var chooseCardUnresolvedParams = [...]string{
	"ChooseEach", "EachBasicType", "WithTotalPower",
	"WithDifferentPowers", "EachDifferentPower", "ControlAndNot",
	"AllCards", "IncludeSpellsOnStack", "TargetControls",
	"ChosenMap", "ChoiceTitleAppend", "StartingWith", "Optional",
	"UnlessResolveSubs", "LockInText", "OrString",
	"Condition", "SorcerySpeed", "ModeCost",
	"ConditionManaNotSpent", "ConditionGameTypes",
}

// chooseCardEffect is ChooseCardEffect.java's default shape: each chooser
// (Defined$/ValidTgts$, default You) picks between MinAmount$ and Amount$
// cards (both default 1) from the ChoiceZone$ cards (default Battlefield)
// matching Choices$, or from DefinedCards$ outright. The union becomes the
// host's chosen cards (Memory.Choose, read back by Defined$ ChosenCard and
// the ChosenCard valid property), then RememberChosen$/ImprintChosen$/
// ForgetChosen$/ForgetOtherRemembered$ update Memory the same way Java's own
// trailing block does. Without Mandatory$ the choice may be empty, Java's
// own isOptional = !Mandatory.
type chooseCardEffect struct{}

func (chooseCardEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	for _, key := range chooseCardUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: ChooseCard: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	choosers, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: ChooseCard: %w", err)
	}

	choices, err := chooseCardPool(g, a, source)
	if err != nil {
		return err
	}

	amountValue, ok := a.Params.Param("Amount")
	if !ok {
		amountValue = "1"
	}
	maxAmount, ok := resolveNamedAmount(g, a.Amounts, source, amountValue)
	if !ok {
		return fmt.Errorf("engine: ChooseCard: Amount$ %q not resolvable yet", amountValue)
	}
	minAmount := maxAmount
	if raw, ok := a.Params.Param("MinAmount"); ok {
		n, err := strconv.Atoi(raw)
		if err != nil {
			return fmt.Errorf("engine: ChooseCard: MinAmount$ %q is not an integer", raw)
		}
		minAmount = n
	}
	if maxAmount <= 0 {
		return nil
	}
	if _, mandatory := a.Params.Param("Mandatory"); !mandatory {
		minAmount = 0
	}

	quasi, err := chooseCardQuasiSearch(g, a, choosers)
	if err != nil {
		return err
	}

	var all []CardID
	for _, pid := range choosers {
		pChoices := choices
		if by, ok := a.Params.Param("ControlledByPlayer"); ok {
			if by != "Chooser" {
				return fmt.Errorf("engine: ChooseCard: ControlledByPlayer$ %q not resolvable yet", by)
			}
			pChoices = nil
			for _, cid := range choices {
				if g.Card(cid).Controller() == pid {
					pChoices = append(pChoices, cid)
				}
			}
		}
		if hasParam(a, "AtRandom") && len(pChoices) > 0 {
			// Aggregates.random's reservoir: Amount$ cards, the sample's order
			// (ChooseCardEffect.java:225-228, ahead of the default and quasi
			// search branches).
			for _, i := range g.randomSample(len(pChoices), maxAmount) {
				all = append(all, pChoices[i])
			}
			continue
		}
		if quasi {
			// ChooseCardEffect.java:260-265: one card, optional unless
			// Mandatory$; nothing chosen -- an empty pool, which Java's
			// chooseSingleEntityForEffect answers null without asking, or a
			// decline -- returns from the whole effect, so the host's chosen,
			// remembered and imprinted cards stay as they were.
			if len(pChoices) == 0 {
				return nil
			}
			lo := 0
			if _, mandatory := a.Params.Param("Mandatory"); mandatory {
				lo = 1
			}
			chosen := controller.ChooseCardsForEffect(g, pid, a.Source, pChoices, lo, 1)
			if err := checkChoice(chosen, pChoices, lo, 1); err != nil {
				return fmt.Errorf("engine: ChooseCard: %w", err)
			}
			if len(chosen) == 0 {
				return nil
			}
			all = append(all, chosen...)
			continue
		}
		lo, hi := minAmount, maxAmount
		if hi > len(pChoices) {
			hi = len(pChoices)
		}
		if lo > hi {
			lo = hi
		}
		chosen := controller.ChooseCardsForEffect(g, pid, a.Source, pChoices, lo, hi)
		if err := checkChoice(chosen, pChoices, lo, hi); err != nil {
			return fmt.Errorf("engine: ChooseCard: %w", err)
		}
		all = append(all, chosen...)
	}

	m := &source.Memory
	m.ClearChosen()
	for _, cid := range all {
		m.Choose(cid)
	}
	if _, ok := a.Params.Param("ForgetOtherRemembered"); ok {
		m.ClearRemembered()
	}
	if _, ok := a.Params.Param("RememberChosen"); ok {
		for _, cid := range all {
			m.Remember(CardEntity(cid))
		}
	}
	if _, ok := a.Params.Param("ForgetChosen"); ok {
		for _, cid := range all {
			m.Forget(CardEntity(cid))
		}
	}
	if _, ok := a.Params.Param("ImprintChosen"); ok {
		for _, cid := range all {
			m.Imprint(cid)
		}
	}
	return nil
}

// chooseCardQuasiSearch reads QuasiLibrarySearch$ (ChooseCardEffect.java:
// 243-269): the choice is "search <player>'s library" -- Doubling Chant,
// Clarion Ultimatum, Dichotomancy pick one card from ChoiceZone$ Library --
// and reports whether the ability names it.
//
// The param's player is only the library Java reveals before the pick
// (DelayedReveal, :253-259), nothing this omniscient engine shows; an empty
// one is an error where Java's get(0) throws. Java checks neither
// Player.canSearchLibraryWith (Leonin Arbiter) nor LimitSearchLibrary (Aven
// Mindcensor: :254-256 shortens only the reveal, never the choices), so
// neither is read here (forge-java-defects.md).
//
// A chooser who is the activator and whose searches another player controls
// (ControlOpponentsSearchingLibrary$, ADR-0040) is refused: Java pushes the
// control grant at :245-251 and pops it at :267-269, after the `return` a
// declined or empty pick takes at :262-264, so a decline leaves the grant on
// for good. That is a Forge bug (PORT-8); reproducing it would leave another
// player controlling this one, so the shape is refused before anything is
// asked.
func chooseCardQuasiSearch(g *Game, a *Ability, choosers []PlayerID) (bool, error) {
	spec, ok := a.Params.Param("QuasiLibrarySearch")
	if !ok {
		return false, nil
	}
	searched, err := definedPlayers(g, a.Controller, a.Source, spec, a.refs())
	if err != nil {
		return false, fmt.Errorf("engine: ChooseCard: QuasiLibrarySearch$: %w", err)
	}
	if len(searched) == 0 {
		return false, fmt.Errorf("engine: ChooseCard: QuasiLibrarySearch$ %q names no player", spec)
	}
	for _, pid := range choosers {
		if who, _ := g.SearchController(pid); pid == a.Controller && who != NoPlayer {
			return false, fmt.Errorf("engine: ChooseCard: QuasiLibrarySearch$ under a ControlOpponentsSearchingLibrary$ redirect not resolvable yet (ChooseCardEffect.java:262-269 never ends the grant on a decline)")
		}
	}
	return true, nil
}

// chooseCardPool is the candidate list ChooseCardEffect.java builds before
// asking anyone: DefinedCards$ outright when present, otherwise every card
// in the ChoiceZone$ zones (comma-separated, default Battlefield) filtered
// by Choices$, in seat order per zone -- game.getCardsIn's own order.
func chooseCardPool(g *Game, a *Ability, source *Card) ([]CardID, error) {
	if defined, ok := a.Params.Param("DefinedCards"); ok {
		cards, err := definedCards(source, defined, a.refs())
		if err != nil {
			return nil, fmt.Errorf("engine: ChooseCard: DefinedCards$: %w", err)
		}
		return cards, nil
	}
	zones := []ZoneType{Battlefield}
	if raw, ok := a.Params.Param("ChoiceZone"); ok {
		zones = nil
		for _, name := range strings.Split(raw, ",") {
			z, ok := ZoneByName(strings.TrimSpace(name))
			if !ok {
				return nil, fmt.Errorf("engine: ChooseCard: ChoiceZone$ %q not resolvable yet", name)
			}
			zones = append(zones, z)
		}
	}
	choicesParam, hasChoices := a.Params.Param("Choices")
	var spec valid.Spec
	if hasChoices {
		spec = valid.Parse(choicesParam)
	}
	var pool []CardID
	for _, z := range zones {
		for _, pid := range g.Players() {
			for _, cid := range g.Zone(z, pid).Cards() {
				if !hasChoices || Matches(g, g.Card(cid), spec, a.Controller, a.Source) {
					pool = append(pool, cid)
				}
			}
		}
	}
	return pool, nil
}
