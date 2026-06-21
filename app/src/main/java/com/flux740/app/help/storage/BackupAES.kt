package com.flux740.app.help.storage

import cn.hutool.crypto.symmetric.AES
import com.flux740.app.help.config.LocalConfig
import com.flux740.app.utils.MD5Utils

class BackupAES : AES(
    MD5Utils.md5Encode(LocalConfig.password ?: "").encodeToByteArray(0, 16)
)