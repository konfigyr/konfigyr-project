package com.konfigyr;

import com.tngtech.archunit.base.DescribedPredicate;
import org.springframework.lang.NonNull;
import org.springframework.modulith.core.ApplicationModuleDetectionStrategy;
import org.springframework.modulith.core.JavaPackage;

import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Implementation of the {@link ApplicationModuleDetectionStrategy} that would ignore the
 * Konfigyr 3rd party libraries and generated Java packages when Spring Moduliths creates
 * the modules.
 *
 * @author : Vladimir Spasic
 * @since : 26.03.24, Tue
 **/
class KonfigyrApplicationModuleDetectionStrategy implements ApplicationModuleDetectionStrategy {

    private static final DescribedPredicate<JavaPackage> IGNORED_PACKAGES = DescribedPredicate.or(
            ignorePackage("com.konfigyr.crypto"),
            ignorePackage("com.konfigyr.data"),
            ignorePackage("com.konfigyr.io")
    );

    private final ApplicationModuleDetectionStrategy delegate = ApplicationModuleDetectionStrategy.directSubPackage();

    @NonNull
    @Override
    public Stream<JavaPackage> getModuleBasePackages(@NonNull JavaPackage basePackage) {
        return delegate.getModuleBasePackages(basePackage).filter(Predicate.not(IGNORED_PACKAGES));
    }

    private static DescribedPredicate<JavaPackage> ignorePackage(String name) {
        return DescribedPredicate.describe(
                "Ignoring '" + name + "' package",
                type -> type.getName().startsWith(name)
        );
    }

}
