#!/usr/bin/env python

pathToMtgData = "mtg-data.txt"

############IMPLEMENTATION FOLLOWS############
import os,sys,fnmatch
import re

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
	forgeSets.append('1E')
	forgeSets.append('2E')
	forgeSets.append('2U')
	forgeSets.append('3E')
	forgeSets.append('4E')
	forgeSets.append('5E')
	forgeSets.append('6E')
	forgeSets.append('7E')
	forgeSets.append('8ED')
	forgeSets.append('9ED')
	forgeSets.append('10E')
	forgeSets.append('M10')
	forgeSets.append('M11')
	forgeSets.append('M12')
	forgeSets.append('M13')

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
	forgeSets.append('AN')
	forgeSets.append('AQ')
	forgeSets.append('LE')
	forgeSets.append('DK')
	forgeSets.append('FE')
	forgeSets.append('HM')

	# Ice Age
	forgeSets.append('IA')
	forgeSets.append('AL')
	forgeSets.append('CSP')

	# Mirage
	forgeSets.append('MI')
	forgeSets.append('VI')
	forgeSets.append('WL')

	# Rath Cycle
	forgeSets.append('TE')
	forgeSets.append('ST')
	forgeSets.append('EX')

	# Artifacts Cycle
	forgeSets.append('UZ')
	forgeSets.append('GU')
	forgeSets.append('CG')

	# Masques
	forgeSets.append('MM')
	forgeSets.append('NE')
	forgeSets.append('PR')

	# Invasion
	forgeSets.append('IN')
	forgeSets.append('PS')
	forgeSets.append('AP')

	# Odyssey
	forgeSets.append('OD')
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
	forgeSets.append('CON')
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
	
	# Return to Ravnica
	forgeSets.append('RTR')
	#forgeSets.append('GTC')
	#forgeSets.append('UNK')

def initKeywords():
        keyWords.append('Cascade')
        keyWords.append('Convoke')
        keyWords.append('Deathtouch')
        keyWords.append('Defender')
        keyWords.append('Delve')
        keyWords.append('Desertwalk')
        keyWords.append('Double Strike')
        keyWords.append('Epic')
        keyWords.append('Exalted')
        keyWords.append('Fear')
        keyWords.append('First Strike')
        keyWords.append('Flanking')
        keyWords.append('Flash')
        keyWords.append('Flying')
        keyWords.append('Forestwalk')
        keyWords.append('Haste')
        keyWords.append('Hexproof')
        keyWords.append('Hideaway')
        keyWords.append('Horsemanship')
        keyWords.append('Indestructible')
        keyWords.append('Infect')
        keyWords.append('Intimidate')
        keyWords.append('Islandwalk')
        keyWords.append('Lifelink')
        keyWords.append('Living Weapon')
        keyWords.append('Mountainwalk')
        keyWords.append('Persist')
        keyWords.append('Phasing')
        keyWords.append('Plainswalk')
        keyWords.append('Provoke')
        keyWords.append('Reach')
        keyWords.append('Rebound')
        keyWords.append('Shadow')
        keyWords.append('Shroud')
        keyWords.append('Soulbond')
        keyWords.append('Storm')
        keyWords.append('Sunburst')
        keyWords.append('Swampwalk')
        keyWords.append('Trample')
        keyWords.append('Unblockable')
        keyWords.append('Undying')
        keyWords.append('Vigilance')
        keyWords.append('Wither')
        

def handleKeyords(line,keywords):
    # split line by spaces to see if first token matches a keyword
    line = line.rstrip();
    p = re.compile( '\s\(.*\)$')
    line = p.sub('',line)
    allKeywords = True
    if line.find('Enchant') != -1 :
        print 'K:'+line
        return allKeywords
    else :
        # Multiple keywords could be comma seperated in mtgdata
        words=line.split(', ')
        for token in words :
            if token.title() in keywords :
                print 'K:'+token.title()
            else :
                allKeywords = False

    return allKeywords
        
if not os.path.exists(pathToMtgData) :
        print("This script requires the text version of Arch's mtg-data to be present.You can download it from slightlymagic.net's forum and either place the text version next to this script or edit this script and provide the path to the file at the top.")
        print("Press Enter to exit")
        raw_input("")
        sys.exit()

forgeSets = []
keyWords = []
mtgDataCards = {}
setCodes = []
tmpName = ""
line = ""

# initialize sets supported by Forge
initSets()
initKeywords()
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
                cleanName = cleanName.replace('-','_')
                print '\nName:'+cardData.name
                print 'ManaCost:'+cardData.cost
                print 'Types:'+cardData.types
                print 'Text:no text'
                if cardData.types.find('Creature') != -1:
                        print 'PT:'+cardData.pt
                elif cardData.types.find('Planeswalker') != -1 :
                        print 'Loyalty:'+cardData.pt
                
                for text in cardData.oracle :
                        # do some prescripting
                        tokens = line.split(' ');
                        if text.find("When CARDNAME enters the battlefield") != -1 :
                                print "\n"+text
                                print "<Trigger Script Start>"
                                print 'T:Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Creature.Self | Execute$ <TriggerFunc> | TriggerDescription$ '+text
                                print 'SVar:<TriggerFunc>:AB$ <Added Triggered Ability HERE>'
                                print "<Trigger Script End>\n"
                        elif text.find("When CARDNAME leaves the battlefield") != -1 :
                                print "\n"+text
                                print "<Trigger Script Start>"
                                print 'T:Mode$ ChangesZone | Origin$ Battlefield | Destination$ Any | ValidCard$ Creature.Self | Execute$ <TriggerFunc> | TriggerDescription$ '+text
                                print 'SVar:<TriggerFunc>:AB$ <Added Triggered Ability HERE>'
                                print "<Trigger Script End>\n"
                        elif text.find("Unleash") != -1 :
                                print 'K:ETBReplacement:Other:Unleash:Optional'
                                print 'SVar:Unleash:DB$ PutCounter | Defined$ Self | CounterType$ P1P1 | CounterNum$ 1 | SpellDescription$ Unleash (You may have this creature enter the battlefield with a +1/+1 counter on it. It can\'t block as long as it has a +1/+1 counter on it.)'
                                print 'S:Mode$ Continuous | Affected$ Card.Self | AddHiddenKeyword$ HIDDEN CARDNAME can\'t block. | CheckSVar$ X | SVarCompare$ GE1 | References$ X'
                                print 'SVar:X:Count$NumCounters.P1P1'
                        else :
                                if handleKeyords(text,keyWords) == False:
                                        print text
                #print "\n"
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
