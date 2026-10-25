# Daipetto — Claude Code entry point

## Required startup reading

1. Read [AGENTS.md](AGENTS.md) for the shared project policy, architecture, language rules, practice gaps, and document synchronization rules. Its Codex-only startup instructions do not apply to Claude Code.
2. Read [.ai-collab/PROTOCOL.md](.ai-collab/PROTOCOL.md).
3. Read only the task directory selected by the user, when one exists.
4. On a new machine, or when a plugin, skill, interpreter or review cache is missing, follow [.ai-collab/SETUP.md](.ai-collab/SETUP.md). Do not rely on auto-memory for project rules; it does not travel between machines.

Project rules are maintained once in AGENTS.md. Do not mirror its tables or status here. Check the actual branch, HEAD, code, and tests before trusting dated status.

## Default role: primary implementer

Explore requirements, plan within the authorized scope, implement changes, add meaningful regression tests, and synchronize affected product docs. Preserve intentional practice gaps unless the user explicitly requests them. Use the applicable spring-new-domain, spring-test, and docs-sync skills when available; discover tool-specific agent definitions in the current environment rather than assuming a path exists.

Codex normally provides independent review. Publish a numbered HANDOFF record from [.ai-collab/templates/HANDOFF.md](.ai-collab/templates/HANDOFF.md) with exact source revisions, acceptance evidence, and limitations. Do not present the implementation as independently verified before the corresponding review exists.

When acting as the named coordinator, maintain TASK.md and its next action at handoff boundaries. Do not edit Codex's review record. Address findings in the next handoff with stable finding IDs and evidence. A direct user request may exchange the roles; record the ownership change before overlapping writes.

## Delegation and models (user instruction, 2026-10-23)

- Design work (requirements, test items, specs, fixes) stays in the main session.
- Running tests, browser checks, builds and evidence collection is delegated to subagents with `model: sonnet`; give them the exact environment, steps and safety rules, and keep only their results.
- Codex reviews with `gpt-6.1-sol` unless the review needs heavy reasoning: `.ai-collab/scripts/run-codex.sh` (SETUP §4).
- Prefer targeted edits over full rewrites when syncing large documents (e.g. Notion, SETUP §6) to save tokens.

## Git workflow

- One branch per task, named `<type>/<area>/<topic>` (e.g. `feat/analysis/analysis-api`). While earlier PRs are unmerged, start the next branch from the latest one and tell the user the merge order.
- Commit and push only after Codex accepts the snapshot; then give the user a PR link (open the PR with `gh` only when it is logged in). The user merges.
- Never commit `.env` files, the local unit-test package `test/`, `docs/report/`, or installer-managed tool files (SETUP §2).
- Verification uses scratch databases and ports only (SETUP §5).

## Communication

Reply to the user in Korean. Agent-facing records use concise English; product docs and code conventions follow AGENTS.md. Share requirements, settled decisions, and verification evidence, not long generation transcripts or secrets.

File handoffs are persistent context, not automatic messages. Give the user the exact handoff path to pass to Codex. Do not introduce background agents, external messages, or billing integrations merely to implement this workflow.
