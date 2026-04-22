package com.example.scraply.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StampDao {
    @Query("SELECT * FROM stamps ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<StampEntity>>

    @Query("SELECT * FROM stamps")
    suspend fun getAllSync(): List<StampEntity>

    @Query("SELECT * FROM stamps WHERE id = :id")
    suspend fun getById(id: String): StampEntity?

    @Query("SELECT * FROM stamps WHERE createdAt BETWEEN :start AND :end ORDER BY createdAt ASC")
    fun observeByDateRange(start: Long, end: Long): Flow<List<StampEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stamp: StampEntity)

    @Update
    suspend fun update(stamp: StampEntity)

    @Delete
    suspend fun delete(stamp: StampEntity)

    @Query("DELETE FROM stamps WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collections ORDER BY isDefault DESC, createdAt ASC")
    fun observeAll(): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections")
    suspend fun getAllSync(): List<CollectionEntity>

    @Query("SELECT * FROM collections WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): CollectionEntity?

    @Query("SELECT * FROM collections WHERE id = :id")
    suspend fun getById(id: String): CollectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(collection: CollectionEntity)

    @Update
    suspend fun update(collection: CollectionEntity)

    @Query("DELETE FROM collections WHERE id = :id AND isDefault = 0")
    suspend fun deleteById(id: String)
}

@Dao
interface CollectionStampDao {
    @Query("SELECT * FROM collection_stamps WHERE collectionId = :collectionId")
    fun observeByCollection(collectionId: String): Flow<List<CollectionStampEntity>>

    @Query("SELECT stampId FROM collection_stamps WHERE collectionId = :collectionId")
    suspend fun stampIdsIn(collectionId: String): List<String>

    @Query("""
        SELECT s.* FROM stamps s
        INNER JOIN collection_stamps cs ON cs.stampId = s.id
        WHERE cs.collectionId = :collectionId
        ORDER BY s.createdAt DESC
    """)
    fun observeStampsIn(collectionId: String): Flow<List<StampEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(link: CollectionStampEntity)

    @Query("DELETE FROM collection_stamps WHERE collectionId = :collectionId AND stampId = :stampId")
    suspend fun remove(collectionId: String, stampId: String)

    @Query("DELETE FROM collection_stamps WHERE collectionId = :collectionId")
    suspend fun removeAllInCollection(collectionId: String)

    @Query("DELETE FROM collection_stamps WHERE stampId = :stampId")
    suspend fun removeAllForStamp(stampId: String)

    @Query("SELECT * FROM collection_stamps")
    suspend fun getAllSync(): List<CollectionStampEntity>
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects")
    suspend fun getAllSync(): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: ProjectEntity)

    @Update
    suspend fun update(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: String)
}
