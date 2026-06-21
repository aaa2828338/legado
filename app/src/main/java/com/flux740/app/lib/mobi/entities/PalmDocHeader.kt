package com.flux740.app.lib.mobi.entities

data class PalmDocHeader(
    val compression: Int,
    val numTextRecords: Int,
    val recordSize: Int,
    val encryption: Int
)
