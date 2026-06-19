# EDHREC Commander Matrix for Forge

This project builds a Forge-compatible `Commander.dat` from EDHREC commander pages in two passes.

The first pass scrapes EDHREC into a raw TSV. The second pass transforms that TSV into integer weights, then invokes `java/ForgeMatrixWriter.java`. The Java bridge uses Forge's own `PaperCard` objects and Java `ObjectOutputStream`, so the output has the same serialized shape that `CardThemedMatrixIO.loadMatrix("Commander")` expects:

```java
HashMap<String, List<Map.Entry<PaperCard, Integer>>>
```

The raw TSV preserves the fields needed to experiment with weight formulas without scraping EDHREC again: commander rank, commander deck count, card section, included deck count, eligible deck count, inclusion percentage, and synergy percentage.

## Usage

Pass 1, scrape raw EDHREC data:

```bash
python3 edhrec_to_forge.py \
  --limit-commanders 1000 \
  --cards-per-commander 500 \
  --min-interval 2.0 \
  --verbose
```

Pass 2, calculate weights and write `Commander.dat`:

```bash
python3 build_forge_matrix.py --weight-mode log
```

Useful options:

```bash
python3 edhrec_to_forge.py --refresh
python3 edhrec_to_forge.py --no-resume
python3 edhrec_to_forge.py --fill-incomplete
python3 edhrec_to_forge.py --min-interval 5 --jitter 2
python3 build_forge_matrix.py --weight-mode count
python3 build_forge_matrix.py --weight-mode inclusion --skip-dat
python3 build_forge_matrix.py --weight-mode sqrt --cards-per-commander 300
python3 build_forge_matrix.py --no-expand-pairs
```

Weight modes:

- `count`: raw included deck count. This is closest to Forge's current matrix semantics, but biases toward old cards.
- `inclusion`: inclusion rate only. This is age-neutral, but can overvalue tiny samples.
- `log`: inclusion rate multiplied by `log1p(included_decks)`. This is the default middle ground.
- `sqrt`: inclusion rate multiplied by `sqrt(included_decks)`. This is stronger sample-size weighting than `log`.

Notes:

- These scripts are intended to run from `forge-gui/tools`. By default they write intermediate files under `forge-gui/tools/build` and write the final matrix to `forge-gui/res/deckgendecks/Commander.dat`.
- Forge must already have `forge-gui-desktop/target/*jar-with-dependencies.jar`. If it does not, build Forge first.
- EDHREC embeds commander ranks in Next.js JSON. The scraper reads that structured data and follows EDHREC's `more` JSON pointer for ranks beyond the first page.
- Scraping resumes by default: if the output TSV already has rows for a commander, that commander is skipped on the next run. Use `--no-resume` to rebuild from scratch, or `--fill-incomplete` to re-scrape commanders with fewer than `--cards-per-commander` rows.
- Pair commanders are expanded by default during the build step. An EDHREC page named `A // B` contributes its card weights to both `A` and `B`, and each commander gets the other partner as a weighted card entry. This matches Forge's individual-commander matrix keys. Use `--no-expand-pairs` only for diagnostics.
- Live EDHREC requests default to at least 2 seconds apart plus random jitter, retry with exponential backoff, and honor numeric `Retry-After` responses. Cached pages do not make network requests.
- Unknown cards and commanders are skipped by the Java writer so the `.dat` file only contains cards present in the local Forge database.
