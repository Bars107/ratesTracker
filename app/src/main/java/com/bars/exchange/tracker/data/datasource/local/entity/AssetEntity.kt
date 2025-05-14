package com.bars.exchange.tracker.data.datasource.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents an asset entity in the local Room database.
 */
@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey val symbol: String,
    val baseAsset: String,
    val quoteAsset: String,
    val status: String,
    val lastUpdated: Long = System.currentTimeMillis() // To help decide if data is stale
)
