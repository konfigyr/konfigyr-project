package com.konfigyr;

import com.konfigyr.crypto.Algorithm;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class KonfigyrApplicationModuleTest {

	private final ApplicationModules modules = ApplicationModules.of(
			KonfigyrApplication.class,
			DescribedPredicate.or(
					JavaClass.Predicates.resideInAnyPackage(
							"com.konfigyr.data..",
							"com.konfigyr.io..",
							"com.konfigyr.security..",
							"com.konfigyr.support..",
							"com.konfigyr.test..",
							"com.konfigyr.web.."
					),
					JavaClass.Predicates.assignableTo(Algorithm.class)
			)
	);

	@Test
	@DisplayName("verify konfigyr-api modules")
	void verifyApplicationModules() {
		modules.verify();
	}

	@Test
	@DisplayName("generate konfigyr-api module documentation")
	void document() {
		new Documenter(modules).writeDocumentation();
	}

}
