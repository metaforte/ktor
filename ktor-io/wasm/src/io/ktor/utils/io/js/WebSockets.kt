package io.ktor.utils.io.js

import io.ktor.utils.io.bits.*
import io.ktor.utils.io.core.*
import io.ktor.utils.io.core.internal.*
import org.w3c.dom.*
import org.khronos.webgl.*

public fun WebSocket.sendPacket(packet: ByteReadPacket) {
    send(packet.readArrayBuffer())
}

public inline fun WebSocket.sendPacket(block: BytePacketBuilder.() -> Unit) {
    sendPacket(buildPacket(block = block))
}

public inline fun MessageEvent.packet(): ByteReadPacket {
    val data = Int8Array((data as DataView).buffer).toByteArray()
    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE", "UnsafeCastFromDynamic")
    return ByteReadPacket(
        ChunkBuffer(Memory(data), null, ChunkBuffer.NoPool),
        ChunkBuffer.NoPool
    )
}
