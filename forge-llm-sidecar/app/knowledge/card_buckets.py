"""Offline decklist-to-profile bucket generation.

Runtime nodes consume archetype profiles, but those profiles should be built
from real metagame decklists rather than only primer prose. This module parses
Forge ``.dck`` files, optionally enriches cards through a local Scryfall cache,
classifies every listed card into strategist buckets, and emits coverage data
for review.
"""

from __future__ import annotations

import datetime as dt
import json
import logging
import re
from collections import Counter, defaultdict
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

import httpx

from app.knowledge import guidance_migration, loader, piloting

log = logging.getLogger(__name__)

ROOT = Path(__file__).resolve().parents[2]
DECKLIST_DIR = ROOT / "app" / "knowledge" / "decklists"
CARD_CACHE_PATH = ROOT / "app" / "knowledge" / "card_cache" / "cards.json"
REPORT_DIR = ROOT / "reports"
PROFILE_DIR = ROOT / "app" / "knowledge" / "archetype_profiles"
PILOTING_DIR = ROOT / "app" / "knowledge" / "piloting"
SCRYFALL_COLLECTION_URL = "https://api.scryfall.com/cards/collection"

_CARD_LINE_RE = re.compile(r"^(\d+)\s+(.+)$")
_SET_SUFFIX_RE = re.compile(r"\s+\|[A-Z0-9]{2,6}$")
_COLOR_ORDER = ("W", "U", "B", "R", "G")


@dataclass
class DeckEntry:
    qty: int
    name: str


@dataclass
class NormalizedDecklist:
    archetype: str
    format: str
    source_file: str
    mainboard: list[DeckEntry] = field(default_factory=list)
    sideboard: list[DeckEntry] = field(default_factory=list)

    def all_card_names(self) -> list[str]:
        seen: set[str] = set()
        out: list[str] = []
        for entry in self.mainboard + self.sideboard:
            key = _norm(entry.name)
            if key and key not in seen:
                out.append(entry.name)
                seen.add(key)
        return out


def _norm(name: str) -> str:
    return "".join(ch for ch in (name or "").casefold() if ch.isalnum())


def _clean_card_name(name: str) -> str:
    return _SET_SUFFIX_RE.sub("", name).strip()


def parse_dck(path: Path, *, fmt: str | None = None, archetype: str | None = None) -> NormalizedDecklist:
    """Parse a Forge ``.dck`` file into mainboard/sideboard entries."""
    name = archetype or path.stem.replace("-", " ").title()
    deck_format = (fmt or path.parent.name).strip().lower()
    main: list[DeckEntry] = []
    side: list[DeckEntry] = []
    section: str | None = None
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line:
            continue
        lower = line.lower()
        if line.startswith("[") and line.endswith("]"):
            if lower == "[main]":
                section = "main"
            elif lower == "[sideboard]":
                section = "sideboard"
            else:
                section = None
            continue
        if lower.startswith("name="):
            name = archetype or line.split("=", 1)[1].strip() or name
            continue
        match = _CARD_LINE_RE.match(line)
        if not match or section not in {"main", "sideboard"}:
            continue
        entry = DeckEntry(int(match.group(1)), _clean_card_name(match.group(2)))
        if section == "main":
            main.append(entry)
        else:
            side.append(entry)
    return NormalizedDecklist(name, deck_format, str(path), main, side)


def decklist_to_json(deck: NormalizedDecklist) -> dict:
    return {
        "archetype": deck.archetype,
        "format": deck.format,
        "source_file": deck.source_file,
        "mainboard": [{"qty": e.qty, "name": e.name} for e in deck.mainboard],
        "sideboard": [{"qty": e.qty, "name": e.name} for e in deck.sideboard],
    }


def write_decklist_json(deck: NormalizedDecklist, *, out_dir: Path = DECKLIST_DIR) -> Path:
    path = out_dir / deck.format / f"{piloting.slugify(deck.archetype)}.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(decklist_to_json(deck), indent=2) + "\n", encoding="utf-8")
    return path


def load_card_cache(path: Path = CARD_CACHE_PATH) -> dict[str, dict]:
    if not path.exists():
        return {}
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError) as exc:
        log.warning("card_buckets: failed to read card cache %s: %s", path, exc)
        return {}
    cards = payload.get("cards", payload)
    return cards if isinstance(cards, dict) else {}


def write_card_cache(cache: dict[str, dict], path: Path = CARD_CACHE_PATH) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "generated_at": dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
        "cards": dict(sorted(cache.items())),
    }
    path.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def fetch_scryfall_metadata(names: list[str], cache: dict[str, dict], *, timeout: float = 30.0) -> dict[str, dict]:
    """Fetch missing card metadata through Scryfall's collection endpoint."""
    missing = [name for name in names if _norm(name) not in cache]
    if not missing:
        return cache
    with httpx.Client(timeout=timeout) as client:
        for start in range(0, len(missing), 75):
            chunk = missing[start : start + 75]
            resp = client.post(
                SCRYFALL_COLLECTION_URL,
                json={"identifiers": [{"name": name} for name in chunk]},
            )
            resp.raise_for_status()
            payload = resp.json()
            for card in payload.get("data", []):
                name = str(card.get("name") or "")
                if not name:
                    continue
                faces = card.get("card_faces") or []
                oracle_text = str(card.get("oracle_text") or "")
                type_line = str(card.get("type_line") or "")
                if faces:
                    oracle_text = "\n".join(str(face.get("oracle_text") or "") for face in faces)
                    type_line = " // ".join(str(face.get("type_line") or "") for face in faces)
                cache[_norm(name)] = {
                    "name": name,
                    "type_line": type_line,
                    "oracle_text": oracle_text,
                    "colors": card.get("colors") or [],
                    "color_identity": card.get("color_identity") or [],
                    "produced_mana": card.get("produced_mana") or [],
                    "keywords": card.get("keywords") or [],
                }
            for item in payload.get("not_found", []):
                miss = str(item.get("name") or "")
                if miss:
                    cache.setdefault(_norm(miss), {"name": miss})
    return cache


def _guide_payload(deck: NormalizedDecklist) -> dict | None:
    path = PILOTING_DIR / deck.format / f"{piloting.slugify(deck.archetype)}.json"
    if not path.exists():
        return None
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return None


def _existing_profile(deck: NormalizedDecklist) -> dict | None:
    path = PROFILE_DIR / deck.format / f"{piloting.slugify(deck.archetype)}.json"
    if not path.exists():
        return None
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return None


def _role_hints(card: str, guide: dict | None, profile: dict | None) -> set[str]:
    hints: set[str] = set()
    key = _norm(card)
    combo_profile = (guide or {}).get("combo_profile") if isinstance((guide or {}).get("combo_profile"), dict) else {}
    category_cards = combo_profile.get("category_cards") if isinstance(combo_profile.get("category_cards"), dict) else {}
    combo_map = {
        "reducers": {"mana_reducers", "engines", "combo_pieces"},
        "rituals": {"rituals", "combo_pieces", "graveyard_enablers"},
        "draw_engine": {"card_advantage", "dig_draw", "engines"},
        "recursion": {"recursion", "graveyard_enablers", "engines"},
        "payoff": {"payoff_cards", "win_conditions", "combo_pieces"},
        "tutor": {"tutors_wildcards"},
        "protection": {"protection", "counterspells"},
    }
    for category, cards in category_cards.items():
        if any(_norm(str(c)) == key for c in (cards or [])):
            hints.update(combo_map.get(str(category), {"combo_pieces"}))
    for line in combo_profile.get("known_lines") or []:
        if isinstance(line, dict) and any(_norm(str(c)) == key for c in (line.get("key_cards") or [])):
            hints.add("combo_pieces")
    for item in (guide or {}).get("key_cards") or []:
        if not isinstance(item, dict) or _norm(str(item.get("name", ""))) != key:
            continue
        text = f"{item.get('role', '')} {item.get('notes', '')}".casefold()
        hints.update(_classify_text_roles(text))
    for bucket, payload in ((profile or {}).get("buckets") or {}).items():
        if bucket not in loader.PROFILE_BUCKETS or not isinstance(payload, dict):
            continue
        if any(_norm(str(c)) == key for c in (payload.get("cards") or [])):
            hints.add(bucket)
    return hints & set(loader.PROFILE_BUCKETS)


def _classify_text_roles(text: str) -> set[str]:
    roles: set[str] = set()
    t = text.casefold()
    if any(w in t for w in ("reduce", "cost less", "discount", "reducer")):
        roles.add("mana_reducers")
    if any(w in t for w in ("ritual", "add mana", "mana burst")):
        roles.add("rituals")
    if any(w in t for w in ("draw", "card advantage", "impulse")):
        roles.add("card_advantage")
    if any(w in t for w in ("scry", "surveil", "cantrip", "loot", "dig", "look at the top")):
        roles.add("dig_draw")
    if any(w in t for w in ("search your library", "tutor", "wish")):
        roles.add("tutors_wildcards")
    if any(w in t for w in ("win", "lethal", "finisher", "payoff")):
        roles.update({"win_conditions", "payoff_cards"})
    if any(w in t for w in ("destroy", "exile target", "damage to", "deals damage", "return target", "bounce")):
        roles.add("removal")
    if any(w in t for w in ("destroy all", "each creature", "all creatures", "sweeper")):
        roles.add("wrath")
    if "counter target" in t or "counterspell" in t:
        roles.add("counterspells")
    if any(w in t for w in ("hexproof", "indestructible", "protection", "can't be countered", "ward")):
        roles.add("protection")
    if any(w in t for w in ("discard a card", "discard outlet")):
        roles.add("discard_outlets")
    if any(w in t for w in ("graveyard", "mill", "surveil", "flashback", "escape")):
        roles.add("graveyard_enablers")
    if any(w in t for w in ("return", "from your graveyard", "cast from your graveyard", "flashback")):
        roles.add("recursion")
    if any(w in t for w in ("engine", "whenever you cast", "storm count")):
        roles.add("engines")
    if any(w in t for w in ("combo", "piece")):
        roles.add("combo_pieces")
    if any(w in t for w in ("graveyard hate", "can't cast", "can't activate", "prevent", "tax")):
        roles.add("hate_pieces")
    if any(w in t for w in ("mana fixing", "any color", "add one mana of any color")):
        roles.add("mana_fixing")
    return roles


def classify_card(
    card: str,
    metadata: dict | None,
    *,
    guide: dict | None = None,
    profile: dict | None = None,
    sideboard_only: bool = False,
) -> set[str]:
    roles = _role_hints(card, guide, profile)
    meta = metadata or {}
    type_line = str(meta.get("type_line") or "")
    oracle = str(meta.get("oracle_text") or "")
    text = f"{card} {type_line} {oracle}"
    roles.update(_classify_text_roles(text))

    lower_type = type_line.casefold()
    if "land" in lower_type or _looks_like_land(card):
        roles.add("lands")
        produced = set(meta.get("produced_mana") or [])
        if len(produced & set(_COLOR_ORDER)) > 1 or "any color" in oracle.casefold() or not _is_basic_land(card):
            roles.add("mana_fixing")
    if "creature" in lower_type:
        roles.add("threats")
    if "planeswalker" in lower_type or "battle" in lower_type:
        roles.update({"threats", "planeswalker_threats"})
    if "add " in oracle.casefold() and "mana" in oracle.casefold():
        if "instant" in lower_type or "sorcery" in lower_type:
            roles.add("rituals")
        else:
            roles.add("mana_fixing")
    if sideboard_only and not roles:
        roles.add("hate_pieces")
    if not roles and not sideboard_only:
        roles.add("threats")
    return roles & set(loader.PROFILE_BUCKETS)


def _looks_like_land(card: str) -> bool:
    lower = card.casefold()
    return lower in {"plains", "island", "swamp", "mountain", "forest", "wastes"} or any(
        w in lower for w in ("verge", "vents", "canal", "foundry", "garden", "passage", "coast", "courtyard", "tunnel")
    )


def _is_basic_land(card: str) -> bool:
    return card.casefold() in {"plains", "island", "swamp", "mountain", "forest", "wastes"}


def build_profile_from_decklist(deck: NormalizedDecklist, *, card_cache: dict[str, dict] | None = None) -> tuple[dict, dict]:
    """Return ``(profile, coverage)`` for one normalized decklist."""
    cache = card_cache or {}
    guide = _guide_payload(deck)
    existing = _existing_profile(deck)
    metagame = guidance_migration.load_metagame(deck.format).get(piloting.slugify(deck.archetype))
    if existing:
        profile = json.loads(json.dumps(existing))
    elif guide:
        profile = guidance_migration.build_archetype_profile(guide, metagame)
    else:
        profile = {
            "schema_version": 1,
            "name": deck.archetype,
            "format": deck.format,
            "colors": [],
            "strategy_type": "midrange",
            "expected_deck_total": sum(e.qty for e in deck.mainboard),
            "macro_plan": "",
            "win_turn_window": [6, 12],
            "tells": [],
            "predicted_lines": [],
            "kill_priority": [],
            "interaction_to_disrupt": [],
        }

    main_counts = Counter({e.name: e.qty for e in deck.mainboard})
    side_counts = Counter({e.name: e.qty for e in deck.sideboard})
    bucket_cards: dict[str, dict[str, dict]] = {bucket: {} for bucket in loader.PROFILE_BUCKETS}
    card_roles: dict[str, set[str]] = {}
    unbucketed: list[str] = []
    for card in deck.all_card_names():
        sideboard_only = main_counts.get(card, 0) == 0 and side_counts.get(card, 0) > 0
        roles = classify_card(
            card,
            cache.get(_norm(card)),
            guide=guide,
            profile=existing,
            sideboard_only=sideboard_only,
        )
        if not roles:
            unbucketed.append(card)
            continue
        card_roles[card] = roles
        for role in roles:
            bucket_cards[role][card] = {
                "mainboard_count": main_counts.get(card, 0),
                "sideboard_count": side_counts.get(card, 0),
            }

    buckets: dict[str, Any] = {}
    for bucket in loader.PROFILE_BUCKETS:
        entries = bucket_cards[bucket]
        cards = sorted(entries)
        buckets[bucket] = {
            "target_count": sum(v["mainboard_count"] + v["sideboard_count"] for v in entries.values()),
            "cards": cards,
            "mainboard_count": sum(v["mainboard_count"] for v in entries.values()),
            "sideboard_count": sum(v["sideboard_count"] for v in entries.values()),
        }
    interaction_cards = (
        set(buckets["removal"]["cards"])
        | set(buckets["counterspells"]["cards"])
        | set(buckets["wrath"]["cards"])
        | set(buckets["protection"]["cards"])
    )
    total_unique = max(1, len(deck.all_card_names()))
    buckets["interaction_density"] = round(min(1.0, len(interaction_cards) / total_unique), 2)
    profile["buckets"] = buckets
    profile["name"] = deck.archetype
    profile["format"] = deck.format
    profile["expected_deck_total"] = sum(e.qty for e in deck.mainboard)
    profile["dual_role_cards"] = [
        {"card": card, "roles": sorted(roles), "notes": ""}
        for card, roles in sorted(card_roles.items())
        if len(roles) > 1
    ]
    profile["kill_priority"] = _priority_cards(
        buckets,
        ("engines", "win_conditions", "payoff_cards", "combo_pieces", "planeswalker_threats", "threats"),
    )
    profile["interaction_to_disrupt"] = _priority_cards(
        buckets,
        ("counterspells", "protection", "removal", "wrath", "hate_pieces"),
    )
    profile["decklist_source"] = {
        "source_file": deck.source_file,
        "generated_at": dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
    }
    profile["last_updated"] = dt.date.today().isoformat()

    bucketed_unique = len(card_roles)
    coverage = {
        "format": deck.format,
        "archetype": deck.archetype,
        "source_file": deck.source_file,
        "mainboard_cards": sum(e.qty for e in deck.mainboard),
        "sideboard_cards": sum(e.qty for e in deck.sideboard),
        "unique_cards": len(deck.all_card_names()),
        "bucketed_unique_cards": bucketed_unique,
        "coverage": round(bucketed_unique / total_unique, 3),
        "unbucketed_cards": sorted(unbucketed),
        "multi_role_cards": sorted(card for card, roles in card_roles.items() if len(roles) > 1),
    }
    return profile, coverage


def _priority_cards(buckets: dict[str, Any], bucket_order: tuple[str, ...], *, limit: int = 8) -> list[str]:
    out: list[str] = []
    seen: set[str] = set()
    for bucket in bucket_order:
        payload = buckets.get(bucket) or {}
        for card in payload.get("cards") or []:
            key = _norm(card)
            if key in seen:
                continue
            out.append(card)
            seen.add(key)
            if len(out) >= limit:
                return out
    return out


def write_profile(profile: dict) -> Path:
    path = PROFILE_DIR / profile["format"] / f"{piloting.slugify(profile['name'])}.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(profile, indent=2) + "\n", encoding="utf-8")
    loader._load_profile_file.cache_clear()
    loader._reverse_index_for.cache_clear()
    return path


def write_coverage_report(rows: list[dict], *, fmt: str, out_dir: Path = REPORT_DIR) -> Path:
    out_dir.mkdir(parents=True, exist_ok=True)
    path = out_dir / f"card_bucket_coverage_{fmt}.md"
    lines = [
        f"# Card Bucket Coverage: {fmt}",
        "",
        "| Archetype | Unique | Bucketed | Coverage | Unbucketed | Multi-role |",
        "|---|---:|---:|---:|---|---:|",
    ]
    for row in sorted(rows, key=lambda r: r["archetype"]):
        unbucketed = ", ".join(row["unbucketed_cards"]) if row["unbucketed_cards"] else "-"
        lines.append(
            f"| {row['archetype']} | {row['unique_cards']} | {row['bucketed_unique_cards']} | "
            f"{row['coverage']:.0%} | {unbucketed} | {len(row['multi_role_cards'])} |"
        )
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return path
