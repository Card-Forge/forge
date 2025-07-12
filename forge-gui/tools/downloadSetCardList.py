#!/usr/bin/env python

# This script fetches all card data for a given Magic: The Gathering set code
# using the Scryfall API, and saves it to a Forge-compatible edition file.
#
# It generates the file in `forge-gui/res/editions/`, named after the set
# (e.g. "The List.txt"). The file includes a [metadata] section and a [cards]
# section. Each card line includes the collector number, rarity (as a one-letter code),
# name, and artist.
#
# Example output line:
#   163 U Young Pyromancer @Steve Argyle
#
# Rarity codes follow a mapping defined in RARITY_MAP, and unsafe filename
# characters in set names are sanitized.

import requests
import time
import os

RARITY_MAP = {
    "common": "C",
    "uncommon": "U",
    "rare": "R",
    "mythic": "M",
    "special": "S",
    "basic": "B",
    "land": "L",
    "token": "T",
    "art_series": "A",
    "double_faced": "DF",
    "emblem": "E",
}


def fetch_all_set_cards(set_code: str):
    all_cards = []
    url = (
        f"https://api.scryfall.com/cards/search?order=set&q=e:{set_code}&unique=prints"
    )

    while url:
        response = requests.get(url)
        response.raise_for_status()
        data = response.json()
        all_cards.extend(data["data"])
        url = data.get("next_page")
        time.sleep(0.1)

    return all_cards


def fetch_set_metadata(set_code: str):
    url = f"https://api.scryfall.com/sets/{set_code}"
    response = requests.get(url)
    if response.status_code == 404:
        raise ValueError(f"Set code '{set_code}' not found on Scryfall.")
    response.raise_for_status()
    return response.json()


def sanitize_filename(name: str) -> str:
    # Replace characters that aren't safe in filenames
    return "".join(c for c in name if c.isalnum() or c in (" ", "_", "-")).rstrip()


def save_as_text(cards, output_dir: str, metadata: dict):
    os.makedirs(output_dir, exist_ok=True)

    sanitized_name = sanitize_filename(metadata["name"])
    filename = f"{sanitized_name}.txt"
    output_path = os.path.join(output_dir, filename)

    with open(output_path, "w", encoding="utf-8") as f:
        # Write metadata
        f.write("[metadata]\n")
        f.write(f"Code={metadata['code'].upper()}\n")
        f.write(f"Date={metadata['released_at']}\n")
        f.write(f"Name={metadata['name']}\n")
        f.write(f"Type={metadata['set_type'].capitalize()}\n")
        f.write(f"ScryfallCode={metadata['code'].upper()}\n\n")
        f.write("[cards]\n")

        # Write cards
        for card in cards:
            name = card["name"]
            rarity_letter = RARITY_MAP.get(card["rarity"], "?")
            collector_number = card["collector_number"]
            artist = card.get("artist", "Unknown Artist")
            line = f"{collector_number} {rarity_letter} {name} @{artist}"
            f.write(line + "\n")

    print(f"Saved {len(cards)} cards to {output_path}")


if __name__ == "__main__":
    set_code = input("Enter set code (e.g., plst, mb1): ").strip().lower()

    try:
        metadata = fetch_set_metadata(set_code)
    except ValueError as e:
        print(e)
        exit(1)

    cards = fetch_all_set_cards(set_code)

    output_directory = os.path.join("forge-gui", "res", "editions")
    save_as_text(cards, output_directory, metadata)
