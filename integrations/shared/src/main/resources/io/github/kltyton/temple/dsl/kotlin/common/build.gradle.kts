import groovy.json.JsonOutput
import groovy.json.JsonGenerator
import org.gradle.api.artifacts.ProjectDependency
import java.util.Properties

plugins { `java-library` }

val commonRoot = buildFile.canonicalFile.parentFile
val templeRoot = commonRoot.parentFile
val identity = Properties().apply { templeRoot.resolve("gradle.properties").reader(Charsets.UTF_8).use { load(it) } }
val loader = rootProject.property("loader").toString()
val minecraft = rootProject.property("minecraft_version").toString()
val targetId = "$loader-$minecraft"
val activeFile = templeRoot.resolve(".temple/active-target")
val active = (if (activeFile.isFile) activeFile.readText().trim() else identity.getProperty("temple_default_target")) == targetId
val javaVersion = rootProject.property("java_version").toString().toInt()
group = identity.getProperty("mod_group_id")
version = identity.getProperty("mod_version")
base.archivesName.set("${identity.getProperty("mod_id")}-common-$targetId")
layout.buildDirectory.set(commonRoot.resolve("build/$targetId"))
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
    withSourcesJar()
}
repositories {
    mavenCentral()
    maven { url = uri("https://repo.spongepowered.org/repository/maven-public") }
    maven { url = uri("https://maven.fabricmc.net/") }
}
dependencies {
    compileOnly("com.google.code.findbugs:jsr305:3.0.1")
    if (javaVersion >= 25) {
        compileOnly("net.fabricmc:sponge-mixin:0.17.3+mixin.0.8.7")
        compileOnly(annotationProcessor("io.github.llamalad7:mixinextras-common:0.5.3"))
    } else if (loader != "fabric") {
        compileOnly("org.spongepowered:mixin:0.8.5")
    }
}

val families = (if (javaVersion >= 25) listOf("components", "extraction")
    else if (javaVersion >= 21) listOf("legacy", "components") else listOf("legacy")).toMutableList()
if (javaVersion < 25 && loader != "neoforge") families.add("text_shadow")
val layers = (listOf(commonRoot.resolve("src/main")) +
    families.map { commonRoot.resolve("src/compat/minecraft/$it") } +
    listOfNotNull(commonRoot.resolve("src/compat/minecraft/$minecraft"),
        commonRoot.resolve("src/compat/$loader"),
        if (javaVersion >= 21) commonRoot.resolve("src/compat/$loader/components") else null,
        if (javaVersion >= 25) commonRoot.resolve("src/compat/$loader/extraction") else null,
        commonRoot.resolve("src/compat/$loader/$minecraft"))).filter { it.isDirectory }
val javaDirectories = layers.map { it.resolve("java") }.filter { it.isDirectory }
val resourceDirectories = layers.asReversed().map { it.resolve("resources") }.filter { it.isDirectory }
val packageName = identity.getProperty("mod_java_package", identity.getProperty("mod_group_id"))
val constants = layout.buildDirectory.get().asFile.resolve("generated/sources/temple")
val constantFile = constants.resolve("${packageName.replace('.', '/')}/BuildInfo.java")
val constantText = "package $packageName;\n\npublic final class BuildInfo {\n" +
        "    public static final String MOD_ID = ${JsonOutput.toJson(identity.getProperty("mod_id"))};\n" +
        "    private BuildInfo() {}\n}\n"
if (!constantFile.isFile || constantFile.readText() != constantText) {
    constantFile.parentFile.mkdirs()
    constantFile.writeText(constantText)
}
sourceSets {
    main {
        java.setSrcDirs(if (active) javaDirectories + constants else emptyList())
        resources.setSrcDirs(if (active) resourceDirectories else emptyList())
    }
    test {
        java.setSrcDirs(if (active) listOf(commonRoot.resolve("src/test/java")) else emptyList())
        resources.setSrcDirs(if (active) listOf(commonRoot.resolve("src/test/resources")) else emptyList())
    }
}
tasks.named<JavaCompile>("compileJava") { source(javaDirectories, constants) }
tasks.named<JavaCompile>("compileTestJava") { source(commonRoot.resolve("src/test/java")) }
tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8"; options.release.set(javaVersion) }
tasks.named<ProcessResources>("processResources") {
    from(resourceDirectories)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    val values = listOf("mod_id", "mod_name", "mod_version", "mod_group_id", "mod_authors", "mod_license", "mod_description")
        .associateWith { identity.getProperty(it) }.toMutableMap<String, Any>()
    values["minecraft_version"] = minecraft
    values["java_version"] = javaVersion
    val json = JsonGenerator.Options().disableUnicodeEscaping().build()
    val escaped = values.mapValues { json.toJson(it.value.toString()).replace("\u007f", "\\u007f").let { encoded -> encoded.substring(1, encoded.length - 1) } }
    inputs.properties(values)
    filesMatching(listOf("*.mixins.json", "pack.mcmeta")) {
        filter { line: String -> Regex("\\$\\{([a-z_]+)\\}").replace(line) { match -> escaped[match.groupValues[1]] ?: match.value } }
    }
}
tasks.named<ProcessResources>("processTestResources") { from(commonRoot.resolve("src/test/resources")) }
tasks.named<Jar>("sourcesJar") {
    from(javaDirectories, constants, resourceDirectories)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
val commonProjectPath = path
val commonSourceSets = sourceSets
val commonCompile = tasks.named<JavaCompile>("compileJava")
val commonAnnotationProcessors = configurations.getByName("annotationProcessor")
rootProject.afterEvaluate {
    val sdk = rootProject.configurations.getByName("compileClasspath").copyRecursive(
            org.gradle.api.specs.Spec { dependency -> !(dependency is ProjectDependency && dependency.path == commonProjectPath) })
    commonSourceSets.main.get().compileClasspath += sdk
    commonSourceSets.test.get().compileClasspath += sdk
    commonCompile.configure {
        options.annotationProcessorPath = files(commonAnnotationProcessors,
            rootProject.tasks.named<JavaCompile>("compileJava").get().options.annotationProcessorPath)
    }
}
extra["templeResourceDirectories"] = resourceDirectories
extra["templeSourceDirectories"] = javaDirectories + constants
apply(from = templeRoot.resolve("gradle/target-conventions/ide-sources.gradle"))
