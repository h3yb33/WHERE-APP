package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AbujaMemoryDao {

    @Query("SELECT * FROM learned_corridors ORDER BY tripCount DESC, updatedAtMillis DESC")
    fun observeLearnedCorridors(): Flow<List<LearnedCorridorEntity>>

    @Query("SELECT * FROM learned_corridors ORDER BY tripCount DESC, updatedAtMillis DESC")
    suspend fun getLearnedCorridorsSnapshot(): List<LearnedCorridorEntity>

    @Query(
        "SELECT * FROM learned_corridors WHERE LOWER(originArea) LIKE '%' || LOWER(:query) || '%' OR LOWER(destinationArea) LIKE '%' || LOWER(:query) || '%' OR LOWER(corridorTitle) LIKE '%' || LOWER(:query) || '%' LIMIT 5"
    )
    suspend fun searchCorridors(query: String): List<LearnedCorridorEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLearnedCorridor(corridor: LearnedCorridorEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLearnedCorridors(corridors: List<LearnedCorridorEntity>)

    @Query("SELECT * FROM trip_surveys ORDER BY createdAtMillis DESC")
    fun observeTripSurveys(): Flow<List<TripSurveyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTripSurvey(survey: TripSurveyEntity)

    @Query("SELECT * FROM saved_routes ORDER BY createdAtMillis DESC")
    fun observeSavedRoutes(): Flow<List<SavedRouteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedRoute(route: SavedRouteEntity)

    @Query("DELETE FROM saved_routes WHERE id = :id")
    suspend fun deleteSavedRouteById(id: Int)

    @Query("SELECT * FROM saved_places ORDER BY createdAtMillis DESC")
    fun observeSavedPlaces(): Flow<List<SavedPlaceEntity>>

    @Query("SELECT * FROM saved_places ORDER BY createdAtMillis DESC")
    suspend fun getSavedPlacesSnapshot(): List<SavedPlaceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedPlace(place: SavedPlaceEntity)

    @Query("DELETE FROM saved_places WHERE id = :id")
    suspend fun deleteSavedPlaceById(id: Int)

    @Query("DELETE FROM learned_corridors")
    suspend fun clearLearnedCorridors()

    @Query("DELETE FROM trip_surveys")
    suspend fun clearTripSurveys()

    @Query("DELETE FROM saved_routes")
    suspend fun clearSavedRoutes()

    @Query("DELETE FROM saved_places")
    suspend fun clearSavedPlaces()
}
