#!/usr/bin/env bash
# Compile and run the tests relevant to PR #9717 (refactor/hidden-keywords).
# Run from project root: ./scripts/compile-and-test-pr.sh
# Requires: Maven (mvn) and Java 17 in PATH.
# Optional: MVN=/path/to/mvn ./scripts/compile-and-test-pr.sh

set -e
cd "$(dirname "$0")/.."
MVN="${MVN:-mvn}"

echo "=== Compiling project ==="
"$MVN" compile -q -T 1C

echo ""
echo "=== Running forge-game tests: ForgetOnMovedTest ==="
"$MVN" test -pl forge-game -am -q -Dtest=forge.game.ability.ForgetOnMovedTest -Dsurefire.failIfNoSpecifiedTests=false

echo ""
echo "=== Running forge-gui-desktop tests: HiddenKeywordsMetaTest, LureTest, LureMechanicTest, Issue4745Test ==="
"$MVN" test -pl forge-gui-desktop -am -q -Dtest=forge.gamesimulationtests.HiddenKeywordsMetaTest,forge.gamesimulationtests.LureTest,forge.gamesimulationtests.LureMechanicTest,forge.gamesimulationtests.Issue4745Test -Dsurefire.failIfNoSpecifiedTests=false

echo ""
echo "=== All compile and requested tests passed ==="
