package us.huseli.thoucylinder

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class CachedValue<V>(val value: V?, val timestamp: Long = System.currentTimeMillis())

open class MutexCache<I, K, V>(
    private val itemToKey: (I) -> K,
    private val fetchMethod: suspend MutexCache<I, K, V>.(I) -> V?,
    private val debugLabel: String? = null,
    private val retentionMs: Long = 20_000L,
) {
    private val cache = mutableMapOf<K, CachedValue<V>>()
    private val mutexes = mutableMapOf<K, Mutex>()
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            while (true) {
                val oldKeys = cache.filterValues { it.timestamp < System.currentTimeMillis() - retentionMs }.keys
                if (oldKeys.isNotEmpty()) {
                    val prefix = debugLabel?.let { "[$it] " } ?: ""
                    Log.i("MutexCache", "${prefix}Throwing ${oldKeys.size} items")
                    cache.minusAssign(oldKeys)
                }
                delay(retentionMs)
            }
        }
    }

    suspend fun get(item: I, forceReload: Boolean = false, retryOnNull: Boolean = false): V {
        return getValueSync(item = item, forceReload = forceReload, retryOnNull = retryOnNull)
            ?: throw Exception("value is null for item=$item")
    }

    suspend fun getOrNull(item: I, forceReload: Boolean = false, retryOnNull: Boolean = false): V? = try {
        getValueSync(item = item, forceReload = forceReload, retryOnNull = retryOnNull)
    } catch (e: Exception) {
        Log.e(javaClass.simpleName, e.toString())
        null
    }

    fun update(key: K, value: V?) {
        cache[key] = CachedValue(value)
    }

    private suspend fun getValueSync(
        item: I,
        forceReload: Boolean = false,
        retryOnNull: Boolean = false,
    ): V? {
        val key = itemToKey(item)

        return mutexes.getOrPut(key) { Mutex() }.withLock {
            if (cache.containsKey(key)) {
                if (forceReload || (cache[key] == null && retryOnNull))
                    fetchMethod(item).also { cache[key] = CachedValue(it) }
                else cache[key]?.value
            } else fetchMethod(item).also { cache[key] = CachedValue(it) }
        }
    }
}


fun <I, V> getMutexCache(debugLabel: String? = null, fetchMethod: suspend MutexCache<I, I, V>.(I) -> V?) =
    MutexCache(itemToKey = { it }, fetchMethod = fetchMethod, debugLabel = debugLabel)
