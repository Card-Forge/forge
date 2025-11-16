# Custom music for adventure mode

In the `res/adventure/common/music` folder resides all the music that is played in various parts of adventure mode:

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
 
The game will play a track from any of the above folders at random. The tracks can be any 
file name with a `.mp3`, `.wav` or `.m4a` extension.

NOTE: On Steam Deck, if you added Forge Adventure mode to Steam Deck gaming mode, the game may
have issues playing tracks that have non-ascii characters in its name. A current
workaround is to rename such tracks with accented characters to their ascii equivalents (eg. `é` to `e`)
and such tracks will play again. Reference: https://github.com/Card-Forge/forge/issues/8290

To customize the music in adventure mode, simply [add to -OR- replace] the existing music track files in the folders referenced above.

# Custom music for regular Forge

In the `res/music` folder resides all the music that is played in regular Forge if you have music enabled.

 * `match` - Tracks that are played inside a game
 * `menus` - Tracks that are played outside of any game (ie. menus)

All the same playback rules apply. The process for customizing the music here is the same as adventure mode.