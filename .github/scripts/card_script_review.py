#!/usr/bin/env python3
"""Turn card-script findings into terse inline PR comments.

Two sources, both advisory:

  1. The Java card-script linter (CardScriptLinterTest in the build), which checks every
     script against the engine's own definitions and writes
     forge-gui-desktop/target/card-script-findings.json. Its messages are already short
     "wrong -> right" comments; this script keeps the ones on changed cards.

  2. A Scryfall fact-check of the card FRAME (name, type line, P/T, mana cost, loyalty)
     against the printed card. Catches transcription slips such as a missing Legendary
     supertype. Silent when the card isn't on Scryfall (e.g. an unreleased card).

Usage:   card_script_review.py [--findings <card-script-findings.json>] <changed_files.txt>
         (one repo-relative card path per line; non-card paths are ignored)
Output:  JSON array of {path, line, body} on stdout; a human summary on stderr.
"""
import sys, os, re, json, time
import urllib.parse, urllib.request, urllib.error

ARROW = "\u2192"  # the comments read "wrong -> right"
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


# The findings come from a build that ran PR code, so treat them as untrusted text: a
# well-formed code, a short body, and no @-mentions.
CODE = re.compile(r"[A-Z][A-Z-]{1,19}")
MAX_BODY = 300


def linter_comments(path, cards):
    """The Java linter's findings on the changed cards, as {path, line, body}."""
    try:
        items = json.load(open(path, encoding="utf-8"))
    except Exception as e:
        print(f"no linter findings ({e})", file=sys.stderr)
        return []
    out = []
    for f in items:
        p = str(f.get("path", "")).replace("\\", "/")
        body = str(f.get("body", ""))
        if p not in cards or not CODE.fullmatch(str(f.get("code", ""))) or not isinstance(f.get("line"), int):
            continue
        mark = "" if f.get("severity") == "ERROR" else "(warning) "
        body = body[:MAX_BODY].replace("@", "@\u200b")
        out.append({"path": p, "line": f["line"], "body": mark + body})
    return out


def main():
    args = sys.argv[1:]
    findings = None
    if "--findings" in args:
        i = args.index("--findings")
        findings = args[i + 1]
        del args[i:i + 2]
    if not args:
        print("usage: card_script_review.py [--findings <file>] <changed_files.txt>", file=sys.stderr)
        return 2
    paths = [l.strip().replace("\\", "/") for l in open(args[0], encoding="utf-8") if l.strip()]
    cards = [p for p in paths if p.endswith(".txt") and "cardsfolder" in p and os.path.exists(p)]

    comments = linter_comments(findings, set(cards)) if findings else []
    for path in cards:
        try:
            for line, body in scryfall_facts(path):
                comments.append({"path": path, "line": line, "body": body})
        except Exception as e:                  # never let one card abort the run
            print(f"scryfall error on {path}: {e}", file=sys.stderr)

    json.dump(comments, sys.stdout, ensure_ascii=False, indent=2)
    print(f"\n{len(comments)} comment(s) across {len(cards)} card(s)", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
