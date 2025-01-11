import gradle.kotlin.dsl.accessors._3e4367e61b26e4e7d88385c6269c7b89.annotationProcessor
import gradle.kotlin.dsl.accessors._3e4367e61b26e4e7d88385c6269c7b89.implementation

plugins {
    id("buildlogic.java-common-conventions")
}

dependencies {
    implementation("org.incendo:cloud-annotations:${properties["cloud_version"]}") {
        exclude("org.checkerframework")
        exclude("io.leangen.geantyref")
    }
    annotationProcessor("org.incendo:cloud-annotations:${properties["cloud_version"]}") {
        exclude("org.checkerframework")
        exclude("io.leangen.geantyref")
    }
}

tasks {
    shadowJar {
        val prefix = "forcepack.libs.${project.name}"
        relocate("org.incendo.cloud", "${prefix}.cloud")
    }
}
