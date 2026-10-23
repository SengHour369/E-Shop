#!/usr/bin/env python3
"""Prepare local TLS without exposing keys or changing machine trust settings."""
import argparse
import ipaddress
import os
from pathlib import Path
import subprocess

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--ip', required=True, help='E-Shop LAN IP, e.g. 192.168.18.176')
parser.add_argument('--port', type=int, default=8443)
args = parser.parse_args()
address = str(ipaddress.ip_address(args.ip))
if not 1024 <= args.port <= 65535:
    parser.error('Use an unprivileged HTTPS port between 1024 and 65535.')
root = Path(__file__).resolve().parent.parent
certs = root / '.local' / 'https'
certs.mkdir(parents=True, exist_ok=True)
os.chmod(certs, 0o700)

def openssl(*arguments):
    subprocess.run(['openssl', *map(str, arguments)], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)

ca_key, ca_cert = certs / 'local-ca.key', certs / 'local-ca.crt'
if ca_key.exists() != ca_cert.exists():
    raise SystemExit('Local CA is incomplete. Restore its matching key/certificate before continuing.')
if not ca_key.exists():
    openssl('req', '-x509', '-newkey', 'rsa:3072', '-nodes', '-sha256', '-days', '3650',
            '-keyout', ca_key, '-out', ca_cert, '-subj', '/CN=E-Shop Local Development CA',
            '-addext', 'basicConstraints=critical,CA:TRUE,pathlen:0',
            '-addext', 'keyUsage=critical,keyCertSign,cRLSign')
    os.chmod(ca_key, 0o600)
ext = certs / 'server.ext'
ext.write_text('basicConstraints=critical,CA:FALSE\nkeyUsage=critical,digitalSignature,keyEncipherment\nextendedKeyUsage=serverAuth\nsubjectAltName=DNS:localhost,IP:127.0.0.1,IP:::1,IP:' + address + '\n')
key, csr, leaf = certs / 'server.key', certs / 'server.csr', certs / 'server.crt'
openssl('req', '-new', '-newkey', 'rsa:2048', '-nodes', '-keyout', key, '-out', csr, '-subj', '/CN=localhost')
os.chmod(key, 0o600)
openssl('x509', '-req', '-in', csr, '-CA', ca_cert, '-CAkey', ca_key, '-CAcreateserial', '-out', leaf,
        '-days', '365', '-sha256', '-extfile', ext)
openssl('verify', '-CAfile', ca_cert, leaf)
csr.unlink()
# Keep private local settings out of Git; preserve all existing credentials and origins.
settings = root / '.env.local'
lines = settings.read_text().splitlines() if settings.exists() else []
values = dict(line.split('=', 1) for line in lines if '=' in line and not line.lstrip().startswith('#'))
origins = values.get('GATEWAY_CORS_ORIGINS', 'http://localhost:4200,http://localhost:8080').strip('\"\'').split(',')
for origin in [f'https://localhost:{args.port}', f'https://127.0.0.1:{args.port}', f'https://{address}:{args.port}']:
    if origin not in origins:
        origins.append(origin)
updates = {'ESHOP_HTTPS_ENABLED':'true', 'ESHOP_HTTPS_PORT':str(args.port),
           'ESHOP_HTTPS_ORIGIN':f'https://{address}:{args.port}', 'GATEWAY_CORS_ORIGINS':','.join(origins)}
for name, value in updates.items():
    for index, line in enumerate(lines):
        if line.startswith(name + '='):
            lines[index] = name + '=' + value
            break
    else:
        lines.append(name + '=' + value)
settings.write_text('\n'.join(lines) + '\n')
print(f'Local HTTPS prepared: https://{address}:{args.port}')
print(f'Public trust certificate: {ca_cert}')
print('Private keys remain in .local/https and must never be shared.')
