#!/usr/bin/env python3
"""
updateCardArtCdnUuids.py

TEMPORARY BRIDGE. Generates forge-gui/res/languages/card-art-cdn-uuid.txt:
one Scryfall id per (set, collector number), English only, used by
CardCdnUuidBridge to fetch card images from the Scryfall CDN
(cards.scryfall.io, no rate limit) instead of api.scryfall.com (rate
limited), ahead of PR #10928's CdnUuidCache.

English only, and bundled (small, ~2MB) - other languages are never
pre-generated or bundled. CardCdnUuidBridge builds those itself at runtime,
in the background, the first time a player picks that language, by paging
through Scryfall's own /cards/search?q=lang:xx API and caching the result
locally. See that class for details.

This intentionally mirrors updateCardArtLanguages.py closely - same bulk
data source, same edition-file matching - so the two are easy to compare
and easy to delete together once #10928 lands.

Delete this file, CardCdnUuidBridge.java, both ForgeConstants entries it
uses, and card-art-cdn-uuid.txt once #10928 merges - CdnUuidCache
supersedes all of it.
"""

import requests
import json
import os
import re
import sys
import argparse
import gzip
import traceback
from datetime import datetime, timezone

TOOLS_DIR = os.path.abspath(os.path.dirname(__file__))
BULK_DATA_API = "https://api.scryfall.com/bulk-data"
TEMP_FILE = os.path.join(TOOLS_DIR, "temp_bulk_data.jsonl.gz")

HEADERS = {
    "User-Agent": "CardArtCdnUuidUpdater/3.0 (temporary bridge, see PR #10928)",
    "Accept": "application/json"
}

CARD_SECTIONS = {
    "cards", "special slot", "precon product", "borderless", "etched",
    "showcase", "full art", "extended art", "alternate art", "retro frame",
    "buy a box", "promo", "prerelease promo", "bundle", "box topper",
    "jumpstart", "rebalanced", "eternal", "conjured", "scheme", "printsheets"
}

CARD_LINE_RE = re.compile(
    r'^(?:(\.?[0-9A-Z][0-9A-Z\-]*\S*[A-Z]*)\s)?(?:([SCURML])\s)?([^@$]+?)(?:\s@([^$]*?))?(?:\s\$\{(.+)\})?\s*$'
)


def fetch_bulk_url():
    response = requests.get(BULK_DATA_API, headers=HEADERS, timeout=30)
    response.raise_for_status()
    data = response.json()
    for item in data["data"]:
        if item["type"] == "all_cards":
            return item.get("jsonl_download_uri", item.get("download_uri"))
    raise RuntimeError("all_cards entry not found")


def download_bulk_file(url):
    if os.path.exists(TEMP_FILE):
        return
    response = requests.get(url, headers=HEADERS, stream=True, timeout=600)
    response.raise_for_status()
    total = int(response.headers.get("content-length", 0))
    downloaded = 0
    with open(TEMP_FILE, "wb") as f:
        for chunk in response.iter_content(8192):
            if not chunk:
                continue
            f.write(chunk)
            downloaded += len(chunk)
            if total:
                percent = downloaded * 100 / total
                print(f"\r  {percent:.1f}%", end="", flush=True)
    print()


def is_real_card(card):
    type_line = card.get("type_line", "")
    return "Token" not in type_line and "Emblem" not in type_line


def process_bulk_english():
    # (set_code, collector_number) -> scryfall_id, English only
    set_cards = {}
    with gzip.open(TEMP_FILE, "rt", encoding="utf-8") as f:
        for line in f:
            try:
                card = json.loads(line)
            except ValueError:
                continue
            if card.get("digital"):
                continue
            if not is_real_card(card):
                continue
            if card.get("lang") != "en":
                continue
            scryfall_id = card.get("id")
            if not scryfall_id:
                continue
            if card.get("image_status") not in ("highres_scan", "lowres"):
                continue
            set_code = card.get("set", "").upper()
            collector_number = card.get("collector_number")
            if not set_code or not collector_number:
                continue
            set_cards[(set_code, collector_number)] = scryfall_id
    return set_cards


def read_edition_lookup_code(content):
    code = None
    scryfall_code = None
    in_metadata = False
    for line in content.splitlines():
        stripped = line.strip()
        if stripped == "[metadata]":
            in_metadata = True
            continue
        if stripped.startswith("[") and stripped != "[metadata]":
            in_metadata = False
            continue
        if not in_metadata or "=" not in stripped:
            continue
        key, _, value = stripped.partition("=")
        key = key.strip().lower()
        value = value.strip()
        if key == "code":
            code = value.upper()
        elif key == "scryfallcode":
            scryfall_code = value.upper()
    return scryfall_code if scryfall_code else code


def collector_numbers_in_edition(content):
    numbers = set()
    current_section = None
    for line in content.splitlines():
        stripped = line.strip()
        if stripped.startswith("[") and stripped.endswith("]"):
            current_section = stripped.strip("[]").lower()
            continue
        if current_section not in CARD_SECTIONS or not stripped:
            continue
        match = CARD_LINE_RE.match(stripped)
        if not match or not match.group(1):
            continue
        numbers.add(match.group(1))
    return numbers


def collect_index(set_cards, editions_dir):
    index = {}
    editions_total = 0
    editions_matched = 0

    for filename in sorted(os.listdir(editions_dir)):
        if not filename.endswith(".txt"):
            continue
        editions_total += 1
        filepath = os.path.join(editions_dir, filename)
        with open(filepath, "r", encoding="utf-8") as f:
            content = f.read()

        lookup_code = read_edition_lookup_code(content)
        if not lookup_code:
            continue

        matched_any = False
        for cn in collector_numbers_in_edition(content):
            scryfall_id = set_cards.get((lookup_code, cn))
            if scryfall_id:
                index[(lookup_code, cn)] = scryfall_id
                matched_any = True
        if matched_any:
            editions_matched += 1

    print(f"total editions: {editions_total}")
    print(f"editions matched: {editions_matched}")
    return index


def write_index_file(index, output_path):
    lines = [f"# TEMPORARY BRIDGE (bundled, English-only) - see PR #10928. generated_at="
             f"{datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')}\n"]
    for set_code, cn in sorted(index.keys()):
        lines.append(f"{set_code}/{cn}={index[(set_code, cn)]}\n")
    with open(output_path, "w", encoding="utf-8") as f:
        f.writelines(lines)
    size_kb = os.path.getsize(output_path) / 1024
    print(f"prints written: {len(index)} -> {output_path} ({size_kb:.0f} KB)")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--editions-dir", default=os.path.join(TOOLS_DIR, "..", "res", "editions"))
    parser.add_argument("--output", default=os.path.join(TOOLS_DIR, "..", "res", "languages", "card-art-cdn-uuid.txt"))
    args = parser.parse_args()
    editions_dir = os.path.abspath(args.editions_dir)
    output_path = os.path.abspath(args.output)

    if not os.path.isdir(editions_dir):
        print(f"not found: {editions_dir}")
        sys.exit(1)

    try:
        url = fetch_bulk_url()
        download_bulk_file(url)
        set_cards = process_bulk_english()
        index = collect_index(set_cards, editions_dir)
        write_index_file(index, output_path)
    except Exception:
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()
