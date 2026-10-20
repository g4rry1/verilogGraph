plugins {
    kotlin("jvm") version "2.1.20"
    application
}

repositories { mavenCentral() }

kotlin { jvmToolchain(17) }

application {
    mainClass.set("dev.veriloggraph.MainKt")
}


