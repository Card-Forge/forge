from httplib import HTTP
from urlparse import urlparse
from urllib import urlopen

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

def getRarity(name):
	html = getURL('http://magiccards.info/query?q=!'+name)
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
	
class Card:
	def __init__(self, name):
		self.name = name
		self.cleanName = clean(name)
		self.hasPicture = False
		self.picURL = ''
		self.hasRarity = False
		self.rarity = ''
		self.lines = ''

#get master card list and drop into a dictionary
file = open('cards.txt')
err = open('cardsScript.log','w')
line = file.readline().strip()
cardDict = {}
rarityStr = 'SVar:Rarity'
pictureStr = 'SVar:Picture'

# parse cards.txt for Card Lines and Rarity/Picture SVars. Filling in any gaps
while line != 'End':
	temp = Card(line)
	cardDict[temp.cleanName] = temp
	skip = file.readline()
	while skip.strip() != '':
		if skip.find(rarityStr) != -1:
			temp.hasRarity = True
			temp.rarity = getSVarString(skip, rarityStr)
		elif skip.find(pictureStr) != -1:
			temp.hasPicture = True
			temp.picURL = getSVarString(skip, pictureStr)
		else: 
			temp.lines += skip
		skip = file.readline()
	
	if not temp.hasRarity:
		rarity = getRarity(temp.name)
		if not rarity == '':
			temp.hasRarity = True
			temp.rarity = rarity
			
	if not temp.hasPicture:
		pic = getPicture(temp.cleanName)
		if not pic == '':
			temp.hasPicture = True
			temp.picURL = pic
	
	line = file.readline().strip()

file.close()

file = open('cards.txt', 'w')

for c in sorted(cardDict):
	card = cardDict[c]
	file.write(card.name+'\n')
	file.write(card.lines)
	if card.hasRarity:
		file.write('SVar:Rarity:'+card.rarity + '\n')
	if card.hasPicture:
		file.write('SVar:Picture:'+card.picURL + '\n')
	file.write('\n')

file.write('End')
file.close()
err.close()