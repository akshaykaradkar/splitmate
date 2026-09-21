#!/bin/bash
# runs the gbreeze EVAL.yaml against the google3 skill eval CLI.
#
# usage:
#   bash .agents/skills/gbreeze/scripts/run-eval.sh [extra flags...]
#
# prerequisites:
#   - must be run from the jetbreeze repo root (or any subdir)
#   - blaze must be available (run from cloudtop)
#   - gcert must be valid
#
# examples:
#   bash .agents/skills/gbreeze/scripts/run-eval.sh
#   bash .agents/skills/gbreeze/scripts/run-eval.sh --model=pro
#   bash .agents/skills/gbreeze/scripts/run-eval.sh --output_dir=/tmp/gbreeze_eval/

set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
SKILL_DIR="$REPO_ROOT/.agents/skills/gbreeze"
TEMPLATE="$SKILL_DIR/EVAL.yaml"

# find google3 workspace — blaze + suite file must be inside one
G3_DIR=""
for candidate in \
  "/google/src/cloud/$(whoami)/head/google3" \
  "/google/src/head/depot/google3"; do
  if [ -d "$candidate" ]; then
    G3_DIR="$candidate"
    break
  fi
done

if [ -z "$G3_DIR" ]; then
  echo "error: no google3 workspace found. run from a cloudtop." >&2
  exit 1
fi

if ! command -v blaze &>/dev/null; then
  echo "error: blaze not found. run this from a cloudtop." >&2
  exit 1
fi

if [ ! -f "$TEMPLATE" ]; then
  echo "error: EVAL.yaml not found at $TEMPLATE" >&2
  exit 1
fi

# the eval CLI requires the suite file inside google3, so we place
# the resolved EVAL.yaml under a temp skill dir in the workspace
EVAL_DEST="$G3_DIR/learning/gemini/agents/skills/_gbreeze_eval"
mkdir -p "$EVAL_DEST"

# copy the full skill dir so the skills: field resolves correctly
cp -r "$SKILL_DIR"/* "$EVAL_DEST/"

# write the resolved EVAL.yaml (skills: points to the copy in google3)
sed "s|__SKILL_DIR__|$EVAL_DEST|g" "$TEMPLATE" > "$EVAL_DEST/EVAL.yaml"

echo "skill dir:     $SKILL_DIR"
echo "google3 dir:   $G3_DIR"
echo "eval dest:     $EVAL_DEST"
echo "running eval..."
echo ""

cd "$G3_DIR"
blaze run //learning/gemini/agents/evaluation/eval_runner:skill_eval_cli -- \
  --suite="$EVAL_DEST/EVAL.yaml" \
  --ensure_auth \
  "$@"

# cleanup
rm -rf "$EVAL_DEST"
