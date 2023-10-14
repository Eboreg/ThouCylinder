package us.huseli.thoucylinder.dataclasses

data class DiscogsSearchResultItem(val title: String, val year: String?, val id: Int) {
    override fun toString() = year?.let { "$title ($year)" } ?: title
}

data class DiscogsSearchResultsData(val results: List<DiscogsSearchResultItem>)

data class DiscogsSearchResults(val data: DiscogsSearchResultsData)
