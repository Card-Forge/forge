#!/usr/bin/env bash
# Tests for the .claude hooks' decision logic: which commands count as a
# commit and which checkout they commit in, and which edits the REV-1 guard
# asks about. Run from anywhere: .claude/hooks/hooks_test.sh
set -u
here=$(cd "$(dirname "$0")" && pwd)
. "$here/lib.sh"
root=$(git -C "$here" rev-parse --show-toplevel)
fails=0

check() { # check NAME GOT WANT
	if [ "$2" != "$3" ]; then
		printf 'FAIL %s\n  got:  %q\n  want: %q\n' "$1" "$2" "$3"
		fails=$((fails + 1))
	fi
}

# commit_dir: a commit is git's subcommand, nothing else.
c=/work/repo
check "plain commit" "$(commit_dir 'git commit -m x' $c)" "$c"
check "commit with heredoc" "$(commit_dir "git commit -q -F - <<'EOF'
subject; with | separators
EOF" $c)" "$c"
check "global options first" "$(commit_dir 'git -c user.name=a --no-pager commit --amend' $c)" "$c"
check "git -C absolute" "$(commit_dir 'git -C /other/wt commit -m x' $c)" "/other/wt"
check "git -C relative" "$(commit_dir 'git -C .claude/worktrees/a commit' $c)" "$c/.claude/worktrees/a"
check "cd then commit" "$(commit_dir 'cd /other/wt && git add -A && git commit -m x' $c)" "/other/wt"
check "env prefix" "$(commit_dir 'GIT_AUTHOR_DATE=now git commit -m x' $c)" "$c"
check "time wrapper" "$(commit_dir 'time git commit -m x' $c)" "$c"
check "sudo wrapper" "$(commit_dir 'sudo git commit -m x' $c)" "$c"
check "nice wrapper with flag" "$(commit_dir 'nice -n10 git commit -m x' $c)" "$c"
check "env wrapper with assignment" "$(commit_dir 'env GIT_AUTHOR_DATE=now git commit -m x' $c)" "$c"
check "subshell cd then commit" "$(commit_dir '(cd /other/wt && git commit -m x)' $c)" "/other/wt"
check "log grep commit" "$(commit_dir 'git log --grep=commit' $c)" ""
check "log then word" "$(commit_dir 'git log --oneline | grep commit' $c)" ""
check "show with comment" "$(commit_dir 'git show HEAD --format=%s # commit' $c)" ""
check "commit-tree" "$(commit_dir 'git commit-tree HEAD^{tree}' $c)" ""
check "not git" "$(commit_dir 'echo git commit' $c)" ""

# guard-upstream.sh: asks for upstream paths, in the main checkout and in a
# nested worktree alike; passes Crucible's own paths.
guard() { # guard PATH -> "ask" or "pass"
	local out
	out=$(jq -n --arg f "$1" --arg c "$root" '{cwd: $c, tool_input: {file_path: $f}}' |
		CLAUDE_PROJECT_DIR=$root bash "$here/guard-upstream.sh")
	case "$out" in *'"ask"'*) echo ask ;; *) echo pass ;; esac
}
check "guard upstream file" "$(guard "$root/forge-gui/res/cardsfolder/a/zz_not_logged.txt")" ask
check "guard crucible file" "$(guard "$root/crucible/internal/engine/game.go")" pass
check "guard docs file" "$(guard "$root/docs/crucible/new-note.md")" pass
check "guard porter plan" "$(guard "$root/PORTER_PLAN.md")" pass
check "guard porter plan only at the root" "$(guard "$root/forge-gui/PORTER_PLAN.md")" ask
check "guard .claude file" "$(guard "$root/.claude/skills/x/SKILL.md")" pass
check "guard worktree upstream" "$(guard "$root/.claude/worktrees/zz-test/forge-gui/res/cardsfolder/a/x.txt")" ask

if [ $fails -gt 0 ]; then
	echo "hooks_test: $fails failed"
	exit 1
fi
echo "hooks_test: all passed"
