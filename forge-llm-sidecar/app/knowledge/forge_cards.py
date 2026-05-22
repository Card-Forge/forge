"""Resolve scraped card names to canonical Forge card names.

MTGGoldfish (and other sources) spell some cards differently from Forge — most
visibly multi-face cards. A Room/Split card like *Roaring Furnace // Steaming
Sauna* comes through as ``Roaring Furnace/Steaming Sauna`` (bare slash), which
Forge's deck loader rejects with "unsupported card", silently dropping it.

The fix: treat **Forge's own card database as ground truth**. We scan the
``cardsfolder`` once, build a normalized name index, and map each scraped name to
the exact string Forge expects. Resolution order per name:

1. exact canonical match,
2. normalized match (separator-/accent-/punctuation-insensitive) — this is what
   catches ``A/B`` → ``A // B``, curly vs. straight apostrophes, accents, etc.,
3. a hand-maintained override map (``card_name_overrides.json``) for the rare
   case heuristics can't reach.

Only **Split**-type faces combine into an ``A // B`` name in Forge
(``CardSplitType.COMBINE``); Transform/Modal/Adventure/Flip/etc. are named by
their front face alone. We mirror that here so a scraped ``Front // Back`` for an
MDFC resolves to just ``Front``.

This runs only in the offline import path (``scripts/import_goldfish_decks.py``),
never on the sidecar request path. If the cardsfolder can't be located it fails
soft: names pass through unchanged and the caller is told nothing resolved.
"""

from __future__ import annotations

import json
import logging
import os
import re
import unicodedata
from functools import lru_cache
from pathlib import Path

log = logging.getLogger(__name__)

# Forge composes a combined "A // B" name only for Split-type cards
# (CardSplitType.Split -> COMBINE). Every other multi-face kind is named by its
# front face, so we index the front name as canonical for those.
_COMBINE_ALT_MODE = "split"

_OVERRIDES_PATH = Path(__file__).parent / "card_name_overrides.json"


def cardsfolder_path() -> Path | None:
    """Locate Forge's ``cardsfolder``: ``$FORGE_CARDSFOLDER`` or the monorepo default."""
    env = os.environ.get("FORGE_CARDSFOLDER", "").strip()
    if env:
        p = Path(env)
        return p if p.is_dir() else None
    # forge-llm-sidecar/app/knowledge/forge_cards.py -> repo root is parents[3]
    default = Path(__file__).resolve().parents[3] / "forge-gui" / "res" / "cardsfolder"
    return default if default.is_dir() else None


def normalize(name: str) -> str:
    """Separator-, accent-, and punctuation-insensitive key for matching.

    ``"Roaring Furnace // Steaming Sauna"``, ``"Roaring Furnace/Steaming Sauna"``
    and ``"roaring furnace  steaming sauna"`` all collapse to the same key. Face
    delimiters (``//`` or ``/``) become spaces so a combined name and its bare
    concatenation match; remaining punctuation (apostrophes, commas, em-dashes)
    is dropped.
    """
    # Fold accents: "Lim-Dûl" -> "Lim-Dul".
    folded = unicodedata.normalize("NFKD", name)
    folded = "".join(c for c in folded if not unicodedata.combining(c))
    folded = folded.casefold()
    folded = folded.replace("//", " ").replace("/", " ")
    # Anything that isn't a letter/digit/space becomes a space.
    folded = re.sub(r"[^0-9a-z ]+", " ", folded)
    return re.sub(r"\s+", " ", folded).strip()


def _parse_card_file(text: str) -> tuple[str, list[str]] | None:
    """Return ``(alt_mode, [face_names])`` for a card .txt, or None if unnamed."""
    names: list[str] = []
    alt_mode = ""
    for raw in text.splitlines():
        if raw.startswith("Name:"):
            names.append(raw[len("Name:") :].strip())
        elif raw.startswith("AlternateMode:") and not alt_mode:
            alt_mode = raw[len("AlternateMode:") :].strip().casefold()
    if not names:
        return None
    return alt_mode, names


def _canonical_name(alt_mode: str, faces: list[str]) -> str:
    """The exact name Forge uses: ``A // B`` only for Split, else the front face."""
    if alt_mode == _COMBINE_ALT_MODE and len(faces) >= 2:
        return f"{faces[0]} // {faces[1]}"
    return faces[0]


def _load_overrides() -> dict[str, str]:
    """Optional hand-maintained ``{scraped_name: forge_name}`` map (normalized keys)."""
    try:
        raw = json.loads(_OVERRIDES_PATH.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return {}
    return {normalize(k): v for k, v in raw.items() if isinstance(v, str)}


@lru_cache(maxsize=1)
def build_index() -> dict[str, str]:
    """Build (and cache) a ``{normalized_name: canonical_forge_name}`` index.

    Authoritative primary names are inserted first; alias keys (combined name,
    individual back faces) only fill gaps via ``setdefault`` so a real single-face
    card (e.g. "Fire") is never shadowed by a split face of the same spelling.
    """
    folder = cardsfolder_path()
    index: dict[str, str] = {}
    if folder is None:
        log.warning("forge_cards: cardsfolder not found; card names will pass through unresolved")
        return index

    aliases: list[tuple[str, str]] = []
    for path in folder.rglob("*.txt"):
        try:
            parsed = _parse_card_file(path.read_text(encoding="utf-8"))
        except OSError:
            continue
        if parsed is None:
            continue
        alt_mode, faces = parsed
        canonical = _canonical_name(alt_mode, faces)
        # Primary face name -> canonical is authoritative.
        index[normalize(faces[0])] = canonical
        # Aliases (filled in only where they don't shadow a primary name):
        # the "Front // Back" combined spelling sources use even for non-Split
        # MDFCs, plus each back face on its own.
        if len(faces) >= 2:
            aliases.append((normalize(f"{faces[0]} // {faces[1]}"), canonical))
        for face in faces[1:]:
            aliases.append((normalize(face), canonical))

    for key, canonical in aliases:
        index.setdefault(key, canonical)

    index.update(_load_overrides())
    log.info("forge_cards: indexed %d names from %s", len(index), folder)
    return index


def resolve(name: str) -> str | None:
    """Return the canonical Forge name for ``name``, or None if unknown to Forge."""
    index = build_index()
    if not index:
        return None
    if (key := normalize(name)) in index:
        return index[key]
    return None


def resolve_or_keep(name: str) -> tuple[str, bool]:
    """``(resolved_name, matched)`` — falls back to the original name when unmatched."""
    resolved = resolve(name)
    return (resolved, True) if resolved is not None else (name, False)
