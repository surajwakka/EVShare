# EVShare — Java EV Home-Charging Marketplace

Spring Boot 3 / Java 17 application with a responsive browser UI, charger discovery, host listings, time-slot reservations and estimated kWh-based pricing.

## Payment policy

**No online payment gateway is included.** Paytm, CheckoutJS, merchant credentials, payment callbacks, payment verification and payout integrations have been removed. Render is retained solely as an optional application hosting/deployment platform; it is not a payment provider and no Render payment integration exists in this application.

EVShare records reservations and displays an estimated charging cost based on charger power, booking duration and the host's ₹/kWh rate. The actual charging amount should be calculated from metered energy delivered. Any settlement between the customer and host is handled independently, outside EVShare.

## Stack

- Java 17, Spring Boot 3.3.5, Spring MVC, Spring Data JPA, Bean Validation
- PostgreSQL for production; H2 for local development
- Flyway schema migrations, Actuator health endpoints
- Static HTML/CSS/JavaScript frontend
- Docker multi-stage image, non-root runtime user

## Local development

Requirements: JDK 17+ and Maven 3.8+.

```bash
mvn clean test
mvn spring-boot:run
```

Open `http://localhost:8080`. Demo chargers are seeded only in the `dev` profile. H2 console is enabled in dev only.

## API

- `GET /api/chargers?city=Prayagraj`
- `GET /api/chargers/{id}`
- `POST /api/chargers`
- `POST /api/bookings` — reserves a slot and returns an estimated cost; no payment is collected
- `GET /api/bookings/{id}`

There are no payment endpoints.

## Charger specifications and quality

Host listings capture manufacturer, model, AC/DC type, rated kW, connector, voltage, phase, current, IP rating, safety certification, warranty, installation year, inspection date, smart features and condition notes. Customers see these details before booking. Condition grades are `EXCELLENT`, `GOOD`, `FAIR`, and `NEEDS_REPAIR`; listings marked `NEEDS_REPAIR` cannot be booked. Host and safety verification flags remain false by default. Certification and IP information are host-provided until an admin verification workflow is implemented. Advertised rate is constrained by the UI to ₹10–₹20/kWh.

## Deployment on Render (optional)

1. Push the contents of this `evshare` folder to a GitHub repository, keeping `render.yaml` and `Dockerfile` at the repository root.
2. In Render, choose **New → Blueprint**, connect the repository and let Render read `render.yaml`.
3. Review and create the web service and managed PostgreSQL database.
4. Render injects database connection details from the managed database resource. No payment credentials or gateway settings are required.

Render is hosting infrastructure only. Its hosting/database plans may incur platform charges; those are unrelated to EVShare customer bookings and are not processed by this application.

## Operational limitations before public launch

1. Add verified sign-up/login, customer/host/admin roles, ownership checks and access control before exposing write APIs publicly.
2. Add address masking; reveal residential address only after an appropriately verified reservation.
3. Integrate OCPP or a charger's authenticated vendor API and meter readings before presenting final energy billing.
4. Add rate limiting, audit logs, alerting, backups/restore drills, TLS, domain, uptime monitoring and security review.
5. Validate electricity resale, DISCOM, housing-society, electrical installation, consumer-protection and tax requirements in each operating jurisdiction.
6. Run automated tests, dependency/container scans, load tests and a manual security review before launch.

## Database migration note

Flyway migration `V3__remove_payment_gateway.sql` removes the legacy payment-order and Paytm transaction columns from existing PostgreSQL databases. Historical bookings with `PENDING_PAYMENT` status are migrated to `CONFIRMED` because reservations no longer depend on an in-app payment step. Back up production data before applying migrations.
