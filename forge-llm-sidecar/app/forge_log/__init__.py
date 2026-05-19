"""Forge game log adapter.

Parses Forge game logs, tracks game state, and produces `RecognitionRequest`
checkpoints that can be sent to the sidecar for analysis.  Supports both
post-game replay and live log tailing.
"""

from __future__ import annotations

import asyncio
import logging
from collections.abc import AsyncIterator
from pathlib import Path

import aiohttp

from app.forge_log.events import Event
from app.forge_log.parser import parse_line, parse_lines
from app.forge_log.state import GameSessionState
from app.schema import RecognitionRequest, RecognitionResponse

log = logging.getLogger(__name__)


class ForgeLogAdapter:
    """Parses Forge game logs and produces analysis checkpoints.

    Usage (offline):
        adapter = ForgeLogAdapter(game_id="my-game")
        adapter.set_opponent("Atlin")
        adapter.set_ai_player("Rogist")
        checkpoints = adapter.parse(log_text)

    Usage (live tailing):
        async for req in adapter.tail("/path/to/log.txt"):
            response = await sidecar_post(req)
    """

    def __init__(
        self,
        game_id: str = "log-session",
        format: str = "Constructed",
    ):
        self.game_id = game_id
        self.format = format
        self._state = GameSessionState(
            game_id=game_id,
            format=format,
        )

    @property
    def state(self) -> GameSessionState:
        return self._state

    def set_opponent(self, name: str) -> None:
        """Explicitly set the opponent player name."""
        self._state.opponent_name = name

    def set_ai_player(self, name: str) -> None:
        """Explicitly set the AI-controlled player name."""
        self._state.ai_name = name

    def parse(self, text: str) -> list[RecognitionRequest]:
        """Parse a full game log and return all checkpoints.

        Returns a list of `RecognitionRequest` objects, one per checkpoint
        (opponent action or turn boundary).

        Forge logs are reverse-chronological (newest first).  This method
        reverses the lines before parsing so events are processed in
        chronological order.
        """
        lines = text.splitlines()
        # Forge logs are newest-first; reverse for chronological processing
        lines = list(reversed([line for line in lines if line.strip()]))
        events = parse_lines(lines)
        checkpoints: list[RecognitionRequest] = []

        for event in events:
            new = self._state.process(event)
            checkpoints.extend(new)

        return checkpoints

    def parse_events(self, text: str) -> list[Event]:
        """Parse a full game log and return all raw events (no state tracking)."""
        lines = text.splitlines()
        return parse_lines(lines)

    async def tail(
        self,
        path: str | Path,
        sidecar_url: str | None = None,
    ) -> AsyncIterator[RecognitionRequest | tuple[RecognitionRequest, RecognitionResponse | None]]:
        """Live-tail a log file, yielding checkpoints as they appear.

        If ``sidecar_url`` is provided, sends each checkpoint to the sidecar
        and yields (request, response) tuples.  Otherwise yields raw requests.

        Use as:
            async for item in adapter.tail("/path/to/log.txt", "http://localhost:8000"):
                if isinstance(item, tuple):
                    req, resp = item
                else:
                    req = item
        """
        path = Path(path)
        if not path.exists():
            log.error("Tail: file not found: %s", path)
            return

        # Start reading from end of file
        pos = path.stat().st_size if path.exists() else 0
        buffer = ""

        while True:
            try:
                current_size = path.stat().st_size
                if current_size < pos:
                    # File was truncated (new game), reset
                    pos = 0
                    buffer = ""
                    self._state = GameSessionState(
                        game_id=self.game_id,
                        format=self.format,
                    )

                if current_size > pos:
                    with open(path, encoding="utf-8", errors="replace") as f:
                        if pos == 0:
                            f.seek(current_size)
                        else:
                            f.seek(pos)
                        new_data = f.read()
                        pos = f.tell()

                    buffer += new_data
                    while "\n" in buffer:
                        line, buffer = buffer.split("\n", 1)
                        if line.strip():
                            event = parse_line(line)
                            if event:
                                new = self._state.process(event)
                                for req in new:
                                    if sidecar_url:
                                        resp = await _post_recognize(req, sidecar_url)
                                        yield (req, resp)
                                    else:
                                        yield req

                await asyncio.sleep(0.2)
            except FileNotFoundError:
                await asyncio.sleep(1)
            except Exception as exc:
                log.warning("Tail error: %s", exc)
                await asyncio.sleep(1)


async def _post_recognize(
    req: RecognitionRequest,
    base_url: str,
    timeout: float = 30.0,
) -> RecognitionResponse | None:
    """POST a RecognitionRequest to the sidecar's /recognize endpoint."""
    url = f"{base_url.rstrip('/')}/recognize"
    try:
        async with (
            aiohttp.ClientSession() as session,
            session.post(
                url,
                json=req.model_dump(),
                timeout=aiohttp.ClientTimeout(total=timeout),
            ) as resp,
        ):
            if resp.status == 200:
                data = await resp.json()
                return RecognitionResponse(**data)
            log.warning("Sidecar returned status %d", resp.status)
    except Exception as exc:
        log.warning("Sidecar call failed: %s", exc)
    return None


async def analyze_log(
    text: str,
    sidecar_url: str,
    game_id: str = "log-session",
    format: str = "Constructed",
    opponent: str | None = None,
    ai_player: str | None = None,
    deck_cards: list[str] | None = None,
) -> list[dict]:
    """Full analysis pipeline: parse log, send all checkpoints to sidecar.

    Returns a list of dicts with turn, request, and response for each checkpoint.
    """
    adapter = ForgeLogAdapter(game_id=game_id, format=format)
    if opponent:
        adapter.set_opponent(opponent)
    if ai_player:
        adapter.set_ai_player(ai_player)

    checkpoints = adapter.parse(text)

    results = []
    for i, req in enumerate(checkpoints):
        if deck_cards:
            req.deck_cards = deck_cards
        resp = await _post_recognize(req, sidecar_url)
        results.append(
            {
                "index": i,
                "turn": req.turn,
                "request": req.model_dump(),
                "response": resp.model_dump() if resp else None,
            }
        )
    return results
