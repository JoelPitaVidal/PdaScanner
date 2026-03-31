package com.example.pdascanner.localdatabase

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(foto: Foto)

    @Query("SELECT * FROM fotos WHERE albaranId = :albaranId")
    fun getFotosByAlbaran(albaranId: Long): Flow<List<Foto>>
}