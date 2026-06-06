# Konfigyr ![CI](https://github.com/konfigyr/konfigyr-project/actions/workflows/ci.yml/badge.svg) [![codecov](https://codecov.io/gh/konfigyr/konfigyr-project/graph/badge.svg?token=81ATZF33YH)](https://codecov.io/gh/konfigyr/konfigyr-project)

Konfigyr is a Spring Boot-native configuration management platform available as both SaaS and on-premise. It solves the configuration chaos that comes with running microservices at scale: config drift across environments, human errors from manual edits, and no auditability or access control over sensitive properties.

Unlike generic secret stores (HashiCorp Vault) or framework-agnostic tools (Infisical), Konfigyr is opinionated about Spring Boot. It reads property metadata generated at build time to power a type-safe UI that validates values before they reach production, surfaces deprecations, and tracks how configuration evolves across artifact versions.

## Architecture

Three independently deployed services communicate via OAuth2/OIDC:

```
┌──────────────────────────────────────────────────────────┐
│  Frontend  ·  konfigyr-frontend                         │
│  React 19 / TanStack Start · OAuth2 Code + PKCE         │
└────────┬─────────────────────────────────┬──────────────┘
         │ auth                             │ API calls
┌────────▼──────────────┐     ┌────────────▼─────────────┐
│  Identity Provider    │     │  REST API                │
│  konfigyr-identity    │     │  konfigyr-api            │
│  Spring Auth Server   │     │  Spring Boot + Modulith  │
│  PS256/ES256 JWTs     │     │  jOOQ + Liquibase        │
│  External IdP broker  │     │  @RequiresScope auth     │
└───────────────────────┘     └──────────────────────────┘
                                         │
                               ┌─────────▼──────────┐
                               │  PostgreSQL         │
                               │  Liquibase managed  │
                               └────────────────────┘
```

The Identity Provider acts as an **identity broker**: it accepts logins from external providers (GitHub, GitLab, Google, SAML enterprise IdPs) and translates them into standardised Konfigyr JWTs. The REST API only trusts tokens from konfigyr-identity — no multi-issuer configuration needed.

A Gradle/Maven plugin layer handles build-time metadata ingestion: plugins extract `spring-configuration-metadata.json` from Spring Boot applications and push it to the REST API, populating the Artifactory and enabling the type-safe UI.

## Modules

| Module | Description |
|--------|-------------|
| `konfigyr-api` | Core REST API — all business domains: namespaces, vault, artifactory, KMS, audit |
| `konfigyr-identity` | Identity Provider — Spring Authorization Server, external IdP federation, JWT issuance |
| `konfigyr-frontend` | React 19 / TanStack Start frontend application |
| `konfigyr-core` | Shared domain primitives — `EntityId`, `OAuthScope`, shared value objects |
| `konfigyr-data` | Shared database infrastructure — jOOQ configuration, Liquibase base migrations |
| `konfigyr-mail` | Email integration — SMTP transport, Thymeleaf templates |
| `konfigyr-test` | Shared test utilities — `TestPrincipals`, test containers, mock factories |
| `konfigyr-jooq-extensions` | Custom jOOQ type bindings and converters |

### Business Domains (inside `konfigyr-api`)

| Domain | Responsibility |
|--------|----------------|
| `namespace` | Multi-tenancy — namespaces are the top-level tenant container; members have `ADMIN` or `USER` roles |
| `vault` | Per-service configuration — profiles, entries, change requests with optional approval workflow, change history |
| `artifactory` | Metadata registry — artifact versions, property descriptors as JSON Schema, provenance tracking |
| `kms` | Per-namespace encryption — keyset lifecycle backed by [konfigyr-crypto](https://github.com/konfigyr/konfigyr-crypto) |
| `audit` | Centralised audit log — listens to all domain events via `@TransactionalEventListener` |
| `account` | User account management — provisioned from external OAuth providers |
| `feature` | Per-namespace feature flags and limits |
| `membership` | Standalone membership subdomain — member invitations, role management |

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 25 |
| Backend framework | Spring Boot 4.0.6 |
| Module architecture | Spring Modulith 2.0.6 |
| Database | PostgreSQL 18 (jOOQ + Liquibase) |
| Auth server | Spring Authorization Server |
| Frontend | React 19, TanStack Start (SSR), Tailwind CSS |
| Build | Gradle (Groovy DSL) |
| Crypto | [konfigyr-crypto](https://github.com/konfigyr/konfigyr-crypto) (Google Tink + BouncyCastle) |
| Artifactory SDK | [konfigyr-artifactory](https://github.com/konfigyr/konfigyr-artifactory) |

## Getting Started

### Prerequisites

- Java 25+
- Node.js (see `konfigyr-frontend/.nvmrc`)
- Docker (for local services)

### Start local services

```bash
docker compose -f docker-compose.dev.yml up -d
```

This starts:
- **PostgreSQL 18** on `localhost:5432` (`konfigyr-database` / `konfigyr-database`)
- **smtp4dev** on `localhost:8880` (SMTP UI) / `localhost:2525` (SMTP)
- **Keycloak 26** on `localhost:8881` (external IdP for local development)

### Build and test

```bash
# Full build with tests
./gradlew build

# Backend tests only
./gradlew test

# Generate jOOQ sources (required after schema changes)
./gradlew generateJooq

# Frontend
cd konfigyr-frontend
npm install
npm run dev
```

### Run the applications

```bash
# Identity Provider
./gradlew :konfigyr-identity:bootRun

# REST API
./gradlew :konfigyr-api:bootRun

# Frontend (dev server with HMR)
cd konfigyr-frontend && npm run dev
```

## Configuration

Each service is configured via `application.yml` and environment variables. Key properties:

| Property | Description |
|----------|-------------|
| `spring.datasource.url` | PostgreSQL connection URL |
| `spring.security.oauth2.resourceserver.jwt.issuer-uri` | konfigyr-identity URL (for REST API) |
| `konfigyr.identity.issuer` | Public URL of the Identity Provider |

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feat/your-feature`
3. Follow the conventions in [CLAUDE.md](CLAUDE.md)
4. Run `./gradlew build` and ensure all checks pass
5. Open a pull request with a clear description of the change and why
