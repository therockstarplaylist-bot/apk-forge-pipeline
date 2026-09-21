#!/usr/bin/env bash
# Open SSH only to SSH_ALLOW_IP. Deny everything else inbound.
# Never 0.0.0.0/0 on port 22.
set -euo pipefail

if [[ -f /etc/forge/agent.env ]]; then
  # shellcheck disable=SC1091
  source /etc/forge/agent.env
fi

ALLOW="${SSH_ALLOW_IP:-}"
if [[ -z "$ALLOW" || "$ALLOW" == "0.0.0.0/0" || "$ALLOW" == "*" ]]; then
  echo "SSH_ALLOW_IP must be a single public IPv4, not 0.0.0.0/0" >&2
  exit 1
fi

apt-get install -y --no-install-recommends ufw
ufw --force reset
ufw default deny incoming
ufw default allow outgoing
ufw allow from "$ALLOW" to any port 22 proto tcp
ufw --force enable
ufw status verbose
