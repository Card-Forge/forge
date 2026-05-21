"""Multi-source primer orchestrator.

For one ``(archetype, fmt)`` request we:

1. Walk a per-format provider chain.
2. For each provider, take the first few candidates and try to fetch them.
3. Pass the cleaned text through :func:`builder_llm.extract_primer_fields`.
4. Score the result — if it has an overview AND at least one of
   ``key_cards``/``matchups``/``game_plan.*``, accept it.
5. Otherwise fall through to the next provider.
6. If everything fails, call :func:`default_primer.synthesize`.

The orchestrator returns a validated :class:`PilotingGuide`. Provenance
entries record every source we consulted (including ones that failed).
"""

from __future__ import annotations

import datetime as dt
import hashlib
import logging
from typing import Optional

from app.knowledge import builder_llm
from app.knowledge.piloting_schema import (
    PILOTING_SCHEMA_VERSION,
    PilotingGuide,
    StrategyType,
)
from app.knowledge.primers import default_primer
from app.knowledge.primers.sources.base import Provider
from app.knowledge.primers.sources.cards_realm import CardsRealmProvider
from app.knowledge.primers.sources.draftsim import DraftsimProvider
from app.knowledge.primers.sources.hareruya import HareruyaProvider
from app.knowledge.primers.sources.moxfield import MoxfieldProvider
from app.knowledge.primers.sources.mtg_arena_zone import MTGArenaZoneProvider

log = logging.getLogger(__name__)

# Per-format provider order. The orchestrator instantiates these lazily so a
# broken provider does not stop the others.
PROVIDER_CHAIN: dict[str, list[type[Provider]]] = {
    # Hareruya/Moxfield/MTGAZ discovery is currently unreliable (Hareruya's
    # English path doesn't expose a usable search URL, Moxfield is
    # Cloudflare-blocked, MTGAZ post-sitemap is partial). Cards Realm covers
    # most of what we need and Draftsim fills the rest; everything else
    # falls through to default_primer synthesis.
    "modern": [CardsRealmProvider, DraftsimProvider],
    "pioneer": [CardsRealmProvider, DraftsimProvider],
    "standard": [CardsRealmProvider, DraftsimProvider],
    "historic": [CardsRealmProvider],
    "legacy": [CardsRealmProvider, DraftsimProvider],
    "pauper": [CardsRealmProvider, DraftsimProvider],
    "vintage": [CardsRealmProvider],
    "premodern": [CardsRealmProvider],
}

#: max candidates to actually fetch per provider (search may return more)
_MAX_FETCH_PER_PROVIDER = 3


def _is_usable_extract(raw: dict) -> bool:
    if not isinstance(raw, dict):
        return False
    if not (raw.get("overview") or "").strip():
        return False
    if raw.get("key_cards") or raw.get("matchups"):
        return True
    gp = raw.get("game_plan") or {}
    if any(gp.get(k) for k in ("early_game", "mid_game", "late_game")):
        return True
    return False


def _hash_signature(signature_cards: list[str]) -> Optional[str]:
    if not signature_cards:
        return None
    canon = "|".join(sorted(c.strip().lower() for c in signature_cards if c))
    return hashlib.sha256(canon.encode("utf-8")).hexdigest()[:16]


def _provenance_from_primer(raw_primer, used_for: list[str]) -> dict:
    return {
        "publisher": raw_primer.publisher,
        "author": raw_primer.candidate.author,
        "source_url": raw_primer.candidate.url,
        "publish_date": raw_primer.candidate.publish_date,
        "fetched_at": raw_primer.fetched_at,
        "http_status": raw_primer.http_status,
        "used_for_fields": used_for,
    }


def build_primer(
    archetype: str,
    fmt: str,
    *,
    signature_cards: Optional[list[str]] = None,
    colors: Optional[list[str]] = None,
    chain_override: Optional[list[type[Provider]]] = None,
) -> Optional[PilotingGuide]:
    """Build a piloting guide for ``archetype`` in ``fmt``.

    Returns the validated guide on success, ``None`` if even the
    default-primer synthesis fails (e.g. builder LLM unreachable).
    """
    fmt_l = (fmt or "").lower()
    signature_cards = signature_cards or []
    colors = colors or []
    provenance: list[dict] = []

    chain = chain_override or PROVIDER_CHAIN.get(fmt_l, [CardsRealmProvider, DraftsimProvider])

    for provider_cls in chain:
        provider = provider_cls()
        try:
            candidates = provider.search(archetype, fmt_l)[:_MAX_FETCH_PER_PROVIDER]
        except Exception as exc:  # noqa: BLE001 - provider failures must not be fatal
            log.warning("%s.search failed: %s", provider.publisher, exc)
            continue
        if not candidates:
            log.info("%s: no candidates for %s/%s", provider.publisher, archetype, fmt_l)
            continue
        for candidate in candidates:
            raw_primer = provider.fetch(candidate)
            if raw_primer is None or not raw_primer.is_usable():
                log.info(
                    "%s: %s not usable", provider.publisher, candidate.url
                )
                continue
            try:
                extracted = builder_llm.extract_primer_fields(
                    raw_primer.cleaned_text,
                    archetype=archetype,
                    fmt=fmt_l,
                    publisher=raw_primer.publisher,
                    source_url=candidate.url,
                )
            except builder_llm.BuilderLLMError as exc:
                log.warning("%s: extraction failed for %s: %s", provider.publisher, candidate.url, exc)
                provenance.append(_provenance_from_primer(raw_primer, ["(failed extraction)"]))
                continue
            if not _is_usable_extract(extracted):
                log.info("%s: extract too sparse for %s", provider.publisher, candidate.url)
                provenance.append(_provenance_from_primer(raw_primer, ["(sparse)"]))
                continue
            provenance.append(_provenance_from_primer(raw_primer, ["overview", "key_cards", "matchups"]))
            log.info(
                "primer: built %s (%s) from %s",
                archetype,
                fmt_l,
                raw_primer.publisher,
            )
            return _finalize_guide(
                extracted,
                archetype=archetype,
                fmt=fmt_l,
                provenance=provenance,
                signature_cards=signature_cards,
                primary_source_url=candidate.url,
            )

    # Default-primer fallback.
    log.warning(
        "primer: no editorial source resolved for %s (%s) — synthesizing default",
        archetype,
        fmt_l,
    )
    try:
        raw, default_prov = default_primer.synthesize(archetype, fmt_l, signature_cards, colors)
    except builder_llm.BuilderLLMError as exc:
        log.error("default_primer synthesis failed for %s: %s", archetype, exc)
        return None
    provenance.append(default_prov)
    return _finalize_guide(
        raw,
        archetype=archetype,
        fmt=fmt_l,
        provenance=provenance,
        signature_cards=signature_cards,
        primary_source_url="",
    )


def _finalize_guide(
    raw: dict,
    *,
    archetype: str,
    fmt: str,
    provenance: list[dict],
    signature_cards: list[str],
    primary_source_url: str,
) -> Optional[PilotingGuide]:
    raw.setdefault("archetype", archetype)
    raw.setdefault("format", fmt)
    raw.setdefault("strategy_type", _guess_strategy_type(raw))
    raw["provenance"] = provenance
    raw["decklist_hash"] = _hash_signature(signature_cards)
    raw["metadata"] = {
        "source": " + ".join(p["publisher"] for p in provenance if p.get("publisher")),
        "source_url": primary_source_url,
        "generated_at": dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
        "model": builder_llm.MODEL_NAME,
        "schema_version": PILOTING_SCHEMA_VERSION,
    }
    _coerce_to_schema(raw)
    try:
        return PilotingGuide.model_validate(raw)
    except Exception as exc:  # noqa: BLE001
        log.error("primer: schema validation failed for %s: %s", archetype, exc)
        return None


def _coerce_to_schema(raw: dict) -> None:
    """Normalize common LLM shape mistakes in place.

    Local models freelance the JSON shape: strings where lists belong,
    capitalized enums, plain card names where ``KeyCard`` objects are
    required, etc. We coerce the easy cases here so the orchestrator can
    accept a wider range of outputs.
    """
    # strategy_type: lowercase + map common synonyms
    st = raw.get("strategy_type")
    if isinstance(st, str):
        s = st.strip().lower()
        synonyms = {
            "aggressive": "aggro",
            "aggro-control": "tempo",
            "midrange-control": "midrange",
            "value": "midrange",
            "tempo-aggro": "tempo",
        }
        raw["strategy_type"] = synonyms.get(s, s)

    # String -> list[str] for simple list fields
    for key in ("win_conditions", "sequencing_tips", "common_threats"):
        v = raw.get(key)
        if isinstance(v, str):
            raw[key] = [seg.strip() for seg in v.split(";") if seg.strip()] if v else []

    # key_cards: plain strings -> {name, role, notes}
    kcs = raw.get("key_cards")
    if isinstance(kcs, list):
        normalized: list = []
        for entry in kcs:
            if isinstance(entry, str):
                normalized.append({"name": entry, "role": "", "notes": ""})
            elif isinstance(entry, dict):
                # Some models return {"name": "...", "role": "...", "description": "..."}
                if "notes" not in entry and "description" in entry:
                    entry["notes"] = entry.pop("description")
                if "name" in entry:
                    normalized.append(entry)
        raw["key_cards"] = normalized

    # matchups: dict-of-strings or list of strings -> list[MatchupNote]
    mus = raw.get("matchups")
    if isinstance(mus, dict):
        raw["matchups"] = [
            {"opponent_archetype": k, "advice": v if isinstance(v, str) else "", "watch_for": []}
            for k, v in mus.items()
        ]
    elif isinstance(mus, list):
        normalized_m: list = []
        for entry in mus:
            if isinstance(entry, str):
                normalized_m.append({"opponent_archetype": entry, "advice": "", "watch_for": []})
            elif isinstance(entry, dict):
                if "opponent_archetype" not in entry:
                    for alt in ("opponent", "name", "archetype", "matchup"):
                        if alt in entry:
                            entry["opponent_archetype"] = entry.pop(alt)
                            break
                if "opponent_archetype" in entry:
                    normalized_m.append(entry)
        raw["matchups"] = normalized_m

    # mulligan: string -> {keep_criteria, mulligan_criteria, examples}
    mull = raw.get("mulligan")
    if isinstance(mull, str):
        raw["mulligan"] = {
            "keep_criteria": [mull.strip()] if mull.strip() else [],
            "mulligan_criteria": [],
            "examples": [],
        }
    elif isinstance(mull, dict):
        for k in ("keep_criteria", "mulligan_criteria"):
            v = mull.get(k)
            if isinstance(v, str):
                mull[k] = [seg.strip() for seg in v.split(";") if seg.strip()] if v else []
            elif v is None:
                mull[k] = []
        # examples: drop unparseable strings; coerce inner shapes
        ex = mull.get("examples")
        if isinstance(ex, list):
            fixed: list = []
            for e in ex:
                if not isinstance(e, dict):
                    continue
                # hand may be a string "Card1, Card2, Card3"
                hand = e.get("hand")
                if isinstance(hand, str):
                    e["hand"] = [c.strip() for c in hand.split(",") if c.strip()]
                elif hand is None:
                    e["hand"] = []
                elif not isinstance(hand, list):
                    e["hand"] = []
                # decision: lowercase + map common aliases
                d = e.get("decision")
                if isinstance(d, str):
                    d_low = d.strip().lower()
                    if d_low in ("keep", "kept", "k"):
                        e["decision"] = "keep"
                    elif d_low in ("mulligan", "mull", "mulled", "shipped"):
                        e["decision"] = "mulligan"
                    else:
                        e["decision"] = "keep"  # default if ambiguous
                else:
                    e["decision"] = "keep"
                e.setdefault("reason", "")
                fixed.append(e)
            mull["examples"] = fixed
        elif ex is None:
            mull["examples"] = []
        else:
            mull["examples"] = []

    # game_plan: string -> early/mid/late
    gp = raw.get("game_plan")
    if isinstance(gp, str):
        raw["game_plan"] = {
            "early_game": [gp.strip()] if gp.strip() else [],
            "mid_game": [],
            "late_game": [],
        }
    elif isinstance(gp, dict):
        for k in ("early_game", "mid_game", "late_game"):
            v = gp.get(k)
            if isinstance(v, str):
                gp[k] = [seg.strip() for seg in v.split(";") if seg.strip()] if v else []
            elif v is None:
                gp[k] = []

    # evidence list fields: drop if not a list
    for key in (
        "win_conditions_evidence",
        "sequencing_tips_evidence",
        "common_threats_evidence",
    ):
        if key in raw and not isinstance(raw[key], list):
            raw[key] = []

    # overview_evidence: drop if not dict
    if "overview_evidence" in raw and not isinstance(raw["overview_evidence"], (dict, type(None))):
        raw["overview_evidence"] = None


def _guess_strategy_type(raw: dict) -> str:
    overview = (raw.get("overview") or "").lower()
    for keyword, strat in (
        ("aggro", StrategyType.AGGRO.value),
        ("tempo", StrategyType.TEMPO.value),
        ("combo", StrategyType.COMBO.value),
        ("ramp", StrategyType.RAMP.value),
        ("control", StrategyType.CONTROL.value),
    ):
        if keyword in overview:
            return strat
    return StrategyType.MIDRANGE.value
