package us.huseli.thoucylinder

import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MutexCache<K, V>(private val fetchMethod: suspend (K) -> V?) {
    private val cache = mutableMapOf<K, V?>()
    private val mutexes = mutableMapOf<K, Mutex>()

    suspend fun get(key: K, forceReload: Boolean = false): V {
        return mutexes.getOrPut(key) { Mutex() }.withLock {
            val value =
                if (!forceReload && cache.contains(key)) cache[key]
                else fetchMethod(key).also { cache[key] = it }

            if (value == null) throw Exception("value is null for key=$key")
            value
        }
    }

    suspend fun getOrNull(key: K, cacheNulls: Boolean = true, forceReload: Boolean = false): V? = try {
        get(key = key, forceReload = forceReload)
    } catch (e: Exception) {
        Log.e(javaClass.simpleName, "$e")
        if (cacheNulls) cache[key] = null
        null
    }
}
