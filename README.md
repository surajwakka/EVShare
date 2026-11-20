# EVShare — Java EV Home-Charging Marketplace

Spring Boot 3 / Java 17 application with a responsive browser UI, charger discovery, host listing, time-slot booking and estimated kWh-based pricing.

## Current release status

This is a **hardened deployment candidate, not yet a production launch**. Production infrastructure settings and database migrations are included, but real-money payments and account/role security must be completed and independently tested before public launch. Do not accept real customer payments or expose host addresses publicly with this release.

## Stack

- Java 17, Spring Boot 3.3.5, Spring MVC, Spring Data JPA, Bean Validation
- PostgreSQL for production; H2 only for local development
- Flyway schema migration, Actuator health endpoints
- Static HTML/CSS/JavaScript frontend
- Docker multi-stage image, non-root runtime user

## Local development

Requirements: JDK 17+ and Maven 3.8+.

```bash
mvn clean test
mvn spring-boot:run
```

Open `http://localhost:8080`. Demo chargers are seeded only in the `dev` profile. H2 console is enabled in dev only.

## Production configuration

Set `SPRING_PROFILES_ACTIVE=prod` and configure a managed PostgreSQL database:

```bash
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://HOST:5432/evshare
DB_USERNAME=...
DB_PASSWORD=...
PORT=8080
```

The production profile disables H2, uses Flyway migrations, and sets Hibernate to `validate` rather than auto-mutating the schema. Back up PostgreSQL and test restore procedures before launch. Use a managed secret store; never commit credentials.

Build and run the Docker image:

```bash
docker build -t evshare:latest .
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL="$DATABASE_URL" \
  -e DB_USERNAME="$DB_USERNAME" \
  -e DB_PASSWORD="$DB_PASSWORD" evshare:latest
```

Health endpoint: `/actuator/health`. Only health/info/metrics are exposed; health details are hidden.

## API

- `GET /api/chargers?city=Prayagraj`
- `GET /api/chargers/{id}`
- `POST /api/chargers`
- `POST /api/bookings`
- `GET /api/bookings/{id}`
- `POST /api/payments/paytm/initiate` — creates a server-side Paytm staging transaction and returns a transaction token
- `POST /api/payments/paytm/verify` — verifies final status server-to-server through Paytm's Order Status API
- `POST /api/payments/paytm/callback` — notification only; independently rechecks status with Paytm

## Paytm staging integration

The app defaults to Paytm **STAGING** and uses `https://securestage.paytmpayments.com`. Configure `PAYTM_MID` and `PAYTM_MERCHANT_KEY` from the Paytm Dashboard Test API Details. `PAYTM_WEBSITE_NAME` defaults to `WEBSTAGING`; set `PAYTM_CALLBACK_URL` to your public HTTPS callback URL. Copy `.env.example` as a deployment reference, but store actual secrets only in your hosting provider's secret manager.

The backend generates a request checksum, calls Initiate Transaction, and passes the returned transaction token to Paytm CheckoutJS. After checkout, it calls `/v3/order/status` server-to-server and confirms the booking only when Paytm reports `TXN_SUCCESS` and the returned transaction amount matches the server-calculated booking amount. A browser callback alone never marks a booking paid. The merchant key is never sent to the browser. Staging transactions are test transactions and do not collect real money. Use Paytm's official checksum implementation and staging test credentials when validating the integration against your merchant account; run the complete Paytm sandbox test suite before any production switch.

Official references: [Initiate Transaction](https://www.paytmpayments.com/docs/api/initiate-transaction-api/), [JS Checkout](https://www.paytmpayments.com/docs/jscheckout-initiate-payment), [Verify Payment Status](https://www.paytmpayments.com/docs/jscheckout-verify-payment), [Checksum](https://www.paytmpayments.com/docs/checksum-implementation/).

## Charger specification and quality information

Host listings now capture manufacturer, model, AC/DC type, rated kW, connector, voltage, phase, current, IP rating, safety certification, warranty, installation year, inspection date, smart features, and condition notes. Customers see these details before booking. Condition grades are `EXCELLENT`, `GOOD`, `FAIR`, and `NEEDS_REPAIR`; listings marked `NEEDS_REPAIR` cannot be booked. Host and safety verification flags remain false by default and are not self-asserted by the host. Certification and IP information are host-provided until an admin verification workflow is implemented. The UI constrains the host's advertised rate to ₹10–₹20/kWh.

## Changes in this hardening pass

- Added production profile with PostgreSQL-only configuration, schema validation, Flyway migrations and disabled H2 console.
- Added Actuator health/metrics with limited endpoint exposure.
- Added security response headers and removed wildcard CORS.
- Demo data is now restricted to the dev profile.
- Booking creation now uses a database pessimistic lock on the charger row to reduce concurrent double-booking races across app instances.
- Added a consistent API exception response that avoids leaking internal exception details.
- Added a non-root, multi-stage Docker image.
- Added Paytm staging transaction-token initiation, CheckoutJS handoff and server-to-server transaction status verification.
- Added charger quality grading and technical specification fields to listings and customer details.
- Added a `.env.example` for staging setup.

## Mandatory launch blockers

1. **Identity and authorization:** Implement verified sign-up/login (OTP or email verification), secure sessions/JWT, customer/host/admin roles, ownership checks on charger edits, and customer-only access to their bookings. Current write/read APIs are not yet adequately access-controlled; do not expose them publicly.
2. **Paytm:** Staging flow is implemented but still requires validation with your own staging MID/key and Paytm's sandbox test paymodes. Before production, validate checksum compatibility against Paytm's official SDK, add idempotency, refunds, replay protection, reconciliation and operational alerts. Never trust a browser redirect or client-provided payment status. Keep secrets in a secret manager.
3. **Host settlement:** Integrate a compliant payout solution and reconcile host earnings, platform fees, refunds, chargebacks and tax records. Customer payment does not automatically pay the host.
4. **Privacy:** Add address masking; reveal the precise residential address only after a confirmed/paid booking. Add consent, retention and account deletion workflows.
5. **Real energy measurement:** Current pricing is an estimate (`charger kW × duration`). Integrate OCPP or the charger's authenticated vendor API and meter start/end readings before charging a final bill.
6. **Operations:** Add rate limiting, audit logs, alerting, structured logs with PII redaction, backups/restore drills, TLS, domain, uptime monitoring and a tested incident process.
7. **Legal and safety:** Validate applicable electricity resale, DISCOM, housing-society, electrical installation, consumer-protection and tax requirements in each operating jurisdiction. Establish host verification, safety checks, cancellation and dispute processes.
8. **Quality gates:** Run automated unit/integration tests, dependency and container vulnerability scans, load tests, payment sandbox tests and a manual security review before launch.

## Important commercial note

This code is a booking marketplace foundation; it does not switch a charger on/off or independently measure electricity. Do not advertise automatic charging or final metered billing until the hardware integration is implemented and validated.
