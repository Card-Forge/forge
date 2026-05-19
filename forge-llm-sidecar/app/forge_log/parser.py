"""Regex-based line parser for Forge game logs.

Compiles a list of (pattern, handler) pairs at load time.  Each handler
receives a `re.Match` object and a line number, and returns an `Event` or
`None`.  The first matching pattern wins.
"""

from __future__ import annotations

import re
from typing import Any

from app.forge_log.events import (
    CombatBlock,
    CombatDeclareAttackers,
    DamageEvent,
    Discard,
    DrawCard,
    Event,
    GameOutcome,
    LandPlayed,
    LifeChange,
    ManaEvent,
    MulliganEvent,
    PhaseChanged,
    PlayerControl,
    ReplacementEffect,
    SpellCast,
    StackAdd,
    StackResolve,
    TurnBegan,
    ZoneChange,
)

# ── Helpers ──────────────────────────────────────────────────────────────────

# Card name with optional Forge ID: "Card Name (123)"
_CARD = r"(.+?)\s*\((\d+)\)"
# Card name without ID
_CARD_NO_ID = r"(.+?)"
# Player name (no parens, no digits at end)
_PLAYER = r"(.+?)"
# Integer
_INT = r"(\d+)"


def _strip_id(text: str) -> tuple[str, int]:
    """Extract 'Card Name' and id from 'Card Name (123)' or similar."""
    m = re.match(r"(.+?)\s*\((\d+)\)\s*$", text.strip())
    if m:
        return m.group(1).strip(), int(m.group(2))
    return text.strip(), 0


# ── Pattern handlers ─────────────────────────────────────────────────────────


class _Parser:
    """Ordered list of (compiled_re, build_event_fn). First match wins."""

    def __init__(self):
        self._patterns: list[tuple[re.Pattern, Any]] = []

    def add(self, pattern: str, handler):
        self._patterns.append((re.compile(pattern, re.DOTALL), handler))
        return self

    def parse(self, line: str, line_number: int = 0):
        line = line.strip()
        if not line:
            return None
        for pat, handler in self._patterns:
            m = pat.match(line)
            if m:
                return handler(m, line_number, line)
        return None


def _build_parser() -> _Parser:
    p = _Parser()

    # Match Result: Player1: N Player2: M
    p.add(
        r"Match Result:\s+.+",
        lambda m, ln, raw: GameOutcome(kind="outcome", line=ln, raw=raw),
    )

    # Game Outcome: Player has won/lost because reason
    p.add(
        r"Game Outcome: " + _PLAYER + r" has (won|lost) because (.+)",
        lambda m, ln, raw: GameOutcome(
            kind="outcome",
            line=ln,
            raw=raw,
            player=m.group(1),
            won=m.group(2) == "won",
            lost=m.group(2) == "lost",
            reason=m.group(3).strip(),
        ),
    )

    # Game Outcome: Turn N
    p.add(
        r"Game Outcome: Turn " + _INT,
        lambda m, ln, raw: GameOutcome(
            kind="outcome",
            line=ln,
            raw=raw,
            turn_number=int(m.group(1)),
        ),
    )

    # Player Control: X has restored control over themself
    p.add(
        r"Player Control: " + _PLAYER + r" has restored control over (.+)",
        lambda m, ln, raw: PlayerControl(
            kind="control",
            line=ln,
            raw=raw,
            player=m.group(1),
            target=m.group(2).strip(),
        ),
    )

    # Life: Life: Player OLD > NEW
    p.add(
        r"Life: Life: " + _PLAYER + r"\s+(-?\d+)\s*>\s*(-?\d+)",
        lambda m, ln, raw: LifeChange(
            kind="life",
            line=ln,
            raw=raw,
            player=m.group(1),
            old_value=int(m.group(2)),
            new_value=int(m.group(3)),
        ),
    )

    # Damage: Source (ID) deals N [combat|non-combat|] damage to Target.
    p.add(
        r"Damage: " + _CARD + r"\s+deals\s+" + _INT + r"(?:\s+(.+?))?\s+damage to\s+(.+?)\.?\s*$",
        lambda m, ln, raw: DamageEvent(
            kind="damage",
            line=ln,
            raw=raw,
            source=m.group(1),
            source_id=int(m.group(2)),
            amount=int(m.group(3)),
            target=m.group(5).rstrip(".").strip(),
            is_combat=m.group(4) is None or m.group(4).strip() == "combat",
        ),
    )

    # Player didn't block Card (ID). (no Combat: prefix, must be before Phase)
    p.add(
        r"^(?!Combat:)" + _PLAYER + r"\s+didn't block\s+" + _CARD + r"\.?\s*$",
        lambda m, ln, raw: CombatBlock(
            kind="combat_block",
            line=ln,
            raw=raw,
            player=m.group(1),
            unblocked=[f"{m.group(2)} ({m.group(3)})"],
        ),
    )

    # Phase: Player's PhaseName Step
    p.add(
        r"Phase: " + _PLAYER + r"'s\s+(.+)",
        lambda m, ln, raw: PhaseChanged(
            kind="phase",
            line=ln,
            raw=raw,
            player=m.group(1),
            phase=m.group(2).strip(),
        ),
    )

    # Phase: generic (no player possessive, e.g. "Phase: Cleanup step")
    p.add(
        r"Phase:\s+(.+)",
        lambda m, ln, raw: PhaseChanged(
            kind="phase",
            line=ln,
            raw=raw,
            phase=m.group(1).strip(),
        ),
    )

    # Turn: Turn N (Player)
    p.add(
        r"Turn: Turn " + _INT + r"\s*\(" + _PLAYER + r"\)",
        lambda m, ln, raw: TurnBegan(
            kind="turn",
            line=ln,
            raw=raw,
            turn_number=int(m.group(1)),
            player=m.group(2),
        ),
    )

    # Mulligan: Player has kept a hand of N cards
    p.add(
        r"Mulligan: " + _PLAYER + r"\s+has kept a hand of " + _INT + r"\s+cards",
        lambda m, ln, raw: MulliganEvent(
            kind="mulligan",
            line=ln,
            raw=raw,
            player=m.group(1),
            kept=True,
            hand_size=int(m.group(2)),
        ),
    )

    # Mulligan: Player has mulliganed down to N cards.
    p.add(
        r"Mulligan: " + _PLAYER + r"\s+has mulliganed down to " + _INT + r"\s+cards\.?",
        lambda m, ln, raw: MulliganEvent(
            kind="mulligan",
            line=ln,
            raw=raw,
            player=m.group(1),
            kept=False,
            hand_size=int(m.group(2)),
        ),
    )

    # Add To Stack: Player cast CardName (ID) targeting [text]
    p.add(
        r"Add To Stack: " + _PLAYER + r"\s+cast\s+" + _CARD + r"\s+targeting\s+\[([^\]]*)\]",
        lambda m, ln, raw: SpellCast(
            kind="spell",
            line=ln,
            raw=raw,
            player=m.group(1),
            card=m.group(2),
            card_id=int(m.group(3)),
            target_info=m.group(4).strip(),
        ),
    )

    # Add To Stack: Player cast CardName (ID)
    p.add(
        r"Add To Stack: " + _PLAYER + r"\s+cast\s+" + _CARD,
        lambda m, ln, raw: SpellCast(
            kind="spell",
            line=ln,
            raw=raw,
            player=m.group(1),
            card=m.group(2),
            card_id=int(m.group(3)),
        ),
    )

    # Add To Stack: Player cast CardName targeting [text] (no ID)
    p.add(
        r"Add To Stack: " + _PLAYER + r"\s+cast\s+(.+?)\s+targeting\s+\[([^\]]*)\]",
        lambda m, ln, raw: SpellCast(
            kind="spell",
            line=ln,
            raw=raw,
            player=m.group(1),
            card=m.group(2).strip(),
            target_info=m.group(3).strip(),
        ),
    )

    # Add To Stack: Player cast CardName (no ID)
    p.add(
        r"Add To Stack: " + _PLAYER + r"\s+cast\s+(.+?)\s*$",
        lambda m, ln, raw: SpellCast(
            kind="spell",
            line=ln,
            raw=raw,
            player=m.group(1),
            card=m.group(2).strip(),
        ),
    )

    # Add To Stack: Player triggered CardName
    p.add(
        r"Add To Stack: " + _PLAYER + r"\s+triggered\s+" + _CARD_NO_ID,
        lambda m, ln, raw: StackAdd(
            kind="stack_add",
            line=ln,
            raw=raw,
            player=m.group(1),
            card=m.group(2).strip(),
            action="triggered",
        ),
    )

    # Add To Stack: Player activated CardName
    p.add(
        r"Add To Stack: " + _PLAYER + r"\s+activated\s+" + _CARD_NO_ID,
        lambda m, ln, raw: StackAdd(
            kind="stack_add",
            line=ln,
            raw=raw,
            player=m.group(1),
            card=m.group(2).strip(),
            action="activated",
        ),
    )

    # Land: Player played CardName (ID)
    p.add(
        r"Land: " + _PLAYER + r"\s+played\s+" + _CARD,
        lambda m, ln, raw: LandPlayed(
            kind="land",
            line=ln,
            raw=raw,
            player=m.group(1),
            card=m.group(2),
            card_id=int(m.group(3)),
        ),
    )

    # Zone Change: CardName (ID) was put into Zone from Zone.
    p.add(
        r"Zone Change: " + _CARD + r"\s+was put into\s+(.+?)\s+from\s+(.+?)\.?\s*$",
        lambda m, ln, raw: ZoneChange(
            kind="zone_change",
            line=ln,
            raw=raw,
            card=m.group(1),
            card_id=int(m.group(2)),
            to_zone=m.group(3).strip(),
            from_zone=m.group(4).rstrip(".").strip(),
        ),
    )

    # Zone Change: Send countered spell to Graveyard
    p.add(
        r"Zone Change:\s+(.+)",
        lambda m, ln, raw: ZoneChange(
            kind="zone_change",
            line=ln,
            raw=raw,
            to_zone=m.group(1).strip(),
        ),
    )

    # Discard: Player discards CardName (ID).
    p.add(
        r"Discard: " + _PLAYER + r"\s+discards?\s+" + _CARD,
        lambda m, ln, raw: Discard(
            kind="discard",
            line=ln,
            raw=raw,
            player=m.group(1),
            card=m.group(2),
            card_id=int(m.group(3)),
        ),
    )

    # Player surveiled N card(s) to the graveyard
    p.add(
        (
            r"Resolve Stack: "
            + _PLAYER
            + r"\s+surveiled\s+"
            + _INT
            + r"\s+card\(s?\)\s+to the graveyard"
        ),
        lambda m, ln, raw: StackResolve(
            kind="resolve",
            line=ln,
            raw=raw,
            card=m.group(1),
            text=f"{m.group(1)} surveiled {m.group(2)} card(s) to the graveyard",
        ),
    )

    # Resolve Stack: CardName (ID) - Player draws a card. (MUST be before generic resolve)
    # Only match when the entire text after dash is "Player draws a card."
    p.add(
        r"Resolve Stack: " + _CARD + r"\s*-\s*([\w\s']+?)\s+draws? a? card\.?\s*$",
        lambda m, ln, raw: DrawCard(
            kind="draw",
            line=ln,
            raw=raw,
            player=m.group(3),
        ),
    )

    # Resolve Stack: CardName - text
    p.add(
        r"Resolve Stack: " + _CARD + r"\s*-\s*(.+)",
        lambda m, ln, raw: StackResolve(
            kind="resolve",
            line=ln,
            raw=raw,
            card=m.group(1),
            card_id=int(m.group(2)),
            text=m.group(3).strip(),
        ),
    )

    # Resolve Stack: CardName (ID)
    p.add(
        r"Resolve Stack: " + _CARD,
        lambda m, ln, raw: StackResolve(
            kind="resolve",
            line=ln,
            raw=raw,
            card=m.group(1),
            card_id=int(m.group(2)),
        ),
    )

    # Resolve Stack: text (no card id)
    p.add(
        r"Resolve Stack: " + _CARD_NO_ID,
        lambda m, ln, raw: StackResolve(
            kind="resolve",
            line=ln,
            raw=raw,
            card=m.group(1).strip(),
            text=m.group(1).strip(),
        ),
    )

    # Replacement Effect: text
    p.add(
        r"Replacement Effect: " + _CARD_NO_ID,
        lambda m, ln, raw: ReplacementEffect(
            kind="replacement",
            line=ln,
            raw=raw,
            text=m.group(1).strip(),
        ),
    )

    # Mana: CardName (ID) - text.
    p.add(
        r"Mana: " + _CARD + r"\s*-\s*(.+?)\.?\s*$",
        lambda m, ln, raw: ManaEvent(
            kind="mana",
            line=ln,
            raw=raw,
            card=m.group(1),
            card_id=int(m.group(2)),
            text=m.group(3).strip().rstrip("."),
        ),
    )

    # Mana: CardName (ID) - text (no trailing period)
    p.add(
        r"Mana: " + _CARD,
        lambda m, ln, raw: ManaEvent(
            kind="mana",
            line=ln,
            raw=raw,
            card=m.group(1),
            card_id=int(m.group(2)),
        ),
    )

    # Combat: Player assigned Card1 (ID), Card2 (ID) ... to attack Target.
    p.add(
        r"Combat: " + _PLAYER + r"\s+assigned\s+(.+?)\s+to attack\s+(.+?)\.?\s*$",
        lambda m, ln, raw: CombatDeclareAttackers(
            kind="combat_attack",
            line=ln,
            raw=raw,
            player=m.group(1),
            attackers=_parse_card_list(m.group(2)),
            target=m.group(3).rstrip(".").strip(),
        ),
    )

    # Combat: Player assigned Card (ID) to block Card (ID).
    p.add(
        r"Combat: " + _PLAYER + r"\s+assigned\s+" + _CARD + r"\s+to block\s+" + _CARD + r"\.?",
        lambda m, ln, raw: CombatBlock(
            kind="combat_block",
            line=ln,
            raw=raw,
            player=m.group(1),
            blockers=[f"{m.group(2)} ({m.group(3)})"],
        ),
    )

    # Combat: Player didn't block Card (ID).
    p.add(
        r"Combat: " + _PLAYER + r"\s+didn't block\s+" + _CARD + r"\.?",
        lambda m, ln, raw: CombatBlock(
            kind="combat_block",
            line=ln,
            raw=raw,
            player=m.group(1),
            unblocked=[f"{m.group(2)} ({m.group(3)})"],
        ),
    )

    # Player picked {X}  (from mana ability choice)
    p.add(
        r"Land: " + _PLAYER + r"\s+picked\s+(.+)",
        lambda m, ln, raw: ManaEvent(
            kind="mana",
            line=ln,
            raw=raw,
            text=f"{m.group(1)} picked {m.group(2)}",
        ),
    )

    return p


def _parse_card_list(text: str) -> list[str]:
    """Parse 'Card1 (ID), Card2 (ID) and Card3 (ID)' into list of names.

    Card names can contain commas (e.g. 'Ral, Monsoon Mage'), so we split
    on ', ' or ' and ' only when followed by '(digits)' pattern.
    """
    cards = []
    # Find all card references: "Name (digits)"
    for m in re.finditer(r"(?:^|, | and )(.+?)\s*\((\d+)\)", text):
        name = m.group(1).strip()
        if name:
            cards.append(name)
    # Also handle cards without IDs (fallback)
    if not cards:
        parts = re.split(r",\s+|\s+and\s+", text)
        for part in parts:
            name, _ = _strip_id(part.strip())
            if name:
                cards.append(name)
    return cards


# Singleton parser
_parser = _build_parser()


def parse_line(line: str, line_number: int = 0) -> Event | None:
    """Parse a single Forge game log line into a typed event, or None."""
    return _parser.parse(line, line_number)


def parse_lines(lines: list[str]) -> list[Event]:
    """Parse multiple lines, returning only successfully parsed events."""
    events = []
    for i, line in enumerate(lines, start=1):
        ev = parse_line(line, i)
        if ev is not None:
            events.append(ev)
    return events
