# About
There are many different file formats used in forge, due to the longevity of forge and it's growth and change, older formats are used and new ones are being created. This is an attempt to capture the requirements of these files for general creation, and modification. Unfortunately these "formats" may change and this list, or the data in them may become obsolete as changes happen, especially in newer features like Adventure Mode.

# List
This is a quick hand jammed list of the formats that I have found looking through the /res/ folder. This is not complete, but is a quick reference and a start to identifying the files that can be "customized" or modified to make the engine work for you.

The list is (roughly identified) "format name", and the folder location and filename or just the extension if a specific filename is not required. Where "plane folder" is used, it is a subfolder of that game mode, `Adventure/Shandalar/` for example, or `Conquest/planes/Shandalar/` for conquests, and `quest/world/Shandalar/` for quests.

- Deck Files - .dck
- Card Files - .txt
- Editions - .txt
- Draft - .draft
- Blocks - blocks.txt
- Formats - type/name.txt
- Puzzle - .pzl
- Quest
  - Theme - .thm
  - Worlds - World folder/worlds.txt
- Adventure - Plane Folder
  - Plane Configuration - config.json
  - Generated Enemy - .json
  - Generated Starter Deck - .json
- Conquest - Plane Folder
  - Banned Cards - banned_cards.txt
  - Cards - cards.txt
  - Plane Cards - plane_cards.txt
  - Regions - regions.txt
  - Sets - sets.txt
  - Events - set/_events.txt