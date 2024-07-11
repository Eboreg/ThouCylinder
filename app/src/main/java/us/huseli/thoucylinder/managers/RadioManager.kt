package us.huseli.thoucylinder.managers

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import us.huseli.thoucylinder.AbstractScopeHolder
import us.huseli.thoucylinder.RadioTrackChannel
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.radio.Radio
import us.huseli.thoucylinder.dataclasses.radio.RadioUiState
import us.huseli.thoucylinder.dataclasses.radio.RadioCombo
import us.huseli.thoucylinder.enums.RadioStatus
import us.huseli.thoucylinder.enums.RadioType
import us.huseli.thoucylinder.interfaces.ILogger
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RadioManager @Inject constructor(
    database: Database,
    private val repos: Repositories,
) : AbstractScopeHolder(), ILogger {
    private var radioJob: Job? = null
    private var worker: RadioTrackChannel? = null

    private val radioDao = database.radioDao()
    private val radioStatus = MutableStateFlow(RadioStatus.INACTIVE)

    val activeRadioCombo: Flow<RadioCombo?> = radioDao.flowActiveRadio()
        .distinctUntilChanged()
        .filter { it == null || it.type == RadioType.LIBRARY || it.title != null }
        .distinctUntilChanged()
    val radioUiState: Flow<RadioUiState?> = combine(activeRadioCombo, radioStatus) { radio, status ->
        if (status != RadioStatus.INACTIVE) radio?.let { RadioUiState(type = it.type, title = it.title) } else null
    }

    init {
        launchOnIOThread {
            activeRadioCombo.filterNotNull().collect {
                worker?.cancel()
                radioJob?.cancel()
                radioJob = launchOnMainThread { startRadio(it) }
            }
        }
        launchOnIOThread {
            for (replaced in repos.player.replaceSignal) deactivateRadio()
        }
    }

    fun deactivateRadio() {
        worker?.cancel()
        worker = null
        radioJob?.cancel()
        radioJob = null
        radioStatus.value = RadioStatus.INACTIVE
        launchOnIOThread { radioDao.clearRadios() }
    }

    fun startAlbumRadio(albumId: String) {
        launchOnIOThread { radioDao.setActiveRadio(Radio(albumId = albumId, type = RadioType.ALBUM)) }
    }

    fun startArtistRadio(artistId: String) {
        launchOnIOThread { radioDao.setActiveRadio(Radio(artistId = artistId, type = RadioType.ARTIST)) }
    }

    fun startLibraryRadio() {
        launchOnIOThread { radioDao.setActiveRadio(Radio(type = RadioType.LIBRARY)) }
    }

    fun startTrackRadio(trackId: String) {
        launchOnIOThread { radioDao.setActiveRadio(Radio(trackId = trackId, type = RadioType.TRACK)) }
    }


    /** PRIVATE METHODS ***********************************************************************************************/

    private suspend fun handleNextRadioTrack(worker: RadioTrackChannel, clearAndPlay: Boolean = false) {
        if (clearAndPlay) repos.player.clearQueue()

        val combo = onIOThread { worker.channel.receive() }

        if (clearAndPlay) repos.player.insertLastAndPlay(combo.queueTrackCombo)
        else repos.player.insertLast(combo.queueTrackCombo)
        if (combo.localId != null) radioDao.addLocalTrackId(worker.radio.id, combo.localId)
        if (combo.spotifyId != null) radioDao.addSpotifyTrackId(worker.radio.id, combo.spotifyId)
    }

    private suspend fun startRadio(radio: RadioCombo) {
        val worker = RadioTrackChannel(radio = radio, repos = repos)

        this.worker = worker
        radioStatus.value = RadioStatus.LOADING

        try {
            if (!radio.isInitialized) {
                handleNextRadioTrack(worker = worker, clearAndPlay = true)
                radioDao.setIsInitialized(worker.radio.id)
            }

            combineTransform(repos.player.trackCount, repos.player.tracksLeft) { trackCount, tracksLeft ->
                if (trackCount < 20 || tracksLeft < 5) {
                    if (radioStatus.value == RadioStatus.LOADED) radioStatus.value = RadioStatus.LOADING_MORE
                    emit(true)
                } else radioStatus.value = RadioStatus.LOADED
            }.collect {
                handleNextRadioTrack(worker = worker)
            }
        } catch (e: ClosedReceiveChannelException) {
            deactivateRadio()
        } catch (e: Throwable) {
            logError(e)
        }
    }
}
