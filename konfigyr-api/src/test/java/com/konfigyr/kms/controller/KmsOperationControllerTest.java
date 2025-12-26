package com.konfigyr.kms.controller;

import com.konfigyr.crypto.KeysetOperation;
import com.konfigyr.entity.EntityId;
import com.konfigyr.kms.InactiveKeysetException;
import com.konfigyr.kms.KeysetNotFoundException;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.test.AbstractControllerTest;
import com.konfigyr.test.TestPrincipals;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

class KmsOperationControllerTest extends AbstractControllerTest {

	@Test
	@DisplayName("should encrypt data using KMS keyset")
	void encrypt() {
		mvc.post().uri("/namespaces/{slug}/kms/{id}/encrypt", "konfigyr", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"plaintext\": \"some data to be encrypted\",\"aad\": \"additional authentication data\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.hasPath("ciphertext")
				.hasPath("checksum");
	}

	@Test
	@DisplayName("should fail to encrypt data with invalid payload")
	void encryptWithInvalidPayload() {
		mvc.post().uri("/namespaces/{slug}/kms/{id}/encrypt", "konfigyr", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.BAD_REQUEST, problem -> problem
						.hasTitleContaining("Invalid request content")
						.hasPropertySatisfying("errors", errors -> assertThat(errors)
								.isNotNull()
								.isInstanceOf(Collection.class)
								.asInstanceOf(InstanceOfAssertFactories.collection(Map.class))
								.extracting("pointer")
								.containsExactlyInAnyOrder("plaintext")
						)
				));
	}

	@Test
	@DisplayName("should decrypt data using KMS keyset")
	void decrypt() {
		mvc.post().uri("/namespaces/{slug}/kms/{id}/decrypt", "konfigyr", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"ciphertext\": \"AZMmMUxIGE7mi3Ozd1bp_wwbrYKckMub9pubA8YJmnAPPsUqZ9D1gALW7lUk6c_HERMdHAOGm6KwZQ==\",\"aad\": \"additional authentication data\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.hasPathSatisfying("plaintext", it -> it.assertThat()
						.isEqualTo("some data to be encrypted")
				);
	}

	@Test
	@DisplayName("should fail to decrypt data when invalid ciphertext is given")
	void decryptInvalidCiphertext() {
		mvc.post().uri("/namespaces/{slug}/kms/{id}/decrypt", "konfigyr", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"ciphertext\": \"aW52YWxpZCBkYXRh\",\"aad\": \"additional authentication data\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.UNPROCESSABLE_CONTENT, detail -> detail
						.hasTitleContaining("Decryption failed")
						.hasDetailContaining("The provided ciphertext could not be decrypted")
						.hasProperty("operation", KeysetOperation.DECRYPT.name())
				));
	}

	@Test
	@DisplayName("should fail to decrypt data when invalid authentication data is given")
	void decryptInvalidAuthenticationData() {
		mvc.post().uri("/namespaces/{slug}/kms/{id}/decrypt", "konfigyr", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"ciphertext\": \"AZMmMUxIGE7mi3Ozd1bp_wwbrYKckMub9pubA8YJmnAPPsUqZ9D1gALW7lUk6c_HERMdHAOGm6KwZQ==\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.UNPROCESSABLE_CONTENT, detail -> detail
						.hasTitleContaining("Decryption failed")
						.hasDetailContaining("The provided ciphertext could not be decrypted")
						.hasProperty("operation", KeysetOperation.DECRYPT.name())
				));
	}

	@Test
	@DisplayName("should fail to decrypt data with invalid payload")
	void decryptWithInvalidPayload() {
		mvc.post().uri("/namespaces/{slug}/kms/{id}/decrypt", "konfigyr", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.BAD_REQUEST, problem -> problem
						.hasTitleContaining("Invalid request content")
						.hasPropertySatisfying("errors", errors -> assertThat(errors)
								.isNotNull()
								.isInstanceOf(Collection.class)
								.asInstanceOf(InstanceOfAssertFactories.collection(Map.class))
								.extracting("pointer")
								.containsExactlyInAnyOrder("ciphertext")
						)
				));
	}

	@Test
	@DisplayName("should sign data using KMS keyset")
	void sign() {
		mvc.post().uri("/namespaces/{slug}/kms/{id}/sign", "john-doe", EntityId.from(6).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"plaintext\": \"some data to be signed\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.hasPath("signature");
	}

	@Test
	@DisplayName("should fail to generate signature with invalid payload")
	void signWithInvalidPayload() {
		mvc.post().uri("/namespaces/{slug}/kms/{id}/sign", "john-doe", EntityId.from(6).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.BAD_REQUEST, problem -> problem
						.hasTitleContaining("Invalid request content")
						.hasPropertySatisfying("errors", errors -> assertThat(errors)
								.isNotNull()
								.isInstanceOf(Collection.class)
								.asInstanceOf(InstanceOfAssertFactories.collection(Map.class))
								.extracting("pointer")
								.containsExactlyInAnyOrder("plaintext")
						)
				));
	}

	@Test
	@DisplayName("should verify digital signature using KMS keyset")
	void verify() {
		mvc.post().uri("/namespaces/{slug}/kms/{id}/verify", "john-doe", EntityId.from(6).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"plaintext\": \"some data to be signed\",\"signature\": \"AUDrngEwRQIgOGOqDkChDCswWyb-a5zPu73A5ucODR4Qsslp6JytE0gCIQDCVDoCcofYm7mbZYiimcHqOAcnIRXp8gM2lSmzoml1vw==\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.hasPathSatisfying("valid", it -> it.assertThat()
						.asBoolean()
						.isTrue()
				);
	}

	@Test
	@DisplayName("should fail to verify invalid digital signature")
	void invalidSignature() {
		mvc.post().uri("/namespaces/{slug}/kms/{id}/verify", "john-doe", EntityId.from(6).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"plaintext\": \"some data to be signed\",\"signature\": \"aW52YWxpZCBkYXRh\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.hasPathSatisfying("valid", it -> it.assertThat()
						.asBoolean()
						.isFalse()
				);
	}

	@Test
	@DisplayName("should fail to verify signature with invalid payload")
	void verifyWithInvalidPayload() {
		mvc.post().uri("/namespaces/{slug}/kms/{id}/verify", "john-doe", EntityId.from(6).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.BAD_REQUEST, problem -> problem
						.hasTitleContaining("Invalid request content")
						.hasPropertySatisfying("errors", errors -> assertThat(errors)
								.isNotNull()
								.isInstanceOf(Collection.class)
								.asInstanceOf(InstanceOfAssertFactories.collection(Map.class))
								.extracting("pointer")
								.containsExactlyInAnyOrder("plaintext", "signature")
						)
				));
	}

	@Test
	@DisplayName("should fail to to perform encryption operation that is not supported by a keyset")
	void unsupportedEncryptOperation() {
		mvc.post().uri("/namespaces/{slug}/kms/{id}/encrypt", "john-doe", EntityId.from(6).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"plaintext\": \"encrypt me\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.BAD_REQUEST, detail -> detail
						.hasTitleContaining("Invalid keyset usage")
						.hasDetailContaining("You have attempted to perform 'ENCRYPT' keyset operation")
						.hasDetailContaining("Allowed operations are: SIGN, VERIFY")
						.hasProperty("operation", KeysetOperation.ENCRYPT.name())
						.hasProperty("supported", List.of(KeysetOperation.SIGN.name(), KeysetOperation.VERIFY.name()))
				));
	}

	@Test
	@DisplayName("should fail to to perform decryption operation that is not supported by a keyset")
	void unsupportedDecryptOperation() {
		mvc.post().uri("/namespaces/{slug}/kms/{id}/decrypt", "john-doe", EntityId.from(6).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"ciphertext\": \"aW52YWxpZCBkYXRh\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.BAD_REQUEST, detail -> detail
						.hasTitleContaining("Invalid keyset usage")
						.hasDetailContaining("You have attempted to perform 'DECRYPT' keyset operation")
						.hasDetailContaining("Allowed operations are: SIGN, VERIFY")
						.hasProperty("operation", KeysetOperation.DECRYPT.name())
						.hasProperty("supported", List.of(KeysetOperation.SIGN.name(), KeysetOperation.VERIFY.name()))
				));
	}

	@Test
	@DisplayName("should fail to to perform signing operation that is not supported by a keyset")
	void unsupportedSigningOperation() {
		mvc.post().uri("/namespaces/{slug}/kms/{id}/sign", "konfigyr", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"plaintext\": \"sign me\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.BAD_REQUEST, detail -> detail
						.hasTitleContaining("Invalid keyset usage")
						.hasDetailContaining("You have attempted to perform 'SIGN' keyset operation")
						.hasDetailContaining("Allowed operations are: DECRYPT, ENCRYPT")
						.hasProperty("operation", KeysetOperation.SIGN.name())
						.hasProperty("supported", List.of(KeysetOperation.DECRYPT.name(), KeysetOperation.ENCRYPT.name()))
				));
	}

	@Test
	@DisplayName("should fail to to perform signature verification operation that is not supported by a keyset")
	void unsupportedVerificationOperation() {
		mvc.post().uri("/namespaces/{slug}/kms/{id}/verify", "konfigyr", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"plaintext\": \"sign me\"}, \"signature\": \"some signature\"")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.BAD_REQUEST, detail -> detail
						.hasTitleContaining("")
				));
	}

	@ValueSource(strings = {"encrypt", "decrypt", "sign", "verify"})
	@ParameterizedTest(name = "KMS keyset operation: {0}")
	@DisplayName("should fail to perform KMS operation when not authenticated")
	void unauthorized(String operation) {
		mvc.post().uri("/namespaces/{slug}/kms/{id}/{operation}", "konfigyr", EntityId.from(2).serialize(), operation)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"plaintext\": \"dummy\",\"ciphertext\": \"dummy\",\"signature\": \"dummy\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(unauthorized());
	}

	@ValueSource(strings = {"encrypt", "decrypt", "sign", "verify"})
	@ParameterizedTest(name = "KMS keyset operation: {0}")
	@DisplayName("should fail to perform KMS operation when OAuth scope is missing")
	void missingScope(String operation) {
		mvc.post().uri("/namespaces/{slug}/kms/{id}/{operation}", "konfigyr", EntityId.from(2).serialize(), operation)
				.with(authentication(TestPrincipals.john()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"plaintext\": \"dummy\",\"ciphertext\": \"dummy\",\"signature\": \"dummy\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@ValueSource(strings = {"encrypt", "decrypt", "sign", "verify"})
	@ParameterizedTest(name = "KMS keyset operation: {0}")
	@DisplayName("should fail to perform KMS operation when user is not a member of namespace")
	void membershipMissing(String operation) {
		mvc.post().uri("/namespaces/{slug}/kms/{id}/{operation}", "john-doe", EntityId.from(1).serialize(), operation)
				.with(authentication(TestPrincipals.jane(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"plaintext\": \"dummy\",\"ciphertext\": \"dummy\",\"signature\": \"dummy\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@ValueSource(strings = {"encrypt", "decrypt", "sign", "verify"})
	@ParameterizedTest(name = "KMS keyset operation: {0}")
	@DisplayName("should fail to perform KMS operation for unknown namespace")
	void unknownNamespace(String operation) {
		mvc.post().uri("/namespaces/{slug}/kms/{id}/{operation}", "unknown", EntityId.from(1).serialize(), operation)
				.with(authentication(TestPrincipals.jane(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"plaintext\": \"dummy\",\"ciphertext\": \"dummy\",\"signature\": \"dummy\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown"));
	}

	@ValueSource(strings = {"encrypt", "decrypt", "sign", "verify"})
	@ParameterizedTest(name = "KMS keyset operation: {0}")
	@DisplayName("should fail to perform KMS operation for unknown keyset")
	void unknownKeyset(String operation) {
		mvc.post().uri("/namespaces/{slug}/kms/{id}/{operation}", "konfigyr", EntityId.from(1).serialize(), operation)
				.with(authentication(TestPrincipals.jane(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"plaintext\": \"dummy\",\"ciphertext\": \"dummy\",\"signature\": \"dummy\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NOT_FOUND)
				.satisfies(hasFailedWithException(KeysetNotFoundException.class));
	}

	@ValueSource(strings = {"encrypt", "decrypt", "sign", "verify"})
	@ParameterizedTest(name = "KMS keyset operation: {0}")
	@DisplayName("should fail to perform KMS operation for inactive keyset")
	void inactiveKeyset(String operation) {
		mvc.post().uri("/namespaces/{slug}/kms/{id}/{operation}", "konfigyr", EntityId.from(3).serialize(), operation)
				.with(authentication(TestPrincipals.jane(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"plaintext\": \"dummy\",\"ciphertext\": \"dummy\",\"signature\": \"dummy\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.CONFLICT)
				.satisfies(hasFailedWithException(InactiveKeysetException.class));
	}

	@ValueSource(strings = {"encrypt", "decrypt", "sign", "verify"})
	@ParameterizedTest(name = "KMS keyset operation: {0}")
	@DisplayName("should fail to perform KMS operation for keyset that is scheduled for destruction")
	void disabledKeyset(String operation) {
		mvc.post().uri("/namespaces/{slug}/kms/{id}/{operation}", "konfigyr", EntityId.from(4).serialize(), operation)
				.with(authentication(TestPrincipals.jane(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"plaintext\": \"dummy\",\"ciphertext\": \"dummy\",\"signature\": \"dummy\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.CONFLICT)
				.satisfies(hasFailedWithException(InactiveKeysetException.class));
	}

	@ValueSource(strings = {"encrypt", "decrypt", "sign", "verify"})
	@ParameterizedTest(name = "KMS keyset operation: {0}")
	@DisplayName("should fail to perform KMS operation for destroyed keyset")
	void destroyedKeyset(String operation) {
		mvc.post().uri("/namespaces/{slug}/kms/{id}/{operation}", "konfigyr", EntityId.from(5).serialize(), operation)
				.with(authentication(TestPrincipals.jane(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"plaintext\": \"dummy\",\"ciphertext\": \"dummy\",\"signature\": \"dummy\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.CONFLICT)
				.satisfies(hasFailedWithException(InactiveKeysetException.class));
	}

}
