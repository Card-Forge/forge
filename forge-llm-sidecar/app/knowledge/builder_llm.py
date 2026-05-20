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


_EXTRACT_SYSTEM = (
    "You are extracting structured piloting guidance from a Magic: The "
    "Gathering primer article. Ground every field in the supplied text. For "
    "each text-bearing field, mark it 'explicit' if the article directly "
    "states it and 'inferred' if you derived it from context. Never invent "
    "cards: every card name must appear verbatim in the article text or be a "
    "well-known staple for the format. Respond with a single JSON object and "
    "nothing else."
)


def _extract_prompt(
    archetype: str,
    fmt: str,
    publisher: str,
    source_url: str,
    cleaned_text: str,
) -> str:
    return (
        f"Archetype: {archetype}\n"
        f"Format: {fmt}\n"
        f"Source: {publisher} ({source_url})\n\n"
        "Primer article text:\n"
        "---\n"
        f"{cleaned_text}\n"
        "---\n\n"
        "Return a single JSON object with these keys (omit a key only if the "
        "article has zero relevant information for it):\n"
        '  "strategy_type": one of "aggro","tempo","midrange","control","combo","ramp",\n'
        '  "overview": short string (2-4 sentences),\n'
        '  "overview_evidence": {"confidence": 0-1, "kind": "explicit"|"inferred", "evidence_span": short quote},\n'
        '  "win_conditions": [string],\n'
        '  "win_conditions_evidence": [{"confidence","kind","evidence_span"}],\n'
        '  "mulligan": {"keep_criteria":[string], "mulligan_criteria":[string], '
        '"examples":[{"hand":[string],"decision":"keep"|"mulligan","reason":string}]},\n'
        '  "game_plan": {"early_game":[string], "mid_game":[string], "late_game":[string]},\n'
        '  "key_cards": [{"name":string, "role":string, "notes":string}],\n'
        '  "sequencing_tips": [string],\n'
        '  "sequencing_tips_evidence": [{"confidence","kind","evidence_span"}],\n'
        '  "matchups": [{"opponent_archetype":string, "advice":string, "watch_for":[string]}],\n'
        '  "common_threats": [string],\n'
        '  "common_threats_evidence": [{"confidence","kind","evidence_span"}].\n'
        "Mark `kind` as 'explicit' for facts you can quote and 'inferred' "
        "otherwise; confidence should reflect that distinction "
        "(0.8-1.0 explicit, 0.3-0.7 inferred)."
    )


def extract_primer_fields(
    cleaned_text: str,
    *,
    archetype: str,
    fmt: str,
    publisher: str = "",
    source_url: str = "",
) -> dict:
    """Ask the builder LLM to extract structured fields from a cleaned primer.

    Returns a JSON dict matching the extended ``PilotingGuide`` v2 schema (the
    caller fills in provenance, metadata, decklist_hash). Raises
    :class:`BuilderLLMError` on transport, decode, or non-JSON failures.
    """
    from app.knowledge.primers.clean_html import truncate_for_prompt

    prompt = _extract_prompt(
        archetype, fmt, publisher, source_url, truncate_for_prompt(cleaned_text)
    )
    return generate_guide_json(prompt, system=_EXTRACT_SYSTEM)
