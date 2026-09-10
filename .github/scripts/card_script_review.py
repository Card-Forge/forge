#!/usr/bin/env python3
"""Review changed card scripts on a pull request and emit terse inline comments.

Runs two independent, low-risk checks on each changed card and prints a JSON list
of `{path, line, body}` comments for the workflow to post. Both are advisory.

  1. cardlint.py — a deterministic linter for the card-script DSL: undefined SVar
     references (a bad `Execute$` means the card fails to load), duplicate or
     unknown params, illegal mana tokens, a missing `ManaCost`, and similar
     mechanical issues. Its set of "known params" is derived from the card corpus
     itself, so when a param is added or renamed the corpus reflects it and the
     linter adapts on its own — nothing here is hard-coded against the game engine.

  2. Scryfall fact-check — compares only the card FRAME (name, type line, P/T, mana
     cost, loyalty) against the printed card. Catches transcription slips such as a
     missing Legendary supertype, an instant/sorcery swap, or a wrong mana symbol.
     When the card is not on Scryfall (e.g. an unreleased card) it stays silent;
     see scryfall_facts().

  3. Engine API findings (optional, --java-findings) — ability/trigger/replacement
     API names validated by the build against the engine's own enums, the same
     check the engine runs at card load. This is the one engine-coupled input, and
     only to the stable API vocabulary; it is produced by the build, not here.

This file never interprets ability semantics. It only (a) relays the linter's
structured findings and (b) diffs frame fields — both decoupled from how any
individual card is scripted, which is what keeps it stable across engine changes.
If the linter emits a finding code this file doesn't recognise, terse_comment()
falls back to the linter's own message rather than failing. Comments are kept
short, in `wrong -> right` form.

Usage:   card_script_review.py <changed_files.txt>
         (one repo-relative card path per line; non-card paths are ignored)
Output:  JSON array of {path, line, body} on stdout; a human summary on stderr.
"""
import sys, os, io, re, json, time, collections, contextlib
import urllib.parse, urllib.request, urllib.error

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import cardlint  # noqa: E402  (sibling module, committed alongside this script)

ARROW = "→"  # → , so the comments read naturally in the GitHub UI
SCRYFALL = "https://api.scryfall.com/cards/named"

# Scryfall asks for ~100 ms between requests. We pace every call and, on the first
# 429, stop calling for the rest of the run so a big PR can't storm the API and
# silently turn every card into "not found". The skip is logged, not hidden.
_MIN_INTERVAL = 0.1
_last_call = [0.0]
_rate_limited = [False]


def _throttle():
    dt = time.time() - _last_call[0]
    if dt < _MIN_INTERVAL:
        time.sleep(_MIN_INTERVAL - dt)
    _last_call[0] = time.time()


# --------------------------------------------------------------------------- #
# 1. Linter findings -> terse comments
# --------------------------------------------------------------------------- #
def terse_comment(code, msg):
    """Render one cardlint finding as a terse PR comment.

    Falls back to the linter's own message for any code it doesn't special-case,
    so a new check added to the linter never breaks this script — the comment just
    reads a little more verbosely until someone adds a template here.
    """
    def grab(pat):
        m = re.search(pat, msg)
        return m.group(1) if m else None

    if code == "REF-UNDEF":
        m = re.match(r"(\w+\$) '([^']+)' undefined SVar", msg)
        if m:
            key, name = m.group(1), m.group(2)
            why = "card won't load" if key.startswith("Execute") else "clause dropped"
            return f"`{key} {name}` {ARROW} undefined SVar ({why})"
    elif code == "KEY-TYPO":
        m = re.match(r"'(\w+)\$' not in corpus; did you mean '(\w+)\$'", msg)
        if m:
            return f"`{m.group(1)}$` {ARROW} `{m.group(2)}$`"
    elif code == "UNKNOWN-KEY":
        k = grab(r"'(\w+)\$'")
        if k:
            return f"`{k}$` {ARROW} appears in no other card (unknown or outdated param?)"
    elif code == "DUP-PARAM":
        k = grab(r"duplicate '(\w+)\$'")
        if k:
            return f"duplicate `{k}$` {ARROW} engine keeps only the last"
    elif code == "NO-MANACOST":
        return "missing a `ManaCost` line"
    elif code == "LOYALTY":
        return "loyalty ability needs `Planeswalker$ True`"
    elif code == "TRIG-CTX":
        t = grab(r"'(\w+)'")
        if t:
            return f"`{t}` is a trigger-only token on an `A:` line (no triggering context)"
    elif code == "LEX-DELIM":
        k = grab(r"(\w+)\$")
        if k:
            return f"`{k}$` is comma-separated, but the engine splits on ` & `"
    elif code == "LEX-FILTER":
        t = grab(r"filter '([^']+)'")
        if t:
            return f"`{t}` has multiple `.` {ARROW} matches nothing"
    elif code == "DESC-NAME":
        t = grab(r"literal '([^']+)'")
        if t:
            return f"literal `{t}` in the description {ARROW} use NICKNAME / CARDNAME"
    elif code == "DESC-COST":
        return "SpellDescription restates the activation cost"
    elif code == "ORPHAN":
        t = grab(r"SVar '([^']+)'")
        if t:
            return f"SVar `{t}` is never referenced (clause never fires)"
    elif code == "CASE":
        m = re.match(r"(\w+)\$ '([^']+)' miscased \(want '([^']+)'", msg)
        if m:
            return f"`{m.group(1)}$ {m.group(2)}` {ARROW} `{m.group(3)}` (case-sensitive)"
    elif code == "LEX-PIPE":
        return "add a space around the `|` separator"
    elif code == "LEX-CURLY":
        return f"curly apostrophe `’` {ARROW} ASCII `'`"
    elif code == "MANA":
        t = grab(r"token '([^']+)'")
        if t:
            return f"illegal mana token `{t}`"
    elif code == "API-UNKNOWN":
        m = re.match(r"(\w+\$) '([^']+)' is not a known (.+)", msg)
        if m:
            return f"`{m.group(1)} {m.group(2)}` {ARROW} unknown {m.group(3)}"
    # default: the linter message is already human-readable
    return msg


def lint_comments(path, freq):
    """Run the linter on one card and return [(line, body), ...]."""
    sink = io.StringIO()
    with contextlib.redirect_stdout(sink):       # lint() also prints; we want the data
        findings = cardlint.lint(path, freq)
    out = []
    for line, sev, code, msg in findings:
        out.append((line, terse_comment(code, msg)))
    return out


# --------------------------------------------------------------------------- #
# 2. Scryfall frame fact-check
# --------------------------------------------------------------------------- #
def read_frame(path):
    """Pull the front face's frame fields and the line each sits on.

    Stops at the `ALTERNATE` separator so a multi-section file (DFC, meld, flip,
    split, adventure) yields only the FRONT face's frame -- otherwise last-wins
    parsing builds a frankenframe from both faces and produces false diffs.
    """
    frame = {}
    for i, raw in enumerate(open(path, encoding="utf-8", errors="ignore").read().split("\n"), 1):
        if raw.strip() == "ALTERNATE":
            break
        for field in ("Name", "ManaCost", "Types", "PT", "Loyalty"):
            if raw.startswith(field + ":"):
                frame[field] = (i, raw[len(field) + 1:].strip())
    return frame


def scryfall_lookup(name):
    """Return the Scryfall card dict, or None if the card isn't indexed.

    Tries an EXACT name match first (no false matches). Only if that misses does
    it fall back to a fuzzy match, and then accepts the result solely when the
    returned name is within a small edit distance of ours — i.e. the same card
    with a transcription typo. Anything farther away is treated as "not on
    Scryfall" and the check stays silent (e.g. an unreleased card).
    """
    def get(params):
        if _rate_limited[0]:
            return None
        _throttle()
        url = SCRYFALL + "?" + urllib.parse.urlencode(params)
        req = urllib.request.Request(url, headers={
            "User-Agent": "ForgeCardScriptReviewBot/1.0 (+github-actions)",
            "Accept": "application/json"})
        try:
            with urllib.request.urlopen(req, timeout=20) as r:
                return json.load(r)
        except urllib.error.HTTPError as e:
            if e.code == 429 and not _rate_limited[0]:
                _rate_limited[0] = True
                print("Scryfall rate-limited (429) — skipping remaining fact-checks "
                      "this run", file=sys.stderr)
            return None          # 404 = not found, 422 = ambiguous fuzzy, etc.
        except Exception:
            return None          # network hiccup -> stay silent, never block

    card = get({"exact": name})
    if card:
        return card
    card = get({"fuzzy": name})
    if card and 0 < _edit_distance(name.lower(), card.get("name", "").lower()) <= 2:
        return card             # same card, name has a small typo
    return None


def _edit_distance(a, b, cap=3):
    """Levenshtein distance, capped (we only care about 'small')."""
    if abs(len(a) - len(b)) > cap:
        return cap + 1
    prev = list(range(len(b) + 1))
    for i, ca in enumerate(a, 1):
        cur = [i]
        for j, cb in enumerate(b, 1):
            cur.append(min(prev[j] + 1, cur[-1] + 1, prev[j - 1] + (ca != cb)))
        prev = cur
        if min(prev) > cap:
            return cap + 1
    return prev[-1]


def _mana_to_forge(scryfall_cost):
    """`{2}{W/U}{R}` -> `2 WU R`; phyrexian `{G/P}` stays `G/P`. For display+compare."""
    out = []
    for sym in re.findall(r"\{([^}]+)\}", scryfall_cost or ""):
        if "/" in sym and "P" not in sym.upper():
            out.append(sym.replace("/", ""))   # two-colour hybrid: W/U -> WU
        else:
            out.append(sym)
    return " ".join(out)


def _norm_mana(s):
    """Order-independent, punctuation-independent mana comparison key."""
    return sorted(re.sub(r"[^A-Za-z0-9]", "", t).upper() for t in s.split() if t)


def scryfall_facts(path):
    """Compare the card frame against Scryfall. Returns [(line, body), ...].

    Silent (returns []) when the card isn't on Scryfall, e.g. an unreleased card.
    Only the stable frame fields are compared, so this never needs to know how an
    ability is scripted.
    """
    frame = read_frame(path)
    if "Name" not in frame:
        return []
    name_line, name = frame["Name"]
    card = scryfall_lookup(name)
    if not card:
        return []
    # Multi-faced cards (DFC/MDFC/split/adventure) keep their frame and text under
    # `card_faces`; a single Forge frame line can't be compared cleanly, so skip
    # them rather than emit false diffs.
    if card.get("card_faces"):
        return []

    out = []

    # Name (only reachable via the fuzzy path, i.e. a real typo)
    real_name = card.get("name", "")
    if real_name and real_name != name:
        out.append((name_line, f"Name `{name}` {ARROW} `{real_name}`"))

    # Type line — compare token SETS so harmless ordering differences don't flag,
    # but a missing supertype (e.g. Legendary) or instant/sorcery swap does.
    if "Types" in frame:
        line, ours = frame["Types"]
        theirs = card.get("type_line", "").replace("—", " ")
        if theirs and set(ours.split()) != set(theirs.split()):
            out.append((line, f"Types `{ours}` {ARROW} `{' '.join(theirs.split())}`"))

    # Power/Toughness — only when both are present and plainly differ.
    if "PT" in frame and card.get("power") is not None:
        line, ours = frame["PT"]
        theirs = f"{card.get('power')}/{card.get('toughness')}"
        if "/" in ours and ours != theirs:
            out.append((line, f"PT `{ours}` {ARROW} `{theirs}`"))

    # Mana cost — order/punctuation-independent compare; show Forge-style suggestion.
    if "ManaCost" in frame and card.get("mana_cost"):
        line, ours = frame["ManaCost"]
        theirs = _mana_to_forge(card["mana_cost"])
        if ours.lower() not in ("no cost", "") and _norm_mana(ours) != _norm_mana(theirs):
            out.append((line, f"ManaCost `{ours}` {ARROW} `{theirs}`"))

    # Loyalty / starting loyalty
    if "Loyalty" in frame and card.get("loyalty"):
        line, ours = frame["Loyalty"]
        if ours != str(card["loyalty"]):
            out.append((line, f"Loyalty `{ours}` {ARROW} `{card['loyalty']}`"))

    return out


# --------------------------------------------------------------------------- #
# main
# --------------------------------------------------------------------------- #
def java_findings_comments(path, changed):
    """Load engine-validated API findings emitted by the build, scoped to changed cards.

    The build's Java test validates every card's ability/trigger/replacement API
    names against the engine's own enums and writes {path, line, code, body}. We
    only keep findings on cards this PR changed; the workflow further scopes them
    to the diff lines before posting.
    """
    try:
        items = json.load(open(path, encoding="utf-8"))
    except Exception as e:
        print(f"no java findings ({e})", file=sys.stderr)
        return []
    out = []
    for f in items:
        p = f.get("path", "").replace("\\", "/")
        if p in changed:
            out.append({"path": p, "line": f["line"],
                        "body": terse_comment(f.get("code", ""), f.get("body", ""))})
    return out


def main():
    args = sys.argv[1:]
    java_path = None
    if "--java-findings" in args:
        i = args.index("--java-findings")
        java_path = args[i + 1]
        del args[i:i + 2]
    if not args:
        print("usage: card_script_review.py [--java-findings <file>] <changed_files.txt>",
              file=sys.stderr)
        return 2
    paths = [l.strip() for l in open(args[0], encoding="utf-8") if l.strip()]
    cards = [p for p in paths if p.endswith(".txt")
             and "cardsfolder" in p.replace("\\", "/") and os.path.exists(p)]

    # The workflow materializes the PR's cards into the corpus, so a freshly
    # computed key_freq counts them too. Subtract each reviewed card's own param
    # counts so a typo'd or made-up param present only in the reviewed card isn't
    # self-counted as "known" (which would silence KEY-TYPO / UNKNOWN-KEY).
    freq = dict(cardlint.key_freq(cardlint.find_corpus()) or {})
    for path in cards:
        text = open(path, encoding="utf-8", errors="ignore").read()
        for k, n in collections.Counter(cardlint.KEY_TOKEN.findall(text)).items():
            if k in freq:
                freq[k] -= n
                if freq[k] <= 0:
                    del freq[k]
    comments = []
    for path in cards:
        found = []
        try:
            found += lint_comments(path, freq)
        except Exception as e:                         # never let one bad card abort the run
            print(f"lint error on {path}: {e}", file=sys.stderr)
        try:
            found += scryfall_facts(path)
        except Exception as e:
            print(f"scryfall error on {path}: {e}", file=sys.stderr)
        for line, body in found:
            comments.append({"path": path.replace("\\", "/"), "line": line, "body": body})

    if java_path:
        comments += java_findings_comments(java_path, set(c.replace("\\", "/") for c in cards))

    json.dump(comments, sys.stdout, ensure_ascii=False, indent=2)
    print(f"\n{len(comments)} comment(s) across {len(cards)} card(s)", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
