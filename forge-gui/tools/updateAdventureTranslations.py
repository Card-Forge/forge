#!/usr/bin/env python3

import argparse
import copy
import json
import os
import sys

TOOLS_DIR = os.path.abspath(os.path.dirname(__file__))
DEFAULT_ADVENTURE_DIR = os.path.join(TOOLS_DIR, "..", "res", "adventure")

TRANSLATABLE_FIELDS = {
    "name", "text", "description", "displayName", "rewardDescription",
    "synopsis", "bossIntro", "bossInsult", "comment",
}

IDENTITY_FIELDS = ("id", "name")


def is_prose(value):
    return isinstance(value, str) and value.strip() != ""


def entry_identity(entry, index):
    if isinstance(entry, dict):
        for field in IDENTITY_FIELDS:
            if field in entry and entry[field] not in (None, ""):
                return f"{field}={entry[field]}"
    return f"index={index}"


def collect_prose(node, path, out):
    if isinstance(node, dict):
        for key, value in node.items():
            child = f"{path}.{key}" if path else key
            if key in TRANSLATABLE_FIELDS and is_prose(value):
                out[child] = value
            else:
                collect_prose(value, child, out)
    elif isinstance(node, list):
        for i, value in enumerate(node):
            collect_prose(value, f"{path}[{i}]", out)


def load_json(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def save_json(path, data):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def merge_missing(base_node, translated_node):
    """Fill gaps in translated_node using base_node's English content.

    Never touches a value that is already present in translated_node, even
    if it looks untranslated (still English, empty, etc.) - only adds what
    is genuinely absent, so existing translator work is never overwritten.
    Returns (merged_node, added_count).
    """
    if isinstance(base_node, dict):
        if not isinstance(translated_node, dict):
            return copy.deepcopy(base_node), count_leaves(base_node)
        result = dict(translated_node)
        added = 0
        for key, base_value in base_node.items():
            if key in result:
                merged, sub_added = merge_missing(base_value, result[key])
                result[key] = merged
                added += sub_added
            else:
                result[key] = copy.deepcopy(base_value)
                added += count_leaves(base_value)
        return result, added
    if isinstance(base_node, list):
        translated_list = translated_node if isinstance(translated_node, list) else []
        result = []
        added = 0
        for i, base_item in enumerate(base_node):
            if i < len(translated_list):
                merged, sub_added = merge_missing(base_item, translated_list[i])
                result.append(merged)
                added += sub_added
            else:
                result.append(copy.deepcopy(base_item))
                added += count_leaves(base_item)
        return result, added
    return translated_node, 0


def count_leaves(node):
    if isinstance(node, dict):
        return sum(count_leaves(v) for v in node.values())
    if isinstance(node, list):
        return sum(count_leaves(v) for v in node)
    return 1


def fix_translation(base_path, translated_path):
    base = load_json(base_path)
    translated = load_json(translated_path)
    if not isinstance(base, list) or not isinstance(translated, list):
        return None, None

    translated_by_id = {}
    for i, entry in enumerate(translated):
        translated_by_id[entry_identity(entry, i)] = entry

    new_list = []
    created_entries = 0
    patched_fields = 0
    for i, base_entry in enumerate(base):
        key = entry_identity(base_entry, i)
        translated_entry = translated_by_id.get(key)
        if translated_entry is None:
            new_list.append(copy.deepcopy(base_entry))
            created_entries += 1
        else:
            merged, added = merge_missing(base_entry, translated_entry)
            new_list.append(merged)
            patched_fields += added

    # Preserve any translated entries that no longer match a base entry
    # (e.g. renamed/removed upstream) instead of silently dropping them.
    base_ids = {entry_identity(entry, i) for i, entry in enumerate(base)}
    for i, entry in enumerate(translated):
        if entry_identity(entry, i) not in base_ids:
            new_list.append(entry)

    if created_entries or patched_fields:
        save_json(translated_path, new_list)
    return created_entries, patched_fields


def compare(base_path, translated_path):
    base = load_json(base_path)
    translated = load_json(translated_path)

    if not isinstance(base, list) or not isinstance(translated, list):
        return None, None, None

    base_by_id = {}
    for i, entry in enumerate(base):
        base_by_id[entry_identity(entry, i)] = entry
    translated_by_id = {}
    for i, entry in enumerate(translated):
        translated_by_id[entry_identity(entry, i)] = entry

    missing = [k for k in base_by_id if k not in translated_by_id]
    orphaned = [k for k in translated_by_id if k not in base_by_id]

    changed = []
    for key, base_entry in base_by_id.items():
        translated_entry = translated_by_id.get(key)
        if translated_entry is None:
            continue
        base_prose = {}
        translated_prose = {}
        collect_prose(base_entry, "", base_prose)
        collect_prose(translated_entry, "", translated_prose)
        for prose_path in base_prose:
            if prose_path not in translated_prose:
                changed.append((key, prose_path, "missing in translation"))
        for prose_path in translated_prose:
            if prose_path not in base_prose:
                changed.append((key, prose_path, "no longer in source"))

    return missing, orphaned, changed


def is_translation_of_existing(data_dir, filename):
    stem, ext = os.path.splitext(filename)
    if "-" not in stem:
        return False
    base_filename = stem.split("-", 1)[0] + ext
    return base_filename != filename and os.path.isfile(os.path.join(data_dir, base_filename))


def find_translations(data_dir, filename, lang):
    stem, ext = os.path.splitext(filename)
    if lang != "all":
        path = os.path.join(data_dir, f"{stem}-{lang}{ext}")
        return [(lang, path)] if os.path.isfile(path) else []
    prefix = stem + "-"
    found = []
    for other in sorted(os.listdir(data_dir)):
        if other.startswith(prefix) and other.endswith(ext) and other != filename:
            found.append((other[len(prefix):-len(ext)], os.path.join(data_dir, other)))
    return found


def report(world_dir, lang, fix):
    world_name = os.path.basename(world_dir.rstrip(os.sep))
    data_dir = os.path.join(world_dir, "world")
    if not os.path.isdir(data_dir):
        return 0

    problems = 0
    for filename in sorted(os.listdir(data_dir)):
        if not filename.endswith(".json") or is_translation_of_existing(data_dir, filename):
            continue
        base_path = os.path.join(data_dir, filename)
        for translation_lang, translated_path in find_translations(data_dir, filename, lang):
            if fix:
                created, patched = fix_translation(base_path, translated_path)
                if created is None:
                    print(f"[{world_name}/{filename}] skipped (not a list of entries)")
                    continue
                if created or patched:
                    print(f"[{world_name}/{filename} -> {translation_lang}] "
                          f"created {created} missing entries, filled {patched} missing fields "
                          f"(English placeholder text, ready for a translator)")
                continue

            missing, orphaned, changed = compare(base_path, translated_path)
            if missing is None:
                print(f"[{world_name}/{filename}] skipped (not a list of entries)")
                continue

            print(f"\n[{world_name}/{filename} -> {translation_lang}]")
            if missing:
                problems += len(missing)
                print(f"  {len(missing)} entries in English but MISSING from the translation "
                      f"(players using {translation_lang} will not see these at all):")
                for key in missing[:20]:
                    print(f"    - {key}")
                if len(missing) > 20:
                    print(f"    ... and {len(missing) - 20} more")
            if orphaned:
                problems += len(orphaned)
                print(f"  {len(orphaned)} entries in the translation that no longer exist in English:")
                for key in orphaned[:20]:
                    print(f"    - {key}")
                if len(orphaned) > 20:
                    print(f"    ... and {len(orphaned) - 20} more")
            if changed:
                problems += len(changed)
                print(f"  {len(changed)} structural differences (text added or removed since translating):")
                for key, prose_path, why in changed[:20]:
                    print(f"    - {key}: {prose_path} ({why})")
                if len(changed) > 20:
                    print(f"    ... and {len(changed) - 20} more")
            if not missing and not orphaned and not changed:
                print("  up to date")

    return problems


def main():
    parser = argparse.ArgumentParser(
        description="Report drift between an Adventure world's English data files "
                    "and their per-language translations (<file>-<lang>.json).")
    parser.add_argument("--adventure-dir", default=DEFAULT_ADVENTURE_DIR)
    parser.add_argument("--lang", default="all",
                        help="Language code to check, e.g. es-ES, or 'all' to check "
                             "every <file>-<lang>.json variant found (default: all)")
    parser.add_argument("--world", default=None,
                        help="Only check this world (default: all worlds)")
    parser.add_argument("--fix", action="store_true",
                        help="Instead of just reporting, create missing entries/fields in "
                             "existing translation files using the English text as a "
                             "placeholder, ready for a translator to fill in. Never touches "
                             "or removes content that is already there.")
    args = parser.parse_args()

    adventure_dir = os.path.abspath(args.adventure_dir)
    if not os.path.isdir(adventure_dir):
        print(f"not found: {adventure_dir}")
        sys.exit(1)

    worlds = sorted(d for d in os.listdir(adventure_dir)
                    if os.path.isdir(os.path.join(adventure_dir, d)))
    if args.world:
        worlds = [w for w in worlds if w == args.world]
        if not worlds:
            print(f"world not found: {args.world}")
            sys.exit(1)

    total = 0
    for world in worlds:
        total += report(os.path.join(adventure_dir, world), args.lang, args.fix)

    if args.fix:
        sys.exit(0)
    print(f"\ntotal problems: {total}")
    sys.exit(1 if total else 0)


if __name__ == "__main__":
    main()
