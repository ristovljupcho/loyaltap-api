# LoyalTap API

Backend API for LoyalTap, an NFC-based digital loyalty and rewards system for
cafes, restaurants, and small businesses. LoyalTap replaces paper stamp cards
with digital memberships, points, rewards, QR redemption, and optional wallet
passes.

The API is responsible for the authoritative state of users, businesses,
memberships, NFC stamp requests, reward reservations, QR redemptions, wallet
pass mappings, and wallet update jobs.

## Core Rule

```text
Database = source of truth
NFC tag = interaction trigger
QR code = redemption proof token
Wallet pass = display/sync surface
```

## MVP Scope

Target users:

- Customers who collect points and redeem rewards.
- Business owners who configure rewards and NFC tags.
- Employees who approve point collection and process redemptions.
- Administrators who monitor system usage and businesses.

MVP capabilities:

- External authentication.
- Business and employee management.
- Membership per user per business.
- NFC-based stamp request flow.
- Employee approval before points are added.
- Reward catalog with optional stock limits.
- Reward reservation with QR generation.
- Employee scan and trade acceptance.
- Google Wallet integration first, Apple Wallet later.

## Recommended Stack

| Layer | Choice | Why |
| --- | --- | --- |
| Backend | Java 25 + Spring Boot 3.5.16 | Strong transactional APIs and long-term maintainability |
| Database | PostgreSQL, including Neon-hosted PostgreSQL | Relational constraints, transactions, row locking, and audit-friendly state |
| Migrations | Liquibase | Repeatable database changes |
| Auth | Clerk first, Firebase Auth as an alternative | Avoid custom password/session logic |
| Mobile app | React Native / Expo | Good fit for NFC, QR, and wallet flows |
| Infrastructure | GitHub Actions CI + Docker-friendly PostgreSQL | Simple local development and repeatable verification |
| Wallets | Google Wallet first, Apple Wallet later | Lower MVP complexity |

## Current Project Versions

| Component | Current Version |
| --- | --- |
| API artifact | `com.loyaltap:loyaltap-api:0.0.1-SNAPSHOT` |
| Java | `25` |
| Spring Boot parent | `3.5.16` |
| Maven Wrapper | `3.3.4` |
| Maven distribution | `3.9.16` |
| CI PostgreSQL image | `postgres:17-alpine` |

## Local Development

The repository is scaffolded as a Spring Boot Maven project. Use the checked-in
Maven wrapper so local builds match CI.

```bash
./mvnw spring-boot:run
./mvnw test
```

On Windows PowerShell, use:

```powershell
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
```

Local application startup requires a PostgreSQL database and matching Spring
datasource/Liquibase configuration. The CI workflow uses:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/loyaltap
SPRING_DATASOURCE_USERNAME=loyaltap
SPRING_DATASOURCE_PASSWORD=loyaltap
SPRING_LIQUIBASE_URL=jdbc:postgresql://localhost:5432/loyaltap
SPRING_LIQUIBASE_USER=loyaltap
SPRING_LIQUIBASE_PASSWORD=loyaltap
SPRING_JPA_OPEN_IN_VIEW=false
```

Recommended local services and configuration:

- PostgreSQL database, aligned with the CI `postgres:17-alpine` image where practical.
- Liquibase migrations on application startup or CI.
- Clerk or Firebase Auth credentials.
- Wallet provider credentials only after the core stamp/reward flow works.

## Neon Database

The project includes a `neon` Spring profile for connecting to a Neon-hosted
PostgreSQL database. Keep real connection strings and passwords in local or
deployment environment variables only; do not commit `.env` files.

1. Create or open a Neon project.
2. In the Neon console, click **Connect** and select the branch, database, and role.
3. Use the pooled hostname with `-pooler` for normal application traffic.
4. Convert the Neon connection string to a JDBC URL for Spring:

```text
Neon URI:
postgresql://<role>:<password>@<endpoint-id>-pooler.<region>.aws.neon.tech/<database>?sslmode=require&channel_binding=require

Spring JDBC URL:
jdbc:postgresql://<endpoint-id>-pooler.<region>.aws.neon.tech/<database>?sslmode=require&channelBinding=require
```

For a local `.env` file, set these values. Spring Boot imports `.env` from the
project root when the application starts:

```text
spring.profiles.active=neon
NEON_DATABASE_URL=jdbc:postgresql://<endpoint-id>-pooler.<region>.aws.neon.tech/<database>?sslmode=require&channelBinding=require
NEON_DATABASE_USERNAME=<role>
NEON_DATABASE_PASSWORD=<password>
```

For Liquibase migrations, a direct Neon connection is safer than PgBouncer
transaction pooling. If needed, also set:

```text
NEON_MIGRATION_DATABASE_URL=jdbc:postgresql://<endpoint-id>.<region>.aws.neon.tech/<database>?sslmode=require&channelBinding=require
NEON_MIGRATION_DATABASE_USERNAME=<role>
NEON_MIGRATION_DATABASE_PASSWORD=<password>
```

Use `.env.example` as the local template, then create an untracked `.env` file
with real Neon values. If you prefer IDE or deployment environment variables,
use `SPRING_PROFILES_ACTIVE=neon` instead of `spring.profiles.active=neon`.

## Continuous Integration

The GitHub Actions workflow in `.github/workflows/ci.yml` runs for pull requests
and pushes to `main`. It currently uses Java 25, Maven Wrapper 3.3.4 with Maven
3.9.16, Spring Boot 3.5.16, and `postgres:17-alpine`. It:

- Compiles the project and runs all tests with `./mvnw verify`.
- Starts a PostgreSQL service container.
- Packages and starts the application with Java 25.
- Requires the application to report healthy at `/actuator/health`.

To prevent broken code from reaching `main`, configure a GitHub branch ruleset:

1. Open **Settings -> Rules -> Rulesets** in the GitHub repository.
2. Create a branch ruleset targeting the `main` branch.
3. Enable **Require a pull request before merging**.
4. Enable **Require status checks to pass**.
5. Add the required status check named `Verify build and startup`.
6. Block force pushes and branch deletion.
7. Disable bypass permissions for contributors who should follow the rule.

The workflow reports failures on any branch where it runs. The branch ruleset
is what prevents direct pushes or pull request merges into `main` when the
required check fails.

## High-Level Architecture

```text
Customer Mobile App
  - Shows memberships and points
  - Reads NFC tag/deep link
  - Allows reward reservation
  - Shows QR code
  - Adds pass to Google/Apple Wallet

Merchant/Employee App or Mode
  - Shows pending stamp requests
  - Approves or rejects point collection
  - Scans reward QR codes
  - Accepts reward trade

Backend API
  - Source of truth
  - Auth verification
  - Membership balances
  - Reservation and expiry logic
  - Reward stock logic
  - Wallet synchronization

PostgreSQL Database
  - Users, businesses, memberships
  - NFC tags and stamp requests
  - Rewards and reward redemptions
  - Wallet pass mappings and sync jobs
```

## Authentication

Use external authentication to avoid building password management, email
verification, password reset, and session handling from scratch. Clerk is a
strong MVP choice; Firebase Auth is a good mobile-first alternative.

Until external authentication is integrated, callers supply the user's string
ID. The verified external provider subject will replace that input source later.

```text
users.id = caller-supplied string (future external provider subject)
users.status = ACTIVE / DISABLED / DELETED
```

Roles:

- `CUSTOMER`: collects points and reserves rewards.
- `OWNER`: manages business, rewards, employees, and tags.
- `MANAGER`: manages operational business actions.
- `EMPLOYEE`: approves stamp requests and scans reward QR codes.
- `ADMIN`: platform-level administration.

## Database Model

The database model is centered on point balances, reserved points, reward stock,
and QR redemption sessions.

| Table | Purpose |
| --- | --- |
| `users` | Users identified by a string ID, with external auth integration planned |
| `business` | Businesses using LoyalTap |
| `employee` | Authorization relationship between users and businesses |
| `memberships` | One loyalty card per user per business, with points and reserved points |
| `nfc_tags` | Physical NFC tags that start stamp request flow |
| `stamp_requests` | Pending employee approval before points are added |
| `rewards` | Reward catalog with required points and optional stock limits |
| `reward_redemptions` | Reward reservation and QR redemption sessions |
| `wallet_passes` | Google Wallet / Apple Wallet pass mapping |
| `wallet_update_jobs` | Async wallet sync retries |

Balance and stock formulas:

```text
available_points = memberships.points_balance - memberships.reserved_points

For unlimited reward:
rewards.stock_quantity = null

For limited reward:
available_stock = rewards.stock_quantity - rewards.stock_reserved - rewards.stock_redeemed
```

Memberships are created with `ACTIVE` status and zero balances/counters. Clients
can create a membership and read its state, but only stamp and redemption
workflows may change points or reward counters.

Database diagram:

- [View the LoyalTap database diagram on dbdiagram.io](https://dbdiagram.io/d/LoyalTap-6929b515d6676488bacfd3b3)

## NFC Stamp Request Flow

The NFC tag does not automatically add points. It creates a pending
`stamp_request` that an employee must approve.

```text
Customer buys item
-> Customer taps NFC tag
-> Backend creates stamp_request with status PENDING
-> Employee approves
-> membership.points_balance increases
-> stamp_request becomes APPROVED
```

The `stamp_requests` table prevents self-stamping. A customer cannot farm points
by repeatedly tapping a public NFC tag because points are not added until an
employee approves the request.

## Reward Reservation And QR Redemption Flow

This is the preferred reward flow. The user reserves a reward first, points are
provisionally locked, a QR code is generated, and the employee completes the
trade by scanning and accepting it.

Reservation flow:

```text
User clicks reward
-> Backend locks membership and reward rows
-> Backend checks available_points >= reward.required_points
-> If reward is limited, backend checks available_stock > 0
-> Backend increases membership.reserved_points
-> If limited, backend increases rewards.stock_reserved
-> Backend creates reward_redemption with status RESERVED
-> Backend generates QR code from redemption_code
```

Employee acceptance flow:

```text
Employee scans QR
-> Backend finds reward_redemption by redemption_code
-> Backend verifies status = RESERVED and expires_at > now
-> Backend verifies employee belongs to same business
-> Backend subtracts points from membership.points_balance
-> Backend subtracts points from membership.reserved_points
-> If limited, backend decreases rewards.stock_reserved
-> Backend increases rewards.stock_redeemed
-> Backend marks reward_redemption as REDEEMED
```

Expiry or cancellation flow:

```text
QR expires or user cancels
-> Backend finds RESERVED reward_redemption
-> Backend releases membership.reserved_points
-> If limited, backend releases rewards.stock_reserved
-> Backend marks reward_redemption as EXPIRED or CANCELLED
```

Example balance movement:

| Step | `points_balance` | `reserved_points` | `available_points` |
| --- | ---: | ---: | ---: |
| Before reserve | 50 | 0 | 50 |
| User reserves 20-point reward | 50 | 20 | 30 |
| Employee accepts trade | 30 | 0 | 30 |
| If QR expires instead | 50 | 0 | 50 |

Reward stock behavior:

| Reward Type | `stock_quantity` | Reservation Behavior |
| --- | --- | --- |
| Unlimited | `null` | Only points are reserved. No stock check. |
| Limited | number | Points and one stock unit are reserved. |

## API Surface

Customer APIs:

```http
POST /users/me/init
GET /business
GET /memberships?userId={userId}
POST /memberships
GET /memberships/{membershipId}
GET /business/{businessId}/rewards
GET /rewards/{rewardId}
POST /rewards/{rewardId}/reserve
GET /reward-redemptions/{redemptionId}
POST /reward-redemptions/{redemptionId}/cancel
POST /memberships/{membershipId}/wallet/google
```

NFC and stamp APIs:

```http
POST /nfc-tags/{tagCode}/stamp-requests
GET /stamp-requests/{stampRequestId}
POST /stamp-requests/{stampRequestId}/cancel
```

Employee APIs:

```http
GET /business/{businessId}/pending-stamp-requests
POST /stamp-requests/{stampRequestId}/approve
POST /stamp-requests/{stampRequestId}/reject
POST /reward-redemptions/scan
POST /reward-redemptions/{redemptionId}/accept
```

Business owner APIs:

```http
POST /business
PUT /business/{businessId}
POST /business/{businessId}/employees
GET /business/{businessId}/employees
GET /business/{businessId}/employees/{employeeId}
PUT /business/{businessId}/employees/{employeeId}
DELETE /business/{businessId}/employees/{employeeId}
POST /business/{businessId}/nfc-tags
POST /business/{businessId}/rewards
PATCH /rewards/{rewardId}
DELETE /rewards/{rewardId}
```

## Wallet Strategy

Wallet passes should mirror backend data. They should not be the authority for
points or redemptions. Start with Google Wallet, then add Apple Wallet later.

- Create a wallet pass per membership.
- Show points balance and business information.
- Update passes asynchronously after point approval or reward redemption.
- Do not block stamp approval or reward redemption if wallet sync fails.

Wallet sync job flow:

```text
Core transaction commits
-> wallet_update_jobs row is created
-> Background worker updates Google/Apple pass
-> Job becomes DONE or RETRY
```

## Security And Fraud Prevention

- Use random opaque `redemption_code` values in QR codes.
- QR codes should expire quickly, for example after 2-5 minutes.
- Only employees of the same business can accept a QR redemption.
- Allow only one active `RESERVED` redemption per membership/business or per
  membership/reward.
- Use database transactions and row locking when reserving or redeeming rewards.
- Run a scheduled expiry job to release reserved points and stock.
- Do not use NFC tag codes as database primary keys; use internal UUIDs and
  unique public tag codes.

Transaction rules:

```text
Reservation transaction:
lock membership
lock reward
validate points and stock
reserve points and stock
create RESERVED redemption

Acceptance transaction:
lock redemption
lock membership
lock reward
validate status and expiry
spend reserved points
finalize stock counters
mark REDEEMED
```

## MVP Roadmap

| Phase | Scope | Goal |
| --- | --- | --- |
| Phase 1 | Core backend and database schema | Prove users, businesses, memberships, points, rewards |
| Phase 2 | NFC stamp flow | Allow point collection with employee approval |
| Phase 3 | Reward reservation and QR scan | Allow users to reserve rewards and employees to accept trade |
| Phase 4 | Wallet sync | Add Google Wallet display and async updates |
| Phase 5 | Admin and fraud tooling | Support more businesses and operational review |
| Phase 6 | Apple Wallet and advanced NFC | Expand platform support |

## Implementation Checklist

Backend:

- Implement schema and migrations.
- Implement external auth mapping.
- Implement memberships with `points_balance` and `reserved_points`.
- Implement NFC tag lookup by `tag_code`.
- Implement stamp request approval transaction.
- Implement rewards with limited/unlimited stock logic.
- Implement reward reservation transaction.
- Implement QR redemption scan and accept transaction.
- Implement expiry job for `RESERVED` redemptions.
- Implement `wallet_update_jobs`.

Mobile:

- Login/register.
- Membership card list.
- NFC tag/deep-link handling.
- Reward catalog screen.
- Reserve reward button.
- QR code display for reserved reward.
- Employee scan screen.
- Employee accept/reject actions.
- Wallet add button later.

## References

- [Google Wallet Loyalty Cards](https://developers.google.com/wallet/retail/loyalty-cards)
- [Google Wallet API Libraries](https://developers.google.com/wallet/retail/loyalty-cards/resources/libraries)
- [Google Wallet Pass Updates](https://developers.google.com/wallet/retail/loyalty-cards/use-cases/updates)
- [Google Wallet Smart Tap](https://developers.google.com/wallet/smart-tap)
- [Apple Wallet Developer Guide](https://developer.apple.com/library/archive/documentation/UserExperience/Conceptual/PassKit_PG/)
- [Android NFC Basics](https://developer.android.com/develop/connectivity/nfc/nfc)
- [Clerk Authenticated Backend Requests](https://clerk.com/docs/guides/development/making-requests)
- [Firebase Authentication](https://firebase.google.com/docs/auth)
- [Supabase Auth](https://supabase.com/docs/guides/auth)
- [Neon Connect from any application](https://neon.com/docs/connect/connect-from-any-app)
- [Neon Connection Pooling](https://neon.com/docs/connect/connection-pooling)
- [PostgreSQL JDBC Connection Parameters](https://jdbc.postgresql.org/documentation/use/)
