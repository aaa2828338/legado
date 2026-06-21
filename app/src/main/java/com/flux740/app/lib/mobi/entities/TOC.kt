package com.flux740.app.lib.mobi.entities

data class TOC(
    val label: String,
    val href: String,
    val subitems: List<TOC>? = null
)
