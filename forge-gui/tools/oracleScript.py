#!/usr/bin/env python3

# This python script is designed to handle individual cards located in /res/cardsfolder/*
# Insert and update Oracle text into data files from scryfall oracle_cards bulk data
# Also rename script filename if the name is incorrect

import json
import fnmatch
import os
import re
import urllib.request
import unidecode


NAME_STR = 'Name:'
ORACLE_STR = 'Oracle:'
ALTERATE_STR = 'AlternateMode:'
ALTERNATE_SEPARATER = ' // '
tools_folder = os.path.dirname(os.path.realpath(__file__))


def download_oracle_cards():
    '''Request Scryfall API to download oracle_cards json file'''
    request = urllib.request.urlopen('https://api.scryfall.com/bulk-data')
    data = json.load(request)['data']
    scryfalldburl = [x for x in data if x['type'] == 'oracle_cards'][0]['download_uri']
    urllib.request.urlretrieve(scryfalldburl, os.path.join(tools_folder, 'oracle_cards.json'))


def load_oracle_cards():
    '''Load oracle card data from oracle_cards json file and build oracle cards dict'''
    with open(os.path.join(tools_folder, 'oracle_cards.json'), 'r', encoding='utf8') as oracle_file:
        oracle_json = json.load(oracle_file)
    oracle_cards = {}
    for card in oracle_json:
        if (card['layout'] == 'token'):
            continue
        name = unidecode.unidecode(card['name'])
        oracle_cards[name] = card
    return oracle_cards


def formalize_name(names):
    name = '_'.join(names)
    name = unidecode.unidecode(name)
    name = name.lower()
    name = name.replace('& ', '')
    name = name.replace(' ', '_')
    name = name.replace('-', '_')
    name = name.replace(',', '')
    name = name.replace('.', '')
    name = name.replace(':', '')
    name = name.replace("'", '')
    name = name.replace('"', '')
    name = name.replace('?', '')
    name = name.replace('!', '')
    name = name.replace('(', '')
    name = name.replace(')', '')
    return name


def read_card_script(cardfile):
    names = []
    oracle_texts = []
    lines = []
    line_num = 0
    alternate_mode = ''
    for line in cardfile.readlines():
        line = line.strip()
        if line.startswith(NAME_STR):
            names.append(line[len(NAME_STR):])
        elif line.startswith(ALTERATE_STR):
            alternate_mode = line[len(ALTERATE_STR):]
        elif line.startswith(ORACLE_STR):
            oracle_texts.append([line_num, line[len(ORACLE_STR):]])
            lines.append('')
            line_num += 1
            continue
        lines.append(line + '\n')
        line_num += 1
    cardfile.close()
    return names, lines, oracle_texts, alternate_mode


def write_card_script(cardfile, lines, oracle_texts):
    line_num = 0
    oracle_index = 0
    for line in lines:
        if oracle_index < len(oracle_texts) and line_num == oracle_texts[oracle_index][0]:
            cardfile.write(ORACLE_STR + oracle_texts[oracle_index][1] + '\n')
            oracle_index += 1
        else:
            cardfile.write(line)
        line_num += 1
    cardfile.close()


def update_oracle(name, lines, oracle_text, new_oracle, is_planeswalker):
    if is_planeswalker:
        new_oracle = re.sub(r'([\+−]?[0-9X]+):', r'[\1]:', new_oracle)
    new_oracle = new_oracle.replace('\n', '\\n')
    if oracle_text[1] == new_oracle:
        return False

    oracle_lines = oracle_text[1].split('\\n')
    new_lines = new_oracle.split('\\n')
    nickname = name.split(', ')[0]
    oracle_text[1] = new_oracle

    if len(oracle_lines) != len(new_lines):
        return True

    # Also replace descriptions
    for org_line, new_line in zip(oracle_lines, new_lines):
        org_line = org_line.replace(name, 'CARDNAME')
        org_line = org_line.replace(nickname, 'NICKNAME')
        if org_line.find(':') != -1:
            if org_line.find('"') == -1 or org_line.find('"') > org_line.find(':'):
                org_line = org_line[org_line.find(':') + 1:].lstrip()
        if org_line.find('• ') != -1:
            org_line = org_line[org_line.find('• ') + 2:].lstrip()
        if len(org_line) == 0:
            continue
        new_line = new_line.replace(name, 'CARDNAME')
        new_line = new_line.replace(nickname, 'NICKNAME')
        if new_line.find(':') != -1:
            if new_line.find('"') == -1 or new_line.find('"') > new_line.find(':'):
                new_line = new_line[new_line.find(':') + 1:].lstrip()
        if new_line.find('• ') != -1:
            new_line = new_line[new_line.find('• ') + 2:].lstrip()
        for i, line in enumerate(lines):
            if line.startswith('K:'):
                continue
            if line.find(org_line) != -1:
                lines[i] = line.replace(org_line, new_line)

    return True

def update_card_script(dirname, filename, oracle_cards, logfile):
    file = open(os.path.join(dirname, filename), 'r', encoding='utf8')
    clean_name = filename.replace('.txt', '')

    names, lines, oracle_texts, alternate_mode = read_card_script(file)
    formal_name = formalize_name(names)
    if clean_name != formal_name:
        logfile.write(f'Rename "{clean_name}" => "{formal_name}"\n')
        print(f'Rename "{clean_name}" => "{formal_name}"')
        full_org_filename = os.path.join(dirname, filename)
        full_new_filename = os.path.join(dirname, formal_name + '.txt')
        filename = formal_name + '.txt'
        os.system(f'git mv "{full_org_filename}" "{full_new_filename}"')

    oracle_updated = False
    if alternate_mode == 'Meld':
        cardname = names[0]
    else:
        cardname = ALTERNATE_SEPARATER.join(names)
    if cardname not in oracle_cards:
        logfile.write(f'Skipped unknown card {formal_name}\n')
        print(f'Skipped unknown card {formal_name}')
        return

    card = oracle_cards[cardname]
    if len(names) == 1:
        is_planeswalker = card['type_line'].find('Planeswalker') != -1
        is_vanguard = card['type_line'].find('Vanguard') != -1
        new_oracle = card['oracle_text']
        if is_vanguard:
            new_oracle = 'Hand {0}, life {1}\n'.format(card['hand_modifier'], card['life_modifier']) + new_oracle
        oracle_updated = update_oracle(names[0], lines, oracle_texts[0], new_oracle, is_planeswalker)
    elif len(names) == 2:
        if alternate_mode == 'Meld':
            new_oracle = card['oracle_text']
            oracle_updated = update_oracle(names[0], lines, oracle_texts[0], new_oracle, False)
            card = oracle_cards[names[1]]
            new_oracle = card['oracle_text']
            oracle_updated = oracle_updated | update_oracle(names[1], lines, oracle_texts[1], new_oracle, False)
        else:
            for i, face in enumerate(card['card_faces']):
                is_planeswalker = face['type_line'].find('Planeswalker') != -1
                new_oracle = face['oracle_text']
                oracle_updated = oracle_updated | update_oracle(names[i], lines, oracle_texts[i], new_oracle, is_planeswalker)


    if not oracle_updated:
        return

    logfile.write(f'Updated {formal_name}\n')
    print(f'Updated {formal_name}')
    file = open(os.path.join(dirname, filename), 'w', encoding='utf8')
    write_card_script(file, lines, oracle_texts)
    full_filename = os.path.join(dirname, filename)
    os.system(f'git add {full_filename}')


def main():
    # download_oracle_cards()
    oracle_cards = load_oracle_cards()

    folder = os.path.join(tools_folder, '..', 'res', 'cardsfolder')
    logfile = open(os.path.join(tools_folder, 'oracleScript.log'), 'w')

    for root, dirnames, filenames in os.walk(folder):
        for filename in fnmatch.filter(filenames, '*.txt'):
            if filename.startswith('.'):
                continue
            update_card_script(root, filename, oracle_cards, logfile)

    logfile.close()

if __name__ == '__main__':
    main()
