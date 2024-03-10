@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import us.huseli.thoucylinder.dataclasses.entities.Radio
import us.huseli.thoucylinder.dataclasses.entities.RadioTrack
import us.huseli.thoucylinder.dataclasses.views.RadioView
import java.util.UUID

@Dao
abstract class RadioDao {
    @Query("DELETE FROM RadioTrack WHERE RadioTrack_radioId = :radioId")
    protected abstract suspend fun _clearRadioTracks(radioId: UUID)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun _insertRadioTracks(vararg radioTracks: RadioTrack)

    @Query("DELETE FROM Radio")
    abstract suspend fun clearRadios()

    @Transaction
    @Query("SELECT * FROM RadioView WHERE Radio_id = :radioId")
    abstract suspend fun getRadioView(radioId: UUID): RadioView?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertRadio(radio: Radio)

    @Transaction
    open suspend fun setLocalTrackIds(radioId: UUID, trackIds: Iterable<UUID>) {
        _clearRadioTracks(radioId)
        _insertRadioTracks(*trackIds.map { RadioTrack(radioId = radioId, trackId = it) }.toTypedArray())
    }

    @Query("UPDATE Radio SET Radio_usedSpotifyTrackIds = :spotifyTrackIds, Radio_isInitialized = 1 WHERE Radio_id = :radioId")
    abstract suspend fun setRadioSpotifyTrackIds(radioId: UUID, spotifyTrackIds: List<String>)
}
