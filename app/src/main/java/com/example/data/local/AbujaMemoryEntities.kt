package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learned_corridors")
data class LearnedCorridorEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originArea: String,
    val destinationArea: String,
    val corridorTitle: String,
    val trekAlightPoint: String,
    val commercialBikePoint: String,
    val kekeTimeWindow: String,
    val kekeCommercialAllowed: String,
    val bikeCommercialAllowed: String,
    val verifiedFareNaira: Int,
    val routeSummary: String,
    val tripCount: Int = 1,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "trip_surveys")
data class TripSurveyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originName: String,
    val destinationName: String,
    val alightAndTrekPoint: String,
    val commercialBikePoint: String,
    val kekeTimeTaken: String,
    val kekeAllowedStatus: String,
    val bikeAllowedStatus: String,
    val areaChecked: String,
    val farePaidNaira: Int,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_routes")
data class SavedRouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originName: String,
    val destinationName: String,
    val distanceKm: Double,
    val estimatedMinutes: Int,
    val fareSummary: String,
    val trekRecommended: Boolean,
    val stepsSummary: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_places")
data class SavedPlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String,
    val placeName: String,
    val lat: Double,
    val lng: Double,
    val createdAtMillis: Long = System.currentTimeMillis()
)
