#!/bin/bash
# lists all gbreeze components and whether they have React wrappers
# usage: bash .agents/skills/gbreeze/scripts/list-components.sh

GBREEZE_DIR="$(git rev-parse --show-toplevel 2>/dev/null || echo ".")/gbreeze/components"

if [ ! -d "$GBREEZE_DIR" ]; then
  echo "error: gbreeze/components not found at $GBREEZE_DIR" >&2
  exit 1
fi

printf "%-22s %-50s %s\n" "COMPONENT" "DIRECTORY" "REACT WRAPPER"
printf "%-22s %-50s %s\n" "---------" "---------" "-------------"

for dir in "$GBREEZE_DIR"/*/; do
  [ -d "$dir" ] || continue
  name=$(basename "$dir")
  # skip internal utils
  [ "$name" = "internal" ] && continue

  wrapper=$(find "$dir" -maxdepth 1 -name "*.tsx" ! -name "*.stories.tsx" ! -name "*.test.tsx" | head -1)
  if [ -n "$wrapper" ]; then
    # make path relative to repo root
    rel_wrapper=$(echo "$wrapper" | sed "s|.*gbreeze/|@gbreeze/|")
    # strip .tsx extension for import path
    import_path="${rel_wrapper%.tsx}"
    printf "%-22s %-50s %s\n" "$name" "gbreeze/components/$name/" "$import_path"
  else
    printf "%-22s %-50s %s\n" "$name" "gbreeze/components/$name/" "(no wrapper)"
  fi
done
