#!/usr/bin/env python3

import json
import os
import re
import urllib.request

# 'scryfall lang code':'ISO 639 lang code'
languages = {'es': 'es-ES', 'de': 'de-DE',
             'it': 'it-IT', 'zhs': 'zh-CN'}
langfiles = {'es': None, 'de': None, 'it': None, 'zhs': None}

# Request Scryfall API to download all_cards json file
request = urllib.request.urlopen('https://api.scryfall.com/bulk-data')
data = json.load(request)['data']
scryfalldburl = [x for x in data if x['type'] == 'all_cards'][0]['download_uri']
urllib.request.urlretrieve(scryfalldburl, 'cards.json')

# Sort file and remove duplicates


def cleanfile(filename, extension1, extension2):
    names_seen = set()
    outfile = open(filename + extension2, "w", encoding='utf8')
    with open(filename + extension1, "r", encoding='utf8') as r:
        for line in sorted(r):
            name = line.split('|')[0]
            if name not in names_seen:
                outfile.write(line)
                names_seen.add(name)
    outfile.close()
    os.remove(filename + extension1)

# Manual patch of file translations


def patchtranslations(filename):
    ffinal = open(filename + '.tmp3', 'w', encoding='utf8')

    try:
        open(filename + '-patch.txt', 'r', encoding='utf8').close()
    except FileNotFoundError:
        open(filename + '-patch.txt', 'w', encoding='utf8').close()

    # First patch all lines in original final that exists in patched file
    with open(filename + '.tmp2', 'r', encoding='utf8') as origfile:
        # For each line in original file
        for oline in origfile:
            oname = oline.split('|')[0]

            patchedline = ""

            # Check if that card is patched
            with open(filename + '-patch.txt', 'r', encoding='utf8') as patchfile:
                # For each line in patch file
                for pline in patchfile:
                    pname = pline.split('|')[0]

                    # If that card is patched
                    if oname == pname:
                        patchedline = pline
                        break

            if patchedline != "":
                ffinal.write(patchedline)
            else:
                ffinal.write(oline)

    origfile.close()
    patchfile.close()

    # Then add all patch new lines that doesn't exist in original final
    with open(filename + '-patch.txt', 'r', encoding='utf8') as patchfile:
        # For each line in patch file
        for pline in patchfile:
            pname = pline.split('|')[0]

            # Check if that patched card exists in original file

            with open(filename + '.tmp2', 'r', encoding='utf8') as origfile:
                found = False

                for oline in origfile:
                    oname = oline.split('|')[0]

                    # Patch line found in original file
                    if pname == oname:
                        found = True
                        break

                if found == False:
                    ffinal.write(pline)

    os.remove(filename + '.tmp2')
    origfile.close()
    patchfile.close()
    ffinal.close()

with open('cards.json', mode='r', encoding='utf8') as json_file:
    # todo:all cards json size >= 800MB,using json iteration library,avoid load all content in to memory.
    data = json.load(json_file)

    for lang in languages.keys():
        langfiles[lang] = open(
            'cardnames-{0}.tmp'.format(languages[lang]), 'w', encoding='utf8')

    for card in data:
        if card['lang'] in languages.keys():
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
                    #make zh-CN reminder text work
                    toracle = toracle.replace('（','(')
                    toracle = toracle.replace('）',')')
                    toracle = toracle.replace('|', 'VERT')
                except:
                    pass

                output = name + '|' + tname + '|' + ttype
                output = output + '|' + toracle
                output = output.replace('\n', '\\n')
                output = output + '\n'

                for lang in languages.keys():
                    if card['lang'] == lang:
                        langfiles[lang].write(output)

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
                    #make zh-CN reminder text work
                    toracle0 = toracle0.replace('（','(')
                    toracle0 = toracle0.replace('）',')')
                except:
                    pass

                try:
                    toracle1 = cardfaces[1]['printed_text']
                    #make zh-CN reminder text work
                    toracle1 = toracle1.replace('（','(')
                    toracle1 = toracle1.replace('）',')')
                except:
                    pass

                # Output Card0

                output0 = name0 + '|' + tname0 + '|' + ttype0
                output0 = output0 + '|' + toracle0
                output0 = output0.replace('\n', '\\n')

                for lang in languages.keys():
                    if card['lang'] == lang:
                        langfiles[lang].write(output0 + '\n')

                # Output Card1

                output1 = name1 + '|' + tname1 + '|' + ttype1
                output1 = output1 + '|' + toracle1
                output1 = output1.replace('\n', '\\n')

                for lang in languages.keys():
                    if card['lang'] == lang:
                        langfiles[lang].write(output1 + '\n')

    for lang in languages.keys():
        langfiles[lang].close()

# Sort file and remove duplicates
for lang in languages.keys():
    cleanfile("cardnames-{0}".format(languages[lang]), ".tmp", ".tmp2")

# Patch language files
for lang in languages.keys():
    patchtranslations("cardnames-{0}".format(languages[lang]))

# Sort file and remove duplicates
for lang in languages.keys():
    cleanfile("cardnames-{0}".format(languages[lang]), ".tmp3", ".txt")

# Call the Japanese translation script
# import JapaneseTranslations
# JapaneseTranslations.main()