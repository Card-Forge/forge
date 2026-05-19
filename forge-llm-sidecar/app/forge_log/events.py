"""Typed event dataclasses parsed from Forge game log lines.

Each class represents one kind of game event that can be extracted from the
Forge log format.  Events carry a `line` reference (1-based line number in
the source log) so downstream tools can trace back to the original text.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Literal


@dataclass(frozen=True)
class Event:
    """Base class for all parsed events."""

    kind: str
    line: int = 0
    raw: str = ""


# ── Game flow ────────────────────────────────────────────────────────────────


@dataclass(frozen=True)
class MulliganEvent(Event):
    """Mulligan: Player has kept/mulliganed a hand of N cards."""

    kind: Literal["mulligan"] = "mulligan"
    player: str = ""
    kept: bool = False
    hand_size: int = 0


@dataclass(frozen=True)
class TurnBegan(Event):
    """Turn: Turn N (Player)"""

    kind: Literal["turn"] = "turn"
    turn_number: int = 0
    player: str = ""


@dataclass(frozen=True)
class PhaseChanged(Event):
    """Phase: Player's PhaseName Step"""

    kind: Literal["phase"] = "phase"
    player: str = ""
    phase: str = ""


@dataclass(frozen=True)
class PlayerControl(Event):
    """Player Control: X has restored control over Y"""

    kind: Literal["control"] = "control"
    player: str = ""
    target: str = ""


@dataclass(frozen=True)
class GameOutcome(Event):
    """Game Outcome: X has won/lost because ... / Turn N"""

    kind: Literal["outcome"] = "outcome"
    player: str = ""
    won: bool = False
    lost: bool = False
    reason: str = ""
    turn_number: int = 0


# ── Card plays ───────────────────────────────────────────────────────────────


@dataclass(frozen=True)
class SpellCast(Event):
    """Add To Stack: Player cast CardName (targeting [text])"""

    kind: Literal["spell"] = "spell"
    player: str = ""
    card: str = ""
    card_id: int = 0
    target_info: str = ""
    ability_text: str = ""  # from Resolve Stack lines


@dataclass(frozen=True)
class LandPlayed(Event):
    """Land: Player played CardName (ID)"""

    kind: Literal["land"] = "land"
    player: str = ""
    card: str = ""
    card_id: int = 0


# ── Stack / resolution ───────────────────────────────────────────────────────


@dataclass(frozen=True)
class StackResolve(Event):
    """Resolve Stack: CardName (ID) - text / ability description"""

    kind: Literal["resolve"] = "resolve"
    card: str = ""
    card_id: int = 0
    text: str = ""


@dataclass(frozen=True)
class StackAdd(Event):
    """Add To Stack: Player triggered/activated CardName"""

    kind: Literal["stack_add"] = "stack_add"
    player: str = ""
    card: str = ""
    action: str = ""  # "triggered" | "activated"


# ── Zone changes ─────────────────────────────────────────────────────────────


@dataclass(frozen=True)
class ZoneChange(Event):
    """Zone Change: CardName (ID) was put into Zone from Zone."""

    kind: Literal["zone_change"] = "zone_change"
    card: str = ""
    card_id: int = 0
    to_zone: str = ""
    from_zone: str = ""


@dataclass(frozen=True)
class Discard(Event):
    """Discard: Player discards CardName (ID)."""

    kind: Literal["discard"] = "discard"
    player: str = ""
    card: str = ""
    card_id: int = 0


@dataclass(frozen=True)
class DrawCard(Event):
    """Player draws a card. (from ability resolution)"""

    kind: Literal["draw"] = "draw"
    player: str = ""
    count: int = 1


# ── Life and damage ──────────────────────────────────────────────────────────


@dataclass(frozen=True)
class LifeChange(Event):
    """Life: Life: Player OLD > NEW"""

    kind: Literal["life"] = "life"
    player: str = ""
    old_value: int = 0
    new_value: int = 0


@dataclass(frozen=True)
class DamageEvent(Event):
    """Damage: Source (ID) deals N [combat|non-combat] damage to Target."""

    kind: Literal["damage"] = "damage"
    source: str = ""
    source_id: int = 0
    amount: int = 0
    target: str = ""
    is_combat: bool = False


# ── Combat ───────────────────────────────────────────────────────────────────


@dataclass(frozen=True)
class CombatDeclareAttackers(Event):
    """Combat: Player assigned Card1, Card2 ... to attack Target."""

    kind: Literal["combat_attack"] = "combat_attack"
    player: str = ""
    attackers: list[str] = field(default_factory=list)
    target: str = ""


@dataclass(frozen=True)
class CombatBlock(Event):
    """Combat: Player assigned Card (ID) to block Card (ID).
    Combat: Player didn't block Card (ID)."""

    kind: Literal["combat_block"] = "combat_block"
    player: str = ""
    blockers: list[str] = field(default_factory=list)
    unblocked: list[str] = field(default_factory=list)


# ── Abilities ────────────────────────────────────────────────────────────────


@dataclass(frozen=True)
class ActivateAbility(Event):
    """Add To Stack: Player activated CardName"""

    kind: Literal["activate"] = "activate"
    player: str = ""
    card: str = ""
    card_id: int = 0


@dataclass(frozen=True)
class ReplacementEffect(Event):
    """Replacement Effect: text"""

    kind: Literal["replacement"] = "replacement"
    text: str = ""


# ── Mana ─────────────────────────────────────────────────────────────────────


@dataclass(frozen=True)
class ManaEvent(Event):
    """Mana: CardName (ID) - ability text."""

    kind: Literal["mana"] = "mana"
    card: str = ""
    card_id: int = 0
    text: str = ""


# All event types for iteration / registration.
ALL_EVENT_TYPES = (
    MulliganEvent,
    TurnBegan,
    PhaseChanged,
    PlayerControl,
    GameOutcome,
    SpellCast,
    LandPlayed,
    StackResolve,
    StackAdd,
    ZoneChange,
    Discard,
    DrawCard,
    LifeChange,
    DamageEvent,
    CombatDeclareAttackers,
    CombatBlock,
    ActivateAbility,
    ReplacementEffect,
    ManaEvent,
)
