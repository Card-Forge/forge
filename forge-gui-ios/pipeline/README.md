# iOS modern-Java pipeline

MobiVM/RoboVM ships a **Java-7-era runtime library** (no `java.util.function`,
`java.util.stream`, `Optional`, `java.time`, `java.nio.file`, and none of the
Java 8 default/static members on `Map`, `List`, `Comparator`, `String`, ...),
while upstream Forge freely uses Java 17. Instead of hand-backporting source
on every merge, this pipeline transforms the **built jars**:

| Stage | Tool | What it does |
|---|---|---|
| 1 | [JvmDowngrader](https://github.com/unimined/JvmDowngrader) `-c 52` | Java 9–17 → Java 8 bytecode: records de-sugared, `List.of`, `String.repeat`, switch expressions, string-concat indy. Lambdas stay `invokedynamic` (MobiVM handles those natively). |
| 2 | `MobiVmBridge` (`src/`) | The Java 8 library surface missing from MobiVM → [streamsupport](https://github.com/stefan-zobel/streamsupport) backports (`java8.*`), `forge.compat` rewrite targets, and app-supplied `java.*` classes. Rules in `bridge.cfg`. `java.time` is the ThreeTen backport relocated in-place (`relocate-time.cfg`); `java.nio.file` is the minimal supply in `java-supply/`. |
| 3 | `MobiVmLinkAudit` (`src/`) | Resolves **every** member reference in the final jars against the real runtime + supplies. Its report *is* the porting workload after an upstream merge — usually empty, occasionally one new `bridge.cfg` line. |

## Usage

```bash
# after every upstream merge / module rebuild:
mvn clean install -pl forge-core,forge-game,forge-gui,forge-gui-mobile,forge-ai -DskipTests
pipeline/ios-pipeline.sh classpath     # transform -> tmp/ios-m2, prints audit
pipeline/ios-pipeline.sh sim           # build + install + launch simulator
pipeline/ios-pipeline.sh device        # build + sign + install to iPad
```

First run bootstraps third-party jars (JvmDowngrader, streamsupport,
ThreeTen) into `tmp/jvmdg/` automatically. Device IDs / signing identity are
env-overridable — see the header of `ios-pipeline.sh`.

## Key facts (hard-won — see git history of feature/ios-jvmdg-pipeline)

- JvmDowngrader `-c 51` (true Java 7 target) is a dead end: its 8→7 stub
  bodies throw `MissingStubError`. `-c 52` is the mature path; the bridge
  covers the 8→MobiVM gap with *real* implementations (streamsupport).
- streamsupport interfaces are bare SAMs: statics/defaults live in companion
  classes (`RefStreams`, `Predicates`, `Comparators`, `Maps`, ...) — that's
  what most `bridge.cfg` member rules encode, receiver-prepended.
- The bridge renames jar **entry paths** when whole-type remapping (RoboVM
  locates classes by entry path) and rewrites method-reference `Handle`s
  inside invokedynamic bootstrap args.
- App-class owners matter: forge collections inherit Java 8 defaults
  (`CardCollection.forEach`), so the bridge resolves *every* indexed owner
  through its hierarchy, not just `java/*`.
- `forge-gui-ios` sources compile against **untransformed** `~/.m2` jars,
  then its `target/classes` go through the same downgrade+bridge — types line
  up at the bytecode level.
- libGDX/RoboVM jars are excluded from transformation (MobiVM-clean by
  construction); they participate in the resolution index only.
- The RoboVM AOT cache is content-hashed — `SKIP_CACHE_CLEAR=1` is safe and
  saves ~15 min per build.
- RoboVM's AOT lambda plugin silently drops `altMetafactory`'s
  `FLAG_SERIALIZABLE` (it only implements `FLAG_MARKERS`/`FLAG_BRIDGES`), so a
  serializable lambda — `(Supplier<T> & Serializable) X::new`, ECJ-compiled
  jgrapht is full of them — comes out NOT implementing `Serializable` and the
  compiler-emitted `checkcast java/io/Serializable` throws at runtime. The
  bridge converts the ignored bit into an explicit `FLAG_MARKERS` +
  `java/io/Serializable` marker, which RoboVM honors; the audit fails on any
  site that reaches the final jars without the marker. CAVEAT: this restores
  type-compatibility only — RoboVM generates no `writeReplace`, so actually
  Java-serializing such a lambda still throws `NotSerializableException`
  (nothing does that today).
- `META-INF/services/` entries are class names in disguise: the bridge remaps
  both the entry name and the impl lines through the type rules (miss this
  and e.g. the relocated ThreeTen zone-rules provider never registers —
  `java.time` works until the first `ZoneId.systemDefault()`, then throws
  "No time-zone data files registered"). The audit flags any services entry
  naming a class that exists nowhere on the final classpath.

## Bundle identifier (bring your own)

`forge-gui-ios/robovm.properties` is **untracked** because the bundle
identifier is developer-specific. Register your own App ID, put
`APP_ID=<your.bundle.id>` in the untracked `.env` at the repo root, and the
pipeline generates `robovm.properties` from `robovm.properties.template` on
first run (or copy the template manually and fill in `@APP_ID@`). Signing
settings (`SIGN_ID`, `PROFILE`, `TEAM_ID`) and device UDIDs also live in
`.env` — nothing machine- or identity-specific is tracked.

## Fresh-clone bootstrap

`ios-pipeline.sh` also builds and installs every `forge-gui-ios/pom.xml`
supply dependency into `~/.m2` on first run (streamsupport, the jvmdg api
jar, `java-nio-supply`, `java-function-stubs`, the sentry no-op stubs via
`sentry-stub/build.sh`, and a `java-time-supply` compile placeholder) — a
fresh clone needs only a JDK 17, Maven, Xcode, and an `.env`.
