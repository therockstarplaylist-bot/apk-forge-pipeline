# apk-forge-pipeline

Public, always-free Android **debug APK** factory.

- **Builds:** GitHub Actions `assembleDebug` on every push. Standard hosted runners on public repos have **unlimited minutes** ([GitHub Actions billing](https://docs.github.com/en/billing/concepts/product-billing/github-actions)).
- **App:** `app/` — Kotlin, AGP 8.5.2, JDK 17, compileSdk 34.
- **Agents:** OAT team (Manager, Coder, Tester, Designer) in `agents/`.
- **Host:** Oracle Always Free ARM bootstrap lives in `infra/` — **not provisioned yet** (see below).

Repo: https://github.com/therockstarplaylist-bot/apk-forge-pipeline

## Pipeline

Workflow: [`.github/workflows/android.yml`](.github/workflows/android.yml)

1. JDK 17 (Temurin)
2. Android SDK (`android-actions/setup-android@v4`) + platform 34
3. Gradle 8.7 `assembleDebug`
4. Upload `app-debug` artifact (30-day retention)

Download the latest APK from the Actions run → Artifacts → `app-debug`.

## Agent team

Evaluated OAT, KageOps, and Captain Claw. **OAT (Open Agent Teams)** won:

- MIT, Ollama-native, GitHub CLI merge-queue + CI watch
- Linux arm64 binaries (Ampere A1)
- Daemon mode for reboot survival
- KageOps is AGPL and product-lifecycle heavy
- Captain Claw is a general workspace, not a coding merge-queue

Roles: see [`agents/team.yaml`](agents/team.yaml). Default model is local Ollama. Set `LLM_API_BASE` + `LLM_API_KEY` to swap to a cloud API.

## Oracle Cloud Always Free VM — blocked

This repo does **not** create an OCI VM. Two independent blockers:

1. **Payment method on signup.** Oracle Free Tier requires a credit/debit card for identity verification and may place a temporary authorization hold ([OCI Free Tier FAQ](https://www.oracle.com/cloud/free/faq/)). No actual charge if you stay Always Free, but a card is mandatory. This project will not proceed past that gate without an explicit OK.
2. **No OCI credentials in this environment.** Provisioning needs a tenancy, API key, and your public IPv4 so SSH is never opened to `0.0.0.0/0`.

When those exist, `infra/bootstrap.sh` installs Docker, Python 3.11, Node 20, Git, Ollama, OAT, systemd units, the watchdog, and a UFW allowlist.

## Security

- Secrets only in environment variables / GitHub Secrets. Never committed.
- `.gitignore` blocks `.env`, keys, keystores, `local.properties`.
- SSH allowlist is a single IPv4. `0.0.0.0/0` is rejected by `infra/firewall.sh`.

## Loop

Manager picks an `agent` issue → Coder opens a PR → Tester watches Actions → Manager squash-merges on green → repeat.
