**[WARNING!!!]**

Page imported from the old SlightlyMagic wiki. To be integrated into other wiki pages and/or README... or deleted.

---

## Duplicate decks checking tool

This python script will check through your deck files and identify which
ones are similar to each other.

It has been tested with Python 2.6.

The source can be found on [it's bitbucket
page](https://bitbucket.org/asret/forge/src/tip/deckdupcheck.py), or
[downloaded
directly](https://bitbucket.org/asret/forge/raw/tip/deckdupcheck.py).

To run it, download it to your forge's "res" directory and invoke the
python interpretor on it.

It still needs work. At present it checks all decks against every other.
This means it will output each matching pair twice - once for each deck.
