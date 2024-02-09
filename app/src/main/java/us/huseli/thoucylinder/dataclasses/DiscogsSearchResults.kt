package us.huseli.thoucylinder.dataclasses

data class DiscogsSearchResults(val data: Data) {
    data class Data(val results: List<Result>)

    data class Result(val title: String, val year: String?, val id: Int) {
        override fun toString() = year?.let { "$title ($year)" } ?: title
    }
}
