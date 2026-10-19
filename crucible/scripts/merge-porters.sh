#!/usr/bin/env bash
# Merges worktree-subagent branches into the current branch and cleans up
# after them.
#
#   merge-porters.sh list               every agent worktree: branch, commits
#                                       not on HEAD, uncommitted files
#   merge-porters.sh merge BRANCH...    cherry-pick each branch's commits onto
#                                       HEAD, resolving the conflicts parallel
#                                       porters always cause (below), then
#                                       regenerate the registry and rewrite the
#                                       resolved-API count in the docs
#   merge-porters.sh commit BRANCH MSG  commit a porter's uncommitted worktree
#                                       (porters cannot run git under the rtk
#                                       hook, see their agent definitions);
#                                       drops PORTER_PLAN.md, runs the commit
#                                       hook's gates, MSG should carry the
#                                       attribution trailer
#   merge-porters.sh cleanup BRANCH...  remove each branch's worktree and
#                                       delete the branch -- refused while the
#                                       branch has a commit HEAD lacks or its
#                                       worktree has uncommitted changes
#
# Conflicts resolved automatically:
#   internal/engine/registry_gen.go   generated: take either side, regenerate
#   append-only docs (index rows,     union of both sides: every porter adds
#   defect/patch tables)              rows, none edits another's
#   CLAUDE.md, the in-progress plan   HEAD's side; the count sentence is
#                                     rewritten from the registry afterwards
# Any other conflict stops the run with the cherry-pick left in progress:
# resolve it, `git cherry-pick --continue`, then rerun merge with the
# remaining branches.
#
# merge leaves the result uncommitted only for the regenerated registry and
# counts; run gates.sh full and commit that as the batch's merge commit.
set -euo pipefail

root=$(git rev-parse --show-toplevel)
cd "$root"

union_docs='docs/crucible/porting/port-log/game-state.md
docs/crucible/porting/forge-java-defects.md
docs/crucible/porting/upstream-patches.md'
ours_docs='CLAUDE.md
docs/crucible/00-master-implementation-plan-in-progress.md'

wt_of() { # worktree path checked out on branch $1, if any
	git worktree list --porcelain | awk -v b="refs/heads/$1" '
		/^worktree /{p=substr($0,10)} $0=="branch "b{print p}'
}

ahead() { git rev-list --count "HEAD..$1"; }

# unique BRANCH: commits on BRANCH whose change HEAD lacks. A commit counts as
# present when HEAD has a patch-identical one, or one merge picked with -x (a
# conflict resolution changes the patch, so only the trailer still links them).
unique() {
	local n=0 sha
	for sha in $(git cherry HEAD "$1" | sed -n 's/^+ //p'); do
		git log HEAD --format=%H -F --grep="cherry picked from commit $sha" -1 | grep -q . || n=$((n + 1))
	done
	echo $n
}

resolve_conflicts() {
	local f unresolved=0
	while IFS= read -r f; do
		[ -n "$f" ] || continue
		if [ "$f" = crucible/internal/engine/registry_gen.go ]; then
			git checkout --theirs -- "$f"
		elif grep -qxF "$f" <<<"$union_docs" || [[ $f == docs/crucible/porting/port-log/game-state/effects-*.md ]]; then
			local b o t
			b=$(mktemp) o=$(mktemp) t=$(mktemp)
			git show ":1:$f" >"$b" 2>/dev/null || : >"$b"
			git show ":2:$f" >"$o"
			git show ":3:$f" >"$t"
			git merge-file --union "$o" "$b" "$t" || true
			cp "$o" "$f"
			rm -f "$b" "$o" "$t"
		elif grep -qxF "$f" <<<"$ours_docs"; then
			git checkout --ours -- "$f"
		else
			echo "conflict needs a human: $f" >&2
			unresolved=1
			continue
		fi
		git add -- "$f"
	done < <(git diff --name-only --diff-filter=U)
	return $unresolved
}

cmd=${1:-}
shift || true
case "$cmd" in
list)
	git worktree list --porcelain | awk '/^worktree /{p=substr($0,10)} /^branch /{print p" "substr($0,19)}' |
		grep '/.claude/worktrees/' | while read -r path branch; do
		dirty=$(git -C "$path" status --porcelain | wc -l | tr -d ' ')
		printf '%-45s ahead=%s not-on-HEAD=%s uncommitted=%s\n' "$branch" "$(ahead "$branch")" "$(unique "$branch")" "$dirty"
	done
	;;
commit)
	[ $# -eq 2 ] || { echo "usage: merge-porters.sh commit BRANCH MSG" >&2; exit 64; }
	wt=$(wt_of "$1")
	[ -n "$wt" ] || { echo "no worktree on branch $1" >&2; exit 1; }
	rm -f "$wt/PORTER_PLAN.md"
	[ -n "$(git -C "$wt" status --porcelain)" ] || { echo "$1: nothing to commit"; exit 0; }
	git -C "$wt" add -A
	git -C "$wt" commit -q -m "$2"
	git -C "$wt" log --oneline -1
	;;
merge)
	[ $# -gt 0 ] || { echo "usage: merge-porters.sh merge BRANCH..." >&2; exit 64; }
	[ -z "$(git status --porcelain --untracked-files=no)" ] || { echo "working tree not clean" >&2; exit 1; }
	for br in "$@"; do
		commits=$(git rev-list --reverse --no-merges "HEAD..$br")
		echo "== $br: $(printf '%s' "$commits" | grep -c . || true) commit(s)"
		for c in $commits; do
			if ! git cherry-pick -x "$c" >/dev/null 2>&1; then
				if [ -z "$(git diff --name-only --diff-filter=U)" ] && git diff --cached --quiet; then
					git cherry-pick --skip # already applied
					continue
				fi
				resolve_conflicts || { echo "stopped in $br at $(git log -1 --format=%h "$c")" >&2; exit 1; }
				GIT_EDITOR=true git cherry-pick --continue >/dev/null
			fi
		done
	done
	out=$(cd crucible && go generate -run genregistry ./internal/engine)
	n=$(printf '%s' "$out" | sed -nE 's/.* ([0-9]+) resolved script-driven/\1/p')
	sed -i.bak -E "s/[0-9]+ of the corpus's ([0-9]+) script-driven/$n of the corpus's \1 script-driven/" \
		CLAUDE.md docs/crucible/00-master-implementation-plan-in-progress.md
	rm -f CLAUDE.md.bak docs/crucible/00-master-implementation-plan-in-progress.md.bak
	echo "merged; $n resolved script-driven APIs. Next: gates.sh full, fix '## Not ported yet', commit."
	git status --short
	;;
cleanup)
	[ $# -gt 0 ] || { echo "usage: merge-porters.sh cleanup BRANCH..." >&2; exit 64; }
	for br in "$@"; do
		wt=$(wt_of "$br")
		if [ "$(unique "$br")" != 0 ]; then
			echo "keep $br: $(unique "$br") commit(s) not on HEAD" >&2
			continue
		fi
		if [ -n "$wt" ] && [ -n "$(git -C "$wt" status --porcelain)" ]; then
			echo "keep $br: uncommitted changes in $wt" >&2
			continue
		fi
		[ -z "$wt" ] || git worktree remove "$wt"
		git branch -D "$br" >/dev/null
		echo "removed $br${wt:+ and $wt}"
	done
	git worktree prune
	;;
*)
	sed -n '2,25p' "$0" | sed 's/^# \{0,1\}//'
	exit 64
	;;
esac
