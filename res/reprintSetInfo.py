#!/usr/bin/env python

# This script grabs the list of all cards in a set and clears out the setInfo
# After running this script, re-run setInfoScript to fill in the slots
# MAKE SURE THE setAbbr VARIABLE IS UPDATED TO THE SET YOU WANT TO CLEAR OUT

from httplib import HTTP
from urlparse import urlparse
from urllib import urlopen
import sys
import os

class SetInfo:
    def __init__(self, set, rarity, image):
        self.set = set
        self.rarity = rarity
        self.image = image

class Card:
    def __init__(self, name, cleanName):
        self.name = name
        self.cleanName = cleanName
        self.hasSet = False
        self.sets = {}

def clean(name):
    return name.replace(' ','_').replace('\'','').replace('-','_').replace('"','').replace(',','').lower()

def getCardsInSet():
    html = urlopen('http://magiccards.info/query?q=e:'+setAbbr+'&v=olist').read()

    start = html.find('<th><b>Card name</b></th>')
    end = html.find('</table>', start)
    block = html[start:end]

    while True:
        nameIndex = block.find('.html">')

        if nameIndex == -1:
            break

        nameEnd = block.find('<',nameIndex)

        name = block[nameIndex+7:nameEnd]

        # Add name to array
        nameList.append(clean(name)+'.txt')

        block = block[nameEnd:]

    return

folder = "cardsfolder"
err = open('reprintSetInfo.log','w')

# THIS NEEDS TO BE UPDATED TO THE SET YOU WANT TO UPDATE
# SOME ARE THREE LETTER ABBR. BUT SOME ARE JUST TWO. CHECK BEFORE YOU RUN!
print "Using Set: " + sys.argv[1]
setAbbr = sys.argv[1]

cardDict = {}
setStr = 'SetInfo:'
nameList = []
getCardsInSet()

for fileName in nameList:
    # if file doesn't exist continue
    filePath = os.path.join(folder, fileName)
    print filePath

    if os.path.isfile(filePath) == False:
        continue

    file = open(filePath)
    cleanName = fileName.replace('.txt', '')

    line = file.readline().strip()
    # Handle name and creation
    name = line.replace('Name:','')

    card = Card(name, cleanName)
    cardDict[cleanName] = card
    card.lines = line + '\n'

    # Start parsing the rest of the data file
    line = file.readline().strip()

    while line != 'End':
        # Skip empty lines
        if line == '':
            line = file.readline().strip()
            continue

        # We really shouldn
        if line == 'End':
            break

        # Skip SetInfo lines
        if line.find(setStr) == -1:
            card.lines += line +'\n'

        line = file.readline().strip()

    file = open(os.path.join(folder, fileName), 'w')
    file.write(card.lines)

    file.write('End')
    file.close()
    err.write(card.name + '... Updated\n')

err.close()
