@file:Suppress("NOTHING_TO_INLINE")

package io.ktor.utils.io.bits

import io.ktor.utils.io.core.internal.*
import kotlin.require

/**
 * Represents a linear range of bytes.
 */
public actual class Memory constructor(public val data: ByteArray) {
    /**
     * Size of memory range in bytes.
     */
    public actual inline val size: Long get() = data.size.toLong()

    /**
     * Size of memory range in bytes represented as signed 32bit integer
     * @throws IllegalStateException when size doesn't fit into a signed 32bit integer
     */
    public actual inline val size32: Int get() = data.size

    /**
     * Returns byte at [index] position.
     */
    public actual inline fun loadAt(index: Int): Byte {
        return data[index]
    }

    /**
     * Returns byte at [index] position.
     */
    public actual inline fun loadAt(index: Long): Byte {
        return data[(index.toIntOrFail("index"))]
    }

    /**
     * Write [value] at the specified [index].
     */
    public actual inline fun storeAt(index: Int, value: Byte) {
        data[index] = value
    }

    /**
     * Write [value] at the specified [index]
     */
    public actual inline fun storeAt(index: Long, value: Byte) {
        data[index.toIntOrFail("index")] = value
    }

    /**
     * Returns memory's subrange. On some platforms it could do range checks but it is not guaranteed to be safe.
     * It also could lead to memory allocations on some platforms.
     */
    public actual fun slice(offset: Int, length: Int): Memory =
        Memory(ByteArray(length) { data[it + offset] })

    /**
     * Returns memory's subrange. On some platforms it could do range checks but it is not guaranteed to be safe.
     * It also could lead to memory allocations on some platforms.
     */
    public actual fun slice(offset: Long, length: Long): Memory =
        slice(offset.toIntOrFail("offset"), length.toIntOrFail("length"))

    /**
     * Copies bytes from this memory range from the specified [offset] and [length]
     * to the [destination] at [destinationOffset].
     * Copying bytes from a memory to itself is allowed.
     */
    public actual fun copyTo(
        destination: Memory,
        offset: Int,
        length: Int,
        destinationOffset: Int
    ) {
        copyTo(destination.data, offset, length, destinationOffset)
    }

    /**
     * Copies bytes from this memory range from the specified [offset] and [length]
     * to the [destination] at [destinationOffset].
     * Copying bytes from a memory to itself is allowed.
     */
    public actual fun copyTo(
        destination: Memory,
        offset: Long,
        length: Long,
        destinationOffset: Long
    ) {
        copyTo(destination.data, offset, length.toIntOrFail("length"), destinationOffset.toIntOrFail("destinationOffset"))
    }

    public actual companion object {
        /**
         * Represents an empty memory region
         */
        public actual val Empty: Memory = Memory(ByteArray(0))
    }
}

/**
 * Copies bytes from this memory range from the specified [offset] and [length]
 * to the [destination] at [destinationOffset].
 */
public actual fun Memory.copyTo(
    destination: ByteArray,
    offset: Int,
    length: Int,
    destinationOffset: Int
) {
    data.copyInto(destination, destinationOffset, offset, offset + length)
}

/**
 * Copies bytes from this memory range from the specified [offset] and [length]
 * to the [destination] at [destinationOffset].
 */
public actual fun Memory.copyTo(
    destination: ByteArray,
    offset: Long,
    length: Int,
    destinationOffset: Int
) {
    copyTo(destination, offset.toIntOrFail("offset"), length, destinationOffset)
}

/**
 * Fill memory range starting at the specified [offset] with [value] repeated [count] times.
 */
public actual fun Memory.fill(offset: Int, count: Int, value: Byte) {
    for (index in offset until offset + count) {
        data[index] = value
    }
}

/**
 * Fill memory range starting at the specified [offset] with [value] repeated [count] times.
 */
public actual fun Memory.fill(offset: Long, count: Long, value: Byte) {
    fill(offset.toIntOrFail("offset"), count.toIntOrFail("count"), value)
}
