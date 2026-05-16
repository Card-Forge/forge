"""Environment-driven configuration for the sidecar."""
from __future__ import annotations

import os
from dataclasses import dataclass


def _env_bool(name: str, default: bool) -> bool:
    return os.environ.get(name, str(default)).strip().lower() in ("1", "true", "yes", "on")


@dataclass(frozen=True)
class Config:
    ollama_url: str
    model_name: str
    port: int
    request_timeout: float
    # Metagame knowledge (pre-scraped JSON, refreshed by a GitHub Action)
    metagame_enable: bool
    default_meta_format: str
    # Scryfall-based format detection
    format_detect_enable: bool

    @staticmethod
    def from_env() -> "Config":
        return Config(
            ollama_url=os.environ.get("OLLAMA_URL", "http://localhost:11434"),
            model_name=os.environ.get("MODEL_NAME", "llama3.1:8b"),
            port=int(os.environ.get("PORT", "8000")),
            request_timeout=float(os.environ.get("OLLAMA_TIMEOUT", "60")),
            metagame_enable=_env_bool("METAGAME_ENABLE", True),
            default_meta_format=os.environ.get("DEFAULT_META_FORMAT", "standard"),
            format_detect_enable=_env_bool("FORMAT_DETECT_ENABLE", True),
        )


CONFIG = Config.from_env()
