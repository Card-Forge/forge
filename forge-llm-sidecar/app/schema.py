"""Request/response models and the LangGraph state schema."""

from __future__ import annotations

from typing import TypedDict

from pydantic import BaseModel, Field

# v2 adds the piloting advice block to the response (see PilotingAdvice).
# v3 adds structured ActionScore with personality weighting.
# v4 adds board-aware advice: RoleAssessment (Who's the Beatdown), hand
# valuations, opponent-hand inference, target priorities, and richer state on
# the request (phase, available_mana).
# v5 adds real hand/library sizes, per-card P/T on the battlefield, opp mana
# color tracking, dimension scores (Board/Cards/Clock/Tempo/Graveyard),
# threat-aware hand valuation, and a graveyard-utility metric.
# v6 adds the opponent_strategist node: archetype-profile-driven hand inference
# (role buckets), a predicted opponent next-turn line, archetype-aware threat
# priorities, and opponent resource signals + decision_type on the request.
# v7 adds combo_plan: deterministic combo readiness plus a strategist-selected
# combo line that can adjust the existing action scores.
# v8 adds early_game_plan, legal_actions, cards_to_return, and a global
# sidecar influence weight.
# v9 adds card-by-card opponent hand probabilities and AI draw probabilities
# computed from decklists minus cards known to have left the library.
SCHEMA_VERSION = 9


class Observation(BaseModel):
    """A single observed opponent play."""

    turn: int
    event: str  # "spell" | "land" | "permanent" | "graveyard"
    card: str
    cmc: int = 0
    colors: list[str] = Field(default_factory=list)
    types: list[str] = Field(default_factory=list)


class BoardCard(BaseModel):
    """Detailed view of one permanent on a battlefield zone.

    Power/toughness are sent only for creatures (and are still optional on
    older clients). Tapped state is public information.
    """

    name: str
    power: int | None = None
    toughness: int | None = None
    types: list[str] = Field(default_factory=list)
    is_creature: bool = False
    tapped: bool = False


class ActionScore(BaseModel):
    """A scored action recommendation from the LLM piloting advice."""

    action_type: str = (
        ""  # PLAY_SPELL | PLAY_LAND | ATTACK | BLOCK | ACTIVATE_ABILITY | MULLIGAN | PASS
    )
    target: str | None = None  # primary target (card name, "all_attackers", etc.)
    targets: list[str] | None = None  # secondary / multiple targets
    percentage: float = 0.0  # 0.0–100.0 — how good this action is
    reasoning: str = ""


class LegalAction(BaseModel):
    """One legal action candidate Forge says the AI can choose right now."""

    action_type: str = ""
    card: str = ""
    target: str = ""
    colors: list[str] = Field(default_factory=list)
    enters_tapped: bool = False
    produces: list[str] = Field(default_factory=list)


class RecognitionRequest(BaseModel):
    # Identifies the calling client/adapter (e.g. "forge"). Any MTG client can
    # be an adapter by producing this request shape — see docs/ADAPTERS.md.
    client: str = "unknown"
    game_id: str
    format: str
    opponent_seat: int = 0
    turn: int = 0
    observations: list[Observation] = Field(default_factory=list)
    # The AI's own deck (card names). Forge knows this exactly; used to detect
    # the precise Constructed format and to identify the AI's own archetype.
    deck_cards: list[str] = Field(default_factory=list)
    # Live game state for piloting advice. All optional: clients that do not
    # send them still get archetype-level advice from the guide.
    hand: list[str] = Field(default_factory=list)
    own_board: list[str] = Field(default_factory=list)
    opponent_board: list[str] = Field(default_factory=list)
    your_graveyard: list[str] = Field(default_factory=list)
    opponent_graveyard: list[str] = Field(default_factory=list)
    your_exile: list[str] = Field(default_factory=list)
    # v9: opponent deck probability inputs. opponent_deck_cards is the
    # opponent's main-deck card list with duplicate copies. The known zones are
    # removed from that list to compute card-by-card hand probabilities.
    opponent_deck_cards: list[str] = Field(default_factory=list)
    opponent_exile: list[str] = Field(default_factory=list)
    opponent_seen_hand: list[str] = Field(default_factory=list)
    life_totals: dict[str, int] = Field(default_factory=dict)
    # Current game phase (e.g. "MAIN1", "COMBAT_DECLARE_ATTACKERS"). Optional —
    # board-aware advice uses it to pick phase-appropriate actions.
    phase: str = ""
    # Mana available to the AI right now, in WUBRGC order or as a list of color
    # strings. Used to filter actions to ones the AI can actually pay for.
    available_mana: list[str] = Field(default_factory=list)
    # AI personality profile. Sent so the LLM can factor it into action scoring.
    personality: dict[str, str | int | float | bool] = Field(default_factory=dict)
    # v5: explicit hand/library sizes — sidecar prefers these over heuristics.
    ai_hand_size: int = 0
    opp_hand_size: int = 0
    ai_library_size: int = 0
    opp_library_size: int = 0
    # v5: richer board state with P/T and types.
    own_board_details: list[BoardCard] = Field(default_factory=list)
    opponent_board_details: list[BoardCard] = Field(default_factory=list)
    # v5: colors of mana the opponent has access to (from their lands).
    opponent_mana_colors_seen: list[str] = Field(default_factory=list)
    # v6: opponent resource signals for the strategist's next-turn prediction.
    opp_mana_available: int = 0
    opp_mana_spent_this_turn: int = 0
    # v6: one entry per untapped opponent mana source, each the colors that
    # source can produce (W/U/B/R/G/C). Lets the strategist reason about which
    # color combinations the opponent has open — e.g. an untapped Hallowed
    # Fountain is ["W", "U"] and a Mana Confluence is ["W", "U", "B", "R", "G"].
    opp_untapped_sources: list[list[str]] = Field(default_factory=list)
    # v6: which decision the AI is making — lets the sidecar spend extra effort
    # on high-impact decisions. One of: "mulligan" | "combat" | "priority" |
    # "critical". Empty defaults to a routine priority decision.
    decision_type: str = ""
    # v8: optional legal action gate. The sidecar may plan broadly, but action
    # scores that influence Forge should be drawn from these current candidates.
    legal_actions: list[LegalAction] = Field(default_factory=list)
    # v8: London mulligan cards to return if this hand is kept.
    cards_to_return: int = 0
    # v8: global sidecar influence weight, 0=no influence, 100=force if legal.
    sidecar_influence: int = 50
    # Pilot mode. "normal" is the production path. "solve" is set ONLY by the
    # self-play goldfishing runner against a non-interactive opponent: it tells
    # the advice engine to assume zero opponent interaction and chase the fastest
    # line. Production callers never set it, so the normal path is unaffected.
    pilot_mode: str = "normal"


class RoleAssessment(BaseModel):
    """Who's the Beatdown? Mike Flores's role-assignment doctrine.

    Role is matchup- and state-dependent and can flip mid-game; the wrong
    role assignment is a known losing pattern. The sidecar consults the
    natural role of each deck's strategy, the current board pressure, card
    advantage, and clock, and decides who should be the aggressor right now.
    """

    ai_role: str = "contested"  # "beatdown" | "control" | "contested"
    opponent_role: str = "contested"
    winning_side: str = "even"  # "ai" | "human" | "even"
    margin: float = 0.0  # 0-1, how decisively the winning side is ahead
    role_flipped: bool = False  # true when current state overrode the deck's natural role
    opp_hand_size: int = 0  # cards in human opponent's hand (real value from Forge, fallback heuristic otherwise)
    ai_attackers: int = 0  # creatures the AI could attack with right now
    opp_attackers: int = 0  # creatures the opponent could attack with right now
    # v5: per-axis strategic scores (-1..+1, AI's perspective). Combined into
    # winning_side but also surfaced so the dashboard can show all axes.
    board_score: float = 0.0  # board presence (creatures, P/T)
    cards_score: float = 0.0  # hand size + hand value vs opp
    clock_score: float = 0.0  # turns until lethal (positive = AI's clock is faster)
    tempo_score: float = 0.0  # land drops, mana usage, color screw
    graveyard_score: float = 0.0  # graveyard utility (opp deck uses graveyard?)
    # v5: tempo / mana signals on the opponent.
    opp_lands_dropped: int = 0
    opp_missed_drops: int = 0
    opp_color_screwed: bool = False
    opp_graveyard_utility: float = 0.0  # 0-1, how useful opp's graveyard is to their deck
    reasoning: str = ""  # one or two sentences citing life/board/card-advantage/clock


class HandValuation(BaseModel):
    """Per-card score for cards currently in the AI's hand."""

    card: str
    value: float = 0.0  # 0-100, higher = more valuable to keep / prioritize playing
    role: str = "filler"  # "threat" | "answer" | "ramp" | "card_draw" | "win_con" | "land" | "filler"
    reasoning: str = ""


class OpponentHandGuess(BaseModel):
    """Inferred category of card the human likely still holds, based on
    observed plays minus what's been seen, weighted by archetype confidence."""

    category: str = ""  # "counterspell" | "removal" | "wrath" | "threat" | "combo_piece" | ...
    example_cards: list[str] = Field(default_factory=list)
    probability: float = 0.0  # 0-1
    reasoning: str = ""


class OpponentCardProbability(BaseModel):
    """Probability the opponent currently has at least one copy of a card."""

    card: str
    remaining_copies: int = 0
    known_removed: int = 0
    probability: float = 0.0  # 0-1
    reasoning: str = ""


class CardDrawProbability(BaseModel):
    """Probability the AI draws a card as the next card from its library."""

    card: str
    remaining_copies: int = 0
    known_removed: int = 0
    probability: float = 0.0  # 0-1
    reasoning: str = ""


class TargetPriority(BaseModel):
    """Ordered target preference for a spell/ability with selectable targets.

    ``spell`` is the AI's spell whose targets we are ranking; an empty string
    means generic priority that applies when no spell-specific entry matches.
    """

    spell: str = ""
    targets: list[str] = Field(default_factory=list)
    reasoning: str = ""


class PredictedOppLine(BaseModel):
    """The opponent_strategist's prediction of the opponent's next turn."""

    primary_play: str = ""
    supporting_plays: list[str] = Field(default_factory=list)
    mana_required: str = ""
    reasoning: str = ""


class BeatdownAssessment(BaseModel):
    """Who's-the-beatdown call from the opponent_strategist (state-dependent)."""

    who_is_beatdown: str = ""  # "ai" | "opponent"
    reasoning: str = ""


class ComboPlan(BaseModel):
    """Combo-line recommendation for the AI's own deck."""

    line_name: str = ""
    go_for_it_now: bool = False
    readiness_score: float = 0.0  # 0-100
    missing_pieces: list[str] = Field(default_factory=list)
    needed_cards: list[str] = Field(default_factory=list)
    needed_mana: str = ""
    preferred_line: str = ""
    wait_reason: str = ""
    sequence: list[str] = Field(default_factory=list)
    protection_plan: str = ""
    risk_assessment: str = ""
    action_adjustments: list[ActionScore] = Field(default_factory=list)


class PlannedTurn(BaseModel):
    turn: int = 0
    land: str = ""
    spells: list[str] = Field(default_factory=list)
    mana_rationale: str = ""
    draw_assumption: str = ""
    opponent_adjustment: str = ""


class EarlyGamePlan(BaseModel):
    decision: str = ""  # keep | mulligan | none
    confidence: float = 0.0
    influence_weight: int = 50
    keep_reason: str = ""
    mulligan_reason: str = ""
    bottom_cards: list[str] = Field(default_factory=list)
    land_sequence: list[str] = Field(default_factory=list)
    fetch_plan: list[str] = Field(default_factory=list)
    planned_turns: list[PlannedTurn] = Field(default_factory=list)
    draw_assumptions: list[str] = Field(default_factory=list)
    probability_notes: str = ""
    contingencies: list[str] = Field(default_factory=list)
    estimated_win_turn: int | None = None
    estimated_loss_turn: int | None = None
    reasoning: str = ""
    last_updated_turn: int = 0


class ManaLandGuidance(BaseModel):
    """Per-land guidance within a deck's mana profile.

    Generated once per decklist by the mana_profile builder. Covers fetchlands,
    duals, basics, and utility lands (Otawara, Boseiju, etc.) so the strategist
    and the Java engine know how each land should be used.
    """

    card: str
    # "fetch" | "dual" | "shock" | "basic" | "fast" | "utility" | "creature_land" | "other"
    role: str = "other"
    colors: list[str] = Field(default_factory=list)  # colors this land can produce
    # When to play / crack / hold. Free text, but a few conventional phrasings:
    # "play_early", "hold_for_channel", "crack_now", "crack_end_of_turn", "sac_for_value".
    play_timing: str = ""
    enters_tapped: bool = False
    notes: str = ""


class ManaProfile(BaseModel):
    """One-time, LLM-generated manabase analysis for a specific decklist.

    Keyed by a hash of the deck's main-deck card list, persisted to disk, and
    reused across every game (and restart) that uses the same list. Because a
    post-sideboard configuration hashes differently, sideboarded games get a
    fresh profile automatically. This is the deck-specific knowledge that
    hand-authored archetype guides do not carry.
    """

    deck_hash: str = ""
    archetype: str = "Unknown"
    format: str = ""
    primary_colors: list[str] = Field(default_factory=list)
    # Prose summary of color/pip requirements over the curve (e.g. "needs RR by
    # turn 2 for the one-drops; splashes W from turn 3 for sideboard answers").
    color_requirements: str = ""
    # How to choose what to fetch: priorities, basic-vs-dual bias, Wasteland fear.
    fetch_priority: str = ""
    # Default crack timing when nothing else dictates: "now" | "end_of_turn" | "hold".
    crack_timing_default: str = "now"
    lands: list[ManaLandGuidance] = Field(default_factory=list)
    # Utility-land policy that doesn't fit a single land entry (sequencing
    # between Otawara/Boseiju, when to channel vs play as a land, etc.).
    utility_land_notes: str = ""
    reasoning: str = ""
    generated_by: str = ""  # model name that produced this profile
    schema_version: int = 1


class ManaPlan(BaseModel):
    """Per-action manabase decision the strategist hands to the Java engine.

    The LLM owns this outright (validated Java-side against the cards actually
    available). Fail-soft: when absent, the engine uses its stock heuristics.
    """

    # --- fetchland / search-to-battlefield ---
    # "now" | "end_of_turn" | "hold" | "auto" (auto = let the engine decide).
    crack_fetch: str = "auto"
    fetch_target: str = ""  # exact card to fetch (the LLM knows the remaining library)
    fetch_alternatives: list[str] = Field(default_factory=list)  # ranked fallbacks
    enter_untapped: bool = True  # pay life on a shock / choose untapped when offered
    # --- from-hand land sequencing ---
    land_to_play: str = ""  # which land to play from hand this turn
    land_alternatives: list[str] = Field(default_factory=list)
    # --- shared deterministic fallback ---
    color_needs: list[str] = Field(default_factory=list)  # colors needed soon, priority order
    # Utility lands the engine should NOT spend yet (hold for channel / late value).
    hold_utility_lands: list[str] = Field(default_factory=list)
    reasoning: str = ""


class PilotingAdvice(BaseModel):
    """Advice on how the AI should play its own deck this turn."""

    own_archetype: str = "Unknown"
    guide_source: str = ""  # which guide answered: "<format>/<slug>" or "generic/<strategy>"
    recommended_play: str = ""
    reasoning: str = ""
    alternatives: list[str] = Field(default_factory=list)
    mulligan_advice: str = ""  # populated on turn 0 instead of recommended_play
    actions: list[ActionScore] = Field(
        default_factory=list
    )  # structured action recommendations with percentages
    # Board-aware additions (v4):
    role: RoleAssessment | None = None
    hand_values: list[HandValuation] = Field(default_factory=list)
    opponent_hand: list[OpponentHandGuess] = Field(default_factory=list)
    opponent_card_probabilities: list[OpponentCardProbability] = Field(default_factory=list)
    ai_draw_probabilities: list[CardDrawProbability] = Field(default_factory=list)
    target_priorities: list[TargetPriority] = Field(default_factory=list)
    # Surfaced from the resolved piloting guide so the dashboard / Forge UI
    # can show a more precise plan rather than just a slug.
    guide_overview: str = ""
    phase_plan: list[str] = Field(default_factory=list)  # plan steps for the current phase
    key_cards: list[dict] = Field(default_factory=list)  # [{name, role, notes}]
    sequencing_tips: list[str] = Field(default_factory=list)
    matchup_advice: str = ""  # specific advice vs the identified opponent archetype
    # v6 opponent_strategist outputs (None when no profile / strategist skipped).
    predicted_opp_line: PredictedOppLine | None = None
    beatdown_assessment: BeatdownAssessment | None = None
    # v7 combo_strategist output (None for non-combo decks / no combo data).
    combo_plan: ComboPlan | None = None
    # v8 rolling mulligan / early-game plan.
    early_game_plan: EarlyGamePlan | None = None
    # v10 per-action manabase decision (fetch/land/utility). LLM-owned, fail-soft.
    mana_plan: ManaPlan | None = None


class TrainingExample(BaseModel):
    """A single training data point from a log analysis checkpoint."""

    game_id: str = ""
    turn: int = 0
    format: str = ""
    observations: list[dict] = Field(default_factory=list)
    deck_cards: list[str] = Field(default_factory=list)
    hand: list[str] = Field(default_factory=list)
    own_board: list[str] = Field(default_factory=list)
    opponent_board: list[str] = Field(default_factory=list)
    your_graveyard: list[str] = Field(default_factory=list)
    opponent_graveyard: list[str] = Field(default_factory=list)
    life_totals: dict[str, int] = Field(default_factory=dict)
    # LLM response at this checkpoint
    archetype: str = ""
    confidence: float = 0.0
    reasoning: str = ""
    alternatives: list[str] = Field(default_factory=list)


class RecognitionResponse(BaseModel):
    archetype: str
    confidence: float
    reasoning: str
    alternatives: list[str] = Field(default_factory=list)
    piloting: PilotingAdvice | None = None
    schema_version: int = SCHEMA_VERSION


# ---------------------------------------------------------------------------
# LangGraph state. Kept as a superset TypedDict so the HTTP contract and the
# graph state can evolve independently.
# ---------------------------------------------------------------------------
class LessonEvidence(BaseModel):
    """Quantitative backing for a self-play lesson."""

    # Mean turns-to-win improvement attributed to following this lesson
    # (positive = faster wins). Reflection fills this from aggregate stats.
    turns_to_win_delta: float = 0.0
    # Number of games the lesson was distilled from. Gates injection.
    n_games: int = 0


class Lesson(BaseModel):
    """A distilled, context-tagged self-play lesson.

    Produced by the /selfplay/reflect endpoint, auto-staged to the learnings
    store, and injected (capped + gated) alongside the curated piloting guide.
    Lessons are stored CONDITIONALLY: ``context`` records the situation they
    were learned in (e.g. "no_interaction"), and injection only fires when the
    live game state matches that context.
    """

    archetype: str
    format: str = ""
    # The situation this lesson applies to. "no_interaction" lessons come from
    # goldfishing and only inject when the opponent has shown no interaction.
    context: str = "no_interaction"
    trigger: str = ""
    recommendation: str
    evidence: LessonEvidence = Field(default_factory=LessonEvidence)
    confidence: float = 0.0
    created_at: str = ""
    source: str = "selfplay"


class GraphState(TypedDict, total=False):
    # inputs
    game_id: str
    # Seat identifier for the AI making the request. Two AI seats in one game
    # share a game_id but have different own decks, so per-game caches key on
    # (game_id, opponent_seat) to avoid cross-contamination.
    opponent_seat: int
    format: str
    turn: int
    observations: list[dict]
    deck_cards: list[str]
    hand: list[str]
    own_board: list[str]
    opponent_board: list[str]
    your_graveyard: list[str]
    opponent_graveyard: list[str]
    your_exile: list[str]
    opponent_deck_cards: list[str]
    opponent_exile: list[str]
    opponent_seen_hand: list[str]
    life_totals: dict[str, int]
    phase: str
    available_mana: list[str]
    personality: dict[str, str | int | float | bool]
    # v5 additions
    ai_hand_size: int
    opp_hand_size: int
    ai_library_size: int
    opp_library_size: int
    own_board_details: list[dict]
    opponent_board_details: list[dict]
    opponent_mana_colors_seen: list[str]
    # v6 inputs for the strategist
    opp_mana_available: int
    opp_mana_spent_this_turn: int
    opp_untapped_sources: list[list[str]]
    decision_type: str
    legal_actions: list[dict]
    cards_to_return: int
    sidecar_influence: int
    pilot_mode: str
    # resolved by the game_advisor node
    resolved_format: str | None
    candidate_archetypes: list[dict]
    own_archetype: str | None
    piloting_guide: dict | None
    # opponent-recognition outputs of the game_advisor node
    archetype: str | None
    confidence: float | None
    reasoning: str | None
    alternatives: list[str]
    # piloting outputs of the game_advisor node
    recommended_play: str | None
    play_reasoning: str | None
    play_alternatives: list[str]
    mulligan_advice: str | None
    guide_source: str | None
    actions: list[dict]
    role: dict | None
    hand_values: list[dict]
    opponent_hand: list[dict]
    opponent_card_probabilities: list[dict]
    ai_draw_probabilities: list[dict]
    target_priorities: list[dict]
    # v6 opponent_strategist outputs
    predicted_opp_line: dict | None
    beatdown_assessment: dict | None
    # v7 combo_strategist outputs
    combo_plan: dict | None
    # v8 mulligan planner outputs
    early_game_plan: dict | None
    # v10 per-deck mana profile (lazy/offline, disk-cached) + per-action plan
    mana_profile: dict | None
    mana_plan: dict | None
