This is a list of basic troubleshooting questions that come up for most new players, before running to the discord about your issue, please review this FAQ for some of the more common issues.

### Check the FAQ in Discord 

https://discord.com/channels/267367946135928833/1095026912927154176

### Search the help posts in Discord

https://discord.com/channels/267367946135928833/1047001034788196452

### Write a post in help section of Discord

https://discord.com/channels/267367946135928833/1047001034788196452

Note: For now, please also check [this](https://www.slightlymagic.net/forum/viewtopic.php?f=26&t=11825) forum topic for some additional information.

# General

### How do I download content?

Forge has content downloaders within the app itself, you can use those tools to update the graphics assets. More information about card and token image assets can be found here. [Card Images, Downloading](Card-Images#downloading)



* If you have an older Android device for increased performance or to save bandwidth it might be a good idea to use lower resolution images instead: https://www.slightlymagic.net/forum/viewtopic.php?f=15&t=29104

### How do I extract Forge?

* Forge uses a .tar.bz2 format for archiving. Depending on your operating system, different utilities can be used to untar the archive. 
  * If you use Windows, you may want to try 7-Zip (http://www.7-zip.org/download.html).

### I think I found a bug in Forge. What do I do?

*Most users, who are running beta versions of Forge, should continue to use these instructions. As for alpha testers, these instructions have yet to be made congruent with the latest automatic bug reporting from within Forge.*

Bug reports from users are the lifeblood of Forge. Please keep in mind that "beta" releases *are* test releases. Forge is constantly evolving, so we do not yet have "stable" or "production" releases. Because of the pace at which new cards are added to the multiverse by external forces, this will be the  norm for some time. We do not expect everything to work 100%. We have a small number of developers and a handful of slightly less technical people actively improving the game. We simply cannot devote the resources to test every single card, much less the nearly infinite ways the cards can interact.

For starters, please take note of (1) what you had in play, (2) what your opponent had in play and (3) what you were doing when the error occurred. If you get a Crash Report from inside Forge, please save the data to a file. This information is very important when reporting a problem. Don't worry if you didn't think of that right away, until your next start, the "Forge.log" in the game directory will also provide that information.

If you did not get a Crash Report, but you have experienced a problem in how Forge handled one or more cards or game rules, *please read the cards (and the Oracle rulings) carefully* to make sure you understand how they work. You may be surprised to find that Forge is actually enforcing the rules 
correctly.

Because duplicate bug reports use up our limited resources, please research your bug with the **Search** box on Forge's [issue tracker](https://git.cardforge.org/core-developers/forge/-/issues) to see if your bug has already been reported there. For Crash Reports, use key words from the second paragraph of the Crash Report.

* If you find a matching issue, examine it to see if you have anything new to contribute. For example, a different way of reproducing a problem can sometimes be helpful. If the issue was posted to the forum, you may post your additional information there.

* If you find nothing, please try to reproduce the problem and take notes. If we can use your notes to reproduce the bug for ourselves, it is *much* easier to fix!

* If you're unsure, you can also post on one of the support channels of the discord. In case you do not get a timely response, please submit a new issue anyway to make sure it doesn't get lost.

### I have an idea to make Forge better. What do I do?

Follow the directions in [Bug Reports](Frequently-Asked-Questions#i-think-i-found-a-bug-in-forge-what-do-i-do), keeping in mind that you are not reporting a bug, but rather a **Feature Request**.

# Development

### I want to help develop Forge. How do I get started?

Forge is written in Java, so knowledge in that language (or similar Object Oriented languages like C++ or C\#) is very helpful. However, it is possible to learn the grammar for writing the data objects of cards without programming experience.

A development environment such as [IntelliJ](https://www.jetbrains.com/idea) is beneficial, as it helps writing, compiling and testing your changes.

Thanks to the nature of how cards are implemented, you can also contribute these as small plain text files. This is especially helpful during a preview season, when there are a lot of new cards in the backlog. This is mostly coordinated in #card-scripting on the Discord (and the pins there).

To obtain the source code of Forge, read our [Development Guide]((SM-autoconverted)--how-to-get-started-developing-forge).

### My system is all setup to help. What now?

Take a look through the /res/cardsfolder folder. This is where all the card data lives. If you know of cards that are missing from Forge, see if there are similar cards that already exist.

# Gameplay

### Where do I use Flashback or a similar ability that is in an External area?

Click on the Lightning Bolt icon in the player panel. Since cards with External Activations aren't as clear to activate, we created this shortcut for this specific purpose.

### How do I target a player?

Just click on the player's Avatar in the Player Panel when prompted to select a Player as a target.

### Where did my mana go?

If you have an effect that generated you some mana, and you don't know where it is. Check out the Player Panel. There are 6 different mana subpools one for each color/colorless that should have it. If you accidentally tapped your mana before your Main Phase, your mana is gone. Sorry, we don't have a way at this time to revert these actions. In general, I'd say it's easier/better to start casting a spell first, then activate your mana so this doesn't happen.

# Quest Mode

### What is the difference between Fantasy Quest and Normal Quest?

In Normal Quest, you start with 20 life and only have access to the Card Shop. In Fantasy Quest, you start at 15 life and gain additional access to the Bazaar which allows you to buy things like extra life points, Pets, Plants and more.

### Sealed Deck Mode

[HOW-TO: Customize your Sealed Deck games with fantasy blocks](https://www.slightlymagic.net/forum/viewtopic.php?f=26&t=8164)
