Maps in the adventure mode are created with the tool Tiled
 [](https://www.mapeditor.org/)

Open the tiled-project under `<adventure>/maps/main.tiled-project`  

![Screenshot_2021-09-01_140346](https://user-images.githubusercontent.com/8047400/162841209-c9c7aae3-b21d-47ab-9bd4-c9c79fb511bf.png)

This will allow you to edit the maps and tile sets.  
To interact with the player, objects needs to be added to the Objects layer.  

Objects templates are stored in the "obj" folder, but are not necessary. 
Impotent are the types of the object and his properties.  

## Map properties

`dungeonEffect` duel effect for every enemy in the map. The top level goes to the enemies and `opponent` goes to the player. A dialog shows it when the player enters a map with enemies.  
`ignoreGlobalEffect` true to skip the plane's `globalEffect` in this map's duels.  
`ignoreBiomeEffect` true to skip the effect of the biome the map is in.  
All three carry over to the next maps of the same point of interest until a map sets them again.  

See [Duel effect layers](Configure-Planes.md#duel-effect-layers) for how the effects stack.
 
## Object types

# enemy
will spawn an Enemy on the map. On collide with the player a magic duel will be started.  
If the player win, the enemy will be removed from the map and the player will get the reward.  
If the player loose, then the player will move 1 step back and receive the standard penalty.  
Loot is also defined as enemy without a deck, then the player will receive the reward right away.
Properties:  
`enemy` name of the enemies  
`effect` duel effect for this enemy. Replaces the `effect` from the enemy's data.  
`ignoreGlobalEffect`, `ignoreBiomeEffect`, `ignoreDungeonEffect` true to skip that layer in duels with this enemy. Each replaces the same field from the enemy's data.

# shop
Will spawn an shop on the map. On collide the player will enter the shop.

Properties:  
`shopList` List of possible shop, leave it empty for all shops.  
`signXOffset` x offset for the shop sign.    
`signYOffset` y offset for the shop sign.    

# inn
Will spawn an inn the map. On collide the player will enter the inn.

Properties:   
# entry 
Will be used as the map entry and exit. On collide the player will be teleported to an other map or the over world.  

Properties:  
`direction` the position where to spawn. up means the player will be teleported to the upper edge of the object rectangle.
`teleport` The map where the player gets teleported. If the property is empty, then the player will be teleported to the over world. 
`teleportObjectId` the object id where the player will be teleported. If empty then it will search for an entry object, that would teleport the player back to the source map.
