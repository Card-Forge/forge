"""FastAPI entrypoint for the Forge LLM sidecar."""

from __future__ import annotations

import hashlib
import json as _json
import logging
from collections import OrderedDict
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel, Field

from app.config import CONFIG
from app.graph import get_graph
from app.knowledge import metagame, piloting
from app.llm_client import is_reachable
from app.nodes import game_advisor
from app.schema import (
    ActionScore,
    HandValuation,
    OpponentHandGuess,
    PilotingAdvice,
    RecognitionRequest,
    RecognitionResponse,
    RoleAssessment,
    TargetPriority,
    TrainingExample,
)
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
    if path not in ("/api/stats", "/dashboard") and not path.startswith("/static"):
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
#   game_id, turn, observations, hand, own/opp board, own/opp graveyard,
#   life totals, phase, available mana, opp mana colors, board details.
#
# Excluded (don't influence the recognizer / advisor in a meaningful way):
#   library sizes, personality (handled client-side post-hoc), ai_hand_size
#   (already in `hand`).
_RECOGNIZE_CACHE: "OrderedDict[str, dict]" = OrderedDict()
_RECOGNIZE_CACHE_LIMIT = 256
_recognize_cache_hits = 0
_recognize_cache_misses = 0


def _recognize_cache_key(req: RecognitionRequest) -> str:
    payload = {
        "g": req.game_id,
        "t": req.turn,
        "o": [
            (o.turn, o.event, o.card, o.cmc, list(o.colors), list(o.types))
            for o in req.observations
        ],
        "h": sorted(req.hand),
        "ob": sorted(req.own_board),
        "pb": sorted(req.opponent_board),
        "og": sorted(req.your_graveyard),
        "pg": sorted(req.opponent_graveyard),
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
        "format": req.format,
        "turn": req.turn,
        "observations": [o.model_dump() for o in req.observations],
        "deck_cards": req.deck_cards,
        "hand": req.hand,
        "own_board": req.own_board,
        "opponent_board": req.opponent_board,
        "your_graveyard": req.your_graveyard,
        "opponent_graveyard": req.opponent_graveyard,
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
    }
    final = await graph.ainvoke(initial)
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
    target_priorities = [
        TargetPriority(**t)
        for t in (final.get("target_priorities") or [])
        if isinstance(t, dict)
    ]
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
        }
    )

    response = RecognitionResponse(
        archetype=final.get("archetype") or "Unknown",
        confidence=final.get("confidence") or 0.0,
        reasoning=final.get("reasoning") or "",
        alternatives=final.get("alternatives") or [],
        piloting=piloting_advice,
    )
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
        piloting_advice = PilotingAdvice(
            own_archetype=final.get("own_archetype") or "Unknown",
            guide_source=final.get("guide_source") or "",
            recommended_play=final.get("recommended_play") or "",
            reasoning=final.get("play_reasoning") or "",
            alternatives=final.get("play_alternatives") or [],
            mulligan_advice=final.get("mulligan_advice") or "",
            actions=actions,
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


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host=CONFIG.host, port=CONFIG.port)
