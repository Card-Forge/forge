Forge provides an in-game console in adventure mode.

You can access (and close) the console while exploring by pressing F9 (or Fn-F9).
The equivalent method to access the console on mobile is to hold down the character image in the right top of the screen.
Holding the character image again will close the console (as will typing `exit`).

To scroll the console window, click and drag the text box.

Commands themselves are case-sensitive. Arguments with spaces need double quotes. Amounts are integers. Durations can have a decimal point.

## Available commands

| Command Example | Description |
| -- | -- |
| clearnosell | Removes all 'no sell value' cards you own, that are not used in a deck, from your inventory |
| crack | Cracks a random item you have equipped |
| fly &lt;duration&gt; | Allows you to walk over obstacles in the overworld or POI map for a number of seconds<br>(TODO BUG: useless in POIs) |
| give boosters &lt;edition code&gt; \[amount\] | Adds a booster of a set (case-insensitive; for example, the edition code for "Limited Edition Beta" is LEB) |
| give \[nosell\] card &lt;name&gt; \[amount\] | Adds a card to your inventory, optionally with no sell value (case insensitive; use the front-left-top title if there are several titles) |
| give gold &lt;amount&gt; | Adds an amount of gold |
| give item &lt;item id&gt; | Adds an Adventure item (such as "Leather Boots", case-insensitive, defined in items.json) |
| give life &lt;amount&gt; | Increases your current health AND max health by an amount |
| give print &lt;edition code&gt; &lt;collector number&gt; | Adds a specific printing of a card to your inventory (case-insensitive, for example, "Black Lotus" from Alpha edition is LEA 232) |
| give quest &lt;id&gt; | Adds the quest by its numeric id (defined in quests.json) |
| give set &lt;edition code&gt; | Adds 4 copies of every card of a set to your inventory, flagged as having no sell value (for example, the edition code for "Secret Lair Drop" is SLD) |
| give shards &lt;amount&gt; | Adds an amount of shards |
| heal amount &lt;amount&gt; | Adds an amount to your current health, capping at max health |
| heal full | Sets current health to max health |
| heal percent &lt;float&gt; | Adds a percentage of your max health to your current health, capping at max health (adding 25% is 0.25) |
| hide &lt;duration&gt; | Makes enemies not chase you in the overworld or POI map for a number of seconds<br>(TODO BUG: useless in POIs) |
| leave | Exits the current POI map (town/dungeon/cave) and returns you to the overworld |
| remove enemy all | Removes all the enemies from the POI map or the overworld |
| remove enemy nearest | Removes the nearest overworld enemy |
| remove enemy &lt;object id&gt; | Removes the enemy from the current POI map (enemy object ids are defined in the POI's *.tmx file) |
| set event &lt;block name or edition code&gt; \[event format\] | Sets the competitive event at the current town POI with an inn (format is case-insensitive, such as  Constructed, Draft, Jumpstart, or Sealed; an omitted format will default to Draft, or Jumpstart for a Jumpstart block like "Dominaria United Jumpstart"; blocks are defined in blocks.txt)<br>(TODO: Constructed events are not yet implemented) |
| setColorID &lt;color letters&gt; | Sets the player color identity (one or more of WUBRGC, one word, case-insensitive); probably used for testing and shops |
| spawn enemy &lt;name&gt; | Spawns an enemy nearby in the overworld (not in a POI map; use a name or nameOverride defined in enemies.json, case-sensitive, as seen in-game) |
| sprint &lt;duration&gt; | Increases your walk speed in the overworld or POI map for a number of seconds |
| teleport to &lt;x&gt; &lt;y&gt; | Moves you x tiles east and y tiles north from the left bottom corner |
| debug collision | Displays bounding boxes around entities |
| debug map | Enables drag gestures on the overworld's mini map (while it's small) to fast travel where the gesture is released |
| debug off | Turns off debugging |
| reset map | Flags the current POI map to reset when you exit it, restoring all deleted POI map objects like enemies |
| resetMapQuests | Resets the current POI map's local side quest flags; all quest progress within that location will be lost |
| resetQuests | Resets the player's global quest flags; current quests won't be abandoned or restarted, but they will lose track of what had been done; POI side quests will be unaffected. |
| sanitize editions | Replaces all cards from non-allowed editions in decks and inventory with printings from allowed editions (if configured for the Adventure plane) |
| dumpEnemyColorIdentity | Prints all enemies, with their colour affinity and deck name, to stdout<br>(TODO: useless on mobile and makes the app unresponsive) |
| dumpEnemyDeckColors | Prints all decks available to enemies and their affinities to stdout<br>(TODO: useless on mobile and makes the app unresponsive) |
| dumpEnemyDeckList | Prints all enemy deck lists to stdout<br>(TODO: useless on mobile and makes the app unresponsive) |
| listPOI | Prints all POIs, with their name and type, to stdout (defined in points_of_interest.json)<br>(TODO: useless on mobile) |
| fullHeal | Same as `heal full` command<br>(TODO: redundant) |
| getShards amount &lt;amount&gt; | Same as `give shards` command<br>(TODO: redundant) |
| exit | Closes the console |
