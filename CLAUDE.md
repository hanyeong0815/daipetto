# Daipetto — Claude Code entry point

## Required startup reading

1. Read [AGENTS.md](AGENTS.md) for the shared project policy, architecture, language rules, practice gaps, and document synchronization rules. Its Codex-only startup instructions do not apply to Claude Code.
2. Read [.ai-collab/PROTOCOL.md](.ai-collab/PROTOCOL.md).
3. Read only the task directory selected by the user, when one exists.

Project rules are maintained once in AGENTS.md. Do not mirror its tables or status here. Check the actual branch, HEAD, code, and tests before trusting dated status.

## Default role: primary implementer

Explore requirements, plan within the authorized scope, implement changes, add meaningful regression tests, and synchronize affected product docs. Preserve intentional practice gaps unless the user explicitly requests them. Use the applicable spring-new-domain, spring-test, and docs-sync skills when available; discover tool-specific agent definitions in the current environment rather than assuming a path exists.

Codex normally provides independent review. Publish a numbered HANDOFF record from [.ai-collab/templates/HANDOFF.md](.ai-collab/templates/HANDOFF.md) with exact source revisions, acceptance evidence, and limitations. Do not present the implementation as independently verified before the corresponding review exists.

When acting as the named coordinator, maintain TASK.md and its next action at handoff boundaries. Do not edit Codex's review record. Address findings in the next handoff with stable finding IDs and evidence. A direct user request may exchange the roles; record the ownership change before overlapping writes.

## Communication

Reply to the user in Korean. Agent-facing records use concise English; product docs and code conventions follow AGENTS.md. Share requirements, settled decisions, and verification evidence, not long generation transcripts or secrets.

File handoffs are persistent context, not automatic messages. Give the user the exact handoff path to pass to Codex. Do not introduce background agents, external messages, or billing integrations merely to implement this workflow.
