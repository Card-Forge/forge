#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
  " Little script to scrap data from play-in.com for french translations.
"""
__author__ = 'add-le'

import sys
import requests
import re

file = open('../res/languages/cardnames-fr-FR.txt', 'r', encoding='utf-8')
lines = file.readlines()
file.close()

counter = 0

# Scrap from play-in.com
url = 'https://www.play-in.com/recherche/result.php?s='

# Constants
SUCCESS_STATUS = 200

# Generate by the method mapAlphabet()
utils = {'A': 0, 'B': 1275, 'C': 2541, 'D': 4121, 'E': 5443, 'F': 6195, 'G': 7172, 'H': 8389, 'I': 9169, 'J': 9712, 'K': 9948, 'L': 10570, 'M': 11248, 'N': 12564, 'O': 13110, 'P': 13575, 'Q': 14594, 'R': 14678, 'S': 15887, 'T': 18948, 'U':
20261, 'V': 20549, 'W': 21167, 'X': 21967, 'Y': 21983, 'Z': 22068}


"""
  " Function to find new cards after scryfall scrap and keep
  " old cards too with their translation.
"""
def findNewCards():
	# Read the scrap file
	scryfall = open('./cardnames-fr-FR.txt', 'r', encoding='utf-8')
	scraps = scryfall.readlines()
	scryfall.close()

	newcards = open('./cardnames-fr-FR-newcards.txt', 'a', encoding='utf-8')

	for scrap in scraps:
		# Start to read to the first letter of the word
		for line in lines[utils[scrap[0]]:]:
			# Test if we have pass the card
			if line[1] <= scrap[1] or scrap[1] == ' ' or line[1] == ' ':
				# The card is already present in the translation file
				if scrap.split("|")[0] == line.split("|")[0]:
					break
			else:
				# Card not found, probably a new card
				newcards.writelines(scrap)
				break

	newcards.close()
	exit(0)


"""
  " Function to map each letter of alphabet to the first time
  " encounter in the translated file.
"""
def mapAlphabet():
	alphabet = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z']
	alphaMap: dict = {"A": 0}
	index = 1

	for line in lines:
		if line[0] == alphabet[index]:
			# Store the line of the letter in the map
			alphaMap[alphabet[index]] = lines.index(line)
			# Reach the last letter 'Z'
			if index == 25:
				print(alphaMap)
				exit(0)
			else:
				# Change to next letter
				index = index + 1


def convertMana(cardInfo: str) -> str:
	"""Convert HTML tag to Forge MTG compatible tags."""
	cardInfo = cardInfo.replace('<br />', '\\n')

	#Replace all correspondances of [0-9A-Z]
	symbol = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '100', '(', ')']
	for sym in symbol:
		cardInfo = cardInfo.replace("<img src='/img/symbole/mana/" + sym + ".png' />", '{' + sym + '}')

	return cardInfo


#Manuel scrap
if len(sys.argv) >= 2:
	if sys.argv[1] == '-m' or sys.argv[1] == '--map':
		mapAlphabet()

	if sys.argv[1] == '-f' or sys.argv[1] == '--find':
		findNewCards()

	if sys.argv[1] == '-u' or sys.argv[1] == '--url':
		if len(sys.argv) <= 2:
			print('Missing second arg : url to scrap')
			exit(1)

		output = open('./cardnames-fr-FR-missing.txt', 'a', encoding='utf-8')

		response = requests.get(sys.argv[2])

		#Founded
		if response.status_code == SUCCESS_STATUS:
			#Get brut chaos the data from web page
			try:
				match = re.search(re.compile('<div class="text_card text_fr txt_fr_right"><div class="type">.+?<\/div><div class="clear"><\/div><div class="cout (1|hide)">.+?<\/div><div class="clear"><\/div><div class="txt">.+?<\/div>(<div class="forc_end">|<\/div>)'), response.content.decode('utf-8').replace('\n', '')).group()

				#Split in different tags, interesting data
				translatedName: str = re.findall(r'<title>.+?<\/title>', response.content.decode('utf-8').replace('\n', ''))[0].split('<title>')[1].split(' -')[0]
				orignalName: str = re.findall(r'<title>.+?<\/title>', response.content.decode('utf-8').replace('\n', ''))[0].split('<title>')[1].split(' -')[1].split(' -')[0]
				translatedType: str = match.split('<div class="text_card text_fr txt_fr_right"><div class="type">')[1].split('</div>')[0]
				translatedInfo: str = convertMana(match.split('<div class="txt">')[1].split('</div>')[0])

				#print(orignalName + '|' + translatedName + '|' + translatedType.replace('&nbsp;', '') + '|' + translatedInfo)
				output.writelines(orignalName + '|' + translatedName + '|' + translatedType.replace('&nbsp;', '') + '|' + translatedInfo + '\n')
			except IndexError:
				#Request probably gives multiple value
				print(response.url)
		output.close()
		exit(0)

output = open('./cardnames-fr-FR-missing.txt', 'a', encoding='utf-8')

# Show only missing lines
for line in lines:
	# None complete line
	if line.endswith('||\n'):
		engName = line.split('|')[0]

		# Check already done
		with open('cardnames-fr-FR-missing.txt', 'r', encoding='utf-8') as f:
			if engName in f.read():
				continue

		response = requests.get(url + engName)
		# Founded
		if response.url.endswith(engName) == False and response.status_code == SUCCESS_STATUS:
			#print(counter, end='\r', flush=True)

			# Get brut chaos the data from web page
			try:
				match = re.search(re.compile('<div class="text_card text_fr txt_fr_right"><div class="type">.+?<\/div><div class="clear"><\/div><div class="cout (1|hide)">.+?<\/div><div class="clear"><\/div><div class="txt">.+?<\/div>(<div class="forc_end">|<\/div>)'), response.content.decode('utf-8').replace('\n', '')).group()
				
				# Split in different tags, interesting data
				translatedName: str = re.findall(r'<title>.+?<\/title>', response.content.decode('utf-8').replace('\n', ''))[0].split('<title>')[1].split(' -')[0]
				translatedType: str = match.split('<div class="text_card text_fr txt_fr_right"><div class="type">')[1].split('</div>')[0]
				translatedInfo: str = convertMana(match.split('<div class="txt">')[1].split('</div>')[0])

				#print(engName + '|' + translatedName + '|' + translatedType.replace('&nbsp;', '') + '|' + translatedInfo)
				output.writelines(engName + '|' + translatedName + '|' + translatedType.replace('&nbsp;', '') + '|' + translatedInfo + '\n')
			except IndexError:
				# Request probably gives multiple value
				print(response.url)
			except AttributeError:
				# Request probably gives multiple value
				print(response.url)
			except UnicodeDecodeError:
				# Request probably gives multiple value
				print(response.url)

		counter += 1


output.close()
print('end: ', counter)
