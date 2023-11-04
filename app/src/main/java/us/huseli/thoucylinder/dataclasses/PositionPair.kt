package us.huseli.thoucylinder.dataclasses

data class PositionPair(val discNumber: Int, val albumPosition: Int) : Comparable<PositionPair> {
    override fun compareTo(other: PositionPair): Int {
        if (discNumber != other.discNumber) return discNumber - other.discNumber
        return albumPosition - other.albumPosition
    }
}
