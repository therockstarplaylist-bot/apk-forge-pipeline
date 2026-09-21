#!/usr/bin/env bash
set -euo pipefail
install -d /etc/ssh/sshd_config.d
cat >/etc/ssh/sshd_config.d/forge.conf <<'EOF'
PasswordAuthentication no
KbdInteractiveAuthentication no
PermitRootLogin no
PubkeyAuthentication yes
AllowUsers ubuntu forge
EOF
systemctl reload ssh || systemctl reload sshd
