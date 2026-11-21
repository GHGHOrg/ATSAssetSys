#!/usr/bin/env bash
# 38b decision 4 (AC-63, the CI part): a release's tag equals v<versionName>, versionName is MAJOR.MINOR.PATCH
# (38a decision 2), and versionCode is higher than the newest earlier release's (C22: Android refuses a downgrade).
#
# Usage: release-version-check.sh [TAG] [BUILD_FILE]
#   TAG         the pushed tag, for example v0.38.0; empty for a dry run, which checks v<versionName> instead.
#   BUILD_FILE  default app/build.gradle.kts.
# Run from the repository root. Earlier releases are read from the git tags v*; with none, the versionCode check
# passes with a note (the first release).
set -euo pipefail

tag="${1:-}"
build_file="${2:-app/build.gradle.kts}"

read_value() {
  # $1: the key (versionCode or versionName); stdin: the build file. Prints the value of the first "key = value".
  sed -n -E "s/^[[:space:]]*$1[[:space:]]*=[[:space:]]*\"?([^\"[:space:]]+)\"?[[:space:]]*\$/\1/p" | head -n 1
}

version_name="$(read_value versionName < "$build_file")"
version_code="$(read_value versionCode < "$build_file")"

if [[ -z "$version_name" || -z "$version_code" ]]; then
  echo "FAIL: versionName or versionCode not found in $build_file." >&2
  exit 1
fi
if ! [[ "$version_name" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "FAIL: versionName $version_name is not MAJOR.MINOR.PATCH." >&2
  exit 1
fi
if ! [[ "$version_code" =~ ^[0-9]+$ ]]; then
  echo "FAIL: versionCode $version_code is not a number." >&2
  exit 1
fi

expected_tag="v$version_name"
if [[ -z "$tag" ]]; then
  echo "Dry run: checking as for tag $expected_tag."
  tag="$expected_tag"
elif [[ "$tag" != "$expected_tag" ]]; then
  echo "FAIL: tag $tag does not equal v<versionName>, $expected_tag." >&2
  exit 1
fi
echo "Tag $tag equals v<versionName>; versionCode $version_code."

previous_tag=""
while IFS= read -r candidate; do
  if [[ -n "$candidate" && "$candidate" != "$tag" ]]; then
    previous_tag="$candidate"
    break
  fi
done < <(git tag --list 'v*' --sort=-v:refname)

if [[ -z "$previous_tag" ]]; then
  echo "No earlier release tag (v*): the first release; versionCode not compared."
  exit 0
fi

previous_code="$(git show "$previous_tag:$build_file" | read_value versionCode)"
if ! [[ "$previous_code" =~ ^[0-9]+$ ]]; then
  echo "FAIL: versionCode not found in $build_file at $previous_tag." >&2
  exit 1
fi
if (( version_code <= previous_code )); then
  echo "FAIL: versionCode $version_code is not higher than $previous_tag's $previous_code." >&2
  exit 1
fi
echo "versionCode $version_code is higher than $previous_tag's $previous_code."
