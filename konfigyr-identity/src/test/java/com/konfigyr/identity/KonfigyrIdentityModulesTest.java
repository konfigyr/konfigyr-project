package com.konfigyr.identity;

import com.tngtech.archunit.core.domain.JavaClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class KonfigyrIdentityModulesTest {

	private final ApplicationModules modules = ApplicationModules.of(
			KonfigyrIdentityApplication.class,
			JavaClass.Predicates.resideInAnyPackage("com.konfigyr.identity.configuration..")
	);

	@Test
	@DisplayName("verify konfigyr-identity modules")
	void verifyApplicationModules() {
		modules.verify();
	}

	@Test
	@DisplayName("generate konfigyr-identity module documentation")
	void document() {
		new Documenter(modules)
				.writeModuleCanvases()
				.writeModulesAsPlantUml()
				.writeIndividualModulesAsPlantUml();
	}

}
