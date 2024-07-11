package us.huseli.thoucylinder

import com.google.gson.GsonBuilder
import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.converters.ConversionException
import com.thoughtworks.xstream.converters.MarshallingContext
import com.thoughtworks.xstream.converters.UnmarshallingContext
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider
import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.io.HierarchicalStreamWriter
import com.thoughtworks.xstream.mapper.Mapper
import us.huseli.retaintheme.extensions.filterValuesNotNull
import us.huseli.thoucylinder.dataclasses.artist.joined
import us.huseli.thoucylinder.dataclasses.track.TrackCombo
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

data class XSPFPlaylist(
    val created: OffsetDateTime = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS),
    val title: String? = null,
    val creator: String? = null,
    val annotation: String? = null,
    val info: String? = null,
    val location: String? = null,
    val identifier: String? = null,
    val image: String? = null,
    val date: String? = created.toString(),
    val license: String? = null,
    val link: List<Link>? = null,
    val meta: List<Link>? = null,
    val trackList: List<XSPFTrack>,
) {
    class Converter(mapper: Mapper, reflectionProvider: ReflectionProvider) :
        ReflectionConverter(mapper, reflectionProvider) {
        override fun canConvert(type: Class<*>?): Boolean =
            type != null && XSPFPlaylist::class.java.isAssignableFrom(type)

        override fun marshal(source: Any?, writer: HierarchicalStreamWriter?, context: MarshallingContext?) {
            writer?.addAttribute("version", "1")
            writer?.addAttribute("xmlns", "http://xspf.org/ns/0/")
            super.marshal(source, writer, context)
        }
    }

    data class Link(val rel: String, val content: String) {
        object Converter : com.thoughtworks.xstream.converters.Converter {
            override fun canConvert(type: Class<*>?): Boolean = type != null && Link::class.java.isAssignableFrom(type)

            override fun marshal(source: Any?, writer: HierarchicalStreamWriter?, context: MarshallingContext?) {
                val link = source as Link

                writer?.addAttribute("rel", link.rel)
                writer?.setValue(link.content)
            }

            override fun unmarshal(reader: HierarchicalStreamReader?, context: UnmarshallingContext?): Any {
                val rel = reader?.getAttribute("rel")
                val content = reader?.value

                if (rel != null && content != null) return Link(rel = rel, content = content)
                throw ConversionException("Missing rel and/or content")
            }
        }
    }

    data class XSPFTrack(
        val location: List<String>? = null,
        val identifier: List<String>? = null,
        val title: String? = null,
        val creator: String? = null,
        val annotation: String? = null,
        val info: String? = null,
        val image: String? = null,
        val album: String? = null,
        val trackNum: Int? = null,
        val duration: Long? = null,
        val link: List<Link>? = null,
        val meta: List<Link>? = null,
    ) {
        fun toMap() = mapOf(
            "location" to location,
            "identifier" to identifier,
            "title" to title,
            "creator" to creator,
            "annotation" to annotation,
            "info" to info,
            "image" to image,
            "album" to album,
            "trackNum" to trackNum,
            "duration" to duration,
            "link" to link?.associate { it.rel to it.content },
            "meta" to meta?.associate { it.rel to it.content },
        ).filterValuesNotNull()
    }

    private fun toMap() = mapOf(
        "playlist" to mapOf(
            "title" to title,
            "creator" to creator,
            "annotation" to annotation,
            "info" to info,
            "location" to location,
            "identifier" to identifier,
            "image" to image,
            "date" to date,
            "license" to license,
            "link" to link?.associate { it.rel to it.content },
            "meta" to meta?.associate { it.rel to it.content },
            "track" to trackList.map { track -> track.toMap() },
        ).filterValuesNotNull(),
    )

    fun toJson(): String = GsonBuilder().setPrettyPrinting().create().toJson(toMap())

    fun toXml(): String? {
        val xstream = XStream()

        xstream.omitField(XSPFPlaylist::class.java, "created")
        xstream.alias("playlist", XSPFPlaylist::class.java)
        xstream.alias("link", Link::class.java)
        xstream.alias("track", XSPFTrack::class.java)
        xstream.useAttributeFor(Link::class.java, "rel")
        xstream.addImplicitCollection(XSPFPlaylist::class.java, "link", "link", Link::class.java)
        xstream.addImplicitCollection(XSPFPlaylist::class.java, "meta", "meta", Link::class.java)
        xstream.addImplicitCollection(XSPFTrack::class.java, "link", "link", Link::class.java)
        xstream.addImplicitCollection(XSPFTrack::class.java, "meta", "meta", Link::class.java)
        xstream.addImplicitCollection(XSPFTrack::class.java, "location", "location", String::class.java)
        xstream.addImplicitCollection(XSPFTrack::class.java, "identifier", "identifier", String::class.java)
        xstream.registerConverter(Link.Converter)
        xstream.registerConverter(Converter(xstream.mapper, xstream.reflectionProvider))

        return xstream.toXML(this)?.let { "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n$it" }
    }

    companion object {
        private const val MB_RECORDING_IRI = "https://musicbrainz.org/recording"
        private const val MB_RELEASE_IRI = "https://musicbrainz.org/release"
        private const val MB_RELEASE_GROUP_IRI = "https://musicbrainz.org/release-group"

        fun fromTrackCombos(
            combos: Iterable<TrackCombo>,
            title: String? = null,
            identifier: String? = null,
            info: String? = null,
            dateTime: OffsetDateTime = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS),
        ): XSPFPlaylist {
            val xspfTracks = combos.map { combo ->
                val meta = mutableListOf<Link>()

                combo.albumArtists.joined()?.also { meta.add(Link(rel = "album-artist", content = it)) }
                combo.year?.also { meta.add(Link(rel = "year", content = it.toString())) }
                combo.track.discNumber?.also { meta.add(Link(rel = "disc-number", content = it.toString())) }
                combo.track.musicBrainzId?.also { meta.add(Link(rel = MB_RECORDING_IRI, content = it)) }
                combo.album?.musicBrainzReleaseId?.also { meta.add(Link(rel = MB_RELEASE_IRI, content = it)) }
                combo.album?.musicBrainzReleaseGroupId
                    ?.also { meta.add(Link(rel = MB_RELEASE_GROUP_IRI, content = it)) }
                combo.track.spotifyId?.also { meta.add(Link(rel = "spotify-track-id", content = it)) }
                combo.album?.spotifyId?.also { meta.add(Link(rel = "spotify-album-id", content = it)) }
                combo.track.youtubeVideo?.id?.also { meta.add(Link(rel = "youtube-video-id", content = it)) }
                combo.album?.youtubePlaylist?.id?.also { meta.add(Link(rel = "youtube-playlist-id", content = it)) }
                combo.track.metadata?.bitrate
                    ?.also { meta.add(Link(rel = "bitrate", content = (it / 1000).toString())) }
                combo.track.metadata?.mimeType?.also { meta.add(Link(rel = "mime-type", content = it)) }
                combo.track.metadata?.loudnessDb?.also { meta.add(Link(rel = "loudness", content = it.toString())) }

                XSPFTrack(
                    title = combo.track.title,
                    location = combo.track.playUri?.let { listOf(it) },
                    creator = combo.artistString,
                    album = combo.album?.title,
                    trackNum = combo.track.albumPosition,
                    image = combo.fullImageUrl,
                    duration = combo.track.duration?.inWholeMilliseconds,
                    meta = meta.takeIf { it.isNotEmpty() },
                )
            }

            return XSPFPlaylist(
                created = dateTime,
                title = title,
                identifier = identifier,
                info = info,
                trackList = xspfTracks,
                creator = "Fistopy ${BuildConfig.VERSION_NAME}",
            )
        }
    }
}
