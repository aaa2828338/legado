package com.flux740.app.ui.qrcode

import com.google.zxing.Result

interface ScanResultCallback {

    fun onScanResultCallback(result: Result?)

}