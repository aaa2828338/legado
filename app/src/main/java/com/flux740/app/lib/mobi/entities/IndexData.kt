package com.flux740.app.lib.mobi.entities

import android.util.SparseArray

data class IndexData(
    val table: List<IndexEntry>,
    val cncx: SparseArray<String>
)
