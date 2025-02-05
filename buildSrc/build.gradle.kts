/*
 * Copyright 2025 Glavo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

tasks.compileJava {
    options.release.set(21)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation("org.glavo.kala:kala-common:0.74.0")
    implementation("org.glavo.kala:kala-template:0.1.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.apache.commons:commons-compress:1.27.1")
    implementation("net.java.dev.jna:jna:5.16.0")
    implementation("org.glavo:classfile:0.5.0")
}
