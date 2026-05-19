"""Offline LLM client for the piloting-guide builder.

Separate from :mod:`app.llm_client` (which serves requests at runtime) because
guide generation runs *offline* — in CI or on demand — and may want a stronger
model than the local llama.cpp server. It is a synchronous client and is never
imported on the request path.

Configured by ``BUILDER_LLM_BASE_URL`` / ``BUILDER_LLM_API_KEY`` /
``BUILDER_MODEL_NAME``; each falls back to the runtime ``LLM_*`` config so the
default behaviour is "use the same local model".
"""

from __future__ import annotations

import json
import logging
import os

import httpx

from app.config import CONFIG

log = logging.getLogger(__name__)

BASE_URL = os.environ.get("BUILDER_LLM_BASE_URL", CONFIG.llm_base_url)
API_KEY = os.environ.get("BUILDER_LLM_API_KEY", CONFIG.llm_api_key)
MODEL_NAME = os.environ.get("BUILDER_MODEL_NAME", CONFIG.model_name)
TIMEOUT = float(os.environ.get("BUILDER_LLM_TIMEOUT", "180"))


class BuilderLLMError(RuntimeError):
    """Raised when the builder LLM cannot be reached or returns garbage."""


def generate_guide_json(prompt: str, *, system: str | None = None) -> dict:
    """Call the chat-completions endpoint in JSON mode and parse the result."""
    messages: list[dict] = []
    if system:
        messages.append({"role": "system", "content": system})
    messages.append({"role": "user", "content": prompt})

    payload = {
        "model": MODEL_NAME,
        "messages": messages,
        "response_format": {"type": "json_object"},
        "temperature": 0.3,
        "stream": False,
    }
    headers = {"Authorization": f"Bearer {API_KEY}"}
    url = BASE_URL.rstrip("/") + "/chat/completions"

    try:
        resp = httpx.post(url, json=payload, headers=headers, timeout=TIMEOUT)
        resp.raise_for_status()
        body = resp.json()
    except (httpx.HTTPError, ValueError) as exc:
        raise BuilderLLMError(f"builder LLM request failed: {exc}") from exc

    try:
        content = body["choices"][0]["message"]["content"]
    except (KeyError, IndexError, TypeError) as exc:
        raise BuilderLLMError(f"unexpected builder LLM response: {body!r}") from exc

    try:
        return json.loads(content)
    except (json.JSONDecodeError, TypeError) as exc:
        raise BuilderLLMError(f"builder LLM returned non-JSON: {content!r}") from exc
