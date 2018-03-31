#!/usr/bin/env python

############IMPLEMENTATION FOLLOWS############
import os,sys,fnmatch,re

# TODO Move these somewhere else?
ignoredSet = [ 'ASTRAL', 'ATH', 'BD', 'BR', 'CM1', 'DD2', 'DDC', 'DDD', 'DDE', 'DDF',
	'DDG', 'DDH', 'DDI', 'DDJ', 'DDK', 'DDL', 'DDM', 'DDN', 'DKM', 'DRB', 'DREAM', 'EVG', 'H09', 'MD1', 'ME2',
	'ME3', 'ME4', 'MED', 'PD2', 'PD3', 'SDC', 'UG', 'UGL', 'UNH', 'V09', 'V10', 'V11', 'V12',
	'V13', 'V14', '', 'DD3_DVD']

toolsDir = os.path.abspath(os.path.dirname( __file__ ))
resDir = os.path.abspath(os.path.join(toolsDir, '..', 'res'))
pathToMtgData = os.path.join(toolsDir, "mtg-data.txt")

def initializeFormats():
	formats = {}
	formatLocation = os.path.join(resDir, 'blockdata', 'formats.txt')
	print "Looking for formats in ", formatLocation
	with open(formatLocation) as formatFile:
		while formatFile:
			try:
				line = formatFile.readline().strip()
				if not line:
					# this should only happen when the file is done processing if we did things correctly?
					break

				format = line[1:-1]
				formats[format] = {}
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
					formats[format]['banned'] = sets.split('; ')

	#print formats
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
	with open(os.path.join(toolsDir, "PerSetTrackingResults", "CompleteStats.txt"), "w") as statsfile:
		files = [statsfile, releaseFile]
		writeToFiles("Set: Implemented (Missing) / Total = Percentage Implemented\n", files)
		for k,dataKey in totalDataList :
			totalImplemented += dataKey[0]
			totalMissing += dataKey[1]
			fullTotal += dataKey[2]
			if dataKey[2] == 0:
				print "SetCode unknown", k
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

	if releaseFile:
		releaseFile.write("[spoiler=%s]\n" % fileName)

	filePath = os.path.join(toolsDir, "PerSetTrackingResults", fileName)
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
	filePath = os.path.join(toolsDir, "PerSetTrackingResults", fileName)
	missing = list(missingSet)
	missing.sort()
	with open(filePath, "w") as outfile:
		for s in missing:
			if s:
				oracle = mtgOracleCards.get(s, "")
				outfile.write("%s\n%s" % (s, oracle))
		outfile.write("\n")


if __name__ == '__main__':
	if not os.path.exists(pathToMtgData) :
		print("This script requires the text version of Arch's mtg-data to be present.You can download it from slightlymagic.net's forum and either place the text version next to this script or edit this script and provide the path to the file at the top.")
		print("Press Enter to exit")
		raw_input("")
		sys.exit()

	if not os.path.isdir(toolsDir + os.sep + 'PerSetTrackingResults') :
		os.mkdir(toolsDir + os.sep + 'PerSetTrackingResults')

	forgeFolderFiles = []
	forgeCards = []
	mtgDataCards = {}
	mtgOracleCards = {}
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

	#Parse mtg-data
	print("Parsing mtg-data")
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
			if hasFetchedSets :
				if not hasFetchedCardName :
					tmpName = line.rstrip().replace("AE", "Ae")
					hasFetchedCardName = True
					oracle = ""

				else:
					oracle += line

				if line == "\n" :
					mtgOracleCards[tmpName] = oracle.replace(prevline, '')
					sets = prevline.split(", ")
					for i in range(len(sets)):
						sets[i] = sets[i].split(' ')[0]
					#print sets
					mtgDataCards[tmpName] = sets
					hasFetchedCardName = False

			prevline = line

	#Parse Forge
	print("Parsing Forge")
	cardsfolderLocation = os.path.join(resDir, 'cardsfolder')
	for root, dirnames, filenames in os.walk(cardsfolderLocation):
		for fileName in fnmatch.filter(filenames, '*.txt'):
			with open(os.path.join(root, fileName))  as currentForgeCard :
				# Check all names for this card
				for line in currentForgeCard.readlines():
					if line.startswith("Name:"):
						forgeCards.append(line[5:].replace("AE","Ae").rstrip())

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
	standardMissing = set()
	standardImplemented = set()

	modernSets = formats.get('Modern', unknownFormat)['sets']
	modernMissing = set()
	modernImplemented = set()

	#extendedSets = formats.get('Extended', unknownFormat)['sets']
	#extendedMissing = set()
	#extendedImplemented = set()

	total = 0
	percentage = 0

	for currentSet in setCodes :
		# Ignore any sets that we don't tabulate
		if currentSet in ignoredSet: continue
		for key in mtgDataCards.keys() :
			setList = mtgDataCards[key]
			if currentSet in setList:
				if key in forgeCards :
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
		with open(toolsDir + os.sep + "PerSetTrackingResults" + os.sep + "set_" + currentSet.strip() + ".txt", "w") as output :
			output.write("Implemented (" + str(len(currentImplemented)) + "):\n")
			for everyImplemented in currentImplemented :
				output.write(everyImplemented + '\n')
			output.write("\n")
			output.write("Missing (" + str(len(currentMissing)) + "):\n")
			for everyMissing in currentMissing :
				output.write(everyMissing + '\n')
				output.write(mtgOracleCards[everyMissing])
			output.write("\n")
			output.write("Total: " + str(total) + "\n")
			output.write("Percentage implemented: " + str(round(percentage,2)) + "%\n")
		totalData[currentSet] = (len(currentImplemented),len(currentMissing),total,percentage)
		allMissing |= set(currentMissing)
		allImplemented |= set(currentImplemented)
		if currentSet in standardSets:
			standardMissing |= set(currentMissing)
			standardImplemented |= set(currentImplemented)
		if currentSet in modernSets:
			modernMissing |= set(currentMissing)
			modernImplemented |= set(currentImplemented)
		#if currentSet in extendedSets:
		#	extendedMissing |= set(currentMissing)
		#	extendedImplemented |= set(currentImplemented)


		del currentMissing[:]
		del currentImplemented[:]

	#sort sets by percentage completed
	totalDataList = sorted(totalData.items(), key=lambda k: k[1][3], reverse=True)

	releaseOutput = open(os.path.join(toolsDir, "PerSetTrackingResults", "ReleaseStats.txt"), "w")

	printCardSet(allImplemented, allMissing, "DistinctStats.txt", releaseFile=releaseOutput)
	printOverallEditions(totalDataList, setCodeToName, releaseFile=releaseOutput)
	printCardSet(standardImplemented, standardMissing, "FormatStandard.txt", setCoverage=standardSets, releaseFile=releaseOutput)
	#printCardSet(extendedImplemented, extendedMissing, "FormatExtended.txt", setCoverage=extendedSets)
	printCardSet(modernImplemented, modernMissing, "FormatModern.txt", setCoverage=modernSets, releaseFile=releaseOutput)
	printDistinctOracle(allMissing, "DistinctOracle.txt")

	releaseOutput.close()

	print ("Done!")
