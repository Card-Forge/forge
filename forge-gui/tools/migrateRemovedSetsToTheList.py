#!/usr/bin/env python

# This script recursively scans all .dck files in a given directory (and subdirectories),
# replacing lines that reference cards from MB1, PLIST, or PLST sets.
# It uses "forge-gui/res/editions/The List.txt" to find the matching card and assigns
# a collector number based on the card’s occurrence index in that file (1-based).
# Artist names are used for more accurate matching when available.
# It expects "The List.txt" lines to follow the format: "<number> <rarity> <card name> @<artist>"

import os
from collections import defaultdict


TARGET_SETS = {"MB1", "PLIST", "PLST"}
LIST_PATH = os.path.join("forge-gui", "res", "editions", "The List.txt")


def load_plst_lookup():
    occurrence_map = defaultdict(list)
    in_cards_section = False

    with open(LIST_PATH, "r", encoding="utf-8") as file:
        for line in file:
            if line.strip() == "[cards]":
                in_cards_section = True
                continue
            if not in_cards_section or not line.strip():
                continue

            try:
                line = line.strip()
                before_artist, artist = line.rsplit(' @', 1)
                tokens = before_artist.split()
                name = " ".join(tokens[2:])
                key = (name, artist)
                occurrence_map[key].append(line)
                occurrence_map[(name, None)].append(line)  # fallback
            except Exception:
                print(f"Warning: could not parse line: {line}")

    return occurrence_map


def parse_dck_line(line:str):
    parts = line.strip().split('|')
    if len(parts) != 3:
        return None, None, None
    quantity_name = parts[0].strip()
    try:
        quantity, card_name = quantity_name.split(' ', 1)
    except ValueError:
        return None, None, None
    return quantity, card_name, line


def extract_artist(line:str):
    if '@' in line:
        try:
            return line.strip().rsplit('@', 1)[-1].strip()
        except Exception:
            pass
    return None


def find_plst_index(card_name:str, artist:str, plst_lookup:dict):
    matches = plst_lookup.get((card_name, artist)) or plst_lookup.get((card_name, None))
    if matches:
        return matches.index(matches[0]) + 1  # 1-based index
    return None


def update_file_lines(filepath:str, plst_lookup:dict):
    updated_lines = []
    changed = False

    with open(filepath, "r", encoding="utf-8") as file:
        for line in file:
            if not any(set_code in line for set_code in TARGET_SETS):
                updated_lines.append(line)
                continue

            quantity, card_name, original_line = parse_dck_line(line)
            if not quantity or not card_name:
                updated_lines.append(line)
                continue

            artist = extract_artist(line)
            index = find_plst_index(card_name, artist, plst_lookup)

            if index:
                new_line = f"{quantity} {card_name}|PLST|{index}\n"
                updated_lines.append(new_line)
                changed = True
            else:
                print(f"Warning: '{card_name}' ({artist or 'no artist'}) not found in The List")
                updated_lines.append(line)

    if changed:
        with open(filepath, "w", encoding="utf-8") as file:
            file.writelines(updated_lines)
        print(f"Updated file: {filepath}")
    else:
        print(f"No changes made to: {filepath}")


def process_directory(directory_path:str):
    plst_lookup = load_plst_lookup()
    for root, _, files in os.walk(directory_path):
        for filename in files:
            if filename.endswith(".dck"):
                full_path = os.path.join(root, filename)
                update_file_lines(full_path, plst_lookup)


if __name__ == "__main__":
    directory = input("Enter path to directory containing .dck files: ").strip()
    if not os.path.isdir(directory):
        print(f"Directory not found: {directory}")
    else:
        process_directory(directory)
