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

- Use Java 25 and Spring Boot 3.5.16 for the backend.
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

## Git Workflow

### General Rules

- Treat `main` as the protected integration branch. Do not commit directly to
  `main`; create a focused topic branch and merge it through a pull request.
- Agents may inspect Git state and prepare changes, but must not create commits,
  push branches, rewrite shared history, merge pull requests, or create tags
  unless the user explicitly requests that action.
- Before editing, run `git status --short --branch` and preserve unrelated user
  changes. Never discard, reset, amend, or include changes that are outside the
  current task.
- Never commit credentials, access tokens, private keys, local environment
  files, IDE state, build output, logs, or other generated artifacts unless the
  repository intentionally tracks them.
- Do not use destructive history or worktree commands such as
  `git reset --hard`, `git clean -fd`, or `git checkout -- <path>` without
  explicit user approval.

### Branch Naming

- Create branches from an up-to-date `main`.
- Use lowercase kebab-case with a type prefix:

```text
feat/reward-reservation
fix/stamp-approval-locking
refactor/membership-service
test/reward-redemption-expiry
docs/local-development
chore/update-dependencies
```

- Keep one coherent task per branch. Do not combine unrelated features, fixes,
  refactors, or dependency upgrades.
- Use issue identifiers when available, for example
  `feat/123-reward-reservation`.

### Starting And Synchronizing Work

- Fetch before starting work so remote state can be reviewed without changing
  the current branch:

```bash
git fetch origin --prune
git switch main
git pull --ff-only origin main
git switch -c <type>/<short-description>
```

- Prefer `git fetch` followed by an explicit rebase or fast-forward operation
  over an unqualified `git pull`.
- Use `git pull --ff-only` on `main`. If it fails because histories diverged,
  stop and inspect the history instead of creating an accidental merge commit.
- To update an unshared topic branch:

```bash
git fetch origin --prune
git rebase origin/main
```

- Rebase only local commits or a branch whose collaborators have agreed to the
  rewrite. Do not rebase, amend, or otherwise rewrite shared history without
  coordination.
- Resolve conflicts by understanding both sides. After resolution, run relevant
  tests again and inspect the resulting diff before continuing.

### Staging And Grouping Commits

- A commit must represent one logical, independently reviewable change.
- Keep production code, its tests, and directly required documentation or
  migration updates together when they form one atomic behavior change.
- Database changes must keep the Liquibase changeset, persistence model,
  repository behavior, tests, and affected documentation consistent in the
  same commit or in an ordered series where every commit remains valid.
- Separate unrelated formatting, refactoring, dependency upgrades, generated
  files, and behavior changes into different commits.
- Avoid partial commits that do not compile or intentionally break tests. Do
  not leave `WIP`, `fixup!`, or `squash!` commits in a review-ready branch.
- Stage deliberately with explicit paths or interactive staging:

```bash
git add <path>...
git add -p
git diff --staged
```

- Review `git status --short` and `git diff --staged` immediately before every
  commit. Do not use `git add .` or `git commit -a` without first reviewing all
  affected files.

### Commit Messages

- Follow Conventional Commits:

```text
<type>(<optional-scope>)!: <imperative summary>

<optional body explaining why>

<optional footers>
```

- Allowed types:
  - `feat`: new user-visible or API capability.
  - `fix`: defect correction.
  - `refactor`: internal restructuring without behavior change.
  - `perf`: performance improvement.
  - `test`: test-only change.
  - `docs`: documentation-only change.
  - `build`: build system or dependency change.
  - `ci`: continuous integration change.
  - `chore`: maintenance not covered by another type.
  - `revert`: revert of an earlier commit.
- Prefer domain scopes such as `auth`, `business`, `membership`, `stamprequest`,
  `reward`, `redemption`, `wallet`, `db`, `security`, `deps`, or `docs`.
- Write the subject in imperative mood, lowercase after the colon, without a
  trailing period, and keep it concise, preferably no more than 72 characters.
- Describe the reason and important tradeoffs in the body when the subject
  alone is insufficient. Do not merely repeat the diff.
- Mark breaking changes with `!` and include a `BREAKING CHANGE:` footer that
  explains the migration impact.
- Reference issues in footers when applicable, for example `Closes #123`.

Examples:

```text
feat(reward): reserve points and limited stock
fix(stamprequest): lock membership during approval
refactor(auth): isolate external token verification
test(redemption): cover expired reservation rollback
build(deps): add PostgreSQL Testcontainers support
docs: document local database setup
```

### Validation Before Push

- Run the smallest relevant test suite first, then the broader suite required
  by the change.
- For normal Java changes, run at least:

```bash
./mvnw test
```

- For database changes, also validate Liquibase against PostgreSQL using the
  project's integration-test workflow once available.
- Before pushing, review:

```bash
git status --short --branch
git diff origin/main...HEAD
git log --oneline origin/main..HEAD
```

- Do not push known failing code unless the user explicitly requests a
  work-in-progress branch and the failure is clearly documented.

### Pushing And Pull Requests

- Push a new topic branch and set its upstream explicitly:

```bash
git push -u origin <branch>
```

- After the upstream is configured, use `git push` for normal fast-forward
  updates.
- Never force-push `main`, release branches, tags, or another contributor's
  branch.
- If an explicitly approved rebase requires updating your own topic branch,
  fetch first and use `git push --force-with-lease`, never `git push --force`.
- Open a pull request into `main`. Its title should follow the same Conventional
  Commit format and its description should summarize the purpose, key design
  decisions, test evidence, migration/configuration impact, and related issue.
- Keep pull requests focused and reasonably small. Split unrelated changes into
  separate branches and pull requests.
- Prefer squash merging for a concise `main` history unless the maintainer asks
  to preserve the individual commit series. Ensure the final squash commit
  message follows these commit-message rules.
- After merge, update local `main` with `git fetch origin --prune` followed by
  `git pull --ff-only origin main`, then delete the merged local topic branch.

### Git References

- [Git staging and snapshot workflow](https://git-scm.com/book/en/v2/Getting-Started-What-is-Git%3F)
- [Git contributing workflows](https://git-scm.com/book/en/v2/Distributed-Git-Contributing-to-a-Project)
- [Git fetch documentation](https://git-scm.com/docs/git-fetch)
- [Git pull documentation](https://git-scm.com/docs/git-pull)
- [Git push documentation](https://git-scm.com/docs/git-push)
- [Conventional Commits 1.0.0](https://www.conventionalcommits.org/en/v1.0.0/)
- [GitHub pull request documentation](https://docs.github.com/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/about-pull-requests)

## Documentation

- Keep `README.md` practical and README-like: setup, architecture decisions,
  endpoint map, data model, workflows, and implementation checklist.
- When adding endpoints, update the API surface in `README.md`.
- When adding or changing tables, update the data model and database diagram
  link in `README.md`.
- Do not paste the DBML/dbdiagram source into `README.md`; link to the shared
  dbdiagram.io diagram instead.
