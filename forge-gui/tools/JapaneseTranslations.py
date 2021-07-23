#!/usr/bin/env python3

import os
import urllib.request

sets = [
    '2ED', 'ARN', 'ATQ', 'LEG', 'DRK', 'FEM', 'ICE', 'HML', 'ALL', 'MIR',
    'VIS', 'POR', 'WTH', 'TMP', 'STH', 'EXO', 'P02', 'UGL', 'USG', 'ULG',
    'PTK', 'UDS', 'MMQ', 'NEM', 'PCY', 'INV', 'PLS', 'APC', 'ODY', 'TOR',
    'JUD', 'ONS', 'LGN', 'SCG', '8ED', 'MRD', 'DST', '5DN', 'CHK', 'UNH',
    'BOK', 'SOK', 'RAV', 'GPT', 'DIS', 'CSP', 'TSP', 'PLC', 'FUT', 'LRW',
    'MOR', 'SHM', 'EVE', 'ALA', 'CFX', 'ARB', 'M10', 'HOP', 'ZEN', 'WWK',
    'ROE', 'ARC', 'M11', 'SOM', 'MBS', 'NPH', 'CMD', 'M12', 'ISD', 'DKA',
    'AVR', 'PC2', 'M13', 'RTR', 'GTC', 'DGM', 'M14', 'THS', 'C13', 'BNG',
    'JOU', 'CNS', 'M15', 'KTK', 'C14', 'FRF', 'DTK', 'ORI', 'BFZ', 'C15',
    'OGW', 'SOI', 'EMN', 'CN2', 'KLD', 'C16', 'AER', 'AKH', 'ANN', 'HOU',
    'C17', 'XLN', 'RIX', 'DOM', 'BBD', 'M19', 'C18', 'GRN', 'RNA', 'WAR',
    'MH1', 'M20', 'C19', 'ELD', 'THB', 'IKO', 'C20', 'M21', 'JMP', 'ZNR',
    'ZNC', 'CMR', 'KHM', 'KHC', 'STX', 'C21', 'MH2', 'AFR', 'AFC',
]

costmap = [
    ('(白)', '{W}'),
    ('(青)', '{U}'),
    ('(黒)', '{B}'),
    ('(赤)', '{R}'),
    ('(緑)', '{G}'),
    ('(◇)', '{C}'),
    ('(０)', '{0}'),
    ('(１)', '{1}'),
    ('(２)', '{2}'),
    ('(３)', '{3}'),
    ('(４)', '{4}'),
    ('(５)', '{5}'),
    ('(６)', '{6}'),
    ('(７)', '{7}'),
    ('(８)', '{8}'),
    ('(９)', '{9}'),
    ('(１０)', '{10}'),
    ('(１１)', '{11}'),
    ('(１２)', '{12}'),
    ('(１３)', '{13}'),
    ('(１４)', '{14}'),
    ('(１５)', '{15}'),
    ('(１６)', '{16}'),
    ('(１００)', '{100}'),
    ('(Ｘ)', '{X}'),
    ('(∞)', '{∞}'),
    ('(白/青)', '{W/U}'),
    ('(白/黒)', '{W/B}'),
    ('(青/黒)', '{U/B}'),
    ('(青/赤)', '{U/R}'),
    ('(黒/赤)', '{B/R}'),
    ('(黒/緑)', '{B/G}'),
    ('(赤/緑)', '{R/G}'),
    ('(赤/白)', '{R/W}'),
    ('(緑/白)', '{G/W}'),
    ('(緑/青)', '{G/U}'),
    ('(２/白)', '{2/W}'),
    ('(２/青)', '{2/U}'),
    ('(２/黒)', '{2/B}'),
    ('(２/赤)', '{2/R}'),
    ('(２/緑)', '{2/G}'),
    ('(Φ)', '{P}'),
    ('(白/Φ)', '{W/P}'),
    ('(青/Φ)', '{U/P}'),
    ('(黒/Φ)', '{B/P}'),
    ('(赤/Φ)', '{R/P}'),
    ('(緑/Φ)', '{G/P}'),
    ('(氷)', '{S}'),
    ('(Ｔ)', '{T}'),
    ('(Ｑ)', '{Q}'),
    ('(Ｅ)', '{E}'),
    ('[chaos]', '{CHAOS}')
]


def remove_engtype(text):
    text = text.replace('(Urza’s)', 'ウルザの')
    while text.rfind('(') != -1:
        left_index = text.rindex('(')
        right_index = text.rindex(')')
        if text[left_index + 1:right_index].isascii():
            text = text[:left_index] + text[right_index + 1:]
        else:
            break
    return text


def replace_reminder_text_parentheses(text):
    start = 0
    while True:
        index = text.find('（', start)
        if index == -1:
            break
        # skip enchant and protection
        if text.find('エンチャント（', start) == index - 6 or text.find('プロテクション（', start) == index - 7:
            start = index + 1
            continue
        if index == 0 or text[index - 1] == '\n':
            text = text.replace('（', '(', 1)
        else:
            text = text.replace('（', ' (', 1)
        text = text.replace('）', ')', 1)
    return text


def replace_cost(text):
    for cm in costmap:
        text = text.replace(cm[0], cm[1])
    return text


def writecard(cardfile, cardname, jap_name, jap_type, jap_text):
    if jap_name.rfind('（') != -1:
        jap_name = jap_name[:jap_name.rindex('（')]
    elif len(jap_name) == 0:
        jap_name = cardname
    jap_type = jap_type.replace(' --- ', ' — ')
    jap_type = remove_engtype(jap_type)
    jap_text = replace_cost(jap_text)
    jap_text = remove_engtype(jap_text)
    jap_text = replace_reminder_text_parentheses(jap_text)
    jap_text = jap_text.replace('\n', '\\n')
    cardfile.write(cardname + '|' + jap_name + '|' + jap_type + '|' + jap_text + '\n')


def processcards(cardfile, cur_set):
    datafilename = f'ja-JP/{cur_set}.txt'
    if not os.path.exists(datafilename):
        if cur_set == 'CFX':
            cur_set = 'CON'
        card_data = urllib.request.urlopen(f'http://whisper.wisdom-guild.net/cardlist/{cur_set}.txt').read().decode('shift_jis')
        with open(datafilename, 'w', encoding='utf8') as datafile:
            datafile.write(card_data)
    with open(datafilename, 'r', encoding='utf8') as datafile:
        cardname = ''
        jap_name = ''
        jap_type = ''
        jap_text = ''
        text_mode = False
        for line in datafile:
            if line.startswith('　英語名：'):
                if text_mode is True:
                    text_mode = False
                    jap_text = jap_text.rstrip('\n')
                    writecard(cardfile, cardname, jap_name, jap_type, jap_text)
                    jap_name = ''
                    jap_type = ''
                    jap_text = ''
                cardname = line[len('　英語名：'):].rstrip('\n')
                cardname = cardname.replace('AEther', 'Aether')
            elif line.startswith('日本語名：'):
                jap_name = line[len('日本語名：'):].rstrip('\n')
            elif line.startswith('　タイプ：'):
                jap_type = line[len('　タイプ：'):].rstrip('\n')
                text_mode = True
            elif line.startswith('イラスト：'):
                text_mode = False
                jap_text = jap_text.rstrip('\n')
                writecard(cardfile, cardname, jap_name, jap_type, jap_text)
                cardname = ''
                jap_name = ''
                jap_type = ''
                jap_text = ''
            elif line.startswith('　Ｐ／Ｔ：'):
                continue
            elif text_mode and not line.isspace():
                jap_text += line


def cleanfile(filename, extension1, extension2):
    names_seen = set()
    outfile = open(filename + extension2, 'w', encoding='utf8')
    with open(filename + extension1, 'r', encoding='utf8') as r:
        for line in sorted(r):
            name = line.split('|')[0]
            if name not in names_seen:
                outfile.write(line)
                names_seen.add(name)
    outfile.close()
    os.remove(filename + extension1)


def main():
    if not os.path.exists('ja-JP'):
        os.makedirs('ja-JP')

    with open('cardnames-ja-JP.tmp', 'w', encoding='utf8') as cardfile:
        for cur_set in sets:
            print (f'Processing {cur_set} ...')
            processcards(cardfile, cur_set)

    # Sort file and remove duplicates
    cleanfile('cardnames-ja-JP', '.tmp', ".txt")

if __name__ == '__main__':
    main()
