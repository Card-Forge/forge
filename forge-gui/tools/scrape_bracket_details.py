#!/usr/bin/env python3
"""Scrape Commander bracket detail lists.

Outputs four headerless text files:
- commander-bracket-combos.txt: category | card_1 | card_2
- gamechangers.txt: one card name per line
- mass-land-denial.txt: one card name per line
- extra-turns.txt: one card name per line
"""

from __future__ import annotations

import argparse
import html.parser
import json
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Any, Iterable


EDHREC_BASE_URL = "https://edhrec.com"
EDHREC_JSON_BASE_URL = "https://json-cloudflare.edhrec.com/pages"
SCRYFALL_SEARCH_API = "https://api.scryfall.com/cards/search"

COMBO_SOURCES = {
    "late_game": "https://edhrec.com/combos/late-game-2-card-combos",
    "early_game": "https://edhrec.com/combos/early-game-2-card-combos",
}
SCRYFALL_LISTS = {
    "gamechangers.txt": "is:gamechanger",
    "mass-land-denial.txt": "otag:mass-land-denial",
    "extra-turns.txt": "oracletag:extra-turn",
}


class NextDataParser(html.parser.HTMLParser):
    """Extract the JSON payload from Next.js' __NEXT_DATA__ script tag."""

    def __init__(self) -> None:
        super().__init__()
        self._in_next_data = False
        self._chunks: list[str] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        attrs_by_name = dict(attrs)
        if tag == "script" and attrs_by_name.get("id") == "__NEXT_DATA__":
            self._in_next_data = True

    def handle_endtag(self, tag: str) -> None:
        if tag == "script" and self._in_next_data:
            self._in_next_data = False

    def handle_data(self, data: str) -> None:
        if self._in_next_data:
            self._chunks.append(data)

    @property
    def data(self) -> str:
        return "".join(self._chunks).strip()


def fetch_text(url: str, accept: str, timeout: int = 30) -> str:
    request = urllib.request.Request(
        url,
        headers={
            "Accept": accept,
            "User-Agent": "Mozilla/5.0 commander-bracket-scraper/1.0",
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return response.read().decode("utf-8")
    except urllib.error.HTTPError as exc:
        body = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"HTTP {exc.code} while fetching {url}: {body[:250]}") from exc
    except urllib.error.URLError as exc:
        raise RuntimeError(f"Could not fetch {url}: {exc.reason}") from exc


def fetch_json(url: str) -> dict[str, Any]:
    return json.loads(fetch_text(url, accept="application/json"))


def parse_initial_payload(html: str) -> dict[str, Any]:
    parser = NextDataParser()
    parser.feed(html)
    if not parser.data:
        raise RuntimeError("Could not find __NEXT_DATA__ JSON in the EDHREC page.")
    return json.loads(parser.data)


def walk_json(value: Any) -> Iterable[Any]:
    yield value
    if isinstance(value, dict):
        for child in value.values():
            yield from walk_json(child)
    elif isinstance(value, list):
        for child in value:
            yield from walk_json(child)


def find_combo_entries(payload: Any) -> list[dict[str, Any]]:
    entries: list[dict[str, Any]] = []
    for value in walk_json(payload):
        if isinstance(value, dict) and {"cardviews", "combo", "href"}.issubset(value):
            entries.append(value)
    return entries


def find_more_path(payload: Any) -> str | None:
    for value in walk_json(payload):
        if isinstance(value, dict) and isinstance(value.get("more"), str):
            return value["more"]
    return None


def edhrec_more_url(path: str) -> str:
    return urllib.parse.urljoin(f"{EDHREC_JSON_BASE_URL}/", path)


def combo_row(category: str, entry: dict[str, Any]) -> tuple[str, str, str]:
    card_names = [
        card.get("name", "").strip()
        for card in entry.get("cardviews") or []
        if isinstance(card, dict) and card.get("name")
    ]
    card_1 = card_names[0] if len(card_names) > 0 else ""
    card_2 = card_names[1] if len(card_names) > 1 else ""
    return category, card_1, card_2


def scrape_combo_source(
    category: str, source_url: str, delay_seconds: float
) -> list[tuple[str, str, str]]:
    print(f"Fetching {category}: {source_url}", file=sys.stderr)
    payload = parse_initial_payload(
        fetch_text(source_url, accept="text/html,application/json")
    )
    rows = [combo_row(category, entry) for entry in find_combo_entries(payload)]
    seen_more_paths: set[str] = set()
    next_path = find_more_path(payload)

    while next_path:
        if next_path in seen_more_paths:
            raise RuntimeError(f"Pagination loop detected at {next_path}")
        seen_more_paths.add(next_path)
        if delay_seconds > 0:
            time.sleep(delay_seconds)

        page_url = edhrec_more_url(next_path)
        print(f"Fetching {category}: {page_url}", file=sys.stderr)
        payload = fetch_json(page_url)
        rows.extend(combo_row(category, entry) for entry in find_combo_entries(payload))
        next_path = find_more_path(payload)

    return rows


def scrape_combos(delay_seconds: float) -> list[tuple[str, str, str]]:
    rows: list[tuple[str, str, str]] = []
    for category, source_url in COMBO_SOURCES.items():
        rows.extend(scrape_combo_source(category, source_url, delay_seconds))
    return rows


def scryfall_search_url(query: str) -> str:
    return f"{SCRYFALL_SEARCH_API}?{urllib.parse.urlencode({'q': query, 'unique': 'cards'})}"


def scrape_scryfall_cards(query: str, delay_seconds: float) -> list[str]:
    names: list[str] = []
    seen_names: set[str] = set()
    next_url: str | None = scryfall_search_url(query)

    while next_url:
        print(f"Fetching: {next_url}", file=sys.stderr)
        payload = fetch_json(next_url)
        for card in payload.get("data", []):
            name = card.get("name", "").strip()
            if name and name not in seen_names:
                names.append(name)
                seen_names.add(name)

        next_url = payload.get("next_page") if payload.get("has_more") else None
        if next_url and delay_seconds > 0:
            time.sleep(delay_seconds)

    return names


def write_combo_file(path: Path, rows: list[tuple[str, str, str]]) -> None:
    with path.open("w", encoding="utf-8", newline="") as file:
        for category, card_1, card_2 in rows:
            file.write(f"{category} | {card_1} | {card_2}\n")


def write_card_file(path: Path, card_names: list[str]) -> None:
    with path.open("w", encoding="utf-8", newline="") as file:
        for name in card_names:
            file.write(f"{name}\n")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Scrape Commander bracket combo and card detail lists."
    )
    parser.add_argument(
        "-o",
        "--output-dir",
        default=".",
        help="Directory for output text files. Default: current directory.",
    )
    parser.add_argument(
        "--delay",
        type=float,
        default=0.5,
        help="Seconds to wait between paginated requests. Default: 0.5",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    combo_rows = scrape_combos(args.delay)
    combo_path = output_dir / "commander-bracket-combos.txt"
    write_combo_file(combo_path, combo_rows)
    print(f"Wrote {len(combo_rows)} combos to {combo_path}")

    for filename, query in SCRYFALL_LISTS.items():
        card_names = scrape_scryfall_cards(query, args.delay)
        output_path = output_dir / filename
        write_card_file(output_path, card_names)
        print(f"Wrote {len(card_names)} cards to {output_path}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
