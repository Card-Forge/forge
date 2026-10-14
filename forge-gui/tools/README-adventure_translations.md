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

## 1. Main UI (menus, options, general strings)

If Forge already ships a `<lang>.properties` file under `forge-gui/res/languages/`
for your language, there's nothing to do here — it's covered by Forge's
existing localization system, unrelated to Adventure Mode.

## 2. Quests and shops

Translated by hand — there's no tooling for this part.

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
3. Translate the readable text fields. Leave keys, IDs, and card references
   untouched.
4. `Config.getFile()` finds it automatically once the UI language matches.
   There's no fallback within this file, so translate it fully before
   shipping.

## 3. NPC dialog in maps (`.tmx` files)

Covered by `adventure_i18n.py`, in `forge-gui/tools/`. Dialog trees are
embedded as JSON inside each `.tmx` map. Each node that needs translating
gets a stable key (`loctext`/`locname`) that survives the tree being
reordered later.

Edits are surgical: the tool locates the exact position in the original text
and inserts only what's needed there. It never parses-and-rewrites the
surrounding JSON, so unrelated content — including formatting choices like
single-line vs multi-line objects — is never touched.

`--world` is the folder under `--adventure-root` holding the actual `.tmx`
content (e.g. `common`, where Shandalar's maps live) — it doesn't have to
match the plane the translations ship under. Point `--out`/`--properties` at
whichever plane actually plays that content.

### Starting a translation

If the world already has keys assigned (e.g. `common`, covered by this PR),
skip straight to `template`. Otherwise, run `inject` first:

```
python adventure_i18n.py inject --adventure-root ..\res\adventure --world common
```

Then generate the translation file, pre-filled with English:

```
python adventure_i18n.py template --adventure-root ..\res\adventure --world common --out ..\res\adventure\Shandalar\languages\adventure-fr-FR.properties
```

Translate each line — the key stays on the left of the `=`. Running
`template` again later (e.g. once new dialog is added) is safe: it keeps
what's translated and only adds new keys.

### Translating repeated strings only once

Some strings show up on dozens of unrelated nodes. To handle one of these
only once instead of retyping it every time, collapse into a CSV first:

```
python adventure_i18n.py unique --adventure-root ..\res\adventure --world common --out unique-strings-fr-FR.csv
```

Fill in the `translation` column (rows can be left blank and finished
later), then expand it into the full file:

```
python adventure_i18n.py fill --csv unique-strings-fr-FR.csv --out ..\res\adventure\Shandalar\languages\adventure-fr-FR.properties
```

This only saves typing — the resulting file still has one independent line
per key, same as `template` produces.

### A small set of labels share a key for real

A short, curated whitelist of purely generic navigation labels — "(Continue)",
"Leave", and a few others (see `SHARED_LABELS` in the script) — is the one
deliberate exception: every node with that exact English text points at the
SAME key, instead of each getting its own. This is only safe because these
are pure UI labels with no narrative content; everything else keeps an
independent key by default.

```
python adventure_i18n.py share --adventure-root ..\res\adventure --world common --properties ..\res\adventure\Shandalar\languages\adventure-es-ES.properties
```

Rewrites the affected `.tmx` nodes to point at the shared key, and (if
`--properties` is given) promotes one existing translation to it while
dropping the old per-node entries. Safe to run again later.

### Optional: a plain English reference

```
python adventure_i18n.py reference --adventure-root ..\res\adventure --world common --out adventure-en-US-reference.properties
```

A clean, read-only English file to check against, never merged with an
existing translation.

### Regenerating keys cleanly

If this tool gets updated and keys need regenerating, don't manually revert
files with git — that risks discarding unrelated changes other contributors
made in the meantime. Instead:

```
python adventure_i18n.py strip --adventure-root ..\res\adventure --world common
python adventure_i18n.py inject --adventure-root ..\res\adventure --world common
```

`strip` removes only the keys this tool itself added (recognized by their
`adv.*` pattern), leaving everything else untouched.

## 4. Testing

Switch Forge's UI language, start Adventure Mode, and load the translated
world. Any line still missing a translation falls back to English
automatically — partial translations are safe to test and ship
incrementally. Loading an existing save uses whatever language Forge is
currently set to, regardless of what language the save was originally
played in.

## Notes

- `--world` is always required — a run never touches more than one world.
- English devs adding new dialog don't need to do anything; untranslated
  content just shows in English until someone runs `inject`.
- `.tmx` files with pre-existing malformed JSON are detected and left
  untouched.
- Adventure translations are loaded on demand by Adventure Mode code itself
  (selecting a plane, loading a save, starting a new game) — never
  automatically by Forge's core `Localizer`, which stays unaware of
  Adventure Mode entirely.
