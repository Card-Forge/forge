# Port: ManaCost

- **Java source:** `forge-core/src/main/java/forge/card/mana/ManaCost.java` (417), `ManaCostShard.java` (336),
  `ManaCostParser.java` (86), `ManaAtom.java` (95), `forge-core/src/main/java/forge/card/MagicColor.java` (221)
- **Go target:** `crucible/internal/mana`
- **Status:** Done — M1, P0 gate

## What it does

Turns the `ManaCost:` line of a card script into a value: a generic amount plus an ordered run of shards. One shard is
one symbol — `{W}`, `{2/U}`, `{G/P}`, `{S}`, `{X}` — described by a bitmask of atoms saying what can pay it.

Everything downstream reads that value: mana value, colour, the mana solver at M6, and the mana-health metrics
([`../../telemetry/metric-definitions.md`](../../telemetry/metric-definitions.md), MET-10 to MET-15).

The golden in `internal/mana/testdata` is derived from `forge-gui/res/cardsfolder`, so `crucible-go.yml` triggers on
that path as well as on `crucible/**`. An upstream sync that adds a mana symbol fails the corpus test on the sync
itself, which is what [ADR-0001](../../adr/0001-fork-layout-and-upstream-sync.md) asks for.

Java's is three classes and an iterator interface, because the parser is an `Iterator<ManaCostShard>` that the cost
constructor drains. Go has one `Parse` function; the iterator carried no behaviour worth keeping.

## Deviations from Java

| Deviation                                                              | Reason                                                                                                                                                                                        |
| ---------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| An unrecognised character is an error, not a silent `GENERIC`          | `ManaCostShard.parseNonGeneric` ORs nothing for an unknown character, so a typo becomes a generic symbol and a mis-costed card. Failing the load is what lets the P2 vocabulary gate be total |
| `"no cost"` is read by the parser                                      | Java special-cases the string in `CardRules.java:791`, one layer up, which leaves its own parser unable to read 2,089 of its own corpus values                                                |
| `Parse` also accepts the braced form it prints                         | Java can read `2 W W` and write `{2}{W}{W}`, and read neither back. Accepting both makes `Parse(c.String())` equal `c`, which is the corpus gate (P0) and TEST-10's fixed-point property      |
| `Equal` compares shards as a multiset                                  | Java's `ManaCost` never overrides `equals`, so there is no behaviour to match. `{X}{R}` and `{R}{X}` print identically and must compare equal                                                 |
| `Colors` holds WUBRG only; the `{C}` symbol is not a colour            | Java disagrees with itself — `MagicColor.COLORLESS` is `0`, `ManaAtom.COLORLESS` is `1<<5` — and every comparison between the two needs adjusting by hand                                     |
| `hasNoCost` comes only from the literal `"no cost"`                    | Java also sets it when the generic total parses to exactly `-1`, which no corpus value produces and which would collide with cost reduction below zero                                        |
| Shards are a dense `uint8` index into a table, not an enum with fields | Keeps a `Shard` comparable and usable as an array index; the table holds what Java's enum constructor computed                                                                                |

Declaration order of the 45 shards is Java's, and is pinned by a test. Java's comment says why: "Place the shards that
offer least ways to be paid for first". The M6 mana solver walks them in that order, so a reordering would change which
payment it finds.

## Not ported yet

Each of these is display or AI-sorting code with no caller in Crucible today. Port it with the unit that needs it, not
before (PORT-6).

| Java                                                  | When                                    |
| ----------------------------------------------------- | --------------------------------------- |
| `getCmpCost` / `compareTo` — float compare weights    | Deck sorting and AI card evaluation, M7 |
| `serialize` / `deserialize` — `char 6` delimited form | Never; it exists for Forge's save files |
| `canBePaidWithAvailable`, `canBePaidWithManaOfColor`  | The mana solver, M6                     |
| `getNormalizedMana`, `combine`, `getColorShardCounts` | Cost modification effects, M6           |
| `getGlyphCount`, `getImageKey`                        | Never; both size a GUI symbol strip     |

`ShardColoredX` exists in the table because Java's enum has it, and it is unreachable from a card script: its atoms are
`WUBRG|X`, which no symbol spells. Only Emblazoned Golem constructs it, from code that is not ported.

## Null decisions

| Java                                                     | Go                                                                          |
| -------------------------------------------------------- | --------------------------------------------------------------------------- |
| `ManaCostShard.valueOf` returns `null` for unknown atoms | `(Shard, error)` — the caller cannot forget, and the error names the symbol |
| `ManaCostParser.next` returns `null` for a generic token | No equivalent; `Parse` adds to the generic total and moves on               |

## Open questions

- **Card scripts spell the same shard three ways** — `2/B`, `2B`, and `PRG` for `{R/G/P}`. All three parse, and the
  printed form is canonical, so the corpus golden shows exactly where the corpus is inconsistent.
