package com.bars.exchange.tracker.data.datasource.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a selected asset in the database.
 */
@Entity(tableName = "selected_assets")
data class SelectedAssetEntity(
    @PrimaryKey val id: String,
    val symbol: String,
    val name: String,
    val imageUrl: String?
)
