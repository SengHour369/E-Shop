#!/usr/bin/env bash
# Run manually in your terminal; sudo requests your OS password locally.
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ ! -f .local/https/local-ca.crt ]]; then
  echo "Generate the local HTTPS certificate first." >&2
  exit 1
fi
openssl x509 -in .local/https/local-ca.crt -noout -subject -fingerprint -sha256
sudo install -m 0644 .local/https/local-ca.crt /usr/local/share/ca-certificates/eshop-local-ca.crt
sudo update-ca-certificates
echo "Restart your browser, then open https://localhost:8443 or your configured LAN HTTPS address."
