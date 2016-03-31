# A simple tool to scrape written scripts from the forum, down to a folder
import requests
import lxml.html
import os

start = 0
incr = 15

# Update these variables, or even better convert to Arguments
url ='http://www.slightlymagic.net/forum/viewtopic.php?f=52&t=18242&start=%s'
txt = ''
folder = 'SOI'
cards = 0

def normalize_name(name):
	return name.lower().replace(' ', '_').replace("'", '').replace(u'\xc6', 'AE')

condition = True
try:
	os.mkdir(folder)
except:
	print folder, "already exists"

while condition:
	r = requests.get(url % start)
	condition = txt != r.text
	if not condition:
		break
	txt = r.text
	tree = lxml.html.fromstring(txt)
	elements = tree.findall('.//code')
	for e in elements:
		if not e.text.startswith('Name:'):
			continue

		script = lxml.html.tostring(e)[6:-7].replace('</a>', '').replace('&lt;','<').replace('&gt;', '>')
		while True:
			idx = script.find('<a ')
			if idx == -1:
				break
			backIdx = script.find('>', idx)
			script = script[0:idx] + script[backIdx+1:]

		lines = script.split('<br>')

		name = ''
		split = False
		for line in lines:
			if line.startswith("Name:"):
				if split:
					name += ' '

				if not name or split:
					name += line[5:].rstrip()
				
			elif line.startswith("AlternateMode") and 'Split' in line:
				split = True

		name = normalize_name(name)

		path = os.path.join(folder, name+'.txt')
		i = 1
		while os.path.isfile(path):
			path = os.path.join(folder, '%s%s.txt' % (name, i))
			i += 1

		with open(path, 'w') as f:
			for line in lines:
				f.write("%s\n" % line)

		cards += 1

	start += incr
	print ("About to loop...")
	raw_input("Press Enter to continue...")


print ("Done!")
raw_input("Press Enter to continue...")