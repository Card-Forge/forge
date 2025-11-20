#!/usr/bin/env python3
import os
import re
from collections import defaultdict

# Allowed editions in chronological order - FIXED: NMS instead of NEM
ALLOWED_EDITIONS = [
    "LEA", "LEB", "2ED", "3ED", "ARN", "ATQ", "LEG", "DRK", "FEM", "4ED", 
    "ICE", "CHR", "HML", "ALL", "MIR", "VIS", "5ED", "POR", "WTH", "TMP", 
    "STH", "EXO", "PO2", "USG", "ATH", "ULG", "UDS", "6ED", "PTK", "S99", 
    "MMQ", "BRB", "NMS", "S00", "PCY", "INV", "BTD", "PLS", "APC", "7ED", 
    "ODY", "DKM", "TOR", "JUD", "ONS", "LGN", "SCG", "PHPR"
]

# Edition dates for sorting - FIXED: NMS instead of NEM
EDITION_DATES = {
    "LEA": "1993-08-05", "LEB": "1993-10-04", "2ED": "1993-12-01", "ARN": "1993-12-17",
    "ATQ": "1994-03-04", "3ED": "1994-04-11", "LEG": "1994-06-01", "DRK": "1994-08-01",
    "PHPR": "1994-09-01", "FEM": "1994-11-01", "4ED": "1995-04-01", "ICE": "1995-06-03",
    "CHR": "1995-07-01", "HML": "1995-10-01", "ALL": "1996-06-10", "MIR": "1996-10-08",
    "VIS": "1997-02-03", "5ED": "1997-03-24", "POR": "1997-05-01", "WTH": "1997-06-09",
    "TMP": "1997-10-14", "STH": "1998-03-02", "EXO": "1998-06-15", "PO2": "1998-06-24",
    "USG": "1998-10-12", "ATH": "1998-11-01", "ULG": "1999-02-15", "6ED": "1999-04-21",
    "UDS": "1999-06-07", "S99": "1999-07-01", "PTK": "1999-07-06", "MMQ": "1999-10-04",
    "BRB": "1999-11-12", "NMS": "2000-02-14", "S00": "2000-04-01", "PCY": "2000-06-05",
    "BTD": "2000-10-01", "INV": "2000-10-02", "PLS": "2001-02-05", "7ED": "2001-04-11",
    "APC": "2001-06-04", "ODY": "2001-10-01", "DKM": "2001-12-01", "TOR": "2002-02-04",
    "JUD": "2002-05-27", "ONS": "2002-10-07", "LGN": "2003-02-03", "SCG": "2003-05-26"
}

def load_card_editions(editions_dir):
    """Load all cards and their editions from edition files."""
    card_editions = defaultdict(set)
    
    for edition_code in ALLOWED_EDITIONS:
        for filename in os.listdir(editions_dir):
            filepath = os.path.join(editions_dir, filename)
            if not filepath.endswith('.txt'):
                continue
            
            try:
                with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
                    content = f.read()
                    
                code_match = re.search(r'^Code=' + re.escape(edition_code) + r'$', content, re.MULTILINE)
                if not code_match:
                    continue
                
                # Found the right edition file, extract cards
                in_cards_section = False
                for line in content.split('\n'):
                    line = line.strip()
                    if line == '[cards]':
                        in_cards_section = True
                        continue
                    if not in_cards_section or not line or line.startswith('['):
                        continue
                    
                    # Parse card line: "1 R Animate Wall @Dan Frazier"
                    parts = line.split('@')[0].strip().split(maxsplit=2)
                    if len(parts) >= 3:
                        card_name = parts[2]
                        card_editions[card_name].add(edition_code)
                
                break  # Found the edition, move to next code
                
            except Exception as e:
                continue
    
    return card_editions

def get_oldest_edition(card_name, card_editions):
    """Get the oldest allowed edition for a card."""
    if card_name not in card_editions:
        return None
    
    editions = list(card_editions[card_name])
    editions.sort(key=lambda e: EDITION_DATES.get(e, '9999-99-99'))
    return editions[0] if editions else None

def is_valid_edition(card_name, edition_code, card_editions):
    """Check if a card exists in the specified edition."""
    return edition_code in card_editions.get(card_name, set())

def validate_and_fix_deck(filepath, card_editions):
    """Validate and fix edition codes in a deck file."""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            lines = f.readlines()
    except Exception as e:
        return False, []
    
    modified = False
    new_lines = []
    errors = []
    
    for line_num, line in enumerate(lines, 1):
        stripped = line.strip()
        
        # Skip empty lines, comments, and section headers
        if not stripped or stripped.startswith('#') or stripped.startswith('['):
            new_lines.append(line)
            continue
        
        # Check if line has edition code
        if '|' not in stripped:
            new_lines.append(line)
            continue
        
        # Parse card line with edition - handle three formats:
        # Format 1: "4 Lightning Bolt|LEA"
        # Format 2: "4 Lightning Bolt|LEA|1" (with art variant)
        # Format 3: "Lightning Bolt|LEA" (no quantity)
        
        match = re.match(r'^(\d+)\s+(.+?)\|([A-Z0-9]+)(?:\|(\d+))?$', stripped)
        if not match:
            # Try without quantity
            match = re.match(r'^(.+?)\|([A-Z0-9]+)(?:\|(\d+))?$', stripped)
            if match:
                card_name = match.group(1).strip()
                edition = match.group(2)
                art_variant = match.group(3) if len(match.groups()) >= 3 else None
                quantity = None
            else:
                new_lines.append(line)
                continue
        else:
            quantity = match.group(1)
            card_name = match.group(2).strip()
            edition = match.group(3)
            art_variant = match.group(4)
        
        # Check if the edition code is valid for this card
        if not is_valid_edition(card_name, edition, card_editions):
            # Find the correct oldest edition
            correct_edition = get_oldest_edition(card_name, card_editions)
            
            if correct_edition:
                # Build the corrected line
                if quantity:
                    new_line = f"{quantity} {card_name}|{correct_edition}"
                else:
                    new_line = f"{card_name}|{correct_edition}"
                
                # Preserve art variant if present
                if art_variant:
                    new_line += f"|{art_variant}"
                
                new_line += "\n"
                new_lines.append(new_line)
                errors.append(f"  Line {line_num}: {card_name}|{edition} → {card_name}|{correct_edition}")
                modified = True
            else:
                # No valid edition found, keep original
                new_lines.append(line)
                errors.append(f"  Line {line_num}: WARNING: No edition found for '{card_name}'")
        else:
            # Edition is valid, keep as is
            new_lines.append(line)
    
    # Write back if modified
    if modified:
        try:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.writelines(new_lines)
            return True, errors
        except Exception as e:
            return False, errors
    
    return False, errors

def main():
    editions_dir = "/Users/vanja/Coding/forge/forge-gui/res/editions"
    base_dir = "/Users/vanja/Coding/forge/forge-gui/res/adventure/Shandalar Old Border/decks"
    
    print("Loading card editions from allowed sets...")
    card_editions = load_card_editions(editions_dir)
    print(f"Loaded {len(card_editions)} unique cards from {len(ALLOWED_EDITIONS)} editions\n")
    
    total_files = 0
    total_fixes = 0
    total_warnings = 0
    
    # Process each deck directory
    for deck_type in ['boss', 'miniboss', 'castle_npc']:
        deck_dir = os.path.join(base_dir, deck_type)
        
        if not os.path.exists(deck_dir):
            continue
        
        print(f"{'='*70}")
        print(f"Processing {deck_type} decks...")
        print(f"{'='*70}")
        
        for filename in sorted(os.listdir(deck_dir)):
            if filename.endswith('.dck'):
                filepath = os.path.join(deck_dir, filename)
                total_files += 1
                
                modified, errors = validate_and_fix_deck(filepath, card_editions)
                
                if errors:
                    print(f"\n{filename}:")
                    for error in errors:
                        print(error)
                        if "WARNING" in error:
                            total_warnings += 1
                        else:
                            total_fixes += 1
                    if modified:
                        print(f"  ✓ Fixed")
        
        print()
    
    print(f"{'='*70}")
    print(f"Summary:")
    print(f"  Files processed: {total_files}")
    print(f"  Edition codes fixed: {total_fixes}")
    print(f"  Warnings (cards not found): {total_warnings}")
    print(f"{'='*70}")

if __name__ == '__main__':
    main()
