from os import listdir, path

preconDecks = [ f.lower().replace("'",'').replace(',', '').replace(' ', '_')[:-4] for f in listdir('../res/quest/precons') if f.endswith('.dck') ]

with open('../res/lists/precon-images.txt') as inp:
	files = inp.read().strip().split('\n')
	preconIcons = [ path.basename(path.splitext(f)[0]).lower() for f in files if len(f) > 0 ]

preconDecks.sort()
preconIcons.sort()

print "Decks without Icons"
print set(preconDecks) - set(preconIcons)

print "Icons without Decks"
print set(preconIcons) - set(preconDecks)