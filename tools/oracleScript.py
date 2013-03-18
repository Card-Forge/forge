#!/usr/bin/env python

# This python script is designed to handle the following: individual cards located in /res/cardsfolder/*
# Insert of Oracle data into data files from mtg-data.txt
# Future possibilities. Using mtg-data to add SetInfo data and other Outside Game Data (Type, PT, etc)
# Hopefully the oracleScript can replace both SetInfo Scripts by current SetInfo scripts by expanding their current functionality

# python oracleScript.py <offlineMode> <setAbbreviation>
# If you run oracleScript without parameters it will run for all sets on the local mtgdata.txt


import os, fnmatch, re, sys
from urllib import urlopen

onlineOptions = [ 'false', 'f', 'no', 'n' ]
offlineSource = True
setAbbr = None

if len(sys.argv) > 1:
    offlineSource = (sys.argv[1].lower() not in onlineOptions)
    print "Using mtgdata.txt: " + str(offlineSource)

if len(sys.argv) > 2:
    if offlineSource:
        print "Running for all sets when in Offline mode"
    else:
        setAbbr = sys.argv[2]
        print "Using Set: " + setAbbr

elif not offlineSource:
    print "Please provide a set abbreviation when in Online Mode. eg: python oracleScript.py False GTC"


mtgData = None
if offlineSource:
    parseFrom = open('mtg-data.txt', 'r')
else:
    # Load Spoiler view of the set
    parseFrom = urlopen("http://magiccards.info/query?q=e:%s&v=spoiler&s=cname" % (setAbbr))

mtgData = parseFrom.read()
parseFrom.close()
print "Size of parse data: %s" % len(mtgData)

folder = os.path.join(sys.path[0], '..', 'res', 'cardsfolder')
err = open('oracleScript.log', 'w')

setStr = 'SetInfo:'
oracleStr = 'Oracle:'

rarity = dict()
rarity['L'] = 'Land'
rarity['C'] = 'Common'
rarity['U'] = 'Uncommon'
rarity['R'] = 'Rare'
rarity['M'] = 'Mythic'

def writeOutCard(root, fileName, lines, oracle, sets):
    cardfile = open(os.path.join(root, fileName), 'w')
    cardfile.write(lines)

    cardfile.write('Oracle:%s\n' % oracle)

    '''
    # Disabled until we're ready to remove SetInfoUrl Parameter
    for i in sets:
        set = sets[i].lstrip()
        setInfo = set.split(' ')
        if len(setInfo) > 2:
            cardfile.write('SetInfo:%s|%s||%s\n' % (setInfo[0],setInfo[1],setInfo[2].replace('(x','').replace(')','')))
        else:
            cardfile.write('SetInfo:%s|%s|\n' % (setInfo[0],setInfo[1]))
    '''
    
    cardfile.close()


def getOracleFromMtgData(name):
    search = '\n%s\n' % name
    found = mtgData.find(search)

    if found == -1:
        err.write(name + '... NOT FOUND\n')
        return None, None

    endFound = mtgData.find('\n\n', found)

    block = mtgData[found+1:endFound]
    splitBlock = block.split('\n')
    typeLine = 2
    if splitBlock[1].find('{') == -1: # Has a Cost not a Land or Ancestral Vision
        typeLine = 1

    startOracle = typeLine + 1
    if splitBlock[typeLine].find('Creature') > -1 or splitBlock[typeLine].find('Planeswalker') > -1:
        # Power/toughness or loyalty adds an additional line to skip
        startOracle = startOracle + 1

    # \n needs to appear in the Oracle line
    oracle = '\\n'.join(splitBlock[startOracle:-1])

    sets = splitBlock[-1]

    return oracle, sets

def getOracleFromMagicCardsInfo(name):
    # Requires set to grab Oracle text from magiccards.info for simplicity meetings
    # http://magiccards.info/query?q=e%3Agtc&v=spoiler&s=cname
    search = '">%s</a>' % name
    found = mtgData.find(search)

    if found == -1:
        err.write(name + '... NOT FOUND\n')
        return None, None

    endFound = mtgData.find('</b></p>', found)
    block = mtgData[found:endFound]
    startOracle = '<p class="ctext"><b>'

    oracleStart = block.find(startOracle)
    oracleBlock = block[oracleStart:]
    oracle = oracleBlock[len(startOracle):].replace('<br><br>', '\\n')
    return oracle, None


def hasOracleLine(cardFile, lines, offlineSource=True):
    # Start parsing the rest of the data file
    hasOracle = False
    
    for line in cardFile.readlines():
        line = line.strip()
        # Skip empty lines
        if line == '':
            continue

        if line.find(oracleStr) != -1:
            hasOracle = True
            break

        # Disabled until we're ready to remove SetInfoUrl Parameter
        #elif line.find(setStr) != -1 and offlineSource:
        #    pass
        else:
            lines += line + '\n'

    cardFile.close()
    return hasOracle, lines

# parse cardsfolder for Card Lines and Rarity/Picture SVars. Filling in any gaps
for root, dirnames, filenames in os.walk(folder):
    for fileName in fnmatch.filter(filenames, '*.txt'):
        if fileName.startswith('.'):
            continue
		
        file = open(os.path.join(root, fileName), 'r')
        cleanName = fileName.replace('.txt', '')

        line = file.readline().strip()
        # Handle name and creation
        name = line.replace('Name:', '')

        hasOracle, lines = hasOracleLine(file, line + '\n', offlineSource)

        if hasOracle:
            #print name + " already has Oracle"
            continue

        if offlineSource:
            oracle, sets = getOracleFromMtgData(name)
        else:
            oracle, sets = getOracleFromMagicCardsInfo(name)

        if oracle is None:
            continue

        print "%s => %s \n" % (name, oracle)
        writeOutCard(root, fileName, lines, oracle, sets)

        err.write(name + '... Updated\n')

err.close()
