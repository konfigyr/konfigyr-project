# Product Marketing Context

**Document version:** v4
**Last updated:** 2026-09-09

## Product Overview
**One-liner:**
Konfigyr is a Spring Boot-native configuration management platform that eliminates config drift and gives teams auditable, access-controlled control over application configuration across environments.

**What it does:**
Konfigyr ingests the configuration metadata Spring Boot already generates at build time (`spring-configuration-metadata.json`, via a Gradle/Maven plugin) and uses it to power a type-safe UI for managing per-service configuration ("vaults") across profiles like dev/staging/prod. It validates property values against their real JSON Schema before they reach production, surfaces deprecated properties, tracks how configuration evolves version-to-version, and — unlike a generic secret store — enforces multi-tenant namespace boundaries, per-namespace encryption (via Google Tink-backed keysets), role-based access, optional change-request/approval workflows, and a full audit trail on every change.

**Product category:**
Spring Boot configuration management. Deliberately narrow: the category itself signals the core differentiator (opinionated on Spring Boot, not a generic secrets/config tool) and matches the anti-persona (non-Spring-Boot teams aren't a fit). Broader terms ("app configuration platform," "config-as-code") can still be used in top-of-funnel content, but the primary category claim is Spring Boot-specific.

**Product type:**
SaaS and on-premise (self-hosted) deployments of the same codebase — three services (`konfigyr-identity`, `konfigyr-api`, `konfigyr-frontend`) deployed together, either as a managed multi-tenant offering or behind a customer's own firewall.

**Business model:**
[gap — not yet defined. Confirmed with user (2026-09-08): no pricing exists yet. The `feature` module implements per-namespace limits (e.g. `MEMBERS_COUNT`, `SERVICES_COUNT`) which implies a tiered/seat- or usage-based plan structure is planned, but plan names, price points, and packaging are undecided. Revisit once pricing is set.]

## Target Audience
**Target companies:**
[Revised 2026-09-08 with proxy evidence from `customer-research` skill Mode 2 mining — see Customer Language section for full sourcing; still not first-party Konfigyr evidence] Engineering organizations running Spring Boot microservices at a scale where config drift and audit/compliance become real problems. The original "mid-size to large only" hypothesis has a genuine wrinkle worth testing in real conversations, but it is **not a confirmed segment expansion**: permission-management friction showed up in mined reviews from teams as small as 1–10 and 11–50 employees (Infisical), not just 10,001+-employee orgs (Vault) — but the small-org quotes are a **UX complaint** ("permission management is a bit confusing," "took a lot of time to clear out permission issues") about configuring access control, while the large-org quotes reflect a **buying-need** around audit/compliance at scale. Those are two different signals, not one broadening trend, and the sample (4 data points total) is below the skill's own 5-per-segment bar. Treat "permission/audit pain reaches smaller Spring Boot shops too, not just enterprise" as a hypothesis worth testing directly, not a basis for redesigning outreach targeting yet. What's on firmer footing is that the **on-prem/self-hosting requirement and formal compliance certification** (SOC 2/HIPAA/PCI-DSS) still looks concentrated at larger/regulated orgs specifically — every mention of those terms came from enterprise/compliance-framed sources. Confidence: Low-Medium — small sample, mixed with paraphrased secondary sources, not large-scale review data.

**Decision-makers:**
Platform/DevOps engineers and engineering managers as primary buyers/champions; backend developers as day-to-day users; security/compliance stakeholders involved given the audit log and KMS features. Confirmed with user (2026-09-08) — see Personas below.

**Primary use case:**
Managing Spring Boot application configuration safely across environments and teams — preventing config drift, catching invalid/deprecated property values before they reach production, and providing an auditable record of who changed what configuration and when.

**Jobs to be done:**
- Prevent bad configuration values from reaching production (type-safe validation against real Spring Boot metadata)
- Know exactly who changed what configuration, when, and why (audit trail, change history, optional approval workflow)
- Keep sensitive configuration (secrets, credentials) encrypted and access-controlled per team/namespace, without running a separate secrets tool
- Track how configuration properties evolve across artifact versions (deprecations, new properties, schema changes)

**Use cases:**
- Rolling out a new Spring Boot service version and verifying which configuration properties changed or are now deprecated
- Requiring peer review/approval before a configuration change reaches a `PROTECTED` production profile
- Auditing who accessed or modified a specific namespace's configuration for a compliance review
- Onboarding a new microservice's configuration into a central, namespace-owned registry via CI/CD (Gradle/Maven plugin publishing to the Artifactory)

## Personas
Confirmed with user (2026-09-08) — drafted from architecture/feature inference (RBAC roles, KMS/audit features, on-premise option), no dedicated customer/sales interviews conducted yet, but user validated these five as-is.

| Persona | Cares about | Challenge | Value we promise |
|---------|-------------|-----------|------------------|
| Platform/DevOps Engineer (likely champion) | Reducing config drift across environments, standardizing how services are configured, integrating config publishing into CI/CD | Manually reconciling `application.yml` across services and environments; no single source of truth for what config exists or who changed it | A type-safe, centrally auditable config registry that plugs into the existing Gradle/Maven build, no new secrets infrastructure to run |
| Engineering Manager (likely decision maker for team adoption) | Change safety, reduced production incidents from bad config, visibility into who changed what | Config-related production incidents traced back to manual, unreviewed edits; no audit trail for compliance asks | Approval workflows (`PROTECTED` profiles) and a full audit log tied to identity, reducing incident risk and answering compliance questions directly |
| Backend Developer (primary day-to-day user) | Not breaking prod with a typo or wrong type in a config value; understanding what a property does and whether it's deprecated | Config errors caught only at runtime, or not caught at all; no descriptions/types available at the point of editing | A UI that validates values against the real Spring Boot metadata (types, defaults, deprecation) before they're applied |
| Security/Compliance stakeholder (likely influencer, not confirmed) | Access control over sensitive properties, encryption, tenant isolation, auditability | Secrets and sensitive config spread across ad hoc tools/files with inconsistent access control | Per-namespace encrypted keysets (KMS), RBAC (ADMIN/USER), and a centralized audit log covering every domain event |
| Financial Buyer / Decision Maker for on-prem deployments | Total cost and control — able to self-host in a regulated environment | Vendor lock-in or inability to run a SaaS tool inside their own network | Same platform, same codebase, deployable on-premise behind their firewall |

## Problems & Pain Points
**Core problem:**
Teams running Spring Boot microservices at scale lose control over configuration: values drift between dev/staging/prod, changes are made by hand with no validation and no review, and there's no reliable record of who changed what, when, or why — especially for sensitive properties.

**Why alternatives fall short:**
- Generic secret stores (e.g. HashiCorp Vault) treat configuration as opaque key/value blobs — no awareness of Spring Boot's actual property types, schemas, defaults, or deprecations, so they can't validate a value before it's applied.
- Framework-agnostic config tools (e.g. Infisical) are similarly generic — they don't read build-time metadata, so they can't offer a type-safe UI, surface deprecated properties, or track how config evolves across artifact versions the way a Spring Boot-native tool can.
- Manual approaches (`application.yml` files, ad hoc environment variables, spreadsheets) have no validation, no audit trail, and no access control at all.

**What it costs them:**
[needs input: no quantified cost data (incident counts, time lost, dollar figures) exists in the codebase or background — do not fabricate metrics here. Qualitatively: production incidents from bad config values, time spent tracing "who changed this and why," and compliance/audit gaps.]

**Emotional tension:**
[needs input — hypothesis] Anxiety around deploying a config change to production without knowing if it's valid; frustration at not having a clear audit trail when something breaks or when a compliance review comes up; distrust of ad hoc secrets handling for sensitive values.

## Competitive Landscape
Researched 2026-09-08 via web/comparison-site search (see Sources below).

**Direct:** No direct, Spring Boot-native competitor known. No product found ingests Spring Boot build-time config metadata (`spring-configuration-metadata.json`) to power a type-safe, Spring-specific config UI. Spring Cloud Config Server is the closest "Spring-native" tool, but it's a generic externalized-config server (Git/file/Vault-backed) with no metadata ingestion, no type safety, no RBAC/approval workflow, and is largely unmaintained relative to newer tooling. Confidence: medium-high — a small or unlaunched competitor could exist below general search visibility.

**Secondary:**
- **HashiCorp Vault** — generic secret store (OSS free; HCP Vault Dedicated from ~$22/mo + $72.92/mo per client; Enterprise custom-quoted, reportedly low six figures). Solves secure storage and access control for secrets but has no concept of Spring Boot configuration metadata, property types, or schema-based validation. Falls short because it treats config as opaque data rather than typed, described properties tied to a specific artifact version.
- **Infisical** — framework-agnostic secrets/config management tool (free tier: 5 identities/3 projects; Pro $18/identity/month with RBAC, versioning, SSO; Enterprise custom with approval workflows, HSM). Same gap as Vault: no build-time metadata ingestion, no Spring Boot-specific type safety, deprecation surfacing, or version-to-version config evolution tracking.
- **Doppler** — polished SaaS secrets/config UI with strong developer-experience focus (environments, change hooks). Same core gap: generic key/value store, no Spring Boot metadata awareness, no per-namespace KMS-style tenant isolation, no approval-gated profiles.
- **Azure App Configuration** — feature-flag/config-value store for cloud apps (free tier: 10MB/1K req per day; Premium adds geo-replication, private link). Generic key-value pairs, no Spring metadata/type schema awareness, no artifact-version provenance, no approval-gated `PROTECTED` profile workflow — just RBAC via Entra ID.
- **AWS AppConfig** — config rollout/validation service with gradual deployment and rollback safety (pay-per-deployment, no free tier). Validation is generic (JSON schema/Lambda validators authored by the customer), not derived from real Spring Boot property metadata; no cross-service namespace/tenant model; AWS-locked.
- **AWS Secrets Manager** — credential/secret store with automatic rotation ($0.40/secret/month + API calls). Distinct enough from AppConfig to list separately (rotation vs. config rollout are different jobs). Opaque secret blobs, no config-schema or type validation, no deprecation/version tracking, AWS-only.
- **Azure Key Vault / Google Cloud Secret Manager** — collapsed into one entry; both are generic, IAM-gated, per-operation-billed cloud secret stores (Key Vault: $0.03–$3.2 per 10K ops; Secret Manager: $0.03 per 10K access + $0.06 per version/month) with no config-schema, Spring metadata, or approval-workflow concept. Too similar in positioning to profile separately.

Common gap across every cloud PaaS/secrets-store competitor above: none reads Spring Boot build-time metadata, none offers a type-safe property UI, none has per-namespace Tink-backed encryption or approval-gated profiles, and all are single-cloud-locked with no on-prem option matching Konfigyr's same-codebase SaaS+on-prem model.

**Indirect:** Manual configuration management (`application.yml` files edited directly, environment variables set by hand, no tooling at all) — the default approach many teams start with. Falls short because it has no validation, no audit trail, and no access control, and doesn't scale past a small number of services/environments.

**Sources:**
- https://infisical.com/blog/hashicorp-vault-pricing
- https://costbench.com/software/secrets-management/hashicorp-vault/
- https://envmanager.com/blog/infisical-pricing
- https://dev.to/beton/infisical-pricing-teardown-2026-1ang
- https://docs.spring.io/spring-cloud-config/docs/current/reference/html/
- https://infisical.com/blog/doppler-alternatives
- https://azure.microsoft.com/en-gb/pricing/details/app-configuration/
- https://zcp.zsoftly.ca/blog/aws-secrets-management-secrets-manager-parameter-store-appconfig/
- https://www.akeyless.io/blog/aws-secrets-manager-cost/
- https://cloud.google.com/secret-manager/pricing
- https://infisical.com/blog/azure-key-vault-pricing

## Differentiation
**Key differentiators:**
- Reads real Spring Boot build-time metadata (`spring-configuration-metadata.json`) rather than treating configuration as opaque key/value data
- Type-safe UI that validates values against each property's actual JSON Schema before they reach production
- Surfaces deprecated properties and tracks how configuration evolves across artifact versions (provenance/diff tracking via the Artifactory metadata registry)
- Per-namespace encryption (isolated keysets, so a key compromise in one tenant doesn't expose others) combined with RBAC and optional approval workflows (`PROTECTED` profiles: submit → review → merge) in one platform, rather than needing separate tools for secrets, RBAC, and change management
- Available as both managed SaaS and on-premise, same codebase
- Architecturally ecosystem-agnostic: the Artifactory registry is built on Konfigyr's own JSON Schema, not literally `spring-configuration-metadata.json` — Spring Boot is the flagship integration today, but the platform isn't architecturally locked to it. Confirmed with user (2026-09-08); no committed roadmap for other ecosystems yet, but worth surfacing as forward-looking positioning rather than a hard boundary.

**How we do it differently:**
By being opinionated about Spring Boot specifically instead of framework-agnostic: build plugins push property metadata directly from CI, so the platform always knows exactly what configuration a given artifact version expects, without any manual schema authoring.

**Why that's better (benefits):**
Fewer production incidents from invalid configuration, a real audit trail tied to identity and (optionally) peer review, and one platform for config + secrets + audit instead of stitching together a generic secret store, a homegrown audit log, and manual change tracking.

**Why customers choose us:**
[needs input — no confirmed win/loss data or customer testimony exists yet. The above is inferred from the feature set relative to Vault/Infisical; validate with actual customer conversations once available.]

## Objections & Anti-Personas
**Top objections:**
[needs input: no sales history exists yet to source real objections. Plausible objections worth validating — expanded 2026-09-08 with the competitive research findings:]
| Objection | Response |
|-----------|----------|
| "We already run HashiCorp Vault for secrets" | Confirmed with user (2026-09-08): depends on how Vault is used. If it's used purely for secrets storage, Konfigyr replaces it completely — plus brings config metadata, validation, auditing, versioning, delivery, and Spring ecosystem know-how that Vault has no concept of. (If Vault is also doing things outside secrets storage — e.g. dynamic database credentials, PKI — that's outside Konfigyr's scope and would need its own answer.) |
| "We're not a Spring Boot shop" | Spring Boot is the flagship (and currently only) integration, not an architectural ceiling. Confirmed with user (2026-09-08): the platform is built on Konfigyr's own Artifactory JSON Schema registry, not literally `spring-configuration-metadata.json` — the underlying architecture is language/framework-agnostic, Spring Boot is just the first ecosystem with a build-time metadata ingestion path shipped. No committed roadmap for other ecosystems yet ("where this leads is not clear"), but it's architecturally open, not a dead end. |
| "Pricing/on-prem cost" | [needs input — no pricing exists yet] |
| "We already use a cloud PaaS config/secrets service (AWS AppConfig, Azure App Configuration, AWS/Azure/GCP secret stores)" | [needs input — draft response, not sales-validated: these solve rollout mechanics or secret rotation well, but only within one cloud, with no Spring Boot property awareness — no type validation, no deprecation surfacing, no schema tied to a specific artifact version. Konfigyr layers Spring-aware validation and cross-cloud/on-prem portability on top, rather than adding another single-cloud silo. **Still flagged unvalidated after the 2026-09-08 mining pass** — this pass focused on Vault/Infisical/Doppler per scope; no AWS/Azure PaaS reviews or forum threads were mined, so no supporting or contradicting evidence was found either way.] |
| "We already use Doppler for config/secrets" | Sharpened 2026-09-08 with proxy evidence, verified directly against Doppler's own docs and announcements rather than secondary sources (a specific factual claim about a competitor is a real risk to get wrong in sales/marketing copy). Doppler's strength is broad, generic developer experience across any language; Konfigyr's differentiation is depth on Spring Boot specifically — real property types, deprecation warnings, and version-to-version schema evolution that a generic tool has no way to know about. Three concrete, verified points sharpen this further: (1) Doppler's audit log is tiered by plan, not free-tier-complete — the free plan includes only 3 days of activity-log retention, with 90-day audit logs requiring the $21/user/month Team plan — vs. Konfigyr's full audit trail as a non-tiered, core platform feature; (2) Doppler shipped an Enterprise-only, custom-priced on-prem deployment on 2026-06-08, a brand-new and narrower initial release ("core secrets management workflows" only, gated to its top plan) vs. Konfigyr's same-codebase SaaS+on-prem model available without an enterprise-tier gate; (3) per Doppler's own docs (docs.doppler.com/docs/change-requests), its Change Requests cover only additions/modifications/deletions of secret *values* — no access-grant workflow, single-approval only (no multi-step sequential chain), no time-limited/temporary access grants — genuinely narrower than Konfigyr's `PROTECTED` profile submit→review→merge model. Confidence: Medium-High on these three facts (primary-sourced against Doppler's own docs/pricing/announcements); Konfigyr's differentiation framing on top of them is still directional, not sales-validated. |

**Anti-persona:**
Confirmed with user (2026-09-08). Teams not running Spring Boot (the metadata ingestion and type-safe UI depend on `spring-configuration-metadata.json`, so there's no value proposition for non-Spring stacks today). Also a poor fit for very small teams/single-service shops where the overhead of namespaces, RBAC, and approval workflows outweighs the benefit.

## Switching Dynamics
Reviewed and confirmed/adjusted with user (2026-09-08). Originally drafted from feature-set/problem-statement inference, not customer interviews — still worth validating against real conversations once available, but no longer flagged as unconfirmed hypotheses.

**Push:** Recurring production incidents traced to invalid or drifted configuration, with no validation to catch it beforehand; no version history or point-in-time recovery, so a bad change is hard to diagnose or roll back; failed or painful compliance/audit reviews with no reliable record of who changed what, when, or why; unreviewed changes reaching production because no approval step exists between "someone edited a file" and "it's live"; and the growing pain of managing secrets and config by hand across an increasing number of microservices and environments.

**Pull:** A platform that validates configuration against real Spring Boot property schemas before it reaches production; keeps full version history with point-in-time recovery to any prior configuration state; maintains a complete audit trail tied to identity for every change; and supports approval workflows (submit → review → merge) for protected profiles — understanding their Spring Boot schema out of the box, with no manual mapping required. Re-sorted by user (2026-09-08): these are standalone reasons to switch, paired against the corresponding Push pains above, not one lumped-together Pull statement.

**Habit:** Existing investment in `application.yml`-based workflows, or an already-running generic secret store (Vault) that "mostly works," even if it doesn't validate config or track deprecations.

**Anxiety:** Migrating existing secrets/config into a new system; trusting a new platform with sensitive production values and encryption keys; whether an on-premise deployment adds operational burden; and a legacy codebase littered with deprecated or unused properties that no one dares to remove, surfaced once real metadata-driven visibility exists. Expanded by user (2026-09-08).

## Customer Language
Mined 2026-09-08 via the `customer-research` skill, Mode 2 (Digital Watering Hole Research). **Konfigyr still has no customers** — everything below is proxy evidence: real, verbatim language from public reviews/discussions of its competitors (HashiCorp Vault, Infisical, Doppler), not first-party Konfigyr customer evidence. Replace with real customer language once design partners exist.

**Access limitations (read before trusting confidence labels below):** G2, Capterra, TrustRadius, and Gartner Peer Insights all blocked direct scraping (HTTP 403) during this pass. Reddit — the skill's own top-recommended source for this developer/DevOps ICP — was completely unreachable across every approach tried, including targeted attempts at r/springboot and Stack Overflow: WebFetch refused every reddit.com URL, and WebSearch surfaced zero genuine Reddit threads across roughly twenty query variations, turning up only vendor blogs and official Spring documentation instead. As a result, real attributed verbatim quotes below come only from **PeerSpot** (gives reviewer role/company size) and **Hacker News** (via its Algolia search API; several threads are 2020–2023, outside the skill's 12-month recency window, marked accordingly). Content sourced from G2/Capterra is WebSearch's own paraphrase of blocked pages, not text pulled directly from the reviews — marked "paraphrase" and weighted lower. **No Spring Boot/Java-specific forum language was found anywhere** — the single biggest open gap in this research pass, not a disproof of the core differentiator. One vendor blog (oneuptime.com) used language close to Konfigyr's own pitch ("configuration drift due to multiple properties files," "managing settings...becomes a significant operational challenge at 50+ microservices") but it's a company's own marketing/technical content, not a customer voicing frustration, so it is **not** included as evidence below. Net: this pass validates general competitive pain (Vault complexity, Doppler trust concerns, permission-management friction) but **not** the Spring-Boot-specific angle — don't write hero copy assuming Spring Boot engineers specifically are saying these things until Reddit access works or real interviews (Mode 3) happen.

**How they describe the problem:**

*[LOW-MEDIUM CONFIDENCE, see Target companies below] Permission-management friction appears at both very small and very large orgs, but the quotes describe two different things* — 4 verbatim points across Vault (10,001+-employee orgs) and Infisical (1–10 and 11–50-employee teams). The terms practitioners actually use are "policies," "ACLs," "permission management," and "access" — none of these quotes contain the word "RBAC," which is Konfigyr's/vendors' internal shorthand, not customer language:
- "Managing policies at scale can also be challenging as the number of teams and applications grows." — Senior Platform Engineer, 10,001+-employee org, PeerSpot, https://www.peerspot.com/products/hashicorp-vault-reviews (undated, checked 2026-09-08)
- "The ACLs within HashiCorp Vault, such as policies and AppRole authentication, were not intuitive at first." — Senior DevOps Engineer, Tech Services, 10,001+ employees, PeerSpot (same URL)
- "Permission management is a bit confusing in Infisical, and it took us a lot of time to clear out the permission issues." — Shivdutt Bhadakwad, Full Stack Engineer, educational institution, PeerSpot, https://www.peerspot.com/products/infisical-reviews
- "There is a limitation on how many people can be given access for a particular organization or project." — Product Engineer, tech vendor (11–50 employees), PeerSpot (same URL)

*[MEDIUM CONFIDENCE] Vault's operational/setup complexity is a recurring, durable complaint:*
- "Setting up HashiCorp Vault can present operational challenges because deploying it involves complex initial setup." — Senior Platform Engineer, PeerSpot
- "I tried to use Vault and it just was a nightmare to get started." — arsalanb, Hacker News, 2020-10, https://news.ycombinator.com/item?id=24719722 (>12mo old)
- "Vault is definitely much more of a beast, but it also does a lot more." — quaffapint, Hacker News, 2020-10 (same thread)

*[MEDIUM CONFIDENCE, dated — clears the skill's 5-source bar but all from one 2020 thread] Reflexive distrust of SaaS-only secret storage:*
- "I can't imagine mentioning in our security policy/audit that we store secrets with a third party." — nyrulez, HN, 2020-10, https://news.ycombinator.com/item?id=24719722
- "You have to give Doppler your secrets which is absolutely crazy." — neximo64, HN, 2020-10 (same thread)
- "I don't want to rely on an external vendor being up to access/manage my secrets." — kfrzcode, HN, 2020-10 (same thread)
- "Storing secrets in the cloud unencrypted is kinda crazy these days." — SkyMarshal, HN, 2020-10 (same thread)
- "it seems like you're being encouraged to send your secrets verbatim over the Internet" — NovemberWhiskey, HN, 2020-10, https://news.ycombinator.com/item?id=24724496

*[LOW CONFIDENCE, but recent (within the 12-month window) and closest signal found to Konfigyr's core pitch]* Config/secret drift and audit gaps in decentralized approaches:
- "How do you handle sharing of common secrets? How do you make sure that api key that everyone uses in dev is actually rotated?" — sofixa, a HashiCorp employee, HN, 2025-07, https://news.ycombinator.com/item?id=44636691
- "You can't revoke, rotate, or audit access to them." (re: secrets encrypted in git) — JeffMcCune, HN, 2025-07 (same thread)

*[LOW CONFIDENCE, single point] Vendor lock-in as a switching consideration:*
- "[Infisical] creates vendor lock-in without flexible app-side handling" — dvtkrlbs, a current Infisical user, HN, 2025-07 (same thread as above)

**How they describe us:**
- [gap — Konfigyr has no customers or public presence yet; cannot be sourced until design partners exist]

**Words to use:**
Unchanged from v2, none contradicted by this pass, but none confirmed in Spring-specific terms either (see the Spring Boot gap above): Config drift, type-safe, audit trail, namespace, provenance, artifact version, deprecation. Adding terms validated by real mined language across competitors: **permission management**, **policies**, **access control** — the terms practitioners actually reach for in the quotes above (notably not "RBAC," which is Konfigyr's/vendors' internal shorthand, not confirmed customer language). Also adding **vendor lock-in** (the term used when justifying or resisting a switch — useful in objection-handling copy), which appears verbatim in the mined dvtkrlbs quote above.

**Words to avoid:**
Unchanged from v2 — still a hypothesis, not customer-validated either way by this pass: likely avoid generic "secrets manager" framing as the primary identity, since that undersells the Spring Boot-specific, type-safe differentiation.

**Sources (this pass):**
- https://www.peerspot.com/products/hashicorp-vault-reviews
- https://www.peerspot.com/products/infisical-reviews
- https://www.peerspot.com/products/doppler-reviews (no reviews collected on this platform as of 2026-09-08)
- https://news.ycombinator.com/item?id=24719722 and https://news.ycombinator.com/item?id=24724496 (Doppler launch thread, 2020-10)
- https://news.ycombinator.com/item?id=37090754 (Infisical vs. Vault, 2023-08)
- https://news.ycombinator.com/item?id=44636691 (SecretSpec, 2025-07)
- G2 product review pages for Vault/Infisical/Doppler (blocked, HTTP 403 — content cited elsewhere in this doc from these is WebSearch paraphrase only, not verbatim)
- https://docs.doppler.com/docs/change-requests (Doppler's own docs, fetched directly — primary source for the Change Requests scoping claim in Objections)
- https://www.prnewswire.com/news-releases/doppler-brings-secrets-management-on-prem-for-full-control-302794005.html (Doppler's own announcement, 2026-06-08 — primary source confirming Doppler's Enterprise-only on-prem option)
- https://www.doppler.com/pricing (Doppler's own pricing page, cross-checked against third-party pricing trackers — primary source for audit-log retention by plan)

**Glossary:**
| Term | Meaning |
|------|---------|
| Namespace | Top-level tenant container; owns services, members (ADMIN/USER roles), and configuration |
| Vault | Per-service configuration: profiles (dev/staging/prod), change sets, change requests, change history |
| Artifactory | Metadata registry indexing artifact versions and their configuration property descriptors (as JSON Schema) |
| Profile | A named configuration environment for a service (e.g. dev/staging/prod), with a policy: `UNPROTECTED` (direct apply), `PROTECTED` (submit → review → merge), or `IMMUTABLE` (no changes permitted) |
| KMS | Per-namespace cryptographic keyset management (backed by Google Tink) used to encrypt vault contents |
| Change Request | A proposed configuration change requiring review/approval before being merged into a `PROTECTED` profile |

## Brand Voice
Confirmed with user (2026-09-08). Inferred from the tone of the README/CLAUDE.md documentation (direct, technical, no marketing fluff, precise about what the product is and isn't), not from an established brand guide — but validated as accurate.

**Tone:** Technical, direct, precise. Comfortable stating what Konfigyr is *not* (not a generic secret store, not framework-agnostic) as part of positioning.

**Style:** Engineer-to-engineer. Leads with concrete mechanics (build-time metadata, JSON Schema validation, per-namespace encryption) rather than abstract benefit language.

**Personality:** Opinionated, precise, trustworthy, pragmatic, unglamorous-by-design.

### Recorded messaging alternatives (artifact, not an input)

Relocated 2026-09-09 from `marketing/homepage-copy.md` when that staging folder was retired. **These are drafted outputs, not positioning inputs** — a skill reading this document for context should treat the sections above as the source of truth and these as a record of options already considered. Kept because tested messaging has cross-channel value: future campaigns, release notes, and emails can reuse or A/B test these rather than re-deriving them.

The live homepage uses **Option A** for the hero and **"Request Early Access"** for the CTA. Where "used" is noted below, it refers to that homepage as of 2026-09-09.

**Hero headline + subheadline options:**

- **Option A (in use):** "Configuration management for Spring Boot." / "Not a generic secrets store with a UI on top. Konfigyr reads your build's own property metadata and validates every value against it before it reaches production." — Matches the `{category}` for `{audience}` formula, leading with the deliberate category claim. Safest fit for brand voice; costs a beat of translation for a reader who hasn't yet framed their problem in category terms.
- **Option B:** "Stop shipping config nobody's checked against the schema." / "Konfigyr reads the property metadata your Spring Boot build already generates and validates every value against it — before a bad type or an invalid setting reaches production, not after a deploy fails." — Leads with felt pain rather than category, so it lands before a reader has framed the need in category terms. Costs the immediate anti-persona self-filtering that a category-first headline gives.
- **Option C:** "Spring Boot configuration, validated before it ships." / "Not a generic secrets store with a UI on top. Konfigyr reads your build's own property metadata — types, defaults, deprecations — and checks every value against it before production ever sees it." — Puts the payoff in the headline itself, so a skimmer who reads only the headline still gets the promise. Trade: less pure category-naming than A, and some mechanism repetition across the two lines.
- **Option D:** "Still shipping config nobody's checked against the schema?" / "Konfigyr reads the property metadata your Spring Boot build already generates and validates every value before it reaches production — no generic secrets store bolted onto a UI." — Rhetorical question, hits the discomfort beat hardest. Riskiest tonally: reads closer to a B2C tactic against this brand's technical, direct voice. Worth A/B testing rather than defaulting to.

**CTA options:**

- **"Request Early Access" (in use)** — Direct, matches this document's chosen conversion action verbatim.
- **"Apply for Early Access"** — Stronger framing for the design-partner stage: "apply" signals a selective process, which is true rather than implied. Best on the dedicated `/early-access` page, where a short qualifying form makes "apply" feel accurate rather than gatekeep-y.
- **"Get Early Access"** — Closest to a standard SaaS CTA shape while still avoiding "Sign Up"/"Start Trial." Less precise about the selectivity of the current stage — best for lower-commitment placements such as a footer link.

## Proof Points
[gap — confirmed with user (2026-09-08): nothing exists yet. No metrics, named customers, or testimonials exist anywhere in the codebase or background provided. The product does not yet have a public marketing site or announced customers (`konfigyr-frontend/apps/console` contains only the authenticated product application, no landing/marketing content). Do not fabricate any of the following until real data exists.]

**Metrics:** [gap]
**Customers:** [gap]
**Testimonials:** [gap]
**Value themes:**
| Theme | Proof |
|-------|-------|
| Type-safe configuration | [gap — e.g. before/after incident data] |
| Full audit trail | [gap] |
| Per-namespace encryption/isolation | [gap] |

## Goals
**Primary business goal:**
Confirmed with user (2026-09-08): pre-launch / design-partner acquisition stage. No payment provider (e.g. Stripe) is integrated yet — user intends to add one in the near future — and no pricing is defined (see Business model), so the near-term goal is landing a small number of design partners in the Spring Boot ecosystem to validate the product and generate the real customer language and proof points that are currently gaps in this document. Paid SaaS launch and billing infrastructure follow once that validates.

**Key conversion action:**
Request early access. Chosen specifically because it requires no payment collection, matching the current no-billing-yet stage — not a self-serve trial signup or demo booking.

**Current metrics:**
[gap — no current traffic, signup, or usage metrics exist; product does not yet have a public marketing presence.]

## Changelog
*Newest first. One line per revision: what changed and why.*
- v4 (2026-09-09) — Added "Recorded messaging alternatives" under Brand Voice, relocating the four hero headline/subheadline options and three CTA options from `marketing/homepage-copy.md` as that staging folder was retired (its copy now lives in the built `konfigyr-frontend/apps/website` app). Explicitly labelled as drafted artifacts rather than positioning inputs, so skills reading this document for context don't mistake prior output for source-of-truth strategy. No positioning, audience, or competitive content changed.
- v3 (2026-09-08) — Ran `customer-research` skill Mode 2 (digital watering hole mining) against HashiCorp Vault, Infisical, and Doppler as proxy evidence (Konfigyr has no customers yet), with every competitor-specific factual claim cross-checked against primary sources before being written into Objections. G2/Capterra/TrustRadius/Gartner Peer Insights all blocked scraping and Reddit was fully unreachable — including targeted attempts at r/springboot and Stack Overflow (the skill's top-recommended source for this ICP, flagged as an open gap, not a disproof) — so real verbatim quotes came only from PeerSpot and Hacker News, with G2 content otherwise limited to search-tool paraphrase. Filled in Customer Language with sourced, confidence-labeled quotes (permission-management friction across company sizes, Vault setup-complexity complaints, dated-but-broad distrust of SaaS-only secret storage, recent HashiCorp-employee commentary on config/secret drift); added validated vocabulary terms — "permission management," "policies," "access control," "vendor lock-in" — drawn from the actual quoted language (not "RBAC," which never appears verbatim in any mined quote). Revised Target companies: permission-friction pain surfaced at teams as small as 1–10 employees as well as 10,001+-employee orgs, but flagged this as two distinct signals (a UX complaint at small orgs vs. a compliance buying-need at large ones) rather than a confirmed broadening of the ICP — the on-prem/compliance-certification pitch specifically stays narrowed to larger/regulated orgs. Sharpened the Doppler objection response with three primary-sourced facts verified directly against Doppler's own docs, pricing page, and its 2026-06-08 on-prem announcement: a plan-tiered (not fully paywalled) audit log, a newly-launched Enterprise-only on-prem option, and a Change Requests feature scoped to secret-value edits only, with no access-grant or time-limited-grant workflow. Left the cloud-PaaS objection flagged unvalidated since it was out of this pass's competitor scope. No Spring Boot/Java-specific pain language was found anywhere despite two rounds of targeted searching — flagged explicitly as the biggest remaining gap given the Reddit access failure, not evidence the differentiator doesn't hold.
- v2 (2026-09-08) — Resolved all v1 `[needs input]` items with the user: locked category to "Spring Boot configuration management"; confirmed 5 personas and anti-persona as-is; ran competitive research (no direct competitor found; added Doppler and cloud PaaS players — Azure App Configuration, AWS AppConfig, AWS Secrets Manager, Azure Key Vault/GCP Secret Manager — as secondary competitors with pricing); clarified Vault objection (full replacement when Vault is secrets-only) and Spring Boot-only positioning (architecturally ecosystem-agnostic via Konfigyr's own Artifactory JSON Schema, no committed non-Spring roadmap); added two draft objections from competitive research; re-sorted Switching Dynamics Push/Pull as paired reasons and expanded Anxiety (legacy deprecated-property visibility); confirmed brand voice adjectives as-is; set goals to pre-launch/design-partner stage with "request early access" as the conversion action (no payment provider integrated yet). Flagged as remaining gaps (no fabricated data): pricing/business model, customer language, and proof points/metrics/testimonials.
- v1 (2026-09-08) — Initial context, auto-drafted from README, CLAUDE.md, and the `project-overview` skill; no marketing copy existed yet to draw from (console app is authenticated-product-only, no landing page).
