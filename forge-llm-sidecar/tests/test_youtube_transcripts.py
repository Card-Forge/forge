"""Tests for transcript coalescing + prompt rendering (no network)."""

from app.knowledge.primers.youtube.transcripts import (
    TranscriptChunk,
    _coalesce,
    render_for_prompt,
)


def test_coalesce_combines_short_segments():
    raw = [
        {"start": 0.0, "duration": 2.0, "text": "alpha"},
        {"start": 2.0, "duration": 2.0, "text": "beta"},
        {"start": 4.0, "duration": 2.0, "text": "gamma"},
    ]
    chunks = _coalesce(raw, chunk_seconds=10.0)
    # all three fit in one chunk
    assert len(chunks) == 1
    assert chunks[0].text == "alpha beta gamma"


def test_coalesce_starts_new_chunk_when_window_exceeded():
    raw = [
        {"start": 0.0, "duration": 25.0, "text": "first"},
        {"start": 25.0, "duration": 25.0, "text": "second"},
        {"start": 50.0, "duration": 5.0, "text": "third"},
    ]
    chunks = _coalesce(raw, chunk_seconds=30.0)
    assert len(chunks) >= 2


def test_coalesce_skips_empty_segments():
    raw = [
        {"start": 0.0, "duration": 1.0, "text": ""},
        {"start": 1.0, "duration": 1.0, "text": "x"},
    ]
    chunks = _coalesce(raw, chunk_seconds=10.0)
    assert len(chunks) == 1
    assert chunks[0].text == "x"


def test_render_prepends_timestamp():
    chunks = [TranscriptChunk(0, 30, "hello"), TranscriptChunk(30, 60, "world")]
    rendered = render_for_prompt(chunks)
    assert "[0:00-0:30] hello" in rendered
    assert "[0:30-1:00] world" in rendered


def test_render_truncates_long_input():
    chunks = [TranscriptChunk(i * 10, (i + 1) * 10, "x" * 200) for i in range(200)]
    rendered = render_for_prompt(chunks, max_chars=500)
    assert "truncated" in rendered
    assert len(rendered) <= 600
