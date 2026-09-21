#!/usr/bin/env bash
# Oracle Cloud Always Free ARM (Ampere A1) host bootstrap.
# Run as root on a fresh Ubuntu 24.04 ARM image.
# Does not print secret values. Requires /etc/forge/agent.env already in place.
set -euo pipefail

if [[ "$(id -u)" -ne 0 ]]; then
  echo "run as root" >&2
  exit 1
fi

export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y --no-install-recommends \
  ca-certificates curl git gnupg jq unzip ufw \
  python3.11 python3.11-venv python3-pip

# Docker
if ! command -v docker >/dev/null 2>&1; then
  curl -fsSL https://get.docker.com | sh
  systemctl enable --now docker
fi

# Node 20
if ! command -v node >/dev/null 2>&1; then
  curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
  apt-get install -y nodejs
fi

# Ollama (systemd unit ships with the installer)
if ! command -v ollama >/dev/null 2>&1; then
  curl -fsSL https://ollama.com/install.sh | sh
fi
systemctl enable --now ollama
# Keep Ollama on loopback. The installer may bind 0.0.0.0; pin it.
mkdir -p /etc/systemd/system/ollama.service.d
cat >/etc/systemd/system/ollama.service.d/override.conf <<'EOF'
[Service]
Environment=OLLAMA_HOST=127.0.0.1:11434
EOF
systemctl daemon-reload
systemctl restart ollama

# OAT — Open Agent Teams (Linux arm64)
if ! command -v oat >/dev/null 2>&1; then
  curl -fsSL https://raw.githubusercontent.com/Root-IO-Labs/open-agent-teams/main/install.sh | bash
fi

id forge >/dev/null 2>&1 || useradd --system --create-home --shell /usr/sbin/nologin forge
usermod -aG docker forge || true

install -d -o forge -g forge /opt/forge
if [[ ! -d /opt/forge/apk-forge-pipeline/.git ]]; then
  git clone https://github.com/therockstarplaylist-bot/apk-forge-pipeline.git /opt/forge/apk-forge-pipeline
  chown -R forge:forge /opt/forge/apk-forge-pipeline
fi

install -d -m 0750 /etc/forge
if [[ ! -f /etc/forge/agent.env ]]; then
  echo "missing /etc/forge/agent.env — copy .env.example and fill GITHUB_TOKEN / SSH_ALLOW_IP" >&2
  exit 1
fi
chmod 0640 /etc/forge/agent.env
chown root:forge /etc/forge/agent.env

install -m 0755 /opt/forge/apk-forge-pipeline/agents/watchdog.sh /usr/local/sbin/forge-watchdog
install -m 0644 /opt/forge/apk-forge-pipeline/infra/systemd/forge-agent.service /etc/systemd/system/forge-agent.service
install -m 0644 /opt/forge/apk-forge-pipeline/infra/systemd/forge-watchdog.service /etc/systemd/system/forge-watchdog.service
install -m 0644 /opt/forge/apk-forge-pipeline/infra/systemd/forge-watchdog.timer /etc/systemd/system/forge-watchdog.timer

systemctl daemon-reload
systemctl enable --now docker ollama forge-agent.service forge-watchdog.timer

bash /opt/forge/apk-forge-pipeline/infra/firewall.sh
bash /opt/forge/apk-forge-pipeline/infra/sshd-hardening.sh

echo "bootstrap complete"
systemctl is-enabled docker ollama forge-agent.service forge-watchdog.timer
systemctl --no-pager --type=service --state=running
