#!/usr/bin/env python3
import os
import re
from collections import defaultdict

# Allowed editions for Old Border mod
ALLOWED_EDITIONS = [
    "LEA", "LEB", "2ED", "3ED", "ARN", "ATQ", "LEG", "DRK", "FEM", "4ED",
    "ICE", "CHR", "HML", "ALL", "MIR", "VIS", "5ED", "POR", "WTH", "TMP",
    "STH", "EXO", "PO2", "USG", "ATH", "ULG", "UDS", "6ED", "PTK", "S99",
    "MMQ", "BRB", "NMS", "S00", "PCY", "INV", "BTD", "PLS", "APC", "7ED",
    "ODY", "DKM", "TOR", "JUD", "ONS", "LGN", "SCG", "PHPR"
]

def load_legal_card_pool(editions_dir):
    """Build a mapping of card names to their legal editions."""
    card_to_editions = defaultdict(list)
    edition_files = {}

    print("Building legal card pool from allowed editions...\n")

    # First, map edition codes to files
    for filename in os.listdir(editions_dir):
        if not filename.endswith('.txt'):
            continue

        filepath = os.path.join(editions_dir, filename)
        try:
            with open(filepath, 'r', encoding='utf-8') as f:
                for line in f:
                    if line.startswith('Code='):
                        code = line.split('=', 1)[1].strip()
                        if code in ALLOWED_EDITIONS:
                            edition_files[code] = filepath
                        break
        except Exception as e:
            continue

    # Now load cards from each allowed edition
    for edition_code in ALLOWED_EDITIONS:
        if edition_code not in edition_files:
            print(f"⚠ Warning: Edition {edition_code} not found in editions directory")
            continue

        filepath = edition_files[edition_code]
        try:
            with open(filepath, 'r', encoding='utf-8') as f:
                in_cards_section = False
                card_count = 0

                for line in f:
                    line = line.strip()

                    if line == '[cards]':
                        in_cards_section = True
                        continue

                    if not in_cards_section or not line:
                        continue

                    # Parse card line: "number rarity card_name @artist"
                    # or "number rarity card_name"
                    # Note: number can have letter suffix (e.g., "2a", "72b") for variant art
                    match = re.match(r'^\d+[a-z]?\s+[CRMUS]\d?\s+(.+?)(?:\s+@.*)?$', line)
                    if match:
                        card_name = match.group(1).strip()
                        card_to_editions[card_name].append(edition_code)
                        card_count += 1

                print(f"✓ {edition_code}: {card_count} cards")

        except Exception as e:
            print(f"✗ Error reading {edition_code}: {e}")

    print(f"\n{'='*70}")
    print(f"Total unique legal cards: {len(card_to_editions)}")
    print(f"{'='*70}\n")

    return card_to_editions

def main():
    editions_dir = "/Users/vanja/Coding/forge/forge-gui/res/editions"

    card_to_editions = load_legal_card_pool(editions_dir)

    # Save to file for easy reuse (repository root)
    # Format: CardName|EDITION1,EDITION2,EDITION3
    output_file = "/Users/vanja/Coding/forge/legal_card_pool.txt"
    with open(output_file, 'w', encoding='utf-8') as f:
        for card_name in sorted(card_to_editions.keys()):
            editions = ','.join(card_to_editions[card_name])
            f.write(f"{card_name}|{editions}\n")

    print(f"Legal card pool saved to: {output_file}")

    # Show some examples
    print("\nSample legal cards:")
    for card_name in sorted(card_to_editions.keys())[:10]:
        editions = ','.join(card_to_editions[card_name])
        print(f"  - {card_name} [{editions}]")
    print("  ...")

if __name__ == '__main__':
    main()
