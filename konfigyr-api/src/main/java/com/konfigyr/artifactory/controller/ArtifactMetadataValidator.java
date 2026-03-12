package com.konfigyr.artifactory.controller;

import com.konfigyr.artifactory.ArtifactCoordinates;
import com.konfigyr.artifactory.DefaultArtifactMetadata;
import com.konfigyr.version.Version;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;
import org.hibernate.validator.cfg.ConstraintMapping;
import org.hibernate.validator.cfg.defs.NotBlankDef;
import org.hibernate.validator.cfg.defs.NotEmptyDef;
import org.jspecify.annotations.NullMarked;
import org.springframework.util.StringUtils;
import org.springframework.util.function.SingletonSupplier;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

import java.util.function.Supplier;

@NullMarked
@RequiredArgsConstructor
final class ArtifactMetadataValidator {

	private static final Supplier<Validator> validator = SingletonSupplier.of(
			ArtifactMetadataValidator::createHibernateValidator
	);

	ArtifactMetadataValidator(String groupId, String artifactId, String version) {
		this(ArtifactCoordinates.of(groupId, artifactId, version));
	}

	private final ArtifactCoordinates coordinates;

	void validate(DefaultArtifactMetadata metadata, BindingResult errors) throws BindException {
		validator.get().validate(metadata, errors);

		if (StringUtils.hasText(metadata.groupId()) && !coordinates.groupId().equals(metadata.groupId())) {
			reject(errors, "groupId", coordinates.groupId());
		}
		if (StringUtils.hasText(metadata.artifactId()) && !coordinates.artifactId().equals(metadata.artifactId())) {
			reject(errors, "artifactId", coordinates.artifactId());
		}
		if (StringUtils.hasText(metadata.version()) && !coordinates.version().equals(Version.of(metadata.version()))) {
			reject(errors, "version", coordinates.artifactId());
		}

		if (errors.hasErrors()) {
			throw new BindException(errors);
		}
	}

	private static void reject(Errors errors, String field, String expected) {
		final String defaultMessage = "The field '%s' of artifact metadata must match '%s' but was: '%s'"
				.formatted(field, expected, errors.getFieldValue(field));

		errors.rejectValue(field, "artifactory.validation.metadata.groupId.mismatch", defaultMessage);
	}

	private static Validator createHibernateValidator() {
		final HibernateValidatorConfiguration configuration = Validation.byProvider(HibernateValidator.class).configure();
		final ConstraintMapping constraintMapping = configuration.createConstraintMapping();

		constraintMapping.type(DefaultArtifactMetadata.class)
				.field("groupId")
					.constraint(new NotBlankDef())
				.field("artifactId")
					.constraint(new NotBlankDef())
				.field("version")
					.constraint(new NotBlankDef())
				.field("properties")
					.constraint(new NotEmptyDef());

		try (ValidatorFactory factory = configuration.addMapping(constraintMapping).buildValidatorFactory()) {
			return new SpringValidatorAdapter(factory.getValidator());
		}
	}
}
