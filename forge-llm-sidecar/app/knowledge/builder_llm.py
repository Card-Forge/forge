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
TIMEOUT = float(os.environ.get("BUILDER_LLM_TIMEOUT", "600"))
# Reasoning-capable models (e.g. Qwen3 thinking variants) spend tokens on a
# hidden chain of thought; the structured guide answer also runs ~1-2k tokens.
# Give them plenty of headroom or the server returns empty content.
MAX_TOKENS = int(os.environ.get("BUILDER_LLM_MAX_TOKENS", "8192"))


class BuilderLLMError(RuntimeError):
    """Raised when the builder LLM cannot be reached or returns garbage."""


def generate_guide_json(prompt: str, *, system: str | None = None) -> dict:
    """Call the chat-completions endpoint in JSON mode and parse the result.

    Retries once on transient connection failures (llama-server occasionally
    crashes mid-generation under heavy load — systemd restarts it but the
    in-flight request fails).
    """
    import time

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
        "max_tokens": MAX_TOKENS,
    }
    headers = {"Authorization": f"Bearer {API_KEY}"}
    url = BASE_URL.rstrip("/") + "/chat/completions"

    last_exc: Exception | None = None
    for attempt in range(2):
        try:
            resp = httpx.post(url, json=payload, headers=headers, timeout=TIMEOUT)
            resp.raise_for_status()
            body = resp.json()
            break
        except (httpx.HTTPError, ValueError) as exc:
            last_exc = exc
            transient = isinstance(exc, (httpx.ConnectError, httpx.RemoteProtocolError, httpx.ReadError))
            if attempt == 0 and transient:
                log.warning("builder LLM transient failure (%s); retrying in 5s", exc)
                time.sleep(5)
                continue
            raise BuilderLLMError(f"builder LLM request failed: {exc}") from exc
    else:
        raise BuilderLLMError(f"builder LLM request failed: {last_exc}") from last_exc

    try:
        message = body["choices"][0]["message"]
    except (KeyError, IndexError, TypeError) as exc:
        raise BuilderLLMError(f"unexpected builder LLM response: {body!r}") from exc

    content = message.get("content") or ""
    # Reasoning models (Qwen3 thinking, deepseek-r1, etc.) sometimes emit the
    # final JSON inside reasoning_content when their visible content is empty.
    if not content.strip():
        content = message.get("reasoning_content") or ""

    parsed = _parse_json_loose(content)
    if parsed is None:
        finish = body["choices"][0].get("finish_reason", "?")
        raise BuilderLLMError(
            f"builder LLM returned non-JSON (finish={finish}): {content[:500]!r}"
        )
    return parsed


def _parse_json_loose(text: str) -> dict | None:
    """Parse JSON, peeling off code fences or surrounding prose if needed."""
    if not text:
        return None
    text = text.strip()
    # Try direct parse first.
    try:
        out = json.loads(text)
        return out if isinstance(out, dict) else None
    except (json.JSONDecodeError, TypeError):
        pass
    # Strip ```json fences.
    if text.startswith("```"):
        text = text.strip("`")
        if text.lower().startswith("json"):
            text = text[4:]
        text = text.strip()
        try:
            out = json.loads(text)
            return out if isinstance(out, dict) else None
        except (json.JSONDecodeError, TypeError):
            pass
    # Last resort: find the first {...} block.
    start = text.find("{")
    end = text.rfind("}")
    if start != -1 and end > start:
        try:
            out = json.loads(text[start : end + 1])
            return out if isinstance(out, dict) else None
        except (json.JSONDecodeError, TypeError):
            pass
    return None


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
        "Return a JSON object EXACTLY matching this skeleton — keep every "
        "field at the type shown. strategy_type must be lowercase.\n\n"
        "{\n"
        '  "strategy_type": "aggro"|"tempo"|"midrange"|"control"|"combo"|"ramp",\n'
        '  "overview": "2-4 sentence summary",\n'
        '  "overview_evidence": {"confidence": 0.8, "kind": "explicit", "evidence_span": "short quote"},\n'
        '  "win_conditions": ["..."],\n'
        '  "win_conditions_evidence": [{"confidence": 0.8, "kind": "explicit", "evidence_span": "..."}],\n'
        '  "mulligan": {\n'
        '    "keep_criteria": ["..."],\n'
        '    "mulligan_criteria": ["..."],\n'
        '    "examples": [{"hand": ["card","card","..."], "decision": "keep"|"mulligan", "reason": "..."}]\n'
        "  },\n"
        '  "game_plan": {"early_game": ["..."], "mid_game": ["..."], "late_game": ["..."]},\n'
        '  "key_cards": [{"name": "Card Name", "role": "...", "notes": "..."}],\n'
        '  "sequencing_tips": ["..."],\n'
        '  "sequencing_tips_evidence": [{"confidence": 0.7, "kind": "inferred", "evidence_span": ""}],\n'
        '  "matchups": [{"opponent_archetype": "name", "advice": "...", "watch_for": ["..."]}],\n'
        '  "common_threats": ["..."],\n'
        '  "common_threats_evidence": []\n'
        "}\n\n"
        "Every key_cards entry MUST be an object with name/role/notes. "
        "win_conditions, sequencing_tips, common_threats MUST be arrays of "
        "strings, NOT single strings. matchups MUST be a list of objects. "
        "mulligan and game_plan MUST be objects, NOT strings. Mark `kind` as "
        "'explicit' for facts you can quote from the article and 'inferred' "
        "otherwise (confidence 0.8-1.0 explicit, 0.3-0.7 inferred)."
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
