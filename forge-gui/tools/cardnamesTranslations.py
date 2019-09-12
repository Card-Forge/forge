#!/usr/bin/env python3

import json
import os
import re
import urllib.request

database = 'scryfall-all-cards.json'
scryfalldburl = 'https://archive.scryfall.com/json/' + database
# 'scryfall lang code':'ISO 639 lang code'
languages = {'es': 'es-ES', 'de': 'de-DE',
             'zhs': 'zh-CN'}
langfiles = {'es': None, 'de': None, 'zhs': None}

urllib.request.urlretrieve(scryfalldburl, database)

# Sort file and remove duplicates


def cleanfile(filename):
    names_seen = set()
    outfile = open(filename + ".tmp2", "w", encoding='utf8')
    with open(filename + ".tmp", "r", encoding='utf8') as r:
        for line in sorted(r):
            name = line.split('|')[0]
            if name not in names_seen:
                outfile.write(line)
                names_seen.add(name)
    outfile.close()
    os.remove(filename + ".tmp")

# Manual patch of file translations


def patchtranslations(filename):
    ffinal = open(filename + '.txt', 'w', encoding='utf8')
    try:
        fpatch = open(filename + '-patch.txt', 'r', encoding='utf8')
    except FileNotFoundError:
        open(filename + '-patch.txt', 'w', encoding='utf8').close()
        fpatch = open(filename + '-patch.txt', 'r', encoding='utf8')

    patchline = fpatch.readline()

    with open(filename + '.tmp2', 'r', encoding='utf8') as temp:
        for templine in temp:
            tempname = templine.split('|')[0]
            patchname = patchline.split('|')[0]
            if patchname == tempname:
                ffinal.write(patchline)
                patchline = fpatch.readline()
            else:
                ffinal.write(templine)

    ffinal.close()
    fpatch.close()
    os.remove(filename + '.tmp2')


with open(database, mode='r', encoding='utf8') as json_file:
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
                except:
                    pass

                try:
                    toracle1 = cardfaces[1]['printed_text']
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
    cleanfile("cardnames-{0}".format(languages[lang]))

# Patch language files
for lang in languages.keys():
    patchtranslations("cardnames-{0}".format(languages[lang]))
