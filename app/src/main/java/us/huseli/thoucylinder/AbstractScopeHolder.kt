package us.huseli.thoucylinder

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("MemberVisibilityCanBePrivate")
abstract class AbstractScopeHolder {
    protected val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    protected fun launchOnIOThread(block: suspend CoroutineScope.() -> Unit) =
        scope.launch(Dispatchers.IO, block = block)

    protected fun launchOnMainThread(block: suspend CoroutineScope.() -> Unit) =
        scope.launch(Dispatchers.Main, block = block)

    protected suspend fun <T> onIOThread(block: suspend CoroutineScope.() -> T) =
        withContext(context = Dispatchers.IO, block = block)

    protected suspend fun <T> onMainThread(block: suspend CoroutineScope.() -> T) =
        withContext(context = Dispatchers.Main, block = block)

    protected fun <T> Flow<T>.stateEagerly(initialValue: T): StateFlow<T> =
        stateIn(scope, SharingStarted.Eagerly, initialValue)

    protected fun <T> Flow<T>.stateEagerly(): StateFlow<T?> = stateEagerly(null)

    protected fun <T> Flow<T>.stateLazily(initialValue: T): StateFlow<T> =
        stateIn(scope, SharingStarted.Lazily, initialValue)

    protected fun <T> Flow<T?>.stateLazily(): StateFlow<T?> = stateLazily(null)

    protected fun <T> Flow<T>.stateWhileSubscribed(initialValue: T): StateFlow<T> =
        stateIn(scope, SharingStarted.WhileSubscribed(5_000), initialValue)

    protected fun <T> Flow<T>.stateWhileSubscribed(): StateFlow<T?> = stateWhileSubscribed(null)
}
