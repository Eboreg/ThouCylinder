package us.huseli.thoucylinder

class MediaStoreFormatException(val filename: String, override val cause: Throwable? = null) : Exception(cause)
