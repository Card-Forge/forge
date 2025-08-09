Setting up your cube in Forge is relatively straightforward. Forge needs to know two things: the contents of your cube and the contents of a cube pack

1) Define the contents of your cube
For this you will need to add your cube list to the 'cube' folder, which can be found under the 'res' folder in your Forge installation directory.
Cube lists are defined in .dck files. You can either create a new one with a text editor, or copy an existing one and change its file name and contents. Regardless of your approach, you should end up with a file that roughly looks like this:

```
[metadata]
Name=Two Card Cube
[Main]
1 Abbot of Keral Keep|ORI
1 Abrade|HOU
```

**Don't change the first and third line**. On the second line you can put in the name of your cube after 'Name=', in this example the name of our cube is Two Card Cube. Note this name, as you will later need to refer to it.
Starting on the fourth line, you can list each card in your cube. Always start with the number of times a given card is included in your cube (important for those breaking singleton), then the name of the card, then a pipe (the '|' character), and finally the three letter set code.
Tip: An easy way to find the three letter code for a card is to look up that card on www.scryfall.com and search up the correct version of the card. The set code will appear at the top of the prints overview behind the full set name.
Note that this example cube contains just two cards. Adding more is simply a matter of following the same pattern and adding a line for each new cards.

Once you're done with your file, save it in the 'cube' folder. Use the value of the Name field as the filename and give it the .dck file extension. In this example, we would save the file as 'Two Card Cube.dck'.

2) Define the contents of your boosters
The next step is to define the draft file that instructs Forge how your boosters look. For this you will need to create a .draft file in the 'draft' folder, which can again be found under the 'res' folder in your Forge installation directory.
Note that the options I will describe here are somewhat limited. Unfortunately seeding boosters is not easily achieved. Let's again look at an example:

```
Name:The Two Card Cube
DeckFile:Two Card Cube
Singleton:True

Booster: 15 Any
NumPacks:3
```

The first line contains the name of the cube as it will be presented in Forge.
The second line contains should match the name field of the .dck file you just created.
The third line defines whether this is a singleton draft format or a regular set format. Note that for cubes breaking singleton, you still want to set this value to True, since you define which cards you break singleton on in the .dck file.

The fifth line defines how many cards appear in a booster. You can change the number, but do not touch the 'Any', that part instructs Forge to pull cards from the entirety of your cube list.
Finally, the sixth line defines the number of packs used when drafting your cube.

Once you're done with this file, save it in the 'draft' folder. You can again use the value in the name field, this time in conjunction with the file extension 'draft'. In this example, we would save the file as 'The Two Card Cube.draft'.

3) Backup both files
The next step is to backup both your .dck file and your .draft file. You'll be happy you did when Forge needs to be reinstalled from scratch for some reason.

4) Draft!
You've done it! Fire up Forge, and select Booster Draft in the left side menu (it's one of the options under Sanctioned Formats). Click the New Booster Draft Game button, select the option Custom Cube, click OK, choose the cube you just created (remember, the name presented here is the value for the field Name you entered in the .draft file), and click OK again. That's it, you're in. Happy drafting!