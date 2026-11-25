#!/usr/bin/env python3
"""Rewrites res/blockdata/printtags.txt, which tags editions and prints, from Scryfall's bulk card data.

Each tag in RULES gets a [Tag] section. A line holds an edition code alone when every print of the
edition has the tag, or the code followed by the collector numbers of the tagged prints.

Usage: python printtags.py [--bulk default-cards.jsonl.gz]
Without --bulk the current Default Cards file is downloaded from Scryfall.
"""
import argparse
import gzip
import json
import os
import re
import shutil
import sys
import tempfile
import urllib.request

TOOLS_DIR = os.path.dirname(os.path.abspath(__file__))
RES_DIR = os.path.join(TOOLS_DIR, "..", "res")
EDITIONS_DIR = os.path.join(RES_DIR, "editions")
TAGS_FILE = os.path.join(RES_DIR, "blockdata", "printtags.txt")
USER_AGENT = "Forge-printtags/1.0"

# To add a tag, add its name and a test on a Scryfall card object, such as
#     "Playtest": lambda card: "playtest" in card.get("promo_types", []),
# Forge then offers it in the Excluded Sets settings, named by an lblTag<Name> key in en-US.properties.
# Sets not yet released can be tagged by hand with Tags=<Name> in their edition file.
RULES = {
    "UniversesBeyond": lambda card: bool({"universesbeyond", "godzillaseries"} & set(card.get("promo_types", []))),
}

# Sections holding card entries, as in CardEdition.EditionSectionWithCollectorNumbers
CARD_SECTIONS = {"cards", "special slot", "precon product", "borderless", "etched", "showcase", "full art",
                 "extended art", "alternate art", "retro frame", "buy a box", "promo", "prerelease promo",
                 "bundle", "box topper", "jumpstart", "rebalanced", "eternal", "conjured", "scheme", "printsheets"}
# CardEdition.Reader.CARD_PATTERN
CARD_PATTERN = re.compile(r"(^(.?[0-9A-Z-]+\S*[A-Z]*)\s)?(([SCURML])\s)?([^@$]+)( @([^$]*))?( \$\{(.+)\})?$")
# Below this share of entries found on Scryfall the download is assumed broken and nothing is written
MIN_MATCHED = 0.95


def read_editions():
    editions = []
    for file_name in sorted(os.listdir(EDITIONS_DIR)):
        if not file_name.endswith(".txt"):
            continue
        metadata, numbers, section = {}, [], None
        with open(os.path.join(EDITIONS_DIR, file_name), encoding="utf-8") as f:
            for raw in f:
                line = raw.strip()
                if not line or line.startswith("#"):
                    continue
                header = re.match(r"^\[(.+)\]$", line)
                if header:
                    section = header.group(1).lower()
                elif section == "metadata":
                    key, _, value = line.partition("=")
                    metadata[key.strip()] = value.strip()
                elif section in CARD_SECTIONS:
                    entry = CARD_PATTERN.match(line)
                    if entry and entry.group(2) and entry.group(2) not in numbers:
                        numbers.append(entry.group(2))
        if "Code" in metadata:
            editions.append((metadata, numbers))
    return sorted(editions, key=lambda edition: edition[0]["Code"].lower())


def scryfall_key(metadata, number):
    """The set and number Forge asks Scryfall for, as in ImageUtil.getScryfallDownloadUrl."""
    code = metadata.get("ScryfallCode", metadata["Code"]).lower()
    for prefix in ("OHOP", "OPCA", "OPC2"):
        if number.startswith(prefix):
            code, number = prefix.lower(), number[len(prefix):]
    if re.match(r"^F\d+[a-z]?$", number):
        number = number[1:]
    return code, number.rstrip("☇")


def with_ranges(numbers):
    """Writes runs of consecutive plain numbers as first..last. Hyphens already appear in numbers such as 2025-1."""
    def plain(n):
        return n.isdigit() and str(int(n)) == n
    out, i = [], 0
    while i < len(numbers):
        j = i
        while plain(numbers[i]) and j + 1 < len(numbers) and plain(numbers[j + 1]) \
                and int(numbers[j + 1]) == int(numbers[j]) + 1:
            j += 1
        if j - i >= 2:
            out.append(f"{numbers[i]}..{numbers[j]}")
        else:
            out.extend(numbers[i:j + 1])
        i = j + 1
    return out


def download_bulk():
    request = urllib.request.Request("https://api.scryfall.com/bulk-data",
                                     headers={"User-Agent": USER_AGENT, "Accept": "application/json"})
    with urllib.request.urlopen(request) as response:
        index = json.load(response)
    url = next(d["jsonl_download_uri"] for d in index["data"] if d["type"] == "default_cards")
    path = os.path.join(tempfile.gettempdir(), "scryfall-default-cards.jsonl.gz")
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request) as response, open(path, "wb") as out:
        shutil.copyfileobj(response, out)
    return path


def read_scryfall(path, wanted):
    """Maps each wanted (set, number) found on Scryfall to the tags RULES give its card."""
    tags = {}
    with gzip.open(path, "rt", encoding="utf-8") as f:
        for line in f:
            card = json.loads(line)
            key = (card["set"], card["collector_number"])
            if key in wanted:
                tags[key] = {tag for tag, rule in RULES.items() if rule(card)}
    return tags


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--bulk", help="a Scryfall Default Cards .jsonl.gz file to use instead of downloading one")
    args = parser.parse_args()
    editions = read_editions()
    wanted = {scryfall_key(metadata, number) for metadata, numbers in editions for number in numbers}
    card_tags = read_scryfall(args.bulk or download_bulk(), wanted)

    lines = {tag: [] for tag in RULES}
    total = found = 0
    unmatched = []
    for metadata, numbers in editions:
        code = metadata["Code"]
        prints = {}
        for number in numbers:
            key = scryfall_key(metadata, number)
            if key in card_tags:
                prints[number] = card_tags[key]
        for number in numbers:
            # Scryfall no longer lists Alchemy rebalances, which share their original's tags
            if number not in prints and number.startswith("A-") and number[2:] in prints:
                prints[number] = prints[number[2:]]
        total += len(numbers)
        found += len(prints)
        unmatched += [f"{code} {n}" for n in numbers if n not in prints]
        for tag in RULES:
            tagged = [n for n in numbers if n in prints and tag in prints[n]]
            if tagged:
                lines[tag].append(code if len(tagged) == len(prints) else code + " " + " ".join(with_ranges(tagged)))

    print(f"{found} of {total} edition entries found on Scryfall")
    if total == 0 or found / total < MIN_MATCHED:
        sys.exit("Too few entries found, leaving the tag file unchanged")
    with open(TAGS_FILE, "w", encoding="utf-8", newline="\n") as f:
        f.write("# Generated by forge-gui/tools/printtags.py from Scryfall. Do not edit by hand.\n")
        for tag, tag_lines in lines.items():
            f.write(f"[{tag}]\n" + "".join(line + "\n" for line in tag_lines))
            print(f"{tag}: {len(tag_lines)} editions")
    print(f"Not found on Scryfall ({len(unmatched)}): " + ", ".join(unmatched))


if __name__ == "__main__":
    main()
