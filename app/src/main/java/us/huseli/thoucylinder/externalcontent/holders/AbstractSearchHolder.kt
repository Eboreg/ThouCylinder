package us.huseli.thoucylinder.externalcontent.holders

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import us.huseli.thoucylinder.externalcontent.SearchCapability
import us.huseli.thoucylinder.externalcontent.SearchParams
import us.huseli.thoucylinder.interfaces.IStringIdItem
import us.huseli.thoucylinder.listItemsBetween

abstract class AbstractSearchHolder<T : IStringIdItem> : AbstractHolder<T>() {
    abstract val searchCapabilities: List<SearchCapability>

    private var _currentParams: SearchParams? = null
    private var _currentFlow: Flow<T>? = null
    private val _searchParams = MutableStateFlow<SearchParams?>(null)
    private var _searchJob: Job? = null

    override val _filteredItems: Flow<List<T>> = _items
    override val isEmpty: Flow<Boolean>
        get() = combine(_isLoading, _items, _searchParams) { isSearching, items, params ->
            // Actual meaning: "a search was attempted and the result was empty".
            !isSearching && items.isEmpty() && params != null && params.isNotEmpty()
        }
    override val isLoadingCurrentPage: Flow<Boolean>
        get() = combine(
            _isLoading,
            _items,
            _currentPage,
            _searchParams,
            _allItemsFetched,
        ) { isLoading, items, page, params, allFetched ->
            isLoading && !allFetched && params != null && items.size < ((page + 1) * ITEMS_PER_PAGE)
        }

    val searchParams = _searchParams.asStateFlow()

    protected abstract fun getResultChannel(searchParams: SearchParams): Channel<T>

    override suspend fun doStart() {
        _searchParams.filterNotNull().filter { it.isNotEmpty() }.collectLatest { params ->
            val channel = getResultChannel(params)

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
                    _allItemsFetched.value = true
                }
                _isLoading.value = false
            }
        }
    }

    fun onItemLongClick(itemId: String) {
        val itemIds = _selectedItemIds.value.lastOrNull()
            ?.let { id ->
                _items.value
                    .map { it.id }
                    .listItemsBetween(id, itemId)
                    .plus(itemId)
                    .minus(_selectedItemIds.value.toSet())
            }
            ?: listOf(itemId)

        _selectedItemIds.value += itemIds
    }

    fun setSearchParams(value: SearchParams) {
        _searchParams.value = value
        _items.value = emptyList()
        _selectedItemIds.value = emptyList()
    }
}
