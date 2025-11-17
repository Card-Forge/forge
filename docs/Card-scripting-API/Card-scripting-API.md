A reference guide for scripting cards using the API parsed by the Forge engine.

# Base Structure

By opening any file in the /res/cardsfolder folder you can see the basic structure of how the data is created.  
Here's an example of a vanilla creature:

```
Name:Vanilla Creature
ManaCost:2 G
Types:Creature Beast
PT:2/2
Oracle:
```

The name of this card is Vanilla Creature.  
It's casting cost is {2}{G}.  
It has the types Creature and Beast.  
It has a Power-Toughness of 2/2.  
It will not display any additional text in the card's template.  

If a card has two faces, use AlternateMode:{CardStateName} in the front face and separate both by a new line with the text "ALTERNATE".

There are a few other properties that will appear in many cards. These are

| Property | Description
| - | -
|`A`|[Ability effect](Card-scripting-API/AbilityFactory.md)
|`AI`|RemoveDeck:<br />* `All`<br />This will prevent the card from appearing in random AI decks. It is applicable for cards the AI can't use at all and also for cards that the AI could use, but only ineffectively. The AI won't draft these cards.<br />* `Random`<br /> This will prevent the card from appearing in random decks. It is only applicable for cards that are too narrow for random decks like *Root Cage* or *Into the North*. The AI won't draft these cards.<br />* `NonCommander`<br />
|`Colors`|Color(s) of the card<br /><br />When a card's color is determined by a color indicator rather than shards in a mana cost, this property must be defined. If no identifier is needed, this property should be omitted.<br /><br />Example:<br />`Colors:red,green` - Since *Arlinn, Embraced by the Moon* has no mana cost (it's the back of a double-faced card), the red and green indicator must be included.
|`DeckHints`|AI-related hints for a deck including this card<br /><br />To improve synergy this will increase the rank of of all other cards that share some of its DeckHints types. The following types are supported:<br />* Color<br />* Keyword<br />* Name<br />* Type<br /><br />This helps with smoothing the selection so cards without these Entries won't be at an unfair disadvantage.<br /><br />The relevant code can be found in the [CardRanker](https://github.com/Card-Forge/forge/blob/master/forge-gui/src/main/java/forge/gamemodes/limited/CardRanker.java) class.
|`DeckNeeds`|This can be considered a stronger variant when the AI should not put this card into its deck unless it has whatever other type is specified. The way this works is "inverted": it will directly decrease the rank of the card unless other cards are able to satisfy its types.<br />If a card demands more than one kind of type you can reuse it:<br />`DeckNeeds:Type$Human & Type$Warrior` will only find Human Warrior compared to `DeckNeeds:Type$Human\|Warrior` which is either
|`DeckHas`|Specifies that the deck now has a certain ability (like, token generation) so that the drafting/deckbuilding AI knows that it now meets requirements for DeckHints/DeckNeeds. This is useful since many of these are not deduced by parsing the abilities, so an explicit hint is necessary. If you want to create a new archetype make sure to tag at least roughly 50 cards to start with and balacing the Has/Needs ratio. Currently used values are:<br />* Counters<br />* Graveyard<br />* Token<br /><br />Using the generic types is also supported in case the implicit parsing wouldn't find it (TokenScript$ is also included).<br />It doesn't require exact matching to have an effect but cards that care about multiple entries for a given type will be judged higher if a card seems to provide even "more" synergy for it.<br /><br />Example:<br />*Chishiro* has two abilities so `DeckHas:Ability$Token & Ability$Counters` is used, therefore score for `DeckNeeds:Ability$Token\|Counters` is increased
|`K`|Keyword (see below)
|`Loyalty`|Number of starting loyalty counters
|`ManaCost`|Cost to cast the card shown in mana shards<br /><br />This property is required. It has a single parameter that is a mana cost.<br /><br />* `ManaCost:no cost` for cards that cannot be cast<br />* `ManaCost:1 W W` sets the casting cost to {1}{W}{W}
|`Name`|Name of the card<br /><br />A string of text that serves as the name of the card. Note that the registered trademark symbol cannot be included, and this property must have at least one character.<br /><br />Example:<br />* `Name:A Display of My Dark Power` sets the card's name to "A Display of My Dark Power"
|`Oracle`|The current Oracle text used by the card.<br /><br />We actually have a Python Script that runs to be able to fill in this information, so don't worry about manually editing a lot of cards when Wizards decides to change the rules. <br /><br />This field is used by the Deck Editor to allow non-Legendary Creatures to be marked as potential commanders. Make sure "CARDNAME can be your commander." appears in the oracle text.
|`PT`|Power and toughness
|`R`|[Replacement effect](Card-scripting-API/Replacements.md)
|`S`|[Static ability](Card-scripting-API/Statics.md)
|`SVar`|String variable. Used throughout scripting in a handful of different ways.
|`T`|[Triggered ability](Card-scripting-API/Triggers.md)
|`Text`|Additional text that needs to be displayed on the CardDetailPanel that doesn't have any spell/ability that generates a description for it, e.g. "CARDNAME can be your commander." or "X can't be 0.".
|`Types`|Card types and subtypes<br /><br />Include all card types and subtypes, separated by spaces.<br /><br />Example:<br />* `Types:Enchantment Artifact Creature Golem` for a card that reads Enchantment Artifact Creature -- Golem

Rarity and Set info are now defined in edition definition files. These can be found at /res/reditions path.

## Conventions
- filename: all lowercase, skip special characters, underscore for spaces
- Unix(LF) line endings
- use empty lines only when separating multiple faces on a card
- AI SVars right before the Oracle
- try to avoid writing default params to keep scripts concise
  - e.g. just `SP$ Draw` instead of `SP$ Draw | Defined$ You | NumCards$ 1`

# Keywords

All keywords need to be prepended with "K:" to be parsed correctly. Each keyword must appear on a separate line.

## Keywords without Parameters

This section is for Keywords that require no additional parameters and are one or two words long. Most of these you would see exactly on cards in the game.  
Examples:

- Cascade
- Cipher
- Convoke
- Devoid
- First Strike
- Flash
- Haste
- Indestructible
- Mentor
- Provoke
- Reach
- Shroud
- Split second
- Storm
- Trample
- Undying
- Vigilance
- Wither

## Keywords with parameters

- Adapt:{cost}
- Afterlife:{N}
- AlternateAdditionalCost:{cost}
- Amplify:{cost}:{validType(comma separated)}
- Annihilator:{magnitude}
- Bloodthirst:{magnitude}
- Bestow:{cost}
- Bushido:{magnitude}
- Champion:{validType}
- Crew:{cost}
- Cumulative upkeep:{cost}:{Description}
- Cycling:{cost}
- Dash:{cost}
- Devour:{magnitude}
- Dredge:{magnitude}
- Echo:{cost}
- Emerge:{cost}
- Enchant:{Type}
- Entwine:{cost}
- Equip:{cost}
- Evoke:{cost}
- Fabricate:{cost}
- Fading:{FadeCounters}
- Flashback:{cost}
- Foretell:{cost}
- Fortify:{cost}
- Graft:{value}
- Haunt:{ability}:<Ability>
- Hexproof:{ValidCards}:[Description]
- Kicker:{cost}
- Level up:{cost}
- Madness:{cost}
- MayEffectFromOpeningHand:{Effect}
- Miracle:{cost}
- Modular:{magnitude}
- Monstrosity:{cost}
- [Mega]Morph:{cost}
- Multikicker:{magnitude}
- Mutate:{cost}
- Ninjutsu:{cost}
- Outlast:{cost}
- Partner:{CardName}
- Protection:{ValidCards}:{Description}
- Prowl:{cost}
- Rampage:{magnitude}
- Recover:{cost}
- Renown:{N}
- Replicate:{cost}
- Ripple:{magnitude}
- Soulshift:{magnitude}
- Strive:{cost}
- Suspend:{turns}:{cost}
- Transmute:{cost}
- Toxic:{poisonCounters}
- TypeCycling:{Type}:{cost}
- Unearth:{cost}
- Vanishing:{TimeCounters}

## Plaintext keywords

These are hardcoded but not truly keywords rules-wise and will eventually be turned into static abilities.
Only listing the most common ones here so you can recognize them.
CARDNAME is replaced by the card's name ingame.

- CARDNAME can't attack or block alone.
- CARDNAME can't block unless a creature with greater power also blocks.
- CARDNAME must be blocked if able.
- Remove CARDNAME from your deck before playing if you're not playing for ante.
- You may choose not to untap CARDNAME during your untap step.
- CantSearchLibrary

# General SVars

`SVar:SoundEffect:goblinpolkaband.mp3`

The sound system supports a special SVar that defines the sound that should be played when the spell is cast.

`SVar:X:Count$`

Count is our general value computation function. It's quite varied with a lot of different things it can calculate and is often being updated.

# AI specific SVars

`SVar:AIPreference:SacCost$Creature.token,Creature.cmcLE2`

`SVar:AntiBuffedBy:[ValidCards]`

If a permanent with this SVar is on the battlefield under human control
the AI will play the specified cards in Main1. Applicable for cards like
Heart Sliver or Timid Drake.

`SVar:BuffedBy:[ValidCards]`

If a permanent with this SVar is on the battlefield under its control
the AI will play the specified cards in Main1. Applicable for creatures
with a P/T setting static ability (Kithkin Rabble) or additional buffes
(Radiant, Archangel).

`SVar:EnchantMe:[Multiple]/[Once]`

Creatures with "Multiple" in this SVar will always be prefered when the
AI enchants (Rabid Wombat), creatures with "Once" only if they are not
enchanted already (Gate Hound).

`SVar:EquipMe:[Multiple]/[Once]`

Creatures with "Multiple" in this SVar will always be prefered when the
AI equippes (Myr Adapter), creatures with "Once" only if they are not
equipped already (Kor Duelist).

`SVar:AIEvaluationModifier:[ValidAmount]`

`SVar:EndOfTurnLeavePlay:True`

`SVar:HasCombatEffect:True`

`SVar:HasAttackEffect:True`

`SVar:HasBlockEffect:True`

`SVar:MustAttack:True`

`SVar:MustBeBlocked:True`

`SVar:NeedsToPlayVar:[ValidCards]`

`SVar:ManaNeededToAvoidNegativeEffect:`

`SVar:NonStackingEffect:True`

`SVar:PlayMain1:TRUE/ALWAYS/OPPONENTCREATURES`

The AI will play cards with this SVar in its first main phase. Without other AILogic, it will usually not play any permanents without this in Main1.

`SVar:SacMe:[number]`

The AI will sacrifice these cards to pay costs. The higher the number the higher the priority. Example: Hatching Plans has SVar:SacMe:5.

`SVar:Targeting:Dies`

`SVar:UntapMe:True`

The AI will prioritize untapping of this card.

`SVar:AIUntapPreference:`

`SVar:NoZeroToughnessAI:True`
