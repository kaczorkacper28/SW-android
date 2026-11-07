#!/usr/bin/env bash
set -euo pipefail
sw_repo_dir="$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
for sw_command in node npm; do
  if ! command -v "$sw_command" >/dev/null 2>&1; then
    echo 'Najpierw zainstaluj narzędzia w Termuxie: pkg install -y nodejs-lts npm git openssl' >&2
    exit 1
  fi
done
case "$(node -p 'process.versions.node.split(".")[0]')" in
  22|24) ;;
  *) echo 'Wymagany Node.js 22 lub 24. W Termuxie użyj: pkg install nodejs-lts npm' >&2; exit 1 ;;
esac
cd "$sw_repo_dir/backend"
if [ "${1:-}" = '--setup' ] || ! node -e "require('express'); require('dotenv')" >/dev/null 2>&1; then
  npm ci --no-audit --no-fund
fi
if [ "${1:-}" = '--setup' ] || [ ! -f .env ]; then
  npm run configure
fi
if command -v termux-wake-lock >/dev/null 2>&1; then termux-wake-lock; fi
exec npm start
