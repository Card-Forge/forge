# About

Card images are assets used to represent the real cards in game. You DO NOT need images to play forge, however representing real cards is a nice ability forge provides. These images can be any image set you like, or custom images too.

Primarily there are two types of images you'll care about; cards, and tokens.

**Cards** - are the primary card image files, and can be generic (as all cards of the same name have the same rules) or per set since there may be different art work in different editions. Typically these are scans of the original cards and are provided by forge's FTP, or scryfall. You can customize a full generic set, or any edition if you desire... (you could for example, blur every card image and play a literal "blind" or "farsighted" game.)

**Tokens** - are the images for the cards replacing a generic "1/1 zombie" for example. These are less frequently updated, and are typically the bulk of what is missing when doing an audit. However, these are probably where the more true "custom" replacements are available, with either custom artwork, or modified of other existing.

A deck may explicitly define the edition and art variant of each card it includes. If a deck specifies those for no card, Forge uses a special algorithm to determine which card printings were the latest by the moment all of deck's had been printed. These very editions of cards are used when loading deck into memory to reflect the flavour of the season when the deck was built.

Card images are cleared from memory cache when switching screens and between games.

# Downloading

Due to charges in Forges hosting and scryfall terms you can no longer predownload card images. Turn on auto download in forge to download card images when first viewed.

## In Forge Auto Download:

**Download Missing Images - Setting**
- This will download the images from the sources as the game requests the image in situ. 
- This can be useful if you don't want to have copies of every card... You can do small pre-caching by loading your decks in the deck editor prior to playing to download just those images.

## Bulk Download Sites: (Not in game)

- [http://download.austeregrim.net](http://download.austeregrim.net) 
  - Note from user AustereGrim;
> I provide my site for free for bulk downloading the entire image catalog. So you don't need to give those spam sites more advertising spots. If the server is loaded bandwidth is shared, right now it's not heavily used so please feel free to download the 4+gb zips, or the individual zips if you need sets. They are the images Kev has uploaded to my site, and the Zips are updated nightly automatically.

**(I'm not gatekeeping, please if you have a private location for bulk downloads or for alternate or custom arts, you can update this wiki too or let us know in the discord. I'll be happy to update the wiki page with additional sources.)**

If you have an older Android device for increased performance or to save bandwidth it might be a good idea to use lower resolution images instead: https://www.slightlymagic.net/forum/viewtopic.php?f=15&t=29104

# Storage

Card images are stored in `pics/cards`, and tokens in `pics/tokens`, in the Cache folder for forge: 

- **Windows** - `C:\Users\<username>\appdata\local\forge\Cache\`
  - You'll need to enable hidden folders.
- **Android 11+** - `Internal Storage/Android/obb/forge.app/Forge/cache/`   
  - *_NOTE: You need a third party File Manager to access the obb folder and allow storage access permission_*
- **Android 8 to 10** - `Internal Storage/Forge/cache/`
- **Linux** - `/home/<username>/.cache/forge/`
- **MacOS** - `/Users/<username>/Library/caches/forge/`
  - Use `Command + Shift + .` to show hidden files.


# Subfolders

If you don't care about the edition's version of cards, images can be stored in the root directory of the cards folder.

`/cache/pics/cards/`

If you want the edition's versions of the cards, they need to go under the edition's code subfolder.

`/cache/pics/cards/AFR` for example for Adventures in the Forgotten Realms.

# File Naming

**File Names:**
- Cards file names follow a simple principle: `Card Name#.border.ext`
  - `Card Name` - Card Name with spaces.
  - `#` - Alternate Art number; if more than one art exists for the card.
  - `border` - Border Type; fullborder, crop. (I don't know all of them.)
  - `ext` - Extension, jpg or png are supported.

**Alternate images:**

Alternate images are defined as cards with the same name in the set's edition file, if the edition file does not have the alternate listed forge will not see the alternate there!

**Standard Alternate Arts:**

So for example the AFR set (as most sets) shows these 4 versions of swamp;
```
270 L Swamp @Piotr Dura
271 L Swamp @Sarah Finnigan
272 L Swamp @Titus Lunter
273 L Swamp @Adam Paquette
```
The file naming would be represented by a number after the name:
```
Swamp1.fullborder.jpg
Swamp2.fullborder.jpg
Swamp3.fullborder.jpg
Swamp4.fullborder.jpg
```

**Additional Alternate Arts:**

They may also be listed separately as "extended arts", "showcase", or "borderless" in the same editions file:
```
[cards]
90 U Black Dragon @Mark Zug
```
and 
```
[borderless]
291 U Black Dragon @Jason A. Engle
```
Where the files are:
```
black dragon1.fullborder.jpg
black dragon2.fullborder.jpg
```

**Forcing an Alternate:**

Renaming and creating a second of an existing card **will not work**, for example creating two "Burning hands" which does not have alternate art;
```
burning hands1.fullborder.jpg
burning hands2.fullborder.jpg
```
Forge will not see either of those, and will probably download the missing `burning hands.fullborder.jpg` for you. Similarly adding a 3rd black dragon `black dragon3.fullborder.jpg` will **not** work either.

