package com.konfigyr.feature;

import com.konfigyr.namespace.NamespaceFeatures;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeaturesTest {

	final FeatureDefinitions definitions = new FeatureDefinitions(
			TestFeatureDefinitions.LIMITED,
			TestFeatureDefinitions.RATE_LIMITED
	);

	@Mock(strictness = Mock.Strictness.LENIENT)
	FeatureValuesProvider empty;

	@Mock
	FeatureValuesProvider provider;

	Features features;

	@BeforeEach
	void setup() {
		doReturn(FeatureValues.empty()).when(empty).get("konfigyr");

		features = new ProviderFeatures(definitions, List.of(provider, empty));
	}

	@Test
	@DisplayName("should fail to resolve feature value for unknown feature definition")
	void shouldResolveUnknownFeatureDefinition() {
		final var unknown = FeatureDefinition.of("unknown", LimitedFeatureValue.class);

		assertThatIllegalArgumentException()
				.isThrownBy(() -> features.get("konfigyr", unknown))
				.withMessage("You attempted to obtain a feature value for feature definition that is not registered.")
				.withNoCause();

		verifyNoInteractions(empty);
		verifyNoInteractions(provider);
	}

	@Test
	@DisplayName("should return an empty feature value for feature definition when provider has no values")
	void shouldResolveEmptyValue() {
		doReturn(FeatureValues.empty()).when(provider).get("konfigyr");

		assertThat(features.get("konfigyr", TestFeatureDefinitions.LIMITED))
				.isEmpty();

		verify(provider).get("konfigyr");
		verify(empty).get("konfigyr");
	}

	@Test
	@DisplayName("should return feature value for feature definition from provider")
	void shouldResolveFeatureValue() {
		final var values = FeatureValues.builder()
				.add(TestFeatureDefinitions.LIMITED, FeatureValue.limited(5))
				.build();

		doReturn(values).when(provider).get("konfigyr");

		assertThat(features.get("konfigyr", TestFeatureDefinitions.LIMITED))
				.isPresent()
				.get(InstanceOfAssertFactories.type(LimitedFeatureValue.class))
				.isEqualTo(FeatureValue.limited(5));

		verify(provider).get("konfigyr");
		verify(empty).get("konfigyr");
	}

	@Test
	@DisplayName("should resolve Beta feature values")
	void shouldResolveBetaFeatures() {
		final var values = new BetaFeatureValuesProvider().get("any");

		assertThat(values.get(NamespaceFeatures.MEMBERS_COUNT))
				.hasValue(FeatureValue.unlimited());
	}

}
