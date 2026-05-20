"""Multi-provider piloting primer ingestion.

The packages exposes :func:`build_primer` which walks a per-format provider
chain (Cards Realm, Hareruya, MTG Arena Zone, Moxfield, Draftsim) and uses the
builder LLM to extract structured fields from cleaned primer HTML. When every
editorial provider fails the orchestrator falls back to a synthesized default
primer.

All of this is OFFLINE — used by ``scripts/build_piloting_guides.py``, never
imported on the request path.
"""

from app.knowledge.primers.orchestrator import build_primer  # noqa: F401
