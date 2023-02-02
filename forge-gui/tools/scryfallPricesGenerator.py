#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import json, os

# Note: currently only loads the first json file found in the folder!
files = os.listdir(".")
for filename in files:
    if filename.endswith(".json"):
        metadata_filename = filename
        break

print(f"Loading {metadata_filename}...")
metadata_file = open(metadata_filename, "r")
metadata = json.load(metadata_file)
metadata_file.close()

prices = {}
art_indexes = {}
always_with_artindex = ["Plains", "Island", "Swamp", "Mountain", "Forest", "Wastes", "Snow-Covered Plains", "Snow-Covered Island", "Snow-Covered Swamp", "Snow-Covered Mountain", "Snow-Covered Forest", "Snow-Covered Wastes"]

for object in metadata:
    obj_type = object["object"]
    if obj_type == "card":
        card_name = object["name"]
        # split cards use //, other cards with two sides (e.g. DFC) use the front face in Forge
        if card_name.find("//") != -1 and object["layout"] != "split":
            card_name = card_name.split("//")[0].strip()
        card_set = object["set"].upper()
        card_cn = object["collector_number"]
        card_foil_only = object["foil"] and not object["nonfoil"]
        card_price = None
        if object["prices"]["usd"] == None and object["prices"]["usd_foil"] == None and object["prices"]["usd_etched"] == None and object["prices"]["eur"] == None and object["prices"]["eur_foil"] == None and object["prices"]["tix"] == None:
            continue
        if not card_foil_only and object["prices"]["usd"] != None:
            card_price = object["prices"]["usd"].replace(".", "")
        elif object["prices"]["usd_foil"] != None:
            card_price = object["prices"]["usd_foil"].replace(".", "")
        elif object["prices"]["usd_etched"] != None:
            card_price = object["prices"]["usd_etched"].replace(".", "")
        elif object["prices"]["eur"] != None:
            card_price = object["prices"]["eur"].replace(".", "")
        elif object["prices"]["eur_foil"] != None:
            card_price = object["prices"]["eur_foil"].replace(".", "")
        elif object["prices"]["tix"] != None:
            card_price = object["prices"]["tix"].replace(".", "")
        if card_price == None:
            continue
        elif card_price.startswith("00"):
            card_price = card_price[2:]
        elif card_price.startswith("0"):
            card_price = card_price[1:]
    # add a key to the prices dictionary, per set
    if not card_set in prices:
        prices[card_set] = {}
    if not card_set in art_indexes:
        art_indexes[card_set] = {}
    if card_name not in art_indexes[card_set]:
        art_indexes[card_set][card_name] = 1
    else:
        art_indexes[card_set][card_name] += 1
    if card_name in prices[card_set] or card_name in always_with_artindex:
        card_name += f" ({art_indexes[card_set][card_name]})"
    prices[card_set][card_name] = card_price

# Merge with the previous price list if appropriate
if os.path.exists("all-prices.prev"):
    print()
    merge = input("Would you like to merge the prices with all-prices.prev? ")
    if merge.lower().startswith("y"):
        prev = open("all-prices.prev", "r")
        prev_data = prev.readlines()
        prev.close()
        for prev_price in prev_data:
            if prev_price.find("=") == -1:
                continue
            data = prev_price.split("=")
            old_price = data[1].strip()
            old_name = data[0].split("|")[0]
            if old_name.find("(") != -1: # unsafe to import a qualified name, we don't know how they'll match up
                continue
            old_set = data[0].split("|")[1]
            if old_set in prices.keys() and old_name not in prices[old_set]:
                prices[old_set][old_name] = old_price
            
# Generate the prices file
output = open("all-prices.txt", "w")
sorted_prices = {key: value for key, value in sorted(prices.items())}
for set in sorted_prices.keys():
    sorted_cards = {key: value for key, value in sorted(prices[set].items())}    
    for name in sorted_cards.keys():
        qualified_name = name
        if name.find("(") == -1 and name in art_indexes[set] and art_indexes[set][name] > 1:
            qualified_name += " (1)"
        print(qualified_name + "|" + set + "=" + sorted_cards[name])
        output.write(f"{qualified_name}|{set}={sorted_cards[name]}\n")
output.close() 
