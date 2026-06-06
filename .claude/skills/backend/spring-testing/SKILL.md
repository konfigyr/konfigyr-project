---
name: spring-testing
description: Writing integration tests with AbstractIntegrationTest, controller tests with AbstractControllerTest, test base classes, assertions, and Arrange-Act-Assert patterns. Use when writing or updating tests for services, repositories, and REST endpoints.
---

# Spring Testing

## Test Base Classes

Konfigyr provides two test base classes that handle Spring context, database, and HTTP mocking setup.

### AbstractIntegrationTest

For integration tests with full Spring context and real database:

```java
// Provides:
//   - Full Spring Boot application context (@SpringBootTest)
//   - TestContainers PostgreSQL (real database, runs Liquibase migrations)
//   - WireMock server (for OAuth JWKS endpoint and external HTTP stubs)
//   - TestSmtpServer (for email assertions)
//   - PublishedEventsExtension (for asserting Spring domain events)
//   - @MockitoSpyBean: features, metadataStore, mailer
// Use for: NamespaceManagerTest, VaultServiceTest — anything needing real DB

@TestProfile
@SpringBootTest
@EnableWireMock
@TestSmtpServer
@TestObservations
@ImportTestcontainers(TestContainers.class)
@ExtendWith(PublishedEventsExtension.class)
public abstract class AbstractIntegrationTest {

    @InjectWireMock
    protected static WireMockServer wiremock;

    @Autowired
    protected DSLContext dsl;

    @MockitoSpyBean
    protected Features features;

    @MockitoSpyBean
    protected Mailer mailer;
}
```

**Inheriting from AbstractIntegrationTest:**

```java
@DisplayName("Namespace Manager")
class NamespaceManagerTest extends AbstractIntegrationTest {

    @Autowired
    private NamespaceManager manager;

    @Test
    @DisplayName("should create namespace and publish event")
    void shouldCreateNamespace(AssertablePublishedEvents events) {
        // Test code here
    }
}
```

### AbstractControllerTest

For controller/REST endpoint tests with MockMvc:

```java
// Extends AbstractIntegrationTest
// Provides:
//   - MockMvcTester mvc (fluent API for HTTP assertions)
//   - static authentication(...) helpers to forge JWT tokens
//   - static forbidden(), unauthorized() ProblemDetail consumers
//   - static namespaceNotFound(slug), serviceNotFound(slug) consumers

@WebMvcTest(NamespaceController.class)
public abstract class AbstractControllerTest extends AbstractIntegrationTest {

    protected static MockMvcTester mvc;
    protected static JsonMapper jsonMapper;

    @BeforeAll
    protected static void setup(WebApplicationContext context) {
        mvc = MockMvcTester.create(
                MockMvcBuilders.webAppContextSetup(context)
                        .apply(springSecurity())
                        .build()
        );
    }
}
```

**Inheriting from AbstractControllerTest:**

```java
@DisplayName("Namespace Controller")
@WebMvcTest(NamespaceController.class)
class NamespaceControllerTest extends AbstractControllerTest {

    @MockBean
    private NamespaceManager namespaces;

    @Test
    @DisplayName("should retrieve namespace by slug")
    void shouldGetNamespace() {
        // Test code here
    }
}
```

---

## Unit Tests (Domain Layer)

Unit tests don't extend any base class. They test pure domain logic with no database or Spring context.

### Test Domain Objects

```java
class NamespaceTest {

    @Test
    @DisplayName("should create namespace using fluent builder")
    void shouldBuildNamespace() {
        // Arrange
        Namespace namespace = Namespace.builder()
                .id(EntityId.from(1L))
                .slug("my-namespace")
                .name("My Namespace")
                .description("Test namespace")
                .avatar("https://example.com/avatar.gif")
                .build();

        // Assert
        assertThat(namespace)
                .returns(EntityId.from(1L), Namespace::id)
                .returns("my-namespace", Namespace::slug)
                .returns("My Namespace", Namespace::name);
    }

    @Test
    @DisplayName("should validate namespace data on build")
    void shouldValidateBuilder() {
        // Arrange
        Namespace.Builder builder = Namespace.builder();

        // Assert - ID required
        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Namespace entity identifier can not be null");

        // Assert - Slug required
        assertThatThrownBy(() -> builder.id(EntityId.from(1L)).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Namespace slug can not be blank");
    }

    @Test
    @DisplayName("should reject invalid slug")
    void shouldRejectInvalidSlug() {
        assertThatThrownBy(() -> Namespace.builder()
                .id(EntityId.from(1L))
                .slug("")  // Empty slug
                .name("Invalid")
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

---

## Integration Tests (Service Layer)

Integration tests extend `AbstractIntegrationTest`. They test service methods with real database.

### Test Service Operations

```java
@DisplayName("Namespace Manager")
class NamespaceManagerTest extends AbstractIntegrationTest {

    @Autowired
    private NamespaceManager manager;

    @Test
    @DisplayName("should fetch namespace by slug")
    void shouldFindBySlug() {
        // Arrange
        Namespace existing = Namespace.builder()
                .id(EntityId.from(1L))
                .slug("existing")
                .name("Existing")
                .build();

        // Act
        Optional<Namespace> found = manager.findBySlug("existing");

        // Assert
        assertThat(found)
                .isPresent()
                .get()
                .returns("existing", Namespace::slug);
    }

    @Test
    @Transactional
    @DisplayName("should create namespace and publish event")
    void shouldCreateNamespace(AssertablePublishedEvents events) {
        // Arrange
        NamespaceDefinition definition = NamespaceDefinition.builder()
                .owner(EntityId.from(1L))
                .slug("new-namespace")
                .name("New Namespace")
                .build();

        // Act
        Namespace created = manager.create(definition);

        // Assert - Domain object
        assertThat(created)
                .isNotNull()
                .returns("new-namespace", Namespace::slug);

        // Assert - Database persistence
        Namespace stored = dsl.selectFrom(NAMESPACES)
                .where(NAMESPACES.SLUG.eq("new-namespace"))
                .fetchOne(record -> /* mapping */);

        assertThat(stored).isNotNull();

        // Assert - Event published
        events.assertThat()
                .contains(NamespaceEvent.Created.class)
                .matching(e -> e.get().slug().equals("new-namespace"));
    }

    @Test
    @DisplayName("should throw NamespaceExistsException on duplicate slug")
    void shouldThrowOnDuplicate() {
        // Arrange
        NamespaceDefinition definition = NamespaceDefinition.builder()
                .owner(EntityId.from(1L))
                .slug("existing")  // Already exists
                .name("Duplicate")
                .build();

        // Assert
        assertThatThrownBy(() -> manager.create(definition))
                .isInstanceOf(NamespaceExistsException.class)
                .hasMessageContaining("existing");
    }

    @Test
    @DisplayName("should search namespaces by criteria")
    void shouldSearchByCriteria() {
        // Arrange
        SearchQuery query = SearchQuery.builder()
                .criteria(SearchQuery.ACCOUNT, EntityId.from(1L))
                .build();

        // Act
        Page<Namespace> results = manager.search(query);

        // Assert
        assertThat(results)
                .isNotEmpty()
                .allSatisfy(ns -> assertThat(ns.name()).isNotBlank());
    }
}
```

### Asserting Database State

```java
@Test
void shouldPersistToDatabase() {
    // Act
    Namespace created = manager.create(definition);

    // Assert - Query database directly using jOOQ
    long count = dsl.fetchCount(
            DSL.selectFrom(NAMESPACES)
                    .where(NAMESPACES.SLUG.eq(created.slug()))
    );

    assertThat(count).isEqualTo(1);

    // Assert - Fetch and verify
    Namespace stored = dsl.selectFrom(NAMESPACES)
            .where(NAMESPACES.ID.eq(created.id().get()))
            .fetchOne(DefaultNamespaceManager::toNamespace);

    assertThat(stored)
            .isEqualTo(created);
}
```

### Asserting Domain Events

```java
@Test
void shouldPublishNamespaceCreatedEvent(AssertablePublishedEvents events) {
    // Act
    Namespace created = manager.create(definition);

    // Assert
    events.assertThat()
            .contains(NamespaceEvent.Created.class)
            .matching(event -> {
                Namespace ns = event.get();
                return ns.id().equals(created.id())
                        && ns.slug().equals(created.slug());
            });
}
```

---

## Controller Tests (REST Endpoints)

Controller tests extend `AbstractControllerTest`. They test HTTP behavior: status codes, headers, response bodies, authentication.

### GET Endpoint

```java
@WebMvcTest(NamespaceController.class)
class NamespaceControllerTest extends AbstractControllerTest {

    @MockBean
    private NamespaceManager namespaces;

    @Test
    @DisplayName("should return namespace when found")
    void shouldGetNamespace() {
        // Arrange
        Namespace konfigyr = Namespace.builder()
                .id(EntityId.from(1L))
                .slug("konfigyr")
                .name("Konfigyr")
                .build();

        given(namespaces.findBySlug("konfigyr"))
                .willReturn(Optional.of(konfigyr));

        // Act & Assert
        mvc.get().uri("/namespaces/{slug}", "konfigyr")
                .with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
                .exchange()
                .assertThat()
                .hasStatusOk()
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .bodyJson()
                .convertTo(Namespace.class)
                .returns("konfigyr", Namespace::slug)
                .returns("Konfigyr", Namespace::name);
    }

    @Test
    @DisplayName("should return 404 when namespace not found")
    void shouldReturn404() {
        // Arrange
        given(namespaces.findBySlug("not-found"))
                .willReturn(Optional.empty());

        // Act & Assert
        mvc.get().uri("/namespaces/{slug}", "not-found")
                .with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
                .exchange()
                .assertThat()
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("should return 403 when scope missing")
    void shouldReturn403WithoutScope() {
        // Act & Assert
        mvc.get().uri("/namespaces/{slug}", "konfigyr")
                .with(authentication(TestPrincipals.jane()))  // No scope
                .exchange()
                .assertThat()
                .satisfies(forbidden(OAuthScope.READ_NAMESPACES));
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401Unauthenticated() {
        // Act & Assert
        mvc.get().uri("/namespaces/{slug}", "konfigyr")
                .exchange()  // No authentication
                .assertThat()
                .satisfies(unauthorized());
    }
}
```

### POST Endpoint

```java
@Test
@Transactional
@DisplayName("should create namespace and return 201")
void shouldCreateNamespace() {
    // Arrange
    NamespaceDefinition definition = NamespaceDefinition.builder()
            .owner(EntityId.from(1L))
            .slug("new-namespace")
            .name("New Namespace")
            .build();

    Namespace created = Namespace.builder()
            .id(EntityId.from(2L))
            .slug("new-namespace")
            .name("New Namespace")
            .build();

    given(namespaces.create(any()))
            .willReturn(created);

    // Act & Assert
    mvc.post().uri("/namespaces")
            .with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(definition))
            .exchange()
            .assertThat()
            .hasStatus(HttpStatus.CREATED)
            .hasHeader("Location", "/namespaces/new-namespace")
            .bodyJson()
            .convertTo(Namespace.class)
            .returns("new-namespace", Namespace::slug);
}

@Test
@DisplayName("should return 400 when validation fails")
void shouldReturn400OnValidationError() {
    // Arrange
    String invalidPayload = "{ \"slug\": \"\" }";  // Empty slug

    // Act & Assert
    mvc.post().uri("/namespaces")
            .with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidPayload)
            .exchange()
            .assertThat()
            .hasStatus(HttpStatus.BAD_REQUEST);
}
```

### DELETE Endpoint

```java
@Test
@DisplayName("should delete namespace and return 204")
void shouldDeleteNamespace() {
    // Arrange
    willDoNothing()
            .given(namespaces)
            .delete("konfigyr");

    // Act & Assert
    mvc.delete().uri("/namespaces/{slug}", "konfigyr")
            .with(authentication(TestPrincipals.john(), OAuthScope.DELETE_NAMESPACES))
            .exchange()
            .assertThat()
            .hasStatus(HttpStatus.NO_CONTENT)
            .hasNoContent();

    // Verify delete was called
    then(namespaces).should().delete("konfigyr");
}
```

---

## Test Helpers

### Authentication Helper

```java
// Create authenticated request with specific scopes
authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES)

// Create authenticated request with multiple scopes
authentication(TestPrincipals.jane(), 
        OAuthScope.READ_NAMESPACES, 
        OAuthScope.WRITE_NAMESPACES)

// Create unauthenticated request
// (just don't add .with(authentication(...)))
```

### Response Assertion Helpers

```java
// Assert 403 Forbidden (missing scope)
.satisfies(forbidden(OAuthScope.READ_NAMESPACES))

// Assert 401 Unauthorized
.satisfies(unauthorized())

// Assert 404 Not Found with specific message
.satisfies(namespaceNotFound("my-namespace"))

// Custom assertion
.satisfies(result -> {
    assertThat(result.getStatus()).isEqualTo(HttpStatus.OK);
    assertThat(result.getResponse().getContentAsString())
            .contains("expected content");
})
```

---

## Arrange-Act-Assert Pattern

All tests follow AAA structure:

```java
@Test
void testBehavior() {
    // ===== ARRANGE =====
    // Set up test data, mocks, conditions
    Namespace existing = Namespace.builder().slug("test").build();
    given(manager.findBySlug("test")).willReturn(Optional.of(existing));

    // ===== ACT =====
    // Call the method under test
    Optional<Namespace> result = manager.findBySlug("test");

    // ===== ASSERT =====
    // Verify the result matches expectations
    assertThat(result)
            .isPresent()
            .get()
            .returns("test", Namespace::slug);
}
```

---

## Test Naming Convention

| Test Type | Pattern | Example |
|-----------|---------|---------|
| Unit (domain) | `shouldX WhenY()` | `shouldRejectEmptySlugWhenBuilding()` |
| Integration | `shouldXWhenY()` | `shouldCreateNamespaceWhenDefinitionValid()` |
| Controller | `shouldReturnXWhenY()` | `shouldReturn403WhenScopeMissing()` |

---

## Common Assertions

```java
// Optional assertions
assertThat(optional)
        .isPresent()
        .get()
        .returns("value", Object::field);

// Collection assertions
assertThat(list)
        .isNotEmpty()
        .hasSize(3)
        .allSatisfy(item -> assertThat(item.name()).isNotBlank())
        .extracting(Item::id)
        .contains(1L, 2L, 3L);

// Exception assertions
assertThatThrownBy(() -> manager.create(invalid))
        .isInstanceOf(NamespaceExistsException.class)
        .hasMessageContaining("already exists");

// Event assertions
events.assertThat()
        .contains(NamespaceEvent.Created.class)
        .matching(e -> e.get().slug().equals("test"));
```

---

## Test Data Setup

### Using Test Fixtures

```java
class NamespaceManagerTest extends AbstractIntegrationTest {

    private Namespace testNamespace;

    @BeforeEach
    void setUp() {
        // Insert test data directly
        testNamespace = Namespace.builder()
                .id(EntityId.generate())
                .slug("test-ns")
                .name("Test Namespace")
                .build();

        // Persist via manager
        testNamespace = manager.create(
                NamespaceDefinition.builder()
                        .slug("test-ns")
                        .name("Test Namespace")
                        .build()
        );
    }

    @Test
    void shouldFindTestNamespace() {
        Optional<Namespace> found = manager.findBySlug("test-ns");
        assertThat(found).isPresent();
    }
}
```

### Using @Sql Annotation

```java
@Test
@Sql("/test-data/namespaces.sql")
void shouldQueryExistingData() {
    // Test code here
}
```

---

## Mocking External Services

### WireMock for HTTP Stubs

```java
@Test
void shouldCallExternalService() {
    // Stub external API
    wiremock.stubFor(get(urlPathEqualTo("/external/api"))
            .willReturn(ok()
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"status\": \"ok\"}")
            ));

    // Test code that calls external service
    String response = externalService.call();
    assertThat(response).contains("ok");
}
```

### MockitoSpyBean for Partial Mocking

```java
@MockitoSpyBean
private Features features;

@Test
void shouldUseFeatureFlag() {
    // Spy allows real calls, but can verify
    given(features.isEnabled("flag"))
            .willReturn(true);

    // Test code
    boolean enabled = features.isEnabled("flag");
    assertThat(enabled).isTrue();

    // Verify it was called
    then(features).should().isEnabled("flag");
}
```

---

## Verification Checklist

- [ ] Tests use Arrange-Act-Assert pattern
- [ ] Test names follow conventions (shouldX / shouldReturnX)
- [ ] Unit tests have no Spring context
- [ ] Integration tests use `AbstractIntegrationTest`
- [ ] Controller tests use `AbstractControllerTest`
- [ ] All endpoints tested with and without authentication
- [ ] OAuth2 scope requirements tested (with scope / without scope)
- [ ] Error cases tested (404, 400, 403, 401)
- [ ] Database state verified in integration tests
- [ ] Domain events asserted in event publishers
- [ ] Mocks verified when behavior should be called
- [ ] No hardcoded test data (use builders, factories)
- [ ] `@Transactional` used on tests that modify database
- [ ] `./gradlew test` passes, no skipped tests

---

## When to Ask for Help

- "How do I test this async operation?"
- "Should I mock this dependency or use the real one?"
- "How do I verify a database query was correct?"
- "What's the best way to set up test data?"
- "How do I test this cross-module event?"
