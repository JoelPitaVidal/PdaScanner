package com.example.pdascanner.localdatabase

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbaranDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(albaran: Albaran): Long

    @Query("SELECT * FROM albaranes WHERE codigoTransporte = :at LIMIT 1")
    suspend fun getAlbaranPorTransporte(at: String): Albaran?

    @Query("SELECT * FROM albaranes")
    fun getAllAlbaranes(): Flow<List<Albaran>>
}