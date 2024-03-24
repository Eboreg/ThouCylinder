package us.huseli.thoucylinder.repositories

import android.content.Context
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.Constants.PREF_ACTIVE_RADIO_ID
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.entities.Radio
import us.huseli.thoucylinder.dataclasses.views.RadioCombo
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RadioRepository @Inject constructor(database: Database, @ApplicationContext context: Context) {
    private val radioDao = database.radioDao()
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _activeRadio = MutableStateFlow<RadioCombo?>(null)

    val activeRadio = _activeRadio.asStateFlow()

    init {
        scope.launch {
            preferences.getString(PREF_ACTIVE_RADIO_ID, null)?.also {
                _activeRadio.value = radioDao.getRadioCombo(UUID.fromString(it))
            }
        }
    }

    fun deactivateRadio() {
        _activeRadio.value = null
        preferences.edit().putString(PREF_ACTIVE_RADIO_ID, null).apply()
        scope.launch { radioDao.clearRadios() }
    }

    suspend fun setActiveRadio(radio: Radio) {
        preferences.edit().putString(PREF_ACTIVE_RADIO_ID, radio.id.toString()).apply()
        radioDao.clearRadios()
        radioDao.insertRadio(radio)
        _activeRadio.value = radioDao.getRadioCombo(radio.id)
    }

    suspend fun updateRadio(radioId: UUID, spotifyTrackIds: List<String>, localTrackIds: Iterable<UUID>) {
        radioDao.setRadioSpotifyTrackIds(radioId = radioId, spotifyTrackIds = spotifyTrackIds)
        radioDao.setLocalTrackIds(radioId, localTrackIds)
    }
}
