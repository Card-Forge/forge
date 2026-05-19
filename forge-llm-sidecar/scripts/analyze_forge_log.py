#!/usr/bin/env python3
"""CLI tool for analyzing Forge game logs.

Modes:
  1. Parse only -- extract events and checkpoints from a log file (offline)
  2. Analyze -- send checkpoints to a running sidecar for LLM analysis
  3. Tail -- watch a live log file and stream analysis in real-time
  4. Training -- generate training data JSONL from analyzed logs

Usage:
  # Parse a log file (offline, no sidecar needed):
  python scripts/analyze_forge_log.py game.txt

  # Analyze with a running sidecar:
  python scripts/analyze_forge_log.py game.txt --sidecar http://localhost:8000

  # Generate training data:
  python scripts/analyze_forge_log.py game.txt \
      --sidecar http://localhost:8000 --training training.jsonl

  # Tail a live log file:
  python scripts/analyze_forge_log.py game.txt --tail --sidecar http://localhost:8000

  # Specify opponent/AI player names explicitly:
  python scripts/analyze_forge_log.py game.txt --opponent Atlin --ai-player Rogist
"""

from __future__ import annotations

import argparse
import asyncio
import json
import sys
from pathlib import Path

import aiohttp

# Ensure the project root is in the Python path (for `python scripts/...` usage)
_script_root = Path(__file__).resolve().parent.parent
if str(_script_root) not in sys.path:
    sys.path.insert(0, str(_script_root))

from app.forge_log import ForgeLogAdapter  # noqa: E402


def mode_parse(args: argparse.Namespace) -> None:
    """Offline parse mode: print extracted events and checkpoints."""
    log_text = Path(args.log).read_text(encoding="utf-8", errors="replace")
    adapter = ForgeLogAdapter(
        game_id=args.game_id,
        format=args.format,
    )
    if args.opponent:
        adapter.set_opponent(args.opponent)
    if args.ai_player:
        adapter.set_ai_player(args.ai_player)

    # Print events
    events = adapter.parse_events(log_text)
    print(f"# Parsed {len(events)} events from {len(log_text.splitlines())} lines", file=sys.stderr)

    if args.events:
        for ev in events:
            print(
                json.dumps(
                    {
                        "line": ev.line,
                        "kind": ev.kind,
                        "raw": ev.raw[:120],
                    }
                ),
                file=sys.stderr,
            )

    # Print checkpoints
    checkpoints = adapter.parse(log_text)
    print(f"# Generated {len(checkpoints)} checkpoints", file=sys.stderr)

    for i, cp in enumerate(checkpoints):
        cp.deck_cards = args.deck_cards
        output = {
            "index": i,
            "turn": cp.turn,
            "observations": [o.model_dump() for o in cp.observations],
            "life_totals": cp.life_totals,
            "opponent_board": cp.opponent_board,
            "own_board": cp.own_board,
            "opponent_graveyard": cp.opponent_graveyard,
            "your_graveyard": cp.your_graveyard,
        }
        if args.output == "jsonl":
            print(json.dumps(output))
        else:
            print(json.dumps(output, indent=2))


async def mode_analyze(args: argparse.Namespace) -> None:
    """Analyze mode: send all checkpoints to the sidecar."""
    log_text = Path(args.log).read_text(encoding="utf-8", errors="replace")
    adapter = ForgeLogAdapter(
        game_id=args.game_id,
        format=args.format,
    )
    if args.opponent:
        adapter.set_opponent(args.opponent)
    if args.ai_player:
        adapter.set_ai_player(args.ai_player)

    checkpoints = adapter.parse(log_text)
    print(f"# {len(checkpoints)} checkpoints to analyze", file=sys.stderr)

    results = []
    async with aiohttp.ClientSession() as session:
        for i, cp in enumerate(checkpoints):
            cp.deck_cards = args.deck_cards
            print(f"# [{i}] turn={cp.turn} obs={len(cp.observations)} ...", file=sys.stderr)

            resp = None
            try:
                async with session.post(
                    f"{args.sidecar.rstrip('/')}/recognize",
                    json=cp.model_dump(),
                    timeout=aiohttp.ClientTimeout(total=60),
                ) as r:
                    if r.status == 200:
                        resp = await r.json()
            except Exception as exc:
                print(f"#   ERROR: {exc}", file=sys.stderr)
                continue

            result = {
                "index": i,
                "turn": cp.turn,
                "archetype": resp.get("archetype") if resp else None,
                "confidence": resp.get("confidence") if resp else None,
                "reasoning": resp.get("reasoning") if resp else None,
                "piloting": resp.get("piloting") if resp else None,
            }
            results.append(result)

            if args.output == "jsonl":
                print(json.dumps(result))
            else:
                print(json.dumps(result, indent=2))

            # Small delay to not overload the LLM
            if args.delay:
                await asyncio.sleep(args.delay)

    # Training data output
    if args.training:
        training_data = []
        for i, cp in enumerate(checkpoints):
            if i < len(results) and results[i].get("archetype"):
                training_data.append(
                    {
                        "game_id": cp.game_id,
                        "turn": cp.turn,
                        "format": cp.format,
                        "observations": [o.model_dump() for o in cp.observations],
                        "deck_cards": cp.deck_cards,
                        "life_totals": cp.life_totals,
                        "archetype": results[i]["archetype"],
                        "confidence": results[i]["confidence"],
                        "reasoning": results[i]["reasoning"],
                    }
                )
        Path(args.training).write_text(
            "\n".join(json.dumps(d) for d in training_data) + "\n",
            encoding="utf-8",
        )
        print(
            f"# Training data written to {args.training}" f" ({len(training_data)} examples)",
            file=sys.stderr,
        )


async def mode_tail(args: argparse.Namespace) -> None:
    """Live tail mode: watch a log file and stream analysis."""
    adapter = ForgeLogAdapter(
        game_id=args.game_id,
        format=args.format,
    )
    if args.opponent:
        adapter.set_opponent(args.opponent)
    if args.ai_player:
        adapter.set_ai_player(args.ai_player)

    print(f"# Tailing {args.log} ...", file=sys.stderr)

    if args.sidecar:
        async for item in adapter.tail(args.log, args.sidecar):
            if isinstance(item, tuple):
                req, resp = item
                if resp:
                    output = {
                        "turn": req.turn,
                        "archetype": resp.archetype,
                        "confidence": resp.confidence,
                        "reasoning": resp.reasoning,
                        "piloting": resp.piloting.model_dump() if resp.piloting else None,
                    }
                else:
                    output = {"turn": req.turn, "error": "sidecar unavailable"}
            else:
                output = {
                    "turn": item.turn,
                    "observations": len(item.observations),
                    "note": "no sidecar, raw checkpoint",
                }
            print(json.dumps(output))
    else:
        async for req in adapter.tail(args.log):
            output = {
                "turn": req.turn,
                "observations": [o.model_dump() for o in req.observations],
                "life_totals": req.life_totals,
            }
            print(json.dumps(output))


def main():
    parser = argparse.ArgumentParser(
        description="Analyze Forge game logs with the LLM sidecar",
    )
    parser.add_argument("log", help="Path to the Forge game log file")
    parser.add_argument(
        "--sidecar", help="Sidecar URL for analysis mode (e.g. http://localhost:8000)"
    )
    parser.add_argument("--game-id", default="log-session", help="Game ID for the session")
    parser.add_argument("--format", default="Constructed", help="Game format")
    parser.add_argument("--opponent", default="", help="Opponent player name")
    parser.add_argument("--ai-player", default="", help="AI-controlled player name")
    parser.add_argument("--deck-cards", nargs="*", default=[], help="AI's deck card names")
    parser.add_argument("--output", choices=["json", "jsonl"], default="json", help="Output format")
    parser.add_argument("--events", action="store_true", help="Also print parsed events to stderr")
    parser.add_argument("--training", help="Output training data to this JSONL file")
    parser.add_argument(
        "--tail", action="store_true", help="Live-tail mode: watch file for new lines"
    )
    parser.add_argument(
        "--delay", type=float, default=0.0, help="Delay between sidecar calls (seconds)"
    )

    args = parser.parse_args()

    if args.tail:
        asyncio.run(mode_tail(args))
    elif args.sidecar:
        asyncio.run(mode_analyze(args))
    else:
        mode_parse(args)


if __name__ == "__main__":
    main()
