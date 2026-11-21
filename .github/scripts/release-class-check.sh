#!/usr/bin/env bash
# 38b decision 6 (C23; open item 72): fails when a class in the release build extends or implements a class that is
# not public and sits in another package. Android refuses to load such a class (IllegalAccessError), as it did when R8
# moved kotlin.collections.CollectionsKt__MutableCollectionsJVMKt to the root package. It does not cover a hidden
# method or field reached across packages; only running the app does.
#
# Usage: release-class-check.sh APK_OR_DEX
# dexdump is taken from $DEXDUMP, else the newest build-tools under $ANDROID_HOME (or $ANDROID_SDK_ROOT).
set -euo pipefail

input="${1:?usage: release-class-check.sh APK_OR_DEX}"

dexdump="${DEXDUMP:-}"
if [[ -z "$dexdump" ]]; then
  sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
  dexdump="$(ls "$sdk"/build-tools/*/dexdump 2>/dev/null | sort -V | tail -n 1 || true)"
fi
if [[ -z "$dexdump" || ! -x "$dexdump" ]]; then
  echo "FAIL: dexdump not found (set DEXDUMP or ANDROID_HOME)." >&2
  exit 1
fi
echo "dexdump: $dexdump"

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
if [[ "$input" == *.dex ]]; then
  cp "$input" "$work/"
else
  unzip -q -o "$input" 'classes*.dex' -d "$work"
fi

for dex in "$work"/*.dex; do
  "$dexdump" "$dex"
done > "$work/dump.txt"

awk '
  function pkg(d,   s, i) {
    s = substr(d, 2, length(d) - 2)
    i = match(s, /\/[^\/]*$/)
    return i ? substr(s, 1, i - 1) : ""
  }
  /^  Class descriptor  : / {
    cur = $0; sub(/^[^\047]*\047/, "", cur); sub(/\047.*$/, "", cur)
    classes[cur] = 1; npar[cur] = 0; inif = 0; next
  }
  cur == "" { next }
  /^  Access flags      : / { public[cur] = ($0 ~ /\(.*PUBLIC/) ? 1 : 0; next }
  /^  Superclass        : / {
    p = $0; sub(/^[^\047]*\047/, "", p); sub(/\047.*$/, "", p)
    par[cur, ++npar[cur]] = p; kind[cur, npar[cur]] = "extends"; next
  }
  /^  Interfaces        -/ { inif = 1; next }
  inif && /^    #[0-9]+ *: \047/ {
    p = $0; sub(/^[^\047]*\047/, "", p); sub(/\047.*$/, "", p)
    par[cur, ++npar[cur]] = p; kind[cur, npar[cur]] = "implements"; next
  }
  /^  Static fields/ { inif = 0; cur = "" }
  END {
    n = 0; root = 0; bad = 0
    for (c in classes) {
      n++
      if (pkg(c) == "") root++
      for (k = 1; k <= npar[c]; k++) {
        p = par[c, k]
        if (!(p in classes) || public[p]) continue
        if (pkg(c) != pkg(p)) { print "cannot load: " c " " kind[c, k] " " p " (not public)"; bad++ }
      }
    }
    print "classes read: " n ", in the root package: " root ", that cannot load: " bad
    exit bad > 0 ? 1 : 0
  }
' "$work/dump.txt"
