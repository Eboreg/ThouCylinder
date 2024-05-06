package us.huseli.thoucylinder.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import us.huseli.thoucylinder.interfaces.ILogger

abstract class AbstractBaseViewModel : ViewModel(), ILogger {
    protected fun <T> Flow<T>.stateWhileSubscribed(initialValue: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue)

    protected fun <T> Flow<T>.stateEagerly(initialValue: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.Eagerly, initialValue)

    protected fun <T> Flow<T>.stateEagerly(): StateFlow<T?> = stateEagerly(null)

    protected fun <T> Flow<T>.stateLazily(initialValue: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.Lazily, initialValue)

    protected fun <T> Flow<T?>.stateLazily(): StateFlow<T?> = stateLazily(null)
}
