#!/usr/bin/env python3

import requests
import json
import os
import re
import sys
import argparse
import gzip
import traceback

TOOLS_DIR = os.path.abspath(os.path.dirname(__file__))
BULK_DATA_API = "https://api.scryfall.com/bulk-data"
TEMP_FILE = os.path.join(TOOLS_DIR, "temp_bulk_data.jsonl.gz")

HEADERS = {
    "User-Agent": "CardLanguageUpdater/5.0",
    "Accept": "application/json"
}

LANGUAGE_ORDER = ['en', 'es', 'fr', 'de', 'it', 'pt', 'ja', 'ko', 'ru', 'zhs', 'zht']

CARD_SECTIONS = {
    "cards", "special slot", "precon product", "borderless", "etched",
    "showcase", "full art", "extended art", "alternate art", "retro frame",
    "buy a box", "promo", "prerelease promo", "bundle", "box topper",
    "jumpstart", "rebalanced", "eternal", "conjured", "scheme", "printsheets"
}

CARD_LINE_RE = re.compile(
    r'^(?:(\.?[0-9A-Z][0-9A-Z\-]*\S*[A-Z]*)\s)?(?:([SCURML])\s)?([^@$]+?)(?:\s@([^$]*?))?(?:\s\$\{(.+)\})?\s*$'
)
EXTRA_PARAM_RE = re.compile(r'"([^"]+)"\s*:\s*"([^"]*)"\s*,?')

def sort_languages(langs):
    ordered = []
    remaining = set(langs)
    for lang in LANGUAGE_ORDER:
        if lang in remaining:
            ordered.append(lang)
            remaining.remove(lang)
    for lang in sorted(remaining):
        ordered.append(lang)
    return ordered

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

def process_bulk():
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
            set_code = card.get("set", "").upper()
            collector_number = card.get("collector_number")
            lang = card.get("lang")
            if not set_code or not collector_number or not lang:
                continue
            set_cards.setdefault(set_code, {}).setdefault(collector_number, set()).add(lang)
    return set_cards

def parse_cardlang_value(raw):
    if raw is None:
        return ["en"]
    parsed = [lang.strip() for lang in raw.split(",") if lang.strip()]
    return parsed if parsed else ["en"]

def read_metadata(content):
    code = None
    scryfall_code = None
    cardlang_raw = None
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
        elif key == "cardlang":
            cardlang_raw = value
    return code, scryfall_code, cardlang_raw

def parse_extra_params(text):
    params = {}
    if not text:
        return params
    for match in EXTRA_PARAM_RE.finditer(text):
        params[match.group(1).strip().lower()] = match.group(2).strip()
    return params

def serialize_extra_params(params):
    return ", ".join(f'"{k}": "{v}"' for k, v in params.items())

def set_line_language(line, lang_str):
    newline = "\n" if line.endswith("\n") else ""
    body = line[:-1] if newline else line
    match = CARD_LINE_RE.match(body)
    if not match:
        return line
    params_text = match.group(5)
    params = parse_extra_params(params_text)
    if params.get("lang") == lang_str:
        return line
    params["lang"] = lang_str
    serialized = serialize_extra_params(params)
    if params_text is not None:
        start = body.rfind("${")
        end = body.rfind("}")
        rebuilt = body[:start] + "${" + serialized + "}" + body[end + 1:]
    else:
        rebuilt = body.rstrip() + ' ${' + serialized + '}'
    return rebuilt + newline

def extract_collector_number(line):
    match = CARD_LINE_RE.match(line.rstrip("\n"))
    if not match:
        return None
    return match.group(1)

def insert_cardlang_line(new_lines, last_metadata_content_idx, new_cardlang):
    insert_at = last_metadata_content_idx + 1 if last_metadata_content_idx is not None else len(new_lines)
    new_lines.insert(insert_at, f"CardLang={new_cardlang}\n")
    return insert_at

def update_editions(set_cards, editions_dir):
    total_updated = 0
    total_editions = 0

    for filename in sorted(os.listdir(editions_dir)):
        if not filename.endswith(".txt"):
            continue
        total_editions += 1
        filepath = os.path.join(editions_dir, filename)
        with open(filepath, "r", encoding="utf-8") as f:
            content = f.read()

        code, scryfall_code, metadata_lang_raw = read_metadata(content)
        lookup_code = scryfall_code if scryfall_code else code
        if not lookup_code or lookup_code not in set_cards:
            continue

        cards_in_set = set_cards[lookup_code]
        mode_counts = {}
        for cn, langs in cards_in_set.items():
            lang_tuple = tuple(sort_languages(langs))
            mode_counts[lang_tuple] = mode_counts.get(lang_tuple, 0) + 1
        if not mode_counts:
            continue

        dominant_tuple = max(mode_counts, key=mode_counts.get)
        dominant_set = set(dominant_tuple)
        if dominant_set == {"en"}:
            continue

        current_set = set(parse_cardlang_value(metadata_lang_raw))
        new_cardlang = ",".join(dominant_tuple)
        cardlang_changed = current_set != dominant_set

        exceptions = {}
        for cn, langs in cards_in_set.items():
            if dominant_set.issubset(set(langs)):
                continue
            exceptions[cn] = set(langs)

        if not cardlang_changed and not exceptions:
            continue

        lines = content.splitlines(True)
        new_lines = []
        in_metadata = False
        cardlang_written = False
        current_section = None
        last_metadata_content_idx = None

        for line in lines:
            stripped = line.strip()

            if stripped.startswith("[") and stripped.endswith("]"):
                if in_metadata and cardlang_changed and not cardlang_written:
                    last_metadata_content_idx = insert_cardlang_line(new_lines, last_metadata_content_idx, new_cardlang)
                    cardlang_written = True
                in_metadata = stripped == "[metadata]"
                if in_metadata:
                    last_metadata_content_idx = None
                current_section = stripped.strip("[]").lower()
                new_lines.append(line)
                continue

            if in_metadata and stripped.lower().startswith("cardlang="):
                if cardlang_changed:
                    new_lines.append(f"CardLang={new_cardlang}\n")
                    cardlang_written = True
                else:
                    new_lines.append(line)
                last_metadata_content_idx = len(new_lines) - 1
                continue

            if in_metadata:
                new_lines.append(line)
                if stripped:
                    last_metadata_content_idx = len(new_lines) - 1
                continue

            if current_section in CARD_SECTIONS and stripped:
                cn = extract_collector_number(line)
                if cn and cn in exceptions:
                    lang_str = "|".join(sort_languages(exceptions[cn]))
                    line = set_line_language(line, lang_str)

            new_lines.append(line)

        if in_metadata and cardlang_changed and not cardlang_written:
            last_metadata_content_idx = insert_cardlang_line(new_lines, last_metadata_content_idx, new_cardlang)
            cardlang_written = True

        if cardlang_changed and not cardlang_written:
            continue

        total_updated += 1
        print(f"{filename}: {len(exceptions)} exceptions, cardlang_changed={cardlang_changed}")
        with open(filepath, "w", encoding="utf-8") as f:
            f.writelines(new_lines)

    print(f"total editions: {total_editions}")
    print(f"total updated: {total_updated}")

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--editions-dir", default=os.path.join(TOOLS_DIR, "..", "res", "editions"))
    args = parser.parse_args()
    editions_dir = os.path.abspath(args.editions_dir)

    if not os.path.isdir(editions_dir):
        print(f"not found: {editions_dir}")
        sys.exit(1)

    try:
        url = fetch_bulk_url()
        download_bulk_file(url)
        set_cards = process_bulk()
        update_editions(set_cards, editions_dir)
    except Exception:
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()
