package us.huseli.thoucylinder

import android.content.Context
import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MutexCache<K, V>(private val fetchMethod: suspend (K) -> V?) {
    private val mutex = Mutex()
    private val cache = mutableMapOf<K, V?>()

    suspend fun get(key: K, forceReload: Boolean = false): V? {
        return mutex.withLock {
            val value = if (!forceReload && cache.contains(key)) cache[key] else try {
                fetchMethod(key)
            } catch (e: Exception) {
                Log.e(javaClass.simpleName, "$e")
                null
            }

            cache[key] = value
            value
        }
    }
}

class ContextMutexCache<I, K, V>(
    private val fetchMethod: suspend (I, Context) -> V?,
    private val keyFromInstance: (I) -> K,
) {
    private val mutex = Mutex()
    private val cache = mutableMapOf<K, V?>()

    suspend fun get(instance: I, context: Context, forceReload: Boolean = false): V? {
        return mutex.withLock {
            val key = keyFromInstance(instance)
            val value = if (!forceReload && cache.contains(key)) cache[key] else try {
                fetchMethod(instance, context)?.also { cache[key] = it }
            } catch (e: Exception) {
                Log.e(javaClass.simpleName, "$e")
                null
            }

            cache[key] = value
            value
        }
    }
}
