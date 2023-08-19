/*
* Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
*/

package io.ktor.server.engine

import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.util.*
import io.ktor.util.logging.*
import io.ktor.util.network.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

/**
 * Engine environment configuration builder
 */
@KtorDsl
public expect class ApplicationEnvironmentBuilder() {
    /**
     * Parent coroutine context for an application
     */
    public var parentCoroutineContext: CoroutineContext

    /**
     * Application logger
     */
    public var log: Logger

    /**
     * Application config
     */
    public var config: ApplicationConfig

    /**
     * Build an application engine environment
     */
    public fun build(builder: ApplicationEnvironmentBuilder.() -> Unit): ApplicationEnvironment
}

public fun applicationEnvironment(block: ApplicationEnvironmentBuilder.() -> Unit): ApplicationEnvironment {
    return ApplicationEnvironmentBuilder().build(block)
}
