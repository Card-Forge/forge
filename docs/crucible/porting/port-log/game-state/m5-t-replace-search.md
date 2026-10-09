# Port Log — Game State: M5 batch T2, library-search and TurnFaceUp replacements

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `replacemententry.go`, `changezoneeffect.go`,
  `choosecardeffect.go`, `valid.go`, `defined.go`, `attachobject.go`, `cloneeffect.go`, `setstateeffect.go`,
  `effecteffect.go`

Siblings: [`m5-s-redirects.md`](m5-s-redirects.md) (ADR-0040 search control),
[`m5-replacement-2.md`](m5-replacement-2.md#turnfaceup-and-transform), [`m5-replacement-3.md`](m5-replacement-3.md).

## FoundSearchingLibrary: Opposition Agent's exile

`changeHiddenOriginResolve` passes `FoundSearchingLibrary` = `searchedLibrary` with every move of a fetcher's chosen
cards (`ChangeZoneEffect.java:1277`, `:1308`); `ReplaceMoved.canReplace` admits a line naming the param only then
(`ReplaceMoved.java:92-98`). `searchedLibrary` is true for a library `Origin$` without `NoLooking$` whose decider may
search (`:1020-1039`), and `Searched$` turns it back to false after its grant (`:1090-1092`).

| Piece                 | Port                                                                                                                                                                                                                                                                                                                            |
| --------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `hiddenChoice.found`  | Java's `HiddenOriginChoices.searchedLibrary`, per fetcher                                                                                                                                                                                                                                                                       |
| `foundSearchReplaced` | Before `moveByEffect`, a CR 616 walk (card controller decides, `ReplacementHandler.java:196-203`) over the `FoundSearchingLibrary$` lines: `Origin$`, `Destination$`, `ValidCard$`, requirements, `Optional$`. Chain via `runReplacementChain`; a chain that must move `ReplacedCard`. Another param or `Layer$`: pending error |
| Replaced card         | `GameAction.changeZone` returns the card (`GameAction.java:341-368`); Java goes on with it: `ExiledWith` for an exiling search, `RememberChanged$`/`Imprint$` (`ChangeZoneEffect.java:1416-1507`). The chain's own move fires its zone-change triggers                                                                          |
| `entryShape` guard    | A `FoundSearchingLibrary$` line never matches a battlefield entry. Before, the Agent's line (no `Destination$`, chain moves `ReplacedCard`) exiled any opponent card entering from a library, search or not                                                                                                                     |
| RepExile + `DBEffect` | `ChangeZone Hidden$ Origin$ All Defined$ ReplacedCard` and `Effect StaticAbilities$ MayPlay ... ForgetOnMoved$ Exile` already resolve; the sub-ability inherits `replacing`                                                                                                                                                     |

Deviation: Java offers the Agent's line in one CR 616 walk with every other `Moved` line of the move. Here it runs
first; the others apply to whichever move then happens (the chain's or the original). The Agent's line has no
`Destination$`, so every order ends with the card exiled; two Agents still get the affected player's choice.

## QuasiLibrarySearch$ and Searched$

`ChooseCardEffect.java:243-269`: one card from the `Choices$` pool, optional unless `Mandatory$`. An empty pool
(`PlayerControllerHuman.java:585-589` answers null without asking) or a decline `return`s from the whole effect, so the
host's chosen, remembered and imprinted cards are left as they were. The param's player is only the revealed library
(`:253-259`); an empty one is an `error` where Java's `get(0)` throws.

| Shape                                          | Port                                                                                                                                                      |
| ---------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Chooser = activator under search control       | `error` before asking: the grant pushed at `:245-251` is popped at `:267-269`, after the decline `return` at `:262-264` (Forge bug, PORT-8)               |
| `canSearchLibraryWith`, `LimitSearchLibrary`   | Not read: Java checks neither for the choices (`:254-256` shortens only the reveal). Reproduced, reported                                                 |
| ChangeZone `Searched$` (3 lines, same 3 cards) | Accepted: the grant around a choice (`:1067-1076`), `found` false. Its `incLibrarySearched` and `SearchedLibrary` trigger are not modelled for any search |
| `RememberSearched$`                            | Still refused                                                                                                                                             |

ChooseCard grants when chooser = activator whoever owns the library (`:245`); ChangeZone grants only when decider =
library owner (`:1071`). Not harmonized; unobservable while the grant shape is refused.

`sharesNameWith` (`CardProperty.java:818-870`): the bare form (the source) and `sharesNameWith <restriction>` for
`YourGraveyard`, `Graveyard`, `Battlefield`, `NonToken`, `Remembered`, `Imprinted`. A valid string is matched without
its ability, and the `valid` group may not import `defined`, so `ThisTurnCast`, `MovedToGrave`, `TriggeredCard` and the
other `Defined$` forms match nothing. `Defined$ ParentTarget` names players too (`AbilityUtils.java:947-951`,
Dichotomancy's `DBShuffle`); no targeting ancestor is an error, as for cards.

| Scenario                                                                        | Proves                                                   |
| ------------------------------------------------------------------------------- | -------------------------------------------------------- |
| `quasi-search-doubling-chant-fetches-a-same-named-creature`                     | RepeatEach + `sharesNameWith Remembered` + `Searched$`   |
| `quasi-search-doubling-chant-declined-finds-nothing`                            | decline: nothing imprinted, nothing moves                |
| `quasi-search-dichotomancy-steals-a-same-named-card-from-the-opponents-library` | `TargetedPlayer` library, `GainControl$`, `ParentTarget` |
| `opposition-agent-exiles-the-land-an-opponent-finds`                            | exile instead of entering; Agent controller plays it     |

Tests: `librarysearchreplaced_test.go`, `quasilibrarysearch_test.go`.

## Event$ TurnFaceUp remainder

Morph and Disguise casting are not ported; every fixture turns a manifested card face up with Ixidor.

| Card                 | Port                                                                                                                                                                                                                                                                                                                                                                                                                                |
| -------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Bubble Smuggler      | Already resolved (`faceChangeReplaced`); fixture added                                                                                                                                                                                                                                                                                                                                                                              |
| Unable to Scream     | `Card.EnchantedBy+faceDown` CantHappen already resolved; module test                                                                                                                                                                                                                                                                                                                                                                |
| Gift of Doom         | `attachObjectToChoice` (`AttachEffect.java:72-152`): a non-optional pick (`PlayerController.java:132`) among `Choices$` permanents every attachment can be attached to (`canBeAttached`: `GameEntity.java:236-307`, `Card.java:6996-7034`; of the CantAttach statics only Protection is read), then the `Optional$` confirm and `RememberAttached$`. `Chooser$`, `PlayerChoices$`, `Move$`, a non-battlefield `ChoiceZone$` refused |
| Vesuvan Shapeshifter | Clone `Duration$ UntilFacedown` (`SpellAbilityEffect.java:1029-1030`): `effectUntilFacedown`, ended by `endCopiesOnFaceDown`. SetState `Mode$ TurnFaceDown` (`Card.java:762-767`, `:841-863`)                                                                                                                                                                                                                                       |

`turnFaceDown`: a face-up permanent becomes a face-down 2/2 with a new timestamp (CR 613.7f); its UntilFacedown copies
end first, since the face-down state takes the one `Def` slot. Double-faced and already face-down permanents do not turn
(Java's `isDoubleFaced`, read off the card's own rules). Refused before anything turns: a card off the battlefield, a
Room, a permanent under any other copy effect. No replacement or trigger runs. `UntilFacedown` also unblocks Vesuvan's
entry copy (`K:ETBReplacement:Copy:DBCopy:Optional`), which failed at `cloneDuration` before.

| Scenario                                                                     | Proves                                                      |
| ---------------------------------------------------------------------------- | ----------------------------------------------------------- |
| `replacement-turn-face-up-bubble-smuggler-gets-four-counters`                | `PutCounter` after the flip                                 |
| `replacement-turn-face-up-gift-of-doom-attaches-to-a-creature`               | `Object$ Self` + `Choices$`, confirmed                      |
| `replacement-turn-face-up-gift-of-doom-declined-goes-to-the-graveyard`       | declined: unattached Aura, CR 704.5m                        |
| `replacement-turn-face-up-vesuvan-shapeshifter-copies-a-creature`            | `Optional$` copy keeps the 0/0 alive                        |
| `replacement-turn-face-up-vesuvan-shapeshifter-declined-dies-as-a-zero-zero` | declined: its own 0/0 dies                                  |
| `vesuvan-shapeshifter-enters-as-a-copy-until-face-down`                      | the entry copy (`ETBReplacement:Copy`) with `UntilFacedown` |

Tests: `turnfaceupremainder_test.go` (copy ends on turning face down, TurnFaceDown no-ops and refusals, the
`canBeAttached` branches).

## Not landed

- `Event$ RollDice` for Vedalken Squirrel-Whacker's `DicePTExchanges`: the swap loop after the roll
  (`RollDiceEffect.java:233-262`) asks `chooseRollToSwap` and `chooseRollSwapValue`, two decisions `PlayerController`
  does not have, and writes a new base P/T. Still fails closed (`rollReplacementResolvable`).
- `incLibrarySearched` and the `SearchedLibrary` trigger mode, for any search.
