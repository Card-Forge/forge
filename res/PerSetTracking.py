#!/usr/bin/env python

pathToMtgData = "mtg-data.txt"

############IMPLEMENTATION FOLLOWS############
import os,sys,fnmatch,re

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
				sets = prevline.split(", ")
				for i in range(len(sets)):
					sets[i] = sets[i].split(' ')[0]
				#print sets
				mtgDataCards[tmpName] = sets
				hasFetchedCardName = False

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
total = 0
percentage = 0
for currentSet in setCodes :
	if currentSet == 'UNH' or currentSet == 'UGL' : continue #skip Unhinged and Unglued since they are only counting basic lands anyway
	if currentSet == 'VG1' or currentSet == 'VG2' or currentSet == 'VG3' : continue
	if currentSet == 'VG4' or currentSet == 'VGO' or currentSet == 'VG ' : continue
	if currentSet == 'FVD' or currentSet == 'FVE' or currentSet == 'FVR' : continue
	if currentSet == 'SDC' or currentSet == 'AST' or currentSet == 'DKM' : continue
	if currentSet == 'BTD' or currentSet == 'ARC' or currentSet == 'COM' : continue
	#if currentSet == 'CHR' or currentSet == 'MED' or currentSet == 'H09' : continue
	if currentSet == 'ME2' or currentSet == 'ME3' or currentSet == 'ME4' : continue
	if currentSet == 'ATH' or currentSet == 'HOP' or currentSet == 'BRB' : continue
	if currentSet == 'EVG' or currentSet == 'GVL' or currentSet == 'JVC' : continue
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

print "Done!"