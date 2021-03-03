import json
import sys
import requests
import argparse
import BeautifulSoup

BESTIAIRE = False
SMDS = True

def bestiaireRanking(code='EMN', name='Eldritch Moon'):
	# POST http://draft.bestiaire.org/ranking.php
	# Params:
	# edition: ORI
	data, code = idToNameLoops(name, code)
	#r = requests.post("http://draft.bestiaire.org/ranking.php", data={'edition': code})
	pass
	# Output to file

def smdsRankings(edition='EldritchMoon', name='Eldritch Moon'):
	# get http://syunakira.com/smd/pointranking/index.php?packname=MAGICORIGINS&language=English
	r = requests.get("http://syunakira.com/smd/pointranking/index.php?packname=%s&language=English" % edition)
	bs = BeautifulSoup.BeautifulSoup(r.text)
	images = bs.findAll('img')
	cards = []
	data, code = idToNameLoops(name)
	with open(name + ' Rankings.txt', 'w') as out:
		# Output to file
		out.write('//Rank|Name|Rarity|Set\n')
		#    #1|Sphinx of the Final Word|M|OGW

		c = 1
		for i in images:
			img = i.get('src')
			idx = img.find(edition)
			if idx > -1:
				parsed = img[idx:-4].split("/")[-1]
				if parsed in data:
					# Basic lands are weird
					card = data[parsed]
					l = [str(c), card['name'], card['rarity'], code]
					c += 1
					out.write('#')
					out.write('|'.join(l))
					out.write('\n')

	return True
	
def draftsimRankings(edition='KHM', name='Kalheim'):
	r = requests.get("http://draftsim.com/generated/%s.js" % edition)
	tx = r.text
	start = tx.find('[')
	end = tx.rfind(']')

	# Deal with illegal JSON :(
	replaceList = ['name', 'castingcost1', 'castingcost2', 'type', 'rarity', 'myrating', 'image', 'cmc', 'colors', 'creaturesort', 'colorsort', 'chase_card']
	# Has an extra comma that json loader doesn't like
	txt = tx[start:end-1]+']'
	for rpl in replaceList:
		txt = txt.replace('%s:'%rpl, '"%s":'%rpl)


	cardlist = json.loads(txt)
	cardlist.sort(key=lambda k:k['myrating'], reverse=True)
	with open(name + '_Rankings.txt', 'w') as out:
		for counter, card in enumerate(cardlist):
			l = [str(counter+1), card['name'].replace('_', ' '), card['rarity'], edition]
			out.write('#')
			out.write('|'.join(l))
			out.write('\n')

def editionsRankssim(edition='SOI', name='Shadows over Innistrad'):
	r = requests.get("http://draftsim.com/%s.js" % edition)
	tx = r.text
	start = tx.find('[')
	end = tx.rfind(']')

	# Deal with illegal JSON :(
	replaceList = ['name', 'castingcost1', 'castingcost2', 'type', 'rarity', 'myrating', 'image', 'cmc', 'colors', 'creaturesort', 'colorsort']
	# Has an extra comma that json loader doesn't like
	txt = tx[start:end-1]+']'
	for rpl in replaceList:
		txt = txt.replace('%s:'%rpl, '"%s":'%rpl)

	cardlist = json.loads(txt)
	out = open('%s.txt' % name, 'w')
	for idx, card in enumerate(cardlist):
		out.write('%s %s %s\n' % (idx+1, card['rarity'], card['name']))
	out.close()


def idToNameLoops(name, code=None):
	metadata = True
	data = {}
	with open("../res/editions/%s.txt" % name) as edition:
		for line in edition.readlines():
			line = line.strip()
			if not metadata:
				d = line.split(' ', 2)
				data[d[0]] = { 'rarity': d[1], 'name': d[2]}
			elif line.startswith('[cards]'):
				metadata = False
			elif code is None and line.startswith('Code='):
				code = line[5:]

	#print data
	return data, code


if __name__ == "__main__":
	parser = argparse.ArgumentParser(description='Edition File Generator')

	# TODO Split setcode and smds "name"
	parser.add_argument('-c', action='store', dest='setcode', help='Required setcode', required=True)
	parser.add_argument('-n', action='store', dest='name', help='Required Name of edition', required=True)

	result = parser.parse_args()

	if len(result.setcode) < 4:
		draftsimRankings(result.setcode, result.name)

	else:
		smdsRankings(result.setcode, result.name)
