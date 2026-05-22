"""FastAPI entrypoint for the Forge LLM sidecar."""

from __future__ import annotations

import hashlib
import json as _json
import logging
from collections import OrderedDict
from contextlib import asynccontextmanager
from datetime import datetime, timezone

from fastapi import FastAPI, Request
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel, Field

from app.config import CONFIG
from app.graph import get_graph
from app.knowledge import learnings, loader, metagame, piloting
from app.llm_client import LLMError, generate_json, is_reachable
from app.nodes import game_advisor
from app.schema import (
    ActionScore,
    BeatdownAssessment,
    CardDrawProbability,
    ComboPlan,
    EarlyGamePlan,
    HandValuation,
    LegalAction,
    Lesson,
    LessonEvidence,
    OpponentCardProbability,
    OpponentHandGuess,
    PilotingAdvice,
    PredictedOppLine,
    RecognitionRequest,
    RecognitionResponse,
    RoleAssessment,
    TargetPriority,
    TrainingExample,
)
from app import selfplay_store
from app.opponent_hand_probability import ai_draw_probabilities, opponent_card_probabilities
from app.store import get_store

logging.basicConfig(level=logging.INFO)
log = logging.getLogger("forge-llm-sidecar")

_static_dir = (__import__("pathlib").Path(__file__).parent / "static").resolve()


@asynccontextmanager
async def _lifespan(_: FastAPI):
    get_graph()
    log.info("Graph compiled. Model=%s LLM=%s", CONFIG.model_name, CONFIG.llm_base_url)
    yield


app = FastAPI(title="Forge LLM Sidecar", version="0.1.0", lifespan=_lifespan)


@app.middleware("http")
async def _log_every_request(request: Request, call_next):
    # Record every HTTP hit so the dashboard reflects connection attempts even
    # when validation fails (422) or the request never reaches /recognize.
    response = await call_next(request)
    path = request.url.path
    if path not in (
        "/api/stats",
        "/api/selfplay/trends",
        "/dashboard",
    ) and not path.startswith("/static"):
        client_host = request.client.host if request.client else "?"
        log.info(
            "HTTP %s %s -> %d (from %s)",
            request.method,
            path,
            response.status_code,
            client_host,
        )
        get_store().record_hit(
            {
                "method": request.method,
                "path": path,
                "status": response.status_code,
                "client": client_host,
            }
        )
    return response


app.mount("/static", StaticFiles(directory=str(_static_dir)), name="static")


@app.get("/")
async def root_dashboard() -> FileResponse:
    """Serve the dashboard HTML at the root for browser convenience."""
    return FileResponse(str(_static_dir / "dashboard.html"))


@app.get("/dashboard")
async def dashboard_root() -> FileResponse:
    """Serve the dashboard HTML."""
    return FileResponse(str(_static_dir / "dashboard.html"))


@app.get("/health")
async def health() -> dict:
    """Lightweight availability check used by Forge for a fail-soft gate."""
    return {
        "status": "ok",
        "model": CONFIG.model_name,
        "llm_reachable": await is_reachable(),
        "metagame_enabled": CONFIG.metagame_enable,
    }


# --- Recognize cache --------------------------------------------------------
#
# Per-game LRU keyed on a digest of inputs that meaningfully affect the result.
# The observer fires /recognize on every life change and zone churn — most of
# those don't change the inputs the model actually reasons about, so a hit
# returns the previous response in <1ms instead of waiting on the LLM.
#
# Cache key inputs (deliberately limited):
#   game_id, turn, observations, decklists, hand, own/opp board, known graveyard/exile,
#   life totals, phase, available mana, opp mana colors, board details.
#
# Excluded (don't influence the recognizer / advisor in a meaningful way):
#   library sizes, personality (handled client-side post-hoc), ai_hand_size
#   (already in `hand`).
_RECOGNIZE_CACHE: "OrderedDict[str, dict]" = OrderedDict()
_RECOGNIZE_CACHE_LIMIT = 256
_recognize_cache_hits = 0
_recognize_cache_misses = 0
# Computed once at startup; busts the cache when any archetype profile changes.
_PROFILES_VERSION = loader.all_profiles_version()
_COMBO_PROFILES_VERSION = piloting.combo_profiles_version()


def _recognize_cache_key(req: RecognitionRequest) -> str:
    payload = {
        "g": req.game_id,
        "seat": req.opponent_seat,
        "t": req.turn,
        "o": [
            (o.turn, o.event, o.card, o.cmc, list(o.colors), list(o.types))
            for o in req.observations
        ],
        "d": sorted(req.deck_cards),
        "h": sorted(req.hand),
        "ob": sorted(req.own_board),
        "pb": sorted(req.opponent_board),
        "og": sorted(req.your_graveyard),
        "pg": sorted(req.opponent_graveyard),
        "ye": sorted(req.your_exile),
        "od": sorted(req.opponent_deck_cards),
        "oe": sorted(req.opponent_exile),
        "osh": sorted(req.opponent_seen_hand),
        "l": sorted(req.life_totals.items()),
        "p": req.phase,
        "m": sorted(req.available_mana),
        "om": sorted(req.opponent_mana_colors_seen),
        "obd": [
            (bc.name, bc.power, bc.toughness, sorted(bc.types), bc.is_creature, bc.tapped)
            for bc in req.own_board_details
        ],
        "opd": [
            (bc.name, bc.power, bc.toughness, sorted(bc.types), bc.is_creature, bc.tapped)
            for bc in req.opponent_board_details
        ],
        # v6/v7: strategist inputs. decision_type changes how hard the sidecar
        # reasons; opp mana feeds disruption and next-turn prediction. Profile
        # tokens bust cache whenever archetype or combo profile files change.
        "dt": req.decision_type,
        "oma": req.opp_mana_available,
        "oms": req.opp_mana_spent_this_turn,
        "ous": req.opp_untapped_sources,
        "la": [a.model_dump() for a in req.legal_actions],
        "ctr": req.cards_to_return,
        "si": req.sidecar_influence,
        "pm": req.pilot_mode,
        "pv": _PROFILES_VERSION,
        "cv": _COMBO_PROFILES_VERSION,
        # Computed per-request (cheap) so cache busts when /selfplay/reflect
        # stages new learnings while the process is running.
        "lv": learnings.learnings_version(),
    }
    encoded = _json.dumps(payload, sort_keys=True, default=str).encode("utf-8")
    return hashlib.blake2b(encoded, digest_size=16).hexdigest()


def _cache_get(key: str) -> dict | None:
    global _recognize_cache_hits, _recognize_cache_misses
    cached = _RECOGNIZE_CACHE.get(key)
    if cached is None:
        _recognize_cache_misses += 1
        return None
    _RECOGNIZE_CACHE.move_to_end(key)
    _recognize_cache_hits += 1
    return cached


def _cache_put(key: str, value: dict) -> None:
    _RECOGNIZE_CACHE[key] = value
    _RECOGNIZE_CACHE.move_to_end(key)
    while len(_RECOGNIZE_CACHE) > _RECOGNIZE_CACHE_LIMIT:
        _RECOGNIZE_CACHE.popitem(last=False)


@app.post("/api/stats/reset")
async def api_stats_reset() -> dict:
    """Clear request history and counters (uptime preserved)."""
    get_store().reset()
    global _recognize_cache_hits, _recognize_cache_misses
    _RECOGNIZE_CACHE.clear()
    _recognize_cache_hits = 0
    _recognize_cache_misses = 0
    return {"status": "ok", "cleared": True}


@app.get("/api/stats")
async def api_stats() -> dict:
    """Dashboard stats: uptime, request counts, and recognition history."""
    store = get_store()
    return {
        "uptime_seconds": round(store.uptime_seconds, 1),
        "total_requests": store.total_requests,
        "history": store.history,
        "raw_hits_total": store.raw_hits_total,
        "raw_hits": store.raw_hits,
        "recognize_cache": {
            "size": len(_RECOGNIZE_CACHE),
            "hits": _recognize_cache_hits,
            "misses": _recognize_cache_misses,
        },
    }


class RecordRunRequest(BaseModel):
    """One self-play run (one runner invocation) to persist into the results DB.

    ``records`` are the runner's per-seat dicts (archetype, opponent, pilot_mode,
    won, win_turn, turns) — the same objects it writes to JSONL.
    """

    records: list[dict] = Field(default_factory=list)
    format: str = ""
    config: str = ""
    label: str = ""
    source_file: str = ""


class RecordRunResponse(BaseModel):
    run_id: int
    n_games: int
    n_wins: int
    db_path: str


@app.post("/selfplay/record", response_model=RecordRunResponse)
async def selfplay_record(req: RecordRunRequest) -> RecordRunResponse:
    """Persist a finished self-play run so it can be baselined and tracked over time.

    Called by the Java ``SelfPlayRunner`` at the end of a run (it also keeps
    writing JSONL as the raw artifact). The server snapshots ``learnings_version``
    and the git sha so the run lines up against the learnings state at that moment.
    """
    conn = selfplay_store.connect()
    try:
        run_id = selfplay_store.insert_run(
            conn,
            records=req.records,
            config=req.config,
            format=req.format,
            learnings_version=selfplay_store.current_learnings_version(),
            git_sha=selfplay_store.current_git_sha(),
            source_file=req.source_file,
            label=req.label,
        )
    finally:
        conn.close()
    n_wins = sum(1 for r in req.records if r.get("won"))
    log.info(
        "Recorded self-play run #%d: %d games, %d wins (%s / %s)",
        run_id,
        len(req.records),
        n_wins,
        req.format or "?",
        req.config or "?",
    )
    return RecordRunResponse(
        run_id=run_id,
        n_games=len(req.records),
        n_wins=n_wins,
        db_path=str(selfplay_store.db_path()),
    )


@app.get("/api/selfplay/trends")
async def api_selfplay_trends(archetype: str | None = None) -> dict:
    """Self-play results over time for the dashboard panel.

    Default: per-deck baseline-vs-latest with deltas. With ``?archetype=`` the
    full per-run series for one deck. Fail-soft: if no results DB exists yet,
    returns empty groups so the panel renders blank rather than erroring.
    """
    if not selfplay_store.db_exists():
        return {"groups": []}
    conn = selfplay_store.connect()
    try:
        if archetype:
            return {"groups": selfplay_store.archetype_trend(conn, archetype)}
        return {"groups": selfplay_store.baseline_vs_latest(conn)}
    finally:
        conn.close()


@app.get("/metagame")
async def get_metagame(format: str = "modern") -> dict:
    """Debug endpoint: show the loaded metagame breakdown for a format.

    ``format`` may be a Forge format name or a metagame slug (e.g. "modern").
    """
    slug = metagame.resolve_meta_format(format) or format.strip().lower()
    archetypes = metagame.get_metagame(slug)
    return {
        "requested": format,
        "meta_slug": slug,
        "enabled": CONFIG.metagame_enable,
        "info": metagame.metagame_info(slug),
        "count": len(archetypes),
        "archetypes": archetypes,
    }


@app.get("/piloting")
async def get_piloting(format: str = "modern", archetype: str = "") -> dict:
    """Debug endpoint: show the piloting guide resolved for an archetype.

    ``format`` may be a Forge format name or a metagame slug. With no
    ``archetype`` it lists the available guides instead.
    """
    slug = metagame.resolve_meta_format(format) or format.strip().lower()
    if not archetype:
        return {"requested": format, "meta_slug": slug, "guides": piloting.available_guides()}
    guide = piloting.get_piloting_guide(archetype, slug)
    return {
        "requested": format,
        "meta_slug": slug,
        "archetype": archetype,
        "guide": guide.model_dump() if guide else None,
    }


class IdentifyOwnArchetypeRequest(BaseModel):
    """Pre-game heuristic lookup of the AI's own archetype from its decklist."""

    game_id: str = ""
    format: str = "Constructed"
    deck_cards: list[str] = Field(default_factory=list)


class IdentifyOwnArchetypeResponse(BaseModel):
    """Result of the deterministic decklist -> archetype match."""

    own_archetype: str = "Unknown"
    strategy_type: str = ""
    guide_source: str = ""
    resolved_format: str = ""


@app.post("/identify-own-archetype", response_model=IdentifyOwnArchetypeResponse)
async def identify_own_archetype(
    req: IdentifyOwnArchetypeRequest,
) -> IdentifyOwnArchetypeResponse:
    """Heuristic, no-LLM identification of the AI's own archetype.

    Forge calls this once at game start so the dashboard knows the AI's archetype
    before any opponent action triggers ``/recognize``. The result is cached in
    the game_advisor per-game maps so the first ``/recognize`` call reuses it.
    """
    slug = metagame.resolve_meta_format(req.format) or CONFIG.default_meta_format
    name, strategy = piloting.identify_own_archetype(req.deck_cards, slug)

    if req.game_id:
        game_advisor._resolved_format[req.game_id] = slug
        game_advisor._own_archetype[req.game_id] = (name, strategy)

    guide = piloting.get_piloting_guide(name or "", slug, strategy)
    guide_source = ""
    if guide:
        is_specific = name and piloting.slugify(guide.archetype) == piloting.slugify(name)
        guide_source = (
            f"{slug}/{piloting.slugify(name)}"
            if is_specific
            else f"generic/{guide.strategy_type.value}"
        )

    own_name = name or "Unknown"
    log.info(
        "identify-own-archetype: game=%s format=%s -> %s (%s)",
        req.game_id,
        slug,
        own_name,
        guide_source or "no-guide",
    )

    # Drop a dashboard entry so "Own Archetype" no longer reads Unknown
    # before the first /recognize call.
    piloting_advice = PilotingAdvice(
        own_archetype=own_name,
        guide_source=guide_source,
        recommended_play="",
        reasoning="",
        alternatives=[],
        mulligan_advice="",
        actions=[],
    )
    get_store().record(
        {
            "game_id": req.game_id,
            "format": req.format,
            "turn": 0,
            "archetype": "Unknown",
            "confidence": 0.0,
            "reasoning": "Pre-game: own archetype identified from decklist.",
            "alternatives": [],
            "piloting": piloting_advice.model_dump(),
            "actions": [],
            "observations": [],
            "opponent_board": [],
            "opponent_graveyard": [],
        }
    )

    return IdentifyOwnArchetypeResponse(
        own_archetype=own_name,
        strategy_type=strategy.value if strategy else "",
        guide_source=guide_source,
        resolved_format=slug,
    )


@app.post("/recognize", response_model=RecognitionResponse)
async def recognize(req: RecognitionRequest) -> RecognitionResponse:
    """Run the game-advisor graph: opponent recognition + own-deck piloting advice."""
    log.info("recognize: client=%s game=%s turn=%s", req.client, req.game_id, req.turn)
    cache_key = _recognize_cache_key(req)
    cached = _cache_get(cache_key)
    if cached is not None:
        log.info(
            "recognize: cache hit game=%s turn=%s (hits=%d misses=%d)",
            req.game_id,
            req.turn,
            _recognize_cache_hits,
            _recognize_cache_misses,
        )
        return RecognitionResponse(**cached)
    graph = get_graph()
    initial = {
        "game_id": req.game_id,
        "opponent_seat": req.opponent_seat,
        "format": req.format,
        "turn": req.turn,
        "observations": [o.model_dump() for o in req.observations],
        "deck_cards": req.deck_cards,
        "hand": req.hand,
        "own_board": req.own_board,
        "opponent_board": req.opponent_board,
        "your_graveyard": req.your_graveyard,
        "opponent_graveyard": req.opponent_graveyard,
        "your_exile": req.your_exile,
        "opponent_deck_cards": req.opponent_deck_cards,
        "opponent_exile": req.opponent_exile,
        "opponent_seen_hand": req.opponent_seen_hand,
        "life_totals": req.life_totals,
        "phase": req.phase,
        "available_mana": req.available_mana,
        "personality": req.personality,
        "alternatives": [],
        "ai_hand_size": req.ai_hand_size,
        "opp_hand_size": req.opp_hand_size,
        "ai_library_size": req.ai_library_size,
        "opp_library_size": req.opp_library_size,
        "own_board_details": [bc.model_dump() for bc in req.own_board_details],
        "opponent_board_details": [bc.model_dump() for bc in req.opponent_board_details],
        "opponent_mana_colors_seen": req.opponent_mana_colors_seen,
        "opp_mana_available": req.opp_mana_available,
        "opp_mana_spent_this_turn": req.opp_mana_spent_this_turn,
        "opp_untapped_sources": req.opp_untapped_sources,
        "decision_type": req.decision_type,
        "legal_actions": [a.model_dump() for a in req.legal_actions],
        "cards_to_return": req.cards_to_return,
        "sidecar_influence": req.sidecar_influence,
        "pilot_mode": req.pilot_mode,
    }
    final = await graph.ainvoke(initial)
    response = _response_from_final(req, final)
    _cache_put(cache_key, response.model_dump())
    return response


def _response_from_final(req: RecognitionRequest, final: dict) -> RecognitionResponse:
    raw_actions = final.get("actions") or []
    actions = [ActionScore(**a) for a in raw_actions if isinstance(a, dict)]
    role_raw = final.get("role")
    role_obj = RoleAssessment(**role_raw) if isinstance(role_raw, dict) else None
    hand_values = [
        HandValuation(**hv) for hv in (final.get("hand_values") or []) if isinstance(hv, dict)
    ]
    opponent_hand = [
        OpponentHandGuess(**g) for g in (final.get("opponent_hand") or []) if isinstance(g, dict)
    ]
    opponent_cards = [
        OpponentCardProbability(**g)
        for g in opponent_card_probabilities(final)
        if isinstance(g, dict)
    ]
    ai_draws = [
        CardDrawProbability(**g)
        for g in ai_draw_probabilities(final)
        if isinstance(g, dict)
    ]
    target_priorities = [
        TargetPriority(**t)
        for t in (final.get("target_priorities") or [])
        if isinstance(t, dict)
    ]
    predicted_raw = final.get("predicted_opp_line")
    predicted_opp_line = (
        PredictedOppLine(**predicted_raw) if isinstance(predicted_raw, dict) else None
    )
    beatdown_raw = final.get("beatdown_assessment")
    beatdown_assessment = (
        BeatdownAssessment(**beatdown_raw) if isinstance(beatdown_raw, dict) else None
    )
    combo_raw = final.get("combo_plan")
    combo_plan = ComboPlan(**combo_raw) if isinstance(combo_raw, dict) else None
    early_raw = final.get("early_game_plan")
    early_game_plan = EarlyGamePlan(**early_raw) if isinstance(early_raw, dict) else None
    guide = final.get("piloting_guide") or {}
    phase_bucket = (
        "early_game" if req.turn <= 3 else "mid_game" if req.turn <= 7 else "late_game"
    )
    phase_plan = (guide.get("game_plan") or {}).get(phase_bucket, []) if guide else []
    key_cards = guide.get("key_cards") or [] if guide else []
    sequencing_tips = guide.get("sequencing_tips") or [] if guide else []
    matchup_advice = ""
    if guide and final.get("archetype"):
        target = (final.get("archetype") or "").strip().lower()
        for m in guide.get("matchups") or []:
            ma = (m.get("opponent_archetype") or "").strip().lower()
            if ma and (ma in target or target in ma):
                matchup_advice = m.get("advice") or ""
                break

    piloting_advice = PilotingAdvice(
        own_archetype=final.get("own_archetype") or "Unknown",
        guide_source=final.get("guide_source") or "",
        recommended_play=final.get("recommended_play") or "",
        reasoning=final.get("play_reasoning") or "",
        alternatives=final.get("play_alternatives") or [],
        mulligan_advice=final.get("mulligan_advice") or "",
        actions=actions,
        role=role_obj,
        hand_values=hand_values,
        opponent_hand=opponent_hand,
        opponent_card_probabilities=opponent_cards,
        ai_draw_probabilities=ai_draws,
        target_priorities=target_priorities,
        guide_overview=(guide.get("overview") or "") if guide else "",
        phase_plan=[str(s) for s in phase_plan if s],
        key_cards=[
            {"name": k.get("name", ""), "role": k.get("role", ""), "notes": k.get("notes", "")}
            for k in key_cards
            if isinstance(k, dict)
        ],
        sequencing_tips=[str(s) for s in sequencing_tips if s],
        matchup_advice=matchup_advice,
        predicted_opp_line=predicted_opp_line,
        beatdown_assessment=beatdown_assessment,
        combo_plan=combo_plan,
        early_game_plan=early_game_plan,
    )

    _store = get_store()
    _store.record(
        {
            "game_id": req.game_id,
            "format": req.format,
            "turn": req.turn,
            "archetype": final.get("archetype") or "Unknown",
            "confidence": final.get("confidence") or 0.0,
            "reasoning": (final.get("reasoning") or "")[:300],
            "alternatives": final.get("alternatives") or [],
            "piloting": piloting_advice.model_dump(),
            "actions": [a.model_dump() for a in actions],
            "observations": [o.model_dump() for o in req.observations],
            "opponent_board": req.opponent_board,
            "opponent_graveyard": req.opponent_graveyard,
            "opp_mana_available": req.opp_mana_available,
            "opp_mana_spent_this_turn": req.opp_mana_spent_this_turn,
            "opp_untapped_sources": req.opp_untapped_sources,
            "decision_type": req.decision_type,
        }
    )

    return RecognitionResponse(
        archetype=final.get("archetype") or "Unknown",
        confidence=final.get("confidence") or 0.0,
        reasoning=final.get("reasoning") or "",
        alternatives=final.get("alternatives") or [],
        piloting=piloting_advice,
    )


@app.post("/mulligan-plan", response_model=RecognitionResponse)
async def mulligan_plan(req: RecognitionRequest) -> RecognitionResponse:
    """Synchronous opening-hand planner for Forge mulligan decisions."""
    req.decision_type = "mulligan"
    log.info(
        "mulligan-plan: client=%s game=%s ai_turn=%s cards_to_return=%s influence=%s",
        req.client,
        req.game_id,
        req.turn,
        req.cards_to_return,
        req.sidecar_influence,
    )
    cache_key = _recognize_cache_key(req)
    cached = _cache_get(cache_key)
    if cached is not None:
        return RecognitionResponse(**cached)
    graph = get_graph()
    initial = {
        "game_id": req.game_id,
        "opponent_seat": req.opponent_seat,
        "format": req.format,
        "turn": req.turn,
        "observations": [o.model_dump() for o in req.observations],
        "deck_cards": req.deck_cards,
        "hand": req.hand,
        "own_board": req.own_board,
        "opponent_board": req.opponent_board,
        "your_graveyard": req.your_graveyard,
        "opponent_graveyard": req.opponent_graveyard,
        "your_exile": req.your_exile,
        "opponent_deck_cards": req.opponent_deck_cards,
        "opponent_exile": req.opponent_exile,
        "opponent_seen_hand": req.opponent_seen_hand,
        "life_totals": req.life_totals,
        "phase": req.phase,
        "available_mana": req.available_mana,
        "personality": req.personality,
        "alternatives": [],
        "ai_hand_size": req.ai_hand_size,
        "opp_hand_size": req.opp_hand_size,
        "ai_library_size": req.ai_library_size,
        "opp_library_size": req.opp_library_size,
        "own_board_details": [bc.model_dump() for bc in req.own_board_details],
        "opponent_board_details": [bc.model_dump() for bc in req.opponent_board_details],
        "opponent_mana_colors_seen": req.opponent_mana_colors_seen,
        "opp_mana_available": req.opp_mana_available,
        "opp_mana_spent_this_turn": req.opp_mana_spent_this_turn,
        "opp_untapped_sources": req.opp_untapped_sources,
        "decision_type": "mulligan",
        "legal_actions": [a.model_dump() for a in req.legal_actions],
        "cards_to_return": req.cards_to_return,
        "sidecar_influence": req.sidecar_influence,
        "pilot_mode": req.pilot_mode,
    }
    final = await graph.ainvoke(initial)
    response = _response_from_final(req, final)
    _cache_put(cache_key, response.model_dump())
    return response


class ForgeLogAnalyzeRequest(BaseModel):
    """Request body for forge-log analysis endpoint."""

    log: str
    game_id: str = "log-session"
    format: str = "Constructed"
    opponent: str = ""
    ai_player: str = ""
    deck_cards: list[str] = Field(default_factory=list)


class ForgeLogAnalyzeResponse(BaseModel):
    """Response from forge-log analysis: list of checkpoints with LLM results."""

    checkpoints: list[dict]
    training_data: list[TrainingExample]


@app.post("/forge-log/analyze", response_model=ForgeLogAnalyzeResponse)
async def forge_log_analyze(req: ForgeLogAnalyzeRequest) -> ForgeLogAnalyzeResponse:
    """Parse a Forge game log, run the graph at each checkpoint, return full analysis.

    Returns every checkpoint (turn boundary + opponent action) with the sidecar's
    recognition + piloting response.  Also returns flat training data suitable for
    model fine-tuning.
    """
    from app.forge_log import ForgeLogAdapter

    adapter = ForgeLogAdapter(game_id=req.game_id, format=req.format)
    if req.opponent:
        adapter.set_opponent(req.opponent)
    if req.ai_player:
        adapter.set_ai_player(req.ai_player)

    checkpoints = adapter.parse(req.log)
    graph = get_graph()

    results = []
    training_data = []

    for i, cp in enumerate(checkpoints):
        cp.deck_cards = req.deck_cards
        log.info(
            "forge-log checkpoint %d: turn=%s obs=%d",
            i,
            cp.turn,
            len(cp.observations),
        )

        initial = {
            "game_id": cp.game_id,
            "format": cp.format,
            "turn": cp.turn,
            "observations": [o.model_dump() for o in cp.observations],
            "deck_cards": cp.deck_cards,
            "hand": cp.hand,
            "own_board": cp.own_board,
            "opponent_board": cp.opponent_board,
            "your_graveyard": cp.your_graveyard,
            "opponent_graveyard": cp.opponent_graveyard,
            "life_totals": cp.life_totals,
            "personality": {},
            "alternatives": [],
        }
        final = await graph.ainvoke(initial)

        raw_actions = final.get("actions") or []
        actions = [ActionScore(**a) for a in raw_actions if isinstance(a, dict)]
        combo_raw = final.get("combo_plan")
        combo_plan = ComboPlan(**combo_raw) if isinstance(combo_raw, dict) else None
        piloting_advice = PilotingAdvice(
            own_archetype=final.get("own_archetype") or "Unknown",
            guide_source=final.get("guide_source") or "",
            recommended_play=final.get("recommended_play") or "",
            reasoning=final.get("play_reasoning") or "",
            alternatives=final.get("play_alternatives") or [],
            mulligan_advice=final.get("mulligan_advice") or "",
            actions=actions,
            combo_plan=combo_plan,
        )
        resp = RecognitionResponse(
            archetype=final.get("archetype") or "Unknown",
            confidence=final.get("confidence") or 0.0,
            reasoning=final.get("reasoning") or "",
            alternatives=final.get("alternatives") or [],
            piloting=piloting_advice,
        )

        results.append(
            {
                "index": i,
                "turn": cp.turn,
                "request": cp.model_dump(),
                "response": resp.model_dump(),
            }
        )

        training_data.append(
            TrainingExample(
                game_id=cp.game_id,
                turn=cp.turn,
                format=cp.format,
                observations=[o.model_dump() for o in cp.observations],
                deck_cards=cp.deck_cards,
                hand=cp.hand,
                own_board=cp.own_board,
                opponent_board=cp.opponent_board,
                your_graveyard=cp.your_graveyard,
                opponent_graveyard=cp.opponent_graveyard,
                life_totals=cp.life_totals,
                archetype=resp.archetype,
                confidence=resp.confidence,
                reasoning=resp.reasoning,
                alternatives=resp.alternatives,
            )
        )

    return ForgeLogAnalyzeResponse(
        checkpoints=results,
        training_data=training_data,
    )


# ---------------------------------------------------------------------------
# Self-play reflection: distill a batch of goldfishing/mirror games into
# context-tagged lessons and auto-stage them to the learnings store.
# ---------------------------------------------------------------------------


class ReflectGame(BaseModel):
    """One self-play game outcome from the runner's per-seat JSONL."""

    won: bool = False
    # Turn the deck won on (None for losses/timeouts). Lower is better.
    win_turn: int | None = None
    # Compact per-turn action summary, if the runner captured it.
    actions: list[str] = Field(default_factory=list)
    # Raw Forge game log, optional fallback when `actions` is absent.
    log: str = ""


class ReflectRequest(BaseModel):
    """A batch of games for ONE (archetype, context, pilot_mode) to reflect on."""

    format: str
    archetype: str
    # Context the resulting lessons are tagged with — gates injection later.
    context: str = "no_interaction"
    pilot_mode: str = "solve"
    games: list[ReflectGame]
    # Auto-stage the distilled lessons to the learnings store.
    stage: bool = True
    # Hard cap on lessons distilled per batch (keeps the injected block small).
    max_lessons: int = 5


class ReflectResponse(BaseModel):
    archetype: str
    format: str
    context: str
    n_games: int
    n_wins: int
    win_rate: float
    fastest_win: int | None = None
    mean_win_turn: float | None = None
    median_win_turn: float | None = None
    lessons: list[Lesson] = Field(default_factory=list)
    staged_path: str | None = None


_REFLECT_SYSTEM_PROMPT = (
    "You are a Magic: The Gathering goldfishing analyst. Given aggregate "
    "self-play results and per-game summaries for ONE deck, distill a few "
    "concise, CONDITIONAL lessons that would help it win in fewer turns. Each "
    "lesson has a 'trigger' (the situation) and a 'recommendation' (the line). "
    "Keep each recommendation under ~25 words. Do not invent cards. Phrase "
    "lessons conditionally, never as 'always ignore interaction'. Always answer "
    "with a single JSON object and nothing else."
)


def _summarize_games(games: list[ReflectGame], *, limit: int = 12) -> str:
    """Compact textual summary of the most informative games (fastest wins
    first, then a few losses), bounded so the reflection stays one cheap call."""
    wins = sorted(
        (g for g in games if g.won and g.win_turn is not None),
        key=lambda g: g.win_turn,
    )
    losses = [g for g in games if not g.won]
    chosen = (wins[:limit] + losses[:3])[:limit]
    lines: list[str] = []
    for g in chosen:
        outcome = f"WON turn {g.win_turn}" if g.won else "did not win"
        seq = "; ".join(g.actions[:20]) if g.actions else (g.log[:400] if g.log else "")
        lines.append(f"- {outcome}: {seq}" if seq else f"- {outcome}")
    return "\n".join(lines)


def _reflect_confidence(n_games: int, win_rate: float) -> float:
    """Grounded confidence: scales with sample size and win rate, capped 0.95."""
    return round(min(0.95, win_rate * min(1.0, n_games / 20.0)), 2)


@app.post("/selfplay/reflect", response_model=ReflectResponse)
async def selfplay_reflect(req: ReflectRequest) -> ReflectResponse:
    """Distill a batch of self-play games into context-tagged lessons.

    One LLM call per batch (not per game) — aggregate stats plus compact
    per-game summaries go in, a short list of conditional lessons comes out,
    each carrying batch-level evidence (sample size + turns-to-win headroom).
    """
    import statistics

    games = req.games or []
    n_games = len(games)
    win_turns = [g.win_turn for g in games if g.won and g.win_turn is not None]
    n_wins = len(win_turns)
    win_rate = (n_wins / n_games) if n_games else 0.0
    fastest = min(win_turns) if win_turns else None
    mean_turn = round(statistics.fmean(win_turns), 2) if win_turns else None
    median_turn = round(statistics.median(win_turns), 2) if win_turns else None
    # Headroom between the average and the best line — how much faster the deck
    # *can* win than it typically does. This is the lesson's evidence delta.
    delta = round((mean_turn - fastest), 2) if (mean_turn is not None and fastest is not None) else 0.0

    log.info(
        "selfplay/reflect: archetype=%s context=%s games=%d wins=%d fastest=%s",
        req.archetype, req.context, n_games, n_wins, fastest,
    )

    lessons: list[Lesson] = []
    if n_games:
        prompt = (
            f"Deck: {req.archetype} ({req.format})\n"
            f"Pilot mode: {req.pilot_mode}  Context: {req.context}\n"
            f"Games: {n_games}  Wins: {n_wins}  Win rate: {win_rate:.0%}\n"
            f"Win turns — fastest: {fastest}, mean: {mean_turn}, median: {median_turn}\n\n"
            f"PER-GAME SUMMARIES (fastest wins first):\n{_summarize_games(games)}\n\n"
            f"Distill at most {req.max_lessons} conditional lessons that would make "
            "this deck win faster. Return exactly:\n"
            '  "lessons": [{"trigger": string, "recommendation": string}]'
        )
        try:
            raw = await generate_json(prompt, system=_REFLECT_SYSTEM_PROMPT)
        except LLMError as exc:
            log.warning("selfplay/reflect: LLM call failed (%s); returning aggregates only", exc)
            raw = {}
        confidence = _reflect_confidence(n_games, win_rate)
        now = datetime.now(timezone.utc).isoformat()
        for item in (raw.get("lessons") or [])[: req.max_lessons]:
            if not isinstance(item, dict):
                continue
            rec = str(item.get("recommendation") or "").strip()
            if not rec:
                continue
            lessons.append(
                Lesson(
                    archetype=req.archetype,
                    format=req.format,
                    context=req.context,
                    trigger=str(item.get("trigger") or "").strip(),
                    recommendation=rec,
                    evidence=LessonEvidence(turns_to_win_delta=delta, n_games=n_games),
                    confidence=confidence,
                    created_at=now,
                    source="selfplay",
                )
            )

    staged_path: str | None = None
    if req.stage and lessons:
        staged_path = str(learnings.append_lessons(req.format, req.archetype, lessons))

    return ReflectResponse(
        archetype=req.archetype,
        format=req.format,
        context=req.context,
        n_games=n_games,
        n_wins=n_wins,
        win_rate=round(win_rate, 3),
        fastest_win=fastest,
        mean_win_turn=mean_turn,
        median_win_turn=median_turn,
        lessons=lessons,
        staged_path=staged_path,
    )


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host=CONFIG.host, port=CONFIG.port)
