# Module Map

- **Status:** Active (empty)

Every Go package under `crucible/` gets a row here, in the same commit that creates the package (DOC-12).
`crucible/tools/docgate` fails the build on a package with no row.

Column meaning:

- **Package** — import path under `crucible/`.
- **Responsibility** — one line. If it needs two, the package is doing two things.
- **Java provenance** — the `forge-*` source it was ported from, or `—` for new code.
- **Port log** — note under `../porting/port-log/`, required for ported packages (PORT-4).

| Package | Responsibility | Java provenance | Port log |
| ------- | -------------- | --------------- | -------- |
| —       | none yet       | —               | —        |
