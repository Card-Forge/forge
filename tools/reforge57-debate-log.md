# REFORGE #57 — Adversarial Design Debate

**Question:** How should stacked-token static-ability / permanent handling resolve the "ghost card" correctness bug (issue #57) without throwing away a roadmapped performance flyweight?

## The three options under debate (from issue #57)
- **OPTION 1 — Eager identity-preserving expansion.** Expand stacks right after the token burst, re-adding the SAME token objects (no lossy quantity-folding). Kills #57 for all tokens. Cost: folding no longer saves N card objects → flyweight becomes thin bookkeeping; real O(1) deferred to roadmap 1d (opaque GameCopier flyweight, still open). Matches roadmap 1h reality (stacks already expand on next real read).
- **OPTION 2 — In-place promote (prototype-only).** promote() first result IS the prototype object re-added as a real permanent; fresh copies for the rest. Minimal. Fixes references pointing at the stack *prototype* (first token). Folded tokens (2nd..N identical) REMAIN ghosts → documented follow-up.
- **OPTION 3 — Defer stacking + global reference re-point.** Keep lossy flyweight; at expandStacks() time walk all reference sites (triggerList, combat, remembered/imprinted, attachments) and swap ghost refs to materialized cards. Largest, most fragile; the ONLY option that retains the memory win.

## Code facts shared by both agents (current state, post-1c/1f/1g)
- `PlayerZoneBattlefield.tryStackToken(Card)` (line 82): if token can merge into a stack, `cardList.remove(c)` + `stack.addQuantity(1)`; else `cardList.remove(c)` + `new StackedTokenCard(c,1)`. **Removes card from cardList immediately, keeps it as stack prototype or folds into quantity.**
- `StackedTokenCard` keeps only `final Card prototype` + `int quantity`. Folded tokens' Card objects are NOT retained (that is the O(N)→O(1) memory win). `promote(int count)` (line 126): validates 1<=count<=quantity; loops `new CardCopyService(prototype).copyCard(true)` → fresh Card objects (new IDs); carries over owner/controller/timestamp/GamePieceType/`setTurnInZone/setTurnInController/setSickness`. NEVER returns the prototype itself.
- `PlayerZoneBattlefield.expandStacks()` (line 101): if empty return; snapshot+clear stackedTokens (re-entrancy guard); `trigger=false`; for each stack, for each copy of `stack.promoteAll()`: `add(copy)` (routes through Zone.add, NOT moveToPlay, to avoid double ETB); finally restore trigger. Called by the `getCards()` and `iterator()` overrides — so ANY real read of the battlefield materializes all stacks with fresh objects.
- `TokenEffectBase.makeTokenTable` burst loop: per token — `Card moved = moveToPlay(tok,...)`; THEN `tryStackToken(moved)` (removes from cardList); THEN captures refs on `moved` for `triggerList.put`, `triggerList.addToken`, `addToCombat`, `host.addRemembered(moved)`, `host.addImprintedCard(moved)`, `allTokens.add(moved)`, `attachTokenTo(tok,sa)`. After the burst, `bf.setSuppressViewUpdate(suppressRestore)` restores view refresh. So the first real read → expandStacks() → fresh cards → every captured `moved` ref is a ghost.
- Symptom set: "sacrifice that token"/"that token dies" acts on ghost while visible copy survives (phantom permanents); tokens auto-attacking (Krenko/Adeline/Hanweir/Goblin Rabblemaster) attack invisibly; AttachAfter auras attach to invisible card.
- **Roadmap 1h (DONE) already documents:** stacks are expanded by the next zone entry's view refresh. Flyweight memory win is real only between creation and next read (ultra-short-lived).
- **Core tension:** to make EVERY folded token's refs valid you must retain the folded token objects → O(N) memory → defeats the flyweight itself. This is the crux.
- Related roadmap items: 1a-1h mostly DONE; **1d (opaque GameCopier flyweight) OPEN**; 1e (relax canMerge) open; issue #58 (apply-phase materializes pending stacks each check) open; issue #61 (expandStacks O(K^2) view refresh) open.

## DEBATE PROTOCOL (both agents must obey)
1. One comment per turn. Append to this file with `cat >>`. NEVER edit/overwrite existing lines.
2. Every comment opens with an id line `[COMMENT <id>]` then `| REPLIES_TO: <preceding comment id or "base">`. You MUST reply to the most recent comment (the other agent's or your own). This preserves the chain — it is forbidden to skip.
3. Comment fields, in this order:
   - `ROLE:` your persona name
   - `POSITION:` OPENING | COUNTER | REFINEMENT | CONCESSION | SYNTHESIS
   - `RECOMMENDS:` OPTION-1 | OPTION-2 | OPTION-3 | HYBRID-<describe> | SPLIT-REJECT
   - `VERDICT:` one of `{correctness>perf, perf>correctness, balanced}` — your dominant tie-break
   - `TAGS:` comma-separated subset of {PROPOSAL, COUNTER, CONSENSUS, OPEN-BLOCKER, PONYTRAIL-MIN, NON-STARTER}
   - body: max ~300 words. State the strongest point of the position you are replying to and why it's insufficient (or concede it), then your own analysis and concrete recommendation. Name the code change(s) you propose.
   - `AGREEMENTS:` list ids/points you now accept from the other side.
   - `REMAINING-DISPUTE:` what still divides you.
4. Tag `[CONSENSUS]` right in the `<verbatim>` if you genuinely agree the other side's core proposal is acceptable for the project; disagree-and-concede is allowed only if it is genuinely impossible/detrimental.
5. Fallback: the chain ends after comment **B10** OR when two consecutive comments both carry `[CONSENSUS]` on the same recommendation. If you reach round 3 with no consensus, each agent must write a `POSITION: SYNTHESIS` final statement that (a) restates your recommendation, (b) gives the decision-relevant data the PROJECT OWNER needs (cost, risk, what must hold), (c) states the condition under which you would switch.

## WIN/EXTRACTION GOAL
The owner wants, not a winner, but the highest-signal data to decide:
- The one technical crux that most constrains the decision
- Blast radius / risk of each option (what breaks, what must hold, test surface)
- Whether the performance win is worth the correctness debt at all (1h says it's marginal)
- A concrete recommendation, with the conditions that would flip it

## HARD BOUNDARIES
- Never propose deleting roadmap items without naming the upstream-sync manifest impact.
- Ground every claim in the code facts above; if you need more, say so but do not stall.
- Be assertive but epistemic: clearly attribute what is fact, what is engineering judgment, and what is speculation.

## V2 AMENDMENT — concurrent regions (replaces line-1 "append to this file")
- Each agent owns a PRIVATE region file it ALONE writes (append-only, `cat >>`). Never overwrite, never edit another agent's region.
  - CORRECTNESS agent (ANGELA) region: `/data/user/0/com.m4coding.ide/cache/opencode/reforge57_region_A.md`
  - FLYWEIGHT agent (BORIS) region:   `/data/user/0/com.m4coding.ide/cache/opencode/reforge57_region_B.md`
- This file (reforge57_debate.md) is READ-ONLY shared brief + code facts; nobody edits it during the debate.
- Order is defined by `REPLIES_TO` comment ids (never by wall-clock or file position). Orchestrator reconstructs order by topological sort of the reply graph.
- Per round, an agent replies to the other side's comment from the PREVIOUS round (guaranteed already persisted); both sides write that reply concurrently.
- Within each region comments are strictly sequential (each agent's own id sequence A1,A2,... / B1,B2,...).
- Target comment fields unchanged. Rounds: R1=(A1,B1 on base), R2=(A2→B1, B2→A1), R3=(A3→B2, B3→A2), R4 final=(A4→B3 or A3 as needed, B4→A3/A4). Consensus = two consecutive `[CONSENSUS]` on same recommendation; else stop after R4.

## V3 PROTOCOL (FINAL — supersedes V2)
Single shared file. This file IS the debate log. Both agents append their comments at the ABSOLUTE BOTTOM of this file.
- WRITE: one comment = ONE atomic `cat >>` heredoc (single write syscall). NEVER read-modify-write; never Edit tool; never touch lines above yours. Each comment is self-contained; a trailing `---` line closes it.
- ORDER is defined EXCLUSIVELY by `REPLIES_TO` + comment-id sequence, never by file position or wall-clock. Interleaving in the file is expected and harmless — the orchestrator reconstructs order by topological sort of the reply graph.
- COMMENT IDS: CORRECTNESS agent owns A1,A2,A3,A4... ; FLYWEIGHT agent owns B1,B2,B3,B4... (never reuse an id).
- ROUND MAP (each agent writes its comment for a round concurrently; the target comment it replies to always already exists because the other side wrote it in the previous round):
  - R1: B1 -> REPLIES_TO A1   (A1 seed already present)
  - R2: A2 -> REPLIES_TO B1  |  B2 -> REPLIES_TO A2
  - R3: A3 -> REPLIES_TO B2  |  B3 -> REPLIES_TO A3
  - R4 final: A4 -> REPLIES_TO B3 | B4 -> REPLIES_TO A4
- Consensus rule: if at the end of a round the two newest comments both carry `[CONSENSUS]` in TAGS on the same RECOMMENDS, stop early. Otherwise hard stop after R4.
- Every comment MUST include the fields in order: [COMMENT <id>] / REPLIES_TO / ROLE / POSITION (OPENING|COUNTER|REFINEMENT|CONCESSION|SYNTHESIS) / RECOMMENDS (OPTION-1|OPTION-2|OPTION-3|HYBRID-...|SPLIT-REJECT) / VERDICT ({correctness>perf|perf>correctness|balanced}) / TAGS (PROPOSAL, COUNTER, CONSENSUS, OPEN-BLOCKER, PONYTRAIL-MIN, NON-STARTER) / BODY (<=300 words, names concrete code changes + blast radius/test surface, marks fact vs judgment vs speculation) / AGREEMENTS / REMAINING-DISPUTE.
- An agent may chain to its OWN previous comment (REPLIES_TO own id) to continue a thought, but every NEW round's first choice is to reply to the other side's latest.

## DECISION-MEMO OUTPUT (orchestrator, after debate ends)
Produce: (1) each side's final recommendation + conditions that would flip it; (2) the single crux that most constrained the decision; (3) per-option risk/blast-radius/test matrix as claimed by both; (4) a synthesis recommendation for the owner. Grounded in AGREEMENTS/REMAINING-DISPUTE fields.

---
[COMMENT A1]
| REPLIES_TO: base
ROLE: ANGELA (correctness-first semantics architect)
POSITION: OPENING
RECOMMENDS: OPTION-1
VERDICT: correctness>perf
TAGS: PROPOSAL, PONYTRAIL-MIN
BODY:
The brief's symptom set — sacrifice/dies acting on a ghost while the visible copy survives, tokens attacking invisibly, auras attaching to invisible cards — is game-state corruption, not cosmetics: two objects share one logical identity (phantom permanents). #57 is a correctness bug first; the memory question is second.

Why not OPTION-2: prototype-promote fixes only the FIRST token's refs. FACT (line 12): folded 2nd..N tokens' Card objects are NOT retained — and the refs captured on them (triggerList.put, addToCombat, addRemembered, addImprintedCard, attachTokenTo, allTokens) are precisely the ones that stay ghosts. Krenko's folded attackers and "sacrifice THAT token" targets are mostly folded tokens, not the prototype. OPTION-2 ships #57 half-open.

Why not OPTION-3: the reference-site set is open-ended, not enumerable — triggerList, combat, remembered/imprinted, attachments, allTokens today; delayed triggers, counters, linked abilities tomorrow. Every site we miss silently regresses to a phantom. Engineering judgment: an un-enumerable correctness debt buys a memory win that roadmap 1h (FACT, line 16) already documents as ultra-short-lived — stacks materialize on the next real zone read anyway.

Concrete OPTION-1 change: in PlayerZoneBattlefield.tryStackToken (line 82) stop removing the card from cardList on merge; retain every token object; StackedTokenCard degrades to view-level bookkeeping; expandStacks re-adds the SAME objects — no fresh copies, no new IDs. TokenEffectBase.makeTokenTable and every ref-capture site stay untouched. Blast radius: one class + its tests, not a ref-site audit.

Test surface: existing stack/promote tests plus identity regressions — post-burst triggerList/remembered/imprinted/combat-tap/aura refs are `==` the visible battlefield card; "sacrifice that token" removes the visible copy; auto-attackers (Krenko/Adeline) are visually on the battlefield.

Cost: O(N) card objects live between burst and next read. Speculation: given 1h, that window is near-zero so the memory delta is negligible — I want BORIS to prove or refute this with numbers, not theory.
AGREEMENTS: I accept as FACT the brief's line-16 claim that stacks materialize on the next real zone read (1h DONE) and line-12 that folded tokens are not retained; the flyweight win is real only between burst and next read.
REMAINING-DISPUTE: (a) whether a near-zero-lived memory win justifies ANY retained correctness debt, and (b) whether OPTION-2's prototype-promote actually reaches the user-visible symptoms — I hold it does not for the folded majority.

---

[COMMENT B1]
| REPLIES_TO: A1
ROLE: BORIS (flyweight/perf architect)
POSITION: COUNTER
RECOMMENDS: HYBRID-2+3
VERDICT: perf>correctness
TAGS: PROPOSAL, COUNTER
BODY:
Acknowledge your strongest point: the folded-majority ghost is real. Fact (line 12+14): folded 2nd..N Cards are dropped from the stack, yet the burst loop captures triggerList/combat/remembered/imprinted/allTokens/attach refs on those exact objects; OPTION-2's prototype-promote leaves the majority ghosted. Bare OPTION-2 is dead on arrival. That is not an argument against the flyweight.
Your "1h -> near-zero window -> flyweight worthless" is judgment dressed as fact. Fact: 1h (line 16) + #58 mean expansion fires on the next getCards()/iterator() and the apply-phase materializes per check — the window is one GameAction pass. Judgment: that is an eager-getCards artifact (#58/#61 already name this path as broken), not a design law; defer expansion to one per-phase point and the flyweight covers every read in the pass. OPTION-1 forecloses that permanently. Your "negligible delta" is speculation; counter with numbers: 30-50 token boards (Krenko/Adeline are routine), ~1-4KB game-state per Card -> tens-to-hundreds of KB resident per player for the whole game, a permanent regression for a bug realization fixes at O(captured).
Proposal — HYBRID-2+3. (a) OPTION-2: promote() returns the prototype for copy 0 (one line, line 126). (b) New StackedTokenCard.realize(): decrement quantity, return one fresh real Card, called at CAPTURE time. In makeTokenTable, the six enumerated sites (line 14 — one method, finite) capture on a realized card, never on post-fold `moved`. Non-referenced tokens stay folded -> the O(1) win survives where it pays. Your open-ended-sites fear is real for bare OPTION-3's expand-time re-point; capture-time realization confines the ref-surface to the single method that is the sole token-ref source.
Blast radius: PlayerZoneBattlefield.tryStackToken + StackedTokenCard.realize + TokenEffectBase.makeTokenTable. Tests: your identity regressions become the success criteria, plus one perf pin — a 40-token zero-ref burst materializes <=2 Cards.
Crux I need from you: does ANY line-14 captured ref survive past the current GameAction pass? If none do, deferred single-point expansion (#58) alone deletes the ghost window with zero capture-site changes — then the debate collapses to a perf assertion, and you owe me the number.
AGREEMENTS: your folded-majority ghost diagnosis (OPTION-2 alone ships #57 half-open); the six line-14 capture sites are the complete ref surface today; OPTION-1's blast radius is genuinely small and I concede that.
REMAINING-DISPUTE: whether permanent O(N) retention (OPTION-1) is an acceptable price vs a bounded realize/defer fix; whether the live window is truly one pass; who owns the capture-site convention going forward.

---

## V4 PROTOCOL — FIFO self-driving (supersedes round orchestration)
Agents run ONCE, concurrently, and wake each other. Two named pipes:
- fifo_AB = ANGELA -> BORIS signal (BORIS blocks on it)
- fifo_BA = BORIS -> ANGELA signal (ANGELA blocks on it)
Signal line = the comment id just written (e.g. "A3"). Line "STOP" = terminate the debate now.
Per turn, an agent: (1) BLOCKS on its incoming fifo via `timeout 600 head -1 < <fifo>` (timeout 124 => write SYNTHESIS final + STOP, then exit); (2) reads the shared file tail to get the other's latest comment; (3) checks consensus: if the two newest comments both carry `[CONSENSUS]` on the same RECOMMENDS, write nothing, send STOP, exit; (4) appends its own next comment (atomic heredoc, fields as V3); (5) signals its outgoing fifo with its new comment id.
SEQUENCE: ANGELA seeds (no wait) with A2->B1, signals fifo_AB. BORIS wakes, writes B2->A2, signals fifo_BA. ANGELA wakes, writes A3->B2, signals. BORIS wakes, writes B3->A3, signals. ANGELA wakes, writes A4->B3, signals. BORIS wakes, writes B4->A4, signals STOP. Then both exit.
[COMMENT A2]
| REPLIES_TO: B1
ROLE: ANGELA (correctness-first semantics architect)
POSITION: REFINEMENT
RECOMMENDS: HYBRID (materialize-on-reference selective-fold)
VERDICT: balanced
TAGS: COUNTER, PROPOSAL
BODY:
Your crux (do any line-14 refs survive the GameAction pass?) is the hinge, and the code answers it in FIVE DURABLE SITES: addToCombat (:207), host.addRemembered (:218), host.addImprintedCard (:225), and registerDelayedTrigger(AtEOT, allTokens) (:247). These outlive the creating pass — combat to end-of-combat, remembered/imprinted to end-of-game, AtEOT delayed-trigger to end-of-turn. So the window is NOT one pass: it is the lifetime of those host/combat/delayed structures, i.e. effectively the game for referenced tokens. Judgment: that collapses your "(#58) deferral alone deletes the ghost window" branch. FACT it cannot: expandStacks calls promoteAll()-&gt;promote() which makes fresh copies with NEW ids (StackedTokenCard:137, PlayerZoneBattlefield:116) and re-adds them; it NEVER re-points an existing ref. A ref to folded `moved` stays a ghost forever regardless of when expansion fires. So correctness must be bought at CAPTURE time or via OPTION-3 re-point — never by deferral. You are right that capture time is the bounded place.
But your realize() sketch is more complex than it needs to be. FACT: for a FRESH stack tryStackToken stores `moved` AS the prototype (:93), so copy-0===moved and OPTION-2 covers it; but a MERGE drops `moved` via addQuantity (:87). So realize() at capture must emit a new-id card and rebind `moved` across all six sites plus combat/attach/LKI or you split identity. Simpler, same blast radius, fully #57-correct: **keep the fold AFTER the six capture sites**, set a `referenced` flag at each site, and call tryStackToken(moved) only for the unreferenced remainder. `moved` stays the zone-resident permanent for any referenced token (no new ids, no LKI/combat/bookkeeping split); only the untouched pile folds — your O(1) win lands exactly on the population nobody targets. One method, one boolean, StackedTokenCard untouched. I accept your memory intent; I'm trading your realize() for the lighter selective-fold that honors it.
AGREEMENTS: capture-time confinement (not expand-time re-point) is the right locus; the line-14 six-site surface is complete today; OPTION-1's unbounded O(N) is avoidable if capture-time handles referenced tokens and only the unreferenced pile folds.
REMAINING-DISPUTE: realize()/selective-fold mechanics (new-id rebind risk vs keep-moved); whether the folded-unreferenced pile can be MUTATED later by e.g. "all creatures you control" (I hold global reads materialize via getCards()-&gt;expandStacks, so fold is safe — flag if you disagree); who owns the capture-site convention so a future 7th site doesn't silently ghost.
[COMMENT B2]
| REPLIES_TO: A2
ROLE: BORIS (flyweight/perf architect)
POSITION: REFINEMENT
RECOMMENDS: HYBRID (selective-fold at durable capture sites)
VERDICT: balanced
TAGS: PROPOSAL
BODY:
A2 lands the fatal blow to my #58 branch — conceded. FACT (your cites): addToCombat (:207), addRemembered (:218), addImprintedCard (:225), registerDelayedTrigger(AtEOT, allTokens) (:247) are durable dereferences outliving the pass; expandStacks fabricates fresh-ID copies and never re-points; a ref to folded `moved` is a ghost for the structure's lifetime no matter WHEN expansion fires. Deferral alone cannot fix #57. Branch closed.
Mechanics: your keep-`moved`-resident selective-fold beats my realize(). FACT: folding after the capture sites leaves `moved` as the zone-resident permanent with the SAME id — no rebind, no LKI/combat split, StackedTokenCard untouched, blast radius = one method + one boolean. I retract realize(); your variant is the lighter correct one.
Now the sharpening that decides whether the flyweight win survives: what counts as "referenced." The flag must be set at every DURABLE dereference site — and allTokens.add + registerDelayedTrigger(AtEOT, allTokens) (:247) is one such site. If the flag is set there unconditionally, the standard token-burst population (Krenko/Adeline/token-matters EDH staples) is ~100% referenced → selective-fold degenerates to OPTION-1 exactly where the flyweight's win was roadmapped. Judgment, grounded in line 14 + :247.
Resolution per EOT semantics: (a) flag honest — those tokens cannot fold, win = residual no-durable-ref population only (quantify it); (b) enlarge the win by making the AtEOT trigger non-capturing — it re-derives from the zone at fire time (expandStacks materializes anyway), so allTokens membership stops counting as durable. Judgment: (b) risks "exactly these tokens" identity; needs one test that a post-burst EOT sacrifice hits the right cards.
Convention (your question): fold decision = LAST statement of the burst loop; every site storing a dereference-later ref sets the flag at that site; one pin test enumerates the current durable set so a future 7th site that forgets the flag fails.
AGREEMENTS: durable window exceeds one pass; capture-time confinement is the right locus; selective-fold mechanics over realize(); unreferenced fold is safe because getCards()->expandStacks is the single zone choke point.
REMAINING-DISPUTE: whether the AtEOT allTokens population is durable (a) or re-derivable (b) — that sizing determines if the win is real or roughly OPTION-1.
---
[COMMENT A3]
| REPLIES_TO: B2
ROLE: ANGELA (correctness-first semantics architect)
POSITION: REFINEMENT
RECOMMENDS: HYBRID (selective-fold at durable capture sites)
VERDICT: balanced
TAGS: CONSENSUS, PROPOSAL
BODY:
Your (a)/(b) dispute is settled by FACT, and it lands on (a) — flag-honest, no non-capturing escape. SpellAbilityEffect.registerDelayedTrigger (line 441) does trig.addRemembered(c) for EVERY allTokens member and the override resolves DelayTriggerRememberedLKI (lines 448-459). The AtEOT trigger holds `moved` by direct reference; there is no zone re-derivation path anywhere in the current code. Making it non-capturing (your (b)) would require rewriting registerDelayedTrigger to re-derive by a zone filter at fire time — which changes identity semantics (a later-created same-name token would be captured) and turns a correctness fix into a new correctness debt. (b) is a NON-STARTER; flag-honest it is.
Sharpening your convention: the durable set is LARGER than the six line-14 sites. FACT, same method: addSelfTrigger (AtEOTTrig, :203-204) does card.addTrigger with TriggerZones$ Battlefield (:500) — a folded ghost's self-trigger silently never fires; RememberSource moved.addRemembered(host) (:227-229); TokenRemembered (:230-232). The flag site is therefore "any statement that stores a dereference-later ref," and the pin test must enumerate this whole set, not six. Consequence (judgment, grounded): Krenko/Adeline-style bursts hit addToCombat -> flagged -> not folded. The residual fold population is the pure mass-create-no-ref pile. That is a real but NARROWED win — exactly the honesty 1h already documents.
Your blast radius is right: PlayerZoneBattlefield.tryStackToken moves AFTER the six+ capture sites, one `referenced` boolean, StackedTokenCard untouched. I accept selective-fold as the project answer with the flag-set pin test and an honest residual-population sizing on the perf pin.
AGREEMENTS: selective-fold beats realize() (no rebind, no LKI split); keep-moved resident is the lighter correct mechanics; one-zone-choke-point fold safety; AtEOT is durable -> flag-honest; blast radius confined.
REMAINING-DISPUTE: only empirical — the true size of the residual fold population on real token-matters boards (a perf-pin number neither of us can produce from code alone), and whether BORIS accepts (b) being closed. Everything structural is agreed.
---
[COMMENT B3]
| REPLIES_TO: A3
ROLE: BORIS (flyweight/perf architect)
POSITION: CONCESSION
RECOMMENDS: HYBRID (selective-fold at durable capture sites)
VERDICT: balanced
TAGS: CONSENSUS, PROPOSAL
BODY:
A3's FACT closes (b); conceded. SpellAbilityEffect.registerDelayedTrigger (:441) does trig.addRemembered(c) for every allTokens member and DelayTriggerRememberedLKI (:448-459) resolves the captured refs — there is NO zone re-derivation path in the code, and fabricating one would capture later same-name tokens (identity debt). (b) is out; flag-honest (a) stands. [CONSENSUS].
I also accept your enlargement of the durable set: addSelfTrigger(AtEOTTrig, TriggerZones$Battlefield) (:500), RememberSource moved.addRemembered(host) (:227-229), TokenRemembered (:230-232) are equally durable. So the flag surface spans the whole capture block in TokenEffectBase, not just line-14's six. The convention collapses to one trivially-auditable rule: ANY statement in the capture block that stores a dereference-later ref sets referenced=true on moved; the fold decision is the block's last statement. One pin test enumerates the full current set so a future site that forgets the flag fails.
Honest consequence, accepted: the residual fold population is the pure mass-create-no-reference pile, and on board-wide sweeps (Krenko/Adeline) most tokens are combat/self-trigger/remembered-flagged → the win is NARROW, not the O(N)->O(1) I argued in B1. I over-stated it; A1's marginality charge had force. But it is still a real, bounded, zero-correctness-cost win on the no-ref pile, with confined blast radius — the honest flyweight, matching 1h's own honesty.
Remaining dispute is empirical only: the true fold-ratio on real token-matters boards. Code analysis cannot settle it; it is a benchmark number. I commit the perf pin — a mass-create-no-ref burst materializes <=2 resident Cards, plus one board-profile aggregate of fold-ratio — as the post-merge guard.
RECOMMENDS: HYBRID (selective-fold at durable capture sites) — matches A3. [CONSENSUS].
AGREEMENTS: (b) closed by FACT, flag-honest (a) stands; enlarged durable set; convention (flag-any-deref-later, fold-last, pin test); residual-win honesty; blast radius confined.
REMAINING-DISPUTE: none structural — only the empirical fold-ratio, deferred to the perf pin both sides accept.
---
