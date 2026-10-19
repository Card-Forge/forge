---
name: check-card
description:
  Audit one or more Forge card scripts (forge-gui/res/cardsfolder) against what the card does in MTG - Oracle text,
  rulings, every param read by the Java that runs it, sibling-card conventions - and decide whether the script, the
  Java, or nothing is wrong. Use when asked "is <card> scripted correctly", "check this card", "will this card fix
  work", or before opening an upstream card-script PR.
---

# Check a card script

A script is correct when every Oracle clause maps to script lines that Forge's Java actually executes with the meaning
the clause needs, and nothing in the script is dead. "Parses" is not "correct": Forge silently ignores unknown params,
so a misspelled or misplaced param fails no build (PORT-8).

Read-only until step 7. Cite every claim as `File.java:line` or `script line`; "probably" is not a verdict.

## 1. Find the script

```bash
cd /home/user/crucible
git grep -l "^Name:<Card Name>$" -- forge-gui/res/cardsfolder      # path: <first letter>/<snake_case>.txt
```

Audit `upstream/master`'s copy when the question is about upstream (`git show upstream/master:<path>`); the fork may
carry a local fix (`docs/crucible/porting/upstream-patches.md`, "Pending upstream fixes").

## 2. Get what the card should do

| Source | Use |
| --- | --- |
| Script's own `Oracle:` line | Always available; `\n` separates abilities |
| Scryfall `https://api.scryfall.com/cards/named?exact=<name>` (WebFetch) | Current Oracle (errata), `oracle_text`, `card_faces` |
| Scryfall `.../cards/<id>/rulings` | Edge cases the script must get right |

Scryfall may be blocked by the network policy (CONNECT 403). Then say so in the report and audit against the `Oracle:`
line - it can lag errata, so name that as an unchecked risk rather than assuming it is current.

Split the Oracle text into clauses. Each clause is one row of the report: "As X enters, choose odd or even", "you may
return a nonland permanent you own to your hand", "If you do, draw a card".

## 3. Build the script's graph

Follow every reference (`SubAbility$`, `Execute$`, `ReplaceWith$`, `ResultSubAbilities$`, `AddTrigger$` and the like; SVars read as amounts). Report every **dangling reference** (names a missing SVar) and **orphan SVar** (nothing references it; `AIPreference`, `PlayMain1`, `Deck*`, `AI:` excepted).

## 4. Map each clause to the lines that implement it

For each clause, name the lines, then check the meaning, not just the presence. Phrases that most often go wrong:

| Oracle wording | Script must say |
| --- | --- |
| "you may" / "up to" | Optional by that effect's own switch: `Optional$`, `OptionalDecider$` (triggers), absence of `Mandatory$` (hidden-origin ChangeZone), `TargetMin$ 0` |
| "return/sacrifice/exile a ..." (no "may") | The forced form, e.g. `Mandatory$ True` on a hidden-origin ChangeZone (Kor Skyfisher) |
| "target" | `ValidTgts$` (targets: hexproof, fizzle). "choose" without "target" must not use `ValidTgts$` |
| "you control" vs "you own" | `YouCtrl` vs `YouOwn` |
| "another" / "nontoken" | `.Other` / `+!token` (or `nonToken`) |
| "If you do" / "if ... this way" | `RememberChanged$`/`RememberObjects$` + `ConditionDefined$ Remembered`, then `DB$ Cleanup` |
| "until end of turn" / durations | `Duration$`, or the effect's default (Pump: until end of turn) |
| Intervening "if" in a trigger | `CheckSVar$`/`IsPresent$` on the `T:` line, checked on trigger and on resolution |
| "As ~ enters" | `K:ETBReplacement:<layer>:<SVar>`, not an ETB trigger |
| "mana value" | `cmc...` properties |

## 5. Check every param is read by the Java that runs it

1. API to class: `forge-game/src/main/java/forge/game/ability/ApiType.java` maps `ChangeZone` to
   `ChangeZoneEffect.class`, and so on. Trigger modes: `forge/game/trigger/Trigger<Mode>.java`; statics:
   `forge/game/staticability/StaticAbility<Mode>.java`; replacements: `forge/game/replacement/Replace<Event>.java`.
2. For each param `Key$` on the line, find a reader:

   ```bash
   git grep -n '"Key"' upstream/master -- 'forge-game/src/main/java/**/*.java' 'forge-ai/src/main/java/**/*.java'
   ```

   A read in the effect class, its base (`SpellAbilityEffect`), the generic layers (`SpellAbility`, `CardTraitBase`,
   `AbilityUtils`, `AbilityFactory`, `SpellAbilityCondition`/`Restriction`) or the API's AI class (`forge-ai`, e.g.
   `ChangeZoneAi`) counts. A read only in some **other** effect class does not: that is the classic defect (`ListTitle$`
   is `ChooseNumberEffect`'s, not `ChooseEvenOddEffect`'s).

3. Corpus-wide cross-check of unread params: `cd crucible && go run ./tools/apiscan -check -api` (both modes are CI
   gates here; a carried fix may be what makes them pass - check `upstream/master` too).
4. For an unfamiliar API, hand the semantics question to the `forge-oracle` subagent instead of reading the whole class.

## 6. Decide who is wrong

Compare against siblings before blaming either side: find cards with the same Oracle phrase and look at how the majority
scripts it.

```bash
git grep -h "you may return a nonland permanent" upstream/master -- forge-gui/res/cardsfolder   # Oracle siblings
git grep -h "Hidden\$ True" upstream/master -- forge-gui/res/cardsfolder | grep -c "Mandatory\$ True"
```

| Finding | Verdict |
| --- | --- |
| Every clause implemented, every param read, siblings agree | **Correct** |
| Param no code reads, and the behavior is already right without it (siblings omit it) | **Script: dead param** - remove it |
| Param no code reads, and the card behaves wrong without it | **Script: wrong param name** - rename to what the Java reads |
| Clause missing, or implemented with the wrong meaning (owner/controller, may/must, target/choose) | **Script: wrong behavior** - name the rules difference |
| Many cards use a param the effect ignores, or the Java cannot express the clause at all | **Java: missing support** - goes to `forge-java-defects.md` |
| Only prompt/description text differs | **Cosmetic** - say so; still fix if it is a dead param |

Give the counts that decided it (e.g. "98 of 128 hidden-origin battlefield ChangeZones use `Mandatory$ True`; 1 uses
`ChoiceOptional$`").

## 7. Report, and fix only when asked

Report one table per card: `Oracle clause | Script line(s) | Java that runs it | Verdict`, then dangling/orphan SVars,
then the overall verdict with the evidence counts.

When asked to fix (PORT-8 - never compensate in Go):

1. Record it: `docs/crucible/porting/card-script-defects.md` (script) or `forge-java-defects.md` (Java).
2. If Crucible's gates need it now: carry the edit in the fork and add a row to `upstream-patches.md`, "Pending upstream
   fixes", in the same commit (REV-1).
3. Upstream branch `upstream-pr/<slug>`, cards only, from `upstream/master` (never the fork's `master`); commit message cites the Java file:line that proves it; push to `origin`. Do not open the PR; record the branch in the pending row.
