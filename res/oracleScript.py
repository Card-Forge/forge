#!/usr/bin/env python

# This python script is designed to handle the following: individual cards located in /res/cardsfolder/*
# Insert of Oracle data into data files from mtg-data.txt
# Future possibilities. Using mtg-data to add SetInfo data and other Outside Game Data (Type, PT, etc)
# Hopefully the oracleScript can replace both SetInfo Scripts by current SetInfo scripts by expanding their current functionality

import os, fnmatch, re

class Card:
    def __init__(self, name, cleanName):
        self.name = name
        self.cleanName = cleanName

#get master card list and drop into a dictionary
folder = "cardsfolder"
parseFrom = open('mtg-data.txt', 'r')
mtgData = parseFrom.read()
parseFrom.close()
err = open('oracleScript.log', 'w')
allSets = {}
cardDict = {}
setStr = 'SetInfo:'
oracleStr = 'Oracle:'

rarity = dict()
rarity['L'] = 'Land'
rarity['C'] = 'Common'
rarity['U'] = 'Uncommon'
rarity['R'] = 'Rare'
rarity['M'] = 'Mythic'

# parse cardsfolder for Card Lines and Rarity/Picture SVars. Filling in any gaps
for root, dirnames, filenames in os.walk(folder):
    for fileName in fnmatch.filter(filenames, '*.txt'):
        if fileName.startswith('.'):
            continue
        
        hasOracle = False
		
        file = open(os.path.join(root, fileName))
        cleanName = fileName.replace('.txt', '')

        line = file.readline().strip()
        # Handle name and creation
        name = line.replace('Name:', '')
        search = '\n%s\n' % name
        found = mtgData.find(search)

        if found == -1:
            err.write(cleanName + '... NOT FOUND\n')
            continue

        endFound = mtgData.find('\n\n', found)

        block = mtgData[found+1:endFound]
        splitBlock = block.split('\n')
        typeLine = 2
        if splitBlock[1].find('{') == -1: # Has a Cost not a Land or Ancestral Vision
            typeLine = 1

        startOracle = typeLine + 1
        if splitBlock[typeLine].find('Creature') > -1 or splitBlock[typeLine].find('Planeswalker') > -1:
            startOracle = startOracle + 1

        # \n needs to appear in the Oracle line
        oracle = '\\n'.join(splitBlock[startOracle:-1])

        sets = splitBlock[-1]

        card = Card(name.replace(' ', '+'), cleanName)
        cardDict[cleanName] = card
        card.lines = line + '\n'

        # Start parsing the rest of the data file
        line = file.readline().strip()

        while line != 'End':
            # Skip empty lines
            if line == '':
                line = file.readline().strip()
                continue

            if line.find(oracleStr) != -1:
                hasOracle = True
                break

            # Disabled until we're ready to remove SetInfoUrl Parameter
            #elif line.find(setStr) != -1:
            #    pass
            else:
                card.lines += line + '\n'

            line = file.readline().strip()

        file.close()

        if hasOracle:
            continue
		
        print "%s => %s \n" % (name, oracle)
        
        file = open(os.path.join(root, fileName), 'w')
        file.write(card.lines)

        file.write('Oracle:%s\n' % oracle)

        '''
        # Disabled until we're ready to remove SetInfoUrl Parameter
        for i in sets:
            set = sets[i].lstrip()
            setInfo = set.split(' ')
            if len(setInfo) > 2:
                file.write('SetInfo:%s|%s||%s\n' % (setInfo[0],setInfo[1],setInfo[2].replace('(x','').replace(')','')))
            else:
                file.write('SetInfo:%s|%s|\n' % (setInfo[0],setInfo[1]))
		'''
				
        file.write('End')
        file.close()

        err.write(card.name + '... Updated\n')

err.close()
