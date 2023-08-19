/*
 * Copyright 2014-2023 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.server.jetty.jakarta

import io.ktor.events.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import kotlinx.coroutines.*
import org.eclipse.jetty.server.*

/**
 * [ApplicationEngine] base type for running in a standalone Jetty
 */
public open class JettyApplicationEngineBase(
    environment: ApplicationEnvironment,
    monitor: Events,
    developmentMode: Boolean,
    /**
     * Application engine configuration specifying engine-specific options such as parallelism level.
     */
    protected val configuration: Configuration
) : BaseApplicationEngine(environment, monitor, developmentMode) {

    /**
     * Jetty-specific engine configuration
     */
    public class Configuration : BaseApplicationEngine.Configuration() {
        /**
         * Property function that will be called during Jetty server initialization
         * with the server instance as receiver.
         */
        public var configureServer: Server.() -> Unit = {}
    }

    private var cancellationDeferred: CompletableJob? = null

    /**
     * Jetty server instance being configuring and starting
     */
    protected val server: Server = Server().apply {
        this@JettyApplicationEngineBase.configuration.configureServer(this)
        initializeServer(configuration)
    }

    override fun start(wait: Boolean): JettyApplicationEngineBase {
        addShutdownHook(monitor) {
            stop(configuration.shutdownGracePeriod, configuration.shutdownTimeout)
        }

        server.start()
        cancellationDeferred =
            stopServerOnCancellation(configuration.shutdownGracePeriod, configuration.shutdownTimeout)

        val connectors = server.connectors.zip(configuration.connectors)
            .map { it.second.withPort((it.first as ServerConnector).localPort) }
        resolvedConnectors.complete(connectors)

        monitor.raiseCatching(ServerReady, environment, environment.log)

        if (wait) {
            server.join()
            stop(configuration.shutdownGracePeriod, configuration.shutdownTimeout)
        }
        return this
    }

    override fun stop(gracePeriodMillis: Long, timeoutMillis: Long) {
        cancellationDeferred?.complete()
        monitor.raise(ApplicationStopPreparing, environment)
        server.stopTimeout = timeoutMillis
        server.stop()
        server.destroy()
    }

    override fun toString(): String {
        return "Jetty($environment)"
    }
}
