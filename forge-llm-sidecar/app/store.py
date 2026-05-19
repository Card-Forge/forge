"""In-memory store for request tracking and dashboard stats."""

from __future__ import annotations

import time
import threading
from typing import Any

_MAX_HISTORY = 20


class RequestStore:
    """Thread-safe in-memory store for /recognize request history and stats."""

    def __init__(self) -> None:
        self._lock = threading.Lock()
        self._start_time = time.time()
        self._total_requests: int = 0
        self._history: list[dict[str, Any]] = []

    @property
    def uptime_seconds(self) -> float:
        return time.time() - self._start_time

    def record(self, entry: dict[str, Any]) -> None:
        """Record a recognition request result. Keeps last ``_MAX_HISTORY`` entries."""
        with self._lock:
            self._total_requests += 1
            self._history.append({
                "timestamp": time.time(),
                **entry,
            })
            if len(self._history) > _MAX_HISTORY:
                self._history.pop(0)

    @property
    def total_requests(self) -> int:
        with self._lock:
            return self._total_requests

    @property
    def history(self) -> list[dict[str, Any]]:
        with self._lock:
            return list(self._history)

    @property
    def last_entry(self) -> dict[str, Any] | None:
        with self._lock:
            return self._history[-1] if self._history else None


_store = RequestStore()


def get_store() -> RequestStore:
    """Return the global request store instance."""
    return _store
