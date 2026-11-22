#!/usr/bin/env bash
# Documentation only for now: every relative link in a tracked Markdown file
# must point to an existing file.
set -euo pipefail

status=0
while IFS= read -r file; do
  dir=$(dirname "$file")
  while IFS= read -r target; do
    case "$target" in
      http://*|https://*|mailto:*|'#'*) continue ;;
    esac
    path=${target%%#*}
    if [ -z "$path" ]; then
      continue
    fi
    if [ ! -e "$dir/$path" ]; then
      echo "$file: $target"
      status=1
    fi
  done < <(grep -oE '\]\([^)]+\)' "$file" | sed -E 's/^\]\((.*)\)$/\1/')
done < <(git ls-files '*.md')

# Stage 0 adds the Maven and pnpm build, test and lint commands here.
exit "$status"
