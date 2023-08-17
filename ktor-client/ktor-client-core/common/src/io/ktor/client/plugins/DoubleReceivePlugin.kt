/*
 * Copyright 2014-2023 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.plugins

import io.ktor.client.call.*
import io.ktor.client.plugins.api.*
import io.ktor.client.plugins.internal.ByteChannelReplay
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.util.date.*
import io.ktor.utils.io.*
import kotlin.coroutines.*

private val SKIP_SAVE_BODY = AttributeKey<Unit>("SkipSaveBody")

private val RESPONSE_BODY_SAVED = AttributeKey<Unit>("ResponseBodySaved")

/**
 * Configuration for [SaveBodyPlugin]
 */
public class SaveBodyPluginConfig {
    /**
     * Disables the plugin for all request.
     *
     * If you need to disable it only for the specific request, please use [HttpRequestBuilder.skipSavingBody]:
     * ```kotlin
     * client.get("http://myurl.com") {
     *     skipSavingBody()
     * }
     * ```
     */
    public var disabled: Boolean = false
}

/**
 * [SaveBodyPlugin] saving the whole body in memory, so it can be received multiple times.
 *
 * It may be useful to prevent saving body in case of big size or streaming. To do so use [HttpRequestBuilder.skipSavingBody]:
 * ```kotlin
 * client.get("http://myurl.com") {
 *     skipSavingBody()
 * }
 * ```
 *
 * The plugin is installed by default, if you need to disable it use:
 * ```kotlin
 * val client = HttpClient {
 *     install(SaveBodyPlugin) {
 *         disabled = true
 *     }
 * }
 * ```
 */
@OptIn(InternalAPI::class)
public val SaveBodyPlugin: ClientPlugin<SaveBodyPluginConfig> = createClientPlugin(
    "DoubleReceivePlugin",
    ::SaveBodyPluginConfig
) {

    client.receivePipeline.intercept(HttpReceivePipeline.Before) { response ->
        if (this@createClientPlugin.pluginConfig.disabled) return@intercept
        val attributes = response.call.attributes
        if (attributes.contains(SKIP_SAVE_BODY)) return@intercept

        val bodyReplay = ByteChannelReplay(response.content)

        val result = object : HttpResponse() {
            override val call: HttpClientCall = response.call

            override val status: HttpStatusCode = response.status

            override val version: HttpProtocolVersion = response.version

            override val requestTime: GMTDate = response.requestTime

            override val responseTime: GMTDate = response.responseTime

            @InternalAPI
            override val content: ByteReadChannel
                get() = bodyReplay.replay()

            override val headers: Headers = response.headers

            override val coroutineContext: CoroutineContext = response.coroutineContext
        }

        response.call.attributes.put(RESPONSE_BODY_SAVED, Unit)
        proceedWith(result)
    }
}

public val HttpResponse.isSaved: Boolean
    get() = call.attributes.contains(RESPONSE_BODY_SAVED)

/**
 * Prevent saving response body in memory for the specific request.
 *
 * To disable the plugin for all requests use [SaveBodyPluginConfig.disabled] property:
 * ```kotlin
 * val client = HttpClient {
 *     install(SaveBodyPlugin) {
 *         disabled = true
 *     }
 * }
 * ```
 */
public fun HttpRequestBuilder.skipSavingBody() {
    attributes.put(SKIP_SAVE_BODY, Unit)
}
