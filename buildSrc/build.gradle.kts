repositories {
    mavenCentral()
}

tasks.compileJava {
    options.release.set(21)

}

dependencies {
    implementation(gradleApi())
    implementation("org.glavo.kala:kala-common:0.74.0")
    implementation("org.glavo.kala:kala-template:0.1.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.apache.commons:commons-compress:1.27.1")
    implementation("net.java.dev.jna:jna:5.14.0")
}
