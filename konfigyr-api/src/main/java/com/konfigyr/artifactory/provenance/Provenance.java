package com.konfigyr.artifactory.provenance;

import com.konfigyr.io.ByteArray;
import com.konfigyr.version.Version;
import org.jspecify.annotations.NonNull;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;

/**
 * Represents the provenance metadata for a single, unique {@link com.konfigyr.artifactory.PropertyMetadata}
 * that defines a Spring Boot configuration property.
 * <p>
 * This record captures the lifecycle of a property's usage across different Spring Boot versions. The core
 * challenge addressed by this record is handling property metadata that can arrive out of chronological order.
 * For example, metadata for version 3.5.6 might be processed before version 3.1.2. The consuming system must be
 * able to correctly determine the first and last versions regardless of the ingestion sequence.
 * <p>
 * This record is designed as a data carrier for these four pieces of information:
 * <ul>
 *     <li>The unique checksum of the property.</li>
 *     <li>The earliest version where the property was observed.</li>
 *     <li>The latest version where the property was observed.</li>
 *     <li>The total number of unique versions that have used this property.</li>
 * </ul>
 *
 * @param checksum The unique SHA-256 checksum of the property. This is used as the primary
 *                 key to identify a specific property across different versions. Can't be {@literal null}.
 * @param firstSeen The first version of Spring Boot where this property was mentioned, can't be {@literal null}.
 * @param lastSeen The last known version of Spring Boot where this property was mentioned, can't be {@literal null}.
 * @param occurrences The total number of unique versions that have included this property. It must be positive.
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see ProvenanceEvaluator
 */
public record Provenance(
		@NonNull ByteArray checksum,
		@NonNull Version firstSeen,
		@NonNull Version lastSeen,
		int occurrences
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 3562369212504524714L;

	public Provenance {
		Assert.isTrue(occurrences > 0, "Property occurrences must be greater than 0");
	}

	@NonNull
	@Override
	public String toString() {
		return "Provenance(checksum=" + checksum.encode() + ", firstSeen=" + firstSeen.get()
				+ ", lastSeen=" + lastSeen.get() + ", occurrences=" + occurrences + ")";
	}
}
