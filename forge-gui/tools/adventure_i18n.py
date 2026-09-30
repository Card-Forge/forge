# adventure_i18n.py
#
# Tool for managing translation keys (loctext/locname) for Adventure Mode
# dialog embedded inside .tmx map files.
#
# Subcommands:
#
#   inject     Walks the .tmx files in the given scope and adds a loctext/locname
#              key to every dialog node that doesn't have one yet. Nodes that
#              already have a key are left untouched (safe to run repeatedly,
#              or after new content is added). Edits are surgical -- only the
#              exact spot where a key is added changes, nothing else in the
#              file is reformatted or reordered.
#
#   strip      Removes only the loctext/locname keys this tool itself added
#              (recognized by their "adv.*" pattern), leaving everything else
#              in the file untouched. Use this to cleanly regenerate keys
#              (e.g. after updating this tool), instead of manually reverting
#              files with git.
#
#   share      Rewrites a small curated whitelist of generic navigation labels
#              (see SHARED_LABELS below -- "(Continue)", "Leave", etc.) so
#              every node with that exact text points at ONE shared key,
#              instead of each having its own. This is a deliberate exception
#              to the tool's default (every node keeps an independent key);
#              it measurably shrinks the .properties file for content that's
#              genuinely just a repeated button label, not narrative text
#              that happens to coincide today.
#
#   template   Creates or updates an adventure-<lang>.properties file, using the
#              English text as the starting value for every key that isn't
#              translated yet. If the output file already exists, existing
#              translations are kept as-is and only missing keys are added.
#
#   reference  Same data as template, but always in plain English and never
#              merged with an existing file. Meant as a lookup reference for a
#              translator, not as the actual working file.
#
#   unique     Groups all keys by identical English text and writes one row
#              per DISTINCT string to a CSV, instead of one row per key. Lets
#              a translator handle a phrase that's reused across dozens of
#              dialog nodes exactly once, without needing to add it to
#              SHARED_LABELS (the .properties file still ends up with one
#              line per key -- this only saves typing, not file size).
#
#   fill       Takes a CSV produced by "unique" (with its "translation" column
#              filled in) and expands it back into a full .properties file,
#              applying each row's translation to every key listed for it.
#
# Where translated files live
# ----------------------------
#
# Each plane/world keeps ALL of its own translations in one place:
#   forge-gui/res/adventure/<World>/languages/adventure-<lang>.properties
#
# The filename does NOT repeat the world's name -- the folder already
# identifies it (forge.util.Localizer.loadAdventureBundle() is given that
# exact folder to load from, one plane at a time, with no fallback to any
# other plane's translations).
#
# --world is the folder under --adventure-root that inject/strip/share/etc.
# operate on (e.g. "common", where Shandalar's dialog content actually
# lives) -- it does not have to match the plane name the translations get
# shipped under. Point --out at whichever plane's languages/ folder actually
# plays that content.
#
# Examples
# --------
#
# Add keys for a world (--world is required, always run scoped to one world):
#   python adventure_i18n.py inject --adventure-root ..\res\adventure --world common
#
# Preview what inject would touch, without writing anything:
#   python adventure_i18n.py inject --adventure-root ..\res\adventure --world common --dry-run
#
# Regenerate keys cleanly (e.g. after updating this tool):
#   python adventure_i18n.py strip --adventure-root ..\res\adventure --world common
#   python adventure_i18n.py inject --adventure-root ..\res\adventure --world common
#
# Share the curated generic-label whitelist across nodes, updating an
# existing translation file's keys to match (promotes each shared label's
# translation, drops the old per-node entries):
#   python adventure_i18n.py share --adventure-root ..\res\adventure --world common --properties ..\res\adventure\Shandalar\languages\adventure-es-ES.properties
#
# Start a brand new language from scratch (creates the file, pre-filled with English):
#   python adventure_i18n.py template --adventure-root ..\res\adventure --world common --out ..\res\adventure\Shandalar\languages\adventure-fr-FR.properties
#
# Re-run template later to pick up newly injected keys, without losing what's
# already translated in that file:
#   python adventure_i18n.py template --adventure-root ..\res\adventure --world common --out ..\res\adventure\Shandalar\languages\adventure-fr-FR.properties
#
# Export a plain English reference file, e.g. to compare against a translation
# instead of translating from another language's file:
#   python adventure_i18n.py reference --adventure-root ..\res\adventure --world common --out adventure-en-US-reference.properties
#
# Translate repeated strings only once instead of once per key: first
# collapse into a CSV of unique strings...
#   python adventure_i18n.py unique --adventure-root ..\res\adventure --world common --out unique-strings-fr-FR.csv
# ...fill in the "translation" column for as many rows as you like, then
# expand it into the full per-key .properties file:
#   python adventure_i18n.py fill --csv unique-strings-fr-FR.csv --out ..\res\adventure\Shandalar\languages\adventure-fr-FR.properties
# "fill" can be run again after translating more rows in the same CSV --
# already-translated keys in --out are kept, only new rows get applied.
#
# To cover a different world, run the same commands again with a different
# --world (e.g. "Realm of Legends"). --world must always be given explicitly;
# there is no "all worlds at once" mode, so a run never touches more than one
# world by accident.

import argparse
import csv
import html
import json
import re
from pathlib import Path

# "Write" regexes: capture the opening/closing tags separately so only the
# inner content can be replaced without rewriting the rest of the XML.
OBJ_RE = re.compile(r'(<object\b[^>]*\bid="(\d+)"[^>]*>)(.*?)(</object>)', re.S)
PROP_RE = re.compile(r'(<property\s+name="(dialog|defeatDialog)">)(.*?)(</property>)', re.S)

# "Read-only" regexes: simpler version without open/close groups, used by the
# template/reference commands which only need to read the .tmx, not rewrite it.
SIMPLE_OBJ_RE = re.compile(r'<object\b[^>]*\bid="(\d+)"[^>]*>(.*?)</object>', re.S)
SIMPLE_PROP_RE = re.compile(r'<property\s+name="(dialog|defeatDialog)">(.*?)</property>', re.S)


def slug(text):
    # Turns a file path into a valid key fragment (no spaces, slashes, or
    # special characters), e.g. "map/aerie/aerie_0" -> "map_aerie_aerie_0"
    return re.sub(r"[^A-Za-z0-9]+", "_", text).strip("_")


def map_key(tmx_path, adventure_root, world):
    # Builds the key fragment identifying a .tmx file, relative to the world
    # folder (not the whole --adventure-root), with the "maps/map/" prefix
    # dropped since every single .tmx lives there -- it disambiguates
    # nothing and just makes every key longer. Keys only need to be unique
    # WITHIN one world's own properties file (each world now has its own
    # dedicated bundle, with no fallback to any other world's translations),
    # so there's no need to encode the world name into the key either.
    world_root = adventure_root / world
    rel = tmx_path.relative_to(world_root).with_suffix("")
    parts = rel.parts
    if len(parts) >= 2 and parts[0] == "maps" and parts[1] == "map":
        parts = parts[2:]
    return slug("_".join(parts)) if parts else slug(rel.name)


def parse_string(text, pos):
    # Reads a JSON string literal starting at pos, respecting backslash escapes,
    # and returns (decoded_value, position_after_closing_quote).
    start = pos
    pos += 1
    while text[pos] != '"':
        if text[pos] == '\\':
            pos += 2
        else:
            pos += 1
    pos += 1
    return json.loads(text[start:pos]), pos


def parse_value(text, pos):
    # Minimal hand-written JSON value parser. Unlike json.loads, this one keeps
    # track of exact character offsets for every object it parses (stored on
    # the object itself as "__start__"/"__end__"), which is what lets "inject"
    # edit the raw text surgically instead of reformatting everything.
    pos = skip_ws(text, pos)
    ch = text[pos]
    if ch == '{':
        return parse_object(text, pos)
    if ch == '[':
        return parse_array(text, pos)
    if ch == '"':
        return parse_string(text, pos)
    if text[pos:pos + 4] == "true":
        return True, pos + 4
    if text[pos:pos + 5] == "false":
        return False, pos + 5
    if text[pos:pos + 4] == "null":
        return None, pos + 4
    start = pos
    while pos < len(text) and text[pos] in "-+0123456789.eE":
        pos += 1
    return json.loads(text[start:pos]), pos


def skip_ws(text, pos):
    while pos < len(text) and text[pos] in " \t\r\n":
        pos += 1
    return pos


def parse_object(text, pos):
    start = pos
    pos += 1
    obj = {}
    fields = []
    pos = skip_ws(text, pos)
    if text[pos] == '}':
        obj["__start__"] = start
        obj["__end__"] = pos + 1
        obj["__fields__"] = fields
        return obj, pos + 1
    while True:
        pos = skip_ws(text, pos)
        key_start = pos
        key, pos = parse_string(text, pos)
        pos = skip_ws(text, pos)
        pos += 1  # skip ':'
        value, pos = parse_value(text, pos)
        obj[key] = value
        fields.append((key, key_start))
        pos = skip_ws(text, pos)
        if text[pos] == ',':
            pos += 1
            continue
        if text[pos] == '}':
            pos += 1
            break
    obj["__start__"] = start
    obj["__end__"] = pos
    obj["__fields__"] = fields
    return obj, pos


def parse_array(text, pos):
    pos += 1
    arr = []
    pos = skip_ws(text, pos)
    if text[pos] == ']':
        return arr, pos + 1
    while True:
        pos = skip_ws(text, pos)
        value, pos = parse_value(text, pos)
        arr.append(value)
        pos = skip_ws(text, pos)
        if text[pos] == ',':
            pos += 1
            continue
        if text[pos] == ']':
            pos += 1
            break
    return arr, pos


def find_insertions(node, base_key, counter, insertions, replacements):
    # Same key-assignment logic as before (stable counter, only for nodes
    # missing a key), but instead of mutating the parsed structure, it just
    # records WHAT needs to change and WHERE, so the actual text edit can be
    # applied surgically afterwards.
    #
    # Three cases per field:
    #   - key absent entirely -> insert a new field (handled via insertions)
    #   - key present but empty ("") -> not a real key (MapDialog treats an
    #     empty loctext/locname the same as absent), so it gets replaced with
    #     a real generated key in place, not skipped and not duplicated
    #   - key present and non-empty -> already has a real key, leave alone
    if not isinstance(node, dict):
        return
    text = node.get("text")
    name = node.get("name")
    if text:
        current = node.get("loctext")
        if current is None:
            insertions.append((node, "loctext", f"{base_key}.n{counter[0]}.text"))
            counter[0] += 1
        elif current == "":
            replacements.append((node, "loctext", f"{base_key}.n{counter[0]}.text"))
            counter[0] += 1
    if name:
        current = node.get("locname")
        if current is None:
            insertions.append((node, "locname", f"{base_key}.n{counter[0]}.name"))
            counter[0] += 1
        elif current == "":
            replacements.append((node, "locname", f"{base_key}.n{counter[0]}.name"))
            counter[0] += 1
    for opt in node.get("options") or []:
        find_insertions(opt, base_key, counter, insertions, replacements)


def detect_field_indent(raw, obj):
    # Looks at the indentation used by the object's own last existing field,
    # so a newly inserted key matches whatever style that specific node
    # already uses. Returns (indent, is_multiline); is_multiline is False for
    # single-line/inline objects like { "deleteMapObject": 58 }, which should
    # never be touched or reformatted since they don't get a new key anyway.
    fields = obj["__fields__"]
    if not fields:
        return None, False
    last_key, last_key_pos = fields[-1]
    line_start = raw.rfind("\n", 0, last_key_pos)
    if line_start == -1:
        return None, False
    indent = raw[line_start + 1:last_key_pos]
    if indent.strip() != "":
        return None, False
    return indent, True


def source_field_indent(decoded, node, field_name):
    # Finds the indentation of the SPECIFIC field that's triggering an
    # insertion (its own line, not necessarily the object's last field), so
    # the new key is formatted consistently with that field's own style.
    for key, key_start in node["__fields__"]:
        if key == field_name:
            line_start = decoded.rfind("\n", 0, key_start)
            if line_start == -1:
                return None, False
            indent = decoded[line_start + 1:key_start]
            if indent.strip() != "":
                return None, False
            return indent, True
    return None, False


def field_value_span(decoded, node, field_name):
    # Finds the exact start/end span of a given field's value within a node,
    # by locating that field's key in __fields__ and parsing past it.
    for key, key_start in node["__fields__"]:
        if key == field_name:
            colon = decoded.index(':', key_start)
            value_start = skip_ws(decoded, colon + 1)
            _, value_end = parse_value(decoded, colon + 1)
            return value_start, value_end
    return None, None


def field_value_end(decoded, node, field_name):
    _, end = field_value_span(decoded, node, field_name)
    return end


def process_property(raw_inner, base_key):
    # Core of "inject". Parses the dialog/defeatDialog JSON while tracking
    # exact offsets, figures out which nodes are missing a key, and edits ONLY
    # the raw text right after each triggering field's own value (loctext
    # right after text, locname right after name) — nothing else in the
    # property value is touched, so unrelated content (like an unrelated
    # "action" array elsewhere in the same tree) stays byte-for-byte identical
    # to the original. Returns None if there's nothing to insert.
    decoded = html.unescape(raw_inner)
    try:
        json.loads(decoded)  # strict validation first: bail out on malformed
    except json.JSONDecodeError:  # source JSON rather than risk a wrong edit
        return None
    try:
        data, _ = parse_value(decoded, 0)
    except Exception:
        return None

    roots = data if isinstance(data, list) else [data]
    seen = [-1]
    for root in roots:
        existing_max_counter(root, seen)
    counter = [seen[0] + 1]

    insertions = []
    replacements = []
    for root in roots:
        find_insertions(root, base_key, counter, insertions, replacements)
    if not insertions and not replacements:
        return None

    edits = []
    for obj, field, value in insertions:
        source_field = "text" if field == "loctext" else "name"
        value_end = field_value_end(decoded, obj, source_field)
        indent, is_multiline = source_field_indent(decoded, obj, source_field)
        if indent is None:
            indent = "\t"

        pos = value_end
        while pos < len(decoded) and decoded[pos] in " \t":
            pos += 1

        if is_multiline and decoded[pos:pos + 1] == ',':
            # The triggering field isn't the last one -> insert our new field
            # right after its existing trailing comma, before whatever field
            # came next originally.
            insert_at = pos + 1
            piece = f"\n{indent}\"{field}\": {json.dumps(value)},"
            edits.append((insert_at, insert_at, piece))
        elif is_multiline:
            # Triggering field is the last one in the object -> add our own
            # comma, matching the same closing-brace-sharing-line safeguard
            # used elsewhere (don't invent an indent that doesn't exist).
            piece = f",\n{indent}\"{field}\": {json.dumps(value)}"
            edits.append((value_end, value_end, piece))
        else:
            piece = f', "{field}": {json.dumps(value)}'
            edits.append((value_end, value_end, piece))

    for node, field, value in replacements:
        # Field already exists but is an empty string -> replace just that
        # value in place, don't insert a new field (that would duplicate it).
        value_start, value_end = field_value_span(decoded, node, field)
        edits.append((value_start, value_end, json.dumps(value)))

    edits.sort(key=lambda e: e[0], reverse=True)
    result = decoded
    for start, end, piece in edits:
        result = result[:start] + piece + result[end:]

    json.loads(result)  # sanity check: must still be valid JSON
    return xml_escape(result)


def prop_escape(value):
    # Escapes a value so it's safe to store as a line in a Java .properties
    # file, and so MessageFormat doesn't try to interpret literal braces { }
    # (used by the game itself, e.g. {var=player_name}) as substitution markers.
    value = value.replace("\\", "\\\\")
    value = value.replace("'", "''")
    value = value.replace("{", "'{'")
    value = value.replace("}", "'}'")
    value = value.replace("\n", "\\n")
    value = value.replace("\r", "\\r")
    value = value.replace("\t", "\\t")
    return value


def resolve_scope(adventure_root, world):
    # Lists the .tmx files to process, limited to a single world (subfolder)
    # under --adventure-root. A world must always be specified explicitly, so
    # a run never silently touches more content than intended.
    tmx_files = sorted(adventure_root.rglob("*.tmx"))
    scope = (adventure_root / world).resolve()
    tmx_files = [
        f for f in tmx_files
        if str(f.resolve()).startswith(str(scope) + "\\") or str(f.resolve()).startswith(str(scope) + "/")
    ]
    return tmx_files


def existing_max_counter(node, seen):
    # Walks the tree looking for keys already assigned (format ".nN.text/name")
    # to figure out which number to continue from, instead of reusing one that
    # already exists.
    if isinstance(node, dict):
        for field in ("loctext", "locname"):
            val = node.get(field)
            if val:
                m = re.search(r"\.n(\d+)\.(text|name)$", val)
                if m:
                    seen[0] = max(seen[0], int(m.group(1)))
        for opt in node.get("options") or []:
            existing_max_counter(opt, seen)


def xml_escape(value):
    # Escapes the already-modified JSON so it can be inserted back as XML text
    # content inside <property name="dialog">...</property>.
    value = value.replace("&", "&amp;")
    value = value.replace("<", "&lt;")
    value = value.replace(">", "&gt;")
    value = value.replace('"', "&quot;")
    return value


def inject_file(path, adventure_root, world, dry_run):
    # Processes a single .tmx: reads it, replaces only the inner content of
    # each object's dialog/defeatDialog properties, and writes it back.
    #
    # The file is read and written while preserving its original line-ending
    # style (CRLF or LF), to avoid producing a massive diff caused purely by a
    # line-ending change on Windows.
    with open(path, "r", encoding="utf-8", newline="") as f:
        raw = f.read()
    uses_crlf = "\r\n" in raw
    content = raw.replace("\r\n", "\n")
    mkey = map_key(path, adventure_root, world)
    changed = False

    def replace_object(obj_match):
        nonlocal changed
        obj_open, obj_id, obj_body, obj_close = obj_match.groups()

        def replace_prop(prop_match):
            nonlocal changed
            prop_open, prop_name, prop_inner, prop_close = prop_match.groups()
            # "d" for dialog, "x" for defeatDialog: keeps both trees of the
            # same NPC from sharing the same key prefix.
            base_key = f"adv.{mkey}.{obj_id}.{'d' if prop_name == 'dialog' else 'x'}"
            new_inner = process_property(prop_inner, base_key)
            if new_inner is None:
                return prop_match.group(0)
            changed = True
            return prop_open + new_inner + prop_close

        new_body = PROP_RE.sub(replace_prop, obj_body)
        return obj_open + new_body + obj_close

    new_content = OBJ_RE.sub(replace_object, content)
    if changed and not dry_run:
        if uses_crlf:
            new_content = new_content.replace("\n", "\r\n")
        with open(path, "w", encoding="utf-8", newline="") as f:
            f.write(new_content)
    return changed


def cmd_inject(args):
    tmx_files = resolve_scope(args.adventure_root, args.world)
    if not tmx_files:
        raise SystemExit(f"No .tmx files under {args.adventure_root / args.world}")
    touched = 0
    for tmx in tmx_files:
        if inject_file(tmx, args.adventure_root, args.world, args.dry_run):
            touched += 1
            print(tmx)
    print(f"\n{touched}/{len(tmx_files)} files modified")


def collect_keyed_text(node, pairs):
    # Walks the dialog tree and returns (key, English text) pairs for every
    # node that already has a loctext/locname assigned. Used by
    # template/reference to know what's available to translate.
    if not isinstance(node, dict):
        return
    if node.get("text") and node.get("loctext"):
        pairs.append((node["loctext"], node["text"]))
    if node.get("name") and node.get("locname"):
        pairs.append((node["locname"], node["name"]))
    for opt in node.get("options") or []:
        collect_keyed_text(opt, pairs)


def collect_all_english(adventure_root, world):
    # Walks every .tmx in the given world and returns a {key: English text}
    # dict for all content that already has a key (requires "inject" to have
    # been run first; nodes without a key are skipped).
    tmx_files = resolve_scope(adventure_root, world)
    result = {}
    for tmx in tmx_files:
        with open(tmx, "r", encoding="utf-8", newline="") as f:
            content = f.read().replace("\r\n", "\n")
        for obj_id, obj_body in SIMPLE_OBJ_RE.findall(content):
            for prop_name, raw in SIMPLE_PROP_RE.findall(obj_body):
                try:
                    data = json.loads(html.unescape(raw))
                except json.JSONDecodeError:
                    continue
                roots = data if isinstance(data, list) else [data]
                pairs = []
                for r in roots:
                    collect_keyed_text(r, pairs)
                for key, text in pairs:
                    result[key] = text
    return result


def cmd_template(args):
    # Creates or updates the .properties file for a new language. If it
    # already exists, keeps whatever is already translated as-is and only adds
    # the missing keys, using English as the starting value. This makes it
    # safe to run again later as more content gets added, without losing any
    # work already done.
    english = collect_all_english(args.adventure_root, args.world)
    existing = {}
    if args.out.exists():
        for line in args.out.read_text(encoding="utf-8").splitlines():
            if "=" in line and not line.startswith("#"):
                k, v = line.split("=", 1)
                existing[k] = v

    new_keys = 0
    for key, text in english.items():
        if key not in existing:
            existing[key] = prop_escape(text)
            new_keys += 1

    lines = [f"{k}={v}" for k, v in sorted(existing.items())]
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"{len(existing)} total keys, {new_keys} pre-filled with English (untranslated) in {args.out}")


def cmd_reference(args):
    # Same data as template, but always plain English and never merged with an
    # existing file. Meant purely as a lookup reference, not a working file.
    english = collect_all_english(args.adventure_root, args.world)
    lines = [f"{k}={prop_escape(v)}" for k, v in sorted(english.items())]
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"{len(english)} keys exported to {args.out}")


def cmd_unique(args):
    # Groups all keys by their identical English source text, and writes one
    # row per DISTINCT text (not per key) to a CSV. This is purely a
    # translator-convenience view: a phrase like "(Continue)" that's reused
    # across dozens of unrelated dialog nodes only needs to be translated
    # once here, instead of once per key. Runtime storage is untouched by
    # this -- "fill" (below) still writes one line per key to the actual
    # .properties file, so there's no coupling between nodes at runtime.
    english = collect_all_english(args.adventure_root, args.world)
    by_text = {}
    for key, text in english.items():
        by_text.setdefault(text, []).append(key)

    rows = sorted(by_text.items(), key=lambda item: -len(item[1]))
    args.out.parent.mkdir(parents=True, exist_ok=True)
    with open(args.out, "w", encoding="utf-8", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["count", "english", "translation", "keys"])
        for text, keys in rows:
            writer.writerow([len(keys), text, "", ";".join(sorted(keys))])

    print(f"{len(english)} keys collapsed into {len(by_text)} unique strings, written to {args.out}")


def cmd_fill(args):
    # Reads a CSV produced by "unique" (with the "translation" column filled
    # in) and expands it back into a full per-key .properties file: every key
    # listed for a given row gets that row's translation. Existing
    # translations already in --out are kept untouched; only rows with a
    # non-empty "translation" cell are applied.
    existing = {}
    if args.out.exists():
        for line in args.out.read_text(encoding="utf-8").splitlines():
            if "=" in line and not line.startswith("#"):
                k, v = line.split("=", 1)
                existing[k] = v

    filled = 0
    with open(args.csv, encoding="utf-8", newline="") as f:
        for row in csv.DictReader(f):
            translation = row.get("translation", "").strip()
            if not translation:
                continue
            keys = [k for k in row["keys"].split(";") if k]
            for key in keys:
                existing[key] = prop_escape(translation)
                filled += 1

    lines = [f"{k}={v}" for k, v in sorted(existing.items())]
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"{filled} keys filled from {args.csv}, {len(existing)} total keys in {args.out}")


KEY_PATTERN = re.compile(r'^adv\.[^"]*\.n\d+\.(text|name)$')


def find_removals(node, removals):
    # Walks the tree looking for loctext/locname values that match OUR key
    # pattern (adv.<map>.<obj>.<d|x>.n<N>.text/name), and records them for
    # removal. Keys that don't match this pattern (e.g. hand-written ones a
    # dev added some other way) are left alone.
    if not isinstance(node, dict):
        return
    for field in ("loctext", "locname"):
        val = node.get(field)
        if isinstance(val, str) and KEY_PATTERN.match(val):
            removals.append((node, field))
    for opt in node.get("options") or []:
        find_removals(opt, removals)


def strip_property(raw_inner):
    # Reverse of "inject": removes only the loctext/locname fields matching
    # our key pattern, surgically, leaving everything else in the property
    # byte-for-byte untouched. Used to undo a previous run cleanly (e.g. to
    # regenerate keys with a newer version of the tool) without touching
    # anything unrelated that may have changed in the file since.
    decoded = html.unescape(raw_inner)
    try:
        json.loads(decoded)
    except json.JSONDecodeError:
        return None
    try:
        data, _ = parse_value(decoded, 0)
    except Exception:
        return None

    roots = data if isinstance(data, list) else [data]
    removals = []
    for root in roots:
        find_removals(root, removals)
    if not removals:
        return None

    field_spans = []
    for node, field in removals:
        # Locate this field's exact span: from the end of the PRECEDING
        # field/opening-brace (including its separating comma) through the
        # end of this field's value, so removing it also removes the comma
        # that introduced it.
        idx = None
        for i, (key, key_start) in enumerate(node["__fields__"]):
            if key == field:
                idx = i
                break
        key_start = node["__fields__"][idx][1]
        colon = decoded.index(':', key_start)
        _, value_end = parse_value(decoded, colon + 1)
        if idx == 0:
            remove_start = node["__start__"] + 1
        else:
            prev_key_start = node["__fields__"][idx - 1][1]
            prev_colon = decoded.index(':', prev_key_start)
            _, prev_value_end = parse_value(decoded, prev_colon + 1)
            remove_start = prev_value_end
        field_spans.append((remove_start, value_end))

    field_spans.sort(reverse=True)
    result = decoded
    for start, end in field_spans:
        result = result[:start] + result[end:]

    json.loads(result)  # sanity check
    return xml_escape(result)


def strip_file(path, dry_run):
    with open(path, "r", encoding="utf-8", newline="") as f:
        raw = f.read()
    uses_crlf = "\r\n" in raw
    content = raw.replace("\r\n", "\n")
    changed = False

    def replace_object(obj_match):
        nonlocal changed
        obj_open, obj_id, obj_body, obj_close = obj_match.groups()

        def replace_prop(prop_match):
            nonlocal changed
            prop_open, prop_name, prop_inner, prop_close = prop_match.groups()
            new_inner = strip_property(prop_inner)
            if new_inner is None:
                return prop_match.group(0)
            changed = True
            return prop_open + new_inner + prop_close

        new_body = PROP_RE.sub(replace_prop, obj_body)
        return obj_open + new_body + obj_close

    new_content = OBJ_RE.sub(replace_object, content)
    if changed and not dry_run:
        if uses_crlf:
            new_content = new_content.replace("\n", "\r\n")
        with open(path, "w", encoding="utf-8", newline="") as f:
            f.write(new_content)
    return changed


def cmd_strip(args):
    tmx_files = resolve_scope(args.adventure_root, args.world)
    if not tmx_files:
        raise SystemExit(f"No .tmx files under {args.adventure_root / args.world}")
    touched = 0
    for tmx in tmx_files:
        if strip_file(tmx, args.dry_run):
            touched += 1
            print(tmx)
    print(f"\n{touched}/{len(tmx_files)} files modified")


# Curated whitelist: only these exact English strings get a TRUE shared key
# across nodes (same literal key value in the .tmx, not just the same
# translated text). This is a deliberate exception to this tool's default
# (every node keeps its own independent key) -- accepted only for pure
# navigation/button labels with no narrative content, where coupling several
# unrelated nodes to the same key carries negligible risk. Everything else,
# including short-but-narrative lines that happen to repeat today, keeps a
# private per-node key by default.
SHARED_LABELS = {
    "(Continue)": "adv.shared.continue_paren",
    "Continue": "adv.shared.continue",
    "Leave": "adv.shared.leave",
    "(Continue your search)": "adv.shared.continue_search",
    "(Chase after them!!!)": "adv.shared.chase",
    "Walk away": "adv.shared.walk_away",
    "Prepare yourself!": "adv.shared.prepare",
}


def cmd_share(args):
    # Rewrites every node whose text/name exactly matches one of
    # SHARED_LABELS so its loctext/locname points at that shared key instead
    # of its own private one. Requires "inject" to have already been run
    # (nodes must already have a private key to rewrite).
    tmx_files = resolve_scope(args.adventure_root, args.world)
    if not tmx_files:
        raise SystemExit(f"No .tmx files under {args.adventure_root / args.world}")

    key_rename = {}
    for tmx in tmx_files:
        with open(tmx, "r", encoding="utf-8", newline="") as f:
            content = f.read().replace("\r\n", "\n")
        for obj_id, obj_body in SIMPLE_OBJ_RE.findall(content):
            for prop_name, raw in SIMPLE_PROP_RE.findall(obj_body):
                try:
                    data = json.loads(html.unescape(raw))
                except json.JSONDecodeError:
                    continue
                roots = data if isinstance(data, list) else [data]

                def walk(node):
                    if not isinstance(node, dict):
                        return
                    text = node.get("text")
                    name = node.get("name")
                    loctext = node.get("loctext")
                    locname = node.get("locname")
                    if text in SHARED_LABELS and loctext:
                        key_rename[loctext] = SHARED_LABELS[text]
                    if name in SHARED_LABELS and locname:
                        key_rename[locname] = SHARED_LABELS[name]
                    for opt in node.get("options") or []:
                        walk(opt)

                for r in roots:
                    walk(r)

    key_rename = {k: v for k, v in key_rename.items() if k != v}
    if not key_rename:
        print("Nothing to share; no nodes with SHARED_LABELS text found (or already shared).")
        return

    touched = 0
    for tmx in tmx_files:
        with open(tmx, "r", encoding="utf-8", newline="") as f:
            content = f.read()
        original = content
        for old_key, new_key in key_rename.items():
            content = content.replace(f"&quot;{old_key}&quot;", f"&quot;{new_key}&quot;")
        if content != original:
            with open(tmx, "w", encoding="utf-8", newline="") as f:
                f.write(content)
            touched += 1

    print(f"{len(key_rename)} node keys rewritten to shared labels, across {touched} files")

    if args.properties and args.properties.exists():
        existing = {}
        for line in args.properties.read_text(encoding="utf-8").splitlines():
            if "=" in line and not line.startswith("#"):
                k, v = line.split("=", 1)
                existing[k] = v

        promoted = 0
        for old_key, new_key in key_rename.items():
            if old_key in existing and new_key not in existing:
                existing[new_key] = existing[old_key]
                promoted += 1
            if old_key in existing and old_key != new_key:
                del existing[old_key]

        lines = [f"{k}={v}" for k, v in sorted(existing.items())]
        args.properties.write_text("\n".join(lines) + "\n", encoding="utf-8")
        print(f"{promoted} translations promoted to shared keys, {len(existing)} total keys in {args.properties}")


def main():
    ap = argparse.ArgumentParser()
    sub = ap.add_subparsers(dest="command", required=True)

    p_inject = sub.add_parser("inject", help="Add missing loctext/locname keys to .tmx dialog nodes")
    p_inject.add_argument("--adventure-root", required=True, type=Path)
    p_inject.add_argument("--world", required=True, help="Subfolder under --adventure-root, e.g. common")
    p_inject.add_argument("--dry-run", action="store_true", help="Show what would change without writing anything")
    p_inject.set_defaults(func=cmd_inject)

    p_template = sub.add_parser("template", help="Create/update a translator-facing .properties file for a language")
    p_template.add_argument("--adventure-root", required=True, type=Path)
    p_template.add_argument("--world", required=True, help="Subfolder under --adventure-root, e.g. common")
    p_template.add_argument("--out", required=True, type=Path, help="e.g. Shandalar/languages/adventure-fr-FR.properties")
    p_template.set_defaults(func=cmd_template)

    p_reference = sub.add_parser("reference", help="Export a pure English key=text reference file")
    p_reference.add_argument("--adventure-root", required=True, type=Path)
    p_reference.add_argument("--world", required=True, help="Subfolder under --adventure-root, e.g. common")
    p_reference.add_argument("--out", required=True, type=Path)
    p_reference.set_defaults(func=cmd_reference)

    p_unique = sub.add_parser("unique", help="Export one row per distinct English string, to translate repeats only once")
    p_unique.add_argument("--adventure-root", required=True, type=Path)
    p_unique.add_argument("--world", required=True, help="Subfolder under --adventure-root, e.g. common")
    p_unique.add_argument("--out", required=True, type=Path, help="e.g. unique-strings-fr-FR.csv")
    p_unique.set_defaults(func=cmd_unique)

    p_fill = sub.add_parser("fill", help="Expand a filled-in 'unique' CSV back into a full per-key .properties file")
    p_fill.add_argument("--csv", required=True, type=Path, help="CSV produced by 'unique', with translations filled in")
    p_fill.add_argument("--out", required=True, type=Path, help="e.g. Shandalar/languages/adventure-fr-FR.properties")
    p_fill.set_defaults(func=cmd_fill)

    p_strip = sub.add_parser("strip", help="Remove only the loctext/locname keys this tool itself added (adv.* pattern)")
    p_strip.add_argument("--adventure-root", required=True, type=Path)
    p_strip.add_argument("--world", required=True, help="Subfolder under --adventure-root, e.g. common")
    p_strip.add_argument("--dry-run", action="store_true", help="Show what would change without writing anything")
    p_strip.set_defaults(func=cmd_strip)

    p_share = sub.add_parser("share", help="Rewrite a curated whitelist of generic labels (see SHARED_LABELS) to a shared key")
    p_share.add_argument("--adventure-root", required=True, type=Path)
    p_share.add_argument("--world", required=True, help="Subfolder under --adventure-root, e.g. common")
    p_share.add_argument("--properties", type=Path, help="Optional: also update this .properties file (promote translations, drop old keys)")
    p_share.set_defaults(func=cmd_share)

    args = ap.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
