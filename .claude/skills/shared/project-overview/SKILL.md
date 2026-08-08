---
name: project-overview
description: Konfigyr system architecture, business domains, module responsibilities, and design decisions. Use when understanding the system structure or designing new features that span multiple modules.
---

# Konfigyr Project Overview

## System Architecture

Three independently deployed services:

```
┌─────────────────────────────────────────────────────────┐
│ Frontend (React/TanStack) - konfigyr-frontend          │
│ - SSR with Nitro                                        │
│ - OAuth2 Authorization Code + PKCE                      │
│ - Session in encrypted HTTP-only cookie                 │
└─────────┬───────────────────────────────────┬───────────┘
          │                                   │
    1. Auth Flow                      2. API Calls
          │                                   │
┌─────────▼─────────────────┐    ┌──────────▼─────────────┐
│ Identity Provider         │    │ REST API                │
│ konfigyr-identity         │    │ konfigyr-api            │
│ - Spring AuthServer       │    │ - Spring Boot 4.1       │
│ - OAuth2/OIDC broker      │    │ - Spring Modulith       │
│ - JWT issuance (PS256)    │    │ - jOOQ + Liquibase      │
│ - External IdP federation │    │ - @RequiresScope auth   │
└──────────────────────────┘    └────────────────────────┘
                                  │
                            ┌─────▼──────────────┐
                            │ PostgreSQL Database │
                            │ - Liquibase managed │
                            │ - Encrypted vaults  │
                            └────────────────────┘
```

### Identity Provider: Protocol Broker

The IdP is not a plain JWT issuer — it is an **identity broker and protocol translator**. It accepts
authentication from upstream external providers and translates those identities into a single,
standardized OIDC JWT issued by Konfigyr. Application code never needs to handle provider-specific
token formats.

Currently configured login providers: **GitHub** and generic **OIDC** (Keycloak-compatible). GitLab is
supported in code as a *trusted issuer* for token-exchange/workload-identity flows, not as a login
provider. Google and SAML are not implemented.

Key responsibilities:
- Delegates authentication to external providers; never stores credentials itself
- Maps external user identities to local `Account` records
- Issues PS256-signed JWT access tokens and ID tokens (ES256 is only used to *verify* tokens from
  trusted external issuers, never for Konfigyr-issued tokens)
- Exposes standard OIDC endpoints: JWKS (`/oauth/jwks`), discovery (`.well-known`), userinfo
  (`/oauth/userinfo`), token introspection (`/oauth/introspect`), and revocation (`/oauth/revoke`)

## Business Domains (Modules)

| Module | Responsibility | Key Entities |
|--------|---|---|
| **namespace** | Multi-tenancy | Namespace, NamespaceRole (enum: ADMIN/USER), Service, NamespaceApplication (OAuth2 clients, created via NamespaceApplicationDefinition), NamespaceTrustedIssuer (Workload Identity), NamespaceFeatures (see dedicated section below) |
| **membership** | Namespace membership | Member, Invite (command), Invitation, InvitationState (see dedicated section below) |
| **vault** | Config management | Profile, ProfilePolicy, PropertyChange(s), ChangeRequest, ChangeHistory |
| **artifactory** | Metadata registry | ArtifactDefinition, VersionedArtifact, PropertyDefinition, ArtifactKey/ArtifactCoordinates, Owner, ArtifactVisibility (see dedicated section below) |
| **kms** | Encryption | KeysetMetadata, KeyMetadata |
| **audit** | Event logging | AuditEvent, AuditRecord |
| **account** | User management | Account (from OAuth) |
| **feature** | Feature flags | FeatureDefinition, FeatureValue (per-namespace limits, e.g. `NamespaceFeatures.MEMBERS_COUNT`) |

## Key Design Decisions

### 1. Per-Namespace Encryption

- Each namespace owns a `KeysetMetadata` in KMS
- Vault data encrypted with the namespace's DEK keyset
- Key compromise in one namespace doesn't expose others
- Benefits: tenant isolation, regulatory compliance

### 2. JSON Schema for Property Metadata

- Property descriptors stored as JSON Schema (not Java types)
- Enables language-agnostic validation
- Rich UI rendering based on schema
- Version-to-version diff analysis

### 3. Property Metadata Deduplication

`PropertyDefinition` is a single entity (not a two-tier definition/occurrence split) keyed by a checksum
over its full contents (name, type, schema, description, deprecation). It belongs to an `ArtifactDefinition`
but is shared many-to-many across the artifact's versions, tracked via an `occurrences` count and
`firstSeen`/`lastSeen` version markers — so identical property definitions across many artifact versions
are still stored once.

### 4. Change Workflow

```
Submit Change Request → Review (approve/comment) → Merge
```

or, for `UNPROTECTED` profiles, skip straight to a direct `apply`. Profiles can be `UNPROTECTED` (direct
apply), `PROTECTED` (submit → review → merge required), or `IMMUTABLE` (no changes permitted).

### 5. Namespace OAuth2 Clients

Namespaces can register OAuth2 clients (`NamespaceApplicationDefinition`/`NamespaceApplication`, identified
by a `NamespaceClientId`) with scoped permissions for CI/CD pipelines, Gradle/Maven plugins, and other
automated integrations. These clients authenticate against the Identity Provider and receive access tokens
scoped to specific operations from the real `OAuthScope` enum, e.g. `artifactory:publish`, `profiles:write`.
This is the primary integration mechanism for build-time metadata ingestion.

## Module Dependencies

```
Frontend
  ├── Identity Provider (OAuth2 login)
  └── REST API (all domain operations)

REST API
  ├── namespace → artifactory (NamespaceOwnerResolver implements OwnerResolver), feature
  ├── membership → namespace (Member/Invitation carry NamespaceRole and a Namespace projection)
  ├── vault → namespace, crypto (konfigyr-core library, to encrypt vault data)
  ├── kms → namespace (NamespaceManager)
  ├── artifactory → (no outbound dependency on vault or kms)
  └── audit → (listens to all events via @TransactionalEventListener: account, namespace, service,
      invitation, vault, kms, artifactory)

Identity Provider
  └── Account provisioning (standalone)
```

Two deliberate exceptions to "modules communicate via events, never direct dependencies" exist today:

- `DefaultNamespaceManager.create()` inserts the initial administrator directly into the `NAMESPACE_MEMBERS`
  table (owned by `membership`) rather than calling `Memberships` — see the Data Flow example below.
- `Dashboards` (`namespace/dashboard`) reads `VAULT_CHANGE_REQUESTS`/`VAULT_PROPERTIES` (owned by `vault`)
  and `ARTIFACTS` (owned by `artifactory`) directly via jOOQ to build a cross-module summary, rather than
  calling those modules' service interfaces.

## KMS Domain: Key Hierarchy and Lifecycle

The KMS module is built on the **konfigyr-crypto** library
(GitHub: https://github.com/konfigyr/konfigyr-crypto), a Spring-compatible abstraction over Google Tink
that enforces a two-tier key architecture and clean key lifecycle management.

### Two-Tier Key Hierarchy

```
KEK (Key Encryption Key) — master key, NEVER stored in the database
  └── wraps ↓
      DEK (Data Encryption Key) — stored in DB as eDEK (encrypted DEK)
        └── encrypts ↓
            vault entries, sensitive fields
```

- The KEK is provided to the application via `CryptoProperties.MasterKey` — a base64-encoded value, or a
  set of Shamir secret shares — typically delivered as a Kubernetes Secret. There is currently no AWS KMS
  or GCP KMS provider implementation in the codebase; SaaS and on-premise deployments use the same
  config-provided master key mechanism today.
- **DEKs** are stored in the database only in their encrypted (wrapped) form

### Key Components (konfigyr-crypto)

| Component | Role |
|-----------|------|
| `Keyset` | The collection of DEKs performing cryptographic operations; has one primary key for new operations, others for decryption/verification only |
| `KeysetStore` | Primary interface — creates, rotates, and drives lifecycle transitions |
| `KeysetFactory` | Bridges the API to an underlying crypto library (Tink, Nimbus JOSE) |
| `Algorithm` | Immutable value object declaring algorithm identity (e.g. `tink:AES256_GCM`, `jose:ES256`) |

### Key Purposes

Each `KeysetMetadata` is assigned exactly one `KeysetPurpose` at creation. Purpose cannot be changed.

| Purpose | Operations | Algorithms (`KeysetMetadataAlgorithm`) |
|---------|-----------|------------|
| `ENCRYPTION` | `encrypt(plaintext)` / `decrypt(ciphertext)` | AES128/256-GCM, ECIES-P256 |
| `SIGNING` | `sign(data)` / `verify(signature)` — exposes public key | ED25519, ECDSA-P256/384/521, RSA-SSA-PKCS1 |

There is no third "key encapsulation"/wrap-only purpose and no post-quantum (ML-KEM/ML-DSA) algorithm
support today.

### Key Lifecycle States

Two related but distinct enums exist — don't conflate them:

- **`KeyStatus`** (per-key, library-level, `com.konfigyr.crypto`): `INITIALIZING`, `INITIALIZATION_FAILED`,
  `ENABLED`, `DISABLED`, `COMPROMISED`, `PENDING_DESTRUCTION`, `DESTRUCTION_FAILED`, `DESTROYED`.
- **`KeysetMetadataState`** (per-keyset, app-level, `com.konfigyr.kms`): `ACTIVE`, `INACTIVE`,
  `PENDING_DESTRUCTION`, `DESTROYED` — a coarser view that buckets the underlying keys' `KeyStatus` values.

```
ENABLED → DISABLED → PENDING_DESTRUCTION → DESTROYED
    ↘ COMPROMISED → PENDING_DESTRUCTION → DESTROYED
```

- `ENABLED` — active; primary key is used for new crypto operations
- `DISABLED` — inactive; excluded from new operations, still readable for decrypt/verify
- `COMPROMISED` — emergency state; triggers immediate rotation
- `PENDING_DESTRUCTION` — grace period before material is wiped (scheduled task runs hourly by default,
  `KeysetTaskAutoConfiguration.KeysetDestructionTask`, configurable via
  `konfigyr.crypto.tasks.keyset-destruction.interval`)
- `DESTROYED` — key material deleted (`data()` set to null); record retained for audit

### Business Rules (invariants to never violate)

1. **Purpose consistency** — a `KeyMetadata`'s algorithm must be compatible with the parent keyset's `KeysetPurpose`
2. **Immutability of material** — once generated, key material can never be altered
3. **Single primary key** — a keyset should have exactly one primary `KeyMetadata` for crypto operations
4. **Soft-deletion** — `KeysetMetadata` cannot be destroyed while it has active `KeyMetadata` records; keys must be scheduled for destruction first

## Artifactory Domain: Two Distinct Layers

The Artifactory domain has two layers that must not be conflated:

1. **konfigyr-artifactory SDK** — a separate, standalone library
   (GitHub: https://github.com/konfigyr/konfigyr-artifactory, Maven: `com.konfigyr:konfigyr-artifactory`)
   used by Gradle/Maven build plugins to model and serialize artifact metadata. It ships **no HTTP client
   and no wire route of its own** — it's a pure domain-model + Jackson-serialization library. Its types
   are consumed on the `konfigyr-api` side as the payload shape for publish requests.
2. **`com.konfigyr.artifactory`** in `konfigyr-api` — the REST API's own persisted domain and service
   layer, built by ingesting the SDK's `ArtifactMetadata`. This is where namespace ownership, visibility,
   search, and the REST endpoints actually live.

### SDK Entities (`com.konfigyr.artifactory` in the konfigyr-artifactory library)

Each entity is a `public interface` backed by a `Default<Interface>` record implementation.

| Entity | Description |
|--------|-------------|
| `Artifact` (+ `ArtifactDescriptor`) | Unique component identified by Maven coordinates (`groupId:artifactId:version`). |
| `PropertyDescriptor` | Configuration property metadata: name, type, description, default, JSON Schema, deprecation. |
| `ArtifactMetadata` | Upload envelope — aggregates all `PropertyDescriptor`s for one artifact version, plus a checksum. This is the publish payload uploaded by build plugins. |
| `Publication` | A version-change/upload event. Lifecycle via `PublicationState`: `PENDING → PUBLISHED → FAILED`. |
| `Manifest` / `ManifestEntry` | A Konfigyr service's current artifact snapshot — used to detect metadata drift across environments. |
| `ServiceRelease` / `ServiceReleaseCandidate` / `ServiceReleaseEntry` | Transient publish/build-attempt report for a service, separate from the content-only `Manifest`. Lifecycle via `ReleaseState`: `PENDING → RELEASED → FAILED` (note: `RELEASED`, not `PUBLISHED` — distinct from `PublicationState`). |

There is no SDK type linking a namespace-owned service to artifact versions — that concept exists only
on the `konfigyr-api` side.

### `konfigyr-api` Domain & Service Layer (`com.konfigyr.artifactory`)

Persisted entities:

| Entity | Description |
|--------|-------------|
| `ArtifactDefinition` | Aggregate root for an artifact identity (`groupId:artifactId`) — owner, visibility, name/description/links. |
| `VersionedArtifact` | Aggregate root for one published version — coordinates, `PublicationState`, checksum, publish timestamp. |
| `PropertyDefinition` | A persisted configuration property for a given artifact/version. |
| `ArtifactKey` / `ArtifactCoordinates` | `ArtifactKey` = `groupId:artifactId` identity pair; `ArtifactCoordinates` extends it with `version`. |
| `Owner` | Minimal namespace projection (`EntityId id`, `String slug`) resolved via `OwnerResolver`. |
| `ArtifactVisibility` | Enum: `PUBLIC` (visible to any caller) / `PRIVATE` (visible only to the owning namespace). |

Two service interfaces — don't conflate their semantics:

- **`Artifactory`** (impl `DefaultArtifactory`) — visibility-based **reads only**: `get`/`exists`/`existing`.
  `PUBLIC` artifacts are visible to any caller; `PRIVATE` artifacts only to their owning namespace (a
  `null` `Owner` sees only `PUBLIC`). Used by machine/OAuth clients via `ArtifactoryController`, routes
  `/artifacts/{groupId}/{artifactId}[/{version}]`, scope `READ_ARTIFACTS` (mutations `PUBLISH_ARTIFACTS`).
- **`Publications`** (impl `DefaultPublications`) — strict **per-namespace ownership**, no visibility
  branching: search (`artifacts`/`versions`), `get`/`exists` at both `ArtifactKey` and `ArtifactCoordinates`
  level, `publish`, `retract` (remove one version), `deregister` (remove an artifact + all versions),
  `changeVisibility`. Used by the namespace registry via `PublicationsController`, routes
  `/namespaces/{namespace}/artifacts/{groupId}/{artifactId}[/{version}]`, authorized via
  `isMember`/`isAdmin` SpEL + `PUBLISH_ARTIFACTS` scope for mutations.
- **`ArtifactoryQueries`** — package-private shared class centralizing jOOQ query building and row
  mapping, used by both `DefaultArtifactory` and `DefaultPublications`.

Related controllers in the same package: `ArtifactOwnershipTransfersController` and
`GroupVerificationsController` (both under `/namespaces/{namespace}/...`, `isAdmin` + `READ_NAMESPACES`/
`WRITE_NAMESPACES` scopes).

Domain events — `ArtifactoryEvent` is a sealed hierarchy, every event carries the artifact's `Owner`:

- `ArtifactEvent` (keyed by `ArtifactCoordinates`): `PublicationCreated`, `PublicationCompleted`,
  `PublicationFailed`, `PublicationRetracted`.
- `DefinitionEvent` (keyed by `ArtifactKey`): `Deregistered`, `VisibilityChanged`.
- `OwnershipTransferEvent` (carries `groupId` + `from`/`to` `Owner`s): `OwnershipTransferAccepted`,
  `OwnershipTransferRejected`, `OwnershipTransferCancelled`.

All of the above are consumed by `AuditEventListener` and have matching message templates in
`audit.properties`.

### Metadata Ingestion Flow

```
Build (Gradle/Maven plugin)
  1. Extract spring-configuration-metadata.json from classpath
  2. Translate property types → JSON Schema, build an SDK ArtifactMetadata payload
  3. POST /namespaces/{namespace}/artifacts/{groupId}/{artifactId}/{version}
     (authenticated as a namespace OAuth2 client with PUBLISH_ARTIFACTS scope)
  ↓
konfigyr-api Publications.publish(Owner, ArtifactMetadata)
  4. Persist/update ArtifactDefinition (groupId:artifactId) and PropertyDefinition rows
  5. Create a new VersionedArtifact, state PENDING → PUBLISHED
  6. Publish PublicationCreated / PublicationCompleted (or PublicationFailed) events
```

## Namespace & Membership Domain: Applications, Services, Trusted Issuers & Invitations

`com.konfigyr.namespace` owns far more than the `Namespace` aggregate itself. `com.konfigyr.membership`
is a separate module that depends on it (`Member`/`Invitation` both carry a `NamespaceRole` and a
namespace projection) — grouped here because both are covered by the "Namespaces & Accounts" area in
CLAUDE.md's top-level domain table.

### Namespace-Owned Entities

| Entity | Description |
|--------|-------------|
| `Namespace` | Aggregate root — `id`, `slug`, `name`, `description`, `avatar`, `createdAt`, `updatedAt`. No status/lifecycle field. |
| `Service` | Aggregate root **within** the `namespace` bounded context (per its own Javadoc) — a deployable Spring Boot app owned by a namespace: `id`, `namespace`, `slug`, `name`, `description`. Other modules may reference it but don't own it. |
| `NamespaceApplication` | Persisted OAuth2 client — `id`, `namespace`, `type` (`NamespaceClientType`), `name`, `clientId`, `clientSecret` (nullable), `settings`, `scopes` (`OAuthScopes`), `expiresAt`. Created via the `NamespaceApplicationDefinition` command. |
| `NamespaceTrustedIssuer` | A trusted OIDC issuer for Workload Identity token exchange — `issuerUri`, `jwksUri` (nullable, falls back to OIDC discovery), `active`, `allowedAudiences`, `customClaims` (name → expected value assertions). Created via `NamespaceTrustedIssuerDefinition`. |
| `NamespaceFeatures` | Two `FeatureDefinition`s: `MEMBERS_COUNT` and `SERVICES_COUNT`, both `LimitedFeatureValue` (per-namespace limits). |

### NamespaceClientType: Three OAuth2 Client Shapes

Encoded as a single byte inside every `NamespaceClientId` (so the type is derivable from `client_id` alone,
no DB lookup needed):

| Type | Grant | Has `clientSecret`? | Used by |
|------|-------|---------------------|---------|
| `SERVICE_ACCOUNT` | Client Credentials (RFC 6749 §4.4) | Yes — HKDF-derived, Argon2id-hashed, shown once | Backend services/scripts that can't do an interactive/federated flow |
| `AGENT` | Authorization Code + PKCE (RFC 7636), loopback redirect | No — public client | AI agents/coding assistants (Claude Code, MCP tools) acting on behalf of a verified member |
| `WORKLOAD` | Token Exchange (RFC 8693) against a `NamespaceTrustedIssuer` | No — public client, the external OIDC token is the sole credential | CI/CD, cloud runtimes, Kubernetes pods, build tooling |

`NamespaceClientType.requiresSecret()` is the single source of truth — only `SERVICE_ACCOUNT` returns
`true`. (Note: `NamespaceApplication`'s own field Javadoc says `WORKLOAD` also carries a secret — that's
stale relative to `requiresSecret()`'s actual behavior and the class's own class-level Javadoc.)

### Service Catalog, Manifest & Dashboard Subsystems

Three subpackages under `namespace/` back functionality that isn't a simple CRUD entity:

- **`namespace/catalog`** — `ConfigurationCatalogService`/`ServiceCatalog` materialize a per-service
  configuration-property catalog into a Postgres table partitioned per-service
  (`service_configuration_catalog_{id}`). Rebuilt asynchronously by a database-backed debounced worker
  queue (`ServiceCatalogWorker`/`ServiceCatalogQueueListener`) reacting to
  `ArtifactoryEvent.PublicationCompleted` and `ServiceEvent.Released`.
- **`namespace/manifest`** — `ServiceManifests`/`DefaultServiceManifests` implement the resolve → upload
  artifacts → complete protocol behind the `POST /releases/{service}...` REST contract (documented below),
  built on the SDK's `ServiceRelease` (`PENDING → RELEASED/FAILED`, see the Artifactory SDK table above).
- **`namespace/dashboard`** — `Dashboards`/`DashboardSummary` back `GET /namespaces/{slug}/dashboard`, a
  read-only cross-module aggregation (services count, member count/limit, open change requests, active
  properties, owned artifacts) computed via direct jOOQ reads spanning `namespace`, `membership`, `vault`,
  and `artifactory` tables — a documented exception to event-only cross-module communication.

### NamespaceOwnerResolver

`namespace` supplies the `namespace` module's implementation of the Artifactory bounded context's
`OwnerResolver` SPI (see "Artifactory Domain" above) — `NamespaceOwnerResolver.resolve(EntityId|String)`
looks up a `Namespace` via `NamespaceManager` and projects it into an `Owner(id, slug)`.

### Membership: Invite → Invitation → Member

| Type | Description |
|------|-------------|
| `Invite` | Value object / command — `sender` (`EntityId`), `recipient` (email), `role` (`NamespaceRole`). Triggers creation of an `Invitation`. |
| `Invitation` | Entity, identified by a single-use `key` string (not an `EntityId`) — `organization` (a self-contained `Namespace` projection), `sender` (nullable — absent if the sender account was later deleted), `recipient`, `role`, `state` (`InvitationState`), `createdAt`, `expiryDate`. |
| `InvitationState` | `PENDING` → `ACCEPTED` \| `EXPIRED` \| `REVOKED`. **In practice only `PENDING`/`EXPIRED` are ever observed on a live row** — `DefaultInvitations` computes `EXPIRED` dynamically from `expiryDate` at read time, and `accept()`/`decline()`/`cancel()` all `DELETE FROM INVITATIONS` rather than transitioning state, so `ACCEPTED`/`REVOKED` never actually persist. |
| `Member` | Entity — `id`, `namespace`, `account`, `role` (`NamespaceRole`), `email`, `fullName` (nullable), `avatar`, `since`. Has real behavior methods: `displayName()` (falls back to email when no name is set), `isMemberOf(Namespace)`, `firstName()`/`lastName()`. |

**Invariant:** a namespace must always retain at least one `ADMIN` member. `DefaultMemberships` checks
`isLastRemainingAdministrator()` before demoting (`update`) or removing (`remove`) a member and throws
`UnsupportedMembershipOperationException` (400) if the operation would leave the namespace without one.

**`InvitationException`** carries 7 `ErrorCode`s: `INVITATION_NOT_FOUND` (404), `INVITATION_EXPIRED` (400),
`RECIPIENT_NOT_FOUND` (500 — indicates a bug, not a client error), `ALREADY_INVITED` (500),
`INSUFFICIENT_PERMISSIONS` (403), `NOT_ALLOWED` (400 — `MEMBERS_COUNT` feature disabled for the plan),
`MEMBER_LIMIT_REACHED` (400).

### Domain Events

`NamespaceEvent` is a sealed hierarchy with three flat events plus three nested sealed sub-hierarchies:

```
NamespaceEvent
├── Created, Renamed (from/to Slug), Deleted
├── MembershipEvent      (abstract, carries the affected account's EntityId)
│   ├── MemberAdded (+ role)       — NOT published for the initial admin created during Namespace.create()
│   ├── MemberUpdated (+ new role)
│   └── MemberRemoved
├── ApplicationEvent      (abstract, carries the NamespaceApplication)
│   ├── ApplicationCreated, ApplicationUpdated, ApplicationReset, ApplicationRemoved
└── TrustedIssuerEvent    (abstract, carries the NamespaceTrustedIssuer)
    ├── TrustedIssuerCreated, TrustedIssuerUpdated, TrustedIssuerRemoved
```

`ServiceEvent` is a separate sealed hierarchy (services are a distinct aggregate root, even though they
live in the same module): `Created`, `Renamed` (from/to `Slug`), `Released` (carries the artifactory
`Manifest`), `ReleaseFailed` (carries a list of error strings), `Deleted`.

`InvitationEvent` (in `com.konfigyr.membership`) is its own sealed hierarchy, keyed by the invitation's
`key` string rather than an `EntityId`: `Created`, `Accepted` (carries the recipient `Account`),
`Declined` (carries the recipient `Account`), `Canceled`. Published by `Invitations` and consumed by
`InvitationSender` for the async, `@Retryable`, `@TransactionalEventListener`-based invitation emails.

## API Contracts

### Namespace Endpoints

The namespace bounded context exposes far more than basic CRUD — ~30 endpoints across 7 controllers.

```
GET      /namespaces                                    @RequiresScope(READ_NAMESPACES)   search/list, scoped to caller (account or OAuth client)
HEAD     /namespaces/{slug}                              @RequiresScope(READ_NAMESPACES)
GET      /namespaces/{slug}                              isMember,  @RequiresScope(READ_NAMESPACES)
POST     /namespaces                                     @RequiresScope(WRITE_NAMESPACES)
PUT      /namespaces/{slug}                              isAdmin,   @RequiresScope(WRITE_NAMESPACES)
DELETE   /namespaces/{slug}                               isAdmin,   @RequiresScope(DELETE_NAMESPACES)
```

`ApplicationsController` — OAuth2 client applications, all `@RequiresScope(READ_NAMESPACES)` class-level,
mutations additionally require `isAdmin` + `WRITE_NAMESPACES`; reads only require `isAdmin`:

```
GET/POST /namespaces/{slug}/applications
GET      /namespaces/{slug}/applications/{id}
PUT      /namespaces/{slug}/applications/{id}
PUT      /namespaces/{slug}/applications/{id}/reset       regenerate client secret (SERVICE_ACCOUNT only)
DELETE   /namespaces/{slug}/applications/{id}
```

`TrustedIssuersController` — same auth shape as applications (`isAdmin` for reads, `isAdmin` +
`WRITE_NAMESPACES` for mutations):

```
GET/POST /namespaces/{slug}/trusted-issuers
GET      /namespaces/{slug}/trusted-issuers/{id}
PUT      /namespaces/{slug}/trusted-issuers/{id}
DELETE   /namespaces/{slug}/trusted-issuers/{id}
```

`DashboardController`:

```
GET      /namespaces/{slug}/dashboard                    isMember, @RequiresScope(READ_NAMESPACES)
```

`ServicesController` — class-level `@RequiresScope(READ_NAMESPACES)`; note mutations (`create`/`update`/
`delete`) do **not** require `WRITE_NAMESPACES`, only `isMember` (`isAdmin` for delete) — worth
double-checking if this is intentional when touching this controller:

```
GET/HEAD /namespaces/{namespace}/services[/{slug}]        isMember
POST     /namespaces/{namespace}/services                 isMember
PUT      /namespaces/{namespace}/services/{slug}           isMember
DELETE   /namespaces/{namespace}/services/{slug}            isAdmin
GET      /namespaces/{namespace}/services/{slug}/manifest        isMember
GET      /namespaces/{namespace}/services/{slug}/releases/{id}   isMember
GET      /namespaces/{namespace}/services/{slug}/catalog          isMember
GET      /namespaces/{namespace}/services/{slug}/catalog/search   isMember
```

### Membership / Invitations Endpoints

`MembersController` — class-level `@RequiresScope(INVITE_MEMBERS)` covers reads too, not just inviting:

```
GET      /namespaces/{slug}/members            isMember, @RequiresScope(INVITE_MEMBERS)
GET      /namespaces/{slug}/members/{member}   isMember, @RequiresScope(INVITE_MEMBERS)
PUT      /namespaces/{slug}/members/{member}   isAdmin,  @RequiresScope(INVITE_MEMBERS)
DELETE   /namespaces/{slug}/members/{member}   isAdmin,  @RequiresScope(INVITE_MEMBERS)
```

`InvitationsController` splits into an admin view (namespace-scoped) and a recipient view (account-scoped,
mirroring the frontend's `/namespace/$namespace/invitations` vs `/invitations` split):

```
GET/POST /namespaces/{slug}/invitations         isAdmin, @RequiresScope(INVITE_MEMBERS)
GET      /namespaces/{slug}/invitations/{key}   isAdmin, @RequiresScope(INVITE_MEMBERS)
DELETE   /namespaces/{slug}/invitations/{key}   isAdmin, @RequiresScope(INVITE_MEMBERS)   (cancel)

GET      /account/invitations         no @RequiresScope — session-authenticated USER_ACCOUNT principal only
GET      /account/invitations/{key}   no @RequiresScope
POST     /account/invitations/{key}   no @RequiresScope   (accept)
DELETE   /account/invitations/{key}   no @RequiresScope   (decline)
```

The `/account/invitations` endpoints deliberately carry no OAuth scope — they're for the invited human,
not a machine client, and `lookupAccount()` rejects any principal that isn't a `USER_ACCOUNT`.

### Vault Endpoints

There is no `/vaults` resource — profiles and change requests are scoped under a namespace's service.
Scopes are `READ_PROFILES`/`WRITE_PROFILES`, not `READ_VAULT`.

```
GET    /namespaces/{namespace}/services/{service}/profiles                          @RequiresScope(READ_PROFILES)
GET    /namespaces/{namespace}/services/{service}/profiles/{profileName}            @RequiresScope(READ_PROFILES)
POST   /namespaces/{namespace}/services/{service}/profiles/{profileName}/submit     @RequiresScope(WRITE_PROFILES)
POST   /namespaces/{namespace}/services/{service}/profiles/{profileName}/apply      (UNPROTECTED profiles only)
POST   /namespaces/{namespace}/services/{service}/changes/{number}/review           (approve/comment)
POST   /namespaces/{namespace}/services/{service}/changes/{number}/merge            (apply an approved change request)
```

`ProfilePolicy` has three values, not two: `UNPROTECTED` (direct `apply`), `PROTECTED` (requires
`submit` → `review`/approve → `merge`), and `IMMUTABLE` (no changes permitted at all). Note the workflow
direction: it's the direct **apply** step that's skipped for `PROTECTED` profiles — `merge` is what makes
an approved change request authoritative, and is always required to actually apply a submitted change.

### Audit Endpoints

There is no top-level `/audit` resource or single-record lookup — audit records are only listed nested
under a namespace, scoped `READ_NAMESPACES` (not a `READ_AUDIT` scope, which doesn't exist).

```
GET    /namespaces/{slug}/audit    @RequiresScope(READ_NAMESPACES), @PreAuthorize("isMember(#slug)")
```

### Artifactory / Publications Endpoints

```
GET/HEAD /artifacts/{groupId}/{artifactId}[/{version}]                              @RequiresScope(READ_ARTIFACTS)
POST     /artifacts/{groupId}/{artifactId}/{version}                                @RequiresScope(PUBLISH_ARTIFACTS)
PUT      /artifacts/{groupId}/{artifactId}/visibility                               @RequiresScope(PUBLISH_ARTIFACTS)

GET/HEAD /namespaces/{namespace}/artifacts[/{groupId}[/{artifactId}[/{version}]]]    isMember, @RequiresScope(READ_ARTIFACTS)
PUT      /namespaces/{namespace}/artifacts/{groupId}/{artifactId}/visibility        isAdmin, @RequiresScope(PUBLISH_ARTIFACTS)
DELETE   /namespaces/{namespace}/artifacts/{groupId}/{artifactId}                   isAdmin, @RequiresScope(PUBLISH_ARTIFACTS)  (deregister)
DELETE   /namespaces/{namespace}/artifacts/{groupId}/{artifactId}/{version}         isAdmin, @RequiresScope(PUBLISH_ARTIFACTS)  (retract)
```

### KMS Endpoints

```
GET      /namespaces/{namespace}/kms                              isMember, @RequiresScope(READ_NAMESPACES)
GET      /namespaces/{namespace}/kms/{id}                         isMember
POST     /namespaces/{namespace}/kms                              isAdmin,  @RequiresScope(WRITE_NAMESPACES)
PUT      /namespaces/{namespace}/kms/{id}/rotate                  isAdmin,  @RequiresScope(WRITE_NAMESPACES)
DELETE   /namespaces/{namespace}/kms/{id}                         isAdmin,  @RequiresScope(WRITE_NAMESPACES)
PUT      /namespaces/{namespace}/kms/{id}/keys/{key}/{deactivate|reactivate|compromised|restore}
                                                                   isAdmin,  @RequiresScope(WRITE_NAMESPACES)
POST     /namespaces/{namespace}/kms/{id}/{encrypt|decrypt|sign|verify}   isMember, @RequiresScope(WRITE_NAMESPACES)
```

### Service Manifest / Release Endpoints

Split by audience into two controllers, each with a different auth model — there is no shared
namespace-scoped `/releases` path used by both:

`ServiceManifestController` — plugin-only, namespace-free. Authenticated as a namespace OAuth2 client
(`NamespaceClientId`, `kfg-` prefixed `client_id`); the owning namespace is resolved from the `namespace`
JWT claim (`KonfigyrClaimNames.NAMESPACE`) via `NamespacedPrincipal`, never from a path variable. No
`isMember`/`isAdmin` check — namespace isolation comes from the trusted claim plus `Services.get(ns, slug)`
scoping the service lookup to that namespace.

```
POST   /releases/{service}                  @RequiresScope(PUBLISH_RELEASES)   resolve/open a release
GET    /releases/{service}/{id}             @RequiresScope(PUBLISH_RELEASES)
POST   /releases/{service}/{id}/artifacts   @RequiresScope(PUBLISH_RELEASES)   upload artifact metadata
POST   /releases/{service}/{id}/complete    @RequiresScope(PUBLISH_RELEASES)
```

`ServicesController` — UI-only, namespace-path-scoped, session-authenticated, unchanged `isMember` +
`READ_NAMESPACES` pattern used by the rest of this controller. Read-only: no resolve/upload/complete.

```
GET    /namespaces/{namespace}/services/{slug}/manifest        isMember, @RequiresScope(READ_NAMESPACES)
GET    /namespaces/{namespace}/services/{slug}/releases/{id}   isMember, @RequiresScope(READ_NAMESPACES)
```

Root is `/releases`, matching the `ServiceRelease` aggregate — deliberately not `/publish` (reserved for
the Artifactory bounded context: `PublicationsController`/`Publications`) and not nested under a generic
`/services` wrapper.

## Frontend Routes

File-based TanStack Start routing under `konfigyr-frontend/src/routes`. Most app routes sit under a
pathless `_authenticated` layout; `/auth/*`, `/api/$`, and `/error` do not.

```
/                                                          Dashboard
/account                                                   User account page
/invitations                                               Pending invitations across all namespaces
/join/$key                                                 Invitation acceptance
/namespace/provision                                       Namespace onboarding
/namespace/$namespace                                      Namespace detail
/namespace/$namespace/settings                             Namespace settings
/namespace/$namespace/members                              Member management
/namespace/$namespace/groups[/create|/$groupId[/edit]]     RBAC groups
/namespace/$namespace/invitations                          Pending invitations
/namespace/$namespace/applications[/create|/$id]           OAuth2 client applications
/namespace/$namespace/audit                                Audit log
/namespace/$namespace/kms[/create|/$keyset]                Keysets
/namespace/$namespace/services/$service                    Service detail/settings/create-profile
/namespace/$namespace/services/$service/manifest[/artifacts]   Service manifest / artifact metadata
/namespace/$namespace/services/$service/profiles/$profile[/history]   Vault profile access
/namespace/$namespace/services/$service/requests[/$number]      Change requests
/auth/code                                                 OAuth2 callback
/auth/scopes                                               OAuth scopes
```

## Data Flow Example: Creating a Namespace

```
Frontend
  1. User submits form: { slug, name, description }
  2. POST /namespaces (with @RequiresScope(WRITE_NAMESPACES))
  ↓
REST API - namespace module
  3. Validate input (slug unique, format valid)
  4. Create Namespace aggregate in database
  5. Insert the owner as an ADMIN Member directly into NAMESPACE_MEMBERS (a documented direct-table
     exception to the "events only" rule — no NamespaceEvent.MemberAdded is published for this initial
     admin, unlike members added later through an accepted Invitation)
  6. Publish NamespaceEvent.Created
  ↓
REST API - audit module
  7. @TransactionalEventListener catches event
  8. Insert audit record
  ↓
REST API - KMS module
  9. Listener creates KeysetMetadata for new namespace
  ↓
Frontend
  10. Receive 201 Created response
  11. Redirect to /namespace/$namespace
  12. Load namespace data (already cached server-side from loader)
```

## Environment-Specific Deployments

### Development
- Single database (PostgreSQL local or Docker)
- Single IdP (local Spring AuthServer)
- Frontend + API + IdP all local
- KEK typically provided as a local secret

### SaaS (Multi-tenant)
- Shared database (PostgreSQL managed service)
- Shared IdP (konfigyr-identity)
- KEK provided via `CryptoProperties.MasterKey`, typically delivered as a Kubernetes Secret
- CDN for static assets, load-balanced API instances

### On-Premise
- Isolated database per deployment
- On-premise IdP (konfigyr-identity)
- KEK provided via `CryptoProperties.MasterKey`, typically delivered as a Kubernetes Secret
- Behind customer firewall, per-deployment SSL certificates

## Namespace Access Control

Two `NamespaceRole` values, namespace-scoped, enforced via `@PreAuthorize("isMember(#namespace)")` /
`@PreAuthorize("isAdmin(#namespace)")` SpEL (backed by `KonfigyrMethodSecurityExpressionRoot`), combined
with `@RequiresScope` for OAuth-scope checks:

| Role | Permissions |
|------|-------------|
| `ADMIN` | Manage members, billing, services, all configurations |
| `USER` | Manage configurations and deployments only |

OAuth2 clients registered by a namespace (`NamespaceApplication`, created from a `NamespaceApplicationDefinition`
command) authenticate as that namespace and carry permission scopes from the real `OAuthScope` enum —
`namespaces:read/write/delete/invite/publish-releases`, `artifactory:read/publish`,
`profiles:read/write/delete`, `openid` — rather than a user role. See "Namespace & Membership Domain"
above for the `NamespaceClientType` (`SERVICE_ACCOUNT`/`AGENT`/`WORKLOAD`) that governs how each
application authenticates.

## Verification Checklist

- [ ] Understand namespace = tenant boundary
- [ ] Know which module owns each domain entity
- [ ] Understand cross-module event flow
- [ ] Aware of per-namespace encryption (KEK/DEK two-tier model)
- [ ] Know KMS key purposes and lifecycle states
- [ ] Know REST API contracts
- [ ] Understand frontend-to-API communication
- [ ] Aware of OAuth2 scope hierarchy
- [ ] Understand IdP as a protocol broker (not just a JWT issuer)
- [ ] Can trace data through all layers
