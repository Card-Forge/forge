import sys
import requests
import argparse
import BeautifulSoup

BESTIAIRE = False
SMDS = True

def bestiaireRanking(code='ORI', name='Magic Origins'):
	# POST http://draft.bestiaire.org/ranking.php
	# Params:
	# edition: ORI
	data, code = idToNameLoops(name, code)
	#r = requests.post("http://draft.bestiaire.org/ranking.php", data={'edition': code})
	pass
	# Output to file

def smdsRankings(edition='MAGICORIGINS', name='Magic Origins'):
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
		bestiaireRanking(result.setcode, result.name)

	else:
		smdsRankings(result.setcode, result.name)