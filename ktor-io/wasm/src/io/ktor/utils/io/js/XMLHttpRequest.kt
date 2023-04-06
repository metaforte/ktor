package io.ktor.utils.io.js

import io.ktor.utils.io.bits.*
import io.ktor.utils.io.core.*
import io.ktor.utils.io.core.internal.*
import org.w3c.xhr.*
import org.khronos.webgl.Int8Array
import org.khronos.webgl.DataView

public inline fun XMLHttpRequest.sendPacket(block: BytePacketBuilder.() -> Unit) {
    sendPacket(buildPacket(block = block))
}

public fun XMLHttpRequest.sendPacket(packet: ByteReadPacket) {
    send(packet.readArrayBuffer())
}

@Suppress("UnsafeCastFromDynamic", "DEPRECATION")
public fun XMLHttpRequest.responsePacket(): ByteReadPacket = when (responseType) {
    XMLHttpRequestResponseType.ARRAYBUFFER -> ByteReadPacket(
        ChunkBuffer(
            Memory(Int8Array((response as DataView).buffer).toByteArray()),
            null,
            ChunkBuffer.NoPool
        ),
        ChunkBuffer.NoPoolManuallyManaged
    )
    XMLHttpRequestResponseType.EMPTY -> ByteReadPacket.Empty
    else -> throw IllegalStateException("Incompatible type $responseType: only ARRAYBUFFER and EMPTY are supported")
}
