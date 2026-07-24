#!/bin/bash
# ios-pipeline.sh — build unmodified upstream Forge for iOS (MobiVM).
#
# MobiVM's runtime library is Java-7-era. Instead of hand-backporting modern
# Java, this pipeline transforms the built jars:
#   1. JvmDowngrader -c 52 : Java 9-17 surface -> Java 8 bytecode
#      (records, List.of, String.repeat, switch expressions, concat-indy)
#   2. MobiVmBridge (src/) : Java 8 library surface missing from MobiVM ->
#      streamsupport backports + forge.compat + app-supplied java.* classes
#      (rules in bridge.cfg; java.time = relocated ThreeTen; java.nio.file =
#      java-supply/)
#   3. MobiVmLinkAudit     : verifies every java/javax/java8/compat member
#      reference in the final jars against the real runtime — the report is
#      the complete porting workload after an upstream merge (usually empty)
#
# Usage:
#   pipeline/ios-pipeline.sh classpath   # transform jars -> tmp/ios-m2 (run
#                                        #   after every module rebuild/merge)
#   pipeline/ios-pipeline.sh sim         # build + install + launch simulator
#   pipeline/ios-pipeline.sh device      # build + sign + install to iPad
#
# Typical merge workflow:
#   git pull && mvn clean install -pl forge-core,forge-game,forge-gui,forge-gui-mobile,forge-ai -DskipTests
#   pipeline/ios-pipeline.sh classpath   # check the audit report it prints
#   pipeline/ios-pipeline.sh sim         # or: device
#
# App identity: robovm.properties is untracked (developer-specific bundle id)
# and is generated on first run from robovm.properties.template using APP_ID
# from your .env. Register your own App ID -- do not ship under someone
# else's bundle identifier.
#
# Machine/developer-specific settings (device UDIDs, signing identity) are
# NOT stored in this script. Put them in the untracked .env at the repo root
# (or export them). Example .env entries:
#
#   SIM_UDID=<simulator UDID from: xcrun simctl list devices available>
#   IPAD_UDID=<device UDID from: xcrun devicectl list devices>
#   SIGN_ID='Apple Development: Your Name (CERTID1234)'
#   PROFILE=/path/to/embedded.mobileprovision
#   TEAM_ID=YOURTEAMID
#
# Environment overrides (all may also live in .env):
#   APP_ID        bundle identifier   (default: app.id from robovm.properties)
#   APP_EXEC      executable name     (default: app.executable from robovm.properties)
#   SIM_UDID      simulator device        (required for: sim)
#   IPAD_UDID     devicectl install target (required for: device)
#   SIGN_ID       codesign identity        (required for: device)
#   PROFILE       provisioning profile     (required for: device)
#   TEAM_ID       Apple team id            (required for: device)
#   SKIP_CACHE_CLEAR=1   reuse the RoboVM AOT cache (safe: content-hashed)
#
# Working artifacts (downloaded jars, transformed output, cloned repo) live
# under tmp/ (untracked): tmp/jvmdg/ and tmp/ios-m2/. First run bootstraps
# the third-party jars from GitHub/Maven Central automatically.
set -e
# (no pipefail: `cmd | head` legitimately SIGPIPEs the left side here)

MODE="${1:-classpath}"

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
PIPE="$ROOT/forge-gui-ios/pipeline"
JV="$ROOT/tmp/jvmdg"
WORK="$JV/work"
CLONE="$ROOT/tmp/ios-m2"
M2="$HOME/.m2/repository"
SETTINGS="$ROOT/.mvn/local-settings.xml"
CP_FILE="$JV/ios-classpath.txt"

# machine/developer-specific settings live in the untracked repo-root .env;
# explicitly exported environment variables take precedence over .env values
# (e.g. SIM_UDID=<other-sim> pipeline/ios-pipeline.sh sim)
_pre_APP_ID="$APP_ID"; _pre_APP_EXEC="$APP_EXEC"; _pre_SIM_UDID="$SIM_UDID"
_pre_IPAD_UDID="$IPAD_UDID"; _pre_SIGN_ID="$SIGN_ID"; _pre_PROFILE="$PROFILE"; _pre_TEAM_ID="$TEAM_ID"
if [ -f "$ROOT/.env" ]; then
    set -a
    # shellcheck disable=SC1091
    . "$ROOT/.env"
    set +a
fi
[ -n "$_pre_APP_ID" ]    && APP_ID="$_pre_APP_ID"
[ -n "$_pre_APP_EXEC" ]  && APP_EXEC="$_pre_APP_EXEC"
[ -n "$_pre_SIM_UDID" ]  && SIM_UDID="$_pre_SIM_UDID"
[ -n "$_pre_IPAD_UDID" ] && IPAD_UDID="$_pre_IPAD_UDID"
[ -n "$_pre_SIGN_ID" ]   && SIGN_ID="$_pre_SIGN_ID"
[ -n "$_pre_PROFILE" ]   && PROFILE="$_pre_PROFILE"
[ -n "$_pre_TEAM_ID" ]   && TEAM_ID="$_pre_TEAM_ID"

# Prefer a native arm64 JDK 17+ when present so the RoboVM AOT compiler doesn't
# run under Rosetta on Apple Silicon (a large build-time win — the whole
# compiler is Java). No-op when JAVA_HOME is already set (e.g. from .env) or no
# native JDK is installed; the sim arch is pinned explicitly below, so a native
# JDK flipping RoboVM's default host arch to arm64-simulator is harmless.
if [ -z "$JAVA_HOME" ] && [ "$(uname -m)" = "arm64" ] && [ -x /usr/libexec/java_home ]; then
    _found_arm64_jdk=""
    for _jh in $(/usr/libexec/java_home -V 2>&1 \
            | grep -oE '/Library/Java/JavaVirtualMachines/[^ ]*/Contents/Home' | sort -u); do
        [ -x "$_jh/bin/java" ] || continue
        file "$_jh/bin/java" 2>/dev/null | grep -q arm64 || continue
        if "$_jh/bin/java" -version 2>&1 | grep -qE '"(1[7-9]|[2-9][0-9])'; then
            export JAVA_HOME="$_jh"; export PATH="$_jh/bin:$PATH"
            echo "using native arm64 JDK: $_jh"
            _found_arm64_jdk=1
            break
        fi
    done
    # Optional accelerator, not a requirement: the build still works on an
    # x86_64 JDK, just under Rosetta (slower AOT). Nudge — once — toward native.
    if [ -z "$_found_arm64_jdk" ]; then
        echo "note: no native arm64 JDK 17+ found — the AOT compile will run under Rosetta." >&2
        echo "      For faster builds install the aarch64 (Apple Silicon) Temurin 17, e.g.:" >&2
        echo "      https://adoptium.net/temurin/releases/?os=mac&arch=aarch64&version=17" >&2
        echo "      (Homebrew's temurin@17 cask under an Intel brew installs the x86_64 build.)" >&2
    fi
fi

# RoboVM simulator arch: match the host so the sim runs NATIVELY (no Rosetta).
# On Apple Silicon that is arm64-simulator (needs the sim-flavored libForgeOSLog
# built in sim()); on Intel it stays x86_64. The gdx/ObjectAL/freetype/box2d
# natives already ship arm64+x86_64 simulator slices in their -natives-ios jars.
SIM_ARCH="x86_64"; [ "$(uname -m)" = "arm64" ] && SIM_ARCH="arm64-simulator"

# app identity: robovm.properties (untracked) is generated from the tracked
# template using YOUR bundle identifier (APP_ID from .env / environment)
PROPS="$ROOT/forge-gui-ios/robovm.properties"
if [ ! -f "$PROPS" ]; then
    if [ -z "$APP_ID" ]; then
        echo "ERROR: $PROPS does not exist and APP_ID is not set." >&2
        echo "       Register your own bundle identifier and add APP_ID=<your.bundle.id>" >&2
        echo "       to $ROOT/.env, then re-run. See robovm.properties.template." >&2
        exit 1
    fi
    sed "s/@APP_ID@/$APP_ID/" "$ROOT/forge-gui-ios/robovm.properties.template" > "$PROPS"
    echo "generated $PROPS for $APP_ID"
fi
APP_ID="${APP_ID:-$(sed -n 's/^app\.id=//p' "$PROPS")}"
APP_EXEC="${APP_EXEC:-$(sed -n 's/^app\.executable=//p' "$PROPS")}"

require_env() {
    for v in "$@"; do
        if [ -z "${!v}" ]; then
            echo "ERROR: $v is not set. Export it or add it to $ROOT/.env" >&2
            echo "       (see the header of this script for expected entries)" >&2
            exit 1
        fi
    done
}

JVMDG_VER=1.3.6
SS_VER=1.7.4
THREETEN_VER=1.7.1
ASM_VER=9.9.1

JVMDG_JAR="$JV/jvmdowngrader-$JVMDG_VER-all.jar"
SS_JAR="$JV/streamsupport-$SS_VER.jar"
SSCF_JAR="$JV/streamsupport-cfuture-$SS_VER.jar"
THREETEN_JAR="$JV/threetenbp-$THREETEN_VER.jar"
API8_JAR="$JV/java-api-8.jar"
NIO_JAR="$JV/java-nio-supply.jar"
ASM="$M2/org/ow2/asm/asm/$ASM_VER/asm-$ASM_VER.jar"
ASMC="$M2/org/ow2/asm/asm-commons/$ASM_VER/asm-commons-$ASM_VER.jar"
TOOLS_CP="$ASM:$ASMC:$JV/tools"

RT="$M2/com/mobidevelop/robovm/robovm-rt/2.3.23/robovm-rt-2.3.23.jar"
ROBOVM_OBJC="$M2/com/mobidevelop/robovm/robovm-objc/2.3.23/robovm-objc-2.3.23.jar"
ROBOVM_CT="$M2/com/mobidevelop/robovm/robovm-cocoatouch/2.3.23/robovm-cocoatouch-2.3.23.jar"
GDXB="$M2/com/badlogicgames/gdx/gdx-backend-robovm/1.13.5/gdx-backend-robovm-1.13.5.jar"

# ---------------------------------------------------------------- bootstrap
bootstrap() {
    mkdir -p "$JV/tools"

    [ -f "$JVMDG_JAR" ] || curl -sL -o "$JVMDG_JAR" \
        "https://github.com/unimined/JvmDowngrader/releases/download/$JVMDG_VER/jvmdowngrader-$JVMDG_VER-all.jar"
    [ -f "$SS_JAR" ] || curl -sL -o "$SS_JAR" \
        "https://repo1.maven.org/maven2/net/sourceforge/streamsupport/streamsupport/$SS_VER/streamsupport-$SS_VER.jar"
    [ -f "$SSCF_JAR" ] || curl -sL -o "$SSCF_JAR" \
        "https://repo1.maven.org/maven2/net/sourceforge/streamsupport/streamsupport-cfuture/$SS_VER/streamsupport-cfuture-$SS_VER.jar"
    [ -f "$THREETEN_JAR" ] || curl -sL -o "$THREETEN_JAR" \
        "https://repo1.maven.org/maven2/org/threeten/threetenbp/$THREETEN_VER/threetenbp-$THREETEN_VER.jar"
    if [ ! -f "$ASM" ]; then
        mvn -q --settings "$SETTINGS" dependency:get -Dartifact=org.ow2.asm:asm:$ASM_VER
        mvn -q --settings "$SETTINGS" dependency:get -Dartifact=org.ow2.asm:asm-commons:$ASM_VER
    fi

    # jvmdg runtime stubs, pre-downgraded to Java 8
    [ -f "$API8_JAR" ] || java -jar "$JVMDG_JAR" -c 52 debug downgradeApi "$API8_JAR"

    # bridge/audit tools (compile when missing or sources newer)
    if [ ! -f "$JV/tools/MobiVmBridge.class" ] \
       || [ "$PIPE/src/MobiVmBridge.java" -nt "$JV/tools/MobiVmBridge.class" ] \
       || [ "$PIPE/src/MobiVmLinkAudit.java" -nt "$JV/tools/MobiVmLinkAudit.class" ]; then
        javac -cp "$ASM:$ASMC" -d "$JV/tools" "$PIPE/src/MobiVmBridge.java" "$PIPE/src/MobiVmLinkAudit.java"
    fi

    # java.nio.file + UncheckedIOException supply (java.base patch-module)
    if [ ! -f "$NIO_JAR" ] || [ -n "$(find "$PIPE/java-supply/src" -name '*.java' -newer "$NIO_JAR" 2>/dev/null | head -1)" ]; then
        rm -rf "$JV/java-supply-classes"
        mkdir -p "$JV/java-supply-classes"
        (cd "$PIPE/java-supply/src" && javac --patch-module java.base=. --release 17 \
            -d "$JV/java-supply-classes" $(find . -name "*.java"))
        (cd "$JV/java-supply-classes" && jar cf "$NIO_JAR" .)
    fi

    # java.util.function stubs (satisfy MobiVM's own Comparator signatures)
    FUNC_JAR="$JV/java-function-stubs.jar"
    if [ ! -f "$FUNC_JAR" ] || [ -n "$(find "$PIPE/java-function-stubs/src" -name '*.java' -newer "$FUNC_JAR" 2>/dev/null | head -1)" ]; then
        rm -rf "$JV/function-stub-classes"
        mkdir -p "$JV/function-stub-classes"
        (cd "$PIPE/java-function-stubs/src" && javac --patch-module java.base=. --release 17 \
            -d "$JV/function-stub-classes" $(find . -name "*.java"))
        (cd "$JV/function-stub-classes" && jar cf "$FUNC_JAR" .)
    fi

    # install every pom-declared supply into ~/.m2 so forge-gui-ios compiles
    # resolve on a fresh clone (the classpath step installs the transformed
    # variants into the build clone separately)
    m2_install() { # groupId artifactId version file
        if [ ! -f "$M2/$(tr . / <<< "$1")/$2/$3/$2-$3.jar" ]; then
            mvn -q --settings "$SETTINGS" install:install-file -Dpackaging=jar \
                -DgroupId="$1" -DartifactId="$2" -Dversion="$3" -Dfile="$4"
        fi
    }
    m2_install net.sourceforge.streamsupport streamsupport "$SS_VER" "$SS_JAR"
    m2_install net.sourceforge.streamsupport streamsupport-cfuture "$SS_VER" "$SSCF_JAR"
    m2_install xyz.wagyourtail.jvmdowngrader jvmdowngrader-java-api "$JVMDG_VER-mobivm" "$API8_JAR"
    m2_install forge.stubs java-nio-supply 1.0 "$NIO_JAR"
    m2_install forge.stubs java-function-stubs 1.0 "$FUNC_JAR"
    if [ ! -f "$M2/forge/stubs/java-time-supply/1.0/java-time-supply-1.0.jar" ]; then
        # compile-time placeholder; the classpath step installs the real
        # (relocated) variant into the build clone
        m2_install forge.stubs java-time-supply 1.0 "$THREETEN_JAR"
    fi
}

# ------------------------------------------------------------- transform all
classpath() {
    echo "=== [1/8] resolve classpath + fix \${revision} poms ==="
    VERSION="$(grep -A1 '<versionCode>' "$ROOT/pom.xml" | grep -o '[0-9.]*')-SNAPSHOT"
    # portable in-place sed: GNU (Linux/CI) uses -i, BSD (macOS) uses -i ''
    if sed --version >/dev/null 2>&1; then SEDI=(sed -i); else SEDI=(sed -i ''); fi
    find "$M2/forge" -name "*.pom" -exec "${SEDI[@]}" "s/\\\${revision}/$VERSION/g" {} \;
    (cd "$ROOT/forge-gui-ios" && mvn -q dependency:build-classpath \
        -Dmdep.outputFile="$CP_FILE" --settings "$SETTINGS")

    echo "=== [2/8] reset work dirs + clone maven repo ==="
    rm -rf "$WORK" "$CLONE"
    mkdir -p "$WORK/dg" "$WORK/out"
    # APFS copy-on-write clone on macOS; plain copy elsewhere (Linux CI)
    cp -Rc "$M2" "$CLONE" 2>/dev/null || cp -R "$M2" "$CLONE"

    echo "=== [3/8] partition classpath ==="
    ALL_JARS=$(tr ':' '\n' < "$CP_FILE")
    TRANSFORM=()
    KEEP=()
    while IFS= read -r j; do
        case "$j" in
            # robovm + ALL badlogicgames (libGDX) jars are MobiVM-clean by
            # construction; transforming them is unnecessary risk
            */robovm-*|*natives*|*com/badlogicgames/*) KEEP+=("$j") ;;
            # compile-time placeholder only: step [5/8] builds the real
            # (relocated) supply and step [7/8] installs it over this GAV in
            # the clone — transforming/scanning the placeholder just feeds the
            # audit stale org.threeten classes and a dead services entry
            */forge/stubs/java-time-supply/*) ;;
            *) TRANSFORM+=("$j") ;;
        esac
    done <<< "$ALL_JARS"
    echo "transform: ${#TRANSFORM[@]} jars, keep: ${#KEEP[@]} jars"
    FULL_CP=$(tr '\n' ':' <<< "$ALL_JARS" | sed 's/:$//')

    echo "=== [4/8] JvmDowngrader -c 52 on ${#TRANSFORM[@]} jars ==="
    for j in "${TRANSFORM[@]}"; do
        java -jar "$JVMDG_JAR" -q -c 52 downgrade -t "$j" "$WORK/dg/$(basename "$j")" \
            -cp "$FULL_CP" 2>&1 | grep -v "^\[" || true
    done

    echo "=== [5/8] relocate ThreeTen -> java.time supply ==="
    java -cp "$TOOLS_CP" MobiVmBridge --rules "$PIPE/relocate-time.cfg" --index "$RT" \
        --in "$THREETEN_JAR" --out "$WORK/java-time-supply.jar" | tail -1
    # duplicate TZDB.dat at the relocated path too
    (cd "$WORK" && unzip -oq java-time-supply.jar org/threeten/bp/TZDB.dat \
      && mkdir -p java/time && cp org/threeten/bp/TZDB.dat java/time/TZDB.dat \
      && jar -uf java-time-supply.jar java/time/TZDB.dat && rm -rf org java)

    echo "=== [6/8] MobiVmBridge on all downgraded jars + jvmdg api jar ==="
    DG_JARS=$(ls "$WORK"/dg/*.jar | tr '\n' ',' | sed 's/,$//')
    KEEP_CS=$(IFS=,; echo "${KEEP[*]}")
    INDEX="$RT,$ROBOVM_OBJC,$ROBOVM_CT,$SS_JAR,$SSCF_JAR,$API8_JAR,$WORK/java-time-supply.jar,$NIO_JAR,$KEEP_CS,$DG_JARS"
    BRIDGE_ARGS=()
    for j in "$WORK"/dg/*.jar; do
        BRIDGE_ARGS+=(--in "$j" --out "$WORK/out/$(basename "$j")")
    done
    BRIDGE_ARGS+=(--in "$API8_JAR" --out "$WORK/out/jvmdg-java-api-mobivm.jar")
    java -cp "$TOOLS_CP" MobiVmBridge --rules "$PIPE/bridge.cfg" --index "$INDEX" \
        "${BRIDGE_ARGS[@]}" > "$WORK/bridge-report.txt" 2>&1 || true
    head -2 "$WORK/bridge-report.txt"
    # Build gate: fail if a jvmdg stub reachable from forge code was left unbridged (the
    # J_U_S_Stream.toList class of latent iOS crash). MobiVmBridge emits BRIDGE-GATE-FAIL for these.
    if grep -q 'BRIDGE-GATE-FAIL' "$WORK/bridge-report.txt"; then
        echo "!!! LINK GATE FAILED — unbridged jvmdg stub reachable from forge code:"
        grep -A50 'BRIDGE-GATE-FAIL' "$WORK/bridge-report.txt"
        exit 1
    fi
    # The downgradeApi output has the runtime API stubs (jXX/stub/*) but NOT the
    # jvmdg runtime-support classes those stubs call: exc/* (MissingStubError) and
    # util/* (Utils, Function, Pair, IOFunction, ...). Missing util/* crashed
    # online hosting (NetworkLogWriter -> a stub -> jvmdg/util/Utils
    # NoClassDefFoundError). Pull both from the CLI jar (they're Java-7 bytecode,
    # no java-8-gap APIs, so MobiVM-safe raw). NOT version/ or runtime/: those are
    # the runtime-downgrader machinery, which drags in shade/asm (~850KB) and is
    # never used in static-downgrade mode.
    (cd "$WORK" && unzip -oq "$JVMDG_JAR" \
        'xyz/wagyourtail/jvmdg/exc/*' \
        'xyz/wagyourtail/jvmdg/util/*' \
      && jar -uf out/jvmdg-java-api-mobivm.jar xyz && rm -rf xyz)

    echo "=== [7/8] overwrite transformed jars in clone + install supplies ==="
    for j in "${TRANSFORM[@]}"; do
        cp "$WORK/out/$(basename "$j")" "$CLONE/${j#"$M2"/}"
    done
    MVN_I=(mvn -q --settings "$SETTINGS" install:install-file -Dmaven.repo.local="$CLONE" -Dpackaging=jar)
    "${MVN_I[@]}" -Dfile="$SS_JAR"   -DgroupId=net.sourceforge.streamsupport -DartifactId=streamsupport -Dversion=$SS_VER
    "${MVN_I[@]}" -Dfile="$SSCF_JAR" -DgroupId=net.sourceforge.streamsupport -DartifactId=streamsupport-cfuture -Dversion=$SS_VER
    "${MVN_I[@]}" -Dfile="$WORK/out/jvmdg-java-api-mobivm.jar" -DgroupId=xyz.wagyourtail.jvmdowngrader -DartifactId=jvmdowngrader-java-api -Dversion=$JVMDG_VER-mobivm
    "${MVN_I[@]}" -Dfile="$WORK/java-time-supply.jar" -DgroupId=forge.stubs -DartifactId=java-time-supply -Dversion=1.0
    "${MVN_I[@]}" -Dfile="$NIO_JAR" -DgroupId=forge.stubs -DartifactId=java-nio-supply -Dversion=1.0

    echo "=== [8/8] linkage audit (this report = your porting workload) ==="
    SCAN=$(ls "$WORK"/out/*.jar | tr '\n' ',' | sed 's/,$//')
    java -cp "$TOOLS_CP" MobiVmLinkAudit \
        --rt "$RT,$ROBOVM_OBJC,$ROBOVM_CT,$GDXB,$SS_JAR,$SSCF_JAR,$WORK/out/jvmdg-java-api-mobivm.jar,$WORK/java-time-supply.jar,$NIO_JAR,$KEEP_CS,$SCAN" \
        --scan "$SCAN" > "$WORK/audit-report.txt" 2>&1 || true
    head -1 "$WORK/audit-report.txt"
    echo "full reports: $WORK/bridge-report.txt  $WORK/audit-report.txt"
}

# --------------------------------------------------- compile + transform app
build_module() {
    echo "=== compile forge-gui-ios (against UNtransformed ~/.m2 jars) ==="
    # Source is written for java.util.* types; the bridge below rewrites the
    # compiled classes to match the transformed runtime classpath.
    (cd "$ROOT/forge-gui-ios" && mvn clean compile --settings "$SETTINGS" -DskipTests -q)

    echo "=== transform forge-gui-ios/target/classes (jvmdg + bridge) ==="
    cd "$ROOT/forge-gui-ios/target"
    jar cf gui-ios-raw.jar -C classes .
    java -jar "$JVMDG_JAR" -q -c 52 downgrade -t gui-ios-raw.jar gui-ios-dg.jar \
        -cp "$(tr ':' '\n' < "$CP_FILE" | tr '\n' ':')$SS_JAR:$SSCF_JAR:$NIO_JAR" 2>&1 | grep -v '^\[' || true
    DG_JARS=$(ls "$WORK"/out/*.jar | tr '\n' ',' | sed 's/,$//')
    java -cp "$TOOLS_CP" MobiVmBridge --rules "$PIPE/bridge.cfg" \
        --index "$RT,$ROBOVM_OBJC,$ROBOVM_CT,$GDXB,$SS_JAR,$SSCF_JAR,$WORK/java-time-supply.jar,$NIO_JAR,$DG_JARS,gui-ios-dg.jar" \
        --in gui-ios-dg.jar --out gui-ios-bridged.jar > "$WORK/gui-ios-bridge-report.txt" 2>&1 || true
    # same link gate as the dependency pass: an unbridged jvmdg stub reachable
    # from the app module is a guaranteed runtime crash on device
    if grep -q 'BRIDGE-GATE-FAIL' "$WORK/gui-ios-bridge-report.txt"; then
        echo "!!! LINK GATE FAILED (forge-gui-ios) — unbridged jvmdg stub reachable:"
        grep -A50 'BRIDGE-GATE-FAIL' "$WORK/gui-ios-bridge-report.txt"
        exit 1
    fi
    rm -rf classes && mkdir classes && (cd classes && jar xf ../gui-ios-bridged.jar && rm -rf META-INF)

    echo "=== audit forge-gui-ios classes ==="
    java -cp "$TOOLS_CP" MobiVmLinkAudit \
        --rt "$RT,$ROBOVM_OBJC,$ROBOVM_CT,$GDXB,$SS_JAR,$SSCF_JAR,$WORK/out/jvmdg-java-api-mobivm.jar,$WORK/java-time-supply.jar,$NIO_JAR,$DG_JARS,gui-ios-bridged.jar" \
        --scan gui-ios-bridged.jar || true
    cd "$ROOT"
}

# libForgeOSLog.a wraps os_log (a C macro) for the Java side. arm64-device and
# arm64-simulator objects cannot coexist in one static archive (same cpu type,
# different LC_BUILD_VERSION platform), so we compile the flavor the current
# target needs from the committed source. The tracked libs/libForgeOSLog.a is
# the DEVICE flavor (also used by a plain `mvn robovm:ios-device`); sim() swaps
# in a simulator flavor and restores the committed lib on exit.
OSLOG_SRC="$ROOT/forge-gui-ios/oslog_wrapper/ForgeOSLog.m"
OSLOG_LIB="$ROOT/forge-gui-ios/libs/libForgeOSLog.a"

build_oslog() { # <device|sim>  -> (over)writes $OSLOG_LIB with that flavor
    local target="$1" work; work="$(mktemp -d)"
    if [ "$target" = "sim" ]; then
        local sdk; sdk="$(xcrun --sdk iphonesimulator --show-sdk-path)"
        xcrun clang -c "$OSLOG_SRC" -o "$work/arm.o" -target arm64-apple-ios11.0-simulator  -isysroot "$sdk"
        xcrun clang -c "$OSLOG_SRC" -o "$work/x86.o" -target x86_64-apple-ios11.0-simulator -isysroot "$sdk"
        xcrun libtool -static -o "$work/arm.a" "$work/arm.o"
        xcrun libtool -static -o "$work/x86.a" "$work/x86.o"
        lipo -create "$work/arm.a" "$work/x86.a" -output "$OSLOG_LIB"
    else
        local sdk; sdk="$(xcrun --sdk iphoneos --show-sdk-path)"
        xcrun clang -c "$OSLOG_SRC" -o "$work/arm.o" -target arm64-apple-ios11.0 -isysroot "$sdk"
        xcrun libtool -static -o "$OSLOG_LIB" "$work/arm.o"
    fi
    rm -rf "$work"
    echo "built libForgeOSLog.a ($target):$(lipo -info "$OSLOG_LIB" | sed 's/.*://')"
}

prep_build() {
    if [ "${SKIP_CACHE_CLEAR:-0}" != "1" ]; then
        rm -rf ~/.robovm/cache
    fi
    NEWEST_TXT=$(find "$ROOT/forge-gui/res/cardsfolder" -name '*.txt' -newer "$ROOT/forge-gui/res/cardsfolder/cardsfolder.zip" 2>/dev/null | head -1)
    if [ ! -f "$ROOT/forge-gui/res/cardsfolder/cardsfolder.zip" ] || [ -n "$NEWEST_TXT" ]; then
        (cd "$ROOT/forge-gui/res/cardsfolder" && bash mkzip.sh)
    fi
    build_module
}

# Replace an app framework's DEVICE binary with the iOS-SIMULATOR slice from the
# gdx -natives-ios jar's xcframework. RoboVM 2.3.24 extracts the ios-arm64 (device)
# slice for an arm64-simulator build, which dyld refuses to load in the simulator.
_swap_sim_framework() { # <framework> <natives-ios-jar> <app>
    local fw="$1" jar="$2" app="$3"
    local sub="META-INF/robovm/ios/libs/$fw.xcframework/ios-arm64_x86_64-simulator/$fw.framework/$fw"
    local tmp; tmp="$(mktemp -d)"
    if unzip -o -q "$jar" "$sub" -d "$tmp" 2>/dev/null && [ -f "$tmp/$sub" ]; then
        cp "$tmp/$sub" "$app/Frameworks/$fw.framework/$fw"
    else
        echo "WARN: no iOS-simulator slice for $fw in $(basename "$jar")"
    fi
    rm -rf "$tmp"
}

# Assemble a runnable arm64 iOS-SIMULATOR .app. RoboVM 2.3.24's robovm:ipad-sim
# goal cannot launch arm64 simulators on Apple Silicon — its device-type table
# predates them, so it aborts at device selection BEFORE bundling the .app. But
# its build() already produced the linked binary + a resolved config.xml, so we
# finish with the standalone AppCompiler and fix up the simulator platform tags
# the maven launch path would otherwise have handled.
assemble_arm64_sim_app() {
    local TMP="$ROOT/forge-gui-ios/target/robovm.tmp"
    local APP="$TMP/$APP_EXEC.app"
    local RVHOME CJ
    RVHOME="$(ls -d "$M2"/com/mobidevelop/robovm/robovm-dist/*/unpacked/robovm-* 2>/dev/null | sort | tail -1)"
    CJ="$(ls "$M2"/com/mobidevelop/robovm/robovm-dist-compiler/*/robovm-dist-compiler-*.jar 2>/dev/null | sort | tail -1)"
    [ -f "$TMP/config.xml" ] || { echo "config.xml missing — robovm build did not run"; exit 1; }
    echo "=== assemble arm64 iOS-simulator .app (RoboVM 2.3.24 can't launch AS sims via maven) ==="

    # 1. resolve robovm.properties placeholders the mojo left in config.xml
    if sed --version >/dev/null 2>&1; then SEDI=(sed -i); else SEDI=(sed -i ''); fi
    cp "$TMP/config.xml" "$TMP/config-sim.xml"
    "${SEDI[@]}" -e "s/\${app.id}/$APP_ID/g" -e "s/\${app.executable}/$APP_EXEC/g" "$TMP/config-sim.xml"

    # 2. bundle the .app via the standalone compiler (no device selection); reuses the AOT cache
    rm -rf "$APP"; mkdir -p "$APP"
    ROBOVM_HOME="$RVHOME" java -cp "$CJ" org.robovm.compiler.AppCompiler \
        -config "$TMP/config-sim.xml" -properties "$PROPS" -skipsign -d "$APP" "$APP_EXEC" \
        > "$TMP/appcompiler-sim.log" 2>&1 \
        || { echo "AppCompiler install failed:"; tail -8 "$TMP/appcompiler-sim.log"; exit 1; }

    # 3. re-stamp the main binary as iOS-simulator (AppCompiler writes device LC_VERSION_MIN_IPHONEOS)
    local SDKV; SDKV="$(xcrun --sdk iphonesimulator --show-sdk-version)"
    vtool -set-build-version 7 14.0 "$SDKV" -replace -output "$APP/$APP_EXEC.sim" "$APP/$APP_EXEC"
    mv -f "$APP/$APP_EXEC.sim" "$APP/$APP_EXEC"

    # 4. swap device framework slices -> simulator slices
    local GDX="$CLONE/com/badlogicgames/gdx"
    _swap_sim_framework gdx          "$GDX/gdx-platform/1.13.5/gdx-platform-1.13.5-natives-ios.jar"                   "$APP"
    _swap_sim_framework ObjectAL     "$GDX/gdx-platform/1.13.5/gdx-platform-1.13.5-natives-ios.jar"                   "$APP"
    _swap_sim_framework gdx-freetype "$GDX/gdx-freetype-platform/1.13.5/gdx-freetype-platform-1.13.5-natives-ios.jar" "$APP"
    _swap_sim_framework gdx-box2d    "$GDX/gdx-box2d-platform/1.13.5/gdx-box2d-platform-1.13.5-natives-ios.jar"       "$APP"

    # 5. ad-hoc sign (the simulator refuses to launch an unsigned bundle)
    local f
    for f in "$APP"/Frameworks/*.framework; do codesign --force --sign - --timestamp=none "$f" >/dev/null 2>&1; done
    codesign --force --sign - --timestamp=none "$APP" >/dev/null 2>&1
    echo "assembled: $APP ($(lipo -info "$APP/$APP_EXEC" | sed 's/.*://'), $(vtool -show-build "$APP/$APP_EXEC" 2>/dev/null | grep -i platform | tr -d ' '))"
}

sim() {
    require_env SIM_UDID
    prep_build
    # Build against a simulator-flavored libForgeOSLog, restoring the committed
    # device flavor on exit (even if the build fails). Without this the device
    # arm64 slice collides with the arm64-simulator target at link time.
    cp "$OSLOG_LIB" "$OSLOG_LIB.committed"
    trap 'mv -f "$OSLOG_LIB.committed" "$OSLOG_LIB" 2>/dev/null || true' EXIT
    build_oslog sim
    echo "=== robovm ipad-sim build ($SIM_ARCH) ==="
    # On Apple Silicon (arch=arm64-simulator) this fails at the mojo's device
    # selection AFTER producing the binary + config.xml — expected; we assemble
    # the .app ourselves below. On Intel (x86_64) it produces the .app directly.
    (cd "$ROOT/forge-gui-ios" && mvn robovm:ipad-sim --settings "$SETTINGS" \
        -Dmaven.repo.local="$CLONE" -Drobovm.arch="$SIM_ARCH" -DskipTests 2>&1 | tail -8) || true
    mv -f "$OSLOG_LIB.committed" "$OSLOG_LIB"; trap - EXIT

    APP="$ROOT/forge-gui-ios/target/robovm.tmp/$APP_EXEC.app"
    if [ "$SIM_ARCH" = "arm64-simulator" ]; then
        assemble_arm64_sim_app
    fi
    [ -f "$APP/$APP_EXEC" ] || { echo "APP BINARY MISSING - build failed"; exit 1; }

    echo "=== install + launch on simulator $SIM_UDID ==="
    xcrun simctl bootstatus "$SIM_UDID" -b || xcrun simctl boot "$SIM_UDID" || true
    xcrun simctl uninstall "$SIM_UDID" "$APP_ID" 2>/dev/null || true
    xcrun simctl install "$SIM_UDID" "$APP"
    xcrun simctl launch "$SIM_UDID" "$APP_ID"
    echo "logs: xcrun simctl spawn $SIM_UDID log stream --predicate \"process == '$APP_EXEC'\""
}

device() {
    require_env IPAD_UDID SIGN_ID PROFILE TEAM_ID
    prep_build
    echo "=== robovm ios-device build (deploy attempt may fail: >1 iPad) ==="
    (cd "$ROOT/forge-gui-ios" && mvn robovm:ios-device --settings "$SETTINGS" \
        -Dmaven.repo.local="$CLONE" -DskipTests 2>&1 | tail -8) || true
    APP="$ROOT/forge-gui-ios/target/robovm.tmp/$APP_EXEC.app"
    [ -f "$APP/$APP_EXEC" ] || { echo "DEVICE BINARY MISSING - build failed"; exit 1; }

    echo "=== sign ==="
    cp "$PROFILE" "$APP/embedded.mobileprovision"
    cat > /tmp/forge-entitlements.plist <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>application-identifier</key>
    <string>${TEAM_ID}.${APP_ID}</string>
    <key>com.apple.developer.team-identifier</key>
    <string>${TEAM_ID}</string>
    <key>get-task-allow</key>
    <true/>
    <key>keychain-access-groups</key>
    <array>
        <string>${TEAM_ID}.*</string>
    </array>
</dict>
</plist>
EOF
    for framework in "$APP"/Frameworks/*.framework; do
        codesign -f -s "$SIGN_ID" "$framework"
    done
    codesign -f -s "$SIGN_ID" --entitlements /tmp/forge-entitlements.plist \
        --generate-entitlement-der "$APP"

    echo "=== install to iPad $IPAD_UDID ==="
    xcrun devicectl device install app --device "$IPAD_UDID" "$APP"
    echo "INSTALLED"
}

# ------------------------------------------------------------------ CI modes
# audit: the full transform + link-gate WITHOUT any Apple tooling. Runs on a
# plain Linux runner in a few minutes and fails (exit 1) when Java code
# reachable from forge would crash on MobiVM (unbridged jvmdg stub, missing
# API). This is the per-PR iOS-compatibility check.
audit() {
    echo "=== install forge modules the iOS classpath resolves against ==="
    # '.' installs the parent POM too — required on a fresh clone, else the
    # iOS classpath resolution fails on the forge:forge:pom \${revision} parent
    (cd "$ROOT" && mvn -B -ntp -q install \
        -pl .,forge-core,forge-game,forge-gui,forge-gui-mobile,forge-ai -DskipTests)
    classpath
    build_module
    echo "iOS COMPATIBILITY GATE PASSED"
}

# ipa: full RoboVM AOT build producing an UNSIGNED .ipa (macOS runner; no
# certificates/profiles required). Users install it via Sideloadly/AltStore,
# which re-sign with their own Apple ID — no jailbreak involved.
ipa() {
    echo "=== install forge modules ==="
    # '.' installs the parent POM too — required on a fresh clone, else the
    # iOS classpath resolution fails on the forge:forge:pom \${revision} parent
    (cd "$ROOT" && mvn -B -ntp -q install \
        -pl .,forge-core,forge-game,forge-gui,forge-gui-mobile,forge-ai -DskipTests)
    classpath
    prep_build
    echo "=== robovm:create-ipa (unsigned) ==="
    (cd "$ROOT/forge-gui-ios" && mvn robovm:create-ipa --settings "$SETTINGS" \
        -Dmaven.repo.local="$CLONE" -DskipTests \
        -Drobovm.iosSkipSigning=true 2>&1 | tail -12)
    IPA=$(ls "$ROOT"/forge-gui-ios/target/robovm/*.ipa 2>/dev/null | head -1)
    [ -n "$IPA" ] || { echo "IPA MISSING - build failed"; exit 1; }
    echo "UNSIGNED IPA: $IPA"
}

bootstrap
case "$MODE" in
    classpath) classpath ;;
    sim)       sim ;;
    device)    device ;;
    audit)     audit ;;
    ipa)       ipa ;;
    *) echo "usage: $0 [classpath|sim|device|audit|ipa]"; exit 1 ;;
esac
