package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// TestEffectsFailClosedOnUnresolvedShapes proves each effect of this pack
// reports an error, rather than guessing, for a param or value it cannot
// honour yet (PORT-8, GO-7) -- one representative line per branch.
func TestEffectsFailClosedOnUnresolvedShapes(t *testing.T) {
	t.Parallel()

	for _, line := range []string{
		"DB$ ChangeZone | Defined$ Self | Origin$ Nowhere | Destination$ Hand",
		"DB$ ChangeZone | Defined$ Self | Origin$ Battlefield | Destination$ Hand | GainControl$ TriggeredPlayer",
		"DB$ ChangeZone | Defined$ TriggeredTarget | Origin$ Battlefield | Destination$ Hand",
		"DB$ ChangeZone | Origin$ Library | Destination$ Hand | DefinedPlayer$ TriggeredPlayer",
		"DB$ ChangeZone | Origin$ Library | Destination$ Hand | ChangeNum$ Bogus",
		"DB$ ChangeZone | Origin$ Library | Destination$ Hand | Defined$ TriggeredTarget",
		"DB$ ChangeZone | Origin$ Library | Destination$ Library | LibraryPosition$ Bogus",
		"DB$ ChangeZoneAll | ChangeType$ Card | Origin$ Hand | Destination$ Nowhere",
		"DB$ ChangeZoneAll | ChangeType$ Card | Origin$ Nowhere | Destination$ Hand",
		"DB$ ChangeZoneAll | ChangeType$ Card | Defined$ TriggeredPlayer | Origin$ Hand | Destination$ Graveyard",
		"DB$ ChangeZoneAll | ChangeType$ Card | Origin$ Hand | Destination$ Library | LibraryPosition$ 3",
		"DB$ Dig | DigNum$ 2 | DestinationZone$ Nowhere",
		"DB$ Dig | DigNum$ 2 | DestinationZone2$ Nowhere",
		"DB$ Dig | DigNum$ 2 | LibraryPosition2$ 5",
		"DB$ Dig | DigNum$ 2 | ChangeNum$ Bogus",
		"DB$ Dig | DigNum$ 2 | Defined$ TriggeredPlayer",
		"DB$ DigUntil | Valid$ Card | Amount$ Bogus | FoundDestination$ Hand",
		"DB$ DigUntil | Valid$ Card | MaxRevealed$ Bogus | FoundDestination$ Hand",
		"DB$ DigUntil | Valid$ Card | FoundDestination$ Nowhere",
		"DB$ DigUntil | Valid$ Card | RevealedDestination$ Nowhere",
		"DB$ DigUntil | Valid$ Card | NoneFoundDestination$ Nowhere",
		"DB$ DigUntil | Valid$ Card | OptionalNoDestination$ Nowhere",
		"DB$ DigUntil | Valid$ Card | FoundDestination$ Library | FoundLibraryPosition$ 4",
		"DB$ DigUntil | Valid$ Card | RevealedDestination$ Library | RevealedLibraryPosition$ 4",
		"DB$ DigUntil | Valid$ Card | NoneFoundDestination$ Library | NoneFoundLibraryPosition$ 4",
		"DB$ DigUntil | Valid$ Card | Defined$ TriggeredPlayer",
		"DB$ DigUntil | Valid$ Creature | FoundDestination$ Hand | Shuffle$ True | ShuffleCondition$ Always",
		"DB$ RearrangeTopOfLibrary | NumCards$ Bogus",
		"DB$ RearrangeTopOfLibrary | NumCards$ 2 | RearrangePlayer$ Opponent",
		"DB$ RearrangeTopOfLibrary | NumCards$ 2 | Defined$ TriggeredPlayer",
		"DB$ Explore | Num$ Bogus",
		"DB$ Explore | Defined$ TriggeredTarget",
		"DB$ Explore | Condition$ Kicked",
		"DB$ LookAt | Defined$ Self | Condition$ Kicked",
		"DB$ Branch | BranchConditionSVar$ 1 | BranchConditionSVarCompare$ GE",
		"DB$ Branch | BranchConditionSVar$ 1 | BranchConditionSVarCompare$ GEBogus",
		"DB$ Branch | BranchConditionSVar$ 1 | Condition$ Kicked",
		"DB$ GenericChoice | Defined$ You | ChoiceAmount$ Bogus",
		"DB$ GenericChoice | Defined$ TriggeredPlayer",
		"DB$ Repeat | MaxRepeat$ Bogus",
		"DB$ Repeat | RepeatDefined$ Self",
		"DB$ RepeatEach | RepeatCards$ Creature | Zone$ Nowhere",
		"DB$ RepeatEach | DefinedCards$ TriggeredTarget",
		"DB$ RepeatEach | RepeatPlayers$ TriggeredPlayer",
		"DB$ Regenerate | Defined$ Self | RegenerationAbility$ DBX",
		"DB$ Regenerate | Defined$ TriggeredTarget",
		"DB$ Fog | Condition$ Threshold",
		"DB$ AddTurn | Defined$ You | NumTurns$ Bogus",
		"DB$ AddTurn | Defined$ TriggeredPlayer | NumTurns$ 1",
		"DB$ SkipTurn | Defined$ You | NumTurns$ Bogus",
		"DB$ SkipTurn | Defined$ TriggeredPlayer | NumTurns$ 1",
		"DB$ SkipTurn | Defined$ You | NumTurns$ 1 | Condition$ Kicked",
		"DB$ GainControl | Defined$ Self | NewController$ TriggeredPlayer",
		"DB$ GainControl | Defined$ TriggeredTarget",
		"DB$ ExchangeControl | Defined$ TriggeredTarget",
		"DB$ ExchangeControl | Defined$ Self | TargetsWithSameCardType$ Creature",
		"DB$ HealDamage | Defined$ TriggeredTarget",
		"DB$ HealDamage | Defined$ Self | Condition$ Kicked",
		"DB$ EachDamage | DefinedDamagers$ TriggeredTarget | Defined$ You",
		"DB$ EachDamage | DefinedDamagers$ Self | NumDmg$ Bogus | Defined$ You",
		"DB$ EachDamage | DefinedDamagers$ Self | ToEachOther$ TriggeredTarget",
		"DB$ EachDamage | DefinedDamagers$ Self | Defined$ TriggeredTarget",
		"DB$ EachDamage | DefinedDamagers$ Self | Defined$ You | Ultimate$ True",
		"DB$ DrainMana | Defined$ TriggeredPlayer",
	} {
		g, p, _ := newTwoPlayerGame(t)
		libraryCards(t, g, p, 3)
		c := engine.NewScriptedController()
		def := etbChainDef(t, "Test Fail Closed", line, "DBX", "DB$ GainLife | Defined$ You | LifeAmount$ 1")
		if _, err := castETBChain(t, g, p, def, c); err == nil {
			t.Errorf("%q: ResolveStack succeeded, want an error", line)
		}
	}
}

// TestTokenAnimateTriggerPackFailsClosed is the same guarantee for the
// token, Animate-shaped, delayed-trigger, dispatch and phase effects: one
// representative unresolved line per branch.
func TestTokenAnimateTriggerPackFailsClosed(t *testing.T) {
	t.Parallel()

	for _, line := range []string{
		"DB$ Token | TokenScript$ w_1_1_soldier | WithCountersType$ P1P1",
		"DB$ Token | TokenScript$ w_1_1_soldier | PumpKeywords$ Haste | PumpDuration$ UntilYourNextTurn",
		"DB$ Token | TokenScript$ w_1_1_soldier | TokenOwner$ TriggeredPlayer",
		"DB$ Token",
		"DB$ Investigate | Optional$ True",
		"DB$ Amass | Type$ Elf | Num$ 1",
		"DB$ Amass | Num$ 1",
		"DB$ Incubate | Amount$ Bogus",
		"DB$ Animate | Defined$ Self | Replacements$ DBX",
		"DB$ Animate | Defined$ Self | Power$ 1 | Duration$ Perpetual",
		"DB$ Animate | Defined$ Self | Types$ ChosenType",
		"DB$ Animate | Defined$ Self | Colors$ Plaid",
		"DB$ Animate | Defined$ Self | Keywords$ HIDDEN CARDNAME can't block.",
		"DB$ AnimateAll | ValidCards$ Creature | Zone$ Graveyard | Power$ 1",
		"DB$ Debuff | Defined$ Self | Keywords$ Forestwalk | AllSuffixKeywords$ walk",
		"DB$ Protection | Defined$ Self | Gains$ TargetedCardColor",
		"DB$ Protection | Defined$ Self | Gains$ Choice | Choices$ AnyColor | Choser$ Controller",
		"DB$ ProtectionAll | ValidCards$ Creature | ValidPlayers$ You | Gains$ red",
		"DB$ DelayedTrigger | Mode$ ChangesZone | Execute$ DBX",
		"DB$ DelayedTrigger | Mode$ Phase | Phase$ Upkeep | RememberNumber$ True | Execute$ DBX",
		"DB$ DelayedTrigger | Mode$ Phase | Phase$ Upkeep",
		"DB$ ImmediateTrigger | RememberSVarAmount$ X | Execute$ DBX",
		"DB$ ImmediateTrigger | TriggerAmount$ Bogus | Execute$ DBX",
		"DB$ Charm | ChoiceRestriction$ ThisGame | Choices$ DBX",
		"DB$ FlipCoin | ForEachPlayer$ Player | WinSubAbility$ DBX",
		"DB$ RollDice | Sides$ 6 | RerollResults$ True",
		"DB$ Clash | Defined$ TriggeredPlayer",
		"DB$ Seek | DefinedCards$ Self",
		"DB$ StoreSVar | SVar$ X | Type$ Targeted | Expression$ CardPower",
		"DB$ StoreSVar | SVar$ EachPlayer | Type$ Number | Expression$ 1",
		"DB$ StoreSVar | SVar$ X | Type$ Calculate | Expression$ Bogus",
		"DB$ Balance | Valid$ Creature | Zone$ Graveyard",
		"DB$ AddPhase | ExtraPhase$ Bogus",
		"DB$ AddPhase | ExtraPhase$ Combat | ExtraPhaseDelayedTrigger$ DBX",
		"DB$ SkipPhase | Defined$ You | Step$ Draw | Start$ True",
		"DB$ SkipPhase | Defined$ You | Step$ Draw | Duration$ UntilYourNextTurn",
		"DB$ SkipPhase | Defined$ You | Step$ Bogus",
	} {
		g, p, _ := newTokenGame(t)
		libraryCards(t, g, p, 3)
		c := engine.NewScriptedController()
		def := etbChainDef(t, "Test Fail Closed", line, "DBX", "DB$ GainLife | Defined$ You | LifeAmount$ 1")
		if _, err := castETBChain(t, g, p, def, c); err == nil {
			t.Errorf("%q: ResolveStack succeeded, want an error", line)
		}
	}
}

// TestFiftyPackFailsClosed is the same guard for the fifty-API pack.
func TestFiftyPackFailsClosed(t *testing.T) {
	t.Parallel()

	for _, line := range []string{
		"DB$ GameDrawn | Condition$ Kicked",
		"DB$ ChangeSpeed | Defined$ You | Mode$ Sideways",
		"DB$ GainOwnership | Defined$ Self | TgtZone$ Ante",
		"DB$ ReorderZone | Zone$ Nowhere | Defined$ You",
		"DB$ ReorderZone | Defined$ You",
		"DB$ EndTurn | Condition$ Kicked",
		"DB$ ExchangeLifeVariant | Defined$ You | Mode$ Colors",
		"DB$ ExchangePower | Defined$ Self | BasePower$ True",
		"DB$ ExchangePower | Defined$ Self | Duration$ Perpetual",
		"DB$ AddOrRemoveCounter | Defined$ Self | RemoveConditionSVar$ X",
		"DB$ AddOrRemoveCounter | Defined$ Self | CounterNum$ Bogus",
		"DB$ BecomesBlocked | Defined$ Self | Condition$ Kicked",
		"DB$ ChangeCombatants | Defined$ Self | Attacking$ TargetedPlayer",
		"DB$ GainControlVariant | AllValid$ Creature | ChangeController$ Bogus",
		"DB$ GainControlVariant | ChangeController$ CardOwner",
		"DB$ TapOrUntapAll | ValidCards$ Creature | Condition$ Kicked",
		"DB$ Intensify | Amount$ Bogus",
		"DB$ Blight | Defined$ You | Num$ Bogus",
		"DB$ TimeTravel | Amount$ Bogus",
		"DB$ Endure | Num$ Bogus",
		"DB$ AssignGroup | Defined$ Player | Chooser$ TriggeredPlayer | Choices$ DBX",
		"DB$ VillainousChoice | Defined$ You | Amount$ Bogus | Choices$ DBX",
		"DB$ TwoPiles | Defined$ You | Zone$ Nowhere",
		"DB$ TwoPiles | Defined$ You | DefinedPiles$ Self",
		"DB$ ChooseType | Defined$ You | Type$ Creature | Note$ True",
		"DB$ ChooseType | Defined$ You | Type$ Creature | AtRandom$ True",
		"DB$ ChooseType | Defined$ You | Type$ Bogus",
		"DB$ ChooseType | Defined$ You | Type$ Card | ValidTypes$ Artifact | InvalidTypes$ Artifact",
		"DB$ NameCard | Defined$ You | ChooseFromDefinedCards$ Self | AtRandom$ True",
		"DB$ NameCard | Defined$ You | ChooseFromDefinedCards$ Bogus",
		"DB$ NameCard | Defined$ You | AtRandom$ True",
		"DB$ NameCard | Defined$ You",
		"DB$ PreventDamage | Defined$ You | Amount$ 1 | DividedAsYouChoose$ True",
		"DB$ PreventDamage | Defined$ You",
		"DB$ PreventDamage | Defined$ You | Amount$ Bogus",
		"DB$ DigMultiple | DigNum$ 2 | ChangeValid$ Card | ChooseAmount$ 1",
		"DB$ DigMultiple | ChangeValid$ Card",
		"DB$ DigMultiple | DigNum$ 2 | ChangeValid$ Card | SourceZone$ Nowhere",
		"DB$ DigMultiple | DigNum$ 2 | ChangeValid$ Card | LibraryPosition$ 3",
		"DB$ Recruit | Defined$ TriggeredPlayer",
		"DB$ BidLife | StartBidding$ Bogus",
		"DB$ ExchangeControlVariant | Defined$ Player | Zone$ Graveyard",
		"DB$ DayTime | Value$ Dusk",
		"DB$ AlterAttribute | Defined$ Self | Attributes$ Saddled",
		"DB$ Vote | Defined$ Player | VoteCard$ Card | StoreVoteNum$ True",
		"DB$ Vote | Defined$ Player | VoteCard$ Card | Zone$ Nowhere",
		"DB$ MakeCard | Name$ Nonexistent Card | Zone$ Hand",
		"DB$ MakeCard | Booster$ True",
		"DB$ MakeCard | Name$ X | Zone$ Nowhere",
		"DB$ Learn | Condition$ Kicked",
		"DB$ CopyPermanent | Defined$ Self | AtEOTTrig$ Exile",
		"DB$ CopyPermanent | Defined$ ChosenMap",
		"DB$ CopyPermanent | Defined$ Self | SetPower$ Bogus",
		"DB$ Counter | Defined$ Parent",
		"DB$ Counter | TargetType$ Activated",
		"DB$ Manifest | Amount$ Bogus",
		"DB$ Manifest | ChoiceZone$ Nowhere",
		"DB$ ManifestDread | Amount$ Bogus",
		"DB$ SetState | Defined$ Self | Mode$ Specialize",
		"DB$ SetState | Defined$ Self | Mode$ Transform | NewState$ Backside",
		"DB$ Goad | Defined$ Self | Duration$ UntilHostLeavesPlay",
		"DB$ ActivateAbility | Defined$ You | Type$ Land",
		"DB$ MultiplePiles | Defined$ Player | Piles$ Bogus",
		"DB$ DamageResolve | ReplaceDyingDefined$ Self | ReplaceDyingZone$ Hand",
	} {
		g, p, _ := newPackGame(t)
		libraryCards(t, g, p, 3)
		c := engine.NewScriptedController()
		def := etbChainDef(t, "Test Fail Closed", line, "DBX", "DB$ GainLife | Defined$ You | LifeAmount$ 1")
		if _, err := castETBChain(t, g, p, def, c); err == nil {
			t.Errorf("%q: resolved, want an error", line)
		}
	}
}

// TestFiftyPackUnresolvableDefined proves each effect of the pack reports
// a Defined$ (or player/card reference) it cannot resolve as an error.
func TestFiftyPackUnresolvableDefined(t *testing.T) {
	t.Parallel()

	for _, line := range []string{
		"DB$ RemoveFromGame | Defined$ Bogus",
		"DB$ ChangeSpeed | Defined$ Bogus",
		"DB$ GainOwnership | Defined$ Bogus",
		"DB$ GainOwnership | Defined$ Self | DefinedPlayer$ Bogus",
		"DB$ ReorderZone | Zone$ Hand | Defined$ Bogus",
		"DB$ EndTurn | Optional$ True | Defined$ Bogus",
		"DB$ ChooseEvenOdd | Defined$ Bogus",
		"DB$ ExchangeLifeVariant | Mode$ Power | Defined$ Bogus",
		"DB$ ExchangePower | Defined$ Bogus",
		"DB$ TapOrUntapAll | Defined$ Bogus",
		"DB$ AddOrRemoveCounter | Defined$ Bogus",
		"DB$ AddOrRemoveCounter | Defined$ Self | DefinedPlayer$ Bogus",
		"DB$ Detain | Defined$ Bogus",
		"DB$ Intensify | Defined$ Bogus",
		"DB$ Blight | Defined$ Bogus",
		"DB$ Endure | Defined$ Bogus",
		"DB$ AssignGroup | Defined$ Bogus | Choices$ DBX",
		"DB$ VillainousChoice | Defined$ Bogus | Choices$ DBX",
		"DB$ TwoPiles | Defined$ Bogus",
		"DB$ TwoPiles | Defined$ You | Separator$ Bogus",
		"DB$ TwoPiles | Defined$ You | DefinedCards$ Bogus",
		"DB$ TwoPiles | Defined$ You | DefinedPiles$ Bogus,Self",
		"DB$ ChooseType | Defined$ Bogus | Type$ Card",
		"DB$ NameCard | Defined$ Bogus | ChooseFromList$ A",
		"DB$ PreventDamage | Defined$ Bogus | Amount$ 1",
		"DB$ DigMultiple | DigNum$ 1 | ChangeValid$ Card | Defined$ Bogus",
		"DB$ Recruit | Defined$ Bogus",
		"DB$ BidLife | OtherBidder$ Bogus",
		"DB$ ExchangeControlVariant | Defined$ Bogus",
		"DB$ AlterAttribute | Defined$ Bogus | Attributes$ Solved",
		"DB$ Vote | Defined$ Bogus | Choices$ DBX",
		"DB$ Vote | Defined$ Player | VotePlayer$ Bogus",
		"DB$ MakeCard | Defined$ Bogus | Name$ X",
		"DB$ MakeCard | DefinedName$ Bogus",
		"DB$ Learn | Defined$ Bogus",
		"DB$ CopyPermanent | Defined$ Bogus",
		"DB$ CopyPermanent | Defined$ Self | Controller$ Bogus",
		"DB$ CopyPermanent | Choices$ Creature | Chooser$ Bogus",
		"DB$ Manifest | Defined$ Bogus",
		"DB$ Manifest | DefinedPlayer$ Bogus",
		"DB$ ManifestDread | DefinedPlayer$ Bogus",
		"DB$ SetState | Defined$ Bogus | Mode$ Transform",
		"DB$ Goad | Defined$ Bogus",
		"DB$ RemoveFromMatch | Defined$ Bogus",
		"DB$ ActivateAbility | Defined$ Bogus | ManaAbility$ True",
		"DB$ MultiplePiles | Defined$ Bogus | Piles$ 2",
		"DB$ MultiplePiles | Defined$ You | Piles$ 2 | DefinedCards$ Bogus",
		"DB$ MultiplePiles | Defined$ You | Piles$ 2 | Zone$ Nowhere",
		"DB$ Block | DefinedAttacker$ Bogus",
		"DB$ Block | DefinedAttacker$ Self | DefinedBlocker$ Bogus",
		"DB$ ChangeCombatants | Defined$ Bogus | Attacking$ True",
		"DB$ TimeTravel | Condition$ Kicked",
	} {
		g, p, _ := newPackGame(t)
		libraryCards(t, g, p, 3)
		c := engine.NewScriptedController()
		def := etbChainDef(t, "Test Fail Closed", line, "DBX", "DB$ GainLife | Defined$ You | LifeAmount$ 1")
		if _, err := castETBChain(t, g, p, def, c); err == nil {
			t.Errorf("%q: resolved, want an error", line)
		}
	}
}
