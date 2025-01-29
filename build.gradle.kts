plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.0"

}

group = "me.upstreambridge"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
    jcenter()
}

dependencies {
    compileOnly(gradleApi()) // Use compileOnly to reduce runtime classpath bloat if applicable
    implementation("commons-io:commons-io:2.13.0") // Updated to the latest
    implementation("com.github.mwiede:jsch:0.2.23") // Updated to the latest jsch
    implementation ("com.googlecode.json-simple:json-simple:1.1.1")

}


tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>().configureEach {
    archiveClassifier.set("all")
}
tasks.test {
    useJUnitPlatform()
}