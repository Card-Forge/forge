import os

# basic variables
CARDSFOLDER = "../../res/cardsfolder"
cardlist = []
total_cards = 0

# main algorithm
print("Loading cards...")
for root, dirs, files in os.walk(CARDSFOLDER):
    for name in files:
        if name.find(".txt") != -1:
            total_cards += 1
            fullpath = os.path.join(root, name)
            cardtext = open(fullpath).read()
            cardtext_lower = cardtext.lower()
            cardname_literal = cardtext.replace('\r','').split('\n')[0].split(':')
            cardname = ":".join(cardname_literal[1:]).strip()
            if (cardtext_lower.find("alternatemode:split") != -1) or (cardtext_lower.find("alternatemode: split") != -1):
            # split card, special handling needed
                cardsplittext = cardtext.replace('\r','').split('\n')
                cardnames = []
                for line in cardsplittext:
                    if line.lower().find("name:") != -1:
                        cardnames.extend([line.split('\n')[0].split(':')[1]])
                cardname = " // ".join(cardnames)

            cardlist.extend([cardname])

print("Loaded %d cards.\n" % total_cards)

filewalker = os.walk(".")
for elem in filewalker:
    for filename in elem[2]:
        if filename.endswith(".java"):
            fully_supported = True
            f = open(filename, "r")
            cubename = filename.replace(".java", "")
            cards = []
            for line in f.readlines():
                if line.find("super") != -1:
                    cubename = line[line.find('"')+1:]
                    cubename = cubename[0:cubename.find('"')]
                    # print(cubename)
                if line.find("cubeCards.add") != -1:
                    line = line.strip()
                    cardname = line[line.find('"')+1:]
                    cardname = cardname[0:cardname.find('"')].replace("'Sleeping Dragon'", '"Sleeping Dragon"').replace("Mardu Woe Reaper","Mardu Woe-Reaper").strip()
                    if cardname not in cardlist:
                        print("Unsupported card in '" + cubename +"': " + cardname)
                        fully_supported = False
                    else:
                        cards.extend([cardname])
                    # print(cardname)
            if fully_supported:
                print("'" + cubename + "' is FULLY SUPPORTED")
            print("")
            out = open("cube/" + cubename + ".dck", "w")
            out.write("[metadata]\n")
            out.write("Name="+cubename+"\n")
            out.write("[main]\n")
            for card in cards:
                out.write("1 " + card + "\n")
            out.close()
            out = open("draft/" + cubename + ".draft", "w")
            out.write("Name:"+cubename+"\n")
            out.write("DeckFile:"+cubename+"\n")
            out.write("Singleton:True\n")
            out.write("\n")
            out.write("Booster: 15 Any\n")
            out.write("NumPacks: 3\n")
            out.close()
