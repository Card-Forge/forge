"""Tests for the primer HTML cleaner."""

from app.knowledge.primers.clean_html import clean_for_extraction, truncate_for_prompt


_SAMPLE = """
<!doctype html>
<html><head><title>x</title><script>var x=1;</script></head>
<body>
  <nav><a>home</a></nav>
  <header class="site-header">SITE</header>
  <article>
    <h1>Boros Energy Deck Guide</h1>
    <p>Boros Energy is a fast aggressive deck.</p>
    <h2>The Decklist</h2>
    <p>Run 4 Ragavan and 4 Guide of Souls.</p>
    <h2>Sideboard Guide</h2>
    <h3>vs Yawgmoth</h3>
    <p>Bring in graveyard hate.</p>
    <ul><li>+2 Surgical Extraction</li><li>-2 Lightning Helix</li></ul>
    <table><tr><th>In</th><th>Out</th></tr><tr><td>Surgical</td><td>Helix</td></tr></table>
  </article>
  <aside class="sidebar">ads</aside>
  <div class="comments-section">a comment</div>
  <footer>FOOTER</footer>
</body></html>
"""


def test_clean_strips_chrome_and_keeps_content():
    out = clean_for_extraction(_SAMPLE)
    assert "Boros Energy Deck Guide" in out
    assert "Run 4 Ragavan" in out
    assert "Sideboard Guide" in out
    assert "Surgical Extraction" in out
    # noise excluded
    assert "SITE" not in out
    assert "FOOTER" not in out
    assert "var x" not in out
    assert "a comment" not in out


def test_clean_emits_heading_markers():
    out = clean_for_extraction(_SAMPLE)
    assert "## The Decklist" in out
    assert "## Sideboard Guide" in out
    assert "### vs Yawgmoth" in out


def test_clean_renders_tables_and_lists():
    out = clean_for_extraction(_SAMPLE)
    assert "- +2 Surgical Extraction" in out
    assert "In | Out" in out
    assert "Surgical | Helix" in out


def test_truncate_marks_truncation():
    text = "a" * 20000
    out = truncate_for_prompt(text, max_chars=500)
    assert len(out) <= 500 + 50
    assert "truncated" in out


def test_truncate_passthrough_short_text():
    text = "short text"
    assert truncate_for_prompt(text, max_chars=500) == text
