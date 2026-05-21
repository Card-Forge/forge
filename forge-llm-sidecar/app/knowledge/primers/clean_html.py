"""Turn primer HTML into LLM-ready plain text.

We do NOT try to keep visual fidelity. The goal is to give the extractor LLM
exactly the prose it needs (headings, paragraphs, sideboard tables, list
items) and none of the chrome (nav menus, ads, sidebars, related-articles
widgets, newsletter signups, comment threads).
"""

from __future__ import annotations

import re

_DROP_TAGS = (
    "script",
    "style",
    "nav",
    "aside",
    "form",
    "footer",
    "header",
    "iframe",
    "noscript",
    "svg",
    "button",
    "input",
)

#: dropped if a class token equals this, starts with ``<entry>-``, or ends with ``-<entry>``
_BLACKLIST_CLASS_TOKENS = frozenset({
    "ad",
    "ads",
    "advert",
    "advertisement",
    "sidebar",
    "breadcrumb",
    "breadcrumbs",
    "related",
    "share",
    "shares",
    "social",
    "newsletter",
    "subscribe",
    "promo",
    "promotion",
    "cookie",
    "cookies",
    "popup",
    "modal",
    "menu",
    "nav",
    "site-header",
    "site-footer",
    "author-box",
    "tag-list",
    "comments",
    "comment-form",
    "comment-list",
})

#: dropped if the id starts with or equals one of these
_BLACKLIST_ID_PREFIXES = (
    "comments",
    "sidebar",
    "newsletter",
    "footer",
    "nav-",
    "ad-",
    "ads-",
)


def _class_matches_blacklist(token: str) -> bool:
    if not token:
        return False
    if token in _BLACKLIST_CLASS_TOKENS:
        return True
    for bad in ("ad", "ads", "advert", "nav", "menu", "sidebar", "promo"):
        if token.startswith(bad + "-") or token.endswith("-" + bad):
            return True
    return False


def _node_should_drop(el) -> bool:
    if not hasattr(el, "get"):
        return False
    classes = el.get("class") or []
    for tok in classes:
        if _class_matches_blacklist(tok.lower()):
            return True
    el_id = (el.get("id") or "").lower()
    if el_id:
        for prefix in _BLACKLIST_ID_PREFIXES:
            if el_id == prefix.rstrip("-") or el_id.startswith(prefix):
                return True
    return False


def _walk(el, out: list[str]) -> None:
    """Depth-first walk producing markdown-ish text."""
    name = getattr(el, "name", None)
    if name is None:
        # NavigableString — only emit at the top level via parent's get_text.
        return
    if _node_should_drop(el):
        return

    if name in {"h1", "h2", "h3", "h4"}:
        level = int(name[1])
        text = el.get_text(" ", strip=True)
        if text:
            out.append("\n" + ("#" * level) + " " + text + "\n")
        return
    if name in {"p", "li", "blockquote"}:
        text = el.get_text(" ", strip=True)
        if text:
            prefix = "- " if name == "li" else ""
            out.append(prefix + text)
        return
    if name == "table":
        # Render tables row-by-row with " | " separators — good enough for
        # sideboard matrices.
        for tr in el.find_all("tr"):
            cells = [c.get_text(" ", strip=True) for c in tr.find_all(["th", "td"])]
            if any(cells):
                out.append(" | ".join(cells))
        return
    # Generic container — recurse.
    for child in el.children:
        _walk(child, out)


def clean_for_extraction(html: str) -> str:
    """Return plain-text prose suitable for an LLM extractor prompt."""
    from bs4 import BeautifulSoup

    soup = BeautifulSoup(html, "html.parser")
    for tag in _DROP_TAGS:
        for el in soup.find_all(tag):
            el.decompose()

    root = (
        soup.find("article")
        or soup.find("main")
        or soup.find("div", attrs={"class": re.compile(r"(post|entry|content|primer)", re.I)})
        or soup.body
        or soup
    )

    out: list[str] = []
    _walk(root, out)
    text = "\n".join(s for s in out if s)
    text = re.sub(r"\n{3,}", "\n\n", text).strip()
    return text


def truncate_for_prompt(text: str, *, max_chars: int = 12000) -> str:
    """Keep the head of the text; mark truncated tails so the LLM knows."""
    if len(text) <= max_chars:
        return text
    return text[:max_chars].rstrip() + "\n\n[...content truncated for length...]"
