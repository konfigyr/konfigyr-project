package com.konfigyr.kms;

import com.konfigyr.crypto.tink.TinkAlgorithm;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

/**
 * Enumeration that defines the supported algorithms for {@link KeysetMetadata}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
@RequiredArgsConstructor
public enum KeysetMetadataAlgorithm {

	/**
	 * AES with a 128-bit key in Galois Counter Mode (GCM).
	 */
	AES128_GCM(TinkAlgorithm.AES128_GCM),

	/**
	 * AES with a 256-bit key in Galois Counter Mode (GCM), recommended for encryption operations.
	 */
	AES256_GCM(TinkAlgorithm.AES256_GCM),

	/**
	 * ECIES with a P-256 Curve and AES-128 in Galois Counter Mode (GCM).
	 */
	ECIES_P256_HKDF_HMAC_SHA256_AES128_GCM(TinkAlgorithm.ECIES_P256_HKDF_HMAC_SHA256_AES128_GCM),

	/**
	 * DH-KEM with X25519 EC key agreement, KDF using SHA256 and AES-128 in Galois Counter Mode (GCM).
	 */
	DHKEM_X25519_HKDF_SHA256_HKDF_SHA256_AES_128_GCM(TinkAlgorithm.DHKEM_X25519_HKDF_SHA256_HKDF_SHA256_AES_128_GCM),

	/**
	 * DH-KEM with X25519 EC key agreement, KDF using SHA256 and AES-256 in Galois Counter Mode (GCM).
	 */
	DHKEM_X25519_HKDF_SHA256_HKDF_SHA256_AES_256_GCM(TinkAlgorithm.DHKEM_X25519_HKDF_SHA256_HKDF_SHA256_AES_256_GCM),

	/**
	 * DH-KEM with P-256 EC key agreement, KDF using SHA256 and AES-128 in Galois Counter Mode (GCM).
	 */
	DHKEM_P256_HKDF_SHA256_HKDF_SHA256_AES_128_GCM(TinkAlgorithm.DHKEM_P256_HKDF_SHA256_HKDF_SHA256_AES_128_GCM),

	/**
	 * DH-KEM with P-256 EC key agreement, KDF using SHA256 and AES-256 in Galois Counter Mode (GCM).
	 */
	DHKEM_P256_HKDF_SHA256_HKDF_SHA256_AES_256_GCM(TinkAlgorithm.DHKEM_P256_HKDF_SHA256_HKDF_SHA256_AES_256_GCM),

	/**
	 * EdDSA on the Curve25519 in PureEdDSA mode, which takes raw data as input instead of hashed data.
	 */
	ED25519(TinkAlgorithm.ED25519),

	/**
	 * ECDSA on the P-256 Curve with an SHA-256 digest, recommended algorithm for digital signatures.
	 */
	ECDSA_P256(TinkAlgorithm.ECDSA_P256),

	/**
	 * ECDSA on the P-384 Curve with an SHA-512 digest.
	 */
	ECDSA_P384(TinkAlgorithm.ECDSA_P384),

	/**
	 * ECDSA on the P-521 Curve with a SHA-512 digest.
	 */
	ECDSA_P521(TinkAlgorithm.ECDSA_P521),

	/**
	 * RSASSA-PSS 3072-bit key with an SHA-256 digest, recommended algorithm for digital signatures.
	 */
	RSA_SSA_PSS_3072_SHA256_SHA256_32_F4(TinkAlgorithm.RSA_SSA_PSS_3072_SHA256_SHA256_32_F4),

	/**
	 * RSASSA-PSS 4096-bit key with an SHA-512 digest.
	 */
	RSA_SSA_PSS_4096_SHA512_SHA512_64_F4(TinkAlgorithm.RSA_SSA_PSS_4096_SHA512_SHA512_64_F4),

	/**
	 * RSASSA-PKCS1 v1_5 with a 3072-bit key and an SHA-256 digest.
	 */
	RSA_SSA_PKCS1_3072_SHA256_F4(TinkAlgorithm.RSA_SSA_PKCS1_3072_SHA256_F4),

	/**
	 * RSASSA-PKCS1 v1_5 with a 4096bit key and am SHA-512 digest.
	 */
	RSA_SSA_PKCS1_4096_SHA512_F4(TinkAlgorithm.RSA_SSA_PKCS1_4096_SHA512_F4);

	private final TinkAlgorithm algorithm;

	TinkAlgorithm get() {
		return algorithm;
	}
}
