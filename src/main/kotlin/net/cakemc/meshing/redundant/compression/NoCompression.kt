package net.cakemc.meshing.redundant.compression

class NoCompression: ByteCompression() {
    /**
     * Compresses the given byte array.
     *
     * This method is responsible for reducing the size of the input byte array
     * using a specific compression algorithm.
     *
     * @param source The byte array to compress.
     * @return A new byte array containing the compressed data.
     */
    override fun compress(source: ByteArray): ByteArray {
        return source
    }

    /**
     * Decompresses the given byte array.
     *
     * This method restores the original data from the compressed byte array,
     * reversing the effect of the `compress()` method.
     *
     * @param source The compressed byte array to decompress.
     * @return A new byte array containing the decompressed data.
     */
    override fun decompress(source: ByteArray): ByteArray {
        return source
    }
}