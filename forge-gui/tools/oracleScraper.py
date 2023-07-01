import os
import requests

setName = 'ths'
nameStart = '<span style="font-size: 1.2em;">'
oracleStart = '<p class="ctext"><b>'
oracleEnd = '</b></p>'

def normalizeName(name):
	return name.lower().replace(',','').replace("'","").replace(' ', '_')

def normalizeOracle(oracle):
	return oracle.replace(u'\u2014', '-').replace(u'\u2018', "'")

r = requests.get('http://magiccards.info/query?v=spoiler&s=issue&q=++e:%s/en' % setName)
spl = r.text.split(nameStart)
spl.pop(0) # Get rid of all of the html that comes before our first card

for s in spl:
	# Extract name and oracle from magiccards.info
	name = s[1 + s.find(">"):s.find("</a>")]
	oracle = s[len(oracleStart)+s.find(oracleStart):s.find(oracleEnd)].replace('<br><br>', '\\n')
	norm = normalizeName(name)
	# Open relative cardsfolder
	path = os.path.join('..','res','cardsfolder', norm[0], norm+'.txt')

	hasOracle = False
	try:
		with open(path, 'r') as f:
			for line in f.readlines():
				hasOracle |= line.startswith("Oracle:")

		if not hasOracle:
			with open(path, "a") as f:
				f.write('\n')
				f.write(normalizeOracle(oracle))
			print '+ ',  norm
		else:
			print '= ', norm

	except:
		print '? ', norm