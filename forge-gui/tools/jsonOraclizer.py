#!/usr/bin/env python

# This python script is designed to handle the following: individual cards located in /res/cardsfolder/*
# Insert of Oracle data into data files from mtg-data.txt
# Future possibilities. Using mtg-data to add SetInfo data and other Outside Game Data (Type, PT, etc)
# Hopefully the oracleScript can replace both SetInfo Scripts by current SetInfo scripts by expanding their current functionality

# python oracleScript.py <offlineMode> <setAbbreviation>
# If you run oracleScript without parameters it will run for all sets on the local mtgdata.txt


import os, fnmatch, re, sys
import json
from urllib import urlopen

pathToMtgData = os.path.join(sys.path[0], "AllCards.json")

singleSet = False
onlineOptions = [ 'false', 'f', 'no', 'n' ]
offlineSource = True
setAbbr = None

if len(sys.argv) > 1:
	offlineSource = (sys.argv[1].lower() not in onlineOptions)
	print "Using AllCards.txt: " + str(offlineSource)

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
	parseFrom = open(pathToMtgData, 'r')
	load = json.loads(parseFrom.read())
	parseFrom.close()
	if singleSet:
		mtgData = {}
		for c in load['cards']:
			mtgData[c['name']] = c

	else:
		mtgData = load
	
	print "Number of cards loaded.. %s" % len(mtgData)
else:
	# Load Spoiler view of the set
	parseFrom = urlopen("http://magiccards.info/query?q=e:%s&v=spoiler&s=cname" % (setAbbr))
	mtgData = parseFrom.read()
	parseFrom.close()
	print "Size of parse data: %s" % len(mtgData)

folder = os.path.join(sys.path[0], '..', 'res', 'cardsfolder')
err = open(os.path.join(sys.path[0], 'jsonOraclizerLog.log'), 'w')

oracleStr = 'Oracle:'

def writeOutCard(root, fileName, lines, oracle):
	cardfile = open(os.path.join(root, fileName), 'w')
	cardfile.write(lines)

	cardfile.write('Oracle:%s\n' % oracle)
	cardfile.close()


def getOracleFromMtgData(name):
	data = mtgData.get(name, None)

	if data is None:
		err.write(name + '... NOT FOUND\n')
		return None

	txt = data.get('text', '').replace(u'\u2014', '-').replace(u'\u2022', '-').replace(u'\u2212', '-')
	return txt

def getOracleFromMagicCardsInfo(name):
	# Requires set to grab Oracle text from magiccards.info for simplicity meetings
	# http://magiccards.info/query?q=e%3Agtc&v=spoiler&s=cname
	search = '">%s</a>' % name
	found = mtgData.find(search)

	if found == -1:
		err.write(name + '... NOT FOUND\n')
		return None

	endFound = mtgData.find('</b></p>', found)
	block = mtgData[found:endFound]
	startOracle = '<p class="ctext"><b>'

	oracleStart = block.find(startOracle)
	oracleBlock = block[oracleStart:]
	oracle = oracleBlock[len(startOracle):].replace('<br><br>', '\\n')
	return oracle


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
			oracle = getOracleFromMtgData(name)
		else:
			oracle = getOracleFromMagicCardsInfo(name)

		if oracle is None:
			continue

		print name
		print " => %s \n" % (oracle)
		writeOutCard(root, fileName, lines, oracle)

		err.write(name + '... Updated\n')

err.close()
