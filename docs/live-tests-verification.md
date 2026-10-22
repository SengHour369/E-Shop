# Live test verification — 20 October 2026

## Commands actually run

```bash
NG_BUILD_MAX_WORKERS=1 NODE_OPTIONS=--max-old-space-size=1024 npm test
MAVEN_OPTS=-Xmx256m JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn -q -pl admin-service -am -Dtest=LiveTestAccessTest -Dsurefire.failIfNoSpecifiedTests=false test
python3 live-tests/run.py --base-url http://localhost:4200 --username admin --guest-session --report live-tests/reports/latest.json
```

Angular: **464 passing tests, 82 files**. Backend access/transaction tests: **4 passing**. Java and Angular production builds passed; Angular retains its existing initial-bundle budget warning.

## Actual real HTTP run

Run `7c0c42bf-7a3a-482f-931b-372ce6b5bfa6`: **13 passed, 0 failed, 2 blocked**. CLI exit code 2 correctly represents blocked prerequisites. No mocked network responses were used for this live run.

| Check | Result | HTTP | Duration |
|---|---|---|---|
| Gateway health | PASSED | 200 | 735 ms |
| Live storefront configuration | PASSED | 200 | 58 ms |
| Anonymous admin access is rejected | PASSED | 401 | 18 ms |
| Current account and effective permissions | PASSED | 200 | 53 ms |
| Real products and SKU structure | PASSED | 200 | 230 ms |
| Anonymous browsing obeys configuration | PASSED | 200 | 114 ms |
| Order service list | PASSED | 200 | 147 ms |
| Inventory service list | PASSED | 200 | 99 ms |
| Payment service list | PASSED | 200 | 276 ms |
| Real KHQR availability | BLOCKED | 200 | 25 ms |
| Current account notification count | PASSED | 200 | 363 ms |
| AI inference/provider information | PASSED | 200 | 354 ms |
| Real AI conversation history | PASSED | 200 | 130 ms |
| Workflow engine availability (AUT-01–11) | BLOCKED | 404 | 28 ms |
| Guest cookie session and revocation | PASSED | 200 | 56 ms |

KHQR is blocked because no receiving account/online configuration exists. Generic workflow APIs are absent (404), so AUT-01–11 cannot yet be verified. AI provider metadata availability is not a successful inference/accuracy test. Order and payment list checks do not prove order placement or bank settlement.

The Admin UI loaded the persisted CLI report and started a second real run using its Run button. Progress was shown from server state. The Admin run completed with 12 passed / 0 failed / 2 blocked (14 default checks; no guest-session option selected). Its Download JSON report button produced a verified JSON file with that actual run ID. The initial failed start exposed an audit transaction bug; it is preserved as a failed historical run, and a regression test now checks atomic run/audit persistence with HTTP execution deferred until after commit.

No orders, stock changes, bank transfers, refunds, messages or workflow activations were performed. One isolated guest cookie session was created and revoked. Only run records, audit entries, and permission/schema definitions were added.
