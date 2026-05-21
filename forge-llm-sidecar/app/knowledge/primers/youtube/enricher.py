"""Merge transcript-extracted heuristics into an existing piloting guide.

Public entrypoint: :func:`enrich`. Takes a fully-built ``PilotingGuide`` and
returns a copy with new sequencing tips, matchup advice, mulligan examples,
and provenance entries appended — each marked with
``EvidenceKind.GAMEPLAY_OBSERVED`` and a back-reference to the source video.
"""

from __future__ import annotations

import datetime as dt
import logging
from typing import Optional

from app.knowledge.piloting_schema import (
    EvidenceKind,
    FieldEvidence,
    MatchupNote,
    MulliganHandExample,
    PilotingGuide,
    Provenance,
)
from app.knowledge.primers.youtube import discover, extract, transcripts
from app.knowledge.primers.youtube.discover import VideoCandidate

log = logging.getLogger(__name__)


def enrich(
    guide: PilotingGuide,
    *,
    archetype: str,
    fmt: str,
    signature_cards: list[str],
    known_archetypes: list[str] | None = None,
    since: Optional[dt.date] = None,
    max_videos: int = 3,
) -> PilotingGuide:
    """Return ``guide`` augmented with transcript-extracted heuristics.

    Safe no-op if no candidate videos or no transcripts are available — the
    base guide is returned unchanged.
    """
    candidates = discover.find_videos(
        archetype,
        fmt,
        since=since,
        max_results=max_videos,
        known_archetypes=known_archetypes,
    )
    if not candidates:
        log.info("enrich: no videos found for %s/%s", archetype, fmt)
        return guide

    additions = 0
    for cand in candidates:
        chunks = transcripts.fetch_transcript(cand.video_id)
        if not chunks:
            log.info("enrich: no transcript for %s (%s)", cand.video_id, cand.title)
            continue
        text = transcripts.render_for_prompt(chunks)
        heuristics = extract.extract_heuristics(
            text,
            archetype=archetype,
            fmt=fmt,
            signature_cards=signature_cards,
            candidate=cand,
        )
        if not heuristics:
            continue
        added = _merge(guide, heuristics, cand, archetype)
        if added:
            additions += added
    if additions:
        log.info(
            "enrich: added %d heuristics to %s/%s from %d video(s)",
            additions,
            archetype,
            fmt,
            len(candidates),
        )
    return guide


def _merge(
    guide: PilotingGuide, heuristics: dict, cand: VideoCandidate, archetype: str
) -> int:
    """Append heuristics to the guide. Returns count of added items.

    No-op if this ``(source_url, extracted_for_archetype)`` pair has already
    been merged into the guide (prevents the same video from re-adding
    provenance + duplicating ``(from gameplay)`` markers on every run).
    """
    for existing in guide.provenance:
        if (
            existing.source_url == cand.url
            and existing.extracted_for_archetype == archetype
        ):
            log.info(
                "enrich: skip %s — already merged for %s",
                cand.url, archetype,
            )
            return 0

    prov_index = len(guide.provenance)
    prov = _build_provenance(cand, archetype)
    guide.provenance.append(prov)

    added = 0

    # Sequencing tips
    for h in heuristics.get("sequencing_tips") or []:
        if not isinstance(h, dict):
            continue
        rule = (h.get("rule") or "").strip()
        if not rule or _seq_tip_exists(guide, rule):
            continue
        guide.sequencing_tips.append(rule)
        guide.sequencing_tips_evidence.append(
            FieldEvidence(
                confidence=float(h.get("confidence") or 0.6),
                kind=EvidenceKind.GAMEPLAY_OBSERVED,
                source_index=prov_index,
                evidence_span=str(h.get("timestamp_range") or ""),
            )
        )
        added += 1

    # Matchup advice
    for h in heuristics.get("matchup_advice") or []:
        if not isinstance(h, dict):
            continue
        opp = (h.get("opponent_archetype") or "").strip()
        advice = (h.get("advice") or "").strip()
        if not opp or not advice:
            continue
        # Skip self-matchups.
        if opp.lower() == archetype.lower():
            continue
        existing = next(
            (m for m in guide.matchups if m.opponent_archetype.lower() == opp.lower()), None
        )
        marker = " (from gameplay)"
        if existing is not None:
            if advice not in existing.advice:
                existing.advice = (existing.advice + " " + advice + marker).strip()
                added += 1
        else:
            guide.matchups.append(
                MatchupNote(opponent_archetype=opp, advice=advice + marker, watch_for=[])
            )
            added += 1

    # Mulligan observations -> mulligan.examples
    for h in heuristics.get("mulligan_observations") or []:
        if not isinstance(h, dict):
            continue
        decision = (h.get("decision") or "").strip().lower()
        if decision not in ("keep", "mulligan"):
            continue
        hand_desc = (h.get("hand_description") or "").strip()
        if not hand_desc:
            continue
        reason = (h.get("reason") or "").strip()
        # We don't know the literal card list, so put the description in `reason`.
        guide.mulligan.examples.append(
            MulliganHandExample(
                hand=[],
                decision=decision,
                reason=f"{hand_desc} — {reason}" if reason else hand_desc,
            )
        )
        added += 1

    # Key-card notes — append to the matching key_card's notes if present.
    for h in heuristics.get("key_card_notes") or []:
        if not isinstance(h, dict):
            continue
        name = (h.get("name") or "").strip()
        note = (h.get("note") or "").strip()
        if not name or not note:
            continue
        kc = next((c for c in guide.key_cards if c.name.lower() == name.lower()), None)
        if kc is None:
            continue
        if note not in kc.notes:
            kc.notes = (kc.notes + (" | " if kc.notes else "") + note + " (from gameplay)").strip()
            added += 1

    return added


def _build_provenance(cand: VideoCandidate, archetype: str) -> Provenance:
    return Provenance(
        publisher=f"YouTube/{cand.channel_name}",
        author=cand.channel_name,
        source_url=cand.url,
        publish_date=cand.upload_date,
        fetched_at=dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
        http_status=200,
        used_for_fields=[
            "sequencing_tips",
            "matchups",
            "mulligan.examples",
            "key_cards.notes",
        ],
        extracted_for_archetype=archetype,
    )


def _seq_tip_exists(guide: PilotingGuide, rule: str) -> bool:
    rule_l = rule.lower()
    for existing in guide.sequencing_tips:
        if _similar(existing.lower(), rule_l):
            return True
    return False


def _similar(a: str, b: str) -> bool:
    """Cheap similarity test — first 50 chars match or 80% token overlap."""
    if a == b:
        return True
    if a[:50] == b[:50]:
        return True
    ta = set(a.split())
    tb = set(b.split())
    if not ta or not tb:
        return False
    overlap = len(ta & tb) / max(len(ta), len(tb))
    return overlap >= 0.8
