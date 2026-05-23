"""Bridge legacy ``guidance/*.json`` into runtime piloting/profile data.

The sidecar no longer reads ``guidance/<format>.json`` at runtime.  This module
keeps that older generated data useful by treating it as an offline enrichment
source for the current ``piloting/`` guides and ``archetype_profiles/``.
"""

from __future__ import annotations

import datetime as dt
import json
import re
from pathlib import Path
from typing import Any

from app.knowledge.loader import PROFILE_BUCKETS
from app.knowledge.piloting import slugify
from app.knowledge.piloting_schema import PilotingGuide, StrategyType

ROOT = Path(__file__).resolve().parents[2]
GUIDANCE_DIR = ROOT / "guidance"
PILOTING_DIR = ROOT / "app" / "knowledge" / "piloting"
PROFILE_DIR = ROOT / "app" / "knowledge" / "archetype_profiles"
METAGAME_DIR = ROOT / "app" / "knowledge" / "metagame_data"

_STRATEGY_KEYWORDS = {
    StrategyType.COMBO: ("combo", "engine", "chain", "infinite", "storm"),
    StrategyType.CONTROL: ("control", "counter", "sweeper", "draw-go"),
    StrategyType.RAMP: ("ramp", "landfall", "mana", "big mana"),
    StrategyType.AGGRO: ("aggro", "aggressive", "burn", "curve out"),
    StrategyType.TEMPO: ("tempo", "prowess", "spell", "delver"),
    StrategyType.MIDRANGE: ("midrange", "value", "grind"),
}

_BUCKET_KEYWORDS = {
    "mana_reducers": ("reduce", "cost reducer", "discount"),
    "rituals": ("ritual", "mana burst", "treasure"),
    "card_advantage": ("draw", "card advantage", "grind", "value"),
    "dig_draw": ("cantrip", "loot", "filter", "dig", "draw"),
    "tutors_wildcards": ("tutor", "search"),
    "win_conditions": ("win", "finisher", "lethal", "payoff", "clock"),
    "threats": ("threat", "creature", "pressure", "clock", "attacker"),
    "removal": ("spot removal", "removal spell", "burn spell", "destroy target", "bounce spell"),
    "wrath": ("sweeper", "wrath"),
    "counterspells": ("counterspell", "countermagic"),
    "protection": ("protect", "protection", "hexproof", "ward"),
    "discard_outlets": ("discard", "loot"),
    "graveyard_enablers": ("graveyard", "mill", "bin", "discard"),
    "reanimation_targets": ("reanimate", "reanimation"),
    "recursion": ("return", "recur", "recursion", "buyback"),
    "engines": ("engine", "repeat", "snowball"),
    "payoff_cards": ("payoff", "reward"),
    "combo_pieces": ("combo", "piece", "engine"),
    "hate_pieces": ("hate", "sideboard"),
    "mana_fixing": ("fix", "dual", "land", "mana"),
    "lands": (),
    "planeswalker_threats": ("planeswalker",),
}

_LAND_WORDS = ("land", "verge", "vents", "canal", "garden", "passage", "tunnel", "island", "plains", "forest", "mountain", "swamp")

_COMBO_CATEGORY_HINTS = {
    "reducers": "cost reducer discount",
    "rituals": "ritual mana burst",
    "draw_engine": "draw cantrip card advantage",
    "recursion": "return recur recursion graveyard",
    "payoff": "payoff win finisher lethal",
    "tutor": "tutor search wildcard",
    "protection": "protect protection counterspell",
}


def load_legacy_guidance(fmt: str) -> dict[str, dict]:
    path = GUIDANCE_DIR / f"{fmt}.json"
    if not path.exists():
        return {}
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, list):
        return {}
    out: dict[str, dict] = {}
    for item in data:
        if isinstance(item, dict) and item.get("archetype"):
            out[slugify(str(item["archetype"]))] = item
    return out


def load_metagame(fmt: str) -> dict[str, dict]:
    path = METAGAME_DIR / f"{fmt}.json"
    if not path.exists():
        return {}
    data = json.loads(path.read_text(encoding="utf-8"))
    return {
        slugify(str(item.get("name", ""))): item
        for item in data.get("archetypes", [])
        if item.get("name")
    }


def legacy_for(fmt: str, archetype: str) -> dict | None:
    return load_legacy_guidance(fmt).get(slugify(archetype))


def _as_list(value: Any) -> list[str]:
    if value is None:
        return []
    if isinstance(value, list):
        return [str(v).strip() for v in value if str(v).strip()]
    text = str(value).strip()
    if not text:
        return []
    parts = re.split(r"(?<=[.!?])\s+(?=[A-Z0-9])|;\s+", text)
    return [p.strip() for p in parts if p.strip()]


def _append_unique(target: list[str], values: list[str], *, limit: int | None = None) -> None:
    seen = {v.casefold() for v in target}
    for value in values:
        key = value.casefold()
        if key in seen:
            continue
        target.append(value)
        seen.add(key)
        if limit is not None and len(target) >= limit:
            return


def _split_mulligan_text(value: Any) -> tuple[list[str], list[str]]:
    keep: list[str] = []
    ship: list[str] = []
    for sentence in _as_list(value):
        lowered = sentence.lower()
        if "mulligan" in lowered or "no-land" in lowered or "0-1 land" in lowered or "lacking" in lowered:
            ship.append(sentence)
        else:
            keep.append(sentence)
    return keep, ship


def _legacy_provenance(fmt: str) -> dict:
    return {
        "publisher": "legacy_guidance",
        "author": "scripts/generate_guidance.py",
        "source_url": f"guidance/{fmt}.json",
        "publish_date": "",
        "fetched_at": dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
        "http_status": 0,
        "used_for_fields": [
            "overview",
            "mulligan",
            "game_plan",
            "sequencing_tips",
            "matchups",
            "common_threats",
        ],
    }


def _dedupe_provenance(provenance: list[dict]) -> list[dict]:
    out: list[dict] = []
    seen: set[tuple[str, str]] = set()
    for item in provenance:
        if not isinstance(item, dict):
            continue
        key = (str(item.get("publisher", "")), str(item.get("source_url", "")))
        if key in seen:
            continue
        out.append(item)
        seen.add(key)
    return out


def _dedupe_source_label(source: str) -> str:
    parts = [p.strip() for p in (source or "").split("+") if p.strip()]
    out: list[str] = []
    seen: set[str] = set()
    for part in parts:
        key = part.casefold()
        if key in seen:
            continue
        out.append(part)
        seen.add(key)
    return " + ".join(out)


def infer_strategy(name: str, legacy: dict | None, metagame: dict | None) -> str:
    text = " ".join(
        str(x)
        for x in (
            name,
            (legacy or {}).get("philosophy", ""),
            (legacy or {}).get("notes", ""),
            (metagame or {}).get("strategy", ""),
        )
    ).lower()
    for strategy, words in _STRATEGY_KEYWORDS.items():
        if any(word in text for word in words):
            return strategy.value
    return StrategyType.MIDRANGE.value


def guide_from_legacy(fmt: str, legacy: dict, metagame: dict | None = None) -> dict:
    """Create a minimal runtime guide from a legacy guidance entry."""
    name = str(legacy.get("archetype") or (metagame or {}).get("name") or "")
    opening = legacy.get("opening_strategy") if isinstance(legacy.get("opening_strategy"), dict) else {}
    mid = legacy.get("midgame_priorities") if isinstance(legacy.get("midgame_priorities"), dict) else {}
    end = legacy.get("endgame_tactics") if isinstance(legacy.get("endgame_tactics"), dict) else {}
    threats = legacy.get("threat_responses") if isinstance(legacy.get("threat_responses"), dict) else {}
    strategy = infer_strategy(name, legacy, metagame)
    keep, ship = _split_mulligan_text(opening.get("mulligan_criteria"))
    key_cards = [
        {"name": c, "role": "signature card", "notes": "From metagame signature cards."}
        for c in (metagame or {}).get("signature_cards", [])[:12]
    ]
    return {
        "archetype": name,
        "format": fmt,
        "strategy_type": strategy,
        "overview": str(legacy.get("philosophy") or ""),
        "win_conditions": _as_list(end.get("finishers")) or _as_list(legacy.get("philosophy")),
        "mulligan": {
            "keep_criteria": keep[:4],
            "mulligan_criteria": ship[:4],
            "examples": [],
        },
        "game_plan": {
            "early_game": _as_list(opening.get("early_game_plays"))[:6],
            "mid_game": (_as_list(mid.get("resource_management")) + _as_list(mid.get("board_control")))[:8],
            "late_game": (_as_list(end.get("finishers")) + _as_list(end.get("when_to_commit")))[:8],
        },
        "key_cards": key_cards,
        "sequencing_tips": _as_list(legacy.get("notes"))[:8],
        "matchups": [
            {
                "opponent_archetype": label.replace("against_", "").replace("vs_", "").replace("_", " ").title(),
                "advice": str(advice),
                "watch_for": [],
            }
            for label, advice in threats.items()
            if str(advice).strip()
        ],
        "common_threats": _as_list(mid.get("threat_tracking")),
        "metadata": {
            "source": "legacy_guidance_migration",
            "source_url": f"guidance/{fmt}.json",
            "generated_at": dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
            "model": "legacy",
            "schema_version": 2,
        },
        "provenance": [_legacy_provenance(fmt)],
    }


def merge_legacy_into_guide(guide_payload: dict, legacy: dict | None, fmt: str) -> dict:
    if not legacy:
        return guide_payload
    guide = json.loads(json.dumps(guide_payload))
    provenance = guide.setdefault("provenance", [])
    if not any((p or {}).get("publisher") == "legacy_guidance" for p in provenance if isinstance(p, dict)):
        provenance.append(_legacy_provenance(fmt))
    guide["provenance"] = _dedupe_provenance(provenance)

    opening = legacy.get("opening_strategy") if isinstance(legacy.get("opening_strategy"), dict) else {}
    mid = legacy.get("midgame_priorities") if isinstance(legacy.get("midgame_priorities"), dict) else {}
    end = legacy.get("endgame_tactics") if isinstance(legacy.get("endgame_tactics"), dict) else {}
    threats = legacy.get("threat_responses") if isinstance(legacy.get("threat_responses"), dict) else {}

    if not guide.get("overview") and legacy.get("philosophy"):
        guide["overview"] = str(legacy["philosophy"])
    guide.setdefault("mulligan", {})
    keep, ship = _split_mulligan_text(opening.get("mulligan_criteria"))
    _append_unique(guide["mulligan"].setdefault("keep_criteria", []), keep[:4], limit=8)
    _append_unique(guide["mulligan"].setdefault("mulligan_criteria", []), ship[:4], limit=8)
    guide.setdefault("game_plan", {})
    _append_unique(guide["game_plan"].setdefault("early_game", []), _as_list(opening.get("early_game_plays"))[:5], limit=10)
    _append_unique(guide["game_plan"].setdefault("mid_game", []), (_as_list(mid.get("resource_management")) + _as_list(mid.get("board_control")))[:6], limit=12)
    _append_unique(guide["game_plan"].setdefault("late_game", []), (_as_list(end.get("finishers")) + _as_list(end.get("when_to_commit")))[:6], limit=12)
    _append_unique(guide.setdefault("sequencing_tips", []), _as_list(legacy.get("notes"))[:6], limit=14)
    _append_unique(guide.setdefault("common_threats", []), _as_list(mid.get("threat_tracking"))[:5], limit=10)

    matchups = guide.setdefault("matchups", [])
    existing_matchups = {
        str(m.get("opponent_archetype", "")).casefold()
        for m in matchups
        if isinstance(m, dict)
    }
    for label, advice in threats.items():
        if not str(advice).strip():
            continue
        opp = label.replace("against_", "").replace("vs_", "").replace("_", " ").title()
        if opp.casefold() in existing_matchups:
            continue
        matchups.append({"opponent_archetype": opp, "advice": str(advice), "watch_for": []})
        existing_matchups.add(opp.casefold())

    meta = guide.setdefault("metadata", {})
    if isinstance(meta, dict):
        old_source = meta.get("source", "")
        if "legacy_guidance" not in old_source:
            meta["source"] = (old_source + " + legacy_guidance").strip(" +")
        meta["source"] = _dedupe_source_label(meta.get("source", ""))
        meta["schema_version"] = 2
    return PilotingGuide.model_validate(guide).model_dump(exclude={"stale_flags"})


def _card_names_from_guide(guide: dict, metagame: dict | None) -> list[str]:
    names: list[str] = []
    for card in guide.get("key_cards") or []:
        if isinstance(card, dict) and card.get("name"):
            names.append(str(card["name"]))
    combo_profile = guide.get("combo_profile") if isinstance(guide.get("combo_profile"), dict) else {}
    category_cards = combo_profile.get("category_cards") if isinstance(combo_profile.get("category_cards"), dict) else {}
    for cards in category_cards.values():
        names.extend(str(c) for c in (cards or []) if c)
    for line in combo_profile.get("known_lines") or []:
        if isinstance(line, dict):
            names.extend(str(c) for c in (line.get("key_cards") or []) if c)
    names.extend(str(c) for c in (metagame or {}).get("signature_cards", []) if c)
    out: list[str] = []
    seen: set[str] = set()
    for name in names:
        key = name.casefold()
        if key and key not in seen:
            out.append(name)
            seen.add(key)
    return out


def _card_text(card: str, guide: dict, legacy: dict | None) -> str:
    bits = [card]
    for item in guide.get("key_cards") or []:
        if isinstance(item, dict) and str(item.get("name", "")).casefold() == card.casefold():
            bits.extend([str(item.get("role", "")), str(item.get("notes", ""))])
    card_key = card.casefold()
    for value in (guide.get("win_conditions") or []) + (guide.get("sequencing_tips") or []):
        text = str(value)
        if card_key in text.casefold():
            bits.append(text)
    combo_profile = guide.get("combo_profile") if isinstance(guide.get("combo_profile"), dict) else {}
    category_cards = combo_profile.get("category_cards") if isinstance(combo_profile.get("category_cards"), dict) else {}
    for category, cards in category_cards.items():
        if any(str(c).casefold() == card_key for c in (cards or [])):
            bits.append(str(category).replace("_", " "))
            bits.append(_COMBO_CATEGORY_HINTS.get(str(category), ""))
    for line in combo_profile.get("known_lines") or []:
        if isinstance(line, dict) and any(str(c).casefold() == card_key for c in (line.get("key_cards") or [])):
            bits.extend(
                str(line.get(key, ""))
                for key in ("name", "payoff")
            )
    for value in ((legacy or {}).get("philosophy", ""), (legacy or {}).get("notes", "")):
        text = str(value)
        if card_key in text.casefold():
            bits.append(text)
    return " ".join(bits).lower()


def _classify_card(card: str, guide: dict, legacy: dict | None) -> set[str]:
    text = _card_text(card, guide, legacy)
    buckets: set[str] = set()
    for bucket, words in _BUCKET_KEYWORDS.items():
        if words and any(word in text for word in words):
            buckets.add(bucket)
    if any(word in card.lower() for word in _LAND_WORDS):
        buckets.update({"lands", "mana_fixing"})
    if not buckets:
        buckets.add("threats")
    return buckets & set(PROFILE_BUCKETS)


def build_archetype_profile(guide_payload: dict, metagame: dict | None = None, legacy: dict | None = None) -> dict:
    guide = PilotingGuide.model_validate(guide_payload).model_dump(exclude={"stale_flags"})
    cards = _card_names_from_guide(guide, metagame)
    buckets: dict[str, dict] = {
        bucket: {"target_count": 0, "cards": []}
        for bucket in PROFILE_BUCKETS
    }
    card_roles: dict[str, set[str]] = {}
    for card in cards:
        roles = _classify_card(card, guide, legacy)
        card_roles[card] = roles
        for role in roles:
            buckets[role]["cards"].append(card)

    for bucket, payload in buckets.items():
        payload["cards"] = sorted(set(payload["cards"]))
        payload["target_count"] = len(payload["cards"])

    interaction = set(buckets["removal"]["cards"]) | set(buckets["counterspells"]["cards"]) | set(buckets["wrath"]["cards"])
    interaction_density = min(0.8, round(len(interaction) / max(1, len(cards)), 2))
    buckets["interaction_density"] = interaction_density

    dual_role_cards = [
        {"card": card, "roles": sorted(roles), "notes": ""}
        for card, roles in card_roles.items()
        if len(roles) > 1
    ]
    win_turn = [4, 8] if guide["strategy_type"] in ("aggro", "tempo", "combo") else [6, 12]
    return {
        "schema_version": 1,
        "name": guide["archetype"],
        "format": guide.get("format") or (metagame or {}).get("format", ""),
        "colors": (metagame or {}).get("colors", []),
        "strategy_type": guide["strategy_type"],
        "expected_deck_total": 60,
        "macro_plan": guide.get("overview", ""),
        "win_turn_window": win_turn,
        "buckets": buckets,
        "dual_role_cards": dual_role_cards,
        "tells": (metagame or {}).get("tells", []) or guide.get("sequencing_tips", [])[:5],
        "predicted_lines": [
            {"trigger": "early game", "line": line}
            for line in (guide.get("game_plan") or {}).get("early_game", [])[:3]
        ],
        "kill_priority": buckets["engines"]["cards"][:3] + buckets["win_conditions"]["cards"][:3] + buckets["threats"]["cards"][:3],
        "interaction_to_disrupt": buckets["counterspells"]["cards"][:4] + buckets["removal"]["cards"][:4],
        "last_updated": dt.date.today().isoformat(),
    }


def write_profile(profile: dict, fmt: str, slug: str) -> Path:
    path = PROFILE_DIR / fmt / f"{slug}.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(profile, indent=2, sort_keys=False) + "\n", encoding="utf-8")
    return path
