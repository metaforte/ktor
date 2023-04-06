/*
 * Copyright 2014-2022 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.io.internal

import io.ktor.io.*
import io.ktor.io.charsets.*
import kotlinx.coroutines.*

@PublishedApi
internal class Utf8StringReader(
    private val input: ByteReadChannel
) : StringReader {
    override val charset: Charset = Charsets.UTF_8

    override val closedCause: Throwable?
        get() = input.closedCause

    override val readablePacket: ReadablePacket
        get() = input.readablePacket

    override fun isClosedForRead(): Boolean {
        return input.isClosedForRead() && chunkIsEmpty()
    }

    var chunk: String = readablePacket.clone().readString()
    var chunkStart: Int = 0
    var bytesInPacket = readablePacket.availableForRead

    override suspend fun readStringChunk(block: (chunk: String, startIndex: Int) -> Int) {
        if (chunkIsEmpty() && isClosedForRead()) return

        if (chunkIsEmpty() || cacheIsInvalid()) {
            if (isEmpty) {
                awaitBytes()
            }

            chunk = readablePacket.clone().readString()
            chunkStart = 0
            bytesInPacket = readablePacket.availableForRead
        }

        val consumed = block(chunk, chunkStart)
        check(consumed >= 0) { "Block should return non-negative number of consumed bytes: $consumed" }

        readablePacket.discardExact(sizeInBytes(chunk, chunkStart, chunkStart + consumed))
        chunkStart += consumed
    }

    override suspend fun awaitBytesWhile(predicate: () -> Boolean) {
        chunk = ""
        chunkStart = 0
        input.awaitBytesWhile(predicate)
    }

    override fun cancel(cause: CancellationException?) {
        input.cancel(cause)
    }

    private fun chunkIsEmpty() = chunkStart >= chunk.length

    private fun cacheIsInvalid() = bytesInPacket != readablePacket.availableForRead
}

private fun sizeInBytes(chunk: String, startIndex: Int, endIndex: Int): Int {
    var result = 0
    for (i in startIndex until endIndex) {
        result += chunk[i].sizeInBytes()
    }
    return result
}

private fun Char.sizeInBytes(): Int {
    return when (this) {
        in '\u0000'..'\u007F' -> 1
        in '\u0080'..'\u07FF' -> 2
        in '\u0800'..'\uFFFF' -> 3
        else -> 4
    }
}