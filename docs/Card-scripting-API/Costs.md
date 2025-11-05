Cost is a class that attempts to streamline costs throughout all cards. It requires that each cost is separated by a space. I will use examples that could be found in Ability, although certain Keyworded abilities do use Cost too.

# Common

## Description

Description is an optional last parameter in the cost. This is to allow
for complex Type definitions to have a nice Description that is readable.

## CostDesc / PrecostDesc

## UnlessCost

UnlessCost allows the player specified with UnlessPayer (same as
Defined, defaults to TargetedController) to pay mana to prevent the
resolving of the ability. If the script has the param "UnlessSwitched",
then the player pays mana to resolve the ability (usually used to handle
"any player may pay ..." ).

## XChoice

XChoice is the variable that basically means "You can choose whatever
you want for this variable. But you need to decide what X is before you
start paying." This would commonly appear as an SVar definition of X.

## xPaid

xPaid is the amount of Mana Paid for an X Cost. There are a few cards
that will use the X Payment to determine other costs (like Abandon Hope)
This would commonly appear as an SVar definition of X.

## CARDNAME

For Costs that do something to themselves (ex. Discard Self, Sacrifice
Self)

# Types of Cost

## Discard

Discard has two required parameters and one optional in the form
Discard<Num/Type/Description>

-   The first is how many cards are being discarded.
-   The second is what card types can be discarded. (Hand for the whole
    hand, Random for chosen randomly)

## Draw

## Exert

## Exile

Exile has two required parameters and one option in the form of
Exile<Num/Type/Description>

There are also a few sister abilities that all fit under the Exile
umbrella.

-   Exile (for cards on the Battlefield)
-   ExileFromGraveyard
-   ExileFromHand
-   ExileFromTop (for cards on top of your library, this doesn't default
    Type to Card, so make sure you add it)

Some Examples

-   Exile&lt;1/Creature&gt;
-   Exile&lt;1/CARDNAME&gt;
-   ExileFromHand&lt;1/CARDNAME&gt;
-   ExileFromHand&lt;2/Creature&gt;
-   ExileFromGrave&lt;1/CARDNAME&gt;
-   ExileFromGrave&lt;1/Treefolk&gt;
-   ExileFromTop&lt;10/Card&gt;

## FlipCoin

Only used by "Karplusan Minotaur".

## Mana

-   Cost$ 2
    -   2 colorless mana
-   Cost$ B R
    -   1 black and 1 red mana
-   Cost$ WG
    -   Hybrid White/Green mana
-   Cost$ S
    -   Snow Mana
-   Cost$ Mana&lt;2\\Creature&gt;
    -   2 colorless produced by a source with type 'creature'. Note the
        backslash - it was chosen because hybrid costs already use slash

Here's some examples:

-   Discard&lt;1/Card&gt;
    -   "Discard 1 Card"
-   Discard&lt;0/Hand&gt; (The number is ignored when Hand is used as a
    type.)
    -   Discard your hand
-   Discard&lt;2/Random&gt;
    -   Discard 2 Cards at Random
-   Discard&lt;1/CARDNAME&gt;
    -   Discard Self (CARDNAME)
-   Discard&lt;1/Creature.Black/Black Creature&gt;
    -   Discard 1 Black Creature

## Mill

## Subtract(Remove) Counter

SubCounter has two required parameters in the form of
SubCounter<Num/CounterName>

-   SubCounter&lt;2/P1P1&gt;
-   SubCounter&lt;1/CHARGE&gt;

Remember the token name should appear all in caps.

As third parameter you can use a ValidCard.

## Sacrifice

Sacrifice has two required parameters and one optional parameter in the
form of Sac<Num/Type/Description>

-   Sac&lt;1/Artifact&gt;
-   Sac&lt;1/CARDNAME&gt;

## Tap

-   Cost$ T

## Untap

-   Cost$ Untap

\- or -

-   Cost$ Q

## Unattach

## PayEnergy

## PayLife

PayLife has one required parameter in the form of PayLife<Num>

-   PayLife&lt;2&gt;

## GainLife

## TapXType

TapXType has two required parameters and one option in the form of
tapXType<Num/Type/Description>

-   tapXType&lt;3/Creature.White&gt;

## Return

Return has two required parameters and one optional in the form of
Return<Num/Type/Description>

-   Return&lt;1/Land&gt;
-   Return&lt;1/CARDNAME&gt;

## Reveal

# Putting it Together

Putting it together is pretty simple. If a card needs to pay mana and tap, it would look like this:

-   Cost$1 W T

For a spell that has an additional cost of sacrificing a land, put the
mana cost and the additional cost in the cost:

-   Cost$2 G Sac&lt;1/Land&gt;

One of the features of Cost is you can have more than one of the same Cost type:

-   Cost$ Sac&lt;1/Swamp&gt; Sac&lt;1/Creature&gt;

There are many examples, but they mostly fall into those categories.
