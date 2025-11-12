This is a tutorial to start creating your own custom set, or implementing an existing one. We'll take you step by step into implementing a few cards from [MSEM Champions](https://msem-instigator.herokuapp.com/set/MPS_MSE). This is a basic guide to help you get started.

**Note:** This tutorial is currently for **Windows only**.

## Where do the files go?

### Non-image files

Everything but your cards images go into `%appdata%/Forge/custom`. You will need to put the files in the correct directories in order for the game to load them correctly.

The important folders are the following (you can create them if they don't exist):

* **cards**: Your card rules (logic) go inside this folder (.txt). I suggest you create subfolders inside it to keep everything clean.
* **editions**: Your editions (sets) definition files (.txt) go inside this folder.
* **tokens**: Your tokens definition files (.txt) go inside this folder.

### Image files

Your card and token images go into `%localappdata%/Forge/Cache/pics`.

The important folders are the following:

* **cards**: This is where you will put your card images. You'll want to create a folder with the Code of your edition. The images are named using this convention `Card Name.full.jpg`. If you have multiple art for the same card (something like basic lands, or maybe alternate art of the same card), then you can name them `Card Name1.full.jpg`, `Card Name2.full.jpg`, and so forth.
* **tokens**: Same as the cards folder, your tokens will go inside this folder. The naming convention is `token_script_name.jpg`. So if your token script name is `b_5_5_golem_trample`, then you can put your token image inside your edition folder named `b_5_5_golem_trample.jpg`. If there is a collector number, append it at the beginning, such as `1_b_5_5_golem_trample.jpg`

## Creating your edition definition file

As mentioned in the opening section, we'll be partially implementing the **MSEM Champions** set. Let's create a new text file (.txt) inside `%appdata%/Forge/custom/editions`. Let's name it `MSEM Champions.txt`.

> Note: The file's name don't matter, but it'll be easier to find it if you ever need to edit anything.

Let's paste the following inside it:

```
[metadata]
Code=MSEM_CHAMPIONS
Name=MSEM Champions
Date=2017-10-01
Type=Custom

[cards]
7 M Master Chef
33 M Golden Touch
34 M Avatar of Basat
35 M Exeunt
62 M Unearth
78 M Fox of the Orange Orchard
78★ S Fox of the Orange Orchard
107 M Inked Summoner
107★ S Inked Summoner
130 M Plains
131 M Island
132 M Swamp
133 M Mountain
134 M Forest

[tokens]
b_1_1_bird_flying
b_3_3_cat_deathtouch
b_5_5_golem_trample

[CreatureTypes]
Artist:Artists
```

Let's break it down.

```
[metadata]
Code=MSEM_CHAMPIONS
Name=MSEM Champions
Date=2017-10-01
Type=Custom
```

The **[metadata]** section contains the information about your edition.

* **Code** is an **unique** identifier for your edition.
* **Name** is the name of your edition.
* **Date** is the date the set was first released/created.
* **Type** should be `Custom` as Forge do things differently for them, including but not limited to skipping the automatic download of the images.

```
[cards]
7 M Master Chef
34 M Avatar of Basat
35 M Exeunt
62 M Unearth
78 M Fox of the Orange Orchard
78★ S Fox of the Orange Orchard
107 M Inked Summoner
107★ S Inked Summoner
130 L Plains
131 L Island
132 L Swamp
133 L Mountain
134 L Forest
```

The **[cards]** section contains the cards of your edition. Any card appearing under this section has a chance to appear as a reward and in shops.

Each line is as follow: `CollectorNumber Rarity CardName @ArtistName`.

* The collector number should be unique in a given set. You can see some numbers have a ★ next to their name. In this case it denotes an alternate art of a card, but it could also have been a different number altogether.
  > Note: While it generally doesn't matter what collector number you use, avoid using the collector number F followed by only digits (ie. `F001`) as it represents a "Funny" card inside a normal edition. (Think of Funny cards as Un- sets cards)
* The rarity is a one letter representation. It can be L (Basic Land), C (Common), U (Uncommon), R (Rare), M (Mythic Rare), S (Special).
* The card name is self-explanatory. It should match the name of the corresponding card rule. (More of that later)
* The artist is optional, but it should be `@Artist Name` if present. We'll omit it in this tutorial.

> Note: You can put the cards in the list even if they aren't scripted yet. Forge will skip over them.

```text
[tokens]
1 b_1_1_bird_flying
2 b_3_3_cat_deathtouch
3 b_5_5_golem_trample
```

The **[tokens]** section is optional, and only needed if you want to use specific token images in this set. They should be named using the name of their token script. `b_1_1_bird_flying` means it is a black 1/1 bird with flying. More on that later.

If you load the game with just file, you'll be able to see that Master Chef, Unearth and the basic lands can already be found in game. That's because they share a name with existing Magic the Gathering cards. **However**, [Master Chef](https://msem-instigator.herokuapp.com/card?q=Master+Chef) from MSEM and [Master Chef](https://scryfall.com/card/clb/241/master-chef) from MTG are two different cards! You must ensure that your custom cards do not have the same name as an existing one, unless you just want it to be another print, just like [Unearth](https://msem-instigator.herokuapp.com/card/CHAMPIONS/62/Unearth) and the basic lands in this example.

Let's comment out Master Chef to avoid a name conflict with an existing MTG card:

```text
[cards]
#7 M Master Chef
33 M Golden Touch
...
```

Save your file, and let's move onto another step.

> If there is a conflict, you can add something in its name for differenciate it, such as a set tag (ie. `Master Chef (MSEM)`).

## Scripting your first cards

Now, you might remember than Unearth was an existing MTG card so we do not need to create a custom card rule for it. Let's create a few others.
> This tutorial will not teach you to script your cards, and they might not be perfect. Please check out [Creating a Custom Card](https://github.com/Card-Forge/forge/wiki/Creating-a-Custom-Card) if you want more info, or look at the existing cards to learn more complex scripting.

Let's create the following files:

avatar_of_basat.txt

```text
Name:Avatar of Basat
ManaCost:R
Types:Creature Avatar
PT:2/1
S:Mode$ CantBlock | ValidCard$ Card.Self | Description$ CARDNAME can't block.
K:Menace
Oracle:Menace\nAvatar of Basat can't block.
```

exhunt.txt

```text
Name:Exeunt
ManaCost:B
Types:Instant
A:SP$ Sacrifice | SacValid$ Creature | Defined$ Player | SpellDescription$ Each player sacrifices a creature.
AI:RemoveDeck:All
Oracle:Each player sacrifices a creature.
```

fox_of_the_orange_orchard.txt

```text
Name:Fox of the Orange Orchard
ManaCost:1 W
Types:Creature Fox Spirit
PT:3/1
Oracle:
```

inked_summoner.txt

```text
Name:Inked Summoner
ManaCost:1 B
Types:Creature Human Warlock Artist
PT:1/2
T:Mode$ Phase | Phase$ End of Turn | ValidPlayer$ You | TriggerZones$ Battlefield | Execute$ TrigBranch | CheckSVar$ X | SVarCompare$ GE2 | TriggerDescription$ At the beginning of your end step, if you lost 2 or more life this turn, create a 1/1 black Bird creature token with flying. If you lost 4 or more life this turn, instead create a 3/3 black Cat creature token with deathtouch. If you lost 6 or more life this turn, instead create a 5/5 black Golem creature token with trample.
SVar:TrigBranch:DB$ Branch | BranchConditionSVar$ Y | TrueSubAbility$ TrigBranch2 | FalseSubAbility$ DBBird
SVar:TrigBranch2:DB$ Branch | BranchConditionSVar$ Z | TrueSubAbility$ DBGolem | FalseSubAbility$ DBCat
SVar:DBBird:DB$ Token | TokenScript$ b_1_1_bird_flying | TokenAmount$ 1
SVar:DBCat:DB$ Token | TokenScript$ b_3_3_cat_deathtouch | TokenAmount$ 1
SVar:DBGolem:DB$ Token | TokenScript$ b_5_5_golem_trample | TokenAmount$ 1
SVar:X:PlayerCountPropertyYou$LifeLostThisTurn
SVar:Y:Count$Compare X GE4.1.0
SVar:Z:Count$Compare X GE6.1.0
Oracle:At the beginning of your end step, if you lost 2 or more life this turn, create a 1/1 black Bird creature token with flying.\nIf you lost 4 or more life this turn, instead create a 3/3 black Cat creature token with deathtouch.\nIf you lost 6 or more life this turn, instead create a 5/5 black Golem creature token with trample.
```

If you load your game now, you should be able to find these cards you just scripted! You'll also notice that Inked Summoner is only listed as a Human Warlock, missing the Artist subtype. That's because Artist is not a real MTG subtype. You can add custom types directing inside the set definition file by following the sections found inside the `res/lists/TypeLists.txt` file. Duplicates will be ignored.

```text
[CreatureTypes]
Artist:Artists
```

Oh no! If you play with Inked Summoner now, you will crash when summoning a token. That's because they don't exist in MTG and we need to define them! Let's go onto the next step!

## Scripting custom tokens

Let's add the new tokens we need to make Inked Summoner work!
> Just like for card scripting, this tutorial will not teach you about scripting them.

b_1_1_bird.flying.txt

```text
Name:Bird Token
ManaCost:no cost
Colors:black
Types:Creature Bird
PT:1/1
K:Flying
Oracle:Flying
```

b_3_3_cat_deathtouch.txt

```text
Name:Cat Token
ManaCost:no cost
Colors:black
Types:Creature Cat
PT:3/3
K:Deathtouch
Oracle:Deathtouch
```

b_5_5_golem_trample.txt

```text
Name:Golem Token
ManaCost:no cost
Colors:black
Types:Creature Golem
PT:5/5
K:Trample
Oracle:Trample
```

Great! Now Inked Summoner no longer make the game crash! Now let's add some images to spice it all up.

## Adding card and token images

You can find the card images for the MSEM Champions edition [here](https://msem-instigator.herokuapp.com/set/CHAMPIONS). Find the ones you need and save them inside `%appdata%/../Local/Forge/Cache/pics/cards/MSEM_CHAMPIONS` Remember the filename format should be `{cardname}.fullborder.jpg` if you only have one variant in your edition. If you have multiples, then it should be `{cardname}{number}.fullborder.jpg` (ie. `Fox of the Orange Orchard1.fullborder.jpg`, `Fox of the Orange Orchard2.fullborder.jpg`, etc). You can find the alternate images from [here](https://msem-instigator.herokuapp.com/set/MPS_MSE) if you want.

For the tokens, we can deposit them inside `%localappdata%/Forge/Cache/pics/tokens/MSEM_CHAMPIONS`. They should be named the same as their number + token script so `1_b_1_1_bird_flying.jpg`, `2_b_3_3_cat_deathtouch.jpg`, and so forth.

![b_1_1_bird_flying](https://github.com/user-attachments/assets/531583c1-3985-4744-858a-3a49fd12740a)
![b_3_3_cat_deathtouch](https://github.com/user-attachments/assets/15a24e62-be43-4c0c-aeac-0ddb38fca97a)
![b_5_5_golem_trample](https://github.com/user-attachments/assets/7ff44dc7-0284-48bd-9233-785ac79106f3)

You can now start your game again, and see that the art loads correctly now.

## Excursion: Card variants

There are currently multiple ways to specify a flavor name:
* Manually, by writing `Variant:Foo:FlavorName:Loret Ipsum` in the card script, and adding `${"variant": "Foo"}` to the end of the edition entry. You'll also want to add `Variant:Foo:Oracle:When Loret Ipsum enters...` if the flavor name would appear in the Oracle text, or if it would otherwise be changed.
* By lookup, again by using `Variant:Foo:FlavorName:Loret Ipsum` in the card script, but simply using "Loret Ipsum" as the name in the edition file.
* Automatically, by putting `${"flavorName": "Loret Ipsum"}` at the end of the edition entry.

The third method is the easiest, but besides a simple find/replace for the card name, it won't be able to make any changes to flavor the Oracle text, such as for ability words. They all function the same under the hood once the CardDB is loaded; the latter two are just shortcuts for the first.

## 🎉 Congratulations

You’ve just added your first custom set in Forge! There's still much more to explore — scripting advanced abilities, custom mechanics, and set structures — but you now have a solid foundation to build from.
