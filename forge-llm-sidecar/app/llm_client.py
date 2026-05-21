"""Client for an OpenAI-compatible LLM server.

Targets a local [llama.cpp](https://github.com/ggml-org/llama.cpp) server
(``llama-server``), which exposes the OpenAI ``/v1/chat/completions`` API. Any
other OpenAI-compatible endpoint works too — set ``LLM_BASE_URL`` accordingly.
"""

from __future__ import annotations

import json
import logging

import httpx

from app.config import CONFIG

log = logging.getLogger(__name__)


class LLMError(RuntimeError):
    """Raised when the LLM server cannot be reached or returns garbage."""


def _chat_url() -> str:
    return CONFIG.llm_base_url.rstrip("/") + "/chat/completions"


def _models_url() -> str:
    return CONFIG.llm_base_url.rstrip("/") + "/models"


async def generate_json(
    prompt: str,
    *,
    system: str | None = None,
    model: str | None = None,
    temperature: float = 0.2,
) -> dict:
    """Call the chat-completions endpoint in JSON mode and parse the result.

    ``response_format={"type": "json_object"}`` constrains the model to emit a
    single JSON object. Any transport or parse failure is surfaced as
    :class:`LLMError` so callers can degrade gracefully.

    ``model`` / ``temperature`` let a caller override the defaults — e.g. the
    opponent_strategist node can point at a faster model for its longer prompt.
    """
    messages: list[dict] = []
    if system:
        messages.append({"role": "system", "content": system})
    messages.append({"role": "user", "content": prompt})

    payload: dict = {
        "model": model or CONFIG.model_name,
        "messages": messages,
        "response_format": {"type": "json_object"},
        "temperature": temperature,
        "stream": False,
    }
    if CONFIG.llm_disable_thinking:
        # Reasoning models (e.g. Qwen3) emit a long <think> block before the
        # answer — ~20x slower for no quality gain on JSON classification.
        # `enable_thinking` is the chat-template kwarg llama.cpp honors with
        # `--jinja`; backends without it simply ignore the extra kwarg.
        payload["chat_template_kwargs"] = {"enable_thinking": False}
    headers = {"Authorization": f"Bearer {CONFIG.llm_api_key}"}

    try:
        async with httpx.AsyncClient(timeout=CONFIG.request_timeout) as client:
            resp = await client.post(_chat_url(), json=payload, headers=headers)
            resp.raise_for_status()
            body = resp.json()
    except (httpx.HTTPError, ValueError) as exc:
        raise LLMError(f"LLM request failed: {exc}") from exc

    try:
        content = body["choices"][0]["message"]["content"]
    except (KeyError, IndexError, TypeError) as exc:
        raise LLMError(f"Unexpected LLM response shape: {body!r}") from exc

    try:
        return json.loads(content)
    except (json.JSONDecodeError, TypeError) as exc:
        raise LLMError(f"LLM returned non-JSON content: {content!r}") from exc


async def is_reachable() -> bool:
    """Best-effort check that the LLM server is up (``GET /v1/models``)."""
    try:
        async with httpx.AsyncClient(timeout=2.0) as client:
            resp = await client.get(
                _models_url(),
                headers={"Authorization": f"Bearer {CONFIG.llm_api_key}"},
            )
            return resp.status_code == 200
    except httpx.HTTPError:
        return False
