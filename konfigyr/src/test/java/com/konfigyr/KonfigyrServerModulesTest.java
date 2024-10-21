package com.konfigyr;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.lang.NonNull;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class KonfigyrServerModulesTest {

	private final ApplicationModules modules = ApplicationModules.of(
			KonfigyrApplication.class,
			DescribedPredicate.or(
					ignore("com.konfigyr.data"),
					ignore("com.konfigyr.jooq"),
					ignore("com.konfigyr.io"),
					ignore("com.konfigyr.security"),
					ignore("com.konfigyr.support"),
					ignore("com.konfigyr.web")
			)
	);

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

	private static DescribedPredicate<JavaClass> ignore(@NonNull String name) {
		return DescribedPredicate.describe(
				"Ignoring '" + name + "' package",
				type -> type.getPackage().getName().startsWith(name)
		);
	}

}
