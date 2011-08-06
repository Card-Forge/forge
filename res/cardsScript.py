# This python script is designed to handle the following: individual cards located in /res/cardsfolder
# Insert of rarity, picture, and sets of 

from httplib import HTTP
from urlparse import urlparse
from urllib import urlopen
import os

def clean(name):
	return name.lower().replace('-',' ').replace(',','').replace('_', ' ').replace('\'', '').replace('\"', '').replace('.', '').strip()

def checkURL(url): 
	p = urlparse(url) 
	h = HTTP(p[1]) 
	h.putrequest('HEAD', p[2]) 
	h.endheaders() 
	if h.getreply()[0] == 200: return 1 
	else: return 0 

def getURL(url): 
	return urlopen(url).read()
	
def getSVarString(line, str):
	start = line.find(str) + len(str) + 1
	return line[start:].strip()

def getRarity(name, html):
	# magiccards.info uses (<rarity>) on the page for rarity.
	# since older editions had funky things like c2, and u1, only use open parenthesis for this search
	landCount = html.count("(Land")
	commonCount = html.count("(Common")
	uncommonCount = html.count("(Uncommon")
	rareCount = html.count("(Rare") 
	mythicCount = html.count("(Mythic Rare)")
	if (landCount > 0):
		return 'Land'
	elif (commonCount + uncommonCount + rareCount + mythicCount == 0):
		err.write("Bad magiccards.info Query: " + name + '\n')
		return ''
	elif (commonCount >= uncommonCount and commonCount >= rareCount and commonCount >= mythicCount):
		return 'Common'
	elif (commonCount < uncommonCount and uncommonCount >= rareCount and uncommonCount >= mythicCount):
		return 'Uncommon'
	elif (rareCount >= mythicCount):
		return 'Rare'
	else:
		return 'Mythic'
	
def getPicture(name):
	urlName = name.replace(' ', '_')
	picUrl ='http://www.wizards.com/global/images/magic/general/' + urlName + '.jpg'
	if not checkURL(picUrl):
		err.write("Bad Picture URL " + name + '\n')
		return ''
	return picUrl
	
def getSets(name, html):
	delimitedStr = ''
	first = True;
	for s in sets:
		if (html.find(s + ' (') != -1) or (html.find(s + '</a> (') != -1):
			if not first:
				delimitedStr = delimitedStr + ','
			first = False
			delimitedStr = delimitedStr + sets[s]
	
	return delimitedStr
	
def initSets():
	# Base Sets
	sets['Limited Edition Beta'] = 'LEB'
	sets['Unlimited Edition'] = '2ED'
	sets['Revised Edition'] = '3ED'
	sets['Fourth Edition'] = '4ED'
	sets['Fifth Edition'] = '5ED'
	sets['Classic Sixth Edition'] = '6ED'
	sets['Seventh Edition'] = '7ED'
	sets['Eighth Edition'] = '8ED'
	sets['Ninth Edition'] = '9ED'
	sets['Tenth Edition'] = '10E'
	sets['Magic 2010'] = 'M10'
	sets['Magic 2011'] = 'M11'
	
	# Portal
	sets['Portal'] = 'POR'
	sets['Portal Second Age'] = 'P02'
	sets['Portal Three Kingdoms'] = 'PTK'
	
	# Starter
	sets['Starter 1999'] = 'S99'
	sets['Starter 2000'] = 'S00'
	
	# Early sets
	sets['Arabian Nights'] = 'ARN'
	sets['Antiquities'] = 'ATQ'
	sets['Legends'] = 'LEG'
	sets['The Dark'] = 'DRK'
	sets['Fallen Empires'] = 'FEM'
	sets['Homelands'] = 'HML'
	
	# Ice Age
	sets['Ice Age'] = 'ICE'
	sets['Alliances'] = 'ALL'
	sets['Coldsnap'] = 'CSP'
	
	# Mirage 
	sets['Mirage'] = 'MIR'
	sets['Visions'] = 'VIS'
	sets['Weatherlight'] = 'WTH'
	
	# Rath Cycle
	sets['Tempest'] = 'TMP'
	sets['Stronghold'] = 'STH'
	sets['Exodus'] = 'EXO'
	
	# Artifacts Cycle
	sets['Urza\'s Saga'] = 'USG'
	sets['Urza\'s Legacy'] = 'ULG'
	sets['Urza\'s Destiny'] = 'UDS'
	
	# Masques
	sets['Mercadian Masques'] = 'MMQ'
	sets['Nemesis'] = 'NMS'
	sets['Prophecy'] = 'PCY'
	
	# Invasion
	sets['Invasion'] = 'INV'
	sets['Planeshift'] = 'PLS'
	sets['Apocalypse'] = 'APC'
	
	# Odyssey
	sets['Odyssey'] = 'ODY'
	sets['Torment'] = 'TOR'
	sets['Judgment'] = 'JUD'
	
	# Onslaught
	sets['Onslaught'] = 'ONS'
	sets['Legions'] = 'LGN'
	sets['Scourge'] = 'SCG'
	
	# Mirrodin
	sets['Mirrodin'] = 'MRD'
	sets['Darksteel'] = 'DST'
	sets['Fifth Dawn'] = '5DN'
	
	# Kamigawa
	sets['Champions of Kamigawa'] = 'CHK'
	sets['Betrayers of Kamigawa'] = 'BOK'
	sets['Saviors of Kamigawa'] = 'SOK'
	
	# Ravnica
	sets['Ravnica: City of Guilds'] = 'RAV'
	sets['Guildpact'] = 'GPT'
	sets['Dissension'] = 'DIS'
	
	# Time Spiral
	sets['Time Spiral'] = 'TSP'
	sets['Planar Chaos'] = 'PLC'
	sets['Future Sight'] = 'FUT'
	
	# Lorwyn
	sets['Lorwyn'] = 'LRW'
	sets['Morningtide'] = 'MOR'
	
	# Shadowmoor
	sets['Shadowmoor'] = 'SHM'
	sets['Eventide'] = 'EVE'
	
	# Alara
	sets['Shards of Alara'] = 'ALA'
	sets['Conflux'] = 'CON'
	sets['Alara Reborn'] = 'ARB'
	
	# Zendikar
	sets['Zendikar'] = 'ZEN'
	sets['Worldwake'] = 'WWK'
	sets['Rise of the Eldrazi'] = 'ROE'
	
	# Scars of Mirrodin
	sets['Scars of Mirrodin'] = 'SOM'
	sets['Mirrodin Beseiged'] = 'MBS'
	#sets['Unknown Mirrodin Set']
	
class Card:
	def __init__(self, name, cleanName):
		self.name = name
		self.cleanName = cleanName
		self.hasPicture = False
		self.picURL = ''
		self.hasRarity = False
		self.rarity = ''
		self.hasSets = False
		self.sets = ''
		self.lines = ''

#get master card list and drop into a dictionary
folder = "cardsfolder"
err = open('cardsScript.log','w')
cardDict = {}
rarityStr = 'SVar:Rarity'
pictureStr = 'SVar:Picture'
setStr = 'SVar:Sets'
sets = {}
initSets()


for fileName in os.listdir(folder):
# parse cardsfolder for Card Lines and Rarity/Picture SVars. Filling in any gaps
	file = open(folder + '\\' + fileName)
	cleanName = fileName.replace('.txt', '')
	
	html = ''
	
	line = file.readline().strip()
	name = line.replace('Name:','')
	
	card = Card(name, cleanName)
	cardDict[cleanName] = card
	card.lines = line + '\n'
	
	line = file.readline().strip()

	while line != 'End':
		if line == '' or line == 'End':
			break
	
		if line.find(rarityStr) != -1:
			card.hasRarity = True
			card.rarity = getSVarString(line, rarityStr)
		elif line.find(pictureStr) != -1:
			card.hasPicture = True
			card.picURL = getSVarString(line, pictureStr)
		elif line.find(setStr) != -1:
			card.hasSets = True
			card.sets = getSVarString(line, setStr)
		else: 
			card.lines += line +'\n'
			
		line = file.readline().strip()
		
	if card.hasRarity and card.hasPicture and card.hasSets:
		err.write(card.name + '... No Changes' + '\n')
		continue
	
	if not card.hasRarity:
		html = getURL('http://magiccards.info/query?q=!'+card.name)
		rarity = getRarity(card.name, html)
		if not rarity == '':
			card.hasRarity = True
			card.rarity = rarity
			
	if not card.hasPicture:
		pic = getPicture(card.cleanName)
		if not pic == '':
			card.hasPicture = True
			card.picURL = pic
	
	if not card.hasSets:
		if html == '':
			html = getURL('http://magiccards.info/query?q=!'+card.name)
		allSets = getSets(card.name, html)
		if not allSets == '':
			card.hasSets = True
			card.sets = allSets
			
	file = open(folder + "/" + fileName, 'w')
	file.write(card.lines)
	if card.hasRarity:
		file.write('SVar:Rarity:'+card.rarity + '\n')
	if card.hasPicture:
		file.write('SVar:Picture:'+card.picURL + '\n')
	if card.hasSets:
		file.write('SVar:Sets:'+card.sets + '\n')
	file.write('End')
	file.close()
	err.write(card.name + '... Updated')


err.close()