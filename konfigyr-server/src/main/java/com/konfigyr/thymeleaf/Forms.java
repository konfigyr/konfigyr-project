package com.konfigyr.thymeleaf;

import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.spring6.context.IThymeleafBindStatus;
import org.thymeleaf.spring6.util.FieldUtils;
import org.thymeleaf.util.StringUtils;

/**
 * Thymeleaf form support object that can be used to generate unique element identifiers and
 * A11Y labels for input fields.
 *
 * @author Vladimir Spasic
 **/
@RequiredArgsConstructor
public final class Forms {

	static final String OBJECT_NAME = "forms";

	private final IExpressionContext context;

	/**
	 * Generates the {@link FormControl} object for the given field name.
	 *
	 * @param name name of the field or control to be generated, can't be {@literal null}
	 * @return form control support for the field
	 */
	public FormControl control(@NonNull String name) {
		return control(name, null);
	}

	/**
	 * Generates the {@link FormControl} object for the given field name.
	 * <p>
	 * If the help text is present, it would also generate the {@link HelpText} object.
	 *
	 * @param name name of the field or control to be generated, can't be {@literal null}
	 * @param helpText help text for the form control, can be [@literal null}
	 * @return form control support for the field
	 */
	public FormControl control(@NonNull String name, @Nullable String helpText) {
		return control(name, helpText, StringUtils.randomAlphanumeric(8));
	}

	/**
	 * Generates the {@link FormControl} object for the given field name.
	 * <p>
	 * If the help text is present, it would also generate the {@link HelpText} object.
	 *
	 * @param name name of the field or control to be generated, can't be {@literal null}
	 * @param helpText help text for the form control, can be [@literal null}
	 * @param suffix suffix added to the field name to generate the input field identifier
	 * @return form control support for the field
	 */
	public FormControl control(@NonNull String name, @Nullable String helpText, @NonNull String suffix) {
		final IThymeleafBindStatus status = FieldUtils.getBindStatus(context, "*{" + name + "}");
		final String id = FieldUtils.idFromName(name) + '-' + suffix;

		return FormControl.create(id, name, helpText, status);
	}

	/**
	 * Record that encapsulates all required HTML element identifiers and attribute values
	 * needed to create a form control input for a field.
	 *
	 * @param id unique input field identifier
	 * @param name input field name
	 * @param value input field value
	 * @param help help text describing the field control
	 * @param errors validation errors for the field
	 * @param aria aria attributes for the input field
	 */
	public record FormControl(
			@NonNull String id,
			@NonNull String name,
			@NonNull String value,
			@Nullable HelpText help,
			@Nullable Errors errors,
			@Nullable Aria aria
	) {
		static FormControl create(String id, String name, String helpText, IThymeleafBindStatus status) {
			final HelpText help = HelpText.create(id, helpText);
			final Errors errors = Errors.create(id, status.getErrorMessagesAsString(", "));

			return new FormControl(id, name, status.getDisplayValue(), help, errors, Aria.create(help, errors));
		}

		/**
		 * Creates a custom HTML element identifier for the current form control.
		 *
		 * @param suffix the suffix element for which the identifier would be created
		 * @return generated custom HTML element identifier, never {@literal null}
		 */
		@NonNull
		public String idFor(String suffix) {
			return generateId(id, suffix);
		}
	}

	/**
	 * Container used to hold the element identifier and the value for the help text element.
	 *
	 * @param id help text element identifier
	 * @param value actual help text value
	 */
	public record HelpText(@NonNull String id, @Nullable String value) {
		static HelpText create(String id, String value) {
			return new HelpText(generateId(id, "help"), value);
		}
	}

	/**
	 * Container used to hold the element identifier for the validation errors element.
	 * <p>
	 * In case the value for the form control is invalid, it would aggregate all error messages
	 * into one sentence.
	 *
	 * @param id validation errors element identifier
	 * @param value validation error messages
	 */
	public record Errors(@NonNull String id, @Nullable String value) {
		static Errors create(String id, String value) {
			return new Errors(generateId(id, "errors"), value);
		}
	}

	/**
	 * Generates the values for the form input field that would mark as invalid and also
	 * define the <code>aria-describedby</code> based on {@link Errors} and {@link HelpText}
	 * states.
	 *
	 * @param invalid value used fpr the {@code aria-invalid} attribute
	 * @param describedBy value used fpr the {@code aria-describedby} attribute
	 */
	public record Aria(@Nullable Boolean invalid, @Nullable String describedBy) {
		static Aria create(HelpText help, Errors errors) {
			final boolean invalid = !StringUtils.isEmptyOrWhitespace(errors.value());

			final StringBuilder builder = new StringBuilder();

			if (!StringUtils.isEmptyOrWhitespace(help.value())) {
				builder.append(help.id());

				if (invalid) {
					builder.append(' ');
				}
			}

			if (invalid) {
				builder.append(errors.id());
			}

			// do not set it to false as we do not want to show the
			// aria-invalid attribute if the field is valid
			return new Aria(invalid ? true : null, builder.toString());
		}
	}

	private static String generateId(String id, String suffix) {
		return id + "-" + suffix;
	}
}
