package com.example.scraply.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stamps")
data class StampEntity(
    @PrimaryKey val id: String,
    val imageUri: String,
    val imageUrl: String? = null,
    val frameType: String = "postage_stamp",
    val title: String? = null,
    val caption: String? = null,
    val createdAt: Long,
    val userId: String? = null,
)

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isDefault: Boolean,
    val createdAt: Long,
    val userId: String? = null,
)

@Entity(tableName = "collection_stamps", primaryKeys = ["collectionId", "stampId"])
data class CollectionStampEntity(
    val collectionId: String,
    val stampId: String,
)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val backgroundType: String = "paper",
    val canvasJson: String = "{\"elements\":[]}",
    val createdAt: Long,
    val updatedAt: Long,
    val userId: String? = null,
)
