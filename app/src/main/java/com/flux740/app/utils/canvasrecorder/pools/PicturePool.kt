package com.flux740.app.utils.canvasrecorder.pools

import android.graphics.Picture
import com.flux740.app.utils.objectpool.BaseObjectPool

class PicturePool : BaseObjectPool<Picture>(64) {

    override fun create(): Picture = Picture()

}
