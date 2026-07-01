import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import javax.inject.Inject

/**
 * Cacheable task that runs an NPM command. Declare [sources] for all input files,
 * [outputDir] for tasks that produce a directory (e.g. build), and [outputFile] for
 * tasks whose only output is a completion stamp (e.g. test, install).
 *
 * The npm executable defaults to the [node.npm][ProviderFactory.gradleProperty] Gradle
 * property and falls back to "npm" on the system PATH. Override [executable] per task
 * when a different binary is needed.
 *
 * To opt a specific registration out of the build cache (e.g. npmInstall, which reads
 * from the network and does not declare node_modules as an output), add:
 *   outputs.cacheIf { false }
 */
@CacheableTask
abstract class NpmExec @Inject constructor(
    private val execOperations: ExecOperations,
    providers: ProviderFactory,
) : DefaultTask() {

    @get:Input
    abstract val executable: Property<String>

    @get:Input
    abstract val args: ListProperty<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:OutputDirectory
    @get:Optional
    abstract val outputDir: DirectoryProperty

    @get:OutputFile
    @get:Optional
    abstract val outputFile: RegularFileProperty

    init {
        executable.convention(providers.gradleProperty("node.npm").orElse("/usr/local/bin/npm"))
    }

    @TaskAction
    fun run() {
        execOperations.exec {
            commandLine(listOf(this@NpmExec.executable.get()) + this@NpmExec.args.get())
        }
        outputFile.orNull?.asFile?.writeText("ok")
    }
}
