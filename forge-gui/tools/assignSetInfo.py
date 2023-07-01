#!/usr/bin/env python

import os,sys,fnmatch,re

pathToMtgData = os.path.join(sys.path[0], "mtg-data.txt")
pathToSetsMatchTable = os.path.join(sys.path[0], "mtgdata-sets-to-forge.txt")

class cis:      # CardInSet
	def __init__(self):
		self.rarity = "C"
		self.arts = 0

	def __str__(self):
		return self.rarityFull() if self.arts <= 1 else "{0} x{1}".format(self.rarityFull(), self.arts)

	def __repr__(self):
		return self.__str__()

	def rarityFull(self):
		if (self.rarity == "B"):
			return "Land"
		if (self.rarity == "R"):
			return "Rare"
		if (self.rarity == "U"):
			return "Uncommon"
		if (self.rarity == "S"):
			return "Special"
		if (self.rarity == "M"):
			return "Mythic"
		return "Common"


if __name__ == '__main__':
	if not os.path.exists(pathToMtgData) :
		print("This script requires the text version of Arch's mtg-data to be present.You can download it from slightlymagic.net's forum and either place the text version next to this script or edit this script and provide the path to the file at the top.")
		print("Press Enter to exit")
		raw_input("")
		sys.exit()

	setCodes = []
	setCodeToName = {}
	setCodeToForge = {}
	mtgDataCards = {}


	hasFetchedSets = False
	hasFetchedCardName = False
	tmpName = ""
	line = ""
	prevline = ""

	#Parse mtg-data
	print("Parsing mtg-data...")
	with open(pathToMtgData) as mtgdata :
		for line in mtgdata :
			# Parse the sets at the top of the mtgdata file
			if not hasFetchedSets :
				if line != "\n" :
					splitLine = line.split('  ') 
					code = splitLine[0]
					setCodeToName[code] = splitLine[-1].replace('\n', '')
					#print splitLine, code, setCodeToName[code]
					setCodes.append(code)
				else :
					hasFetchedSets = True

			# Once all sets are parsed, time to parse the cards
			elif hasFetchedSets :
				if not hasFetchedCardName :
					tmpName = line.rstrip()
					hasFetchedCardName = True
					oracle = ""

				else:
					oracle += line

				if line == "\n" :
					#mtgOracleCards[tmpName] = oracle.replace(prevline, '')

					sets = prevline.split(", ")
					editions = {}
					for i in range(len(sets)):
						ee = sets[i].split(' ')

						setName = ee[0]
						if not setName in editions:
							editions[setName] = cis()

						editions[setName].rarity = ee[1].strip()
						prints = int(ee[2][2:3]) if len(ee) > 2 else 1

						editions[setName].arts += prints
					#print sets
					mtgDataCards[tmpName] = editions
					hasFetchedCardName = False

			prevline = line


	print("Matching mtg-data and Forge sets")
	with open(pathToSetsMatchTable) as setsMatch :
		for line in setsMatch:
			if line[0:3] == "---":
				code = line[3:].split(" ")[0]
				setCodeToForge[code] = None

			elif line[0:3] == "===":
				code = line[3:].split(" ")[0]				
				setCodeToForge[code] = code;
			else:
				code1 = line.split(" ")[0]
				code2 = line.split(" ")[1]
				setCodeToForge[code1] = code2


	folder = os.path.join(sys.path[0], '..', 'res', 'cardsfolder')
	for root, dirnames, filenames in os.walk(folder):
		for fileName in fnmatch.filter(filenames, '*.txt'):
			if fileName.startswith('.'):
				continue
			
			cardfile = open(os.path.join(root, fileName), 'r')

			firstLine = cardfile.readline().strip()
			cardName = firstLine[5:]
			altName = None

			previousLines = []
			previousLines.append(firstLine)

			validLines = []
			validLines.append(firstLine)

			for line in cardfile.readlines(): 
				previousLines.append(line.strip())
				# Just in case SVar:Rar is used as a legitimate SVar
				if not line.startswith("SetInfo:") and not line.startswith("SVar:Rarity:"):
					validLines.append(line.strip())

				if line.startswith("Name:"):
					altName = line[5:].strip()
			cardfile.close()

			if not cardName in mtgDataCards and not altName is None:
				cardName = altName

			for e in mtgDataCards[cardName]:
				if setCodeToForge[e] is not None:
					validLines.append( "SetInfo:{0} {1}".format(setCodeToForge[e], mtgDataCards[cardName][e]))

			if previousLines == validLines:
				continue

			print (cardName, altName, fileName)

			toWrite = "\n".join(validLines)

			cardfile = open(os.path.join(root, fileName), 'w')
			cardfile.write(toWrite)
			cardfile.close();
		
