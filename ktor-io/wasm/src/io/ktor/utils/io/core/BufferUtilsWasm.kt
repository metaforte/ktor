@file:Suppress("ReplaceRangeToWithUntil", "RedundantModalityModifier")

package io.ktor.utils.io.core

import io.ktor.utils.io.bits.*
import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
public inline fun Buffer.writeDirect(block: (ByteArray) -> Int): Int {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return write { memory, start, endExclusive ->
        val array = ByteArray(endExclusive - start) { memory.data[start + it] }
        block(array).also {
            array.copyInto(memory.data, start)
        }
    }
}

@OptIn(ExperimentalContracts::class)
public inline fun Buffer.readDirect(block: (ByteArray) -> Int): Int {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return read { memory, start, endExclusive ->
        val array = ByteArray(endExclusive - start) { memory.data[start + it] }
        block(array)
    }
}

@OptIn(ExperimentalContracts::class)
public inline fun Buffer.readDirectByteArray(block: (ByteArray) -> Int): Int {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return read { memory, start, endExclusive ->
        val array = ByteArray(endExclusive - start).also {
            memory.copyTo(it, start, endExclusive - start, 0)
        }
        block(array)
    }
}
