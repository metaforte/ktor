kotlin {
    createCInterop("threadUtils", nixTargets()) {
        defFile = File(projectDir, "nix/interop/threadUtils.def")
    }

    sourceSets {
        commonMain {
            dependencies {
                api(project(":ktor-io"))
            }
        }
        commonTest {
            dependencies {
                api(project(":ktor-test-dispatcher"))
            }
        }

        val jsWasmMain by creating { }
        val jsWasmTest by creating { }

        jsMain {
            dependsOn(jsWasmMain)
        }

        jsTest {
            dependsOn(jsWasmTest)
        }

        wasmMain {
            dependsOn(jsWasmMain)
        }

        wasmTest {
            dependsOn(jsWasmTest)
        }
    }
}
