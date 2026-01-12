# An oral/written history of Forge

Here is a roughly written history of some notable points in Forge's history along with some Java 8 playable versions of the game linked for some of the older ones...

## 2007-04-26 - Forge introduced on https://mtgrares.blogspot.com/ by mtgrares. 

365 cards available. 

`The card set is the best of Magic, and includes such cards like Ancestral Recall, Juzam Djinn, Serendib Efreet, Flametongue Kavu, Man-o'-War, all the moxes, Kokusho, the Evening Star, Keiga, the Tide Star, as well as old favorites like Wrath of God, Psychic Blast, Serra Angel, and a few Planar Chaos cards like Pyrohemia, Serra Sphinx, Damnation.`

All of the blogposts are still available as of this writing (May 2023), but most of the links are defunct since they are pointing to since abandoned locations. 

## 2008-07/08 - Oldest known historic archive of Forge. 

850 cards available. Original white-gray Java Swing interface. **All** cards live in a cards.txt file. No card "scripting" exists yet.

## 2010-07 - Improved original interface

Still using cards.txt file. Beginnings of the "res" folder for resources to be used inside Forge.

## 2011-09 - Start transitioning from "vintage" Forge to "modern" Forge. 

Mostly old-style interface with new age loading screen. Initial card scripting is being used along with the cardsfolder structure. Although the individual cards scripts, contain lots of vestiges of the past. (SetInfo, End tags, Picture tags, even the original scripting format)

## 2012-02 - Desktop interface transitions

Supports over 10,000 cards. We start using a reconstructed UI that allows for better theming. And makes things feel less like an "Application" and more like a "Game"

## 2013-10 - ver 1.5.1 is the first with Commander

## 2014-05 - Android app is published via Maven

## 2015-04 - Network play is rudimentary but available

## 2018-04 - Focus on adding automation tooling

## 2021-12 - Adventure mode functional

## 2024-07 - _All non-Un set cards have been added to Forge_

## 2024-09 - Duskmouth Release 1.6.65 (**Last release that supports Java 1.8**)

Supports over 28,500 cards. 

### Some significant events that need to be dated

* Quest mode
* Enable all phases
* AI can cast instants
* Creation of the Ability Factory/Scripting 2.0 system
* Creation of costing system
* Allowance of more than one player

# Legends

## Recounting of "Forge 2.0" by @Agetian

My favorite "Forge legend" is probably the legend about Forge 2.0 (that never came to be, at least thus far). Forge 2.0 was something that was brought up multiple times in Forge history, different targets were set for the hypothetical "future Forge" that would be worthy of increasing the major version number to 2, including, but not limited to, network play support, better UI, better AI (such as Minimax etc.), 100% support for standard cards, and more. Quite amusingly, the absolute majority of those targets have been met since, and even far surpassed, what with the addition of the mobile port, Planar Conquest, Adventure mode, and much more, but Forge is still using the 1.x version scheme it's been using since way back, hehe. Originally, Forge 2.0 was brought up my MtgRares (including a few times on his blog), but the original developer quit the active development before reaching his 2.0 targets, sadly. Thus, at the moment "Forge 2.0" represents a certain hypothetical, future version of Forge that's significantly and unconditionally better than what we currently have, whatever it may be.

The alternative variation of this historical legend is the legend about "Forge 2" - something that was also brought up a couple times in Forge history, mostly coming, unfortunately, from single developers. The hypothetical "Forge 2" was the complete, from-the-ground-up rewrite of Forge that would solve the many poor design choices that are difficult to solve through refactoring, and would thus pave the way for better UI/UX and AI implementations, make the game more streamlined and easier to code, among other things. Sadly, "Forge 2", just as well as "Forge 2.0", hasn't come to be, since the task to recreate all the game mechanics essentially from scratch, mostly as a single developer effort, has proven to be insurmountable.

## Original Adventure Mode

https://www.slightlymagic.net/forum/viewtopic.php?f=52&t=2907

For any of you curious about what the first attempt at making a Shandalar-like Adventure mode in Forge looked like (a development effort from 2010-2011), here are a few screenshots posted by the original developer who left circa 2011 or 2012. The project was abandoned, as far as I know. Pretty sure I had a test version somewhere in my backups (where you could walk around a mostly empty World Map and maybe start a battle or something, I don't remember the details), but I can't find it anymore.

Interestingly, the above-mentioned thread refers to the proposed game mode as "Adventure Mode" a few times (closer to the end of the thread), something that we actually currently have and actively develop 👍
The interesting aspect of that development was the day/night cycle, I don't know why but I remember I thought that it looked quite atmospheric back in the day

## Abe Sergeant writes about Forge

2009-09-11 - Here is the article on star city. 

https://articles.starcitygames.com/articles/the-kitchen-table-302-an-mtg-forge-quest/

And here is it in archive.org in case that gets taken down

https://web.archive.org/web/20210707215155/https://articles.starcitygames.com/articles/the-kitchen-table-302-an-mtg-forge-quest/

> MTG Forge comes with a Quest Mode. In Quest Mode, you begin with a random selection of cards, and have to build a 60-card deck. Then you play against decks by various computer opponents, and as you win, you get more cards, and the difficulty of your opponents increases.<br />
> Quest is the most fun I’ve had playing Magic in a year.

# Major disruptions

One interesting thing about Forge is the way it grew. Much of the first few years was solely on the back of an Amateur software engineer called "MTG Rares". Soon enough "Dennis Bergkamp" came along and was doing a bunch more development. New Software Engineers joined the ranks off and on. From there on people would jump in, help for a handful of years and get too busy with life, or stop really playing magic or whatever. A handful of us have been around the longhaul, but not too many. 

## Sourceforge SVN (2007-2008)
 Original location. Moved when the name changed.
## Google Code SVN (2008-2011)
 Google code was shutting down "soon" so we used it as an excuse to find a new home
## Bitbucket/GIT (2011-2011)
 This one lasted barely a few weeks. It was hated pretty universally, even by the people who suggested it.
## Slightly Magic SVN (2011-2017)
 The nice folks at Slightly Magic hosted an SVN for us.
## Hosted Gitlab (2017?-2021)
 One of the devs was comfortable in Git/Gitlab and moved everything over. Took a few attempts to get there but we made it. 
## Github (2021-Present)
 Conversion from Git to Git was a lot easier.

## Jendave's modularization (2011)
The initial modularization attempt pulled everything from living under forge-gui/ module and built out some of the Maven structure. 

## Maxmtg's modularization (2013?)
Took this the next step further massively reorganizing the codebase. It caused major issues and took a few months to get resolved.

## Hanmac's modularization (2017?)
This was a smaller modularization mostly within certains areas that ultimately was positive, but led to some headaches during the process.
