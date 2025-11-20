#!/usr/bin/env python3
import os
import re
import json
from collections import defaultdict

def load_legal_cards(filepath):
    """Load the legal card pool from file.
    Format: CardName|EDITION1,EDITION2,EDITION3
    Returns: set of card names
    """
    legal_cards = set()
    with open(filepath, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            # Split on pipe to get card name (before pipe)
            if '|' in line:
                card_name = line.split('|')[0].strip()
                legal_cards.add(card_name)
            else:
                # Backwards compatibility with old format
                legal_cards.add(line)
    return legal_cards

def extract_card_name(card_spec):
    """Extract card name from 'Card Name' or 'Card Name|SET' or 'Card Name|SET|ART' format."""
    if '|' in card_spec:
        return card_spec.split('|')[0].strip()
    return card_spec.strip()

def check_treasure_rewards(maps_dir, legal_cards):
    """Scan all map files for treasure chest rewards with illegal cards."""
    illegal_cards = defaultdict(list)

    for root, dirs, files in os.walk(maps_dir):
        for filename in files:
            if not filename.endswith('.tmx'):
                continue

            filepath = os.path.join(root, filename)
            try:
                with open(filepath, 'r', encoding='utf-8') as f:
                    content = f.read()

                # Find all reward properties
                # Looking for: <property name="reward" value="..."/> or <property name="reward">...</property>

                # Pattern 1: value attribute format
                rewards_inline = re.findall(
                    r'<property name="reward" value="([^"]+)"',
                    content
                )

                # Pattern 2: content format
                rewards_content = re.findall(
                    r'<property name="reward">([^<]+)</property>',
                    content
                )

                all_rewards = rewards_inline + rewards_content

                rel_path = os.path.relpath(filepath, maps_dir)

                for reward_str in all_rewards:
                    # Decode HTML entities
                    reward_str = reward_str.replace('&quot;', '"')

                    try:
                        # Parse JSON
                        rewards = json.loads(reward_str)

                        # Handle both single reward object and array
                        if isinstance(rewards, dict):
                            rewards = [rewards]

                        for reward in rewards:
                            # Check for specific card rewards
                            if reward.get('type') == 'card' and 'cardName' in reward:
                                card_spec = reward['cardName']
                                card_name = extract_card_name(card_spec)

                                if card_name not in legal_cards:
                                    illegal_cards[card_name].append({
                                        'file': rel_path,
                                        'spec': card_spec
                                    })

                    except (json.JSONDecodeError, ValueError) as e:
                        # Some rewards might not be JSON formatted
                        continue

            except Exception as e:
                continue

    return illegal_cards

def main():
    maps_dir = "/Users/vanja/Coding/forge/forge-gui/res/adventure/Shandalar Old Border/maps"
    legal_cards_file = "/Users/vanja/Coding/forge/legal_card_pool.txt"

    print("Loading legal card pool...")
    legal_cards = load_legal_cards(legal_cards_file)
    print(f"✓ Loaded {len(legal_cards)} legal cards\n")

    print("Scanning treasure chest rewards for illegal cards...\n")
    illegal_cards = check_treasure_rewards(maps_dir, legal_cards)

    if not illegal_cards:
        print("✓ No illegal cards found in treasure chest rewards!")
        return

    print(f"{'='*70}")
    print("ILLEGAL CARDS FOUND IN TREASURE CHEST REWARDS")
    print(f"{'='*70}\n")

    total_occurrences = 0
    for card_name, occurrences in sorted(illegal_cards.items()):
        print(f"{card_name}: {len(occurrences)} occurrence(s)")
        for occurrence in occurrences:
            print(f"  - {occurrence['file']}")
            print(f"    Card spec: {occurrence['spec']}")
            total_occurrences += 1
        print()

    print(f"{'='*70}")
    print(f"Summary: {len(illegal_cards)} illegal cards found in {total_occurrences} locations")
    print(f"{'='*70}")

if __name__ == '__main__':
    main()
