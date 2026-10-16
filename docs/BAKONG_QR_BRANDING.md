# Bakong QR logo — 2026-10-17

The owner's supplied Bakong PNG is stored unchanged at `payment-service/src/main/resources/images/bakong-logo.png`. `BakongQrRenderer` renders the existing authoritative QR payload with high error correction, a four-module quiet zone, and a small centered logo with a white border. `BakongServiceImpl.getQRImage` reuses this renderer, so existing payment screens receive the logo without a frontend deployment or changes to payment data.

Tests decode individual and merchant PNGs at 300px and the customer chat's 240px display size for USD amounts 0.01, 10 and 12345.67. They verify the original QR text, checksum, quiet-zone color and red logo pixels. Selected backend suites passed 29 tests, and the Maven package succeeded.

Rebuilt and recreated only Payment with the existing automation configuration. Gateway Payment OpenAPI returned HTTP 200. A temporary diagnostic harness invoked the deployed service classes inside the actual Java runtime, generated a $0.01 individual KHQR, decoded its rendered PNG, and verified its recipient/amount/reference/checksum. The harness was removed from the container afterward. No order or transfer was created.

Preview: `/home/senghour/.cache/eshop-ai-validation/qr-test/bakong-branded-qr.png`. The test QR expires at 01:53:14 on 2026-10-17, Asia/Phnom_Penh. Production QR expiry and paid-status checks remain unchanged.
