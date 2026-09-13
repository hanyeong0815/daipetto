# Shared collaboration workspace

This folder is the shared, repository-local handoff channel for Claude Code and Codex. Use concise English for records and Korean when speaking to the user.

## Entry points

- [Protocol](PROTOCOL.md): roles, ownership, evidence, and lifecycle.
- [Task template](templates/TASK.md): scope, acceptance criteria, and coordinator.
- [Handoff template](templates/HANDOFF.md): implementation snapshot and verification.
- [Review template](templates/REVIEW.md): findings tied to that snapshot.
- [Codex role](../.codex/ROLE.md) and [review procedure](../.codex/REVIEW.md).
- [Shared project policy](../AGENTS.md): architecture, language, and practice gaps.

## Design choice

Use task folders with separate writers rather than one continuously edited chat log or global STATUS.md. This keeps evidence small, makes Git history useful, and avoids both agents replacing each other's conclusions. Existing numbered docs remain the product specification; do not copy them here.

```text
.ai-collab/tasks/<task-id>/
  TASK.md                 coordinator-owned scope and final status
  HANDOFF-001.md           implementer-owned snapshot
  REVIEW-001.md            reviewer-owned response
  HANDOFF-002.md           fixes and a new snapshot, if needed
  REVIEW-002.md            re-review of the new snapshot
```

Templates are not active tasks. Choose a task explicitly; never guess that the most recently modified folder is the user's task. A small standalone review may use the conversation instead of creating files, unless persistent sharing was requested.

Use a unique task ID such as YYYY-MM-DD-short-topic, adding a suffix if needed. Create its folder under `.ai-collab/tasks/` when work needs a persistent handoff. Keep completed records in place for stable references. No active task or global status file is created by this setup.

## Skill and agent files

`.agents/skills/` contains the maintained project skill bodies. `.claude/skills/` contains short entry points that read those bodies, preserving Claude's skill names without duplicating the instructions. Update the shared body when a procedure changes. Codex discovers repository skills in `.agents/skills/`; Claude discovers its project entry points in `.claude/skills/`. See the [Codex skill documentation](https://learn.chatgpt.com/docs/build-skills) and [Claude skill documentation](https://code.claude.com/docs/en/skills).

The `.claude/agents/` Markdown and `.codex/agents/` TOML files are tool-specific specialist definitions, not handoff records. Keep their shared-policy references pointing to AGENTS.md. Their presence does not authorize delegation or replace the task's assigned roles.

## Starting a task

Ask Claude Code:

```text
Read CLAUDE.md and .ai-collab/PROTOCOL.md. Create a task folder for this request,
record its scope and acceptance criteria in TASK.md, implement within that scope,
and produce HANDOFF-001.md with the exact code revision and test evidence.
```

Then ask Codex, replacing the task path:

```text
Read AGENTS.md and .codex/ROLE.md. Review .ai-collab/tasks/<task-id>/HANDOFF-001.md
against TASK.md and the referenced code snapshot. Write REVIEW-001.md.
This request is review-only.
```

Then ask the implementer to address the identified findings and publish a new handoff. Role changes must be explicit in the request and TASK.md.

Files provide persistent context, not automatic delivery: saving a file does not wake either agent, run a test, grant permission, or synchronize another worktree. The user opens/resumes the receiving task and names the handoff path. No watcher, API billing, or external messaging integration is installed by this setup.

Commit these instructions and task records with normal project changes when requested. Until committed or otherwise explicitly transferred, untracked records exist only in this checkout. Do not include credentials, personal data, or unrestricted console transcripts.
