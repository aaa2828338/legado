package com.flux740.app.utils.objectpool

interface ObjectPool<T> {

    fun obtain(): T

    fun recycle(target: T)

    fun create(): T

}
