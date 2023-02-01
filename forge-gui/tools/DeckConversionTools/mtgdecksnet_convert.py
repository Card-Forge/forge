#!/usr/bin/env python3
# -*- coding: utf-8 -*-

# Modify key directories here
# Recommended parameters: -i -w (will add ! to all unsupported decks) OR -i -f (will only convert supported decks)
CARDSFOLDER = "/home/agetian/Software/ForgeDeckAnalyzer/cardsfolder"
DECKFOLDER = "."
OUT_DECKFOLDER = "./ForgeDecks"

import argparse, os, re

print("Agetian's MtgDecks.net DEC to MTG Forge Deck Converter v4.0\n")

parser = argparse.ArgumentParser(description="Convert MtgDecks.net DEC to Forge DCK.")

parser.add_argument("-f", action="store_true", help="only convert decks that have 100% card support in Forge")
parser.add_argument("-i", action="store_true", help="add MtgDecksNet deck ID to the output deck name for uniqueness")
parser.add_argument("-t", help="only convert decks that belong to the specified format")
parser.add_argument("-w", action="store_true", help="add [!] to the name of decks that have unsupported cards")
parser.add_argument("-U", action="store_true", help="preserve UTF-8 characters in file names")
parser.add_argument("-D", action="store_true", help="sort converted decks into folders according to format")
parser.add_argument("-P", action="store_true", help="convert period (.) to underscore (_) in deck names")

args = parser.parse_args()

# simple structural self-test (can this tool work?)
if not (os.access(os.path.join(CARDSFOLDER,"a","abu_jafar.txt"),os.F_OK) or os.access(os.path.join("decks"),os.F_OK)):
    print("Fatal error:\n    This utility requires the 'cardsfolder' folder with unpacked card files at " + CARDSFOLDER + " and the 'decks' folder with .dck files at " + DECKFOLDER + " in order to operate. Exiting.")
    exit(1)

# basic variables
cardlist = {}
total_cards = 0
ai_playable_cards = 0
total_decks = 0
playable_decks = 0
nonplayable_in_deck = 0

# main algorithm
print("Loading cards...")
for root, dirs, files in os.walk(CARDSFOLDER):
    for name in files:
        if name.find(".txt") != -1:
            total_cards += 1
            fullpath = os.path.join(root, name)
            cardtext = open(fullpath).read()
            cardtext_lower = cardtext.lower()
            cardname_lines = cardtext.replace('\r','').split('\n')
            cardname = ""
            for line in cardname_lines:
                if line.strip().lower().startswith("name:"):
                    if line.count(':') == 1:
                        cardname = line.split(':')[1].strip()
                    break
            if cardname == "":
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
            if (cardtext_lower.find("alternatemode:modal") != -1) or (cardtext_lower.find("alternatemode: modal") != -1):
                # ZNR modal card, special handling needed
                cardsplittext = cardtext.replace('\r','').split('\n')
                cardnames = []
                for line in cardsplittext:
                    if line.lower().find("name:") != -1:
                        cardnames.extend([line.split('\n')[0].split(':')[1]])
                cardname = cardnames[0].strip()
            if cardtext.lower().find("remaideck") != -1:
                cardlist[cardname] = 0
            else:
                cardlist[cardname] = 1
                ai_playable_cards += 1

perc_playable = (float(ai_playable_cards) / total_cards) * 100
perc_unplayable = ((float(total_cards) - ai_playable_cards) / total_cards) * 100

print("Loaded %d cards, among them %d playable by the AI (%d%%), %d unplayable by the AI (%d%%).\n" % (total_cards, ai_playable_cards, perc_playable, total_cards - ai_playable_cards, perc_unplayable))

re_Metadata = '^//(.*) a (.*) deck by (.*) \(dec\) Version$'
re_Metadata2 = '^//(.*) a ([A-Za-z]+) MTG deck played by (.*) in (.*) - MTGDECKS.NET.*$'
re_Metadata3 = '^//(.*) a (.*) deck by (.*)$'
re_DeckID = '^([0-9]+)\.dec$'
re_Maindeck = '^([0-9]+) (.*)$'
re_Sideboard = '^SB:[ \t]+([0-9]+) (.*)$'
re_Timeinfo = '<!--.*>$'

unsupportedList = []
badChars = ['/', '\\', '*']

print("Converting decks...")
for root, dirs, files in os.walk(DECKFOLDER):
    for name in files:
        if name.find(".dec") != -1:
            print("Converting deck: " + name + "...")
            deck_id = -1
            s_DeckID = re.search(re_DeckID, name)
            if s_DeckID:
                deck_id = s_DeckID.groups()[0]
            fullpath = os.path.join(root, name)
            deckdata = open(fullpath).readlines()
            name = ""
            creator = ""
            format = ""
            maindeck = []
            maindeck_cards = 0
            sideboard = []
            supported = True
            deckHasUnsupportedCards = False

            for line in deckdata:
                #line = line.replace("\xE1", "a")
                #line = line.replace("\xFB", "u")
                #line = line.replace("\xE9", "e")
                line = line.replace("\xC3\x86", "AE")
                line = line.replace("\xC3\xA9", "e")
                line = line.replace("\xC3\xBB", "u")
                line = line.replace("\xC3\xA1", "a")
                line = line.replace("\xC3\xAD", "i")
                #line = line.replace("Unravel the Aether", "Unravel the AEther")
                #line = line.replace("Aether", "AEther")
                line = line.replace("Chandra, Roaring Flame", "Chandra, Fire of Kaladesh")
                line = line.replace("Lurrus of the Dream Den", "Lurrus of the Dream-Den")
                line = line.replace("Nissa, Sage Animist", "Nissa, Vastwood Seer")
                line = line.replace("Neck Breaker", "Breakneck Rider")
                line = line.replace("Avacyn, the Purifier", "Archangel Avacyn")
                line = line.replace("Dandân", "Dandan")
                line = line.replace("Séance", "Seance")
                line = line.replace("Jötun Grunt", "Jotun Grunt")
                line = line.replace("Ifh-Bíff Efreet", "Ifh-Biff Efreet")
                line = line.replace("Juzám Djinn", "Juzam Djinn")
                line = line.replace("\xC3\xB6", "o")
                line = line.replace("\x97", "-")
                line = line.replace("\x91", "'")
                line = line.replace("\xFB", "u")
                line = line.replace("\xFC", "u")
                line = line.replace("\xC4", "A")
                if line[0] != "/" and line.find(" // ") == -1:
                    line = line.replace("/"," // ")
                timepos = line.find("<!")
                if timepos > -1:
                    line = line[0:timepos]
                isCardSupported = True
                mode = 0
                s_Metadata = re.search(re_Metadata, line)
                if not s_Metadata:
                    s_Metadata = re.search(re_Metadata2, line)
                    mode = 1
                if not s_Metadata:
                    s_Metadata = re.search(re_Metadata3, line)
                    mode = 0
                if s_Metadata:
                    name = s_Metadata.groups()[0].strip()
                    format = s_Metadata.groups()[1].strip()
                    creator = s_Metadata.groups()[2].strip()
                    event = ""
                    if mode == 1:
                        event = s_Metadata.groups()[3].strip()
                        for badChar in badChars:
                            event = event.replace(badChar, "-")
                    if args.t and args.t != format:
                        print("Skipping an off-format deck " + name + " (format = " + format + ")")
                        supported = False
                    continue
                s_Maindeck = re.search(re_Maindeck, line)
                if s_Maindeck:
                    cardAmount = s_Maindeck.groups()[0].strip()
                    cardName = s_Maindeck.groups()[1].strip()
                    if cardName == "":
                        continue
                    altModalKey = cardName.split(" // ")[0].strip()
                    if not cardName in cardlist.keys() and not cardName.replace("Aether", "AEther") in cardlist.keys() and not cardName.replace("AEther", "Aether") in cardlist.keys() and not altModalKey in cardlist.keys():
                        print("Unsupported card (MAIN): " + cardName)
                        if args.f:
                            supported = False
                        else:
                            isCardSupported = False
                            deckHasUnsupportedCards = True
                            if not cardName in unsupportedList:
                                unsupportedList.extend([cardName])
                    if altModalKey in cardlist.keys():
                        mdline = cardAmount + " " + altModalKey # ZNR modal cards with //
                    elif cardName in cardlist.keys():
                        mdline = cardAmount + " " + cardName
                    elif cardName.replace("Aether", "AEther") in cardlist.keys():
                        mdline = cardAmount + " " + cardName.replace("Aether", "AEther")
                    elif cardName.replace("AEther", "Aether") in cardlist.keys():
                        mdline = cardAmount + " " + cardName.replace("AEther", "Aether")
                    else:
                        mdline = cardAmount + " " + cardName # for the purposes of unsupported cards
                    if isCardSupported:
                        maindeck.extend([mdline])
                    else:
                        maindeck.extend(["#"+mdline])
                    maindeck_cards += int(cardAmount)
                    continue
                s_Sideboard = re.search(re_Sideboard, line)
                if s_Sideboard:
                    cardAmount = s_Sideboard.groups()[0].strip()
                    cardName = s_Sideboard.groups()[1].strip()
                    if cardName == "":
                        continue
                    altModalKey = cardName.split(" // ")[0].strip()
                    if not cardName in cardlist.keys() and not cardName.replace("Aether", "AEther") in cardlist.keys() and not cardName.replace("AEther", "Aether") in cardlist.keys() and not altModalKey in cardlist.keys():
                        print("Unsupported card (SIDE): " + cardName)
                        if args.f:
                            supported = False
                        else:
                            isCardSupported = False
                            deckHasUnsupportedCards = True
                            if not cardName in unsupportedList:
                                unsupportedList.extend([cardName])
                    if altModalKey in cardlist.keys():
                        sdline = cardAmount + " " + altModalKey # ZNR modal cards with //
                    elif cardName in cardlist.keys():
                        sdline = cardAmount + " " + cardName
                    elif cardName.replace("Aether", "AEther") in cardlist.keys():
                        sdline = cardAmount + " " + cardName.replace("Aether", "AEther")
                    elif cardName.replace("AEther", "Aether") in cardlist.keys():
                        sdline = cardAmount + " " + cardName.replace("AEther", "Aether")
                    else:
                        sdline = cardAmount + " " + cardName # for the purposes of unsupported cards
                    if isCardSupported:
                        sideboard.extend([sdline])
                    else:
                        sideboard.extend(["#"+sdline])
                    continue

            # convert here
            if supported and len(maindeck) > 0:
                if creator != "":
                    deckname = creator + " - " + name + " ("
                else:
                    deckname = name + " ("
                deckname += format
                if args.i and int(deck_id) > -1:
                    if format != "":
                        deckname += ", #" + deck_id
                    else:
                        deckname += "#" + deck_id
                deckname += ")"
                #deckname = (c for c in deckname if ord(c) < 128)
                if not args.U:
                    deckname = re.sub(r'[^\x00-\x7F]', '@', deckname)
                deckname = re.sub(r'[/\\]', '-', deckname)
                deckname = re.sub(r'[?*]', '_', deckname)
                if args.w and deckHasUnsupportedCards:
                    deckname += " [!]"
                if args.D:
                    if deckHasUnsupportedCards:
                        outname = "Unsupported" + "/" + format + "/"
                        pathToUnsupported = "./" + OUT_DECKFOLDER + "/Unsupported/" + format
                        if not os.path.isdir(pathToUnsupported):
                            os.makedirs(pathToUnsupported)
                    else:
                        outname = format + "/"
                    if not os.path.isdir("./" + OUT_DECKFOLDER + "/" + format):
                        os.makedirs("./" + OUT_DECKFOLDER + "/" + format)
                else:
                    outname = ""
                outname += deckname + ".dck"
                print ("Writing converted deck: " + outname)
                dck = open(OUT_DECKFOLDER + "/" + outname, "w")

                if event:
                    dck.write("#EVENT:"+event+"\n")

                dck.write("[metadata]\n")
                dck.write("Name="+deckname+"\n")
                dck.write("[general]\n")
                dck.write("Constructed\n")
                dck.write("[Main]\n")
                for m in maindeck:
                    dck.write(m+"\n")
                if not ((format == "Commander" or format == "Brawl" or format == "Duel-Commander" or format == "Historic-Brawl" or format == "Archon") and len(sideboard) == 1):
                    dck.write("[Sideboard]\n")
                else:
                    dck.write("[Commander]\n")
                for s in sideboard:
                    dck.write(s+"\n")

# write out unsupported cards
log = open("dec2forge.log", "w")
log.write("Unsupported cards:\n")
for uns in unsupportedList:
    log.write(uns+"\n")

