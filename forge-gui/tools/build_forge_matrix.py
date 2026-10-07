#!/usr/bin/env python3
"""Pass 2: transform raw EDHREC TSV data into a Forge-compatible Commander.dat."""

from __future__ import annotations

import argparse
import csv
import math
import os
import subprocess
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path


SCRIPT_DIR = Path(__file__).resolve().parent


@dataclass(frozen=True)
class WeightedRow:
    commander_rank: int
    commander: str
    card: str
    weight: int


PAIR_SEPARATOR = " // "
NAME_REPLACEMENTS = {
    "\ua789": ":",
}


def parse_int(value: str | None) -> int | None:
    if value is None or value == "":
        return None
    return int(float(value))


def parse_float(value: str | None) -> float | None:
    if value is None or value == "":
        return None
    return float(value)


def calculate_weight(row: dict[str, str], mode: str, scale: float, min_weight: int) -> int:
    included = parse_int(row.get("included_decks")) or 0
    eligible = parse_int(row.get("eligible_decks")) or parse_int(row.get("commander_decks")) or 0
    inclusion_percent = parse_float(row.get("inclusion_percent"))

    if eligible > 0:
        inclusion_rate = included / eligible
    elif inclusion_percent is not None:
        inclusion_rate = inclusion_percent / 100.0
    else:
        inclusion_rate = 0.0

    if mode == "count":
        score = included
    elif mode == "inclusion":
        score = inclusion_rate * scale
    elif mode == "log":
        score = inclusion_rate * math.log1p(included) * scale
    elif mode == "sqrt":
        score = inclusion_rate * math.sqrt(included) * scale
    else:
        raise ValueError(f"unknown weight mode: {mode}")

    return score_to_weight(score, min_weight)


def score_to_weight(score: float, min_weight: int) -> int:
    if score <= 0:
        return 0
    return max(min_weight, int(round(score)))


def commander_names(raw_name: str, expand_pairs: bool) -> list[str]:
    raw_name = normalize_name(raw_name)
    if expand_pairs and PAIR_SEPARATOR in raw_name:
        return [part.strip() for part in raw_name.split(PAIR_SEPARATOR) if part.strip()]
    return [raw_name]


def normalize_name(name: str) -> str:
    for source, replacement in NAME_REPLACEMENTS.items():
        name = name.replace(source, replacement)
    return name


def partner_weight(row: dict[str, str], args: argparse.Namespace) -> int:
    commander_decks = parse_int(row.get("commander_decks")) or 0
    if commander_decks <= 0:
        return args.min_weight

    if args.weight_mode == "count":
        score = commander_decks
    elif args.weight_mode == "log":
        score = math.log1p(commander_decks) * args.scale
    elif args.weight_mode == "sqrt":
        score = math.sqrt(commander_decks) * args.scale
    else:
        score = args.scale
    return score_to_weight(score, args.min_weight)


def read_weighted_rows(args: argparse.Namespace) -> list[WeightedRow]:
    by_commander_card: dict[str, dict[str, WeightedRow]] = defaultdict(dict)
    pair_partner_rows: dict[tuple[str, str], WeightedRow] = {}

    with args.input.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle, delimiter="\t")
        for row in reader:
            included = parse_int(row.get("included_decks")) or 0
            if included < args.min_included_decks:
                continue
            weight = calculate_weight(row, args.weight_mode, args.scale, args.min_weight)
            if weight <= 0:
                continue
            rank = parse_int(row.get("commander_rank")) or 999999
            raw_commander = row["commander"]
            commanders = commander_names(raw_commander, args.expand_pairs)
            for commander in commanders:
                weighted = WeightedRow(
                    commander_rank=rank,
                    commander=commander,
                    card=normalize_name(row["card"]),
                    weight=weight,
                )
                keep_best_row(by_commander_card[commander], weighted)

            if args.expand_pairs and len(commanders) > 1:
                p_weight = partner_weight(row, args)
                for commander in commanders:
                    for partner in commanders:
                        if partner == commander:
                            continue
                        key = (commander, partner)
                        existing = pair_partner_rows.get(key)
                        candidate = WeightedRow(rank, commander, partner, p_weight)
                        if existing is None or candidate.weight > existing.weight:
                            pair_partner_rows[key] = candidate

    for row in pair_partner_rows.values():
        keep_best_row(by_commander_card[row.commander], row)

    rows: list[WeightedRow] = []
    for commander_cards in by_commander_card.values():
        commander_rows = list(commander_cards.values())
        commander_rows.sort(key=lambda item: (-item.weight, item.card))
        if args.cards_per_commander > 0:
            commander_rows = commander_rows[: args.cards_per_commander]
        rows.extend(commander_rows)

    rows.sort(key=lambda item: (item.commander_rank, item.commander, -item.weight, item.card))
    return rows


def keep_best_row(rows_by_card: dict[str, WeightedRow], candidate: WeightedRow) -> None:
    existing = rows_by_card.get(candidate.card)
    if existing is None or candidate.weight > existing.weight:
        rows_by_card[candidate.card] = candidate


def write_weighted_tsv(path: Path, rows: list[WeightedRow]) -> None:
    if path.parent:
        path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as handle:
        handle.write("# commander\tcard\tweight\n")
        for row in rows:
            handle.write(f"{row.commander}\t{row.card}\t{row.weight}\n")


def compile_writer(args: argparse.Namespace) -> Path:
    jar_candidates = sorted((args.forge_dir / "forge-gui-desktop/target").glob("forge-gui-desktop-*-jar-with-dependencies.jar"))
    if not jar_candidates:
        raise SystemExit("Cannot find forge-gui-desktop jar-with-dependencies. Build Forge first with Maven.")
    jar = jar_candidates[-1]
    classes_dir = (args.build_dir / "classes").resolve()
    classes_dir.mkdir(parents=True, exist_ok=True)
    source = Path(__file__).resolve().parent / "java" / "ForgeMatrixWriter.java"
    if not source.exists():
        raise SystemExit(f"Cannot find Java writer source: {source}")
    subprocess.run(["javac", "-cp", str(jar), "-d", str(classes_dir), str(source)], check=True)
    return jar


def write_dat(args: argparse.Namespace) -> None:
    jar = compile_writer(args)
    classes_dir = (args.build_dir / "classes").resolve()
    forge_gui_dir = (args.forge_dir / "forge-gui").resolve()
    cmd = [
        "java",
        "-cp",
        os.pathsep.join([str(classes_dir), str(jar)]),
        "ForgeMatrixWriter",
        str(forge_gui_dir),
        str(args.weighted_tsv.resolve()),
        str(args.output.resolve()),
    ]
    subprocess.run(cmd, cwd=forge_gui_dir, check=True)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, default=SCRIPT_DIR / "build" / "edhrec_raw.tsv")
    parser.add_argument("--weighted-tsv", type=Path, default=SCRIPT_DIR / "build" / "edhrec_weighted.tsv")
    parser.add_argument("--output", type=Path, default=default_forge_gui_dir() / "res" / "deckgendecks" / "Commander.dat")
    parser.add_argument("--forge-dir", type=Path, default=default_forge_root())
    parser.add_argument("--build-dir", type=Path, default=SCRIPT_DIR / "build")
    parser.add_argument(
        "--weight-mode",
        choices=("count", "inclusion", "log", "sqrt"),
        default="log",
        help="count preserves raw EDHREC counts; the other modes reduce age bias with denominator-aware scores.",
    )
    parser.add_argument("--scale", type=float, default=1000.0)
    parser.add_argument("--min-weight", type=int, default=1)
    parser.add_argument("--min-included-decks", type=int, default=1)
    parser.add_argument("--cards-per-commander", type=int, default=500, help="Use 0 for no per-commander cap.")
    parser.add_argument(
        "--no-expand-pairs",
        dest="expand_pairs",
        action="store_false",
        help="Keep EDHREC pair commanders as literal 'A // B' keys instead of expanding them to Forge's individual commander keys.",
    )
    parser.add_argument("--skip-dat", action="store_true", help="Only write the weighted TSV.")
    parser.set_defaults(expand_pairs=True)
    args = parser.parse_args()
    args.input = args.input.resolve()
    args.weighted_tsv = args.weighted_tsv.resolve()
    args.output = args.output.resolve()
    args.forge_dir = args.forge_dir.resolve()
    args.build_dir = args.build_dir.resolve()
    return args


def default_forge_gui_dir() -> Path:
    candidate = SCRIPT_DIR.parent
    if candidate.name == "forge-gui":
        return candidate
    return Path.cwd()


def default_forge_root() -> Path:
    forge_gui_dir = default_forge_gui_dir()
    if forge_gui_dir.name == "forge-gui":
        return forge_gui_dir.parent
    return Path.cwd()


def main() -> None:
    args = parse_args()
    rows = read_weighted_rows(args)
    write_weighted_tsv(args.weighted_tsv, rows)
    print(f"Wrote weighted TSV: {args.weighted_tsv} ({len(rows)} rows)")
    if not args.skip_dat:
        write_dat(args)


if __name__ == "__main__":
    main()
