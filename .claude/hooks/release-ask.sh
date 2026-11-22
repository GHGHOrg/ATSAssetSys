#!/usr/bin/env bash
# 38b step 4 (CR4): a PreToolUse hook for the Bash and PowerShell tools. A command that installs on the phone, tags,
# makes a GitHub release or builds the release asks every time, even where an allow rule would let it run:
#   adb ... install (uninstall included), git ... tag, gh release, assembleRelease / bundleRelease / packageRelease.
# It reads the whole tool call (JSON on stdin) and matches it as text, so a command inside another one still asks;
# a description naming one of them asks as well. Anything else gets no answer from this hook (exit 0, no output).
input="$(cat)"

if printf '%s' "$input" | grep -Eiq \
  -e 'adb.*install' \
  -e '(^|[^[:alnum:]_-])git([[:space:]]+[^[:space:];&|]+)*[[:space:]]+tag([^[:alnum:]_-]|$)' \
  -e '(^|[^[:alnum:]_-])gh[[:space:]]+release([^[:alnum:]_-]|$)' \
  -e '(assemble|bundle|package)Release'; then
  printf '%s\n' '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"ask","permissionDecisionReason":"Release command (38b hook): adb install, git tag, gh release or a release build. Answer each one yourself."}}'
fi
exit 0
