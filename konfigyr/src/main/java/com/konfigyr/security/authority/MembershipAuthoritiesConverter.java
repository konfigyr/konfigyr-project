package com.konfigyr.security.authority;

import com.konfigyr.account.Membership;
import com.konfigyr.account.Memberships;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.function.SingletonSupplier;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * Spring {@link Converter} implementation that would convert the {@link Memberships} into
 * a collection of {@link GrantedAuthority granted authorities} that can be assigned to an
 * {@link org.springframework.security.core.Authentication}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public final class MembershipAuthoritiesConverter implements Converter<Memberships, Collection<GrantedAuthority>> {

	private static final Supplier<MembershipAuthoritiesConverter> instance =
			SingletonSupplier.of(MembershipAuthoritiesConverter::new);

	public static MembershipAuthoritiesConverter getInstance() {
		return instance.get();
	}

	private MembershipAuthoritiesConverter() {
	}

	@NonNull
	@Override
	public Collection<GrantedAuthority> convert(@NonNull Memberships memberships) {
		return memberships.map(this::convert).toSet();
	}

	private GrantedAuthority convert(@NonNull Membership membership) {
		final String role = membership.role().name().toLowerCase();
		return new SimpleGrantedAuthority(membership.namespace() + ":" + role);
	}

}
