"""Identify the archetypes featured in a tournament VOD from its transcript.

Used when title/description parsing (:mod:`match_parse`) doesn't surface
archetype names — which is the common case for Play MTG feature matches
(``"Semifinal | Larsen vs. Zhang | Standard | #PTSOS"``).

The insight: pro commentators say signature card names *constantly* during
a match. We count how many of each archetype's signature cards appear in
the transcript; archetypes that clear a threshold (distinct cards + total
mentions) are tagged as featured.

Self-contained: no external API, no LLM. Uses signature_cards data we
already maintain in ``app/knowledge/metagame_data/<fmt>.json``.
"""

from __future__ import annotations

import logging
import re
from collections import defaultdict
from dataclasses import dataclass

log = logging.getLogger(__name__)

#: First-word tokens that are too generic to count as a distinctive match.
#: Always fall back to the full card name for these.
_COMMON_FIRST_WORDS = frozenset({
    "force",
    "the",
    "wrath",
    "sword",
    "goblin",
    "elf",
    "march",
    "spell",
    "lightning",
    "fire",
    "blood",
    "elder",
    "deep",
    "dark",
    "swift",
    "ancient",
    "blue",
    "red",
    "white",
    "black",
    "green",
    "mana",
    "field",
    "forest",
    "mountain",
    "plains",
    "swamp",
    "island",
    "primal",
    "primary",
    "mind",
    "war",
})


@dataclass(slots=True, frozen=True)
class ArchetypeScore:
    name: str
    distinct_cards: int  # how many *distinct* signature cards appeared
    total_mentions: int  # raw mention count across all signature cards
    unique_distinct: int  # of those, how many appear ONLY in this archetype's list
    confidence: float  # 0-1, see _confidence()


def identify_archetypes(
    transcript_text: str,
    archetypes: list[dict],
    *,
    min_distinct_cards: int = 4,
    min_total_mentions: int = 6,
    min_confidence: float = 0.45,
    max_results: int = 4,
    require_unique_card: bool = True,
) -> list[ArchetypeScore]:
    """Score each archetype by signature-card mentions in the transcript.

    ``archetypes`` is a list of dicts with at least ``name`` and
    ``signature_cards`` keys (matches the schema in metagame_data).

    Returns scored matches sorted by ``confidence`` desc, dropping those below
    threshold. A feature match should produce exactly two results.

    ``require_unique_card`` (default True) caps confidence at 0.4 for any
    archetype whose only matched signature cards are shared with another
    archetype in the supplied list. This prevents false positives like
    'Azorius Control' getting credit for a Living End vs Bant Nadu match
    just because Force of Negation / Solitude appear in many lists.
    """
    if not transcript_text:
        return []
    haystack = transcript_text.lower()
    rarity = _build_card_rarity(archetypes)
    scored: list[ArchetypeScore] = []
    for arch in archetypes:
        name = arch.get("name") or ""
        sig_cards = arch.get("signature_cards") or []
        if not name or not sig_cards:
            continue
        distinct, total, unique_distinct = _score_archetype(
            haystack, sig_cards, name=name, rarity=rarity
        )
        if distinct < min_distinct_cards and total < min_total_mentions:
            continue
        if distinct < min_distinct_cards or total < min_total_mentions:
            if not (
                distinct >= min_distinct_cards // 2
                and total >= min_total_mentions // 2
            ):
                continue
        confidence = _confidence(
            distinct, total, unique_distinct, len(sig_cards),
            cap_to_shared_only=(require_unique_card and unique_distinct == 0),
        )
        if confidence < min_confidence:
            continue
        scored.append(
            ArchetypeScore(
                name=name,
                distinct_cards=distinct,
                total_mentions=total,
                unique_distinct=unique_distinct,
                confidence=confidence,
            )
        )
    scored.sort(key=lambda s: s.confidence, reverse=True)
    return scored[:max_results]


def _build_card_rarity(archetypes: list[dict]) -> dict[str, set[str]]:
    """Map lowercased signature card -> set of archetype names that list it.

    Used to flag cards as 'shared' (in 2+ archetypes) vs 'unique' (only this
    archetype lists it).
    """
    rarity: dict[str, set[str]] = {}
    for arch in archetypes:
        name = arch.get("name") or ""
        for card in arch.get("signature_cards") or []:
            c = (card or "").strip().lower()
            if not c:
                continue
            rarity.setdefault(c, set()).add(name)
    return rarity


def _score_archetype(
    haystack_lower: str,
    sig_cards: list[str],
    *,
    name: str,
    rarity: dict[str, set[str]],
) -> tuple[int, int, int]:
    """Return (distinct, total, unique_distinct) for one archetype.

    ``unique_distinct`` counts how many of the *matched* signature cards are
    listed ONLY by this archetype (not shared with any other archetype in the
    rarity map).
    """
    distinct = 0
    total = 0
    unique_distinct = 0
    for card in sig_cards:
        c = (card or "").strip().lower()
        if not c:
            continue
        mentions = 0
        first_word = c.split()[0].strip(",.'\"")
        if (
            first_word
            and len(first_word) > 4
            and first_word not in _COMMON_FIRST_WORDS
        ):
            mentions = _count_word(haystack_lower, first_word)
        if not mentions:
            clean = re.sub(r"[^a-z0-9 ]", "", c).strip()
            if clean:
                mentions = _count_phrase(haystack_lower, clean)
        if not mentions:
            continue
        distinct += 1
        total += mentions
        owners = rarity.get(c, set())
        if len(owners) <= 1:
            unique_distinct += 1
    return distinct, total, unique_distinct


def _count_word(haystack: str, word: str) -> int:
    """Whole-word count. ``word`` must already be lowercase."""
    return len(re.findall(r"\b" + re.escape(word) + r"\b", haystack))


def _count_phrase(haystack: str, phrase: str) -> int:
    """Approximate phrase match — allows whitespace flexibility."""
    pattern = r"\b" + r"\s+".join(re.escape(w) for w in phrase.split()) + r"\b"
    return len(re.findall(pattern, haystack))


_VERIFY_SYSTEM = (
    "You are reading an MTG match transcript and reconstructing which decks "
    "the two players were on. Auto-captions mangle card names — cross-"
    "reference against the supplied list of known archetype signature cards. "
    "Output ONLY a JSON object. Be specific: if a player's deck cannot be "
    "identified from the transcript, say 'unknown', do not guess wildly."
)


def _verify_prompt(transcript_text: str, fmt: str, archetypes: list[dict]) -> str:
    sig_block = "\n".join(
        f"- {a['name']}: " + ", ".join((a.get("signature_cards") or [])[:10])
        for a in archetypes[:40]
    )
    return (
        f"Format: {fmt}\n\n"
        f"Known {fmt} archetypes and their signature cards:\n"
        f"{sig_block}\n\n"
        "Transcript (timestamped):\n"
        "---\n"
        f"{transcript_text}\n"
        "---\n\n"
        "Identify the two decks being played. List ALL distinct card names "
        "mentioned for each player (correct caption garbles using the "
        "signature list above). Pick the archetype name that best matches "
        "each player's card pool from the list above, or say 'unknown' if "
        "no listed archetype clearly fits.\n\n"
        "Return JSON EXACTLY in this shape:\n"
        "{\n"
        '  "player_a": {\n'
        '    "name": "player name from commentary",\n'
        '    "cards_mentioned": ["card1", "card2", ...],\n'
        '    "archetype_guess": "name from the list above OR \\"unknown\\"",\n'
        '    "reasoning": "1-2 sentences why",\n'
        '    "confidence": 0.0-1.0\n'
        "  },\n"
        '  "player_b": {... same shape ...}\n'
        "}\n"
    )


def verify_with_llm(
    transcript_text: str,
    archetypes: list[dict],
    *,
    fmt: str,
    initial_scores: list["ArchetypeScore"] | None = None,
) -> dict | None:
    """Ask the builder LLM to identify both players' decks from a transcript.

    Returns the LLM's JSON verdict or None on failure. Caller decides whether
    to override signature-card identifications based on this.

    ``initial_scores`` is informational — included so the prompt has
    additional context if needed.
    """
    from app.knowledge import builder_llm

    try:
        return builder_llm.generate_guide_json(
            _verify_prompt(transcript_text, fmt, archetypes),
            system=_VERIFY_SYSTEM,
        )
    except builder_llm.BuilderLLMError as exc:
        log.warning("verify_with_llm: LLM failed (%s)", exc)
        return None


def reconcile_with_llm(
    sig_scores: list["ArchetypeScore"],
    llm_verdict: dict,
    known_names: set[str],
) -> tuple[list["ArchetypeScore"], list[dict]]:
    """Merge signature-card scores with LLM verdict.

    Returns ``(final_scores, audit_log)``.

    Rule of thumb:
      - LLM names an archetype in our metagame that's NOT in sig_scores at all
        → ADD that archetype with confidence from LLM (or 0.7 default), tag
        ``via=llm_verification``.
      - LLM names a different archetype than the top signature-card score AND
        the LLM-named archetype is in our metagame
        → REPLACE the top sig score with the LLM choice, tag
        ``signature_card_guess_was`` for provenance.
      - LLM says 'unknown' or names something not in metagame → keep
        signature-card scores unchanged.
    """
    audit: list[dict] = []
    if not isinstance(llm_verdict, dict):
        return sig_scores, audit

    final: dict[str, ArchetypeScore] = {s.name: s for s in sig_scores}
    sig_names = set(final.keys())

    llm_archetypes: list[str] = []
    for side_key in ("player_a", "player_b"):
        side = llm_verdict.get(side_key) or {}
        name = (side.get("archetype_guess") or "").strip()
        if not name or name.lower() == "unknown":
            continue
        if name not in known_names:
            audit.append({"action": "llm_named_unknown", "name": name, "side": side_key})
            continue
        llm_archetypes.append(name)

    # If LLM lists names that aren't in sig_scores, add them.
    for name in llm_archetypes:
        if name not in sig_names:
            # Use LLM-side confidence if present, else 0.7
            side_a = llm_verdict.get("player_a") or {}
            side_b = llm_verdict.get("player_b") or {}
            llm_conf = 0.7
            if side_a.get("archetype_guess") == name:
                llm_conf = float(side_a.get("confidence") or 0.7)
            elif side_b.get("archetype_guess") == name:
                llm_conf = float(side_b.get("confidence") or 0.7)
            final[name] = ArchetypeScore(
                name=name,
                distinct_cards=0,
                total_mentions=0,
                unique_distinct=0,
                confidence=llm_conf,
            )
            audit.append({"action": "llm_added", "name": name, "confidence": llm_conf})

    # If the LLM names exactly 2 archetypes and they don't include the
    # top sig score, REPLACE the top sig score with the LLM's choice
    # (preserving evidence the original guess was different).
    if (
        len(llm_archetypes) == 2
        and sig_scores
        and sig_scores[0].name not in llm_archetypes
    ):
        rejected = sig_scores[0].name
        # Remove the rejected archetype
        final.pop(rejected, None)
        audit.append(
            {
                "action": "llm_rejected_sig_top",
                "rejected": rejected,
                "kept": llm_archetypes,
            }
        )

    # Return sorted by confidence
    result = sorted(final.values(), key=lambda s: s.confidence, reverse=True)
    return result, audit


def _confidence(
    distinct: int,
    total: int,
    unique_distinct: int,
    sig_count: int,
    *,
    cap_to_shared_only: bool = False,
) -> float:
    """A 0-1 confidence combining breadth, volume, and unique-card matches.

    When ``cap_to_shared_only`` is True (i.e. zero unique signature cards
    matched), the confidence is capped at 0.4. That keeps archetypes whose
    only matched cards are shared with other archetypes from being mistaken
    for the deck actually featured in the video.
    """
    if sig_count == 0:
        return 0.0
    breadth = min(1.0, distinct / max(1, min(sig_count, 8)))
    volume = min(1.0, total / 20.0)
    uniqueness = min(1.0, unique_distinct / 2.0)  # 2 unique matches = full credit
    score = 0.4 * breadth + 0.3 * volume + 0.3 * uniqueness
    if cap_to_shared_only:
        score = min(score, 0.4)
    return round(score, 3)
