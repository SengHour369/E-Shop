# Local E-Shop HTTPS

HTTPS entry points:

- https://192.168.18.176:8443
- https://localhost:8443

The nginx TLS proxy serves the existing production Angular build and forwards `/api/` to the existing gateway on the Compose network. Auth and guest cookies are Secure, and the exact HTTPS origins are added to existing CORS/CSRF origin allowlists. Gemini audio uses the SDK's own WSS connection. HTTP development servers are not removed; use the HTTPS origin for signed-in shopping and microphone access after enabling Secure cookies.

A local certificate authority signs a server certificate valid for localhost, 127.0.0.1, ::1 and 192.168.18.176. Its private key stays on the host and is not mounted in nginx. All generated material is ignored by Git. This is local development HTTPS, not a publicly trusted certificate or a public hosting setup.

## Trust the certificate once

On the development Ubuntu machine run:

```bash
cd /home/senghour/Desktop/E-Shop
./scripts/trust-local-https.sh
```

This requires your OS administrator password in your terminal. The setup agent could not install system trust because passwordless sudo is unavailable. The script shows the CA fingerprint, installs only its public certificate, and refreshes the OS trust store. Restart the browser. If your browser uses a separate certificate store, import `.local/https/local-ca.crt` under trusted certificate authorities. Do not rely on bypassing a certificate warning for microphone support.

On each phone or other computer, securely transfer only `.local/https/local-ca.crt` and install it as a trusted CA. On iOS also enable full trust for the installed certificate in Certificate Trust Settings. Never copy `local-ca.key` or `server.key` to clients. The public CA is also available at `/local-ca.crt` over the configured HTTPS origin, but verify it against the host's SHA-256 fingerprint before trusting it.

Use the new HTTPS origin and sign in again; the old HTTP-origin local storage does not transfer. Host-scoped cookies from an earlier HTTP login may need clearing. Once trust is established, the browser should report `window.isSecureContext === true`, and Start Call can request microphone permission.

## Rebuild and restart

```bash
cd /home/senghour/Desktop/Order_Shop_Frontend
npm run build
cd /home/senghour/Desktop/E-Shop
./run-docker.sh https-proxy api-gateway auth-service order-service
```

`ESHOP_HTTPS_ENABLED=true` in ignored `.env.local` makes `run-docker.sh` append `compose.https.yaml` last, including after SMTP overrides. The frontend build must exist under `dist/e-shop/browser`; no source or framework replacement is involved.

For a changed LAN IP or certificate renewal:

```bash
python3 scripts/configure-local-https.py --ip NEW_LAN_IP --port 8443
./run-docker.sh https-proxy api-gateway auth-service order-service
docker compose --env-file env --env-file .env.local -f compose.yaml -f compose.https.yaml exec https-proxy nginx -s reload
```

The existing CA is retained, so renewed server certificates do not require reinstalling trust. The HTTPS origin, port, Secure cookies and origin allowlists are saved locally. If the port changes, use the new port in your URLs. Existing allowlisted origins are preserved.

For internet hosting with a browser-trusted certificate, a real domain and DNS/public ingress configuration are required. Do not expose this development CA or treat a locally trusted certificate as a public certificate.

## Verified on setup

- LAN and localhost HTTPS pages return 200 with CA validation enabled.
- nginx configuration validation passes.
- The protected voice config endpoint returns 401 without authentication.
- The exact HTTPS LAN login origin passes CORS preflight (200).
- Running auth-service and order-service both have Secure cookies enabled.
- The CA private key is not mounted into the web server, and both private keys are ignored by Git.
- Shell launcher syntax checks pass. Browser and phone trust installation remains a per-device user step.
