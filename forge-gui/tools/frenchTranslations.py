#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
  " Little script to scrap data from play-in.com for french translations.
"""
__author__ = 'add-le'

import requests
import signal
import json
import sys
import re

from bs4 import BeautifulSoup

file = open('../res/languages/cardnames-fr-FR.txt', 'r', encoding='utf-8')
lines = file.readlines()
file.close()

output = open('./cardnames-fr-FR-missing.txt', 'a', encoding='utf-8')
notfound = open('./cardnames-fr-FR-notfound.txt', 'a', encoding='utf-8')

# Scrap from play-in.com
url = 'https://www.play-in.com/recherche/result.php?s='

# Constants
SUCCESS_STATUS = 200

# Generate by the method mapAlphabet()
utils = {'A': 0, 'B': 1396, 'C': 2795, 'D': 4515, 'E': 5927, 'F': 6804, 'G': 7869, 'H': 9184, 'I': 10029, 'J': 10639, 'K': 10914, 'L': 11584, 'M': 12336, 'N': 13765, 'O': 14349, 'P': 14849, 'Q': 15970, 'R': 16063, 'S': 17391, 'T': 20691, 'U': 22167, 'V': 22485, 'W': 23145, 'X': 23978, 'Y': 23999, 'Z': 24099}

"""
  " Function to get all missing cards in the french translation
  " thanks to the scryfall bulk data.
"""
def getMissingCards():
	bulkfile = open('./scryfallcards.json', 'r', encoding='utf-8')
	bulk = json.load(bulkfile)
	bulkfile.close()

	allcards = open('./cardnames-fr-FR.txt', 'w', encoding='utf-8')
	for cards in bulk:
		allcards.writelines(cards['name'] + '|||\n')

	allcards.close()
	exit(0)


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
	alphaMap: dict = {'A': 0}
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
	exit(0)


def convertMana(cardInfo: str) -> str:
	"""Convert HTML tag to Forge MTG compatible tags."""
	# Replace all symbol by its correspondance
	symbol = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '100', '(', ')', '!']

	card: str = str(cardInfo)
	card = card.replace('\n', '')
	card = card.replace('<br/>', '\\n')
	card = card.replace('<br />', '\\n')
	card = card.replace('<br>', '\\n')
	card = card.replace('</img>', '')
	card = card.replace('<div class="txt">', '')
	card = card.replace('</div>', '')
	card = card.replace('/>', '')
	card = card.replace('>', '')

	for sym in symbol:
		card = card.replace('<img src="/img/symbole/mana/' + sym + '.png"', '{' + sym + '}')

	return card


"""
  " Get the differents part of the translatedInfo for a split card.
"""
def getSplittedInfo(cardInfo: str):
	cardSplitted = re.split(r'- - [- ]*', cardInfo)
	cardSplitted[0] = cardSplitted[0].replace('\\n', '')
	cardSplitted[1] = cardSplitted[1].replace('\\n', '')
	return [cardSplitted[0], cardSplitted[1]]


"""
  " Main function to scrap play-in website and create the translation unit.
"""
def scrap(url: str):
	# Request the website with the url of the card on play-in
	response = requests.get(url)
	try:
		# Use BeautifulSoup to scrap the html page
		soup = BeautifulSoup(response.content.decode('utf-8'), features='html.parser')
		# Check if the request returns HTTP Code OK
		if response.status_code == SUCCESS_STATUS:

			# Check if the card is found on the website
			title = soup.find('title').text
			if 'Résultat recherche' in title:
				 notfound.writelines(response.url + '\n')
				 return
			else:
				orignalName = title.split(' - ')[1]
				translatedName = title.split(' - ')[0]

			# If the english name and french name is the same
			if orignalName == 'Carte Magic The Gathering':
				orignalName = translatedName

			try:
				translatedGroup = list(soup.find('div', {'class': 'text_card text_fr txt_fr_right'}).children)
				translatedType = translatedGroup[0].text.replace(' ', ' ') # Replace nbsp to a regular space
				translatedInfo = convertMana(translatedGroup[6])

				# Is a split card or a double faced card?
				if '//' in orignalName:
					# Is a split card
					if '- - -' in translatedInfo:
						splittedInfo = getSplittedInfo(translatedInfo)
						if ' - ' in translatedType:
							output.writelines(orignalName.split(' // ')[0] + '|' + translatedName.split(' / ')[0] + '|' + translatedType.split(' - ')[0] + '|' + splittedInfo[0] + '\n')
							output.writelines(orignalName.split(' // ')[1] + '|' + translatedName.split(' / ')[1] + '|' + translatedType.split(' - ')[1] + '|' + splittedInfo[1] + '\n')
						else:
							output.writelines(orignalName.split(' // ')[0] + '|' + translatedName.split(' / ')[0] + '|' + translatedType + '|' + splittedInfo[0] + '\n')
							output.writelines(orignalName.split(' // ')[1] + '|' + translatedName.split(' / ')[1] + '|' + translatedType + '|' + splittedInfo[1] + '\n')
					else:
						# Is a double faced card
						output.writelines(orignalName.split(' // ')[0] + '|' + translatedName.split(' / ')[0] + '|' + translatedType + '|' + translatedInfo + '\n')
						translatedGroup = list(soup.find('div', {'class': 'text_card text_fr txt_fr_back img_hidden'}).children)
						translatedType = translatedGroup[3].text.split('\n')[0]
						translatedInfo = convertMana(translatedGroup[3])
						output.writelines(orignalName.split(' // ')[1] + '|' + translatedName.split(' / ')[1] + '|' + translatedType + '|' + translatedInfo + '\n')
				else:
					# Write the translated line in the missing file
					output.writelines(orignalName + '|' + translatedName + '|' + translatedType + '|' + translatedInfo + '\n')
			except AttributeError:
				notfound.writelines(response.url + '\n')
			except IndexError:
				notfound.writelines(response.url + '\n')
	except UnicodeDecodeError:
		notfound.writelines(response.url + '\n')


# Handler to Ctrl+C
def signal_handler(sig, frame):
	output.close()
	notfound.close()
	exit(0)

# Main program
if __name__ == "__main__":
	signal.signal(signal.SIGINT, signal_handler)
	# Manuel scrap
	if len(sys.argv) >= 2:
		if sys.argv[1] == '-m' or sys.argv[1] == '--map':
			mapAlphabet()

		if sys.argv[1] == '-f' or sys.argv[1] == '--find':
			findNewCards()

		if sys.argv[1] == '-g' or sys.argv[1] == '--get':
			getMissingCards()

		if sys.argv[1] == '-u' or sys.argv[1] == '--url':
			if len(sys.argv) <= 2:
				print('Missing second arg : url to scrap')
				exit(1)
			scrap(sys.argv[2])
	else:
		# Show only missing lines
		for line in lines:
			# None complete line
			if line.endswith('||\n'):
				engName = line.split('|')[0]

				# Check already done
				with open('cardnames-fr-FR-missing.txt', 'r', encoding='utf-8') as f:
					if engName in f.read():
						continue

				# Launch the scrapping on all missing translation cards
				scrap(url + engName)

	output.close()
	notfound.close()
	exit(0)
