package io.ktor.utils.io.charsets

import io.ktor.utils.io.core.*
import io.ktor.utils.io.js.*

public actual abstract class Charset(internal val _name: String) {
    public actual abstract fun newEncoder(): CharsetEncoder
    public actual abstract fun newDecoder(): CharsetDecoder

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is Charset && (_name != other._name)
    }

    override fun hashCode(): Int = _name.hashCode()

    override fun toString(): String = _name

    public actual companion object {
        private fun getCharset(name: String): Charset? = when (name.replace('_', '-').lowercase()) {
            "utf8", "utf-8" -> Charsets.UTF_8
            "iso-8859-1", "latin1" -> Charsets.ISO_8859_1
            else -> null
        }

        @Suppress("LocalVariableName")
        public actual fun forName(name: String): Charset =
            getCharset(name) ?: throw IllegalArgumentException("Charset $name is not supported")

        public actual fun isSupported(charset: String): Boolean =
            getCharset(charset) != null
    }
}

public actual val Charset.name: String get() = _name

// -----------------------

public actual abstract class CharsetEncoder(internal val _charset: Charset)
public actual val CharsetEncoder.charset: Charset get() = _charset

public actual abstract class CharsetDecoder(internal val _charset: Charset)
public actual val CharsetDecoder.charset: Charset get() = _charset

private data class CharsetImpl(val name: String) : Charset(name) {
    override fun newEncoder(): CharsetEncoder = object : CharsetEncoder(this) { }
    override fun newDecoder(): CharsetDecoder = object : CharsetDecoder(this) { }
}

public actual object Charsets {
    public actual val UTF_8: Charset = CharsetImpl("UTF-8")
    public actual val ISO_8859_1: Charset = CharsetImpl("ISO-8859-1")
}

// ----------------------------------------------------------------------

public actual fun CharsetEncoder.encodeToByteArray(input: CharSequence, fromIndex: Int, toIndex: Int): ByteArray =
    encodeToByteArrayImpl1(input, fromIndex, toIndex)

internal actual fun CharsetEncoder.encodeImpl(input: CharSequence, fromIndex: Int, toIndex: Int, dst: Buffer): Int {
    require(fromIndex <= toIndex)
    return when (charset) {
        Charsets.ISO_8859_1 -> encodeISO88591(input, fromIndex, toIndex, dst)
        Charsets.UTF_8 -> encodeUTF8(input, fromIndex, toIndex, dst)
        else -> error { "Only UTF-8 encoding is supported in JS" }
    }
}

public actual fun CharsetEncoder.encodeUTF8(input: ByteReadPacket, dst: Output) {
    require(charset === Charsets.UTF_8)
    // we only support UTF-8 so as far as input is UTF-8 encoded string then we simply copy bytes
    dst.writePacket(input)
}

internal actual fun CharsetEncoder.encodeComplete(dst: Buffer): Boolean = true

// ----------------------------------------------------------------------

internal actual fun CharsetDecoder.decodeBuffer(
    input: Buffer,
    out: Appendable,
    lastBuffer: Boolean,
    max: Int
): Int {
    if (max == 0) return 0

    val decoder = Decoder(charset.name)
    val copied: Int

    input.readDirectByteArray { data ->
        val result = data.decodeBufferImpl(decoder, max)
        out.append(result.charactersDecoded)
        copied = result.bytesConsumed

        result.bytesConsumed
    }

    return copied
}

public actual fun CharsetDecoder.decode(input: Input, dst: Appendable, max: Int): Int {
    val decoder = Decoder(charset.name, true)
    var charactersCopied = 0

    // use decode stream while we have remaining characters count > buffer size in bytes
    // it is much faster than using decodeBufferImpl
    input.takeWhileSize { buffer ->
        val rem = max - charactersCopied
        val bufferSize = buffer.readRemaining
        if (rem < bufferSize) return@takeWhileSize 0

        buffer.readDirectByteArray { data ->
            val decodedText = decodeWrap {
                decoder.decodeStream(data)
            }
            dst.append(decodedText)
            charactersCopied += decodedText.length
            data.size
        }

        when {
            charactersCopied == max -> {

                val tail = try {
                    decoder.decode()
                } catch (_: Throwable) {
                    ""
                }

                if (tail.isNotEmpty()) {
                    // if we have a trailing byte then we can't handle this chunk via fast-path
                    // because we don't know how many bytes in the end we need to preserve
                    buffer.rewind(bufferSize)
                }
                0
            }
            charactersCopied < max -> MAX_CHARACTERS_SIZE_IN_BYTES
            else -> 0
        }
    }

    if (charactersCopied < max) {
        var size = 1
        input.takeWhileSize(1) { buffer ->
            val rc = buffer.readDirectByteArray { data ->
                val result = data.decodeBufferImpl(decoder, max - charactersCopied)
                dst.append(result.charactersDecoded)
                charactersCopied += result.charactersDecoded.length
                result.bytesConsumed
            }
            when {
                rc > 0 -> size = 1
                size == MAX_CHARACTERS_SIZE_IN_BYTES -> size = 0
                else -> size++
            }

            size
        }
    }

    return charactersCopied
}

public actual fun CharsetDecoder.decodeExactBytes(input: Input, inputLength: Int): String {
    if (inputLength == 0) return ""
    if (input.headRemaining >= inputLength) {
        val decoder = Decoder(charset._name, true)

        val head = input.head
        val data = input.headMemory.data
        val readPosition = head.readPosition

        val text = decodeWrap {
            val subData: ByteArray = when {
                readPosition == 0 && inputLength == data.size -> data
                else -> ByteArray(inputLength) { data[it + readPosition] }
            }
            decoder.decode(subData)
        }

        input.discardExact(inputLength)
        return text
    }

    return decodeExactBytesSlow(input, inputLength)
}

// -----------------------------------------------------------

public actual open class MalformedInputException actual constructor(message: String) : Throwable(message)

private fun CharsetDecoder.decodeExactBytesSlow(input: Input, inputLength: Int): String {
    val decoder = Decoder(charset.name, true)
    var inputRemaining = inputLength
    val sb = StringBuilder(inputLength)

    decodeWrap {
        input.takeWhileSize(6) { buffer ->
            val chunkSize = buffer.readRemaining
            val size = minOf(chunkSize, inputRemaining)
            val text = when {
                buffer.readPosition == 0 && buffer.memory.data.size == size ->
                    decoder.decodeStream(buffer.memory.data)
                else ->
                    decoder.decodeStream(buffer.memory.let { it.slice(buffer.readPosition, size) }.data)
            }
            sb.append(text)

            buffer.discardExact(size)
            inputRemaining -= size

            if (inputRemaining > 0) 6 else 0
        }

        if (inputRemaining > 0) {
            input.takeWhile { buffer ->
                val chunkSize = buffer.readRemaining
                val size = minOf(chunkSize, inputRemaining)
                val text = when {
                    buffer.readPosition == 0 && buffer.memory.data.size == size ->
                        decoder.decode(buffer.memory.data)
                    else ->
                        decoder.decodeStream(buffer.memory.let { it.slice(buffer.readPosition, size) }.data)
                }
                sb.append(text)
                buffer.discardExact(size)
                inputRemaining -= size
                true
            }
        }

        sb.append(decoder.decode())
    }

    if (inputRemaining > 0) {
        throw EOFException(
            "Not enough bytes available: had only ${inputLength - inputRemaining} instead of $inputLength"
        )
    }
    return sb.toString()
}
