# About Forge's Artificial Intelligence

The AI is *not* "trained". It uses basic rules and can be easy to overcome knowing its weaknesses.

The AI is:
- Best with Aggro and midrange decks
- Poor to Ok in control decks
- Pretty bad for most combo decks

The logic is mostly based on heuristics and split between effect APIs and all other ingame decisions. Sometimes there is hardcoded logic for single cards but that's usually not a healthy approach though it can be more justifiable for highly iconic cards.  
Defining general concepts of smart play can help improve the win rate much easier, e.g. the AI will always attack with creatures that it has temporarily gained control of until end of turn in order not to miss the opportunity and thus waste the control effect.

If you want to train a model for the AI, please do. We would love to see something like that implemented in Forge.

# AI Matches from Command Line

The AI can battle itself in the command line, allowing the tests to be performed on headless servers or on computers that have poor graphic performance, and when you just don't need to see the match. This can be useful if you want to script testing of decks, test a large tournament, or just bash 100's of games out to see how well a deck performs.

Please understand, the AI is still the AI, and it's limitations exist even against itself. Games can lag and become almost unbearably long when the AI has a lot to think about, and you can't see what's on the table for it to play against. It's best if you set up the tournament and walk away, you can analyze logs later, results are printed at the end.

## Syntax 

`sim -d <deck1[.dck]> ... <deckX[.dck]> -D [path] -n [N] -f [F] -t [T] -p [P] -q`

In linux and mac, command line arguments are not currently passed through the sh script, please call `java -jar` manually, instead of the exe.
- `sim` - "Simulation Mode" forces Forge to not start the GUI and automatically runs the AI matches in command line. Enables all other switches for simulation mode.
- `-d <deck1[.dck]> ... <deckX[.dck]>` - Space separated list of deck files, in `-f` game type path. (For example; If `-f` is set to Commander, decks from `<userdata>/decks/commander/` will be searched. If `-f` is not set then default is `<userdata>/decks/constructed/`.) Names must use quote marks when they contain spaces.
  - `deck1.dck` - Literal deck file name, when the value has ".dck" extension.
  - `deck` - A meta deck name of a deck file.
- `-D [path]` - [path] is absolute directory path to load decks from. (Overrides path for `-d`.)
- `-n [N]` - [N] number of games, just flat test the AI multiple times. Default is 1.
- `-m [M]` - [M] number of matches, best of [M] matches. (Overrides -n) Recommended 1, 3, or 5. Default is 1.
- `-f [F]` - Runs [F] format of game. Default is "constructed" (other options may not work, list extracted from code)
  - `Commander`
  - `Oathbreaker`
  - `TinyLeaders`
  - `Brawl`
  - `MomirBasic`
  - `Vanguard`
  - `MoJhoSto`
- `-t [T]` - for Tournament Mode, [T] for type of tournament.
  - `Bracket` - See wikipedia for [Bracket Tournament](https://en.wikipedia.org/wiki/Bracket_(tournament))
  - `RoundRobin` - See wikipedia for [Round Robin Tournaments](https://en.wikipedia.org/wiki/Round-robin_tournament)
  - `Swiss` - See wikipedia for [Swiss Pairing Tournaments](https://en.wikipedia.org/wiki/Swiss-system_tournament)
- `-p [P]` - [P] number of players paired, only used in tournament mode. Default is 2.
- `-q` - Quiet Mode, only prints the result not the entire log.

## Examples
In linux and macos you must run forge by evoking java and calling the jar, currently command line parameters are not passed through the script. The forge jar filename is truncated in these examples from `forge-whatever-version-youre-on.jar` to `forge.jar`.

In Windows, if you use the EXE file as described below, the simulation runs in the background and output is sent to the forge log file only. If you want to have output to the console, please use the `java -jar` evocation of forge.

To simulate a basic three games of two decks (deck1 and deck2 must be meta deck names of decks in `<userdata>\decks\constructed\`):
- Windows/Linux/MacOS: `java -jar forge.jar sim -d deck1 deck2 -n 3`
- Windows: `.\forge.exe sim -d deck1 deck2 -n 3`

To simulate a single 3-player Commander game (deck1, deck2, and deck3 must be meta deck names of decks in `<userdata>\decks\commander\`):
- Windows/Linux/MacOS: `java -jar forge.jar sim -d deck1 deck2 deck3 -f commander`
- Windows: `.\forge.exe sim -d deck1 deck2 deck3 -f commander`

To simulate a round robin tournament; best of three, with all decks in a directory:
- Windows/Linux/MacOS: `java -jar forge.jar sim -D /path/to/DecksFolder/ -m 3 -t RoundRobin`
- Windows: `.\forge.exe sim -D C:\DecksFolder\ -m 3 -t RoundRobin`

To simulate a swiss tournament; best of three, all decks in a directory, 3 player pairings:
- Windows/Linux/MacOS: `java -jar forge.jar sim -D /path/to/DecksFolder/ -m 3 -t Swiss -p 3`
- Windows: `.\forge.exe sim -D C:\DecksFolder\ -m 3 -t Swiss -p 3`

***

Each game ends with an announcement of the winner, and the current status of the match. 
