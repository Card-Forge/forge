import argparse
import json
import requests


def draftsimRankings(edition='TDM', extra=None):
	edition = edition.upper()
	url1 = 'https://draftsim.com/generated/%s/' % (edition)
	url2 = '%s' % edition
	url = url1 + url2 + '.js'
	r = requests.get(url)
	tx = r.text
	start = tx.find('[')
	end = tx.rfind(']')
	# Deal with illegal JSON :(
	replaceList = ['name', 'castingcost1', 'castingcost2', 'type', 'rarity', 'myrating', 'image', "image2", 'cmc', 'colors', 'creaturesort', 'colorsort', 'chase_card']
	# Has an extra comma that json loader doesn't like
	txt = tx[start:end-1]+']'
	for rpl in replaceList:
		txt = txt.replace('%s:'%rpl, '"%s":'%rpl)

	txt2 = ""
	if extra:
		url3 = '%s' % extra
		urlx = url1 + url3 + '.js'
		x = requests.get(urlx)
		tx2 = x.text
		start = tx2.find('[')
		end = tx2.rfind(']')
		txt2 = tx2[start:end-1]+']'
		for rpl in replaceList:
			txt2 = txt2.replace('%s:'%rpl, '"%s":'%rpl)
		txt2 = txt2.replace('[', '', 1)
		trim = txt.rfind(']')
		txt = txt[:trim] + ', ' + txt[trim+1:]

	txt3 = txt + txt2
	txt3 = txt3.replace(u'\xa9', '')
	print(txt3)

	cardlist = json.loads(txt3)

	# remove duplicates
	unique_cards = dict()
	for card in cardlist:
		if card['name'] not in unique_cards:
			unique_cards[card['name']] = card

	cardlist = list(unique_cards.values())
	cardlist.sort(key=lambda k:k['myrating'], reverse=True)
	with open("../res/draft/rankings/" + edition.lower() + '.rnk', 'w') as out:
		out.write('//Rank|Name|Rarity|Set\n')
		for counter, card in enumerate(cardlist):
			l = [str(counter+1), card['name'].replace('_', ' '), card['rarity'], edition]
			out.write('#')
			out.write('|'.join(l))
			out.write('\n')


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

	parser.add_argument('-c', action='store', dest='setcode', help='Required setcode', required=True)
	parser.add_argument('-x', action='store', dest='altpage', help='Additional rankings page', required=False)

	result = parser.parse_args()

	draftsimRankings(result.setcode, result.altpage)
