# AGENTS.md

Guidance for AI coding agents working in the LoyalTap API repository.

## Scope

This file applies to the entire `Loyal Tap API` repository.

## Product Context

LoyalTap is an NFC-based digital loyalty and rewards backend. The API is
responsible for the authoritative state of users, businesses, memberships,
points, NFC stamp requests, reward reservations, QR redemptions, wallet pass
mappings, and wallet update jobs.

Keep this architectural rule intact:

```text
Database = source of truth
NFC tag = interaction trigger
QR code = redemption proof token
Wallet pass = display/sync surface
```

## Technical Direction

- Use Java 21 and Spring Boot 3 for the backend.
- Use PostgreSQL as the primary database.
- Use Liquibase for schema creation, validation, and migrations.
- Use external authentication, preferably Clerk for MVP or Firebase Auth as an
  alternative.
- Do not implement local password storage unless explicitly requested.
- Add Google Wallet integration before Apple Wallet integration.
- Treat wallet sync as asynchronous background work.

## Naming

- Follow standard Java naming conventions.
- Packages and folder names should be lowercase.
- Classes, records, enums, and interfaces should use `PascalCase`.
- Methods, fields, local variables, and parameters should use `camelCase`.
- Constants should use `UPPER_SNAKE_CASE`.
- Names should describe business meaning, not implementation mechanics.

## Project Structure

- Keep application code under a real package, never the Java default package.
- Place the main `@SpringBootApplication` class in the root package above
  feature packages so component scanning, entity scanning, and configuration
  stay bounded to this application.
- Organize code by business capability first. A feature package usually maps to
  a business table, aggregate, or workflow.
- For each database-backed feature, create a dedicated package/folder named
  after the domain/table concept, for example `membership`, `reward`, or
  `stamprequest`.
- Inside a feature package, split code into focused subpackages when needed:
  `model`, `dto`, `mapper`, `repository`, `service`, `controller`, `utils`, and
  other clear roles.
- Keep shared cross-cutting code under `common`, such as errors, validation,
  security, auditing, and reusable infrastructure.

Expected capability packages:

```text
com.loyaltap
  auth
  user
  business
  employee
  membership
  nfc
  stamprequest
  reward
  rewardredemption
  wallet
    google
    apple
  common
    error
    validation
    security
    auditing
```

## Java And Spring Boot Standards

- Prefer constructor injection for all required collaborators. Dependencies
  should be `final`; avoid field injection.
- Keep controllers thin. Controllers validate and translate HTTP concerns;
  services enforce business rules and transactions.
- Use DTO/request/response types at API boundaries. Do not expose persistence
  entities directly from controllers.
- Use `jakarta.validation` constraints on request DTOs and service inputs where
  rules are structural. Use `@Validated` on services that rely on method
  validation.
- Keep domain state transitions explicit and readable. Avoid boolean-heavy
  method signatures; prefer small command/request objects when inputs grow.
- Prefer immutable values where practical: records for simple DTOs, final fields
  for dependencies, and narrow mutability in entities.
- Do not swallow exceptions inside transactional methods. Let business or data
  exceptions propagate so rollback behavior remains clear.
- When checked exceptions must roll back a transaction, configure
  `@Transactional(rollbackFor = ...)` explicitly.
- Keep external integrations behind interfaces/adapters so service logic can be
  tested without calling external systems.

## Domain Rules

- A user can have one membership per business.
- `memberships.points_balance` is the user's total points.
- `memberships.reserved_points` is temporarily locked when the user reserves a
  reward and receives a QR code.
- Available points are `points_balance - reserved_points`.
- NFC tag taps create `stamp_requests`; they must not add points directly.
- Employee approval is required before a stamp request changes membership
  points.
- Reward reservation must lock membership and reward rows before checking points
  or stock.
- Unlimited rewards have `rewards.stock_quantity = null`.
- Limited rewards reserve one stock unit by increasing `rewards.stock_reserved`.
- Reward redemption must be completed through a valid, unexpired QR
  `redemption_code` accepted by an employee of the same business.
- Wallet passes mirror backend data and must not be treated as authoritative.

## Methods

- Each method should have one clear responsibility and a readable control flow.
- Create helper methods or helper classes when logic becomes complicated,
  repeated, or difficult to scan.
- Avoid boilerplate-heavy methods; extract reusable mapping, validation, or
  lookup logic when it improves clarity.
- Prefer explicit names over comments that explain unclear names.
- Use Javadocs for methods, classes, fields, or decisions that are not fully
  understandable from the name and implementation.

### Service Methods

- Follow CRUD-style naming and ordering where it fits the feature.
- Put business rules in services, not controllers or repositories.
- Prefer service-layer transactions for stamp approval, reward reservation,
  reward acceptance, and reservation expiry.
- Validate all IDs, roles, ownership, and state transitions before mutating
  data.
- Use database row locking when reserving or redeeming rewards.
- Keep idempotency explicit for operations that clients or workers may retry.
- Queue wallet updates after core transactions commit.
- A wallet API failure should create or update a retryable job, not roll back a
  successful point approval or reward redemption.

### Repository Methods

- Be careful of N+1 query problems. Use custom queries, fetch joins, entity
  graphs, projections, or dedicated read models when needed.
- Keep repository methods focused on persistence. Do not hide business decisions
  in repository queries.
- Include locking queries for workflows that require serialized updates, such as
  reward reservation and redemption acceptance.

## Persistence And Liquibase Rules

- Liquibase is the owner of schema creation and schema evolution.
- Every database schema change must be represented in a Liquibase changelog.
- Every database schema change must also be reflected in the Java persistence
  model: entities, repositories, projections, DTOs, validation, and tests as
  applicable.
- Do not rely on Hibernate/JPA auto-DDL to create or alter schema.
- Keep changelog files append-only after they have been shared or applied. Add a
  new changeset for follow-up changes instead of editing old applied changesets.
- Include constraints and indexes in Liquibase when they express business
  invariants, lookup paths, idempotency, uniqueness, or locking expectations.
- Use internal UUID primary keys for tables. Public values such as NFC
  `tag_code` and QR `redemption_code` must be unique business identifiers, not
  primary keys.
- Validate Liquibase changelogs before database-related work is considered done.
- Keep the README data model and dbdiagram.io diagram link aligned when schema
  changes affect documented tables or relationships.

## Implementation Guidelines

- Store external provider IDs, wallet object IDs, and pass serial numbers
  separately from internal UUIDs.
- Generate random opaque `redemption_code` values for QR redemption.
- Add structured logging around business-critical state transitions, but never
  log secrets, access tokens, or raw credentials.

## Testing Rules

- Every new or changed service method must have Mockito-based unit tests.
- Mockito service tests should mock repositories, external providers, wallet
  clients, clocks, and other collaborators while exercising the real service
  class.
- Service tests must cover success paths, validation failures, authorization or
  ownership failures, missing data, invalid state transitions, idempotency, and
  rollback-relevant exceptions where applicable.
- Add integration tests for transaction-heavy flows, especially stamp approval,
  reward reservation, reward acceptance, reservation expiry, and wallet job
  creation.
- Use Spring test slices where they fit the scope: web/controller tests should
  use controller-focused Spring tests with mocked service collaborators.
- Prefer assertions on observable behavior and persisted state over verifying
  incidental internal calls.
- Do not mock value objects or simple data carriers. Mock boundaries and
  collaborators, not the domain values being reasoned about.
- Run the relevant test suite before finishing changes. If tests cannot be run,
  document the exact reason.

## Security Checklist

- Verify external auth tokens before initializing or loading users.
- Enforce role checks for employee, manager, owner, and admin APIs.
- Only employees of the same business can approve stamp requests or accept QR
  redemptions.
- QR codes should expire quickly, usually after 2-5 minutes.
- Allow only one active `RESERVED` redemption per membership/business or per
  membership/reward.
- Run a scheduled expiry job to release reserved points and stock.
- Never trust client-provided balances, reserved points, reward stock, redemption
  status, or wallet state.

## Documentation

- Keep `README.md` practical and README-like: setup, architecture decisions,
  endpoint map, data model, workflows, and implementation checklist.
- When adding endpoints, update the API surface in `README.md`.
- When adding or changing tables, update the data model and database diagram
  link in `README.md`.
- Do not paste the DBML/dbdiagram source into `README.md`; link to the shared
  dbdiagram.io diagram instead.
