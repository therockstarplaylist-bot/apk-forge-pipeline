# Agent team

Framework: **OAT (Open Agent Teams)** — https://github.com/Root-IO-Labs/open-agent-teams

## Why OAT (not KageOps, not Captain Claw)

| | OAT | KageOps | Captain Claw |
|---|---|---|---|
| License | MIT | AGPL-3.0 | MIT |
| GitHub PRs / CI watch | Native via `gh` | Product lifecycle, not merge-queue first | Tools exist, not the core loop |
| Ollama default | Yes (`ollama:model`) | Possible | Yes |
| ARM64 Linux | Published binaries | Source | pip / docker |
| Always-on | `oat start` daemon | Local engine | Web UI + extra ports |
| Roles we need | Supervisor, worker, verification, merge-queue | Sensei + 8 Autonauts (heavier) | Flight Deck specialists |

OAT maps 1:1 onto Manager / Coder / Tester / Designer and already watches GitHub Actions before merge.

## Roles

- **Manager** — OAT supervisor + merge-queue. Picks `agent` issues, merges on green.
- **Coder** — OAT worker. Implements, opens the PR.
- **Tester** — OAT verification. Reports Actions pass/fail. Never merges.
- **Designer** — OAT worker scoped to `app/src/main/res/**`.

All four read `OLLAMA_HOST` / `OLLAMA_MODEL` by default. Set `LLM_API_BASE` + `LLM_API_KEY` to swap every role onto a cloud API without code changes.

## Local loop without OAT

`python3.11 agents/loop.py --once` uses the GitHub API only (stdlib). OAT is the long-running team on the VM.
