#!/usr/bin/env python3

CARDSFOLDER = "../../res/cardsfolder"
EDITIONS = "../../res/editions"
DECKFOLDER = "."

import argparse, os, re, shutil

print("Agetian's MTG Forge Deck Sorter v2.0\n")

parser = argparse.ArgumentParser(description="Sort decks into folders (by edition).")
parser.add_argument("-d", action="store_true", help="physically delete original (unsorted) decks")
parser.add_argument("-x", action="store_true", help="exclude sorting by event")

args = parser.parse_args()

# simple structural self-test (can this tool work?)
if not (os.access(os.path.join(CARDSFOLDER,"a","abu_jafar.txt"),os.F_OK) or os.access(os.path.join("decks"),os.F_OK) or os.access(os.path.join(EDITIONS,"Alara Reborn.txt"),os.F_OK)):
    print("Fatal error:\n    This utility requires the 'cardsfolder' folder with unpacked card files at " + CARDSFOLDER + ", 'editions' folder at " + EDITIONS + " and the 'decks' folder with .dck files at " + DECKFOLDER + " in order to operate. Exiting.")
    exit(1)

# basic variables
cardlist = {}
editions = {}
edition_names = {}
cards_by_edition = {}
total_cards = 0
total_editions = 0
ai_playable_cards = 0
total_decks = 0
playable_decks = 0
nonplayable_in_deck = 0

ignore_cards = ['Swamp', 'Plains', 'Mountain', 'Island', 'Forest']

# regexes
re_Code = '^Code=(.*)$'
re_Date = '^Date=([0-9]+-[0-9]+-[0-9]+)$'
re_Date2 = '^Date=([0-9]+-[0-9]+)$'
re_Type = '^Type=(.*)$'
re_Name = '^Name=(.*)$'
re_Card = '^[0-9]* *[A-Z] (.*)$'

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

print("Loading editions...")
for root, dirs, files in os.walk(EDITIONS):
    for name in files:
        if name.find(".txt") != -1:
            total_editions += 1
            fullpath = os.path.join(root, name)
            edition = open(fullpath).readlines()
            foundCards = False
            code = ""
            date = ""
            etype = ""
            name = ""
            for line in edition:
                line = line.replace("\r\n","")
                line = line.split(" @")[0]
                if not foundCards:
                    if line.find("[cards]") != -1:
                        foundCards = True
                    else:
                        s_Code = re.search(re_Code, line)
                        if s_Code:
                            code = s_Code.groups()[0]
                            #print("Code found: " + code)
                        s_Date = re.search(re_Date, line)
                        if s_Date:
                            date = s_Date.groups()[0]
                            #print("Date found: " + date)
                        s_Date2 = re.search(re_Date2, line)
                        if s_Date2:
                            date = s_Date2.groups()[0] + "-01"
                            #print("Date found: " + date)
                        s_Type = re.search(re_Type, line)
                        if s_Type:
                            etype = s_Type.groups()[0]
                            #print("Type found: " + etype)
                        s_Name = re.search(re_Name, line)
                        if s_Name:
                            name = s_Name.groups()[0]
                            #print("Name found: " + name)
                else:
                    if etype != "Expansion" and etype != "Core" and etype != "Starter" and code != "VOC" and code != "MIC" and code != "AFC":
                        #print("NOT LOADING: " + code)
                        continue
                    else:
                        if not code in editions.keys():
                            editions[code] = date
                            edition_names[code] = name
                            #print(editions)
                        s_Card = re.search(re_Card, line)
                        if s_Card:
                            card = s_Card.groups()[0].strip()
                            #print("Card found: " + card)
                            if not card in cards_by_edition.keys():
                                cards_by_edition[card] = []
                            cards_by_edition[card].append(code)
                            

print("Loaded " + str(len(editions)) + " editions.")

def get_latest_set_for_card(card):
    cdate = "0000-00-00"
    edition = "XXX"
    if ignore_cards.count(card) != 0:
        return "LEA"
    if not card in cards_by_edition.keys():
        #print("Warning: couldn't determine an edition for card: " + card)
        return "LEA"
    for code in cards_by_edition[card]:
        if editions[code] > cdate:
            cdate = editions[code]
            edition = code
    return edition
        
def get_earliest_set_for_card(card):
    cdate = "9999-99-99"
    edition = "XXX"
    if not card in cards_by_edition.keys():
        #print("Warning: couldn't determine an edition for card: " + card)
        return "LEA"
    for code in cards_by_edition[card]:
        if editions[code] < cdate:
            cdate = editions[code]
            edition = code
    return edition

def get_latest_set_for_deck(deck):
    edition = "LEA"
    for line in deck.split('\n'):
        #print("Line: " + line)
        regexobj = re.search('^([0-9]+) +([^|]+)', line)
        if regexobj:
            cardname = regexobj.groups()[1].replace('\n','').replace('\r','').strip()
            earliest_for_card = get_earliest_set_for_card(cardname)
            #print("Card: " + cardname + ", latest for it: " + latest_for_card)
            if editions[earliest_for_card] > editions[edition]:
                edition = earliest_for_card
    return edition

def get_event_for_deck(deck):
    evline = deck.split('\n')[0].split("#EVENT:")
    if len(evline) == 2:
        return evline[1]
    else:
        return ""

#print(cards_by_edition["Fireball"])
#print(edition_names[get_latest_set_for_card("Fireball")])
#testdeck = """
#3[main]
#4 Fireball
#4 Lim-Dul's Vault
#4 Bygone Bishop
#[sideboard]
#4 Thassa's Bounty
#"""
#print(edition_names[get_latest_set_for_deck(testdeck)])

print("Scanning decks...")
for root, dirs, files in os.walk(DECKFOLDER):
    for name in files:
        if name.find(".dck") != -1:
            total_decks += 1
            fullpath = os.path.join(root, name)
            deckdata = open(fullpath).read()
            set_for_deck = edition_names[get_latest_set_for_deck(deckdata)]
            event_for_deck = get_event_for_deck(deckdata)
            if args.x:
                event_for_deck = ""
            if event_for_deck != "" and event_for_deck[len(event_for_deck)-1] == ".":
                event_for_deck = event_for_deck[0:len(event_for_deck)-1]
            print("Deck: " + name + ", Set: " + set_for_deck + ", Event: " + event_for_deck)
            if not os.access(os.path.join(root, set_for_deck, event_for_deck), os.F_OK):
                os.makedirs(os.path.join(root, set_for_deck, event_for_deck))
            shutil.copy(fullpath, os.path.join(root, set_for_deck, event_for_deck, name))
            if args.d:
                os.remove(fullpath)

