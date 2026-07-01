import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import javax.inject.Inject

/**
 * Cacheable task that runs an NPM command. Declare [sources] for all input files,
 * [outputDir] for tasks that produce a directory (e.g. build), and [outputFile] for
 * tasks whose only output is a completion stamp (e.g. test, install).
 *
 * To opt a specific registration out of the build cache (e.g. npmInstall, which reads
 * from the network and does not declare node_modules as an output), add:
 *   outputs.cacheIf { false }
 */
@CacheableTask
abstract class NpmExec @Inject constructor(
    private val execOperations: ExecOperations,
) : DefaultTask() {

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

    @TaskAction
    fun run() {
        execOperations.exec {
            commandLine(buildList { add("npm"); addAll(this@NpmExec.args.get()) })
        }
        outputFile.orNull?.asFile?.writeText("ok")
    }
}
