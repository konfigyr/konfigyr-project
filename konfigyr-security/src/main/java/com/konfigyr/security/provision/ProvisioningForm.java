package com.konfigyr.security.provision;

import com.konfigyr.namespace.NamespaceType;
import com.konfigyr.security.provisioning.ProvisioningHints;
import com.konfigyr.support.FullName;
import com.konfigyr.support.Slug;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.PropertyMapper;

import java.net.URI;

/**
 * Type that is used by the {@link ProvisioningController} to collect the values from the
 * account provisioning form.
 *
 * @author Vladimir Spasic
 * @see Provisioner
 **/
@Data
class ProvisioningForm {

	@NotEmpty
	@Email
	private String email;

	@URL
	private String avatar;

	@Length(min = 2, max = 60)
	private String firstName;

	@Length(min = 2, max = 60)
	private String lastName;

	@NotEmpty
	private String namespace;

	@NotNull
	private NamespaceType type;

	static ProvisioningForm from(@NotNull ProvisioningHints hints) {
		final PropertyMapper mapper = PropertyMapper.get();
		final ProvisioningForm form = new ProvisioningForm();
		mapper.from(hints::getEmail).whenHasText().to(form::setEmail);
		mapper.from(hints::getAvatar).whenNonNull().as(URI::toString).to(form::setAvatar);
		mapper.from(hints::getName).whenNonNull().as(FullName::firstName).whenHasText().to(form::setFirstName);
		mapper.from(hints::getName).whenNonNull().as(FullName::lastName).whenHasText().to(form::setLastName);
		mapper.from(hints::getNamespace).whenNonNull().as(Slug::get).to(form::setNamespace);
		mapper.from(hints::getType).whenNonNull().to(form::setType);
		return form;
	}
}
