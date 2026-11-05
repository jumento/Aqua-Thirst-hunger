plugins {
    id("java")
}

group = "mx.jume.aquahunger"
version = "1.1.0"

// Server API the mod is compiled against and the range written to manifest.json.
// Maven only keeps recent patches (0.7.2 was removed), so compile against the newest one;
// the minimum stays 0.7.2 because every server class the mod uses has the same signatures
// in 0.7.2 and 0.7.5. Re-check both after a new Hytale release.
val hytaleServerVersion = "0.7.5"
val hytaleServerRange = ">=0.7.2 <0.8.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
    maven("https://maven.hytale.com/release")
}

dependencies {
    compileOnly("com.hypixel.hytale:Server:$hytaleServerVersion")
    compileOnly("com.google.code.gson:gson:2.10.1")
    testImplementation("com.hypixel.hytale:Server:$hytaleServerVersion")
    testImplementation("com.google.code.gson:gson:2.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
    // Config code writes backups under mods/Aqua-Thirst-hunger relative to the working directory.
    val testWorkDir = layout.buildDirectory.dir("test-work").get().asFile
    workingDir = testWorkDir
    doFirst { testWorkDir.mkdirs() }
}

val generateManifest by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/resources")
    val modGroup = project.group.toString()
    val modVersion = project.version.toString()
    outputs.dir(outputDir)

    inputs.property("group", modGroup)
    inputs.property("version", modVersion)
    inputs.property("serverRange", hytaleServerRange)

    doLast {
        val json = """
            {
              "Group": "$modGroup",
              "Name": "Aqua-Thirst-hunger",
              "Version": "$modVersion",
              "Description": "A mod that adds thirst and hunger mechanics to Hytale.",
              "Authors": [
                  { "Name": "jume" },
                  { "Name": "andiemg" },
                  { "Name": "antigravity" }
              ],
              "ServerVersion": "$hytaleServerRange",
              "Main": "$modGroup.AquaThirstHunger",
              "Dependencies": {
                "Hytale:EntityModule": "*"
              },
              "OptionalDependencies": {
                "Zuxaw:RPGLeveling": "*",
                "Airijko:EndlessLevelingCore": "*"
              },
              "LoadBefore": {
                "Ziggfreed:MMOSkillTree": "*"
              },
              "IncludesAssetPack": true,
              "DisabledByDefault": false
            }
        """.trimIndent()

        outputDir.get().file("manifest.json").asFile.apply {
            parentFile.mkdirs()
            writeText(json)
        }
    }
}

sourceSets {
    main {
        resources {
            srcDir(generateManifest)
        }
    }
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes(
            "Implementation-Title" to "Aqua-Thirst-hunger",
            "Implementation-Version" to project.version,
            "Main-Class" to "${project.group}.AquaThirstHunger"
        )
    }
}
