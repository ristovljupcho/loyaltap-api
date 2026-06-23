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

## Naming

Use camel format for naming files, functions, variables and etc. ex. camelFormat

## Project Structure

For each db table create separate folder named after the table. In the folder it should have different folder for model, dto, mapper, repository, service, utils, controller and other folders if needed.

## Methods

Each method should be properly structured. Create helper methods / classes if needed. Helper methods are created when the code gets too compplicated of there is biler code. 

### Service methods

In the service methods follow CRUD principles for naming, order and general implementation of code.

### Repository methods

Be careful of N + 1 problem that repo methods may have, so to prevent that use custom queries.

## Javadocs

Use Javadocs for methods, variables or anything that is not 100% understandable by jsut reading the name or implementation of code.



