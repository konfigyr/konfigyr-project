package com.konfigyr.vault.state;

import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.vault.PropertyChanges;
import org.apache.commons.io.output.WriterOutputStream;
import org.eclipse.jgit.diff.Sequence;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.merge.MergeFormatter;
import org.eclipse.jgit.merge.MergeResult;
import org.eclipse.jgit.merge.ResolveMerger;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.io.InputStreamSource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Utility class that provides converter functions to performing mapping of {@code JGit} specific
 * APIs to the Konfigyr domain.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
final class GitConverters {

	private GitConverters() {
		// noop
	}

	/**
	 * Attempts to convert the resolved {@link RevCommit} to a {@link RepositoryState}.
	 *
	 * @param commit the Git commit to convert to a {@link RepositoryState}, cannot be {@literal null}.
	 * @param contents the source of the configuration state contents, cannot be {@literal null}.
	 * @return the converted {@link RepositoryState}, never {@literal null}.
	 */
	static RepositoryState convertToRepositoryState(RevCommit commit, InputStreamSource contents) {
		return RepositoryState.builder()
				.revision(commit.name())
				.summary(commit.getFirstMessageLine())
				.author(resolveAuthor(commit))
				.timestamp(resolveTimestamp(commit))
				.contents(contents)
				.build();
	}

	/**
	 * Attempts to convert the resolved {@link RevCommit} to a {@link RepositoryVersion}.
	 *
	 * @param commit the Git commit to convert to a {@link RepositoryVersion}, cannot be {@literal null}.
	 * @return the converted {@link RepositoryVersion}, never {@literal null}.
	 */
	static RepositoryVersion convertToRepositoryVersion(RevCommit commit) {
		return RepositoryVersion.builder()
				.revision(commit.name())
				.summary(commit.getFirstMessageLine())
				.author(resolveAuthor(commit))
				.timestamp(resolveTimestamp(commit))
				.build();
	}

	/**
	 * Converts the given {@link AuthenticatedPrincipal} to a {@link PersonIdent}.
	 *
	 * @param principal the authenticated principal to convert, cannot be {@literal null}.
	 * @return the person ident, never {@literal null}.
	 */
	static PersonIdent convertToPersonIdent(AuthenticatedPrincipal principal) {
		return new PersonIdent(
				principal.getDisplayName().orElseGet(principal),
				principal.getEmail().orElseGet(principal),
				Instant.now(),
				ZoneOffset.UTC
		);
	}

	/**
	 * Converts the given {@link PropertyChanges} to a Git commit message.
	 *
	 * @param changes the property changes, cannot be {@literal null}.
	 * @return the commit message, never {@literal null}.
	 */
	static String convertToCommitMessage(PropertyChanges changes) {
		final StringBuilder builder = new StringBuilder(changes.subject());

		if (StringUtils.hasText(changes.description())) {
			builder.append("\n\n").append(changes.description());
		}

		return builder.toString();
	}

	static MergeOutcome convertToConflictingOutcome(PersonIdent author, ResolveMerger merger, String source, String target) throws IOException {
		final MergeFormatter formatter = new MergeFormatter();

		final MergeResult<? extends Sequence> result = merger.getMergeResults()
				.get(GitStateRepository.CONFIGURATION_STATE_FILE_NAME);

		if (result == null) {
			return MergeOutcome.unknown(target, formatPerson(author));
		}

		final StringWriter writer = new StringWriter();
		final WriterOutputStream.Builder builder = WriterOutputStream.builder()
				.setCharset(StandardCharsets.UTF_8)
				.setBufferSize(1024)
				.setBufferSizeMax(2048)
				.setWriter(writer);

		try (WriterOutputStream output = builder.get()) {
			formatter.formatMerge(output, result, "Base", source, target, StandardCharsets.UTF_8);
		}

		return MergeOutcome.conflicting(target, author.toExternalString(), writer.toString());
	}

	/**
	 * Formats the Git person identifier into an author string.
	 *
	 * @param person the git person identifer, never {@literal null}.
	 * @return the formated author string, never {@literal null}.
	 */
	static String formatPerson(PersonIdent person) {
		final StringBuilder builder = new StringBuilder();
		PersonIdent.appendSanitized(builder, person.getName());
		builder.append(" <");
		PersonIdent.appendSanitized(builder, person.getEmailAddress());
		builder.append(">");
		return builder.toString();
	}

	private static String resolveAuthor(RevCommit commit) {
		String author = null;

		if (commit.getAuthorIdent() != null) {
			author = formatPerson(commit.getAuthorIdent());
		}

		if (author == null && commit.getCommitterIdent() != null) {
			author = formatPerson(commit.getCommitterIdent());
		}

		if (author == null) {
			author = "<unknown>";
		}

		return author;
	}

	private static OffsetDateTime resolveTimestamp(RevCommit commit) {
		ZoneOffset zone = null;

		if (commit.getAuthorIdent() != null) {
			zone = commit.getAuthorIdent().getZoneOffset();
		}

		if (zone == null && commit.getCommitterIdent() != null) {
			zone = commit.getCommitterIdent().getZoneOffset();
		}

		if (zone == null) {
			zone = ZoneOffset.UTC;
		}

		return Instant.ofEpochSecond(commit.getCommitTime()).atOffset(zone);
	}

}
