package com.konfigyr;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class KonfigyrServerModulesTest {

	private final ApplicationModules modules = ApplicationModules.of(KonfigyrApplication.class);

	@Test
	@DisplayName("verify konfigyr-server modules")
	void verifyApplicationModules() {
		modules.verify().forEach(System.out::println);
	}

	@Test
	@DisplayName("generate konfigyr-server module documentation")
	void document() {
		new Documenter(modules)
				.writeModuleCanvases()
				.writeModulesAsPlantUml()
				.writeIndividualModulesAsPlantUml();
	}

}