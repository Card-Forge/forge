# Translating Adventure Mode

This guide covers everything needed to add a new language to Adventure Mode.
There are three separate translation systems, covered in order below: the
main UI, quests/shops, and NPC dialog embedded in maps.

## Where translated files live

Every translated file for a given plane/world lives in ONE folder:

```
forge-gui/res/adventure/<World>/languages/
```

For example, Shandalar's Spanish translations all live together:

```
forge-gui/res/adventure/Shandalar/languages/
  adventure-es-ES.properties   NPC dialog (see part 3 below)
  quests-es-ES.json            Quest text (see part 2 below)
  shops-es-ES.json             Shop text (see part 2 below)
```

Filenames never repeat the world's name — the folder already identifies it.
This keeps every language file for a world in one predictable place instead
of scattered next to whatever original file each one happens to translate.

## 1. Main UI (menus, options, general strings)

If Forge already ships a `<lang>.properties` file under `forge-gui/res/languages/`
for your language, there's nothing to do here — it's covered by Forge's
existing localization system, unrelated to Adventure Mode.

## 2. Quests and shops

Quest and shop text is translated by hand — there's no tooling for this part.

1. Find the English source files, e.g.:
   ```
   forge-gui/res/adventure/Shandalar/world/quests.json
   forge-gui/res/adventure/Shandalar/world/shops.json
   ```
2. Copy each one into that plane's `languages/` folder, renamed with the
   locale suffix:
   ```
   forge-gui/res/adventure/Shandalar/languages/quests-es-ES.json
   forge-gui/res/adventure/Shandalar/languages/shops-es-ES.json
   ```
3. Translate the readable text fields inside. Leave keys, IDs, and card
   references untouched — only translate text a player would actually read.
4. That's it — `Config.getFile()` finds it automatically once the UI
   language matches. There's no fallback within this file, so make sure the
   whole thing is translated before shipping it.

## 3. NPC dialog in maps (`.tmx` files)

This part is covered by `adventure_i18n.py`, in `forge-gui/tools/`. Dialog
trees are embedded as JSON inside each `.tmx` map. Each node that needs
translating gets a stable key (`loctext`/`locname`) that survives the tree
being reordered later — a translation never silently ends up attached to the
wrong node just because someone edited a map.

Edits are surgical: only the exact spot where a key gets added changes.
Nothing else in the file — including unrelated content elsewhere in the same
map — gets reformatted or reordered.

`--world` is the folder under `--adventure-root` holding the actual `.tmx`
content (e.g. `common`, where Shandalar's maps live) — it doesn't have to
match the plane the translations ship under. Point `--out` at whichever
plane actually plays that content.

### If the world already has keys assigned (e.g. `common`, covered by this PR)

You don't need to run `inject` — the keys already exist. Generate a
translation template and fill it in:

```
python adventure_i18n.py template --adventure-root ..\res\adventure --world common --out ..\res\adventure\Shandalar\languages\adventure-fr-FR.properties
```

This creates the file with every existing key, pre-filled with English as a
placeholder. Translate each line — the key stays on the left of the `=`,
only the value on the right changes.

Running the same command again later (e.g. once new dialog is added) is
safe: it keeps whatever's already translated and only adds new keys.

### If the world doesn't have keys yet

Run `inject` first, once:

```
python adventure_i18n.py inject --adventure-root ..\res\adventure --world <world_name>
```

Then generate the template exactly as above.

### Translating repeated strings only once

Some strings ("(Continue)", "Leave"...) show up on dozens of unrelated
nodes. To translate one of these only once instead of retyping it every
time:

```
python adventure_i18n.py unique --adventure-root ..\res\adventure --world common --out unique-strings-fr-FR.csv
```

This collapses every key sharing identical English text into one CSV row.
Fill in the `translation` column (rows can be left blank and finished
later), then expand it into the full file:

```
python adventure_i18n.py fill --csv unique-strings-fr-FR.csv --out ..\res\adventure\Shandalar\languages\adventure-fr-FR.properties
```

`fill` only touches rows with a translation filled in, and never overwrites
a key that's already translated in `--out`. This doesn't change how
translations are stored at runtime — the file still has one independent
line per key, same as `template` would produce.

### A small set of labels share a key for real

A short, curated whitelist of purely generic navigation labels — "(Continue)",
"Leave", and a few others (see `SHARED_LABELS` in the script) — are the one
deliberate exception: every node with that exact English text points at the
SAME key, rather than each getting its own. This is only safe because these
are pure UI labels with no narrative content; everything else keeps an
independent key by default, specifically to avoid two unrelated dialog lines
ever silently affecting each other.

```
python adventure_i18n.py share --adventure-root ..\res\adventure --world common --properties ..\res\adventure\Shandalar\languages\adventure-es-ES.properties
```

This rewrites the affected `.tmx` nodes to point at the shared key, and (if
`--properties` is given) promotes one existing translation to that shared
key while dropping the old per-node entries. Safe to run again later —
already-shared nodes are left alone.

### Optional: a plain English reference

For a clean read-only English file to check against, instead of translating
a template file directly:

```
python adventure_i18n.py reference --adventure-root ..\res\adventure --world common --out adventure-en-US-reference.properties
```

### Regenerating keys cleanly

If this tool itself gets updated and you need to regenerate keys (rather
than manually reverting files with git — which risks discarding unrelated
changes other contributors made to the same files in the meantime), use:

```
python adventure_i18n.py strip --adventure-root ..\res\adventure --world common
python adventure_i18n.py inject --adventure-root ..\res\adventure --world common
```

`strip` removes only the keys this tool itself added (recognized by their
`adv.*` pattern), leaving everything else — including anyone else's
unrelated edits — untouched.

## 4. Testing

Switch Forge's UI language to the one you translated, start Adventure Mode,
and load the world you translated. Any dialog line still missing a
translation falls back to English automatically — nothing breaks, so partial
translations are safe to test and ship incrementally. This applies the same
way to a save file created in a different language: whatever language Forge
is set to when you load a save is what gets used, regardless of what
language the save was originally played in.

## Notes

- `--world` is always required for every `adventure_i18n.py` command — a run
  never touches more than one world at a time.
- English devs adding new dialog don't need to do anything with this
  process; untranslated content just displays in English until someone runs
  `inject` and translates it.
- `.tmx` files with pre-existing malformed JSON (unrelated to this tool) are
  detected and left untouched rather than risking a bad edit.
- Adventure translations are loaded on demand by Adventure Mode code itself
  (selecting a plane, loading a save, starting a new game) — never
  automatically by Forge's core `Localizer`, which stays unaware of Adventure
  Mode entirely. A plane with no translation file for the active language
  simply shows English; nothing scans or depends on Adventure assets outside
  of Adventure Mode itself.
