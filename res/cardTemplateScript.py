#!/usr/bin/env python

pathToMtgData = "mtg-data.txt"

############IMPLEMENTATION FOLLOWS############
import os,sys,fnmatch

class Card:
	def __init__(self, name):
		self.name = name
		self.cost = ""
		self.types = ""
		self.pt = ""
		self.oracle = []
		self.sets = ""

def initSets():
	# Base Sets
	forgeSets.append('LEA')
	forgeSets.append('LEB')
	forgeSets.append('2ED')
	forgeSets.append('3ED')
	forgeSets.append('4ED')
	forgeSets.append('5ED')
	forgeSets.append('6ED')
	forgeSets.append('7ED')
	forgeSets.append('8ED')
	forgeSets.append('9ED')
	forgeSets.append('10E')
	forgeSets.append('M10')
	forgeSets.append('M11')
	forgeSets.append('M12')

	# Multiplayer Sets
	forgeSets.append('COM')

	# Portal
	forgeSets.append('POR')
	forgeSets.append('PO2')
	forgeSets.append('PTK')

	# Starter
	forgeSets.append('S99')
	forgeSets.append('S00')

	# Early Sets
	forgeSets.append('ARN')
	forgeSets.append('ATQ')
	forgeSets.append('LEG')
	forgeSets.append('DRK')
	forgeSets.append('FEM')
	forgeSets.append('HML')

	# Ice Age
	forgeSets.append('ICE')
	forgeSets.append('ALL')
	forgeSets.append('CSP')

	# Mirage
	forgeSets.append('MIR')
	forgeSets.append('VIS')
	forgeSets.append('WTH')

	# Rath Cycle
	forgeSets.append('TMP')
	forgeSets.append('STH')
	forgeSets.append('EXO')

	# Artifacts Cycle
	forgeSets.append('USG')
	forgeSets.append('ULG')
	forgeSets.append('UDS')

	# Masques
	forgeSets.append('MMQ')
	forgeSets.append('NMS')
	forgeSets.append('PCY')

	# Invasion
	forgeSets.append('INV')
	forgeSets.append('PLS')
	forgeSets.append('APC')

	# Odyssey
	forgeSets.append('ODY')
	forgeSets.append('TOR')
	forgeSets.append('JUD')

	# Onslaught
	forgeSets.append('ONS')
	forgeSets.append('LGN')
	forgeSets.append('SCG')

	# Mirrodin
	forgeSets.append('MRD')
	forgeSets.append('DST')
	forgeSets.append('5DN')

	# Kamigawa
	forgeSets.append('CHK')
	forgeSets.append('BOK')
	forgeSets.append('SOK')

	# Ravnica
	forgeSets.append('RAV')
	forgeSets.append('GPT')
	forgeSets.append('DIS')

	# Time Spiral
	forgeSets.append('TSP')
	forgeSets.append('TSB')
	forgeSets.append('PLC')
	forgeSets.append('FUT')

	# Lorwyn
	forgeSets.append('LRW')
	forgeSets.append('MOR')

	# Shadowmoor
	forgeSets.append('SHM')
	forgeSets.append('EVE')

	# Alara
	forgeSets.append('ALA')
	forgeSets.append('CFX')
	forgeSets.append('ARB')

	# Zendikar
	forgeSets.append('ZEN')
	forgeSets.append('WWK')
	forgeSets.append('ROE')

	# Scars of Mirrodin
	forgeSets.append('SOM')
	forgeSets.append('MBS')
	forgeSets.append('NPH')
	
	# Innistrad
	forgeSets.append('ISD')
	forgeSets.append('DKA')
	forgeSets.append('AVR')

if not os.path.exists(pathToMtgData) :
        print("This script requires the text version of Arch's mtg-data to be present.You can download it from slightlymagic.net's forum and either place the text version next to this script or edit this script and provide the path to the file at the top.")
        print("Press Enter to exit")
        raw_input("")
        sys.exit()

forgeSets = []
mtgDataCards = {}
setCodes = []
tmpName = ""
line = ""

# initialize sets supported by Forge
initSets()
#Parse mtg-data
mtgdata = open(pathToMtgData,"r")
line = mtgdata.readline()
# Read set codes at top of file
while line != "\n" and line != "":
        setCodes.append(line[0:3])
        line = mtgdata.readline()
# loop over remaining file parsing cards
while line:
        # Ignore blank lines
        while line == "\n" and line != "":
                line = mtgdata.readline()
        # process card data
        linesFound = 0
        foundCost = False
        foundType = False
        foundPT = False
        isPlaneswalker = False
        oracleText = ""
        prevLine = ""
        while line != "\n" and line != "":
                linesFound += 1
                tmpLine = line
                tmpLine = tmpLine.rstrip()
                # First line is always the name
                if linesFound == 1 :
                        mtgName = tmpLine
                        cardName = tmpLine.replace('AE', 'Ae')
                        card = Card(cardName)
                # Second line is either cost or type
                elif not foundCost :
                        if line[0] == '{' :
                                tmpLine = tmpLine.replace('}{',' ')
                                tmpLine = tmpLine.replace('{','')
                                tmpLine = tmpLine.replace('}','')
                                tmpLine = tmpLine.replace('/','')
                                card.cost = tmpLine
                        else :
                                card.cost = "no cost"
                                tmpLine = tmpLine.replace(' - ',' ');
                                card.types = tmpLine
                                foundType = True
                        foundCost = True
                elif not foundType :
                        tmpLine = tmpLine.replace(' - ',' ');
                        card.types = tmpLine
                        foundType = True
                elif not foundPT :
                        card.pt = tmpLine
                        foundPT = True
                else :
                        if prevLine != '' : card.oracle.append(prevLine)
                        prevLine = tmpLine.replace(mtgName,'CARDNAME')
                # if card is not creature, set foundPT to true
                if foundType and not foundPT :
                        if card.types.find('Creature') == -1 and card.types.find('Planeswalker') == -1: foundPT = True
                line = mtgdata.readline()
        # found blank line or end of file so store last line as set info
        card.sets = prevLine.rstrip()
        # store Card object in hash table
        mtgDataCards[cardName] = card
        
inputName = raw_input("Enter Card Name: ")
inputName = inputName.rstrip()
while inputName != 'quit' :
        if mtgDataCards.keys().count(inputName) != 0 :
                cardData = mtgDataCards[inputName]
                cleanName = cardData.name.lower()
                cleanName = cleanName.replace("'",'')
                cleanName = cleanName.replace(',','')
                cleanName = cleanName.replace(' ','_')
                print '\nName:'+cardData.name
                print 'ManaCost:'+cardData.cost
                print 'Types:'+cardData.types
                print 'Text:no text'
                if cardData.types.find('Creature') != -1:
                        print 'PT:'+cardData.pt
                elif cardData.types.find('Planeswalker') != -1 :
                        print 'Loyalty:'+cardData.pt
                print "\n<Script Start>"
                for text in cardData.oracle :
                        print text
                print "<Script End>\n"
                tmpSets = cardData.sets
                tmpSets = tmpSets.split(', ')
                setInfo = [];
                for edition in tmpSets :
                        edition = edition.split(' ');
                        if forgeSets.count(edition[0]) != 0 :
                                if edition[1] == 'C' :
                                        rarity = 'Common'
                                elif edition[1] == 'U' :
                                        rarity = 'Uncommon'
                                elif edition[1] == 'R' :
                                        rarity = 'Rare'
                                elif edition[1] == 'M' :
                                        rarity = 'Mythic'
                                setInfoStr = 'SetInfo:'+edition[0]+'|'+rarity+'|'+'http://dummy.com/dummy.jpg'
                                setInfo.append(setInfoStr)
                print 'SVar:Rarity:'+rarity
                print 'SVar:Picture:http://www.wizards.com/global/images/magic/general/'+cleanName+'.jpg'
                print 'End\n'
        else :
                print inputName+' not found\n'
        inputName = raw_input("Enter Card Name: ")
        inputName = inputName.rstrip()



