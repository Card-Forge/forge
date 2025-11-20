#!/usr/bin/env python3
import os
import re

# Card replacements (illegal → legal)
REPLACEMENTS = {
    # Myr cards → Old-border artifact creatures
    "Myr Convert": "Bottle Gnomes",      # 3 CMC artifact creature
    "Coretapper": "Brass Man",           # Low-cost artifact creature
    "Myr Custodian": "Clockwork Steed",  # 4 CMC artifact creature
    "Myr Adapter": "Su-Chi",             # 4 CMC artifact creature

    # Board wipes → Old-border equivalents
    "River's Rebuke": "Evacuation",      # Blue bounce all
    "Crippling Fear": "Mutilate",        # Black board wipe
}

def fix_treasure_cards_in_file(filepath):
    """Replace illegal card names in treasure chest rewards."""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
    except Exception as e:
        return False, []

    original_content = content
    changes = []

    for illegal_card, legal_card in REPLACEMENTS.items():
        # Pattern: "cardName": "IllegalCard" or "itemName": "IllegalCard"
        pattern = f'(&quot;(?:card|item)Name&quot;:\\s*&quot;){re.escape(illegal_card)}(&quot;)'

        if re.search(pattern, content):
            content = re.sub(pattern, f'\\1{legal_card}\\2', content)
            changes.append(f"  {illegal_card} → {legal_card}")

    modified = content != original_content

    if modified:
        try:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            return True, changes
        except Exception as e:
            return False, changes

    return False, changes

def main():
    maps_dir = "/Users/vanja/Coding/forge/forge-gui/res/adventure/Shandalar Old Border/maps"

    print("Fixing illegal cards in treasure chest rewards...")
    print(f"Replacements:")
    for illegal, legal in REPLACEMENTS.items():
        print(f"  {illegal} → {legal}")
    print()

    total_files = 0
    total_changes = 0

    files_to_check = [
        "map/main_story_explore/library_of_varsil_4.tmx",
        "map/skep/sliverqueen.tmx"
    ]

    for rel_path in files_to_check:
        filepath = os.path.join(maps_dir, rel_path)

        if not os.path.exists(filepath):
            print(f"⚠ File not found: {rel_path}")
            continue

        modified, changes = fix_treasure_cards_in_file(filepath)

        if changes:
            print(f"{rel_path}:")
            for change in changes:
                print(change)
                total_changes += 1
            if modified:
                print("  ✓ Fixed")
            print()
            total_files += 1

    print(f"{'='*70}")
    print(f"Summary: Fixed {total_changes} card names in {total_files} files")
    print(f"{'='*70}")

if __name__ == '__main__':
    main()
