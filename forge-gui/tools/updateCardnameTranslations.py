#!/usr/bin/env python3
# -*- coding: utf-8 -*-

# Fills in missing/empty entries in cardnames-*.txt from Scryfall's official
# printed_name/printed_type_line/printed_text, without touching anything
# that's already translated. Only es/de/fr/it/pt (ja/ko are handled elsewhere).
#
# Run from forge-gui/tools/:
#   python updateCardnameTranslations.py [--lang es de ...] [--dry-run]

import argparse
import glob
import gzip
import json
import os
import sys
import requests

TOOLS_DIR = os.path.abspath(os.path.dirname(__file__))
CARDSFOLDER_DIR = os.path.abspath(os.path.join(TOOLS_DIR, "..", "res", "cardsfolder"))
LANG_DIR = os.path.abspath(os.path.join(TOOLS_DIR, "..", "res", "languages"))
BULK_CACHE = os.path.join(TOOLS_DIR, "all_cards.jsonl.gz")

BULK_DATA_API = "https://api.scryfall.com/bulk-data"
HEADERS = {
    "User-Agent": "CardDataUpdater/1.0",
    "Accept": "application/json"
}

LANGUAGES = {
    "es": "es-ES",
    "de": "de-DE",
    "fr": "fr-FR",
    "it": "it-IT",
    "pt": "pt-BR",
}


def get_all_forge_face_names():
    names = set()
    for path in glob.glob(os.path.join(CARDSFOLDER_DIR, "**", "*.txt"), recursive=True):
        with open(path, encoding="utf-8", errors="ignore") as f:
            for line in f:
                if line.startswith("Name:"):
                    names.add(line.strip()[len("Name:"):])
    return names


def download_bulk_data():
    if os.path.exists(BULK_CACHE):
        print("Using cached all_cards.jsonl.gz.")
        return

    print("Querying Scryfall API...")
    response = requests.get(BULK_DATA_API, headers=HEADERS, timeout=30)
    response.raise_for_status()
    data = response.json()["data"]
    bulk_url = next(item["jsonl_download_uri"] for item in data if item["type"] == "all_cards")

    print("Downloading all_cards bulk data...")
    tmp = BULK_CACHE + ".partial"
    with requests.get(bulk_url, headers=HEADERS, stream=True, timeout=60) as r:
        r.raise_for_status()
        total = int(r.headers.get("content-length", 0))
        downloaded = 0
        with open(tmp, "wb") as f:
            for chunk in r.iter_content(8192):
                if not chunk:
                    continue
                f.write(chunk)
                downloaded += len(chunk)
                if total:
                    print(f"\r{downloaded * 100 / total:.1f}%", end="", flush=True)
    os.replace(tmp, BULK_CACHE)
    print("\nDone.")


def clean_text(value):
    if not value:
        return ""
    return value.replace("\n", "\\n").replace("|", "VERT")


def build_scryfall_translation_index(target_langs):
    """{scryfall_lang: {english_face_name: (tname, ttype, toracle)}}"""
    index = {lang: {} for lang in target_langs}

    with gzip.open(BULK_CACHE, "rt", encoding="utf-8") as f:
        for raw_line in f:
            line = raw_line.strip()
            if not line:
                continue
            try:
                card = json.loads(line)
            except json.JSONDecodeError:
                continue

            lang = card.get("lang")
            if lang not in target_langs:
                continue

            faces = card.get("card_faces")
            if faces and "printed_name" not in card:
                for face in faces:
                    name = face.get("name")
                    tname = face.get("printed_name", "")
                    if name and tname and name not in index[lang]:
                        index[lang][name] = (tname, face.get("printed_type_line", ""),
                                              clean_text(face.get("printed_text", "")))
            else:
                name = card.get("name")
                tname = card.get("printed_name", "")
                if name and tname and name not in index[lang]:
                    index[lang][name] = (tname, card.get("printed_type_line", ""),
                                          clean_text(card.get("printed_text", "")))

    return index


def load_lines(path):
    entries = {}
    if not os.path.exists(path):
        return entries
    with open(path, encoding="utf-8") as f:
        for raw_line in f:
            line = raw_line.rstrip("\r\n")
            if line:
                entries[line.split("|", 1)[0]] = line
    return entries


def needs_translation(line, include_same_as_english):
    parts = line.split("|")
    if len(parts) < 2:
        return True
    en_name, tname = parts[0], parts[1]
    if tname == "":
        return True
    return include_same_as_english and tname == en_name


def process_language(scryfall_lang, forge_suffix, all_forge_names, scryfall_index,
                      include_same_as_english, dry_run):
    filename = f"cardnames-{forge_suffix}.txt"
    path = os.path.join(LANG_DIR, filename)
    patch_path = os.path.join(LANG_DIR, f"cardnames-{forge_suffix}-patch.txt")

    existing = load_lines(path)
    lang_data = scryfall_index.get(scryfall_lang, {})

    filled = 0
    missing = 0

    for name in sorted(all_forge_names):
        line = existing.get(name)
        if line is not None and not needs_translation(line, include_same_as_english):
            continue

        translation = lang_data.get(name)
        if translation:
            tname, ttype, toracle = translation
            existing[name] = f"{name}|{tname}|{ttype}|{toracle}"
            filled += 1
        elif line is None:
            missing += 1

    # manual patch always wins, can also add entries not in cardsfolder yet
    patch = load_lines(patch_path)
    existing.update(patch)

    extra = f", {len(patch)} from patch file" if patch else ""
    print(f"{filename}: filled {filled}, {missing} still not found on Scryfall{extra}")

    if dry_run:
        return

    with open(path, "w", encoding="utf-8", newline="") as f:
        for name in sorted(existing.keys()):
            f.write(existing[name] + "\r\n")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--lang", nargs="*", choices=list(LANGUAGES.keys()))
    parser.add_argument("--include-same-as-english", action="store_true",
                         help="also try to refill entries where the translated name matches English")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    target_langs = args.lang if args.lang else list(LANGUAGES.keys())

    print("Reading card/face names from cardsfolder...")
    all_forge_names = get_all_forge_face_names()
    print(f"Found {len(all_forge_names)} card faces.")

    download_bulk_data()

    print("Indexing Scryfall translations: " + ", ".join(target_langs))
    scryfall_index = build_scryfall_translation_index(target_langs)

    for lang in target_langs:
        process_language(lang, LANGUAGES[lang], all_forge_names, scryfall_index,
                          args.include_same_as_english, args.dry_run)


if __name__ == "__main__":
    try:
        main()
    except Exception:
        import traceback
        print("Failed:", file=sys.stderr)
        traceback.print_exc()
        sys.exit(1)
