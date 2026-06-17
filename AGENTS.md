# AGENTS.md

Guidance for AI coding agents working in the LoyalTap API repository.

## Scope

This file applies to the entire `Loyal Tap API` repository.

## Product Context

LoyalTap is an NFC-based digital loyalty card backend. The API is responsible
for the authoritative state of users, businesses, memberships, stamp events,
rewards, NFC tags, employee approvals, and wallet synchronization.

Keep this architectural rule intact:

```text
Backend database = source of truth
Wallets = display and redemption surface
NFC tags = interaction trigger, not purchase proof
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
- `coupon_events` is the audit trail and should be append-only for normal
  business operations.
- `memberships.current_counter` is a display/read optimization, not the only
  source of historical truth.
- Employee approval is required before a stamp changes membership state.
- Public NFC tags can be copied or repeatedly tapped, so NFC alone must not
  authorize stamps.
- Reward redemption must be confirmed by an authorized employee or equivalent
  controlled redemption flow.
- Approve and redeem operations should be idempotent.

## Expected Modules

Keep packages organized around business capabilities:

```text
com.loyaltap
  auth
  user
  business
  employee
  loyaltyprogram
  membership
  coupon
  reward
  nfc
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

- Prefer service-layer transactions around stamp approval, reward creation, and
  reward redemption.
- Keep controller logic thin.
- Validate all IDs, roles, and state transitions before mutating data.
- Use database constraints for uniqueness and invariants where possible.
- Store external provider IDs, wallet object IDs, and pass serial numbers
  separately from internal UUIDs.
- Queue wallet updates after the database transaction commits.
- A wallet API failure should create or update a retryable job, not roll back a
  successful stamp approval.
- Add integration tests for transaction-heavy flows.

## Security Checklist

- Verify external auth tokens before initializing or loading users.
- Enforce role checks for employee, manager, business owner, and admin APIs.
- Expire stamp requests quickly, usually after 1-2 minutes.
- Allow only one pending stamp request per user/business at a time.
- Rate limit repeated stamp requests by user, business, device, and NFC tag.
- Require idempotency keys for mutation endpoints that may be retried.
- Never trust client-provided counters, reward states, or wallet state.

## Documentation

- Keep `README.md` practical and README-like: setup, architecture decisions,
  endpoint map, data model, workflows, and implementation checklist.
- When adding endpoints, update the API surface in `README.md`.
- When adding or changing tables, update the domain model and constraints in
  `README.md`.
