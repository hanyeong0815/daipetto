# Shared protocol

## Authority and default roles

Follow the user's current request and applicable platform instructions. Shared project constraints live in [AGENTS.md](../AGENTS.md); intended product behavior lives in the existing numbered design docs. Actual implementation facts come from the selected code snapshot. If these disagree, record the discrepancy and resolve only what the task needs. Preserve practice gaps; never implement them merely because a reviewer noticed missing functionality.

| Responsibility | Default owner |
|---|---|
| Exploration, implementation plan, source changes, regression tests, doc sync | Claude Code |
| Independent requirement, architecture, security, concurrency, and test review | Codex |
| Task metadata and accepted decisions | One named coordinator, normally the implementer |
| Scope changes and unresolved product tradeoffs | User |

These are workflow defaults, not claims that one model is inherently better. A direct request to Codex to fix or implement something authorizes that work within its stated scope. Record a role transfer before touching overlapping files. Do not ask again for actions already authorized. If Codex becomes the implementer, it must not describe its own verification as an independent Claude review.

## Context and independence

Provide requirements, acceptance criteria, exact changes, relevant dependencies, short settled design decisions, test commands, and known limitations. Do not require private reasoning traces or entire generation conversations. The reviewer derives checks from requirements before relying on the implementer's conclusions. Different vendors do not guarantee correctness; reproducible evidence decides findings.

Read the selected task and relevant docs only. Resolve hash-bearing docs by prefix using `rg --files docs`. Avoid duplicating API tables, version lists, or project status in this folder. Record disagreements with paths and revision identifiers rather than silently selecting a convenient version.

## Ownership and synchronization

1. Name one coordinator, implementer, and reviewer in TASK.md. The coordinator alone updates TASK.md. Each handoff round belongs to the implementer; each review round belongs to the reviewer. Never edit another writer's conclusion.
2. In one checkout, use sequential turns: implementation, published handoff, review, then fixes. Freeze the reviewed source until that review ends. Markdown ownership is a convention, not an enforced filesystem lock. If unexpected edits appear, stop overlapping writes and reconcile with the active owner.
3. For concurrent work, use separate worktrees, branches, and non-overlapping responsibilities. Worktree files do not live-sync. Transfer committed records and the required code refs through the normal authorized Git workflow, or explicitly provide an immutable local snapshot. Record the transport revision separately from the source revision.
4. Verify the receiver actually has the intended revisions. For a local dirty snapshot, capture staged, unstaged, and untracked content; list SHA-256 hashes for included files and the captured patch. Freeze those files during review. If this cannot be done, report that the review target is not reproducible instead of reviewing a moving target.
5. Write the record completely before marking it ready/final. Once consumed by the other agent, preserve that round; issue a new numbered round for corrections. Never reuse round numbers or claim an old review covers changed code.

Do not switch branches, reset, clean, or overwrite someone else's checkout to acquire a review target. Tests can also write build output or affect a shared database; isolate those resources when running concurrently.

## Task lifecycle

The coordinator updates TASK.md only from evidence, at the next handoff boundary. A review file can signal the next action before TASK.md is updated; there is no need for a central polling process.

| Status | Required evidence / next actor |
|---|---|
| PLANNED | Scope and acceptance criteria recorded; implementer begins |
| IMPLEMENTING | Named implementer owns the source writes |
| READY_FOR_REVIEW | Published handoff identifies the frozen snapshot; reviewer begins |
| CHANGES_REQUESTED | Review contains actionable unresolved findings; implementer responds |
| VERIFIED | Reviewer accepted the same snapshot and required checks have evidence |
| CLOSED | Coordinator records delivery or authorized integration; does not imply deployment |
| BLOCKED | Missing information/resource and exact unblock condition recorded |

Fixes return to IMPLEMENTING and create the next handoff round. A changed requirement invalidates affected acceptance results. If review is intentionally skipped for a low-risk task, the coordinator may move from IMPLEMENTING to CLOSED with `review: skipped`, rationale, and verification evidence; never label this VERIFIED. Resume BLOCKED only after the stated condition changes.

## Evidence and completion

Distinguish tests personally executed from results reported by the other agent. Record working directory, command, code revision, relevant environment, exit code, and outcome. If unrun, say so. Passing mocks do not prove SQL, database constraints, or concurrency behavior. Do not reuse historical test counts as current evidence.

P0/P1 and other required fixes must be resolved before VERIFIED. Nonblocking findings may remain only with a recorded disposition and rationale; material risk acceptance belongs to the user. Unsupported findings should be withdrawn with evidence, not by silent deletion. Repeated disagreement should be reduced to a reproducible case or a focused user question.

Implementation changes follow the existing docs-sync process. Document-only workflow changes require file/link/consistency checks, not application tests. Commit, push, merge, deploy, external messages, and paid integrations follow the actual request and permissions; a handoff file itself grants none of these.
