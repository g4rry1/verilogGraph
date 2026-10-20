plugins {
    kotlin("jvm") version "2.1.20"
    application
}

repositories { mavenCentral() }

kotlin { jvmToolchain(17) }

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("dev.veriloggraph.MainKt")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

val grammarCheck by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Checks representative Annex A token paths and graph edges"
    dependsOn(tasks.classes)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set(application.mainClass)
    args("--check")
}

tasks.check {
    dependsOn(grammarCheck)
}


