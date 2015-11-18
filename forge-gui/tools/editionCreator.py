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

r = requests.get('http://mtgjson.com/json/%s.json' % result.setcode)
d = r.json()
cards = d['cards']

f = open('%s.txt' % result.name, 'w')

f.write('[metadata]\n')
f.write('Code=%s\n' % result.setcode)
f.write('Date=2015-XX-XX\n')
f.write('Name=%s\n' % result.name)
f.write('Code2=%s\n' % result.setcode)
f.write('Type=%s\n\n' % result.settype)
f.write('[cards]\n')

for c in cards:
	rarity = c['rarity'][0]
	if rarity == 'B':
		rarity = 'L'
	f.write(rarity + ' ' + c['name'].replace(u'\xc6', 'AE') + '\n')