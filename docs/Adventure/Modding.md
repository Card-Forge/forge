## Modding and Development

With the addition of new planes in Adventure Mode, comes a framework to allow greater customization and even expansion of the available planes. The details behind the framework and each category can be found below. As well as a new section being built with some basic information on each piece of plane construction and modification. The sections will also define some Best Practices to be maintained.

### Getting Started

Modding Adventure mode comes in many fashions. From making small changes to a core plane, such as changing the music that plays on the overworld. To something as complex as an entirely new plane. Regardless of your intended goals, the first thing to do is set-up a back-up method. As any changes you haven't had incorporated into the main game, will potentially be lost on each update of Forge. Since this is a Git project, the method that will be recommended by this wiki, and referenced for the future, is simply to create your own git branch, and use a local repository to control all your files. It is also recommended to follow the directions to [set-up IntelliJ](https://github.com/Card-Forge/forge/wiki/IntelliJ-setup), to manage your local files. (Again, this is the method that will be referenced elsewhere in this wiki.) 

### Battle Backgrounds

Adventure planes can provide one or more battle backgrounds in `<plane>/skin/battle_backgrounds/<category>/`. Supported categories are `forest`, `swamp`, `mountain`, `island`, `plains`, `waste`, `common`, `cave`, `dungeon`, and `castle`. Add any number of `.jpg`, `.jpeg`, or `.png` files directly to a category folder. Forge selects one at random for each battle and avoids repeating the previous image when alternatives are available.

Planes can keep these images outside the Forge distribution by declaring them in `<plane>/skin/battle-backgrounds.txt`. Each line contains a path relative to `battle_backgrounds/`, followed by its HTTPS URL:

`forest/forest_01.jpg https://example.com/forest/forest_01.jpg`

The URL must point directly to a JPG, JPEG, or PNG response with an image content type. Lines beginning with `#` are comments. Forge automatically downloads missing files to the selected plane's cache. The list is authoritative, so cached images removed from it are also removed. This download does not require a user setting. Local plane images and the existing Adventure backgrounds remain available as fallbacks when a download is incomplete or unavailable.

Point-of-interest categories can also use biome subfolders named `white`, `blue`, `black`, `red`, `green`, or `colorless`, for example `battle_backgrounds/castle/red/`. The first non-empty folder in this order is used:

1. A background set on the individual enemy in a TMX map.
2. A background set on the enemy archetype.
3. A background set on the point of interest.
4. The point-of-interest category and biome folder.
5. The generic category folder.

Set `battleBackground` to a folder path relative to `battle_backgrounds/`. The same path can be used in `battle-backgrounds.txt`, so local and downloaded backgrounds have identical configuration:

```json
{ "name": "Goblin Warlord", "battleBackground": "enemy/goblin-warlord" }
```

```json
{ "name": "Fallen Empires Castle", "battleBackground": "poi/fallen-empires-castle" }
```

An individual enemy object in a TMX map can override its archetype with:

```xml
<property name="battleBackground" value="encounter/ashnod-boss"/>
```

For each folder above, Forge checks the selected plane's cache, the current plane, and then the common Adventure assets. Folder values are explicit and can contain any number of nested directories.

Images are not combined between fallback folders. If no rotation folder contains an image, Forge continues to use the existing single-image override, common Adventure image, current skin, or default skin. Existing plane configurations therefore do not need to change.

### Tools

The following additional tools can also be very useful, or even mandatory, to have for your mod, depending on what all you want to do in your mod/addition.

**[Tiled](https://www.mapeditor.org/)**: If you want to modify or create any maps; (caves, dungeons, towns, etc.) Forge utilizes Tiled. If you want to learn more on this topic, you can find it in the [Create new Maps](https://github.com/Card-Forge/forge/wiki/Create-new-Maps) section of the Wiki.

**[GIMP](https://www.gimp.org/)**: Many of the art files such as the tilesets used in Tiled, are made in GIMP. (A free graphical manipulation program, similar to Photoshop.) While not required to work in Forge's files, if you want to create your own art assets, this is the program that will be used for examples in this Wiki.
