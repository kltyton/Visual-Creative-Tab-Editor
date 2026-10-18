import net.neoforged.moddevgradle.legacyforge.dsl.LegacyForgeExtension

plugins {
    `java-library`
    id("net.neoforged.moddev.legacyforge")
    id("me.modmuss50.mod-publish-plugin")
}
apply(from = file("../../gradle/target-conventions/base.gradle.kts"))
val minecraft_version: String by project
val loader_version: String by project
val mod_id: String by project
extensions.configure<LegacyForgeExtension> {
    version = "$minecraft_version-$loader_version"
    validateAccessTransformers.set(true)
    runs {
        create("client") { client() }
        create("server") { server(); programArgument("--nogui") }
        create("data") {
            data()
            programArguments.addAll("--mod", mod_id, "--all", "--output", file("src/generated/resources").absolutePath,
                    *(project.extra["templeDatagenExistingArgs"] as Array<String>))
        }
    }
    mods.create(mod_id) {
        sourceSet(sourceSets.main.get())
        sourceSet(project(":common").extensions.getByType<SourceSetContainer>().named("main").get())
    }
}
apply(from = file("../../gradle/target-conventions/publish.gradle.kts"))

dependencies { add("annotationProcessor", "org.spongepowered:mixin:0.8.5-SNAPSHOT:processor") }
extensions.configure<net.neoforged.moddevgradle.legacyforge.dsl.MixinExtension> {
    config("$mod_id.mixins.json")
    config("$mod_id.forge.mixins.json")
}
apply(from = file("../../gradle/target-conventions/forge-common-mixin.gradle"))
