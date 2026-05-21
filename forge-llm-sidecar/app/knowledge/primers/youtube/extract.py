"""LLM-based heuristic extraction from a gameplay transcript.

Takes a rendered transcript and asks the builder LLM to extract concrete
piloting heuristics: sequencing rules, mulligan observations, matchup-
specific tactics. The prompt explicitly tells the model:

  * Auto-captions garble card names — cross-reference against the supplied
    signature card list and correct obvious mishearings.
  * Skip filler / sponsor reads / intros.
  * Only emit heuristics that are stated or demonstrated in the transcript;
    do not invent.

For tournament VOD videos the prompt also receives the opponent archetype
so matchup advice gets a real opponent name attached.
"""

from __future__ import annotations

import logging
from typing import Optional

from app.knowledge import builder_llm
from app.knowledge.primers.youtube.discover import VideoCandidate

log = logging.getLogger(__name__)


_SYSTEM = (
    "You are extracting piloting heuristics from a Magic: The Gathering "
    "gameplay transcript (auto-generated captions). The speaker is a "
    "competitive player or commentator narrating decisions. Your job is to "
    "find concrete, actionable rules — mulligan decisions, sequencing rules, "
    "matchup tactics — that a future player of this deck would benefit from. "
    "Auto-captions garble card names; cross-reference against the supplied "
    "signature card list and correct obvious mishearings (e.g. 'flog' near "
    "energy context is 'Phlage'). Skip filler, intros, sponsor reads, and "
    "non-MTG chatter. Respond with a single JSON object."
)


def _prompt(
    transcript_text: str,
    archetype: str,
    fmt: str,
    signature_cards: list[str],
    candidate: VideoCandidate,
) -> str:
    sig = ", ".join(signature_cards) or "(none listed)"
    opp_block = ""
    if candidate.opponent_side:
        opp_block = (
            f"This is a FEATURE MATCH against {candidate.opponent_side}. "
            "Tag any matchup-specific advice in the transcript with "
            f"opponent_archetype = \"{candidate.opponent_side}\".\n\n"
        )
    return (
        f"Archetype: {archetype}\n"
        f"Format: {fmt}\n"
        f"Signature cards (for caption-garble correction): {sig}\n"
        f"Video: {candidate.title}\n"
        f"Channel: {candidate.channel_name}\n"
        f"URL: {candidate.url}\n\n"
        f"{opp_block}"
        "Transcript (timestamped):\n"
        "---\n"
        f"{transcript_text}\n"
        "---\n\n"
        "Return a JSON object EXACTLY matching this skeleton. Every heuristic "
        "must include the timestamp range it came from (HH:MM:SS-HH:MM:SS or "
        "MM:SS-MM:SS, exactly as it appears in the transcript). Skip a list "
        "entirely if the transcript has nothing relevant for it.\n\n"
        "{\n"
        '  "sequencing_tips": [\n'
        '    {"rule": "concrete sequencing rule", "timestamp_range": "MM:SS-MM:SS", "confidence": 0.6-0.9}\n'
        "  ],\n"
        '  "mulligan_observations": [\n'
        '    {"hand_description": "what the hand contained", "decision": "keep"|"mulligan", "reason": "...", "timestamp_range": "MM:SS-MM:SS"}\n'
        "  ],\n"
        '  "matchup_advice": [\n'
        '    {"opponent_archetype": "name", "advice": "concrete advice", "timestamp_range": "MM:SS-MM:SS", "confidence": 0.6-0.9}\n'
        "  ],\n"
        '  "key_card_notes": [\n'
        '    {"name": "Card Name", "note": "how this player used or sequenced it", "timestamp_range": "MM:SS-MM:SS"}\n'
        "  ]\n"
        "}\n\n"
        "Be selective — better to return 3 high-quality heuristics than 15 "
        "filler ones. Never invent card names that don't appear in the "
        "signature list. confidence must be between 0.5 and 0.9 (this is "
        "real gameplay evidence, but a single play, not a general claim)."
    )


def extract_heuristics(
    transcript_text: str,
    *,
    archetype: str,
    fmt: str,
    signature_cards: list[str],
    candidate: VideoCandidate,
) -> Optional[dict]:
    """Run the extractor LLM. Returns parsed JSON or None on failure."""
    try:
        return builder_llm.generate_guide_json(
            _prompt(transcript_text, archetype, fmt, signature_cards, candidate),
            system=_SYSTEM,
        )
    except builder_llm.BuilderLLMError as exc:
        log.warning(
            "youtube extract: LLM failed for %s on %s: %s",
            candidate.video_id,
            archetype,
            exc,
        )
        return None


def parse_ts_range(ts_range: str) -> tuple[float, float] | None:
    """Parse 'MM:SS-MM:SS' or 'H:MM:SS-H:MM:SS' into a (start, end) tuple."""
    if not isinstance(ts_range, str) or "-" not in ts_range:
        return None
    parts = ts_range.replace(" ", "").split("-", 1)
    if len(parts) != 2:
        return None
    try:
        return _parse_ts(parts[0]), _parse_ts(parts[1])
    except ValueError:
        return None


def _parse_ts(s: str) -> float:
    bits = s.split(":")
    if len(bits) == 2:
        m, sec = bits
        return int(m) * 60 + float(sec)
    if len(bits) == 3:
        h, m, sec = bits
        return int(h) * 3600 + int(m) * 60 + float(sec)
    raise ValueError(s)
