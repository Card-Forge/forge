#!/usr/bin/env python3
"""Pass 1: scrape raw EDHREC commander/card data into a TSV."""

from __future__ import annotations

import argparse
import csv
import html
import json
import re
import random
import sys
import time
import unicodedata
from dataclasses import dataclass
from html.parser import HTMLParser
from pathlib import Path
from typing import Iterable
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


BASE_URL = "https://edhrec.com"
SECTION_HEADINGS = {
    "New Cards",
    "High Synergy Cards",
    "Top Cards",
    "Game Changers",
    "Creatures",
    "Instants",
    "Sorceries",
    "Utility Artifacts",
    "Artifacts",
    "Enchantments",
    "Battles",
    "Planeswalkers",
    "Utility Lands",
    "Mana Artifacts",
    "Lands",
    "Back to Top",
}
LAST_REQUEST_AT = 0.0
SCRIPT_DIR = Path(__file__).resolve().parent
RETRY_STATUS_CODES = {429, 500, 502, 503, 504}
TSV_FIELDS = (
    "commander_rank",
    "commander",
    "commander_slug",
    "commander_decks",
    "card",
    "section",
    "included_decks",
    "eligible_decks",
    "inclusion_percent",
    "synergy_percent",
)


@dataclass(frozen=True)
class Commander:
    rank: int
    name: str
    slug: str
    decks: int | None = None


@dataclass(frozen=True)
class RawCardData:
    commander_rank: int
    commander: str
    commander_slug: str
    commander_decks: int | None
    card: str
    section: str | None
    included_decks: int
    eligible_decks: int | None
    inclusion: float | None
    synergy: int | None


class TextExtractor(HTMLParser):
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.parts: list[str] = []

    def handle_data(self, data: str) -> None:
        text = " ".join(html.unescape(data).split())
        if text:
            self.parts.append(text)

    def lines(self) -> list[str]:
        return self.parts


def wait_for_rate_limit(min_interval: float, jitter: float) -> None:
    global LAST_REQUEST_AT
    elapsed = time.monotonic() - LAST_REQUEST_AT
    target = min_interval + random.uniform(0, jitter)
    if elapsed < target:
        time.sleep(target - elapsed)


def fetch(url: str, cache_path: Path, refresh: bool, timeout: int, min_interval: float, jitter: float, retries: int) -> str:
    global LAST_REQUEST_AT
    if cache_path.exists() and not refresh:
        return cache_path.read_text(encoding="utf-8")

    cache_path.parent.mkdir(parents=True, exist_ok=True)
    request = Request(
        url,
        headers={
            "User-Agent": "CommanderData/0.1 (+local Forge deckgen data build)",
            "Accept": "text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8",
        },
    )
    for attempt in range(retries + 1):
        wait_for_rate_limit(min_interval, jitter)
        try:
            with urlopen(request, timeout=timeout) as response:
                LAST_REQUEST_AT = time.monotonic()
                body = response.read().decode(response.headers.get_content_charset() or "utf-8", errors="replace")
            cache_path.write_text(body, encoding="utf-8")
            return body
        except HTTPError as exc:
            LAST_REQUEST_AT = time.monotonic()
            retry_after = exc.headers.get("Retry-After")
            if exc.code in RETRY_STATUS_CODES and attempt < retries:
                sleep_for = parse_retry_after(retry_after) or min(120.0, min_interval * (2 ** (attempt + 1)))
                print(f"warning: {exc.code} for {url}; sleeping {sleep_for:.1f}s before retry", file=sys.stderr)
                time.sleep(sleep_for)
                continue
            raise
        except (URLError, TimeoutError):
            LAST_REQUEST_AT = time.monotonic()
            if attempt < retries:
                sleep_for = min(120.0, min_interval * (2 ** (attempt + 1)))
                print(f"warning: network error for {url}; sleeping {sleep_for:.1f}s before retry", file=sys.stderr)
                time.sleep(sleep_for)
                continue
            raise
    raise RuntimeError(f"unreachable fetch retry state for {url}")


def parse_retry_after(value: str | None) -> float | None:
    if not value:
        return None
    try:
        return max(0.0, float(value))
    except ValueError:
        return None


def parse_json_text(text: str) -> dict | None:
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        return None


def extract_next_data(page: str) -> dict | None:
    match = re.search(r'<script id="__NEXT_DATA__" type="application/json">(.*?)</script>', page, re.DOTALL)
    if not match:
        return None
    return parse_json_text(html.unescape(match.group(1)))


def iter_cardlists(value: object) -> Iterable[dict]:
    if isinstance(value, dict):
        cardlists = value.get("cardlists")
        if isinstance(cardlists, list):
            for cardlist in cardlists:
                if isinstance(cardlist, dict):
                    yield cardlist
        for child in value.values():
            yield from iter_cardlists(child)
    elif isinstance(value, list):
        for child in value:
            yield from iter_cardlists(child)


def commanders_from_cardviews(cardviews: object) -> list[Commander]:
    commanders: list[Commander] = []
    if not isinstance(cardviews, list):
        return commanders
    for card in cardviews:
        if not isinstance(card, dict):
            continue
        name = card.get("name")
        rank = card.get("rank")
        if not name or rank is None:
            continue
        slug = card.get("sanitized") or str(card.get("url", "")).removeprefix("/commanders/")
        if not slug:
            slug = slugify(name)
        commanders.append(
            Commander(
                rank=int(rank),
                name=name,
                slug=slug,
                decks=card.get("num_decks") or card.get("inclusion"),
            )
        )
    return commanders


def parse_commanders_from_json_payload(payload: dict, limit: int) -> tuple[list[Commander], str | None]:
    commanders: list[Commander] = []
    more = payload.get("more") if isinstance(payload.get("more"), str) else None

    commanders.extend(commanders_from_cardviews(payload.get("cardviews")))

    for cardlist in iter_cardlists(payload):
        parsed = commanders_from_cardviews(cardlist.get("cardviews"))
        if parsed:
            commanders.extend(parsed)
            if more is None and isinstance(cardlist.get("more"), str):
                more = cardlist["more"]

    deduped = {commander.rank: commander for commander in commanders}
    result = [deduped[rank] for rank in sorted(deduped)[:limit]]
    return result, more


def visible_lines(page: str) -> list[str]:
    parser = TextExtractor()
    parser.feed(page)
    return parser.lines()


def slugify(name: str) -> str:
    text = unicodedata.normalize("NFKD", name).encode("ascii", "ignore").decode("ascii")
    text = text.replace("//", " ")
    text = text.replace("&", " and ")
    text = re.sub(r"[^A-Za-z0-9]+", "-", text.lower())
    return text.strip("-")


def parse_deck_count(value: str) -> int:
    value = value.replace(",", "").strip()
    match = re.fullmatch(r"(\d+(?:\.\d+)?)([KkMm]?)", value)
    if not match:
        raise ValueError(value)
    number = float(match.group(1))
    suffix = match.group(2).lower()
    if suffix == "k":
        number *= 1_000
    elif suffix == "m":
        number *= 1_000_000
    return int(round(number))


def parse_commanders_from_html(page: str, limit: int) -> list[Commander]:
    next_data = extract_next_data(page)
    if next_data is not None:
        commanders, _more = parse_commanders_from_json_payload(next_data, limit)
        if commanders:
            return commanders

    lines = visible_lines(page)
    commanders: list[Commander] = []
    seen: set[int] = set()

    for i, line in enumerate(lines):
        match = re.fullmatch(r"Rank #(\d+)", line)
        if not match:
            continue
        rank = int(match.group(1))
        if rank in seen:
            continue
        name = previous_content_line(lines, i)
        if not name or name in SECTION_HEADINGS:
            continue
        decks = None
        if i + 1 < len(lines):
            deck_match = re.fullmatch(r"([\d,.]+[KkMm]?) decks", lines[i + 1])
            if deck_match:
                decks = parse_deck_count(deck_match.group(1))
        commanders.append(Commander(rank=rank, name=name, slug=slugify(name), decks=decks))
        seen.add(rank)
        if len(commanders) >= limit:
            break

    commanders.sort(key=lambda item: item.rank)
    return commanders


def previous_content_line(lines: list[str], index: int) -> str | None:
    for j in range(index - 1, -1, -1):
        candidate = lines[j].strip()
        if candidate and not candidate.endswith("decks"):
            return candidate
    return None


def commander_page_urls(page_number: int) -> Iterable[str]:
    if page_number == 1:
        yield f"{BASE_URL}/commanders"
    else:
        yield f"{BASE_URL}/commanders?page={page_number}"
        yield f"{BASE_URL}/commanders/{page_number}"
        yield f"{BASE_URL}/commanders?p={page_number}"


def more_json_urls(path: str) -> Iterable[str]:
    normalized = path.lstrip("/")
    yield f"https://json-cloudflare.edhrec.com/pages/{normalized}"
    yield f"https://json.edhrec.com/pages/{normalized}"
    yield f"{BASE_URL}/{normalized}"


def scrape_commanders(args: argparse.Namespace) -> list[Commander]:
    by_rank: dict[int, Commander] = {}
    more: str | None = None

    cache_path = args.cache_dir / "commanders" / "commanders.html"
    page = fetch(f"{BASE_URL}/commanders", cache_path, args.refresh, args.timeout, args.min_interval, args.jitter, args.retries)
    next_data = extract_next_data(page)
    if next_data is not None:
        parsed, more = parse_commanders_from_json_payload(next_data, args.limit_commanders)
    else:
        parsed = parse_commanders_from_html(page, args.limit_commanders)
    for commander in parsed:
        by_rank.setdefault(commander.rank, commander)

    if args.verbose:
        print(f"commander page 1: +{len(parsed)}, total {len(by_rank)}", file=sys.stderr)

    page_number = 2
    while more and len(by_rank) < args.limit_commanders and page_number <= args.max_commander_pages:
        page_added = 0
        payload = None
        for url in more_json_urls(more):
            cache_name = re.sub(r"[^A-Za-z0-9]+", "_", more).strip("_")
            cache_path = args.cache_dir / "commanders" / f"{cache_name}.json"
            try:
                text = fetch(url, cache_path, args.refresh, args.timeout, args.min_interval, args.jitter, args.retries)
            except (HTTPError, URLError, TimeoutError) as exc:
                if args.verbose:
                    print(f"warning: cannot fetch {url}: {exc}", file=sys.stderr)
                continue
            payload = parse_json_text(text)
            if payload is not None:
                break

        if payload is None:
            break

        parsed, more = parse_commanders_from_json_payload(payload, args.limit_commanders)
        for commander in parsed:
            if commander.rank not in by_rank:
                by_rank[commander.rank] = commander
                page_added += 1

        if args.verbose:
            print(f"commander page {page_number}: +{page_added}, total {len(by_rank)}", file=sys.stderr)
        if page_added == 0:
            break
        page_number += 1

    return [by_rank[rank] for rank in sorted(by_rank)[: args.limit_commanders]]


def read_existing_rows(path: Path) -> list[RawCardData]:
    if not path.exists():
        return []

    rows: list[RawCardData] = []
    with path.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle, delimiter="\t")
        for row in reader:
            if not row:
                continue
            rows.append(
                RawCardData(
                    commander_rank=int(row["commander_rank"]),
                    commander=row["commander"],
                    commander_slug=row["commander_slug"],
                    commander_decks=parse_optional_int(row.get("commander_decks")),
                    card=row["card"],
                    section=row.get("section") or None,
                    included_decks=int(row["included_decks"]),
                    eligible_decks=parse_optional_int(row.get("eligible_decks")),
                    inclusion=parse_optional_float(row.get("inclusion_percent")),
                    synergy=parse_optional_int(row.get("synergy_percent")),
                )
            )
    return rows


def parse_optional_int(value: str | None) -> int | None:
    if value is None or value == "":
        return None
    return int(float(value))


def parse_optional_float(value: str | None) -> float | None:
    if value is None or value == "":
        return None
    return float(value)


def parse_card_data(commander: Commander, page: str, per_commander_limit: int) -> list[RawCardData]:
    if per_commander_limit <= 0:
        return []

    next_data = extract_next_data(page)
    if next_data is not None:
        rows = parse_card_data_from_json_payload(commander, next_data, per_commander_limit)
        if rows:
            return rows

    lines = visible_lines(page)
    rows: list[RawCardData] = []
    seen_cards: set[str] = set()
    inclusion_re = re.compile(r"(\d+(?:\.\d+)?)%inclusion\s+([\d,.]+[KkMm]?) decks(?:\s+([\d,.]+[KkMm]?) decks)?")
    synergy_re = re.compile(r"(-?\d+)%\s*synergy")
    current_section: str | None = None

    for i, line in enumerate(lines):
        if line in SECTION_HEADINGS:
            current_section = line
            continue
        match = inclusion_re.fullmatch(line)
        if not match:
            continue
        card = previous_content_line(lines, i)
        if not card or card in SECTION_HEADINGS or card == commander.name or card in seen_cards:
            continue
        try:
            included_decks = parse_deck_count(match.group(2))
            eligible_decks = parse_deck_count(match.group(3)) if match.group(3) else None
        except ValueError:
            continue
        synergy = None
        for lookahead in lines[i + 1 : i + 4]:
            synergy_match = synergy_re.fullmatch(lookahead)
            if synergy_match:
                synergy = int(synergy_match.group(1))
                break
        rows.append(
            RawCardData(
                commander_rank=commander.rank,
                commander=commander.name,
                commander_slug=commander.slug,
                commander_decks=commander.decks,
                card=card,
                section=current_section,
                included_decks=included_decks,
                eligible_decks=eligible_decks,
                inclusion=float(match.group(1)),
                synergy=synergy,
            )
        )
        seen_cards.add(card)
        if len(rows) >= per_commander_limit:
            break

    return rows


def parse_card_data_from_json_payload(commander: Commander, payload: dict, per_commander_limit: int) -> list[RawCardData]:
    rows: list[RawCardData] = []
    seen_cards: set[str] = set()

    for cardlist in iter_cardlists(payload):
        section = cardlist.get("header") if isinstance(cardlist.get("header"), str) else None
        for card in cardlist.get("cardviews", []):
            if not isinstance(card, dict):
                continue
            name = card.get("name")
            if not name or name == commander.name or name in seen_cards:
                continue

            included = card.get("num_decks") or card.get("inclusion")
            eligible = card.get("potential_decks")
            if included is None:
                continue

            included_decks = int(included)
            eligible_decks = int(eligible) if eligible is not None else None
            inclusion_percent = None
            if eligible_decks:
                inclusion_percent = round((included_decks / eligible_decks) * 100.0, 4)

            synergy = card.get("synergy")
            synergy_percent = round(float(synergy) * 100) if synergy is not None else None

            rows.append(
                RawCardData(
                    commander_rank=commander.rank,
                    commander=commander.name,
                    commander_slug=commander.slug,
                    commander_decks=commander.decks,
                    card=name,
                    section=section,
                    included_decks=included_decks,
                    eligible_decks=eligible_decks,
                    inclusion=inclusion_percent,
                    synergy=synergy_percent,
                )
            )
            seen_cards.add(name)
            if len(rows) >= per_commander_limit:
                return rows

    return rows


def scrape_card_data(
    args: argparse.Namespace,
    commanders: list[Commander],
    existing_rows: list[RawCardData] | None = None,
) -> list[RawCardData]:
    all_rows: list[RawCardData] = list(existing_rows or [])
    seen_pairs = {(row.commander_slug, row.card) for row in all_rows}
    existing_counts: dict[str, int] = {}
    for row in all_rows:
        existing_counts[row.commander_slug] = existing_counts.get(row.commander_slug, 0) + 1

    to_scrape_slugs = {
        commander.slug
        for commander in commanders
        if (
            existing_counts.get(commander.slug, 0) < args.cards_per_commander
            if args.fill_incomplete
            else existing_counts.get(commander.slug, 0) == 0
        )
    }
    if args.verbose and existing_rows:
        skipped = len(commanders) - len(to_scrape_slugs)
        print(f"resume: loaded {len(existing_rows)} existing rows; skipping {skipped} existing commanders", file=sys.stderr)

    for index, commander in enumerate(commanders, start=1):
        if commander.slug not in to_scrape_slugs:
            if args.verbose:
                print(f"{index}/{len(commanders)} {commander.name}: already have {existing_counts.get(commander.slug, 0)} cards", file=sys.stderr)
            continue
        url = f"{BASE_URL}/commanders/{commander.slug}"
        cache_path = args.cache_dir / "cards" / f"{commander.slug}.html"
        try:
            page = fetch(url, cache_path, args.refresh, args.timeout, args.min_interval, args.jitter, args.retries)
        except (HTTPError, URLError, TimeoutError) as exc:
            print(f"warning: cannot fetch {commander.name} at {url}: {exc}", file=sys.stderr)
            continue
        rows = parse_card_data(commander, page, args.cards_per_commander)
        added = 0
        for row in rows:
            key = (row.commander_slug, row.card)
            if key in seen_pairs:
                continue
            all_rows.append(row)
            seen_pairs.add(key)
            added += 1
        if args.verbose or index % 25 == 0:
            print(f"{index}/{len(commanders)} {commander.name}: {len(rows)} cards, +{added} new", file=sys.stderr)
    return all_rows


def write_tsv(path: Path, rows: list[RawCardData]) -> None:
    if path.parent:
        path.parent.mkdir(parents=True, exist_ok=True)
    rows = sorted(rows, key=lambda row: (row.commander_rank, row.commander, row.card))
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=TSV_FIELDS, delimiter="\t", lineterminator="\n")
        writer.writeheader()
        for row in rows:
            writer.writerow(
                {
                    "commander_rank": row.commander_rank,
                    "commander": row.commander,
                    "commander_slug": row.commander_slug,
                    "commander_decks": "" if row.commander_decks is None else row.commander_decks,
                    "card": row.card,
                    "section": "" if row.section is None else row.section,
                    "included_decks": row.included_decks,
                    "eligible_decks": "" if row.eligible_decks is None else row.eligible_decks,
                    "inclusion_percent": "" if row.inclusion is None else row.inclusion,
                    "synergy_percent": "" if row.synergy is None else row.synergy,
                }
            )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=SCRIPT_DIR / "build" / "edhrec_raw.tsv")
    parser.add_argument("--cache-dir", type=Path, default=SCRIPT_DIR / "build" / "cache")
    parser.add_argument("--limit-commanders", type=int, default=1000)
    parser.add_argument("--cards-per-commander", type=int, default=500)
    parser.add_argument("--max-commander-pages", type=int, default=20)
    parser.add_argument(
        "--min-interval",
        type=float,
        default=2.0,
        help="Minimum seconds between live EDHREC requests. Cached pages do not wait.",
    )
    parser.add_argument("--jitter", type=float, default=0.75, help="Random extra seconds added to live request spacing.")
    parser.add_argument("--retries", type=int, default=3)
    parser.add_argument("--timeout", type=int, default=30)
    parser.add_argument("--refresh", action="store_true")
    parser.add_argument("--no-resume", action="store_true", help="Ignore an existing output TSV and rebuild it from scratch.")
    parser.add_argument(
        "--fill-incomplete",
        action="store_true",
        help="When resuming, re-scrape commanders that have fewer than --cards-per-commander rows. By default any existing commander is skipped.",
    )
    parser.add_argument("--verbose", action="store_true")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    commanders = scrape_commanders(args)
    if len(commanders) < args.limit_commanders:
        print(
            f"warning: found {len(commanders)} commanders; EDHREC may require a browser API path for ranks beyond the HTML pages",
            file=sys.stderr,
        )
    existing_rows = [] if args.no_resume or args.refresh else read_existing_rows(args.output)
    rows = scrape_card_data(args, commanders, existing_rows)
    write_tsv(args.output, rows)
    print(f"Wrote raw TSV: {args.output} ({len(rows)} rows)", file=sys.stderr)


if __name__ == "__main__":
    main()
