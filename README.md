# LoyalTap API

Backend API for LoyalTap, an NFC-based digital loyalty card system for cafes,
restaurants, and small businesses. LoyalTap replaces paper stamp cards and
coupon cards with a backend-driven membership model, a customer mobile app,
employee-approved stamp collection, and optional Google Wallet / Apple Wallet
passes.

## What This API Owns

The backend is the source of truth for:

- External-auth user mapping.
- Business and employee management.
- Loyalty programs and memberships.
- NFC-triggered stamp requests.
- Employee stamp approval and reward redemption.
- Coupon event audit history.
- Wallet pass mappings and async wallet updates.

Core architectural rule:

```text
Backend database = source of truth
Wallets = display and redemption surface
NFC tags = interaction trigger, not purchase proof
```

## MVP Scope

Target users:

- Customers who collect stamps, rewards, or coupons from local businesses.
- Business owners who create loyalty programs.
- Employees who approve stamp collection and redeem rewards.
- Administrators who manage businesses, NFC tags, and system settings.

MVP capabilities:

- User authentication through an external provider.
- Business creation and loyalty program configuration.
- One membership card per user per business.
- Single NFC tag per business/store for starting the stamp flow.
- Employee approval before adding stamps.
- Reward generation when a stamp threshold is reached.
- Reward redemption by employee confirmation or QR/barcode scanning.
- Google Wallet integration as the first wallet integration.

## Recommended Stack

| Layer | Choice | Why |
| --- | --- | --- |
| Backend | Java 21 + Spring Boot 3 | Strong transactional APIs and long-term maintainability |
| Database | PostgreSQL | Relational constraints, transactions, and audit logs |
| Migrations | Liquibase | Repeatable database changes |
| Auth | Clerk first, Firebase Auth as an alternative | Avoid custom password/session logic |
| Mobile app | React Native / Expo | Good cross-platform fit for NFC and wallet flows |
| Infrastructure | Docker + docker-compose | Simple local development |
| Wallets | Google Wallet first, Apple Wallet later | Lower MVP complexity |

## Local Development

The Spring Boot project is not scaffolded yet. Once it exists, the expected
developer flow is:

```bash
docker compose up -d postgres
./mvnw spring-boot:run
./mvnw test
```

Recommended local services and configuration:

- PostgreSQL database.
- Liquibase migrations on application startup or CI.
- Clerk or Firebase Auth credentials.
- Wallet provider credentials only after the core stamp/reward flow works.

## Repository Shape

Expected backend repository layout:

```text
Loyal Tap API/
  src/
    main/
    test/
  pom.xml
  Dockerfile
  README.md
  AGENTS.md
```

Expected Java package layout:

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

## High-Level Architecture

```text
Customer Mobile App
  - Displays loyalty cards
  - Opens NFC deep links
  - Shows rewards
  - Adds cards to Google/Apple Wallet

Merchant/Employee App or Mode
  - Sees pending stamp requests
  - Approves stamps
  - Scans or confirms reward redemptions

Backend API
  - Source of truth
  - Business rules
  - Auth verification
  - Membership counters
  - Coupon event log
  - Reward lifecycle
  - Wallet pass synchronization

PostgreSQL Database
  - Users
  - Businesses
  - Loyalty programs
  - Memberships
  - Coupon events
  - Rewards
  - NFC tags
  - Wallet pass mappings

Google Wallet / Apple Wallet
  - Display layer
  - Barcode / pass presentation
  - Updated from backend
```

## Authentication

Use external authentication to avoid building passwords, email verification,
password resets, session management, and social login from scratch.

Recommended for MVP: Clerk. Firebase Auth is a strong alternative. Supabase Auth
is viable if the database is hosted in Supabase.

| Provider | Use Case | Recommendation |
| --- | --- | --- |
| Clerk | Fast auth for mobile/web and business/employee concepts | Best MVP choice |
| Firebase Auth | Mobile-first Google ecosystem and simple user management | Strong alternative |
| Supabase Auth | Auth plus hosted Postgres in one platform | Good if Supabase hosts the backend database |
| Auth0 | Enterprise identity | Too heavy for MVP |
| Keycloak | Self-hosted enterprise auth | Too much setup at the start |

Do not store passwords in the LoyalTap database. Store the external identity
provider ID and map it to an internal user ID.

```sql
users
  id UUID PRIMARY KEY
  auth_provider VARCHAR(30) NOT NULL
  auth_provider_user_id VARCHAR(150) NOT NULL
  created_at TIMESTAMP NOT NULL
  status VARCHAR(30) NOT NULL

UNIQUE(auth_provider, auth_provider_user_id)
```

Roles:

- `CUSTOMER`: collects stamps and redeems rewards.
- `BUSINESS_OWNER`: manages business profile, loyalty programs, tags, and employees.
- `MANAGER`: manages store operations and employees.
- `EMPLOYEE`: approves stamps and redeems rewards.
- `ADMIN`: platform-level administration.

## Domain Model

The database should not rely only on a mutable counter.
`memberships.current_counter` is useful for quick display, but `coupon_events`
is the audit trail.

| Table | Purpose |
| --- | --- |
| `users` | Internal representation of authenticated users |
| `businesses` | Cafe, restaurant, shop, or merchant using LoyalTap |
| `business_employees` | Relationship between users and businesses with employee roles |
| `loyalty_programs` | Rules such as required stamps and reward description |
| `memberships` | One loyalty card per user per business |
| `coupon_events` | Immutable audit log of stamps, redemptions, adjustments, and reversals |
| `rewards` | Redeemable rewards created after enough stamps are collected |
| `nfc_tags` | Registered NFC tags assigned to a business or location |
| `wallet_passes` | Mapping between memberships and external wallet objects/passes |
| `wallet_update_jobs` | Retryable async jobs for Google/Apple Wallet synchronization |

Key constraints:

```text
memberships: UNIQUE(user_id, business_id)
users: UNIQUE(auth_provider, auth_provider_user_id)
nfc_tags: UNIQUE(tag_code)
wallet_passes: UNIQUE(provider, external_object_id)
coupon_events: UNIQUE(idempotency_key)
```

Suggested SQL draft:

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    auth_provider VARCHAR(30) NOT NULL,
    auth_provider_user_id VARCHAR(150) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    status VARCHAR(30) NOT NULL,
    CONSTRAINT uq_user_provider UNIQUE (auth_provider, auth_provider_user_id)
);

CREATE TABLE businesses (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(150) NOT NULL UNIQUE,
    description TEXT,
    logo_url TEXT,
    created_at TIMESTAMP NOT NULL,
    status VARCHAR(30) NOT NULL
);

CREATE TABLE business_employees (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL REFERENCES businesses(id),
    user_id UUID NOT NULL REFERENCES users(id),
    role VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    status VARCHAR(30) NOT NULL,
    CONSTRAINT uq_business_employee UNIQUE (business_id, user_id)
);

CREATE TABLE loyalty_programs (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL REFERENCES businesses(id),
    name VARCHAR(150) NOT NULL,
    stamps_required INT NOT NULL,
    reward_name VARCHAR(150) NOT NULL,
    reward_description TEXT,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE memberships (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    business_id UUID NOT NULL REFERENCES businesses(id),
    loyalty_program_id UUID NOT NULL REFERENCES loyalty_programs(id),
    current_counter INT NOT NULL DEFAULT 0,
    total_stamps_earned INT NOT NULL DEFAULT 0,
    total_rewards_redeemed INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    status VARCHAR(30) NOT NULL,
    CONSTRAINT uq_user_business_membership UNIQUE (user_id, business_id)
);

CREATE TABLE nfc_tags (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL REFERENCES businesses(id),
    tag_code VARCHAR(150) NOT NULL UNIQUE,
    location_name VARCHAR(150),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    last_used_at TIMESTAMP
);

CREATE TABLE coupon_events (
    id UUID PRIMARY KEY,
    membership_id UUID NOT NULL REFERENCES memberships(id),
    business_id UUID NOT NULL REFERENCES businesses(id),
    employee_id UUID REFERENCES users(id),
    nfc_tag_id UUID REFERENCES nfc_tags(id),
    event_type VARCHAR(40) NOT NULL,
    delta INT NOT NULL,
    idempotency_key VARCHAR(150) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL,
    metadata JSONB
);

CREATE TABLE rewards (
    id UUID PRIMARY KEY,
    membership_id UUID NOT NULL REFERENCES memberships(id),
    business_id UUID NOT NULL REFERENCES businesses(id),
    status VARCHAR(30) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    redeemed_at TIMESTAMP,
    expires_at TIMESTAMP,
    redemption_code VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE wallet_passes (
    id UUID PRIMARY KEY,
    membership_id UUID NOT NULL REFERENCES memberships(id),
    provider VARCHAR(30) NOT NULL,
    external_object_id VARCHAR(200),
    serial_number VARCHAR(200),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE wallet_update_jobs (
    id UUID PRIMARY KEY,
    membership_id UUID NOT NULL REFERENCES memberships(id),
    provider VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP
);
```

## Coupon And Reward Lifecycle

```text
Membership created
-> Stamp request created from NFC tag
-> Employee approves stamp
-> Coupon event inserted
-> Membership counter incremented
-> Reward created when threshold is reached
-> User redeems reward
-> Employee confirms redemption
-> Reward marked REDEEMED
-> Wallet pass update queued
```

Event types:

- `STAMP_REQUESTED`
- `STAMP_ADDED`
- `STAMP_REJECTED`
- `REWARD_CREATED`
- `REWARD_REDEEMED`
- `MANUAL_ADJUSTMENT`
- `STAMP_REVERSED`

Reward states:

- `ACTIVE`: reward is available to redeem.
- `REDEEMED`: reward was used.
- `EXPIRED`: reward is no longer valid.
- `CANCELLED`: reward was removed or invalidated.

## NFC Flow

A single NFC tag per store is acceptable for MVP. The tag should be placed near
the cashier/counter and should only identify the business or location.

The NFC tag should:

- Identify the business or location.
- Open the mobile app or web deep link.
- Start a stamp request.

The NFC tag should not:

- Automatically prove purchase.
- Store authoritative coupon state.
- Directly increment counters without validation.

Recommended flow:

```text
Customer buys coffee
-> Customer taps store NFC tag
-> App opens loyaltap://stamp?tag=TAG_CODE
-> Backend creates pending stamp request
-> Employee approves request
-> Backend increments membership counter
-> Backend queues wallet update
```

When one tag is not enough:

| Case | Recommendation |
| --- | --- |
| Multiple locations | One tag per location |
| Multiple cash registers | One tag per register |
| Separate loyalty programs | One tag per program or tag with program selection |
| High traffic | Multiple tags near the counter |
| Fraud concerns | Add secure/dynamic tags later |

## API Surface

Customer APIs:

```http
POST /users/me/init
GET /businesses
GET /businesses/{businessId}
POST /memberships
GET /memberships
GET /memberships/{membershipId}
GET /memberships/{membershipId}/rewards
POST /memberships/{membershipId}/wallet/google
POST /memberships/{membershipId}/wallet/apple
```

NFC and stamp request APIs:

```http
POST /nfc-tags/{tagCode}/stamp-requests
GET /stamp-requests/{stampRequestId}
POST /stamp-requests/{stampRequestId}/cancel
```

Employee APIs:

```http
GET /businesses/{businessId}/pending-stamp-requests
POST /stamp-requests/{stampRequestId}/approve
POST /stamp-requests/{stampRequestId}/reject
POST /rewards/{rewardId}/redeem
GET /businesses/{businessId}/memberships/{membershipId}
```

Business owner APIs:

```http
POST /businesses
PATCH /businesses/{businessId}
POST /businesses/{businessId}/employees
DELETE /businesses/{businessId}/employees/{employeeId}
POST /businesses/{businessId}/loyalty-programs
PATCH /loyalty-programs/{programId}
POST /businesses/{businessId}/nfc-tags
PATCH /nfc-tags/{tagId}
```

Wallet sync APIs:

```http
POST /wallet/google/sync/{membershipId}
POST /wallet/apple/sync/{membershipId}
POST /wallet/jobs/{jobId}/retry
```

## Wallet Strategy

Wallet integration should be added after the backend membership and coupon logic
works. The wallet pass should reflect backend state, not replace it.

Google Wallet first:

- Create a `LoyaltyObject` per membership.
- Set `accountId` to a public membership code or membership identifier.
- Display `loyaltyPoints` as the current stamp count.
- Use `barcode.value` or `smartTapRedemptionValue` for redemption/reference.
- Patch the `LoyaltyObject` after stamps or rewards change.

Apple Wallet later:

- Generate signed `.pkpass` files.
- Use `storeCard` style for loyalty cards.
- Use pass type identifier and serial number to uniquely identify passes.
- Implement the Apple pass update web service for dynamic updates.
- Store pass serial number in `wallet_passes`.

Wallet update flow:

```text
Stamp approved
-> DB transaction commits
-> wallet_update_jobs row created
-> Background worker processes job
-> Google/Apple pass is updated
-> Job marked DONE or RETRY
```

Wallet API failures should not block stamp approval. Wallet updates should be
retried asynchronously.

## Security And Fraud Controls

The system should assume that public NFC tags can be tapped repeatedly and
possibly copied. Therefore, the tag is not enough to authorize a stamp.

MVP protections:

- Employee approval required for each stamp.
- Only one pending stamp request per user/business at a time.
- Stamp requests expire after 1-2 minutes.
- Rate limit repeated requests by user, business, device, and NFC tag.
- Every stamp and redemption writes a `coupon_event` row.
- Use idempotency keys for approve/redeem endpoints.
- Use role checks for all employee and business owner operations.

Future protections:

- Secure NFC tags with challenge-response.
- Employee device registration.
- Suspicious activity dashboard.
- Geo-fencing or location validation for employees.
- Rotating QR/barcodes for redemption.
- Separate POS integration for automatic purchase validation.

## MVP Roadmap

| Phase | Scope | Goal |
| --- | --- | --- |
| Phase 1 | Backend core: users, businesses, loyalty programs, memberships, events, rewards | Prove core business logic |
| Phase 2 | Mobile app: auth, my cards, business card, NFC tap flow | Allow customers to collect stamps |
| Phase 3 | Employee mode: pending requests, approve/reject, redeem reward | Make the in-store flow usable |
| Phase 4 | Google Wallet: add pass, update counter, barcode display | Add wallet convenience |
| Phase 5 | Apple Wallet: signed passes and update service | Support iOS Wallet users |
| Phase 6 | Admin panel and fraud tooling | Prepare for multiple businesses |

## Implementation Checklist

Backend first:

- Create backend Spring Boot project.
- Add PostgreSQL docker-compose service.
- Add Liquibase migrations.
- Implement users table and external auth mapping.
- Implement businesses and employees.
- Implement loyalty programs and memberships.
- Implement NFC tags.
- Implement stamp request approval flow.
- Implement `coupon_events` and rewards.
- Add `wallet_update_jobs` table.
- Add integration tests for stamp/redeem transactions.

Mobile follow-up:

- Create React Native / Expo app.
- Add external auth SDK.
- Add card list screen.
- Add business details and join flow.
- Add NFC/deep-link handling.
- Add stamp request screen.
- Add employee approval mode.
- Add reward display and redemption screen.
- Add Google Wallet button later.

## References

- [Google Wallet Loyalty Cards](https://developers.google.com/wallet/retail/loyalty-cards)
- [Google Wallet API Libraries](https://developers.google.com/wallet/retail/loyalty-cards/resources/libraries)
- [Google Wallet Pass Updates](https://developers.google.com/wallet/retail/loyalty-cards/use-cases/updates)
- [Google Wallet Smart Tap](https://developers.google.com/wallet/smart-tap)
- [Apple Wallet Developer Guide](https://developer.apple.com/library/archive/documentation/UserExperience/Conceptual/PassKit_PG/)
- [Apple Wallet Pass Updates](https://developer.apple.com/library/archive/documentation/UserExperience/Conceptual/PassKit_PG/Updating.html)
- [Android NFC Basics](https://developer.android.com/develop/connectivity/nfc/nfc)
- [Clerk Authenticated Backend Requests](https://clerk.com/docs/guides/development/making-requests)
- [Firebase Authentication](https://firebase.google.com/docs/auth)
- [Supabase Auth](https://supabase.com/docs/guides/auth)
