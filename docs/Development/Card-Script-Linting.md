# Card-script linting

The card-script linter checks card scripts against the engine code: whether each API, trigger mode and replacement event exists, and whether the engine actually reads each param a script sets. It runs in the test suite, in the Card Workshop, and on pull requests, where it comments on the changed lines.

## Checking your cards

You can check a script in two ways before it is merged.

**Open a pull request.** When the build finishes, a bot comments on the lines your PR changes. For example:

> `PreCostDesc$` → `PrecostDesc$` (params are case-sensitive)

> (warning) `NumCards$` → not used by LoseLife

> Types `Sorcery` → `Instant`

The last comment comes from comparing the card with its printed version on Scryfall. That comparison covers the name, type line, P/T, mana cost and loyalty. Cards that Scryfall doesn't list yet are skipped.

**Run the linter yourself.** You need a clone of the repository, a JDK and Maven. From the repository root, run:

```
mvn -pl forge-gui-desktop -am test "-Dtest=CardScriptLinterTest#lintCorpus" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dcardscript.path=forge-gui/res/cardsfolder/upcoming"
```

`cardscript.path` can be a folder or a single file, in the repository or anywhere else. The results appear near the end of the output:

```
Findings in ...\forge-gui\res\cardsfolder\upcoming:
  test_bolt.txt:4: [ERROR] KEY-TYPO `NumDmgg$` -> `NumDmg$`
  test_bolt.txt:4: [ERROR] REF-UNDEF `SubAbility$ DBDraw` -> no such SVar on this face
  test_bolt.txt:4: [ERROR] MISSING-KEY `DealDamage` needs `NumDmg$`
1 script(s), 3 error(s), 0 warning(s)
```

Each line shows the file, the line number, the severity, a code, and what to change.

### Errors and warnings

- **Error:** the engine ignores this part of the script or fails on it, so the card doesn't work as written. A PR build fails if one of its changed lines has an error.
- **Warning:** probably a mistake, but check before you change it. Some params are read by shared engine code that the linter can't attribute to one API yet, so a correct param is sometimes reported as not used.

### What the codes mean

| Code | Severity | Meaning | Usual fix |
|---|---|---|---|
| `KEY-TYPO` | error | Not a param, but a similar one exists. | Use the suggested name. |
| `KEY-CASE` | error | The right name in the wrong case. Params are case-sensitive. | Use the suggested case. |
| `UNKNOWN-KEY` | error | No engine code reads this param. | Check the spelling, or remove it. |
| `MISSING-KEY` | error | The ability needs this param. | Add it. |
| `API-UNKNOWN` | error | The API, trigger mode, replacement event or static mode doesn't exist. | Check the spelling. |
| `REF-UNDEF` | error | `SubAbility$`, `Execute$` or a similar param names an SVar that this face doesn't define. | Fix the name, or move the SVar to this face. |
| `DUP-PARAM` | error, or warning if both values are the same | The param appears twice on one line. The engine uses the last one. | Remove one of them. |
| `COST` | error | A cost the engine doesn't recognise. It would be treated as free. | Check the cost syntax. |
| `MANA` | error | Not a mana symbol, or a repeated letter such as `WW`. | Write each symbol separately, e.g. `W W`. |
| `NO-MANACOST` | error | A card that isn't a land has no `ManaCost` line. | Add one. |
| `LEX-PREFIX` | error | A line starts with a prefix the card reader doesn't recognise, e.g. `Oracel:`. | Fix the prefix. |
| `LEX-DELIM` | error | A list uses the wrong separator: ` & ` instead of `,`, or the reverse. | Use the separator the message names. |
| `CASE` | error | A zone or `Defined$` value in the wrong case, e.g. `self`. | Use the suggested case. |
| `TRIG-CTX` | error | A `Triggered...` value on an `A:` line. An activated ability has no trigger to refer to. | Move it to the trigger's SVar. |
| `LOYALTY` | error | A loyalty ability without `Planeswalker$ True`. | Add it. |
| `WRONG-KEY` | warning | A real param, but this API doesn't read it. | Remove it, or check whether it belongs on another line. |
| `INTERNAL-KEY` | warning | A param the engine sets itself. Scripts shouldn't set it. | Remove it. |
| `ORPHAN` | warning | An SVar ability that nothing refers to. | Remove it, or add the missing reference. |
| `LEX-PIPE`, `LEX-DBLSPACE`, `LEX-NOSPACE`, `LEX-CURLY` | warning | Spacing around `\|` or `$`, or a curly apostrophe. | Fix as the message says. |
| `DESC-COST` | warning | `SpellDescription$` repeats the activation cost. | Remove the cost from the description. |

The linter doesn't yet check param values (`Defined$`, `Valid*$`, values that must be `True`), keyword (`K:`) lines, or whether the script matches the Oracle text.

## How it works

| Step | What it does | Where | Runs in |
|---|---|---|---|
| 1. Param declarations | Classes list the params they read (see below). | `IHasForgeParams` | — |
| 2. Declaration check | Reads the compiled engine code to find every param passed to `getParam`, `hasParam` and similar methods. Fails if the declarations don't match. | `CardScriptParamDeclarationTest` | test suite |
| 3. Lint | Checks every card and token script (see the codes above). | `CardScriptLinter`, `CardScriptLinterTest` | test suite, Card Workshop |
| 4. Scryfall check | Compares each changed card with its printed version. | `.github/scripts/card_script_review.py` | CI on PRs |
| 5. PR comments | Posts the results of steps 3 and 4 on the lines a PR changes. | `.github/workflows/card-script-review.yml` | CI on PRs |

### Declaring params

A class lists the params it reads in `public static final` fields:

```java
public class DamageDealEffect extends DamageBaseEffect {
    public static final String[] OPTIONAL_PARAMS = { "DamageSource", "DivideEvenly", ... };
    public static final String[][] REQUIRED_PARAMS = {{"NumDmg"}};   // each group: one of these
```

- **Framework classes** (listed in `CardScriptParams.FRAMEWORK`, such as `AbilityFactory`, `CardTraitBase` and `SpellAbility`) declare the params that any ability can use. Each param is declared once, in the class that reads it.
- **Effects** declare the params their API uses in addition to those. This includes params that the effect's AI or helper methods read for it.
- **`INTERNAL_PARAMS`** lists params that the engine sets itself, for example when it builds keyword abilities. A script that sets one gets a warning.

Effects are being declared in batches. Until an effect declares its params, CI checks them against the compiled code instead, and the Card Workshop doesn't check them.

### When the declaration check fails

If you add a param read to the engine, `CardScriptParamDeclarationTest` fails until the param is declared. The message names the class and gives the array to paste:

```
card-script param declarations are out of sync with the code:
  DrawEffect: reads [Defined] without declaring them
  DrawEffect wants
    public static final String[] OPTIONAL_PARAMS = {
        "Defined", "IfDesc", "NumCards", "NumCardsDesc", "OptionalDecider", "Reveal", "Upto",
    };
```

A param read by an effect goes in that effect's `OPTIONAL_PARAMS`. A param read by a framework class goes in that class's array.

The check also follows method calls. A param read in a helper method belongs to the effects that call the helper. If shared code starts to call the helper, the param belongs to every ability instead. So an engine change can make the check fail for a class you didn't edit. The message still names the class and the fix.

### Declaring the next batch

This command prints an array ready to paste for every effect that hasn't declared yet:

```
mvn -pl forge-gui-desktop -am test "-Dtest=CardScriptParamDeclarationTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-DsuggestParams=true"
```

It also lists the shared code that still has to declare, and candidates for `REQUIRED_PARAMS` and `INTERNAL_PARAMS`. A required-param candidate is a param that 99.5% of an API's abilities in the card corpus set. Check the effect's code before declaring it required: a param with a default value isn't required.

### When a build fails

| | Local test run | CI on a PR | Card Workshop |
|---|---|---|---|
| **Error** | reported | fails the build if on a changed line; commented | highlighted |
| **Warning** | reported | commented | not shown |

A PR build only fails on errors in the lines the PR changes, so existing errors in other cards don't block it. These options change what fails a run:

- `-Dcardscript.gate=upcoming`: also fail on errors in `cardsfolder/upcoming`.
- `-Dcardscript.gate=all`: fail on every error.
- `-Dcardscript.diff=<file>`: fail on errors in the lines a `git diff -U0` output changes. CI uses this.

Every result is also written to `forge-gui-desktop/target/card-script-findings.json`.

The Card Workshop only knows declared params. Until an API declares its params, the Workshop doesn't flag param typos on that API's lines.

### How accurate it is

Each run prints which kinds of lines it checked and which it skipped.

`-Dcardscript.selfcheck=true` also measures how many mistakes the linter misses. It takes copies of real scripts in memory, adds a known mistake to each copy, and counts how many the linter reports. The script files aren't changed. The linter reports over 99% of misspelt param names, and about 63% of params set on an API that doesn't read them. It misses the rest because shared engine code reads those params for any ability.

Trigger, replacement and static params are only checked in CI until their classes declare.

### Why the comments come from a separate workflow

Most card PRs come from forks. A `pull_request` build of a fork PR has a read-only token, so it can't post comments. Instead the build saves its results as an artifact. `card-script-review.yml` runs after the build with a token that can comment. It checks that the artifact belongs to the PR's latest commit, then posts the comments. It treats the artifact as data and never runs code from the PR. GitHub only runs this workflow from the default branch, so it can't be tested on a PR before merge.

### Running all the checks

```
mvn -pl forge-gui-desktop -am test "-Dtest=CardScript*Test" "-Dsurefire.failIfNoSpecifiedTests=false"
```
