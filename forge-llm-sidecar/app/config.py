"""Environment-driven configuration for the sidecar."""

from __future__ import annotations

import os
from dataclasses import dataclass


def _env_bool(name: str, default: bool) -> bool:
    return os.environ.get(name, str(default)).strip().lower() in ("1", "true", "yes", "on")


@dataclass(frozen=True)
class Config:
    # LLM backend: any OpenAI-compatible server (default: a local llama.cpp server)
    llm_base_url: str
    llm_api_key: str
    model_name: str
    port: int
    request_timeout: float
    # Metagame knowledge (pre-scraped JSON, refreshed by a GitHub Action)
    metagame_enable: bool
    default_meta_format: str
    # Scryfall-based format detection
    format_detect_enable: bool

    @staticmethod
    def from_env() -> Config:
        return Config(
            llm_base_url=os.environ.get("LLM_BASE_URL", "http://localhost:8080/v1"),
            llm_api_key=os.environ.get("LLM_API_KEY", "not-needed"),
            model_name=os.environ.get("MODEL_NAME", "local-model"),
            port=int(os.environ.get("PORT", "8000")),
            request_timeout=float(os.environ.get("LLM_TIMEOUT", "60")),
            metagame_enable=_env_bool("METAGAME_ENABLE", True),
            default_meta_format=os.environ.get("DEFAULT_META_FORMAT", "standard"),
            format_detect_enable=_env_bool("FORMAT_DETECT_ENABLE", True),
        )


CONFIG = Config.from_env()
