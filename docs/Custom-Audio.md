# Custom Music

Custom music and sound sets can be added to your `%appdata%/Forge/custom/music/` directory. You may need to create this
directory yourself. Within that folder, create a folder with the name of your music set. And then within _that_ folder,
create one or more folders with the names of the playlists below. Custom tracks should be added to those folders.

The playlists Forge uses are:
 * `match` - Tracks that are played inside a game
 * `menus` - Tracks that are played outside of any game (ie. menus)

The resulting directory structure should look something like: 
```
%appdata%/Forge/custom/music/My Music Set/match/My Track 1.mp3
%appdata%/Forge/custom/music/My Music Set/match/My Track 2.mp3
%appdata%/Forge/custom/music/My Music Set/menus/My Menu Track 1.mp3
```

Finally, select your music set in the in-game _Options_ or _Preferences_ menu.

The game will play a track from your playlist at random. The tracks can use any file name, and the music set can
use any folder name. Valid formats may vary depending on platform, but generally `.mp3`, `.wav`, and `.ogg` are the
most reliable options.

# Custom Sound Effects

Custom SFX can be added using a similar process to the above, and using the `%appdata%/Forge/custom/sound/` directory.
Create a folder with the name of your sound set, and place sound files inside it. Sounds must use the same file names
as the ones in the game's `Forge/res/sound/` directory. The result might look like:
```
%appdata%/Forge/custom/sound/My Sound Set/creature.mp3
%appdata%/Forge/custom/sound/My Sound Set/draw.wav
```

File extensions and formats can can change, but the names must match for Forge to recognize your sound file. Supported
audio formats are the same as the ones Music files use.

Select your sound set in the in game _Options_ or _Preferences_ menu.

# Custom Audio in Adventure Mode

Adventure audio can be overridden with custom files by following the above process. The only difference is that instead
of using a custom name for your music or sound set, you name it after the plane of your adventure. If you are playing
Shandalar for instance, the resulting structure might look like this:

```
%appdata%/Forge/custom/music/Shandalar/match/My Track 1.mp3
%appdata%/Forge/custom/music/Shandalar/town/Town Track 1.mp3
%appdata%/Forge/custom/music/Shandalar/red/Red Track 1.mp3
```

Adventure Mode uses additional playlists beyond the ones listed above. Those are:
 * `black` - Tracks that are played in the black magic biome in the overworld
 * `blue` - Tracks that are played in the blue magic biome in the overworld
 * `boss` - Tracks that are played in a boss battle
 * `castle` - Tracks that are played inside biome boss dungeons
 * `cave` - Tracks that are played in any dungeon that is not a biome boss dungeon
 * `colorless` - Tracks that are played in the colorless magic (wastes) biome in the overworld
 * `green` - Tracks that are played in the green magic biome in the overworld
 * `match` - Tracks that are played during a non-boss battle
 * `menus` - Tracks that are played in the opening menu of adventure mode
 * `red` - Tracks that are played in the red magic biome in the overworld
 * `town` - Tracks that are played when you are in any town
 * `white` - Tracks that are played in the white magic biome in the overworld

NOTE: On Steam Deck, if you added Forge Adventure mode to Steam Deck gaming mode, the game may
have issues playing tracks that have non-ascii characters in its name. A current
workaround is to rename such tracks with accented characters to their ascii equivalents (eg. `é` to `e`)
and such tracks will play again. Reference: https://github.com/Card-Forge/forge/issues/8290