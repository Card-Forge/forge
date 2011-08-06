pathToMtgData = "mtg-data.txt"

############IMPLEMENTATION FOLLOWS############
import os,sys

if not os.path.exists(pathToMtgData) :
        print("This script requires the text version of Arch's mtg-data to be present.You can download it from slightlymagic.net's forum and either place the text version next to this script or edit this script and provide the path to the file at the top.")
        print("Press Enter to exit")
        raw_input("")
        sys.exit()

if not os.path.isdir(sys.path[0] + "\\PerSetTracking Results") :
        os.mkdir(sys.path[0] + "\\PerSetTracking Results")

forgeFolderContents = os.listdir(sys.path[0] + "\\cardsfolder")
forgeFolderFiles = []
forgeCards = []
mtgDataCards = {}
setCodes = []
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
                                setCodes.append(line[0:3])
                        else :
                                hasFetchedSets = True
                if hasFetchedSets :
                        if not hasFetchedCardName :
                                tmpName = line
                                hasFetchedCardName = True
                        if line == "\n" :
                                mtgDataCards[tmpName] = prevline
                                hasFetchedCardName = False

                prevline = line

#Parse Forge
print("Parsing Forge")
for i in forgeFolderContents :
        if os.path.isfile(sys.path[0] + "\\cardsfolder\\" + i) == True :
                forgeFolderFiles.append(i)
for file in forgeFolderFiles :
        with open(sys.path[0] + "\\cardsfolder\\" + file) as currentForgeCard :
                tmpname = currentForgeCard.readline()
                tmpname = tmpname[5:len(tmpname)].replace("AE","Ae")
                forgeCards.append(tmpname)
                
#Compare datasets and output results
print("Comparing datasets and outputting results.")
totalData = {}
currentMissing = []
currentImplemented = []
total = 0
percentage = 0
for currentSet in setCodes :
        for card in mtgDataCards.keys() :
                if mtgDataCards[card].count(currentSet) > 0 :
                        if card in forgeCards :
                                currentImplemented.append(card)
                        else :
                                currentMissing.append(card)
        total = len(currentMissing)+len(currentImplemented)
        percentage = 100        
        if total > 0 :
                percentage = (float(len(currentImplemented))/float(total))*100
        
		currentMissing.sort()
		currentImplemented.sort()
		
        with open(sys.path[0] + "\PerSetTracking Results\set_" + currentSet + ".txt","w") as output :
                output.write("Implemented (" + str(len(currentImplemented)) + "):\n")
                for everyImplemented in currentImplemented :
                        output.write(everyImplemented)
                output.write("\n")
                output.write("Missing (" + str(len(currentMissing)) + "):\n")
                for everyMissing in currentMissing :
                        output.write(everyMissing)
                output.write("\n")
                output.write("Total: " + str(total) + "\n")
                output.write("Percentage implemented: " + str(percentage) + "%\n")
        totalData[currentSet] = (len(currentImplemented),len(currentMissing),total,percentage)
        del currentMissing[:]
        del currentImplemented[:]

#UNTESTED sort sets by percentage completed
#totalData = sorted(totalData,key=lambda entry: entry[3],reverse=True)
#UNTESTED

totalPercentage = 0
totalMissing = 0
totalImplemented = 0
fullTotal = 0
with open(sys.path[0] + "\PerSetTracking Results\CompleteStats.txt","w") as statsfile:
        statsfile.write("Set: Implemented (Missing) / Total = Percentage Implemented\n")
        for dataKey in sorted(totalData.keys()) :
                totalImplemented += totalData[dataKey][0]
                totalMissing += totalData[dataKey][1]
                fullTotal += totalData[dataKey][2]
                statsfile.write(dataKey + ": " + str(totalData[dataKey][0]) + " (" + str(totalData[dataKey][1]) + ") / " + str(totalData[dataKey][2]) + " = " + str(round(totalData[dataKey][3])) + "%\n")
        totalPercentage = totalImplemented / fullTotal
        statsfile.write("\n")
        statsfile.write("Total over all sets: " + str(totalImplemented) + " (" + str(totalMissing) + ") / " + str(fullTotal))
        
print "Done!"
print "Press Enter to exit."
raw_input("")
