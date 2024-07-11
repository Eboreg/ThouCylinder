package us.huseli.thoucylinder.externalcontent.holders

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import us.huseli.retaintheme.extensions.slice
import us.huseli.thoucylinder.AbstractScopeHolder
import us.huseli.thoucylinder.interfaces.IStringIdItem

@Suppress("PropertyName")
abstract class AbstractHolder<T : IStringIdItem> : AbstractScopeHolder() {
    private var _isStarted = false

    protected val _items = MutableStateFlow<List<T>>(emptyList())
    protected val _isLoading = MutableStateFlow(true)
    protected val _selectedItemIds = MutableStateFlow<List<String>>(emptyList())
    protected val _currentPage = MutableStateFlow(0)

    val currentPage = _currentPage.asStateFlow()
    val selectedItemIds = _selectedItemIds.asStateFlow()

    protected abstract val _filteredItems: Flow<List<T>>
    abstract val isTotalCountExact: Flow<Boolean>

    abstract suspend fun doStart()

    open val isEmpty: Flow<Boolean>
        get() = combine(_isLoading, _filteredItems) { isLoading, items ->
            !isLoading && items.isEmpty()
        }
    open val totalItemCount: Flow<Int>
        get() = _filteredItems.map { it.size }

    val canSelectAll: Flow<Boolean>
        get() = currentPageItems.map { it.isNotEmpty() }
    val currentPageItems: Flow<List<T>>
        get() = combine(_currentPage, _filteredItems) { page, items ->
            items.slice(page * ITEMS_PER_PAGE, ITEMS_PER_PAGE)
        }
    val displayOffset = _currentPage.map { it * ITEMS_PER_PAGE }
    val hasNextPage: Flow<Boolean>
        get() = combine(_filteredItems, _currentPage) { items, page ->
            items.size > (page + 1) * ITEMS_PER_PAGE
        }
    val isWholeCurrentPageSelected: Flow<Boolean>
        get() = combine(currentPageItems, _selectedItemIds) { items, selectedIds ->
            items.isNotEmpty() && selectedIds.containsAll(items.map { it.id })
        }

    fun deselectAll() {
        _selectedItemIds.value = emptyList()
    }

    fun deselectItem(itemId: String) {
        _selectedItemIds.value -= itemId
    }

    suspend fun getSelectedItems(): List<T> = currentPageItems.first().filter { _selectedItemIds.value.contains(it.id) }

    fun gotoNextPage() {
        _currentPage.value++
    }

    fun gotoPreviousPage() {
        if (_currentPage.value > 0) _currentPage.value--
    }

    fun selectAll() {
        launchOnIOThread {
            _selectedItemIds.value = currentPageItems.first().map { it.id }
        }
    }

    fun start() {
        if (!_isStarted) {
            _isStarted = true
            launchOnIOThread { doStart() }
        }
    }

    fun toggleItemSelected(itemId: String) {
        if (_selectedItemIds.value.contains(itemId)) _selectedItemIds.value -= itemId
        else _selectedItemIds.value += itemId
    }

    companion object {
        const val ITEMS_PER_PAGE = 50
    }
}
