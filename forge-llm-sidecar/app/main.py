"""FastAPI entrypoint for the Forge LLM sidecar."""
from __future__ import annotations

import logging

from fastapi import FastAPI

from app.config import CONFIG
from app.graph import get_graph
from app.knowledge import metagame
from app.ollama_client import is_reachable
from app.schema import RecognitionRequest, RecognitionResponse

logging.basicConfig(level=logging.INFO)
log = logging.getLogger("forge-llm-sidecar")

app = FastAPI(title="Forge LLM Sidecar", version="0.1.0")


@app.on_event("startup")
async def _warm_graph() -> None:
    # Compile the graph eagerly so the first /recognize call is not slow.
    get_graph()
    log.info("Graph compiled. Model=%s Ollama=%s", CONFIG.model_name, CONFIG.ollama_url)


@app.get("/health")
async def health() -> dict:
    """Lightweight availability check used by Forge for a fail-soft gate."""
    return {
        "status": "ok",
        "model": CONFIG.model_name,
        "ollama_reachable": await is_reachable(),
        "metagame_enabled": CONFIG.metagame_enable,
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


@app.post("/recognize", response_model=RecognitionResponse)
async def recognize(req: RecognitionRequest) -> RecognitionResponse:
    """Run the deck-recognition graph for one opponent."""
    graph = get_graph()
    initial = {
        "game_id": req.game_id,
        "format": req.format,
        "turn": req.turn,
        "observations": [o.model_dump() for o in req.observations],
        "deck_cards": req.deck_cards,
        "alternatives": [],
    }
    final = await graph.ainvoke(initial)
    return RecognitionResponse(
        archetype=final.get("archetype") or "Unknown",
        confidence=final.get("confidence") or 0.0,
        reasoning=final.get("reasoning") or "",
        alternatives=final.get("alternatives") or [],
    )


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="127.0.0.1", port=CONFIG.port)
