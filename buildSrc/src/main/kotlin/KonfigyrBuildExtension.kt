import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import javax.inject.Inject

/**
 * Project-level extension that exposes lazy build properties derived from the environment.
 * Registered on every subproject by the root build script under the name "konfigyr".
 *
 * Access in any subproject build file via:
 *   the<KonfigyrBuildExtension>().ci
 *   the<KonfigyrBuildExtension>().dockerImageTag
 */
abstract class KonfigyrBuildExtension @Inject constructor(
    providers: ProviderFactory,
    projectName: String,
    projectVersion: String,
) {
    /** True when running inside GitHub Actions (CI=true is set automatically by the runner). */
    val ci: Provider<Boolean> = providers.environmentVariable("CI")
        .map { it.toBoolean() }
        .orElse(false)

    /** True when the NIGHTLY environment variable is set. */
    val nightly: Provider<Boolean> = providers.environmentVariable("NIGHTLY")
        .map { it.toBoolean() }
        .orElse(false)

    /** Full Docker image name including tag — latest when NIGHTLY=true, version otherwise. */
    val dockerImageTag: Provider<String> = nightly
        .map { if (it) "latest" else projectVersion }
        .map { "konfigyr/$projectName:$it" }
}
