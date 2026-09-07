# ADR-0002 — Toolchain and Dependency Policy

- **Status:** Accepted
- **Date:** 2026-09-06
- **Deciders:** `jc@archlab.pl`

## Context

Two toolchains live in this repository and must not entangle. Go builds Crucible. Maven builds upstream Forge and the
differential oracle in `crucible/oracle-java/` (ADR-0001). Neither build may require the other to be present.

What Crucible actually is, computationally, decides the dependency posture. It is a batch simulator: 10^5 to 10^6 games
per run, goroutine-per-game, CPU-bound, no network, no UI, no server. The hard problems are rules correctness and
determinism, not I/O or framework integration. That is a profile the standard library covers almost entirely.

Dependency weight also matters more here than on a typical project, because the engine is a port of a 517,069-line Java
codebase whose behaviour is not fully documented anywhere. When a rules bug is being chased through the layer system,
every third-party abstraction in the stack is one more thing that has to be understood or ruled out first.

Reproducibility is a correctness requirement, not hygiene. Every telemetry run writes a `manifest.json` claiming a git
SHA and an engine version. A run that cannot be reproduced from its manifest is a bug (plan, Phase 4), and that only
holds if the toolchain is pinned too.

## Decision Drivers

- Determinism and reproducibility end to end, from toolchain to telemetry output.
- Long project life. Every dependency is a future migration.
- Contributors and CI must build identical binaries without manual toolchain setup.
- The oracle must build without editing any upstream file.
- Go language features actually needed: type parameters, `testing.F` fuzzing, `slices`/`maps`, `math/rand/v2`.

## Considered Options

### Go version pinning

1. Track whatever is installed. Rejected — a telemetry run stops being reproducible the moment two machines differ.
2. Pin the language version only (`go 1.x` in `go.mod`). Rejected — pins semantics, not the compiler; codegen and
   inlining still vary, so benchmark thresholds drift for no visible reason.
3. Pin language version and toolchain. **Chosen.**

### Dependency policy

1. Normal Go ecosystem usage. Rejected for the reasons in Context.
2. Absolute zero, stdlib only, no exceptions. Rejected — honest right up until telemetry storage at M8, at which point
   the rule gets broken quietly rather than amended openly.
3. Near-zero, with an ADR gate per dependency and separate rules per scope. **Chosen.**

## Decision

**Go 1.27, pinned in `crucible/go.mod`:**

```text
go 1.27
toolchain go1.27.0
```

`GOTOOLCHAIN=auto` is the default, so the pinned toolchain is fetched automatically. No contributor has to install a
specific Go by hand. Version bumps happen in their own PR, never as a side effect of feature work.

**Dependencies, by scope. The scopes have different rules because they carry different risk.**

| Scope                                         | Policy                                 | Allowed today                                          |
| --------------------------------------------- | -------------------------------------- | ------------------------------------------------------ |
| Runtime — compiled into the `crucible` binary | stdlib only; any addition needs an ADR | none                                                   |
| Test-only                                     | narrow, justified additions            | `github.com/google/go-cmp`                             |
| Tools — invoked, never imported               | pinned by version in CI                | `golangci-lint`, `prettier`, `markdownlint-cli2`, `gh` |

Tools are deliberately a separate category. A linter can be swapped without touching a byte of the shipped binary, so it
does not deserve the same gate as an import.

**Two runtime pressure points are named now rather than discovered later.** Telemetry storage at M8 needs zstd
compression and a columnar format; neither is in the standard library. That decision belongs to ADR-0014, and until it
is made the answer is `compress/gzip` and NDJSON. Naming the pressure point is what keeps the near-zero rule from being
broken silently.

**Pinning without an update path is just rot, so Dependabot is part of this decision, not a follow-up.**
`.github/dependabot.yml` tracks two ecosystems weekly: `github-actions` at the root, and `gomod` scoped to `/crucible`.
Dependabot reports an error on every run for an ecosystem entry whose directory holds no manifest, so each entry lands
in the same pull request as the manifest it tracks.

Each ecosystem splits into a security group and a version group via `applies-to`. The split matters because the
`schedule` interval governs version updates only — security updates open as soon as an advisory is published, so a
weekly cadence never delays a CVE fix. Within version updates, test-only dependencies are grouped ahead of runtime ones:
Dependabot assigns each dependency to the first matching group, so a `go-cmp` bump that reddens CI lands in its own PR
instead of blocking a runtime update.

**No `maven` ecosystem, ever, at the repository root.** Dependabot would open weekly PRs editing upstream's `pom.xml`
and the `forge-*` modules — generating exactly the conflict surface ADR-0001 exists to prevent, automatically, without
anyone deciding to. `crucible/oracle-java/pom.xml` can be tracked once it carries dependencies, scoped to that directory
and nothing wider. The reasoning is repeated as a comment in `dependabot.yml` itself, because that is where someone will
be when they are tempted.

**That exclusion is necessary but not sufficient, because `dependabot.yml` governs version updates only.** Security
updates scan the whole dependency graph regardless of that file and open pull requests against any manifest holding a
vulnerable dependency. There is no path scoping for them — security updates are repo-wide or off. Enabling
`automated-security-fixes` therefore reaches `forge-gui/pom.xml` through a door the `maven` exclusion cannot close: a
CVE fix in `at.yawk.lz4:lz4-java` arrives as a pull request editing an upstream Card-Forge file. So the two settings are
split:

| Setting                    | State   | Reason                                                     |
| -------------------------- | ------- | ---------------------------------------------------------- |
| `vulnerability-alerts`     | **on**  | Read-only. Reports CVEs, touches no file, opens no PR      |
| `automated-security-fixes` | **off** | This is the part that opens PRs editing upstream `pom.xml` |

The security signal is kept; the automated edit is not. That trade is right here because Crucible does not ship Forge's
jars — the Java engine is a CI-only differential-testing oracle — so an upstream Java CVE is upstream's fix to make, not
a row in `upstream-patches.md`. Revisit when `crucible/oracle-java/pom.xml` carries dependencies of its own; the answer
is probably still no while any upstream `pom.xml` remains in the tree.

Verify current state with:

```bash
gh api repos/jczastkiewicz/crucible/vulnerability-alerts -i | head -1        # 204 = on
gh api repos/jczastkiewicz/crucible/automated-security-fixes                # {"enabled":false}
```

**Markdown tooling is pinned but not Dependabot-tracked**, and that gap is deliberate. Tracking it needs a
`package.json` at the repository root, which needs `node_modules` in `.gitignore` — and `.gitignore` is an upstream
file, so that would be the first real _edit_ in `upstream-patches.md`. Two formatters are not worth trading the "edits:
none" target state for. Versions are recorded in the guidelines and bumped by hand when a check starts disagreeing
between a machine and CI.

**Java side, for the oracle:**

| Item            | Pin            | Why                                                                       |
| --------------- | -------------- | ------------------------------------------------------------------------- |
| JDK             | 21             | Upstream's own CI matrix tests 17 and 21 only. 21 is the newer tested LTS |
| Maven           | 3.9.x          | Matches upstream's build                                                  |
| Target bytecode | `--release 17` | What upstream's `pom.xml` declares                                        |

The machine this was written on runs JDK 25. It compiles `--release 17` correctly, but upstream does not test it, so CI
pins 21 and local mismatches are tolerated rather than blessed.

**`crucible/oracle-java/pom.xml` is standalone** — no `<parent>`, own coordinates, `maven.compiler.release` set to 17 to
match upstream, and not listed in the root POM's `<modules>`. Adding a `<module>` line would be a one-line upstream edit
in a file upstream changes often, which is what ADR-0001 exists to prevent.

Inheriting from the root POM is the arrangement that looks right and does not work. Maven does permit a child to name a
parent that does not aggregate it, so dependency management could in principle be inherited with the root left unedited.
What defeats it is how upstream versions that root:

```xml
<version>${revision}</version>
<revision>${versionCode}${snapshotName}</revision>
```

CI-friendly versioning. A child naming this parent must hardcode the **resolved** value — `2.0.15-SNAPSHOT` today —
because Maven cannot interpolate a property into a parent version before the parent is resolved. Attempting it fails
outright:

```text
Non-resolvable parent POM for crucible:crucible-oracle: Could not find artifact forge:forge:pom:2.0.05-SNAPSHOT
```

Hardcoding the correct value only moves the problem: `versionCode` changes on every upstream release, so the file breaks
on the next sync — precisely the coupling ADR-0001 exists to avoid, arriving through the mechanism chosen to avoid it.

Standalone costs nothing today, because the dumpers use only the JDK: `mvn -f crucible/oracle-java/pom.xml compile`
succeeds against an unbuilt upstream. When a dumper needs `forge-game`, that becomes an ordinary `<dependency>` resolved
from `mvn install -DskipTests` at the root, with the version passed as `-Dforge.version=...` rather than hardcoded.

## Consequences

**Good.** A run's `manifest.json` now identifies a reproducible binary, because the toolchain that produced it is pinned
alongside the source. Benchmark regression thresholds mean something, since codegen no longer varies between machines.
The two builds stay genuinely independent — Go contributors never install a JDK, and the oracle never needs Go.
Dependency review is cheap because the default answer is no and the exceptions are enumerated.

**Bad.** Toolchain bumps become deliberate work rather than something that happens by itself, and the pin will lag
upstream Go releases. Markdown tool versions drift by hand until someone notices a check disagreeing between a laptop
and CI. Refusing convenient libraries means writing things the ecosystem already solved — an ordered set, generic slice
helpers, a bit-compatible `java.util.Random` — which is more code to own and test. A standalone oracle POM is unusual
enough that it needs the comment in the file explaining why, or someone will helpfully "fix" it by adding a `<parent>`
or a root `<module>` line, either of which re-couples the oracle to upstream's version scheme.

**Neutral.** Pinning tool versions in CI while leaving local installs unpinned means a contributor can see lint results
that CI does not reproduce. Acceptable, since CI is the authority and the gap is visible immediately.

## Related

- [01-go-coding-standards.md](../guidelines/01-go-coding-standards.md) — GO-1 and GO-14 implement this ADR
- ADR-0001 — why the oracle POM must not touch the root `pom.xml`
- ADR-0014 — telemetry storage, which will revisit the runtime dependency list
