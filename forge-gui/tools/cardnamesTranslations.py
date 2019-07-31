import json
import os
import re
import urllib.request

database = 'scryfall-all-cards.json'
scryfalldburl = 'https://archive.scryfall.com/json/' + database
languages = ['es', 'de']

urllib.request.urlretrieve(scryfalldburl, database)

def oracleformat(oracle):
    oracle = re.sub(r"[)]([A-Z,{,•,-])", ")\n\1", toracle)
    oracle = re.sub(r"(.)[\.]([A-Z,{,•,-])", "\1.\n\2", toracle)
    return oracle

with open(database, mode='r', encoding='utf8') as json_file:
    data = json.load(json_file)

    feses = open('cardnames-es-ES.tmp', 'w', encoding='utf8')
    fdede = open('cardnames-de-DE.tmp', 'w', encoding='utf8')

    for card in data:
        if card['lang'] in languages:
            try:
                name = card['name']
            except:
                pass

            # Parse simple card
            if ' // ' not in name:
                tname = ttype = toracle = ''

                try:
                    tname = card['printed_name']
                except:
                    pass

                try:
                    ttype = card['printed_type_line']
                except:
                    pass

                try:
                    toracle = card['printed_text']
                except:
                    pass

                output = name + '|' + tname + '|' + ttype

                # Format oracle
                if toracle != "":
                    toracle = oracleformat(toracle)
                
                output = output + '|' + toracle
                output = output.replace('\n', '\\n')

                if card['lang'] == 'es':
                    feses.write(output + '\n')
                if card['lang'] == 'de':
                    fdede.write(output + '\n')

            # Parse double card
            else:
                tname0 = tname1 = ttype0 = ttype1 = toracle0 = toracle1 = ''

                cardfaces = card['card_faces']

                try:
                    name0 = cardfaces[0]['name']
                except:
                    pass

                try:
                    name1 = cardfaces[1]['name']
                except:
                    pass

                try:
                    tname0 = cardfaces[0]['printed_name']
                except:
                    pass

                try:
                    tname1 = cardfaces[1]['printed_name']
                except:
                    pass

                try:
                    ttype0 = cardfaces[0]['printed_type_line']
                except:
                    pass

                try:
                    ttype1 = cardfaces[1]['printed_type_line']
                except:
                    pass

                try:
                    toracle0 = cardfaces[0]['printed_text']
                except:
                    pass

                try:
                    toracle1 = cardfaces[1]['printed_text']
                except:
                    pass
                
                # Output Card0

                output0 = name0 + '|' + tname0 + '|' + ttype0

                # Format oracle for card0
                if toracle0 != "":
                    toracle0 = oracleformat(toracle0)
                
                output0 = output0 + '|' + toracle0
                output0 = output0.replace('\n', '\\n')

                if card['lang'] == 'es':
                    feses.write(output0 + '\n')
                if card['lang'] == 'de':
                    fdede.write(output0 + '\n')

                # Output Card1
                
                output1 = name1 + '|' + tname1 + '|' + ttype1

                # Format oracle for card0
                if toracle1 != "":
                    toracle1 = oracleformat(toracle1)
                
                output1 = output1 + '|' + toracle1
                output1 = output1.replace('\n', '\\n')

                if card['lang'] == 'es':
                    feses.write(output1 + '\n')
                if card['lang'] == 'de':
                    fdede.write(output1 + '\n')
    
    feses.close()
    fdede.close()

# Remove duplicates
names_seen = set()
outfile = open("cardnames-es-ES.txt", "w", encoding='utf8')
for line in open("cardnames-es-ES.tmp", "r", encoding='utf8'):
    name = line.split('|')[0]
    if name not in names_seen:
        outfile.write(line)
        names_seen.add(name)
outfile.close()
os.remove("cardnames-es-ES.tmp")

# Remove duplicates
names_seen = set()
outfile = open("cardnames-de-DE.txt", "w", encoding='utf8')
for line in open("cardnames-de-DE.tmp", "r", encoding='utf8'):
    name = line.split('|')[0]
    if name not in names_seen:
        outfile.write(line)
        names_seen.add(name)
outfile.close()
os.remove("cardnames-de-DE.tmp")