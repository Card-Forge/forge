#!/usr/bin/env python3

CARDSFOLDER = "../res/cardsfolder"
DECKFOLDER = "."

import argparse, os, re

print("Agetian's MTG Forge Deck AI Compatibility Analyzer v4.0\n")

parser = argparse.ArgumentParser(description="Analyze MTG Forge decks for AI compatibility.")
parser.add_argument("-p", action="store_true", help="print only AI-playable decks")
parser.add_argument("-u", action="store_true", help="print only AI-unplayable decks")
parser.add_argument("-d", action="store_true", help="physically delete unplayable decks")
parser.add_argument("-s", action="store_true", help="ignore sideboard when judging playability of decks")
parser.add_argument("-l", action="store_true", help="log unsupported cards to ai_unsupported.log")
parser.add_argument("-x", action="store_true", help="account for limited-playable cards from ai_limitedplayable.lst")

args = parser.parse_args()

# simple structural self-test (can this tool work?)
if not (os.access(os.path.join(CARDSFOLDER,"a","abu_jafar.txt"),os.F_OK) or os.access(os.path.join("decks"),os.F_OK)):
    print("Fatal error:\n    This utility requires the 'cardsfolder' folder with unpacked card files at " + CARDSFOLDER + " and the 'decks' folder with .dck files at " + DECKFOLDER + " in order to operate. Exiting.")
    exit(1)
if args.p and args.u:
    print("Fatal error:\n    The -p and -u options are mutually exclusive, please specify one of these options and not both of them at the same time.")
    exit(1)

# basic variables
cardlist = {}
total_cards = 0
ai_playable_cards = 0
total_decks = 0
playable_decks = 0
nonplayable_in_deck = 0

unplayable_cards = {}
limited_playable_cards = []

# limited-playable
if args.x:
    ff = open("ai_limitedplayable.lst").readlines()
    for line in ff:
        limited_playable_cards.extend([line.replace("\n","")])

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
            if cardtext.lower().find("remaideck") != -1 or cardtext.lower().find("ai:removedeck:all") != -1:
                cardlist[cardname] = 0
            else:
                cardlist[cardname] = 1
                ai_playable_cards += 1

perc_playable = (float(ai_playable_cards) / total_cards) * 100
perc_unplayable = ((float(total_cards) - ai_playable_cards) / total_cards) * 100

print("Loaded %d cards, among them %d playable by the AI (%d%%), %d unplayable by the AI (%d%%).\n" % (total_cards, ai_playable_cards, perc_playable, total_cards - ai_playable_cards, perc_unplayable))

print("Scanning decks...")
for root, dirs, files in os.walk(DECKFOLDER):
    for name in files:
        if name.find(".dck") != -1:
            total_decks += 1
            nonplayable_in_deck = 0
            cardnames = []
            fullpath = os.path.join(root, name)
            deckdata = open(fullpath).readlines()
            lim_playable = False
            for line in deckdata:
                if args.s:
                    if line.strip().lower() == "[sideboard]":
                        break
                regexobj = re.search('^([0-9]+) +([^|]+)', line)
                if regexobj:
                    cardname = regexobj.groups()[1].replace('\n','').replace('\r','').strip()
                    cardname = cardname.replace('\xC6', 'AE')
                    cardname = cardname.replace("AEther Mutation", "Aether Mutation")
                    cardname = cardname.replace("AEther Membrane", "Aether Membrane")
                    if cardlist[cardname] == 0:
                        if limited_playable_cards.count(cardname) > 0:
                            print("Found limited playable: " + cardname)
                            lim_playable = True
                            continue
                        cardnames.extend([cardname])
                        nonplayable_in_deck += 1
                        if not cardname in unplayable_cards.keys():
                            unplayable_cards[cardname] = 1
                        else:
                            unplayable_cards[cardname] = unplayable_cards[cardname] + 1
            if nonplayable_in_deck == 0:
                if not args.u:
                    playable_decks += 1
                    print("%s is PLAYABLE by the AI." % name)
                if lim_playable:
                    os.rename(os.path.join(root, name), os.path.join(root, name.replace(".dck", " [!].dck").replace("[!] [!]", "[!]")))
            else:
                if not args.p:
                    print("%s is UNPLAYABLE by the AI (%d unplayable cards: %s)." % (name, nonplayable_in_deck, str(cardnames)))
                if args.d:
                    os.remove(os.path.join(root, name))

perc_playable_decks = (float(playable_decks) / total_decks) * 100
perc_unplayable_decks = ((float(total_decks) - playable_decks) / total_decks) * 100

print("\nScanned %d decks, among them %d playable by the AI (%d%%), %d unplayable by the AI (%d%%)." % (total_decks, playable_decks, perc_playable_decks, total_decks - playable_decks, perc_unplayable_decks))

if args.l:
    logfile = open("ai_unplayable.log", "w")
    sorted_dict = sorted(unplayable_cards, key=unplayable_cards.__getitem__)
    for k in sorted_dict:
        logfile.write(str(unplayable_cards[k]) + " times: " + k + "\n")
    logfile.close()
