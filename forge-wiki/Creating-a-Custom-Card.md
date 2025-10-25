# Guide - Creating a Custom Card

Using the Forge API you can create your own custom cards and sets. This guide will walk you through the process of creating custom cards via the Force Custom Card Script. 

The next tutorial will walk you through the process of adding your new cards to a custom set. 


## File Locations

### Custom Card Scripts

Windows:
> C:/Users/<your username>/Application Data/Roaming/Forge/custom/cards

Linux:
> ~/.forge/custom/cards

Mac:
> TODO Add filepath

### Custom Tokens

Windows:
> C:/Users/<your username>/Application Data/Roaming/Forge/custom/tokens

Linux:
> ~/.forge/custom/tokens

Mac:
> TODO Add filepath

### Custom Editions

> C:/Users/<your username>/Application Data/Roaming/Forge/custom/editions

Linux:
> ~/.forge/custom/editions

Mac:
> TODO Add filepath

### Card Images

Windows:
> C:/Users/<your username>/Application Data/Local/Forge/Cache/pics/cards

Linux:
> ~/.cache/forge/pics/cards

Mac:
> TODO Add filepath

## Creating a New Card

In this tutorial we'll create a new card called "Goblin Card Guide".

1. Open Script Directory

Navigate to your Custom Card Scripts folder and open (or create) the subfolder for the starting letter of your new card. 
In our example:
> C:/Users/<your username>/Application Data/Roaming/Forge/custom/cards/g

2. Add Card Script

Create a new .txt file using all lowercase letters. Spaces are represented with "_" and you can leave out special characters like commas or apostrophes. 
> goblin_card_guide.txt

Now we need to define our card. Add the following to your Goblin_Card_Guide text file:

```
Name:Goblin Card Guide
ManaCost:1 R
Types:Creature Goblin
PT:2/2
K:Haste
Oracle:Haste
```

Let's break our card down:
 - Name - The name as it appears on the card.
 - ManaCost - The card's cost with colorless mana first and spaces between different mana symbols.  
 - Types - The card's type and then any subtypes seperated by spaces. 
 - PT - Power and Toughness, which is only used for Creatures (or some cards that turn into creatures like Vehicles)
 - K - A Keyword that gives our creature an ability. 
 - Oracle - The actual text that appears on that card. 

For a refrence of possible fields and their descriptions please see the [Card Scripting API](Card-scripting-API) document. Note that maintaining an up to date list of every ability of every Magic Card ever printed is not feasable. The API is meant to serve as a guide and includes most common used information. 

3. Add Card to a Set

Now that we have a new card we need to add it to a set so that it will be included in the game. For the purposes of this turorial we'll add out card to an existing set. If you wish to create your own set you can follow this guide: `Guide Coming Soon`

Navigate to your Custom Editions folder and open the text file for the set you would like to add your card to. Let's add our card to the "DCI Promos" set.

In the "CDI Promos.txt" file we can see a list of the cards in the set. Add a new entry at the end of the list for our new card:

```
78 R Circle of Flame @James Paick
79 U Stormblood Berserker @Greg Staples
80 R Dungrove Elder @Scott Chou
81 R Goblin Card Guide @Forge Team
```

The first number is the collector number for the card in the set. This must be unique.
The next field is the card's rarity in the set - we've made our card a rare.
Next is the card's name and finally the name of the artist for the cards artwork.

Speaking of which - our card doesn't have any artwork. Let's fix that. 

4. Add Card Image

Open your Card Images folder. Find the code for the set you added your new card to and open the corrosponding subfolder. For us this will be:

> C:/Users/<your username>/Application Data/Local/Forge/Cache/pics/cards/PDCI

Add your card image in the folder. Ther are various online tools to create custom cards. For the purposes of the guide you can use this image:

<img src="https://github.com/user-attachments/assets/55363e68-0232-42e2-a1f7-8971686119e6" width="250"/>

5. Add a Triggered Ability

So we've created a creature card and added it to the game. However our creature is a little... boring, so let's update it to have a triggered ability. 

"Whenever Goblin Card Guide deals damage to an opponent, draw a card."

Open the txt file again:
> goblin_card_guide.txt

And update the contents to the following:

```
Name:Goblin Card Guide
ManaCost:1 R
Types:Creature Goblin
PT:2/2
K:Haste
T:Mode$ DamageDone | ValidSource$ Card.Self | ValidTarget$ Opponent | TriggerZones$ Battlefield | Execute$ TrigDraw | TriggerDescription$ Whenever CARDNAME deals damage to an opponent, draw a card.
SVar:TrigDraw:DB$ Draw | Defined$ You | NumCards$ 1
Oracle:Haste
\nWhenever Goblin Card Guide deals damage to an opponent, draw a card.
```

Let's take a look at our changes:
 - T:Mode$ DamageDone - Trigger when Damage is Done.
 - ValidSource$ Card.Self - The source of the trigger. "Self" means our our card must be the source of the damage for the trigger to occur.
 - ValidTarget$ Opponent - The target of the trigger. "Oppenent" means our oppenent must receive damage for the trigger to occur.
 - TriggerZones$ Battlefield - Means the card must be on the battlefield for the trigger to occur.
 - Execute$ TrigDraw - What happens then the trigger occurs - in this case trigger a Draw effect.
 - SVar:TrigDraw:DB$ Draw - A String Variable used by the script. This one takes the Triggered Draw (TrigDraw) and tells the script to draw a card. "DB$ Draw" means "Drawback" "Draw".* 
 - Defined$ You - Who is drawing the card(s), in this case the controller of the card.
 - NumCards$ 1 - Draw 1 card. This could be changed to any number to trigger drawing that many card. 
 - TriggerDescription$ - The description of the trigger.

*"Drawback" is a connotation that has since been replaced with "SubAbility".
The original connotation differentiated between AB (Ability), SP (Spell) and DB (Drawback).
AP and SP require costs, whereas Drawback do not. The card scripts still use the "DB$" connotation.

6. Updating our Image

Finally since we createda new effect on our card, we need to update the image. Card images are simply .jpg files and don't update or read from any scripts or gamefiles. 

I used the same online card creator to make the change to our card:

<img src="https://github.com/user-attachments/assets/9c1df095-7d94-4a1a-a30f-9d0f64f6da38" width="250"/>

Simply save and rename the image to "Goblin Card Guide.fullborder.jpg" then overwrite the previous file we used in:

> C:/Users/<your username>/Application Data/Roaming/Forge/custom/cards/g

7. Next Steps

You can check the [Abilities](AbilityFactory) and [Triggers](Triggers) documentation for more information on this topic. These documents are meant as a guide and are unlikely to contain information about every ability in the game.

The sinplest method for creating an effect on your card is to find another card that does the same thing and copying the ability. These can be found in your Forge folder:

>./Forge/res/cardsfolder/cardsfolder.zip

Unzipping this file will sllow you to search for any card in the containing subfolders. 
