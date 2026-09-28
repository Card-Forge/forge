#!/usr/bin/env bash
# Makes sure `rtk` is on PATH, installing the static musl Linux build from
# rtk-ai/rtk's GitHub releases when it's missing. Claude Code on the web's
# container has no Homebrew, unlike a local machine where `brew install rtk`
# already put it on PATH -- this is the cloud-only equivalent of that.
# No-op, well under a second, once installed.
set -euo pipefail
version=v0.50.0
sha256=bc2b8902b0d9c796c82ef45f16ae2307e17757afeca5ee156235a3dc7bda5f89

command -v rtk >/dev/null 2>&1 && exit 0

bindir="$HOME/.local/bin"
mkdir -p "$bindir"
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

asset="rtk-x86_64-unknown-linux-musl.tar.gz"
curl -fsSL "https://github.com/rtk-ai/rtk/releases/download/${version}/${asset}" -o "$tmp/rtk.tar.gz"

got=$(shasum -a 256 "$tmp/rtk.tar.gz" | cut -d' ' -f1)
if [ "$got" != "$sha256" ]; then
	echo "ensure-rtk: checksum mismatch for $asset: got $got, want $sha256" >&2
	exit 1
fi

tar -xzf "$tmp/rtk.tar.gz" -C "$tmp"
install -m 755 "$tmp/rtk" "$bindir/rtk"

case ":$PATH:" in
*":$bindir:"*) ;;
*)
	echo "ensure-rtk: installed to $bindir, which is not on PATH -- add it" >&2
	;;
esac
