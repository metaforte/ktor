/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.engine.js

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.fetch.RequestInit
import io.ktor.client.request.*
import io.ktor.http.content.*
import io.ktor.util.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import org.khronos.webgl.Uint8Array
import org.w3c.fetch.*
import kotlin.coroutines.*
import io.ktor.client.utils.*

@OptIn(InternalAPI::class, DelicateCoroutinesApi::class)
internal suspend fun HttpRequestData.toRaw(
    clientConfig: HttpClientConfig<*>,
    callContext: CoroutineContext
): RequestInit {
    val jsHeaders = makeJsObject<JsAny>().also { obj ->
        mergeHeaders(this@toRaw.headers, this@toRaw.body) { key, value ->
            obj[key] = value
        }
    }

    val bodyBytes: ByteArray? = when (val content = body) {
        is OutgoingContent.ByteArrayContent -> content.bytes()
        is OutgoingContent.ReadChannelContent -> content.readFrom().readRemaining().readBytes()
        is OutgoingContent.WriteChannelContent -> {
            GlobalScope.writer(callContext) {
                content.writeTo(channel)
            }.channel.readRemaining().readBytes()
        }
        else -> null
    }

    return makeJsObject<RequestInit>().also {
        it["method"] = this@toRaw.method.value
        it["headers"] = jsHeaders
        it["redirect"] = if (clientConfig.followRedirects) "follow" else "manual"
        if (bodyBytes != null) {
            it["body"] = bodyBytes.asJsArray()
        }
    }
}
