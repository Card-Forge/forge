#!/usr/bin/env python3
"""Deterministic linter for Forge card scripts.

Checks a card script for mechanical issues: dead references, duplicate params,
orphaned SVars, malformed costs/filters, lexical typos, literal names in
descriptions, loyalty-ability hints, near-miss param-key typos, unknown params
absent from the whole corpus (UNKNOWN-KEY), a missing ManaCost on a non-Land card
(NO-MANACOST), and trigger-only tokens used outside a trigger. It does not check
value accuracy (P/T, cost VALUE, may-vs-mandatory all need the real card) or what
an ability actually does. Output: file:line: [ERROR|WARN] CODE msg.

Usage:  python3 cardlint.py [--corpus <cardsfolder>] <card.txt> [more.txt ...]
        --corpus enables the KEY near-miss check (defaults to auto-detected
        forge-gui/res/cardsfolder near cwd; skipped if not found).
Exit:   1 if any findings, else 0.
"""
import re, sys, os, json, time, tempfile, hashlib

KEY_TOKEN = re.compile(r"([A-Za-z][A-Za-z0-9]*)\$")  # a `key$` param token

LINE_PREFIXES = {"Name","ManaCost","Types","PT","Loyalty","Defense","Colors","Text",
    "Oracle","K","A","T","S","R","SVar","AI","DeckHints","DeckNeeds","DeckHas",
    "AlternateMode","Variant","ALTERNATE","SetColor"}
PFX_LOWER = {p.lower():p for p in LINE_PREFIXES}
# The sub-ability keys in the engine's additionalAbilityKeys are validated against
# defined SVars by the build's Java test; only the keys outside that list stay here.
REF_KEYS = {"Execute","SubAbility","AbilityX","ChosenSubAbility"}
LIST_REF_KEYS = {"Choices"}
AMP_LIST_KEYS = {"AddTypes","AddKeyword","AddKeywords","RemoveKeywords",
                 "AddTrigger","AddStatic","AddReplacement","Triggers"}
VALID_KEYS = {"ValidTgts","ValidCard","ValidCards","Valid","Affected","ValidSource",
              "ValidTarget","ChangeType","ChangeValid","SacValid","ValidCause"}
DESC_KEYS = {"SpellDescription","TriggerDescription","StackDescription","Description"}
SPACED_KEYS = DESC_KEYS | {"Name"}
CANON = {"self":"Self","targeted":"Targeted","remembered":"Remembered",
         "imprinted":"Imprinted","battlefield":"Battlefield","exile":"Exile",
         "graveyard":"Graveyard","hand":"Hand","library":"Library",
         "command":"Command","stack":"Stack"}
TRIG_TOKEN = re.compile(r"\bTriggered[A-Za-z]+\b")  # trigger-context-only references

def split_params(body):
    out=[]
    for piece in re.split(r"\s*\|\s*", body.strip()):
        if not piece: continue
        if "$" in piece:
            k,v=piece.split("$",1); out.append((k.strip(),v.strip(),piece))
        else: out.append((piece.strip(),None,piece))
    return out

def check_mana(cost,i,add):
    # Lexical only: hybrids (WB, RW, 2R, U4, W/P) are legal and indistinguishable
    # from a typo like 'R2' by form -- mana-cost VALUE accuracy needs the real card.
    if cost.lower() in ("no cost","nocost",""): return
    for tok in cost.split():
        if not re.fullmatch(r"[0-9WUBRGCSXYP/]+",tok):
            add(i,"ERROR","MANA",f"illegal character in mana token '{tok}' in ManaCost '{cost}'")

def edit1(a,b):
    """True iff a and b differ by exactly one edit (sub / ins / del)."""
    if a==b: return False
    la,lb=len(a),len(b)
    if abs(la-lb)>1: return False
    if la==lb:
        return sum(1 for x,y in zip(a,b) if x!=y)==1
    if la>lb: a,b,la,lb=b,a,lb,la   # a is the shorter
    i=0
    while i<la and a[i]==b[i]: i+=1
    return a[i:]==b[i+1:]

def find_corpus():
    d=os.getcwd()
    for _ in range(8):
        c=os.path.join(d,"forge-gui","res","cardsfolder")
        if os.path.isdir(c): return c
        nd=os.path.dirname(d)
        if nd==d: break
        d=nd
    return None

def key_freq(corpus):
    """Frequency of every `key$` token across the corpus (cached 7 days in temp)."""
    if not corpus or not os.path.isdir(corpus): return None
    cache=os.path.join(tempfile.gettempdir(),
                       "cardlint_keyfreq_"+hashlib.md5(corpus.encode()).hexdigest()[:8]+".json")
    try:
        if os.path.exists(cache) and (os.path.getmtime(cache) > time.time()-7*86400):
            return json.load(open(cache,encoding="utf-8"))
    except Exception: pass
    freq={}
    for root,_,files in os.walk(corpus):
        for fn in files:
            if not fn.endswith(".txt"): continue
            try: txt=open(os.path.join(root,fn),encoding="utf-8",errors="ignore").read()
            except Exception: continue
            for k in KEY_TOKEN.findall(txt):
                freq[k]=freq.get(k,0)+1
    try: json.dump(freq,open(cache,"w",encoding="utf-8"))
    except Exception: pass
    return freq

def lint(path, freq=None):
    F=[]; add=lambda ln,s,c,m:F.append((ln,s,c,m))
    text=open(path,encoding="utf-8").read(); lines=text.split("\n")
    nick=""; defined={}
    for i,l in enumerate(lines,1):
        if l.startswith("Name:"): nick=l[5:].strip().split(",")[0].strip()
        if l.startswith("SVar:"):
            p=l.split(":",2)
            if len(p)==3: defined[p[1]]=(i,p[2])
    def wordcount(w): return len(re.findall(rf"\b{re.escape(w)}\b",text))
    bare=lambda s: re.fullmatch(r"[A-Za-z0-9_]+",s) is not None
    # candidate "known" keys to suggest against (frequent in the corpus)
    known=[k for k,n in (freq or {}).items() if n>=50] if freq else []
    # case-insensitive set of every corpus key (param map is case-insensitive,
    # so a case-only difference like PreCostDesc vs PrecostDesc is harmless)
    freq_lower=set(k.lower() for k in (freq or {}))

    for i,l in enumerate(lines,1):
        if l in ("","ALTERNATE"): continue
        m=re.match(r"([A-Za-z]+)(::?)",l)
        if m:
            pfx,col=m.group(1),m.group(2)
            if col=="::": add(i,"ERROR","LEX-PREFIX",f"double-colon prefix '{pfx}::'")
            elif pfx not in LINE_PREFIXES and pfx.lower() in PFX_LOWER:
                add(i,"ERROR","LEX-PREFIX",f"miscased prefix '{pfx}:' (want '{PFX_LOWER[pfx.lower()]}:')")
        if "’" in l: add(i,"WARN","LEX-CURLY","curly apostrophe U+2019 (use ASCII apostrophe)")
        if not re.match(r"(DeckHas|DeckHints|DeckNeeds):",l) and (re.search(r"\S\|",l) or re.search(r"\|\S",l)):
            add(i,"WARN","LEX-PIPE","'|' separator missing a surrounding space")
        if "$  " in l: add(i,"WARN","LEX-DBLSPACE","double space after '$'")

        if l.startswith("K:Chapter:"):
            cps=l.split(":")
            if len(cps)>=4:
                for nm in cps[3].split(","):
                    nm=nm.strip()
                    if nm and nm not in defined:
                        add(i,"ERROR","REF-UNDEF",f"Chapter ability '{nm}' is not a defined SVar")

        is_a = l.startswith("A:")
        body=None
        if l.startswith("SVar:"):
            p=l.split(":",2); body=p[2] if len(p)==3 else None
        elif re.match(r"(A|T|S|R):",l): body=l.split(":",1)[1]
        if body is None:
            if l.startswith("ManaCost:"): check_mana(l[9:].strip(),i,add)
            continue

        seen={}; desc=""
        for k,v,raw in split_params(body):
            if k in seen:
                add(i,"ERROR","DUP-PARAM",f"duplicate '{k}$' (engine keeps last '{v}', drops '{seen[k]}')")
            seen[k]=v
            # param-key sanity: a key$ token absent from the whole corpus (the param
            # map is case-insensitive, so a case-only difference is harmless). A
            # near-miss of a frequent key is a typo (ERROR); no near-miss at all is an
            # unknown param the engine silently ignores (WARN). Guard on v so a bare
            # SVar value token (e.g. `PlayMain1:TRUE`) isn't mistaken for a key.
            if (freq is not None and v is not None and bare(k) and len(k)>=4
                    and freq.get(k,0)==0 and k.lower() not in freq_lower):
                cand=[kk for kk in known if edit1(k,kk)]
                if cand:
                    best=max(cand,key=lambda kk:freq[kk])
                    add(i,"ERROR","KEY-TYPO",f"'{k}$' not in corpus; did you mean '{best}$' (freq {freq[best]})?")
                else:
                    add(i,"WARN","UNKNOWN-KEY",f"'{k}$' appears in no other card — engine silently ignores unknown params (typo, outdated, or made-up?). NB: a param valid for a DIFFERENT API still slips past this.")
            if v is None: continue
            if k in REF_KEYS and bare(v) and v not in defined:
                note=("card FAILS TO LOAD - hard RuntimeException at parse (Trigger.ensureAbility)"
                      if k=="Execute" else "clause silently does nothing")
                add(i,"ERROR","REF-UNDEF",f"{k}$ '{v}' undefined SVar - {note}")
            if k in LIST_REF_KEYS:
                for nm in v.split(","):
                    nm=nm.strip()
                    if nm and bare(nm) and nm not in defined:
                        add(i,"ERROR","REF-UNDEF",f"{k}$ choice '{nm}' undefined")
            if k in SPACED_KEYS and raw[len(k)+1:len(k)+2] not in (" ",""):
                add(i,"ERROR","LEX-NOSPACE",f"'{k}$' not followed by a space")
            if k in AMP_LIST_KEYS and "," in v:
                add(i,"ERROR","LEX-DELIM",f"{k}$ uses ',' but engine splits on ' & ' -> becomes one bogus entry")
            if k in VALID_KEYS:
                for tok in re.split(r"[ ,]",v):
                    if tok.count(".")>=2:
                        add(i,"ERROR","LEX-FILTER",f"{k}$ filter '{tok}' has multiple '.' (malformed property; matches nothing)")
            head=v.split()[0] if v.split() else ""
            if k in ("Defined","Origin","Destination") and head.lower() in CANON and head!=CANON[head.lower()]:
                add(i,"ERROR","CASE",f"{k}$ '{head}' miscased (want '{CANON[head.lower()]}', engine is case-sensitive)")
            if k in DESC_KEYS: desc+=" "+v
            if k=="SpellDescription" and re.match(r"\{[^}]+\}:",v.strip()):
                add(i,"WARN","DESC-COST","SpellDescription restates the activation cost ('{..}:')")
        if desc and nick and len(nick)>=3 and re.search(rf"\b{re.escape(nick)}\b",desc):
            add(i,"WARN","DESC-NAME",f"literal '{nick}' in description (use NICKNAME/CARDNAME)")
        if "LOYALTY" in (seen.get("Cost") or "") and seen.get("Planeswalker")!="True":
            add(i,"ERROR","LOYALTY","loyalty-cost ability lacks 'Planeswalker$ True'")
        # trigger-only tokens (TriggeredSource*, TriggeredTarget*, ...) never resolve
        # in a directly-activated A: ability -- they belong in a trigger's SVar.
        if is_a:
            for tok in sorted(set(TRIG_TOKEN.findall(body))):
                add(i,"ERROR","TRIG-CTX",f"trigger-only token '{tok}' on an A: line — belongs in a trigger's SVar (an A: ability has no triggering context)")
        # NOTE: a controller<->description target check is intentionally absent --
        # it is too noisy to be a reliable lexical rule.

    # every non-Land card needs a ManaCost (lands have none; DFC/back faces under
    # ALTERNATE are exempt -- the front face always carries the cost). 0 FP across
    # the corpus.
    if "ALTERNATE" not in text:
        types=next((l[6:] for l in lines if l.startswith("Types:")),None)
        if (types is not None and "Land" not in types
                and not any(l.startswith("ManaCost:") for l in lines)):
            ln=next((j for j,l in enumerate(lines,1) if l.startswith("Name:")),1)
            add(ln,"ERROR","NO-MANACOST","card has no ManaCost line (non-Land cards need one)")

    for nm,(ln,bdy) in defined.items():
        if re.match(r"\s*(DB|AB|SP)\$",bdy) and wordcount(nm)<=1:
            add(ln,"WARN","ORPHAN",f"SVar '{nm}' never referenced (clause never fires)")

    base=os.path.basename(path); F.sort()
    for ln,s,c,msg in F: print(f"{base}:{ln}: [{s}] {c} {msg}")
    return F

if __name__=="__main__":
    args=sys.argv[1:]; corpus=None
    if "--corpus" in args:
        j=args.index("--corpus"); corpus=args[j+1]; del args[j:j+2]
    if corpus is None: corpus=find_corpus()
    freq=key_freq(corpus)
    total=0
    for f in args:
        try: total+=len(lint(f,freq))
        except Exception as e: print(f"{f}: [ERROR] LINT-FAIL {e}")
    print(f"\n== {total} finding(s) ==")
    sys.exit(1 if total else 0)
