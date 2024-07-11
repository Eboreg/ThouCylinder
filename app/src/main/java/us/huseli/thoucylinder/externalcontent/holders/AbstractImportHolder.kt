package us.huseli.thoucylinder.externalcontent.holders

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import us.huseli.thoucylinder.interfaces.IStringIdItem

abstract class AbstractImportHolder<T : IStringIdItem> : AbstractHolder<T>() {
    private val _searchTerm = MutableStateFlow("")

    val searchTerm = _searchTerm.asStateFlow()

    val isLoadingCurrentPage: Flow<Boolean>
        get() = combine(_isLoading, _filteredItems, _currentPage) { isLoading, items, page ->
            isLoading && items.size < ((page + 1) * ITEMS_PER_PAGE)
        }

    override val _filteredItems: Flow<List<T>> = combine(_items, _searchTerm) { items, searchTerm ->
        if (searchTerm.isNotBlank()) items.filter { itemMatchesSearchTerm(it, searchTerm) }
        else items
    }

    abstract fun getResultChannel(): Channel<T>
    abstract fun itemMatchesSearchTerm(item: T, term: String): Boolean
    abstract fun onItemImportError(itemId: String, error: String)
    abstract fun onItemImportFinished(itemId: String)

    override suspend fun doStart() {
        val channel = getResultChannel()

        combine(_currentPage, _filteredItems) { page, items ->
            ((page + 2) * ITEMS_PER_PAGE) - items.size
        }.filter { it > 0 }.collectLatest { required ->
            _isLoading.value = true
            try {
                repeat(required) {
                    val item = channel.receive()
                    _items.value += item
                }
            } catch (_: ClosedReceiveChannelException) {
            }
            _isLoading.value = false
        }
    }

    fun setSearchTerm(value: String) {
        if (value != _searchTerm.value) {
            _searchTerm.value = value
            _currentPage.value = 0
        }
    }
}
