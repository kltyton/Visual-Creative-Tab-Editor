import net.minecraftforge.gradle.userdev.UserDevExtension

plugins {
    `java-library`
    `maven-publish`
    id("org.spongepowered.mixin") version "0.7-SNAPSHOT"
    id("net.minecraftforge.gradle")
    id("me.modmuss50.mod-publish-plugin")
}
apply(from = file("../../gradle/target-conventions/base.gradle.kts"))
val minecraft_version: String by project
val loader_version: String by project
val mod_id: String by project
extensions.configure<UserDevExtension> {
    mappings("official", minecraft_version)
    copyIdeResources = true
    reobf = false
    runs {
        configureEach {
            workingDirectory(file("run"))
            mods.create(mod_id) {
                source(sourceSets.main.get())
                source(project(":common").extensions.getByType<SourceSetContainer>().named("main").get())
            }
        }
        create("client")
        create("server") { args("--nogui") }
        create("data") {
            args("--mod", mod_id, "--all", "--output", file("src/generated/resources").absolutePath,
                    *(project.extra["templeDatagenExistingArgs"] as Array<String>))
        }
    }
}
dependencies { add("minecraft", "net.minecraftforge:forge:$minecraft_version-$loader_version") }
apply(from = file("../../gradle/target-conventions/publish.gradle.kts"))

dependencies { add("annotationProcessor", "org.spongepowered:mixin:0.8.5-SNAPSHOT:processor") }
tasks.withType<net.minecraftforge.gradle.common.util.runs.MinecraftRunTask>().configureEach {
    dependsOn(project(":common").tasks.named("classes"))
}
extra["templeMixinPluginVersion"] = org.spongepowered.asm.gradle.plugins.MixinGradlePlugin.getVERSION()
apply(from = file("../../gradle/target-conventions/forgegradle-common-mixin.gradle"))
