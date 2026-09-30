package engine

//enginelint:allow id card game player ability defined condition control amount parts zone effecthelpers zonemove

import "fmt"

// digUntilUnresolvedParams are DigUntilEffect.java's params this port
// cannot honour yet: a non-library dig site (DigZone$), the MinTotalCMC$
// variant, entering-state/combat modifiers (AttachedTo$, Attacking$), and
// the ShuffleCondition$ values other than NoneFound.
var digUntilUnresolvedParams = [...]string{
	"DigZone", "MinTotalCMC", "AttachedTo", "Attacking", "Blocking", "ValidPlayer",
	"Condition", "ConditionDefined", "SorcerySpeed", "PlayerTurn", "CheckSVar",
}

// digUntilEffect is DigUntilEffect.java: each target player (default You)
// reveals cards from the top of their library until Amount$ (default 1)
// cards matching Valid$ are found, or MaxRevealed$ cards are revealed, or
// the library runs out. Found cards go to FoundDestination$ and the rest to
// RevealedDestination$ (NoneFoundDestination$ instead when too few were
// found). When both destinations are the same zone Java's "sequential" mode
// moves every revealed card in reveal order instead. Two or more rest cards
// into a known zone, or into a library that will not be shuffled, are
// ordered by the revealing player; RevealRandomOrder$ shuffles them with the
// game's own RNG. 153 real lines.
type digUntilEffect struct{}

func (digUntilEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	for _, key := range digUntilUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: DigUntil: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	until := 1
	if raw, ok := a.Params.Param("Amount"); ok {
		n, ok := resolveNamedAmount(g, a.Amounts, source, raw)
		if !ok {
			return fmt.Errorf("engine: DigUntil: Amount$ %q not resolvable yet", raw)
		}
		if n == 0 {
			return nil
		}
		until = n
	}
	maxRevealed := -1
	if raw, ok := a.Params.Param("MaxRevealed"); ok {
		n, ok := resolveNamedAmount(g, a.Amounts, source, raw)
		if !ok {
			return fmt.Errorf("engine: DigUntil: MaxRevealed$ %q not resolvable yet", raw)
		}
		maxRevealed = n
	}
	validSpec, ok := a.Params.Param("Valid")
	if !ok {
		validSpec = "Card"
	}
	foundDest, hasFound, err := digUntilZone(a, "FoundDestination")
	if err != nil {
		return err
	}
	revealedDest, hasRevealed, err := digUntilZone(a, "RevealedDestination")
	if err != nil {
		return err
	}
	noneFoundDest, hasNoneFound, err := digUntilZone(a, "NoneFoundDestination")
	if err != nil {
		return err
	}
	optNoDest, hasOptNoDest, err := digUntilZone(a, "OptionalNoDestination")
	if err != nil {
		return err
	}
	foundLibPos, err := digUntilLibPos(g, a, source, "FoundLibraryPosition")
	if err != nil {
		return err
	}
	revealedLibPos, err := digUntilLibPos(g, a, source, "RevealedLibraryPosition")
	if err != nil {
		return err
	}
	noneFoundLibPos, err := digUntilLibPos(g, a, source, "NoneFoundLibraryPosition")
	if err != nil {
		return err
	}
	_, shuffle := a.Params.Param("Shuffle")
	_, optional := a.Params.Param("Optional")
	_, optionalFound := a.Params.Param("OptionalFoundMove")
	_, noMoveFound := a.Params.Param("NoMoveFound")
	_, noMoveRevealed := a.Params.Param("NoMoveRevealed")
	_, randomOrder := a.Params.Param("RevealRandomOrder")
	_, tapped := a.Params.Param("Tapped")
	newController := NoPlayer
	if _, ok := a.Params.Param("GainControl"); ok {
		newController = a.Controller
	}
	sequential := hasRevealed && hasFound && revealedDest == foundDest

	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: DigUntil: %w", err)
	}
	for _, p := range players {
		if g.Player(p).Lost {
			continue
		}
		if optional && !controller.ConfirmEffect(g, p, a.Source) {
			continue
		}
		library := g.Zone(Library, p).Cards()
		maxToDig := len(library)
		if maxRevealed >= 0 && maxRevealed < maxToDig {
			maxToDig = maxRevealed
		}
		var found, revealed []CardID
		for i := 0; i < maxToDig; i++ {
			id := library[i]
			revealed = append(revealed, id)
			if len(filterValid(g, []CardID{id}, validSpec, a.Controller, a.Source)) == 0 {
				continue
			}
			found = append(found, id)
			if _, ok := a.Params.Param("ForgetOtherRemembered"); ok {
				source.Memory.ClearRemembered()
			}
			if _, ok := a.Params.Param("RememberFound"); ok {
				source.Memory.Remember(CardEntity(id))
			}
			if _, ok := a.Params.Param("ImprintFound"); ok {
				source.Memory.Imprint(id)
			}
			if len(found) == until {
				break
			}
		}
		pShuffle := shuffle
		if cond, ok := a.Params.Param("ShuffleCondition"); ok {
			if cond != "NoneFound" {
				return fmt.Errorf("engine: DigUntil: ShuffleCondition$ %q not resolvable yet", cond)
			}
			pShuffle = shuffle && len(found) == 0
		}

		if hasFound {
			dest := foundDest
			toMove := found
			if sequential {
				toMove = revealed
			}
			var kept []CardID
			for _, id := range toMove {
				if optionalFound && !controller.ConfirmEffect(g, p, a.Source) {
					if !hasOptNoDest {
						continue
					}
					dest = optNoDest
				}
				kept = append(kept, id)
				if noMoveFound && dest != Battlefield {
					continue
				}
				melded := g.moveByEffect(controller, id, dest, foundLibPos, newController, tapped && dest == Battlefield)
				if dest == Exile {
					g.markExiledWith(id, a.Source)
					if melded != NoCard {
						g.markExiledWith(melded, a.Source)
					}
				}
			}
			g.checkChangesZoneAllTriggers(controller, kept, Library, dest)
			revealed = withoutCards(revealed, found)
		}
		if _, ok := a.Params.Param("RememberRevealed"); ok {
			for _, id := range revealed {
				source.Memory.Remember(CardEntity(id))
			}
		}
		if _, ok := a.Params.Param("ImprintRevealed"); ok {
			for _, id := range revealed {
				source.Memory.Imprint(id)
			}
		}
		if randomOrder {
			revealed = append([]CardID(nil), revealed...)
			g.rand.Shuffle(len(revealed), func(i, j int) { revealed[i], revealed[j] = revealed[j], revealed[i] })
		}
		if !noMoveRevealed && !sequential && hasRevealed {
			finalDest, finalPos := revealedDest, revealedLibPos
			if hasNoneFound && len(found) < until {
				finalDest, finalPos = noneFoundDest, noneFoundLibPos
			}
			known := finalDest == Battlefield || finalDest == Graveyard || finalDest == Exile
			if (known || (finalDest == Library && !pShuffle && !randomOrder)) && len(revealed) >= 2 {
				ordered := controller.OrderCardsForZone(g, p, revealed, finalDest)
				if err := checkChoice(ordered, revealed, len(revealed), len(revealed)); err != nil {
					return fmt.Errorf("engine: DigUntil: %w", err)
				}
				revealed = ordered
			}
			for _, id := range revealed {
				melded := g.moveByEffect(controller, id, finalDest, finalPos, NoPlayer, false)
				if finalDest == Exile {
					g.markExiledWith(id, a.Source)
					if melded != NoCard {
						g.markExiledWith(melded, a.Source)
					}
				}
			}
			g.checkChangesZoneAllTriggers(controller, revealed, Library, finalDest)
		}
		if pShuffle {
			g.Shuffle(Library, p)
		}
	}
	return nil
}

func digUntilZone(a *Ability, key string) (ZoneType, bool, error) {
	raw, ok := a.Params.Param(key)
	if !ok {
		return 0, false, nil
	}
	z, err := changeZoneDestination(raw)
	if err != nil {
		return 0, false, fmt.Errorf("engine: DigUntil: %s$: %w", key, err)
	}
	return z, true, nil
}

// digUntilLibPos is Java's calculateAmount on a *LibraryPosition$ param,
// which is 0 (the top) when the param is absent.
func digUntilLibPos(g *Game, a *Ability, source *Card, key string) (int, error) {
	raw, ok := a.Params.Param(key)
	if !ok {
		return 0, nil
	}
	n, ok := resolveNamedAmount(g, a.Amounts, source, raw)
	if !ok || (n != 0 && n != libraryBottom) {
		return 0, fmt.Errorf("engine: DigUntil: %s$ %q not resolvable yet", key, raw)
	}
	return n, nil
}
