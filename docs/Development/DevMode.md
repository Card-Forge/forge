Developer Mode is a Mode that allows Developers to try out different
things and gain different shortcuts during play. In a Normal program,
this Mode would be available in a Debug build, but removed from the code
during the Retail build. Since Forge is in constant Beta, this Mode is
available during our Beta releases. You can turn on or off this mode at the
Home View -> Game Settings -> Preferences -> Gameplay Options section.

It's important to note you won't get achievements in a game once you started cheating.

## View Zone

There are a few View Zone menu item which help to make sure your Library
has what you think or the AI isn't being too overly foolish. They do
exactly what you might think. Click on the menu item, and all Cards in
that Zone are revealed to you.

## Generate Mana

Useful for not needing Land in your Deck while you are just trying to
test something out, by selecting Generate Mana, 7 of each type is generated and put into your Mana Pool.

## Setup Game State

The Setup Game State is a Command that lets you open an external file to
Add things into the Game State. Currently the command uses an external
text file that is opened through a file open dialog when you click on
the Setup Game State menu item. The file structure is simple. It must
contain any of the following lines defining what needs to be changed in
the current game state:

### HumanLife

Defines the value to set the human's current life to. Example:
HumanLife=6

### AILife

Defines the value to set the computer opponent's life to. Example:
AILife=14

### HumanCardsInPlay

Defines the cards that are to be placed under Human's control on the
battlefield. Can be just one card name or a list of card names separated
with semicolons. Example: HumanCardsInPlay=Swamp; Swamp; Drudge
Skeletons

### AICardsInPlay

Defines the cards that are to be placed under Computer's control on the
battlefield. Can be just one card name or a list of card names separated
with semicolons. Example: AICardsInPlay=Island

### HumanCardsInHand

Defines the cards that are to replace the Human's current hand. Old
cards in the Human's card will be removed. Can be just one card name or
a list of card names separated with semicolons, see HumanCardsInPlay or
AICardsInPlay above for examples.

### AICardsInHand

Defines the cards that are to replace the Computer's current hand. Old
cards in the Computer's card will be removed. Can be just one card name
or a list of card names separated with semicolons, see HumanCardsInPlay
or AICardsInPlay above for examples.

### HumanCardsInGraveyard

Defines the cards that are to replace the Human's current graveyard. Old
cards in the Human's graveyard will be removed. Can be just one card
name or a list of card names separated with semicolons, see
HumanCardsInPlay or AICardsInPlay above for examples.

### AICardsInGraveyard

Defines the cards that are to replace the Computer's current graveyard.
Old cards in the Computer's graveyard will be removed. Can be just one
card name or a list of card names separated with semicolons, see
HumanCardsInPlay or AICardsInPlay above for examples.

### HumanCardsInLibrary

Defines the cards that are to replace the Human's current library. Old
cards in the Human's library will be removed. Can be just one card name
or a list of card names separated with semicolons, see HumanCardsInPlay
or AICardsInPlay above for examples.

### AICardsInLibrary

Defines the cards that are to replace the Computer's current library.
Old cards in the Computer's library will be removed. Can be just one
card name or a list of card names separated with semicolons, see
HumanCardsInPlay or AICardsInPlay above for examples.

### HumanCardsInExile

Defines the cards that are to replace the Human's current exile area.
Old cards in the Human's exile area will be removed. Can be just one
card name or a list of card names separated with semicolons, see
HumanCardsInPlay or AICardsInPlay above for examples.

### AICardsInExile

Defines the cards that are to replace the Computer's current exile area.
Old cards in the Computer's exile area will be removed. Can be just one
card name or a list of card names separated with semicolons, see
HumanCardsInPlay or AICardsInPlay above for examples.

### ActivePlayer

Defines the active player that must be given priority when the game
state is set up. If this line is absent, the current player is not
changed. There are two valid values for this option: Human and AI. Note
that this option does not change the current phase, only the current
player. Example: ActivePlayer=AI

### ActivePhase

Changes the current phase of the game to the one specified. The valid
values are Untap, Upkeep, Draw, Main1, Declare Attackers, Declare
Blockers, Main2, End of Turn, and Cleanup. Note that the phase names are
case-sensitive.

### Comments

Game state files are allowed to have comments - any line that is
prefixed with a pound sign (\#) is not parsed and is considered to be a
text fragment. Also, empty lines are ignored. Example: <b>\# This is a
comment.</b>

### Specifying a Set for Spawned Cards

When specifying card names, you can optionally add the set code by
appending the pipe sign (|) and the three-letter set code to the card
name you are spawning. That will allow you to spawn cards from specific
sets. For example, <b>HumanCardsInPlay=Mountain; Mountain</b> will just
spawn two Mountains on the human battlefield, using the latest possible
set for them, while <b>HumanCardsInPlay=Mountain|4ED; Mountain|4ED</b>
will spawn two Mountains from the 4th Edition on the human battlefield.

### Samples

Here are two examples of valid state configuration files:

    HumanLife=5
    AILife=6
    HumanCardsInPlay=Forest; Forest; Forest; Llanowar Elves
    AICardsInPlay=Mountain; Mountain; Mountain
    HumanCardsInHand=Island; Raging Goblin
    AICardsInHand=Swamp; Swamp; Forest

Sets human's life to 5, AI life to 6, puts 3 Forests and Llanowar Elves
on the human battlefield, puts 3 Mountains on the computer battlefield,
replaces the human hand with Island and Raging Goblin, replaces the AI
hand with 2 Swamps and a Forest. Does not add any cards to either
graveyard and does not replace either library.

    HumanCardsInPlay=Plains|10E
    AICardsInPlay=Mountain; Raging Goblin
    AICardsInGraveyard=Force of Nature; Raging Goblin; Amulet of Kroog
    ActivePlayer=Human
    ActivePhase=Main1

Does not change the human's life and the AI life, puts a Plains from the
10th Edition on the human battlefield, puts a Mountain and a Raging
Goblin on the opponent's battlefield, does not replace the human and the
AI hands and leaves them intact, and adds Force of Nature, Raging
Goblin, and Amulet of Kroog to the computer's graveyard. Does not
replace either library. Sets the current player to Human and the current
phase to Main 1.
