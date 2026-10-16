package keyword

import (
	"strconv"
	"strings"
)

// Expansion is the script text a keyword stands for: the `A:`, `T:`, `S:` and
// `R:` values CardFactoryUtil builds for it (ADR-0038), and the SVars those
// lines name. Each trigger line names its effect by `Execute$` and the matching
// SVar is in SVars, with the placeholder name {n} replaced by the caller so two
// keywords on one card never share an SVar.
type Expansion struct {
	Abilities, Triggers, Statics, Replacements []string
	SVars                                      []SVarDef
	// Amounts are SVars that are values (Count$ ...), not abilities: the
	// compiler parses them into the face's Amounts, where a param such as
	// Amount$ KWAffinity{n} finds them.
	Amounts []SVarDef
}

// SVarDef is one SVar an Expansion defines.
type SVarDef struct{ Name, Value string }

// Slot is the placeholder an expansion's SVar names carry; the compiler
// replaces it with a number unique to the keyword line on the card.
const Slot = "{n}"

// Expand returns what k stands for, ported from the CardFactoryUtil branches
// ADR-0038 calls expressible, or false when k has no template or its details
// carry a shape this port does not translate (ok false leaves the keyword
// inert, never half-expanded, GO-7). The returned lines are Java's own script
// text minus the display-only params (PrecostDesc$, CostDesc$,
// SpellDescription$, TriggerDescription$).
func Expand(k Keyword) (Expansion, bool) {
	switch k.Name {
	case "Equip":
		return expandEquip(k)
	case "Cycling":
		return expandCycling(k)
	case "TypeCycling":
		// TypeCycling:<type>:<cost>, CardFactoryUtil.java:3733.
		args := k.Args()
		if len(args) < 2 || args[0] == "" || args[1] == "" {
			return Expansion{}, false
		}
		return Expansion{Abilities: []string{"AB$ ChangeZone | Cost$ " + args[1] + " Discard<1/CARDNAME> | ActivationZone$ Hand | Origin$ Library | Destination$ Hand | ChangeType$ " + args[0]}}, true
	case "Crew":
		// Crew:<power>[:<extra>], CardFactoryUtil.java:3701: tap any number of
		// other creatures with total power N or more to make it an artifact
		// creature until end of turn.
		args := k.Args()
		if len(args) < 1 || strings.Trim(args[0], "0123456789") != "" || args[0] == "" {
			return Expansion{}, false
		}
		line := "AB$ Animate | Cost$ tapXType<Any/Creature.Other+withTotalPowerGE" + args[0] + "> | Defined$ Self | Types$ Artifact,Creature"
		if len(args) > 1 && args[1] != "" {
			line += " | " + args[1]
		}
		return Expansion{Abilities: []string{line}}, true
	case "Affinity":
		// Affinity:<type>, CardFactoryUtil.java:3753: this spell costs {1} less
		// for each <type> you control.
		args := k.Args()
		if len(args) < 1 || args[0] == "" {
			return Expansion{}, false
		}
		sep := "."
		if strings.Contains(args[0], ".") {
			sep = "+"
		}
		return Expansion{
			Statics: []string{"Mode$ ReduceCost | ValidCard$ Card.Self | Type$ Spell | Amount$ KWAffinity" + Slot + " | EffectZone$ All"},
			Amounts: []SVarDef{{"KWAffinity" + Slot, "Count$Valid " + args[0] + sep + "YouCtrl"}},
		}, true
	case "Prowess":
		if k.Details != "" {
			return Expansion{}, false
		}
		return Expansion{
			Triggers: []string{"Mode$ SpellCast | ValidCard$ Card.nonCreature | ValidActivatingPlayer$ You | TriggerZones$ Battlefield | Execute$ KWProwess" + Slot},
			SVars:    []SVarDef{{"KWProwess" + Slot, "DB$ Pump | Defined$ Self | NumAtt$ +1 | NumDef$ +1"}},
		}, true
	case "Annihilator":
		n, ok := amountDetail(k)
		if !ok {
			return Expansion{}, false
		}
		return Expansion{
			Triggers: []string{"Mode$ Attacks | ValidCard$ Card.Self | TriggerZones$ Battlefield | Secondary$ True | Execute$ KWAnnihilator" + Slot},
			SVars:    []SVarDef{{"KWAnnihilator" + Slot, "DB$ Sacrifice | Defined$ TriggeredDefendingPlayer | SacValid$ Permanent | Amount$ " + n}},
		}, true
	case "Bushido":
		n, ok := amountDetail(k)
		if !ok {
			return Expansion{}, false
		}
		return Expansion{
			Triggers: []string{
				"Mode$ Blocks | ValidCard$ Card.Self | Secondary$ True | Execute$ KWBushido" + Slot,
				"Mode$ AttackerBlocked | ValidCard$ Card.Self | Secondary$ True | Execute$ KWBushido" + Slot,
			},
			SVars: []SVarDef{{"KWBushido" + Slot, "DB$ Pump | Defined$ Self | NumAtt$ " + n + " | NumDef$ " + n}},
		}, true
	case "Afterlife":
		n, ok := amountDetail(k)
		if !ok {
			return Expansion{}, false
		}
		return Expansion{
			Triggers: []string{"Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard | ValidCard$ Card.Self | Secondary$ True | Execute$ KWAfterlife" + Slot},
			SVars:    []SVarDef{{"KWAfterlife" + Slot, "DB$ Token | TokenAmount$ " + n + " | TokenScript$ wb_1_1_spirit_flying"}},
		}, true
	case "Persist", "Undying":
		if k.Details != "" {
			return Expansion{}, false
		}
		counter := "M1M1"
		if k.Name == "Undying" {
			counter = "P1P1"
		}
		return Expansion{
			Triggers: []string{"Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard | ValidCard$ Card.Self+counters_EQ0_" + counter + " | TriggerZones$ Battlefield | Secondary$ True | Execute$ KW" + k.Name + Slot},
			SVars:    []SVarDef{{"KW" + k.Name + Slot, "DB$ ChangeZone | Defined$ TriggeredNewCardLKICopy | Origin$ Graveyard | Destination$ Battlefield | WithCountersType$ " + counter}},
		}, true
	case "Exalted":
		if k.Details != "" {
			return Expansion{}, false
		}
		return Expansion{
			Triggers: []string{"Mode$ Attacks | ValidCard$ Creature.YouCtrl | Alone$ True | TriggerZones$ Battlefield | Secondary$ True | Execute$ KWExalted" + Slot},
			SVars:    []SVarDef{{"KWExalted" + Slot, "DB$ Pump | Defined$ TriggeredAttackerLKICopy | NumAtt$ +1 | NumDef$ +1"}},
		}, true
	case "Battle cry":
		// CardFactoryUtil.java:677.
		if k.Details != "" {
			return Expansion{}, false
		}
		return Expansion{
			Triggers: []string{"Mode$ Attacks | ValidCard$ Card.Self | TriggerZones$ Battlefield | Secondary$ True | Execute$ KWBattleCry" + Slot},
			SVars:    []SVarDef{{"KWBattleCry" + Slot, "DB$ PumpAll | ValidCards$ Creature.attacking+Other | NumAtt$ 1"}},
		}, true
	case "Dethrone":
		// CardFactoryUtil.java:934.
		if k.Details != "" {
			return Expansion{}, false
		}
		return Expansion{
			Triggers: []string{"Mode$ Attacks | ValidCard$ Card.Self | Attacked$ Player.withMostLife | Secondary$ True | TriggerZones$ Battlefield | Execute$ KWDethrone" + Slot},
			SVars:    []SVarDef{{"KWDethrone" + Slot, "DB$ PutCounter | Defined$ Self | CounterType$ P1P1 | CounterNum$ 1"}},
		}, true
	case "Flanking":
		// CardFactoryUtil.java:1096.
		if k.Details != "" {
			return Expansion{}, false
		}
		return Expansion{
			Triggers: []string{"Mode$ AttackerBlockedByCreature | ValidCard$ Card.Self | ValidBlocker$ Creature.withoutFlanking | TriggerZones$ Battlefield | Secondary$ True | Execute$ KWFlanking" + Slot},
			SVars:    []SVarDef{{"KWFlanking" + Slot, "DB$ Pump | Defined$ TriggeredBlockerLKICopy | NumAtt$ -1 | NumDef$ -1"}},
		}, true
	case "etbCounter":
		// CardFactoryUtil.makeEtbCounter (545): etbCounter:<type>:<amount>
		// [:<extra replacement params>|no Condition[:<description>]]. The
		// description is display only. An EACH type is a different
		// ability (CounterTypes$) this port does not translate.
		args := k.Args()
		if len(args) < 2 || args[0] == "" || args[1] == "" || strings.HasPrefix(args[0], "EACH") {
			return Expansion{}, false
		}
		line := "Event$ Moved | ValidCard$ Card.Self | Destination$ Battlefield | Secondary$ True | ReplacementResult$ Updated | ReplaceWith$ KWEtbCounter" + Slot
		if len(args) > 2 && args[2] != "" && args[2] != "no Condition" {
			line += " | " + args[2]
		}
		return Expansion{
			Replacements: []string{line},
			SVars:        []SVarDef{{"KWEtbCounter" + Slot, "DB$ PutCounter | Defined$ Self | CounterType$ " + args[0] + " | ETB$ True | CounterNum$ " + args[1]}},
		}, true
	case "Fabricate":
		// CardFactoryUtil.java:1034: when this enters, put N +1/+1 counters on
		// it unless you create N 1/1 Servo artifact creature tokens -- as
		// scripted, create the tokens unless you pay "put the counters".
		n, ok := amountDetail(k)
		if !ok {
			return Expansion{}, false
		}
		return Expansion{
			Triggers: []string{"Mode$ ChangesZone | Destination$ Battlefield | ValidCard$ Card.Self | Secondary$ True | Execute$ KWFabricate" + Slot},
			SVars:    []SVarDef{{"KWFabricate" + Slot, "DB$ Token | TokenAmount$ " + n + " | TokenScript$ c_1_1_a_servo | UnlessCost$ AddCounter<" + n + "/P1P1> | UnlessPayer$ You"}},
		}, true
	case "Extort":
		// CardFactoryUtil.java:1016: whenever you cast a spell, you may pay
		// {W/B}; if you do, each opponent loses 1 life and you gain that much.
		// Java scopes AFLifeLost to the ability and defaults it to 0; the
		// default never reads, since the gain only runs after the loss.
		if k.Details != "" {
			return Expansion{}, false
		}
		return Expansion{
			Triggers: []string{"Mode$ SpellCast | ValidActivatingPlayer$ You | TriggerZones$ Battlefield | Secondary$ True | Execute$ KWExtort" + Slot},
			SVars: []SVarDef{
				{"KWExtort" + Slot, "AB$ LoseLife | Cost$ WB | Defined$ Player.Opponent | LifeAmount$ 1 | SubAbility$ KWExtortGain" + Slot},
				{"KWExtortGain" + Slot, "DB$ GainLife | Defined$ You | LifeAmount$ AFLifeLost"},
			},
		}, true
	case "Cascade":
		// CardFactoryUtil.java:711: when you cast this spell, exile cards from
		// the top of your library until a nonland card with lesser mana value;
		// you may cast it without paying its mana cost, then the exiled cards
		// go to the bottom in a random order.
		if k.Details != "" {
			return Expansion{}, false
		}
		return Expansion{
			Triggers: []string{"Mode$ SpellCast | ValidCard$ Card.Self | TriggerZones$ Stack | Secondary$ True | Execute$ KWCascade" + Slot},
			SVars: []SVarDef{
				{"KWCascade" + Slot, "DB$ DigUntil | Defined$ You | Amount$ 1 | Valid$ Card.nonLand+cmcLTKWCascadeX" + Slot + " | FoundDestination$ Exile | RevealedDestination$ Exile | ImprintFound$ True | RememberRevealed$ True | SubAbility$ KWCascadeCast" + Slot},
				{"KWCascadeCast" + Slot, "DB$ Play | Defined$ Imprinted | WithoutManaCost$ True | Optional$ True | ValidSA$ Spell.cmcLTKWCascadeX" + Slot + " | SubAbility$ KWCascadeLib" + Slot},
				{"KWCascadeLib" + Slot, "DB$ ChangeZoneAll | ChangeType$ Card.IsRemembered,Card.IsImprinted | Origin$ Exile | Destination$ Library | RandomOrder$ True | LibraryPosition$ -1 | SubAbility$ KWCascadeClean" + Slot},
				{"KWCascadeClean" + Slot, "DB$ Cleanup | ClearRemembered$ True | ClearImprinted$ True"},
			},
			Amounts: []SVarDef{{"KWCascadeX" + Slot, "Count$CardManaCost"}},
		}, true
	case "Storm":
		// CardFactoryUtil.java:1814: when you cast this spell, copy it for each
		// spell cast before it this turn; you may choose new targets.
		if k.Details != "" {
			return Expansion{}, false
		}
		return Expansion{
			Triggers: []string{"Mode$ SpellCast | ValidCard$ Card.Self | TriggerZones$ Stack | Secondary$ True | Execute$ KWStorm" + Slot},
			SVars: []SVarDef{
				{"KWStorm" + Slot, "DB$ CopySpellAbility | Defined$ TriggeredSpellAbility | Amount$ KWStormCount" + Slot + " | MayChooseTarget$ True"},
			},
			Amounts: []SVarDef{{"KWStormCount" + Slot, "TriggerCount$CurrentStormCount/Minus.1"}},
		}, true
	case "Exploit":
		// CardFactoryUtil.java:1006: when this enters, you may sacrifice a
		// creature.
		if k.Details != "" {
			return Expansion{}, false
		}
		return Expansion{
			Triggers: []string{"Mode$ ChangesZone | ValidCard$ Card.Self | Destination$ Battlefield | Secondary$ True | Execute$ KWExploit" + Slot},
			SVars:    []SVarDef{{"KWExploit" + Slot, "DB$ Sacrifice | SacValid$ Creature | Optional$ True"}},
		}, true
	case "Echo":
		// CardFactoryUtil.java:963: at the beginning of your upkeep, if this
		// came under your control since your last upkeep, sacrifice it unless
		// you pay its echo cost (SacrificeEffect's Echo$).
		cost := costDetail(k)
		if cost == "" {
			return Expansion{}, false
		}
		return Expansion{
			Triggers: []string{"Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You | TriggerZones$ Battlefield | IsPresent$ Card.Self+cameUnderControlSinceLastUpkeep | Secondary$ True | Execute$ KWEcho" + Slot},
			SVars:    []SVarDef{{"KWEcho" + Slot, "DB$ Sacrifice | SacValid$ Self | Echo$ " + cost}},
		}, true
	case "Cumulative upkeep":
		// CardFactoryUtil.java:860: at the beginning of your upkeep, put an age
		// counter on this, then sacrifice it unless you pay its cost once for
		// each age counter (SacrificeEffect's CumulativeUpkeep$).
		cost := costDetail(k)
		if cost == "" {
			return Expansion{}, false
		}
		return Expansion{
			Triggers: []string{"Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You | TriggerZones$ Battlefield | IsPresent$ Card.Self | Secondary$ True | Execute$ KWCumulativeUpkeep" + Slot},
			SVars:    []SVarDef{{"KWCumulativeUpkeep" + Slot, "DB$ Sacrifice | SacValid$ Self | CumulativeUpkeep$ " + cost}},
		}, true
	case "Chapter":
		// CardFactoryUtil.java:812: Chapter:<N>:<SVar>,<SVar>,...: one
		// CounterAdded trigger per chapter, running that chapter's own SVar when
		// the lore counters reach its number. The Saga's lore counter on entering
		// and each turn, and its sacrifice, are native (saga.go).
		args := k.Args()
		if len(args) < 2 {
			return Expansion{}, false
		}
		final, err := strconv.Atoi(args[0])
		abilities := strings.Split(args[1], ",")
		if err != nil || final < 1 || len(abilities) != final {
			return Expansion{}, false
		}
		var triggers []string
		for i, ability := range abilities {
			if ability == "" {
				return Expansion{}, false
			}
			n := strconv.Itoa(i + 1)
			triggers = append(triggers, "Mode$ CounterAdded | ValidCard$ Card.Self | TriggerZones$ Battlefield | Chapter$ "+n+" | CounterType$ LORE | CounterAmount$ EQ"+n+" | Execute$ "+ability)
		}
		return Expansion{Triggers: triggers}, true
	case "Renown":
		// CardFactoryUtil.java:1655: when this deals combat damage to a player, if
		// it is not renowned, put N +1/+1 counters on it and it becomes renowned
		// (PutCounter sets that when its ability is this keyword's).
		n, ok := amountDetail(k)
		if !ok {
			return Expansion{}, false
		}
		return Expansion{
			Triggers: []string{"Mode$ DamageDone | ValidSource$ Card.Self | ValidTarget$ Player | IsPresent$ Card.Self+!IsRenowned | CombatDamage$ True | Secondary$ True | Execute$ KWRenown" + Slot},
			SVars:    []SVarDef{{"KWRenown" + Slot, "DB$ PutCounter | Defined$ Self | CounterType$ P1P1 | CounterNum$ " + n}},
		}, true
	case "Bloodthirst":
		// CardFactoryUtil.java:2119: enters with N +1/+1 counters if an opponent
		// was dealt damage this turn; Bloodthirst X counts that damage.
		args := k.Args()
		if len(args) < 1 || args[0] == "" {
			return Expansion{}, false
		}
		n := args[0]
		exp := Expansion{Replacements: []string{"Event$ Moved | ValidCard$ Card.Self | Destination$ Battlefield | Secondary$ True | ReplacementResult$ Updated | ReplaceWith$ KWBloodthirst" + Slot + " | Bloodthirst$ True"}}
		if n == "X" {
			n = "KWBloodthirstX" + Slot
			exp.Amounts = []SVarDef{{n, "Count$BloodthirstAmount"}}
		} else if strings.Trim(n, "0123456789") != "" {
			return Expansion{}, false
		}
		exp.SVars = []SVarDef{{"KWBloodthirst" + Slot, "DB$ PutCounter | Defined$ Self | CounterType$ P1P1 | ETB$ True | CounterNum$ " + n}}
		return exp, true
	case "Devour":
		return expandDevour(k)
	case "Modular":
		// CardFactoryUtil.java:1490 and :2377: enters with N +1/+1 counters, and
		// when it dies you may put a +1/+1 counter on target artifact creature for
		// each +1/+1 counter on it. Java's decider is the triggered card's
		// controller, which for this trigger on the dying card itself is "You".
		n, ok := amountDetail(k)
		if !ok {
			return Expansion{}, false
		}
		return Expansion{
			Replacements: []string{"Event$ Moved | ValidCard$ Card.Self | Destination$ Battlefield | Secondary$ True | ReplacementResult$ Updated | ReplaceWith$ KWModularEtb" + Slot},
			Triggers:     []string{"Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard | ValidCard$ Card.Self | OptionalDecider$ You | Secondary$ True | Execute$ KWModular" + Slot},
			SVars: []SVarDef{
				{"KWModularEtb" + Slot, "DB$ PutCounter | Defined$ Self | CounterType$ P1P1 | ETB$ True | CounterNum$ " + n},
				{"KWModular" + Slot, "DB$ PutCounter | ValidTgts$ Artifact.Creature | TgtPrompt$ Select target artifact creature | CounterType$ P1P1 | CounterNum$ KWModularX" + Slot},
			},
			Amounts: []SVarDef{{"KWModularX" + Slot, "TriggeredCard$CardCounters.P1P1"}},
		}, true
	case "Evolve":
		// CardFactoryUtil.java:987. Trigger.java:377 reads the keyword to
		// check CR 702.100c; the script spelling of that check is Condition$
		// Evolve, which the two cards that script it by hand already use.
		if k.Details != "" {
			return Expansion{}, false
		}
		return Expansion{
			Triggers: []string{"Mode$ ChangesZone | Destination$ Battlefield | ValidCard$ Creature.YouCtrl+Other | Condition$ Evolve | TriggerZones$ Battlefield | Secondary$ True | Execute$ KWEvolve" + Slot},
			SVars:    []SVarDef{{"KWEvolve" + Slot, "DB$ PutCounter | Defined$ Self | CounterType$ P1P1 | CounterNum$ 1"}},
		}, true
	case "Mentor":
		// CardFactoryUtil.java:1434: put a +1/+1 counter on target attacking
		// creature with lesser power. Java scopes SVar X to the ability; here
		// it is a slotted name, so the card's own X is never shadowed.
		if k.Details != "" {
			return Expansion{}, false
		}
		return Expansion{
			Triggers: []string{"Mode$ Attacks | ValidCard$ Card.Self | TriggerZones$ Battlefield | Secondary$ True | Execute$ KWMentor" + Slot},
			SVars:    []SVarDef{{"KWMentor" + Slot, "DB$ PutCounter | CounterType$ P1P1 | CounterNum$ 1 | ValidTgts$ Creature.attacking+powerLTKWPower" + Slot}},
			Amounts:  []SVarDef{{"KWPower" + Slot, "Count$CardPower"}},
		}, true
	case "Training":
		// CardFactoryUtil.java:1874: when this attacks with another creature
		// with greater power, put a +1/+1 counter on it. The ability's own
		// Training$ param is read by nothing in Forge and is dropped.
		if k.Details != "" {
			return Expansion{}, false
		}
		return Expansion{
			Triggers: []string{"Mode$ Attacks | ValidCard$ Card.Self | Secondary$ True | IsPresent$ Creature.attacking+Other+powerGTKWPower" + Slot + " | NoResolvingCheck$ True | Execute$ KWTraining" + Slot},
			SVars:    []SVarDef{{"KWTraining" + Slot, "DB$ PutCounter | CounterType$ P1P1 | CounterNum$ 1 | Defined$ Self"}},
			Amounts:  []SVarDef{{"KWPower" + Slot, "Count$CardPower"}},
		}, true
	case "Afflict":
		// CardFactoryUtil.java:588.
		n, ok := amountDetail(k)
		if !ok {
			return Expansion{}, false
		}
		return Expansion{
			Triggers: []string{"Mode$ AttackerBlocked | ValidCard$ Card.Self | TriggerZones$ Battlefield | ValidBlocker$ Creature | Secondary$ True | Execute$ KWAfflict" + Slot},
			SVars:    []SVarDef{{"KWAfflict" + Slot, "DB$ LoseLife | Defined$ TriggeredDefendingPlayer | LifeAmount$ " + n}},
		}, true
	case "Soulbond":
		// CardFactoryUtil.java:1740-1760: when it or another creature enters
		// under your control and one of the pair is unpaired, you may pair
		// them. The Execute$ SVar is Java's setOverridingAbility.
		if k.Details != "" {
			return Expansion{}, false
		}
		return Expansion{
			Triggers: []string{
				"Mode$ ChangesZone | Destination$ Battlefield | ValidCard$ Card.Self | IsPresent$ Creature.Other+YouCtrl+!Paired | Secondary$ True | Execute$ KWSoulbondSelf" + Slot,
				"Mode$ ChangesZone | Destination$ Battlefield | ValidCard$ Creature.Other+YouCtrl | TriggerZones$ Battlefield | IsPresent$ Creature.Self+!Paired | Secondary$ True | Execute$ KWSoulbondOther" + Slot,
			},
			SVars: []SVarDef{
				{"KWSoulbondSelf" + Slot, "DB$ Bond | Defined$ TriggeredCardLKICopy | ValidCards$ Creature.Other+YouCtrl+!Paired"},
				{"KWSoulbondOther" + Slot, "DB$ Bond | Defined$ TriggeredCardLKICopy | ValidCards$ Creature.Self+!Paired"},
			},
		}, true
	case "Soulshift":
		// CardFactoryUtil.java:1763: when this dies, you may return target
		// Spirit card with mana value N or less from your graveyard to hand.
		n, ok := amountDetail(k)
		if !ok {
			return Expansion{}, false
		}
		return Expansion{
			Triggers: []string{"Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard | Secondary$ True | OptionalDecider$ You | ValidCard$ Card.Self | Execute$ KWSoulshift" + Slot},
			SVars:    []SVarDef{{"KWSoulshift" + Slot, "DB$ ChangeZone | Origin$ Graveyard | Destination$ Hand | ValidTgts$ Spirit.YouOwn+cmcLE" + n}},
		}, true
	}
	return Expansion{}, false
}

// expandEquip is CardFactoryUtil.java:2852's Equip branch: "Equip:<cost>
// [:<valid>[:<description>[:<extra>[:<extra description>]]]]", an Attach
// ability at sorcery speed aimed at a creature the controller controls (or
// <valid>). <extra> is appended to the ability as written; the two shapes
// whose params change what the ability costs (ReduceCost$, AlternateCost$) are
// not read by any ability this port resolves, so an Equip carrying one is left
// inert rather than activated at the wrong price.
func expandEquip(k Keyword) (Expansion, bool) {
	args := k.Args()
	if len(args) < 1 || args[0] == "" {
		return Expansion{}, false
	}
	valid := "Creature.YouCtrl"
	if len(args) > 1 && args[1] != "" {
		valid = args[1]
	}
	desc := "creature"
	if len(args) > 2 && args[2] != "" {
		desc = args[2]
	}
	line := "AB$ Attach | Cost$ " + args[0] + " | ValidTgts$ " + valid +
		" | TgtPrompt$ Select target " + desc + " you control | SorcerySpeed$ True"
	if len(args) > 3 && args[3] != "" {
		extra := args[3]
		if strings.Contains(extra, "ReduceCost$") || strings.Contains(extra, "AlternateCost$") {
			return Expansion{}, false
		}
		line += " | " + extra
	}
	return Expansion{Abilities: []string{line}}, true
}

// expandDevour is CardFactoryUtil.java:2211-2238's Devour branch,
// "Devour:<N|X>[:<type>[:<extra description>]]" (Devour.java:32-51): as the
// permanent enters, its controller may sacrifice any number of other
// permanents of the type (Creature by default), then it enters with N +1/+1
// counters for each, and the remembered sacrifices are cleared. Java scopes
// DevourSacX and DevourX to the two abilities; here they are slotted face
// amounts. SacMessage$ is display only and dropped. An X amount reads the
// card's own SVar X (Thromok the Insatiable); any other non-number is not
// translated.
func expandDevour(k Keyword) (Expansion, bool) {
	args := k.Args()
	if len(args) < 1 || args[0] == "" || (args[0] != "X" && strings.Trim(args[0], "0123456789") != "") {
		return Expansion{}, false
	}
	valid := "Creature"
	if len(args) > 1 && args[1] != "" {
		valid = args[1]
	}
	return Expansion{
		Replacements: []string{"Event$ Moved | ValidCard$ Card.Self | Destination$ Battlefield | ReplacementResult$ Updated | ReplaceWith$ KWDevour" + Slot},
		SVars: []SVarDef{
			{"KWDevour" + Slot, "DB$ Sacrifice | Defined$ You | Amount$ KWDevourSacX" + Slot + " | RememberSacrificed$ True | Optional$ True | SacValid$ " + valid + ".Other | SubAbility$ KWDevourCounters" + Slot},
			{"KWDevourCounters" + Slot, "DB$ PutCounter | ETB$ True | Defined$ Self | CounterType$ P1P1 | CounterNum$ KWDevourX" + Slot + " | SubAbility$ KWDevourCleanup" + Slot},
			{"KWDevourCleanup" + Slot, "DB$ Cleanup | ClearRemembered$ True"},
		},
		Amounts: []SVarDef{
			{"KWDevourSacX" + Slot, "Count$Valid " + valid + ".YouCtrl+Other"},
			{"KWDevourX" + Slot, "Count$RememberedSize/Times." + args[0]},
		},
	}, true
}

// expandCycling is CardFactoryUtil.java:3717's Cycling branch: pay the cost and
// discard the card from hand to draw a card.
func expandCycling(k Keyword) (Expansion, bool) {
	args := k.Args()
	if len(args) < 1 || args[0] == "" {
		return Expansion{}, false
	}
	return Expansion{Abilities: []string{"AB$ Draw | Cost$ " + args[0] + " Discard<1/CARDNAME> | ActivationZone$ Hand"}}, true
}

// amountDetail is the first detail of an Amount-shaped keyword ("Bushido:2"),
// false when it is missing or not a plain number.
func amountDetail(k Keyword) (string, bool) {
	args := k.Args()
	if len(args) < 1 || args[0] == "" || strings.Trim(args[0], "0123456789") != "" {
		return "", false
	}
	return args[0], true
}

// costDetail is the cost an Echo-shaped keyword names: its first detail, the
// whole text up to the next colon.
func costDetail(k Keyword) string {
	args := k.Args()
	if len(args) < 1 {
		return ""
	}
	return args[0]
}
