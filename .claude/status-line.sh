#!/bin/bash
# /Users/dmitry.danilov/StudioProjects/please-stop/.claude/status-line.sh

# 1. Capture the JSON input from Claude Code
JSON_INPUT=$(cat)

# 2. Extract official parameters using jq
# Model ID (simplified)
MODEL=$(echo "$JSON_INPUT" | jq -r '.model.id // "unknown"' | sed 's/claude-3-7-//;s/claude-3-5-//')

# Cost (total session spend)
COST=$(echo "$JSON_INPUT" | jq -r '.cost.total_cost_usd // 0' | xargs printf "%.2f")

# Context Usage (Official Docs recommend .context_window.current_usage)
# This includes: input_tokens, output_tokens, cache_read, cache_creation
CUR_INPUT=$(echo "$JSON_INPUT" | jq -r '.context_window.current_usage.input_tokens // 0')
CACHE_READ=$(echo "$JSON_INPUT" | jq -r '.context_window.current_usage.cache_read_input_tokens // 0')

# Calculate Total "Active" Context and Percentage (Max 200k)
TOTAL_ACTIVE=$(( CUR_INPUT + CACHE_READ ))
PCT=$(( TOTAL_ACTIVE * 100 / 200000 ))

# 3. Formating and Colors
if [ "$PCT" -gt 85 ]; then COLOR="\033[31m" # Red (Danger)
elif [ "$PCT" -gt 60 ]; then COLOR="\033[33m" # Yellow (Warning)
else COLOR="\033[32m"; fi # Green (Safe)

# 4. Final Concise Output
# CTX: Current Tokens | CH: Cache Hit Tokens | %: Usage
echo -e "🤖 ${MODEL} | \$${COST} | CTX:${COLOR}$((TOTAL_ACTIVE/1000))k (${PCT}%)\033[0m | 📥$((CACHE_READ/1000))k"