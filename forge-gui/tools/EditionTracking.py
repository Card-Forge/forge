#!/usr/bin/env python

############IMPLEMENTATION FOLLOWS############
import json
import os,sys,fnmatch,re
import requests
import pdb

toolsDir = os.path.abspath(os.path.dirname( __file__ ))
resDir = os.path.abspath(os.path.join(toolsDir, '..', 'res'))
editionsDir = os.path.join(resDir, 'editions')
allJson = os.path.join(toolsDir, 'AllCards.json')
allJsonUrl = 'http://mtgjson.com/json/AllCards.json'

def initializeEditions():
	ignoredTypes = [ "From_the_Vault", "Duel_Decks", "Online", "Premium_Deck_Series" , "Funny" , "Promos"]
	ignoredBorders = [ "Silver" ]
	editionSections = [ "[cards]", "[precon product]", "[borderless]", "[showcase]", "[extended art]", "[buy a box]", "[promo]" ]

	print("Parsing Editions folder")
	for root, dirnames, filenames in os.walk(editionsDir):
		for fileName in fnmatch.filter(filenames, '*.txt'):
			#print "Parsing...", fileName
			hasSetNumbers = None
			with open(os.path.join(root, fileName)) as currentEdition:
				# Check all names for this card
				metadata = True
				setcode = setname = settype = border = None
				for line in currentEdition.readlines():
					line = line.strip()
					if metadata:
						if line in editionSections:
							metadata = False
							if setcode and setcode not in setCodes:
								setCodes.append(setcode)
								setCodeToName[setcode] = setname
								if settype in ignoredTypes or border in ignoredBorders:
									ignoredSet.append(setcode)

						elif line.startswith("Code="):
							setcode = line.split("=")[1].rstrip()

						elif line.startswith("Name="):
							setname = line.split("=")[1].rstrip()

						elif line.startswith("Type="):
							settype = line.split("=")[1].rstrip()

						elif line.startswith("Border="):
							border = line.split("=")[1].rstrip()

					else:
						if not line:
							continue

						if line.startswith("["):
							if line not in editionSections:
								metadata = True
							continue

						if line.startswith("#"):
							continue

						hasSetNumbers = line[0].isdigit()
						card = line.split(" ", 2 if hasSetNumbers else 1)[-1].rstrip().split('|')[0]

						if card.endswith('+'):
							card = card[:-1]

						if card not in mtgDataCards:
							#print card
							mtgDataCards[card] = [setcode]

						else:
							mtgDataCards[card].append(setcode)

	print("Total Cards Found in all editions", len(mtgDataCards))
	print("These sets will be ignored in some output files", ignoredSet)

def initializeOracleText():
	print("Initializing Oracle text")
	oracleDict = None
	if not os.path.exists(allJson):
		print("Need to download Json file...")
		r = requests.get(allJsonUrl)
		with open(allJson, 'w') as f:
			f.write(r.text.encode("utf-8"))

		oracleDict = r.json()

	else:
		with open(allJson) as f:
			oracleDict = json.loads(f.read())


	aes = [k for k in oracleDict.keys() if u'\xc6' in k or u'\xf6' in k]
	print("Normalizing %s names" % len(aes))
	for ae in aes:
		data = oracleDict.pop(ae)
		oracleDict[normalizeOracle(ae)] = data

	print("Found Oracle text for ", len(oracleDict))
	return oracleDict

def normalizeOracle(oracle):
	return oracle.replace(u'\u2014', '-').replace(u'\u2212', '-').replace(u'\u2018', "'").replace(u'\u201c', '"').replace(u'\u201d', '"').replace(u'\u2022', '-').replace(u'\xc6', 'AE').replace(u'\xf6', 'o').replace(u'\xb2', '^2').replace(u'\xae', '(R)').replace(u'\u221e', 'INF')


def initializeForgeCards():
	#Parse Forge
	print("Parsing Forge")
	cardsfolderLocation = os.path.join(resDir, 'cardsfolder')
	for root, dirnames, filenames in os.walk(cardsfolderLocation):
		for fileName in fnmatch.filter(filenames, '*.txt'):
			with open(os.path.join(root, fileName))  as currentForgeCard :
				# Check all names for this card
				name = ''
				split = False
				for line in currentForgeCard.readlines():
					if line.startswith("Name:"):
						if split:
							name += ' // '

						if not name or split:
							name += line[5:].rstrip().lower()

					elif line.startswith("AlternateMode") and 'Split' in line:
						split = True

				forgeCards.append(name)

def initializeFormats():
	formats = {}
	formatLocation = os.path.join(resDir, 'formats', 'Sanctioned')
	print("Looking for formats in ", formatLocation)
	for root, dirnames, filenames in os.walk(formatLocation):
		for fileName in fnmatch.filter(filenames, '*.txt'):
			if fileName not in ['Standard.txt', 'Modern.txt']:
				continue

			with open(os.path.join(root, fileName)) as formatFile:
				while formatFile:
					try:
						line = formatFile.readline().strip()
						if not line:
							# this should only happen when the file is done processing if we did things correctly?
							break
						if line == '[format]':
							line = formatFile.readline().strip()

						if line.startswith('Name:'):
							format = line.split(':')[1]
							formats[format] = {}
						else:
							break
					except:
						break

					# Pull valid sets
					while line != '':
						line = formatFile.readline().strip()
						if line.startswith('Sets:'):
							sets = line.split(':')[1]
							formats[format]['sets'] = sets.split(', ')
						elif line.startswith('Banned'):
							banned = line.split(':')[1]
							formats[format]['banned'] = banned.split('; ')

	return formats

def writeToFiles(text, files):
	for f in files:
		if f:
			f.write(text)

def printOverallEditions(totalDataList, setCodeToName, releaseFile=None):
	totalPercentage = 0
	totalMissing = 0
	totalImplemented = 0
	fullTotal = 0
	if releaseFile:
		releaseFile.write("[spoiler=Overall Editions]\n")
	with open(os.path.join(toolsDir, "EditionTrackingResults", "CompleteStats.txt"), "w") as statsfile:
		files = [statsfile, releaseFile]
		writeToFiles("Set: Implemented (Missing) / Total = Percentage Implemented\n", files)
		for k,dataKey in totalDataList :
			totalImplemented += dataKey[0]
			totalMissing += dataKey[1]
			fullTotal += dataKey[2]
			if dataKey[2] == 0:
				print("SetCode unknown", k)
				continue
			writeToFiles(setCodeToName[k].lstrip() + ": " + str(dataKey[0]) + " (" + str(dataKey[1]) + ") / " + str(dataKey[2]) + " = " + str(round(dataKey[3], 2)) + "%\n", files)
		totalPercentage = totalImplemented / fullTotal
		writeToFiles("\nTotal over all sets: " + str(totalImplemented) + " (" + str(totalMissing) + ") / " + str(fullTotal) + "\n", files)

	if releaseFile:
		releaseFile.write("[/spoiler]\n\n")

def printCardSet(implementedSet, missingSet, fileName, setCoverage=None, printImplemented=False, printMissing=True, releaseFile=None):
	# Add another file that will print out whichever set is requested
	# Convert back to lists so they can be sorted
	impCount = len(implementedSet)
	misCount = len(missingSet)
	totalCount = impCount + misCount
	print(fileName, "Counts: ", impCount, misCount, totalCount)

	if totalCount == 0:
		print("Something definitely wrong, why is total count 0 for ", fileName)
		return

	if releaseFile:
		releaseFile.write("[spoiler=%s]\n" % fileName)

	filePath = os.path.join(toolsDir, "EditionTrackingResults", fileName)
	with open(filePath, "w") as outfile:
		files = [outfile, releaseFile]
		if setCoverage:
			writeToFiles(' '.join(setCoverage), files)
			writeToFiles('\n', files)
		writeToFiles("Implemented (Missing) / Total = Percentage Implemented\n", files)
		writeToFiles("%d (%d) / %d = %.2f %%\n" % (impCount, misCount, totalCount, float(impCount)/totalCount*100), files)

		# If you really need to, we can print implemented cards
		if printImplemented:
			implemented = list(implementedSet)
			implemented.sort()
			outfile.write("\nImplemented (%d):" % impCount)
			for s in implemented:
				outfile.write("\n%s" % s)

		# By default Missing will print, but you can disable it
		if printMissing:
			missing = list(missingSet)
			missing.sort()
			writeToFiles("\nMissing (%d):" % misCount, files)
			for s in missing:
				writeToFiles("\n%s" % s, files)

		writeToFiles("\n", files)

	if releaseFile:
		releaseFile.write("[/spoiler]\n\n")

def printDistinctOracle(missingSet, fileName):
	filePath = os.path.join(toolsDir, "EditionTrackingResults", fileName)
	missing = list(missingSet)
	missing.sort()
	with open(filePath, "w") as outfile:
		for s in missing:
			if s:
				try:
					oracle = normalizeOracle(mtgOracleCards.get(s).get('text'))
					outfile.write("%s\n%s\n\n" % (s, oracle))
				except:
					outfile.write("%s\n\n" % (s))
					print("Failed to grab oracle for ", s)
		outfile.write("\n")


if __name__ == '__main__':
	if not os.path.isdir(toolsDir + os.sep + 'EditionTrackingResults') :
		os.mkdir(toolsDir + os.sep + 'EditionTrackingResults')

	ignoredSet = []
	forgeFolderFiles = []
	forgeCards = []
	mtgDataCards = {}
	setCodes = []
	setCodeToName = {}
	forgeCardCount = 0
	mtgDataCardCount = 0
	setCodeCount = 0

	hasFetchedSets = False
	hasFetchedCardName = False
	tmpName = ""
	line = ""
	prevline = ""

	# Initialize Editions
	initializeEditions()

	initializeForgeCards()
	mtgOracleCards = initializeOracleText()

	#Compare datasets and output results
	print("Comparing datasets and outputting results.")
	totalData = {}
	currentMissing = []
	currentImplemented = []
	allMissing = set()
	allImplemented = set()
	formats = initializeFormats()
	unknownFormat =  {'sets': []}

	standardSets = formats.get('Standard', unknownFormat)['sets']
	#print "Standard sets", standardSets, len(standardSets)
	standardMissing = set()
	standardImplemented = set()

	modernSets = formats.get('Modern', unknownFormat)['sets']
	modernMissing = set()
	modernImplemented = set()

	total = 0
	percentage = 0

	for currentSet in setCodes :
		# Ignore any sets that we don't tabulate
		if currentSet in ignoredSet: continue
		#print "Tabulating set", currentSet

		for key in mtgDataCards.keys():
			setList = mtgDataCards[key]
			if currentSet in setList:
				if key.lower() in forgeCards:
					currentImplemented.append(key)
				elif key != "":
					currentMissing.append(key)
		total = len(currentMissing)+len(currentImplemented)
		percentage = 0
		if total > 0 :
			percentage = (float(len(currentImplemented))/float(total))*100
		currentMissing.sort()
		currentImplemented.sort()

		# Output each edition file on it's own
		with open(toolsDir + os.sep + "EditionTrackingResults" + os.sep + "set_" + currentSet.strip() + ".txt", "w") as output :
			output.write("Implemented (" + str(len(currentImplemented)) + "):\n")
			for everyImplemented in currentImplemented :
				output.write(everyImplemented + '\n')
			output.write("\n")
			output.write("Missing (%s):\n" % len(currentMissing))
			for everyMissing in currentMissing :
				output.write(everyMissing + '\n')
				orc = mtgOracleCards.get(everyMissing)
				try:
					if 'manaCost' in orc:
						output.write(orc.get('manaCost') + '\n')

					#output.write(' '.join(orc.get('supertypes', [])))
					#output.write(' '.join(orc.get('types', [])))
					#output.write(' '.join(orc.get('subtypes', [])))
					output.write(normalizeOracle(orc.get('type')))
					output.write('\n')

					if 'power' in orc:
						output.write( "PT:" + orc.get('power') + '/' + orc.get('toughness') + '\n')
					if 'loyalty' in orc:
						output.write('Loyalty:' + orc.get('loyalty'))
				except Exception as e:
					print("some issue?", str(e))

				# Blah


				try:
					text = normalizeOracle(orc.get('text'))
				except:
					text = ''

				try:
					output.write(text + '\n\n')
				except:
					print(everyMissing)
			output.write("\n")
			output.write("Total: " + str(total) + "\n")
			output.write("Percentage implemented: " + str(round(percentage,2)) + "%\n")
		totalData[currentSet] = (len(currentImplemented),len(currentMissing),total,percentage)
		allMissing |= set(currentMissing)
		allImplemented |= set(currentImplemented)
		if currentSet in standardSets:
			#print "Found a standard set", currentSet
			standardMissing |= set(currentMissing)
			standardImplemented |= set(currentImplemented)
		if currentSet in modernSets:
			modernMissing |= set(currentMissing)
			modernImplemented |= set(currentImplemented)

		del currentMissing[:]
		del currentImplemented[:]

	#sort sets by percentage completed
	totalDataList = sorted(totalData.items(), key=lambda k: k[1][3], reverse=True)

	releaseOutput = open(os.path.join(toolsDir, "EditionTrackingResults", "ReleaseStats.txt"), "w")

	printCardSet(allImplemented, allMissing, "DistinctStats.txt", releaseFile=releaseOutput)
	printOverallEditions(totalDataList, setCodeToName, releaseFile=releaseOutput)
	printCardSet(standardImplemented, standardMissing, "FormatStandard.txt", setCoverage=standardSets, releaseFile=releaseOutput)
	printCardSet(modernImplemented, modernMissing, "FormatModern.txt", setCoverage=modernSets, releaseFile=releaseOutput)
	printDistinctOracle(allMissing, "DistinctOracle.txt")

	releaseOutput.close()

	print("Done!")
