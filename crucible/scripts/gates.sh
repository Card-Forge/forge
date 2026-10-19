#!/usr/bin/env bash
# Runs the CI gates from .github/workflows/crucible-go.yml locally, in CI order.
#
#   gates.sh fast   gofmt, go vet, golangci-lint, enginelint, genregistry, docgate, apiscan,
#                   hooks (.claude/hooks/hooks_test.sh; not a CI gate)
#   gates.sh full   fast + go test -race, covergate, javacycles, prettier, markdownlint
#
# GATES_SKIP="apiscan ..." skips gates whose name starts with a listed word.
#
# GATES_AUTO_SKIP=1 (both .claude hooks) adds to it from `git status`: a gate
# is skipped when nothing it reads has changed since HEAD. CI never sets it.
#
#   apiscan            unless the Java tree, corpus, parity-matrix.md or the
#                      carddb/cardtype/expr parsers compiled into it changed
#   javacycles         unless forge-game/ changed
#   Go vet/lint/tests  unless something under crucible/ or forge-gui/res/
#                      changed -- several Go tests (internal/carddb,
#                      internal/cost, internal/deck, and every corpus golden)
#                      read the card/deck corpus as a fixture, so a
#                      card-script-only change (an upstream sync, a PORT-8
#                      fix) is a real input to them even though no file
#                      under crucible/ moved
#   prettier           unless a .md/.json/.yml/.yaml file changed
#   markdownlint       unless a .md file changed
#   hooks              unless .claude/hooks/ changed
#
# Tests run without -count=1 here, unlike CI: go test caches per package and
# invalidates on any changed source, dependency, or file the test opened (the
# card corpus included), so an unchanged package costs nothing.
#
# Exit status is non-zero when any gate fails; every gate still runs so one
# pass reports every failure. Output is the failing gates only, plus a summary.
set -u

mode=${1:-full}
case "$mode" in fast | full) ;; *)
	echo "usage: gates.sh [fast|full]" >&2
	exit 64
	;;
esac

repo=$(cd "$(dirname "$0")/../.." && pwd)
cd "$repo/crucible" || exit 1

if [ "${GATES_AUTO_SKIP:-}" = 1 ]; then
	changed=$(git -C "$repo" status --porcelain --untracked-files=all | cut -c4- | sed 's/.* -> //')
	has() { printf '%s\n' "$changed" | grep -Eq "$1"; }
	auto=""
	has '^(forge-|crucible/internal/(carddb|cardtype|expr)/|crucible/tools/apiscan/|docs/crucible/porting/parity-matrix\.md)' || auto="$auto apiscan"
	has '^forge-game/' || auto="$auto javacycles"
	has '^(crucible/|forge-gui/res/)' || auto="$auto gofmt go golangci-lint covergate"
	has '\.(md|json|ya?ml)$' || auto="$auto prettier"
	has '\.md$' || auto="$auto markdownlint"
	has '^\.claude/hooks/' || auto="$auto hooks"
	GATES_SKIP="${GATES_SKIP:-}$auto"
fi

# The first installed golangci-lint built with a Go new enough for go.mod
# (ensure-golangci.sh), not merely the first on PATH.
lint=$(scripts/ensure-golangci.sh -check || true)
failed=()
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

gate() {
	local name=$1
	shift
	local w
	for w in ${GATES_SKIP:-}; do
		case "$name" in "$w"*)
			echo "== skip: $name"
			return 0
			;;
		esac
	done
	if ! "$@" >"$tmp/out" 2>&1; then
		failed+=("$name")
		echo "== FAIL: $name"
		tail -40 "$tmp/out"
	fi
}

gofmt_clean() { test -z "$(gofmt -l .)" || { gofmt -l .; return 1; }; }
golangci() {
	[ -n "$lint" ] || {
		echo "golangci-lint missing or built with a Go older than go.mod's: run crucible/scripts/ensure-golangci.sh"
		return 1
	}
	# Parallel agents run gates in separate worktrees at once; without this the
	# second run fails with "parallel golangci-lint is running".
	"$lint" run --allow-parallel-runners
}

gate gofmt gofmt_clean
gate "go vet" go vet ./...
gate golangci-lint golangci
gate enginelint go run ./tools/enginelint -config internal/engine/enginelint.json
gate genregistry go run ./tools/genregistry -dir internal/engine -check \
	-docs ../CLAUDE.md
gate docgate go run ./tools/docgate -module . -docs ../docs/crucible
gate "apiscan -check" go run ./tools/apiscan -check
gate "apiscan -check -api" go run ./tools/apiscan -check -api
[ -x ../.claude/hooks/hooks_test.sh ] && gate hooks ../.claude/hooks/hooks_test.sh

if [ "$mode" = full ]; then
	gate "go test -race" go test -race -coverprofile="$tmp/cover.out" ./...
	[ -s "$tmp/cover.out" ] && gate covergate go run ./tools/covergate -profile "$tmp/cover.out"
	gate javacycles go run ./tools/javacycles -root ../forge-game/src/main/java -prefix forge.game -expect 87
	gate "prettier --check" sh -c 'cd .. && prettier --check . --log-level warn'
	gate markdownlint sh -c 'cd .. && npx --yes markdownlint-cli2 "CLAUDE.md" "docs/crucible/**/*.md"'
fi

if [ ${#failed[@]} -gt 0 ]; then
	echo "gates ($mode): FAILED — ${failed[*]}"
	exit 1
fi
echo "gates ($mode): all green"
