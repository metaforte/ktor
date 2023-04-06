@file:Suppress("FunctionName")

package io.ktor.utils.io.core

import io.ktor.utils.io.bits.*
import io.ktor.utils.io.core.internal.*
import io.ktor.utils.io.pool.*

public actual fun ByteReadPacket(
    array: ByteArray,
    offset: Int,
    length: Int,
    block: (ByteArray) -> Unit
): ByteReadPacket {
    val sub = when {
        offset == 0 && length == array.size -> array
        else -> ByteArray(length) { array[offset + it] }
    }

    val pool = object : SingleInstancePool<ChunkBuffer>() {
        override fun produceInstance(): ChunkBuffer =
            ChunkBuffer(Memory(sub), null, this)

        override fun disposeInstance(instance: ChunkBuffer) {
            block(array)
        }
    }

    return ByteReadPacket(pool.borrow().apply { resetForRead() }, pool)
}
