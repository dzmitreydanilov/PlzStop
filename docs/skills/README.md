# Shared Skill Source

This directory is the canonical source of truth for all agent skills used by this repo.

Generated wrappers are kept in sync by `scripts/sync-agent-assets.sh`:

- Claude Code skill symlink: `.claude/skills/` -> `docs/skills/`
- Claude Code entry file: `.claude/CLAUDE.md`
- Codex entry file: `AGENTS.md`

Edit the files in this directory, then run the sync script.
