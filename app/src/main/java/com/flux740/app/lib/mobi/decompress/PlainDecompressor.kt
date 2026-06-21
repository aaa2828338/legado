package com.flux740.app.lib.mobi.decompress

class PlainDecompressor : Decompressor {

    override fun decompress(data: ByteArray): ByteArray {
        return data
    }

}
