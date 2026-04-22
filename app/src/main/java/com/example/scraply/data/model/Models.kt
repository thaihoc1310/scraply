package com.example.scraply.data.model

import com.example.scraply.data.local.CollectionEntity
import com.example.scraply.data.local.ProjectEntity
import com.example.scraply.data.local.StampEntity

data class Stamp(
    val id: String,
    val imageUri: String,
    val frameType: String,
    val title: String?,
    val caption: String?,
    val createdAt: Long,
)

data class StampCollection(
    val id: String,
    val name: String,
    val isDefault: Boolean,
    val createdAt: Long,
)

data class ScrapbookProject(
    val id: String,
    val name: String,
    val backgroundType: String,
    val canvasJson: String,
    val createdAt: Long,
    val updatedAt: Long,
)

fun StampEntity.toDomain() = Stamp(id, imageUri, frameType, title, caption, createdAt)
fun CollectionEntity.toDomain() = StampCollection(id, name, isDefault, createdAt)
fun ProjectEntity.toDomain() =
    ScrapbookProject(id, name, backgroundType, canvasJson, createdAt, updatedAt)
