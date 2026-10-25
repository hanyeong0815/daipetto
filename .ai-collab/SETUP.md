# New machine setup

Goal: the same agent behavior on any computer. Everything below is either committed or restorable with the listed commands. Claude Code's auto-memory (`~/.claude/projects/<checkout-path>/memory/`) is per machine and per path, so it is not a source of rules: durable rules live in `CLAUDE.md`, `AGENTS.md`, `.codex/` and this file.

## 1. Tools

| Tool | Version | Notes |
|---|---|---|
| Git + GitHub CLI | latest | `gh auth login`; without it Claude Code cannot open PRs and gives compare links instead |
| JDK | Temurin 17 (latest 17.0.x) | Spring API |
| Node.js | 24.22.1 (`nvm install 24.22.1`) | frontend; Docker uses `node:24.22.1-alpine` |
| uv + Python | `uv python install 3.12` | analysis API (local 3.12.x; Docker `python:3.12.15-slim`) |
| Docker Desktop | Compose v2 | `docker compose up -d postgres` |
| Claude Code | latest | open the repository folder and trust it |
| Codex CLI | latest, logged in | reviewer; driven through the Claude Code `codex` plugin |

## 2. Claude Code plugins, skills and tools

| Item | How it is restored |
|---|---|
| Plugins (superpowers, context7, code-simplifier, ponytail, codex) | Declared in the committed `.claude/settings.json` (`extraKnownMarketplaces`, `enabledPlugins`). Accept the install prompt when the project is trusted, or `/plugin install <name>@<marketplace>` |
| Project skills `docs-sync`, `spring-new-domain`, `spring-test` | Committed: entry points in `.claude/skills/`, bodies in `.agents/skills/` |
| Agent definitions (`analysis-api-dev`, `docs-sync`, `frontend-dev`, `spring-api-dev`) | Committed: `.claude/agents/*.md`, `.codex/agents/*.toml` |
| Third-party skills (agent-browser, design-taste-frontend, find-skills, mcp-builder) | `npx skills experimental_install` (restores from the committed `skills-lock.json`; files are git-ignored) |
| GSD (Get Shit Done) 1.18.0, optional | `npx get-shit-done-cc@1.18.0`, local Claude Code install. It writes `.claude/gsd-*`, `.claude/commands/`, hooks and their registration in `.claude/settings.local.json` (all git-ignored) |
| Codex MCP server (sequential-thinking) | Committed `.codex/config.toml` uses the Windows path `C:\Program Files\nodejs\npx.cmd`; change `command` on macOS/Linux |
| Dev server for the browser pane | Committed `.claude/launch.json` (`frontend-dev`, port 5173) |

## 3. Environment files

Copy each example and fill in local values (never commit them): `.env` (root, Docker Compose), `spring-api/.env`, `analysis-api/.env`, `frontend/.env.local` from the matching `.env.example`. `INTERNAL_API_KEY` must be the same in all three backend files. The Spring `local` profile reads `.env` from the working directory, so start Spring from `spring-api/`.

## 4. Review prerequisites (Codex sandbox)

Codex runs in a sandbox without network and cannot launch interpreters from the user profile. Prepare once per machine:

```bash
# Gradle cache for `--offline` reviews (online, from spring-api/)
GRADLE_USER_HOME=.gradle/review-home ./gradlew test

# Workspace-local Python for pytest (from analysis-api/; both folders are git-ignored)
UV_PYTHON_INSTALL_DIR="$PWD/.review-python" uv python install 3.12 --no-bin
uv venv --python .review-python/cpython-3.12.*/python.exe .review-venv   # macOS/Linux: .review-python/cpython-3.12.*/bin/python3
uv pip install --python .review-venv -r requirements-dev.txt
```

Run a Codex task: `CODEX_MODEL=gpt-6.1-sol .ai-collab/scripts/run-codex.sh <prompt-file> <log-file> [max-wait-minutes]` (gpt-6.1-sol is the default; use a stronger model only for heavy reasoning). In prompts, point Codex to `GRADLE_USER_HOME=spring-api/.gradle/review-home --offline` and `analysis-api/.review-venv/Scripts/python.exe -m pytest`.

## 5. Verification environment rules

- Never use the database `daipetto` or port 8080: they are the user's own environment. Use scratch databases (`daipetto_<purpose>`) on the `daipetto-postgres` container, ports 18081–18084 for backends/analysis and 5174 for an extra Vite, and drop the scratch databases afterwards. A scratch backend serving a Vite on 5174 needs `--cors.allowed-origins=http://localhost:5174` (CORS allows only 5173 by default).
- Test scripts seed accounts with a localhost-only fixture password (defined in the scripts); type it only into localhost apps and never write it or tokens into evidence or reports.
- Never read `.env` files into a conversation; ask for key names via `.env.example`.
- Before upgrading dependencies, run `node scripts/osv-scan.mjs` (docs/09 §22). Avoid npm versions published less than 3 days ago (`npm install --before=<date>`).

## 6. Notion

- The 13 design docs map 1:1 to the pages under "PetCare Platform"; the page ID is the hash in each `docs/` file name. `docs/` is the source of truth (user decision 2026-10-25).
- `python scripts/notion_prep.py <out-dir>` converts the docs: drops the title line, escapes tag-like text, turns cross-references into page mentions, and warns about bold spans CommonMark will not render (fix those in `docs/` first).
- Update only the pages that changed, and prefer `update_content` for the changed parts over `replace_content` (replacing all 13 pages cost about 570k subagent tokens on 2026-10-25).
