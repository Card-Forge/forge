package engine

//enginelint:allow ability additional card condition control defined effecthelpers game id parts replacement zone

import (
	"fmt"
	"sort"
	"strconv"
	"strings"
)

// rollDiceUnresolvedParams are RollDiceEffect params this port does not
// model: rerolls, attraction visits, stored rolls, a chosen result, and
// the per-roll sub-abilities.
var rollDiceUnresolvedParams = [...]string{
	"RerollResults", "ToVisitYourAttractions", "StoreResults", "ChosenSVar", "OtherSVar",
	"Condition"}

// rollDiceEffect is RollDiceEffect.java (CR 706): each target or Defined$
// player (default You) rolls Amount$ (default 1) Sides$-sided dice (default
// 6), each nextInt(sides)+1 on the game's stream, sorted, the IgnoreLower$
// lowest set aside, Modifier$ added to each. The total (or, with
// UseDifferenceBetweenRolls$, highest minus lowest; with UseHighestRoll$,
// the highest alone) picks the ResultSubAbilities$ entry whose "n" or
// "lo-hi" key covers it, else Else$ -- once per roll with SubsForEach$.
// ResultSVar$, EvenOddResults$ (EvenResults/OddResults),
// DifferentResults$, MaxRollsResults$ (MaxRolls) and NoteDoubles$ (Doubles)
// are bound for this ability's sub-abilities, Java's sa.setSVar.
// RememberHighestPlayer$ remembers the highest roller.
//
// Every card that could change a roll -- a Monitor Monitor reroll,
// Xenosquirrels/Night Shift increments, a RollDice replacement (roll twice
// and ignore one, Vedalken exchange) -- fails the roll closed rather than
// letting it land unmodified; Mode$ RolledDie/RolledDieOnce triggers are
// not ported.
type rollDiceEffect struct{}

func (rollDiceEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectParams(a, "RollDice", rollDiceUnresolvedParams[:]...); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	if diceModifierInPlay(g) {
		return fmt.Errorf("engine: RollDice: a card that changes die rolls is in play, not resolvable yet")
	}
	amount, err := optionalAmount(g, a, "RollDice", "Amount", 1)
	if err != nil {
		return err
	}
	sides, err := optionalAmount(g, a, "RollDice", "Sides", 6)
	if err != nil {
		return err
	}
	modifier, err := optionalAmount(g, a, "RollDice", "Modifier", 0)
	if err != nil {
		return err
	}
	ignore, err := optionalAmount(g, a, "RollDice", "IgnoreLower", 0)
	if err != nil {
		return err
	}
	if sides < 1 {
		return fmt.Errorf("engine: RollDice: %d sides", sides)
	}
	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return err
	}
	players = g.inAPNAPOrder(players)
	var totals []int
	for _, pid := range players {
		total, err := g.rollDiceFor(a, controller, pid, amount, sides, ignore, modifier)
		if err != nil {
			return err
		}
		totals = append(totals, total)
	}
	if hasParam(a, "RememberHighestPlayer") {
		highest := 0
		for _, t := range totals {
			if t > highest {
				highest = t
			}
		}
		for i, t := range totals {
			if t == highest {
				source.Memory.Remember(PlayerEntity(players[i]))
			}
		}
	}
	return nil
}

// rollDiceFor is one player's rollDice: the rolls, the bound SVars, the
// result sub-ability. It answers the total.
func (g *Game) rollDiceFor(a *Ability, controller PlayerController, pid PlayerID, amount, sides, ignore, modifier int) (int, error) {
	if amount <= 0 {
		return 0, nil
	}
	// ReplacementType.RollDice runs before the dice are rolled; Updated edits the
	// number of dice and the lowest rolls ignored (RollDiceEffect.rollAction).
	amount, ignore = g.rollReplaced(controller, pid, amount, sides, ignore)
	var rolls []int
	for i := 0; i < amount; i++ {
		rolls = append(rolls, int(g.rand.Int32n(int32(sides)))+1)
	}
	sort.Ints(rolls)
	if ignore > len(rolls) {
		ignore = len(rolls)
	}
	natural := rolls[ignore:]
	if hasParam(a, "UseHighestRoll") && len(natural) > 0 {
		natural = natural[len(natural)-1:]
	}
	var final []int
	even, odd, different, maxRolls := 0, 0, 0, 0
	seen := map[int]bool{}
	doubles := false
	total := 0
	for _, n := range natural {
		m := n + modifier
		if !seen[m] {
			different++
		} else {
			doubles = true
		}
		seen[m] = true
		if m%2 == 0 {
			even++
		} else {
			odd++
		}
		if n == sides {
			maxRolls++
		}
		final = append(final, m)
		total += m
	}
	if hasParam(a, "UseDifferenceBetweenRolls") && len(final) > 0 {
		total = final[len(final)-1] - final[0]
	}
	bind := func(name string, v int) { a.Amounts = withAmount(a.Amounts, name, v) }
	if hasParam(a, "EvenOddResults") {
		bind("EvenResults", even)
		bind("OddResults", odd)
	}
	if hasParam(a, "DifferentResults") {
		bind("DifferentResults", different)
	}
	if hasParam(a, "MaxRollsResults") {
		bind("MaxRolls", maxRolls)
	}
	if name, ok := a.Params.Param("ResultSVar"); ok {
		bind(name, total)
	}
	if hasParam(a, "SubsForEach") {
		for _, r := range final {
			if err := g.resolveDiceResult(a, controller, r); err != nil {
				return 0, err
			}
		}
	} else if err := g.resolveDiceResult(a, controller, total); err != nil {
		return 0, err
	}
	if hasParam(a, "NoteDoubles") {
		// RollDiceEffect.java:526-533 sets Doubles only on a double; a sub-ability
		// reading it unset gets calculateAmount's 0 for an undefined SVar
		// (AbilityUtils.java:415-423), and a later player's roll never clears
		// an earlier one's 1.
		if doubles {
			bind("Doubles", 1)
		} else if _, set := a.Amounts["doubles"]; !set {
			bind("Doubles", 0)
		}
	}
	return total, nil
}

// resolveDiceResult is resolveSub (RollDiceEffect.java:459-481): the
// ResultSubAbilities$ entry whose key ("n" or "lo-hi") covers num, else the
// Else ability. AbilityFactory.java:265-271 makes each ResultSubAbilities$
// "key:SVar" pair an additional ability under its key, so an "Else:SVar"
// pair there is that Else ability (Sword of Dungeons & Dragons'
// "20:Win,Else:Lose").
func (g *Game) resolveDiceResult(a *Ability, controller PlayerController, num int) error {
	elseSVar := ""
	if raw, ok := a.Params.Param("ResultSubAbilities"); ok {
		for _, pair := range strings.Split(raw, ",") {
			key, svar, ok := strings.Cut(strings.TrimSpace(pair), ":")
			key, svar = strings.TrimSpace(key), strings.TrimSpace(svar)
			if ok && key == "Else" {
				elseSVar = svar
			}
			if !ok || !diceKeyCovers(key, num) {
				continue
			}
			return g.resolveDiceSub(a, controller, key, svar)
		}
	}
	if elseSVar != "" {
		return g.resolveDiceSub(a, controller, "Else", elseSVar)
	}
	if subs := additionalAbilities(a.Params, "Else"); len(subs) > 0 {
		return g.resolveAdditional(a, controller, subs[0])
	}
	return nil
}

// resolveDiceSub resolves the ResultSubAbilities$ ability compiled from svar,
// the one key names.
func (g *Game) resolveDiceSub(a *Ability, controller PlayerController, key, svar string) error {
	for _, sub := range additionalAbilities(a.Params, "ResultSubAbilities") {
		if sub.SVar == svar {
			return g.resolveAdditional(a, controller, sub)
		}
	}
	return fmt.Errorf("engine: RollDice: result %s names no compiled SVar %q", key, svar)
}

func diceKeyCovers(key string, num int) bool {
	if lo, hi, isRange := strings.Cut(key, "-"); isRange {
		l, err1 := strconv.Atoi(lo)
		h, err2 := strconv.Atoi(hi)
		return err1 == nil && err2 == nil && l <= num && num <= h
	}
	n, err := strconv.Atoi(key)
	return err == nil && n == num
}

// diceModifierInPlay reports whether a card anywhere a static or
// replacement can work from could change a die roll: a RollDice
// replacement, or the reroll/increment keywords getRerollCards and
// getIncrementCards look for.
func diceModifierInPlay(g *Game) bool {
	for _, p := range g.Players() {
		for _, z := range []ZoneType{Battlefield, Command, Graveyard, Exile, Hand} {
			for _, id := range g.Zone(z, p).Cards() {
				c := g.Card(id)
				if c.Def == nil {
					continue
				}
				for _, face := range c.traitFaces() {
					for _, r := range face.Replacements {
						// A line rollReplaced runs (Pixie Guide's extra die) is not a
						// modifier this check refuses; Vedalken's exchange is.
						if strings.EqualFold(r.Name, "RollDice") && !rollReplacementResolvable(r) {
							return true
						}
					}
				}
				for _, line := range c.KeywordLines() {
					if strings.Contains(line, "reroll one or more dice") || strings.HasPrefix(line, "After you roll a die") {
						return true
					}
				}
			}
		}
	}
	return false
}
