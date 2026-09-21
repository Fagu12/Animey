package com.example.data.extension.mangayomi.bridge

import java.util.concurrent.ConcurrentHashMap

/**
 * Isolated key-value storage bridge for a single Mangayomi extension.
 * Provides standard localStorage/storage semantics without granting raw filesystem access.
 */
class MangayomiStorageBridge(
    val extensionId: String,
    private val store: MutableMap<String, String> = ConcurrentHashMap()
) {

    fun getItem(key: String): String? {
        return store[key]
    }

    fun setItem(key: String, value: Any?) {
        if (value != null) {
            store[key] = value.toString()
        } else {
            store.remove(key)
        }
    }

    fun removeItem(key: String) {
        store.remove(key)
    }

    fun clear() {
        store.clear()
    }

    fun key(index: Int): String? {
        return store.keys.toList().getOrNull(index)
    }

    val length: Int
        get() = store.size
}
