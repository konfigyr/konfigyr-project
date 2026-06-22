package com.konfigyr.artifactory.ownership;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class SourceCodeHostTest {

	@ParameterizedTest(name = "{0} → {1}")
	@CsvSource({
			"io.github.alice,    GITHUB",
			"io.gitlab.alice,    GITLAB",
			"io.bitbucket.alice, BITBUCKET"
	})
	@DisplayName("should resolve host from groupId")
	void fromGroupId(String groupId, SourceCodeHost expected) {
		assertThat(SourceCodeHost.fromGroupId(groupId)).contains(expected);
	}

	@ParameterizedTest(name = "{0} → empty")
	@ValueSource(strings = {
			"com.example",
			"io.github",
			"io.unknown.alice",
			"io.github.alice.utils",
			"github.io.alice",
			"io.github."
	})
	@DisplayName("should return empty for groupIds that do not match io.{host}.{username}")
	void fromGroupIdUnknown(String groupId) {
		assertThat(SourceCodeHost.fromGroupId(groupId)).isEmpty();
	}

	@ParameterizedTest(name = "{0}: {1} + {2} → {3}")
	@CsvSource({
			"GITHUB,    io.github.alice,    abc, https://api.github.com/repos/alice/kfgyr-abc",
			"GITLAB,    io.gitlab.alice,    abc, https://gitlab.com/api/v4/projects/alice%2Fkfgyr-abc",
			"BITBUCKET, io.bitbucket.alice, abc, https://api.bitbucket.org/2.0/repositories/alice/kfgyr-abc"
	})
	@DisplayName("should build repository URL from groupId and token per host")
	void toURI(SourceCodeHost host, String groupId, String token, String expectedUrl) {
		assertThat(host.toURI(groupId, token)).hasToString(expectedUrl);
	}

	@Test
	@DisplayName("should throw for groupId with more than 3 components")
	void toURIInvalidGroupId() {
		assertThatIllegalStateException()
				.isThrownBy(() -> SourceCodeHost.GITHUB.toURI("io.github.alice.utils", "abc"));
	}
}
