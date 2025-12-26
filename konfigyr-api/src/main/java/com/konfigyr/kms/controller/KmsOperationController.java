package com.konfigyr.kms.controller;

import com.konfigyr.crypto.KeysetOperations;
import com.konfigyr.entity.EntityId;
import com.konfigyr.io.ByteArray;
import com.konfigyr.kms.KeysetManager;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceManager;
import com.konfigyr.namespace.NamespaceNotFoundException;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.oauth.RequiresScope;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequiresScope(OAuthScope.WRITE_NAMESPACES)
@RequestMapping("/namespaces/{namespace}/kms/{id}")
class KmsOperationController {

	private final KeysetManager manager;
	private final NamespaceManager namespaces;

	@PostMapping("encrypt")
	@PreAuthorize("isMember(#namespace)")
	Map<String, Object> encrypt(
			@PathVariable @NonNull String namespace,
			@PathVariable @NonNull EntityId id,
			@RequestBody @Validated EncryptAttributes attributes
	) {
		return execute(namespace, id, attributes);
	}

	@PostMapping("decrypt")
	@PreAuthorize("isMember(#namespace)")
	Map<String, Object> decrypt(
			@PathVariable @NonNull String namespace,
			@PathVariable @NonNull EntityId id,
			@RequestBody @Validated DecryptAttributes attributes
	) {
		return execute(namespace, id, attributes);
	}

	@PostMapping("sign")
	@PreAuthorize("isMember(#namespace)")
	Map<String, Object> sign(
			@PathVariable @NonNull String namespace,
			@PathVariable @NonNull EntityId id,
			@RequestBody @Validated SignAttributes attributes
	) {
		return execute(namespace, id, attributes);
	}

	@PostMapping("verify")
	@PreAuthorize("isMember(#namespace)")
	Map<String, Object> verify(
			@PathVariable @NonNull String namespace,
			@PathVariable @NonNull EntityId id,
			@RequestBody @Validated VerifyAttributes attributes
	) {
		return execute(namespace, id, attributes);
	}

	@NullMarked
	private Map<String, Object> execute(String slug, EntityId id, KeysetOperationPerformer performer) {
		final Namespace namespace = namespaces.findBySlug(slug).orElseThrow(() -> new NamespaceNotFoundException(slug));
		final KeysetOperations operations = manager.operations(namespace.id(), id);

		return performer.perform(operations);
	}

	@NullMarked
	sealed interface KeysetOperationPerformer permits
			KmsOperationController.EncryptAttributes,
			KmsOperationController.DecryptAttributes,
			KmsOperationController.SignAttributes,
			KmsOperationController.VerifyAttributes {

		Map<String, Object> perform(KeysetOperations operations);

	}

	record EncryptAttributes(@NotBlank String plaintext, String aad) implements KeysetOperationPerformer {

		@NonNull
		@Override
		public Map<String, Object> perform(@NonNull KeysetOperations operations) {
			final ByteArray ciphertext = operations.encrypt(
					ByteArray.fromString(plaintext()),
					StringUtils.hasText(aad()) ? ByteArray.fromString(aad()) : null
			);

			final Digest digest = new SHA256Digest();
			digest.update(ciphertext.array(), 0, ciphertext.size());

			final byte[] checksum = new byte[digest.getDigestSize()];
			digest.doFinal(checksum, 0);

			return Map.of(
					"ciphertext", ciphertext.encode(),
					"checksum", new ByteArray(checksum).encode()
			);
		}

	}

	record DecryptAttributes(@NotBlank String ciphertext, String aad) implements KeysetOperationPerformer {

		@NonNull
		@Override
		public Map<String, Object> perform(@NonNull KeysetOperations operations) {
			final ByteArray plaintext = operations.decrypt(
					ByteArray.fromBase64String(ciphertext()),
					StringUtils.hasText(aad()) ? ByteArray.fromString(aad()) : null
			);

			return Map.of("plaintext", new String(plaintext.array(), StandardCharsets.UTF_8));
		}

	}

	record SignAttributes(@NotBlank String plaintext) implements KeysetOperationPerformer {

		@NonNull
		@Override
		public Map<String, Object> perform(@NonNull KeysetOperations operations) {
			final ByteArray signature = operations.sign(ByteArray.fromString(plaintext()));

			return Map.of("signature", signature.encode());
		}

	}

	record VerifyAttributes(@NotBlank String plaintext, @NotBlank String signature) implements KeysetOperationPerformer {

		@NonNull
		@Override
		public Map<String, Object> perform(@NonNull KeysetOperations operations) {
			final boolean valid = operations.verify(
					ByteArray.fromBase64String(signature()),
					ByteArray.fromString(plaintext())
			);

			return Map.of("valid", valid);
		}

	}
}
