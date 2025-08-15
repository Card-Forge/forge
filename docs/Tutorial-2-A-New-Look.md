## Tutorial 2: A New Look  (Creating your first map)

Okay, you've got a new plane, now let's make that plane our own. This tutorial is going to teach you how to make a very basic map and get it into the game in your plane. This is a much lengthier tutorial, so we are going to split it up into parts.

1. [Part 1: Setting up Tiled](https://github.com/Card-Forge/forge/wiki/Tutorial-2-A-New-Look#part-1-setting-up-tiled)

2. [Part 2: Building a Basic Map](https://github.com/Card-Forge/forge/wiki/Tutorial-2-A-New-Look#part-2-building-a-basic-map)

3. [Part 3: Adding the Map to your Plane](https://github.com/Card-Forge/forge/wiki/Tutorial-2-A-New-Look#part-3-adding-the-map-to-your-plane)

4. [Part 4: Put it to the Test!](https://github.com/Card-Forge/forge/wiki/Tutorial-2-A-New-Look#part-4-put-it-to-the-test)

### Part 1: Setting up Tiled

To make everything work smoothly, we need to do some set-up of your Tiled project. First, in IntelliJ, navigate to your plane directory (in our case, we named it Test) like so `...\YourIntelliJProjectFolder\forge-gui\res\adventure\Test\`, and create a new directory named 'maps'. Inside that directory make another directory called 'map'. Now open up Tiled, click on New Project, and navigate to the maps directory you just created, (the one with the "map" directory inside it.) Create a new project by picking a name in the file name line, (it is recommended you name it the same as your plane, for ease of bug solving,) and click save.

Next we need to tell Tiled where to find the files Forge uses. Click on Project > "Add Folder to Project...", navigate to `...\YourIntelliJProjectFolder\forge-gui\res\adventure\common\maps`. Select the 'obj' directory, and click "Select Folder". If you are planning to use any of Forge's tilesets, then do the same thing, but this time selecting the 'tileset' directory from `...\YourIntelliJProjectFolder\forge-gui\res\adventure\common\maps` aswell. This adds access to the various objects Forge uses to actually interact with the player. (Enemies, rewards, map transfers, etc.) The tilesets provide the visual appearance of your map, _as well as the collision mapping_. **_Any of these you intend to modify, you must create a new entry in an appropriate subdirectory inside your own plane's directory!_** Otherwise you will be changing it for every single map in every plane that touches those objects or tilesets, which quickly causes unintended consequences. (Further details on how to modify these items, what the subdirectory should look like, and what needs to remain unchanged to be properly read by Forge, will be described in their specific Wiki pages. For now, just keep this information in mind.)

Next click on Project > "Project Properties..." In the 'Paths & Files' section you will a text bow that says 'Extensions Directory' and three dots to the right. Click on those three dots, and navigate to `...\YourIntelliJProjectFolder\forge-gui\res\adventure\common\maps`, select the extensions directory, and click the "Select Folder" button. Then click on "OK". This adds the extensions written for Forge to your project. Without this, your maps will throw errors when Forge tries to do certain object actions, and crash the game. (Learn from my fail on this one.)

Voila! That is the minimum work necessary to prepare your project for adding, or editing, maps unique to your plane.



### Part 2: Building a Basic Map

Alright, so you've got your fancy project, and while this gray screen with a view of multiple folders on the left is cool and all. We know you really came here because you wanted to actually _make a new map_. (Well, technically, you may want to just slightly modify an existing map. But to have it unique to your plane, it will still be effectively a new map in the eyes of IntelliJ.) So let's get to it, let's make a very basic map for Forge. Step one, click the fancy, tempting, "New Map..." button I know you've been staring at. (And possibly already clicked, then came back here to see what you need to do on that next window.) This will bring up a new dialog box, with a bunch of options. 

1. Orientation should be Orthogonal 
2. tile layout format should be CSV
3. Tile render order should Right Down

Those are the Tiled defaults for those options at the time of this writing, but I want it here in case that changes. 

Next up we have the Map size section, the bigger the map, the more you can add, and the longer it will take you to finish. I recommend for our first map, a simple 15 by 15 tiles. You'll make bigger maps, but for your first, don't get overwhelmed. **_DON'T CLICK OK YET!_** I know you want to get started, but we have to change a couple more things. The Tile size needs to be set to 16 wide by 16 high. This is the Forge standard, and the tilesets use it too. Different settings can lead to very unintended side effects. **NOW** you can click "OK".

Welcome to the gray box that is Tiled's default map menu. There's a lot here, but we are going to build a map together, so relax and just follow along. First off, you will often find yourself in need of multiple layers, otherwise your map is simply not going to look good. So, on the right side, you will see a small box with a text line that says "Tile Layer 1". Double click that line, and rename it to "Ground" without the quotes. Next,  right-click and select New > Tile Layer. Name this one "Clutter". Do it again, but name this one "Walls". Finally right-click one more time and select New > Object Layer, name this one "Objects". This gives a basic series of layers to allow us to make a map. You can add more if really needed, but try to limit yourself to six or fewer Tile layers to preserve loading speeds. **Only have ONE object layer**, having a second one will confuse the heck out of Forge.

Now that we have our layers, let's make sure they are in the right order. Click and drag each layer so that, from top to bottom, they rea: Objects, Walls, Clutter, Ground. In tiled, like many programs, tiles on layers farther up will appear on top of ones below them. Objects on top ensure nothing gets lost, or accidentally put under a wall, and can easily be seen. Next up, while we have an Object layer, we need to actually tell Forge what layer to render sprites on. (This allows you make roofs or arches, etc, that a character can walk "under"... it also tells Forge where to actually _put_ the sprite, since it doesn't actually default to the Object layer.) For our map, we want the player to be walking on top of any dirt or grime we add to the map, so left click on the Walls layer. On the left side of the screen, you'll see the properties toolbox. Scroll down to the bottom and you will see the "Custom Properties" line. Right click > Add Property. Click the drop-down that says "string" and choose "bool". In the text box, type "spriteLayer" without the quotes. **This is a value read by Forge, so it is case sensitive. Lowercase 's', uppercase 'L', no spaces.** Click "OK". You will see it has added the property to the custom properties, with a check-box. The box is unchecked, check it. (If you had not added this property, made it a bool, set it's name correctly, and checked the box... Any attempts to load this map would cause Forge to bug-out pretty hard and require a restart to just work right again.)

Now that our map-space is set-up, let's go ahead and save all this work we have done. File > Save As. Open your "map" directory inside the "maps" directory, this is a directory of all your maps you are going to use. To save yourself future head-aches, organize maps into sub-directories from the get-go. In our tutorial case, I will be making a beach, so first I will right-click > new > folder while inside of 'map' and name it "beach". Then I will double-click on that "beach" folder. In this sub-directory I name my tutorial map as "test", but you should name your map as something you'll remember it by, then click save. Now that, that's done, let's get around to the art part of this. Click on the Ground layer. You'll notice you can move your mouse over the map and it'll turn red or blue depending on it you are inside the tile space or not. But no matter where you click, nothing happens. That's because we need to actually choose a tileset to use. On the far left side of the screen, you'll see the various folders we imported into our project. Open the tileset folder, and double click on "main.tsx". This will open a new tab, that shows the entire contents of the "main" tileset from Forge. _Since we don't plan to actually make ANY changes to this tileset_ (gentle reminder.) Simply click on the tab for your map (test.tmx in my case,) and you'll see the "main" tileset has appeared in the bottom-right corner of your screen.

Since I chose a beach, I'm going to make a beach for this tutorial, so I'm going to click on the sand-colored tile near the top-left of the "main" tileset, right next to the default empty one. If you move your mouse back over your map, you will see it shows that sand-colored tile for every square you move it over. If you click, it will even place that color on that tile. While you could individually click on each tile one at a time, for filling in large areas that's much more exasperating. Clicking and dragging will simply apply each tile the mouse moves over. So, instead, along the top, you will see a paint-bucket. Click on that. now if you move your mouse back to the map, it'll show a ghostly tan color across the entire map, minus any squares you already clicked on. (The highlight will only apply to every tile of the same type that you hover over, which is not completely separated from the mouse by a different tileset tile. If you are confused, this will make more sense as you make or modify maps.) Click on the empty space, and it will fill in the entire map with our sandy ground.

Now, a blank sandy beach may be more than you had before, but it's not that interesting, so let's add some detail. Click on the Clutter layer. Then, on our "main" tile-set on the bottom right, click on the water tile just below the blank one in the top left. If you move your mouse back to the map, you will see it wants to change every tile to water. That's because we are still in Bucket Fill mode. On the top of the window, you'll see a rubber stamp, two tools to the left of the pain bucket. Click on that. Now we are once again designing our map one tile at a time. I'm going to draw out some water tiles along the edges of the map, leaving just a couple sandy ones at the bottom center of the map. 

Now, The map is clearly a sand-bar jutting out into water, but it looks pretty rudimentary, even for Forge. As such, let's click on the Walls layer. Then, on the tile-set on the bottom right, there are a bunch of sandy edges looking like they are meant for the edge of water. I'm going to carefully select various tiles from among them and make my coast-line look much nicer. Exactly which tiles where will depend on how you built the previous layer. If you ever place a tile and it doesn't look right. You can click on the Eraser tool at the top of the screen, and then click that tile on your map. As long as you are on the right layer, it will erase that tile.

Huzzah! You've made you little sandbar... But how can you be certain where players and enemies can walk? Well, that's called Collision. Go to View > and make sure there is a check-mark in "Show Tile Collision Shapes". The water tiles will have much strongly defined border now, and depending on your sand borders and how you built it, you may have new lines in them, or not. Collision is defined by the tile in the Tileset. Each tile in the tile set either has collision shapes, or doesn't. **If you are not happy with the collisions as-is, you will need to either change your map some, or make your own tileset.** As a reminder DO NOT MODIFY THE TILESETS FROM MAIN FORGE. If you need to make changes, look for my future tutorial that explains building your own tile-set and modifying it.  

Alright, you have a map, it has water, it has collision... Still looks pretty empty. You could add more details on various layers to make it look nicer, but that won't change the most important things. Namely, there's nothing to _do_ on our sandy beach. Let's fix that. On the left side of the screen, open up the "obj" folder, this has all the objects in Forge. Since they are all Objects, let's click on the Objects layer. Now, click and drag a "gold.tx" object from the obj folder, onto our sand. (make sure its on a portion of sand that isn't blocked by collision.) Unlike the tile layers, objects don't have to be centered to any tile. They can even sit on a grid intersection without issue.

Alright, we got some gold on our map, but that's a bit boring. let's spice it up, by adding an enemy. So click and drag an "enemy.tx" onto the map. Now, all our objects have many properties that make sure they work. But most of them come pre-populated to some degree, so you can just drag-and-drop them. Enemies, however, are diverse and different enough we need to actually define some things about them in order to even function. So click on your placed enemy object, and scroll down the left side to the Custom Properties section. In here, the only option you _have_ define, is the "enemy" property. (Remember, the one in Custom Properties, not the space earlier in the normal properties... Again, learn from my mistakes.) In our case, click on the text box to the right of "enemy", and type "Ghost" without the quotation marks. (While a later tutorial will give more details on all the properties, just know that for this one. It is looking for an entry in your plane's "enemies.json" file. If your plane does not have that file, like ours, it will instead look for the entry in the enemies.json file found in `...\YourIntelliJProjectFolder\forge-gui\res\adventure\common\world`... A later tutorial will guide you through making your own enemies.) You have now defined the enemy, but as-is, it will just stand in place, even if a player gets near. Let's liven things up a little, and make our ghost a bit more annoyed and less sleeping on the job. Go down to the "pursueRange" custom property, and set that text box to 50. Then scroll down further to the "threatRange" box, and set that, also, to 50. Now, if the player gets within 50 pixels of it, the ghost will chase them. The ghost will only move to a distance of 50 pixels from their starting point, doing so, however. (For a more lively enemy that patrols, or jump out, etc. You can find that information in the Configuring Enemies portion of the Wiki.) 

Alright, we got a map, we got a reward, and we got an enemy to protect that reward. All good, except, Forge has no idea where to spawn a player who enters the map, and no idea where the exit to the map is. So, go and drag an "entry_up.tx" object to the little entrance of our sandbar, and resize it so that the arrow takes up the majority of the entry space. Congratulations, your map is made, don't forget to **_SAVE_**.

### Part 3: Adding the Map to your Plane

Alright, so you have a map you want to play, it's saved in your plane, why can't you find it? Well, that's because Forge doesn't **actually** know it's ready to _be_ found yet. We need to get some "biomes" and "points of interest" added to your plane, aswell as. So, first, let's give them a place to go in your plane. Go to your plane's directory in IntelliJ, and open the 'world' sub-directory. Inside of 'world' create a new subdirectory called 'biomes'. Navigate to `...\YourIntelliJProjectFolder\forge-gui\res\adventure\common\world\biomes` and copy the following files to your plane's biomes directory, 'base.json' and 'colorless.json'. We also need to copy the 'points_of_interest.json' from `...\YourIntelliJProjectFolder\forge-gui\res\adventure\common\world` to our custom plane's 'world' directory. Now, open up the 'points_of_interest.json' from our plane's 'world' sub-directory. Inside you will find all the maps used by Forge. If you want to add a map to the list, but keep all the others, you would add it's info the existing json. But for the sake of simplicity in this tutorial, we are going to delete everything inside the json except for the following.
```
[
    {
		"name": "Aerie",
		"displayName": "Aerie",
		"type": "dungeon",
		"count": 30,
		"spriteAtlas": "../common/maps/tileset/buildings.atlas",
		"sprite": "Aerie",
		"map": "../common/maps/map/aerie/aerie_0.tmx",
		"radiusFactor": 0.8,
		"questTags": [
			"Hostile",
			"Nest",
			"Dungeon",
			"Sidequest"
		]
	},
```
and
```
{
		"name": "Spawn",
		"displayName": "Secluded Encampment",
		"type": "town",
		"count": 1,
		"spriteAtlas": "../common/maps/tileset/buildings.atlas",
		"sprite": "Spawn",
		"map": "../common/maps/map/main_story/spawn.tmx",
		"questTags": [
			"Story",
			"Spawn",
			"BiomeColorless"
		]
	}
]
```

For this tutorial, we need to know the following details. 

"name" is the map name in Tiled, so we will change `"name": "Aerie",` to `"name": "YOUR MAP NAME",` ("Test" in our case.) 

"displayName" is the name that appears when the player enters a map. So we will change `"displayName": "Aerie",` to `"displayName": "YOUR MAP DESCRIPTION",` (in our case, I'm going with "Fanciest Beach").

"count" is how many of this map can spawn in the overworld. Since we want to actually find it, let's change `"count": 1,` to `"count": 30,`

and "map" is where we actually STORED the map file. So we are going to change  `"map": "../common/maps/map/aerie/aerie_0.tmx",` to `"map": "../YOUR_PLANE_NAME/maps/map/YOUR_FOLDER_NAME/YOUR_MAP.tmx"` . For my tutorial, that becomes `"map": "../Test/maps/map/beach/test.tmx",`.

You can leave the rest of the Aerie code-block alone for this tutorial. If you followed me exactly, "points_of_interest.json" file should now look like this.
```
[
	{
		"name": "Test",
		"displayName": "Fanciest Beach",
		"type": "dungeon",
		"count": 1,
		"spriteAtlas": "../common/maps/tileset/buildings.atlas",
		"sprite": "Aerie",
		"map": "../Test/maps/map/beach/test.tmx",
		"radiusFactor": 0.8,
		"questTags": [
			"Hostile",
			"Nest",
			"Dungeon",
			"Sidequest"
		]
	},
	{
		"name": "Spawn",
		"displayName": "Secluded Encampment",
		"type": "town",
		"count": 1,
		"spriteAtlas": "../common/maps/tileset/buildings.atlas",
		"sprite": "Spawn",
		"map": "../common/maps/map/main_story/spawn.tmx",
		"questTags": [
			"Story",
			"Spawn",
			"BiomeColorless"
		]
	}
]
```



Now, since we want to play our new map, we still need to make sure it spawns in Forge. Go to "colorless.json" file we copied into our Plane's 'biomes' directory. Scroll down until you see the `"pointsOfInterest":` array. We are going to delete everything in this array except for `"Spawn",` and then we are going to add `"YOUR_MAP_NAME"` ("Test" in my case) immediately afterwards. If you copied this tutorial exactly so far, it would like this.
```
"pointsOfInterest": [
		"Spawn",
		"Test"
	],
```

### Part 4: Put it to the test!

Alright, time for the fun part. If you've come this far, everything is ready to go. Tell IntelliJ to fire up Forge, go into Adventure mode. If you hadn't previously, set your plane to your custom plane, (and restart Forge in that case.) You'll still have the tutorial quest prompts and spawn to work through. But when done, when you leave the portal, things will immediately be visibly different. If you don't see a dungeon right away, bring up the Map, track down your dungeon icon in the Wastes biome, and walk on over. (You may need to avoid a few spawns along the way.) Enter, and, assuming you following perfectly, you'll show up in your new map. If you have questions or run into any problems, reach out to me (shenshinoman) on the Forge discord and I will be happy to help.

After you've done that, you may notice the entire world is devoid of any map except the overworld, the spawn to the old man, and the map you just built. While this is nice for testing a new map, we should probably add a city back in, that way we actually have a place to heal up and test other changes to the plane. To make life really easy, we are just going to add a basic town back to the "Wastes". Navigate to ``...\YourIntelliJProjectFolder\forge-gui\res\adventure\common\world' and open up the "points_of_interest.json" file. Towards the bottom we will find the code block for "Waste Town Generic", select it, and copy it into the "points_of_interest.json" inside your plane's 'world' directory. So, if you've followed along precisely, your "points_of_interest.json" should look like this.

```
[
	{
		"name": "Test",
		"displayName": "Fanciest Beach",
		"type": "dungeon",
		"count": 30,
		"spriteAtlas": "../common/maps/tileset/buildings.atlas",
		"sprite": "Aerie",
		"map": "../Test/maps/map/beach/test.tmx",
		"radiusFactor": 0.8,
		"questTags": [
			"Hostile",
			"Nest",
			"Dungeon",
			"Sidequest"
		]
	},
	{
		"name": "Spawn",
		"displayName": "Secluded Encampment",
		"type": "town",
		"count": 1,
		"spriteAtlas": "../common/maps/tileset/buildings.atlas",
		"sprite": "Spawn",
		"map": "../common/maps/map/main_story/spawn.tmx",
		"questTags": [
			"Story",
			"Spawn",
			"BiomeColorless"
		]
	},
	{
		"name": "Waste Town Generic",
		"type": "town",
		"count": 30,
		"spriteAtlas": "../common/maps/tileset/buildings.atlas",
		"sprite": "WasteTown",
		"map": "../common/maps/map/towns/waste_town_generic.tmx",
		"radiusFactor": 0.8,
		"questTags": [
			"Town",
			"TownGeneric",
			"BiomeColorless",
			"Sidequest",
			"QuestSource"
		]
	}
]
```

Now we need to go and tell the game to actually spawn this town. Inside your plane's "world\biomes" directory, open back up the "colorless.json" file, and add "Waste Town Generic" to the `"pointsOfInterest"` array. If you followed along, that array should now look like this.

```
pointsOfInterest": [
		"Spawn",
		"Test",
		"Waste Town Generic"
	],
```

Now if you open up your plane, you have a bunch of dungeons and a bunch of towns. Congratulations, you have completed this tutorial. Pat yourself on the back, cus it was a long one. (Also, don't forget to save if your system doesn't auto-save.)