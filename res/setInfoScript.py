#!/usr/bin/env python

# This python script is designed to handle the following: individual cards located in /res/cardsfolder
# Insert of SetInfo data into data files from magiccards.info

from httplib import HTTP
from urlparse import urlparse
from urllib import urlopen
import os,fnmatch

def getURL(url): 
	return urlopen(url).read()

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

def initSets():
	# Base Sets
	allSets['Limited Edition Alpha'] = 'LEA'
	allSets['Limited Edition Beta'] = 'LEB'
	allSets['Unlimited Edition'] = '2ED'
	allSets['Revised Edition'] = '3ED'
	allSets['Fourth Edition'] = '4ED'
	allSets['Fifth Edition'] = '5ED'
	allSets['Classic Sixth Edition'] = '6ED'
	allSets['Seventh Edition'] = '7ED'
	allSets['Eighth Edition'] = '8ED'
	allSets['Ninth Edition'] = '9ED'
	allSets['Tenth Edition'] = '10E'
	allSets['Magic 2010'] = 'M10'
	allSets['Magic 2011'] = 'M11'
	allSets['Magic 2012'] = 'M12'
	
	# Multiplayer Sets
	allSets['Commander'] = 'COM'
	
	# Portal
	allSets['Portal'] = 'POR'
	allSets['Portal Second Age'] = 'PO2'
	allSets['Portal Three Kingdoms'] = 'PTK'

	# Starter
	allSets['Starter 1999'] = 'S99'
	allSets['Starter 2000'] = 'S00'

	# Early Sets
	allSets['Arabian Nights'] = 'ARN'
	allSets['Antiquities'] = 'ATQ'
	allSets['Legends'] = 'LEG'
	allSets['The Dark'] = 'DRK'
	allSets['Fallen Empires'] = 'FEM'
	allSets['Homelands'] = 'HML'
	allSets['Chronicles'] = 'CHR'

	# Ice Age
	allSets['Ice Age'] = 'ICE'
	allSets['Alliances'] = 'ALL'
	allSets['Coldsnap'] = 'CSP'

	# Mirage 
	allSets['Mirage'] = 'MIR'
	allSets['Visions'] = 'VIS'
	allSets['Weatherlight'] = 'WTH'

	# Rath Cycle
	allSets['Tempest'] = 'TMP'
	allSets['Stronghold'] = 'STH'
	allSets['Exodus'] = 'EXO'

	# Artifacts Cycle
	allSets['Urza\'s Saga'] = 'USG'
	allSets['Urza\'s Legacy'] = 'ULG'
	allSets['Urza\'s Destiny'] = 'UDS'

	# Masques
	allSets['Mercadian Masques'] = 'MMQ'
	allSets['Nemesis'] = 'NMS'
	allSets['Prophecy'] = 'PCY'

	# Invasion
	allSets['Invasion'] = 'INV'
	allSets['Planeshift'] = 'PLS'
	allSets['Apocalypse'] = 'APC'

	# Odyssey
	allSets['Odyssey'] = 'ODY'
	allSets['Torment'] = 'TOR'
	allSets['Judgment'] = 'JUD'

	# Onslaught
	allSets['Onslaught'] = 'ONS'
	allSets['Legions'] = 'LGN'
	allSets['Scourge'] = 'SCG'

	# Mirrodin
	allSets['Mirrodin'] = 'MRD'
	allSets['Darksteel'] = 'DST'
	allSets['Fifth Dawn'] = '5DN'

	# Kamigawa
	allSets['Champions of Kamigawa'] = 'CHK'
	allSets['Betrayers of Kamigawa'] = 'BOK'
	allSets['Saviors of Kamigawa'] = 'SOK'

	# Ravnica
	allSets['Ravnica: City of Guilds'] = 'RAV'
	allSets['Guildpact'] = 'GPT'
	allSets['Dissension'] = 'DIS'

	# Time Spiral
	allSets['Time Spiral'] = 'TSP'
	allSets['Time Spiral "Timeshifted"'] = 'TSB'
	allSets['Planar Chaos'] = 'PLC'
	allSets['Future Sight'] = 'FUT'

	# Lorwyn
	allSets['Lorwyn'] = 'LRW'
	allSets['Morningtide'] = 'MOR'

	# Shadowmoor
	allSets['Shadowmoor'] = 'SHM'
	allSets['Eventide'] = 'EVE'

	# Alara
	allSets['Shards of Alara'] = 'ALA'
	allSets['Conflux'] = 'CFX'
	allSets['Alara Reborn'] = 'ARB'

	# Zendikar
	allSets['Zendikar'] = 'ZEN'
	allSets['Worldwake'] = 'WWK'
	allSets['Rise of the Eldrazi'] = 'ROE'

	# Scars of Mirrodin
	allSets['Scars of Mirrodin'] = 'SOM'
	allSets['Mirrodin Besieged'] = 'MBS'
	allSets['New Phyrexia'] = 'NPH'

	# Innistrad
	allSets['Innistrad']='ISD'
	allSets['Dark Ascension']='DKA'
	allSets['Avacyn Restored']='AVR'

def addSets(card):
	html = getURL('http://magiccards.info/query?q=!'+card.name)
	start = html.find('<br><u><b>Editions:</b></u><br>')
	end = html.find('<br><u><b>Languages:</b></u><br>', start)
	block = html[start:end]

	print card.name

	for edition in allSets.keys():
		edIndex = block.find('>'+edition+'<') # Portal/Mirrodin issue

		if edIndex == -1:   
			edIndex = block.find('>'+edition+' (') # Single set issue
		if edIndex == -1:
			continue

		# Scrape rarity
		rarityIndex = block.find('(',edIndex)
		rarity = block[rarityIndex+1:block.find(')',rarityIndex)]
		raritySpace = rarity.find(' ')
		if raritySpace != -1:
			rarity = rarity[0:raritySpace]  # For older cards

		# What to do with TimeShifted cards?
		if rarity == 'Special' and edition != 'Time Spiral "Timeshifted"':
			continue

		# Get setAbbreviation and setNumber
		dataIndex = block.rfind('"/',0,edIndex)
		data = block[dataIndex+2:edIndex-1] # 1 instead of 2 because of Portal/Mirrodin Issue

		splitData = data.split('/')
		setAbbr = splitData[0]
		setNum = splitData[2].replace('.html', '')

		if len(setNum) > 4:
			# Setnum not available here for most recent set. Switch to the .jpg used on page
			jpgIndex = html.find('.jpg')
			data = html[html.rfind('scans/en/', 0, jpgIndex):jpgIndex]

			# data = scans/en/[set]/[num]
			splitData = data.split('/')
			setAbbr = splitData[2]
			setNum = splitData[3]

		image = 'http://magiccards.info/scans/en/' + setAbbr + '/' + setNum + '.jpg'

		card.sets[allSets[edition]] = SetInfo(allSets[edition], rarity, image)

	return


#get master card list and drop into a dictionary
folder = "cardsfolder"
err = open('setInfoScript.log','w')
allSets = {}
initSets()
cardDict = {}
setStr = 'SetInfo:'

for root, dirnames, filenames in os.walk(folder):
	for fileName in fnmatch.filter(filenames, '*.txt'):
		if fileName.startswith('.'):
			continue

		# parse cardsfolder for Card Lines and Rarity/Picture SVars. Filling in any gaps
		file = open(os.path.join(root, fileName))
		cleanName = fileName.replace('.txt', '')

		line = file.readline().strip()
		# Handle name and creation
		name = line.replace('Name:','')

		card = Card(name.replace(' ','+'), cleanName) #This makes it work on Mac OS X.  Will test Windows and FreeBSD when I can.
		cardDict[cleanName] = card
		card.lines = line + '\n'

		# Start parsing the rest of the data file
		line = file.readline().strip()

		while line != 'End':
			# Skip empty lines
			if line == '':
				line = file.readline().strip()
				continue

			# We really shouldn't
			if line == 'End':
				break

			if line.find(setStr) != -1:
				info = line.replace('SetInfo:','')
				parts = info.split('|')

				card.hasSet = True
				card.sets[parts[0]] = SetInfo(parts[0], parts[1], parts[2])
			else: 
				card.lines += line +'\n'

			line = file.readline().strip()

		if not card.hasSet:
			addSets(card)
			card.hasSet = True

			file = open(os.path.join(root, fileName), 'w')
			file.write(card.lines)
			if card.hasSet:
				for s in card.sets.values():
					file.write('SetInfo:'+ s.set + '|' + s.rarity + '|' + s.image + '\n')

			file.write('End')
			
			err.write(card.name + '... Updated\n')
		
		file.close()
err.close()
