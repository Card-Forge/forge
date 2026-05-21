"""YouTube gameplay-transcript enrichment for piloting guides.

This package runs OFFLINE — never on the sidecar request path — and adds
extracted-from-gameplay heuristics to existing piloting guides. Use
:func:`enrich` from :mod:`enricher` as the entrypoint.

See `/home/lou/.claude/plans/youtube-piloting-enrichment.md` for design.
"""

from app.knowledge.primers.youtube.enricher import enrich  # noqa: F401
