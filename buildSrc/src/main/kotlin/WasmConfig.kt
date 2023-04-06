/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("UNUSED_VARIABLE")

import org.gradle.api.*
import org.gradle.kotlin.dsl.*
import java.io.*

fun Project.configureWasm() {
    configureWasmTasks()

    kotlin {
        sourceSets {
            val wasmTest by getting {
                dependencies {
                    implementation(npm("puppeteer", "*"))
                }
            }
        }
    }

    configureJsTestTasks()
}

private fun Project.configureWasmTasks() {
    kotlin {
        wasm {
            nodejs {
                testTask {
                    useMocha {
                        timeout = "10000"
                    }
                }
            }

            browser {
                testTask {
                    useKarma {
                        useChromeHeadlessWasmGc()
                        useConfigDirectory(File(project.rootProject.projectDir, "karma"))
                    }
                }
            }
        }
    }
}

private fun Project.configureJsTestTasks() {
    val shouldRunWasmBrowserTest = !hasProperty("teamcity") || hasProperty("enable-wasm-tests")
    if (shouldRunWasmBrowserTest) return

    val cleanWasmBrowserTest by tasks.getting
    val wasmBrowserTest by tasks.getting
    cleanWasmBrowserTest.onlyIf { false }
    wasmBrowserTest.onlyIf { false }
}
