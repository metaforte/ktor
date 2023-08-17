/*
 * Copyright 2014-2023 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.plugins.internal

import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*

internal class ByteChannelReplay(private val origin: ByteReadChannel) {
    var content: ByteArray? = null

    @OptIn(DelicateCoroutinesApi::class)
    fun replay(): ByteReadChannel {
        val value = content
        if (value != null) {
            return ByteReadChannel(value)
        }

        if (origin.closedCause != null) {
            throw origin.closedCause!!
        }

        return GlobalScope.writer(Dispatchers.Unconfined) {
            val body = BytePacketBuilder()
            try {
                while (!origin.isClosedForRead) {
                    if (origin.availableForRead == 0) origin.awaitContent()
                    val packet = origin.readPacket(origin.availableForRead)

                    body.writePacket(packet.copy())
                    channel.writePacket(packet)
                    channel.flush()
                }

                origin.closedCause?.let { throw it }
            } catch (cause: Throwable) {
                body.release()
                throw cause
            }

            content = body.build().readBytes()
        }.channel
    }
}
