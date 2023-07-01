import sys
import requests
import argparse

parser = argparse.ArgumentParser(description='Edition File Generator')

# -s for set code
parser.add_argument('-c', action='store', dest='setcode', help='Required setcode', default=None)
parser.add_argument('-n', action='store', dest='name', help='Name of edition', default='NEW SET XXX')
parser.add_argument('-t', action='store', dest='settype', help='Type of edition (Expansion, Duel_Decks, Other, etc)', default='Expansion')

result = parser.parse_args()

if result.setcode is None:
	print "Missing required set code. Please provide a -c command line argument.\n"
	print parser.parse_args(['-h'])
	sys.exit(1)

r = requests.get('http://mtgjson.com/v4/json/%s.json' % result.setcode)
d = r.json()
cards = d['cards']

f = open('%s.txt' % d['name'], 'w')

f.write('[metadata]\n')
f.write('Code=%s\n' % result.setcode)
f.write('Date=%s\n' % d['releaseDate'])
f.write('Name=%s\n' % d['name'])
f.write('Code2=%s\n' % result.setcode)
f.write('Type=%s\n\n' % d['type'])
f.write('[cards]\n')

for c in cards:
	l = []
	l.append(c['number']) 

	rarity = c['rarity'][0].upper()
	if rarity == 'C' and c['supertypes'].count('Basic') > 0:
		rarity = 'L'
	l.append(rarity)
	l.append(c['name'].replace(u'\xc6', 'Ae'))
	l.append('\n')
	f.write(' '.join(l))
