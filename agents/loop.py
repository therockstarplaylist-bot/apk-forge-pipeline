#!/usr/bin/env python3
"""Forge agent loop — Manager, Coder, Tester, Designer.

Default LLM: local Ollama (OLLAMA_HOST).
Cloud swap: set LLM_API_BASE + LLM_API_KEY (+ optional LLM_MODEL).
Never logs secret values. Stdlib only.
"""
from __future__ import annotations

import json
import os
import sys
import time
import urllib.error
import urllib.request
from typing import Any

OWNER = os.environ.get("GITHUB_OWNER", "therockstarplaylist-bot")
REPO = os.environ.get("GITHUB_REPO", "apk-forge-pipeline")
API = "https://api.github.com"
POLL_SECONDS = int(os.environ.get("FORGE_POLL_SECONDS", "30"))


def _token() -> str:
    token = os.environ.get("GITHUB_TOKEN", "").strip()
    if not token:
        print("GITHUB_TOKEN is not set", file=sys.stderr)
        sys.exit(1)
    return token


def _redact(url: str) -> str:
    return url.split("?")[0]


def gh(method: str, path: str, body: dict[str, Any] | None = None) -> Any:
    req = urllib.request.Request(
        f"{API}{path}",
        data=None if body is None else json.dumps(body).encode(),
        method=method,
        headers={
            "Accept": "application/vnd.github+json",
            "Authorization": f"Bearer {_token()}",
            "X-GitHub-Api-Version": "2022-11-28",
            "User-Agent": "apk-forge-loop",
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            raw = resp.read()
            return json.loads(raw) if raw else {}
    except urllib.error.HTTPError as exc:
        print(f"github {method} {_redact(path)} -> {exc.code}", file=sys.stderr)
        raise


def llm_chat(role: str, prompt: str) -> str:
    """Ollama by default; OpenAI-compatible cloud if LLM_API_BASE is set."""
    cloud_base = os.environ.get("LLM_API_BASE", "").rstrip("/")
    if cloud_base:
        model = os.environ.get("LLM_MODEL") or "gpt-4o-mini"
        key = os.environ.get("LLM_API_KEY", "")
        payload = {
            "model": model,
            "messages": [
                {"role": "system", "content": f"You are the {role} agent for {OWNER}/{REPO}."},
                {"role": "user", "content": prompt},
            ],
            "temperature": 0.2,
        }
        req = urllib.request.Request(
            f"{cloud_base}/chat/completions",
            data=json.dumps(payload).encode(),
            method="POST",
            headers={
                "Content-Type": "application/json",
                "Authorization": f"Bearer {key}",
            },
        )
        with urllib.request.urlopen(req, timeout=120) as resp:
            data = json.loads(resp.read())
        return data["choices"][0]["message"]["content"]

    host = os.environ.get("OLLAMA_HOST", "http://127.0.0.1:11434").rstrip("/")
    model = os.environ.get("OLLAMA_MODEL", "llama3.1:8b")
    payload = {
        "model": model,
        "stream": False,
        "messages": [
            {"role": "system", "content": f"You are the {role} agent for {OWNER}/{REPO}."},
            {"role": "user", "content": prompt},
        ],
    }
    req = urllib.request.Request(
        f"{host}/api/chat",
        data=json.dumps(payload).encode(),
        method="POST",
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(req, timeout=180) as resp:
        data = json.loads(resp.read())
    return data.get("message", {}).get("content", "")


def manager_pick_issue() -> dict[str, Any] | None:
    issues = gh("GET", f"/repos/{OWNER}/{REPO}/issues?state=open&labels=agent&per_page=10")
    issues = [i for i in issues if "pull_request" not in i]
    if not issues:
        print("manager: no open agent issues")
        return None
    issue = issues[0]
    print(f"manager: picked issue #{issue['number']} {issue['title']}")
    return issue


def tester_wait_for_run(sha: str, timeout_s: int = 1200) -> tuple[bool, str]:
    deadline = time.time() + timeout_s
    last_url = ""
    while time.time() < deadline:
        runs = gh("GET", f"/repos/{OWNER}/{REPO}/actions/runs?head_sha={sha}&per_page=5")
        items = runs.get("workflow_runs") or []
        if items:
            run = items[0]
            last_url = run.get("html_url", "")
            status, conclusion = run.get("status"), run.get("conclusion")
            print(f"tester: run {run.get('id')} status={status} conclusion={conclusion}")
            if status == "completed":
                return conclusion == "success", last_url
        time.sleep(POLL_SECONDS)
    return False, last_url


def manager_merge(pr_number: int) -> None:
    gh("PUT", f"/repos/{OWNER}/{REPO}/pulls/{pr_number}/merge", {"merge_method": "squash"})
    print(f"manager: merged PR #{pr_number}")


def once() -> int:
    issue = manager_pick_issue()
    if issue is None:
        return 0
    print("loop: coder/tester/manager continue via GitHub PRs opened against this issue")
    print(f"loop: issue url {issue.get('html_url')}")
    return 0


def main() -> int:
    if "--once" in sys.argv:
        return once()
    print("forge loop watching for agent issues")
    while True:
        once()
        time.sleep(POLL_SECONDS)


if __name__ == "__main__":
    raise SystemExit(main())
