import groovy.json.JsonGenerator
import java.util.Properties

val templeRoot = file("../..").canonicalFile
val shared = Properties().apply { templeRoot.resolve("gradle.properties").reader(Charsets.UTF_8).use { load(it) } }
shared.forEach { key, value -> if (!project.hasProperty(key.toString())) extra[key.toString()] = value.toString() }
fun setting(name: String) = project.property(name).toString()
val modId = setting("mod_id")
require(Regex("[a-z][a-z0-9_]{1,63}").matches(modId) &&
        Regex("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+").matches(setting("mod_group_id"))) { "Invalid mod identity" }
group = setting("mod_group_id")
version = setting("mod_version")
val loader = setting("loader")
val javaVersion = setting("java_version").toInt()
extensions.configure<BasePluginExtension> { archivesName.set("$modId-$loader-${setting("minecraft_version")}") }
extensions.configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
    withSourcesJar()
    withJavadocJar()
}
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaVersion)
}
evaluationDependsOn(":common")
val commonProject = project(":common")
dependencies { add("implementation", project(":common")) }
@Suppress("UNCHECKED_CAST")
val commonResources = commonProject.extra["templeResourceDirectories"] as List<File>
@Suppress("UNCHECKED_CAST")
val commonSources = commonProject.extra["templeSourceDirectories"] as List<File>
val resourceLayers = listOf(file("src/main/resources")) + commonResources
extra["templeDatagenExistingArgs"] = resourceLayers.filter { it.isDirectory }.asReversed()
        .flatMap { listOf("--existing", it.absolutePath) }.toTypedArray()
extensions.configure<SourceSetContainer> {
    named("main") {
        resources.setSrcDirs(listOf(resourceLayers.first(), file("src/generated/resources")))
    }
}
tasks.named<Jar>("sourcesJar") {
    from(commonSources, commonResources)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
tasks.named<ProcessResources>("processResources") {
    from(commonResources)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    val raw = (listOf("mod_id", "mod_name", "mod_license", "mod_version", "mod_authors", "mod_description",
            "mod_group_id", "minecraft_version", "minecraft_version_range", "loader_version", "loader_version_range",
            "java_version").associateWith { setting(it) } + mapOf(
            "fml_version_range" to (project.findProperty("fml_version_range")?.toString() ?: ""),
            "mod_java_package" to (project.findProperty("mod_java_package")?.toString() ?: setting("mod_group_id")))).toMutableMap()
    raw.putAll(mapOf("version" to setting("mod_version"), "group" to setting("mod_group_id"), "license" to setting("mod_license"),
        "mod_author" to setting("mod_authors"), "description" to setting("mod_description"),
        "credits" to (findProperty("credits") ?: "").toString(),
        "fabric_version" to (findProperty("fabric_version") ?: "").toString(),
        "fabric_loader_version" to (findProperty("fabric_loader_version") ?: "").toString(),
        "forge_version" to (findProperty("forge_version") ?: "").toString(),
        "forge_loader_version_range" to (findProperty("forge_loader_version_range") ?: "").toString(),
        "neoforge_version" to (findProperty("neoforge_version") ?: "").toString(),
        "neoforge_loader_version_range" to (findProperty("neoforge_loader_version_range") ?: "").toString()))
    val json = JsonGenerator.Options().disableUnicodeEscaping().build()
    val escaped = raw.mapValues { json.toJson(it.value).replace("\u007f", "\\u007f").let { encoded -> encoded.substring(1, encoded.length - 1) } }
    inputs.properties(raw)
    filesMatching(listOf("fabric.mod.json", "META-INF/mods.toml", "META-INF/neoforge.mods.toml", "pack.mcmeta", "*.mixins.json")) {
        filter { line: String -> Regex("\\$\\{([a-z_]+)\\}").replace(line) { match -> escaped[match.groupValues[1]] ?: match.value } }
    }
}
tasks.named<Jar>("jar") {
    manifest.attributes("Implementation-Version" to project.version.toString())
    from(commonProject.extensions.getByType<SourceSetContainer>().named("main").map { it.output })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(templeRoot.resolve("LICENSE")) { rename { "LICENSE_$modId" } }
}
repositories { mavenCentral() }
val localJars = fileTree("libs") { include("*.jar"); exclude("*-sources.jar", "*-javadoc.jar") }
dependencies { add(if (loader == "fabric" && javaVersion < 25) "modImplementation" else "implementation", localJars) }

repositories {
    maven { url = uri("https://repo.spongepowered.org/repository/maven-public") }
    maven { url = uri("https://maven.fabricmc.net/") }
    maven { url = uri("https://maven.parchmentmc.org/") }
    maven { url = uri("https://maven.neoforged.net/releases") }
}
tasks.withType<Javadoc>().configureEach {
    source(commonSources)
    classpath += commonProject.extensions.getByType<SourceSetContainer>().getByName("main").compileClasspath
}
