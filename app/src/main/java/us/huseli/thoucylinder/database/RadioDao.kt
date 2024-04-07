@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import us.huseli.thoucylinder.dataclasses.entities.Radio
import us.huseli.thoucylinder.dataclasses.entities.RadioTrack
import us.huseli.thoucylinder.dataclasses.views.RadioCombo

@Dao
abstract class RadioDao {
    @Query("DELETE FROM RadioTrack WHERE RadioTrack_radioId = :radioId")
    protected abstract suspend fun _clearRadioTracks(radioId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun _insertRadioTracks(vararg radioTracks: RadioTrack)

    @Query("UPDATE Radio SET Radio_usedSpotifyTrackIds = :spotifyTrackIds, Radio_isInitialized = 1 WHERE Radio_id = :radioId")
    protected abstract suspend fun _setRadioSpotifyTrackIds(radioId: String, spotifyTrackIds: String)

    @Query("DELETE FROM Radio")
    abstract suspend fun clearRadios()

    @Transaction
    @Query("SELECT * FROM RadioCombo WHERE Radio_id = :radioId")
    abstract suspend fun getRadioCombo(radioId: String): RadioCombo?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertRadio(radio: Radio)

    @Transaction
    open suspend fun setLocalTrackIds(radioId: String, trackIds: Iterable<String>) {
        _clearRadioTracks(radioId)
        _insertRadioTracks(*trackIds.map { RadioTrack(radioId = radioId, trackId = it) }.toTypedArray())
    }

    open suspend fun setRadioSpotifyTrackIds(radioId: String, spotifyTrackIds: List<String>) =
        _setRadioSpotifyTrackIds(radioId, gson.toJson(spotifyTrackIds))

    companion object {
        val gson: Gson = GsonBuilder().create()
    }
}
