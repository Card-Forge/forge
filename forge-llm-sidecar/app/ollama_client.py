"""Thin async wrapper around a local Ollama server."""
from __future__ import annotations

import json
import logging

import httpx

from app.config import CONFIG

log = logging.getLogger(__name__)


class OllamaError(RuntimeError):
    """Raised when the local model cannot be reached or returns garbage."""


async def generate_json(prompt: str, *, system: str | None = None) -> dict:
    """Call Ollama's /api/generate with JSON-mode and return the parsed object.

    Ollama's ``format: "json"`` constrains the model to emit a single JSON
    object, which we then parse. Any transport or parse failure is surfaced as
    :class:`OllamaError` so callers can degrade gracefully.
    """
    payload: dict = {
        "model": CONFIG.model_name,
        "prompt": prompt,
        "format": "json",
        "stream": False,
        "options": {"temperature": 0.2},
    }
    if system:
        payload["system"] = system

    url = f"{CONFIG.ollama_url.rstrip('/')}/api/generate"
    try:
        async with httpx.AsyncClient(timeout=CONFIG.request_timeout) as client:
            resp = await client.post(url, json=payload)
            resp.raise_for_status()
            body = resp.json()
    except (httpx.HTTPError, ValueError) as exc:
        raise OllamaError(f"Ollama request failed: {exc}") from exc

    raw = body.get("response", "")
    try:
        return json.loads(raw)
    except (json.JSONDecodeError, TypeError) as exc:
        raise OllamaError(f"Ollama returned non-JSON content: {raw!r}") from exc


async def is_reachable() -> bool:
    """Best-effort check that the Ollama server is up."""
    try:
        async with httpx.AsyncClient(timeout=2.0) as client:
            resp = await client.get(f"{CONFIG.ollama_url.rstrip('/')}/api/tags")
            return resp.status_code == 200
    except httpx.HTTPError:
        return False
