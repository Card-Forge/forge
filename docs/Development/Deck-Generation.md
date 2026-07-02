This is based on Latent Dirichlet allocation:
1) use Agetian's getmtgdecks.sh scripts to download recent decks from mtgdecks.net and convert them to Forge format and separate into Standard/Modern/Pioneer using his python tools (deck2forge_3.5.py and deckSorter_1.4a)
2) drop as many .dck files in the forge-gui/res/deckgendecks/$format/ folder for each format that needs refreshing (ideally around 2000-5000 decks per format) and remove the old .dat files
3) Run the LDAModelGenerator (just the main method with no parameters) - this will then start the training for each missing file
the LDA training process will take several hours to run for each format - longer based on how many decks are in the format folder
(Open Forge locally to check the decks look reasonable - optional - if they don't look good maybe find more data/decks and repeat steps 1-3)
4) Commit the new $format.*.dat files to the repo
