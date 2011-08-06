# This script parses through cards.txt and makes sure there are matching entries in card-pictures.txt and the different rarity files
# It appends any new entries to the bottom of the current lists. I(Sol) can fix this later
# Check for errors in cardsScript.log

# To Install Python check out: http://www.python.org/download/ The latest Python 2 should do the trick (2.7 as of July 27, 2010)
# Once installed, just run the script from the location. Double Click on PC. Or run through a command prompt.


from httplib import HTTP
from urlparse import urlparse
from urllib import urlopen

def clean(name):
	return name.lower().replace('-',' ').replace(',','').replace('_', ' ').replace('\'', '').replace('\"', '').replace('.', '').strip()
	
def getRarity(fileName, cardDict, quest):
	file = open(fileName)
	line = clean(file.readline())
	while line != '':
		if cardDict.has_key(line):
			if quest:
				cardDict[line].qRarity = True
			else:
				cardDict[line].rarity = True
		line = clean(file.readline())
	file.close()
	
def writeRarity(fileName, card):
	if card.rarity == False:
		rarity = open(fileName, 'a')
		rarity.write(card.name+'\n')
		rarity.close()
	if card.qRarity == False:
		rarity = open("quest/"+fileName, 'a')
		rarity.write(card.name+'\n')
		rarity.close()
	
def checkURL(url): 
	p = urlparse(url) 
	h = HTTP(p[1]) 
	h.putrequest('HEAD', p[2]) 
	h.endheaders() 
	if h.getreply()[0] == 200: return 1 
	else: return 0 

def getURL(url): 
	return urlopen(url).read()

	
class Card:
	def __init__(self, name):
		self.name = name
		self.cleanName = clean(name)
		self.picture = False
		self.rarity = False
		self.qRarity = False
		self.picURL = ''
	
	
#get master card list and drop into a dictionary
file = open('cards.txt')
line = file.readline().strip()
cardDict = {}

while line != 'End':
	temp = Card(line)
	cardDict[temp.cleanName] = temp
	skip = file.readline()
	while skip.strip() != '':
		skip = file.readline()
	line = file.readline().strip()

file.close()

#compare entires to card pictures
file = open('card-pictures.txt')

line = file.readline()
while line != '':
	start = line.find('.jpg')
	picName = clean(line[0:start])
	if cardDict.has_key(picName):
		cardDict[picName].picture = True
	line = file.readline()

file.close()

#compre to rarity fies
getRarity('common.txt', cardDict, False)
getRarity('uncommon.txt', cardDict, False)
getRarity('rare.txt', cardDict, False)
getRarity('quest/common.txt', cardDict, True)
getRarity('quest/uncommon.txt', cardDict, True)
getRarity('quest/rare.txt', cardDict, True)
	
#output
picFile = open('card-pictures.txt', 'a')
errFile = open('cardsScript.log', 'w')

cards = cardDict.values()
cards.sort()

for i in range(len(cards)):
	c = cards[i]
	# skip basic land
	if (c.name == "Swamp" or c.name == "Forest" or c.name == "Plains" or c.name == "Mountain" or c.name == "Island" or c.name == "Snow-Covered Swamp" or c.name == "Snow-Covered Forest" or c.name == "Snow-Covered Plains" or c.name == "Snow-Covered Mountain" or c.name == "Snow-Covered Island"):
		continue;
	
	if c.picture == False:
		urlName = c.cleanName.replace(' ', '_')
		picUrl ='http://www.wizards.com/global/images/magic/general/' + urlName + '.jpg'
		if checkURL(picUrl):
			c.picURL = picUrl
			picFile.write(urlName + '.jpg\t\t' + c.picURL + '\n')
		else:
			errFile.write("Bad Picture URL " + c.name + '\n')
			# generally for portal cards, where the primary URL doesn't work

	if c.rarity == False or c.qRarity == False:
		html = getURL('http://magiccards.info/query?q=!'+c.name)
		# magiccards.info uses (<rarity>) on the page for rarity.
		# since older editions had funky things like c2, and u1, only use open parenthesis for this search
		commonCount = html.count("(Common")
		uncommonCount = html.count("(Uncommon")
		rareCount = html.count("(Rare") + html.count("(Mythic Rare)")
		if (commonCount == 0 and uncommonCount == 0 and rareCount == 0):
			errFile.write("Bad magiccards.info Query: " + c.name + '\n')
		elif (commonCount >= uncommonCount and commonCount >= rareCount):
			writeRarity("common.txt", c)
		elif (commonCount < uncommonCount and uncommonCount >= rareCount):
			writeRarity("uncommon.txt", c)
		else:
			writeRarity("rare.txt", c)


picFile.close()
errFile.close()