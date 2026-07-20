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
| **namespace** | Multi-tenancy | Namespace, NamespaceRole (enum: ADMIN/USER), NamespaceApplicationDefinition (OAuth2 clients) |
| **membership** | Namespace membership | Member, Invitation |
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
  ├── namespace → artifactory, feature
  ├── vault → namespace, crypto (konfigyr-core library, to encrypt vault data)
  ├── kms → namespace (NamespaceManager)
  ├── artifactory → (no outbound dependency on vault or kms)
  └── audit → (listens to all events via @TransactionalEventListener: account, namespace, service,
      invitation, vault, kms, artifactory)

Identity Provider
  └── Account provisioning (standalone)
```

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

## API Contracts

### Namespace Endpoints

```
GET    /namespaces/{slug}          @RequiresScope(READ_NAMESPACES)
POST   /namespaces                 @RequiresScope(WRITE_NAMESPACES)
PUT    /namespaces/{slug}          @RequiresScope(WRITE_NAMESPACES)
DELETE /namespaces/{slug}          @RequiresScope(DELETE_NAMESPACES)
```

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

## Frontend Routes

File-based TanStack Start routing under `konfigyr-frontend/src/routes`. Most app routes sit under a
pathless `_authenticated` layout; `/auth/*`, `/api/$`, and `/error` do not.

```
/                                                          Dashboard
/account                                                   User account page
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
  5. Publish NamespaceEvent.Created
  ↓
REST API - audit module
  6. @TransactionalEventListener catches event
  7. Insert audit record
  ↓
REST API - KMS module
  8. Listener creates KeysetMetadata for new namespace
  ↓
Frontend
  9. Receive 201 Created response
  10. Redirect to /namespace/$namespace
  11. Load namespace data (already cached server-side from loader)
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

OAuth2 clients registered by a namespace (`NamespaceApplicationDefinition`) authenticate as that namespace
and carry permission scopes from the real `OAuthScope` enum — `namespaces:read/write/delete/invite/
publish-releases`, `artifactory:read/publish`, `profiles:read/write/delete`, `openid` — rather than a
user role.

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
