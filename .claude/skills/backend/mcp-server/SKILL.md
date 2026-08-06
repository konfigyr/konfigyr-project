---
name: mcp-server
description: Registering MCP tools and resources with @McpTool/@McpResource, structured output schemas, namespace scoping, @RequiresScope enforcement, and JSON-RPC error/status conventions. Use when adding or changing anything under com.konfigyr.mcp.
---

# MCP Server

## Overview

`konfigyr-api`'s `/mcp` endpoint exposes namespace-scoped domain operations to LLM/agent clients
over the Model Context Protocol. It's built on the plain MCP Java SDK
(`io.modelcontextprotocol.sdk:mcp`), not Spring AI's starter — a hand-rolled Spring MVC controller
(`McpEndpoint`) implements the JSON-RPC transport directly so it composes with this codebase's
existing OAuth2 security and MockMvc-based testing infrastructure.

`com.konfigyr.mcp` is not a business-domain module like `namespace`/`vault`/`artifactory` — it's an
integration layer. It never owns data or aggregates; every tool/resource method is a thin adapter
that resolves the caller's `Namespace` and delegates straight to another module's existing service
interface (`Services`, `ProfileManager`, `ChangeRequestManager`, ...).

**Package layout:**

| Package | Responsibility |
|---|---|
| `mcp.annotation` | `@McpTool`, `@McpResource`, `@McpToolParam`, `@McpTemplateVariable`, `@McpComponent`, `@ConditionalOnMcpServer` |
| `mcp.registry` | `McpAnnotationBeanPostProcessor` scans `@McpComponent` beans for annotated methods; `McpAnnotationRegistry` holds the discovered registrations |
| `mcp.invoke` | `AbstractMcpHandlerMethodInvoker` — generic reflection-driven method invocation shared by tools and resources |
| `mcp.tool` | `McpToolSpecificationFactory`/`McpToolInvoker`, `StructuredOutput` hierarchy |
| `mcp.resource` | `McpResourceSpecificationFactory`/`McpResourceInvoker` |
| `mcp.schema` | JSON Schema generation for tool input/output schemas |
| `McpTools`/`McpResources` | The actual registered tools/resources — one `@McpComponent` bean each |
| `McpEndpoint` | The `/mcp` HTTP entry point: JSON-RPC parsing, dispatch, error/status mapping |
| `McpAutoConfiguration` | Wires the MCP server bean, registries, and factories together |

---

## Resource vs. Tool

Two different MCP primitives, picked per the shape of the question, not by habit:

- **Resource** (`@McpResource`) — "fetch a known document by identity, no query construction." One
  already-known identifier in (via URI template variables), the current whole document out. No
  ranking, no filtering. Example: `service_manifest` — once the model has a service slug, reading
  its manifest is "attach this document," not "answer a question with parameters."
- **Tool** (`@McpTool`) — anything requiring the model to construct query/write arguments from
  conversation context: search terms, filters, pagination, mutations. Example: `list_services` —
  the model may supply a free-text `term` to narrow results.

If a handler method takes no meaningful arguments beyond an already-known identifier, it's a
resource. If the model has to decide *what* to pass, it's a tool.

---

## Registering a Resource

```java
@McpResource(
        uri = "konfigyr://services/{service}/manifest",
        name = "service_manifest",
        title = "Service Manifest",
        description = "Current manifest of the service identified by the 'service' URI variable " +
                "(its exact slug, scoped to your namespace) - the artifact coordinates and versions " +
                "this service's latest completed release declares. Does not include configuration " +
                "properties; use the service_catalog resource for those.",
        mimeType = MediaType.APPLICATION_JSON_VALUE
)
@RequiresScope(OAuthScope.READ_NAMESPACES)
Manifest serviceManifest(McpTransportContext context, @McpTemplateVariable("service") String slug) {
    final Namespace namespace = (Namespace) context.get(KonfigyrClaimNames.NAMESPACE);
    final Service service = services.get(namespace, slug)
            .orElseThrow(() -> new ServiceNotFoundException(namespace.slug(), slug));

    return manifests.get(service);
}
```

- `uri` uses RFC 6570 template syntax; each `{variable}` is bound to a `@McpTemplateVariable`-annotated
  parameter (blank value falls back to the parameter's own name).
- `mimeType` is declared **once**, on the annotation — not inferred per-call. It's threaded through
  to `McpResourceInvoker` and applied to whatever content the handler method returns. Default is
  `text/plain`.
- Return type conversion (`McpResourceInvoker.convertContents`): a `String` becomes text content; a
  `ByteArray` or `InputStreamSource` is read and **base64-encoded** into a blob (never pass raw
  bytes straight through — the MCP spec requires `blob` to be base64); a `Collection` is flattened
  into multiple contents; anything else falls back to JSON serialization (always tagged
  `application/json`, regardless of the declared `mimeType`).

---

## Registering a Tool

```java
@McpTool(
        name = "list_profiles",
        title = "List service profiles",
        description = "List `{service}`'s profiles with their slug, name, and policy (`UNPROTECTED`, " +
                "`PROTECTED`, or `IMMUTABLE`), optionally filtered by name via `term`. Use before " +
                "`propose_profile_change` to confirm a profile slug exists and isn't `IMMUTABLE` - " +
                "proposing changes against an `IMMUTABLE` profile will be rejected."
)
@RequiresScope(OAuthScope.READ_PROFILES)
StructuredCollectionOutput<Profile> listProfiles(
        McpTransportContext context,
        @McpToolParam(name = "service", description = "Unique service slug") String slug,
        @McpToolParam(description = "Profile search term", required = false) String term
) {
    final Service service = lookupService(context, slug);
    final SearchQuery query = SearchQuery.builder().term(term).pageable(Pageable.ofSize(10)).build();

    return StructuredOutput.of(profiles.find(service, query));
}
```

### Writing the `description`

It's read by the model to decide *whether* and *how* to call the tool — write for that audience,
not for a human reading API docs. State, as concisely as possible:

- **What it does** — one clear sentence.
- **When to use it** — the trigger condition, especially if it disambiguates from a sibling tool
  (e.g. a targeted search tool vs. a "fetch everything" resource).
- **What it returns** — shape/content, not full schema (the JSON schema already carries that).
- **Constraints/caveats that change model behavior** — read-only vs. mutating, scoping rules,
  what a not-found/empty result means if it's non-obvious.

### Return types and `StructuredOutput`

Wrap a tool's return value with `StructuredOutput.of(...)` (three overloads: a single value, a
`Collection`, or a `Page`) to get `StructuredEntityOutput`/`StructuredCollectionOutput` — these
serialize to both text content and MCP's `structuredContent`, **and** are the only return types
`McpToolSpecificationFactory` generates an output schema for (via `@McpTool(generateOutputSchema =
true)`, checked against `StructuredOutput.class.isAssignableFrom(returnType)`). Any other return
type (a plain `String`, an arbitrary object) still serializes fine at invocation time, but the tool
is advertised via `tools/list` with no output schema — a model can't discover its result shape
ahead of calling it.

---

## Namespace scoping — do not leak cross-namespace existence

The namespace is resolved once, from the `kfg_namespace` JWT claim via `McpTransportContext`
(`context.get(KonfigyrClaimNames.NAMESPACE)`) — **never** from a tool/resource argument or URI
variable. Every lookup that takes a slug (service, profile, ...) must be scoped to that resolved
namespace, e.g. `services.get(namespace, slug)`.

The critical invariant: a slug belonging to a *different* namespace must produce the **exact same**
error as a slug that doesn't exist anywhere — same exception type, same message shape. Otherwise a
model (and the human behind it) can enumerate what services/profiles exist in namespaces it has no
access to, just by noticing the error changes.

```java
private Service lookupService(McpTransportContext context, String slug) {
    final Namespace namespace = (Namespace) context.get(KonfigyrClaimNames.NAMESPACE);
    return services.get(namespace, slug).orElseThrow(() -> new ServiceNotFoundException(namespace.slug(), slug));
}
```

Since `services.get(namespace, slug)` filters by namespace in the query itself, a foreign
namespace's service and a nonexistent one both resolve to `Optional.empty()` — there's no separate
"found, but wrong namespace" branch to accidentally get right or wrong. Reuse this same pattern
(a namespace-scoped lookup throwing a proper domain exception, not a bare `.orElseThrow()`) for
every tool/resource that takes an identifier — a bare `.orElseThrow()` throws a generic
`NoSuchElementException("No value present")`, which is technically safe (still no leak) but useless
for anyone debugging a real "not found" from the client side.

---

## Error handling and HTTP status

`McpEndpoint.resolveStatusCode` maps a completed JSON-RPC response to an HTTP status by its error
**code**, not by whether an error is present at all:

```java
static HttpStatusCode resolveStatusCode(McpSchema.@Nullable JSONRPCResponse response) {
    final boolean internalError = response != null && response.error() != null
            && response.error().code() == McpSchema.ErrorCodes.INTERNAL_ERROR;

    return internalError ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.OK;
}
```

Only `INTERNAL_ERROR` (`-32603`) is transported as HTTP 500. Every other protocol-level error
(`INVALID_PARAMS`, `METHOD_NOT_FOUND`, ...) is HTTP 200 with the error in the body, per JSON-RPC/MCP
convention — a client shouldn't treat "unknown tool" or "invalid params" as a transport failure.

**Any exception a tool/resource handler method throws is caught by the MCP SDK itself** and
converted into a JSON-RPC error with code `INTERNAL_ERROR` and **message = the exception's own
`getMessage()`** — which means:

- A domain "not found" exception surfaces as HTTP 500 with your exception's message verbatim in the
  response body. Don't put anything sensitive in a domain exception message reachable from a tool.
- `@RequiresScope` denial surfaces the same way. `@RequiresScope` here is enforced by a global AOP
  `Advisor` (`OAuthSecurityConfiguration.requiresScope()`), not Spring MVC controller security — so
  a missing scope on a tool/resource method throws `AuthorizationDeniedException` from *inside* the
  reactive invocation chain, which the SDK's catch-all converts the same way as any other exception:
  **HTTP 500, code `-32603`, message `"Access Denied"`** — not an HTTP 403 `ProblemDetail` like
  `@RequiresScope(OAuthScope.MCP)` on `McpEndpoint.handle(...)` itself produces. Don't reach for the
  `forbidden(...)` test helper here; see Testing below.

---

## Testing

Extend `AbstractMcpTest` (adds `mapper` and `serializeMcpRequest(method, params)` over
`AbstractControllerTest`).

```java
class McpToolsTest extends AbstractMcpTest {

    @Test
    @DisplayName("should list a service's profiles")
    void shouldListProfiles() {
        final var request = McpSchema.CallToolRequest.builder("list_profiles")
                .arguments(Map.of("service", "konfigyr-id"))
                .build();

        mvc.post().uri("/mcp")
                .with(authentication(EntityId.from(2), OAuthScope.MCP, OAuthScope.READ_PROFILES))
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializeMcpRequest(McpSchema.METHOD_TOOLS_CALL, request))
                .exchange()
                .assertThat()
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.result.structuredContent.contents")
                .convertTo(InstanceOfAssertFactories.list(Profile.class))
                // ...
    }
}
```

- `authentication(EntityId namespace, OAuthScope... scopes)` builds a JWT carrying the `namespace`
  claim — this is what `McpTransportContext` resolves at request time, not a URL path variable.
- `mcpErrorFor(HttpStatus status, ThrowingConsumer<JSONRPCErrorAssert> consumer)` asserts the HTTP
  status, content type, and `$.error` body together — use this for every error-path test:

```java
.satisfies(mcpErrorFor(HttpStatus.INTERNAL_SERVER_ERROR, error -> error
        .hasErrorCode(McpSchema.ErrorCodes.INTERNAL_ERROR)
        .hasMessage("Could not find a service with the following name: john-doe-blog within a konfigyr Namespace")
));
```

**Test every tool/resource for three things, not just the happy path:**

1. **Missing scope** — call with `OAuthScope.MCP` only, no domain scope. Expect
   `mcpErrorFor(HttpStatus.INTERNAL_SERVER_ERROR, error -> error.hasErrorCode(INTERNAL_ERROR).hasMessage("Access Denied"))`.
   Do **not** use the `forbidden(...)` helper — that asserts an HTTP 403 `ProblemDetail`, which is
   only what `@RequiresScope(OAuthScope.MCP)` on `McpEndpoint` itself produces, not what a
   `McpTools`/`McpResources` method's own `@RequiresScope` produces.
2. **Unknown identifier** — a slug that doesn't exist at all.
3. **Cross-namespace identifier** — a slug known to belong to a *different* namespace (the
   `john-doe-blog` service, namespace 1, is the existing fixture for this — see
   `konfigyr-test/src/main/resources/data/services.sql`). Assert the **exact same** error as (2),
   proving no existence leak across namespaces.

For unit-testing the annotation/registry/factory/invoker machinery in isolation (not through the
full `/mcp` HTTP endpoint), see `McpToolFixtures`/`McpResourceFixtures` and the existing
`Mcp*SpecificationFactoryTest`/`Mcp*InvokerTest` classes for the pattern of building a
`McpAnnotationRegistration` by hand.

---

## Do's and Don'ts

### Do

✅ Resolve the namespace once from `McpTransportContext`, never from an argument or URI variable
✅ Scope every identifier lookup by that namespace, and throw the same domain exception for
"doesn't exist" and "belongs to another namespace"
✅ Wrap tool return values in `StructuredOutput.of(...)` when you want an output schema generated
✅ Write tool/resource descriptions for the model (what/when/returns/constraints), not for a human
reading API docs
✅ Declare a resource's `mimeType` on the annotation; base64-encode binary content, never pass raw
bytes into a blob
✅ Test missing-scope, unknown-identifier, and cross-namespace-identifier for every tool/resource
✅ Use `mcpErrorFor(...)` for every error-path assertion, matching the exact HTTP status the code
in this file actually produces (INTERNAL_ERROR → 500, everything else → 200)

### Don't

❌ Put sensitive detail in a domain exception message thrown from a tool/resource handler — it
lands verbatim in the JSON-RPC error body
❌ Assume `@RequiresScope` denial on a tool/resource method looks like a 403 — verify against
`mcpErrorFor(HttpStatus.INTERNAL_SERVER_ERROR, ...)`, not `forbidden(...)`
❌ Use a bare `.orElseThrow()` for a namespace-scoped lookup — it's not a data leak, but it throws
an uninformative `NoSuchElementException("No value present")`
❌ Add a resource for something that needs model-constructed query/filter/write arguments — that's
a tool
❌ Reuse another type's exception message verbatim when copying an invoker/factory pattern — write
one accurate to what actually failed

---

## When to Ask for Help

- "Should this be a resource or a tool?" — if there's any argument beyond an already-known
  identifier, it's a tool.
- "What scope should this require?" — match the REST endpoint that exposes the same underlying
  data, if one exists.
- "Is this identifier lookup namespace-scoped correctly?" — trace it back to
  `context.get(KonfigyrClaimNames.NAMESPACE)`; if the query it feeds isn't filtered by that
  namespace, it's a data-leak bug, not a style nit.
