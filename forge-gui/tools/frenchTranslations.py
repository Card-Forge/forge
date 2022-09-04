#!/usr/bin/env python3
"""Little script to scrap data from play-in.com for french translations."""

__author__ = 'add-le'

import sys

import requests
import re

file = open('../res/languages/cardnames-fr-FR.txt', 'r')
lines = file.readlines()
file.close()

counter = 0

#Scrap from play-in.com
url = 'https://www.play-in.com/recherche/result.php?s='

#Constants
SUCCESS_STATUS = 200

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
	if sys.argv[1] == '-u' or sys.argv[1] == '-url':
		if len(sys.argv) <= 2:
			print('Missing second arg : url to scrap')
			exit(1)

		output = open('cardnames-fr-FR-missing.txt', 'a')

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

output = open('cardnames-fr-FR-missing.txt', 'a')

#Show only missing lines
for line in lines:
	#None complete line
	if line.endswith('||\n'):
		engName = line.split('|')[0]

		#Check already done
		with open('cardnames-fr-FR-missing.txt') as f:
			if engName in f.read():
				continue

		response = requests.get(url + engName)

		#Founded
		if response.url.endswith(engName) == False and response.status_code == SUCCESS_STATUS:
			#print(counter, end='\r', flush=True)

			#Get brut chaos the data from web page
			try:
				match = re.search(re.compile('<div class="text_card text_fr txt_fr_right"><div class="type">.+?<\/div><div class="clear"><\/div><div class="cout (1|hide)">.+?<\/div><div class="clear"><\/div><div class="txt">.+?<\/div>(<div class="forc_end">|<\/div>)'), response.content.decode('utf-8').replace('\n', '')).group()
				
				#Split in different tags, interesting data
				translatedName: str = re.findall(r'<title>.+?<\/title>', response.content.decode('utf-8').replace('\n', ''))[0].split('<title>')[1].split(' -')[0]
				translatedType: str = match.split('<div class="text_card text_fr txt_fr_right"><div class="type">')[1].split('</div>')[0]
				translatedInfo: str = convertMana(match.split('<div class="txt">')[1].split('</div>')[0])

				#print(engName + '|' + translatedName + '|' + translatedType.replace('&nbsp;', '') + '|' + translatedInfo)
				output.writelines(engName + '|' + translatedName + '|' + translatedType.replace('&nbsp;', '') + '|' + translatedInfo + '\n')
			except IndexError:
				#Request probably gives multiple value
				print(response.url)
			except AttributeError:
				#Request probably gives multiple value
				print(response.url)
			except UnicodeDecodeError:
				#Request probably gives multiple value
				print(response.url)

		counter += 1


output.close()
print('end: ', counter)
