package us.huseli.thoucylinder.dataclasses.lastFm

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import us.huseli.thoucylinder.Request
import us.huseli.thoucylinder.dataclasses.entities.LastFmAlbum

data class LastFmAlbumResponse(val album: Album) {
    data class Album(
        val artist: String,
        val name: String,
        val tracks: Tracks?,
        val listeners: String,
        val image: List<LastFmImage>,
        val playcount: String,
        val url: String,
        val wiki: LastFmWiki?,
        val tags: Tags?,
    ) {
        data class Tags(val tag: List<Tag>) {
            data class Tag(
                val url: String,
                val name: String,
            )
        }

        data class Tracks(val track: List<Track>) {
            data class Track(
                val duration: Int,
                val url: String,
                val name: String,
                val artist: LastFmArtist,
                val mbid: String?,
            )
        }

        fun toEntity(musicBrainzId: String, artist: LastFmArtist) = LastFmAlbum(
            musicBrainzId = musicBrainzId,
            artist = artist,
            name = name,
            tracks = tracks?.track ?: emptyList(),
            listeners = listeners.toIntOrNull(),
            playCount = playcount.toIntOrNull(),
            tags = tags?.tag?.map { it.name } ?: emptyList(),
            wiki = wiki,
            url = url,
            fullImageUrl = image.getFullImage()?.url,
            thumbnailUrl = image.getThumbnail()?.url,
        )
    }
}

object LastFmTagsTypeAdapter : TypeAdapter<LastFmAlbumResponse.Album.Tags>() {
    private val responseType = object : TypeToken<LastFmAlbumResponse.Album.Tags>() {}
    private val gson: Gson = GsonBuilder().create()

    override fun write(out: JsonWriter?, value: LastFmAlbumResponse.Album.Tags?) {
        out?.value(value?.let { gson.toJson(value, LastFmAlbumResponse.Album.Tags::class.java) })
    }

    override fun read(`in`: JsonReader?): LastFmAlbumResponse.Album.Tags? {
        return when (`in`?.peek()) {
            JsonToken.NULL -> {
                `in`.nextNull()
                null
            }
            JsonToken.STRING -> {
                `in`.nextString()
                null
            }
            else -> gson.fromJson(`in`, responseType)
        }
    }
}

suspend fun Request.getLastFmAlbum(): LastFmAlbumResponse.Album? {
    val responseType = object : TypeToken<LastFmAlbumResponse>() {}
    val gson: Gson =
        GsonBuilder().registerTypeAdapter(LastFmAlbumResponse.Album.Tags::class.java, LastFmTagsTypeAdapter).create()
    val string = getString()

    return try {
        gson.fromJson(string, responseType)?.album
    } catch (e: Exception) {
        Log.e(javaClass.simpleName, string, e)
        null
    }
}
