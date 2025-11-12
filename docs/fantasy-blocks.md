# How to customize your Forge Sealed Deck game with fantasy blocks

You can define your own dream block for sealed deck games by choosing the exact sets you want to include in the game - and even easily spice up your sealed deck experience with special booster packs that generate cards based on several different sets, the full cardpool or your customized cube.

To do this, you need to modify your "fantasyblocks.txt" file with your text editor of choice. You will find the file in the res/blockdata folder. Notice that there is also a "blocks.txt" file in this folder - it contains the predefined official MtG blocks (better leave it alone). The "fantasyblocks.txt" file currently includes some sample blocks we have built. You can delete them all, replace them with your own blocks, or just add new blocks after them. You can have as many fantasy blocks as you like - just remember to increment the index number every time you add a new one!

When you are finished, just save the fantasyblocks.txt file and relaunch Forge. If have made errors when editing the fantasyblocks.txt file, you may get error messages in your block now and Forge may fail to launch. (If this happens, check your logfile to see what went wrong.)

# What is a block file made of?

A block definition file (fantasyblocks.txt or blocks.txt) contains block definitions that are parsed when the game launches and then used internally when the game is running. One block definition per line. Let's look at a sample line:

`Index:0|Set0:LEA|Name:Alpha|DraftPacks:3|LandSetCode:LEA|SealedPacks:6`

This is the first line in the official blocks.txt file. It contains several parameters and values that are separated with |'s. The format for a parameter value/pair is "Parameter:value". Here's what the parameters mean:

Index: The internal index number of the block. This needs to be unique. When you add a new block (row), just check the last number and add 1.
Set0: This is a 3-letter code for MtG set. "LEA" value means that one of the sets included in this block is Alpha. A block can contain as many as 9 different sets (given as parameters Set0:xxx|Set1:yyy| etc. until Set8:zzz). Hint: If you don't know the 3-letter code for a set you want, you can look it up in the setdata.txt file in the same directory (res/blockdata).
Name: This is the name that is displayed in the block selection dialog box.
DraftPacks: How many boosters you get when Drafting this block.
LandSetCode: Which edition basic lands are used when playing Draft/Sealed Deck games with this block. Again, if necessary, you can look up the set code in the setdata.txt file. Note that if your block contains a core set, it is usually a good idea to use that edition's code here or may get basic lands from different editions and they will not show up properly in the deck editor.
SealedPacks: How many boosters you get when playing Sealed Deck games with this block. Note that the current version of Forge supports 4-9 booster pack SD games but if you give the players (for example) only 4 booster packs and your block contains more than four sets, you will be able to choose from the first four sets only. Thus, this number should always be at least as great as the number of different sets in the block if you want to be able to use them all. But it can, of course, be higher than number of sets in a sealed deck game. In this particular example, all players get 6 booster packs but, since the only set included in the block is Alpha, all 6 will be Alpha boosters.

Let's try another fairly straightforward example, this time from fantasyblocks.txt:

`Index:25|Set0:2ED|Set1:ATQ|Set2:ARN|Set3:DRK|Set4:LEG|Set5:FEM|Set6:ICE|Set7:HML|Set8:ALL|Name:(9) MtG Encyclopedia|DraftPacks:3|LandSetCode:2ED|SealedPacks:9`

Yes: this is fantasy block #25 (actually 26th block, since the first block is #0, not #1). It contains 9 different sets, basically everything from the birth of the game to Alliances. Hence, the name "MtG Encyclopedia" - theoretically, any card from the old printed book Magic the Gathering: Official Encyclopedia ("godbook") would be included here (except the promo cards). (The "(9)" in front of the name is simply a clue to give you an idea how many sets there are in the block.) Unlimited lands are used here. Draft players get only 3 different booster packs, so this block is poorly suited to Draft games (they would only get Unlimited, Arabian Nights and Antiquities boosters from this blocks). In Sealed Deck games, however, all you would get 9 different boosters if you select this block - and have the option of selecting starter packs instead of booster packs for Unlimited and/or Ice Age.

But the fun doesn't stop here...

# MetaSets: What are they, why should I care, and how do I use them?

Even 9 different sets may seem limiting sometimes. Or maybe you would have booster based your special, customized fantasy card list? MetaSets are have been added to allow you do exactly that: create a special booster pack that wouldn't be possible in real life.

Here is a MetaSet sample from fantasyblocks.txt:

`Index:26|Meta1:CUBE/ArabianExtended/ARAB|Meta2:META/ISD,DKA,AVR,M13/M13-ISD|Meta3:FULL/*/*|Name:(3) Metaset Sample|DraftPacks:3|LandSetCode:2ED|SealedPacks:6`

To define a MetaSet, you use the Meta0...Meta8 parameters, just like you would use the Set0...Set8 parameters. (Notice that, illogically, here I have accidentally started with Meta1 and not Meta0, shame on me! But the number doesn't really matter that much, it's the amount of different sets/MetaSets in a block that matters.)

The main difference lies in the values that a Meta(x) parameter can have. First we have:

`Meta1:CUBE/ArabianExtended/ARAB`

A MetaSet always needs 3 values, separated with slashes. The first value is the MetaSet type, in this case, a CUBE. The first value must be CUBE, FULL, or META. Any other value will cause an error.
The other two values are data and display name (they behave differently for the different MetaSet types). For a cube, the second value must be the cube name. If Forge cannot find this cube (defined in res/sealed/), it will give you an error when you try to choose this block. The last name is simply a visual name that is displayed when you choose the set distribution for you sealed deck game. For a cube, a "*C:" prefix is always automatically appended.

Next we have:

`Meta2:META/ISD,DKA,AVR,M13/M13-ISD`

This is a genuine 'meta' MetaSet - i.e., a set of sets, a block within a block. The first value is the type (META) and the last value is the display name (M13-ISD), just like in the cube example. For meta-type MetaSets, a "*B:" prefix is added in indicate that it is really a sub-block within the block.
The second value is the interesting part. It is a comma-separated list of sets that are combined to make the cardpool for this booster, in this case the whole Innistrad block and M13 core set. (Again, look up the codes in setdata.txt if you don't know them.) And here's the cool part: you can list any number of sets you like in the comma-separated list!

The final MetaSet in this sample block is:

`Meta3:FULL/*/*`

This one is pretty straightforward. It simply indicates that the boosters for this 'set' are based on the full cardpool available in Forge. While values 2 and 3 must be supplied (otherwise the line won't parse correctly), they are not important for the full cardpool MetaSet type. Value 2 is not used at all, and its display name will always be "*FULL".

So, effectively, the above "Metaset Sample" block can generate boosters based on the "Arabian Extended" cube, a sub-block consisting of the Innistrad block + M13, and/or full cardpool.

Finally, note that you can mix-and-match regular sets and MetaSets in a block - just be sure to count both when you set the SealedPacks parameter. For example, if your block contains regular sets Set0, Set1, and Set2 (3 sets), and MetaSets Meta1 and Meta2, the value will need to be at least 5. (The regular 6 would work nicely, too.)

# MetaSets: The Next Level

`Meta-Choose(S(RTR Prerelease Azorius Guild)Azorius guild;S(RTR Prerelease Selesnya Guild)Selesnya guild;S(RTR Prerelease Izzet Guild)Izzet guild;S(RTR Prerelease Rakdos Guild)Rakdos guild;S(RTR Prerelease Golgari Guild)Golgari guild)Guild`

Forge has these MetaSet types:

* Full("F") - All cards
* Cube("C") - Cube
* JoinedSet("J") - Joined set.. ex: J(ICE ALL HML CSP)Ice_Age_Block_Extended
* Choose("Select") - Choose from a list of nested metasets
* Random("Any") - Randomly select one of nested metasets
* Combo("All") - Combined booster means all nested sets will be selected
* Booster("B") - a common booster, associated with card edition... ex: B(DKA)
* SpecialBooster("S") - Special booster defined to support special events, that is not linked to any edition, see note below.
* Pack("T") - Tournament pack or Starter, valid only for editions where it was avaliable

* You may use either name or a shorter alias to denote a meta set. They are case insensitive now
* There is a new meta type "SpecialBooster" (added during 1.3.16 development), it's used to refer to special boosters declared in res\blockdata\boosters-special.txt . These boosters are used to hold RTR block sealed events, and may be used for MBS faction booster generation (if anyone would like to build a themed sealed game)
