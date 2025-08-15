This is based on Latent Dirichlet allocation:

1) use Agetian's getmtgdecks.sh scripts to download recent decks from mtgdecks.net and convert them to forge format and separate into Standard/Modern/Pioneer using his python tools (deck2forge_3.5.py and deckSorter_1.4a)
2) copy the new decks into forge-gui/res/deckgendecks/$format for each format and remove the $format.lda.dat and $format.raw.dat files from that folder
3) check out the Austin/ldastream branch and merge it with the latest
4) run the forge.deck.generate.LDAModelGenerator class in the forge-gui-desktop module
this will generate updated .raw.dat and .lda.dat files that can then be checked in
probably need to do step 3 before 2 actually