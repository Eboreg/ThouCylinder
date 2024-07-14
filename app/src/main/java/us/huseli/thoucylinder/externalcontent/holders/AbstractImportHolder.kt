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
    private val _previouslyImportedIds = mutableListOf<String>()

    abstract val canImport: Flow<Boolean>

    override val isEmpty: Flow<Boolean>
        get() = combine(_isLoading, _filteredItems, canImport) { isLoading, items, canImport ->
            !isLoading && canImport && items.isEmpty()
        }
    override val isLoadingCurrentPage: Flow<Boolean>
        get() = combine(
            _isLoading,
            _filteredItems,
            _currentPage,
            canImport,
            _allItemsFetched,
        ) { isLoading, items, page, canImport, allFetched ->
            canImport && isLoading && !allFetched && items.size < ((page + 1) * ITEMS_PER_PAGE)
        }
    override val _filteredItems: Flow<List<T>> = combine(_items, _searchTerm) { items, searchTerm ->
        if (searchTerm.isNotBlank()) items.filter { itemMatchesSearchTerm(it, searchTerm) }
        else items
    }

    val searchTerm = _searchTerm.asStateFlow()

    abstract suspend fun getPreviouslyImportedIds(): List<String>
    abstract fun getResultChannel(): Channel<T>
    abstract fun itemMatchesSearchTerm(item: T, term: String): Boolean
    abstract fun onItemImportError(itemId: String, error: String)
    abstract fun onItemImportFinished(itemId: String)

    override suspend fun doStart() {
        val channel = getResultChannel()

        _previouslyImportedIds.addAll(getPreviouslyImportedIds())

        combine(_currentPage, _filteredItems, _allItemsFetched) { page, items, allFetched ->
            if (allFetched) 0
            else ((page + 2) * ITEMS_PER_PAGE) - items.size
        }.filter { it > 0 }.collectLatest { required ->
            _isLoading.value = true
            try {
                repeat(required) {
                    val item = channel.receive()

                    if (!_previouslyImportedIds.contains(item.id)) {
                        _items.value += item
                    }
                }
            } catch (_: ClosedReceiveChannelException) {
                _allItemsFetched.value = true
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
