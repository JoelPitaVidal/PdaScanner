package com.example.pdascanner.localdatabase

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FotoDao {
    // Al devolver Long, Room nos da el ID autogenerado tras la inserción
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(foto: Foto): Long

    @Query("SELECT * FROM fotos WHERE subida = 0")
    suspend fun obtenerPendientesDeSubida(): List<Foto>

    @Query("UPDATE fotos SET subida = 1 WHERE id = :fotoId")
    suspend fun marcarComoSubida(fotoId: Int)

    // Verifica que este nombre coincida con el que usas en el Repository
    @Query("SELECT * FROM fotos WHERE albaranId = :albaranId")
    fun getFotosByAlbaran(albaranId: Long): Flow<List<Foto>>

    // Cambia qr_asociado por qrCodigo (o como lo hayas nombrado en Foto.kt)
    @Query("SELECT COUNT(*) FROM fotos WHERE qrCodigo = :qr")
    suspend fun contarFotosPorQr(qr: String): Int
}