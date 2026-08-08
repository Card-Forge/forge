#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Builds forge-gui/res/languages/card_languages.txt — a compact index of which
non-English languages exist for each paper Magic card printing, as recorded
by Scryfall. Used by Forge to decide, before hitting the Scryfall image API,
whether the user's preferred card-image language actually exists for a given
card (avoiding pointless 404s), falling back to English otherwise.

Run from within forge-gui/tools/ (same convention as the other data-update
scripts in this folder, e.g. scryfallPricesGenerator.py):
    python3 update_languages.py

English is NOT stored: if a set/collector-number pair is absent from the
index, or a specific language bit isn't set, Forge treats it as "not
confirmed available in that language" and falls back to English.

Output format (plain text, one line per set):
    # meta bulk_updated_at=<ISO8601> generated_at=<ISO8601> languages=es,fr,de,it,pt,ja,ko,ru,zhs,zht
    <setcode> <collector>:<bitmask> <collector>:<bitmask> ...
    <setcode> <collector>:<bitmask> ...

Sets/collector-numbers with bitmask 0 (English-only) are omitted entirely,
and sets with no non-English printings at all are omitted too.
"""

import requests
import json
import os
import gzip
import sys
import traceback
from datetime import datetime, timezone

TOOLS_DIR = os.path.abspath(os.path.dirname(__file__))
OUTPUT_DIR = os.path.abspath(os.path.join(TOOLS_DIR, "..", "res", "languages"))

BULK_DATA_API = "https://api.scryfall.com/bulk-data"
TEMP_FILE = os.path.join(TOOLS_DIR, "temp_scryfall_data.jsonl.gz")
OUTPUT_FILE = os.path.join(OUTPUT_DIR, "card_languages.txt")

HEADERS = {
    "User-Agent": "ForgeCardLanguageUpdater/4.0",
    "Accept": "application/json"
}

# Non-English languages supported by Forge's language-selection preference
# (must stay in sync with ForgeConstants.getScryfallCardLanguageMapping() and
# CardLanguageIndex.LANGUAGE_BITS on the Java side).
# https://scryfall.com/docs/api/languages
# English is implicit and therefore is NOT stored.
LANGUAGE_BITS = {
    "es": 1 << 0,
    "fr": 1 << 1,
    "de": 1 << 2,
    "it": 1 << 3,
    "pt": 1 << 4,
    "ja": 1 << 5,
    "ko": 1 << 6,
    "ru": 1 << 7,
    "zhs": 1 << 8,
    "zht": 1 << 9,
}


def get_bulk_data_info():
    print("Querying Scryfall API...")

    response = requests.get(BULK_DATA_API, headers=HEADERS, timeout=30)
    response.raise_for_status()

    data = response.json()

    for item in data["data"]:
        if item["type"] == "all_cards":
            return item["jsonl_download_uri"], item.get("updated_at")

    raise RuntimeError("Couldn't find all_cards bulk data")


def download_file(url, filename):
    print("Downloading bulk file...")

    tmp_partial = filename + ".partial"
    with requests.get(url, headers=HEADERS, stream=True, timeout=60) as r:
        r.raise_for_status()

        total = int(r.headers.get("content-length", 0))
        downloaded = 0

        with open(tmp_partial, "wb") as f:
            for chunk in r.iter_content(8192):
                if not chunk:
                    continue
                f.write(chunk)
                downloaded += len(chunk)
                if total:
                    percent = downloaded * 100 / total
                    print(f"\r{percent:.1f}%", end="", flush=True)

    os.replace(tmp_partial, filename)
    print("\nDone.")


def collector_sort(value):
    try:
        return (0, int(value))
    except ValueError:
        return (1, value)


def process_data(filename):
    print("Building language bitmasks...")

    result = {}

    with gzip.open(filename, "rt", encoding="utf-8") as file:
        for line in file:
            card = json.loads(line)

            # Skip digital-only prints (e.g. Alchemy/Arena rebalances):
            # Forge only ever requests paper card images from Scryfall.
            if card.get("digital"):
                continue

            set_code = card.get("set")
            collector = card.get("collector_number")
            lang = card.get("lang")

            if not set_code or not collector or not lang:
                continue

            bit = LANGUAGE_BITS.get(lang)
            if not bit:
                # English, or a language Forge doesn't track (e.g. "qya"/"tk"/"cs"
                # test languages some print proxies use). Nothing to record.
                continue

            set_entries = result.setdefault(set_code, {})
            set_entries[collector] = set_entries.get(collector, 0) | bit

    # Drop empty/zero entries: absence from the index already means
    # "no confirmed non-English printing", so a stored 0 carries no
    # information and only bloats the file.
    ordered = {}
    for set_code in sorted(result.keys()):
        collectors = {c: m for c, m in result[set_code].items() if m}
        if not collectors:
            continue
        ordered[set_code] = {
            c: collectors[c] for c in sorted(collectors, key=collector_sort)
        }

    return ordered


def save_output(data, filename, bulk_updated_at):
    print("Saving index...")

    os.makedirs(os.path.dirname(filename), exist_ok=True)
    tmp_partial = filename + ".partial"
    lang_list = ",".join(LANGUAGE_BITS.keys())
    generated_at = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

    with open(tmp_partial, "w", encoding="utf-8") as f:
        f.write(
            f"# meta bulk_updated_at={bulk_updated_at} "
            f"generated_at={generated_at} languages={lang_list}\n"
        )
        for set_code, collectors in data.items():
            entries = " ".join(f"{c}:{m}" for c, m in collectors.items())
            f.write(f"{set_code} {entries}\n")

    os.replace(tmp_partial, filename)
    print("Finished.")


def main():
    try:
        if os.path.exists(TEMP_FILE):
            print("Using cached bulk file.")
            bulk_updated_at = "unknown (cached file reused)"
        else:
            url, bulk_updated_at = get_bulk_data_info()
            download_file(url, TEMP_FILE)

        data = process_data(TEMP_FILE)
        save_output(data, OUTPUT_FILE, bulk_updated_at)

    except Exception:
        print("Failed to update card language index:", file=sys.stderr)
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()
