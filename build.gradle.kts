plugins {
    kotlin("jvm") version "2.1.20"
    application
}

repositories { mavenCentral() }

kotlin { jvmToolchain(17) }

application {
    mainClass.set("dev.veriloggraph.MainKt")
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


