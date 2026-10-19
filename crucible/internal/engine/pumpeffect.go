// Pump: CR 611's own biggest single script-driven effect by real corpus
// count after ChangeZone/Draw -- 4,103 real (AB|DB)$ Pump lines, 1,335 of
// them naming Defined$ Self/Enchanted/Equipped rather than a target, 1,148
// of THOSE resolvable here (2 of them past resolveUnlessCost's own gate,
// effect.go, below). This is the first script-driven effect whose own
// contribution outlives its own Resolve call: "until end of turn" is a
// continuous effect this port never needed a duration for before (Card.PT's
// own doc comment, PT.Clear -- "an until end of turn pump wearing off on its
// own is a separate, unbuilt mechanic"), closed by pumpRecord (game.go) and
// applyPumpEffects (continuous.go) rather than anything in this file.
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/PumpEffect.java's
// resolve/applyPump.

package engine

import (
	"fmt"
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// pumpUnresolvedParams names PumpEffect.resolve's own params past
// NumAtt$/NumDef$/KW$/Duration$/PumpZone$/Defined$ this port does not
// evaluate. Every one fails the whole line loudly rather than applying half
// of it and guessing at the rest (PORT-8/GO-7): Condition$/ConditionDefined$/ConditionZone$/
// ConditionPlayerTurn$/ConditionActivationLimit$ (0/19/0/0/4) --
// SpellAbilityCondition's own shapes subAbilityConditionMet does not cover,
// the identical DealDamage/GainLife-shaped gap;
// CanBlockAmount$/CanBlockAny$ (4/0) -- an additional-blocker grant this
// port's own block-legality gate
// (staticability.go) has nowhere to consult a one-shot record from;
// KWChoice$/RandomKeyword$/RandomKWNum$/NoRepetition$ (3/1/0/0) -- an
// interactive choice and a random draw, neither of which this port's own KW$
// handling (below) does (DefinedKW$ is pumpDefinedKeywords'); SharedKeywordsZone$/
// SharedRestrictions$ (2/2) -- CardFactoryUtil.sharedKeywords' own zone scan,
// a further mechanic; AtEOT$ (9) -- registerDelayedTrigger, a new
// trigger this effect would silently fail to create; DefinedLandwalk$/
// RememberPumped$/LeaveBattlefield$ (0/0) -- each its own further
// tracking mechanic; NoteCards$/NoteCardsFor$/ClearNotedCardsFor$/
// NoteNumber$ (0/0/0/0) -- Player.noteNumberForName's own tracking, nothing
// downstream reads yet;
// Optional$/OptionQuestion$ (0/0) -- a "may" confirmation this port's own
// PlayerController has no hook for.
//
// SubAbility$ no longer blocks: resolveSubAbility (subability.go) chains it
// through Registry.Resolve (effect.go) once this effect's own body
// finishes, whether or not subAbilityConditionMet let it run at all --
// rabaroo_troop.txt's own real Pump-chaining-into-GainLife shape is why. 17
// of the corpus's own 571 real SVar-defined Pump lines naming SubAbility$
// chain to an already-built leaf ability and resolve end to end.
//
// UnlessCost$/UnlessPayer$/UnlessSwitched$ no longer block either:
// resolveUnlessCost (effect.go) gates the whole ability, this effect's own
// body included, before Registry.Resolve ever reaches it -- CR 601.2i's own
// "unless a cost is paid." 2 of the corpus's own 15 real Pump lines naming
// UnlessCost$ resolve past that gate and are actually reachable at all --
// spitting_slug.txt's own real "gains first strike... unless you pay
// {1}{G}" (Mode$ AttackerBlocked/Blocks, both already built), chaining its
// own UnlessResolveSubs$ WhenNotPaid into PumpAll (pumpalleffect.go) when
// the cost goes unpaid, and nakaya_shade.txt's own real activated {B}:
// ability itself gated by its own nested "unless any player pays {2}" --
// reachable once ActivateAbility (activateability.go) landed too, its own
// outer Cost$ B activation and this file's own inner UnlessCost$ composing
// with no special-casing, since ActivateAbility threads the ability's own
// compiled Params (UnlessCost$ included) through unchanged. Of the 3 real
// Pump lines clearing the pure-mana-cost/resolvable-payer filter itself
// (resolveUnlessCost's own doc comment, effect.go), the third is
// wild_might.txt's own spell-level ValidTgts$, reachable since a cast
// spell's targets resolve (ADR-0018).
var pumpUnresolvedParams = [...]string{
	"Condition", "ConditionZone", "ConditionPlayerTurn",
	"ConditionActivationLimit",
	"CanBlockAmount", "CanBlockAny", "KWChoice", "RandomKeyword", "RandomKWNum",
	"NoRepetition", "SharedKeywordsZone", "SharedRestrictions", "AtEOT",
	"DefinedLandwalk", "RememberPumped", "LeaveBattlefield",
	"NoteCards", "NoteCardsFor", "ClearNotedCardsFor",
	"NoteNumber", "Optional", "OptionQuestion",
}

// pumpImprint is PumpEffect.java:431-437: ImprintCards$ adds the defined
// cards to the host's imprinted list and ForgetImprinted$ removes them
// (Mystic Reflection's DBImprint, which counts what its replacement copied).
func pumpImprint(a *Ability, host *Card, key string, forget bool) error {
	spec, ok := a.Params.Param(key)
	if !ok {
		return nil
	}
	cards, err := definedCards(host, spec, a.refs())
	if err != nil {
		return fmt.Errorf("engine: Pump: %s$: %w", key, err)
	}
	for _, id := range cards {
		if forget {
			host.Memory.forgetImprinted(id)
		} else {
			host.Memory.Imprint(id)
		}
	}
	return nil
}

// pumpEffect resolves SP$/DB$/AB$ Pump on its Defined$ cards (Self by
// default) or, for a ValidTgts$ line, its chosen card targets
// (targetedOrDefinedCards, defined.go; PumpEffect.java's
// getCardsfromTargets) -- 2,555 of the corpus's 5,034 (SP|AB|DB)$ Pump lines
// name ValidTgts$ (Giant Growth's shape), 2,318 of them no unresolved param.
// A target that phased out or left PumpZone$ (Battlefield by default) since
// it was chosen is skipped, as Java's own loop does. ConditionPresent$/ConditionCompare$/ConditionCheckSVar$/
// ConditionSVarCompare$ are resolved through subAbilityConditionMet
// (condition.go), the identical way DealDamage's/GainLife's own do.
type pumpEffect struct{}

func (pumpEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	for _, key := range pumpUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: Pump: %s$ not resolvable yet", key)
		}
	}
	if err := checkReplaceDying(g, a, "Pump"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}

	// A host-bound Duration$ (AsLongAsControl) keeps the record past cleanup and
	// ends it with a command on the host (addUntilCommand,
	// SpellAbilityEffect.java:1007-1017); checkValidDuration runs first
	// (PumpEffect.java:271).
	permanent, hostBound, untilNextTurn, perpetual := false, "", false, false
	if d, ok := a.Params.Param("Duration"); ok {
		switch {
		case d == "Permanent":
			permanent = true
		case d == "Perpetual":
			permanent, perpetual = true, true
		case d == "UntilYourNextTurn":
			// SpellAbilityEffect.addUntilCommand: the untap phase of the
			// activator's next turn (PhaseHandler.java:252).
			permanent, untilNextTurn = true, true
		case hostBoundDuration(d):
			if !validHostDuration(a, source, d) {
				return nil
			}
			permanent, hostBound = true, d
		default:
			return fmt.Errorf("engine: Pump: Duration$ %q not resolvable yet", d)
		}
	}

	power, err := pumpAmount(g, "Pump", a, source, "NumAtt")
	if err != nil {
		return err
	}
	toughness, err := pumpAmount(g, "Pump", a, source, "NumDef")
	if err != nil {
		return err
	}

	grant, err := pumpKeywords("Pump", a.Params)
	if err != nil {
		return err
	}
	keywords, proceed, err := g.pumpDefinedKeywords(a, source, grant.keywords)
	if err != nil {
		return err
	}
	if !proceed {
		return nil
	}
	grant.keywords = keywords
	if perpetual && (len(grant.hidden) > 0 || grant.switched) {
		// Java keeps HIDDEN keywords out of the perpetual list
		// (PumpEffect.java:40-50, :82), so they would not survive a zone change.
		return fmt.Errorf("engine: Pump: Duration$ Perpetual with a HIDDEN keyword not resolvable yet")
	}
	players, err := g.pumpPlayers(a, grant)
	if err != nil {
		return err
	}

	// PumpEffect.java:401-402 and :427-428: RememberObjects$ then
	// ForgetObjects$, whether or not the line pumps anything (Stolen
	// Uniform's Pump only remembers its target).
	if err := pumpRememberObjects(g, a, source, "RememberObjects", false); err != nil {
		return err
	}
	if err := pumpRememberObjects(g, a, source, "ForgetObjects", true); err != nil {
		return err
	}

	if err := pumpImprint(a, source, "ImprintCards", false); err != nil {
		return err
	}
	if err := pumpImprint(a, source, "ForgetImprinted", true); err != nil {
		return err
	}

	// NumAtt$/NumDef$ Double or Triple is read per target (pumpMultiplier).
	multiplied := false
	for _, key := range [...]string{"NumAtt", "NumDef"} {
		if v, _ := a.Params.Param(key); v == "Double" || v == "Triple" {
			multiplied = true
		}
	}
	if _, radiance := a.Params.Param("Radiance"); multiplied && radiance {
		// Java's Radiance loop reuses whatever the last target's stat made the
		// amount (PumpEffect.java:489-496); no corpus line combines them.
		return fmt.Errorf("engine: Pump: Radiance$ with a Double/Triple amount not resolvable yet")
	}
	if power == 0 && toughness == 0 && grant.empty() && !multiplied {
		// Java has no such return: replaceDying (PumpEffect.java:506) still
		// runs, so an X=0 Necrotic Wound still exiles a creature dying this turn.
		return g.replaceDying(a, "Pump")
	}

	cards, err := targetedOrDefinedCards(source, a.Params, a.refs())
	if err != nil {
		// A Defined$ that names players (You) names no card (AbilityUtils.
		// getDefinedCards skips it); only a spec neither side knows is an error.
		spec, _ := a.Params.Param("Defined")
		if ps, perr := definedPlayers(g, a.Controller, a.Source, spec, a.refs()); perr != nil || len(ps) == 0 {
			return fmt.Errorf("engine: Pump: %w", err)
		}
		cards = nil
	}
	// Radiance$ (PumpEffect.java:489): the permanents sharing a color with a
	// target are pumped with it.
	cards = append(cards, g.radianceCards(a)...)

	g.timestamp++
	timestamp := g.timestamp
	for _, p := range players {
		if g.Player(p).Lost {
			continue
		}
		g.pumps = append(g.pumps, pumpRecord{
			Timestamp: timestamp, Keywords: grant.keywords, OnPlayer: true, Player: p,
			// A Perpetual keyword pump of a player wears off at end of turn
			// (PumpEffect.java:135 lacks the `!perpetual` the card branch has).
			Permanent: permanent && !perpetual, HasUntil: untilNextTurn, UntilTurnOf: a.Controller,
		})
	}
	for _, cid := range cards {
		c := g.Card(cid)
		// CR 702.26e: a phased-out target is not pumped (PumpEffect.java).
		if c.IsPhasedOut() || !pumpZoneMatches(a.Params, c.Zone) {
			continue
		}
		power, toughness := power, toughness
		if multiplied {
			extraP, err := pumpMultiplier(a, c, "NumAtt")
			if err != nil {
				return err
			}
			extraT, err := pumpMultiplier(a, c, "NumDef")
			if err != nil {
				return err
			}
			power, toughness = power+extraP, toughness+extraT
		}
		g.pumps = append(g.pumps, pumpRecord{
			Card: cid, Timestamp: timestamp, Power: power, Toughness: toughness,
			Keywords: grant.keywords, Hidden: grant.hidden, Switched: grant.switched, Permanent: permanent,
			HasUntil: untilNextTurn, UntilTurnOf: a.Controller, Perpetual: perpetual,
		})
		if hostBound != "" {
			g.registerHostBoundEnd(source, cardCommand{Kind: commandEndPump, Target: cid, Timestamp: timestamp}, hostBound)
		}
	}
	// PumpEffect.java:506: Bleed Dry's "if that creature would die this turn,
	// exile it instead" (replacedying.go).
	return g.replaceDying(a, "Pump")
}

// pumpPlayers is the players a keyword pump reaches: PumpEffect.java's
// tgtPlayers loop (:498-504) gives a player keywords only (a player has no
// power or toughness). getTargetPlayers is the targeted players, or the
// players Defined$ names ("You"; a card-only spec names none). A hidden line
// for a player is an error, checked before the effect changes anything: Java
// hands the player the "HIDDEN ..." string itself as a keyword
// (applyPump(player), PumpEffect.java:131-132), which nothing reads.
func (g *Game) pumpPlayers(a *Ability, grant pumpGrant) ([]PlayerID, error) {
	if len(grant.keywords) == 0 && len(grant.hidden) == 0 {
		return nil, nil
	}
	var players []PlayerID
	if _, ok := a.Params.Param("ValidTgts"); ok {
		for _, e := range a.Targets {
			if p, ok := e.AsPlayer(); ok {
				players = append(players, p)
			}
		}
	} else if spec, ok := a.Params.Param("Defined"); ok {
		if ps, err := definedPlayers(g, a.Controller, a.Source, spec, a.refs()); err == nil {
			players = ps
		}
	}
	if len(players) > 0 && len(grant.hidden) > 0 {
		return nil, fmt.Errorf("engine: Pump: KW$ hidden keyword %q on a player not resolvable yet", grant.hidden[0])
	}
	return players, nil
}

// pumpDefinedKeywords is PumpEffect.java:318-371, DefinedKW$: the placeholder a
// keyword names is replaced by what the host chose or the players Defined
// names. ChosenType$/ChosenPlayer/ChosenColor read the host's own choice;
// anything else is a player spec, and each keyword with ChosenPlayerUID or
// ChosenPlayerName becomes one keyword per player (the others stay). proceed is
// false when the choice or the player list is empty: the effect then does
// nothing (Java returns). A player's UID is its PlayerID, the number
// PlayerUID_<n> reads back (layerSubstituteKeyword's reasoning).
func (g *Game) pumpDefinedKeywords(a *Ability, host *Card, keywords []string) (out []string, proceed bool, err error) {
	defined, ok := a.Params.Param("DefinedKW")
	if !ok {
		return keywords, true, nil
	}
	replaceAll := func(from, to string) []string {
		replaced := make([]string, len(keywords))
		for i, kw := range keywords {
			replaced[i] = strings.ReplaceAll(kw, from, to)
		}
		return replaced
	}
	playerName := func(p PlayerID) string { return g.Player(p).Name }
	switch defined {
	case "ChosenType":
		t := host.Memory.ChosenType(false)
		if t == "" {
			return nil, false, nil
		}
		return replaceAll(defined, t), true, nil
	case "ChosenPlayer":
		p := host.Memory.ChosenPlayer()
		if p == NoPlayer {
			return nil, false, nil
		}
		keywords = replaceAll("ChosenPlayerUID", strconv.Itoa(int(p)))
		return replaceAll("ChosenPlayerName", playerName(p)), true, nil
	case "ChosenColor":
		colors := host.Memory.ChosenColors()
		if colors.Count() != 1 {
			return nil, false, fmt.Errorf("engine: Pump: DefinedKW$ ChosenColor with %d chosen colors not resolvable yet", colors.Count())
		}
		return layerKeywordPerColorAll(keywords, colors), true, nil
	}
	players, err := definedPlayers(g, a.Controller, host.ID, defined, a.refs())
	if err != nil {
		return nil, false, fmt.Errorf("engine: Pump: DefinedKW$: %w", err)
	}
	if len(players) == 0 {
		return nil, false, nil
	}
	var expanded []string
	for _, kw := range keywords {
		if !strings.Contains(kw, "ChosenPlayerUID") && !strings.Contains(kw, "ChosenPlayerName") {
			expanded = append(expanded, kw)
			continue
		}
		for _, p := range players {
			s := strings.ReplaceAll(kw, "ChosenPlayerUID", strconv.Itoa(int(p)))
			expanded = append(expanded, strings.ReplaceAll(s, "ChosenPlayerName", playerName(p)))
		}
	}
	return expanded, true, nil
}

// layerKeywordPerColorAll is each keyword with ChosenColor/chosenColor replaced
// by the single chosen color's name.
func layerKeywordPerColorAll(keywords []string, colors mana.Colors) []string {
	out := make([]string, 0, len(keywords))
	for _, kw := range keywords {
		out = append(out, layerKeywordPerColor(kw, "ChosenColor", "chosenColor", colors)...)
	}
	return out
}

// pumpRememberObjects adds (or, forget, removes) the Defined$ objects key
// names to the host's remembered list.
func pumpRememberObjects(g *Game, a *Ability, host *Card, key string, forget bool) error {
	spec, ok := a.Params.Param(key)
	if !ok {
		return nil
	}
	for _, part := range strings.Split(spec, " & ") {
		objs, err := definedEntities(g, a.Controller, host, strings.TrimSpace(part), a.refs())
		if err != nil {
			return fmt.Errorf("engine: Pump: %s$: %w", key, err)
		}
		for _, e := range objs {
			if forget {
				host.Memory.Forget(e)
			} else {
				host.Memory.Remember(e)
			}
		}
	}
	return nil
}

// pumpAmount reads NumAtt$/NumDef$, defaulting to 0 when the key is absent
// (a KW$-only Pump/PumpAll line, granting no stat change at all). effect
// names the caller (Pump/PumpAll) for the error message. "Double"/"Triple"
// read 0 here: they are the target's own power or toughness, computed per
// target (pumpMultiplier) -- Pump's only (PumpEffect.java:305-310,
// :468-480); PumpAll has no such case (PumpAllEffect.java computes the
// amount once against the host), so it refuses them.
func pumpAmount(g *Game, effect string, a *Ability, host *Card, key string) (int, error) {
	v, ok := a.Params.Param(key)
	if !ok {
		return 0, nil
	}
	if v == "Double" || v == "Triple" {
		if effect == "Pump" {
			return 0, nil
		}
		return 0, fmt.Errorf("engine: %s: %s$ %q not resolvable yet", effect, key, v)
	}
	amount, ok := resolveNamedAmount(g, a.Amounts, host, v)
	if !ok {
		return 0, fmt.Errorf("engine: %s: %s$ %q is not resolvable", effect, key, v)
	}
	return amount, nil
}

// pumpMultiplier is PumpEffect.java:468-480 for key (NumAtt$ or NumDef$):
// "Double" adds the target's net power (toughness) once, doubling it, and
// "Triple" adds it twice; 0 for any other value. Java reads the stat off
// the card the Defined$ names -- an LKI copy for TriggeredAttackerLKICopy,
// as it was when the trigger ran -- and this port off the card as it is now
// (definedCards' doc comment: one CardID per card, no snapshot), so a pump
// in response to Overclocked Electromancer's attack trigger is doubled with
// it here and not in Java. A stat that does not resolve (a printed "X" with
// nothing defining it) is an error, not a zero (GO-7).
func pumpMultiplier(a *Ability, c *Card, key string) (int, error) {
	v, _ := a.Params.Param(key)
	times := 0
	switch v {
	case "Double":
		times = 1
	case "Triple":
		times = 2
	default:
		return 0, nil
	}
	stat, ok := c.Power()
	if key == "NumDef" {
		stat, ok = c.Toughness()
	}
	if !ok {
		return 0, fmt.Errorf("engine: Pump: %s$ %s on %s: its stat is not resolvable", key, v, c.Def.Name)
	}
	return stat * times, nil
}

// pumpZoneMatches reports whether zone is one PumpZone$ permits: absent
// means the Java default, ZoneType.listValueOf("Battlefield") -- Battlefield
// alone -- otherwise zone's own name must appear in the comma list (hasZone,
// trigger.go), the identical shape TriggerZones$ already has for a trigger
// instead of a resolving ability.
func pumpZoneMatches(a *compile.Ability, zone ZoneType) bool {
	if _, ok := a.Param("PumpZone"); !ok {
		return zone == Battlefield
	}
	return hasZone(a, "PumpZone", zone.String())
}

// switchPTKeyword is the hidden keyword Forge's switch effects carry (Card.java:4448,
// PumpEffect.java:51): Layer 7d (CR 613.4d) has no layer of its own there.
const switchPTKeyword = "CARDNAME's power and toughness are switched"

// pumpGrant is what KW$ grants, split the way PumpEffect.applyPump
// (PumpEffect.java:48-55) and PumpAllEffect.applyPumpAll (PumpAllEffect.java:34-40)
// split it: plain keywords go to Layer 6 (addChangedCardKeywords), "HIDDEN "
// lines lose that prefix and become hidden extrinsic keywords
// (addHiddenExtrinsicKeywords), and switchPTKeyword is the Layer 7d switch.
type pumpGrant struct {
	keywords []string
	hidden   []string
	switched bool
}

// empty reports whether the grant changes nothing.
func (k pumpGrant) empty() bool {
	return len(k.keywords) == 0 && len(k.hidden) == 0 && !k.switched
}

// pumpKeywords reads KW$ -- shared between pumpEffect and pumpAllEffect,
// PumpEffect.java/PumpAllEffect.java's own identical two-line KW$ handling
// (split on " & "). effect names the caller (Pump/PumpAll) for the error
// message. A "HIDDEN " token is accepted when something in this port reads
// its line (hiddenKeywordRead, continuous.go): Java stores it on the card and
// Card.hasKeyword's exact-text shortcut (Card.java:4981) is the only reader,
// so a line nothing reads would be granted and then ignored, and fails loudly
// instead (GO-7) -- naming the token. Absent KW$ (a NumAtt$/NumDef$-only line)
// grants nothing, not an error.
func pumpKeywords(effect string, a *compile.Ability) (pumpGrant, error) {
	var grant pumpGrant
	raw, ok := a.Param("KW")
	if !ok {
		return grant, nil
	}
	_, definedKW := a.Param("DefinedKW")
	for _, tok := range strings.Split(raw, " & ") {
		if line, isHidden := strings.CutPrefix(tok, "HIDDEN "); isHidden {
			switch {
			case line == switchPTKeyword:
				grant.switched = !grant.switched
			case hiddenKeywordRead(line):
				grant.hidden = append(grant.hidden, line)
			default:
				return pumpGrant{}, fmt.Errorf("engine: %s: KW$ hidden keyword %q not resolvable yet", effect, line)
			}
			continue
		}
		// pumpDefinedKeywords substitutes the placeholders a DefinedKW$ line
		// names; anything else carrying one is a dynamic value this port
		// does not compute.
		substituted := definedKW && effect == "Pump"
		if !substituted && keywordTokenDynamic(tok) {
			return pumpGrant{}, fmt.Errorf("engine: %s: KW$ %q not resolvable yet", effect, tok)
		}
		grant.keywords = append(grant.keywords, tok)
	}
	return grant, nil
}
