import com.google.protobuf.gradle.*

plugins {
    java
    id("com.google.protobuf") version "0.8.16"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.protobuf:protobuf-java:3.15.3")
    implementation("io.grpc:grpc-protobuf:1.15.1")
}

java {
    sourceSets {
        main {
            java.srcDir("src/main/java")
            proto.srcDir("src/main/java/ru/hse/java/proto")
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.17.3"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.38.0"
        }
    }
    generatedFilesBaseDir = "src"
    generateProtoTasks {
        ofSourceSet("proto").forEach {
            it.plugins {
                id("grpc")
            }
        }
    }
}
