---
name: testing-bank-of-anthos-local
description: How to bring up and UI-test the Bank of Anthos fork locally (Docker stack + host-run ledgerwriter), including demo credentials, seeded accounts, JDK selection for the Boot 4 / Java 25 ledgerwriter, and which log lines are benign.
---

# Local end-to-end testing of Bank of Anthos (this fork)

## Topology used for UI testing
Most services run as Docker containers on network `boa`; the service under test
(usually `ledgerwriter`) is run on the **host** so you can point the UI at a specific build.

- `frontend` container → http://localhost:8090 (UI under test)
- `accounts-db`, `ledger-db-boa` (ledger Postgres on host port **5433**), `userservice`,
  `contacts`, `balancereader` (host port **8082**), `transactionhistory`
- `frontend` reaches ledgerwriter through `TRANSACTIONS_API_ADDR=host.docker.internal:8080`,
  so whatever listens on host :8080 is what the UI exercises. Stop any stale ledgerwriter
  on :8080 before starting the build you want to test.
- Bring-up script / setup doc (may exist on the box): `/home/ubuntu/boa/up.sh`,
  `/home/ubuntu/boa/REBUILD.md`.

## Running the ledgerwriter you want to test
```bash
cd src/ledger/ledgerwriter
JAVA_HOME=/home/ubuntu/.local/share/jdk-25 \
PORT=8080 ENABLE_TRACING=false ENABLE_METRICS=false LOG_LEVEL=info \
VERSION=boot4 HOSTNAME=ledgerwriter-local NAMESPACE=default \
LOCAL_ROUTING_NUM=883745000 PUB_KEY_PATH=/home/ubuntu/jwt/publickey \
BALANCES_API_ADDR=localhost:8082 \
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/postgresdb \
SPRING_DATASOURCE_USERNAME=admin SPRING_DATASOURCE_PASSWORD=password \
mvn spring-boot:run > /tmp/ledgerwriter.log 2>&1 &
```
- The default `java` on PATH may be 17; the Boot 4 module needs **JDK 25**
  (`/home/ubuntu/.local/share/jdk-25` or `/usr/lib/jvm/jdk-25.0.4.1+1`).
- Always set a distinctive `VERSION` and prove which build the UI hits:
  `curl localhost:8080/version` (expect your value) and `curl localhost:8080/ready` → `ok`.

## Demo login and seeded data
- Login: `testuser` / `bankofanthos`; own account `1011226111`; local routing `883745000`.
- Contacts: Alice `1033623433`, Bob `1055757655`, Eve `1077441377`.
- Seeded external contact (the only pre-seeded way to exercise the external-routing
  deposit path): `9099791699` / routing `808889588`.
- To trigger `can't send to self`, use Send Payment → **New Recipient** → own account number
  (existing contacts can't include yourself). The frontend blocks a "New External Account"
  with the local routing number client-side, so that validator branch can't be reached via UI.
- Balances/history are served by `balancereader`/`transactionhistory`, which read the ledger
  asynchronously; the post-redirect home page has reflected new transactions immediately in
  practice, but if a balance looks stale, reload before declaring a failure.
- accounts-db credentials (for looking up demo accounts/contacts): user `accounts-admin`,
  password `accounts-pwd`, db `accounts-db`. Ledger db: `admin` / `password` / `postgresdb`.

## Benign log noise (do not report as failures)
- Startup `DefaultCredentialsProvider` / `GoogleCredentialsProvider` `IOException:
  Your default credentials were not found` — no GCP ADC locally.
- The app logs its own startup banner at ERROR level:
  `Started LedgerWriter service. Log level is: INFO`.
- Guava `sun.misc.Unsafe` deprecation warning on JDK 25; Hibernate "dialect does not need
  to be specified"; Spring `spring.jpa.open-in-view` warning.
- A rejected transaction legitimately logs two ERROR lines (e.g.
  `Invalid transaction: Sender is also receiver` and
  `Failed to retrieve account balance: bad request`) while still returning 400 to the UI.
  Real breakage would look like `NoSuchMethodError`/`NoClassDefFoundError`, Spring context
  failures, or `Internal Server Error`.

## Devin Secrets Needed
None — everything runs locally with the JWT keypair in `/home/ubuntu/jwt`.
