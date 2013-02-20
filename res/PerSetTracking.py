#!/usr/bin/env python

pathToMtgData = "mtg-data.txt"

############IMPLEMENTATION FOLLOWS############
import os,sys,fnmatch,re

def getSetByFormat(requestedFormat):
	# Parse out Standard sets from the Format file
	formatLocation = os.path.join(sys.path[0], 'blockdata', 'formats.txt')
	with open(formatLocation) as formatFile:
		formats = formatFile.readlines()

		for format in formats:
			if requestedFormat not in format:
				continue
			parsed = format.split('|')
			for p in parsed:
				if not p.startswith('Sets:'):
					continue

				sets = p.strip().split(':')[1]
				return sets.split(', ')

	return []

def printCardSet(implementedSet, missingSet, fileName, setCoverage=None, printImplemented=False, printMissing=True):
	# Add another file that will print out whichever set is requested
	# Convert back to lists so they can be sorted
	impCount = len(implementedSet)
	misCount = len(missingSet)
	totalCount = impCount + misCount

	filePath = os.path.join(sys.path[0], "PerSetTrackingResults", fileName)
	with open(filePath, "w") as outfile:
		if setCoverage:
			outfile.write(' '.join(setCoverage))
			outfile.write('\n')
		outfile.write("Implemented (Missing) / Total = Percentage Implemented\n")
		outfile.write("%d (%d) / %d = %.2f %%\n" % (impCount, misCount, totalCount, float(impCount)/totalCount*100))

		# If you really need to, we can print implemented cards
		if printImplemented:
			implemented = list(implementedSet)
			implemented.sort()
			outfile.write("\nImplemented (%d):" % impCount)
			for s in implemented:
				outfile.write("\n%s" % s)
			outfile.write("\n")

		# By default Missing will print, but you can disable it
		if printMissing:
			missing = list(missingSet)
			missing.sort()
			outfile.write("\nMissing (%d):" % misCount)
			for s in missing:
				outfile.write("\n%s" % s)

def printDistinctOracle(missingSet, fileName):
	filePath = os.path.join(sys.path[0], "PerSetTrackingResults", fileName)
	missing = list(missingSet)
	missing.sort()
	with open(filePath, "w") as outfile:
		for s in missing:
			if s:
				oracle = mtgOracleCards.get(s, "")
				outfile.write("%s\n%s\n" % (s, oracle))


if __name__ == '__main__':
	if not os.path.exists(pathToMtgData) :
		print("This script requires the text version of Arch's mtg-data to be present.You can download it from slightlymagic.net's forum and either place the text version next to this script or edit this script and provide the path to the file at the top.")
		print("Press Enter to exit")
		raw_input("")
		sys.exit()

	if not os.path.isdir(sys.path[0] + os.sep + 'PerSetTrackingResults') :
		os.mkdir(sys.path[0] + os.sep + 'PerSetTrackingResults')

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
	twoPrior = ""

	#Parse mtg-data
	print("Parsing mtg-data")
	with open(pathToMtgData) as mtgdata :
		for line in mtgdata :
			if not hasFetchedSets :
				if line != "\n" :
					splitLine = line.split('  ') 
					code = splitLine[0]
					setCodeToName[code] = splitLine[-1].replace('\n', '')
					#print splitLine, code, setCodeToName[code]
					setCodes.append(code)
				else :
					hasFetchedSets = True

			if hasFetchedSets :
				if not hasFetchedCardName :
					tmpName = line.rstrip().replace("AE", "Ae")
					hasFetchedCardName = True

				if line == "\n" :
					mtgOracleCards[tmpName] = twoPrior
					sets = prevline.split(", ")
					for i in range(len(sets)):
						sets[i] = sets[i].split(' ')[0]
					#print sets
					mtgDataCards[tmpName] = sets
					hasFetchedCardName = False

			twoPrior = prevline
			prevline = line

	#Parse Forge
	print("Parsing Forge")
	for root, dirnames, filenames in os.walk("cardsfolder"):
		for fileName in fnmatch.filter(filenames, '*.txt'):
			with open(os.path.join(root, fileName))  as currentForgeCard :
				tmpname = currentForgeCard.readline()
				tmpname = tmpname[5:].replace("AE","Ae")
				tmpname = tmpname.rstrip()
				forgeCards.append(tmpname)

	#Compare datasets and output results
	print("Comparing datasets and outputting results.")
	totalData = {}
	currentMissing = []
	currentImplemented = []
	allMissing = set()
	allImplemented = set()
	standardMissing = set()
	standardImplemented = set()
	total = 0
	percentage = 0

	standardSets = getSetByFormat('Standard')

	ignoredSet = [ 'ASTRAL', 'ATH', 'BD', 'BR', 'CM1', 'DD2', 'DDC', 'DDD', 'DDE', 'DDF',
		'DDG', 'DDH', 'DDI', 'DDJ', 'DKM', 'DRB', 'DREAM', 'EVG', 'H09', 'ME2',
		'ME3', 'ME4', 'MED', 'PD2', 'PD3', 'SDC', 'UG', 'UGL', 'UNH', 
		'V09', 'V10', 'V11', 'V12', '']

	for currentSet in setCodes :
		# Ignore any sets that we don't tabulate
		if currentSet in ignoredSet: continue
		for key in mtgDataCards.keys() :
			setList = mtgDataCards[key]
			if currentSet in setList:
				if key in forgeCards :
					currentImplemented.append(key)
				else :
					currentMissing.append(key)
		total = len(currentMissing)+len(currentImplemented)
		percentage = 0       
		if total > 0 :
			percentage = (float(len(currentImplemented))/float(total))*100
		currentMissing.sort()
		currentImplemented.sort()

		with open(sys.path[0] + os.sep + "PerSetTrackingResults" + os.sep + "set_" + currentSet.strip() + ".txt", "w") as output :
			output.write("Implemented (" + str(len(currentImplemented)) + "):\n")
			for everyImplemented in currentImplemented :
				output.write(everyImplemented + '\n')
			output.write("\n")
			output.write("Missing (" + str(len(currentMissing)) + "):\n")
			for everyMissing in currentMissing :
				output.write(everyMissing + '\n')
			output.write("\n")
			output.write("Total: " + str(total) + "\n")
			output.write("Percentage implemented: " + str(round(percentage,2)) + "%\n")
		totalData[currentSet] = (len(currentImplemented),len(currentMissing),total,percentage)
		allMissing |= set(currentMissing)
		allImplemented |= set(currentImplemented)
		if currentSet in standardSets:
			standardMissing |= set(currentMissing)
			standardImplemented |= set(currentImplemented)

		del currentMissing[:]
		del currentImplemented[:]

	#sort sets by percentage completed
	totalDataList = sorted(totalData.items(), key=lambda (key,entry): entry[3], reverse=True)

	totalPercentage = 0
	totalMissing = 0
	totalImplemented = 0
	fullTotal = 0
	with open(sys.path[0] + os.sep + "PerSetTrackingResults" + os.sep + "CompleteStats.txt", "w") as statsfile:
		statsfile.write("Set: Implemented (Missing) / Total = Percentage Implemented\n")
		for k,dataKey in totalDataList :
			totalImplemented += dataKey[0]
			totalMissing += dataKey[1]
			fullTotal += dataKey[2]
			statsfile.write(setCodeToName[k].lstrip() + ": " + str(dataKey[0]) + " (" + str(dataKey[1]) + ") / " + str(dataKey[2]) + " = " + str(round(dataKey[3], 2)) + "%\n")
		totalPercentage = totalImplemented / fullTotal
		statsfile.write("\n")
		statsfile.write("Total over all sets: " + str(totalImplemented) + " (" + str(totalMissing) + ") / " + str(fullTotal))

	printCardSet(allImplemented, allMissing, "DistinctStats.txt")
	printCardSet(standardImplemented, standardMissing, "FormatStandard.txt", setCoverage=standardSets)
	printDistinctOracle(allMissing, "DistinctOracle.txt")

	print "Done!"