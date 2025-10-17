package com.konfigyr.version;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

interface VersionParser {

	Pattern SEMVER_PATTERN = Pattern.compile(
			"^v?(\\d+)\\.(\\d+)\\.(\\d+)(?:-([0-9a-zA-Z.-]+))?(?:\\+([0-9a-zA-Z.-]+))?$"
	);

	Pattern CALVER_PATTERN = Pattern.compile(
			"^(\\d{1,4})[.\\-_](\\d{1,4})(?:[.\\-_](\\d{1,4}))?(?:[.\\-_]?([a-zA-Z0-9]+))?$"
	);

	static Version parse(String value) {
		Assert.hasText(value, "Version must not be empty");

		final String trimmed = StringUtils.trimAllWhitespace(value);

		Matcher semverMatcher = SEMVER_PATTERN.matcher(trimmed);
		if (semverMatcher.matches()) {
			try {
				return new SemanticVersion(
						Integer.parseInt(semverMatcher.group(1)),
						Integer.parseInt(semverMatcher.group(2)),
						Integer.parseInt(semverMatcher.group(3)),
						MetadataVersion.of(semverMatcher.group(4)),
						MetadataVersion.of(semverMatcher.group(5)),
						trimmed
				);
			} catch (NumberFormatException e) {
				// ignore, check if calendar version...
			}
		}

		Matcher calverMatcher = CALVER_PATTERN.matcher(trimmed);
		if (calverMatcher.matches()) {
			try {
				String year = calverMatcher.group(1);
				int primary = Integer.parseInt(calverMatcher.group(2));

				String patchStr = calverMatcher.group(3);
				int secondary = patchStr != null ? Integer.parseInt(patchStr) : -1;

				String modifier = calverMatcher.group(4);

				// Heuristic check and conversion to Year
				// - year is 2 digits (e.g., 2024)
				// - year is 2 digits (e.g., 24)
				// - year is zero padded or has 3 digits (e.g., 0024 or 180)
				final Year y;
				if (year.length() == 4) {
					y = Year.parse(year, DateTimeFormatter.ofPattern("yyyy"));
				} else if (year.startsWith("0") || year.length() == 3) {
					y = Year.of(Integer.parseInt(year));
				} else if (year.length() == 2) {
					y = Year.parse(year, DateTimeFormatter.ofPattern("yy"));
				} else {
					throw new NumberFormatException("Invalid year '" + year + "' in '" + value + "'");
				}

				return new CalendarVersion(y, primary, secondary, modifier, trimmed);
			} catch (NumberFormatException | DateTimeParseException e) {
				// ignore, it is an unknown version
			}
		}

		return new UnknownVersion(trimmed);
	}
}
