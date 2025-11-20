#!/usr/bin/env python3
import os
import re
import random

# Allowed editions - good sets for booster packs with variety
# Includes core sets from 3ED onward and all expansions
BOOSTER_POOL = [
    "3ED", "4ED", "5ED", "6ED", "7ED",  # Core sets
    "TMP", "MIR", "USG", "MMQ", "INV", "ONS", "ULG", "EXO",
    "STH", "VIS", "WTH", "APC", "ODY", "TOR", "JUD", "SCG"
]

# All allowed editions for validation
ALLOWED_EDITIONS = [
    "LEA", "LEB", "2ED", "3ED", "ARN", "ATQ", "LEG", "DRK", "FEM", "4ED", 
    "ICE", "CHR", "HML", "ALL", "MIR", "VIS", "5ED", "POR", "WTH", "TMP", 
    "STH", "EXO", "PO2", "USG", "ATH", "ULG", "UDS", "6ED", "PTK", "S99", 
    "MMQ", "BRB", "NMS", "S00", "PCY", "INV", "BTD", "PLS", "APC", "7ED", 
    "ODY", "DKM", "TOR", "JUD", "ONS", "LGN", "SCG", "PHPR"
]

def fix_booster_editions_in_file(filepath):
    """Replace disallowed booster editions with random allowed ones."""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
    except Exception as e:
        return False, []
    
    changes = []
    modified = False
    
    # Find all edition references in rewards
    def replace_edition(match):
        nonlocal modified, changes
        old_edition = match.group(1)

        # Always replace with random edition from expanded booster pool for variety
        new_edition = random.choice(BOOSTER_POOL)
        if new_edition != old_edition:
            changes.append(f"  {old_edition} → {new_edition}")
            modified = True
            return f'&quot;editions&quot;: [ &quot;{new_edition}&quot;'
        else:
            return match.group(0)
    
    content = re.sub(
        r'&quot;editions&quot;:\s*\[\s*&quot;([A-Z0-9]+)&quot;',
        replace_edition,
        content
    )
    
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
    
    print("Fixing booster pack editions in map files...")
    print(f"Using booster pool: {', '.join(BOOSTER_POOL)}\n")
    
    total_files = 0
    total_changes = 0
    
    for root, dirs, files in os.walk(maps_dir):
        for filename in files:
            if not filename.endswith('.tmx'):
                continue
            
            filepath = os.path.join(root, filename)
            modified, changes = fix_booster_editions_in_file(filepath)
            
            if changes:
                rel_path = os.path.relpath(filepath, maps_dir)
                print(f"{rel_path}:")
                for change in changes:
                    print(change)
                    total_changes += 1
                if modified:
                    print("  ✓ Fixed")
                print()
                total_files += 1
    
    print(f"{'='*70}")
    print(f"Summary: Fixed {total_changes} edition codes in {total_files} files")
    print(f"{'='*70}")

if __name__ == '__main__':
    # Set seed for reproducibility if desired
    # random.seed(42)
    main()
