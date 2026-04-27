import java.net.URL
plugins {
    id("java")
}

group = "mx.jume.aquahunger"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(fileTree("libs") { include("*.jar") })
    compileOnly("com.google.code.gson:gson:2.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

// Generate Manifest Task based on AquaSanity template
val generateManifest by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/resources")
    outputs.dir(outputDir)
    
    inputs.property("group", project.group)
    inputs.property("name", "Aqua-Thirst-hunger")
    inputs.property("version", project.version)

    doLast {
        var activeServerVersion = "2026.03.26-89796e57b" // Base version
        try {
            val xmlText = URL("https://maven.hytale.com/release/com/hypixel/hytale/Server/maven-metadata.xml").readText()
            val regex = "<release>(.*?)</release>".toRegex()
            val match = regex.find(xmlText)
            if (match != null) {
                activeServerVersion = match.groupValues[1]
            }
        } catch (e: Exception) {
            println("Warning: Could not connect to Hytale Maven. Using base version.")
        }

        val json = """
            {
              "Group": "${project.group}",
              "Name": "Aqua-Thirst-hunger",
              "Version": "${project.version}",
              "Description": "A mod that adds thirst and hunger mechanics to Hytale.",
              "Authors": [
                  { "Name": "jume" },
                  { "Name": "andiemg" },
                  { "Name": "antigravity" }
              ],
              "ServerVersion": "$activeServerVersion",
              "Main": "${project.group}.AquaThirstHunger",
              "Dependencies": {
                "Hytale:EntityModule": "*"
              },
              "OptionalDependencies": {
                "Zuxaw:RPGLeveling": "*",
                "ziggfreed:MMOSkillTree": "*",
                "airijko:EndlessLeveling": "*"
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
