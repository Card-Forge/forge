"""FastAPI entrypoint for the Forge LLM sidecar."""

from __future__ import annotations

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel, Field

from app.config import CONFIG
from app.graph import get_graph
from app.knowledge import metagame, piloting
from app.llm_client import is_reachable
from app.schema import (
    PilotingAdvice,
    RecognitionRequest,
    RecognitionResponse,
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


@app.get("/api/stats")
async def api_stats() -> dict:
    """Dashboard stats: uptime, request counts, and recognition history."""
    store = get_store()
    return {
        "uptime_seconds": round(store.uptime_seconds, 1),
        "total_requests": store.total_requests,
        "history": store.history,
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


@app.post("/recognize", response_model=RecognitionResponse)
async def recognize(req: RecognitionRequest) -> RecognitionResponse:
    """Run the game-advisor graph: opponent recognition + own-deck piloting advice."""
    log.info("recognize: client=%s game=%s turn=%s", req.client, req.game_id, req.turn)
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
        "alternatives": [],
    }
    final = await graph.ainvoke(initial)
    piloting_advice = PilotingAdvice(
        own_archetype=final.get("own_archetype") or "Unknown",
        guide_source=final.get("guide_source") or "",
        recommended_play=final.get("recommended_play") or "",
        reasoning=final.get("play_reasoning") or "",
        alternatives=final.get("play_alternatives") or [],
        mulligan_advice=final.get("mulligan_advice") or "",
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
        }
    )

    return RecognitionResponse(
        archetype=final.get("archetype") or "Unknown",
        confidence=final.get("confidence") or 0.0,
        reasoning=final.get("reasoning") or "",
        alternatives=final.get("alternatives") or [],
        piloting=piloting_advice,
    )


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
            "alternatives": [],
        }
        final = await graph.ainvoke(initial)

        piloting_advice = PilotingAdvice(
            own_archetype=final.get("own_archetype") or "Unknown",
            guide_source=final.get("guide_source") or "",
            recommended_play=final.get("recommended_play") or "",
            reasoning=final.get("play_reasoning") or "",
            alternatives=final.get("play_alternatives") or [],
            mulligan_advice=final.get("mulligan_advice") or "",
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

    uvicorn.run(app, host="127.0.0.1", port=CONFIG.port)
