#!/usr/bin/env python

import os,sys,fnmatch,re,string

#Use with caution, since it handles split cards incorrectly

pathToMtgData = os.path.join(sys.path[0], "mtg-data.txt")
pathToSetsMatchTable = os.path.join(sys.path[0], "mtgdata-sets-to-forge.txt")
pathToForgeSets = os.path.join(sys.path[0], '..', 'res', 'blockdata', "setdata.txt")
pathToForgeBoosters = os.path.join(sys.path[0], '..', 'res', 'blockdata', "boosters.txt")

class cis:      # CardInSet
	def __init__(self, Name, Rarity = None):
		self.rarity = "C" if Rarity is None else Rarity
		self.name = Name

	def __str__(self):
		return self.name + " | " + self.rarityFull()

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

	def rarityShort(self):
		return "L" if self.rarity == "B" else self.rarity


if __name__ == '__main__':
	if not os.path.exists(pathToMtgData) :
		print("This script requires the text version of Arch's mtg-data to be present.You can download it from slightlymagic.net's forum and either place the text version next to this script or edit this script and provide the path to the file at the top.")
		print("Press Enter to exit")
		raw_input("")
		sys.exit()

	setsMtgData = {}

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
					code = line[0:8].strip()
					if code != '':
						setsMtgData[code] = {}
						setsMtgData[code]["Name"] = line[20:].strip()
						setsMtgData[code]["Date"] = line[8:19].strip()
						setsMtgData[code]["Cards"] = []
						#print (setsMtgData[code])
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

				if line == "\n":

					
					sets = prevline.split(", ")
					hasFetchedCardName = False

					

					for i in range(len(sets)):
						ee = sets[i].split(' ')
						sc = setsMtgData[ee[0]]["Cards"]

						prints = int(ee[2][2:3]) if len(ee) > 2 else 1
						for x in range(0, prints):
							sc.append(cis(tmpName, ee[1].strip()))
			prevline = line

	
	print("Matching mtg-data and Forge sets")
	with open(pathToSetsMatchTable) as setsMatch :
		for line in setsMatch:
			if line[0:3] == "---":
				code = line[3:].split(" ")[0]
				setsMtgData.pop(code, None)

			elif line[0:3] == "===":
				code = line[3:].split(" ")[0] 
				# no action needed
			else:
				code1 = line.split(" ")[0]
				code2 = line.split(" ")[1]
				setsMtgData[code2] = setsMtgData.pop(code1, None)



			

	forgeSetData = {}
	
	
	print("Reading Forge sets masterdata")
	with open(pathToForgeSets) as forgesets :
		for line in forgesets :
			if line.strip() == '':
				continue
				
			thisSet = {}
			for kv in line.split("|"):
				dd = kv.split(":", 1)
				thisSet[dd[0]] = dd[1].strip()
			
			forgeSetData[thisSet["Code3"]] = thisSet
		

	with open(pathToForgeBoosters) as boostersFile:
		for line in boostersFile:
			kv = line.split(":", 1)
			arts = kv[1].split(",", 1)
			forgeSetData[kv[0]]["BoosterCovers"] = arts[0].strip()[0:1];
			forgeSetData[kv[0]]["Booster"] = arts[1].strip()


	for setCode in setsMtgData:
		mtgData = setsMtgData[setCode]
		
		
		forgeData = None if not setCode in forgeSetData else forgeSetData[setCode]
		
		valid_chars = "-_.() %s%s" % (string.ascii_letters, string.digits)
		fileName = ''.join(c for c in mtgData["Name"] if c in valid_chars) + ".txt"

		with open(sys.path[0] + os.sep + "sets" + os.sep + fileName, "w") as output :
			output.write("[metadata]\n")
			output.write("Code="+setCode+"\n")
			for k in mtgData:
				if ( k not in ["Cards"] ):
					output.write(k + "=" + mtgData[k] + "\n")
			if not forgeData is None:
				for k in forgeData:
					if ( k not in ["Code3", "Name"] ):
						output.write(k + "=" + forgeData[k] + "\n")

			output.write("\n[cards]\n")
			for cs in mtgData["Cards"]:
				output.write(cs.rarityShort() + " " + cs.name + "\n")
			
		
		#break

	sys.exit() 
	