#!/usr/bin/env bash
# Restart the agent unit if it is not active. Invoked by systemd timer.
set -euo pipefail
UNIT="${FORGE_UNIT:-forge-agent.service}"
if ! systemctl is-active --quiet "$UNIT"; then
  logger -t forge-watchdog "restarting $UNIT"
  systemctl restart "$UNIT"
fi
