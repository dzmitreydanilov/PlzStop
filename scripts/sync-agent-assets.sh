#!/usr/bin/env bash
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
DOC_SKILLS="$ROOT/docs/skills"
AGENTS_FILE="$ROOT/AGENTS.md"
CLAUDE_FILE="$ROOT/.claude/CLAUDE.md"
CODEX_FILE="$ROOT/.codex/CODEX.md"
CLAUDE_SKILLS="$ROOT/.claude/skills"
CLAUDE_SKILLS_TARGET="../docs/skills"

write_wrapper() {
  local file="$1"
  local title="$2"
  local agent_rules_ref="$3"
  local skills_ref="$4"

  mkdir -p "$(dirname "$file")"
  cat >"$file" <<EOF
# $title

Canonical source of truth:
- [Agent rules]($agent_rules_ref)
- [Skills]($skills_ref)

Edit the files above, then rerun \`scripts/sync-agent-assets.sh\`.
EOF
}

sync_skill_link() {
  local target_root="$1"
  local link_target="$2"

  rm -rf "$target_root"
  ln -s "$link_target" "$target_root"
}

write_wrapper "$AGENTS_FILE" "PlzStop Shared Agent Instructions" "./docs/agent-rules.md" "./docs/skills/"
write_wrapper "$CLAUDE_FILE" "PlzStop Claude Code Instructions" "../docs/agent-rules.md" "../docs/skills/"
write_wrapper "$CODEX_FILE" "PlzStop Codex Instructions" "../docs/agent-rules.md" "../docs/skills/"

sync_skill_link "$CLAUDE_SKILLS" "$CLAUDE_SKILLS_TARGET"
