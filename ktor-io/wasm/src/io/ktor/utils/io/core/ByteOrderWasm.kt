// ktlint-disable filename
package io.ktor.utils.io.core


public actual enum class ByteOrder {
    BIG_ENDIAN, LITTLE_ENDIAN;

    public actual companion object {
        public actual fun nativeOrder(): ByteOrder = LITTLE_ENDIAN
    }
}
