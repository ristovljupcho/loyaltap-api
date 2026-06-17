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
- Use Liquibase for schema migrations.
- Use external authentication, preferably Clerk for MVP or Firebase Auth as an
  alternative.
- Do not implement local password storage unless explicitly requested.
- Add Google Wallet integration before Apple Wallet integration.
- Treat wallet sync as asynchronous background work.

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

## Expected Modules

Keep packages organized around business capabilities:

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

## Implementation Guidelines

- Keep controller logic thin.
- Prefer service-layer transactions for stamp approval, reward reservation,
  reward acceptance, and reservation expiry.
- Use database row locking when reserving or redeeming rewards.
- Validate all IDs, roles, ownership, and state transitions before mutating
  data.
- Store external provider IDs, wallet object IDs, and pass serial numbers
  separately from internal UUIDs.
- Use internal UUIDs as primary keys; do not use public NFC tag codes as primary
  keys.
- Generate random opaque `redemption_code` values for QR redemption.
- Queue wallet updates after core transactions commit.
- A wallet API failure should create or update a retryable job, not roll back a
  successful point approval or reward redemption.
- Add integration tests for transaction-heavy flows.

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
