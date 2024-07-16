@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import us.huseli.thoucylinder.dataclasses.radio.Radio
import us.huseli.thoucylinder.dataclasses.radio.RadioCombo
import us.huseli.thoucylinder.dataclasses.track.RadioTrack

@Dao
abstract class RadioDao {
    @Query("SELECT * FROM Radio WHERE Radio_id = :radioId")
    protected abstract suspend fun _getRadio(radioId: String): Radio?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun _insertRadio(radio: Radio)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun _insertRadioTracks(vararg radioTracks: RadioTrack)

    @Query("UPDATE Radio SET Radio_usedSpotifyTrackIds = :spotifyTrackIds WHERE Radio_id = :radioId")
    protected abstract suspend fun _setRadioSpotifyTrackIds(radioId: String, spotifyTrackIds: List<String>)

    suspend fun addLocalTrackId(radioId: String, trackId: String) {
        _insertRadioTracks(RadioTrack(radioId = radioId, trackId = trackId))
    }

    @Transaction
    open suspend fun addSpotifyTrackId(radioId: String, spotifyTrackId: String) {
        _getRadio(radioId)?.also { radio ->
            _setRadioSpotifyTrackIds(radioId, radio.usedSpotifyTrackIds.plus(spotifyTrackId))
        }
    }

    @Query("DELETE FROM Radio")
    abstract suspend fun clearRadios()

    @Transaction
    @Query("SELECT * FROM RadioCombo LIMIT 1")
    abstract fun flowActiveRadio(): Flow<RadioCombo?>

    @Transaction
    open suspend fun setActiveRadio(radio: Radio) {
        clearRadios()
        _insertRadio(radio)
    }

    @Query("UPDATE Radio SET Radio_isInitialized = 1 WHERE Radio_id = :radioId")
    abstract suspend fun setIsInitialized(radioId: String)
}
