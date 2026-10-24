import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import groovy.json.JsonSlurper

plugins {
    kotlin("jvm") version "2.2.21"
    id("earth.terrarium.cloche") version "0.18.10"
    id("dev.mixinmcp.decompile") version "1.5.0"
    `maven-publish`
}
version = providers.gradleProperty("mod_version").get()
group = providers.gradleProperty("maven_group").get()
base { archivesName.set(providers.gradleProperty("archives_base_name")) }
val targetMinecraft = providers.gradleProperty("minecraft_version").get()
val fabricLoader = providers.gradleProperty("loader_version").get()
val neoForgeLoader = providers.gradleProperty("neoforge_loader_version").get()
val fabricApiVersion = providers.gradleProperty("fabric_version").get().substringBefore('+')
val fabricKotlinVersion = providers.gradleProperty("fabric_kotlin_version").get()
val kotlinForForgeVersion = providers.gradleProperty("kotlin_for_forge_version").get()
val originsVersion = providers.gradleProperty("overgrown_origins_version").get()
val opoliVersion = providers.gradleProperty("opoli_version").get()
val fzzyVersion = providers.gradleProperty("fzzy_config_version").get()
val aeaVersion = providers.gradleProperty("aea_version").get()
fun releasedModrinth(project: String, property: String) = "maven.modrinth:$project:${providers.gradleProperty(property).get()}"
repositories {
    cloche {
        librariesMinecraft()
        main()
        mavenFabric()
        mavenForge()
        mavenNeoforgedMeta()
        mavenNeoforged()
    }
    mavenCentral()
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") }
        filter { includeGroup("maven.modrinth") }
    }
    maven("https://maven.fzzyhmstrs.me/")
    maven("https://thedarkcolour.github.io/KotlinForForge/")
}
cloche {
    minecraftVersion = targetMinecraft
    metadata {
        modId = "andromeda_origins"
        name = "Andromeda Origins"
        description = "Thirteen custom Origins, Champion variants, and a rich Origin reader for Andromeda MC."
        license = "BML-1.0"
        icon = "assets/andromeda_origins/icon.png"
        author { name = "jaselumena" }
        author { name = "pokesmells" }
        author { name = "aulatris" }
        sources = "https://github.com/BareMinimumStudios/andromeda-origins"
        issues = "https://github.com/BareMinimumStudios/andromeda-origins/issues"
    }
    common {
        mappings { official() }
        mixins.from("src/main/andromeda_origins.mixins.json", "src/main/andromeda_origins.compat.mixins.json")
    }
    fabric {
        mixins.from("src/fabric/andromeda_origins.fabric-diagnostics.mixins.json")
        loaderVersion = fabricLoader
        includedClient()
        runs { server(); client() }
        dependencies {
            fabricApi(fabricApiVersion)
            modImplementation("net.fabricmc:fabric-language-kotlin:$fabricKotlinVersion")
            modImplementation(releasedModrinth("i2NQbLvK", "opoli_version_id"))
            modImplementation(releasedModrinth("tYolxBPN", "overgrown_origins_version_id"))
            modImplementation(releasedModrinth("additionalentityattributes", "aea_fabric_version_id"))
            include(releasedModrinth("additionalentityattributes", "aea_fabric_version_id"))
            modImplementation("me.fzzyhmstrs:fzzy_config:$fzzyVersion")
        }
        metadata {
            entrypoint("main") { value = "andromeda.origins.platform.FabricEntrypoint" }
            entrypoint("client") { value = "andromeda.origins.platform.FabricClientEntrypoint" }
            dependencies {
                dependency { modId = "fabric-api" }
                dependency { modId = "fabric-language-kotlin"; version(fabricKotlinVersion) }
                dependency { modId = "origins"; version(originsVersion) }
                dependency { modId = "apoli"; version(opoliVersion) }
                dependency { modId = "fzzy_config"; version(fzzyVersion) }
                dependency { modId = "additionalentityattributes"; version(aeaVersion) }
            }
        }
    }
    neoforge {
        loaderVersion = neoForgeLoader
        runs { server(); client() }
        dependencies {
            modImplementation("thedarkcolour:kotlinforforge-neoforge:$kotlinForForgeVersion")
            modImplementation(releasedModrinth("i2NQbLvK", "opoli_neoforge_version_id"))
            modImplementation(releasedModrinth("tYolxBPN", "overgrown_origins_neoforge_version_id"))
            modImplementation(releasedModrinth("additionalentityattributes", "aea_neoforge_version_id"))
            include(releasedModrinth("additionalentityattributes", "aea_neoforge_version_id"))
            modImplementation("me.fzzyhmstrs:fzzy_config:$fzzyVersion+neoforge")
        }
        metadata {
            modLoader = "kotlinforforge"
            loaderVersion { start = kotlinForForgeVersion }
            blurLogo = false
            dependencies {
                dependency { modId = "kotlinforforge"; version(kotlinForForgeVersion) }
                dependency { modId = "origins"; version(originsVersion) }
                dependency { modId = "apoli"; version(opoliVersion) }
                dependency { modId = "fzzy_config"; version("$fzzyVersion+neoforge") }
                dependency { modId = "additionalentityattributes"; version(aeaVersion) }
            }
        }
    }
}
kotlin { jvmToolchain(21) }
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.add("-Xlint:deprecation")
}
tasks.withType<Jar>().configureEach { duplicatesStrategy = DuplicatesStrategy.EXCLUDE; from("LICENSE") }
// Version-neutral checks of packaged content, run by every build.
val verifyAndromedaResources by tasks.registering {
    group = "verification"
    description = "Checks JSON, Origin references, sound assets, and transient ownership invariants."
    inputs.dir("src/main/resources")
    doLast {
        val resourceRoot = file("src/main/resources")
        val parser = JsonSlurper()
        val json = resourceRoot.walkTopDown().filter { it.extension == "json" }.associateWith {
            try { parser.parse(it) } catch (exception: Exception) {
                throw GradleException("Invalid JSON: $it", exception)
            }
        }
        val dataRoot = resourceRoot.resolve("data/andromeda_origins")
        val powersRoot = dataRoot.resolve("powers")
        val powerIds = powersRoot.walkTopDown().filter { it.extension == "json" }
            .map { "andromeda_origins:" + it.relativeTo(powersRoot).invariantSeparatorsPath.removeSuffix(".json") }.toSet()
        check(powerIds.isNotEmpty()) { "No Andromeda powers packaged" }
        val origins = dataRoot.resolve("origins").walkTopDown().filter { it.extension == "json" }.toList()
        check(origins.isNotEmpty()) { "No Andromeda Origins packaged" }
        origins.forEach { origin ->
            val definition = json[origin] as Map<*, *>
            val powers = definition["powers"] as? List<*> ?: error("Missing powers in $origin")
            powers.filterIsInstance<String>().filter { it.startsWith("andromeda_origins:") }.forEach {
                check(it in powerIds) { "$origin references missing power $it" }
            }
            check((definition["tags"] as? List<*>)?.isNotEmpty() == true) { "$origin has no native Origin tags" }
        }
        val sounds = json[resourceRoot.resolve("assets/andromeda_origins/sounds.json")] as Map<*, *>
        sounds.values.forEach { definition ->
            ((definition as Map<*, *>)["sounds"] as List<*>).forEach { entry ->
                val name = if (entry is String) entry else (entry as Map<*, *>)["name"] as String
                val namespace = if (':' in name) name.substringBefore(':') else "andromeda_origins"
                if (namespace == "andromeda_origins") check(resourceRoot.resolve("assets/$namespace/sounds/${name.substringAfter(':')}.ogg").exists()) {
                    "Missing sound $name"
                }
            }
        }
        powersRoot.walkTopDown().filter { it.extension == "json" }.forEach {
            check(!it.readText().contains("tp @s @p[tag=mantileap")) { "Nearest-player Manticore dragging is unsafe: $it" }
        }
        logger.lifecycle("Validated ${json.size} JSON resources, ${powerIds.size} powers, ${origins.size} Origins.")
    }
}
tasks.check { dependsOn(verifyAndromedaResources) }


// Static-only Kotlin object injectors need static callbacks, but no singleton constructor.
// Keep their authored static methods and annotations; remove unused object scaffolding so
// Mixin never copies an initializer that attempts to instantiate a target class.
val staticMixinObjects = setOf(
    "andromeda/origins/mixin/NereidGillsPowerMixin.class",
    "andromeda/origins/mixin/compat/IrisNightVisionMixin.class",
    "andromeda/origins/mixin/UndetectableSensorMixin.class",
    "andromeda/origins/mixin/OriginManagerConfigMixin.class",
    "andromeda/origins/mixin/compat/ArmorModelApiRenderMixin.class",
    "andromeda/origins/mixin/compat/IncapacitatedFoodMixin.class"
)
tasks.withType<KotlinCompile>().configureEach {
    doLast {
        staticMixinObjects.forEach { relative ->
            val file = destinationDirectory.get().asFile.resolve(relative)
            if (file.exists()) {
                val reader = ClassReader(file.readBytes())
                val writer = ClassWriter(0)
                reader.accept(object : ClassVisitor(Opcodes.ASM9, writer) {
                    override fun visitField(access: Int, name: String, descriptor: String, signature: String?, value: Any?) =
                        if (name == "INSTANCE") null else super.visitField(access, name, descriptor, signature, value)
                    override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?) =
                        if (name == "<init>" || name == "<clinit>") null else super.visitMethod(access, name, descriptor, signature, exceptions)
                }, 0)
                file.writeBytes(writer.toByteArray())
            }
        }
    }
}

val verifyStaticMixinBytecode by tasks.registering {
    group = "verification"
    description = "Rejects Kotlin singleton initialization in static-only Mixin injectors."
    dependsOn(tasks.withType<KotlinCompile>())
    doLast {
        var verified = 0
        tasks.withType<KotlinCompile>().forEach { task ->
            staticMixinObjects.forEach { relative ->
                val output = task.destinationDirectory.get().asFile.resolve(relative)
                if (output.exists()) {
                    ClassReader(output.readBytes()).accept(object : ClassVisitor(Opcodes.ASM9) {
                        override fun visitField(access: Int, name: String, descriptor: String, signature: String?, value: Any?): org.objectweb.asm.FieldVisitor? {
                            check(name != "INSTANCE") { "Mixin singleton field remains: $output" }
                            return null
                        }
                        override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): org.objectweb.asm.MethodVisitor? {
                            check(name != "<init>" && name != "<clinit>") { "Mixin initializer remains: $output" }
                            check(access and Opcodes.ACC_STATIC != 0) { "Static Mixin callback became an instance method: $output::$name" }
                            return null
                        }
                    }, ClassReader.SKIP_CODE)
                    verified++
                }
            }
        }
        check(verified >= 6) { "Expected static Mixin bytecode for both loaders; checked $verified classes" }
        logger.lifecycle("Verified $verified compiled static-only Mixin classes.")
    }
}
tasks.check { dependsOn(verifyStaticMixinBytecode) }
