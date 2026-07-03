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
│ - Spring AuthServer       │    │ - Spring Boot 3.5+      │
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
authentication from upstream external providers (GitHub, GitLab, Google, SAML-based enterprise IdPs, or
customer-managed OIDC providers) and translates those identities into a single, standardized OIDC JWT
issued by Konfigyr. Application code never needs to handle provider-specific token formats.

Key responsibilities:
- Delegates authentication to external providers; never stores credentials itself
- Maps external user identities to local `Account` records
- Issues PS256/ES256 JWT access tokens and ID tokens
- Exposes standard OIDC endpoints: JWKS, discovery (`.well-known`), token introspection, and revocation

## Business Domains (Modules)

| Module | Responsibility | Key Entities |
|--------|---|---|
| **namespace** | Multi-tenancy | Namespace, Member, Role |
| **vault** | Config management | Profile, Entry, ChangeRequest, ChangeHistory |
| **artifactory** | Metadata registry | Artifact, ArtifactVersion, PropertyDescriptor, PropertyOccurrence, ServiceManifest |
| **kms** | Encryption | KeysetMetadata, KeyVersion |
| **audit** | Event logging | AuditEvent, AuditLog |
| **account** | User management | Account (from OAuth) |
| **feature** | Feature flags | FeatureFlag (per-namespace limits) |

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

### 3. Three-Tier Metadata Deduplication

```
Property Definition (identity: name + typeName → SHA-256)
    ↓
Property Occurrence (schema + description + deprecation → SHA-256)
    ↓
Version-to-Property Links
```

Identical schemas across thousands of artifact versions stored once.

### 4. Change Workflow

```
Create → Submit Change Request → Approve → Apply
         (optional if PROTECTED)
```

Profiles can be `UNPROTECTED` (direct apply) or `PROTECTED` (requires approval).

### 5. Namespace OAuth2 Clients

Namespaces can register OAuth2 clients (service tokens) with scoped permissions for CI/CD pipelines,
Gradle/Maven plugins, and other automated integrations. These clients authenticate against the Identity
Provider and receive access tokens scoped to specific operations (e.g. `metadata:upload`, `config:read`).
This is the primary integration mechanism for build-time metadata ingestion.

## Module Dependencies

```
Frontend
  ├── Identity Provider (OAuth2 login)
  └── REST API (all domain operations)

REST API
  ├── namespace → vault (config belongs to service in namespace)
  ├── namespace → audit (all changes logged)
  ├── vault → kms (encrypt vault data)
  ├── vault → artifactory (get property metadata)
  ├── artifactory → (read-only)
  ├── kms → (standalone, no outbound)
  └── audit → (listens to all events via @TransactionalEventListener)

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

- **SaaS deployment**: KEK lives in an external KMS (AWS KMS, GCP KMS)
- **On-premise deployment**: KEK provided as a Kubernetes Secret
- **DEKs** are stored in the database only in their encrypted (wrapped) form

### Key Components (konfigyr-crypto)

| Component | Role |
|-----------|------|
| `Keyset` | The collection of DEKs performing cryptographic operations; has one primary key for new operations, others for decryption/verification only |
| `KeysetStore` | Primary interface — creates, rotates, and drives lifecycle transitions |
| `KeysetFactory` | Bridges the API to an underlying crypto library (Tink, Nimbus JOSE) |
| `Algorithm` | Immutable value object declaring algorithm identity (e.g. `tink:AES256_GCM`, `jose:ES256`) |

### Key Purposes

Each `KeysetMetadata` is assigned exactly one `KeyPurpose` at creation. Purpose cannot be changed.

| Purpose | Operations | Algorithms |
|---------|-----------|------------|
| `ENCRYPT` | `encrypt(plaintext)` / `decrypt(ciphertext)` | AES-GCM (symmetric) |
| `KEY_ENCAPSULATION` | `wrap(key)` / `unwrap(ciphertext)` | RSA, EC, or ML-KEM (post-quantum) |
| `SIGN` | `sign(data)` / `verify(signature)` — exposes public key | ECDSA, Ed25519, ML-DSA |

### Key Lifecycle States

```
ENABLED → DISABLED → PENDING_DESTRUCTION → DESTROYED
    ↘ COMPROMISED → PENDING_DESTRUCTION → DESTROYED
```

- `ENABLED` — active; primary key is used for new crypto operations
- `DISABLED` — inactive; excluded from new operations, still readable for decrypt/verify
- `COMPROMISED` — emergency state; triggers immediate rotation
- `PENDING_DESTRUCTION` — grace period before material is wiped (scheduled tasks run hourly by default)
- `DESTROYED` — material deleted; record retained for audit

### Business Rules (invariants to never violate)

1. **Purpose consistency** — a `KeyVersion` spec must be compatible with the parent keyset's `KeyPurpose`
2. **Immutability of material** — once generated, key material can never be altered
3. **Single primary key** — a keyset should have exactly one primary version for crypto operations
4. **Soft-deletion** — `KeysetMetadata` cannot be destroyed while it has active `KeyVersion` records; versions must be scheduled for destruction first

## Artifactory Domain: SDK and Key Entities

The Artifactory module is backed by the **konfigyr-artifactory** SDK
(GitHub: https://github.com/konfigyr/konfigyr-artifactory, Maven: `com.konfigyr:konfigyr-artifactory`),
a lightweight Java library providing the shared abstractions used by the backend, Gradle/Maven plugins,
and third-party integrations alike.

### Key SDK Entities

| Entity | Description |
|--------|-------------|
| `Artifact` | Unique component identified by Maven coordinates (`groupId:artifactId`). Parent of all versions. |
| `ArtifactVersion` | Immutable snapshot of a specific release. Owns `PropertyOccurrence` links. |
| `PropertyDescriptor` | Configuration property metadata: name, type, description, default, JSON Schema. |
| `ArtifactMetadata` | Upload envelope — aggregates all `PropertyDescriptor`s for one artifact version. Uploaded by build plugins via REST. |
| `Publication` | Version change event with lifecycle: `PENDING → PUBLISHED → FAILED`. |
| `Manifest` | A Konfigyr service's current metadata state — used to detect property differences across environments. |
| `ServiceManifest` | Relational link between a namespace-owned service and one or more artifact versions. Enables metadata reuse (shared library configs) across services. |

### Metadata Ingestion Flow

```
Build (Gradle/Maven plugin)
  1. Extract spring-configuration-metadata.json from classpath
  2. Translate property types → JSON Schema
  3. POST /artifactory/artifacts/{group}/{artifact}/{version}
     (ArtifactMetadata payload, authenticated as namespace OAuth2 client)
  ↓
Artifactory module
  4. Compute Definition Checksum (name + typeName)
  5. Compute Occurrence Checksum (schema + description + deprecation)
  6. Deduplicate: reuse existing PropertyOccurrence if checksum matches
  7. Create Version-to-Property links
  8. Update Publication status: PENDING → PUBLISHED
```

## API Contracts

### Namespace Endpoints

```
GET    /namespaces/:slug           @RequiresScope(READ_NAMESPACES)
POST   /namespaces                 @RequiresScope(WRITE_NAMESPACES)
PATCH  /namespaces/:slug           @RequiresScope(WRITE_NAMESPACES)
DELETE /namespaces/:slug           @RequiresScope(DELETE_NAMESPACES)
```

### Vault Endpoints

```
GET    /namespaces/:ns/vaults      @RequiresScope(READ_VAULT)
GET    /namespaces/:ns/vaults/:id/profiles/:prof
POST   /namespaces/:ns/vaults/:id/entries
PATCH  /namespaces/:ns/vaults/:id/entries/:eid
```

### Audit Endpoints

```
GET    /audit                      @RequiresScope(READ_AUDIT)
GET    /audit/:id                  @RequiresScope(READ_AUDIT)
```

## Frontend Routes

```
/                            Dashboard
/namespace/:slug             Namespace detail
/namespace/:slug/members     Member management
/namespace/:slug/services/:svc/profiles/:prof   Vault access
/auth/code                   OAuth2 callback
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
  10. Redirect to /namespace/:slug
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
- KEK managed by external KMS (AWS KMS, GCP KMS)
- CDN for static assets, load-balanced API instances

### On-Premise
- Isolated database per deployment
- On-premise IdP (konfigyr-identity)
- KEK provided as a Kubernetes Secret
- Behind customer firewall, per-deployment SSL certificates

## Namespace Access Control

Two roles, namespace-scoped:

| Role | Permissions |
|------|-------------|
| `ADMIN` | Manage members, billing, services, all configurations |
| `USER` | Manage configurations and deployments only |

OAuth2 clients registered by a namespace authenticate as that namespace and carry permission scopes
(`metadata:upload`, `config:read`, etc.) rather than a user role.

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
