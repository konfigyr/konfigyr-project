---
name: spring-security-oauth2
description: Using @RequiresScope annotation for OAuth2 scope validation, understanding scope hierarchy, protecting endpoints, testing authentication. Use when protecting REST endpoints, enforcing access control, or handling OAuth2 tokens.
---

# Spring Security & OAuth2 Scopes

## Overview

Konfigyr uses OAuth2 JWT tokens issued by the Identity Provider (`konfigyr-identity`). All REST API endpoints are protected by JWT scopes enforced via the custom `@RequiresScope` annotation.

The Identity Provider is an **identity broker**: it accepts authentication from external providers (GitHub, GitLab, Google, SAML enterprise IdPs) and translates them into a single standardised Konfigyr JWT. The REST API only ever validates against konfigyr-identity — no external JWKS endpoints or multi-issuer configuration is needed.

**User token flow (frontend):**
1. Frontend authenticates via Authorization Code + PKCE with konfigyr-identity
2. konfigyr-identity issues PS256 JWT with `scope` claim and Konfigyr-internal `sub`
3. Frontend includes token in `Authorization: Bearer <token>` header
4. REST API validates token signature against konfigyr-identity's JWKS endpoint
5. REST API checks `@RequiresScope` annotation matches token scopes
6. If scope missing → 403 Forbidden

**Machine token flow (namespace OAuth2 clients):**
1. CI/CD pipeline or build plugin authenticates via Client Credentials with konfigyr-identity
2. konfigyr-identity issues PS256 JWT with `scope` claim and `namespace` claim
3. Plugin/pipeline includes token in `Authorization: Bearer <token>` header
4. Same scope enforcement applies

### Token types and their claims

There are two distinct token personas. Code that reads JWT claims must account for both:

| Claim | User token | Namespace client token |
|-------|-----------|----------------------|
| `sub` | Konfigyr Account ID (internal, **not** the external provider's UID) | OAuth2 client ID (starts with `kfg-`) |
| `email` | User's email address | not present |
| `scope` | User's granted scopes | Client's granted scopes (e.g. `namespaces:publish-releases`) |
| `namespace` (`KonfigyrClaimNames.NAMESPACE`) | not present | Namespace `EntityId` (serialized) — identifies which namespace the client belongs to |

> `sub` is always a Konfigyr-internal identifier. It will never be a GitHub username or external UID — the broker normalises all external identities before issuing the token.
>
> The `namespace` claim name is defined by the `KonfigyrClaimNames.NAMESPACE` constant (`konfigyr-core`) — always reference the constant, never the raw string literal.
>
> The claim is added whenever the *registered client's* `ClientSettings` carries a `NamespaceClientSettingNames.NAMESPACE` setting (`TokenCustomizer.customize(...)`, `konfigyr-identity`) — independent of grant or authentication type, and independent of `NamespaceClientType`. It's still absent from every user token issued today only because no currently provisioned registered client that can authorize a Konfigyr account is namespace-owned: the frontend's own client isn't one, and none of `SERVICE_ACCOUNT`, `AGENT`, or `WORKLOAD` currently has a client provisioned that authenticates a real member. If that ever changes for any of these types, the resulting token would carry the `namespace` claim despite being a user token by grant shape.

---

## @RequiresScope Annotation

Use `@RequiresScope` on controller methods to enforce OAuth2 scope requirements.

### Basic Usage

```java
@RestController
@RequestMapping("/namespaces")
@RequiredArgsConstructor
class NamespaceController {

    private final NamespaceManager namespaces;

    @GetMapping("/{slug}")
    @RequiresScope(OAuthScope.READ_NAMESPACES)
    EntityModel<Namespace> get(@PathVariable String slug) {
        return namespaces.findBySlug(slug)
                .map(Assemblers.namespace()::toModel)
                .orElseThrow(() -> new NamespaceNotFoundException(slug));
    }

    @PostMapping
    @RequiresScope(OAuthScope.WRITE_NAMESPACES)
    ResponseEntity<EntityModel<Namespace>> create(@Valid @RequestBody NamespaceDefinition definition) {
        Namespace namespace = namespaces.create(definition);
        return ResponseEntity.created(URI.create("/namespaces/" + namespace.slug()))
                .body(Assemblers.namespace().toModel(namespace));
    }

    @DeleteMapping("/{slug}")
    @RequiresScope(OAuthScope.DELETE_NAMESPACES)
    ResponseEntity<Void> delete(@PathVariable String slug) {
        namespaces.delete(slug);
        return ResponseEntity.noContent().build();
    }
}
```

### Available Scopes

`OAuthScope` is an enum in `konfigyr-core`. Always use the enum constant, never raw strings:

```java
// ✓ Correct: Use enum
@RequiresScope(OAuthScope.READ_NAMESPACES)

// ✗ Wrong: Don't use raw strings
@RequiresScope("read:namespaces")
```

**Scopes** (`OAuthScope` enum, `konfigyr-core`):
- `OPENID` — OIDC identity verification
- `READ_NAMESPACES` — Read namespace metadata
- `WRITE_NAMESPACES` — Create/update namespaces (implies `READ_NAMESPACES`)
- `DELETE_NAMESPACES` — Delete namespaces (implies `WRITE_NAMESPACES`)
- `INVITE_MEMBERS` — Create and manage invitations (implies `READ_NAMESPACES`)
- `PUBLISH_RELEASES` — Resolve, upload to, and complete service releases (implies `READ_NAMESPACES`)
- `NAMESPACES` — Aggregate: all of the above namespace scopes
- `READ_ARTIFACTS` — Read Artifactory artifacts and their property definitions
- `PUBLISH_ARTIFACTS` — Publish new artifact versions (implies `READ_ARTIFACTS`)
- `READ_PROFILES` — Read service profiles
- `WRITE_PROFILES` — Write service profiles (implies `READ_PROFILES`)
- `DELETE_PROFILES` — Delete service profiles (implies `WRITE_PROFILES`)
- `PROFILES` — Aggregate: all of the above profile scopes

---

## Scope Hierarchy

Scopes form a hierarchy—broader scopes imply narrower ones. Each `OAuthScope` enum constant declares its included scopes directly (see `OAuthScope#getIncluded()`), rather than following one blanket READ → WRITE → DELETE rule:

```
DELETE_NAMESPACES implies WRITE_NAMESPACES implies READ_NAMESPACES
INVITE_MEMBERS implies READ_NAMESPACES
PUBLISH_RELEASES implies READ_NAMESPACES
NAMESPACES implies all of the above namespace scopes

PUBLISH_ARTIFACTS implies READ_ARTIFACTS

DELETE_PROFILES implies WRITE_PROFILES implies READ_PROFILES
PROFILES implies all of the above profile scopes
```

**Examples:**
- Client with `WRITE_NAMESPACES` can also call `READ_NAMESPACES` endpoints
- Client with `PUBLISH_RELEASES` can also call `READ_NAMESPACES` endpoints, but not `WRITE_NAMESPACES` ones
- Scopes across different resource families never imply one another — `PUBLISH_ARTIFACTS` doesn't grant anything in the `NAMESPACES` or `PROFILES` families

---

## Error Responses

### 403 Forbidden (Missing Scope)

When a token lacks required scope:

```json
HTTP/1.1 403 Forbidden
Content-Type: application/problem+json

{
  "type": "about:blank",
  "title": "Forbidden",
  "status": 403,
  "detail": "Access denied: missing required scope 'read:namespaces'"
}
```

### 401 Unauthorized (No Token)

When request has no token or token is invalid:

```json
HTTP/1.1 401 Unauthorized
Content-Type: application/problem+json
WWW-Authenticate: Bearer realm="konfigyr"

{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Full authentication is required to access this resource"
}
```

---

## Testing with @RequiresScope

### Test: With Valid Scope

```java
@WebMvcTest(NamespaceController.class)
class NamespaceControllerTest extends AbstractControllerTest {

    @MockBean
    private NamespaceManager namespaces;

    @Test
    @DisplayName("should return namespace when scopes valid")
    void shouldGetNamespaceWithValidScope() throws Exception {
        Namespace konfigyr = Namespace.builder()
                .id(EntityId.from(1L))
                .slug("konfigyr")
                .name("Konfigyr")
                .build();

        given(namespaces.findBySlug("konfigyr"))
                .willReturn(Optional.of(konfigyr));

        mvc.get().uri("/namespaces/konfigyr")
                .with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
                .exchange()
                .assertThat()
                .hasStatusOk()
                .bodyJson()
                .convertTo(Namespace.class)
                .returns("konfigyr", Namespace::slug);
    }
}
```

### Test: Without Scope (403 Forbidden)

```java
@Test
@DisplayName("should return 403 when scope missing")
void shouldReturn403WhenScopeMissing() throws Exception {
    mvc.get().uri("/namespaces/konfigyr")
            .with(authentication(TestPrincipals.jane()))  // No scopes
            .exchange()
            .assertThat()
            .satisfies(forbidden(OAuthScope.READ_NAMESPACES));
}
```

Helper method `forbidden()`:
```java
private static Consumer<MvcTestResult> forbidden(OAuthScope... requiredScopes) {
    return result -> {
        assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(result.getResponse().getContentAsString())
                .contains("missing required scope");
    };
}
```

### Test: Without Authentication (401 Unauthorized)

```java
@Test
@DisplayName("should return 401 when no token provided")
void shouldReturn401WhenNoAuthentication() throws Exception {
    mvc.get().uri("/namespaces/konfigyr")
            .exchange()
            .assertThat()
            .satisfies(unauthorized());
}
```

Helper method `unauthorized()`:
```java
private static Consumer<MvcTestResult> unauthorized() {
    return result -> {
        assertThat(result.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    };
}
```

---

## Access Current User

Inside a secured endpoint, access the current user via Spring Security:

```java
@GetMapping("/me")
@RequiresScope(OAuthScope.READ_ACCOUNT)
AccountDetails getCurrentUser(Authentication auth) {
    JwtAuthenticationToken token = (JwtAuthenticationToken) auth;
    String accountId = token.getClaim("sub");  // Subject (user ID)
    String email = token.getClaim("email");
    // ... fetch account details, return response
}
```

Or use a custom annotation:

```java
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {}

// Resolver:
@Component
public class CurrentUserResolver implements HandlerMethodArgumentResolver {
    @Override
    public Object resolveArgument(MethodParameter parameter, /* ... */) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        JwtAuthenticationToken token = (JwtAuthenticationToken) auth;
        return token.getClaim("sub");
    }
}

// Usage:
@GetMapping("/me")
@RequiresScope(OAuthScope.READ_ACCOUNT)
AccountDetails getCurrentUser(@CurrentUser String userId) {
    // userId automatically injected
}
```

---

## Common Patterns

### Class-Level Scope

Apply same scope to all methods in a controller:

```java
@RestController
@RequestMapping("/namespaces/{namespace}/services")
@RequiresScope(OAuthScope.READ_NAMESPACES)
class ServicesController {
    // All methods require READ_NAMESPACES or higher
}
```

### Multiple Scopes (Any One Required)

If endpoint requires multiple scopes, list them:

```java
@PostMapping("/{slug}/trusted-issuers")
@RequiresScope({OAuthScope.WRITE_NAMESPACES, OAuthScope.INVITE_MEMBERS})
ResponseEntity<Void> createTrustedIssuer(@RequestBody NamespaceTrustedIssuerDefinition definition) {
    // Accepts if client has WRITE_NAMESPACES OR INVITE_MEMBERS
}
```

### Machine Client: Resolving the Namespace from the Token Claim

Namespace-scoped OAuth2 clients (e.g. build plugins with `PUBLISH_RELEASES`) never pass their namespace
as a path variable — resolve it from the trusted `namespace` JWT claim instead, via `NamespacedPrincipal`:

```java
@PostMapping
@RequiresScope(OAuthScope.PUBLISH_RELEASES)
EntityModel<ServiceRelease> resolve(@RequestBody List<ServiceReleaseCandidate> candidates) {
    final Namespace ns = currentNamespace();
    // ...
}

@NonNull
private Namespace currentNamespace() {
    final AuthenticatedPrincipal principal = AuthenticatedPrincipal.resolve();

    if (!(principal instanceof NamespacedPrincipal namespaced)) {
        throw new AccessDeniedException("Authenticated principal is not scoped to a namespace");
    }

    final EntityId id = namespaced.getNamespaceId().orElseThrow(
            () -> new AccessDeniedException("Authenticated principal is missing its namespace claim")
    );

    return namespaces.findById(id).orElseThrow(
            () -> new AccessDeniedException("Authenticated principal's namespace claim does not match a known namespace")
    );
}
```

Treat every failure to resolve a namespace this way as an authorization failure (`AccessDeniedException`,
403), not a not-found or server error: a token that reaches the endpoint without a resolvable namespace
claim isn't entitled to act on any namespace, regardless of the reason. `AccessDeniedException`'s detail
message is replaced app-wide by a generic message (see `problem-detail.properties`), so it's safe to
describe the failure reason in the exception message without leaking it to the client.

### Accessing Token Claims

Inside secured methods:

```java
@GetMapping("/protected")
@RequiresScope(OAuthScope.READ_NAMESPACES)
ResponseEntity<String> protectedEndpoint(
        JwtAuthenticationToken token,
        Authentication auth) {
    
    // JWT claims
    String userId = token.getClaim("sub");
    String email = token.getClaim("email");
    List<String> scopes = token.getClaim("scope");
    
    return ResponseEntity.ok("User: " + email);
}
```

---

## Configuration

OAuth2 configuration in `application.yml`:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8081  # Identity Provider URL
          jwk-set-uri: http://localhost:8081/oauth2/authorization/jwks
          claim-set-uri: http://localhost:8081/oauth2/authorization/claims

# Custom scope configuration (if needed)
konfigyr:
  security:
    scopes:
      - name: read:namespaces
        description: Read namespace metadata
      - name: write:namespaces
        description: Create and update namespaces
```

---

## Environment Variables

```bash
# Identity Provider
KONFIGYR_ID_URL=http://localhost:8081

# OAuth2 Client (for SSO, if needed)
KONFIGYR_OAUTH_CLIENT_ID=konfigyr-api
KONFIGYR_OAUTH_CLIENT_SECRET=...

# JWT Verification
KONFIGYR_JWT_ISSUER=http://localhost:8081
KONFIGYR_JWT_JWKS_URI=http://localhost:8081/.well-known/jwks.json
```

---

## Debugging

### Check Token in Browser

```javascript
// In browser DevTools console after login:
const token = sessionStorage.getItem('access_token');
console.log(JSON.parse(atob(token.split('.')[1])));  // Decode JWT payload
```

Output:
```json
{
  "sub": "user-123",
  "email": "user@example.com",
  "scope": "read:namespaces write:namespaces",
  "exp": 1701234567,
  "iat": 1701230967
}
```

### Enable Security Logging

```yaml
logging:
  level:
    org.springframework.security: DEBUG
```

---

## Common Mistakes

### ❌ Using Raw String Scopes

```java
// ✗ Wrong: String instead of enum
@RequiresScope("read:namespaces")

// ✓ Correct: Use OAuthScope enum
@RequiresScope(OAuthScope.READ_NAMESPACES)
```

### ❌ Missing @RequiresScope on Public Endpoint

```java
// ✗ Wrong: No scope protection
@GetMapping("/{slug}")
EntityModel<Namespace> get(@PathVariable String slug) { ... }

// ✓ Correct: Even read endpoints need scopes
@GetMapping("/{slug}")
@RequiresScope(OAuthScope.READ_NAMESPACES)
EntityModel<Namespace> get(@PathVariable String slug) { ... }
```

### ❌ Checking Scope in Code Instead of Annotation

```java
// ✗ Wrong: Manual scope checking
if (!token.getScopes().contains("read:namespaces")) {
    throw new AccessDeniedException("...");
}

// ✓ Correct: Use @RequiresScope annotation
@RequiresScope(OAuthScope.READ_NAMESPACES)
```

### ❌ Trusting User Input as Authorization Signal

```java
// ✗ Wrong: Client-provided scope claim
@GetMapping
String getData(@RequestParam String scope) {
    if (scope.equals("admin")) { ... }  // Not authenticated!
}

// ✓ Correct: Use authenticated token
@RequiresScope(OAuthScope.NAMESPACES)
String getData() { ... }
```

---

## Scope Design Best Practices

### Be Specific

```java
// ✗ Too broad: One aggregate scope for a narrow operation
@RequiresScope(OAuthScope.NAMESPACES)

// ✓ Specific: Granular scope for what the endpoint actually needs
@RequiresScope(OAuthScope.PUBLISH_RELEASES)
```

### Follow Hierarchy

```
READ → WRITE → DELETE
Basic operations → Advanced operations
```

### Document in OpenAPI

```java
@Operation(summary = "Get namespace details")
@ApiResponse(
    responseCode = "403",
    description = "Missing required scope: read:namespaces"
)
@GetMapping("/{slug}")
@RequiresScope(OAuthScope.READ_NAMESPACES)
EntityModel<Namespace> get(@PathVariable String slug) { ... }
```

---

## Verification Checklist

- [ ] All endpoints have `@RequiresScope` annotation
- [ ] Scope values use `OAuthScope` enum, not raw strings
- [ ] Scope hierarchy makes sense (READ < WRITE < DELETE)
- [ ] Tests verify both with-scope and without-scope cases
- [ ] 403 and 401 error responses documented
- [ ] Error messages mention required scope
- [ ] Token issuer configured in `application.yml`
- [ ] JWKS endpoint configured for signature verification
- [ ] Scope claims verified from authenticated token
- [ ] No manual scope checking in code (use annotation)

---

## When to Ask for Help

- "What scopes should this endpoint require?"
- "Do I need multiple scopes or is one sufficient?"
- "How do I handle cross-module authorization?"
- "Should I create a new scope or reuse an existing one?"
- "How do I test both authenticated and unauthenticated requests?"
